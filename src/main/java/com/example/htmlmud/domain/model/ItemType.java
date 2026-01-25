package com.example.htmlmud.domain.model;

/**
 * 物品類型定義
 */
public enum ItemType {
  /** 武器 */
  WEAPON("武器"),
  /** 盾牌 */
  SHIELD("盾牌"),
  /** 防具 */
  ARMOR("防具"),
  /** 飾品 */
  ACCESSORY("飾品"),
  /** 消耗品 (藥水、食物等) */
  CONSUMABLE("消耗品"),
  /** 關鍵物品 (任務道具、鑰匙) */
  KEY_ITEM("關鍵物品"),
  /** 材料 (鍛造、煉金原料) */
  MATERIAL("材料"),
  /** 容器 */
  CONTAINER("容器"),
  /** 屍體 */
  CORPSE("屍體"),
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
