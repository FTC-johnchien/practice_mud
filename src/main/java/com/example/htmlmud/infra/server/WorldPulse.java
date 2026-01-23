package com.example.htmlmud.infra.server;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.protocol.RoomMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorldPulse {

  private final WorldManager worldManager;

  // 全域計數器，伺服器啟動後開始累加
  private final AtomicLong globalTickCounter = new AtomicLong(0);

  // 設定基礎頻率為 1000ms (1秒)
  // 這是 "戰鬥心跳" 的速度，也是最小單位
  @Scheduled(fixedRate = 1000)
  public void pulse() {
    long currentTick = globalTickCounter.incrementAndGet();
    long now = System.currentTimeMillis();

    // 優化：只對「活躍」的房間發送
    worldManager.getActiveRooms().values().forEach(room -> {

      // Active Room 定義：有玩家在裡面，重生時間，或者有未結束的戰鬥/腳本
      boolean isRespawnTick = (currentTick % room.getZoneTemplate().respawnRate() == 0);
      if (!room.getPlayers().isEmpty() || isRespawnTick
          || room.getMobs().stream().anyMatch(m -> m.getState().isInCombat())) {
        room.send(new RoomMessage.Tick(currentTick, now));
      }
    });

    // 可選：每 60 秒印一次 Log 確保心臟還在跳
    if (currentTick % 60 == 0) {
      log.debug("World Pulse alive. Tick: {}", currentTick);

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
