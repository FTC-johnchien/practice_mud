package com.example.htmlmud.application.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.event.DomainEvent;
import com.example.htmlmud.domain.event.MobEvents;
import com.example.htmlmud.domain.event.PlayerEvents;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuditLogEventListener {
  @Async // <--- 關鍵：丟到另一個 Thread (或 Virtual Thread) 執行
  @EventListener
  public void logPlayerAction(DomainEvent event) {
    // 這裡使用了 switch pattern matching (Java 21+)
    // 可以優雅地處理所有 DomainEvent
    String logMsg = switch (event) {
      case PlayerEvents.LoggedIn e -> "使用者登入: " + e.username() + " IP:" + e.ipAddress();
      case PlayerEvents.LevelUp e -> "玩家升級: " + e.newLevel();
      case MobEvents.MobDead e -> "怪物死亡: " + e.mobId();
      default -> "未知事件: " + event.getClass().getSimpleName();
    };

    // 模擬寫入 DB
    log.info("[AUDIT LOG] " + event.occurredOn() + " - " + logMsg);
  }
}
