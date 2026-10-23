package com.example.htmlmud.party;

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
  private String itemId;
  @Builder.Default
  private GambitActionType actionType = GambitActionType.SKILL;
  @Builder.Default
  private boolean enabled = true;

  public String formatDescription(String skillName) {
    String actionName;
    if (actionType == GambitActionType.ITEM) {
      actionName = "使用藥品 " + (itemId != null ? itemId : "");
    } else if (actionType == GambitActionType.DEFENSE) {
      actionName = "防禦招架";
    } else if (actionType == GambitActionType.ATTACK) {
      actionName = "普通攻擊";
    } else {
      actionName = (skillName != null && !skillName.isEmpty()) ? skillName : (skillId != null ? skillId : "戰術招式");
    }

    String condStr = switch (condition) {
      case ALLY_HP_LESS_THAN -> "隊友氣血 < " + conditionValue + "%";
      case SELF_HP_LESS_THAN -> "自身氣血 < " + conditionValue + "%";
      case ENEMY_COUNT_GTE -> "敵方存活 >= " + conditionValue + " 體";
      case ENEMY_IS_BOSS -> "遭遇煞氣首領";
      case RESOURCE_GTE -> "自身戰意資源 >= " + conditionValue;
      case TARGET_LACKS_BUFF -> "目標缺少狀態";
      case TARGET_HAS_BUFF -> "目標已擁有狀態";
      case BUFF_TIME_LESS_THAN -> "目標狀態剩餘時間 < " + conditionValue + "秒";
      case ALLY_HAS_DEBUFF -> "隊友身中異常/減益";
      case ALLY_IS_CONTROLLED -> "隊友處於受控/封咒狀態";
      case SELF_LACKS_SHIELD -> "自身未具備護盾";
      case ENEMY_IS_CASTING -> "敵方正在施法蓄力";
      case ENEMY_CASTING_INTERRUPTIBLE -> "敵方處於可打斷施法中";
      case ENEMY_POISE_BROKEN -> "敵方處於破防失衡狀態";
      case ENEMY_HP_LESS_THAN -> "敵方氣血 < " + conditionValue + "%";
      case ALLY_SAN_LESS_THAN -> "隊友道心 < " + conditionValue + "%";
      case ALWAYS -> "總是施展";
      default -> condition != null ? condition.getLabel() : "未明條件";
    };
    return String.format("#%d [%s] 當【%s】對【%s】%s【%s】",
        priority, enabled ? "啟用" : "停用", condStr, target != null ? target.getLabel() : "目標",
        actionType == GambitActionType.ITEM ? "" : "施展", actionName);
  }
}
