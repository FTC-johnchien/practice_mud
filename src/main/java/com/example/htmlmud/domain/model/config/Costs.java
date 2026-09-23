package com.example.htmlmud.domain.model.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Costs(
    int hp,
    int mp,
    @JsonAlias({"sp", "stamina"})
    int stamina,
    int charge, // 攻擊所累積的能量
    int ammo // 彈藥數量
) {
  public int sp() {
    return stamina > 0 ? stamina : charge;
  }
}
