package com.example.htmlmud.domain.model.map;

import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

// 2. 房間 (Room)
@Builder(toBuilder = true)
public record RoomTemplate(

    String id, // e.g., square

    String zoneId,

    String name, // e.g., "中央廣場"

    String description, // 描述

    List<String> flags, // e.g., ["SAFE", "OUTDOORS"]

    Map<String, RoomExit> exits, // Key: 方向 (north, east), Value: 出口詳細資訊

    @JsonProperty("population") Set<SpawnRule> mobSpawnRules,

    Set<SpawnRule> itemSpawnRules,

    @JsonProperty("extra_data") Map<String, Object> extraData // 額外擴充資料 (e.g. 腳本觸發參數)

) {
}
