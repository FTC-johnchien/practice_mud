package com.example.htmlmud.domain.dungeon.dto;

public record PartySkillViewDto(
    String id,
    String name,
    String icon,
    String description,
    String costType,
    int costValue,
    String costDescription,
    long cooldownMs,
    long remainingCooldownMs,
    boolean available
) {}
