package com.example.htmlmud.domain.model.map;

import com.fasterxml.jackson.annotation.JsonProperty;

// 5. 物品/裝備重置 (ItemReset)
public record ItemReset(@JsonProperty("item_id") String itemTemplateId, String slot, // "inventory"(背包),
                                                                                     // "weapon"(手),
                                                                                     // "body"(身)...
    double chance // 0.0 - 1.0 (機率)
) {
}
