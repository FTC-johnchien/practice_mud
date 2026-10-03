package com.example.htmlmud.domain.model.enums;

import java.util.List;

/**
 * 陣法與隊伍角色職能分類 (Role Category)
 * 統一陣法需求檢定與職業定位，取代字串匹配與硬編碼中文別名。
 */
public enum RoleCategory {

  TANK("肉盾", "體修/肉盾", List.of("WARRIOR", "TANK")),

  MELEE_DPS("近戰輸出", "劍修/近戰", List.of("SWORDSMAN", "MONK")),

  RANGED_DPS("遠程輸出", "遊俠/刺客", List.of("ROGUE", "RANGER")),

  MAGIC_DPS("法術輸出", "符修/法修", List.of("MAGE", "WIZARD", "TAOIST")),

  HEALER("治療輔助", "丹修/靈醫", List.of("CLERIC", "HEALER"));

  private final String displayName;
  private final String roleDescription;
  private final List<String> matchingClassIds;

  RoleCategory(String displayName, String roleDescription, List<String> matchingClassIds) {
    this.displayName = displayName;
    this.roleDescription = roleDescription;
    this.matchingClassIds = matchingClassIds;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getRoleDescription() {
    return roleDescription;
  }

  public List<String> getMatchingClassIds() {
    return matchingClassIds;
  }

  /**
   * 根據職業 ID 或角色稱號判定所屬職能
   */
  public static RoleCategory fromClassOrRole(String classId, String roleTitle) {
    String cId = (classId != null) ? classId.trim().toUpperCase() : "";

    switch (cId) {
      case "WARRIOR", "TANK" -> { return TANK; }
      case "SWORDSMAN", "MONK" -> { return MELEE_DPS; }
      case "ROGUE", "RANGER" -> { return RANGED_DPS; }
      case "MAGE", "WIZARD", "TAOIST" -> { return MAGIC_DPS; }
      case "CLERIC", "HEALER" -> { return HEALER; }
    }

    if (roleTitle != null) {
      String r = roleTitle.trim();
      if (r.contains("體修") || r.contains("力士") || r.contains("玄甲")) return TANK;
      if (r.contains("劍修") || r.contains("劍宗") || r.contains("武僧")) return MELEE_DPS;
      if (r.contains("遊俠") || r.contains("刺客") || r.contains("追魂")) return RANGED_DPS;
      if (r.contains("符修") || r.contains("法修") || r.contains("幽冥")) return MAGIC_DPS;
      if (r.contains("丹修") || r.contains("靈醫") || r.contains("醫仙") || r.contains("素問")) return HEALER;
    }

    return null;
  }

  /**
   * 解析需求字串為職能類別
   */
  public static RoleCategory parseRequirement(String req) {
    if (req == null || req.isBlank()) return null;
    String r = req.trim().toUpperCase();

    try {
      return RoleCategory.valueOf(r);
    } catch (IllegalArgumentException ignored) {}

    switch (r) {
      case "WARRIOR", "TANK", "戰", "戰士", "肉盾", "體修" -> { return TANK; }
      case "SWORDSMAN", "MONK", "劍", "劍修", "近戰" -> { return MELEE_DPS; }
      case "ROGUE", "RANGER", "遊", "遊俠", "刺客" -> { return RANGED_DPS; }
      case "MAGE", "WIZARD", "TAOIST", "法", "法師", "符修", "法修" -> { return MAGIC_DPS; }
      case "CLERIC", "HEALER", "牧", "牧師", "丹修", "靈醫", "醫仙" -> { return HEALER; }
      default -> { return null; }
    }
  }
}
