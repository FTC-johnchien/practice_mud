package com.example.htmlmud.combat;

import java.util.List;

/**
 * 戰鬥敵方實體資料傳輸物件 (BattleEnemyDto)
 * 提供精簡戰況與即時蓄力打斷狀態
 */
public record BattleEnemyDto(
    int index,
    String id,
    String name,
    int hp,
    int maxHp,
    String currentAction,
    int actionProgressPercent,
    boolean isCasting,
    long castDurationMs,
    long castRemainingMs,
    boolean interruptible,
    int poise,
    int maxPoise,
    boolean isPoiseBroken,
    List<ActiveBuffDto> activeBuffs,
    int currentPhase,
    int maxPhases,
    List<Integer> phaseThresholds,
    boolean isEnraged
) {
  public BattleEnemyDto(
      int index,
      String id,
      String name,
      int hp,
      int maxHp,
      String currentAction,
      int actionProgressPercent,
      boolean isCasting,
      long castDurationMs,
      long castRemainingMs,
      boolean interruptible,
      int poise,
      int maxPoise,
      boolean isPoiseBroken,
      List<ActiveBuffDto> activeBuffs
  ) {
    this(
        index, id, name, hp, maxHp, currentAction, actionProgressPercent,
        isCasting, castDurationMs, castRemainingMs, interruptible,
        poise, maxPoise, isPoiseBroken, activeBuffs,
        1, 1, List.of(), false
    );
  }

  public static BattleEnemyDto from(BattleEnemyViewDto v) {
    if (v == null) return null;
    return new BattleEnemyDto(
        v.index(),
        v.id(),
        v.name(),
        v.hp(),
        v.maxHp(),
        v.currentAction(),
        v.actionProgressPercent(),
        v.isCasting(),
        v.castDurationMs(),
        v.castRemainingMs(),
        v.interruptible(),
        v.poise(),
        v.maxPoise(),
        v.isPoiseBroken(),
        v.activeBuffs(),
        v.currentPhase(),
        v.maxPhases(),
        v.phaseThresholds() != null ? v.phaseThresholds() : List.of(),
        v.isEnraged()
    );
  }

  public static BattleEnemyDto from(int index, com.example.htmlmud.combat.BattleEnemy e) {
    if (e == null) return null;
    List<ActiveBuffDto> buffs = e.getActiveBuffs() != null
        ? e.getActiveBuffs().stream().map(ActiveBuffDto::fromActiveBuff).toList()
        : List.of();
    return new BattleEnemyDto(
        index,
        e.getId(),
        e.getName(),
        e.getHp(),
        e.getMaxHp(),
        e.getCurrentAction(),
        e.getActionProgressPercent(),
        e.isCasting(),
        e.getCastDurationMs(),
        e.getCastRemainingMs(),
        e.isInterruptible(),
        e.getPoise(),
        e.getMaxPoise(),
        e.isPoiseBroken(),
        buffs,
        e.getCurrentPhase(),
        e.getMaxPhases(),
        e.getPhaseThresholds() != null ? e.getPhaseThresholds() : List.of(),
        e.isEnraged()
    );
  }
}
