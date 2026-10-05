package com.example.htmlmud.domain.dungeon.dto;

import java.util.List;
import com.example.htmlmud.domain.dungeon.battle.event.BattleEvent;

public record BattleEventsDto(
    String type,
    String battleId,
    List<BattleEvent> events
) {
  public BattleEventsDto(String battleId, List<BattleEvent> events) {
    this("BATTLE_EVENTS", battleId, events);
  }
}
