package com.example.htmlmud.domain.model.definition;

import java.util.List;
import java.util.Map;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import lombok.Builder;

/**
 * 道具不可變規範模型 (Canonical Item Definition)
 * 作為單一事實來源 (Single Source of Truth)，集中解析靜態模板數值、部位與圖標。
 */
@Builder(toBuilder = true)
public record ItemDefinition(
    String id,
    String name,
    List<String> aliases,
    String description,
    ItemType type,
    String subType,
    EquipmentSlot equipSlot,
    String quality,
    int value,
    int level,
    boolean isStackable,
    int maxStack,
    String icon,
    String effectType,
    int effectValue,
    int bonusMinDamage,
    int bonusMaxDamage,
    int bonusDefense,
    int bonusHp,
    int bonusSan,
    Map<String, Integer> bonusStats,
    Map<String, Object> extraProps
) {

  public boolean isConsumable() {
    return type == ItemType.CONSUMABLE;
  }

  public boolean isWeapon() {
    return type == ItemType.WEAPON || equipSlot == EquipmentSlot.MAIN_HAND;
  }

  public boolean isArmor() {
    return type == ItemType.ARMOR
        || (equipSlot != null && (equipSlot == EquipmentSlot.BODY || equipSlot == EquipmentSlot.HEAD || equipSlot == EquipmentSlot.FEET));
  }

  public boolean isShield() {
    return equipSlot == EquipmentSlot.OFF_HAND;
  }

  public boolean isAccessory() {
    return equipSlot == EquipmentSlot.ACCESSORY_1 || equipSlot == EquipmentSlot.ACCESSORY_2;
  }

  public boolean isEquipment() {
    return equipSlot != null || isWeapon() || isArmor() || isShield() || isAccessory();
  }

  /**
   * 由 ItemTemplate 解析為標準不可變 ItemDefinition
   */
  public static ItemDefinition fromTemplate(ItemTemplate t) {
    if (t == null) return null;

    EquipmentSlot equipSlot = null;
    if (t.equipmentProp() != null && t.equipmentProp().slot() != null) {
      equipSlot = t.equipmentProp().slot();
    } else if (t.type() == ItemType.WEAPON) {
      equipSlot = EquipmentSlot.MAIN_HAND;
    } else if (t.type() == ItemType.ARMOR) {
      equipSlot = EquipmentSlot.BODY;
    }

    String icon = "📦";
    String effectType = null;
    int effectValue = 0;
    int minDmg = 0;
    int maxDmg = 0;
    int def = 0;
    int hp = 0;
    int san = 0;

    if (t.type() == ItemType.CONSUMABLE) {
      icon = "🧪";
      if (t.subType() != null && t.subType().equalsIgnoreCase("PILL")) {
        icon = "💊";
      }
      if (t.consumableProp() != null) {
        if (t.consumableProp().effect() != null) {
          effectType = t.consumableProp().effect();
        }
        if (t.consumableProp().value() > 0) {
          effectValue = t.consumableProp().value();
        }
      }
    } else if (equipSlot != null) {
      icon = switch (equipSlot) {
        case MAIN_HAND -> "🗡️";
        case OFF_HAND -> "🛡️";
        case HEAD -> "🧢";
        case BODY -> "🥋";
        case FEET -> "👢";
        case ACCESSORY_1, ACCESSORY_2 -> "📿";
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

    boolean stackable = t.isStackable();
    if (t.type() == ItemType.CONSUMABLE || t.type() == ItemType.MATERIAL) {
      stackable = true;
    }

    return ItemDefinition.builder()
        .id(t.id())
        .name(t.name())
        .aliases(t.aliases() != null ? t.aliases() : List.of())
        .description(t.description())
        .type(t.type())
        .subType(t.subType())
        .equipSlot(equipSlot)
        .quality(t.quality() != null ? t.quality() : "COMMON")
        .value(t.value())
        .level(t.level())
        .isStackable(stackable)
        .maxStack(stackable ? 99 : 1)
        .icon(icon)
        .effectType(effectType)
        .effectValue(effectValue)
        .bonusMinDamage(minDmg)
        .bonusMaxDamage(maxDmg)
        .bonusDefense(def)
        .bonusHp(hp)
        .bonusSan(san)
        .bonusStats(t.bonusStats() != null ? t.bonusStats() : Map.of())
        .extraProps(t.extraProps() != null ? t.extraProps() : Map.of())
        .build();
  }
}
