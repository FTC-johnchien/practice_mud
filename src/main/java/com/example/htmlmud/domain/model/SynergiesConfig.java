package com.example.htmlmud.domain.model;

import java.util.Set;

public record SynergiesConfig(

    String name,

    TriggerType triggerType,

    ConditionConfig condition,

    Set<Effect> effects

) {

}
