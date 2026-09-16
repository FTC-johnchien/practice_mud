package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;

@SpringBootTest
class PartyEquipmentIntegrationTest {

  private PartyService partyService;

  @BeforeEach
  void setUp() {
    partyService = new PartyService();
    partyService.initDefaultFormations();
  }

  @Test
  @DisplayName("驗證 EquipmentSlot 枚舉收斂與 JSON 舊部位反序列化相容性")
  void testEquipmentSlotBackwardsCompatibility() {
    // 7 大核心部位正常解析
    assertThat(EquipmentSlot.fromString("MAIN_HAND")).isEqualTo(EquipmentSlot.MAIN_HAND);
    assertThat(EquipmentSlot.fromString("OFF_HAND")).isEqualTo(EquipmentSlot.OFF_HAND);
    assertThat(EquipmentSlot.fromString("HEAD")).isEqualTo(EquipmentSlot.HEAD);
    assertThat(EquipmentSlot.fromString("BODY")).isEqualTo(EquipmentSlot.BODY);
    assertThat(EquipmentSlot.fromString("FEET")).isEqualTo(EquipmentSlot.FEET);
    assertThat(EquipmentSlot.fromString("ACCESSORY_1")).isEqualTo(EquipmentSlot.ACCESSORY_1);
    assertThat(EquipmentSlot.fromString("ACCESSORY_2")).isEqualTo(EquipmentSlot.ACCESSORY_2);

    // 歷史 MUD 冗餘部位平滑映射至對應黃金部位
    assertThat(EquipmentSlot.fromString("CHEST")).isEqualTo(EquipmentSlot.BODY);
    assertThat(EquipmentSlot.fromString("BACK")).isEqualTo(EquipmentSlot.BODY);
    assertThat(EquipmentSlot.fromString("SHOULDERS")).isEqualTo(EquipmentSlot.BODY);
    assertThat(EquipmentSlot.fromString("ARMS")).isEqualTo(EquipmentSlot.BODY);
    assertThat(EquipmentSlot.fromString("HANDS")).isEqualTo(EquipmentSlot.BODY);
    assertThat(EquipmentSlot.fromString("LEGS")).isEqualTo(EquipmentSlot.FEET);
    assertThat(EquipmentSlot.fromString("FINGER")).isEqualTo(EquipmentSlot.ACCESSORY_1);
    assertThat(EquipmentSlot.fromString("NECK")).isEqualTo(EquipmentSlot.ACCESSORY_1);
    assertThat(EquipmentSlot.fromString("TRINKET")).isEqualTo(EquipmentSlot.ACCESSORY_1);
    assertThat(EquipmentSlot.fromString("WINGS")).isEqualTo(EquipmentSlot.BODY);

    assertThat(EquipmentSlot.ACCESSORY_1.isAccessory()).isTrue();
    assertThat(EquipmentSlot.ACCESSORY_2.isAccessory()).isTrue();
    assertThat(EquipmentSlot.MAIN_HAND.isAccessory()).isFalse();
  }

  @Test
  @DisplayName("測試從 MUD 模板庫讀取裝備並穿戴至小隊成員：屬性動態累加與舊裝備退回行囊")
  void testMudItemTemplateEquipAndAttributeAggregation() {
    Party party = partyService.getOrCreateParty("equipment_tester");
    PartyMember member = party.getMembers().get(0);

    int initialMinDmg = member.getEffectiveMinDamage();
    int initialMaxDmg = member.getEffectiveMaxDamage();
    int initialDef = member.getEffectiveDefense();
    int initialMaxHp = member.getStats().getMaxHp();
    int initialMaxSan = member.getMaxSan();

    // 1. 從 MUD 模板載入青銅古劍 (WEAPON / MAIN_HAND)
    boolean swordAdded = party.getInventory().addItem("bronze_sword", 1);
    assertThat(swordAdded).isTrue();
    PartyItemSlot swordSlot = party.getInventory().getSlots().stream()
        .filter(s -> s.getItemId() != null && s.getItemId().endsWith("bronze_sword"))
        .findFirst().orElseThrow();
    assertThat(swordSlot.getEquipSlot()).isEqualTo(EquipmentSlot.MAIN_HAND);
    assertThat(swordSlot.getBonusMinDamage()).isEqualTo(14);
    assertThat(swordSlot.getBonusMaxDamage()).isEqualTo(22);

    // 穿戴青銅劍
    String swordEquipMsg = party.equipItemOnMember(swordSlot.getSlotId(), 0);
    assertThat(swordEquipMsg).contains("裝備了武器【鏽蝕青銅古劍】");
    assertThat(member.getEquipment().get(EquipmentSlot.MAIN_HAND)).isNotNull();
    assertThat(member.getEffectiveMinDamage()).isEqualTo(initialMinDmg + 14);
    assertThat(member.getEffectiveMaxDamage()).isEqualTo(initialMaxDmg + 22);

    // 2. 從 MUD 模板載入陰煞道袍 (ARMOR / CHEST -> BODY, bonusStats: MAX_HP=30, MAX_SAN=10, defense=6)
    boolean robeAdded = party.getInventory().addItem("yin_robe", 1);
    assertThat(robeAdded).isTrue();
    PartyItemSlot robeSlot = party.getInventory().getSlots().stream()
        .filter(s -> s.getItemId() != null && s.getItemId().endsWith("yin_robe"))
        .findFirst().orElseThrow();
    assertThat(robeSlot.getEquipSlot()).isEqualTo(EquipmentSlot.BODY);
    assertThat(robeSlot.getBonusDefense()).isEqualTo(6);
    assertThat(robeSlot.getBonusHp()).isEqualTo(30);
    assertThat(robeSlot.getBonusSan()).isEqualTo(10);

    // 穿戴道袍
    String robeEquipMsg = party.equipItemOnMember(robeSlot.getSlotId(), 0);
    assertThat(robeEquipMsg).contains("穿戴了防具【陰煞道袍】");
    assertThat(member.getEquipment().get(EquipmentSlot.BODY)).isNotNull();
    assertThat(member.getEffectiveDefense()).isEqualTo(initialDef + 6);
    assertThat(member.getStats().getMaxHp()).isEqualTo(initialMaxHp + 30);
    assertThat(member.getMaxSan()).isEqualTo(initialMaxSan + 10);

    // 3. 測試法寶飾品自動落入空槽 (ACCESSORY_1 -> ACCESSORY_2)
    PartyItemSlot ring1 = PartyItemSlot.builder()
        .slotId("slot-ring-1")
        .itemId("jade_ring")
        .name("寒玉戒")
        .itemType(ItemType.ACCESSORY)
        .equipSlot(EquipmentSlot.ACCESSORY_1)
        .bonusDefense(2)
        .bonusSan(15)
        .build();
    PartyItemSlot ring2 = PartyItemSlot.builder()
        .slotId("slot-ring-2")
        .itemId("soul_pendant")
        .name("定魂珮")
        .itemType(ItemType.ACCESSORY)
        .equipSlot(EquipmentSlot.ACCESSORY_1)
        .bonusDefense(3)
        .bonusSan(20)
        .build();

    party.getInventory().addSlot(ring1);
    party.getInventory().addSlot(ring2);

    // 穿戴第一件飾品 -> 應進入 ACCESSORY_1
    party.equipItemOnMember(ring1.getSlotId(), 0);
    assertThat(member.getEquipment().get(EquipmentSlot.ACCESSORY_1)).isNotNull();
    assertThat(member.getEquipment().get(EquipmentSlot.ACCESSORY_1).getName()).isEqualTo("寒玉戒");

    // 穿戴第二件飾品 -> 智慧尋找空槽，應自動進入 ACCESSORY_2
    party.equipItemOnMember(ring2.getSlotId(), 0);
    assertThat(member.getEquipment().get(EquipmentSlot.ACCESSORY_2)).isNotNull();
    assertThat(member.getEquipment().get(EquipmentSlot.ACCESSORY_2).getName()).isEqualTo("定魂珮");

    // 驗證總防禦與 SAN 同時疊加了兩件飾品
    assertThat(member.getEffectiveDefense()).isEqualTo(initialDef + 6 + 2 + 3);
    assertThat(member.getMaxSan()).isEqualTo(initialMaxSan + 10 + 15 + 20);

    // 4. 測試換裝機制：穿戴新武器時，舊武器應自動退回背包
    PartyItemSlot newSword = PartyItemSlot.builder()
        .slotId("slot-new-sword")
        .itemId("iron_blade")
        .name("百煉玄鐵刀")
        .itemType(ItemType.WEAPON)
        .equipSlot(EquipmentSlot.MAIN_HAND)
        .bonusMinDamage(20)
        .bonusMaxDamage(30)
        .build();
    party.getInventory().addSlot(newSword);

    party.equipItemOnMember(newSword.getSlotId(), 0);
    assertThat(member.getEquipment().get(EquipmentSlot.MAIN_HAND).getName()).isEqualTo("百煉玄鐵刀");
    assertThat(member.getEffectiveMinDamage()).isEqualTo(initialMinDmg + 20);
    // 驗證青銅劍退回行囊
    assertThat(party.getInventory().getSlots().stream().anyMatch(s -> s.getItemId() != null && s.getItemId().endsWith("bronze_sword"))).isTrue();

    // 5. 測試卸裝機制：卸下飾品、防具與武器，各項數值正確回滾
    party.unequipItemFromMember("acc1", 0);
    assertThat(member.getEquipment().get(EquipmentSlot.ACCESSORY_1)).isNull();
    party.unequipItemFromMember("acc2", 0);
    assertThat(member.getEquipment().get(EquipmentSlot.ACCESSORY_2)).isNull();
    party.unequipItemFromMember("armor", 0);
    assertThat(member.getEquipment().get(EquipmentSlot.BODY)).isNull();
    party.unequipItemFromMember("weapon", 0);
    assertThat(member.getEquipment().get(EquipmentSlot.MAIN_HAND)).isNull();

    // 數值全數回歸初始基準
    assertThat(member.getEffectiveMinDamage()).isEqualTo(initialMinDmg);
    assertThat(member.getEffectiveMaxDamage()).isEqualTo(initialMaxDmg);
    assertThat(member.getEffectiveDefense()).isEqualTo(initialDef);
    assertThat(member.getStats().getMaxHp()).isEqualTo(initialMaxHp);
    assertThat(member.getMaxSan()).isEqualTo(initialMaxSan);
  }
}
