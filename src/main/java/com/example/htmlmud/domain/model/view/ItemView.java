package com.example.htmlmud.domain.model.view;

import com.example.htmlmud.domain.model.definition.ItemDefinition;
import com.example.htmlmud.domain.model.entity.ItemInstance;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import lombok.Builder;

/**
 * 道具只讀呈現模型 (Canonical Item View)
 * 由 ItemDefinition 與 ItemInstance 聚合生成的純只讀數據傳輸視圖，防止執行期污染與並行競態。
 */
@Builder
public record ItemView(
    String slotId,
    String itemId,
    String name,
    String icon,
    ItemType itemType,
    String subType,
    EquipmentSlot equipSlot,
    int count,
    int maxStack,
    String description,
    String quality,
    String effectType,
    int effectValue,
    String grantedSkillId,
    String grantedSkillName,
    int bonusMinDamage,
    int bonusMaxDamage,
    int bonusDefense,
    int bonusHp,
    int bonusSan,
    boolean consumable,
    boolean weapon,
    boolean armor,
    boolean shield,
    boolean accessory,
    boolean equipment
) {
  public static ItemView of(ItemDefinition def, ItemInstance inst) {
    if (def == null && inst == null) return null;
    String id = inst != null ? inst.getInstanceId() : (def != null ? def.id() : "unknown");
    String defId = def != null ? def.id() : (inst != null ? inst.getDefinitionId() : "unknown");
    String name = (def != null) ? def.name() : defId;
    if (inst != null && inst.getLevel() > 0) {
      name = name + " (+" + inst.getLevel() + ")";
    }
    String quality = (inst != null && inst.getDynamicProps() != null && inst.getDynamicProps().containsKey("quality"))
        ? String.valueOf(inst.getDynamicProps().get("quality"))
        : (def != null ? def.quality() : "COMMON");

    int count = inst != null ? inst.getQuantity() : 1;
    String icon = def != null ? def.icon() : "📦";
    ItemType type = def != null ? def.type() : ItemType.MISC;
    String subType = def != null ? def.subType() : null;
    EquipmentSlot equipSlot = def != null ? def.equipSlot() : null;
    int maxStack = def != null ? def.maxStack() : 99;
    String desc = def != null ? def.description() : "";
    String effectType = def != null ? def.effectType() : null;
    int effectValue = def != null ? def.effectValue() : 0;
    int minDmg = def != null ? def.bonusMinDamage() : 0;
    int maxDmg = def != null ? def.bonusMaxDamage() : 0;
    int defVal = def != null ? def.bonusDefense() : 0;
    int hp = def != null ? def.bonusHp() : 0;
    int san = def != null ? def.bonusSan() : 0;

    // 檢查 dynamicProps 是否有額外詞綴加成
    if (inst != null && inst.getDynamicProps() != null) {
      if (inst.getDynamicProps().get("attack_bonus") instanceof Number n) {
        minDmg += n.intValue();
        maxDmg += n.intValue();
      }
    }

    boolean isConsumable = def != null && def.isConsumable();
    boolean isWeapon = def != null && def.isWeapon();
    boolean isArmor = def != null && def.isArmor();
    boolean isShield = def != null && def.isShield();
    boolean isAccessory = def != null && def.isAccessory();
    boolean isEquip = def != null && def.isEquipment();

    return ItemView.builder()
        .slotId(id)
        .itemId(defId)
        .name(name)
        .icon(icon)
        .itemType(type)
        .subType(subType)
        .equipSlot(equipSlot)
        .count(count)
        .maxStack(maxStack)
        .description(desc)
        .quality(quality)
        .effectType(effectType)
        .effectValue(effectValue)
        .bonusMinDamage(minDmg)
        .bonusMaxDamage(maxDmg)
        .bonusDefense(defVal)
        .bonusHp(hp)
        .bonusSan(san)
        .consumable(isConsumable)
        .weapon(isWeapon)
        .armor(isArmor)
        .shield(isShield)
        .accessory(isAccessory)
        .equipment(isEquip)
        .build();
  }
}
