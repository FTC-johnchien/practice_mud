package com.example.htmlmud.domain.model.map;

public record CombatAction(

    String name, // 招式名稱 (可選)
    String verb, // 動作描述 (包含 $n:目標, $l:部位)
    double damageMod // 傷害倍率
) {
}
