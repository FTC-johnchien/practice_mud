package com.example.htmlmud.domain.party.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 戰術方針規則實體 (Tactics Rule / Gambit Rule)
 * 依照 priority 優先級由小到大 (1, 2, 3...) 循序求值。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TacticsRule {
  private int priority;
  private TacticsCondition condition;
  private int conditionValue;
  @Builder.Default
  private TacticsTarget target = TacticsTarget.LOWEST_HP_ALLY;
  private String skillId;
  @Builder.Default
  private boolean enabled = true;

  public String formatDescription(String skillName) {
    String sName = (skillName != null && !skillName.isEmpty()) ? skillName : skillId;
    String condStr = switch (condition) {
      case ALLY_HP_LESS_THAN -> "隊友氣血 < " + conditionValue + "%";
      case SELF_HP_LESS_THAN -> "自身氣血 < " + conditionValue + "%";
      case ENEMY_COUNT_GTE -> "敵方存活 >= " + conditionValue + " 體";
      case ENEMY_IS_BOSS -> "遭遇煞氣首領";
      case RESOURCE_GTE -> "自身戰意資源 >= " + conditionValue;
      case ALWAYS -> "總是施展";
    };
    return String.format("#%d [%s] 當【%s】對【%s】施展【%s】",
        priority, enabled ? "啟用" : "停用", condStr, target.getLabel(), sName);
  }
}
