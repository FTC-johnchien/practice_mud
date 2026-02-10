package com.example.htmlmud.infra.server;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.service.CombatService;
import com.example.htmlmud.domain.service.WorldManager;
import com.example.htmlmud.infra.monitor.GameMetrics;
import com.example.htmlmud.infra.util.AnsiColor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorldPulse {

  private final WorldManager worldManager;

  private final CombatService combatService;

  private final GameMetrics gameMetrics;


  // 全域計數器，伺服器啟動後開始累加
  private final AtomicLong globalTickCounter = new AtomicLong(0);

  // 設定基礎頻率為 100ms (0.1秒)
  // 這是 "戰鬥心跳" 的速度，也是最小單位
  @Scheduled(fixedRate = 100)
  public void pulse() {
    long currentTick = globalTickCounter.incrementAndGet();
    long startTime = System.nanoTime();
    long duration = 0;
    try {
      long now = System.currentTimeMillis();

      // 對處於戰鬥清單
      combatService.tick(currentTick, now);

      // 對 Active Players

      // 對 Dirty Mobs

      // 對 Active Rooms 定義：只對「活躍」的房間發送
      worldManager.getActiveRooms().values().forEach(room -> {
        // 先檢查最簡單的條件：有沒有玩家
        boolean hasPlayers = !room.getPlayers().isEmpty();
        boolean isRespawnTick = (currentTick % 10 % room.getZoneTemplate().respawnTime() == 0);

        if (hasPlayers || isRespawnTick) {
          room.tick(currentTick, now);
        }
      });

      // 對房間事件 Spawn rule

      // 對 zone / world 事件

    } finally {
      duration = System.nanoTime() - startTime;

      // 紀錄到 Metrics
      gameMetrics.addPulseDurationNanos(duration);
    }

    // 每次心跳為 100毫秒，若有處理時間超過 50毫秒的就印出來警告
    if (duration > 50_000_000) {
      log.warn(
          AnsiColor.YELLOW + "高負載警告 - Tick: {}, Pulse: {}ms, Player Cmds: {}, System Tasks: {}"
              + AnsiColor.RESET,
          currentTick, duration / 1_000_000.0, gameMetrics.getPerCommands(),
          gameMetrics.getPerTasks());
    }

    // 清除每次心跳的計數
    gameMetrics.resetPerMetrics();

    // 每分鐘印一次 Log 確保心臟還在跳
    if (currentTick % 600 == 0) {
      long totalNanos = gameMetrics.getTotalPulseDurationNanos();
      log.info(
          "World Pulse Stats - Tick: {}, Total Work: {}ms, Avg Pulse: {}ms, Player Cmds: {}, System Tasks: {}",
          currentTick, totalNanos / 1_000_000.0, (totalNanos / 10.0) / 1_000_000.0,
          gameMetrics.getPlayerCommands(), gameMetrics.getSystemTasks());
      gameMetrics.resetMetrics();
    }
  }

  // 每 1000 毫秒 (1秒) 執行一次
  // @Scheduled(fixedRate = 1000)
  // public void tick() {
  // // 只針對「有人」或「活躍」的房間進行更新，效能最佳化
  // worldManager.getActiveRooms().values().forEach(room -> {
  // // 使用 Virtual Thread 並行處理每個房間的邏輯
  // Thread.ofVirtual().name("room-tick-" + room.getId()).start(() -> {
  // try {
  // room.tick();
  // } catch (Exception e) {
  // log.error("Room tick error: {}", room.getId(), e);
  // }
  // });
  // });
  // }
}
