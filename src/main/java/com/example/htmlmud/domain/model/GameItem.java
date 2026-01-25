package com.example.htmlmud.domain.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.htmlmud.domain.model.map.ItemTemplate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data // Lombok
@NoArgsConstructor
@AllArgsConstructor
public class GameItem {

  // 1. 唯一識別 (UUID)
  // 每個物品都有獨立 ID，方便追蹤 (例如防止複製 Bug)
  @Getter
  private String id;

  @Getter
  private ItemTemplate template;

  private String name;
  private String description;
  private ItemType type;

  // 容器內容物 (如果這不是容器，則是 empty)
  // 注意：這裡直接存 GameItem 物件，因為屍體是暫時的，不需要存回 DB
  private List<GameItem> contents = new ArrayList<>();

  // 動態數據 (會變的)
  private int level;
  private int currentDurability; // 耐久性
  private int maxDurability; // 最大耐久性
  private int amount; // 堆疊數量 (如果是藥水/錢幣)

  // 隨機數值/詞綴 (Affixes)
  // 例如：{ "attack_bonus": 5, "crafter": "玩家A" }
  private Map<String, Object> dynamicProps = new HashMap<>();

  @Getter
  @Setter
  private boolean isDirty = false;

  // 輔助方法：獲取顯示名稱 (包含強化等級)
  // e.g., "鐵劍 (+5)"
  public String getDisplayName() {
    if (level > 1) {
      return template.name() + " (+" + level + ")";
    }
    return template.name();
  }

  // 方便的方法
  public void addContent(GameItem item) {
    contents.add(item);
  }

  public List<GameItem> getContents() {
    return contents;
  }

  // 扣耐久 (只在記憶體運算)
  public boolean decreaseDurability(int amount) {
    this.currentDurability -= amount;
    if (this.currentDurability <= 0) {
      this.currentDurability = 0;
      return true; // 壞掉了
    }
    return false; // 還沒壞
  }

  public boolean isStackable() {
    return template.isStackable();
  }
}
