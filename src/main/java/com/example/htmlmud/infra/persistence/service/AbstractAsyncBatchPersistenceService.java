package com.example.htmlmud.infra.persistence.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * 通用非同步批次持久化抽象基類 (Write-Behind Cache Pattern)
 * 封裝虛擬執行緒消費迴圈、批量閥值寫入、優雅關機與排空邏輯
 */
@Slf4j
public abstract class AbstractAsyncBatchPersistenceService<T> {

  private final BlockingQueue<T> saveQueue = new LinkedBlockingQueue<>();
  private volatile boolean running = true;

  protected abstract String getWorkerThreadName();

  protected abstract void flushBatch(List<T> batch);

  public void saveAsync(T record) {
    if (record == null) {
      return;
    }
    if (!saveQueue.offer(record)) {
      log.error("[{}] 存檔佇列已滿！資料庫寫入可能過慢，資料遺失風險: {}", getWorkerThreadName(), record);
    }
  }

  @PostConstruct
  public void init() {
    Thread.ofVirtual().name(getWorkerThreadName()).start(this::processQueue);
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

  @PreDestroy
  public void shutdown() {
    log.info("Shutting down [{}] PersistenceService...", getWorkerThreadName());
    running = false;

    List<T> remaining = new ArrayList<>();
    saveQueue.drainTo(remaining);

    if (!remaining.isEmpty()) {
      log.info("[{}] Flushing remaining {} records...", getWorkerThreadName(), remaining.size());
      flushBatch(remaining);
      remaining.clear();
    }
    log.info("[{}] PersistenceService shutdown complete.", getWorkerThreadName());
  }
}
