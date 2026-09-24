package com.example.htmlmud.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 道具使用即時效果類型 (Item Consumable Action Type)
 * 區隔於生靈身上持續性的 Buff / Debuff 狀態體系。
 */
public enum ItemActionType {
  HEAL_HP("恢復氣血"),
  RESTORE_MP("恢復真元"),
  RESTORE_SAN("恢復理智"),
  LEARN_SKILL("領悟技能"),
  CURE_POISON("解毒"),
  TELEPORT("回城傳送");

  private final String description;

  ItemActionType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  @JsonCreator
  public static ItemActionType fromString(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return ItemActionType.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
