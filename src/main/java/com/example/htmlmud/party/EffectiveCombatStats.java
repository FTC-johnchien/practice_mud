package com.example.htmlmud.party;

public record EffectiveCombatStats(
    int minDamage,
    int maxDamage,
    int defense,
    int hp,
    int maxHp,
    int mp,
    int maxMp,
    int san,
    int maxSan
) {}
