package com.example.htmlmud.domain.model;

public enum LivingState2 {

  STANDING("站立"),

  SITTING("坐下"),

  RESTING("躺下休息"),

  SLEEPING("睡覺"),

  FIGHTING("戰鬥中"),

  STUNNED("暈眩"),

  DEAD("死亡");

  private final String description;

  LivingState2(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

}
