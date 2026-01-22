package com.example.htmlmud.domain.model;

public enum ItemLocation {

  GROUND("地上"),

  INVENTORY("背包中"),

  EQUIPPED("裝備中"),

  CONTAINER("在容器內");

  private final String description;

  ItemLocation(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
