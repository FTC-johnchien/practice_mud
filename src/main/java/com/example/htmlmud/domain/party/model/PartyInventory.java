package com.example.htmlmud.domain.party.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.domain.service.TemplateCatalog;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class PartyInventory {
  public static final int DEFAULT_CAPACITY = 30;

  private int capacity = DEFAULT_CAPACITY;
  private List<PartyItemSlot> slots = new ArrayList<>();

  @JsonIgnore
  private transient TemplateReader templateReader;

  public PartyInventory() {
    this(DEFAULT_CAPACITY, new TemplateCatalog());
  }

  public PartyInventory(int capacity) {
    this(capacity, new TemplateCatalog());
  }

  public PartyInventory(int capacity, TemplateReader templateReader) {
    this.capacity = capacity;
    this.templateReader = templateReader;
    // 容器本身只負責格位與堆疊機制，不帶入任何硬編碼資料；初始物品由 PartyService/開局工廠統一派發
  }

  public static boolean isSameItemId(String id1, String id2) {
    if (id1 == null || id2 == null) return false;
    if (id1.equalsIgnoreCase(id2)) return true;
    String s1 = id1.contains(":") ? id1.substring(id1.indexOf(":") + 1) : id1;
    String s2 = id2.contains(":") ? id2.substring(id2.indexOf(":") + 1) : id2;
    return s1.equalsIgnoreCase(s2);
  }

  public synchronized boolean addItem(String templateId) {
    return addItem(templateId, 1);
  }

  public synchronized boolean addItem(String templateId, int count) {
    if (templateId == null || count <= 0) return false;

    int remaining = count;

    // 1. 若可堆疊，嘗試尋找現有未達上限的槽位 (支援 ID 跨前綴比對)
    for (PartyItemSlot slot : slots) {
      if (isSameItemId(templateId, slot.getItemId()) && slot.isStackable()) {
        int max = slot.getMaxStack() > 0 ? slot.getMaxStack() : 99;
        int space = max - slot.getCount();
        if (space > 0) {
          int toAdd = Math.min(space, remaining);
          slot.setCount(slot.getCount() + toAdd);
          remaining -= toAdd;
          if (remaining <= 0) {
            return true;
          }
        }
      }
    }

    // 2. 剩餘數量需要開闢新槽位，檢查容量
    while (remaining > 0) {
      if (slots.size() >= capacity) {
        return false;
      }
      PartyItemSlot newSlot = createFromTemplate(templateId, remaining, getTemplateReader());
      if (newSlot == null) {
        return false;
      }
      int max = newSlot.getMaxStack() > 0 ? newSlot.getMaxStack() : 99;
      if (!newSlot.isStackable() || remaining <= max) {
        newSlot.setCount(remaining);
        slots.add(newSlot);
        remaining = 0;
      } else {
        newSlot.setCount(max);
        slots.add(newSlot);
        remaining -= max;
      }
    }

    return true;
  }

  public synchronized boolean addSlot(PartyItemSlot slot) {
    if (slot == null || slots.size() >= capacity) return false;
    if (slot.getSlotId() == null) {
      slot.setSlotId("slot-" + UUID.randomUUID().toString().substring(0, 8));
    }
    slots.add(slot);
    return true;
  }

  public synchronized PartyItemSlot getItem(String slotId) {
    if (slotId == null) return null;
    return slots.stream().filter(s -> slotId.equals(s.getSlotId())).findFirst().orElse(null);
  }

  public synchronized boolean removeItem(String slotId, int count) {
    PartyItemSlot slot = getItem(slotId);
    if (slot == null) return false;

    if (slot.getCount() <= count) {
      slots.remove(slot);
    } else {
      slot.setCount(slot.getCount() - count);
    }
    return true;
  }

  public synchronized boolean addGameItem(com.example.htmlmud.domain.model.entity.GameItem item) {
    if (item == null) return false;
    if (item.getTemplate() != null && item.getTemplate().id() != null) {
      boolean added = addItem(item.getTemplate().id(), item.getAmount() > 0 ? item.getAmount() : 1);
      if (added) return true;
    }
    PartyItemSlot slot = PartyItemSlot.fromGameItem(item);
    return addSlot(slot);
  }

  public static PartyItemSlot createFromTemplate(String templateId, int count) {
    return createFromTemplate(templateId, count, new TemplateCatalog());
  }

  public static PartyItemSlot createFromTemplate(String templateId, int count,
      TemplateReader templateReader) {
    String slotId = "slot-" + UUID.randomUUID().toString().substring(0, 8);
    var opt = templateReader.findItem(templateId);

    if (opt.isPresent()) {
      return PartyItemSlot.fromItemTemplate(opt.get(), count, slotId);
    }

    // 內建 fallback 物品
    if (templateId.contains("pill")) {
      return PartyItemSlot.builder().slotId(slotId).itemId(templateId).name("太陰培元丹").icon("🧪").itemType(ItemType.CONSUMABLE).subType("POTION").count(count).description("太陰靈丹，服之可調和陰陽，回復 50 HP。").quality("UNCOMMON").effectType("HEAL_HP").effectValue(50).build();
    } else if (templateId.contains("talisman")) {
      return PartyItemSlot.builder().slotId(slotId).itemId(templateId).name("辟邪清心符").icon("📜").itemType(ItemType.CONSUMABLE).subType("TALISMAN").count(count).description("平復心魔與雜念，恢復 25 SAN，可解走火入魔。").quality("RARE").effectType("RESTORE_SAN").effectValue(25).build();
    } else if (templateId.contains("sword")) {
      return PartyItemSlot.builder().slotId(slotId).itemId(templateId).name("鏽蝕青銅古劍").icon("🗡️").itemType(ItemType.WEAPON).subType("SWORD").equipSlot(com.example.htmlmud.domain.model.enums.EquipmentSlot.MAIN_HAND).count(1).description("古墓出土古劍，攻擊力 +12~20。").quality("UNCOMMON").bonusMinDamage(12).bonusMaxDamage(20).build();
    } else if (templateId.contains("robe")) {
      return PartyItemSlot.builder().slotId(slotId).itemId(templateId).name("陰煞道袍").icon("🥋").itemType(ItemType.ARMOR).subType("CHEST").equipSlot(com.example.htmlmud.domain.model.enums.EquipmentSlot.BODY).count(1).description("玄絲道袍，防禦 +6，生命上限 +30。").quality("RARE").bonusDefense(6).bonusHp(30).build();
    } else {
      return PartyItemSlot.builder().slotId(slotId).itemId(templateId).name("古仙法物").icon("🔮").itemType(ItemType.MISC).count(count).description("古塚中掘出之神秘法物。").quality("COMMON").build();
    }
  }

  private TemplateReader getTemplateReader() {
    if (templateReader == null) {
      templateReader = new TemplateCatalog();
    }
    return templateReader;
  }
}
