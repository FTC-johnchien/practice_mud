package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.dungeon.battle.ActiveBuff;
import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.BuffSettlementService;
import com.example.htmlmud.domain.dungeon.battle.DrpgCombatLoop;
import com.example.htmlmud.domain.dungeon.battle.DrpgEnemyTacticsService;
import com.example.htmlmud.domain.model.config.BuffConfig;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.BuffCategory;
import com.example.htmlmud.domain.model.enums.BuffType;
import com.example.htmlmud.domain.model.enums.EffectType;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.party.model.TacticsCondition;
import com.example.htmlmud.domain.party.model.TacticsRule;
import com.example.htmlmud.domain.party.model.TacticsTarget;
import com.example.htmlmud.domain.repository.TemplateReader;

/**
 * 針對 WoW 風格 Buff / Debuff 體系之核心純粹機制測試：
 * 1. 相同技能/同名 Buff 刷新持續時間且護盾取最大值（不無效累加）
 * 2. 不同技能/多重護盾共存，受創時依短時間優先（Shortest Duration First）依序吸收
 * 3. 戰術方針 AI 防呆（Gambit Recast Prevention）：目標已擁有相同未過期 Buff 時自動略過，不重複施放
 * 4. 週期性跳算（Tick Settlement）：1 Tick = 500ms，HoT 正確回血、DoT 正確扣血且過期自動清理
 */
@SpringBootTest
class BuffDebuffSystemTest {

  @Autowired
  private BuffSettlementService buffSettlementService;

  @Autowired
  private TemplateReader templateReader;

  @Test
  @DisplayName("機制 1：相同技能/同名狀態刷新持續時間，護盾取最大值不無限膨脹")
  void testSameBuff_refreshDuration_and_maxShield() {
    LivingStats stats = new LivingStats();
    stats.setHp(100);
    stats.setMaxHp(100);
    PartyMember member = PartyMember.builder()
        .id("test-tank")
        .name("測試鐵牛")
        .stats(stats)
        .build();

    // 第一次施加：辟邪護盾 40 點，持續 40 ticks
    ActiveBuff shield1 = ActiveBuff.builder()
        .id("buff_gold_shield")
        .name("金光辟邪護體")
        .category(BuffCategory.SHIELD)
        .durationTicks(40)
        .remainingTicks(40)
        .value(40)
        .build();
    buffSettlementService.applyBuff(member, shield1);

    assertThat(member.getActiveBuffs()).hasSize(1);
    assertThat(member.getActiveBuff("buff_gold_shield").getValue()).isEqualTo(40);
    assertThat(member.getActiveBuff("buff_gold_shield").getRemainingTicks()).isEqualTo(40);

    // 消耗 10 ticks (時間推移 5 秒)
    for (int i = 0; i < 10; i++) {
      buffSettlementService.processTicks(member);
    }
    assertThat(member.getActiveBuff("buff_gold_shield").getRemainingTicks()).isEqualTo(30);

    // 受到 15 點傷害，護盾剩餘 25
    member.takeDamage(15);
    assertThat(member.getActiveBuff("buff_gold_shield").getValue()).isEqualTo(25);
    assertThat(member.getHp()).isEqualTo(100); // 氣血無損

    // 第二次補施相同技能：新護盾 40 點，持續 40 ticks
    ActiveBuff shield2 = ActiveBuff.builder()
        .id("buff_gold_shield")
        .name("金光辟邪護體")
        .category(BuffCategory.SHIELD)
        .durationTicks(40)
        .remainingTicks(40)
        .value(40)
        .build();
    buffSettlementService.applyBuff(member, shield2);

    // 驗證 WoW 規則：Buff 不堆疊重複條目，持續時間重置為最大值 40 ticks，護盾值取 max(25, 40) = 40
    assertThat(member.getActiveBuffs()).hasSize(1);
    assertThat(member.getActiveBuff("buff_gold_shield").getRemainingTicks()).isEqualTo(40);
    assertThat(member.getActiveBuff("buff_gold_shield").getValue()).isEqualTo(40);
  }

  @Test
  @DisplayName("機制 2：不同技能/多重護盾共存，受擊時依短時間優先（Shortest Duration First）依序抵扣")
  void testMultipleShields_shortestDurationFirst() {
    LivingStats stats = new LivingStats();
    stats.setHp(100);
    stats.setMaxHp(100);
    PartyMember member = PartyMember.builder()
        .id("test-tank")
        .name("測試鐵牛")
        .stats(stats)
        .build();

    // 護盾 A：金身護盾 30 點，剩餘 10 ticks（較短，先過期）
    ActiveBuff shieldShort = ActiveBuff.builder()
        .id("buff_fudo_myoo")
        .name("不動明王")
        .category(BuffCategory.SHIELD)
        .durationTicks(20)
        .remainingTicks(10)
        .value(30)
        .build();

    // 護盾 B：辟邪護盾 40 點，剩餘 35 ticks（較長）
    ActiveBuff shieldLong = ActiveBuff.builder()
        .id("buff_gold_shield")
        .name("辟邪金光")
        .category(BuffCategory.SHIELD)
        .durationTicks(40)
        .remainingTicks(35)
        .value(40)
        .build();

    buffSettlementService.applyBuff(member, shieldLong);
    buffSettlementService.applyBuff(member, shieldShort);

    assertThat(member.getActiveBuffs()).hasSize(2);
    assertThat(member.getTotalShield()).isEqualTo(70);

    // 受到 45 點傷害：
    // 先由剩餘 10 ticks 的「不動明王」吸收 30 點（不動明王耗盡破碎移除）
    // 剩餘 15 點由「辟邪金光」吸收（辟邪金光剩餘 40 - 15 = 25 點）
    // 本體氣血依然保持 100 滿血！
    member.takeDamage(45);

    assertThat(member.getHp()).isEqualTo(100);
    assertThat(member.hasActiveBuff("buff_fudo_myoo")).isFalse(); // 已破碎移除
    assertThat(member.hasActiveBuff("buff_gold_shield")).isTrue();
    assertThat(member.getActiveBuff("buff_gold_shield").getValue()).isEqualTo(25);
    assertThat(member.getTotalShield()).isEqualTo(25);
  }

  @Test
  @DisplayName("機制 3：戰術方針 AI 防呆（Gambit AI）— 目標已有未過期相同 Buff 時自動略過，不重複施法")
  void testGambitAi_recastPrevention() {
    LivingStats lingStats = new LivingStats();
    lingStats.setHp(80);
    lingStats.setMaxHp(80);
    lingStats.setMp(100);
    lingStats.setMaxMp(100);
    PartyMember lingShuang = PartyMember.builder()
        .id("ling-shuang")
        .name("凌霜")
        .stats(lingStats)
        .row(RowPosition.BACK)
        .build();

    LivingStats tieStats = new LivingStats();
    tieStats.setHp(150);
    tieStats.setMaxHp(150);
    PartyMember tieNiu = PartyMember.builder()
        .id("tie-niu")
        .name("鐵牛")
        .stats(tieStats)
        .row(RowPosition.FRONT)
        .build();

    Party party = new Party();
    party.setMembers(new java.util.ArrayList<>(List.of(tieNiu, lingShuang)));

    BattleEnemy enemy = BattleEnemy.builder()
        .id("mob-1")
        .name("試煉妖獸")
        .hp(200)
        .maxHp(200)
        .alive(true)
        .build();

    BattleContext ctx = new BattleContext();
    ctx.setParty(party);
    ctx.setEnemies(new java.util.ArrayList<>(List.of(enemy)));

    // 配置金光辟邪護體招式
    PartyMemberSkill shieldSkill = PartyMemberSkill.builder()
        .id("class_cleric_bless")
        .name("金光辟邪護體")
        .shield(true)
        .buffConfig(new BuffConfig(
            "buff_gold_shield", "金光辟邪護體", "🛡️",
            BuffType.BUFF, BuffCategory.SHIELD, EffectType.SHIELD,
            20, 0, 0.25, 35, 0, 0.0, 0, 1, java.util.Map.of()
        ))
        .cooldownMs(0) // 假設冷卻已就緒
        .build();
    lingShuang.setSkills(List.of(shieldSkill));

    // 配置戰術方針：敵方數量 >= 1 時對前衛隊員施展辟邪護體
    lingShuang.clearTactics();
    lingShuang.addTacticsRule(TacticsRule.builder()
        .priority(1)
        .condition(TacticsCondition.ENEMY_COUNT_GTE)
        .conditionValue(1)
        .target(TacticsTarget.FRONT_ROW_ALLY)
        .skillId("class_cleric_bless")
        .enabled(true)
        .build());

    DrpgCombatLoop combatLoop = new DrpgCombatLoop(new DrpgEnemyTacticsService(templateReader), null, null, buffSettlementService);

    // 第一回合：鐵牛身上無 Buff，凌霜戰術方針順利觸發施展！
    boolean triggeredFirst = combatLoop.tryTriggerCompanionTactics(mock(Player.class), ctx, lingShuang, null);
    assertThat(triggeredFirst).isTrue();
    assertThat(tieNiu.hasActiveBuff("buff_gold_shield")).isTrue();

    // 第二回合：敵方數量依然 >= 1，但鐵牛身上的辟邪護盾依然有效未過期！
    // 驗證戰術 AI 自動略過該規則，防止重複施法浪費真元與出手回合
    boolean triggeredSecond = combatLoop.tryTriggerCompanionTactics(mock(Player.class), ctx, lingShuang, null);
    assertThat(triggeredSecond).isFalse();
  }

  @Test
  @DisplayName("機制 4：週期性跳算（Tick Settlement）— HoT 週期治療與 DoT 週期傷害正確結算與到期移除")
  void testHotAndDot_tickSettlement() {
    LivingStats heroStats = new LivingStats();
    heroStats.setHp(50);
    heroStats.setMaxHp(100);
    PartyMember hero = PartyMember.builder()
        .id("hero")
        .name("主角")
        .stats(heroStats) // 50 HP
        .build();

    // 掛載春風化雨 (HoT)：每 2 ticks (1 秒) 恢復 15 點氣血，持續 6 ticks (3 秒)
    ActiveBuff hot = ActiveBuff.builder()
        .id("buff_spring_heal")
        .name("春風化雨")
        .category(BuffCategory.HOT)
        .value(15)
        .tickIntervalTicks(2)
        .durationTicks(6)
        .remainingTicks(6)
        .build();

    // 掛載玄陰屍毒 (DoT)：每 2 ticks (1 秒) 造成 10 點傷害，持續 6 ticks (3 秒)
    ActiveBuff dot = ActiveBuff.builder()
        .id("debuff_corpse_poison")
        .name("玄陰屍毒")
        .category(BuffCategory.DOT)
        .value(10)
        .tickIntervalTicks(2)
        .durationTicks(6)
        .remainingTicks(6)
        .build();

    buffSettlementService.applyBuff(hero, hot);
    buffSettlementService.applyBuff(hero, dot);

    // 第 1 次 Tick (500ms)：未達 interval (2 ticks)，不跳算數值
    buffSettlementService.processTicks(hero);
    assertThat(hero.getHp()).isEqualTo(50);

    // 第 2 次 Tick (1000ms)：達到跳算門檻！
    // HoT +15，DoT -10，淨恢復 +5，HP 變為 55
    buffSettlementService.processTicks(hero);
    assertThat(hero.getHp()).isEqualTo(55);

    // 繼續推進 4 個 ticks 直至完全到期
    for (int i = 0; i < 4; i++) {
      buffSettlementService.processTicks(hero);
    }

    // 總共歷經 6 個 ticks (跳算 3 次：+45 HP, -30 HP) -> 淨加 15，總 HP = 65
    assertThat(hero.getHp()).isEqualTo(65);
    // 狀態到期已自動自身上清理
    assertThat(hero.getActiveBuffs()).isEmpty();
  }
}
