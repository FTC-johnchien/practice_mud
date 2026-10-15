package com.example.htmlmud.application.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.event.MobEvents;
import com.example.htmlmud.domain.service.WorldManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 墨竹山·道祖是克蘇魯 劇情任務監聽器
 * 當擊倒畸變大師兄·宋天衡時，觸發「現代思維邏輯錨點格式化」與「秦劍師黑話玉牌」劇情事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MozhuMinesQuestListener {

  private final WorldManager worldManager;
  private final com.example.htmlmud.domain.repository.TemplateReader templateReader;

  @Async
  @EventListener
  public void onBossDefeated(MobEvents.MobDead event) {
    if (event.mobId() == null || templateReader == null) {
      return;
    }
    templateReader.findQuestByTriggerMob(event.mobId()).ifPresent(quest -> {
      if (quest.logMessage() != null && !quest.logMessage().isBlank()) {
        log.info("{} Killer: {}", quest.logMessage(), event.killerId());
      }
      if (event.killerId() != null && quest.storyLines() != null && !quest.storyLines().isEmpty()) {
        worldManager.findPlayerActor(event.killerId()).ifPresent(player -> {
          player.reply("");
          for (String line : quest.storyLines()) {
            player.reply(line);
          }
          player.reply("");
        });
      }
    });
  }
}
