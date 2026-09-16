package com.example.htmlmud.domain.model.enums;

public enum WeaponType {

  AXE("斧"),

  BLADE("刀"),

  BOW("弓"),

  CHAIN("鎖鏈"),

  CLUB("短棍"),

  CROSSBOW("弩"),

  DAGGER("短刀"),

  DART("飛鏢"),

  DIRK("短劍"),

  FLAIL("連枷"),

  HALBERD("長戟"),

  HAMMER("錘"),

  JAVELIN("標槍"),

  KATANA("太刀"),

  KNIFE("小刀"),

  MACE("晨星錘"),

  MAUL("重槌"),

  POLEAXE("長柄斧"),

  POLEARM("長柄武器"),

  ROD("短魔杖"),

  ROPE("繩索"),

  SABER("彎刀"),

  SCEPTER("權杖"),

  SCIMITAR("波斯彎刀"),

  SHURIKEN("手裏劍"),

  SPEAR("長槍"),

  STAFF("棍"),

  STILETTO("細刃短劍"),

  STONE("飛石"),

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
