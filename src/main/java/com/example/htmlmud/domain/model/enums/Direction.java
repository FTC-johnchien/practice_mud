package com.example.htmlmud.domain.model.enums;

import lombok.Getter;

/**
 * 核心 4 方向移動列舉 (North, East, South, West)
 * 統一支援 MUD 房間移動與 DRPG 網格步進探索。
 * 非 4 方向指令（如 UP/DOWN 樓梯、ENTER/OUT 進出、BUY/SELL 交易）統一由網頁功能按鈕與動作指令處理。
 */
@Getter
public enum Direction {
  NORTH("north", "n", "北方", 0, -1, "^", "▲"),
  EAST("east", "e", "東方", 1, 0, ">", "▶"),
  SOUTH("south", "s", "南方", 0, 1, "v", "▼"),
  WEST("west", "w", "西方", -1, 0, "<", "◀");

  private final String fullName;
  private final String shortName;
  private final String displayName;
  private final int dx;
  private final int dy;
  private final String symbol;
  private final String arrow;

  Direction(String fullName, String shortName, String displayName, int dx, int dy, String symbol, String arrow) {
    this.fullName = fullName;
    this.shortName = shortName;
    this.displayName = displayName;
    this.dx = dx;
    this.dy = dy;
    this.symbol = symbol;
    this.arrow = arrow;
  }

  public String getChineseName() {
    return displayName;
  }

  /**
   * 左轉 90 度
   */
  public Direction turnLeft() {
    return switch (this) {
      case NORTH -> WEST;
      case WEST -> SOUTH;
      case SOUTH -> EAST;
      case EAST -> NORTH;
    };
  }

  /**
   * 右轉 90 度
   */
  public Direction turnRight() {
    return switch (this) {
      case NORTH -> EAST;
      case EAST -> SOUTH;
      case SOUTH -> WEST;
      case WEST -> NORTH;
    };
  }

  /**
   * 取得反方向 (180 度)
   */
  public Direction opposite() {
    return switch (this) {
      case NORTH -> SOUTH;
      case SOUTH -> NORTH;
      case EAST -> WEST;
      case WEST -> EAST;
    };
  }

  public Direction getOpposite() {
    return opposite();
  }

  /**
   * 解析輸入字串 (如 "n", "North", "w", "step w" 等)
   */
  public static Direction parse(String input) {
    if (input == null || input.isBlank()) {
      return null;
    }
    String normalized = input.trim().toLowerCase();
    for (Direction d : values()) {
      if (d.fullName.equals(normalized) || d.shortName.equals(normalized)) {
        return d;
      }
    }
    return switch (normalized) {
      case "w", "up_key" -> NORTH;
      case "s", "down_key" -> SOUTH;
      case "a", "left_key" -> WEST;
      case "d", "right_key" -> EAST;
      default -> null;
    };
  }
}
