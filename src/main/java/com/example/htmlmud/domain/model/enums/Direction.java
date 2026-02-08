package com.example.htmlmud.domain.model.enums;

import lombok.Getter;

@Getter
public enum Direction {
  NORTH("north", "n", "北方"),

  SOUTH("south", "s", "南方"),

  EAST("east", "e", "東方"),

  WEST("west", "w", "西方"),

  NORTHEAST("northeast", "ne", "東北方"),

  NORTHWEST("northwest", "nw", "西北方"),

  SOUTHEAST("southeast", "se", "東南方"),

  SOUTHWEST("southwest", "sw", "西南方"),

  UP("up", "u", "上方"),

  DOWN("down", "d", "下方");

  private final String fullName;
  private final String shortName;
  private final String displayName;

  Direction(String fullName, String shortName, String displayName) {
    this.fullName = fullName;
    this.shortName = shortName;
    this.displayName = displayName;
  }

  // 解析輸入字串 (例如輸入 "n" 或 "North" 都能找到)
  public static Direction parse(String input) {
    if (input == null)
      return null;
    String normalized = input.trim().toLowerCase();
    for (Direction d : values()) {
      if (d.fullName.equals(normalized) || d.shortName.equals(normalized)) {
        return d;
      }
    }
    return null;
  }

  // 取得反方向 (用於：你往北走，別人看到你從"南"邊來)
  public Direction opposite() {
    return switch (this) {
      case NORTH -> SOUTH;
      case SOUTH -> NORTH;
      case EAST -> WEST;
      case WEST -> EAST;
      case NORTHEAST -> SOUTHWEST;
      case NORTHWEST -> SOUTHEAST;
      case SOUTHEAST -> NORTHWEST;
      case SOUTHWEST -> NORTHEAST;
      case UP -> DOWN;
      case DOWN -> UP;
    };
  }
}
