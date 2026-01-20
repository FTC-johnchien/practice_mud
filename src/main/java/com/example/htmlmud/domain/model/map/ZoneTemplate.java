package com.example.htmlmud.domain.model.map;

import java.util.List;

// 1. 區域 (Zone) - 靜態地圖檔的根物件
public record ZoneTemplate(

    String id, // e.g., "area_newbie"

    String name, // e.g., "新手村"

    int minLevel,

    int maxLevel,

    int respawnRate, // 怪物重生間隔 (秒)

    List<String> authors, // 作者與可修改者

    List<String> flags // e.g., ["SAFE", "OUTDOORS"]

) {
}
