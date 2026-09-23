package com.example.htmlmud.domain.dungeon.model;

import lombok.Getter;

/**
 * @deprecated 請改用 {@link GridDirection} 以避免與 MUD 拓撲 {@link com.example.htmlmud.domain.model.enums.Direction} 命名衝突。
 */
@Deprecated
@Getter
public enum Direction {
  NORTH(0, -1, "^", "北"),
  EAST(1, 0, ">", "東"),
  SOUTH(0, 1, "v", "南"),
  WEST(-1, 0, "<", "西");

  private final int dx;
  private final int dy;
  private final String symbol;
  private final String chineseName;

  Direction(int dx, int dy, String symbol, String chineseName) {
    this.dx = dx;
    this.dy = dy;
    this.symbol = symbol;
    this.chineseName = chineseName;
  }

  public Direction turnLeft() {
    return switch (this) {
      case NORTH -> WEST;
      case WEST -> SOUTH;
      case SOUTH -> EAST;
      case EAST -> NORTH;
    };
  }

  public Direction turnRight() {
    return switch (this) {
      case NORTH -> EAST;
      case EAST -> SOUTH;
      case SOUTH -> WEST;
      case WEST -> NORTH;
    };
  }

  public Direction getOpposite() {
    return switch (this) {
      case NORTH -> SOUTH;
      case SOUTH -> NORTH;
      case EAST -> WEST;
      case WEST -> EAST;
    };
  }

  public String getArrow() {
    return switch (this) {
      case NORTH -> "▲";
      case EAST -> "▶";
      case SOUTH -> "▼";
      case WEST -> "◀";
    };
  }

  public GridDirection toGridDirection() {
    return GridDirection.valueOf(this.name());
  }

  public static Direction fromGridDirection(GridDirection gd) {
    if (gd == null) return null;
    return Direction.valueOf(gd.name());
  }
}
