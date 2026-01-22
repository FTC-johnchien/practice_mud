package com.example.htmlmud.domain.model.map;

import java.util.Map;
import com.example.htmlmud.domain.model.EquipmentSlot;
import com.example.htmlmud.domain.model.ConsumableProp;
import com.example.htmlmud.domain.model.EquipmentProp;
import com.example.htmlmud.domain.model.ItemLocation;
import com.example.htmlmud.domain.model.ItemType;
import com.fasterxml.jackson.annotation.JsonProperty;

// 5. 物品/裝備重置 (ItemReset)
public record ItemTemplate(

    String id,

    String name, // "鐵劍"

    @JsonProperty("keywords") String[] aliases,

    String description, // "一把普通的鐵劍。"

    // ItemLocation location, // "inventory"(背包), "weapon"(手), "body"(身)...

    ItemType type,

    EquipmentProp equipmentProp, // 装備屬性

    ConsumableProp consumableProp, // 消秏品屬性

    String quality, // 品質 common,

    int value, // 價值

    int level, // 等級

    // 屬性加成 (例如：力量+5)
    Map<String, Integer> bonusStats,

    Map<String, Object> extraProps // 其他靜態屬性

) {
}
