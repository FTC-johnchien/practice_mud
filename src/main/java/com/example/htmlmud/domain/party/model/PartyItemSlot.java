package com.example.htmlmud.domain.party.model;

import java.util.HashMap;
import java.util.Map;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PartyItemSlot {
  private String slotId;
  private String itemId;
  private String name;
  @Builder.Default
  private String icon = "📦";
  private ItemType itemType;
  private String subType;
  private EquipmentSlot equipSlot;
  @Builder.Default
  private int count = 1;
  @Builder.Default
  private int maxStack = 99;
  private String description;
  @Builder.Default
  private String quality = "COMMON";

  // 消耗品或遺物效果
  private String effectType; // HEAL_HP, RESTORE_SAN, RESTORE_MP, LEARN_SKILL
  private int effectValue;

  // 技能道種專屬 (若是異變隊員遺物)
  private String grantedSkillId;
  private String grantedSkillName;

  // 裝備屬性加成
  @Builder.Default
  private int bonusMinDamage = 0;
  @Builder.Default
  private int bonusMaxDamage = 0;
  @Builder.Default
  private int bonusDefense = 0;
  @Builder.Default
  private int bonusHp = 0;
  @Builder.Default
  private int bonusSan = 0;

  public boolean isConsumable() {
    return itemType == ItemType.CONSUMABLE || "SKILL_CORE".equals(subType);
  }

  public boolean isEquipment() {
    return isWeapon() || isArmor() || isShield() || isAccessory() || equipSlot != null;
  }

  public boolean isWeapon() {
    return itemType == ItemType.WEAPON || equipSlot == EquipmentSlot.MAIN_HAND;
  }

  public boolean isShield() {
    return itemType == ItemType.SHIELD || equipSlot == EquipmentSlot.OFF_HAND;
  }

  public boolean isAccessory() {
    return itemType == ItemType.ACCESSORY || (equipSlot != null && equipSlot.isAccessory());
  }

  public boolean isArmor() {
    return itemType == ItemType.ARMOR
        || (equipSlot != null && (equipSlot == EquipmentSlot.BODY || equipSlot == EquipmentSlot.HEAD || equipSlot == EquipmentSlot.FEET));
  }

  public boolean isStackable() {
    return itemType == ItemType.CONSUMABLE || itemType == ItemType.MATERIAL;
  }

  public static PartyItemSlot fromItemTemplate(com.example.htmlmud.domain.model.template.ItemTemplate t) {
    return fromItemTemplate(t, 1, null);
  }

  public static PartyItemSlot fromItemTemplate(com.example.htmlmud.domain.model.template.ItemTemplate t, int count) {
    return fromItemTemplate(t, count, null);
  }

  public static PartyItemSlot fromItemTemplate(com.example.htmlmud.domain.model.template.ItemTemplate t, int count, String slotId) {
    if (t == null) return null;
    if (slotId == null || slotId.isBlank()) {
      slotId = "slot-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }
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
      } else if ("FOOD".equals(t.subType())) {
        icon = "🍞";
        effectType = "HEAL_HP";
        effectValue = 20;
      } else {
        icon = "💊";
        effectType = "HEAL_HP";
        effectValue = 30;
      }

      if (t.consumableProp() != null) {
        if (t.consumableProp().effect() != null) {
          String effStr = t.consumableProp().effect().toUpperCase();
          if (effStr.contains("SAN")) {
            effectType = "RESTORE_SAN";
          } else if (effStr.contains("HEAL") || effStr.contains("HP")) {
            effectType = "HEAL_HP";
          } else if (effStr.contains("MP")) {
            effectType = "RESTORE_MP";
          }
        }
        if (t.consumableProp().value() > 0) {
          effectValue = t.consumableProp().value();
        }
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

  public static PartyItemSlot fromGameItem(com.example.htmlmud.domain.model.entity.GameItem item) {
    if (item == null) return null;
    if (item.getTemplate() != null) {
      PartyItemSlot slot = fromItemTemplate(item.getTemplate(), item.getAmount() > 0 ? item.getAmount() : 1, item.getId());
      if (item.getName() != null && !item.getName().isBlank()) slot.setName(item.getName());
      if (item.getDescription() != null && !item.getDescription().isBlank()) slot.setDescription(item.getDescription());
      return slot;
    }
    return PartyItemSlot.builder()
        .slotId(item.getId() != null ? item.getId() : "slot-" + java.util.UUID.randomUUID().toString().substring(0, 8))
        .itemId(item.getId() != null ? item.getId() : "item")
        .name(item.getDisplayName())
        .icon("📦")
        .itemType(item.getType() != null ? item.getType() : ItemType.MISC)
        .subType(item.getSubType())
        .description(item.getDescription() != null ? item.getDescription() : item.getDisplayName())
        .count(item.getAmount() > 0 ? item.getAmount() : 1)
        .maxStack(99)
        .quality("COMMON")
        .build();
  }

  public com.example.htmlmud.domain.model.entity.GameItem toGameItem() {
    com.example.htmlmud.domain.model.entity.GameItem item = new com.example.htmlmud.domain.model.entity.GameItem();
    item.setId(this.slotId != null ? this.slotId : java.util.UUID.randomUUID().toString());
    item.setName(this.name);
    item.setDescription(this.description);
    item.setType(this.itemType);
    item.setSubType(this.subType);
    item.setAmount(this.count);
    var tpl = com.example.htmlmud.infra.persistence.repository.TemplateRepository.findItem(this.itemId);
    tpl.ifPresent(item::setTemplate);
    return item;
  }
}
