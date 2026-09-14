package com.example.htmlmud.domain.party.model;

/**
 * 小隊成員戰鬥放招資源類型
 */
public enum ResourceType {
  MP("真元", "點"),
  RAGE("怒氣", "點"),
  COMBO("連擊點", "層");

  private final String displayName;
  private final String unit;

  ResourceType(String displayName, String unit) {
    this.displayName = displayName;
    this.unit = unit;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getUnit() {
    return unit;
  }
}
