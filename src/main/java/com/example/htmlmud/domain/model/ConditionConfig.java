package com.example.htmlmud.domain.model;

import java.util.Map;

public record ConditionConfig(

    Map<SkillCategory, String> enabledSkill,

    Map<SkillCategory, String> enabledSkillTag

) {

}
