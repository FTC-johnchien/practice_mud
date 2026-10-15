package com.example.htmlmud.domain.dungeon.dto;

/**
 * 戰鬥怪物資料模型傳輸物件 (CombatMobDto)
 */
public record CombatMobDto(
    String id,
    String name,
    int hp,
    int maxHp,
    String currentAction,
    int actionProgressPercent,
    boolean interruptible
) {
  public static CombatMobDto of(String id, String name, int hp, int maxHp, String currentAction, int actionProgressPercent, boolean interruptible) {
    return new CombatMobDto(id, name, hp, maxHp, currentAction, actionProgressPercent, interruptible);
  }
}
