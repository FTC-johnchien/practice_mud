package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.application.command.impl.PartyCommand;
import com.example.htmlmud.application.command.impl.StatCommand;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.service.CharacterSyncService;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.WorldManager;
import com.example.htmlmud.domain.service.XpProgressionService;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;

/**
 * 3.4 主角自由潛能點 (Potential Points) 雙軌升級專屬測試
 */
@SpringBootTest
public class DualTrackPotentialPointsTest {

  @Autowired
  private XpProgressionService xpProgressionService;

  @Autowired
  private PartyService partyService;

  @Autowired
  private CharacterSyncService characterSyncService;

  @Autowired
  private StatCommand statCommand;

  @Autowired
  private PartyCommand partyCommand;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private TemplateRepository templateRepository;

  private Player player;
  private List<String> capturedReplies;

  @BeforeEach
  void setUp() {
    xpProgressionService.setFreeStatPointsPerLevel(XpProgressionService.DEFAULT_FREE_STAT_POINTS_PER_LEVEL);
    partyService.clearCache();
    capturedReplies = new java.util.concurrent.CopyOnWriteArrayList<>();

    MessageOutput mockOutput = new MessageOutput() {
      @Override public void sendJson(Object payload) {
        if (payload != null) {
          capturedReplies.add(payload.toString());
        }
      }
      @Override public void close() {}
      @Override public org.springframework.web.socket.WebSocketSession getSession() { return null; }
    };

    player = Player.createSinglePlayer(mockOutput, worldManager, playerService, "太虛劍主");
    player.start();
    LivingStats stats = player.getStats();
    stats.setLevel(1);
    stats.setExp(0);
    stats.setNextLevelExp(180);
    stats.setFreeStatPoints(0);
    stats.setHp(100);
    stats.setMaxHp(100);
    stats.setMp(50);
    stats.setMaxMp(50);
    stats.setStamina(100);
    stats.setMaxStamina(100);
    stats.setSan(100);
    stats.setMaxSan(100);
    stats.setStr(10);
    stats.setCon(10);
    stats.setDex(10);
    stats.setIntelligence(10);
    stats.setWis(10);
  }

  @Test
  @DisplayName("雙軌升級模式驗證：夥伴純依成長模板自律升級 (0 自由潛能點)，主角額外獲得自由潛能點")
  void testDualTrackProgressionCompanionVsLeader() {
    // 1. 一般隊友 (非 Leader)
    PartyMember companion = new PartyMember();
    companion.setName("阿飛");
    companion.setLeader(false);
    companion.setClassId("swordsman");
    companion.setTemplateReader(templateRepository);

    LivingStats cStats = new LivingStats();
    cStats.setLevel(1);
    cStats.setExp(0);
    cStats.setNextLevelExp(180);
    cStats.setFreeStatPoints(0);
    cStats.setHp(100);
    cStats.setMaxHp(100);
    cStats.setStr(5);
    cStats.setCon(5);
    cStats.setDex(5);
    companion.setStats(cStats);

    var companionResult = xpProgressionService.awardExp(companion, 200);
    assertTrue(companionResult.isLeveledUp());
    assertEquals(2, companionResult.newLevel());
    assertEquals(0, companionResult.freeStatPointsGained(), "夥伴升級不應獲得自由潛能點");
    assertEquals(0, companion.getFreeStatPoints(), "夥伴 freeStatPoints 必須為 0");
    assertTrue(cStats.getMaxHp() > 100, "夥伴依職業模板基礎氣血成長");

    // 2. 小隊隊長 (Leader)
    PartyMember leader = new PartyMember();
    leader.setName("太虛劍主");
    leader.setLeader(true);
    leader.setClassId("swordsman");
    leader.setTemplateReader(templateRepository);

    LivingStats lStats = new LivingStats();
    lStats.setLevel(1);
    lStats.setExp(0);
    lStats.setNextLevelExp(180);
    lStats.setFreeStatPoints(0);
    lStats.setHp(100);
    lStats.setMaxHp(100);
    lStats.setStr(5);
    lStats.setCon(5);
    lStats.setDex(5);
    leader.setStats(lStats);

    var leaderResult = xpProgressionService.awardExp(leader, 200);
    assertTrue(leaderResult.isLeveledUp());
    assertEquals(2, leaderResult.newLevel());
    assertEquals(2, leaderResult.freeStatPointsGained(), "主角/隊長每級應獲得預設 2 點自由潛能點");
    assertEquals(2, leader.getFreeStatPoints(), "隊長 freeStatPoints 應入帳 2 點");
  }

  @Test
  @DisplayName("自訂每級自由潛能點成長率驗證 (支援 +2~3 點自訂)")
  void testConfigurableFreeStatPointsPerLevel() {
    xpProgressionService.setFreeStatPointsPerLevel(3);
    assertEquals(3, xpProgressionService.getFreeStatPointsPerLevel());

    var result = xpProgressionService.awardExp(player, 200);
    assertTrue(result.isLeveledUp());
    assertEquals(3, result.freeStatPointsGained());
    assertEquals(3, player.getStats().getFreeStatPoints());
  }

  @Test
  @DisplayName("自由加點公式與數值閉環驗證 (STR, CON, DEX, INT, WIS, HP, MP)")
  void testStatAllocationMechanics() {
    LivingStats stats = player.getStats();
    stats.setFreeStatPoints(5);

    // 1. CON +1 -> con +1, maxHp +10, hp +10
    int oldHp = stats.getHp();
    int oldMaxHp = stats.getMaxHp();
    int oldCon = stats.getCon();
    boolean okCon = xpProgressionService.allocateStatPoint(player, "con", 1);
    assertTrue(okCon);
    assertEquals(oldCon + 1, stats.getCon());
    assertEquals(oldMaxHp + 10, stats.getMaxHp());
    assertEquals(oldHp + 10, stats.getHp());
    assertEquals(4, stats.getFreeStatPoints());

    // 2. INT +1 -> intelligence +1, maxMp +8, mp +8
    int oldMp = stats.getMp();
    int oldMaxMp = stats.getMaxMp();
    int oldInt = stats.getIntelligence();
    boolean okInt = xpProgressionService.allocateStatPoint(player, "int", 1);
    assertTrue(okInt);
    assertEquals(oldInt + 1, stats.getIntelligence());
    assertEquals(oldMaxMp + 8, stats.getMaxMp());
    assertEquals(oldMp + 8, stats.getMp());
    assertEquals(3, stats.getFreeStatPoints());

    // 3. STR +1 -> str +1
    int oldStr = stats.getStr();
    boolean okStr = xpProgressionService.allocateStatPoint(player, "str", 1);
    assertTrue(okStr);
    assertEquals(oldStr + 1, stats.getStr());
    assertEquals(2, stats.getFreeStatPoints());

    // 4. DEX +1 -> dex +1
    int oldDex = stats.getDex();
    boolean okDex = xpProgressionService.allocateStatPoint(player, "dex", 1);
    assertTrue(okDex);
    assertEquals(oldDex + 1, stats.getDex());
    assertEquals(1, stats.getFreeStatPoints());

    // 5. WIS +1 -> wis +1
    int oldWis = stats.getWis();
    boolean okWis = xpProgressionService.allocateStatPoint(player, "wis", 1);
    assertTrue(okWis);
    assertEquals(oldWis + 1, stats.getWis());
    assertEquals(0, stats.getFreeStatPoints());

    // 6. 點數耗盡後再加點應失敗
    boolean failOver = xpProgressionService.allocateStatPoint(player, "con", 1);
    assertFalse(failOver);

    // 7. 無效屬性加點應失敗
    stats.setFreeStatPoints(2);
    boolean failInvalid = xpProgressionService.allocateStatPoint(player, "invalid_stat", 1);
    assertFalse(failInvalid);
  }

  @Test
  @DisplayName("MUD 端 StatCommand 指令狀態面板顯示格式驗證")
  void testMudStatCommandStatusDisplay() throws Exception {
    player.getStats().setFreeStatPoints(3);

    String card = statCommand.formatPlayerStats(player);
    assertThat(card).contains("太虛劍主");
    assertThat(card).contains("未分配自由潛能點：【3】點");
    assertThat(card).contains("[STR] 力量/臂力：10 點");
    assertThat(card).contains("[CON] 根骨/體質：10 點");
    assertThat(card).contains("[DEX] 靈巧/身法：10 點");
    assertThat(card).contains("[INT] 悟性/智力：10 點");
    assertThat(card).contains("[WIS] 定力/精神：10 點");

    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      statCommand.execute("");
    });

    for (int i = 0; i < 25; i++) {
      if (capturedReplies.stream().anyMatch(s -> s.contains("道途修為・角色狀態"))) {
        break;
      }
      Thread.sleep(20);
    }

    String output = capturedReplies.stream().filter(s -> s.contains("道途修為・角色狀態")).findFirst().orElse("");
    assertThat(output).isNotEmpty();
    assertThat(output).contains("太虛劍主");
    assertThat(output).contains("未分配自由潛能點：【3】點");
  }

  @Test
  @DisplayName("雙向同步閉環：MUD StatCommand 加點即時同步至 DRPG Party 隊長實體")
  void testMudAllocationSyncsToPartyLeader() {
    Party party = partyService.getOrCreateParty(player);
    PartyMember leader = party.getLeader();
    characterSyncService.syncFromPlayerToParty(player, leader);

    player.getStats().setFreeStatPoints(2);
    leader.getStats().setFreeStatPoints(2);

    int oldCon = player.getStats().getCon();
    int oldMaxHp = player.getStats().getMaxHp();

    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      statCommand.execute("add con 1");
    });

    // 驗證 Player 實體
    assertEquals(oldCon + 1, player.getStats().getCon());
    assertEquals(oldMaxHp + 10, player.getStats().getMaxHp());
    assertEquals(1, player.getStats().getFreeStatPoints());

    // 驗證 Party 隊長實體已雙向同步
    assertEquals(player.getStats().getCon(), leader.getStats().getCon());
    assertEquals(player.getStats().getMaxHp(), leader.getStats().getMaxHp());
    assertEquals(player.getStats().getFreeStatPoints(), leader.getStats().getFreeStatPoints());
  }

  @Test
  @DisplayName("雙向同步閉環：DRPG PartyCommand 加點即時同步至 MUD Player 實體")
  void testDrpgAllocationSyncsToPlayer() {
    Party party = partyService.getOrCreateParty(player);
    PartyMember leader = party.getLeader();
    characterSyncService.syncFromPlayerToParty(player, leader);

    player.getStats().setFreeStatPoints(2);
    leader.getStats().setFreeStatPoints(2);

    int oldStr = leader.getStats().getStr();

    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      partyCommand.execute("stat add str 1");
    });

    // 驗證 Party 隊長實體
    assertEquals(oldStr + 1, leader.getStats().getStr());
    assertEquals(1, leader.getStats().getFreeStatPoints());

    // 驗證 Player 實體已同步
    assertEquals(leader.getStats().getStr(), player.getStats().getStr());
    assertEquals(leader.getStats().getFreeStatPoints(), player.getStats().getFreeStatPoints());
  }
}
