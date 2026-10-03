package com.example.htmlmud.domain.model.enums;

public enum ClassType {

  NONE("無門無派"),

  WARRIOR("戰士"),

  MAGE("魔法師"),

  ROGUE("盜賊"),

  CLERIC("牧師"),

  SWORDSMAN("劍士"),

  MONK("武僧");

  private final String description;

  ClassType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public String getId() {
    return this.name();
  }

  public static ClassType fromId(String id) {
    if (id == null || id.isBlank()) {
      return NONE;
    }
    try {
      return ClassType.valueOf(id.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return NONE;
    }
  }
}
