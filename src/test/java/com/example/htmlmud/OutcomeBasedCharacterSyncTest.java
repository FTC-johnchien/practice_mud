package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.dungeon.battle.BattleOutcome;
import com.example.htmlmud.domain.dungeon.battle.BattleOutcomeApplier;
import com.example.htmlmud.domain.dungeon.battle.BattleParticipantSnapshot;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.service.CharacterSyncService;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.WorldManager;

/**
 * CANON-04 (Phase 5): 基於 Snapshot / Outcome 的角色戰鬥同步專屬測試
 */
@SpringBootTest
public class OutcomeBasedCharacterSyncTest {

  @Autowired
  private CharacterSyncService characterSyncService;

  @Autowired
  private BattleOutcomeApplier outcomeApplier;

  @Autowired
  private PartyService partyService;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  private Player player;
  private Party party;

  @BeforeEach
  void setUp() {
    outcomeApplier.clearHistory();

    MessageOutput mockOutput = new MessageOutput() {
      @Override public void sendJson(Object payload) {}
      @Override public void close() {}
      @Override public org.springframework.web.socket.WebSocketSession getSession() { return null; }
    };
    player = Player.createSinglePlayer(mockOutput, worldManager, playerService, "太虛真人");
    player.getStats().setLevel(3);
    player.getStats().setHp(120);
    player.getStats().setMaxHp(120);
    player.getStats().setMp(60);
    player.getStats().setMaxMp(60);
    player.getStats().setStr(15);
    player.getStats().setCon(14);
    player.getStats().setDex(12);
    player.getStats().setExp(200);
    player.getStats().setFreeStatPoints(1);

    party = partyService.createSoloParty("太虛真人");
  }

  @Test
  @DisplayName("CANON-04: BattleParticipantSnapshot 具備不可變性與深拷貝隔離，修改 Player 不污染快照")
  void testSnapshotImmutableIsolation() {
    // 1. 產生開戰快照
    BattleParticipantSnapshot snapshot = characterSyncService.createSnapshot(player);
    assertThat(snapshot.characterId()).isEqualTo(player.getId());
    assertThat(snapshot.name()).isEqualTo("太虛真人");
    assertThat(snapshot.level()).isEqualTo(3);
    assertThat(snapshot.hp()).isEqualTo(120);
    assertThat(snapshot.str()).isEqualTo(15);

    // 2. 模擬 Player 在 MUD 端被異步修改 (例如受到外界影響)
    player.getStats().setLevel(99);
    player.getStats().setHp(1);
    player.getStats().setStr(999);

    // 3. 驗證 Snapshot 保持原始數據不受影響
    assertThat(snapshot.level()).isEqualTo(3);
    assertThat(snapshot.hp()).isEqualTo(120);
    assertThat(snapshot.str()).isEqualTo(15);

    // 4. 將快照套用至 Leader，驗證數值源自快照而非被污染的 Player
    PartyMember leader = party.getLeader();
    characterSyncService.applySnapshotToParty(snapshot, leader);
    assertThat(leader.getStats().getLevel()).isEqualTo(3);
    assertThat(leader.getStats().getHp()).isEqualTo(120);
    assertThat(leader.getStats().getStr()).isEqualTo(15);

    // 5. 確保物件參照完全獨立，無共享 LivingStats
    assertThat(leader.getStats()).isNotSameAs(player.getStats());
  }

  @Test
  @DisplayName("CANON-04: BattleOutcomeApplier 單向套用結算，並依賴 idempotencyKey 杜絕重複發獎")
  void testBattleOutcomeIdempotencyAndApplication() {
    int initialExp = player.getStats().getExp();
    int initialPoints = player.getStats().getFreeStatPoints();

    BattleOutcome outcome = BattleOutcome.builder()
        .battleId("battle-unique-001")
        .characterId(player.getId())
        .victory(true)
        .finalHp(90)
        .finalMp(40)
        .expGained(300)
        .freeStatPointsGained(2)
        .idempotencyKey("idempotency-key-001")
        .announcements(List.of("獲得戰鬥修為 300 點！"))
        .build();

    // 第一次套用應成功
    boolean firstResult = outcomeApplier.apply(player, outcome);
    assertThat(firstResult).isTrue();
    assertThat(player.getStats().getExp()).isEqualTo(initialExp + 300);
    assertThat(player.getStats().getFreeStatPoints()).isEqualTo(initialPoints + 2);
    assertThat(player.getStats().getHp()).isEqualTo(90);
    assertThat(player.getStats().getMp()).isEqualTo(40);

    // 第二次重複套用相同 key，必須被冪等性防護攔截 (不可重複加經驗或點數)
    boolean secondResult = outcomeApplier.apply(player, outcome);
    assertThat(secondResult).isFalse();
    assertThat(player.getStats().getExp()).isEqualTo(initialExp + 300);
    assertThat(player.getStats().getFreeStatPoints()).isEqualTo(initialPoints + 2);
  }

  @Test
  @DisplayName("CANON-04: BattleOutcome 升級判定與屬性增量單向結算")
  void testBattleOutcomeLevelUpAndStatIncreases() {
    BattleOutcome levelUpOutcome = BattleOutcome.builder()
        .battleId("battle-lvl-up")
        .characterId(player.getId())
        .victory(true)
        .leveledUp(true)
        .newLevel(4)
        .expGained(500)
        .freeStatPointsGained(2)
        .statIncreases(Map.of("STR", 2, "CON", 3))
        .idempotencyKey("idempotency-lvl-up")
        .build();

    int oldStr = player.getStats().getStr();
    int oldCon = player.getStats().getCon();

    boolean applied = outcomeApplier.apply(player, levelUpOutcome);
    assertThat(applied).isTrue();

    assertThat(player.getStats().getLevel()).isEqualTo(4);
    assertThat(player.getStats().getStr()).isEqualTo(oldStr + 2);
    assertThat(player.getStats().getCon()).isEqualTo(oldCon + 3);
  }

  @Test
  @DisplayName("CANON-04: 端到端同步閉環 (Player -> Snapshot -> Leader 戰鬥 -> Outcome -> Player)")
  void testEndToEndSyncCycle() {
    PartyMember leader = party.getLeader();

    // 1. 開戰同步
    characterSyncService.syncFromPlayerToParty(player, leader);
    assertThat(leader.getStats().getLevel()).isEqualTo(player.getStats().getLevel());
    assertThat(leader.getStats()).isNotSameAs(player.getStats());

    // 2. 模擬戰鬥結算後數值成長
    leader.getStats().setLevel(4);
    leader.getStats().setHp(80);
    leader.getStats().setExp(450);
    leader.getStats().setFreeStatPoints(3);

    // 3. 戰後結算回寫
    characterSyncService.syncFromPartyToPlayer(leader, player);

    // 4. 驗證 Player 獲得最新數值且無競態參照
    assertThat(player.getStats().getLevel()).isEqualTo(4);
    assertThat(player.getStats().getHp()).isEqualTo(80);
    assertThat(player.getStats().getExp()).isEqualTo(450);
    assertThat(player.getStats().getFreeStatPoints()).isEqualTo(3);
    assertThat(player.getStats()).isNotSameAs(leader.getStats());
  }
}
