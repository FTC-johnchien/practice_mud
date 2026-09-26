package com.example.htmlmud.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.model.enums.WeaponType;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;

@SpringBootTest
@ActiveProfiles("test")
public class SkillsTaxonomyAndWeaponBindingTest {

  @Autowired
  private PartyService partyService;

  @Test
  @DisplayName("驗證技能庫全域載入與九大武器基礎招式註冊完整")
  void testSkillsTaxonomyAndBasicWeapons() {
    // 1. 驗證全域技能總量充足且均已加載 (遷移後為 67 個)
    assertThat(TemplateRepository.getAllSkills().size()).isGreaterThanOrEqualTo(60);

    // 2. 驗證九大武器類別與空手之基礎招式皆存在
    List<String> expectedBasicSkills = List.of(
        "basic_sword", "basic_blade", "basic_polearm", "basic_blunt",
        "basic_dagger", "basic_archery", "basic_throwing", "basic_axe",
        "basic_whip", "basic_fist", "basic_dodge", "basic_parry", "basic_magic"
    );

    for (String skillId : expectedBasicSkills) {
      Optional<SkillTemplate> skillOpt = TemplateRepository.findSkill(skillId);
      assertThat(skillOpt)
          .as("基礎技能 %s 必須存在於 TemplateRepository", skillId)
          .isPresent();
      SkillTemplate skill = skillOpt.get();
      assertThat(skill.getName()).isNotBlank();
      if (skill.getType() != com.example.htmlmud.domain.model.enums.SkillType.PASSIVE) {
        assertThat(skill.getMoves()).isNotEmpty();
      }
    }
  }

  @Test
  @DisplayName("驗證門派特色高階武學與道術之元數據與門派歸屬")
  void testSectAdvancedSkillsAndSchools() {
    // 驗證新增的各門派武學
    SkillTemplate badao = TemplateRepository.findSkill("badao_blade").orElse(null);
    assertThat(badao).isNotNull();
    assertThat(badao.getName()).isEqualTo("霸刀歸一斬");
    assertThat(badao.getSchool()).isEqualTo("BADAO");
    assertThat(badao.getTags()).contains("BLADE", "RAGE");

    SkillTemplate storm = TemplateRepository.findSkill("storm_blade").orElse(null);
    assertThat(storm).isNotNull();
    assertThat(storm.getName()).isEqualTo("狂風絕息刀");
    assertThat(storm.getSchool()).isEqualTo("KUANGFENG");
    assertThat(storm.getTags()).contains("BLADE", "COMBO");

    SkillTemplate dragonSpear = TemplateRepository.findSkill("dragon_spear").orElse(null);
    assertThat(dragonSpear).isNotNull();
    assertThat(dragonSpear.getName()).isEqualTo("破陣遊龍槍");
    assertThat(dragonSpear.getSchool()).isEqualTo("TIANCE");

    SkillTemplate madStaff = TemplateRepository.findSkill("mad_demon_staff").orElse(null);
    assertThat(madStaff).isNotNull();
    assertThat(madStaff.getName()).isEqualTo("瘋魔杖法");
    assertThat(madStaff.getSchool()).isEqualTo("SHAOLIN");

    SkillTemplate shadowDagger = TemplateRepository.findSkill("shadow_strike").orElse(null);
    assertThat(shadowDagger).isNotNull();
    assertThat(shadowDagger.getName()).isEqualTo("無影幽冥刺");
    assertThat(shadowDagger.getSchool()).isEqualTo("SHADOW");

    SkillTemplate axeSkill = TemplateRepository.findSkill("mountain_split_axe").orElse(null);
    assertThat(axeSkill).isNotNull();
    assertThat(axeSkill.getName()).isEqualTo("開山裂地斧");
    assertThat(axeSkill.getSchool()).isEqualTo("JULI");

    SkillTemplate thunder = TemplateRepository.findSkill("thunder_strike").orElse(null);
    assertThat(thunder).isNotNull();
    assertThat(thunder.getName()).isEqualTo("九天應元雷訣");
    assertThat(thunder.getSchool()).isEqualTo("SHENXIAO");

    SkillTemplate iceSpear = TemplateRepository.findSkill("ice_spear").orElse(null);
    assertThat(iceSpear).isNotNull();
    assertThat(iceSpear.getName()).isEqualTo("玄冰聚煞引");
    assertThat(iceSpear.getSchool()).isEqualTo("TAIYIN");
  }

  @Test
  @DisplayName("驗證小隊夥伴裝備多樣武器時的自動普攻招式與門派套路啟用切換")
  void testPartyMemberWeaponSwitchingAndStanceEnablement() {
    Party party = partyService.createSoloParty("令狐掌門");
    PartyMember member = party.getMembers().get(0);

    // 1. 裝備長刀 -> 預設 basic_blade，啟用霸刀歸一斬
    member.getEquipment().put(EquipmentSlot.MAIN_HAND,
        PartyItemSlot.builder()
            .itemId("test_blade")
            .name("赤練狂刀")
            .itemType(ItemType.WEAPON)
            .subType("BLADE")
            .equipSlot(EquipmentSlot.MAIN_HAND)
            .bonusMinDamage(10)
            .bonusMaxDamage(20)
            .build());
    assertThat(member.getMainHandWeaponType()).isEqualTo(WeaponType.BLADE);
    assertThat(member.getEffectiveBasicSkillId()).isEqualTo("basic_blade");

    member.enableSkill(SkillCategory.BLADE, "badao_blade");
    assertThat(member.getEffectiveBasicSkillId()).isEqualTo("badao_blade");
    assertThat(member.getEnabledBasicSkill().getName()).isEqualTo("霸刀歸一斬");

    // 2. 裝備重錘 -> 預設 basic_blunt，啟用瘋魔杖法
    member.getEquipment().put(EquipmentSlot.MAIN_HAND,
        PartyItemSlot.builder()
            .itemId("test_hammer")
            .name("撼地破軍錘")
            .itemType(ItemType.WEAPON)
            .subType("HAMMER")
            .equipSlot(EquipmentSlot.MAIN_HAND)
            .bonusMinDamage(12)
            .bonusMaxDamage(22)
            .build());
    assertThat(member.getMainHandWeaponType()).isEqualTo(WeaponType.HAMMER);
    assertThat(member.getEffectiveBasicSkillId()).isEqualTo("basic_blunt");

    member.enableSkill(SkillCategory.HAMMER, "mad_demon_staff");
    assertThat(member.getEffectiveBasicSkillId()).isEqualTo("mad_demon_staff");
    assertThat(member.getEnabledBasicSkill().getName()).isEqualTo("瘋魔杖法");

    // 3. 裝備匕首 -> 預設 basic_dagger，啟用無影幽冥刺
    member.getEquipment().put(EquipmentSlot.MAIN_HAND,
        PartyItemSlot.builder()
            .itemId("test_dagger")
            .name("殘影匕首")
            .itemType(ItemType.WEAPON)
            .subType("DAGGER")
            .equipSlot(EquipmentSlot.MAIN_HAND)
            .bonusMinDamage(8)
            .bonusMaxDamage(16)
            .build());
    assertThat(member.getMainHandWeaponType()).isEqualTo(WeaponType.DAGGER);
    assertThat(member.getEffectiveBasicSkillId()).isEqualTo("basic_dagger");

    member.enableSkill(SkillCategory.DAGGER, "shadow_strike");
    assertThat(member.getEffectiveBasicSkillId()).isEqualTo("shadow_strike");
    assertThat(member.getEnabledBasicSkill().getName()).isEqualTo("無影幽冥刺");

    // 4. 裝備開山斧 -> 預設 basic_axe，啟用開山裂地斧
    member.getEquipment().put(EquipmentSlot.MAIN_HAND,
        PartyItemSlot.builder()
            .itemId("test_axe")
            .name("鎢金巨斧")
            .itemType(ItemType.WEAPON)
            .subType("AXE")
            .equipSlot(EquipmentSlot.MAIN_HAND)
            .bonusMinDamage(14)
            .bonusMaxDamage(25)
            .build());
    assertThat(member.getMainHandWeaponType()).isEqualTo(WeaponType.AXE);
    assertThat(member.getEffectiveBasicSkillId()).isEqualTo("basic_axe");

    member.enableSkill(SkillCategory.AXE, "mountain_split_axe");
    assertThat(member.getEffectiveBasicSkillId()).isEqualTo("mountain_split_axe");
    assertThat(member.getEnabledBasicSkill().getName()).isEqualTo("開山裂地斧");

    // 5. 測試容錯：裝備斑駁靈石鶴嘴鋤 (subType: PICKAXE) -> 應辨識為 AXE
    member.getEquipment().put(EquipmentSlot.MAIN_HAND,
        PartyItemSlot.builder()
            .itemId("miner_pickaxe")
            .name("斑駁靈石鶴嘴鋤")
            .itemType(ItemType.WEAPON)
            .subType("PICKAXE")
            .equipSlot(EquipmentSlot.MAIN_HAND)
            .bonusMinDamage(5)
            .bonusMaxDamage(10)
            .build());
    assertThat(member.getMainHandWeaponType()).isEqualTo(WeaponType.AXE);
    assertThat(member.getEffectiveBasicSkillId()).isEqualTo("mountain_split_axe");

    // 6. 卸下武器 (空手) -> basic_fist
    member.getEquipment().remove(EquipmentSlot.MAIN_HAND);
    assertThat(member.getMainHandWeaponType()).isEqualTo(WeaponType.UNARMED);
    assertThat(member.getEffectiveBasicSkillId()).isEqualTo("basic_fist");
  }

  @Test
  @DisplayName("驗證預設夥伴之已領悟套路 (learnedStances) 包含其專長進階套路")
  void testCompanionsAdvancedStancesConfiguration() {
    // 鐵牛應領悟少林瘋魔杖與巨力開山斧
    PartyMember tieNiu = partyService.createCompanion("tie_niu");
    assertThat(tieNiu).isNotNull();
    assertThat(tieNiu.getLearnedStances()).contains("basic_blunt", "mad_demon_staff", "mountain_split_axe", "iron_cloth");

    // 燕青應領悟無影幽冥刺與破陣遊龍槍
    PartyMember yanQing = partyService.createCompanion("yan_qing");
    assertThat(yanQing).isNotNull();
    assertThat(yanQing.getLearnedStances()).contains("basic_dagger", "shadow_strike", "dragon_spear", "cloud_step");

    // 墨道人應領悟狂風絕息刀與神霄雷訣
    PartyMember moYan = partyService.createCompanion("mo_yan");
    assertThat(moYan).isNotNull();
    assertThat(moYan.getLearnedStances()).contains("basic_blade", "storm_blade", "thunder_strike");
  }

  @Test
  @DisplayName("驗證狂風絕息刀分類解析、裝備刀類時可用套路過濾與切換武器獨立記憶")
  void testStormBladeWeaponSwitchingAndCustomStanceResolution() {
    SkillTemplate storm = TemplateRepository.findSkill("storm_blade").orElse(null);
    assertThat(storm).isNotNull();
    // 1. 狂風絕息刀必須被解析為 BLADE 分類（而非誤判為 UNARMED）
    assertThat(PartyMember.resolveSkillCategory(storm)).isEqualTo(SkillCategory.BLADE);
    assertThat(PartyMember.supportsSkillCategory(storm, SkillCategory.BLADE)).isTrue();
    assertThat(PartyMember.supportsSkillCategory(storm, SkillCategory.SWORD)).isFalse();

    PartyMember moYan = partyService.createCompanion("mo_yan");
    // 2. 初始空手時：主手為 UNARMED，未自訂時 custom 為 null，effective 回落 basic_fist
    assertThat(moYan.getMainHandWeaponType()).isEqualTo(WeaponType.UNARMED);
    assertThat(moYan.getCustomEnabledBasicSkillId()).isNull();
    assertThat(moYan.getEffectiveBasicSkillId()).isEqualTo("basic_fist");

    // 3. 裝備百辟精鋼刀 (BLADE)
    moYan.getEquipment().put(EquipmentSlot.MAIN_HAND,
        PartyItemSlot.builder()
            .itemId("steel_blade")
            .name("百辟精鋼刀")
            .itemType(ItemType.WEAPON)
            .subType("BLADE")
            .equipSlot(EquipmentSlot.MAIN_HAND)
            .build());
    assertThat(moYan.getMainHandWeaponType()).isEqualTo(WeaponType.BLADE);
    // 尚未手動 enable 時，custom 為 null (對應 UI 顯示 "(無)")，effective 回落 basic_blade
    assertThat(moYan.getCustomEnabledBasicSkillId()).isNull();
    assertThat(moYan.getEffectiveBasicSkillId()).isEqualTo("basic_blade");

    // 4. 手動 enable 狂風絕息刀
    moYan.enableSkill(SkillCategory.BLADE, "storm_blade");
    assertThat(moYan.getCustomEnabledBasicSkillId()).isEqualTo("storm_blade");
    assertThat(moYan.getEffectiveBasicSkillId()).isEqualTo("storm_blade");

    // 5. 切換為長劍 (SWORD) -> 刀法套路不應洩漏，SWORD 未設定 custom 應為 null
    moYan.getEquipment().put(EquipmentSlot.MAIN_HAND,
        PartyItemSlot.builder()
            .itemId("test_sword")
            .name("青銅古劍")
            .itemType(ItemType.WEAPON)
            .subType("SWORD")
            .equipSlot(EquipmentSlot.MAIN_HAND)
            .build());
    assertThat(moYan.getMainHandWeaponType()).isEqualTo(WeaponType.SWORD);
    assertThat(moYan.getCustomEnabledBasicSkillId()).isNull();
    assertThat(moYan.getEffectiveBasicSkillId()).isEqualTo("basic_sword");

    // 6. 再切換回百辟精鋼刀 (BLADE) -> 應自動恢復狂風絕息刀
    moYan.getEquipment().put(EquipmentSlot.MAIN_HAND,
        PartyItemSlot.builder()
            .itemId("steel_blade")
            .name("百辟精鋼刀")
            .itemType(ItemType.WEAPON)
            .subType("BLADE")
            .equipSlot(EquipmentSlot.MAIN_HAND)
            .build());
    assertThat(moYan.getMainHandWeaponType()).isEqualTo(WeaponType.BLADE);
    assertThat(moYan.getCustomEnabledBasicSkillId()).isEqualTo("storm_blade");
    assertThat(moYan.getEffectiveBasicSkillId()).isEqualTo("storm_blade");
  }
}
