package com.example.htmlmud.domain.combat;

import com.example.htmlmud.domain.dungeon.battle.Buffable;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.MobRank;
import com.example.htmlmud.domain.party.model.RowPosition;

/**
 * 大一統戰鬥參與者抽象 (UNIFY-03 Universal Combatant)
 * 統一抽象 MUD Living (Player / Mob) 與 DRPG PartyMember / BattleEnemy。
 */
public interface Combatant extends Buffable {

  int getEffectiveMinDamage();

  int getEffectiveMaxDamage();

  int getEffectiveDefense();

  int getEffectiveDex();

  int getEffectiveStr();

  int getEffectiveCon();

  default MobRank getRank() {
    return MobRank.NORMAL;
  }

  default RowPosition getRow() {
    return RowPosition.FRONT;
  }

  default LivingStats getCombatStats() {
    return null;
  }

  @Override
  void takeDamage(int damage);
}
