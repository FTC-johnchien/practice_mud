package com.example.htmlmud.domain.model.map;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

// 1. 區域 (Area) - 靜態地圖檔的根物件
public record Area(String id, // e.g., "area_newbie"
    String name, // e.g., "新手村"
    @JsonProperty("level_min") int levelMin, @JsonProperty("level_max") int levelMax,
    @JsonProperty("reset_interval") int resetInterval, // 重置時間 (分鐘)
    List<Room> rooms, // 區域內的所有房間
    @JsonProperty("mob_resets") List<MobReset> mobResets // 怪物重生配置
) {
}
