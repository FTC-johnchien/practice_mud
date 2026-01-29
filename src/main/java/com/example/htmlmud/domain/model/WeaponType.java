package com.example.htmlmud.domain.model;

public enum WeaponType {

  AXE("斧"),

  BLADE("刀"),

  BOW("弓"),

  DAGGER("短刀"),

  HAMMER("锤"),

  POLEARM("長柄武器"),

  STAFF("棍"),

  SPEAR("長槍"),

  SWORD("劍"),

  UNARMED("拳腳"),

  WAND("法杖"),

  WHIP("鞭");


  private final String description;

  WeaponType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
