package com.example.htmlmud.domain.model.map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

// Mob/Item 重生設定 (Mob/Item Reset)
@Builder(toBuilder = true)
public record SpawnRule(

    String id, // 怪物原型 mobTemplateId

    int count, // 該房間上限幾隻

    int respawnTime, // 重生秒數 (-1 代表不重生，0 代表使用區域預設)

    double respawnChance // 重生機率 (0.0 - 1.0)

) {
}
