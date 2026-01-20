package com.example.htmlmud.domain.model.map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

// 4. 怪物重生設定 (MobReset)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Population(

    @JsonProperty("mobId") String mobTemplateId, // 怪物原型 ID

    int count, // 該房間上限幾隻

    String roomId, // 房間 ID //

    @JsonProperty("respawnChance") double respawnChance // 重生機率 (0.0 - 1.0)

) {
}
