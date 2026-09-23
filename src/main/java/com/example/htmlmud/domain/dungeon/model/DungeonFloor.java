package com.example.htmlmud.domain.dungeon.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DungeonFloor {
  private String id;
  private String name;
  private String description;
  private int width;
  private int height;
  private DungeonTile[][] tiles;
  private GridCoord startCoord;
  private GridDirection startFacing;
  @Builder.Default
  private int dangerRate = 15;
  @Builder.Default
  private List<String> mobPool = new ArrayList<>();

  @Deprecated
  public void setStartFacing(Direction dir) {
    this.startFacing = (dir != null) ? dir.toGridDirection() : null;
  }

  public void setStartFacing(GridDirection dir) {
    this.startFacing = dir;
  }

  public boolean isInBounds(int x, int y) {
    return x >= 0 && x < width && y >= 0 && y < height;
  }

  public boolean isInBounds(GridCoord coord) {
    return coord != null && isInBounds(coord.x(), coord.y());
  }

  public DungeonTile getTile(int x, int y) {
    if (!isInBounds(x, y)) {
      return DungeonTile.wall();
    }
    return tiles[y][x];
  }

  public DungeonTile getTile(GridCoord coord) {
    return getTile(coord.x(), coord.y());
  }

  public void setTile(int x, int y, DungeonTile tile) {
    if (isInBounds(x, y)) {
      tiles[y][x] = tile;
    }
  }
}
