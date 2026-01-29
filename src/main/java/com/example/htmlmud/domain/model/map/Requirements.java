package com.example.htmlmud.domain.model.map;

import java.util.Set;
import com.example.htmlmud.domain.model.ClassType;
import com.example.htmlmud.domain.model.WeaponType;

public record Requirements(

    int level,

    Set<ClassType> reqClass,

    Set<WeaponType> reqWeapon,

    String reqState

) {
}
