package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.combat.BattleContext;
import com.example.htmlmud.combat.BattleEnemy;
import com.example.htmlmud.combat.BattleOutcome;
import com.example.htmlmud.combat.BattleOutcomeApplier;
import com.example.htmlmud.combat.DrpgBattleService;
import com.example.htmlmud.combat.DrpgRewardService;
import com.example.htmlmud.dungeon.Direction;
import com.example.htmlmud.dungeon.DungeonPosition;
import com.example.htmlmud.domain.event.DomainEvent;
import com.example.htmlmud.domain.event.MobEvents;
import com.example.htmlmud.domain.event.PlayerEvents;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.PartyService;
import com.example.htmlmud.domain.actor.service.CharacterSyncService;
import com.example.htmlmud.domain.actor.service.LivingService;
import com.example.htmlmud.domain.actor.service.PlayerService;
import com.example.htmlmud.domain.world.WorldManager;
import com.example.htmlmud.progression.XpProgressionService;

@SpringBootTest
@Import(ActorOutcomeSyncAndEventBackboneTest.TestEventConfig.class)
public class ActorOutcomeSyncAndEventBackboneTest {

  @TestConfiguration
  static class TestEventConfig {
    @Bean
    TestEventCollector eventCollector() {
      return new TestEventCollector();
    }
  }

  static class TestEventCollector {
    private final List<DomainEvent> events = new CopyOnWriteArrayList<>();

    @EventListener
    public void onEvent(DomainEvent event) {
      events.add(event);
    }

    public List<DomainEvent> getEvents() {
      return events;
    }

    public void clear() {
      events.clear();
    }
  }

  @Autowired
  private CharacterSyncService characterSyncService;

  @Autowired
  private BattleOutcomeApplier outcomeApplier;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private LivingService livingService;

  @Autowired
  private XpProgressionService xpProgressionService;

  @Autowired
  private DrpgRewardService drpgRewardService;

  @Autowired
  private DrpgBattleService drpgBattleService;

  @Autowired
  private PartyService partyService;

  @Autowired
  private WorldFactory worldFactory;

  @Autowired
  private TestEventCollector testEventCollector;

  private Player player;

  @BeforeEach
  void setUp() {
    testEventCollector.clear();
    outcomeApplier.clearHistory();

    MessageOutput mockOutput = new MessageOutput() {
      @Override public void sendJson(Object payload) {}
      @Override public void close() {}
      @Override public org.springframework.web.socket.WebSocketSession getSession() { return null; }
    };
    player = Player.createSinglePlayer(mockOutput, worldManager, playerService, "測試主角");
    player.getStats().setLevel(1);
    player.getStats().setHp(100);
    player.getStats().setMaxHp(100);
    player.getStats().setMp(50);
    player.getStats().setMaxMp(50);
    player.getStats().setExp(0);
    player.getStats().setFreeStatPoints(0);
  }

  @AfterEach
  void tearDown() {
    if (player != null && player.isRunning()) {
      player.stop();
    }
  }

  @Test
  @DisplayName("SYNC-01: 當 Player 虛擬執行緒運行中，applyOutcome 透過 Mailbox 安全派發並完成狀態結算")
  void testActorMailboxOutcomeDeliveryWhenPlayerRunning() throws Exception {
    player.start();
    // 等待虛擬執行緒啟動
    long deadline = System.currentTimeMillis() + 2000;
    while (!player.isRunning() && System.currentTimeMillis() < deadline) {
      Thread.sleep(10);
    }
    assertThat(player.isRunning()).isTrue();

    BattleOutcome outcome = BattleOutcome.builder()
        .battleId("sync-battle-001")
        .characterId(player.getId())
        .victory(true)
        .finalHp(80)
        .finalMp(30)
        .expGained(250)
        .freeStatPointsGained(1)
        .idempotencyKey("idempotency-sync-001")
        .announcements(List.of("戰鬥結算完成"))
        .build();

    boolean applied = characterSyncService.applyOutcome(player, outcome);
    assertThat(applied).isTrue();

    // 驗證屬性已由 Actor Mailbox 循序應用
    assertThat(player.getStats().getHp()).isEqualTo(80);
    assertThat(player.getStats().getMp()).isEqualTo(30);
    assertThat(player.getStats().getExp()).isEqualTo(250);
    assertThat(player.getStats().getFreeStatPoints()).isEqualTo(1);
  }

  @Test
  @DisplayName("SYNC-01: 當 Player Actor 未啟動時，applyOutcome 自動退回同步套用，不阻塞且無死鎖")
  void testOutcomeDeliveryWhenPlayerNotRunning() {
    assertThat(player.isRunning()).isFalse();

    BattleOutcome outcome = BattleOutcome.builder()
        .battleId("sync-battle-offline")
        .characterId(player.getId())
        .victory(true)
        .finalHp(95)
        .finalMp(45)
        .expGained(100)
        .freeStatPointsGained(0)
        .idempotencyKey("idempotency-offline-001")
        .announcements(List.of("離線結算"))
        .build();

    boolean applied = characterSyncService.applyOutcome(player, outcome);
    assertThat(applied).isTrue();
    assertThat(player.getStats().getHp()).isEqualTo(95);
    assertThat(player.getStats().getExp()).isEqualTo(100);
  }

  @Test
  @DisplayName("EVENT-01: MUD 生靈死亡 (LivingService.onDeath) 應發布 MobEvents.MobDead 領域事件")
  void testEventBackboneMudMobDeadPublished() {
    String roomId = worldManager.getActiveRooms().keySet().iterator().next();
    Room testRoom = worldManager.getRoomActor(roomId);
    Mob mob = worldFactory.createMob("wild_rat");
    mob.setCurrentRoomId(roomId);
    testRoom.addMob(mob);

    livingService.onDeath(mob, player.getId());

    assertThat(testEventCollector.getEvents())
        .anyMatch(event -> event instanceof MobEvents.MobDead md
            && mob.getTemplateId().equals(md.mobId()));
  }

  @Test
  @DisplayName("EVENT-01: 經驗成長升級 (XpProgressionService.awardExp) 應發布 PlayerEvents.LevelUp 領域事件")
  void testEventBackbonePlayerLevelUpPublished() {
    // 授予足夠升級的經驗值
    long needExp = xpProgressionService.calculateNextLevelExp(1);
    xpProgressionService.awardExp(player, needExp + 50);

    assertThat(player.getStats().getLevel()).isGreaterThanOrEqualTo(2);
    assertThat(testEventCollector.getEvents())
        .anyMatch(event -> event instanceof PlayerEvents.LevelUp lu
            && (player.getId().equals(lu.playerId()) || player.getName().equals(lu.playerId()))
            && lu.newLevel() >= 2);
  }

  @Test
  @DisplayName("EVENT-01: DRPG 結算戰利品 (DrpgRewardService.resolveVictory) 應發布 PlayerEvents.ItemAcquired 領域事件")
  void testEventBackboneDrpgItemAcquiredPublished() {
    Party party = partyService.createSoloParty(player.getName());
    DungeonPosition pos = new DungeonPosition("taiyin_tomb_b1f", 1, 1, Direction.NORTH, 10, 10);
    BattleEnemy enemy = BattleEnemy.builder()
        .id("test-enemy-1")
        .templateId("taiyin_tomb:skeleton_soldier")
        .name("骷髏兵")
        .hp(0)
        .maxHp(100)
        .alive(false)
        .dropItemId("iron_sword")
        .dropItemIds(List.of("iron_sword"))
        .build();

    BattleContext ctx = BattleContext.builder()
        .battleId("battle-event-test")
        .playerId(player.getName())
        .party(party)
        .enemies(new java.util.ArrayList<>(List.of(enemy)))
        .state(com.example.htmlmud.combat.BattleState.FIGHTING)
        .build();

    drpgBattleService.resolveVictory(player, ctx, pos);

    assertThat(testEventCollector.getEvents())
        .anyMatch(event -> event instanceof PlayerEvents.ItemAcquired ia
            && "iron_sword".equals(ia.itemId())
            && ia.count() == 1);
  }
}
