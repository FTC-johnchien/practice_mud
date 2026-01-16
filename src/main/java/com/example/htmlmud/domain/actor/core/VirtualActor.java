package com.example.htmlmud.domain.actor.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import com.example.htmlmud.protocol.ActorMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class VirtualActor<T> {

  // 每個 Actor 都有自己的信箱
  protected final BlockingQueue<T> mailbox = new LinkedBlockingQueue<>();
  private final AtomicBoolean running = new AtomicBoolean(true);
  private final String actorName;

  public VirtualActor(String actorName) {
    this.actorName = actorName;
  }

  // 啟動 Actor：這會生成一個專屬的 Virtual Thread
  public void start() {
    Thread.ofVirtual().name("actor-" + actorName).start(this::runLoop);
  }

  // 非阻塞投遞訊息 (給外部呼叫用)
  public void send(T message) {
    mailbox.offer(message);
  }

  // 核心迴圈
  private void runLoop() {
    log.info("Actor [{}] started on thread: {}", actorName, Thread.currentThread());

    try {
      while (running.get()) {
        // 這裡會 Block，虛擬執行緒會 Unmount，不佔用 OS Thread
        T message = mailbox.take();
        log.info("take -----------------------------------------------------");
        handleMessage(message);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Actor [{}] interrupted.", actorName);
    } catch (Exception e) {
      log.error("Actor [{}] encountered unexpected error", actorName, e);
    }
  }

  public void stop() {
    if (running.compareAndSet(true, false)) { // 確保只執行一次
      log.info("Stopping Actor [{}]", actorName);
      // 重要：送出一個中斷訊號給執行該 Actor 的 Virtual Thread
      // 因為 runLoop 卡在 mailbox.take()，如果不 interrupt，它會永遠卡在那裡直到有新訊息
      // 注意：這需要你在 start() 時保存 Thread 參照，或者發送一個 Poison Pill
    }
  }

  // 子類別實作具體邏輯
  protected abstract void handleMessage(T message);
}
