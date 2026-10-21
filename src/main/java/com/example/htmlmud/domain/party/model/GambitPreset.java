package com.example.htmlmud.domain.party.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 戰術方針範本 (Gambit Preset)，提供一鍵配置策略鏈之資料模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GambitPreset {
  private String id;
  private String name;
  private String description;
  private String icon;
  private List<GambitPresetRule> rules;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GambitPresetRule {
    private int priority;
    private TacticsCondition condition;
    private int conditionValue;
    private TacticsTarget target;
    @Builder.Default
    private GambitActionType actionType = GambitActionType.SKILL;
    private String preferredTag;
    private String fallbackSkillId;
  }
}
