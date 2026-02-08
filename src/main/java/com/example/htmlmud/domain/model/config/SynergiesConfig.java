package com.example.htmlmud.domain.model.config;

import java.util.List;
import com.example.htmlmud.domain.model.Effect;
import com.example.htmlmud.domain.model.enums.TriggerType;

public record SynergiesConfig(

    String name,

    TriggerType triggerType,

    ConditionConfig condition,

    List<Effect> effects

) {

}
