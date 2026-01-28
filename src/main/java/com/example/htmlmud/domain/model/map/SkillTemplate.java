package com.example.htmlmud.domain.model.map;

import java.util.List;
import com.example.htmlmud.domain.model.SkillType;

public record SkillTemplate(

    String id,

    String name,

    SkillType type, // UNARMED, WEAPON, MAGIC, PASSIVE

    List<CombatAction> actions

) {
}
