package com.example.htmlmud.domain.party.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import lombok.Data;

@Data
public class PartyInventory {
  public static final int DEFAULT_CAPACITY = 30;

  private int capacity = DEFAULT_CAPACITY;
  private List<PartyItemSlot> slots = new ArrayList<>();

  public PartyInventory() {
    this(DEFAULT_CAPACITY);
  }

  public PartyInventory(int capacity) {
    this.capacity = capacity;
    // 初始預設攜帶一些應急靈藥
    addItem("taiyin_pill", 3);
    addItem("purify_talisman", 2);
  }

  public synchronized boolean addItem(String templateId) {
    return addItem(templateId, 1);
  }

  public synchronized boolean addItem(String templateId, int count) {
    if (templateId == null || count <= 0) return false;

    // 1. 若可堆疊，嘗試尋找現有槽位
    for (PartyItemSlot slot : slots) {
      if (templateId.equals(slot.getItemId()) && slot.isStackable()) {
        slot.setCount(slot.getCount() + count);
        return true;
      }
    }

    // 2. 新增槽位，檢查容量
    if (slots.size() >= capacity) {
      return false;
    }

    PartyItemSlot newSlot = createFromTemplate(templateId, count);
    if (newSlot != null) {
      slots.add(newSlot);
      return true;
    }
    return false;
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

  public static PartyItemSlot createFromTemplate(String templateId, int count) {
    String slotId = "slot-" + UUID.randomUUID().toString().substring(0, 8);
    var opt = TemplateRepository.findItem(templateId);

    if (opt.isPresent()) {
      ItemTemplate t = opt.get();
      String icon = "📦";
      String effectType = null;
      int effectValue = 0;
      int minDmg = 0;
      int maxDmg = 0;
      int def = 0;
      int hp = 0;
      int san = 0;

      EquipmentSlot equipSlot = null;
      if (t.equipmentProp() != null && t.equipmentProp().slot() != null) {
        equipSlot = t.equipmentProp().slot();
      } else if (t.type() == ItemType.WEAPON) {
        equipSlot = EquipmentSlot.MAIN_HAND;
      } else if (t.type() == ItemType.SHIELD) {
        equipSlot = EquipmentSlot.OFF_HAND;
      } else if (t.type() == ItemType.ACCESSORY) {
        equipSlot = EquipmentSlot.ACCESSORY_1;
      } else if (t.type() == ItemType.ARMOR) {
        equipSlot = EquipmentSlot.fromString(t.subType());
      }

      if (t.type() == ItemType.CONSUMABLE) {
        if ("POTION".equals(t.subType())) {
          icon = "🧪";
          effectType = "HEAL_HP";
          effectValue = 50;
        } else if ("TALISMAN".equals(t.subType())) {
          icon = "📜";
          effectType = "RESTORE_SAN";
          effectValue = 25;
        }
      } else if (equipSlot != null) {
        icon = switch (equipSlot) {
          case MAIN_HAND -> "🗡️";
          case OFF_HAND -> "🛡️";
          case HEAD -> "👑";
          case BODY -> "🥋";
          case FEET -> "👢";
          case ACCESSORY_1, ACCESSORY_2 -> "💍";
        };

        if (t.equipmentProp() != null) {
          minDmg = t.equipmentProp().minDamage();
          maxDmg = t.equipmentProp().maxDamage();
          def = t.equipmentProp().defense();
        } else if (t.type() == ItemType.WEAPON) {
          minDmg = 12;
          maxDmg = 20;
        } else if (t.type() == ItemType.ARMOR) {
          def = 6;
        }

        if (t.bonusStats() != null) {
          hp += t.bonusStats().getOrDefault("MAX_HP", t.bonusStats().getOrDefault("hp", 0));
          san += t.bonusStats().getOrDefault("MAX_SAN", t.bonusStats().getOrDefault("san", 0));
          def += t.bonusStats().getOrDefault("DEFENSE", t.bonusStats().getOrDefault("def", 0));
          minDmg += t.bonusStats().getOrDefault("MIN_DAMAGE", 0);
          maxDmg += t.bonusStats().getOrDefault("MAX_DAMAGE", 0);
        }
      } else if (t.type() == ItemType.KEY_ITEM) {
        icon = "🗝️";
      }

      return PartyItemSlot.builder()
          .slotId(slotId)
          .itemId(t.id())
          .name(t.name())
          .icon(icon)
          .itemType(t.type())
          .subType(t.subType())
          .equipSlot(equipSlot)
          .count(count)
          .description(t.description())
          .quality(t.quality() != null ? t.quality() : "COMMON")
          .effectType(effectType)
          .effectValue(effectValue)
          .bonusMinDamage(minDmg)
          .bonusMaxDamage(maxDmg)
          .bonusDefense(def)
          .bonusHp(hp)
          .bonusSan(san)
          .build();
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
}
