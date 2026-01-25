package com.example.htmlmud.domain.model;

// 定義單個掉落規則
public record LootEntry(
    String itemId,   // 物品 ID
    double chance,   // 機率 (0.0 ~ 1.0)
    int minAmount,   // 最少掉幾個
    int maxAmount    // 最多掉幾個
) {}
