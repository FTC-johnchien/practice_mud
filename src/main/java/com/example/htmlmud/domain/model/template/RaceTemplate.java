package com.example.htmlmud.domain.model.template;

import java.util.List;
import java.util.Map;
import com.example.htmlmud.domain.model.config.CombatConfig;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ResourceType;

public record RaceTemplate(

    String id,

    String name,

    String description,

    Map<ResourceType, Double> stats,

    List<EquipmentSlot> bodyParts,

    CombatConfig combat,

    Map<String, Double> innateSkills,

    Map<String, Double> resistances

) {

}
