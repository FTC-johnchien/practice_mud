package com.example.htmlmud.domain.util;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import com.example.htmlmud.domain.model.config.Weighted;

/**
 * 領域層隨機數與機率運算工具
 * 封裝權重隨機挑選、百分比判定、區間隨機數等演算法，無外部依賴。
 */
public class RandomUtil {

  /**
   * 從列表中依照權重隨機挑選一個物件
   *
   * @param items 實作了 Weighted 介面的物件列表 (可以是 Skill, Loot, Mob 等)
   * @param <T> 泛型型別
   * @return 被抽中的物件，如果列表為空或總權重為 0 則回傳 null
   */
  public static <T extends Weighted> T pickWeighted(Collection<T> items) {
    if (items == null || items.isEmpty()) {
      return null;
    }

    int totalWeight = 0;
    for (T item : items) {
      int w = item.getWeight();
      if (w > 0) {
        totalWeight += w;
      }
    }

    if (totalWeight <= 0) {
      return null;
    }

    int r = ThreadLocalRandom.current().nextInt(totalWeight);

    for (T item : items) {
      int w = item.getWeight();
      if (w <= 0) {
        continue;
      }

      r -= w;
      if (r < 0) {
        return item;
      }
    }

    return null;
  }

  /**
   * 簡單的百分比判斷 (例如 30% 機率觸發)
   *
   * @param chance 0~100 的整數
   */
  public static boolean percent(int chance) {
    return ThreadLocalRandom.current().nextInt(100) < chance;
  }

  public static long range(long min, long max) {
    return ThreadLocalRandom.current().nextLong(min, max);
  }

  public static int jitter(int value) {
    return ThreadLocalRandom.current().nextInt(-value, value + 1);
  }
}
