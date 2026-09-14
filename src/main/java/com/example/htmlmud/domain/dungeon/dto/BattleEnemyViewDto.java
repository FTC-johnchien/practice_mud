package com.example.htmlmud.domain.dungeon.dto;

public record BattleEnemyViewDto(
    int index,
    String id,
    String name,
    int hp,
    int maxHp,
    String row,
    boolean alive,
    boolean isTarget,
    boolean isStunned
) {}
