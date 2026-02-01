package com.example.htmlmud.domain.model;

import java.util.Set;

public record UsageConfig(

    String reqGuild,

    Set<WeaponType> allowedWeapons,

    boolean allowMounted

) {
}
