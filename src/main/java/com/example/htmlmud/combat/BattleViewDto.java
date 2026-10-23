package com.example.htmlmud.combat;

import java.util.List;

public record BattleViewDto(
    boolean inBattle,
    String battleState,
    List<BattleEnemyViewDto> enemies,
    int selectedTargetIndex,
    List<String> logs
) {}
