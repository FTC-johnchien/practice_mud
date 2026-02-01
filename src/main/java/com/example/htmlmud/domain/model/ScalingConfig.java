package com.example.htmlmud.domain.model;

public record ScalingConfig(

    int maxLevel,

    int baseDamage,

    double damagePerLevel,

    double hitRatePerLevel,

    double parryRatePerLevel,

    double dodgeRatePerLevel,

    double counterRatePerLevel,

    double critRatePerLevel

) {

}
