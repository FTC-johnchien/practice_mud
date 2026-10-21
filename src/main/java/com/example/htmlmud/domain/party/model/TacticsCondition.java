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
  ALLY_HAS_DEBUFF("隊友身中異常", ""),
  ALLY_IS_CONTROLLED("隊友受控場限制", ""),
  SELF_LACKS_SHIELD("自身缺少護盾", ""),
  ENEMY_IS_CASTING("敵方正在施法蓄力", ""),
  ENEMY_CASTING_INTERRUPTIBLE("敵方處於可打斷施法中", ""),
  ENEMY_POISE_BROKEN("敵方處於破防失衡狀態", ""),
  ENEMY_HP_LESS_THAN("敵方氣血低於", "%"),
  ALLY_SAN_LESS_THAN("隊友道心低於", "%"),
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
