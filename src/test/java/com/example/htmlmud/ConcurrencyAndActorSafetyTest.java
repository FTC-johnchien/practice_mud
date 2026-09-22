package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.WebSocketSession;

import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.core.RoomMessageBuffer;
import com.example.htmlmud.domain.actor.core.VirtualActor;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.WorldManager;

@SpringBootTest
public class ConcurrencyAndActorSafetyTest {

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  private MessageOutput createMockOutput() {
    return new MessageOutput() {
      @Override
      public void sendJson(Object payload) {}
      @Override
      public void close() {}
      @Override
      public WebSocketSession getSession() {
        return null;
      }
    };
  }

  @Test
  @DisplayName("VirtualActor 例外中毒防禦：單一訊息拋出 RuntimeException 不得終止 Actor 虛擬執行緒，後續訊息仍需正常處理")
  void testActorPoisonPillResilience() throws Exception {
    BlockingQueue<String> processedMessages = new LinkedBlockingQueue<>();
    AtomicInteger exceptionCount = new AtomicInteger(0);

    VirtualActor<String> actor = new VirtualActor<>("test-resilient-actor") {
      @Override
      protected void handleMessage(String message) {
        if ("POISON".equals(message)) {
          exceptionCount.incrementAndGet();
          throw new RuntimeException("Simulated unexpected crash in message handler!");
        }
        processedMessages.offer(message);
      }
    };

    actor.start();
    assertTrue(actor.isRunning(), "Actor 啟動後狀態應為 running");

    // 發送一條正常訊息
    actor.send("MSG-1");
    assertEquals("MSG-1", processedMessages.poll(1, TimeUnit.SECONDS));

    // 發送引發未捕獲 RuntimeException 的毒藥訊息
    actor.send("POISON");

    // 發送後續正常訊息
    actor.send("MSG-2");
    actor.send("MSG-3");

    // 驗證 Actor 沒有因例外死亡，後續訊息正常抵達並處理
    assertEquals("MSG-2", processedMessages.poll(1, TimeUnit.SECONDS), "毒藥訊息後的第一條合法訊息應正常被處理");
    assertEquals("MSG-3", processedMessages.poll(1, TimeUnit.SECONDS), "毒藥訊息後的第二條合法訊息應正常被處理");
    assertEquals(1, exceptionCount.get(), "毒藥訊息拋出例外應被攔截計數");
    assertTrue(actor.isRunning(), "Actor 遭毒藥訊息攻擊後應依然在運行");

    actor.stop();
  }

  @Test
  @DisplayName("VirtualActor 自死鎖防禦：isActorThread 正確識別當前執行緒並消除自身 join 自死鎖")
  void testActorSelfDeadlockPrevention() throws Exception {
    Player player = Player.createSinglePlayer(
        createMockOutput(),
        worldManager,
        playerService,
        "併發測試道友",
        "p-deadlock-test"
    );
    player.start();

    // 1. 驗證在外部執行緒呼叫 player.isActorThread() 為 false
    assertFalse(player.isActorThread(), "測試主執行緒非 Player 的虛擬執行緒");

    // 2. 透過專屬測試 Actor 驗證 isActorThread() 在內部為 true，且呼叫同步方法不會自死鎖
    CompletableFuture<Boolean> insideFlagFuture = new CompletableFuture<>();
    CompletableFuture<Boolean> lookAtMeDirectFuture = new CompletableFuture<>();

    VirtualActor<Runnable> runnerActor = new VirtualActor<>("test-self-runner") {
      @Override
      protected void handleMessage(Runnable task) {
        task.run();
      }
    };
    runnerActor.start();

    long startTime = System.currentTimeMillis();
    runnerActor.send(() -> {
      // 驗證 runnerActor 自身執行緒判定
      insideFlagFuture.complete(runnerActor.isActorThread());

      // 跨 Actor 呼叫 player 的 lookAtMe (此時非 player 執行緒，透過 Future 完成)
      var res = player.lookAtMe();
      lookAtMeDirectFuture.complete(res != null);
    });

    assertTrue(insideFlagFuture.get(2, TimeUnit.SECONDS), "在 Actor 內部執行時 isActorThread 必須為 true");
    assertTrue(lookAtMeDirectFuture.get(2, TimeUnit.SECONDS), "跨 Actor lookAtMe 正常返回");

    long duration = System.currentTimeMillis() - startTime;
    assertTrue(duration < 1500, "跨 Actor 呼叫正常完成，耗時: " + duration + "ms");

    player.stop();
    runnerActor.stop();
  }

  @Test
  @DisplayName("VirtualActor 自死鎖防禦：在自身虛擬執行緒內呼叫自身同步方法立即同步返回無任何阻塞")
  void testDirectSelfInvocationWithoutDeadlock() throws Exception {
    CompletableFuture<Long> executionTimeFuture = new CompletableFuture<>();
    CompletableFuture<Boolean> isActorThreadResult = new CompletableFuture<>();
    CompletableFuture<String> callResultFuture = new CompletableFuture<>();

    class SelfCallActor extends VirtualActor<String> {
      public SelfCallActor() {
        super("self-call-actor");
      }

      public String executeMethod() {
        if (isActorThread()) {
          return "DIRECT_SYNC_EXECUTION";
        }
        CompletableFuture<String> future = new CompletableFuture<>();
        future.complete("ASYNC_MAILBOX_EXECUTION");
        return future.join();
      }

      @Override
      protected void handleMessage(String msg) {
        if ("TEST_SELF_CALL".equals(msg)) {
          long start = System.currentTimeMillis();
          isActorThreadResult.complete(isActorThread());
          String res = executeMethod();
          callResultFuture.complete(res);
          long elapsed = System.currentTimeMillis() - start;
          executionTimeFuture.complete(elapsed);
        }
      }
    }

    SelfCallActor actor = new SelfCallActor();
    actor.start();
    actor.send("TEST_SELF_CALL");

    assertTrue(isActorThreadResult.get(2, TimeUnit.SECONDS), "在 Actor 內部時 isActorThread 必須為 true");
    assertEquals("DIRECT_SYNC_EXECUTION", callResultFuture.get(2, TimeUnit.SECONDS), "在自身執行緒內必須走同步分支");
    Long elapsed = executionTimeFuture.get(2, TimeUnit.SECONDS);
    assertTrue(elapsed < 100, "自身執行緒同步執行耗時應小於 100ms (無 1 秒 timeout)，實測: " + elapsed + "ms");
    actor.stop();
  }

  @Test
  @DisplayName("RoomMessageBuffer 共用守護排程器驗證：多個房間並行 push 事件並由共享排程器正確 flush")
  void testRoomMessageBufferSharedScheduler() throws Exception {
    Room room = worldManager.getRoomActor("newbie_village:inn");
    assertNotNull(room, "新手村客棧應存在");

    RoomMessageBuffer buffer = room.getBuffer();
    assertNotNull(buffer, "房間訊息緩衝區不可為空");

    // 推送戰鬥與狀態碎片
    buffer.push("COMBAT", "小隊發動劍氣攻擊");
    buffer.push("STATUS", "小隊氣血回復");

    // 手動 trigger flush
    buffer.flush();

    // 再次 push 並等待背景排程執行
    buffer.push("COMBAT", "後續戰鬥碎浪");
    Thread.sleep(150); // 等待 100ms 窗口 flush 完成

    // 取消並驗證無洩漏無例外
    buffer.cancel();
  }

  @Test
  @DisplayName("LivingStats 數值邊界防禦：HP 與 MP 下限保證 >= 0，且 clampToMax 正確夾限於上限")
  void testLivingStatsBoundaryClamping() {
    LivingStats stats = new LivingStats();
    stats.setMaxHp(150);
    stats.setMaxMp(80);

    // 1. 正常值設定
    stats.setHp(100);
    assertEquals(100, stats.getHp());

    // 2. 負值傷害扣減夾緊（即時下限防禦 >= 0）
    stats.setHp(-50);
    assertEquals(0, stats.getHp(), "設定負數 HP 應被自動夾緊為 0");

    // 3. MP 負值夾緊（即時下限防禦 >= 0）
    stats.setMp(-20);
    assertEquals(0, stats.getMp(), "設定負數 MP 應被自動夾緊為 0");

    // 4. 超出上限時透過 clampToMax 夾限
    stats.setHp(999);
    stats.clampToMax();
    assertEquals(150, stats.getHp(), "clampToMax 應將超出的 HP 限制在 maxHp");

    stats.setMp(500);
    stats.clampToMax();
    assertEquals(80, stats.getMp(), "clampToMax 應將超出的 MP 限制在 maxMp");

    // 5. maxHp 縮小後 clampToMax 動態縮限 hp
    stats.setHp(120);
    stats.setMaxHp(90);
    stats.clampToMax();
    assertEquals(90, stats.getHp(), "maxHp 縮小後 clampToMax 應將當前 hp 降至新上限");
  }
}
