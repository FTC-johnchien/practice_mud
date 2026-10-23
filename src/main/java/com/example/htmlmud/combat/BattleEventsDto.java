package com.example.htmlmud.combat;

import java.util.List;
import com.example.htmlmud.combat.BattleEvent;

public record BattleEventsDto(
    String type,
    String battleId,
    List<BattleEvent> events
) {
  public BattleEventsDto(String battleId, List<BattleEvent> events) {
    this("BATTLE_EVENTS", battleId, events);
  }
}
