package com.example.htmlmud.domain.model.config;

import com.example.htmlmud.domain.model.enums.BuffCategory;
import com.example.htmlmud.domain.model.enums.BuffType;
import com.example.htmlmud.domain.model.enums.EffectType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/**
 * 技能所附帶之 Buff / Debuff 效果設定 (定義於技能模板 JSON 的 buff 節點)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BuffConfig(
    String id,
    String name,
    String icon,
    BuffType type,
    BuffCategory category,
    EffectType effectType,
    int durationSeconds,
    int tickIntervalSeconds,
    double percentOfTargetHp,
    int baseShield,
    int healPerTick,
    double healPercentPerTick,
    int damagePerTick,
    int maxStacks,
    Map<String, Double> statModifiers
) {
  public BuffConfig {
    if (type == null) type = BuffType.BUFF;
    if (category == null) category = BuffCategory.SHIELD;
    if (maxStacks <= 0) maxStacks = 1;
  }
}
