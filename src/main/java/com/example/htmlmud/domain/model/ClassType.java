package com.example.htmlmud.domain.model;

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
}
