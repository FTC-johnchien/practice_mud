package com.example.htmlmud.domain.model.map;

import java.util.List;
import java.util.Map;
import com.example.htmlmud.domain.model.CombatConfig;
import com.example.htmlmud.domain.model.EquipmentSlot;
import com.example.htmlmud.domain.model.ResourceType;

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
