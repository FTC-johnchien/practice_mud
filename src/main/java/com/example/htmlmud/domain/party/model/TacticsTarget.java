package com.example.htmlmud.domain.party.model;

/**
 * 戰術方針作用目標 (Tactics Target / Gambit Target)
 */
public enum TacticsTarget {
  LOWEST_HP_ALLY("氣血最低隊友"),
  SELF("自身"),
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
