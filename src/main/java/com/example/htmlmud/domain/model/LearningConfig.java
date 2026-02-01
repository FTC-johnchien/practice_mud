package com.example.htmlmud.domain.model;

import java.util.Map;

public record LearningConfig(

    int reqLevel,

    Map<ResourceType, Integer> reqStats,

    String reqGuild,

    boolean loseOnLeave,

    Map<String, Integer> reqSkills,

    // 直接用 Map 接收，Key 是 Enum
    Map<ResourceType, Integer> costs

) {

}
