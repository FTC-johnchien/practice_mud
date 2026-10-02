package com.example.htmlmud.domain.model.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 道具可變執行期實例 (Canonical Item Instance)
 * 僅持有運行期動態數據（耐久度、強化等級、隨機詞綴、堆疊數量等），不複製靜態模板。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemInstance {

  private String instanceId;
  private String definitionId;
  @Builder.Default
  private int quantity = 1;
  @Builder.Default
  private int currentDurability = 100;
  @Builder.Default
  private int maxDurability = 100;
  @Builder.Default
  private int level = 0;

  @Builder.Default
  private Map<String, Object> dynamicProps = new HashMap<>();

  @Builder.Default
  private List<ItemInstance> contents = new ArrayList<>();

  /**
   * 扣除耐久度
   * @return true 若已損壞歸零
   */
  public boolean decreaseDurability(int amount) {
    this.currentDurability -= amount;
    if (this.currentDurability <= 0) {
      this.currentDurability = 0;
      return true;
    }
    return false;
  }
}
