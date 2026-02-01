package com.example.htmlmud.domain.model;

import java.util.Map;

public record Mechanics(

    double chargeRegen,

    double counterRate,

    double critRate,

    int damage,

    DamageType damageType,

    double dodgeRate,

    double hitRate,

    double learningDifficulty, // 難度係數 (1.0 標準, 1.5 難練)

    int maxLevel,

    double scaleFactor, // scaleStat的倍數 1.0 (有多少力打多少痛)

    ResourceType scaleStat, // 吃「屬性 STR, INT, DEX, CON」加成的

    Map<String, String> formulas // 計算公式

) {
  public Mechanics {
    if (scaleFactor == 0) {
      scaleFactor = 1;
    }
    if (learningDifficulty == 0) {
      learningDifficulty = 1;
    }
    if (maxLevel == 0) {
      maxLevel = 100;
    }
  }

  public String getFormula(String name) {
    return formulas.get(name);
  }
}
