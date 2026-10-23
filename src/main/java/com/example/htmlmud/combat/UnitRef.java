package com.example.htmlmud.combat;

public record UnitRef(Side side, String id, int index) {
  public enum Side {
    PARTY,
    ENEMY
  }
}
