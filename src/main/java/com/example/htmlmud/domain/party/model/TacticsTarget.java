package com.example.htmlmud.domain.party.model;

/**
 * 戰術方針作用目標 (Tactics Target / Gambit Target)
 */
public enum TacticsTarget {
  LOWEST_HP_ALLY("氣血最低隊友"),
  FRONT_ROW_ALLY("前衛肉盾 (Tank)"),
  LEADER("小隊隊長"),
  SELF("自身"),
  MEMBER_1("隊員 #1"),
  MEMBER_2("隊員 #2"),
  MEMBER_3("隊員 #3"),
  MEMBER_4("隊員 #4"),
  MEMBER_5("隊員 #5"),
  BACK_ROW_ALLY("後衛隊友"),
  CURRENT_ENEMY("當前集火目標"),
  ALL_ENEMIES("全體敵怪"),
  ALL_ALLIES("全體隊友");

  private final String label;

  TacticsTarget(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
