package com.example.htmlmud.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.command.parser.BodyPartSelector;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.application.service.SkillManager;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.actor.impl.LivingActor;
import com.example.htmlmud.domain.actor.impl.RoomActor;
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.EquipmentSlot;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.LivingState;
import com.example.htmlmud.domain.model.SkillType;
import com.example.htmlmud.domain.model.map.CombatAction;
import com.example.htmlmud.domain.model.vo.DamageSource;
import com.example.htmlmud.infra.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CombatService {

  private final MessageUtil messageUtil;
  private final SkillManager skillManager;
  private final ObjectProvider<WorldManager> worldManagerProvider;
  private final WorldFactory worldFactory;

  /**
   * 執行一次攻擊判定
   *
   * @return 造成的傷害值 (0 代表未命中或被格擋)
   */
  public int calculateDamage(LivingActor attacker, LivingActor defender) {
    LivingState attState = attacker.getState();
    LivingState defState = defender.getState();

    // 1. 命中判定 (範例：靈巧越高，命中越高)
    // 假設基礎命中 80% + (攻方靈巧 - 守方靈巧)%
    double hitChance = 0.8 + ((attState.dex - defState.dex) * 0.01);
    if (ThreadLocalRandom.current().nextDouble() > hitChance) {
      RoomActor room = worldManagerProvider.getObject().getRoomActor(attacker.getCurrentRoomId());
      room.broadcast("log:calculateDamage " + attacker.getName() + " miss");
      // log.info("calculateDamage miss");
      return -1; // -1 代表 Miss
    }

    DamageSource weapon = attacker.getCurrentAttackSource();

    // 2. 傷害公式 (範例：攻擊力 - 防禦力，浮動 10%)
    int damage = random(weapon.minDamage(), weapon.maxDamage());
    // log.info("attState.damage:{} defState.defense:{}", damage, defState.defense);
    int rawDmg = damage - defState.defense;
    if (rawDmg <= 0)
      rawDmg = 1; // 至少造成 1 點傷害

    // 加入浮動 (0.9 ~ 1.1)
    double variance = 0.9 + (ThreadLocalRandom.current().nextDouble() * 0.2);
    int finalDmg = (int) (rawDmg * variance);
    if (finalDmg <= 0) {
      finalDmg = 0; // 至少造成 1 點傷害
    }
    // log.info("finalDmg:{}", finalDmg);
    return finalDmg;
  }

  /**
   * 計算獲得經驗值 (範例)
   */
  public int calculateExp(LivingActor mob, LivingActor player) {
    return mob.getState().level * 10;
  }

  public void onAttacked(LivingActor self, String attackerId) {
    self.getState().isInCombat = true;
    if (self.getState().combatTargetId == null) {
      self.getState().combatTargetId = attackerId;
    }
  }

  public void processAutoAttack(LivingActor self, long now) {

    // 檢查是否正在戰鬥
    if (!self.getState().isInCombat) {
      stopCombat(self);
      return;
    }

    // 檢查攻速冷卻 (例如 2000ms 一刀)
    if (now < self.getState().nextAttackTime) {
      // log.info(self.getName() + " 還在冷卻中，無法攻擊");
      return; // 還在冷卻中
    }

    // 檢查是否戰鬥target是否存在 (需自行實作)
    if (self.getState().combatTargetId == null) {
      stopCombat(self);
      return;
    }

    // 檢查與self同一個房間里的 target 是否存在!!! TODO 遠程攻擊的要另外寫
    WorldManager manager = worldManagerProvider.getObject();
    CompletableFuture<LivingActor> future = new CompletableFuture<>();
    RoomActor room = manager.getRoomActor(self.getCurrentRoomId());
    room.findActor(self.getState().combatTargetId, future);
    LivingActor target = null;
    try {
      target = future.orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("尋找戰鬥目標時發生錯誤: {}", e.getMessage());
      stopCombat(self); // 目標消失，停止戰鬥
      throw new MudException("尋找戰鬥目標時發生錯誤");
    }

    if (target.getState().isDead()) {
      stopCombat(self); // 目標消失或死亡，停止戰鬥
      // self.reply("你的戰鬥目標 " + target.getName() + " 已經死了");
      return;
    }



    // 1. 決定攻擊類型 (空手 or 拿武器?)
    SkillType attackType =
        self.getState().equipment.get(EquipmentSlot.MAIN_HAND) == null ? SkillType.UNARMED
            : SkillType.WEAPON;

    // 2. 從 SkillManager 取得當前的一招 (包含描述與倍率)
    CombatAction action = skillManager.getCombatAction(attackType);

    // 3. 計算基礎傷害 取出 attacker 的 DamageSource
    DamageSource weapon = self.getCurrentAttackSource();

    // 設定下一次攻擊時間 (攻速 2秒)
    self.getState().nextAttackTime = now + weapon.attackSpeed();


    int rawDmg = calculateDamage(self, target);
    // log.info("dmgAmout:{}", dmgAmout);

    // 4. 套用招式倍率
    int dmgAmout = (int) (rawDmg * action.damageMod());

    // 5. 格式化戰鬥訊息
    // verb: "雙掌一分，使出一招「野馬分鬃」，推向 $n 的 $l"
    String msg = action.verb().replace("$n", target.getName()).replace("$l",
        BodyPartSelector.getRandomBodyPart());

    // 6. 加上結果描述 (根據傷害高低)
    // String resultDesc = getDamageResultDescription(dmgAmout);
    // ex: "造成了皮肉傷" or "造成了 嚴重 的傷害！"

    // 7. 廣播
    // room.broadcast(self.getName() + " " + msg + "，" + resultDesc);

    // 8. 扣血
    // target.receiveDamage(dmgAmout);



    // dmgAmout = -1 代表 Miss ?



    // TODO 閃躲 dodge



    // template: $N舉起wepon，用盡全力揮向$n！
    String combatTemplate = "$N舉起" + weapon.name() + "，用盡全力" + weapon.verb() + "$n！\r\n";

    // 訊息處理
    List<LivingActor> audiences = new ArrayList<>();
    audiences.addAll(room.getPlayers());

    // 招架 parry
    if (dmgAmout <= 0) {
      combatTemplate += "卻被$n用武器招架！";
      for (LivingActor receiver : audiences) {
        messageUtil.send(combatTemplate, self, target, receiver);
      }
      return;
    }


    // 將傷害送給 target
    target.onDamage(dmgAmout, self);

    combatTemplate += "造成 " + dmgAmout + " 點傷害！";
    for (LivingActor receiver : audiences) {
      messageUtil.send(combatTemplate, self, target, receiver);
    }
  }

  public void onDamage(int amount, LivingActor self, LivingActor attacker) {

    // 檢查是否還存在，死亡可能會消失
    if (self == null) {
      log.info("onDamage 對象已消失");
      return;
    }

    // 檢查是否還活著
    if (self.getState().isDead()) {
      log.info("{} 已經死亡，無法受傷", self.getName());
      return;
    }

    // 扣除 HP
    self.getState().hp -= amount;

    // for test----------------------------------------------------------------------------------
    RoomActor room = worldManagerProvider.getObject().getRoomActor(self.getCurrentRoomId());
    room.broadcast(
        "log:" + self.getName() + " 目前 HP: " + self.getState().hp + "/" + self.getState().maxHp);
    // for test----------------------------------------------------------------------------------

    // 檢查是否死亡，通知 self 死亡事件
    if (self.getState().isDead()) {
      log.info("{} 死亡ing", self.getName());
      self.die(attacker);
    }
  }

  public void onDie(LivingActor self, LivingActor killer) {
    RoomActor room = worldManagerProvider.getObject().getRoomActor(self.getCurrentRoomId());

    String messageTemplate = "$N殺死了$n";
    List<LivingActor> audiences = new ArrayList<>();
    audiences.addAll(room.getPlayers());
    for (LivingActor receiver : audiences) {
      messageUtil.send(messageTemplate, killer, self, receiver);
    }
    // room.broadcast(killerId + " 殺死了 " + self.getName());

    // 停止戰鬥狀態
    self.getState().hp = 0;
    self.getState().isInCombat = false;
    self.getState().combatTargetId = null;

    // 製造屍體
    GameItem corpse = worldFactory.createCorpse(self);

    if (self.getCurrentRoomId() == null) {
      log.error("onDie currentRoomId is null: {}", self.getCurrentRoomId());
      return;
    }

    // TODO 應交由房間處理 roomMessage livingDead 房間廣播死亡訊息
    // RoomActor room = worldManagerProvider.getObject().getRoomActor(self.getCurrentRoomId());
    // if (room != null) {
    // CompletableFuture<LivingActor> future = new CompletableFuture<>();
    // room.findActor(killerId, future);
    // LivingActor killer = future.orTimeout(1, java.util.concurrent.TimeUnit.SECONDS).join();
    // if (killer == null) {
    // log.error("killerId LivingActor not found: {}", killerId);
    // return;
    // }
    // room.broadcastToOthers(killerId, self.getName() + " 被 " + killer.getName() + " 殺死了！");
    // } else {
    // log.error("currentRoomId RoomActor not found: {}", self.getCurrentRoomId());
    // }
  }



  // 取出隨機數值
  private int random(int min, int max) {
    return ThreadLocalRandom.current().nextInt(min, max + 1);
  }

  private void stopCombat(LivingActor attacker) {
    attacker.getState().isInCombat = false;
    attacker.getState().combatTargetId = null;
  }
}
