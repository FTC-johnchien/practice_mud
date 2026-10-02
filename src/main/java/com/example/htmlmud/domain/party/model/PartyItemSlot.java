package com.example.htmlmud.domain.party.model;

import java.util.HashMap;
import java.util.Map;
import com.example.htmlmud.domain.model.definition.ItemDefinition;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.entity.ItemInstance;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.view.ItemView;
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

  public static PartyItemSlot fromDefinition(ItemDefinition d) {
    return fromDefinition(d, 1, null);
  }

  public static PartyItemSlot fromDefinition(ItemDefinition d, int count) {
    return fromDefinition(d, count, null);
  }

  public static PartyItemSlot fromDefinition(ItemDefinition d, int count, String slotId) {
    if (d == null) return null;
    if (slotId == null || slotId.isBlank()) {
      slotId = "slot-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }
    return PartyItemSlot.builder()
        .slotId(slotId)
        .itemId(d.id())
        .name(d.name())
        .icon(d.icon())
        .itemType(d.type())
        .subType(d.subType())
        .equipSlot(d.equipSlot())
        .count(count)
        .maxStack(d.maxStack())
        .description(d.description())
        .quality(d.quality() != null ? d.quality() : "COMMON")
        .effectType(d.effectType())
        .effectValue(d.effectValue())
        .bonusMinDamage(d.bonusMinDamage())
        .bonusMaxDamage(d.bonusMaxDamage())
        .bonusDefense(d.bonusDefense())
        .bonusHp(d.bonusHp())
        .bonusSan(d.bonusSan())
        .build();
  }

  public static PartyItemSlot fromItemTemplate(ItemTemplate t) {
    return fromItemTemplate(t, 1, null);
  }

  public static PartyItemSlot fromItemTemplate(ItemTemplate t, int count) {
    return fromItemTemplate(t, count, null);
  }

  public static PartyItemSlot fromItemTemplate(ItemTemplate t, int count, String slotId) {
    if (t == null) return null;
    return fromDefinition(ItemDefinition.fromTemplate(t), count, slotId);
  }

  public static PartyItemSlot createFallback(String templateId, int count) {
    String slotId = "slot-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    return PartyItemSlot.builder()
        .slotId(slotId)
        .itemId(templateId)
        .name("法寶遺物 (" + templateId + ")")
        .icon("📦")
        .itemType(ItemType.MISC)
        .count(count)
        .description("未明之仙道遺物。")
        .quality("COMMON")
        .build();
  }

  public static PartyItemSlot fromGameItem(GameItem item) {
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

  public GameItem toGameItem() {
    GameItem item = new GameItem();
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

  public ItemInstance toItemInstance() {
    return ItemInstance.builder()
        .instanceId(this.slotId != null ? this.slotId : java.util.UUID.randomUUID().toString())
        .definitionId(this.itemId)
        .quantity(this.count > 0 ? this.count : 1)
        .build();
  }

  public ItemView toItemView() {
    return ItemView.builder()
        .slotId(this.slotId)
        .itemId(this.itemId)
        .name(this.name)
        .icon(this.icon)
        .itemType(this.itemType)
        .subType(this.subType)
        .equipSlot(this.equipSlot)
        .count(this.count)
        .maxStack(this.maxStack)
        .description(this.description)
        .quality(this.quality)
        .effectType(this.effectType)
        .effectValue(this.effectValue)
        .grantedSkillId(this.grantedSkillId)
        .grantedSkillName(this.grantedSkillName)
        .bonusMinDamage(this.bonusMinDamage)
        .bonusMaxDamage(this.bonusMaxDamage)
        .bonusDefense(this.bonusDefense)
        .bonusHp(this.bonusHp)
        .bonusSan(this.bonusSan)
        .consumable(isConsumable())
        .weapon(isWeapon())
        .armor(isArmor())
        .shield(isShield())
        .accessory(isAccessory())
        .equipment(isEquipment())
        .build();
  }
}
