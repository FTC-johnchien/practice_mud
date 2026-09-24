package com.example.htmlmud.domain.party.model;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @deprecated 專案已全面統一收斂為 {@link CombatResourceType}。
 * 此處保留以維持平滑相容性。
 */
@Deprecated
public enum ResourceType {
  MP("真元", "點"),
  SP("戰氣", "點"),
  HP("氣血", "點"),
  STAMINA("體力", "點"),
  RAGE("怒氣", "點"),
  COMBO("連擊點", "層"),
  ENERGY("精力", "點"),
  FORCE("內力", "點");

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

  public CombatResourceType toCombatResourceType() {
    return CombatResourceType.valueOf(this.name());
  }

  public static ResourceType fromCombatResourceType(CombatResourceType crt) {
    if (crt == null) return null;
    return ResourceType.valueOf(crt.name());
  }

  @JsonCreator
  public static ResourceType fromString(String value) {
    if (value == null || value.isBlank()) return MP;
    CombatResourceType crt = CombatResourceType.fromString(value);
    return ResourceType.valueOf(crt.name());
  }
}
