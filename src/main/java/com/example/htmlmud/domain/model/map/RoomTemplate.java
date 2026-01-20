package com.example.htmlmud.domain.model.map;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

// 2. 房間 (Room)
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomTemplate(

    String id, // e.g., square

    String name, // e.g., "中央廣場"

    @JsonProperty("description") String description, // 描述

    List<String> flags, // e.g., ["SAFE", "OUTDOORS"]

    Map<String, RoomExit> exits, // Key: 方向 (north, east), Value: 出口詳細資訊

    List<String> items, // [ "newbie_village:village_bread" ]

    @JsonProperty("data") Map<String, Object> data // 額外擴充資料 (e.g. 腳本觸發參數)

) {
}
