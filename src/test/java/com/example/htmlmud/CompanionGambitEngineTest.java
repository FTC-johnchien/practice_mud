package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.CombatFsmService;
import com.example.htmlmud.domain.dungeon.battle.DrpgCombatLoop;
import com.example.htmlmud.domain.dungeon.battle.DrpgEnemyTacticsService;
import com.example.htmlmud.party.GambitActionType;
import com.example.htmlmud.party.GambitPreset;
import com.example.htmlmud.party.GambitRule;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.PartyMemberSkill;
import com.example.htmlmud.party.RowPosition;
import com.example.htmlmud.party.TacticsCondition;
import com.example.htmlmud.party.TacticsRule;
import com.example.htmlmud.party.TacticsTarget;
import com.example.htmlmud.template.TemplateReader;

/**
 * Milestone F3-05: 隊友自動戰術 Gambit 規則鏈引擎 (Companion Gambit AI Chains) 專屬整合測試
 */
@SpringBootTest
class CompanionGambitEngineTest {

  @Autowired
  private TemplateReader templateReader;

  @Autowired
  private DrpgCombatLoop combatLoop;

  @Autowired
  private DrpgEnemyTacticsService tacticsService;

  private CombatFsmService fsmService;

  @BeforeEach
  void setUp() {
    fsmService = new CombatFsmService();
  }

  @Test
  @DisplayName("F3-05-T1: 驗證 GambitRule 領域模型、工廠方法與隊友預設打斷最高優先順序")
  void testGambitModelAndDefaultInterruptPriority() {
    // 1. 驗證工廠方法構建
    TacticsRule interruptRule = GambitRule.ofInterrupt(1, "taibai_interrupt");
    assertThat(interruptRule.getPriority()).isEqualTo(1);
    assertThat(interruptRule.getCondition()).isEqualTo(TacticsCondition.ENEMY_CASTING_INTERRUPTIBLE);
    assertThat(interruptRule.getTarget()).isEqualTo(TacticsTarget.CASTING_ENEMY);
    assertThat(interruptRule.getActionType()).isEqualTo(GambitActionType.SKILL);
    assertThat(interruptRule.getSkillId()).isEqualTo("taibai_interrupt");

    TacticsRule poiseRule = GambitRule.ofPoiseBrokenBurst(2, "sword_storm");
    assertThat(poiseRule.getPriority()).isEqualTo(2);
    assertThat(poiseRule.getCondition()).isEqualTo(TacticsCondition.ENEMY_POISE_BROKEN);
    assertThat(poiseRule.getTarget()).isEqualTo(TacticsTarget.POISE_BROKEN_ENEMY);

    TacticsRule sanRule = GambitRule.ofEmergencySan(3, 30, "tranquil_mind");
    assertThat(sanRule.getPriority()).isEqualTo(3);
    assertThat(sanRule.getCondition()).isEqualTo(TacticsCondition.ALLY_SAN_LESS_THAN);
    assertThat(sanRule.getConditionValue()).isEqualTo(30);

    // 2. 驗證 PartyMember 預設自動配置：具備打斷技能時，打斷規則為 Priority 1
    PartyMember swordsman = PartyMember.builder()
        .id("comp-sword-01")
        .name("白衣劍修")
        .gambitEnabled(true)
        .build();

    PartyMemberSkill interruptSkill = PartyMemberSkill.builder()
        .id("taibai_interrupt")
        .name("太白斷念劍")
        .stun(true)
        .tags(List.of("SWORD", "INTERRUPT", "SILENCE"))
        .build();

    PartyMemberSkill normalSkill = PartyMemberSkill.builder()
        .id("sword_pierce")
        .name("青元貫石劍")
        .build();

    swordsman.setSkills(new ArrayList<>(List.of(interruptSkill, normalSkill)));
    swordsman.initDefaultTactics();

    assertThat(swordsman.getTactics()).isNotEmpty();
    TacticsRule firstRule = swordsman.getTactics().get(0);
    assertThat(firstRule.getPriority()).isEqualTo(1);
    assertThat(firstRule.getSkillId()).isEqualTo("taibai_interrupt");
    assertThat(firstRule.getCondition()).isEqualTo(TacticsCondition.ENEMY_CASTING_INTERRUPTIBLE);
    assertThat(firstRule.getTarget()).isEqualTo(TacticsTarget.CASTING_ENEMY);
  }

  @Test
  @DisplayName("F3-05-T2: 驗證戰術條件求值 (CASTING, POISE_BROKEN, HP, SAN) 與智慧目標解析")
  void testTacticsConditionEvaluationAndTargetResolution() {
    PartyMember healer = PartyMember.builder()
        .id("comp-healer-01")
        .name("素問醫修")
        .currentSan(35)
        .maxSan(100)
        .build();

    PartyMember tank = PartyMember.builder()
        .id("comp-tank-01")
        .name("金剛伏魔")
        .currentSan(80)
        .maxSan(100)
        .build();

    Party party = Party.builder()
        .id("party-01")
        .partyName("問道小隊")
        .members(new ArrayList<>(List.of(healer, tank)))
        .build();

    BattleEnemy boss = BattleEnemy.builder()
        .id("enemy-boss-01")
        .name("赤焰真魔")
        .hp(1000)
        .maxHp(1000)
        .poise(100)
        .maxPoise(100)
        .alive(true)
        .build();

    BattleEnemy minion = BattleEnemy.builder()
        .id("enemy-minion-01")
        .name("火靈小妖")
        .hp(20)
        .maxHp(100) // 殘血 20%
        .poise(50)
        .maxPoise(50)
        .alive(true)
        .build();
    minion.triggerPoiseBreak(); // 觸發破防失衡狀態

    BattleContext ctx = BattleContext.builder()
        .battleId("battle-gambit-01")
        .party(party)
        .enemies(new ArrayList<>(List.of(boss, minion)))
        .build();

    // 1. 條件測試：目前無人吟唱，ENEMY_CASTING_INTERRUPTIBLE 應為 false
    TacticsRule condInterrupt = TacticsRule.builder()
        .condition(TacticsCondition.ENEMY_CASTING_INTERRUPTIBLE)
        .build();
    assertThat(tacticsService.evaluateTacticsCondition(ctx, healer, condInterrupt)).isFalse();

    // Boss 開始蓄力
    fsmService.startEnemyCast(boss, "🔥", "【天火焚滅】", "SPELL", "ALL", 3000L, true, "fire_storm");
    assertThat(tacticsService.evaluateTacticsCondition(ctx, healer, condInterrupt)).isTrue();

    // 目標解析：CASTING_ENEMY 應選中 boss
    BattleEnemy resolvedCastingEnemy = tacticsService.resolveEnemyTarget(ctx, TacticsTarget.CASTING_ENEMY);
    assertThat(resolvedCastingEnemy).isNotNull();
    assertThat(resolvedCastingEnemy.getId()).isEqualTo("enemy-boss-01");

    // 2. 條件測試：minion 處於破防狀態，ENEMY_POISE_BROKEN 應為 true
    TacticsRule condPoise = TacticsRule.builder()
        .condition(TacticsCondition.ENEMY_POISE_BROKEN)
        .build();
    assertThat(tacticsService.evaluateTacticsCondition(ctx, healer, condPoise)).isTrue();

    // 目標解析：POISE_BROKEN_ENEMY 應選中 minion
    BattleEnemy resolvedPoiseEnemy = tacticsService.resolveEnemyTarget(ctx, TacticsTarget.POISE_BROKEN_ENEMY);
    assertThat(resolvedPoiseEnemy).isNotNull();
    assertThat(resolvedPoiseEnemy.getId()).isEqualTo("enemy-minion-01");

    // 3. 條件測試：ENEMY_HP_LESS_THAN 25%，minion (20%) 滿足
    TacticsRule condLowHp = TacticsRule.builder()
        .condition(TacticsCondition.ENEMY_HP_LESS_THAN)
        .conditionValue(25)
        .build();
    assertThat(tacticsService.evaluateTacticsCondition(ctx, healer, condLowHp)).isTrue();

    // 目標解析：LOWEST_HP_ENEMY 應選中 minion (20%)
    BattleEnemy resolvedLowestEnemy = tacticsService.resolveEnemyTarget(ctx, TacticsTarget.LOWEST_HP_ENEMY);
    assertThat(resolvedLowestEnemy).isNotNull();
    assertThat(resolvedLowestEnemy.getId()).isEqualTo("enemy-minion-01");

    // 4. 條件測試：ALLY_SAN_LESS_THAN 40，healer 當前 35 滿足
    TacticsRule condSan = TacticsRule.builder()
        .condition(TacticsCondition.ALLY_SAN_LESS_THAN)
        .conditionValue(40)
        .target(TacticsTarget.LOWEST_HP_ALLY)
        .build();
    assertThat(tacticsService.evaluateTacticsCondition(ctx, healer, condSan)).isTrue();

    // 友方目標解析：道心低於 40 的隊友應為 healer
    PartyMember resolvedSanAlly = tacticsService.resolveAllyTarget(ctx, healer, condSan, -1);
    assertThat(resolvedSanAlly).isNotNull();
    assertThat(resolvedSanAlly.getId()).isEqualTo("comp-healer-01");
  }

  @Test
  @DisplayName("F3-05-T3: 驗證戰術範本庫 gambit_presets.json 載入與 applyGambitPreset 智慧配置")
  void testGambitPresetsLoadingAndApplication() {
    // 1. 驗證從 TemplateReader 取得 4 大預設範本
    assertThat(templateReader.getAllGambitPresets()).hasSizeGreaterThanOrEqualTo(4);

    GambitPreset interruptPreset = templateReader.findGambitPreset("interrupt_focus").orElseThrow();
    assertThat(interruptPreset.getName()).contains("斷法攻堅");
    assertThat(interruptPreset.getRules()).isNotEmpty();

    GambitPreset sustainPreset = templateReader.findGambitPreset("emergency_sustain").orElseThrow();
    assertThat(sustainPreset.getName()).contains("急救續航");

    // 2. 套用範本至同伴：醫修同伴套用急救續航
    PartyMember cleric = PartyMember.builder()
        .id("comp-cleric")
        .name("靈素仙子")
        .build();

    PartyMemberSkill healSkill = PartyMemberSkill.builder()
        .id("divine_healing")
        .name("回春靈術")
        .heal(true)
        .healAmount(60)
        .build();

    PartyMemberSkill cleanseSkill = PartyMemberSkill.builder()
        .id("tranquil_mind")
        .name("清心定神訣")
        .sanRestore(25)
        .tags(List.of("CLEANSE"))
        .build();

    cleric.setSkills(new ArrayList<>(List.of(healSkill, cleanseSkill)));
    cleric.applyGambitPreset(sustainPreset);

    assertThat(cleric.getTactics()).isNotEmpty();
    // 檢查第一條規則應為低血急救 (healSkill)
    TacticsRule r1 = cleric.getTactics().get(0);
    assertThat(r1.getSkillId()).isEqualTo("divine_healing");
    assertThat(r1.getCondition()).isEqualTo(TacticsCondition.ALLY_HP_LESS_THAN);

    // 檢查道心安撫規則存在
    boolean hasSanRule = cleric.getTactics().stream()
        .anyMatch(r -> r.getCondition() == TacticsCondition.ALLY_SAN_LESS_THAN && "tranquil_mind".equals(r.getSkillId()));
    assertThat(hasSanRule).isTrue();
  }

  @Test
  @DisplayName("F3-05-T4: 驗證戰鬥循環執行與 gambitEnabled 開關防護")
  void testCombatLoopGambitExecutionAndSwitchGuard() {
    Player player = mock(Player.class);
    when(player.getName()).thenReturn("測試道長");
    when(player.isValid()).thenReturn(true);
    when(player.getId()).thenReturn("player-test-01");

    PartyMember swordComp = PartyMember.builder()
        .id("sword-companion")
        .name("青鋒劍侍")
        .row(RowPosition.FRONT)
        .gambitEnabled(true)
        .build();

    PartyMemberSkill interruptSkill = PartyMemberSkill.builder()
        .id("taibai_interrupt")
        .name("太白斷念劍")
        .stun(true)
        .cooldownMs(5000)
        .damageMultiplier(1.5)
        .tags(List.of("SWORD", "INTERRUPT", "SILENCE"))
        .build();

    swordComp.setSkills(new ArrayList<>(List.of(interruptSkill)));
    swordComp.initDefaultTactics();

    Party party = Party.builder()
        .id("party-switch-01")
        .members(new ArrayList<>(List.of(swordComp)))
        .build();

    BattleEnemy boss = BattleEnemy.builder()
        .id("boss-casting-01")
        .name("深淵喚魔祭司")
        .hp(500)
        .maxHp(500)
        .poise(100)
        .maxPoise(100)
        .alive(true)
        .build();

    // 敵怪開始吟唱
    fsmService.startEnemyCast(boss, "🔮", "【召喚虛空古魔】", "SPELL", "ALL", 4000L, true, "summon_void");

    BattleContext ctx = BattleContext.builder()
        .battleId("battle-switch-01")
        .playerId(player.getId())
        .party(party)
        .enemies(new ArrayList<>(List.of(boss)))
        .build();

    // 1. 當 gambitEnabled 為 false 時，同伴絕不執行自動戰術
    swordComp.setGambitEnabled(false);
    boolean triggeredDisabled = combatLoop.tryTriggerCompanionTactics(player, ctx, swordComp, null);
    assertThat(triggeredDisabled).isFalse();
    assertThat(boss.isCasting()).isTrue(); // 仍未被打斷

    // 2. 當 gambitEnabled 為 true 時，同伴立刻觸發太白斷念劍打斷
    swordComp.setGambitEnabled(true);
    boolean triggeredEnabled = combatLoop.tryTriggerCompanionTactics(player, ctx, swordComp, null);
    assertThat(triggeredEnabled).isTrue();

    // 驗證敵怪施法已被成功中斷
    assertThat(boss.isCasting()).isFalse();
    assertThat(boss.getCastingState()).isEqualTo(BattleEnemy.CastingState.INTERRUPTED);
  }
}
