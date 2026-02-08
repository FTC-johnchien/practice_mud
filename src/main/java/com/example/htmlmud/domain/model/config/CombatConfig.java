package com.example.htmlmud.domain.model.config;

import java.util.ArrayList;
import java.util.List;

public record CombatConfig(

    List<NaturalAttack> naturalAttacks,

    String naturalDodge,

    String naturalParry,

    String naturalForce,

    int naturalArmor,

    int attacksPerRound

) {
  public CombatConfig {
    if (attacksPerRound == 0) {
      attacksPerRound = 1;
    }
    if (naturalAttacks == null) {
      naturalAttacks = new ArrayList<>();
    }
  }
}
