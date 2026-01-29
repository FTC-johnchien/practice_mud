package com.example.htmlmud.domain.model.map;

import java.util.List;
import java.util.Set;
import com.example.htmlmud.domain.model.Costs;
import com.example.htmlmud.domain.model.Mechanics;
import com.example.htmlmud.domain.model.Messages;
import com.example.htmlmud.domain.model.School;
import com.example.htmlmud.domain.model.SkillType;
import com.example.htmlmud.domain.model.Targeting;
import com.example.htmlmud.domain.model.Timing;

public record SkillTemplate(

    // 基礎資訊
    String id,

    String name,

    String description,

    SkillType type, // UNARMED, WEAPON, MAGIC, PASSIVE

    School school,

    Set<String> tags,

    boolean isHidden,


    // 施展條件
    Requirements requirements,
    // 消耗與成本
    Costs costs,
    // 時間與冷卻
    Timing timing,
    // 目標與範圍
    Targeting targeting,
    // 效果與數值
    Mechanics mechanics,

    List<CombatAction> actions,

    // 訊息描述
    Messages messages

) {
}
