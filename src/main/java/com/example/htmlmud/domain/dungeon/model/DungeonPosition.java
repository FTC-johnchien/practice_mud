package com.example.htmlmud.domain.dungeon.model;

import lombok.Data;

@Data
public class DungeonPosition {
  private String floorId;
  private GridCoord coord;
  private GridDirection facing;
  private boolean[][] visited;
  private int width;
  private int height;
  private int dangerLevel = 0;
  private final java.util.Set<GridCoord> openedChests = new java.util.HashSet<>();

  public DungeonPosition(String floorId, int startX, int startY, GridDirection facing, int width, int height) {
    this.floorId = floorId;
    this.coord = new GridCoord(startX, startY);
    this.facing = facing != null ? facing : GridDirection.NORTH;
    this.width = width;
    this.height = height;
    this.visited = new boolean[height][width];
    this.dangerLevel = 0;
    revealAround(startX, startY, 1);
  }

  @Deprecated
  public DungeonPosition(String floorId, int startX, int startY, Direction facing, int width, int height) {
    this(floorId, startX, startY, facing != null ? facing.toGridDirection() : GridDirection.NORTH, width, height);
  }

  @Deprecated
  public void setFacing(Direction facing) {
    this.facing = facing != null ? facing.toGridDirection() : null;
  }

  public void setFacing(GridDirection facing) {
    this.facing = facing;
  }

  public int addDanger(int delta) {
    this.dangerLevel = Math.min(100, Math.max(0, this.dangerLevel + delta));
    return this.dangerLevel;
  }

  public void resetDanger() {
    this.dangerLevel = 0;
  }

  public String getDangerStatus() {
    if (dangerLevel < 40) return "CALM";
    if (dangerLevel < 70) return "CAUTION";
    if (dangerLevel < 90) return "DANGER";
    return "ENCOUNTER";
  }

  public boolean isChestOpened(int x, int y) {
    return openedChests.contains(new GridCoord(x, y));
  }

  public void markChestOpened(int x, int y) {
    openedChests.add(new GridCoord(x, y));
  }

  public int getX() {
    return coord.x();
  }

  public int getY() {
    return coord.y();
  }

  public void setCoord(int x, int y) {
    this.coord = new GridCoord(x, y);
    revealAround(x, y, 1);
  }

  public void setCoord(GridCoord newCoord) {
    if (newCoord != null) {
      setCoord(newCoord.x(), newCoord.y());
    }
  }

  public boolean isVisited(int x, int y) {
    if (x >= 0 && x < width && y >= 0 && y < height) {
      return visited[y][x];
    }
    return false;
  }

  public void markVisited(int x, int y) {
    if (x >= 0 && x < width && y >= 0 && y < height) {
      visited[y][x] = true;
    }
  }

  public void revealAround(int cx, int cy, int radius) {
    for (int dy = -radius; dy <= radius; dy++) {
      for (int dx = -radius; dx <= radius; dx++) {
        markVisited(cx + dx, cy + dy);
      }
    }
  }

  public void turnLeft() {
    this.facing = this.facing.turnLeft();
  }

  public void turnRight() {
    this.facing = this.facing.turnRight();
  }
}
