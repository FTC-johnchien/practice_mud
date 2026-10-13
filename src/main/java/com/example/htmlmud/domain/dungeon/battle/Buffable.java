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

  /**
   * 接受生命值回復 (不可超過最大生命值上限)
   */
  default void heal(int amount) {
    if (amount <= 0 || !isAlive()) return;
    setHp(Math.min(getMaxHp(), getHp() + amount));
  }

  // === 罡氣破防條 (Poise / Break Gauge) 契約 ===
  default int getPoise() { return 100; }
  default int getMaxPoise() { return 100; }
  default boolean isPoiseBroken() { return false; }
  default void applyPoiseDamage(int amount) {}
  default void recoverPoise() {}

  /**
   * 心跳遞減破防力竭計數，若力竭結束回傳復甦日誌
   */
  default String processPoiseTick() { return null; }

  /**
   * 累加身上所有未過期 Buff 的特定屬性修正值 (加計層數)
   */
  default double getStatModifierSum(String statKey) {
    if (getActiveBuffs() == null || getActiveBuffs().isEmpty() || statKey == null) return 0.0;
    return getActiveBuffs().stream()
        .filter(b -> !b.isExpired() && b.getStatModifiers() != null)
        .mapToDouble(b -> b.getStatModifiers().getOrDefault(statKey, 0.0) * Math.max(1, b.getStacks()))
        .sum();
  }

  /**
   * 是否處於硬控場狀態 (暈眩、冰凍等無法行動)
   */
  default boolean isControlled() {
    if (getActiveBuffs() == null || getActiveBuffs().isEmpty()) return false;
    return getActiveBuffs().stream()
        .anyMatch(b -> !b.isExpired() && (
            b.getCategory() == com.example.htmlmud.domain.model.enums.BuffCategory.CONTROL ||
            b.getEffectType() == com.example.htmlmud.domain.model.enums.EffectType.STUNED ||
            b.getEffectType() == com.example.htmlmud.domain.model.enums.EffectType.FROZEN
        ));
  }

  /**
   * 是否處於封咒禁言狀態 (無法引導念咒)
   */
  default boolean isSilenced() {
    if (getActiveBuffs() == null || getActiveBuffs().isEmpty()) return false;
    return getActiveBuffs().stream()
        .anyMatch(b -> !b.isExpired() && b.getEffectType() == com.example.htmlmud.domain.model.enums.EffectType.SILENCE);
  }
}
