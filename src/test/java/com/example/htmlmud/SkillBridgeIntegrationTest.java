package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.entity.SkillEntry;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.ResourceType;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.domain.service.CharacterSyncService;
import com.example.htmlmud.domain.service.SkillBridgeService;
import com.example.htmlmud.domain.service.TemplateCatalog;

class SkillBridgeIntegrationTest {

  private SkillBridgeService skillBridgeService;
  private CharacterSyncService characterSyncService;
  private TemplateReader templateReader;

  @BeforeEach
  void setUp() {
    templateReader = new TemplateCatalog();
    skillBridgeService = new SkillBridgeService(templateReader);
    characterSyncService = new CharacterSyncService();
    characterSyncService.setSkillBridgeService(skillBridgeService);
  }

  @Test
  @DisplayName("測試 MUD 基礎劍法映射至 DRPG 戰術技能 (破空劍氣)")
  void testBasicSwordMapping() {
    List<String> lowLvl = skillBridgeService.mapMudSkillToDrpgSkills("basic_sword", 1);
    assertThat(lowLvl).containsExactly("sword_pierce");

    // Lv.5 以上應額外解鎖進階群攻「太陰萬劍訣」
    List<String> highLvl = skillBridgeService.mapMudSkillToDrpgSkills("basic_sword", 5);
    assertThat(highLvl).containsExactly("sword_pierce", "sword_storm");
  }

  @Test
  @DisplayName("測試 MUD 高級劍法 (太極劍法/太陰劍法) 直接解鎖全套劍系戰術技能")
  void testAdvancedSwordMapping() {
    List<String> skills = skillBridgeService.mapMudSkillToDrpgSkills("taiji_sword", 1);
    assertThat(skills).contains("sword_pierce", "sword_storm");
  }

  @Test
  @DisplayName("測試 MUD 醫道、守禦、刺術技能映射")
  void testDiverseSkillMappings() {
    assertThat(skillBridgeService.mapMudSkillToDrpgSkills("basic_first_aid", 1))
        .containsExactly("heal_single");
    assertThat(skillBridgeService.mapMudSkillToDrpgSkills("basic_first_aid", 5))
        .containsExactly("heal_single", "heal_all_purify");

    assertThat(skillBridgeService.mapMudSkillToDrpgSkills("basic_parry", 1))
        .containsExactly("tank_taunt");

    assertThat(skillBridgeService.mapMudSkillToDrpgSkills("basic_dagger", 1))
        .containsExactly("rogue_shadow_strike");
    assertThat(skillBridgeService.mapMudSkillToDrpgSkills("basic_dagger", 5))
        .containsExactly("rogue_shadow_strike", "rogue_seven_star");
  }

  @Test
  @DisplayName("測試動態數值縮放：高等級 MUD 技能擁有更高傷害倍率與更短 CD")
  void testDynamicSkillScaling() {
    PartyMemberSkill baseSkill = templateReader.findPartySkill("sword_pierce").orElseThrow();

    LivingStats stats = new LivingStats();
    stats.setStr(20);
    stats.setDex(20);

    PartyMemberSkill lvl1 = skillBridgeService.scaleSkill(baseSkill, 1, stats);
    PartyMemberSkill lvl10 = skillBridgeService.scaleSkill(baseSkill, 10, stats);

    assertThat(lvl10.getDamageMultiplier()).isGreaterThan(lvl1.getDamageMultiplier());
    assertThat(lvl10.getCooldownMs()).isLessThan(lvl1.getCooldownMs());
    assertThat(lvl10.getDescription()).contains("Lv.10");
  }

  @Test
  @DisplayName("測試 CharacterSyncService 整合：從 Player 帶有之 MUD 技能動態注入並縮放至 PartyMember 隊長")
  void testCharacterSyncWithSkills() {
    Player player = mock(Player.class);
    LivingStats stats = new LivingStats();
    stats.setLevel(8);
    stats.setHp(250);
    stats.setMaxHp(250);
    stats.setStr(18);
    stats.setDex(18);

    // 玩家習得 basic_sword Lv.6 與 basic_parry Lv.3
    stats.getLearnedSkills().put("basic_sword", new SkillEntry("basic_sword", 6));
    stats.getLearnedSkills().put("basic_parry", new SkillEntry("basic_parry", 3));

    when(player.getStats()).thenReturn(stats);
    when(player.getLearnedSkills()).thenReturn(stats.getLearnedSkills());
    when(player.getName()).thenReturn("劍狂");

    PartyMember leader = PartyMember.builder()
        .id("m-leader")
        .name("劍狂")
        .stats(new LivingStats())
        .resourceType(ResourceType.COMBO)
        .row(RowPosition.FRONT)
        .build();

    // 執行同步
    characterSyncService.syncFromPlayerToParty(player, leader);

    // 隊長屬性同步驗證
    assertThat(leader.getStats().getMaxHp()).isEqualTo(250);
    assertThat(leader.getStats().getStr()).isEqualTo(18);

    // 隊長技能解鎖與縮放驗證 (應包含 sword_pierce, sword_storm, tank_taunt)
    assertThat(leader.getSkills()).extracting(PartyMemberSkill::getId)
        .contains("sword_pierce", "sword_storm", "tank_taunt");

    PartyMemberSkill pierce = leader.getSkills().stream()
        .filter(s -> s.getId().equals("sword_pierce")).findFirst().orElseThrow();
    assertThat(pierce.getDescription()).contains("Lv.6");
    assertThat(pierce.getDamageMultiplier()).isGreaterThan(1.8);
  }

  @Test
  @DisplayName("測試戰後修為回饋：戰鬥獲勝為 MUD 技能發放熟練度並促成突破升級")
  void testAwardCombatSkillXpAndLevelUp() {
    Player player = mock(Player.class);
    when(player.isValid()).thenReturn(true);

    LivingStats stats = new LivingStats();
    SkillEntry swordEntry = new SkillEntry("basic_sword", 1);
    swordEntry.setXp(90); // 差 10 點升級
    stats.getLearnedSkills().put("basic_sword", swordEntry);

    when(player.getLearnedSkills()).thenReturn(stats.getLearnedSkills());

    skillBridgeService.awardCombatSkillXp(player, 100);

    // 獲得 25 點熟練度 (100 / 4)，突破 100 門檻升為 Lv.2
    assertThat(swordEntry.getLevel()).isEqualTo(2);
    assertThat(swordEntry.getXp()).isZero();
  }

  @Test
  @DisplayName("測試反查解鎖前置武學")
  void testPrerequisiteLookup() {
    Set<String> prereqs = skillBridgeService.getPrerequisiteMudSkills("sword_storm");
    assertThat(prereqs).isNotEmpty();
    assertThat(prereqs).anyMatch(p -> p.contains("basic_sword"));
  }
}
