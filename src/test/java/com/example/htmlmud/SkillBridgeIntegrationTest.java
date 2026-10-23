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
import com.example.htmlmud.progression.SkillEntry;
import com.example.htmlmud.party.CombatResourceType;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.PartyMemberSkill;
import com.example.htmlmud.party.RowPosition;
import com.example.htmlmud.progression.SkillDefinition;
import com.example.htmlmud.template.TemplateReader;
import com.example.htmlmud.domain.actor.service.CharacterSyncService;
import com.example.htmlmud.template.TemplateCatalog;

/**
 * 大一統技能模型與無膠水架構整合測試 (SP-07: 徹底除役 SkillBridgeService 膠水代碼)
 */
class SkillBridgeIntegrationTest {

  private CharacterSyncService characterSyncService;
  private TemplateReader templateReader;

  @BeforeEach
  void setUp() {
    templateReader = new TemplateCatalog();
    characterSyncService = new CharacterSyncService(new com.example.htmlmud.combat.BattleOutcomeApplier(), templateReader, null);
  }

  @Test
  @DisplayName("測試基礎武學大一統映射至戰鬥技能 (破空劍氣)")
  void testBasicSwordMapping() {
    List<String> lowLvl = SkillDefinition.resolveDefaultBridges("basic_sword", 1);
    assertThat(lowLvl).containsExactly("sword_pierce");

    // Lv.5 以上應額外解鎖進階群攻「太陰萬劍訣」
    List<String> highLvl = SkillDefinition.resolveDefaultBridges("basic_sword", 5);
    assertThat(highLvl).containsExactly("sword_pierce", "sword_storm");
  }

  @Test
  @DisplayName("測試高級劍法 (太極劍法/太陰劍法) 直接解鎖全套劍系戰鬥技能")
  void testAdvancedSwordMapping() {
    List<String> skills = SkillDefinition.resolveDefaultBridges("taiji_sword", 1);
    assertThat(skills).contains("sword_pierce", "sword_storm");
  }

  @Test
  @DisplayName("測試醫道、守禦、刺術技能映射")
  void testDiverseSkillMappings() {
    assertThat(SkillDefinition.resolveDefaultBridges("basic_first_aid", 1))
        .containsExactly("heal_single");
    assertThat(SkillDefinition.resolveDefaultBridges("basic_first_aid", 5))
        .containsExactly("heal_single", "heal_all_purify");

    assertThat(SkillDefinition.resolveDefaultBridges("basic_parry", 1))
        .containsExactly("tank_taunt");

    assertThat(SkillDefinition.resolveDefaultBridges("basic_dagger", 1))
        .containsExactly("rogue_shadow_strike");
    assertThat(SkillDefinition.resolveDefaultBridges("basic_dagger", 5))
        .containsExactly("rogue_shadow_strike", "rogue_seven_star");
  }

  @Test
  @DisplayName("測試動態數值縮放：UnifiedSkill 內建修煉境界，高等級技能擁有更高傷害倍率與更短 CD")
  void testDynamicSkillScaling() {
    PartyMemberSkill skill = templateReader.findPartySkill("sword_pierce").orElseThrow();
    try {
      skill.setLevel(1);
      double dmgLvl1 = skill.getEffectiveDamageMultiplier();
      long cdLvl1 = skill.getEffectiveCooldownMs();

      skill.setLevel(10);
      double dmgLvl10 = skill.getEffectiveDamageMultiplier();
      long cdLvl10 = skill.getEffectiveCooldownMs();

      assertThat(dmgLvl10).isGreaterThan(dmgLvl1);
      assertThat(cdLvl10).isLessThan(cdLvl1);
    } finally {
      skill.setLevel(1);
    }
  }

  @Test
  @DisplayName("測試 CharacterSyncService 整合：從 Player 帶有之技能直接同步至 PartyMember 隊長大一統模型")
  void testCharacterSyncWithSkills() {
    Player player = mock(Player.class);
    LivingStats stats = new LivingStats();
    stats.setLevel(8);
    stats.setHp(250);
    stats.setMaxHp(250);
    stats.setStr(18);
    stats.setDex(18);

    // 玩家習得 sword_pierce Lv.6 與 tank_taunt Lv.3
    stats.getLearnedSkills().put("sword_pierce", new SkillEntry("sword_pierce", 6));
    stats.getLearnedSkills().put("tank_taunt", new SkillEntry("tank_taunt", 3));

    when(player.getStats()).thenReturn(stats);
    when(player.getLearnedSkills()).thenReturn(stats.getLearnedSkills());
    when(player.getName()).thenReturn("劍狂");

    PartyMember leader = PartyMember.builder()
        .id("m-leader")
        .name("劍狂")
        .stats(new LivingStats())
        .resourceType(CombatResourceType.COMBO)
        .row(RowPosition.FRONT)
        .build();

    // 執行同步
    characterSyncService.syncFromPlayerToParty(player, leader);

    // 隊長屬性同步驗證
    assertThat(leader.getStats().getMaxHp()).isEqualTo(250);
    assertThat(leader.getStats().getStr()).isEqualTo(18);

    // 隊長技能大一統同步驗證
    assertThat(leader.getSkills()).extracting(PartyMemberSkill::getId)
        .contains("sword_pierce", "tank_taunt");

    PartyMemberSkill pierce = leader.getSkills().stream()
        .filter(s -> s.getId().equals("sword_pierce")).findFirst().orElseThrow();
    assertThat(pierce.getLevel()).isEqualTo(6);
    assertThat(pierce.getEffectiveDamageMultiplier()).isGreaterThan(1.2);
  }

  @Test
  @DisplayName("測試戰後原地熟練度增長：大一統技能自內聚升級與熟練度保留")
  void testAwardCombatSkillXpAndLevelUp() {
    PartyMemberSkill skill = PartyMemberSkill.builder()
        .id("sword_pierce")
        .name("破空劍氣")
        .level(1)
        .xp(90L)
        .maxXp(100L)
        .build();

    // 獲得 25 點熟練度，突破 Lv.1 門檻 (100) 升為 Lv.2，保留溢出熟練度 15
    skill.addXp(25L);

    assertThat(skill.getLevel()).isEqualTo(2);
    assertThat(skill.getXp()).isEqualTo(15L);
  }

  @Test
  @DisplayName("測試反查解鎖前置武學")
  void testPrerequisiteLookup() {
    List<String> prereqs = SkillDefinition.findDefaultPrerequisites("sword_storm");
    assertThat(prereqs).isNotEmpty();
    assertThat(prereqs).anyMatch(p -> p.contains("basic_sword"));
  }
}
