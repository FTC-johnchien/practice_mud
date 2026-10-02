package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.htmlmud.domain.factory.ItemFactory;
import com.example.htmlmud.domain.model.config.ConsumableProp;
import com.example.htmlmud.domain.model.config.EquipmentProp;
import com.example.htmlmud.domain.model.definition.ItemDefinition;
import com.example.htmlmud.domain.model.entity.ItemInstance;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.view.ItemView;
import com.example.htmlmud.domain.party.model.PartyInventory;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.service.TemplateCatalog;

public class CanonicalItemModelTest {

  @Test
  @DisplayName("CANON-01: ItemDefinition correctly parses weapon template into canonical definition")
  void testItemDefinitionFromWeaponTemplate() {
    ItemTemplate weaponTpl = ItemTemplate.builder()
        .id("snow:iron_sword")
        .name("精鐵長劍")
        .type(ItemType.WEAPON)
        .subType("SWORD")
        .equipmentProp(EquipmentProp.builder()
            .slot(EquipmentSlot.MAIN_HAND)
            .minDamage(15)
            .maxDamage(25)
            .maxDurability(100)
            .build())
        .bonusStats(Map.of("MIN_DAMAGE", 3, "MAX_DAMAGE", 5))
        .quality("UNCOMMON")
        .value(100)
        .build();

    ItemDefinition def = ItemDefinition.fromTemplate(weaponTpl);
    assertNotNull(def);
    assertEquals("snow:iron_sword", def.id());
    assertEquals("精鐵長劍", def.name());
    assertEquals(ItemType.WEAPON, def.type());
    assertEquals("SWORD", def.subType());
    assertEquals(EquipmentSlot.MAIN_HAND, def.equipSlot());
    assertEquals("🗡️", def.icon());
    assertTrue(def.isWeapon());
    assertTrue(def.isEquipment());
    assertFalse(def.isConsumable());
    // 15 + 3 = 18 min damage; 25 + 5 = 30 max damage
    assertEquals(18, def.bonusMinDamage());
    assertEquals(30, def.bonusMaxDamage());
  }

  @Test
  @DisplayName("CANON-01: ItemDefinition correctly parses consumable potion template")
  void testItemDefinitionFromConsumableTemplate() {
    ItemTemplate potionTpl = ItemTemplate.builder()
        .id("taiyin:pill")
        .name("太陰回春丹")
        .type(ItemType.CONSUMABLE)
        .subType("POTION")
        .consumableProp(new ConsumableProp("HEAL_HP", 50))
        .isStackable(true)
        .quality("RARE")
        .build();

    ItemDefinition def = potionTpl.toDefinition();
    assertNotNull(def);
    assertEquals("taiyin:pill", def.id());
    assertEquals("🧪", def.icon());
    assertEquals("HEAL_HP", def.effectType());
    assertEquals(50, def.effectValue());
    assertTrue(def.isConsumable());
    assertTrue(def.isStackable());
    assertEquals(99, def.maxStack());
  }

  @Test
  @DisplayName("CANON-01: ItemInstance tracks runtime state and durability")
  void testItemInstanceDurabilityAndLevel() {
    ItemInstance inst = ItemInstance.builder()
        .instanceId("inst-001")
        .definitionId("snow:iron_sword")
        .quantity(1)
        .currentDurability(50)
        .maxDurability(50)
        .level(3)
        .dynamicProps(Map.of("attack_bonus", 5))
        .build();

    assertEquals("inst-001", inst.getInstanceId());
    assertEquals("snow:iron_sword", inst.getDefinitionId());
    assertEquals(3, inst.getLevel());

    boolean broken = inst.decreaseDurability(30);
    assertFalse(broken);
    assertEquals(20, inst.getCurrentDurability());

    broken = inst.decreaseDurability(25);
    assertTrue(broken);
    assertEquals(0, inst.getCurrentDurability());
  }

  @Test
  @DisplayName("CANON-01: ItemView synthesizes definition and instance without leaking mutability")
  void testItemViewSynthesis() {
    ItemTemplate swordTpl = ItemTemplate.builder()
        .id("snow:iron_sword")
        .name("精鐵長劍")
        .type(ItemType.WEAPON)
        .subType("SWORD")
        .equipmentProp(EquipmentProp.builder()
            .slot(EquipmentSlot.MAIN_HAND)
            .minDamage(10)
            .maxDamage(20)
            .maxDurability(100)
            .build())
        .build();
    ItemDefinition def = swordTpl.toDefinition();

    ItemInstance inst = ItemInstance.builder()
        .instanceId("inst-sword-99")
        .definitionId("snow:iron_sword")
        .quantity(1)
        .level(2)
        .dynamicProps(Map.of("attack_bonus", 7, "quality", "EPIC"))
        .build();

    ItemView view = ItemView.of(def, inst);
    assertNotNull(view);
    assertEquals("inst-sword-99", view.slotId());
    assertEquals("snow:iron_sword", view.itemId());
    assertEquals("精鐵長劍 (+2)", view.name());
    assertEquals("EPIC", view.quality());
    // 10 base + 7 affix = 17 min damage; 20 base + 7 affix = 27 max damage
    assertEquals(17, view.bonusMinDamage());
    assertEquals(27, view.bonusMaxDamage());
    assertTrue(view.weapon());
    assertTrue(view.equipment());
  }

  @Test
  @DisplayName("CANON-01: PartyItemSlot delegates seamlessly to ItemDefinition and ItemView")
  void testPartyItemSlotDelegation() {
    ItemTemplate armorTpl = ItemTemplate.builder()
        .id("snow:iron_armor")
        .name("玄鐵戰鎧")
        .type(ItemType.ARMOR)
        .subType("BODY")
        .equipmentProp(EquipmentProp.builder()
            .slot(EquipmentSlot.BODY)
            .defense(12)
            .maxDurability(100)
            .build())
        .bonusStats(Map.of("MAX_HP", 50, "DEFENSE", 4))
        .build();

    PartyItemSlot slot = PartyItemSlot.fromItemTemplate(armorTpl, 1, "slot-armor-01");
    assertNotNull(slot);
    assertEquals("slot-armor-01", slot.getSlotId());
    assertEquals("snow:iron_armor", slot.getItemId());
    assertEquals("玄鐵戰鎧", slot.getName());
    assertEquals("🥋", slot.getIcon());
    assertEquals(16, slot.getBonusDefense()); // 12 + 4
    assertEquals(50, slot.getBonusHp());

    ItemView view = slot.toItemView();
    assertEquals("玄鐵戰鎧", view.name());
    assertEquals(16, view.bonusDefense());
    assertEquals(50, view.bonusHp());
  }

  @Test
  @DisplayName("CANON-01: ItemFactory creates Dao Core relics and standard instances")
  void testItemFactoryDaoCore() {
    ItemFactory factory = new ItemFactory(new TemplateCatalog());
    PartyItemSlot coreSlot = factory.createDaoCore("凌霜", "slash_storm", "太乙分光劍");
    assertNotNull(coreSlot);
    assertEquals("dao_core_slash_storm", coreSlot.getItemId());
    assertEquals("凝煞道核·【凌霜】", coreSlot.getName());
    assertEquals("🔮", coreSlot.getIcon());
    assertEquals("LEARN_SKILL", coreSlot.getEffectType());
    assertEquals("太乙分光劍", coreSlot.getGrantedSkillName());
  }

  @Test
  @DisplayName("CANON-01: PartyInventory handles stacking and unknown item fallback gracefully")
  void testPartyInventoryFallback() {
    PartyItemSlot fallbackSlot = PartyInventory.createFromTemplate("unknown_mystery_item", 2);
    assertNotNull(fallbackSlot);
    assertEquals("unknown_mystery_item", fallbackSlot.getItemId());
    assertTrue(fallbackSlot.getName().contains("unknown_mystery_item"));
    assertEquals(2, fallbackSlot.getCount());
  }
}
