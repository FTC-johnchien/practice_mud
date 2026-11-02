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
import com.example.htmlmud.party.PartyItemSlot;
import com.example.htmlmud.party.PartyMemberSkill;
import com.example.htmlmud.party.PartyMemberSkill.ComboRole;
import com.example.htmlmud.party.PartyMemberSkill.OriginSchool;
import com.example.htmlmud.party.PartyService;

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
