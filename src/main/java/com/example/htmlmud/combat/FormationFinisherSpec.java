package com.example.htmlmud.combat;

/**
 * 陣法天外終結技規格資料 (Formation Finisher Spec)
 * 對齊 Level 2《SYNERGY_AND_COMBOS.md》第六節 15 大動態小隊終結技
 */
public record FormationFinisherSpec(
    String id,
    String name,
    String cyberLore,
    String damageType,
    double damageMultiplierBonus,
    int poiseBonus,
    int hpHealBonus,
    int sanHealBonus,
    String logDescription
) {}
