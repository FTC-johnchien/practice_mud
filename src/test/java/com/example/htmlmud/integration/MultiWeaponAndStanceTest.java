package com.example.htmlmud.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;

@SpringBootTest
@ActiveProfiles("test")
public class MultiWeaponAndStanceTest {

  @Autowired
  private PartyService partyService;

  private Party party;
  private PartyMember leader;

  @BeforeEach
  void setUp() {
    party = partyService.createSoloParty("測試天劍傳人");
    leader = party.getMembers().get(0);
  }

  @Test
  @DisplayName("測試主角在持劍時可切換多種高級劍法 (太極劍法 / 太陰幽冥劍法 / 天劍飛仙術)")
  void testSwordStancesSwitching() {
    // 預設持劍，基礎技能為 basic_sword
    assertThat(leader.getEffectiveBasicSkillId()).isEqualTo("basic_sword");

    // 切換為太極劍法
    leader.enableSkill(SkillCategory.SWORD, "taiji_sword");
    assertThat(leader.getEffectiveBasicSkillId()).isEqualTo("taiji_sword");
    var taiji = leader.getEnabledBasicSkill();
    assertThat(taiji).isNotNull();
    assertThat(taiji.getName()).isEqualTo("太極劍法");
    assertThat(taiji.getMoves()).isNotEmpty();

    // 切換為太陰幽冥劍法
    leader.enableSkill(SkillCategory.SWORD, "taiyin_sword");
    assertThat(leader.getEffectiveBasicSkillId()).isEqualTo("taiyin_sword");
    var taiyin = leader.getEnabledBasicSkill();
    assertThat(taiyin).isNotNull();
    assertThat(taiyin.getName()).isEqualTo("太陰幽冥劍法");

    // 切換為天劍飛仙術
    leader.enableSkill(SkillCategory.SWORD, "tianjian_sword");
    assertThat(leader.getEffectiveBasicSkillId()).isEqualTo("tianjian_sword");
    var tianjian = leader.getEnabledBasicSkill();
    assertThat(tianjian).isNotNull();
    assertThat(tianjian.getName()).isEqualTo("天劍飛仙術");

    // 切回 null / 空字串，自動還原回 basic_sword
    leader.enableSkill(SkillCategory.SWORD, null);
    assertThat(leader.getEffectiveBasicSkillId()).isEqualTo("basic_sword");
  }

  @Test
  @DisplayName("測試裝備多種武器 (刀、斧、棍、錘、匕首、弓箭) 時自動引用對應基礎技能")
  void testMultipleWeaponsAutoAttackBinding() {
    // 1. 裝備百辟精鋼刀 (BLADE)
    equipWeapon("steel_blade", "百辟精鋼刀", "BLADE");
    assertThat(leader.getEffectiveBasicSkillId()).isEqualTo("basic_blade");

    // 2. 裝備開山大斧 (AXE)
    equipWeapon("iron_axe", "開山大斧", "AXE");
    assertThat(leader.getEffectiveBasicSkillId()).isEqualTo("basic_axe");

    // 3. 裝備齊眉熟銅棍 (STAFF)
    equipWeapon("copper_staff", "齊眉熟銅棍", "STAFF");
    assertThat(leader.getEffectiveBasicSkillId()).isEqualTo("basic_magic_staff");

    // 4. 裝備八棱玄鐵錘 (HAMMER)
    equipWeapon("iron_hammer", "八棱玄鐵錘", "HAMMER");
    assertThat(leader.getEffectiveBasicSkillId()).isEqualTo("basic_blunt");

    // 5. 裝備無影短匕 (DAGGER)
    equipWeapon("shadow_dagger", "無影短匕", "DAGGER");
    assertThat(leader.getEffectiveBasicSkillId()).isEqualTo("basic_dagger");

    // 6. 裝備穿雲桑木弓 (BOW)
    equipWeapon("hunter_bow", "穿雲桑木弓", "BOW");
    assertThat(leader.getEffectiveBasicSkillId()).isEqualTo("basic_archery");

    // 7. 卸下武器，赤手空拳回歸 basic_fist
    leader.unequip(EquipmentSlot.MAIN_HAND);
    assertThat(leader.getEffectiveBasicSkillId()).isEqualTo("basic_fist");
  }

  @Test
  @DisplayName("測試非人怪物 (鼠、狼、龍) 能正確解析種族天然攻擊")
  void testMonsterNaturalAttacksResolution() {
    // 1. 野鼠 (rat)
    var ratRace = TemplateRepository.findRace("rat");
    assertThat(ratRace).isPresent();
    assertThat(ratRace.get().combat().naturalAttacks()).isNotEmpty();
    List<String> ratAttacks = ratRace.get().combat().naturalAttacks().stream().map(a -> a.getId()).toList();
    assertThat(ratAttacks).contains("mob_claw", "mob_bite");

    // 2. 狼族 (wolf)
    var wolfRace = TemplateRepository.findRace("wolf");
    assertThat(wolfRace).isPresent();
    assertThat(wolfRace.get().combat().naturalAttacks()).isNotEmpty();
    List<String> wolfAttacks = wolfRace.get().combat().naturalAttacks().stream().map(a -> a.getId()).toList();
    assertThat(wolfAttacks).contains("mob_claw", "mob_bite", "mob_tail_swipe");

    // 3. 龍族 (dragon)
    var dragonRace = TemplateRepository.findRace("dragon");
    assertThat(dragonRace).isPresent();
    assertThat(dragonRace.get().combat().naturalAttacks()).isNotEmpty();
    List<String> dragonAttacks = dragonRace.get().combat().naturalAttacks().stream().map(a -> a.getId()).toList();
    assertThat(dragonAttacks).contains("mob_claw", "mob_bite", "mob_tail_swipe", "mob_smash");

    // 4. BattleEnemy fromTemplate 正確攜帶 race
    var mobOpt = TemplateRepository.findMob("wild_rat");
    assertThat(mobOpt).isPresent();
    BattleEnemy enemy = BattleEnemy.fromTemplate("e-1", mobOpt.get(), null, null);
    assertThat(enemy.getRace()).isEqualTo("rat");
  }

  private void equipWeapon(String itemId, String name, String subType) {
    PartyItemSlot weapon = PartyItemSlot.builder()
        .slotId("slot-" + itemId)
        .itemId(itemId)
        .name(name)
        .itemType(ItemType.WEAPON)
        .subType(subType)
        .equipSlot(EquipmentSlot.MAIN_HAND)
        .bonusMinDamage(15)
        .bonusMaxDamage(25)
        .build();
    leader.equip(EquipmentSlot.MAIN_HAND, weapon);
  }
}
