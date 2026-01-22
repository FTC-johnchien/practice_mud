package com.example.htmlmud.domain.model;

import java.util.HashMap;
import java.util.Map;
import com.example.htmlmud.domain.model.map.ItemTemplate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data // Lombok
@NoArgsConstructor
@AllArgsConstructor
public class GameItem {

  // 1. 唯一識別 (UUID)
  // 每個物品都有獨立 ID，方便追蹤 (例如防止複製 Bug)
  private String id;

  @Getter
  private ItemTemplate template;

  // 動態數據 (會變的)
  private int level;
  private int currentDurability; // 耐久性
  private int amount; // 堆疊數量 (如果是藥水/錢幣)

  // 隨機數值/詞綴 (Affixes)
  // 例如：{ "attack_bonus": 5, "crafter": "玩家A" }
  private Map<String, Object> dynamicProps = new HashMap<>();

  // 輔助方法：獲取顯示名稱 (包含強化等級)
  // e.g., "鐵劍 (+5)"
  public String getDisplayName(ItemTemplate tpl) {
    if (level > 1) {
      return tpl.name() + " (+" + level + ")";
    }
    return tpl.name();
  }

  // 業務邏輯直接寫在 POJO 裡
  public void decreaseDurability(int amount) {
    this.currentDurability -= amount;
    if (this.currentDurability < 0)
      this.currentDurability = 0;
  }
}
