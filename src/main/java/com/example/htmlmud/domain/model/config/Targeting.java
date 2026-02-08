package com.example.htmlmud.domain.model.config;

import com.example.htmlmud.domain.model.enums.TargetType;

public record Targeting(

    TargetType type, // 目標類型: SELF, ENEMY(預設), ALLY, AREA_ENEMY, AREA_ALL

    int range, // 射程: 0=近戰(預設), 1=同一房間, >1=遠程

    int aoeRadius, // 範圍半徑 (若 type 為 AREA)

    int maxTargets, // 最大影響人數 (AOE 上限)

    boolean mustHaveTarget, // 是否必須選中目標才能施放 (如: 治療術可對空施放補自己)

    boolean lineOfSight // 是否需要視線 (不能隔牆打)

) {
}
