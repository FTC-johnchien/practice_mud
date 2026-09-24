package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.BattleState;
import com.example.htmlmud.domain.dungeon.battle.DrpgCombatLoop;
import com.example.htmlmud.domain.dungeon.battle.DrpgEnemyTacticsService;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.MobRank;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.RowPosition;

@SpringBootTest
@ActiveProfiles("test")
class CombatFsmGcdAndThreatTest {

  private DrpgEnemyTacticsService tacticsService;
  private DrpgCombatLoop combatLoop;

  @BeforeEach
  void setUp() {
    tacticsService = new DrpgEnemyTacticsService();
    combatLoop = new DrpgCombatLoop(tacticsService, null);
  }

  private LivingStats createStats(int hp, int mp) {
    LivingStats s = new LivingStats();
    s.setHp(hp);
    s.setMaxHp(hp);
    s.setMp(mp);
    s.setMaxMp(mp);
    return s;
  }

  @Test
  @DisplayName("機制測試 1: 全域冷卻 (GCD) 觸發、剩餘時長與阻擋")
  void testGcdTriggerAndBlock() {
    PartyMember member = PartyMember.builder()
        .id("test-warrior")
        .name("凌霄劍客")
        .row(RowPosition.FRONT)
        .stats(createStats(100, 50))
        .build();

    assertFalse(member.isOnGcd(), "初始狀態不應處於 GCD");
    assertEquals(0L, member.getRemainingGcdMs());

    // 觸發 1200ms GCD
    member.triggerGcd(1200L);
    assertTrue(member.isOnGcd(), "觸發後應處於 GCD");
    assertTrue(member.getRemainingGcdMs() > 0, "剩餘 GCD 應大於 0");
    assertTrue(member.getRemainingGcdMs() <= 1200L, "剩餘 GCD 不應超過設定值");

    // 測試 Off-GCD 技能定義
    PartyMemberSkill offGcdSkill = PartyMemberSkill.builder()
        .id("potion_heal")
        .name("回陽救逆丸")
        .triggersGcd(false)
        .gcdMs(0L)
        .build();

    assertFalse(offGcdSkill.isTriggersGcd(), "離線 GCD 技能不應觸發 GCD");
  }

  @Test
  @DisplayName("機制測試 2: 施法唱條 (Casting FSM) 推進與完成結算")
  void testCastingStateAndCompletion() {
    PartyMember member = PartyMember.builder()
        .id("test-mage")
        .name("玄冰法師")
        .row(RowPosition.BACK)
        .stats(createStats(80, 100))
        .build();

    PartyMemberSkill heavySpell = PartyMemberSkill.builder()
        .id("thunder_strike")
        .name("九天應元雷訣")
        .castTimeMs(1500L)
        .damageMultiplier(2.5)
        .triggersGcd(true)
        .gcdMs(1200L)
        .build();

    assertFalse(member.isCasting(), "初始不應在施法中");

    // 啟動 1500ms 吟唱
    member.startCasting(heavySpell, 0, 1500L);
    assertTrue(member.isCasting(), "啟動後應在施法狀態");
    assertEquals(heavySpell, member.getCurrentCastingSkill());
    assertTrue(member.getCastingRemainingMs() > 0);
    assertEquals(1500L, member.getCastingDurationMs());

    // 建立戰鬥上下文與目標
    BattleEnemy enemy = BattleEnemy.builder()
        .id("enemy-1")
        .name("墨竹妖蜂")
        .hp(200)
        .maxHp(200)
        .defense(2)
        .alive(true)
        .row(RowPosition.FRONT)
        .build();

    Party party = Party.builder().id("p-test").members(new ArrayList<>(List.of(member))).build();
    BattleContext ctx = BattleContext.builder()
        .battleId("b-test")
        .party(party)
        .enemies(new ArrayList<>(List.of(enemy)))
        .state(BattleState.FIGHTING)
        .build();

    // 模擬唱條完成
    member.finishCasting();
    assertFalse(member.isCasting(), "完成後施法狀態應解除");

    // 施展技能效果並觸發 GCD
    combatLoop.applySkillEffects(null, ctx, member, heavySpell, 0, "");
    member.triggerGcd(heavySpell.getGcdMs());

    assertTrue(enemy.getHp() < 200, "技能效果應正確對敵人造成傷害");
    assertTrue(member.isOnGcd(), "施法完成後應進入 GCD 調息");
  }

  @Test
  @DisplayName("機制測試 3: 施法唱條遭巨額傷害或死亡打斷 (Interrupt)")
  void testCastingInterruptedByHeavyDamage() {
    PartyMember member = PartyMember.builder()
        .id("test-cleric")
        .name("小醫仙")
        .row(RowPosition.BACK)
        .stats(createStats(100, 100))
        .build();

    PartyMemberSkill healSpell = PartyMemberSkill.builder()
        .id("big_heal")
        .name("起死回生術")
        .castTimeMs(2000L)
        .interruptible(true)
        .build();

    // 1. 輕微受傷 (8 點傷害，< 15% HP) 不打斷
    member.startCasting(healSpell, -1, 2000L);
    assertTrue(member.isCasting());
    member.takeDamage(8);
    assertTrue(member.isCasting(), "小額刮痧受傷不應打斷施法");
    assertNull(member.popLastInterruptReason());

    // 2. 遭受重創 (30 點傷害，> 15% HP) 體內靈力紊亂打斷
    member.takeDamage(30);
    assertFalse(member.isCasting(), "重傷應立即打斷施法");
    assertEquals("受創打斷", member.popLastInterruptReason(), "打斷原因應標記為受創打斷");

    // 3. 致命致死傷害打斷
    member.startCasting(healSpell, -1, 2000L);
    member.takeDamage(100);
    assertFalse(member.isAlive(), "血量歸零應陣亡");
    assertFalse(member.isCasting(), "陣亡應強制中斷施法");
  }

  @Test
  @DisplayName("機制測試 4: 獨立怪物威脅表 (Threat Table)、前排權重與轉火機制")
  void testPerEnemyThreatTableAndAggroSwitching() {
    PartyMember tank = PartyMember.builder()
        .id("m-tank")
        .name("撼山力士・鐵牛")
        .row(RowPosition.FRONT)
        .stats(createStats(300, 30))
        .build();

    PartyMember healer = PartyMember.builder()
        .id("m-healer")
        .name("青囊醫仙・凌霜")
        .row(RowPosition.BACK)
        .stats(createStats(120, 150))
        .build();

    BattleEnemy boss = BattleEnemy.builder()
        .id("boss-1")
        .name("黑水玄蛇")
        .hp(1000)
        .maxHp(1000)
        .rank(MobRank.BOSS)
        .alive(true)
        .row(RowPosition.FRONT)
        .build();

    Party party = Party.builder().id("p-test").members(new ArrayList<>(List.of(tank, healer))).build();
    BattleContext ctx = BattleContext.builder()
        .battleId("b-test")
        .party(party)
        .enemies(new ArrayList<>(List.of(boss)))
        .state(BattleState.FIGHTING)
        .build();

    // 1. 鐵牛前排普攻 50 傷害 -> boss 記錄鐵牛仇恨 50
    boss.addThreat(tank.getId(), 50);
    assertEquals(50, boss.getThreat(tank.getId()));
    assertEquals(0, boss.getThreat(healer.getId()));

    // Boss 索敵：鐵牛有 50 * 1.3 = 65 有效仇恨，後排凌霜 0
    PartyMember target1 = tacticsService.selectPartyTarget(ctx, boss);
    assertEquals(tank, target1, "Boss 應鎖定仇恨最高的前排坦克");
    assertEquals(tank.getId(), boss.getTargetMemberId());
    assertEquals(tank.getName(), boss.getTargetMemberName());

    // 2. 凌霜後排施展巨額治療，產生 120 點威脅度
    boss.addThreat(healer.getId(), 120);
    // 比較有效仇恨：
    // 鐵牛 (FRONT): 50 * 1.3 = 65
    // 凌霜 (BACK): 120 * 1.0 = 120 -> 凌霜 OT！
    PartyMember target2 = tacticsService.selectPartyTarget(ctx, boss);
    assertEquals(healer, target2, "醫修治療 OT，Boss 應轉火鎖定後排醫仙！");
    assertEquals(healer.getId(), boss.getTargetMemberId());

    // 3. 鐵牛爆發金剛嘲諷 (Taunt)
    boss.setTaunt(tank.getId(), 5000L);
    assertTrue(boss.isTaunted(), "Boss 應處於被嘲諷狀態");
    // 驗證嘲諷鞏固仇恨：鐵牛仇恨拉升至 max(top + 100, current + 100) = 120 + 100 = 220
    assertTrue(boss.getThreat(tank.getId()) >= 220, "嘲諷應使坦克的威脅度躍居全場第一");

    PartyMember target3 = tacticsService.selectPartyTarget(ctx, boss);
    assertEquals(tank, target3, "被嘲諷後 Boss 應強行鎖定坦克鐵牛！");
  }

  @Test
  @DisplayName("機制測試 5: 多怪情境下治療威脅分攤 (Healing Threat Splitting)")
  void testHealingThreatSplittingAcrossEnemies() {
    PartyMember healer = PartyMember.builder()
        .id("m-healer")
        .name("凌霜")
        .row(RowPosition.BACK)
        .stats(createStats(100, 100))
        .build();

    BattleEnemy mob1 = BattleEnemy.builder().id("mob-1").name("妖狼甲").hp(100).maxHp(100).alive(true).build();
    BattleEnemy mob2 = BattleEnemy.builder().id("mob-2").name("妖狼乙").hp(100).maxHp(100).alive(true).build();

    Party party = Party.builder().id("p-test").members(new ArrayList<>(List.of(healer))).build();
    BattleContext ctx = BattleContext.builder()
        .battleId("b-test")
        .party(party)
        .enemies(new ArrayList<>(List.of(mob1, mob2)))
        .state(BattleState.FIGHTING)
        .build();

    PartyMemberSkill aoeHeal = PartyMemberSkill.builder()
        .id("aoe_heal")
        .name("甘霖普降")
        .heal(true)
        .aoe(true)
        .healAmount(60)
        .cooldownMs(5000L)
        .build();

    combatLoop.applySkillEffects(null, ctx, healer, aoeHeal, -1, "");

    // 治療 60 HP -> 總威脅 60，兩隻存活怪各均攤 30 威脅
    assertTrue(mob1.getThreat(healer.getId()) >= 25, "妖狼甲應累積均攤之治療仇恨");
    assertTrue(mob2.getThreat(healer.getId()) >= 25, "妖狼乙應累積均攤之治療仇恨");
  }
}
