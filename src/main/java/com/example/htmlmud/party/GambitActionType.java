package com.example.htmlmud.party;

/**
 * 戰術方針動作類型 (Gambit Action Type)
 */
public enum GambitActionType {
  SKILL("施放技能"),
  ITEM("使用藥品"),
  DEFENSE("防禦招架"),
  ATTACK("普通攻擊");

  private final String label;

  GambitActionType(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
