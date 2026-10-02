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

    while (running) {
      try {
        T record = saveQueue.poll(1, TimeUnit.SECONDS);
        if (record != null) {
          batch.add(record);
        }

        if (batch.size() >= 50 || (record == null && !batch.isEmpty())) {
          flushBatch(new ArrayList<>(batch));
          batch.clear();
        }

        if (!saveQueue.isEmpty() && batch.size() < 100) {
          saveQueue.drainTo(batch, 100 - batch.size());
          flushBatch(new ArrayList<>(batch));
          batch.clear();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("[{}] DB Writer thread interrupted.", getWorkerThreadName());
        break;
      } catch (Exception e) {
        log.error("[{}] DB Writer loop error", getWorkerThreadName(), e);
      }
    }
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
      try {
        workerThread.join(TimeUnit.SECONDS.toMillis(3));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("[{}] Interrupted while awaiting worker thread shutdown", getWorkerThreadName());
      }
    }

    List<T> remaining = new ArrayList<>();
    saveQueue.drainTo(remaining);

    if (!remaining.isEmpty()) {
      log.info("[{}] Flushing remaining {} records...", getWorkerThreadName(), remaining.size());
      try {
        flushBatch(remaining);
      } catch (Exception e) {
        log.error("[{}] Error flushing remaining records during shutdown", getWorkerThreadName(), e);
      }
      remaining.clear();
    }
    log.info("[{}] PersistenceService shutdown complete.", getWorkerThreadName());
  }
}
