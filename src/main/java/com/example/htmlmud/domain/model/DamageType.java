package com.example.htmlmud.domain.model;

/**
 * 枚举，表示战斗系统中的不同伤害类型。
 */
public enum DamageType {

  // === 物理系 ===
  PHYSICAL(null), // 物理 (根類別)
  SLASH(PHYSICAL), // 揮砍
  PIERCE(PHYSICAL), // 穿刺
  BLUNT(PHYSICAL), // 鈍擊


  // === 魔法系 ===
  MAGIC(null), // 魔法 (根類別)
  FIRE(MAGIC), // 火焰
  ICE(MAGIC), // 冰霜
  LIGHTNING(MAGIC), // 雷電
  POISON(MAGIC), // 毒素
  HOLY(MAGIC), // 神聖
  DARK(MAGIC), // 暗影


  // === 特殊 ===
  SONIC(null), // 音波
  TRUE(null); // 真實傷害


  private final DamageType parent;

  DamageType(DamageType parent) {
    this.parent = parent;
  }

  public DamageType getParent() {
    return parent;
  }

  /**
   * 檢查此類型是否屬於另一種類型 (包含自己) 例如: FIRE.is(MAGIC) 會回傳 true
   */
  public boolean is(DamageType other) {
    if (this == other)
      return true;
    if (parent == null)
      return false;
    return parent.is(other);
  }
}
