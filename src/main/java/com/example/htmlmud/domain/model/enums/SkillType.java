package com.example.htmlmud.domain.model.enums;

public enum SkillType {

  UNARMED("拳擊"),

  WEAPON("武器"),

  MAGIC("魔法"),

  ACTIVE("主動技能"),

  PASSIVE("被動技能"),

  REACTIVE("反應技能"),

  CHANNEL("導引技能"),

  COMBO("多人合擊"),

  FORMATION("陣法絕技");


  private final String description;

  SkillType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
