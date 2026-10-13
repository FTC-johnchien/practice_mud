package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.htmlmud.infra.persistence.service.AbstractAsyncBatchPersistenceService;
import com.example.htmlmud.infra.persistence.service.SubmitResult;

/**
 * ASYNC-03: 持久化提交與停機生命週期原子化、滿載明確回報與競態測試
 */
class AsyncPersistenceLifecycleTest {

  @Test
  @DisplayName("場景 1：有界佇列滿載時觸發明確背壓拒收 (REJECTED_QUEUE_FULL)，杜絕靜默遺失")
  void testExplicitBackpressureOnQueueFull() {
    // 建立一個極小容量 (5 筆) 且不啟動 worker 消費的測試服務
    List<String> flushed = Collections.synchronizedList(new ArrayList<>());
    var service = new AbstractAsyncBatchPersistenceService<String>(5) {
      @Override
      protected String getWorkerThreadName() {
        return "test-bp-worker";
      }

      @Override
      protected void flushBatch(List<String> batch) {
        flushed.addAll(batch);
      }
    };

    // 填滿 5 筆
    for (int i = 0; i < 5; i++) {
      SubmitResult res = service.submitAsync("item-" + i);
      assertThat(res).isEqualTo(SubmitResult.ACCEPTED);
      assertThat(res.isAccepted()).isTrue();
    }
    assertThat(service.getQueueSize()).isEqualTo(5);

    // 第 6 筆應明確回傳 REJECTED_QUEUE_FULL，不再靜默吞掉
    SubmitResult overflowResult = service.submitAsync("item-overflow");
    assertThat(overflowResult).isEqualTo(SubmitResult.REJECTED_QUEUE_FULL);
    assertThat(overflowResult.isRejected()).isTrue();
    assertThat(service.getRejectedQueueFullCount()).isEqualTo(1);

    // 空資料回傳 REJECTED_NULL
    assertThat(service.submitAsync(null)).isEqualTo(SubmitResult.REJECTED_NULL);
  }

  @Test
  @DisplayName("場景 2：停機後呼叫 submitAsync 明確回傳 REJECTED_SHUTTING_DOWN")
  void testRejectionAfterShutdown() {
    List<String> flushed = Collections.synchronizedList(new ArrayList<>());
    var service = new AbstractAsyncBatchPersistenceService<String>(100) {
      @Override
      protected String getWorkerThreadName() {
        return "test-shutdown-reject-worker";
      }

      @Override
      protected void flushBatch(List<String> batch) {
        flushed.addAll(batch);
      }
    };
    service.init();
    service.submitAsync("item-1");
    service.shutdown();

    assertThat(service.isAccepting()).isFalse();

    // 停機後提交必然被明確拒絕
    SubmitResult afterShutdown = service.submitAsync("item-after-shutdown");
    assertThat(afterShutdown).isEqualTo(SubmitResult.REJECTED_SHUTTING_DOWN);
    assertThat(afterShutdown.isRejected()).isTrue();
    assertThat(service.getRejectedShuttingDownCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("場景 3：高併發交錯競態證明 (CountDownLatch) —— 停機瞬間提交不是成功落盤就是明確拒絕，零滯留丟失")
  void testConcurrentSubmissionAndShutdownRaceCondition() throws Exception {
    List<String> flushed = Collections.synchronizedList(new ArrayList<>());
    var service = new AbstractAsyncBatchPersistenceService<String>(2000) {
      @Override
      protected String getWorkerThreadName() {
        return "test-race-worker";
      }

      @Override
      protected void flushBatch(List<String> batch) {
        flushed.addAll(batch);
      }
    };
    service.init();

    int totalThreads = 10;
    int itemsPerThread = 50;
    int totalItems = totalThreads * itemsPerThread;

    CountDownLatch startGate = new CountDownLatch(1);
    CountDownLatch doneGate = new CountDownLatch(totalThreads);
    AtomicInteger acceptedCount = new AtomicInteger(0);
    AtomicInteger rejectedCount = new AtomicInteger(0);

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int t = 0; t < totalThreads; t++) {
        final int threadId = t;
        executor.submit(() -> {
          try {
            startGate.await();
            for (int i = 0; i < itemsPerThread; i++) {
              SubmitResult res = service.submitAsync("t" + threadId + "-item-" + i);
              if (res.isAccepted()) {
                acceptedCount.incrementAndGet();
              } else {
                rejectedCount.incrementAndGet();
              }
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            doneGate.countDown();
          }
        });
      }

      // 所有線程就緒，鳴槍起跑
      startGate.countDown();

      // 在寫入期間隨機微幅等待後立即觸發 shutdown，製造高併發交錯競態
      Thread.sleep(5);
      service.shutdown();

      // 等待所有提交線程結束
      boolean completed = doneGate.await(5, TimeUnit.SECONDS);
      assertThat(completed).isTrue();
    }

    // 核心數學定理驗收：
    // 1. 每筆提交要麼是 ACCEPTED，要麼是 REJECTED，總數等於 totalItems (無靜默失蹤)
    assertThat(acceptedCount.get() + rejectedCount.get())
        .as("每筆呼叫必須有明確的 ACCEPTED 或 REJECTED 結果契約")
        .isEqualTo(totalItems);

    // 2. 凡是回傳 ACCEPTED 的紀錄，100% 已經被 flush 落盤 (無任何遺失)
    assertThat(flushed.size())
        .as("已接受 (ACCEPTED) 的紀錄數量必須 100% 等於落盤數量 (flushed)")
        .isEqualTo(acceptedCount.get());

    // 3. 停機後佇列必須為 0 (無任何紀錄滯留死鎖在隊列中)
    assertThat(service.getQueueSize())
        .as("停機後隊列必須為空")
        .isEqualTo(0);
  }

  @Test
  @DisplayName("場景 4：Worker 遭遇中斷 (InterruptedException) 時觸發 Emergency Drain 保障落盤")
  void testEmergencyDrainOnWorkerInterrupt() throws Exception {
    List<String> flushed = Collections.synchronizedList(new ArrayList<>());
    var service = new AbstractAsyncBatchPersistenceService<String>(100) {
      @Override
      protected String getWorkerThreadName() {
        return "test-interrupt-drain-worker";
      }

      @Override
      protected void flushBatch(List<String> batch) {
        flushed.addAll(batch);
      }
    };
    service.init();

    // 寫入 15 筆
    for (int i = 0; i < 15; i++) {
      service.submitAsync("item-" + i);
    }

    // 呼叫 shutdown (會發送 interrupt 並執行 emergency drain)
    service.shutdown();

    assertThat(flushed.size())
        .as("Worker 中斷並關機後，所有 15 筆資料應被 Emergency Drain 完整落盤")
        .isEqualTo(15);
    assertThat(service.getQueueSize()).isEqualTo(0);
  }
}
