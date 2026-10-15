package com.example.htmlmud.application.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.event.PlayerEvents;

@Slf4j
@Component
public class QuestEventListener {
  @EventListener
  public void checkLevelUpQuest(PlayerEvents.LevelUp event) {
    // 檢查該玩家是否有 "升到 10 級" 的任務
    // if (event.newLevel() >= 10) ...
    log.debug("任務檢查: 玩家 {} 升級至 {}", event.playerId(), event.newLevel());
  }
}
