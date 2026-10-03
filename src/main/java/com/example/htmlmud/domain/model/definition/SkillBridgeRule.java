package com.example.htmlmud.domain.model.definition;

/**
 * MUD 技能達到指定等級時解鎖 DRPG 小隊技能之橋接規則 (可由 JSON 定義驅動)
 */
public record SkillBridgeRule(
    String targetDrpgSkillId,
    int requiredLevel,
    String notes
) {
  public static SkillBridgeRule of(String targetDrpgSkillId, int requiredLevel) {
    return new SkillBridgeRule(targetDrpgSkillId, requiredLevel, null);
  }

  public static SkillBridgeRule of(String targetDrpgSkillId, int requiredLevel, String notes) {
    return new SkillBridgeRule(targetDrpgSkillId, requiredLevel, notes);
  }
}
