package com.example.htmlmud.domain.actor.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class VirtualActor<T> {

  // 每個 Actor 都有自己的信箱
  protected final BlockingQueue<T> mailbox = new LinkedBlockingQueue<>();
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final String actorId;
  private volatile Thread actorThread;

  public VirtualActor(String actorId) {
    this.actorId = actorId;
  }

  // 啟動 Actor：這會生成一個專屬的 Virtual Thread
  public void start() {
    if (started.compareAndSet(false, true)) {
      running.set(true); // 確保啟動時狀態為 true
      this.actorThread = Thread.ofVirtual().name(actorId).unstarted(this::runLoop);
      this.actorThread.start();
    } else {
      log.warn("[{}] 已經啟動，忽略重複的啟動請求。", actorId);
    }
  }

  // 非阻塞投遞訊息 (給外部呼叫用)
  public void send(T message) {
    mailbox.offer(message);
  }

  // 核心迴圈
  private void runLoop() {
    log.info("[{}] started on thread: {}", actorId, Thread.currentThread());

    try {
      while (running.get()) {
        // 這裡會 Block，虛擬執行緒會 Unmount，不佔用 OS Thread
        T message = mailbox.take();
        handleMessage(message);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("[{}] interrupted.", actorId);
    } catch (Exception e) {
      log.error("[{}] encountered unexpected error", actorId, e);
    } finally {
      running.set(false);
      log.info("[{}] has terminated permanently.", actorId);
    }
  }

  public void stop() {
    if (running.compareAndSet(true, false)) { // 確保只執行一次
      log.info("Stopping [{}]", actorId);
      if (actorThread != null) {
        actorThread.interrupt(); // 中斷 take() 的阻塞狀態
      }
    }
  }

  // 子類別實作具體邏輯
  protected abstract void handleMessage(T message);
}
