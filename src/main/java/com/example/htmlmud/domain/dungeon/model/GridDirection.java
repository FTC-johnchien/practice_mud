package com.example.htmlmud.domain.dungeon.model;

import lombok.Getter;

/**
 * 2D 地牢網格專用之 4 方向 Enum
 * (區隔於 MUD 拓撲 10 方向 WorldDirection / Direction)
 */
@Getter
public enum GridDirection {
  NORTH(0, -1, "^", "北"),
  EAST(1, 0, ">", "東"),
  SOUTH(0, 1, "v", "南"),
  WEST(-1, 0, "<", "西");

  private final int dx;
  private final int dy;
  private final String symbol;
  private final String chineseName;

  GridDirection(int dx, int dy, String symbol, String chineseName) {
    this.dx = dx;
    this.dy = dy;
    this.symbol = symbol;
    this.chineseName = chineseName;
  }

  public GridDirection turnLeft() {
    return switch (this) {
      case NORTH -> WEST;
      case WEST -> SOUTH;
      case SOUTH -> EAST;
      case EAST -> NORTH;
    };
  }

  public GridDirection turnRight() {
    return switch (this) {
      case NORTH -> EAST;
      case EAST -> SOUTH;
      case SOUTH -> WEST;
      case WEST -> NORTH;
    };
  }

  public GridDirection getOpposite() {
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

  /**
   * 轉向 MUD 10 方向 Enum (正交映射)
   */
  public com.example.htmlmud.domain.model.enums.Direction toMudDirection() {
    return switch (this) {
      case NORTH -> com.example.htmlmud.domain.model.enums.Direction.NORTH;
      case EAST -> com.example.htmlmud.domain.model.enums.Direction.EAST;
      case SOUTH -> com.example.htmlmud.domain.model.enums.Direction.SOUTH;
      case WEST -> com.example.htmlmud.domain.model.enums.Direction.WEST;
    };
  }

  /**
   * 由 MUD 10 方向 Enum 轉向 GridDirection (僅支援正交 4 向)
   */
  public static GridDirection fromMudDirection(com.example.htmlmud.domain.model.enums.Direction dir) {
    if (dir == null) return null;
    return switch (dir) {
      case NORTH -> NORTH;
      case EAST -> EAST;
      case SOUTH -> SOUTH;
      case WEST -> WEST;
      default -> null;
    };
  }
}
