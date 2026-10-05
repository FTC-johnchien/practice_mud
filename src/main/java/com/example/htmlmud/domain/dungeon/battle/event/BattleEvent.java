package com.example.htmlmud.domain.dungeon.battle.event;

import java.util.List;

public record BattleEvent(
    long seq,
    long ts,
    BattleEventType type,
    UnitRef actor,
    String skillId,
    String skillName,
    String weaponType,
    String damageType,
    FxShape shape,
    String fxKey,
    List<BattleHit> hits
) {
  public BattleEvent {
    hits = (hits != null) ? List.copyOf(hits) : List.of();
  }
}
