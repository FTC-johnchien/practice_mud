package com.example.htmlmud.domain.service;

import java.util.Map;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.model.LivingState;
import com.example.htmlmud.domain.model.ResourceType;
import com.example.htmlmud.domain.model.map.SkillTemplate;
import com.example.htmlmud.infra.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillCostService {

  private final MessageUtil messageUtil;

  /**
   * 檢查資源是否足夠
   */
  public boolean checkResources(Living actor, SkillTemplate skill) {
    if (skill.getLearning().costs() == null)
      return true;

    for (Map.Entry<ResourceType, Integer> entry : skill.getLearning().costs().entrySet()) {
      ResourceType type = entry.getKey();
      int cost = entry.getValue();

      // 0 消耗直接跳過
      if (cost <= 0)
        continue;

      // 利用 Enum 的多型直接取得當前數值
      if (type.getCurrent(actor.getState()) < cost) {

        messageUtil.send("$N的 " + type.name() + " 不足！(需要: " + cost + ")", actor);

        return false;
      }
    }
    return true;
  }

  /**
   * 執行扣除
   */
  public void deductResources(LivingState actor, SkillTemplate skill) {
    if (skill.getLearning().costs() == null)
      return;

    for (Map.Entry<ResourceType, Integer> entry : skill.getLearning().costs().entrySet()) {
      ResourceType type = entry.getKey();
      int cost = entry.getValue();

      if (cost > 0) {
        // 利用 Enum 的多型執行扣除
        type.deduct(actor, cost);
      }
    }
  }
}
