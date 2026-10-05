package com.example.htmlmud.domain.dungeon.battle.event;

public record BattleHit(
    UnitRef target,
    HitOutcome outcome,
    int amount,
    int absorbed,
    boolean killed
) {
  public BattleHit(UnitRef target, HitOutcome outcome, int amount, boolean killed) {
    this(target, outcome, amount, 0, killed);
  }
}
