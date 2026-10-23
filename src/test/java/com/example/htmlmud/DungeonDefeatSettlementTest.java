package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.WebSocketSession;

import com.example.htmlmud.application.command.impl.SaveCommand;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.service.PlayerService;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.BattleState;
import com.example.htmlmud.domain.dungeon.battle.DrpgRewardService;
import com.example.htmlmud.dungeon.DungeonFloor;
import com.example.htmlmud.dungeon.DungeonPosition;
import com.example.htmlmud.dungeon.DungeonTile;
import com.example.htmlmud.dungeon.DungeonManager;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.PartyService;
import com.example.htmlmud.save.SaveGameService;
import com.example.htmlmud.domain.world.GameStateBroadcastService;
import com.example.htmlmud.domain.world.WorldManager;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
class DungeonDefeatSettlementTest {

  @Autowired
  private DrpgRewardService rewardService;

  @Autowired
  private SaveGameService saveGameService;

  @Autowired
  private PartyService partyService;

  @Autowired
  private DungeonManager dungeonManager;

  @Autowired
  private SaveCommand saveCommand;

  @Autowired
  private GameStateBroadcastService broadcastService;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String testPlayerId = "defeat_test_player";
  private final List<String> capturedMessages = new CopyOnWriteArrayList<>();
  private Player player;

  @BeforeEach
  void setUp() {
    saveGameService.init();
    saveGameService.deleteSave(0);
    saveGameService.deleteSave(1);

    capturedMessages.clear();
    MessageOutput mockOutput = new MessageOutput() {
      @Override
      public void sendJson(Object payload) {
        if (payload != null) {
          try {
            capturedMessages.add(objectMapper.writeValueAsString(payload));
          } catch (Exception e) {
            capturedMessages.add(payload.toString());
          }
        }
      }

      @Override
      public void close() {}

      @Override
      public WebSocketSession getSession() {
        return null;
      }
    };

    player = Player.createSinglePlayer(mockOutput, worldManager, playerService, testPlayerId);
    player.setCurrentRoomId("newbie_village:inn");
    player.setInDungeon(false);
    player.start();
    worldManager.addLivingActor(player);

    partyService.resetParty(testPlayerId, testPlayerId);
  }

  @AfterEach
  void tearDown() {
    if (player != null) {
      player.stop();
    }
    saveGameService.deleteSave(0);
    saveGameService.deleteSave(1);
  }

  @Test
  @DisplayName("Task C2-1 (M2-3): 地牢全滅戰敗結算——自動遣返營地、虛弱復甦30%氣血、扣除20點SAN並觸發AutoSave")
  void testDungeonWipeDefeatSettlement() {
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      player.setInDungeon(true);
      player.setCurrentRoomId("mozhu_mines:mine_entrance");

      DungeonPosition pos = dungeonManager.getOrCreatePosition(testPlayerId, "mozhu_mines_b1f");
      // 移入地牢深處
      pos.setCoord(5, 5);

      Party party = partyService.getOrCreateParty(testPlayerId);
      for (PartyMember member : party.getMembers()) {
        member.getStats().setHp(0);
        member.setAlive(false);
        member.setCurrentSan(80);
        member.getStats().setSan(80);
      }

      Map<String, BattleContext> activeBattles = new ConcurrentHashMap<>();
      BattleContext ctx = BattleContext.builder()
          .battleId("battle-wipe-1")
          .party(party)
          .state(BattleState.DEFEAT)
          .enemies(new ArrayList<>())
          .build();
      activeBattles.put(testPlayerId, ctx);

      // 執行全滅結算
      rewardService.resolveDefeat(player, ctx, pos, activeBattles, p -> broadcastService.broadcastState(p));

      // 1. 戰鬥已自活躍清單移除
      assertThat(activeBattles).doesNotContainKey(testPlayerId);

      // 2. 玩家狀態脫離地牢，傳送回營地客棧/入口
      assertFalse(player.isInDungeon(), "全滅後玩家應脫離地牢 (inDungeon = false)");
      assertThat(player.getCurrentRoomId()).isIn("mozhu_mines:mine_entrance", "newbie_village:inn");

      // 3. 地牢座標已重置至樓層起始點，防止下次進入直接死鎖在深淵
      DungeonFloor floor = dungeonManager.getFloor("mozhu_mines_b1f");
      assertThat(pos.getX()).isEqualTo(floor.getStartCoord().x());
      assertThat(pos.getY()).isEqualTo(floor.getStartCoord().y());

      // 4. 全體隊員虛弱復甦 (30% HP，活著，SAN 扣除 20 點)
      for (PartyMember member : party.getMembers()) {
        assertTrue(member.isAlive(), "隊員應已重塑生機復活");
        int expectedHp = Math.max(20, (int) (member.getStats().getMaxHp() * 0.3));
        assertThat(member.getStats().getHp()).isEqualTo(expectedHp);
        assertThat(member.getStats().getSan()).isEqualTo(60); // 80 - 20
      }

      // 5. 驗證自動存檔 (AutoSave slot 0) 已成功落地保存
      assertThat(saveGameService.readSlotSummary(0, "自動存檔").isEmpty()).isFalse();
      var autoSave = saveGameService.loadGame(testPlayerId, 0);
      assertThat(autoSave).isNotNull();
      assertThat(autoSave.isInDungeon()).isFalse();
      assertThat(autoSave.getSlotId()).isEqualTo(0);
    });
  }

  @Test
  @DisplayName("Task C2-2 (M2-4): 地牢內部手動存檔限制——走廊阻斷，石階與神秘殘碑生路允許存檔")
  void testInDungeonManualSaveRestrictions() {
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      player.setInDungeon(true);
      DungeonPosition pos = dungeonManager.getOrCreatePosition(testPlayerId, "mozhu_mines_b1f");
      DungeonFloor floor = dungeonManager.getFloor("mozhu_mines_b1f");

      // 1. 站在一般地牢走廊格 (FLOOR '.')
      pos.setCoord(1, 1);
      DungeonTile tile1 = floor.getTile(1, 1);
      assertThat(tile1.getType()).isEqualTo(DungeonTile.TileType.FLOOR);

      assertFalse(saveGameService.isManualSaveAllowed(player), "在深處走廊格應禁止手動存檔");

      capturedMessages.clear();
      saveCommand.execute("1");
      assertThat(capturedMessages).anyMatch(r -> r.contains("地脈煞氣擾動") && r.contains("唯有抵達脫離石階或安全生路"));
      assertThat(saveGameService.readSlotSummary(1, "槽位1").isEmpty()).isTrue();

      // 2. 移動至生路撤離石階 (< STAIRS_UP)
      capturedMessages.clear();
      pos.setCoord(1, 8); // mozhu_mines_b1f 初始石階位置
      DungeonTile tileStairs = floor.getTile(1, 8);
      assertThat(tileStairs.getType()).isEqualTo(DungeonTile.TileType.STAIRS_UP);

      assertTrue(saveGameService.isManualSaveAllowed(player), "在撤離石階處應允許手動存檔");

      saveCommand.execute("1 墨竹礦坑石階存檔");
      assertThat(capturedMessages).anyMatch(r -> r.contains("道印封存") && r.contains("存檔槽位 1"));
      assertThat(saveGameService.readSlotSummary(1, "槽位1").isEmpty()).isFalse();

      // 3. 移動至神秘殘碑生門 (EVENT '!')
      capturedMessages.clear();
      saveGameService.deleteSave(1);
      pos.setCoord(4, 4); // mozhu_mines_b1f 殘碑位置 (4, 4)
      DungeonTile tileEvent = floor.getTile(4, 4);
      assertThat(tileEvent.getType()).isEqualTo(DungeonTile.TileType.EVENT);

      assertTrue(saveGameService.isManualSaveAllowed(player), "在古仙殘碑生門處應允許手動存檔");

      saveCommand.execute("1 古仙殘碑入定");
      assertThat(capturedMessages).anyMatch(r -> r.contains("道印封存") && r.contains("存檔槽位 1"));
      assertThat(saveGameService.readSlotSummary(1, "槽位1").isEmpty()).isFalse();

      // 4. 回到地表營地 (inDungeon = false)
      capturedMessages.clear();
      player.setInDungeon(false);
      assertTrue(saveGameService.isManualSaveAllowed(player), "地表營地隨時允許手動存檔");
    });
  }
}
