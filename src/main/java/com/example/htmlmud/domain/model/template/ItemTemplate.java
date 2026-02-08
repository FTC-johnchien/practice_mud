package com.example.htmlmud.domain.model.template;

import java.util.List;
import java.util.Map;
import com.example.htmlmud.domain.model.config.ConsumableProp;
import com.example.htmlmud.domain.model.config.EquipmentProp;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

// 5. 物品/裝備重置 (ItemReset)
@Builder(toBuilder = true)
public record ItemTemplate(

    String id,

    String name, // "鐵劍"

    List<String> aliases,

    String description, // "一把普通的鐵劍。"

    // ItemLocation location, // "inventory"(背包), "weapon"(手), "body"(身)...

    ItemType type,

    String subType, // "SWORD",

    @JsonProperty("stats") EquipmentProp equipmentProp, // 装備屬性

    @JsonProperty("behavior") ConsumableProp consumableProp, // 消秏品屬性

    String quality, // 品質 common,

    int value, // 價值

    int level, // 等級

    boolean isStackable, // 是否可堆疊

    // 屬性加成 (例如：力量+5)
    Map<String, Integer> bonusStats,

    Map<String, Object> extraProps // 其他靜態屬性
) {

  public boolean isStackable() {
    return isStackable;
  }
}
