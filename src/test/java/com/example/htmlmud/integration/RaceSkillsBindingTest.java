package com.example.htmlmud.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.model.template.RaceTemplate;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;

@SpringBootTest
@ActiveProfiles("test")
public class RaceSkillsBindingTest {

  @Autowired
  private WorldFactory worldFactory;

  @Autowired
  private PartyService partyService;

  @Test
  @DisplayName("測試所有種族在 races.json 中均具備專屬的 naturalDodge 與 naturalParry")
  void testRacesDefinitionAndSkillExistence() {
    String[] races = {"human", "rat", "wolf", "beast", "humanoid", "dragon", "undead"};

    for (String raceId : races) {
      var raceOpt = TemplateRepository.findRace(raceId);
      assertThat(raceOpt).as("種族 " + raceId + " 必須存在").isPresent();
      RaceTemplate race = raceOpt.get();
      assertThat(race.combat()).as("種族 " + raceId + " 必須有 combat 設定").isNotNull();

      String dodge = race.combat().naturalDodge();
      String parry = race.combat().naturalParry();
      assertThat(dodge).as("種族 " + raceId + " 的 naturalDodge 不能為空").isNotBlank();
      assertThat(parry).as("種族 " + raceId + " 的 naturalParry 不能為空").isNotBlank();

      // 驗證技能模板在庫中真實存在
      var dodgeSkill = TemplateRepository.findSkill(dodge);
      assertThat(dodgeSkill).as("閃避技能 " + dodge + " 必須載入成功").isPresent();
      assertThat(dodgeSkill.get().getMoves()).as("閃避技能 " + dodge + " 必須擁有招式").isNotEmpty();

      var parrySkill = TemplateRepository.findSkill(parry);
      assertThat(parrySkill).as("招架技能 " + parry + " 必須載入成功").isPresent();
      assertThat(parrySkill.get().getMoves()).as("招架技能 " + parry + " 必須擁有招式").isNotEmpty();
    }
  }

  @Test
  @DisplayName("測試 WorldFactory.createMob 生成鼠類時自動綁定狡鼠閃避與尖齒偏架")
  void testRatMobSkillBinding() {
    Mob rat = worldFactory.createMob("wild_rat");
    assertThat(rat).isNotNull();
    assertThat(rat.getStats().getRace()).isEqualTo("rat");

    // 檢查 enabledSkills
    assertThat(rat.getEnabledSkills().get(SkillCategory.DODGE)).isEqualTo("mob_rat_dodge");
    assertThat(rat.getEnabledSkills().get(SkillCategory.PARRY)).isEqualTo("mob_rat_parry");

    // 檢查 learnedSkills
    assertThat(rat.getLearnedSkills()).containsKey("mob_rat_dodge");
    assertThat(rat.getLearnedSkills()).containsKey("mob_rat_parry");
  }

  @Test
  @DisplayName("測試 WorldFactory.createMob 依據不同種族自動補齊專屬技能")
  void testDifferentRacesMobSkillBinding() {
    // 1. 人類守衛
    Mob guard = worldFactory.createMob("village_guard");
    assertThat(guard).isNotNull();
    assertThat(guard.getEnabledSkills().get(SkillCategory.DODGE)).isEqualTo("basic_dodge");
    assertThat(guard.getEnabledSkills().get(SkillCategory.PARRY)).isEqualTo("basic_parry");

    // 2. 測試動態種族生成 (不死族與龍族)
    MobTemplate undeadTpl = MobTemplate.builder()
        .id("test_skeleton")
        .name("骷髏兵")
        .race("undead")
        .level(1)
        .maxHp(100)
        .build();
    TemplateRepository.registerMob(undeadTpl);

    Mob skeleton = worldFactory.createMob("test_skeleton");
    assertThat(skeleton.getEnabledSkills().get(SkillCategory.DODGE)).isEqualTo("mob_undead_dodge");
    assertThat(skeleton.getEnabledSkills().get(SkillCategory.PARRY)).isEqualTo("mob_undead_parry");

    // 3. 龍族怪
    MobTemplate dragonTpl = MobTemplate.builder()
        .id("test_dragon")
        .name("太古赤龍")
        .race("dragon")
        .level(50)
        .maxHp(5000)
        .build();
    TemplateRepository.registerMob(dragonTpl);

    Mob dragon = worldFactory.createMob("test_dragon");
    assertThat(dragon.getEnabledSkills().get(SkillCategory.DODGE)).isEqualTo("mob_dragon_dodge");
    assertThat(dragon.getEnabledSkills().get(SkillCategory.PARRY)).isEqualTo("mob_dragon_parry");
  }

  @Test
  @DisplayName("測試 BattleEnemy.fromTemplate 地牢戰鬥怪物自動關聯種族 Dodge 與 Parry")
  void testBattleEnemyRaceSkillBinding() {
    var ratTplOpt = TemplateRepository.findMob("wild_rat");
    assertThat(ratTplOpt).isPresent();

    BattleEnemy enemy = BattleEnemy.fromTemplate("e-1", ratTplOpt.get(), RowPosition.FRONT, null);
    assertThat(enemy).isNotNull();
    assertThat(enemy.getDodgeSkillId()).isEqualTo("mob_rat_dodge");
    assertThat(enemy.getParrySkillId()).isEqualTo("mob_rat_parry");
    assertThat(enemy.getSkills()).contains("mob_rat_dodge", "mob_rat_parry");
  }

  @Test
  @DisplayName("測試小隊主角與招募夥伴自動啟用基礎身法與招架 (basic_dodge, basic_parry)")
  void testPartyMembersDodgeAndParry() {
    Party party = partyService.createSoloParty("玄天子");
    PartyMember leader = party.getMembers().get(0);

    // 主角
    assertThat(leader.getEnabledSkills().get(SkillCategory.DODGE)).isEqualTo("basic_dodge");
    assertThat(leader.getEnabledSkills().get(SkillCategory.PARRY)).isEqualTo("basic_parry");

    // 招募夥伴 (鐵牛)
    partyService.recruitCompanion(party, "tie_niu");
    PartyMember tieNiu = party.getMembers().stream()
        .filter(m -> m.getName().contains("鐵牛"))
        .findFirst()
        .orElse(null);

    assertThat(tieNiu).isNotNull();
    assertThat(tieNiu.getEnabledSkills().get(SkillCategory.DODGE)).isEqualTo("basic_dodge");
    assertThat(tieNiu.getEnabledSkills().get(SkillCategory.PARRY)).isEqualTo("basic_parry");
  }
}
