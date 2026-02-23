package com.example.htmlmud.domain.service;

import java.util.Map;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.ResourceType;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillCostService {

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
      if (type.getCurrent(actor.getStats()) < cost) {

        if (actor instanceof Player player) {
          player.reply("$N的 " + type.name() + " 不足！(需要: " + cost + ")");
        }

        return false;
      }
    }
    return true;
  }

  /**
   * 執行扣除
   */
  public void deductResources(LivingStats actor, SkillTemplate skill) {
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
