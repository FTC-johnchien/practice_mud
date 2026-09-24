package com.example.htmlmud.domain.party.model;

import com.example.htmlmud.domain.model.entity.LivingStats;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 全域統一戰鬥與放招資源類型 (Unified Combat Resource Type)
 * 統一支援 MUD 技能消耗與 DRPG 小隊技能與合擊消耗。
 */
public enum CombatResourceType {
  HP("氣血", "點"),
  MP("真元", "點"),
  SP("戰氣", "點"),
  STAMINA("體力", "點"),
  RAGE("怒氣", "點"),
  COMBO("連擊點", "層"),
  ENERGY("精力", "點"),
  FORCE("內力", "點");

  private final String displayName;
  private final String unit;

  CombatResourceType(String displayName, String unit) {
    this.displayName = displayName;
    this.unit = unit;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getUnit() {
    return unit;
  }

  /**
   * 取得生靈當前該資源數值
   */
  public int getCurrent(LivingStats stats) {
    if (stats == null) return 0;
    return switch (this) {
      case HP -> stats.getHp();
      case MP -> stats.getMp();
      case SP, STAMINA -> stats.getStamina();
      case RAGE -> stats.getCombatResource("rage");
      case COMBO -> stats.getCombatResource("combo");
      case ENERGY -> stats.getCombatResource("energy");
      case FORCE -> stats.getCombatResource("force");
    };
  }

  /**
   * 扣除生靈該資源數值
   */
  public void deduct(LivingStats stats, int amount) {
    if (stats == null || amount <= 0) return;
    switch (this) {
      case HP -> stats.setHp(Math.max(0, stats.getHp() - amount));
      case MP -> stats.setMp(Math.max(0, stats.getMp() - amount));
      case SP, STAMINA -> stats.setStamina(Math.max(0, stats.getStamina() - amount));
      case RAGE -> stats.modifyCombatResource("rage", -amount);
      case COMBO -> stats.modifyCombatResource("combo", -amount);
      case ENERGY -> stats.modifyCombatResource("energy", -amount);
      case FORCE -> stats.modifyCombatResource("force", -amount);
    }
  }

  @JsonCreator
  public static CombatResourceType fromString(String value) {
    if (value == null || value.isBlank()) return MP;
    String upper = value.trim().toUpperCase();
    return switch (upper) {
      case "SP", "STAMINA", "SKILL" -> SP;
      case "HP", "HEALTH", "LIFE" -> HP;
      case "MANA", "MP" -> MP;
      case "RAGE" -> RAGE;
      case "COMBO" -> COMBO;
      case "ENERGY" -> ENERGY;
      case "FORCE" -> FORCE;
      default -> MP;
    };
  }
}
