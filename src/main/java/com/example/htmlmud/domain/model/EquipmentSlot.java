package com.example.htmlmud.domain.model;

// 裝備部位 (這也是一種 Enum)
public enum EquipmentSlot {

  // 裝備部位
  HEAD("頭部"),

  FACE("臉部"),

  NECK("頸部"),

  SHOULDERS("肩部"),

  ARMS("手臂"),

  WRISTS("腕部"),

  HANDS("手部"),

  BODY("身體"),

  CHEST("胸部"),

  BACK("背部"),

  WAIST("腰部"),

  LEGS("腿部"),

  FEET("腳部"),

  FINGER_1("戒指1"), FINGER_2("戒指2"),


  // 武器部位
  MAIN_HAND("主手"),

  OFF_HAND("副手");

  private final String displayName;

  EquipmentSlot(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
