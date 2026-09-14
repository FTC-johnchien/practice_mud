package com.example.htmlmud.domain.dungeon.model;

public record StepResult(
    boolean success,
    String message,
    GridCoord newCoord,
    Direction facing,
    DungeonTile tile
) {
  public static StepResult blocked(String message, GridCoord currentCoord, Direction facing) {
    return new StepResult(false, message, currentCoord, facing, null);
  }

  public static StepResult ok(String message, GridCoord newCoord, Direction facing, DungeonTile tile) {
    return new StepResult(true, message, newCoord, facing, tile);
  }
}
