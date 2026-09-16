package com.example.htmlmud.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 角色裝備部位定義 (7 大黃金部位體系)
 * 兼顧小隊旅團 DRPG 與 MUD 單人探索，消除過度設計之冗餘部位。
 */
public enum EquipmentSlot {

  /** 主手武器 (刀、劍、槍、杖等) */
  MAIN_HAND("主手"),

  /** 副手裝備 (盾牌、法器、副手刃等) */
  OFF_HAND("副手"),

  /** 頭部防具 (道冠、頭盔、兜帽等) */
  HEAD("頭部"),

  /** 身軀防具 (道袍、鎧甲、重甲等，合併舊 CHEST/BODY) */
  BODY("身體"),

  /** 靴履 (踏雲履、鐵靴、草鞋等，合併舊 LEGS/FEET) */
  FEET("腳部"),

  /** 本命法寶 / 飾品 1 (戒指、項鍊、護符、玉佩) */
  ACCESSORY_1("飾品一"),

  /** 靈寶 / 飾品 2 (法印、定魂珠、乾坤圈) */
  ACCESSORY_2("飾品二");

  private final String displayName;

  EquipmentSlot(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public boolean isAccessory() {
    return this == ACCESSORY_1 || this == ACCESSORY_2;
  }

  /**
   * 具備容錯與向下相容的反序列化解析器
   */
  @JsonCreator
  public static EquipmentSlot fromString(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    String upper = value.trim().toUpperCase();
    return switch (upper) {
      case "MAIN_HAND", "WEAPON" -> MAIN_HAND;
      case "OFF_HAND", "SHIELD" -> OFF_HAND;
      case "HEAD", "FACE" -> HEAD;
      case "BODY", "CHEST", "BACK", "SHOULDERS", "ARMS", "WRISTS", "HANDS", "WAIST", "WINGS", "TAIL", "CLAWS" -> BODY;
      case "FEET", "LEGS" -> FEET;
      case "ACCESSORY", "ACCESSORY_1", "FINGER", "RING", "NECK", "TRINKET" -> ACCESSORY_1;
      case "ACCESSORY_2" -> ACCESSORY_2;
      default -> {
        try {
          yield EquipmentSlot.valueOf(upper);
        } catch (IllegalArgumentException e) {
          yield BODY;
        }
      }
    };
  }
}

