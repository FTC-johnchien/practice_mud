package com.example.htmlmud.domain.dungeon.model;

public record GridCoord(int x, int y) {
  public GridCoord step(Direction direction) {
    return new GridCoord(this.x + direction.getDx(), this.y + direction.getDy());
  }

  public GridCoord step(Direction direction, int steps) {
    return new GridCoord(this.x + direction.getDx() * steps, this.y + direction.getDy() * steps);
  }

  @Override
  public String toString() {
    return "(" + x + ", " + y + ")";
  }
}
