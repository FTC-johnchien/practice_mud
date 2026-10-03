package com.example.htmlmud.domain.model.definition;

import java.util.List;
import java.util.Map;
import com.example.htmlmud.domain.model.config.BuffConfig;
import com.example.htmlmud.domain.model.config.Costs;
import com.example.htmlmud.domain.model.config.DefaultConfig;
import com.example.htmlmud.domain.model.config.LearningConfig;
import com.example.htmlmud.domain.model.config.Mechanics;
import com.example.htmlmud.domain.model.config.MoveAction;
import com.example.htmlmud.domain.model.config.ScalingConfig;
import com.example.htmlmud.domain.model.config.SynergiesConfig;
import com.example.htmlmud.domain.model.config.UsageConfig;
import lombok.Builder;

/**
 * MUD 世界/個人模式之戰鬥與招式規則切面
 */
@Builder(toBuilder = true)
public record MudSkillRules(
    LearningConfig learning,
    UsageConfig usage,
    Costs costs,
    ScalingConfig scaling,
    Mechanics mechanics,
    List<SynergiesConfig> synergies,
    List<MoveAction> moves,
    DefaultConfig comboDefaults,
    List<MoveAction> combo,
    DefaultConfig counterDefaults,
    List<MoveAction> counter,
    Map<String, Object> messages,
    BuffConfig buff
) {}
