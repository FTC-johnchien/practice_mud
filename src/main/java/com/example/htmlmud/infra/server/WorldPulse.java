package com.example.htmlmud.infra.server;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.infra.monitor.GameMetrics;
import com.example.htmlmud.domain.service.CombatService;
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

  // 設定基礎頻率為 1000ms (1秒)
  // 這是 "戰鬥心跳" 的速度，也是最小單位
  @Scheduled(fixedRate = 100)
  public void pulse() {
    long currentTick = globalTickCounter.incrementAndGet();
    long startTime = System.currentTimeMillis();

    try {
      long now = startTime;

      // 優化：只對「活躍」的房間發送
      worldManager.getActiveRooms().values().forEach(room -> {
        // Active Room 定義：有玩家在裡面，重生時間，或者有未結束的戰鬥/腳本
        boolean isRespawnTick = (currentTick % room.getZoneTemplate().respawnTime() == 0);
        if (!room.getPlayers().isEmpty() || isRespawnTick
            || room.getMobs().stream().anyMatch(m -> m.isInCombat())) {
          room.tick(currentTick, now);
        }
      });

      combatService.tick(currentTick, now);

    } finally {
      long endTime = System.currentTimeMillis();
      long duration = endTime - startTime;

      // 紀錄到 Metrics
      gameMetrics.recordPulseDuration(duration);
    }

    // 每秒印一次 Log 確保心臟還在跳 (遊戲時間1小時)
    if (currentTick % 10 == 0) {
      log.info("World Pulse alive. Tick: {}, Last Pulse: {}ms, Total Commands: {}", currentTick,
          gameMetrics.getLastPulseDurationMs(), gameMetrics.getProcessedCommands());
      gameMetrics.resetCommands();
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
