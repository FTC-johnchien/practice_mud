package com.example.htmlmud.domain.party.model;

import java.util.List;
import com.example.htmlmud.domain.model.enums.SkillType;

/**
 * 大一統規範技能介面 (UnifiedSkill)
 * 統合 MUD 修煉境界 (熟練度/道法等級/招式敘事) 與 DRPG 回合戰術 (CD/消耗/傷害倍率/戰術標籤)
 */
public interface UnifiedSkill {

  // === 基礎識別 ===
  String getId();
  String getName();
  String getIcon();
  String getDescription();
  SkillType getSkillType();
  String getCategory();

  // === 修煉境界 (Mastery & Level) ===
  int getLevel();
  void setLevel(int level);
  long getXp();
  void setXp(long xp);
  long getMaxXp();
  void setMaxXp(long maxXp);
  void addXp(long amount);
  void levelUp();
  boolean isMaxLevel();
  double getProficiencyProgress();

  // === 戰術數值與動態等級縮放 ===
  CombatResourceType getCostType();
  int getCostValue();
  int getSpCost();
  int getMpCost();
  int getHpCost();
  long getCooldownMs();
  double getDamageMultiplier();
  double getEffectiveDamageMultiplier();
  long getEffectiveCooldownMs();
  int getEffectiveHealAmount();
  int getEffectiveSanRestore();

  // === 戰術機制標籤 ===
  boolean isAoe();
  boolean isHeal();
  boolean isTaunt();
  boolean isStun();
  int getStunDurationSeconds();
  boolean isShield();
  boolean isBuff();
  boolean isDefense();
  List<String> getTags();
  List<String> getAllowedWeapons();

  // === 施法蓄力與反制機制 (Combat React) ===
  default long getCastTimeMs() { return 0L; }
  default boolean isInterruptible() { return true; }

  // === 元素反應與引爆機制 (Milestone F2-05) ===
  default boolean isDetonator() {
    List<String> tags = getTags();
    return tags != null && (tags.contains("DETONATOR") || tags.contains("DETONATE"));
  }

  default boolean isPrimer() {
    List<String> tags = getTags();
    return tags != null && (tags.contains("PRIMER") || tags.contains("SOUL_REND"));
  }

  // === 敘事文本 (招式動態播報) ===
  List<String> getMoves();
}

