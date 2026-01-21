package com.example.htmlmud.infra.server;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.service.WorldManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorldPulse {

  private final WorldManager worldManager;

  // 每 1000 毫秒 (1秒) 執行一次
  @Scheduled(fixedRate = 1000)
  public void tick() {
    // 只針對「有人」或「活躍」的房間進行更新，效能最佳化
    worldManager.getActiveRooms().values().forEach(room -> {
      // 使用 Virtual Thread 並行處理每個房間的邏輯
      Thread.ofVirtual().name("room-tick-" + room.getId()).start(() -> {
        try {
          room.tick();
        } catch (Exception e) {
          log.error("Room tick error: {}", room.getId(), e);
        }
      });
    });
  }
}
