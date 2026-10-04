package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.BattleState;
import com.example.htmlmud.domain.dungeon.battle.DrpgCombatLoop;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.service.WorldManager;

@SpringBootTest
@ActiveProfiles("test")
public class SystemDefenseAndStabilityTest {

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private DrpgCombatLoop combatLoop;

  @Autowired
  @Qualifier("combatExecutor")
  private ExecutorService combatExecutor;

  private LivingStats createStats(int hp, int mp) {
    LivingStats s = new LivingStats();
    s.setHp(hp);
    s.setMaxHp(hp);
    s.setMp(mp);
    s.setMaxMp(mp);
    return s;
  }

  private BattleContext createStallBattleContext(String battleId, String playerName, long maxDurationMs, int maxRounds) {
    PartyMember member = PartyMember.builder()
        .id("test-stall-p")
        .name(playerName)
        .row(com.example.htmlmud.domain.party.model.RowPosition.FRONT)
        .stats(createStats(1000, 100))
        .attackIntervalMs(10000) // 超長攻擊間隔，模擬長久對峙
        .nextAttackTime(System.currentTimeMillis() + 10000)
        .alive(true)
        .build();

    Party party = Party.builder()
        .id("party-" + battleId)
        .members(new ArrayList<>(List.of(member)))
        .build();

    BattleEnemy enemy = BattleEnemy.builder()
        .id("stall-enemy")
        .templateId("taiyin:stalemate_ghost")
        .name("對峙陰魂")
        .hp(99999)
        .maxHp(99999)
        .defense(999)
        .alive(true)
        .attackIntervalMs(10000)
        .nextAttackTime(System.currentTimeMillis() + 10000)
        .build();

    return BattleContext.builder()
        .battleId(battleId)
        .playerId(playerName)
        .party(party)
        .enemies(new ArrayList<>(List.of(enemy)))
        .state(BattleState.FIGHTING)
        .maxDurationMs(maxDurationMs)
        .maxRounds(maxRounds)
        .build();
  }

  @Test
  @DisplayName("ACT-01: Room Actor 狀態單一所有與併發安全驗證")
  void testRoomActorStateModificationSafety() throws Exception {
    Room room = worldManager.getRoomActor("newbie_village:inn");
    assertThat(room).isNotNull();

    int concurrentOperations = 20;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(concurrentOperations);
    List<GameItem> createdItems = new ArrayList<>();

    for (int i = 0; i < concurrentOperations; i++) {
      GameItem item = new GameItem();
      item.setId("test-defense-item-" + i + "-" + UUID.randomUUID());
      item.setName("測試陣法殘片" + i);
      item.setType(ItemType.MISC);
      createdItems.add(item);
    }

    // 20 個虛擬執行緒併發 dropItem
    for (int i = 0; i < concurrentOperations; i++) {
      final int idx = i;
      Thread.ofVirtual().start(() -> {
        try {
          startLatch.await();
          room.dropItem(createdItems.get(idx));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "併發 dropItem 應在 5 秒內完成");

    // 驗證全部物品已被安全納入
    List<GameItem> currentItems = room.getItems();
    for (GameItem item : createdItems) {
      assertThat(currentItems).anyMatch(it -> it.getId().equals(item.getId()));
    }

    // 測試 removeItemAsync 同步確認通道
    CountDownLatch removeLatch = new CountDownLatch(concurrentOperations);
    for (GameItem item : createdItems) {
      room.removeItemAsync(item.getId()).thenAccept(removed -> {
        removeLatch.countDown();
      });
    }

    assertTrue(removeLatch.await(5, TimeUnit.SECONDS), "併發 removeItemAsync 應在 5 秒內完成");
    assertThat(room.getItems()).noneMatch(it -> it.getId().startsWith("test-defense-item-"));
  }

  @Test
  @DisplayName("ACT-02: Spring 託管 combatExecutor 成功注入與治理驗證")
  void testCombatExecutorBeanInjected() {
    assertThat(combatExecutor).isNotNull();
    assertThat(combatLoop.getCombatExecutor()).isNotNull();
  }

  @Test
  @DisplayName("ACT-02: 戰鬥超時斷路器 (Circuit Breaker) 觸發驗證：超時後強制切換 TIMEOUT 並自動清理")
  void testCombatLoopCircuitBreakerMaxDuration() throws Exception {
    Map<String, BattleContext> activeBattles = new ConcurrentHashMap<>();
    String playerName = "超時熔斷測試者";
    // 設置最大持續時間為 200 毫秒
    BattleContext ctx = createStallBattleContext("b-timeout-1", playerName, 200L, 100);
    activeBattles.put(playerName, ctx);

    Future<?> future = combatLoop.startBattleLoop(null, ctx, null, activeBattles, null);
    assertThat(future).isNotNull();

    // 等待超過 200ms (例如等待 1 秒)，斷路器應被觸發
    Thread.sleep(1000);

    // 驗證戰鬥狀態為 TIMEOUT 且 isOver() 為 true
    assertEquals(BattleState.TIMEOUT, ctx.getState(), "超時後戰鬥狀態必須被斷路器標記為 TIMEOUT");
    assertTrue(ctx.isOver(), "戰鬥必須判定為已結束");

    // 驗證 activeBattles 已乾淨移除
    assertThat(activeBattles.containsKey(playerName)).isFalse();
    assertTrue(future.isDone(), "戰鬥循環執行緒應已終止退出");
  }

  @Test
  @DisplayName("ACT-02: 戰鬥最大回合數斷路器 (Circuit Breaker) 觸發驗證：超過回合上限強制熔斷退出")
  void testCombatLoopCircuitBreakerMaxRounds() throws Exception {
    Map<String, BattleContext> activeBattles = new ConcurrentHashMap<>();
    String playerName = "回合熔斷測試者";
    // 設置最大回合數為 2 回合，超時為 5 分鐘
    BattleContext ctx = createStallBattleContext("b-rounds-1", playerName, 300_000L, 2);
    activeBattles.put(playerName, ctx);

    Future<?> future = combatLoop.startBattleLoop(null, ctx, null, activeBattles, null);
    assertThat(future).isNotNull();

    // 等待 2 回合 (每回合約 500ms sleep，等待 1.5 秒即可)
    Thread.sleep(1500);

    assertEquals(BattleState.TIMEOUT, ctx.getState(), "超過最大回合數後戰鬥狀態必須標記為 TIMEOUT");
    assertTrue(ctx.isOver(), "戰鬥必須判定為已結束");
    assertThat(activeBattles.containsKey(playerName)).isFalse();
    assertTrue(future.isDone(), "戰鬥循環執行緒應已終止退出");
  }

  @Test
  @DisplayName("ACT-02: 執行緒中斷協調取消驗證：cancelBattle 即刻中斷戰鬥循環並清理資源")
  void testCombatLoopInterruptionGovernance() throws Exception {
    Map<String, BattleContext> activeBattles = new ConcurrentHashMap<>();
    String playerName = "中斷治理測試者";
    // 設置正常時限
    BattleContext ctx = createStallBattleContext("b-interrupt-1", playerName, 300_000L, 100);
    activeBattles.put(playerName, ctx);

    Future<?> future = combatLoop.startBattleLoop(null, ctx, null, activeBattles, null);
    assertThat(future).isNotNull();

    // 執行緒啟動後立即請求取消
    Thread.sleep(200);
    ctx.cancelBattle();

    // 等待執行緒響應中斷
    Thread.sleep(400);

    assertTrue(future.isDone(), "戰鬥循環執行緒在收到 cancel 後應及時退出");
    assertThat(activeBattles.containsKey(playerName)).isFalse();
  }

  @Test
  @DisplayName("Combat-02: 玩家無操作放置超時 (AFK Idle Timeout) 熔斷驗證")
  void testCombatLoopIdleTimeoutGuard() throws Exception {
    Map<String, BattleContext> activeBattles = new ConcurrentHashMap<>();
    String playerName = "放置超時測試者";
    // 設置 maxDurationMs 很大 (5 分鐘)，但 maxIdleDurationMs 為 300ms
    BattleContext ctx = createStallBattleContext("b-idle-1", playerName, 300_000L, 50_000);
    ctx.setMaxIdleDurationMs(300L);
    activeBattles.put(playerName, ctx);

    Future<?> future = combatLoop.startBattleLoop(null, ctx, null, activeBattles, null);
    assertThat(future).isNotNull();

    // 等待 800ms 超過 300ms 放置門檻
    Thread.sleep(800);

    assertEquals(BattleState.TIMEOUT, ctx.getState(), "超過無操作放置時間後戰鬥狀態必須標記為 TIMEOUT");
    assertTrue(ctx.isOver(), "戰鬥必須判定為已結束");
    assertThat(activeBattles.containsKey(playerName)).isFalse();
    assertTrue(future.isDone(), "戰鬥循環執行緒應因放置超時退出");
  }
}
