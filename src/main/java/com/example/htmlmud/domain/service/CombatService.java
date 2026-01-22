package com.example.htmlmud.domain.service;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.actor.LivingActor;
import com.example.htmlmud.domain.actor.impl.RoomActor;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.domain.model.LivingState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CombatService {

  private final ObjectProvider<WorldManager> servicesProvider;

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
    double hitChance = 0.8 + ((attState.agi - defState.agi) * 0.01);
    if (ThreadLocalRandom.current().nextDouble() > hitChance) {
      return -1; // -1 代表 Miss
    }

    // 2. 傷害公式 (範例：攻擊力 - 防禦力，浮動 10%)
    log.info("attState.damage:{} defState.defense:{}", attState.damage, defState.defense);
    int rawDmg = attState.damage - defState.defense;
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

  public LivingActor processAutoAttack(LivingActor attacker, String roomId, String targetId) {
    WorldManager manager = servicesProvider.getObject();

    // 1. 檢查攻速冷卻
    // 2. 檢查目標是否還在房間裡 / 是否還活著
    // 這裡需要透過 WorldManager 取得真實的 Actor 物件
    RoomActor room = manager.getRoomActor(roomId);
    LivingActor target = room.findActor(targetId);
    log.info("attacker.getDisplayName:{} target DisplayName:{}", attacker.getDisplayName(),
        target.getDisplayName());
    if (target == null || target.getState().hp <= 0) {
      // stopCombat(); // 目標消失或死亡，停止戰鬥
      return target;
    }

    // 3. 執行攻擊 (呼叫 CombatService)
    // 這裡會用到我們上一輪討論的 DamageSource
    int dmg = calculateDamage(attacker, target);

    // 4. 應用傷害
    target.onAttacked(attacker, dmg);

    // 5. 發送訊息給房間所有人
    // attacker.equip(null)
    String verb = "攻擊"; // 或是從 weapon.getVerb() 取得 "揮砍"
    String msg = String.format("%s 用 %s %s %s，造成 %d 點傷害！", attacker.getDisplayName(), "拳頭", verb,
        target.getDisplayName(), dmg);
    room.broadcast(msg);

    return target;
  }
}
