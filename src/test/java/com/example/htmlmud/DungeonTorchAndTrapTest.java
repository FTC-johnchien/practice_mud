package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.WebSocketSession;

import com.example.htmlmud.application.command.impl.DungeonCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.service.PlayerService;
import com.example.htmlmud.dungeon.DungeonFloor;
import com.example.htmlmud.dungeon.DungeonPosition;
import com.example.htmlmud.dungeon.GridDirection;
import com.example.htmlmud.dungeon.DungeonManager;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyItemSlot;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.PartyService;
import com.example.htmlmud.domain.world.WorldManager;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.context.MudContext;

@SpringBootTest
class DungeonTorchAndTrapTest {

  @Autowired
  private DungeonManager dungeonManager;

  @Autowired
  private DungeonCommand dungeonCommand;

  @Autowired
  private PartyService partyService;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private com.example.htmlmud.dungeon.DungeonNavigator dungeonNavigator;

  @Autowired
  private com.example.htmlmud.domain.dungeon.battle.DrpgBattleService battleService;

  private Player testPlayer;

  @BeforeEach
  void setUp() {
    partyService.clearCache();
    dungeonManager.clearPositions();
    if (battleService != null) {
      battleService.getActiveBattles().clear();
    }

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
    testPlayer = Player.createSinglePlayer(mockOutput, worldManager, playerService, "火把測試道友");
    testPlayer.setCurrentRoomId("taiyin_tomb:entrance");
    testPlayer.start();
    worldManager.addLivingActor(testPlayer);
  }

  @AfterEach
  void tearDown() {
    if (testPlayer != null) {
      testPlayer.stop();
      worldManager.removeLivingActor(testPlayer.getId());
    }
    partyService.clearCache();
    dungeonManager.clearPositions();
    if (battleService != null) {
      battleService.getActiveBattles().clear();
    }
  }

  @Test
  @DisplayName("1. 驗證 DungeonPosition 火把光明度數值模型與三段照度狀態流轉")
  void testTorchLightModelAndStatusFlow() {
    DungeonPosition pos = new DungeonPosition("taiyin_tomb_b1f", 1, 1, GridDirection.NORTH, 10, 10);
    assertThat(pos.getTorchLight()).isEqualTo(100);
    assertThat(pos.getTorchStatus()).isEqualTo(DungeonPosition.TorchStatus.BRIGHT);

    // 衰減至 50：切換至殘焰微光 (DIM)
    pos.consumeTorch(50);
    assertThat(pos.getTorchLight()).isEqualTo(50);
    assertThat(pos.getTorchStatus()).isEqualTo(DungeonPosition.TorchStatus.DIM);

    // 衰減至 15：切換至絕對黑暗 (DARKNESS)
    pos.consumeTorch(35);
    assertThat(pos.getTorchLight()).isEqualTo(15);
    assertThat(pos.getTorchStatus()).isEqualTo(DungeonPosition.TorchStatus.DARKNESS);

    // 扣減至負數時自動鉗位至 0
    pos.consumeTorch(50);
    assertThat(pos.getTorchLight()).isEqualTo(0);
    assertThat(pos.getTorchStatus()).isEqualTo(DungeonPosition.TorchStatus.DARKNESS);

    // 回補光明度
    pos.restoreTorch(30);
    assertThat(pos.getTorchLight()).isEqualTo(30);
    assertThat(pos.getTorchStatus()).isEqualTo(DungeonPosition.TorchStatus.DIM);

    // 重設火把
    pos.resetTorch();
    assertThat(pos.getTorchLight()).isEqualTo(100);
    assertThat(pos.getTorchStatus()).isEqualTo(DungeonPosition.TorchStatus.BRIGHT);
  }

  @Test
  @DisplayName("2. 驗證地牢步進位移自動衰減火把光明度，且黑暗環境扣減全隊 SAN 值")
  void testStepMovementTorchDecayAndDarknessSanLoss() {
    testPlayer.setInDungeon(true);
    DungeonPosition pos = dungeonManager.getOrCreatePosition(testPlayer.getName(), "taiyin_tomb_b1f");
    pos.setCoord(1, 1);
    pos.setFacing(GridDirection.SOUTH);
    pos.resetTorch();

    Party party = partyService.getOrCreateParty(testPlayer.getName());
    PartyMember leader = party.getLeader();

    ScopedValue.where(MudContext.CURRENT_PLAYER, testPlayer).run(() -> {
      // 執行一次向前邁步 (step w 面向 SOUTH 向前移動至 (1, 2) 可通行石廊)
      dungeonCommand.execute("step w");
      assertThat(pos.getTorchLight()).isEqualTo(99);

      // 人工將火把耗盡至絕對黑暗 (0)
      pos.consumeTorch(100);
      assertThat(pos.getTorchLight()).isEqualTo(0);
      assertThat(pos.getTorchStatus()).isEqualTo(DungeonPosition.TorchStatus.DARKNESS);

      // 於黑暗中再次位移：應觸發黑暗侵蝕扣減全隊 SAN 5 點
      int beforeMoveSan = leader.getCurrentSan();
      dungeonCommand.execute("step w");
      assertThat(leader.getCurrentSan()).isLessThan(beforeMoveSan);
    });
  }

  @Test
  @DisplayName("3. 驗證點燃火把指令 (dungeon torch / light) 優先消耗行囊火把，無火把時燃靈應急")
  void testLightTorchCommandConsumption() {
    testPlayer.setInDungeon(true);
    DungeonPosition pos = dungeonManager.getOrCreatePosition(testPlayer.getName(), "taiyin_tomb_b1f");
    pos.resetTorch();
    pos.consumeTorch(80); // 降至 20 點
    assertThat(pos.getTorchLight()).isEqualTo(20);

    Party party = partyService.getOrCreateParty(testPlayer.getName());
    party.getInventory().getSlots().clear(); // 清空行囊
    party.getInventory().addItem("torch", 2); // 放入 2 支火把

    ScopedValue.where(MudContext.CURRENT_PLAYER, testPlayer).run(() -> {
      // 執行點燃火把指令
      dungeonCommand.execute("dungeon torch");
      assertThat(pos.getTorchLight()).isEqualTo(100); // 滿血回滿

      // 驗證行囊中火把數量扣減為 1
      PartyItemSlot torchSlot = party.getInventory().getSlots().stream()
          .filter(s -> s.getItemId().contains("torch"))
          .findFirst().orElseThrow();
      assertThat(torchSlot.getCount()).isEqualTo(1);

      // 再次消耗火把至 10
      pos.consumeTorch(90);
      party.getInventory().getSlots().clear(); // 清空背包火把
      party.setCoin(50); // 保留足夠靈石

      // 無火把時執行點火指令：觸發燃靈術 (扣 10 靈石，回復 60 照度：10 + 60 = 70)
      dungeonCommand.execute("dungeon light");
      assertThat(party.getCoin()).isEqualTo(40);
      assertThat(pos.getTorchLight()).isEqualTo(70);
    });
  }

  @Test
  @DisplayName("4. 驗證動態火把照度射線視距 (BRIGHT 3步 / DIM 2步 / DARKNESS 1步) 與 DRPG 視圖狀態傳輸")
  void testDynamicVisionRaycastAndDtoTransmission() {
    testPlayer.setInDungeon(true);
    DungeonFloor floor = dungeonManager.getFloor("taiyin_tomb_b1f");
    DungeonPosition pos = dungeonManager.getOrCreatePosition(testPlayer.getName(), "taiyin_tomb_b1f");
    pos.resetTorch(); // 照度 100% (BRIGHT)
    pos.setCoord(1, 1);
    pos.setFacing(GridDirection.SOUTH);

    // 1. 滿光 (BRIGHT) 應可見前方 3 步（(1,2), (1,3), (1,4) 皆為幽暗石廊）
    String brightVision = dungeonNavigator.inspectForward(floor, pos);
    assertThat(brightVision).contains("正前方 1 步");
    assertThat(brightVision).contains("前方 2 步");
    assertThat(brightVision).contains("前方 3 步");

    // 2. 衰減至微光 (DIM 30%)，視距縮減至 2 步
    pos.consumeTorch(70); // 100 - 70 = 30
    assertThat(pos.getTorchStatus()).isEqualTo(DungeonPosition.TorchStatus.DIM);
    String dimVision = dungeonNavigator.inspectForward(floor, pos);
    assertThat(dimVision).contains("正前方 1 步");
    assertThat(dimVision).contains("前方 2 步");
    assertThat(dimVision).doesNotContain("前方 3 步");
    assertThat(dimVision).contains("殘焰微光");

    // 3. 衰減至絕對黑暗 (DARKNESS 10%)，視距僅限 1 步
    pos.consumeTorch(20); // 30 - 20 = 10
    assertThat(pos.getTorchStatus()).isEqualTo(DungeonPosition.TorchStatus.DARKNESS);
    String darknessVision = dungeonNavigator.inspectForward(floor, pos);
    assertThat(darknessVision).contains("正前方 1 步");
    assertThat(darknessVision).doesNotContain("前方 2 步");
    assertThat(darknessVision).contains("絕對黑暗");

    // 4. 驗證前方為牆壁時觸發射線遮擋 (Occlusion)，即使 BRIGHT 也不穿牆探測
    pos.resetTorch();
    pos.setCoord(1, 1);
    pos.setFacing(GridDirection.NORTH); // (1, 0) 為厚壁牆壁
    String wallOcclusion = dungeonNavigator.inspectForward(floor, pos);
    assertThat(wallOcclusion).contains("正前方 1 步");
    assertThat(wallOcclusion).doesNotContain("前方 2 步");

    // 5. 驗證 DrpgStateDto.of 正確封裝 torchLight, torchStatus, torchIcon
    Party party = partyService.getOrCreateParty(testPlayer.getName());
    com.example.htmlmud.dungeon.DrpgStateDto state =
        com.example.htmlmud.dungeon.DrpgStateDto.of(floor, pos, brightVision, party);
    assertThat(state.dungeon()).isNotNull();
    assertThat(state.dungeon().torchLight()).isEqualTo(100);
    assertThat(state.dungeon().torchStatus()).isEqualTo("BRIGHT");
    assertThat(state.dungeon().torchIcon()).isEqualTo("🔥");
  }

  @Test
  @DisplayName("5. 驗證地牢多元機關陷阱 (暗弩身法檢定、靈泉地窟滋養、鎖鏈暗門破拆)")
  void testDiverseTrapsAndVanguardSkillChecksAndSpringRejuvenation() {
    testPlayer.setInDungeon(true);
    DungeonFloor floor = dungeonManager.getFloor("taiyin_tomb_b1f");
    DungeonPosition pos = dungeonManager.getOrCreatePosition(testPlayer.getName(), "taiyin_tomb_b1f");
    Party party = partyService.getOrCreateParty(testPlayer.getName());
    PartyMember vanguard = party.getFrontRowMember();
    assertThat(vanguard).isNotNull();

    ScopedValue.where(MudContext.CURRENT_PLAYER, testPlayer).run(() -> {
      // --- 1. 暗弩陷阱身法檢定 ---
      com.example.htmlmud.dungeon.DungeonTile crossbowTile =
          com.example.htmlmud.dungeon.DungeonTile.of(
              com.example.htmlmud.dungeon.DungeonTile.TileType.TRAP,
              "青銅暗弩機關陷阱",
              "隱蔽於石磚下的連環發射暗弩。"
          );
      floor.getTiles()[1][2] = crossbowTile; // 放置於 (2, 1)

      // 先鋒高身法 (DEX=15 >= 12)：檢定通過，全隊免傷
      vanguard.getStats().setDex(15);
      int beforeHp = vanguard.getHp();
      int beforeSan = vanguard.getCurrentSan();
      pos.setCoord(2, 1);
      // 觸發踩點
      dungeonCommand.execute("dungeon look");
      // 模擬踩入地塊事件
      java.lang.reflect.Method handleTileEvent = null;
      try {
        handleTileEvent = DungeonCommand.class.getDeclaredMethod("handleTileEvent", Player.class, DungeonFloor.class, DungeonPosition.class);
        handleTileEvent.setAccessible(true);
        handleTileEvent.invoke(dungeonCommand, testPlayer, floor, pos);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      assertThat(vanguard.getHp()).isEqualTo(beforeHp); // 免傷
      assertThat(vanguard.getCurrentSan()).isEqualTo(beforeSan);

      // 先鋒低身法 (DEX=5 < 12)：檢定失敗，先鋒 -25 HP，扣減 5 SAN
      vanguard.getStats().setDex(5);
      try {
        handleTileEvent.invoke(dungeonCommand, testPlayer, floor, pos);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      assertThat(vanguard.getHp()).isEqualTo(beforeHp - 25);
      assertThat(vanguard.getCurrentSan()).isEqualTo(beforeSan - 5);

      // --- 2. 靈泉地窟滋養 ---
      com.example.htmlmud.dungeon.DungeonTile springTile =
          com.example.htmlmud.dungeon.DungeonTile.of(
              com.example.htmlmud.dungeon.DungeonTile.TileType.SPRING,
              "太玄清心靈泉",
              "氤氳著濃郁靈氣的溫潤泉穴。"
          );
      floor.getTiles()[1][3] = springTile;
      pos.setCoord(3, 1);

      int damagedHp = vanguard.getHp();
      int damagedMp = vanguard.getCombatStats().getMp();
      int damagedSan = vanguard.getCurrentSan();

      // 首次飲用靈泉：氣血 (+50)、真元 (+30)、道心 (+15)
      try {
        handleTileEvent.invoke(dungeonCommand, testPlayer, floor, pos);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      assertThat(springTile.isTriggered()).isTrue();
      assertThat(vanguard.getHp()).isGreaterThan(damagedHp);
      assertThat(vanguard.getCurrentSan()).isGreaterThan(damagedSan);

      // 再次踩入：靈氣已涸，不重複觸發
      int refreshedHp = vanguard.getHp();
      try {
        handleTileEvent.invoke(dungeonCommand, testPlayer, floor, pos);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      assertThat(vanguard.getHp()).isEqualTo(refreshedHp);

      // --- 3. 鎖鏈暗門破拆 ---
      com.example.htmlmud.dungeon.DungeonTile doorTile =
          com.example.htmlmud.dungeon.DungeonTile.of(
              com.example.htmlmud.dungeon.DungeonTile.TileType.DOOR,
              "玄鐵鎖鏈暗門",
              "以粗大玄鐵鎖鏈纏繞的厚重暗門。"
          );
      floor.getTiles()[1][4] = doorTile;
      pos.setCoord(4, 1);

      // 力量不足 (STR=8 < 12) 且無破拆工具：無法強開
      vanguard.getStats().setStr(8);
      party.getInventory().getSlots().clear();
      try {
        handleTileEvent.invoke(dungeonCommand, testPlayer, floor, pos);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      assertThat(doorTile.isTriggered()).isFalse();

      // 力量爆發 (STR=14 >= 12)：先鋒神力斬斷鎖鏈，獲得 +20 戰氣
      vanguard.getStats().setStr(14);
      int beforeSp = vanguard.getCurrentSp();
      try {
        handleTileEvent.invoke(dungeonCommand, testPlayer, floor, pos);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      assertThat(doorTile.isTriggered()).isTrue();
      assertThat(vanguard.getCurrentSp()).isEqualTo(beforeSp + 20);
    });
  }
}
