package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.service.XpProgressionService;

public class XpProgressionServiceTest {

  private XpProgressionService service;

  @BeforeEach
  void setUp() {
    service = new XpProgressionService();
    com.example.htmlmud.infra.persistence.repository.TemplateRepository.registerClass(
        com.example.htmlmud.domain.model.template.ClassTemplate.builder()
            .id("swordsman")
            .name("劍客")
            .growth(new com.example.htmlmud.domain.model.template.ClassTemplate.ClassGrowth(20, 8, java.util.Map.of(
                "DEX", 2.5,
                "STR", 1.5,
                "CON", 1.0,
                "INT", 0.0,
                "WIS", 0.0
            )))
            .build());
    com.example.htmlmud.infra.persistence.repository.TemplateRepository.registerClass(
        com.example.htmlmud.domain.model.template.ClassTemplate.builder()
            .id("warrior")
            .name("豪俠")
            .growth(new com.example.htmlmud.domain.model.template.ClassTemplate.ClassGrowth(35, 0, java.util.Map.of(
                "CON", 3.0,
                "STR", 1.5,
                "DEX", 0.5,
                "INT", 0.0,
                "WIS", 0.0
            )))
            .build());
  }

  @Test
  @DisplayName("測試等級曲線公式平滑度與極限值 (Lv.1 ~ Lv.1000)")
  void testNextLevelExpCurve() {
    long xp1 = service.calculateNextLevelExp(1);
    assertEquals(180L, xp1);

    long xp2 = service.calculateNextLevelExp(2);
    assertTrue(xp2 > xp1, "Lv.2 經驗值需求應大於 Lv.1");

    long xp10 = service.calculateNextLevelExp(10);
    assertTrue(xp10 > xp2);

    long xp100 = service.calculateNextLevelExp(100);
    assertTrue(xp100 > xp10);

    long xp1000 = service.calculateNextLevelExp(1000);
    assertTrue(xp1000 > xp100);
    // 檢查數值在合理範圍 (Lv.1000 約 400 萬 EXP，在 long 與 int 範圍內)
    assertTrue(xp1000 < 10_000_000L);
  }

  @Test
  @DisplayName("測試主角升級：獲得基礎屬性與 +2 自由分配點數")
  void testLeaderLevelUpAndFreePoints() {
    PartyMember leader = new PartyMember();
    leader.setName("測試俠士");
    leader.setLeader(true);
    leader.setClassId("swordsman");

    LivingStats stats = new LivingStats();
    stats.setLevel(1);
    stats.setExp(0);
    stats.setNextLevelExp(180);
    stats.setFreeStatPoints(0);
    stats.setHp(100);
    stats.setMaxHp(100);
    stats.setMp(50);
    stats.setMaxMp(50);
    stats.setStr(5);
    stats.setDex(5);
    stats.setCon(5);
    stats.setIntelligence(5);
    stats.setWis(5);
    leader.setStats(stats);

    // 發放 200 點修為，應從 Lv.1 升至 Lv.2 (剩餘 20 EXP)
    var result = service.awardExp(leader, 200);

    assertTrue(result.isLeveledUp());
    assertEquals(1, result.oldLevel());
    assertEquals(2, result.newLevel());
    assertEquals(2, result.freeStatPointsGained());
    assertEquals(2, leader.getFreeStatPoints());
    assertEquals(20, leader.getExp());

    // 測試自由分配點數
    // 劍客升級基礎 STR +2 (5 -> 7)，手動分配 +1 STR 應為 8
    boolean ok = service.allocateStatPoint(leader, "str", 1);
    assertTrue(ok);
    assertEquals(1, leader.getFreeStatPoints());
    assertEquals(8, stats.getStr());

    // 劍客升級基礎 CON +1 (5 -> 6)，手動分配 +1 CON 應為 7
    boolean ok2 = service.allocateStatPoint(leader, "con", 1);
    assertTrue(ok2);
    assertEquals(0, leader.getFreeStatPoints());
    assertEquals(7, stats.getCon());
    // 體質加點額外提升 HP
    assertTrue(stats.getMaxHp() > 100);

    // 點數不足應分配失敗
    boolean fail = service.allocateStatPoint(leader, "dex", 1);
    assertFalse(fail);
  }

  @Test
  @DisplayName("測試同伴升級：依職業範本自適應成長，無自由點數")
  void testCompanionClassAdaptiveGrowth() {
    PartyMember tieNiu = new PartyMember();
    tieNiu.setName("鐵牛");
    tieNiu.setLeader(false);
    tieNiu.setClassId("warrior");

    LivingStats stats = new LivingStats();
    stats.setLevel(1);
    stats.setExp(0);
    stats.setNextLevelExp(180);
    stats.setFreeStatPoints(0);
    stats.setHp(150);
    stats.setMaxHp(150);
    stats.setStr(10);
    stats.setCon(10);
    tieNiu.setStats(stats);

    var result = service.awardExp(tieNiu, 180);

    assertTrue(result.isLeveledUp());
    assertEquals(2, tieNiu.getLevel());
    assertEquals(0, tieNiu.getFreeStatPoints(), "一般隊友不應獲得自由點數");
    assertTrue(tieNiu.getCon() > 10, "戰士職業體質 CON 應顯著成長");
  }

  @Test
  @DisplayName("測試跨多等級爆發升級 (Multi-level jump)")
  void testMultiLevelJump() {
    PartyMember member = new PartyMember();
    member.setName("潛力弟子");
    member.setLeader(true);
    member.setClassId("mage");

    LivingStats stats = new LivingStats();
    stats.setLevel(1);
    stats.setExp(0);
    stats.setNextLevelExp(180);
    member.setStats(stats);

    // 注入大量經驗值 (例如 5,000 EXP)
    var result = service.awardExp(member, 5000);

    assertTrue(result.isLeveledUp());
    assertTrue(result.newLevel() >= 5, "獲得 5000 EXP 應能連升數級");
    assertEquals((result.newLevel() - 1) * 2, member.getFreeStatPoints());
  }
}
