package com.example.htmlmud.domain.model;

public enum EffectType {

  // debuffs
  ANCHORED("定身"), // 定身
  BLIND("失明"), // 失明
  BURNING("燃燒"), // 燃燒
  CHARMED("魅惑"), // 魅惑
  CONFUSED("混亂"), // 混亂
  CURSED("詛咒"), // 詛咒
  CURSED_WEAPON("詛咒武器"), // 詛咒武器
  DEAFENED("失聰"), // 失聰
  DISEASED("疾病"), // 疾病
  ENTANGLED("糾纏"), // 糾纏
  FATIGUED("疲勞"), // 疲勞
  FEAR("恐懼"), // 恐懼
  FROZEN("冰凍"), // 冰凍
  PARALYZE("麻痺"), // 麻痺
  POISON("中毒"), // 中毒
  SAPPED("能量耗盡"), // 能量耗盡
  SILENCE("禁言"), // 禁言
  SLOW("減速"), // 減速
  STUNED("昏迷"), // 昏迷
  WEAKENED("虛弱"), // 虛弱


  // buffs
  BLESSING("祝福"), // 祝福
  BRAVERY("勇敢"), // 勇敢
  CLARITY("清晰"), // 清晰
  COURAGE("勇氣"), // 勇氣
  DAMAGE_REDUCE("傷害減免"), // 傷害減免
  ENERGY_BOOST("能量提升"), // 能量提升
  FOCUSING("專注中"), // 專注中
  FORTITUDE("堅韌"), // 堅韌
  FURY("狂怒"), // 狂怒
  HASTE("加速"), // 加速
  HEALING("治療"), // 治療
  INVIGORATE("振奮"), // 振奮
  LUCK("幸運"), // 幸運
  PROTECTION("保護"), // 保護
  REGENERATION("再生"), // 再生
  RESISTANCE("抵抗"), // 抵抗
  SANCTUARY("聖護"), // 聖護
  SHIELD("盾牌"), // 盾牌
  STRENGTHEN("強化"), // 強化
  SWIFTNESS("敏捷"), // 敏捷
  TRANQUILITY("寧靜"), // 寧靜
  VISION("視野"), // 視野


  // neutral
  INVISIBLE("隱形"); // 隱形

  private final String description;

  EffectType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
