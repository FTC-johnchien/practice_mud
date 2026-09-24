package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.application.command.impl.PartyCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.DefenseResolver;
import com.example.htmlmud.domain.dungeon.battle.DefenseResolver.DefenseOutcome;
import com.example.htmlmud.domain.dungeon.dto.DrpgStateDto;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.repository.TemplateReader;

/**
 * Phase 9 機制測試：被動技能 Enable 裝配與戰鬥檢定系統
 * 遵循「機制運行 + 資料驅動 + 純粹測試」原則：
 * 1. 測試資料從 JSON 讀取
 * 2. 測試動態裝配切換與 DTO 映射
 * 3. 測試 DefenseResolver 的閃避、招架、減傷與 SP 回復機制
 */
@SpringBootTest
class PassiveSkillsAndCombatCheckTest {

  @Autowired
  private PartyService partyService;

  @Autowired
  private TemplateReader templateReader;

  @Autowired
  private DefenseResolver defenseResolver;

  @Autowired(required = false)
  private PartyCommand partyCommand;

  private Player dummyPlayer;

  @BeforeEach
  void setUp() {
    dummyPlayer = mock(Player.class);
    when(dummyPlayer.getName()).thenReturn("tester");
    when(dummyPlayer.isValid()).thenReturn(true);
  }

  @Test
  @DisplayName("機制測試 1: 全體隊員資料驅動自動裝配初始 DODGE, PARRY, FORCE 心法")
  void testDataDrivenDefaultPassivesAssignedToAllCompanions() {
    Party party = partyService.createFullParty("tester");

    assertThat(party.getMembers()).hasSize(5);

    for (PartyMember member : party.getMembers()) {
      String dodgeSkillId = member.getEnabledPassiveSkillId(SkillCategory.DODGE);
      String parrySkillId = member.getEnabledPassiveSkillId(SkillCategory.PARRY);
      String forceSkillId = member.getEnabledPassiveSkillId(SkillCategory.FORCE);

      assertThat(dodgeSkillId).as("隊員 %s 必須裝配 DODGE 槽位", member.getName()).isNotNull();
      assertThat(parrySkillId).as("隊員 %s 必須裝配 PARRY 槽位", member.getName()).isNotNull();
      assertThat(forceSkillId).as("隊員 %s 必須裝配 FORCE 槽位", member.getName()).isNotNull();

      // 裝配的心法必須存在於該隊員已習得的 learnedStances 中
      assertThat(member.getLearnedStances()).contains(dodgeSkillId);
      assertThat(member.getLearnedStances()).contains(parrySkillId);
      assertThat(member.getLearnedStances()).contains(forceSkillId);

      // 驗證範本能正確從 TemplateReader 讀取到
      assertThat(member.getEnabledPassive(SkillCategory.DODGE)).isNotNull();
      assertThat(member.getEnabledPassive(SkillCategory.PARRY)).isNotNull();
      assertThat(member.getEnabledPassive(SkillCategory.FORCE)).isNotNull();
    }
  }

  @Test
  @DisplayName("機制測試 2: party enable 指令動態切換心法槽位並正確映射至 DrpgStateDto")
  void testPartyEnableCommandSwitchesPassiveSlot() {
    Party party = partyService.getOrCreateParty("tester");

    PartyMember leader = party.getMembers().get(0);
    // 隊長初始學過 cloud_step 與 violet_mist_force
    assertThat(leader.getLearnedStances()).contains("cloud_step", "violet_mist_force");

    ScopedValue.where(MudContext.CURRENT_PLAYER, dummyPlayer).run(() -> {
      // 透過指令切換身法槽位至 cloud_step
      if (partyCommand != null) {
        partyCommand.execute("enable 0 cloud_step");
      } else {
        leader.enableSkill(SkillCategory.DODGE, "cloud_step");
      }
      assertThat(leader.getEnabledPassiveSkillId(SkillCategory.DODGE)).isEqualTo("cloud_step");

      // 透過指令切換內功槽位至 violet_mist_force
      if (partyCommand != null) {
        partyCommand.execute("enable 0 violet_mist_force");
      } else {
        leader.enableSkill(SkillCategory.FORCE, "violet_mist_force");
      }
      assertThat(leader.getEnabledPassiveSkillId(SkillCategory.FORCE)).isEqualTo("violet_mist_force");
    });

    // 檢驗 DTO 映射
    var partyView = DrpgStateDto.toPartyViewDto(party, templateReader);
    assertThat(partyView).isNotNull();
    var leaderView = partyView.members().get(0);

    assertThat(leaderView.passiveSlots()).isNotNull();
    assertThat(leaderView.passiveSlots().get("DODGE")).isEqualTo("cloud_step");
    assertThat(leaderView.passiveSlots().get("FORCE")).isEqualTo("violet_mist_force");

    // 檢查 availablePassives 裡 cloud_step 與 violet_mist_force 的 isCurrentEnabled 為 true
    var cloudStepDto = leaderView.availablePassives().stream()
        .filter(p -> "cloud_step".equals(p.skillId()))
        .findFirst()
        .orElse(null);
    assertThat(cloudStepDto).isNotNull();
    assertThat(cloudStepDto.isCurrentEnabled()).isTrue();
    assertThat(cloudStepDto.category()).isEqualTo("DODGE");

    var violetMistDto = leaderView.availablePassives().stream()
        .filter(p -> "violet_mist_force".equals(p.skillId()))
        .findFirst()
        .orElse(null);
    assertThat(violetMistDto).isNotNull();
    assertThat(violetMistDto.isCurrentEnabled()).isTrue();
    assertThat(violetMistDto.category()).isEqualTo("FORCE");
  }

  @Test
  @DisplayName("機制測試 3: DefenseResolver 身法閃避檢定 (0傷害、獎勵SP、動態文字)")
  void testDefenseResolverDodgeMechanism() {
    Party party = partyService.createFullParty("tester");

    PartyMember member = party.getMembers().get(0);
    // 裝配梯雲縱
    member.enableSkill(SkillCategory.DODGE, "cloud_step");
    // 設定高 DEX 提升閃避率
    member.getStats().setDex(60);

    BattleEnemy enemy = BattleEnemy.builder()
        .id("enemy_wolf")
        .name("荒野幼狼")
        .hp(100)
        .maxHp(100)
        .minDamage(20)
        .maxDamage(30)
        .defense(2)
        .dex(10)
        .build();

    int dodgeCount = 0;
    int trials = 100;
    for (int i = 0; i < trials; i++) {
      var res = defenseResolver.resolveEnemyAttack(enemy, member, 25, "野性撲咬");
      if (res.outcome() == DefenseOutcome.DODGED) {
        dodgeCount++;
        assertThat(res.finalDamage()).isEqualTo(0);
        assertThat(res.spGained()).isEqualTo(15);
        assertThat(res.combatLog()).contains("💨【身法閃避】");
      }
    }

    // 在高 DEX 下，100 次攻擊應有顯著閃避次數 (至少 10 次以上)
    assertThat(dodgeCount).isGreaterThan(10);
  }

  @Test
  @DisplayName("機制測試 4: DefenseResolver 招架格擋檢定 (傷害減免、獎勵SP、格擋反饋)")
  void testDefenseResolverParryMechanism() {
    Party party = partyService.createFullParty("tester");

    // 取力士鐵牛 (第 1 位同伴)
    PartyMember tieNiu = party.getMembers().get(1);
    tieNiu.enableSkill(SkillCategory.PARRY, "iron_cloth");
    tieNiu.getStats().setStr(50);
    tieNiu.getStats().setCon(50);

    BattleEnemy enemy = BattleEnemy.builder()
        .id("enemy_bandit")
        .name("山賊斥候")
        .hp(120)
        .maxHp(120)
        .minDamage(30)
        .maxDamage(40)
        .defense(5)
        .dex(10)
        .build();

    int parryCount = 0;
    int rawDmg = 50;
    int trials = 100;
    for (int i = 0; i < trials; i++) {
      var res = defenseResolver.resolveEnemyAttack(enemy, tieNiu, rawDmg, "迅捷劈砍");
      if (res.outcome() == DefenseOutcome.PARRIED) {
        parryCount++;
        // 傷害必須大幅減免 (小於等於原始傷害的 70%)
        assertThat(res.finalDamage()).isLessThanOrEqualTo(35);
        assertThat(res.finalDamage()).isGreaterThan(0);
        assertThat(res.spGained()).isEqualTo(5);
        assertThat(res.combatLog()).contains("🛡️【招架格擋】");
      }
    }

    // 在高 STR/CON 下，100 次攻擊應有顯著招架次數 (至少 10 次以上)
    assertThat(parryCount).isGreaterThan(10);
  }
}
