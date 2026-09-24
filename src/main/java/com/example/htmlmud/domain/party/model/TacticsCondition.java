package com.example.htmlmud.domain.party.model;

/**
 * 戰術方針觸發條件 (Tactics Condition / Gambit Condition)
 */
public enum TacticsCondition {
  ALLY_HP_LESS_THAN("隊友氣血低於", "%"),
  SELF_HP_LESS_THAN("自身氣血低於", "%"),
  ENEMY_COUNT_GTE("敵方存活數量 >=", "體"),
  ENEMY_IS_BOSS("敵方存在首領", ""),
  RESOURCE_GTE("自身資源 >=", "點"),
  TARGET_LACKS_BUFF("目標缺少狀態", ""),
  TARGET_HAS_BUFF("目標擁有狀態", ""),
  BUFF_TIME_LESS_THAN("狀態剩餘時間低於", "秒"),
  ALWAYS("無條件施展", "");

  private final String label;
  private final String unit;

  TacticsCondition(String label, String unit) {
    this.label = label;
    this.unit = unit;
  }

  public String getLabel() {
    return label;
  }

  public String getUnit() {
    return unit;
  }
}
