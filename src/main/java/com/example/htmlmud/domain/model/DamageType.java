package com.example.htmlmud.domain.model;

/**
 * 枚举，表示战斗系统中的不同伤害类型。
 */
public enum DamageType {
  /**
   * 物理伤害，通常来自武器或徒手攻击。
   */
  PHYSICAL,

  /**
   * 魔法伤害，来自法术或附魔物品。
   */
  MAGICAL,

  /**
   * 内伤，影响气或精神，通常能穿透物理护甲。
   */
  INTERNAL
}
