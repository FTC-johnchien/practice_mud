package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.config.Mechanics;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.entity.SkillEntry;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.service.XpProgressionService;
import com.example.htmlmud.domain.service.XpService;

public class XpProgressionServiceTest {

  private XpProgressionService service;
  private com.example.htmlmud.infra.persistence.repository.TemplateRepository templateRepository;

  @BeforeEach
  void setUp() {
    service = new XpProgressionService();
    templateRepository = new com.example.htmlmud.infra.persistence.repository.TemplateRepository();
    templateRepository.registerClass(
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
    templateRepository.registerClass(
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
    leader.setTemplateReader(templateRepository);

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
    tieNiu.setTemplateReader(templateRepository);

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

  @Test
  @DisplayName("測試技能熟練度曲線公式：50 * Lv^2 * learningDifficulty")
  void testSkillRequiredExpCurve() {
    // 標準難度 1.0
    assertEquals(50L, service.calculateSkillRequiredExp(1, 1.0));
    assertEquals(200L, service.calculateSkillRequiredExp(2, 1.0));
    assertEquals(450L, service.calculateSkillRequiredExp(3, 1.0));
    assertEquals(1250L, service.calculateSkillRequiredExp(5, 1.0));

    // 高難度 1.5 (例如進階絕學)
    assertEquals(75L, service.calculateSkillRequiredExp(1, 1.5));
    assertEquals(300L, service.calculateSkillRequiredExp(2, 1.5));
    assertEquals(1875L, service.calculateSkillRequiredExp(5, 1.5));
  }

  @Test
  @DisplayName("測試技能獲得熟練度並升級 (溢出經驗保留與上限防禦)")
  void testAwardSkillExpProgression() {
    SkillEntry entry = new SkillEntry("taiji_sword", 1);
    SkillTemplate template = new SkillTemplate();
    template.setId("taiji_sword");
    template.setName("太極劍法");
    template.setMechanics(Mechanics.builder()
        .learningDifficulty(1.0)
        .maxLevel(10)
        .build());

    // Lv.1 升 Lv.2 需 50 熟練度；灌注 75 熟練度，升至 Lv.2，剩餘 25 熟練度
    var res1 = service.awardSkillExp(entry, template, 75);
    assertTrue(res1.leveledUp());
    assertEquals(1, res1.oldLevel());
    assertEquals(2, res1.newLevel());
    assertEquals(25L, entry.getXp());
    assertEquals(200L, res1.requiredXp()); // Lv.2 升 Lv.3 需 50 * 4 = 200

    // 再灌注 175 熟練度 (25 + 175 = 200)，恰好升至 Lv.3，剩餘 0 熟練度
    var res2 = service.awardSkillExp(entry, template, 175);
    assertTrue(res2.leveledUp());
    assertEquals(3, res2.newLevel());
    assertEquals(0L, entry.getXp());
    assertEquals(450L, res2.requiredXp()); // Lv.3 升 Lv.4 需 50 * 9 = 450
  }

  @Test
  @DisplayName("測試 Player 實體經驗發放與升級閉環 (HP/MP回滿與自由修為點數)")
  void testAwardExpToPlayer() {
    Player player = mock(Player.class);
    when(player.getName()).thenReturn("太玄道人");
    when(player.isValid()).thenReturn(true);

    LivingStats stats = new LivingStats();
    stats.setLevel(1);
    stats.setExp(0);
    stats.setNextLevelExp(180);
    stats.setHp(80);
    stats.setMaxHp(100);
    stats.setMp(30);
    stats.setMaxMp(50);
    stats.setFreeStatPoints(0);
    stats.setCon(5);
    stats.setStr(5);

    when(player.getStats()).thenReturn(stats);

    // 發放 220 點經驗值，從 Lv.1 升至 Lv.2 (需 180，剩餘 40)
    var result = service.awardExp(player, 220);
    assertTrue(result.isLeveledUp());
    assertEquals(1, result.oldLevel());
    assertEquals(2, result.newLevel());
    assertEquals(2, result.freeStatPointsGained());
    assertEquals(2, stats.getFreeStatPoints());
    assertEquals(40, stats.getExp());

    // 驗證主角升級 HP/MP 回滿與基礎體質成長
    assertEquals(stats.getMaxHp(), stats.getHp());
    assertEquals(stats.getMaxMp(), stats.getMp());
    assertEquals(6, stats.getCon());
    assertEquals(6, stats.getStr());
  }

  @Test
  @DisplayName("測試怪物擊殺修為經驗計算 (等級差動態補正)")
  void testMobExpRewardCalculation() {
    // 同等級怪 (Lv.1 vs Lv.1): 15 EXP
    assertEquals(15, service.calculateMobExpReward(1, 1));

    // 高等級怪 (Lv.5 vs Lv.1): levelDiff=4 -> 5*15 + 4*5 = 75 + 20 = 95 EXP
    assertEquals(95, service.calculateMobExpReward(5, 1));

    // 低等級怪超過 5 級 (Lv.1 vs Lv.10): levelDiff=-9 -> 15 - 18 = -3，保底 5 EXP
    assertEquals(5, service.calculateMobExpReward(1, 10));
  }

  @Test
  @DisplayName("測試 XpService 向下相容轉發委託")
  void testBackwardCompatibilityXpService() {
    XpService xpService = new XpService(service);
    SkillEntry entry = new SkillEntry("taiji_sword", 2);
    SkillTemplate template = new SkillTemplate();
    template.setId("taiji_sword");
    template.setMechanics(Mechanics.builder().learningDifficulty(1.0).maxLevel(10).build());

    // Lv.2 需 50 * 4 = 200
    assertEquals(200L, xpService.getRequiredXp(entry, template));
  }
}
