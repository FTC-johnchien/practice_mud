package com.example.htmlmud.service.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.event.PlayerEvents;

@Component
public class QuestEventListener {
  @EventListener
  public void checkLevelUpQuest(PlayerEvents.LevelUp event) {
    // 檢查該玩家是否有 "升到 10 級" 的任務
    // if (event.newLevel() >= 10) ...
    System.out.println("任務檢查: 玩家 " + event.playerId() + " 升級了。");
  }
}
