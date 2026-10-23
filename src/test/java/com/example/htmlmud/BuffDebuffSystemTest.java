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
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.PartyMemberSkill;
import com.example.htmlmud.party.RowPosition;
import com.example.htmlmud.party.TacticsCondition;
import com.example.htmlmud.party.TacticsRule;
import com.example.htmlmud.party.TacticsTarget;
import com.example.htmlmud.template.TemplateReader;

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

  @Test
  @DisplayName("機制 5：常駐光環 (PERMANENT_DURATION = -1) 歷經 100 Ticks 永不消退")
  void testPermanentAura_doesNotExpireAfter100Ticks() {
    LivingStats stats = new LivingStats();
    stats.setHp(100);
    stats.setMaxHp(100);
    PartyMember member = PartyMember.builder()
        .id("test-aura-member")
        .name("陣法弟子")
        .stats(stats)
        .build();

    ActiveBuff formationAura = ActiveBuff.builder()
        .id("buff_formation_slot_0")
        .name("太極陣·陣眼加成")
        .category(BuffCategory.STAT_MODIFIER)
        .durationTicks(ActiveBuff.PERMANENT_DURATION)
        .remainingTicks(ActiveBuff.PERMANENT_DURATION)
        .build();
    buffSettlementService.applyBuff(member, formationAura);

    assertThat(member.hasActiveBuff("buff_formation_slot_0")).isTrue();
    assertThat(formationAura.isPermanent()).isTrue();
    assertThat(formationAura.getRemainingSeconds()).isEqualTo(-1);

    // 歷經 100 次心跳 (50 秒)
    for (int i = 0; i < 100; i++) {
      buffSettlementService.processTicks(member);
    }

    assertThat(member.hasActiveBuff("buff_formation_slot_0")).isTrue();
    assertThat(formationAura.isExpired()).isFalse();
    assertThat(formationAura.getRemainingTicks()).isEqualTo(-1);
  }

  @Test
  @DisplayName("機制 6：多重護盾依最短剩餘時間 (Shortest Duration First) 優先抵扣，常駐護盾排最後")
  void testMultipleShields_shortestDurationFirst_and_permanentLast() {
    LivingStats stats = new LivingStats();
    stats.setHp(100);
    stats.setMaxHp(100);
    PartyMember member = PartyMember.builder()
        .id("test-tank-shields")
        .name("金剛力士")
        .stats(stats)
        .build();

    // 短期護盾：20 點，10 ticks (5s)
    ActiveBuff shortShield = ActiveBuff.builder()
        .id("buff_short_shield")
        .name("玄武護甲")
        .category(BuffCategory.SHIELD)
        .durationTicks(10)
        .remainingTicks(10)
        .value(20)
        .build();

    // 長期護盾：30 點，40 ticks (20s)
    ActiveBuff longShield = ActiveBuff.builder()
        .id("buff_long_shield")
        .name("金光神咒")
        .category(BuffCategory.SHIELD)
        .durationTicks(40)
        .remainingTicks(40)
        .value(30)
        .build();

    // 常駐護盾：50 點，-1 ticks (常駐陣法護盾)
    ActiveBuff permShield = ActiveBuff.builder()
        .id("buff_perm_shield")
        .name("厚土古陣壁")
        .category(BuffCategory.SHIELD)
        .durationTicks(ActiveBuff.PERMANENT_DURATION)
        .remainingTicks(ActiveBuff.PERMANENT_DURATION)
        .value(50)
        .build();

    buffSettlementService.applyBuff(member, shortShield);
    buffSettlementService.applyBuff(member, longShield);
    buffSettlementService.applyBuff(member, permShield);

    assertThat(member.getTotalShield()).isEqualTo(100);

    // 受到 25 點傷害：短期護盾 (20) 優先被耗盡並破裂，剩餘 5 點由長期護盾 (30) 抵扣 (剩 25)
    member.takeDamage(25);
    assertThat(member.hasActiveBuff("buff_short_shield")).isFalse();
    assertThat(member.getActiveBuff("buff_long_shield").getValue()).isEqualTo(25);
    assertThat(member.getActiveBuff("buff_perm_shield").getValue()).isEqualTo(50);
    assertThat(member.getTotalShield()).isEqualTo(75);

    // 再受到 35 點傷害：長期護盾 (25) 被耗盡破裂，剩餘 10 點由常駐護盾 (50) 抵扣 (剩 40)
    member.takeDamage(35);
    assertThat(member.hasActiveBuff("buff_long_shield")).isFalse();
    assertThat(member.getActiveBuff("buff_perm_shield").getValue()).isEqualTo(40);
    assertThat(member.getTotalShield()).isEqualTo(40);
    assertThat(member.getHp()).isEqualTo(100); // 氣血仍完好
  }

  @Test
  @DisplayName("機制 7：敵方罡氣破防 (Poise Gauge)、未破防 1/3 減傷霸體與破防力竭全額爆發")
  void testPoiseBreak_enemyDamageReduction_and_burstWindow() {
    BattleEnemy boss = BattleEnemy.builder()
        .id("enemy-boss")
        .name("宋天衡")
        .hp(200)
        .maxHp(200)
        .poise(30)
        .maxPoise(30)
        .build();

    // 1. 未破防階段：承受 30 點攻擊，折減為 1/3 (10 點傷害)，削韌扣減 15 點 Poise
    assertThat(boss.isControlled()).isFalse();
    boss.takeDamage(30);
    assertThat(boss.getHp()).isEqualTo(190);
    assertThat(boss.getPoise()).isEqualTo(15);
    assertThat(boss.isPoiseBroken()).isFalse();

    // 敵方開始蓄力大招
    boss.startIntent("⚡", "紫霄神雷", "MAGIC", "ALL", 1500, true);
    assertThat(boss.isCasting()).isTrue();

    // 2. 再受 30 點攻擊：Poise 歸零，觸發破防力竭！強制打斷大招！
    boss.takeDamage(30);
    assertThat(boss.isPoiseBroken()).isTrue();
    assertThat(boss.getPoise()).isEqualTo(0);
    assertThat(boss.isCasting()).isFalse(); // 大招意圖被打斷清除

    // 3. 破防力竭期：受到攻擊恢復 100% 全額輸出 (40 點傷害全額扣除)
    boss.takeDamage(40);
    assertThat(boss.getHp()).isEqualTo(140); // 180 - 40 = 140

    // 4. 掛載眩暈狀態：在力竭期 100% 受控！
    ActiveBuff stunBuff = ActiveBuff.builder()
        .id("debuff_stun")
        .name("紫霄雷痹")
        .category(BuffCategory.CONTROL)
        .effectType(EffectType.STUNED)
        .durationTicks(4)
        .remainingTicks(4)
        .build();
    buffSettlementService.applyBuff(boss, stunBuff);
    assertThat(boss.isControlled()).isTrue();

    // 5. 歷經 14 Ticks 力竭期心跳結束：敵方罡氣自動復甦，回滿 Poise 並重獲霸體
    for (int i = 0; i < 14; i++) {
      boss.processPoiseTick();
    }
    assertThat(boss.isPoiseBroken()).isFalse();
    assertThat(boss.getPoise()).isEqualTo(30);
    // 重獲霸體後，硬控場被霸體隔絕！
    assertThat(boss.isControlled()).isFalse();
  }

  @Test
  @DisplayName("機制 8：隊員破防力竭受傷提升 35%，補甲解控 (Armor Cleanse) 瞬間拉滿架勢解除控制")
  void testPoiseBreak_partyMemberPenalty_and_armorCleanseRecovery() {
    LivingStats stats = new LivingStats();
    stats.setHp(100);
    stats.setMaxHp(100);
    PartyMember member = PartyMember.builder()
        .id("test-tank-broken")
        .name("鐵牛")
        .stats(stats)
        .poise(100)
        .maxPoise(100)
        .build();

    // 主動觸發架勢破防
    member.triggerPoiseBreak();
    assertThat(member.isPoiseBroken()).isTrue();

    // 承受 20 點傷害，破防懲罰提升 35% -> 27 點傷害
    member.takeDamage(20);
    assertThat(member.getHp()).isEqualTo(73); // 100 - 27 = 73

    // 掛載眩暈
    ActiveBuff stun = ActiveBuff.builder()
        .id("debuff_stun_tank")
        .name("定身咒")
        .category(BuffCategory.CONTROL)
        .effectType(EffectType.STUNED)
        .durationTicks(6)
        .remainingTicks(6)
        .build();
    member.addBuff(stun);
    assertThat(member.isControlled()).isTrue();

    // 醫修施加護盾 (補甲解控 Armor Cleanse)
    ActiveBuff shield = ActiveBuff.builder()
        .id("buff_gold_armor")
        .name("不動明王罡")
        .category(BuffCategory.SHIELD)
        .durationTicks(20)
        .remainingTicks(20)
        .value(40)
        .build();
    buffSettlementService.applyBuff(member, shield);

    // 驗證：破防被強制修復，眩暈被清除！
    assertThat(member.isPoiseBroken()).isFalse();
    assertThat(member.getPoise()).isEqualTo(100);
    assertThat(member.isControlled()).isFalse();
  }

  @Test
  @DisplayName("機制 9：屬性修正 (STAT_MODIFIER) 動態加乘攻防數值與攻擊間隔 (SPEED_PCT)")
  void testStatModifiers_atk_def_speed_scaling() {
    LivingStats stats = new LivingStats();
    stats.setHp(100);
    stats.setMaxHp(100);
    PartyMember member = PartyMember.builder()
        .id("test-swordsman")
        .name("劍修")
        .stats(stats)
        .baseMinDamage(10)
        .baseMaxDamage(20)
        .baseDefense(10)
        .attackIntervalMs(2000)
        .build();

    // 掛載強化 Buff：攻擊 +30%，防禦 +50%，攻擊速度 +25% (間隔縮短)
    ActiveBuff empowerBuff = ActiveBuff.builder()
        .id("buff_taiyin_power")
        .name("天罡神武加持")
        .category(BuffCategory.STAT_MODIFIER)
        .durationTicks(20)
        .remainingTicks(20)
        .statModifiers(java.util.Map.of(
            "ATK_PCT", 0.30,
            "DEF_PCT", 0.50,
            "SPEED_PCT", 0.25
        ))
        .build();
    buffSettlementService.applyBuff(member, empowerBuff);

    assertThat(member.getEffectiveMinDamage()).isEqualTo(13); // 10 * 1.3 = 13
    assertThat(member.getEffectiveMaxDamage()).isEqualTo(26); // 20 * 1.3 = 26
    assertThat(member.getEffectiveDefense()).isEqualTo(15);   // 10 * 1.5 = 15
    assertThat(member.getEffectiveAttackIntervalMs()).isEqualTo(1500); // 2000 * (1 - 0.25) = 1500ms
  }

  @Test
  @DisplayName("機制 10：五行法術抗性 > 100% 逆流回血契約 (順成人，逆成仙)")
  void testElementalResistance_reverseCirculationHealing() {
    // 1. 物理傷害：即使抗性超標 (如 1.5)，依然受限於保底 10% 穿透 (10 點傷害)，絕不轉為回血
    var physResult = com.example.htmlmud.domain.combat.CombatRules.resolveResistance(
        100, com.example.htmlmud.domain.model.enums.DamageType.PHYSICAL, 1.5);
    assertThat(physResult.damage()).isEqualTo(10);
    assertThat(physResult.heal()).isEqualTo(0);

    // 2. 常規五行法術減傷 (抗性 40%)
    var fireNormal = com.example.htmlmud.domain.combat.CombatRules.resolveResistance(
        100, com.example.htmlmud.domain.model.enums.DamageType.FIRE, 0.40);
    assertThat(fireNormal.damage()).isEqualTo(60);
    assertThat(fireNormal.heal()).isEqualTo(0);

    // 3. 超額五行法術抗性 (火抗 125%)：承受 0 傷害，超額 25% 轉化為氣血回復！
    var fireAbsorb = com.example.htmlmud.domain.combat.CombatRules.resolveResistance(
        100, com.example.htmlmud.domain.model.enums.DamageType.FIRE, 1.25);
    assertThat(fireAbsorb.damage()).isEqualTo(0);
    assertThat(fireAbsorb.heal()).isEqualTo(25);
  }
}
