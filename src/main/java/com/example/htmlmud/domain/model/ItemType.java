package com.example.htmlmud.domain.model;

/**
 * 物品類型定義
 */
public enum ItemType {
  /** 裝備類 */
  EQUIPMENT("裝備"),
  /** 消耗品 (藥水、食物等) */
  CONSUMABLE("消耗品"),
  /** 關鍵物品 (任務道具、鑰匙) */
  KEY_ITEM("關鍵物品"),
  /** 材料 (鍛造、煉金原料) */
  MATERIAL("材料"),
  /** 其他 */
  OTHER("其他");

  private final String description;

  ItemType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
