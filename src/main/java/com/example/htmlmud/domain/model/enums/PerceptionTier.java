package com.example.htmlmud.domain.model.enums;

import lombok.Getter;

/**
 * 靈識認知透鏡位階 (Cognitive Perception Tier)
 * 對齊 Level 1《GAMEPLAY_CORE_LOOPS.md §5.5》與 Level 2《M1_TOWN_HUB_AND_FACILITIES.md §2.7》
 */
@Getter
public enum PerceptionTier {
  IGNORANT_XIANXIA(0, 29, "愚昧求生·純仙俠"),
  FRACTURED_GLITCH(30, 70, "知見初裂·異象微顯"),
  TRANSCENDED_TECH(71, 100, "真理洞察·科技覺醒");

  private final int minRate;
  private final int maxRate;
  private final String title;

  PerceptionTier(int minRate, int maxRate, String title) {
    this.minRate = minRate;
    this.maxRate = maxRate;
    this.title = title;
  }

  public static PerceptionTier fromRate(int rate) {
    if (rate < 30) {
      return IGNORANT_XIANXIA;
    } else if (rate <= 70) {
      return FRACTURED_GLITCH;
    } else {
      return TRANSCENDED_TECH;
    }
  }

  public String getDisplayName() {
    return title;
  }
}
