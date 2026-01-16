package com.example.htmlmud.domain.model.map;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

// 4. 怪物重生設定 (MobReset)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MobReset(@JsonProperty("mob_id") String mobTemplateId, // 怪物原型 ID
    @JsonProperty("room_id") int roomId, // 重生在哪個房間
    @JsonProperty("max_qty") int maxQty, // 該房間上限幾隻
    List<ItemReset> items // 攜帶物品/裝備
) {
}
