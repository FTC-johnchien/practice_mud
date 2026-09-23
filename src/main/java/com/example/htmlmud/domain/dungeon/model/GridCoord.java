package com.example.htmlmud.domain.dungeon.model;

public record GridCoord(int x, int y) {
  public GridCoord step(GridDirection direction) {
    if (direction == null) return this;
    return new GridCoord(this.x + direction.getDx(), this.y + direction.getDy());
  }

  public GridCoord step(GridDirection direction, int steps) {
    if (direction == null) return this;
    return new GridCoord(this.x + direction.getDx() * steps, this.y + direction.getDy() * steps);
  }

  @Deprecated
  public GridCoord step(Direction direction) {
    return direction != null ? step(direction.toGridDirection()) : this;
  }

  @Deprecated
  public GridCoord step(Direction direction, int steps) {
    return direction != null ? step(direction.toGridDirection(), steps) : this;
  }

  @Override
  public String toString() {
    return "(" + x + ", " + y + ")";
  }
}
