package com.example.htmlmud.domain.dungeon.model;

import lombok.Getter;

/**
 * 2D 地牢網格專用之 4 方向 Enum
 * @deprecated 專案已全面統一收斂為 4 方向 {@link com.example.htmlmud.domain.model.enums.Direction}。
 * 此處保留以維持平滑相容性。
 */
@Deprecated
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
   * 轉向 MUD 統一 4 方向 Direction
   */
  public com.example.htmlmud.domain.model.enums.Direction toMudDirection() {
    return com.example.htmlmud.domain.model.enums.Direction.valueOf(this.name());
  }

  /**
   * 由統一 Direction 轉向 GridDirection
   */
  public static GridDirection fromMudDirection(com.example.htmlmud.domain.model.enums.Direction dir) {
    if (dir == null) return null;
    return GridDirection.valueOf(dir.name());
  }
}
