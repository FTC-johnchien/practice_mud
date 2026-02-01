package com.example.htmlmud.infra.server;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.command.CommandRequest;
import com.example.htmlmud.application.service.CommandQueueService;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.service.CombatService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServerEngine implements Runnable {

  private final CommandQueueService commandQueueService;

  private final WorldManager worldManager;

  private final CombatService combatService;


  // 全域計數器，伺服器啟動後開始累加
  private final AtomicLong globalTickCounter = new AtomicLong(0);


  private boolean running = true;

  // 設定 Tick 頻率：每秒 10 次 (即每 100ms 跑一次)
  private static final int TICK_RATE_MS = 1000;

  // 用來統計：這個 Tick 處理了多少指令/事件
  private int commandsProcessed = 0;

  @Override
  public void run() {
    System.out.println("MUD Server Engine Started.");

    while (running) {
      long currentTick = globalTickCounter.incrementAndGet();
      // 1. 【開始計時】
      long startTime = System.nanoTime();
      commandsProcessed = 0; // 重置計數器

      try {
        // 2. 【處理核心邏輯】 (這就是遊戲的一幀)
        System.out.println("Tick:" + currentTick);
        tick(currentTick, System.currentTimeMillis());

      } catch (Exception e) {
        e.printStackTrace(); // 防止單一錯誤導致伺服器崩潰
      }

      // 3. 【結束計時】
      long endTime = System.nanoTime();
      long durationNanos = endTime - startTime;
      long durationMs = durationNanos / 1_000_000;

      GameMetrics.updateTickDuration(durationNanos);
      commandsProcessed = GameMetrics.getAndResetCommandCount();


      // 4. 【效能監控】 (這裡回答了你上一題的問題)
      // 如果這個 Tick 跑超過 50ms，或者處理了很多指令，顯示 Log
      if (durationMs > 50 || commandsProcessed > 0) {
        // 這就是你要的：「執行 xx 條指令，花費 xx 毫秒」
        System.out.printf("[Tick] Cmds: %d | Time: %d ms\n", commandsProcessed, durationMs);
      }

      // 5. 【休眠維持節奏】
      // 如果處理只花了 5ms，我們就睡 95ms，確保每個 Tick 間隔大約是 100ms
      long sleepTime = TICK_RATE_MS - durationMs;
      if (sleepTime > 0) {
        try {
          Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      } else {
        // 如果 sleepTime < 0，代表伺服器 LAG 了 (處理超過 100ms)
        System.out.println("[WARNING] Server is overloaded! Tick took " + durationMs + "ms");
      }
    }

    System.out.println("🛑 ServerEngine 已停止。");
  }

  /**
   * 這裡匯總所有系統的推進
   */
  private void tick(long currentTick, long now) {
    // A. 處理玩家輸入指令 (從 Queue 拿出指令執行)
    // 假設 CommandService 有一個 Queue 存放玩家輸入
    // commandsProcessed += CommandService.processQueue();
    // --- A. 處理玩家指令 ---
    processCommands();

    // B. 驅動世界心跳 (天氣、重生、全頻廣播)
    // World.tick();

    // C. 驅動戰鬥系統 (這就是解決你頓挫感的地方)
    combatService.tick();

    // D. 驅動所有區域/房間 (如果你的 Mob 是掛在房間下的)
    // RoomManager.tickAll();
    processRooms(currentTick, now);
  }

  public void stop() {
    this.running = false;
  }

  private void processCommands() {
    // 設定一個上限，避免有人惡意洗頻導致這一次 tick 跑不完卡死
    // 例如：每個 tick 最多處理 1000 條指令
    int processedCount = 0;
    int maxCommandsPerTick = 1000;

    while (!commandQueueService.isEmpty() && processedCount < maxCommandsPerTick) {

      // 1. 取出指令
      CommandRequest request = commandQueueService.poll();
      if (request == null)
        break;

      try {
        // 2. 產生 Trace ID 並交給 Player Actor 處理
        // 這樣可以確保指令經過 Actor 的 Behavior (處理登入、戰鬥狀態等)
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        request.getPlayer().command(traceId, request.getCommand());

      } catch (Exception e) {
        // 捕捉單一指令錯誤，避免讓整個 Server 崩潰
        System.err.println("Error processing cmd for player: " + request.getPlayer().getId());
        e.printStackTrace();
      }

      // 記錄統計數據
      GameMetrics.incrementCommand();
      processedCount++;
    }
  }

  private void processRooms(long currentTick, long now) {
    // 優化：只對「活躍」的房間發送
    worldManager.getActiveRooms().values().forEach(room -> {
      // Active Room 定義：有玩家在裡面，重生時間，或者有未結束的戰鬥/腳本
      boolean isRespawnTick = (currentTick % room.getZoneTemplate().respawnTime() == 0);
      if (!room.getPlayers().isEmpty() || isRespawnTick
          || room.getMobs().stream().anyMatch(m -> m.isInCombat())) {
        room.tick(currentTick, now);
      }
    });

  }
}
