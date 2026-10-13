package com.example.htmlmud.infra.persistence.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * 通用非同步批次持久化抽象基類 (Write-Behind Cache Pattern - ASYNC-03 加固版)
 * 封裝虛擬執行緒消費迴圈、批量閥值寫入、讀寫鎖生命週期原子協定、有界背壓與優雅關機排空邏輯
 */
@Slf4j
public abstract class AbstractAsyncBatchPersistenceService<T> {

  public static final int DEFAULT_QUEUE_CAPACITY = 10_000;

  private final int queueCapacity;
  private final BlockingQueue<T> saveQueue;
  private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock();

  // 狀態變數：在 lifecycleLock 保護下變更
  private boolean accepting = true;
  private volatile boolean running = true;
  private Thread workerThread;

  // 可觀測度量指標 (Observability Metrics)
  private final AtomicLong submittedCount = new AtomicLong(0);
  private final AtomicLong acceptedCount = new AtomicLong(0);
  private final AtomicLong flushedCount = new AtomicLong(0);
  private final AtomicLong rejectedShuttingDownCount = new AtomicLong(0);
  private final AtomicLong rejectedQueueFullCount = new AtomicLong(0);

  public AbstractAsyncBatchPersistenceService() {
    this(DEFAULT_QUEUE_CAPACITY);
  }

  public AbstractAsyncBatchPersistenceService(int queueCapacity) {
    this.queueCapacity = queueCapacity > 0 ? queueCapacity : DEFAULT_QUEUE_CAPACITY;
    this.saveQueue = new LinkedBlockingQueue<>(this.queueCapacity);
  }

  protected abstract String getWorkerThreadName();

  protected abstract void flushBatch(List<T> batch);

  /**
   * 內部安全呼叫批次持久化並維護度量計數
   */
  private void flushBatchInternal(List<T> batch) {
    if (batch == null || batch.isEmpty()) {
      return;
    }
    try {
      flushBatch(batch);
      flushedCount.addAndGet(batch.size());
    } catch (Exception e) {
      log.error("[{}] Error flushing batch of size {}", getWorkerThreadName(), batch.size(), e);
      throw e;
    }
  }

  /**
   * 提交待持久化紀錄並獲取明確結果契約 (ASYNC-03)
   * 遵循讀寫鎖生命週期原子協定，杜絕關閉排空後的靜默入列遺失
   */
  public SubmitResult submitAsync(T record) {
    if (record == null) {
      return SubmitResult.REJECTED_NULL;
    }
    submittedCount.incrementAndGet();

    lifecycleLock.readLock().lock();
    try {
      if (!accepting) {
        rejectedShuttingDownCount.incrementAndGet();
        log.warn("[{}] 持久化服務關閉中，拒收新資料: {}", getWorkerThreadName(), record);
        return SubmitResult.REJECTED_SHUTTING_DOWN;
      }
      boolean queued = saveQueue.offer(record);
      if (!queued) {
        rejectedQueueFullCount.incrementAndGet();
        log.error("[{}] 存檔佇列已滿 (容量: {})！資料庫寫入可能過慢，觸發背壓拒收: {}",
            getWorkerThreadName(), saveQueue.size(), record);
        return SubmitResult.REJECTED_QUEUE_FULL;
      }
      acceptedCount.incrementAndGet();
      return SubmitResult.ACCEPTED;
    } finally {
      lifecycleLock.readLock().unlock();
    }
  }

  /**
   * 向下相容的非同步儲存呼叫
   */
  public void saveAsync(T record) {
    submitAsync(record);
  }

  @PostConstruct
  public void init() {
    workerThread = Thread.ofVirtual().name(getWorkerThreadName()).start(this::processQueue);
  }

  private void processQueue() {
    log.info("[{}] Write-Behind DB Writer started.", getWorkerThreadName());
    List<T> batch = new ArrayList<>();

    while (running || !saveQueue.isEmpty() || !batch.isEmpty()) {
      try {
        T record = null;
        if (running && saveQueue.isEmpty() && batch.isEmpty()) {
          record = saveQueue.poll(1, TimeUnit.SECONDS);
        } else {
          record = saveQueue.poll();
        }

        if (record != null) {
          batch.add(record);
        }

        if (!saveQueue.isEmpty() && batch.size() < 100) {
          saveQueue.drainTo(batch, 100 - batch.size());
        }

        // 當累積滿 50 筆、或輪詢超時且有積累、或處於關閉階段 (running == false) 時觸發 flush
        if (batch.size() >= 50 || (!running && !batch.isEmpty()) || (record == null && !batch.isEmpty())) {
          flushBatchInternal(new ArrayList<>(batch));
          batch.clear();
        }
      } catch (InterruptedException e) {
        log.warn("[{}] DB Writer thread interrupted, executing final emergency drain.", getWorkerThreadName());
        // 中斷時排空 batch 與 saveQueue
        if (!batch.isEmpty()) {
          try {
            flushBatchInternal(new ArrayList<>(batch));
            batch.clear();
          } catch (Exception ex) {
            log.error("[{}] Error flushing local batch on interrupt", getWorkerThreadName(), ex);
          }
        }
        List<T> remaining = new ArrayList<>();
        saveQueue.drainTo(remaining);
        if (!remaining.isEmpty()) {
          try {
            flushBatchInternal(remaining);
          } catch (Exception ex) {
            log.error("[{}] Error flushing remaining queue on interrupt", getWorkerThreadName(), ex);
          }
        }
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        log.error("[{}] DB Writer loop error", getWorkerThreadName(), e);
      }
    }
    log.info("[{}] Write-Behind DB Writer loop ended cleanly.", getWorkerThreadName());
  }

  /**
   * 同步排空並立即可視化提交（可用於測試或明確存檔點）
   */
  public synchronized void flushImmediately() {
    List<T> pending = new ArrayList<>();
    saveQueue.drainTo(pending);
    if (!pending.isEmpty()) {
      flushBatchInternal(pending);
    }
  }

  @PreDestroy
  public void shutdown() {
    log.info("Shutting down [{}] PersistenceService...", getWorkerThreadName());

    // 1. 取得 WriteLock，原子化切斷接受通道並標記停止 (ASYNC-03 核心防護)
    lifecycleLock.writeLock().lock();
    try {
      accepting = false;
      running = false;
    } finally {
      lifecycleLock.writeLock().unlock();
    }
    // 至此，所有先前拿到 readLock 的線程必已完成 offer；後續任何 submitAsync 必被拒絕！
    // saveQueue 在此後絕不會有任何新元素偷偷溜進來！

    // 2. 中斷 worker 並等待其優雅排空
    if (workerThread != null) {
      workerThread.interrupt();
      try {
        workerThread.join(TimeUnit.SECONDS.toMillis(3));
        if (workerThread.isAlive()) {
          log.warn("[{}] Worker thread did not terminate within 3s", getWorkerThreadName());
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("[{}] Interrupted while awaiting worker thread shutdown", getWorkerThreadName());
      }
    }

    // 3. 兜底排空保障 (Drain Protocol Final Safeguard)
    List<T> remaining = new ArrayList<>();
    saveQueue.drainTo(remaining);

    if (!remaining.isEmpty()) {
      log.info("[{}] Fallback flushing remaining {} records...", getWorkerThreadName(), remaining.size());
      try {
        flushBatchInternal(remaining);
      } catch (Exception e) {
        log.error("[{}] Error fallback flushing remaining records during shutdown", getWorkerThreadName(), e);
      }
      remaining.clear();
    }
    log.info("[{}] PersistenceService shutdown complete. Metrics: submitted={}, accepted={}, flushed={}, rejShutdown={}, rejQueueFull={}",
        getWorkerThreadName(), submittedCount.get(), acceptedCount.get(), flushedCount.get(),
        rejectedShuttingDownCount.get(), rejectedQueueFullCount.get());
  }

  // --- 查詢度量與狀態 ---

  public int getQueueSize() {
    return saveQueue.size();
  }

  public int getQueueCapacity() {
    return queueCapacity;
  }

  public boolean isAccepting() {
    lifecycleLock.readLock().lock();
    try {
      return accepting;
    } finally {
      lifecycleLock.readLock().unlock();
    }
  }

  public boolean isRunning() {
    return running;
  }

  public long getSubmittedCount() {
    return submittedCount.get();
  }

  public long getAcceptedCount() {
    return acceptedCount.get();
  }

  public long getFlushedCount() {
    return flushedCount.get();
  }

  public long getRejectedShuttingDownCount() {
    return rejectedShuttingDownCount.get();
  }

  public long getRejectedQueueFullCount() {
    return rejectedQueueFullCount.get();
  }
}
