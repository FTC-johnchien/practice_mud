package com.example.htmlmud.domain.model.map;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

// 2. 房間 (Room)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomTemplate(

    int id, // e.g., 1001

    String name, // e.g., "村莊廣場"

    @JsonProperty("desc") String description, // 描述

    List<String> flags, // e.g., ["SAFE", "OUTDOORS"]

    Map<String, RoomExit> exits, // Key: 方向 (north, east), Value: 出口詳細資訊

    @JsonProperty("data") Map<String, Object> data // 額外擴充資料 (e.g. 腳本觸發參數)

) {
}
