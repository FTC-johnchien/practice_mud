package com.example.htmlmud.domain.model.map;

import java.util.Map;

// 5. 物品/裝備重置 (ItemReset)
public record ItemTemplate(

    int id,

    String name, // "鐵劍"

    String description, // "一把普通的鐵劍。"

    String slot, // "inventory"(背包), "weapon"(手), "body"(身)...

    int maxDurability, // 最大耐久 100

    int baseAttack, // 基礎攻擊 10

    int baseDefense, // 基礎防禦 10

    double chance, // 0.0 - 1.0 (機率)

    Map<String, Object> extraProps // 其他靜態屬性
) {
}
