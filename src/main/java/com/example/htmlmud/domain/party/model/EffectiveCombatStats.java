package com.example.htmlmud.domain.party.model;

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
