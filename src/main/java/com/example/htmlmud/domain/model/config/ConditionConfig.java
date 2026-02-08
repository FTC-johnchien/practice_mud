package com.example.htmlmud.domain.model.config;

import java.util.Map;
import com.example.htmlmud.domain.model.enums.SkillCategory;

public record ConditionConfig(

    Map<SkillCategory, String> enabledSkill,

    Map<SkillCategory, String> enabledSkillTag

) {

}
