package com.example.htmlmud.domain.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.actor.impl.LivingActor;
import com.example.htmlmud.domain.actor.impl.MobActor;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.domain.actor.impl.RoomActor;
import com.example.htmlmud.domain.model.EquipmentSlot;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.ItemType;
import com.example.htmlmud.domain.model.LivingState;
import com.example.htmlmud.domain.model.vo.DamageSource;
import com.example.htmlmud.protocol.ActorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CombatService {

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

    // 1. 命中判定 (範例：敏捷越高，命中越高)
    // 假設基礎命中 80% + (攻方敏捷 - 守方敏捷)%
    double hitChance = 0.8 + ((attState.agi - defState.agi) * 0.01);
    if (ThreadLocalRandom.current().nextDouble() > hitChance) {
      RoomActor room = worldManagerProvider.getObject().getRoomActor(attacker.getCurrentRoomId());
      room.broadcast("calculateDamage " + attacker.getName() + " miss");
      // log.info("calculateDamage miss");
      return -1; // -1 代表 Miss
    }

    // 2. 傷害公式 (範例：攻擊力 - 防禦力，浮動 10%)
    int damage = random(attState.minDamage, attState.maxDamage);
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

    // 檢查 target 是否存在
    WorldManager manager = worldManagerProvider.getObject();
    CompletableFuture<LivingActor> future = new CompletableFuture<>();
    RoomActor room = manager.getRoomActor(self.getCurrentRoomId());
    room.findActor(self.getState().combatTargetId, future);
    LivingActor target = future.orTimeout(1, TimeUnit.SECONDS).join();
    if (target == null) {
      stopCombat(self); // 目標消失或死亡，停止戰鬥
      self.reply("戰鬥目標不在視野範圍內");
      return;
    }

    if (target.getState().isDead()) {
      stopCombat(self); // 目標消失或死亡，停止戰鬥
      self.reply("你要攻擊的對象 " + target.getName() + " 已經死了");
      return;
    }

    // 取出 attacker 的 DamageSource
    DamageSource weapon = self.getCurrentAttackSource();

    // 設定下一次攻擊時間 (攻速 2秒)
    self.getState().nextAttackTime = now + weapon.attackSpeed();

    int dmgAmout = calculateDamage(self, target);
    // log.info("dmgAmout:{}", dmgAmout);

    // dmgAmout = -1 代表 Miss

    // 閃躲 dodge

    // 招架 parry
    if (dmgAmout == -1) {
      String attackMsg =
          "用 " + weapon.name() + " " + weapon.verb() + target.getName() + "，被對方用武器招架！";
      self.reply("你" + attackMsg);
      room.broadcastToOthers(self.getId(), self.getName() + attackMsg);
      return;
    }



    // 將傷害送給 target
    target.onDamage(dmgAmout, self.getId());


    // 你 用 拳頭 攻擊 巨大的野鼠，造成 9 點傷害！
    String attackMsg =
        "用 " + weapon.name() + " " + weapon.verb() + target.getName() + "，造成 " + dmgAmout + " 點傷害！";

    // 發送訊息給房間 (讓其他人看到噴血)
    self.reply("你" + attackMsg);
    room.broadcastToOthers(self.getId(), self.getName() + attackMsg);



    // try {

    // // 4. 應用傷害
    // // target.onAttacked(attacker);

    // // 5. 發送訊息給房間所有人
    // // attacker.equip(null)
    // String verb = "攻擊"; // 或是從 weapon.getVerb() 取得 "揮砍"
    // String pattern = "%s 用 %s %s %s，造成 %d 點傷害！";
    // String msg = pattern;
    // msg = String.format(pattern, "你", "拳頭", verb, target.getName(), dmg);
    // attacker.reply(msg);
    // msg = String.format(pattern, attacker.getName(), "拳頭", verb, target.getName(), dmg);
    // room.broadcastToOthers(attacker.getId(), msg);

    // // return target;
    // } catch (Exception e) {
    // log.error("processAutoAttack", e);
    // }

    // stopCombat(attacker);
    // return null;
  }

  public void onDamage(int amount, LivingActor self, String attackerId) {

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
    String msg = self.getName() + " 目前 HP: " + self.getState().hp + "/" + self.getState().maxHp;
    RoomActor room = worldManagerProvider.getObject().getRoomActor(self.getCurrentRoomId());
    room.broadcast(msg);
    // for test----------------------------------------------------------------------------------

    // 檢查是否死亡，通知 self 死亡事件
    if (self.getState().isDead()) {
      log.info("{} 死亡ing", self.getName());
      self.die(attackerId);
    }
  }

  public void onDie(LivingActor self, String killerId) {
    RoomActor room = worldManagerProvider.getObject().getRoomActor(self.getCurrentRoomId());
    room.broadcast(killerId + " 殺死了 " + self.getName());

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
    if (room != null) {
      CompletableFuture<LivingActor> future = new CompletableFuture<>();
      room.findActor(killerId, future);
      LivingActor killer = future.orTimeout(1, java.util.concurrent.TimeUnit.SECONDS).join();
      if (killer == null) {
        log.error("killerId LivingActor not found: {}", killerId);
        return;
      }
      room.broadcastToOthers(killerId, self.getName() + " 被 " + killer.getName() + " 殺死了！");
    } else {
      log.error("currentRoomId RoomActor not found: {}", self.getCurrentRoomId());
    }
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
