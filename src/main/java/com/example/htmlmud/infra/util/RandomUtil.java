package com.example.htmlmud.infra.util;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.model.config.Weighted;

@Component
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

    // 1. 計算總權重
    int totalWeight = 0;
    for (T item : items) {
      int w = item.getWeight();
      if (w > 0) {
        totalWeight += w;
      }
    }

    if (totalWeight <= 0) {
      return null; // 所有選項權重都是 0，無法抽選
    }

    // 2. 取得隨機數 (0 <= r < totalWeight)
    // 使用 ThreadLocalRandom 效能比 Random 好，且執行緒安全
    int r = ThreadLocalRandom.current().nextInt(totalWeight);

    // 3. 尋找對應的項目 (累減法)
    for (T item : items) {
      int w = item.getWeight();
      if (w <= 0)
        continue; // 跳過權重為 0 的項目

      r -= w;
      if (r < 0) {
        return item; // 找到中獎者
      }
    }

    // 理論上程式不該跑到這裡，但為了安全起見回傳最後一個非零項或 null
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
