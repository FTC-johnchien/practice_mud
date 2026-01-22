package com.example.htmlmud.domain.model;

public enum Effect {

  // debuffs
  POISONED("中毒"), // 中毒
  STUNED("昏迷"), // 昏迷
  SILENCED("禁言"), // 禁言
  FEAR("恐懼"), // 恐懼
  SLOW("減速"), // 減速
  WEAKENED("虛弱"), // 虛弱
  CURSED("詛咒"), // 詛咒
  BURNING("燃燒"), // 燃燒
  FROZEN("冰凍"), // 冰凍
  PARALYZED("麻痺"), // 麻痺
  CONFUSED("混亂"), // 混亂
  ENTANGLED("糾纏"), // 糾纏
  DISEASED("疾病"), // 疾病
  FATIGUED("疲勞"), // 疲勞
  CHARMED("魅惑"), // 魅惑
  POOR_VISION("視力不良"), // 視力不良
  DEAFENED("失聰"), // 失聰
  BLINDED("失明"), // 失明
  SAPPED("能量耗盡"), // 能量耗盡
  ANCHORED("定身"), // 定身
  CURSED_WEAPON("詛咒武器"), // 詛咒武器

  // buffs
  HEALING("治療"), // 治療
  REGENERATION("再生"), // 再生
  HASTE("加速"), // 加速
  SANCTUARY("聖護"), // 聖護
  STRENGTHEN("強化"), // 強化
  SHIELD("盾牌"), // 盾牌
  INVIGORATE("振奮"), // 振奮
  PROTECTION("保護"), // 保護
  BLESSING("祝福"), // 祝福
  FORTITUDE("堅韌"), // 堅韌
  CLARITY("清晰"), // 清晰
  SWIFTNESS("敏捷"), // 敏捷
  RESISTANCE("抵抗"), // 抵抗
  LUCK("幸運"), // 幸運
  VISION("視野"), // 視野
  ENERGY_BOOST("能量提升"), // 能量提升
  COURAGE("勇氣"), // 勇氣
  TRANQUILITY("寧靜"), // 寧靜
  FURY("狂怒"), // 狂怒
  BRAVERY("勇敢"), // 勇敢
  FOCUSING("專注中"), // 專注中

  // neutral
  INVISIBLE("隱形"); // 隱形

  private final String description;

  Effect(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
