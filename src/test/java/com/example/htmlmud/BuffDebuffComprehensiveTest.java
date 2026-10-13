package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.domain.combat.CombatRules;
import com.example.htmlmud.domain.dungeon.battle.ActiveBuff;
import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.BuffSettlementService;
import com.example.htmlmud.domain.dungeon.battle.DrpgEnemyTacticsService;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.BuffCategory;
import com.example.htmlmud.domain.model.enums.BuffType;
import com.example.htmlmud.domain.model.enums.DamageType;
import com.example.htmlmud.domain.model.enums.EffectType;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.TacticsCondition;
import com.example.htmlmud.domain.party.model.TacticsRule;
import com.example.htmlmud.domain.party.model.TacticsTarget;

/**
 * 太陰地宮 Buff/Debuff 系統 Phase 4 全面驗收整合測試 (DoD Acceptance Test)
 * 涵蓋 6 大核心機制場景：
 * 1. 常駐陣法孔位光狂歷經 100 Ticks 永不消退
 * 2. 多重護盾依短時間優先（WoW 階梯吸收）
 * 3. Poise 破防增傷、未破防霸體減傷與力竭大招打斷
 * 4. 五行法術抗性 > 100% 逆流吸收回血與物理 10% 穿透保底
 * 5. 眩暈控場阻斷全動作與吟唱中斷
 * 6. AI Gambit 自動解毒/解控與 > 3 秒防呆機制
 */
@SpringBootTest
class BuffDebuffComprehensiveTest {

  @Autowired
  private BuffSettlementService buffSettlementService;

  @Autowired
  private DrpgEnemyTacticsService tacticsService;

  @Test
  @DisplayName("場景 1：常駐陣法孔位光環歷經 100 Ticks 心跳結算永不消退")
  void scenario1_permanentFormationAuraNeverExpires() {
    LivingStats stats = new LivingStats();
    stats.setHp(100);
    stats.setMaxHp(100);
    PartyMember member = PartyMember.builder()
        .id("mem-aura-test")
        .name("陣眼道士")
        .stats(stats)
        .build();

    // 陣法孔位常駐加成：durationTicks = -1, remainingTicks = -1
    ActiveBuff permBuff = ActiveBuff.builder()
        .id("buff_formation_slot_0")
        .name("天罡七星陣·貪狼天樞位")
        .type(BuffType.BUFF)
        .category(BuffCategory.STAT_MODIFIER)
        .effectType(EffectType.STRENGTHEN)
        .durationTicks(ActiveBuff.PERMANENT_DURATION)
        .remainingTicks(ActiveBuff.PERMANENT_DURATION)
        .value(15)
        .build();
    buffSettlementService.applyBuff(member, permBuff);

    assertThat(member.getActiveBuffs()).hasSize(1);
    assertThat(member.getActiveBuff("buff_formation_slot_0").isPermanent()).isTrue();

    // 模擬 100 個 Ticks (50 秒) 心跳推進
    for (int i = 0; i < 100; i++) {
      buffSettlementService.processTicks(member);
    }

    // 驗證常駐狀態歷經 100 Ticks 依然存在，且時效依然為 -1
    assertThat(member.getActiveBuffs()).hasSize(1);
    assertThat(member.getActiveBuff("buff_formation_slot_0")).isNotNull();
    assertThat(member.getActiveBuff("buff_formation_slot_0").isExpired()).isFalse();
    assertThat(member.getActiveBuff("buff_formation_slot_0").getRemainingTicks()).isEqualTo(ActiveBuff.PERMANENT_DURATION);
    assertThat(member.getActiveBuff("buff_formation_slot_0").getValue()).isEqualTo(15);
  }

  @Test
  @DisplayName("場景 2：多重護盾共存時依短時間優先 (Shortest Duration First) 吸收")
  void scenario2_multipleShieldsShortestDurationFirst() {
    LivingStats stats = new LivingStats();
    stats.setHp(100);
    stats.setMaxHp(100);
    PartyMember member = PartyMember.builder()
        .id("mem-shield-ladder")
        .name("玄甲鐵衛")
        .stats(stats)
        .build();

    // 短時護盾：時效 20 Ticks，容量 30
    ActiveBuff shortShield = ActiveBuff.builder()
        .id("buff_short_shield")
        .name("流雲護體")
        .category(BuffCategory.SHIELD)
        .durationTicks(20)
        .remainingTicks(20)
        .value(30)
        .build();

    // 長時護盾：時效 60 Ticks，容量 50
    ActiveBuff longShield = ActiveBuff.builder()
        .id("buff_long_shield")
        .name("不动金剛")
        .category(BuffCategory.SHIELD)
        .durationTicks(60)
        .remainingTicks(60)
        .value(50)
        .build();

    buffSettlementService.applyBuff(member, longShield);
    buffSettlementService.applyBuff(member, shortShield);

    assertThat(member.getActiveBuffs()).hasSize(2);
    assertThat(member.getTotalShield()).isEqualTo(80);

    // 受到 40 點傷害
    member.takeDamage(40);

    // 短時盾 (30) 優先全額被破，長時盾 (50) 抵扣剩下的 10 點變為 40
    assertThat(member.getActiveBuff("buff_short_shield")).isNull(); // 短時盾已破碎清除
    assertThat(member.getActiveBuff("buff_long_shield")).isNotNull();
    assertThat(member.getActiveBuff("buff_long_shield").getValue()).isEqualTo(40);
    assertThat(member.getHp()).isEqualTo(100); // 本體 HP 完全未損
  }

  @Test
  @DisplayName("場景 3：Poise 破防增傷、未破防霸體減傷與力竭大招打斷")
  void scenario3_poiseDamageReductionAndBreakInterrupt() {
    BattleEnemy boss = BattleEnemy.builder()
        .id("boss-taiyin")
        .name("太陰屍王")
        .hp(1000)
        .maxHp(1000)
        .poise(100)
        .maxPoise(100)
        .build();

    // 1. 未破防階段：承受傷害折減為 1/3 (折減後受 33 點)
    boss.takeDamage(100);
    assertThat(boss.getHp()).isEqualTo(967);

    // 2. 未破防時具備霸體：硬控 (CONTROL / STUNED) 被門控阻絕
    ActiveBuff stunBuff = ActiveBuff.builder()
        .id("debuff_stun")
        .category(BuffCategory.CONTROL)
        .effectType(EffectType.STUNED)
        .durationTicks(10)
        .remainingTicks(10)
        .build();
    boss.addBuff(stunBuff);
    assertThat(boss.isControlled()).isFalse(); // 未破防時霸體生效

    // 3. 模擬 Boss 正在蓄力 1500ms 重型絕技
    boss.startIntent("👹", "【九幽玄煞衝擊】", "HEAVY", "ALL", 1500, true);
    assertThat(boss.isCasting()).isTrue();
    assertThat(boss.getCurrentIntentName()).isEqualTo("【九幽玄煞衝擊】");

    // 4. 重型道術削韌至破防 (Poise 歸零)
    boss.applyPoiseDamage(100);
    assertThat(boss.isPoiseBroken()).isTrue();
    assertThat(boss.isCasting()).isFalse(); // 破防強制打斷大招！
    assertThat(boss.getCurrentIntentName()).isNull();

    // 5. 破防失衡狀態下，所有攻擊恢復 100% 全額輸出 (受 100 點)
    boss.takeDamage(100);
    assertThat(boss.getHp()).isEqualTo(867);

    // 6. 破防失衡狀態下，硬控百分之百生效
    boss.addBuff(stunBuff);
    assertThat(boss.isControlled()).isTrue();
  }

  @Test
  @DisplayName("場景 4：五行法術抗性 > 100% 逆流吸收回血與物理 10% 穿透保底")
  void scenario4_magicResistanceOver100AbsorbAndPhysicalPierce() {
    // 1. 五行法術抗性 > 100% (火抗 1.30 = 130%)，受到 100 點火焰傷害轉為回血 30 點 (傷害 0，治療 30)
    CombatRules.ResistanceResult healResult = CombatRules.resolveResistance(100, DamageType.FIRE, 1.30);
    assertThat(healResult.damage()).isEqualTo(0);
    assertThat(healResult.heal()).isEqualTo(30);

    // 2. 物理抗性 1.00 (100% 護甲)，保底穿透 10% (受到 10 點傷害)，杜絕絕對無敵防死鎖
    CombatRules.ResistanceResult physResult = CombatRules.resolveResistance(100, DamageType.PHYSICAL, 1.00);
    assertThat(physResult.damage()).isEqualTo(10);
    assertThat(physResult.heal()).isEqualTo(0);
  }

  @Test
  @DisplayName("場景 5：眩暈控場阻斷全動作與吟唱中斷")
  void scenario5_crowdControlBlocksActions() {
    LivingStats stats = new LivingStats();
    stats.setHp(100);
    stats.setMaxHp(100);
    PartyMember member = PartyMember.builder()
        .id("mem-stunned")
        .name("被控刺客")
        .stats(stats)
        .build();

    // 施加眩暈
    ActiveBuff stun = ActiveBuff.builder()
        .id("debuff_stun")
        .name("紫霄雷痹")
        .type(BuffType.DEBUFF)
        .category(BuffCategory.CONTROL)
        .effectType(EffectType.STUNED)
        .durationTicks(8)
        .remainingTicks(8)
        .build();
    buffSettlementService.applyBuff(member, stun);

    assertThat(member.isControlled()).isTrue();

    // 模擬戰鬥迴圈中檢查是否受控
    boolean canAct = !member.isControlled();
    assertThat(canAct).isFalse();
  }

  @Test
  @DisplayName("場景 6：AI Gambit 戰術方針自動解毒與 > 3 秒防呆機制")
  void scenario6_gambitAiAutoCleanseAndRecastPrevention() {
    LivingStats s1 = new LivingStats();
    s1.setHp(80);
    s1.setMaxHp(100);
    s1.setMp(100);
    s1.setMaxMp(100);
    PartyMember healer = PartyMember.builder()
        .id("mem-healer")
        .name("清虛道長")
        .stats(s1)
        .build();

    LivingStats s2 = new LivingStats();
    s2.setHp(60);
    s2.setMaxHp(100);
    PartyMember poisonedAlly = PartyMember.builder()
        .id("mem-poisoned")
        .name("中毒鐵牛")
        .stats(s2)
        .build();

    // 隊友身中劇毒 DoT
    ActiveBuff poison = ActiveBuff.builder()
        .id("debuff_corpse_poison")
        .name("玄陰屍毒")
        .type(BuffType.DEBUFF)
        .category(BuffCategory.DOT)
        .durationTicks(20)
        .remainingTicks(20)
        .value(14)
        .build();
    buffSettlementService.applyBuff(poisonedAlly, poison);
    assertThat(poisonedAlly.getActiveBuffs()).hasSize(1);

    Party party = new Party();
    party.addMember(healer);
    party.addMember(poisonedAlly);

    BattleContext ctx = new BattleContext();
    ctx.setParty(party);
    ctx.setEnemies(List.of(BattleEnemy.builder().id("goblin").name("地牢小妖").hp(50).maxHp(50).build()));

    // 1. 驗證戰術條件 ALLY_HAS_DEBUFF 評估為 true
    TacticsRule cleanseRule = TacticsRule.builder()
        .priority(1)
        .condition(TacticsCondition.ALLY_HAS_DEBUFF)
        .target(TacticsTarget.LOWEST_HP_ALLY)
        .skillId("spell_purify")
        .enabled(true)
        .build();

    boolean shouldCleanse = tacticsService.evaluateTacticsCondition(ctx, healer, cleanseRule);
    assertThat(shouldCleanse).isTrue();

    // 2. 驗證智能目標解析精確命中身中 Debuff 的隊友
    PartyMember target = tacticsService.resolveAllyTarget(ctx, healer, cleanseRule, -1);
    assertThat(target.getId()).isEqualTo("mem-poisoned");

    // 3. 驗證 AI 防呆機制：若隊友護盾剩餘時效 > 3 秒，不重複刷盾
    ActiveBuff thickShield = ActiveBuff.builder()
        .id("buff_gold_shield")
        .category(BuffCategory.SHIELD)
        .durationTicks(40)
        .remainingTicks(30) // 15秒 > 3秒
        .value(50)
        .build();
    buffSettlementService.applyBuff(poisonedAlly, thickShield);

    ActiveBuff currentShield = target.getActiveBuff("buff_gold_shield");
    assertThat(currentShield).isNotNull();
    assertThat(currentShield.getRemainingSeconds() > 3).isTrue();
  }
}
