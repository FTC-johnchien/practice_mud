package com.example.htmlmud.domain.model.config;

public record Costs(

    int hp,

    int mp,

    int stamina,

    int charge, // 攻擊所累積的能量

    int ammo // 彈藥數量

) {
}
