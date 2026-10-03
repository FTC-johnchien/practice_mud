package com.example.htmlmud.domain.model.definition;

import java.util.List;
import com.example.htmlmud.domain.model.config.BuffConfig;
import com.example.htmlmud.domain.party.model.CombatResourceType;
import lombok.Builder;

/**
 * DRPG 回合小隊戰術規則切面
 */
@Builder(toBuilder = true)
public record DrpgSkillRules(
    CombatResourceType costType,
    int costValue,
    int spCost,
    int mpCost,
    int hpCost,
    long cooldownMs,
    double damageMultiplier,
    boolean aoe,
    boolean heal,
    boolean taunt,
    boolean stun,
    int stunDurationSeconds,
    int healAmount,
    int sanRestore,
    boolean shield,
    boolean buff,
    boolean defense,
    BuffConfig buffConfig,
    long gcdMs,
    boolean triggersGcd,
    long castTimeMs,
    boolean interruptible,
    int threatBonus,
    boolean synergy,
    List<String> requiredClasses,
    int minPartyAlive,
    String requiredFormation,
    int formationEnergyCost
) {}
