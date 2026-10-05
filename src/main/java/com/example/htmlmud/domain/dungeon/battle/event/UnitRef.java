package com.example.htmlmud.domain.dungeon.battle.event;

public record UnitRef(Side side, String id, int index) {
  public enum Side {
    PARTY,
    ENEMY
  }
}
