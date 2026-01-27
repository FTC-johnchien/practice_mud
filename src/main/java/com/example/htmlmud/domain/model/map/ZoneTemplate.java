package com.example.htmlmud.domain.model.map;

import java.util.List;
import com.example.htmlmud.domain.model.RoomFlag;

// 1. 區域 (Zone) - 靜態地圖檔的根物件
public record ZoneTemplate(

    String id, // e.g., "area_newbie"

    String name, // e.g., "新手村"

    int minLevel,

    int maxLevel,

    int respawnTime, // 怪物重生間隔 (秒)

    List<String> authors, // 作者與可修改者

    List<String> flags // e.g., ["SAFE", "OUTDOORS"]

) {
  public ZoneTemplate {
    if (minLevel == 0) {
      minLevel = 1;
    }
    if (maxLevel == 0) {
      maxLevel = 100;
    }
    if (respawnTime == 0) {
      respawnTime = 300; // 5分鐘
    }
    if (flags == null || flags.isEmpty()) {
      flags = List.of(RoomFlag.OUTDOORS.name(), RoomFlag.NO_PVP.name());
    }
  }
}
