package com.example.htmlmud.domain.model;

public enum SkillType {

  UNARMED("拳擊"),

  WEAPON("武器"),

  MAGIC("魔法"),

  ACTIVE("主动技能"),

  PASSIVE("被動技能");


  private final String description;

  SkillType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
