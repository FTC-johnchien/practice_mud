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
import com.example.htmlmud.domain.combat.CombatRules.ElementalReaction;
import com.example.htmlmud.domain.dungeon.battle.ActiveBuff;
import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.DrpgCombatLoop;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.BuffCategory;
import com.example.htmlmud.domain.model.enums.DamageType;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.PartyMemberSkill;
import com.example.htmlmud.party.RowPosition;



/**
 * Milestone F2-05 複合靈氣連鎖反應與狀態引爆 (Elemental Reactions & Primer/Detonator) 全面測試
 */
@SpringBootTest
@ActiveProfiles("test")
public class ElementalReactionAndDetonateTest {

  @Autowired
  private DrpgCombatLoop drpgCombatLoop;

  @Test
  @DisplayName("F2-05-01: 火 + 冰/水 應觸發【氣化爆裂 (Vaporize)】造成 2.0x 傷害與 +20 削韌並消耗先導狀態")
  void testVaporizeReaction() {
    ActiveBuff frostBuff = ActiveBuff.builder()
        .id("buff_test_ice")
        .name("玄冰寒煞")
        .element("ICE")
        .category(BuffCategory.DOT)
        .durationTicks(20)
        .remainingTicks(20)
        .value(10)
        .build();

    List<ActiveBuff> buffs = new ArrayList<>(List.of(frostBuff));

    var res = CombatRules.evaluateElementalReaction(DamageType.FIRE, 100, 10, buffs);
    assertTrue(res.hasReaction());
    assertEquals(ElementalReaction.VAPORIZE, res.reaction());
    assertEquals(200, res.damage(), "火打冰應觸發 2.0x 蒸發爆裂傷害");
    assertEquals(30, res.poiseDamage(), "應增加 20 點罡氣破防");
    assertEquals("buff_test_ice", res.consumedBuffId(), "應消耗先導冰 Buff");
  }

  @Test
  @DisplayName("F2-05-02: 雷 + 水/冰 應觸發【感電傳導 (Electro-Charged)】造成 1.5x 傷害、微眩暈與行傳導")
  void testElectroChargedReaction() {
    ActiveBuff waterBuff = ActiveBuff.builder()
        .id("buff_test_water")
        .name("水煞浸潤")
        .element("WATER")
        .category(BuffCategory.DOT)
        .durationTicks(20)
        .remainingTicks(20)
        .value(10)
        .build();

    List<ActiveBuff> buffs = new ArrayList<>(List.of(waterBuff));

    var res = CombatRules.evaluateElementalReaction(DamageType.LIGHTNING, 80, 10, buffs);
    assertTrue(res.hasReaction());
    assertEquals(ElementalReaction.ELECTRO_CHARGED, res.reaction());
    assertEquals(120, res.damage(), "雷打水應觸發 1.5x 感電打擊");
    assertEquals(20, res.poiseDamage());
    assertTrue(res.chainToRow(), "感電應支援連鎖跳躍至同排");
    assertEquals(800L, res.stunDurationMs(), "應附加 800ms 微眩暈");
    assertEquals("buff_test_water", res.consumedBuffId());
  }

  @Test
  @DisplayName("F2-05-03: 聖光 + 陰煞 應觸發【太極混沌湮滅 (Chaos Annihilation)】造成 1.5x 傷害與 +50 削韌")
  void testChaosAnnihilationReaction() {
    ActiveBuff darkBuff = ActiveBuff.builder()
        .id("buff_test_dark")
        .name("九幽陰煞")
        .element("DARK")
        .category(BuffCategory.DOT)
        .durationTicks(20)
        .remainingTicks(20)
        .value(10)
        .build();

    List<ActiveBuff> buffs = new ArrayList<>(List.of(darkBuff));

    var res = CombatRules.evaluateElementalReaction(DamageType.HOLY, 100, 15, buffs);
    assertTrue(res.hasReaction());
    assertEquals(ElementalReaction.CHAOS_ANNIHILATION, res.reaction());
    assertEquals(150, res.damage());
    assertEquals(65, res.poiseDamage(), "太極湮滅應強制重創 50 點罡氣");
    assertEquals("buff_test_dark", res.consumedBuffId());
  }

  @Test
  @DisplayName("F2-05-04: 火 + 毒/木 應觸發【三昧燎原 (Combustion)】造成 1.8x 傷害")
  void testCombustionReaction() {
    ActiveBuff poisonBuff = ActiveBuff.builder()
        .id("buff_test_poison")
        .name("幽冥劇毒")
        .element("POISON")
        .category(BuffCategory.DOT)
        .durationTicks(20)
        .remainingTicks(20)
        .value(10)
        .build();

    List<ActiveBuff> buffs = new ArrayList<>(List.of(poisonBuff));

    var res = CombatRules.evaluateElementalReaction(DamageType.FIRE, 100, 10, buffs);
    assertTrue(res.hasReaction());
    assertEquals(ElementalReaction.COMBUSTION, res.reaction());
    assertEquals(180, res.damage());
    assertEquals(25, res.poiseDamage());
    assertEquals("buff_test_poison", res.consumedBuffId());
  }

  @Test
  @DisplayName("F2-05-05: 裂魂印記 (Primer) 配合引爆戰技 (Detonator) 應瞬間結算殘存 DoT 總合並轉化全場衝擊波")
  void testSoulRendPrimerDetonation() {
    ActiveBuff primerBuff = ActiveBuff.builder()
        .id("buff_soul_primer")
        .name("裂魂印記")
        .category(BuffCategory.PRIMER)
        .durationTicks(30)
        .remainingTicks(30)
        .value(30)
        .build();

    // DoT 1: 每跳 15 點傷害，週期 2 ticks (1s)，剩餘 6 ticks -> 剩 3 跳，總計 45 傷害
    ActiveBuff dot1 = ActiveBuff.builder()
        .id("buff_poison")
        .name("蝕骨劇毒")
        .category(BuffCategory.DOT)
        .tickIntervalTicks(2)
        .durationTicks(10)
        .remainingTicks(6)
        .value(15)
        .stacks(1)
        .build();

    // DoT 2: 每跳 20 點傷害，週期 4 ticks (2s)，剩餘 8 ticks -> 剩 2 跳，總計 40 傷害
    ActiveBuff dot2 = ActiveBuff.builder()
        .id("buff_burn")
        .name("烈焰灼燒")
        .category(BuffCategory.DOT)
        .tickIntervalTicks(4)
        .durationTicks(16)
        .remainingTicks(8)
        .value(20)
        .stacks(1)
        .build();

    List<ActiveBuff> targetBuffs = new ArrayList<>(List.of(primerBuff, dot1, dot2));

    // 1. 若非引爆技能，不應觸發印爆
    var nonDetonate = CombatRules.evaluateDetonation(false, 0, targetBuffs);
    assertFalse(nonDetonate.triggered());

    // 2. 引爆戰技命中
    var detonateRes = CombatRules.evaluateDetonation(true, 0, targetBuffs);
    assertTrue(detonateRes.triggered());
    // 殘存 DoT 總計 45 + 40 = 85，加上 primerBase 30 = 115 點單體重創
    assertEquals(115, detonateRes.singleTargetDamage());
    // 全場衝擊波為 115 * 0.5 = 58
    assertEquals(58, detonateRes.aoeDamage());
    assertEquals(35, detonateRes.bonusPoiseDamage());
    assertTrue(detonateRes.consumedBuffIds().contains("buff_soul_primer"));
    assertTrue(detonateRes.consumedBuffIds().contains("buff_poison"));
    assertTrue(detonateRes.consumedBuffIds().contains("buff_burn"));
  }

  @Test
  @DisplayName("F2-05-06: 戰鬥迴圈整合測試 - 重鎚引爆帶有裂魂印記之首領，瞬間消滅殘存狀態並擴散全場衝擊波")
  void testCombatLoopDetonationIntegration() {
    Party party = new Party();
    LivingStats warriorStats = new LivingStats();
    warriorStats.setHp(200);
    warriorStats.setMaxHp(200);
    PartyMember warrior = PartyMember.builder()
        .id("p-warrior")
        .name("巨力戰士")
        .stats(warriorStats)
        .baseMinDamage(30)
        .baseMaxDamage(40)
        .baseDefense(10)
        .row(RowPosition.FRONT)
        .alive(true)
        .build();

    party.addMember(warrior);

    BattleEnemy boss = BattleEnemy.builder()
        .id("e-boss")
        .name("深淵裂隙者")
        .hp(500)
        .maxHp(500)
        .defense(5)
        .poise(100)
        .maxPoise(100)
        .row(RowPosition.FRONT)
        .alive(true)
        .build();

    BattleEnemy minion = BattleEnemy.builder()
        .id("e-minion")
        .name("侍從魔蛛")
        .hp(100)
        .maxHp(100)
        .defense(2)
        .poise(40)
        .maxPoise(40)
        .row(RowPosition.BACK)
        .alive(true)
        .build();

    // 在 Boss 身上施加裂魂印記與劇毒
    boss.addBuff(ActiveBuff.builder()
        .id("buff_soul_primer")
        .name("裂魂印記")
        .category(BuffCategory.PRIMER)
        .remainingTicks(20)
        .value(30)
        .build());
    boss.addBuff(ActiveBuff.builder()
        .id("buff_poison")
        .name("腐蝕幽毒")
        .category(BuffCategory.DOT)
        .tickIntervalTicks(2)
        .remainingTicks(6)
        .value(20) // 3 跳 * 20 = 60
        .build());

    BattleContext ctx = new BattleContext();
    ctx.setParty(party);
    ctx.setEnemies(new ArrayList<>(List.of(boss, minion)));

    // 戰士施展重鎚【裂地崩山】(帶有 DETONATOR 標籤)
    PartyMemberSkill smashSkill = PartyMemberSkill.builder()
        .id("tank_smash")
        .name("裂地崩山")
        .damageMultiplier(1.0)
        .tags(List.of("ATTACK", "BLUNT", "DETONATOR"))
        .poiseDamage(20)
        .build();

    int initialMinionHp = minion.getHp();

    drpgCombatLoop.applySkillEffects(null, ctx, warrior, smashSkill, 0, "【戰術】");

    // 驗證 Boss 身上的印記與毒已被引爆消耗
    assertTrue(boss.getActiveBuffs().stream().noneMatch(b -> b.getId().equals("buff_soul_primer")),
        "裂魂印記應已被引爆移除");
    assertTrue(boss.getActiveBuffs().stream().noneMatch(b -> b.getId().equals("buff_poison")),
        "腐蝕幽毒應已被提前引爆移除");

    // 驗證全場衝擊波波及到侍從魔蛛 (minion)
    assertTrue(minion.getHp() < initialMinionHp,
        "侍從魔蛛應承受引爆所產生的全場衝擊波傷害");
  }

  @Test
  @DisplayName("F2-05-07: 戰鬥迴圈整合測試 - 雷擊命中帶有水煞之敵怪觸發【感電傳導】，電弧跳躍至同排其他敵怪")
  void testCombatLoopElectroChargedIntegration() {
    Party party = new Party();
    LivingStats mageStats = new LivingStats();
    mageStats.setHp(150);
    mageStats.setMaxHp(150);
    PartyMember mage = PartyMember.builder()
        .id("p-mage")
        .name("雷法天尊")
        .stats(mageStats)
        .baseMinDamage(40)
        .baseMaxDamage(50)
        .baseDefense(5)
        .row(RowPosition.BACK)
        .alive(true)
        .build();

    party.addMember(mage);

    BattleEnemy enemy1 = BattleEnemy.builder()
        .id("e-front-1")
        .name("前排水怪甲")
        .hp(200)
        .maxHp(200)
        .defense(0)
        .poise(50)
        .maxPoise(50)
        .row(RowPosition.FRONT)
        .alive(true)
        .build();

    BattleEnemy enemy2 = BattleEnemy.builder()
        .id("e-front-2")
        .name("前排妖魔乙")
        .hp(200)
        .maxHp(200)
        .defense(0)
        .poise(50)
        .maxPoise(50)
        .row(RowPosition.FRONT)
        .alive(true)
        .build();

    // 在 enemy1 身上施加水屬性 Buff
    enemy1.addBuff(ActiveBuff.builder()
        .id("buff_water")
        .name("水煞浸潤")
        .element("WATER")
        .category(BuffCategory.DOT)
        .remainingTicks(20)
        .value(10)
        .build());

    BattleContext ctx = new BattleContext();
    ctx.setParty(party);
    ctx.setEnemies(new ArrayList<>(List.of(enemy1, enemy2)));

    // 施展單體雷擊
    PartyMemberSkill thunderSkill = PartyMemberSkill.builder()
        .id("spell_thunder")
        .name("九天神雷")
        .damageType("LIGHTNING")
        .damageMultiplier(1.0)
        .tags(List.of("SPELL", "LIGHTNING"))
        .poiseDamage(15)
        .build();

    int initialEnemy2Hp = enemy2.getHp();

    drpgCombatLoop.applySkillEffects(null, ctx, mage, thunderSkill, 0, "【神雷】");

    // 驗證 enemy1 的水 Buff 已被感電反應消耗
    assertTrue(enemy1.getActiveBuffs().stream().noneMatch(b -> b.getId().equals("buff_water")),
        "水煞狀態應已被感電反應消耗");

    // 驗證同排的 enemy2 承受了跳躍電弧傷害
    assertTrue(enemy2.getHp() < initialEnemy2Hp,
        "同排的前排妖魔乙應承受感電傳導電弧傷害");
  }
}
