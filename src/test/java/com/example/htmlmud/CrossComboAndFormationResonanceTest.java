package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.example.htmlmud.domain.combat.CombatRules;
import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.ComboResolver;
import com.example.htmlmud.domain.dungeon.battle.DrpgCombatLoop;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.party.model.FormationTemplate;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.RowPosition;

/**
 * Milestone F3-01 古仙陣法動態連攜合擊技與共鳴印記 (Cross Combos & Formation Resonance) 全面測試
 */
@SpringBootTest
@ActiveProfiles("test")
public class CrossComboAndFormationResonanceTest {

  @Autowired
  private DrpgCombatLoop drpgCombatLoop;

  @Autowired
  private ComboResolver comboResolver;

  @Test
  @DisplayName("F3-01-01: 未裝備陣法或陣法破碎時，無法推進古仙陣法跨隊員連攜")
  void testNoFormationCannotCrossCombo() {
    long now = System.currentTimeMillis();
    var res = CombatRules.evaluateCrossComboStep(
        false, 0, null, "p-1", "劍修", "e-1", null, 0L, now, 100, 10);
    assertFalse(res.chainAdvanced(), "未啟用陣法時不得開啟連攜");
    assertEquals(0, res.newStep());
  }

  @Test
  @DisplayName("F3-01-02: 陣法先導起手、隊友接力推進階數與數值遞增檢定 (Step 1 -> 2 -> 3 -> 4)")
  void testCrossComboProgressionSteps() {
    long now = System.currentTimeMillis();
    long window = now + 3500L;

    // 1. 隊員 A 率先破陣，開啟第 1 階先導位
    var step1 = CombatRules.evaluateCrossComboStep(
        true, 0, null, "p-1", "巨力戰士", "e-boss", null, 0L, now, 100, 10);
    assertTrue(step1.chainAdvanced());
    assertEquals(1, step1.newStep(), "先導起手應為第 1 階");
    assertEquals(0, step1.bonusDamage());
    assertFalse(step1.finisherReady());

    // 2. 隊員 A 自己連續出招，不能自我連攜
    var selfCombo = CombatRules.evaluateCrossComboStep(
        true, 1, "p-1", "p-1", "巨力戰士", "e-boss", "e-boss", window, now + 500L, 100, 10);
    assertFalse(selfCombo.chainAdvanced(), "同一隊員不能與自己連攜");
    assertEquals(1, selfCombo.newStep());

    // 3. 隊員 B 接力出招 -> 觸發【雙星合璧・連攜 2 階】(+25% 傷害)
    var step2 = CombatRules.evaluateCrossComboStep(
        true, 1, "p-1", "p-2", "疾風刺客", "e-boss", "e-boss", window, now + 800L, 100, 10);
    assertTrue(step2.chainAdvanced());
    assertEquals(2, step2.newStep());
    assertEquals(25, step2.bonusDamage(), "第 2 階應額外獲得 25% 傷害加成");
    assertEquals(15, step2.bonusPoiseDamage(), "削韌應提升 15");

    // 4. 隊員 C 接力出招 -> 觸發【三才混元・連攜 3 階】(+50% 傷害，全隊回穩 5 點 SAN)
    var step3 = CombatRules.evaluateCrossComboStep(
        true, 2, "p-2", "p-3", "雷法天尊", "e-boss", "e-boss", window, now + 1500L, 100, 10);
    assertTrue(step3.chainAdvanced());
    assertEquals(3, step3.newStep());
    assertEquals(50, step3.bonusDamage(), "第 3 階應額外獲得 50% 傷害加成");
    assertEquals(5, step3.sanRestored(), "第 3 階應滋養全隊 5 點道心");

    // 5. 隊員 D 接力出招 -> 觸發【四象誅仙・極意 4 階】(+100% 翻倍傷害，陣眼終結技就緒)
    var step4 = CombatRules.evaluateCrossComboStep(
        true, 3, "p-3", "p-4", "素問醫仙", "e-boss", "e-boss", window, now + 2200L, 100, 10);
    assertTrue(step4.chainAdvanced());
    assertEquals(4, step4.newStep());
    assertEquals(100, step4.bonusDamage(), "第 4 階應獲得 100% 傷害翻倍");
    assertEquals(10, step4.sanRestored(), "第 4 階應滋養全隊 10 點道心");
    assertTrue(step4.finisherReady(), "第 4 階應解鎖陣眼天外合擊終結技");
  }

  @Test
  @DisplayName("F3-01-03: 時間窗口逾時或轉移目標時，連攜隊列應自動重置並由當前隊員重新起手")
  void testCrossComboResetOnTimeoutOrTargetSwitch() {
    long now = System.currentTimeMillis();
    long expiredWindow = now - 100L; // 已逾時

    // 逾時攻擊
    var timeoutRes = CombatRules.evaluateCrossComboStep(
        true, 3, "p-2", "p-3", "雷法天尊", "e-boss", "e-boss", expiredWindow, now, 100, 10);
    assertTrue(timeoutRes.chainAdvanced());
    assertEquals(1, timeoutRes.newStep(), "逾時後應自動重置為第 1 階先導位");

    // 換目標攻擊
    var switchRes = CombatRules.evaluateCrossComboStep(
        true, 3, "p-2", "p-3", "雷法天尊", "e-minion", "e-boss", now + 3000L, now, 100, 10);
    assertTrue(switchRes.chainAdvanced());
    assertEquals(1, switchRes.newStep(), "更換目標後應自動重置為第 1 階先導位");
  }

  @Test
  @DisplayName("F3-01-04: 陣法共鳴點數不足時無法召喚天外合擊，滿 3 點以上爆發全場毀滅天威並全隊回血回 SAN")
  void testFormationFinisherExecution() {
    Party party = new Party();
    FormationTemplate formation = new FormationTemplate();
    formation.setId("formation_four_symbols");
    formation.setName("四象辟邪陣");
    party.setEquippedFormation(formation);

    LivingStats leaderStats = new LivingStats();
    leaderStats.setHp(200);
    leaderStats.setMaxHp(200);
    PartyMember leader = PartyMember.builder()
        .id("p-leader")
        .name("凌霜")
        .stats(leaderStats)
        .currentSan(80)
        .maxSan(100)
        .baseMinDamage(30)
        .baseMaxDamage(50)
        .alive(true)
        .build();
    party.addMember(leader);

    BattleEnemy boss = BattleEnemy.builder()
        .id("e-boss")
        .name("太陰邪屍")
        .hp(1000)
        .maxHp(1000)
        .poise(100)
        .maxPoise(100)
        .alive(true)
        .build();

    BattleContext ctx = new BattleContext();
    ctx.setParty(party);
    ctx.setEnemies(new ArrayList<>(List.of(boss)));

    // 1. 共鳴點數為 1 時，無法施展
    ctx.setResonancePoints(1);
    boolean failed = comboResolver.executeFormationFinisher(null, ctx, leader, null);
    assertFalse(failed, "共鳴點數不足 3 點不可發動天外合擊");

    // 2. 共鳴點數達到 4 點時，引爆天外終結合擊！
    ctx.setResonancePoints(4);
    int initialHp = boss.getHp();
    boolean success = comboResolver.executeFormationFinisher(null, ctx, leader, null);

    assertTrue(success, "天外合擊應成功發動");
    assertEquals(0, ctx.getResonancePoints(), "發動後共鳴點數應被消耗歸零");
    assertEquals(0, ctx.getCrossComboStep(), "連攜隊列應被重置");
    assertTrue(boss.getHp() < initialHp, "Boss 應受到巨額天外合擊傷害");
    assertEquals(95, leader.getCurrentSan(), "隊長道心應滋養 +15 SAN");
  }

  @Test
  @DisplayName("F3-01-05: 戰鬥迴圈整合測試 - 兩名隊員先後施展技能接力觸發【雙星合璧】，傷害暴增且共鳴點數攀升")
  void testCombatLoopCrossComboIntegration() {
    Party party = new Party();
    FormationTemplate formation = new FormationTemplate();
    formation.setId("formation_four_symbols");
    formation.setName("四象辟邪陣");
    party.setEquippedFormation(formation);

    LivingStats warriorStats = new LivingStats();
    warriorStats.setHp(200);
    warriorStats.setMaxHp(200);
    PartyMember warrior = PartyMember.builder()
        .id("p-warrior")
        .name("巨力戰士")
        .stats(warriorStats)
        .baseMinDamage(40)
        .baseMaxDamage(40)
        .row(RowPosition.FRONT)
        .alive(true)
        .build();

    LivingStats rogueStats = new LivingStats();
    rogueStats.setHp(150);
    rogueStats.setMaxHp(150);
    PartyMember rogue = PartyMember.builder()
        .id("p-rogue")
        .name("疾風刺客")
        .stats(rogueStats)
        .baseMinDamage(40)
        .baseMaxDamage(40)
        .row(RowPosition.FRONT)
        .alive(true)
        .build();

    party.addMember(warrior);
    party.addMember(rogue);

    BattleEnemy boss = BattleEnemy.builder()
        .id("e-target-boss")
        .name("守墓道奴")
        .hp(800)
        .maxHp(800)
        .defense(0)
        .poise(100)
        .maxPoise(100)
        .alive(true)
        .build();

    BattleContext ctx = new BattleContext();
    ctx.setParty(party);
    ctx.setEnemies(new ArrayList<>(List.of(boss)));

    PartyMemberSkill skill1 = PartyMemberSkill.builder()
        .id("skill_strike_1")
        .name("裂山斬")
        .damageMultiplier(1.0)
        .build();

    PartyMemberSkill skill2 = PartyMemberSkill.builder()
        .id("skill_strike_2")
        .name("疾風突刺")
        .damageMultiplier(1.0)
        .build();

    // 1. 戰士率先出招（先導 Step 1）
    drpgCombatLoop.applySkillEffects(null, ctx, warrior, skill1, 0, "【起式】");
    assertEquals(1, ctx.getCrossComboStep(), "戰士出招後應開啟連攜 Step 1");
    assertEquals("p-warrior", ctx.getCrossComboLastActorId());
    assertEquals("e-target-boss", ctx.getCrossComboTargetId());
    assertEquals(1, ctx.getResonancePoints());

    int bossHpAfterStep1 = boss.getHp();

    // 2. 刺客接力出招（連攜 Step 2 雙星合璧）
    drpgCombatLoop.applySkillEffects(null, ctx, rogue, skill2, 0, "【接力】");
    assertEquals(2, ctx.getCrossComboStep(), "刺客接力後應推進至連攜 Step 2");
    assertEquals("p-rogue", ctx.getCrossComboLastActorId());
    assertEquals(2, ctx.getResonancePoints(), "陣法共鳴度應累積至 2 點");

    int damageDealtInStep2 = bossHpAfterStep1 - boss.getHp();
    // 驗證 Step 2 成功擊中並造成傷害 (包含雙星合璧加成)
    assertTrue(damageDealtInStep2 >= 15, "Step 2 應包含雙星合璧加成傷害 (預期 >= 15, 實際: " + damageDealtInStep2 + ")");
  }
}
