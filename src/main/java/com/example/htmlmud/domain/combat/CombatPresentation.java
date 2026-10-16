package com.example.htmlmud.domain.combat;

import java.util.List;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.dungeon.battle.DefenseResolver.DefenseOutcome;
import com.example.htmlmud.domain.dungeon.battle.DefenseResolver.DefenseResolution;
import com.example.htmlmud.domain.dungeon.battle.event.BattleEvent;
import com.example.htmlmud.domain.dungeon.battle.event.BattleEventType;
import com.example.htmlmud.domain.dungeon.battle.event.BattleHit;
import com.example.htmlmud.domain.dungeon.battle.event.FxShape;
import com.example.htmlmud.domain.dungeon.battle.event.HitOutcome;
import com.example.htmlmud.domain.dungeon.battle.event.UnitRef;
import com.example.htmlmud.domain.model.config.MoveAction;
import com.example.htmlmud.domain.service.MessageUtil;
import com.example.htmlmud.protocol.util.ColorText;

/**
 * 戰鬥雙模呈現投影服務 (UNIFY-03 Dual Presentation Projector)
 * - MUD 終端視窗：格式化武俠文字長篇招式文學
 * - Web/GUI 介面：發布結構化 BattleEventsDto 驅動卡片與粒子動畫
 */
public final class CombatPresentation {

  private CombatPresentation() {}

  /**
   * 格式化並發送 MUD 長篇文字敘事戰鬥訊息
   */
  public static void projectMudCombatLog(
      Living self,
      Living target,
      MoveAction action,
      DefenseResolution resolution,
      int finalDamage,
      String sWeapon,
      String tWeapon,
      String bodyPart) {

    if (self == null || target == null || action == null || resolution == null) return;
    if (self.getCurrentRoom() == null) return;

    String msg = action.msg().cast();

    if (resolution.outcome() == DefenseOutcome.MISS) {
      msg += "\r\n" + action.msg().miss();
    } else if (resolution.outcome() == DefenseOutcome.DODGED) {
      msg += "\r\n" + resolution.combatLog();
    } else if (resolution.outcome() == DefenseOutcome.CRIT) {
      msg += "\r\n" + resolution.combatLog();
    } else if (resolution.outcome() == DefenseOutcome.PARRIED || resolution.outcome() == DefenseOutcome.BLOCKED) {
      msg += "\r\n" + resolution.combatLog();
    } else {
      msg += "\r\n" + action.msg().hit();
      if (resolution.poiseBroken()) {
        msg += "\r\n\u001B[1;35m⚠️【架勢破防】" + target.getName() + " 精力枯竭架勢崩潰，破綻大開！受到額外 20% 傷害！\u001B[0m";
      }
    }

    if (resolution.riposteTriggered() && resolution.riposteDamage() > 0 && !msg.contains("破招反擊")) {
      msg += "\r\n\u001B[1;36m⚔️【破招反擊】" + target.getName() + " 借力打力，反手一記突刺，對 " + self.getName() + " 造成了 " + resolution.riposteDamage() + " 點反擊傷害！\u001B[0m";
    }

    msg = msg.replace("$W", sWeapon != null ? sWeapon : "兵刃")
             .replace("$w", tWeapon != null ? tWeapon : "兵刃")
             .replace("$l", bodyPart != null ? bodyPart : "要害")
             .replace("$d", ColorText.damage(finalDamage));

    Player protagonist = self.getCurrentRoom().getProtagonist();
    if (protagonist != null) {
      MessageUtil.send(msg, self, target, protagonist);
    }
  }

  /**
   * 投影生成 GUI Web 結構化戰鬥事件
   */
  public static BattleEvent projectGuiCombatEvent(
      long seq,
      Living self,
      Living target,
      String moveName,
      DefenseResolution resolution,
      int finalDamage,
      String weaponType,
      String damageType) {

    HitOutcome hitOutcome = switch (resolution.outcome()) {
      case MISS -> HitOutcome.MISS;
      case DODGED -> HitOutcome.DODGED;
      case PARRIED -> HitOutcome.PARRIED;
      case BLOCKED -> HitOutcome.BLOCKED;
      case CRIT -> HitOutcome.CRIT;
      default -> HitOutcome.HIT;
    };

    boolean isKilled = target.getStats() != null && target.getStats().getHp() <= 0;

    return new BattleEvent(
        seq,
        System.currentTimeMillis(),
        BattleEventType.ATTACK,
        new UnitRef(self instanceof Player ? UnitRef.Side.PARTY : UnitRef.Side.ENEMY, self.getId(), 0),
        null,
        moveName != null ? moveName : "攻擊",
        weaponType != null ? weaponType : "UNARMED",
        damageType != null ? damageType : "PHYSICAL",
        FxShape.SINGLE,
        null,
        List.of(new BattleHit(
            new UnitRef(target instanceof Player ? UnitRef.Side.PARTY : UnitRef.Side.ENEMY, target.getId(), 0),
            hitOutcome,
            finalDamage,
            0,
            isKilled
        ))
    );
  }
}
