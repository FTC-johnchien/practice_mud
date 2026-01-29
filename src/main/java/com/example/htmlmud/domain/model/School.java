package com.example.htmlmud.domain.model;

public enum School {

  COMMON("無門無派，人人可學"),

  WARRIOR("戰士"),

  MAGE("魔法師"),

  ROGUE("盜賊"),

  CLERIC("牧師"),

  SWORDSMAN("劍士"),

  ONK("武僧");

  private final String description;

  School(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
