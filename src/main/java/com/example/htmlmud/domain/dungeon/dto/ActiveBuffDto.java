package com.example.htmlmud.domain.dungeon.dto;

import com.example.htmlmud.domain.dungeon.battle.ActiveBuff;

/**
 * 前端雙舞台（戰鬥 HUD 與角色道基面板）共用之狀態效果投影 DTO (BUFF-01 & Phase 3)
 */
public record ActiveBuffDto(
    String id,
    String name,
    String icon,
    String category,      // SHIELD, HOT, DOT, STAT_MODIFIER, CONTROL
    String effectType,    // POISON, HASTE, STUNED...
    String element,       // METAL, WOOD, WATER, FIRE, EARTH, WIND, LIGHTNING, DARK, HOLY
    int value,            // 護盾值或每跳數值
    int stacks,           // 當前層數
    int remainingSeconds, // 剩餘秒數 (常駐為 -1)
    boolean isPermanent,  // 是否為常駐陣法
    String description    // 詳細說明
) {
  public static ActiveBuffDto fromActiveBuff(ActiveBuff b) {
    if (b == null) return null;
    String elem = b.getElement() != null && !b.getElement().isBlank() ? b.getElement() : "NONE";
    if ("NONE".equals(elem)) {
      if (b.getEffectType() != null) {
        String eff = b.getEffectType().name();
        if (eff.contains("POISON")) elem = "WOOD";
        else if (eff.contains("BURN") || eff.contains("FIRE")) elem = "FIRE";
        else if (eff.contains("FROZEN") || eff.contains("ICE")) elem = "WATER";
        else if (eff.contains("PARALYZE") || eff.contains("LIGHTNING")) elem = "LIGHTNING";
        else if (eff.contains("HASTE") || eff.contains("SWIFT")) elem = "WIND";
      }
    }
    return new ActiveBuffDto(
        b.getId(),
        b.getName(),
        b.getIcon() != null ? b.getIcon() : "✨",
        b.getCategory() != null ? b.getCategory().name() : "BUFF",
        b.getEffectType() != null ? b.getEffectType().name() : "NONE",
        elem,
        b.getValue(),
        Math.max(1, b.getStacks()),
        b.getRemainingSeconds(),
        b.isPermanent(),
        b.getDescription() != null ? b.getDescription() : ""
    );
  }
}
