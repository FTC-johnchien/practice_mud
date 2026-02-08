package com.example.htmlmud.domain.model.config;

import java.util.List;
import java.util.Map;
import com.example.htmlmud.domain.model.Effect;
import com.example.htmlmud.domain.model.enums.ResourceType;

public record MoveAction(

    String name,

    int reqSkillLevel,

    int weight,

    double damageMod,

    double hitRateMod,

    double critRateMod,

    Map<ResourceType, Integer> costs,

    int cooldown,

    MoveMessage msg,

    List<Effect> effects

) implements Weighted {
  public MoveAction {
    if (weight == 0) {
      weight = 50;
    }
    if (cooldown == 0) {
      cooldown = 2000;
    }
    if (damageMod == 0) {
      damageMod = 1;
    }
  }

  @Override
  public int getWeight() {
    return weight;
  }
}
