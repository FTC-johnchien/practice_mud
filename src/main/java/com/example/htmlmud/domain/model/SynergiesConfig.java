package com.example.htmlmud.domain.model;

import java.util.List;

public record SynergiesConfig(

    String name,

    TriggerType triggerType,

    ConditionConfig condition,

    List<Effect> effects

) {

}
