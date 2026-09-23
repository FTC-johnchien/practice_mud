package com.example.htmlmud.domain.party.model;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 小隊成員戰鬥放招資源類型 (Combat Resource Type)
 * 區隔於生靈屬性 Enum (domain.model.enums.ResourceType)
 */
public enum CombatResourceType {
  MP("真元", "點"),
  SP("戰氣", "點"),
  HP("氣血", "點"),
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
