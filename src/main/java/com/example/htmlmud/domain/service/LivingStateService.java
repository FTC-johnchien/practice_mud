package com.example.htmlmud.domain.service;

import com.example.htmlmud.domain.event.PlayerEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LivingStateService {

  private final ApplicationEventPublisher eventPublisher;

  public void processLevelUp(String playerId, int currentExp) {
    // 1. 執行核心邏輯 (計算等級、加屬性...)
    // int newLevel = calculateLevel(currentExp);

    // 2. 狀態改變完成後，發布事件 (這是一個 Fact)
    // 這行程式碼不關心誰要聽，它只負責說「這件事發生了」
    // eventPublisher.publishEvent(new PlayerEvents.LevelUp(playerId, newLevel, null));
  }
}
