package com.example.htmlmud.infra.persistence.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * 通用非同步批次持久化抽象基類 (Write-Behind Cache Pattern)
 * 封裝虛擬執行緒消費迴圈、批量閥值寫入、有界背壓與優雅關機排空邏輯
 */
@Slf4j
public abstract class AbstractAsyncBatchPersistenceService<T> {

  public static final int DEFAULT_QUEUE_CAPACITY = 10_000;

  private final BlockingQueue<T> saveQueue;
  private final AtomicBoolean accepting = new AtomicBoolean(true);
  private volatile boolean running = true;
  private Thread workerThread;

  public AbstractAsyncBatchPersistenceService() {
    this(DEFAULT_QUEUE_CAPACITY);
  }

  public AbstractAsyncBatchPersistenceService(int queueCapacity) {
    this.saveQueue = new LinkedBlockingQueue<>(queueCapacity > 0 ? queueCapacity : DEFAULT_QUEUE_CAPACITY);
  }

  protected abstract String getWorkerThreadName();

  protected abstract void flushBatch(List<T> batch);

  public void saveAsync(T record) {
    if (record == null) {
      return;
    }
    if (!accepting.get()) {
      log.warn("[{}] 持久化服務關閉中，拒收新資料: {}", getWorkerThreadName(), record);
      return;
    }
    if (!saveQueue.offer(record)) {
      log.error("[{}] 存檔佇列已滿 (容量: {})！資料庫寫入可能過慢，觸發背壓拒收: {}",
          getWorkerThreadName(), saveQueue.size(), record);
    }
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
          flushBatch(new ArrayList<>(batch));
          batch.clear();
        }
      } catch (InterruptedException e) {
        log.warn("[{}] DB Writer thread interrupted, executing final emergency drain.", getWorkerThreadName());
        // 中斷時排空 batch 與 saveQueue
        if (!batch.isEmpty()) {
          try {
            flushBatch(new ArrayList<>(batch));
            batch.clear();
          } catch (Exception ex) {
            log.error("[{}] Error flushing local batch on interrupt", getWorkerThreadName(), ex);
          }
        }
        List<T> remaining = new ArrayList<>();
        saveQueue.drainTo(remaining);
        if (!remaining.isEmpty()) {
          try {
            flushBatch(remaining);
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
      flushBatch(pending);
    }
  }

  @PreDestroy
  public void shutdown() {
    log.info("Shutting down [{}] PersistenceService...", getWorkerThreadName());
    accepting.set(false);
    running = false;

    if (workerThread != null) {
      // 喚醒可能在 poll 阻塞的 worker
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

    // 最後兜底保障：若 worker 逾時仍有遺留
    List<T> remaining = new ArrayList<>();
    saveQueue.drainTo(remaining);

    if (!remaining.isEmpty()) {
      log.info("[{}] Fallback flushing remaining {} records...", getWorkerThreadName(), remaining.size());
      try {
        flushBatch(remaining);
      } catch (Exception e) {
        log.error("[{}] Error fallback flushing remaining records during shutdown", getWorkerThreadName(), e);
      }
      remaining.clear();
    }
    log.info("[{}] PersistenceService shutdown complete.", getWorkerThreadName());
  }
}
