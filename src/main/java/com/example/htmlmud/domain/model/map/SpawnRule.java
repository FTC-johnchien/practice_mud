package com.example.htmlmud.domain.model.map;

import lombok.Builder;

// Mob/Item 重生設定 (Mob/Item Reset)
@Builder(toBuilder = true)
public record SpawnRule(

    String type, // MOB 或 ITEM

    String id, // 物件原型 mob/item TemplateId

    int count, // 每次spawn幾隻

    int maxCount, // 該房間上限幾隻

    int time, // 重生秒數 (-1 代表不重生，0 代表使用區域預設)

    double rate, // 重生機率 (0.01 - 1) 未設定代表 1

    boolean isHidden // 是否隱藏



) {
  public SpawnRule {
    if (count == 0) {
      count = 1;
    }
    if (maxCount == 0) {
      maxCount = count;
    }
    if (rate == 0) {
      rate = 1;
    }
  }
}
