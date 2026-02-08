package com.example.htmlmud.domain.model.config;

import java.util.Set;
import com.example.htmlmud.domain.model.enums.WeaponType;

public record UsageConfig(

    String reqGuild,

    Set<WeaponType> allowedWeapons,

    boolean allowMounted

) {
}
