package com.example.htmlmud.domain.model;

import java.util.Map;
import java.util.Set;

public record MoveAction(

    String name,

    int reqSkillLevel,

    double damageMod,

    double hitMod,

    Map<ResourceType, Integer> costs,

    int cooldown,

    MoveMessage msg,

    Set<Effect> effects

) {

}
