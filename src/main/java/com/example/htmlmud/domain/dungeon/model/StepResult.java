package com.example.htmlmud.domain.dungeon.model;

public record StepResult(
    boolean success,
    String message,
    GridCoord newCoord,
    GridDirection facing,
    DungeonTile tile
) {
  public static StepResult blocked(String message, GridCoord currentCoord, GridDirection facing) {
    return new StepResult(false, message, currentCoord, facing, null);
  }

  public static StepResult ok(String message, GridCoord newCoord, GridDirection facing, DungeonTile tile) {
    return new StepResult(true, message, newCoord, facing, tile);
  }

  @Deprecated
  public static StepResult blocked(String message, GridCoord currentCoord, Direction facing) {
    return blocked(message, currentCoord, facing != null ? facing.toGridDirection() : null);
  }

  @Deprecated
  public static StepResult ok(String message, GridCoord newCoord, Direction facing, DungeonTile tile) {
    return ok(message, newCoord, facing != null ? facing.toGridDirection() : null, tile);
  }
}
