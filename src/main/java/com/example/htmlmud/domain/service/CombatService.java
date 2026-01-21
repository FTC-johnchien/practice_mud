package com.example.htmlmud.domain.service;

import com.example.htmlmud.domain.actor.LivingActor;
import com.example.htmlmud.domain.model.LivingState;
import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CombatService {

  /**
   * 執行一次攻擊判定
   * 
   * @return 造成的傷害值 (0 代表未命中或被格擋)
   */
  public int calculateDamage(LivingActor attacker, LivingActor defender) {
    LivingState attState = attacker.getState();
    LivingState defState = defender.getState();

    // 1. 命中判定 (範例：敏捷越高，命中越高)
    // 假設基礎命中 80% + (攻方敏捷 - 守方敏捷)%
    double hitChance = 0.8 + ((attState.dex - defState.dex) * 0.01);
    if (ThreadLocalRandom.current().nextDouble() > hitChance) {
      return -1; // -1 代表 Miss
    }

    // 2. 傷害公式 (範例：攻擊力 - 防禦力，浮動 10%)
    int rawDmg = attState.attackPower - defState.defense;
    if (rawDmg <= 0)
      rawDmg = 1; // 至少造成 1 點傷害

    // 加入浮動 (0.9 ~ 1.1)
    double variance = 0.9 + (ThreadLocalRandom.current().nextDouble() * 0.2);
    int finalDmg = (int) (rawDmg * variance);

    return finalDmg;
  }

  /**
   * 計算獲得經驗值 (範例)
   */
  public int calculateExp(LivingActor mob, LivingActor player) {
    return mob.getState().level * 10;
  }
}
