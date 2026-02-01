package com.example.htmlmud.domain.model;

public record ScalingConfig(

    double chargeRegenPerLevel,

    double counterRatePerLevel,

    double critRatePerLevel,

    double damagePerLevel,

    double dodgeRatePerLevel,

    double hitRatePerLevel,

    double parryRatePerLevel

) {
}
