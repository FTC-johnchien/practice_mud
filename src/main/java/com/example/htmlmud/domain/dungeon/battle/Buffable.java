package com.example.htmlmud.domain.dungeon.battle;

import java.util.List;

/**
 * 具備 Buff / Debuff / 護盾承受能力的戰鬥實體共用介面。
 * 讓 PartyMember 與 BattleEnemy 能統一接受狀態效果運算。
 */
public interface Buffable {

  String getId();

  String getName();

  boolean isAlive();

  int getHp();

  int getMaxHp();

  void setHp(int hp);

  List<ActiveBuff> getActiveBuffs();

  boolean hasActiveBuff(String buffId);

  ActiveBuff getActiveBuff(String buffId);

  void addBuff(ActiveBuff buff);

  void removeBuff(String buffId);

  /**
   * 受到傷害時，先依護盾隊列（Shortest Duration First）進行吸收
   * @param incomingDmg 原始傷害
   * @return 經護盾抵扣後的剩餘穿透傷害
   */
  int absorbShieldDamage(int incomingDmg);

  /**
   * 取得身上所有護盾吸收值的總和
   */
  int getTotalShield();

  /**
   * 承受直接或穿透傷害
   */
  void takeDamage(int dmg);
}
