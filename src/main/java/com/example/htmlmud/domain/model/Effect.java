package com.example.htmlmud.domain.model;

public record Effect(

    String name,

    EffectType type,

    TargetType target,

    double chance,

    int duration,

    double value,

    String triggerMsg,

    String effectMsg

) {
  public Effect {
    if (name == null) {
      name = "";
    }
    if (target == null) {
      target = TargetType.ENEMY;
    }
    if (chance == 0) {
      chance = 1;
    }
    if (duration == 0) {
      duration = 2000;
    }
    if (triggerMsg == null) {
      triggerMsg = "";
    }
    if (effectMsg == null) {
      effectMsg = "";
    }
  }
}
