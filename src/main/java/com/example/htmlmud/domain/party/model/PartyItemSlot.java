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
}
