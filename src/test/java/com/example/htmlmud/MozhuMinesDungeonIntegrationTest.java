package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.application.command.impl.DungeonCommand;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.dungeon.model.Direction;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.model.DungeonTile;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.dungeon.service.DungeonNavigator;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.WorldManager;

@SpringBootTest
class MozhuMinesDungeonIntegrationTest {

  @Autowired
  private DungeonManager dungeonManager;

  @Autowired
  private DungeonNavigator dungeonNavigator;

  @Autowired
  private DrpgBattleService battleService;

  @Autowired
  private PartyService partyService;

  @Autowired
  private DungeonCommand dungeonCommand;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  private Player testPlayer;

  @BeforeEach
  void setUp() {
    MessageOutput mockOutput = new MessageOutput() {
      @Override
      public void sendJson(Object payload) {}

      @Override
      public void close() {}

      @Override
      public WebSocketSession getSession() {
        return null;
      }
    };
    testPlayer = Player.createSinglePlayer(mockOutput, worldManager, playerService, "墨竹道友");
    testPlayer.setCurrentRoomId("mozhu_mines:mine_entrance");
    testPlayer.start();
    worldManager.addLivingActor(testPlayer);
  }

  @Test
  @DisplayName("1. 驗證墨竹礦坑 B1F 網格地牢數據成功載入與地塊配置")
  void testMozhuMinesB1FLoaded() {
    DungeonFloor floor = dungeonManager.getFloor("mozhu_mines_b1f");
    assertNotNull(floor, "mozhu_mines_b1f 地牢樓層應成功載入");
    assertThat(floor.getName()).contains("墨竹山");
    assertThat(floor.getWidth()).isEqualTo(10);
    assertThat(floor.getHeight()).isEqualTo(10);

    // 起點 (1, 8) 應為 STAIRS_UP
    DungeonTile startTile = floor.getTile(1, 8);
    assertNotNull(startTile);
    assertThat(startTile.getType()).isEqualTo(DungeonTile.TileType.STAIRS_UP);

    // 首領祭壇 (4, 1) 應為 BOSS 地塊，且 eventId 為 boss_song_tianheng
    DungeonTile bossTile = floor.getTile(4, 1);
    assertNotNull(bossTile);
    assertThat(bossTile.getType()).isEqualTo(DungeonTile.TileType.BOSS);
    assertThat(bossTile.getEventId()).isEqualTo("mozhu_mines:boss_song_tianheng");

    // 寶箱 (8, 6) 應為 TREASURE 地塊，且掉落物包含 corrupted_spirit_stone
    DungeonTile chestTile = floor.getTile(8, 6);
    assertNotNull(chestTile);
    assertThat(chestTile.getType()).isEqualTo(DungeonTile.TileType.TREASURE);
    assertThat(chestTile.getDrops()).contains("mozhu_mines:corrupted_spirit_stone");

    // 泥沼 (2, 7) 應為 TRAP
    DungeonTile trapTile = floor.getTile(2, 7);
    assertNotNull(trapTile);
    assertThat(trapTile.getType()).isEqualTo(DungeonTile.TileType.TRAP);

    // 殘碑 (4, 4) 應為 EVENT
    DungeonTile eventTile = floor.getTile(4, 4);
    assertNotNull(eventTile);
    assertThat(eventTile.getType()).isEqualTo(DungeonTile.TileType.EVENT);
  }

  @Test
  @DisplayName("2. 驗證玩家於墨竹礦坑 B1F 之移動、轉向與靈識感應雷達")
  void testPlayerMovementInMozhuMines() {
    DungeonFloor floor = dungeonManager.getFloor("mozhu_mines_b1f");
    DungeonPosition pos = dungeonManager.switchFloor(testPlayer.getName(), "mozhu_mines_b1f");

    assertThat(pos.getX()).isEqualTo(1);
    assertThat(pos.getY()).isEqualTo(8);
    assertThat(pos.getFacing()).isEqualTo(Direction.NORTH);

    // 往前踏步 (1, 8) -> (1, 7)
    var stepResult = dungeonNavigator.moveForward(floor, pos);
    assertTrue(stepResult.success());
    assertThat(pos.getY()).isEqualTo(7);

    // 向右轉 (NORTH -> EAST)
    dungeonNavigator.turnRight(pos);
    assertThat(pos.getFacing()).isEqualTo(Direction.EAST);

    // 前方應為 (2, 7) 泥沼陷阱
    String inspect = dungeonNavigator.inspectForward(floor, pos);
    assertThat(inspect).contains("泥沼");
  }

  @Test
  @DisplayName("3. 驗證踏入墨竹首領祭壇觸發宋天衡 Boss 戰與戰鬥上下文屬性")
  void testBossBattleInitiation() {
    DungeonFloor floor = dungeonManager.getFloor("mozhu_mines_b1f");
    DungeonPosition pos = dungeonManager.switchFloor(testPlayer.getName(), "mozhu_mines_b1f");

    // 觸發 Boss 戰鬥
    battleService.startBossBattle(testPlayer, pos, "mozhu_mines:boss_song_tianheng");
    assertTrue(battleService.isInBattle(testPlayer.getName()));

    BattleContext ctx = battleService.getBattle(testPlayer.getName());
    assertNotNull(ctx);
    assertThat(ctx.getEnemies()).hasSize(1);

    var boss = ctx.getEnemies().get(0);
    assertThat(boss.getTemplateId()).isEqualTo("mozhu_mines:boss_song_tianheng");
    assertThat(boss.getName()).contains("宋天衡");
    assertThat(boss.getMaxHp()).isGreaterThanOrEqualTo(450);
    assertThat(boss.getDropItemId()).isEqualTo("mozhu_mines:black_obsidian_scythe");

    // 撤退脫戰以釋放鎖定
    battleService.flee(testPlayer, pos);
  }

  @Test
  @DisplayName("4. 驗證 DungeonCommand 指令：enter, list, leave 完整閉環")
  void testDungeonCommandWorkflow() {
    ScopedValue.where(MudContext.CURRENT_PLAYER, testPlayer).run(() -> {
      // 1. 執行 dungeon list
      dungeonCommand.execute("list");

      // 2. 踏入墨竹礦坑 B1F
      dungeonCommand.execute("enter mozhu_mines_b1f");
      DungeonPosition pos = dungeonManager.getOrCreatePosition(testPlayer.getName(), null);
      assertThat(pos.getFloorId()).isEqualTo("mozhu_mines_b1f");
      assertThat(pos.getX()).isEqualTo(1);
      assertThat(pos.getY()).isEqualTo(8);

      // 3. 在起點階梯處執行 dungeon leave，安全撤退回墨竹礦坑入口
      dungeonCommand.execute("leave");
      assertThat(testPlayer.getCurrentRoomId()).isEqualTo("mozhu_mines:mine_entrance");
    });
  }
}
