package com.example.htmlmud.domain.model.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.infra.factory.MessageFactory;
import com.example.htmlmud.protocol.MudMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data // Lombok
@NoArgsConstructor
@AllArgsConstructor
public class GameItem {

  // 1. 唯一識別 (UUID)
  // 每個物品都有獨立 ID，方便追蹤 (例如防止複製 Bug)
  private String id;

  private ItemTemplate template;

  private String name;
  private String description;

  private List<String> aliases = new ArrayList<>();

  // 主類型 (Enum)
  private ItemType type;
  // 子類型 (String)
  // 為什麼用 String？因為不同 Type 的 SubType Enum 不一樣，
  // 用 String 在 JSON 裡最通用，讀進來後再用 switch/helper 處理。
  private String subType;

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

  private boolean isDirty = false;

  // 輔助方法：獲取顯示名稱 (包含強化等級)
  // e.g., "鐵劍 (+5)"
  public String getDisplayName() {
    if (level > 1) {
      return name + " (+" + level + ")";
    }
    return name;
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

  /**
   * 取得此物品對應的技能類別 (給 SkillManager 用)
   */
  public SkillCategory getWeaponSkillCategory() {
    // log.info("type:{} subType:{}", this.type, this.subType);
    if (this.type != ItemType.WEAPON)
      return SkillCategory.UNARMED;

    try {
      // 將 "SWORD" 字串轉為 SkillCategory.SWORD
      return SkillCategory.valueOf(this.subType);
    } catch (IllegalArgumentException e) {
      return SkillCategory.UNARMED; // 預設/容錯
    }
  }

  /**
   * 取得此物品對應的裝備欄位 (給 EquipmentService 用)
   */
  public EquipmentSlot getEquipmentSlot() {
    if (this.type == ItemType.WEAPON)
      return EquipmentSlot.MAIN_HAND;
    if (this.type == ItemType.ARMOR) {
      // 將 "HEAD" 轉為 EquipmentSlot.HEAD
      return EquipmentSlot.valueOf(this.subType);
    }
    return null; // 不可裝備
  }

  public MudMessage<?> lookAtMe() {
    switch (this.type) {
      case ItemType.CORPSE:
        log.info("lookAtMe corpse");
        return MessageFactory.corpseDetail(this);
      case ItemType.CONTAINER:
        log.info("lookAtMe CONTAINER");
        return MessageFactory.containerDetail(this);
      default:
        log.info("lookAtMe type:{}", this.type);
        return MessageFactory.itemDetail(this);
    }
  }
}
