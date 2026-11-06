package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.item.ItemAffix;
import com.example.htmlmud.item.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.party.PartyItemSlot;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyMemberSkill;
import com.example.htmlmud.party.PartyMemberSkill.ComboRole;
import com.example.htmlmud.party.PartyMemberSkill.OriginSchool;
import com.example.htmlmud.party.PartyService;
import com.example.htmlmud.combat.BattleContext;
import com.example.htmlmud.combat.BattleEnemy;
import com.example.htmlmud.combat.BattleState;
import com.example.htmlmud.combat.ComboResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
class CyberSkillAndItemAffixTest {

  @Autowired
  private PartyService partyService;

  @Autowired
  private com.example.htmlmud.template.TemplateRepository templateRepository;

  @Test
  @DisplayName("測試賽博修仙技能模型：四大淵源流派、超頻階級與 Primer/Detonator 連攜角色判定")
  void testCyberSkillModelAndSchools() {
    PartyMemberSkill hackSkill = PartyMemberSkill.builder()
        .id("skill_time_tier1_step")
        .name("神經超頻步")
        .school(OriginSchool.HIGH_TECH)
        .overclockTier(1)
        .comboRole(ComboRole.PRIMER)
        .poiseDamage(25)
        .damageMultiplier(1.4)
        .cyberLore("特工晶片向運動神經束注入瞬時超頻電流。")
        .build();

    assertThat(hackSkill.getSchool()).isEqualTo(OriginSchool.HIGH_TECH);
    assertThat(hackSkill.getOverclockTier()).isEqualTo(1);
    assertThat(hackSkill.isPrimer()).isTrue();
    assertThat(hackSkill.isDetonator()).isFalse();
    assertThat(hackSkill.getPoiseDamage()).isEqualTo(25);
    assertThat(hackSkill.getCyberLore()).contains("特工晶片");

    PartyMemberSkill bioSkill = PartyMemberSkill.builder()
        .id("skill_chitin_tier1_blade")
        .name("強殖角質刃")
        .school(OriginSchool.CTHULHU)
        .overclockTier(2)
        .comboRole(ComboRole.DETONATOR)
        .poiseDamage(45)
        .damageMultiplier(2.2)
        .build();

    assertThat(bioSkill.getSchool()).isEqualTo(OriginSchool.CTHULHU);
    assertThat(bioSkill.getOverclockTier()).isEqualTo(2);
    assertThat(bioSkill.isPrimer()).isFalse();
    assertThat(bioSkill.isDetonator()).isTrue();
    assertThat(bioSkill.getDamageMultiplier()).isEqualTo(2.2);
  }

  @Test
  @DisplayName("測試裝備隨機詞綴池 (ItemAffix)：前綴後綴名稱組合與動態屬性疊加結算")
  void testItemAffixesAndDynamicBonusCalculation() {
    // 構造一把基礎青銅古劍
    PartyItemSlot sword = PartyItemSlot.builder()
        .slotId("slot-test-sword")
        .itemId("bronze_sword")
        .name("青銅古劍")
        .equipSlot(EquipmentSlot.MAIN_HAND)
        .itemType(ItemType.WEAPON)
        .bonusMinDamage(10)
        .bonusMaxDamage(15)
        .bonusDefense(2)
        .poiseBreakPower(20)
        .build();

    assertThat(sword.getDisplayNameWithAffixes()).isEqualTo("青銅古劍");
    assertThat(sword.getTotalBonusMinDamage()).isEqualTo(10);
    assertThat(sword.getTotalBonusMaxDamage()).isEqualTo(15);
    assertThat(sword.getTotalBonusDefense()).isEqualTo(2);
    assertThat(sword.getTotalPoiseBreakPower()).isEqualTo(20);

    // 附加前綴：超頻透鏡之 (+5 物理攻擊)
    ItemAffix prefix = ItemAffix.builder()
        .id("affix_overclock_prefix")
        .type(ItemAffix.AffixType.PREFIX)
        .name("超頻透鏡之")
        .statKey("physicalAttack")
        .value(5.0)
        .build();
    sword.addAffix(prefix);

    // 附加後綴：干擾頻譜 (+10 削韌強度, +3 護甲)
    ItemAffix suffixPoise = ItemAffix.builder()
        .id("affix_jam_suffix")
        .type(ItemAffix.AffixType.SUFFIX)
        .name("干擾頻譜")
        .statKey("poiseBreakPower")
        .value(10.0)
        .build();
    ItemAffix suffixArmor = ItemAffix.builder()
        .id("affix_armor_suffix")
        .type(ItemAffix.AffixType.SUFFIX)
        .name("之厚重")
        .statKey("armor")
        .value(3.0)
        .build();
    sword.addAffix(suffixPoise);
    sword.addAffix(suffixArmor);

    // 驗證動態合成名稱包含前綴與後綴
    assertThat(sword.getDisplayNameWithAffixes()).contains("超頻透鏡之青銅古劍");
    assertThat(sword.getDisplayNameWithAffixes()).contains("干擾頻譜");

    // 驗證屬性動態累加
    assertThat(sword.getTotalBonusMinDamage()).isEqualTo(15); // 10 + 5
    assertThat(sword.getTotalBonusMaxDamage()).isEqualTo(20); // 15 + 5
    assertThat(sword.getTotalBonusDefense()).isEqualTo(5);    // 2 + 3
    assertThat(sword.getTotalPoiseBreakPower()).isEqualTo(30); // 20 + 10

    // 附加生命與道心詞綴
    ItemAffix prefixHp = ItemAffix.builder()
        .id("affix_hp_prefix")
        .type(ItemAffix.AffixType.PREFIX)
        .name("長生之")
        .statKey("bonusHp")
        .value(40.0)
        .build();
    ItemAffix suffixSan = ItemAffix.builder()
        .id("affix_san_suffix")
        .type(ItemAffix.AffixType.SUFFIX)
        .name("·定心")
        .statKey("bonusSan")
        .value(15.0)
        .build();
    sword.addAffix(prefixHp);
    sword.addAffix(suffixSan);

    assertThat(sword.getTotalBonusHp()).isEqualTo(40);
    assertThat(sword.getTotalBonusSan()).isEqualTo(15);

    // 驗證 toItemInstance() 完整保留 affixes 與 poiseBreakPower
    var inst = sword.toItemInstance();
    assertThat(inst.getDefinitionId()).isEqualTo("bronze_sword");
    assertThat(inst.getPoiseBreakPower()).isEqualTo(20);
    assertThat(inst.getAffixes()).hasSize(5);

    // 驗證穿戴至 PartyMember 後動態匯總裝備削韌加成與氣血/道心
    com.example.htmlmud.domain.model.entity.LivingStats memberStats = new com.example.htmlmud.domain.model.entity.LivingStats();
    memberStats.setMaxHp(100);
    memberStats.setHp(100);
    com.example.htmlmud.party.PartyMember member = com.example.htmlmud.party.PartyMember.builder()
        .id("test-member")
        .name("凌劍客")
        .stats(memberStats)
        .maxSan(100)
        .currentSan(100)
        .build();

    assertThat(member.getEquippedPoiseBreakBonus()).isEqualTo(0);

    member.equip(EquipmentSlot.MAIN_HAND, sword);
    assertThat(member.getEquippedPoiseBreakBonus()).isEqualTo(30);
    assertThat(member.getStats().getMaxHp()).isEqualTo(140); // 100 + 40
    assertThat(member.getStats().getHp()).isEqualTo(140);
    assertThat(member.getMaxSan()).isEqualTo(115); // 100 + 15
    assertThat(member.getCurrentSan()).isEqualTo(115);

    // 卸除裝備
    member.unequip(EquipmentSlot.MAIN_HAND);
    assertThat(member.getEquippedPoiseBreakBonus()).isEqualTo(0);
    assertThat(member.getStats().getMaxHp()).isEqualTo(100);
    assertThat(member.getMaxSan()).isEqualTo(100);

    // 驗證 Party.equipItemToMember 與 unequipItemFromMember 回饋訊息均正確顯示完整含詞綴名稱
    Party testParty = Party.builder()
        .partyName("測試陣容")
        .members(new java.util.ArrayList<>(List.of(member)))
        .inventory(new com.example.htmlmud.party.PartyInventory(40))
        .build();
    testParty.getInventory().addSlot(sword);

    String equipMsg = testParty.equipItemOnMember(sword.getSlotId(), 0);
    assertThat(equipMsg).contains("裝備了武器【超頻透鏡之長生之青銅古劍干擾頻譜之厚重·定心】");

    String unequipMsg = testParty.unequipItemFromMember("weapon", 0);
    assertThat(unequipMsg).contains("卸下了武器【超頻透鏡之長生之青銅古劍干擾頻譜之厚重·定心】");
  }

  @Test
  @DisplayName("ALIGN2-01: 測試戰鬥結算掉落裝備附加道韻詞綴後，卡牌與日誌正確顯示含詞綴全名")
  void testRewardServiceDropNameIncludesAffixes() {
    com.example.htmlmud.item.ItemFactory factory = new com.example.htmlmud.item.ItemFactory(templateRepository);
    com.example.htmlmud.item.ItemAffixService affixService = new com.example.htmlmud.item.ItemAffixService();
    PartyItemSlot slot = factory.createPartySlot("bronze_sword", 1);
    assertThat(slot).isNotNull();
    affixService.applyRandomAffixes(slot);
    // 斷言 slot 具備詞綴時，getDisplayNameWithAffixes 包含前後綴而 getName 為基礎名
    assertThat(slot.getName()).isEqualTo("鏽蝕青銅古劍");
    assertThat(slot.getDisplayNameWithAffixes()).isNotEqualTo("鏽蝕青銅古劍");
    assertThat(slot.getDisplayNameWithAffixes()).contains("鏽蝕青銅古劍");
  }

  @Test
  @DisplayName("ALIGN2-01: 測試真實 Jackson ObjectMapper 連續兩次序列化/反序列化 Round-trip，斷言名稱與詞綴零重複疊加且衍生 getter 不污染 JSON")
  void testJacksonSerializationIdempotencyAndNoAffixDuplication() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    PartyItemSlot sword = PartyItemSlot.builder()
        .slotId("slot_test_save")
        .itemId("frost_blade")
        .name("寒霜靈刃")
        .equipSlot(EquipmentSlot.MAIN_HAND)
        .itemType(ItemType.WEAPON)
        .bonusMinDamage(10)
        .bonusMaxDamage(20)
        .bonusDefense(5)
        .bonusHp(50)
        .bonusSan(15)
        .poiseBreakPower(20)
        .build();

    ItemAffix prefix = ItemAffix.builder()
        .id("pre_1")
        .type(ItemAffix.AffixType.PREFIX)
        .name("超頻之")
        .statKey("bonusHp")
        .value(30.0)
        .build();
    ItemAffix suffix = ItemAffix.builder()
        .id("suf_1")
        .type(ItemAffix.AffixType.SUFFIX)
        .name("·破陣")
        .statKey("poiseBreakPower")
        .value(10.0)
        .build();

    sword.addAffix(prefix);
    sword.addAffix(suffix);

    assertThat(sword.getName()).isEqualTo("寒霜靈刃");
    assertThat(sword.getDisplayNameWithAffixes()).isEqualTo("超頻之寒霜靈刃·破陣");
    assertThat(sword.getTotalBonusHp()).isEqualTo(80);
    assertThat(sword.getTotalPoiseBreakPower()).isEqualTo(30);

    // Round 1: 第一次序列化與反序列化
    String json1 = mapper.writeValueAsString(sword);
    PartyItemSlot loaded1 = mapper.readValue(json1, PartyItemSlot.class);

    assertThat(loaded1.getName()).isEqualTo("寒霜靈刃");
    assertThat(loaded1.getDisplayNameWithAffixes()).isEqualTo("超頻之寒霜靈刃·破陣");
    assertThat(loaded1.getAffixes()).hasSize(2);
    assertThat(loaded1.getTotalBonusHp()).isEqualTo(80);
    assertThat(loaded1.getTotalPoiseBreakPower()).isEqualTo(30);

    // Round 2: 第二次序列化與反序列化
    String json2 = mapper.writeValueAsString(loaded1);
    PartyItemSlot loaded2 = mapper.readValue(json2, PartyItemSlot.class);

    // 關鍵斷言：名稱絕不疊加成「超頻之超頻之寒霜靈刃·破陣·破陣」
    assertThat(loaded2.getName()).isEqualTo("寒霜靈刃");
    assertThat(loaded2.getDisplayNameWithAffixes()).isEqualTo("超頻之寒霜靈刃·破陣");
    assertThat(loaded2.getAffixes()).hasSize(2);
    assertThat(loaded2.getTotalBonusHp()).isEqualTo(80);
    assertThat(loaded2.getTotalPoiseBreakPower()).isEqualTo(30);

    // 斷言衍生計算 getter 均已標註 @JsonIgnore，未污染存檔 JSON
    JsonNode node = mapper.readTree(json2);
    assertThat(node.has("displayNameWithAffixes")).isFalse();
    assertThat(node.has("baseName")).isFalse();
    assertThat(node.has("totalBonusMinDamage")).isFalse();
    assertThat(node.has("totalBonusMaxDamage")).isFalse();
    assertThat(node.has("totalBonusDefense")).isFalse();
    assertThat(node.has("totalPoiseBreakPower")).isFalse();
    assertThat(node.has("totalBonusHp")).isFalse();
    assertThat(node.has("totalBonusSan")).isFalse();
    assertThat(node.has("consumable")).isFalse();
    assertThat(node.has("equipment")).isFalse();
    assertThat(node.has("weapon")).isFalse();
    assertThat(node.has("shield")).isFalse();
    assertThat(node.has("accessory")).isFalse();
    assertThat(node.has("armor")).isFalse();
    assertThat(node.has("stackable")).isFalse();
    assertThat(node.has("empty")).isFalse();
  }

  @Test
  @DisplayName("ALIGN2-01: 測試真實 ComboResolver 戰鬥結算流程：穿戴高 poiseBreakPower 裝備在多人合擊與 5 人陣天外終結技中正確結算削韌差值並擊破首領霸體")
  void testEquippedPoiseBreakBonusInCombatSettlement() {
    ComboResolver comboResolver = new ComboResolver();

    // 準備高削韌武器 (+40 削韌)
    PartyItemSlot heavyBreaker = PartyItemSlot.builder()
        .slotId("slot_breaker")
        .itemId("plasma_hammer")
        .name("等離子重錘")
        .equipSlot(EquipmentSlot.MAIN_HAND)
        .itemType(ItemType.WEAPON)
        .poiseBreakPower(25)
        .build();
    ItemAffix affixBreach = ItemAffix.builder()
        .id("affix_breach")
        .type(ItemAffix.AffixType.SUFFIX)
        .name("·破陣")
        .statKey("poiseBreakPower")
        .value(15.0)
        .build();
    heavyBreaker.addAffix(affixBreach);
    assertThat(heavyBreaker.getTotalPoiseBreakPower()).isEqualTo(40);

    // 測試 1: executeCombo 多人合擊戰鬥結算真實削韌差值
    PartyMemberSkill comboSkill = PartyMemberSkill.builder()
        .id("combo_blade_breaker")
        .name("刀劍合璧·裂空震")
        .damageMultiplier(2.0)
        .poiseDamage(25) // 基礎技能削韌 25
        .minPartyAlive(2)
        .build();

    // 對照組：未裝備削韌武器
    LivingStats s1 = new LivingStats();
    s1.setMaxHp(100); s1.setHp(100);
    LivingStats s2 = new LivingStats();
    s2.setMaxHp(100); s2.setHp(100);
    PartyMember m1NoEquip = PartyMember.builder().id("m1").name("弟子甲").stats(s1).alive(true).currentSp(50).build();
    PartyMember m2NoEquip = PartyMember.builder().id("m2").name("弟子乙").stats(s2).alive(true).currentSp(50).build();
    Party partyNoEquip = Party.builder()
        .partyName("對照隊")
        .members(new java.util.ArrayList<>(List.of(m1NoEquip, m2NoEquip)))
        .formationEnergy(60)
        .build();
    BattleEnemy bossNoEquip = BattleEnemy.builder()
        .id("boss_1").name("傀儡甲").hp(500).maxHp(500).poise(150).maxPoise(150).alive(true).build();
    BattleContext ctxNoEquip = BattleContext.builder()
        .party(partyNoEquip)
        .enemies(new java.util.ArrayList<>(List.of(bossNoEquip)))
        .state(BattleState.FIGHTING)
        .selectedTargetIndex(0)
        .build();

    boolean okNoEquip = comboResolver.executeCombo(null, ctxNoEquip, comboSkill, (p, msg) -> {});
    assertThat(okNoEquip).isTrue();
    // 受到傷害折減受擊削韌 40 點 + 技能合擊削韌 25 點 = 65 點，剩餘 85 點
    assertThat(bossNoEquip.getPoise()).isEqualTo(85);
    assertThat(bossNoEquip.isPoiseBroken()).isFalse();

    // 實驗組：弟子丙穿戴 +40 削韌神兵
    LivingStats s3 = new LivingStats();
    s3.setMaxHp(100); s3.setHp(100);
    LivingStats s4 = new LivingStats();
    s4.setMaxHp(100); s4.setHp(100);
    PartyMember m1WithEquip = PartyMember.builder().id("m3").name("弟子丙").stats(s3).alive(true).currentSp(50).build();
    m1WithEquip.equip(EquipmentSlot.MAIN_HAND, heavyBreaker);
    assertThat(m1WithEquip.getEquippedPoiseBreakBonus()).isEqualTo(40);
    PartyMember m2WithEquip = PartyMember.builder().id("m4").name("弟子丁").stats(s4).alive(true).currentSp(50).build();
    Party partyWithEquip = Party.builder()
        .partyName("實驗隊")
        .members(new java.util.ArrayList<>(List.of(m1WithEquip, m2WithEquip)))
        .formationEnergy(60)
        .build();
    BattleEnemy bossWithEquip = BattleEnemy.builder()
        .id("boss_2").name("傀儡乙").hp(500).maxHp(500).poise(150).maxPoise(150).alive(true).build();
    BattleContext ctxWithEquip = BattleContext.builder()
        .party(partyWithEquip)
        .enemies(new java.util.ArrayList<>(List.of(bossWithEquip)))
        .state(BattleState.FIGHTING)
        .selectedTargetIndex(0)
        .build();

    boolean okWithEquip = comboResolver.executeCombo(null, ctxWithEquip, comboSkill, (p, msg) -> {});
    assertThat(okWithEquip).isTrue();
    // 受到傷害受擊削韌 40 點 + 技能 25 點 + 裝備 40 點 = 105 點削韌，剩餘 45 點（兩者削韌差值剛好為裝備加成 40 點）
    assertThat(bossWithEquip.getPoise()).isEqualTo(45);
    assertThat(bossNoEquip.getPoise() - bossWithEquip.getPoise()).isEqualTo(40);

    // 測試 2: executeFormationFinisher 5 人陣法天外終結技真實戰鬥引擎削韌加成
    com.example.htmlmud.party.FormationTemplate ft = com.example.htmlmud.party.FormationTemplate.builder()
        .id("five_elements_matrix").name("五行大陣").requiredPartySize(5).build();
    partyWithEquip.setEquippedFormation(ft);

    // 補齊至 5 人
    for (int i = 5; i <= 7; i++) {
      LivingStats extraStats = new LivingStats();
      extraStats.setMaxHp(100); extraStats.setHp(100);
      partyWithEquip.getMembers().add(PartyMember.builder().id("m" + i).name("陣員" + i).stats(extraStats).alive(true).build());
    }

    ctxWithEquip.setResonancePoints(3);
    // 怪物剩餘 35 點罡氣，終結技造成 50 + 30 + finisher + 40 (裝備) > 120 點削韌
    boolean finisherExecuted = comboResolver.executeFormationFinisher(null, ctxWithEquip, m1WithEquip, (p, msg) -> {});
    assertThat(finisherExecuted).isTrue();
    assertThat(bossWithEquip.getPoise()).isEqualTo(0);
    assertThat(bossWithEquip.isPoiseBroken()).isTrue();
    assertThat(bossWithEquip.getPoiseBrokenTicksRemaining()).isEqualTo(14);
  }

  @Test
  @DisplayName("CONV-01-16-SKILLS-DATA: 驗證 16 大標準賽博神通資料矩陣均已成功載入且具備古典仙俠展示名稱、有效 17 樹 ID 與底層科技疊影")
  void testCanonical16CyberSkillsLoadedFromData() {
    String[] expectedSkillIds = {
        "skill_mitm_transfer",
        "skill_docker_sandbox",
        "skill_ddos_flooding",
        "skill_bruteforce_crack",
        "skill_hotpatch_firmware",
        "skill_time_tier1_step",
        "skill_neural_overclock_step",
        "skill_chitin_tier1_blade",
        "skill_bio_keratin_blade",
        "skill_shell_injection",
        "skill_immuno_override",
        "skill_glitch_texture",
        "skill_pixel_mosaic",
        "skill_deepfake_mirror",
        "skill_framerate_lock",
        "skill_null_pointer",
        "skill_hard_reboot",
        "skill_memory_leak",
        "skill_format_drive"
    };

    List<String> canonical17Trees = List.of(
        "tree_gold_sword", "tree_wood_life", "tree_water_frost", "tree_fire_flame", "tree_earth_shield",
        "tree_gravity", "tree_fracture", "tree_forcefield", "tree_plasma_core",
        "tree_time_overclock", "tree_cyber_warfare", "tree_optics_glitch", "tree_kinetic_rails",
        "tree_chitin_blade", "tree_blood_siphon", "tree_cosmic_madness", "tree_null_void"
    );

    for (String skillId : expectedSkillIds) {
      PartyMemberSkill skill = templateRepository.getPartySkill(skillId).orElse(null);
      assertThat(skill).as("技能 [%s] 應成功加載於 TemplateRepository", skillId).isNotNull();
      assertThat(skill.getName()).as("技能 [%s] 應具備古典仙俠展示名稱（包含【】）", skillId)
          .startsWith("【")
          .endsWith("】");
      assertThat(skill.getSchool()).as("技能 [%s] 應綁定四大淵源流派之一", skillId)
          .isNotEqualTo(OriginSchool.NONE);
      assertThat(skill.getTreeId()).as("技能 [%s] 的 treeId 必須非空且為 17 專精樹之一", skillId)
          .isNotBlank()
          .isIn(canonical17Trees);
      assertThat(skill.getCyberLore()).as("技能 [%s] 應具備底層科技疊影描述", skillId)
          .isNotBlank();
    }

    // 驗證連攜先手與引爆手角色定義
    assertThat(templateRepository.getPartySkill("skill_mitm_transfer").orElseThrow().isPrimer()).isTrue();
    assertThat(templateRepository.getPartySkill("skill_ddos_flooding").orElseThrow().isDetonator()).isTrue();
    assertThat(templateRepository.getPartySkill("skill_bruteforce_crack").orElseThrow().isDetonator()).isTrue();
    assertThat(templateRepository.getPartySkill("skill_null_pointer").orElseThrow().isDetonator()).isTrue();
    assertThat(templateRepository.getPartySkill("skill_memory_leak").orElseThrow().isPrimer()).isTrue();
    assertThat(templateRepository.getPartySkill("skill_format_drive").orElseThrow().isDetonator()).isTrue();

    // 驗證 Task ALIGN-04: skill_format_drive 與 skill_hard_reboot 歸屬 CTHULHU / tree_null_void
    PartyMemberSkill formatDrive = templateRepository.getPartySkill("skill_format_drive").orElseThrow();
    assertThat(formatDrive.getSchool()).isEqualTo(OriginSchool.CTHULHU);
    assertThat(formatDrive.getTreeId()).isEqualTo("tree_null_void");

    PartyMemberSkill hardReboot = templateRepository.getPartySkill("skill_hard_reboot").orElseThrow();
    assertThat(hardReboot.getSchool()).isEqualTo(OriginSchool.CTHULHU);
    assertThat(hardReboot.getTreeId()).isEqualTo("tree_null_void");
  }
}
