package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyInventory;
import com.example.htmlmud.party.PartyItemSlot;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.PartyService;
import com.example.htmlmud.save.SaveGameService;
import com.example.htmlmud.template.TemplateCatalog;

import com.example.htmlmud.domain.model.enums.DamageType;
import com.example.htmlmud.combat.BattleEnemy;
import com.example.htmlmud.party.PartyMemberSkill;

@SpringBootTest
public class MechanismPurityAndBugfixTest {

  @Autowired
  private PartyService partyService;

  @Autowired
  private SaveGameService saveGameService;

  @Autowired
  private com.example.htmlmud.template.TemplateReader templateReader;

  @Test
  @DisplayName("機制驗證：PartyInventory 容器建構子必須純淨，無任何寫死初始道具")
  void testPartyInventoryConstructorHasNoSideEffects() {
    PartyInventory pureInv = new PartyInventory(10, templateReader);
    assertThat(pureInv.getSlots()).isEmpty();

    PartyInventory defaultInv = new PartyInventory(PartyInventory.DEFAULT_CAPACITY, templateReader);
    assertThat(defaultInv.getSlots()).isEmpty();
  }

  @Test
  @DisplayName("機制驗證：可堆疊物品超過 maxStack 時必須自動分槽，且不超出容量上限")
  void testPartyInventoryItemStackCapMechanism() {
    PartyInventory inv = new PartyInventory(5, templateReader);

    // taiyin_pill 為可堆疊消耗品，maxStack 預設為 99
    // 加入 150 顆藥丸：第一格應滿 99 顆，第二格應有 51 顆
    boolean added = inv.addItem("taiyin_pill", 150);
    assertThat(added).isTrue();
    assertThat(inv.getSlots()).hasSize(2);

    PartyItemSlot slot1 = inv.getSlots().get(0);
    PartyItemSlot slot2 = inv.getSlots().get(1);

    assertThat(slot1.getCount()).isEqualTo(slot1.getMaxStack());
    assertThat(slot2.getCount()).isEqualTo(150 - slot1.getMaxStack());

    // 填滿剩餘容量至 5 格
    inv.addItem("taiyin_pill", 99);
    inv.addItem("taiyin_pill", 99);
    inv.addItem("taiyin_pill", 99);
    assertThat(inv.getSlots()).hasSize(5);

    // 當容量已達上限 (5)，再次加入新堆疊時應返回 false
    boolean overflowAdded = inv.addItem("steel_blade", 1);
    assertThat(overflowAdded).isFalse();
    assertThat(inv.getSlots()).hasSize(5);
  }

  @Test
  @DisplayName("機制驗證：夥伴別名與職業特徵技能完全由 JSON 資料驅動載入")
  void testDataDrivenCompanionLoading() {
    // 透過 JSON 中的別名查詢 (例如 "鐵牛" 定義於 default_companions.json 之 aliases)
    PartyMember tieNiu = partyService.createCompanion("鐵牛");
    assertThat(tieNiu).isNotNull();
    assertThat(tieNiu.getId()).isEqualTo("m-tie_niu");

    // 職業技能是動態掃描 tags:["CLASS"] 與 school:"WARRIOR" 而載入，絕非 Java switch hardcode
    boolean hasTaunt = tieNiu.getSkills().stream()
        .anyMatch(s -> s.getId().equals("class_warrior_taunt"));
    assertThat(hasTaunt).isTrue();

    // 驗證主角別名
    PartyMember leader = partyService.createCompanion("主角");
    assertThat(leader).isNotNull();
    assertThat(leader.getId()).isEqualTo("m-leader");
  }

  @Test
  @DisplayName("機制驗證：SaveGameService 槽位邊界校驗與原子化寫入防禦")
  void testSaveGameAtomicWriteAndSlotValidation() {
    // 1. 邊界校驗：非法槽位 (<0 或 >5) 必須拒絕
    assertThatThrownBy(() -> saveGameService.saveGame("tester", -1, "Illegal Slot"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("無效的存檔槽位");

    assertThatThrownBy(() -> saveGameService.saveGame("tester", 99, "Illegal Slot"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("無效的存檔槽位");

    // 2. 正常存檔：寫入 slot 1，驗證成功落盤且不殘留 .tmp 暫存檔
    var saved = saveGameService.saveGame("tester", 1, "機制驗證存檔");
    assertThat(saved).isNotNull();

    File targetFile = new File("saves", "slot_1.json");
    File tempFile = new File("saves", "slot_1.json.tmp");

    assertThat(targetFile.exists()).isTrue();
    assertThat(tempFile.exists()).isFalse();

    // 3. 讀檔驗證
    var loaded = saveGameService.loadGame("tester", 1);
    assertThat(loaded.getTitle()).isEqualTo("機制驗證存檔");
  }

  @Test
  @DisplayName("機制驗證：同伴與怪物之罡氣破防值與九大靈氣抗性完全由 JSON 驅動載入")
  void testDataDrivenPoiseAndResistances() {
    // 1. 同伴鐵牛：驗證從 default_companions.json 載入之 poise 與 resistances
    PartyMember tieNiu = partyService.createCompanion("鐵牛");
    assertThat(tieNiu).isNotNull();
    assertThat(tieNiu.getPoise()).isEqualTo(180);
    assertThat(tieNiu.getMaxPoise()).isEqualTo(180);
    assertThat(tieNiu.getResistance(DamageType.PHYSICAL)).isEqualTo(0.15);
    assertThat(tieNiu.getResistance(DamageType.EARTH)).isEqualTo(0.2);

    // 2. 怪物腐化屍偶：驗證從 taiyin_tomb/mobs.json 載入之 poise 與 resistances
    var dollOpt = templateReader.findMob("taiyin_tomb:corpse_doll");
    assertThat(dollOpt).isPresent();
    var dollTpl = dollOpt.get();
    assertThat(dollTpl.poise()).isEqualTo(25);
    assertThat(dollTpl.resistances()).containsEntry("DARK", 0.3).containsEntry("FIRE", -0.25);

    BattleEnemy enemy = BattleEnemy.fromTemplate("e-1", dollTpl, com.example.htmlmud.party.RowPosition.FRONT, null, templateReader);
    assertThat(enemy.getPoise()).isEqualTo(25);
    assertThat(enemy.getMaxPoise()).isEqualTo(25);
    assertThat(enemy.getResistance(DamageType.DARK)).isEqualTo(0.3);
    assertThat(enemy.getResistance(DamageType.FIRE)).isEqualTo(-0.25);
  }

  @Test
  @DisplayName("機制驗證：PartyMemberSkill 升級縮放必須完整保留 poiseDamage、shield 與 tags (CLEANSE)")
  void testSkillScalingPreservesAllCombatAttributes() {
    var rawSkill = templateReader.findPartySkill("sword_pierce");
    assertThat(rawSkill).isPresent();
    PartyMemberSkill base = rawSkill.get();
    assertThat(base.getPoiseDamage()).isEqualTo(20);
    assertThat(base.getDamageType()).isEqualTo("SLASH");
    assertThat(base.getTags()).contains("METAL", "SWORD");

    base.setLevel(5);
    assertThat(base.getPoiseDamage()).isEqualTo(20);
    assertThat(base.getDamageType()).isEqualTo("SLASH");
    assertThat(base.getEffectiveDamageMultiplier()).isGreaterThan(base.getDamageMultiplier());
    assertThat(base.getTags()).contains("METAL", "SWORD");

    // 驗證淨化解控技能 CLEANSE 標籤保留
    var purifySkill = templateReader.findPartySkill("heal_all_purify");
    assertThat(purifySkill).isPresent();
    PartyMemberSkill purify = purifySkill.get();
    purify.setLevel(3);
    assertThat(purify.getTags()).contains("CLEANSE");
  }

  @Test
  @DisplayName("機制驗證：LivingStats.deepCopy() 必須防禦性深拷貝所有數值、抗性、裝備與技能集合 (DEEPCOPY-01)")
  void testLivingStatsDeepCopyPreservesAllAttributes() {
    com.example.htmlmud.domain.model.entity.LivingStats original = new com.example.htmlmud.domain.model.entity.LivingStats();
    original.setLevel(10);
    original.setHp(500);
    original.setMaxHp(500);
    original.setStr(50);
    original.setResistance(DamageType.FIRE, 0.4);
    original.setResistance(DamageType.DARK, 0.25);
    original.modifyCombatResource("charge", 3);

    com.example.htmlmud.domain.model.entity.GameItem testItem = new com.example.htmlmud.domain.model.entity.GameItem();
    testItem.setId("item-test");
    original.equipment.put(com.example.htmlmud.item.EquipmentSlot.MAIN_HAND, testItem);
    original.enabledSkills.put(com.example.htmlmud.progression.SkillCategory.SWORD, "taiji_sword");
    original.learnedSkills.put("taiji_sword", new com.example.htmlmud.progression.SkillEntry("taiji_sword", 5));

    com.example.htmlmud.domain.model.entity.LivingStats copy = original.deepCopy();

    // 驗證基本屬性
    assertThat(copy.getLevel()).isEqualTo(10);
    assertThat(copy.getHp()).isEqualTo(500);
    assertThat(copy.getStr()).isEqualTo(50);

    // 驗證集合與抗性完整拷貝
    assertThat(copy.getResistance(DamageType.FIRE)).isEqualTo(0.4);
    assertThat(copy.getResistance(DamageType.DARK)).isEqualTo(0.25);
    assertThat(copy.getCombatResource("charge")).isEqualTo(3);
    assertThat(copy.equipment).containsEntry(com.example.htmlmud.item.EquipmentSlot.MAIN_HAND, testItem);
    assertThat(copy.enabledSkills).containsEntry(com.example.htmlmud.progression.SkillCategory.SWORD, "taiji_sword");
    assertThat(copy.learnedSkills).containsKey("taiji_sword");

    // 驗證獨立性 (防禦性拷貝，非共享同一 Map 參照)
    copy.setResistance(DamageType.FIRE, 0.9);
    assertThat(original.getResistance(DamageType.FIRE)).isEqualTo(0.4);

    copy.equipment.clear();
    assertThat(original.equipment).isNotEmpty();
  }

  @Test
  @DisplayName("環境與字節碼驗證：所有 com.example.htmlmud 下的 Class 必須能成功載入且無 ClassFormatError")
  void testAllClasspathClassesLoadSuccessfully() throws Exception {
    java.io.File classesDir = new java.io.File("target/classes/com/example/htmlmud");
    if (!classesDir.exists()) {
      return;
    }
    java.nio.file.Files.walk(classesDir.toPath())
        .filter(p -> p.toString().endsWith(".class"))
        .forEach(p -> {
          String relative = classesDir.toPath().relativize(p).toString();
          String className = "com.example.htmlmud." + relative.replace(java.io.File.separatorChar, '.').replace(".class", "");
          try {
            Class<?> clazz = Class.forName(className, true, getClass().getClassLoader());
            assertNotNull(clazz);
            clazz.getDeclaredConstructors();
            clazz.getDeclaredMethods();
          } catch (Throwable t) {
            fail("Failed to load/verify class: " + className + ", error: " + t.getMessage(), t);
          }
        });
  }
}
