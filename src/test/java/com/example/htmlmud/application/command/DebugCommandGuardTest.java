package com.example.htmlmud.application.command;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import com.example.htmlmud.application.command.impl.BattleCommand;
import com.example.htmlmud.application.command.impl.DungeonCommand;
import com.example.htmlmud.config.GameConfig;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.model.GridDirection;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.dungeon.service.DungeonNavigator;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.domain.world.GameStateBroadcastService;
import com.example.htmlmud.domain.world.RoomMovementService;

class DebugCommandGuardTest {

  private DrpgBattleService battleService;
  private DungeonManager dungeonManager;
  private DungeonNavigator dungeonNavigator;
  private PartyService partyService;
  private RoomMovementService roomMovementService;
  private GameStateBroadcastService broadcastService;
  private TemplateReader templateReader;
  private Player player;
  private GameConfig gameConfig;
  private MockEnvironment environment;

  @BeforeEach
  void setUp() {
    battleService = mock(DrpgBattleService.class);
    dungeonManager = mock(DungeonManager.class);
    dungeonNavigator = mock(DungeonNavigator.class);
    partyService = mock(PartyService.class);
    roomMovementService = mock(RoomMovementService.class);
    broadcastService = mock(GameStateBroadcastService.class);
    templateReader = mock(TemplateReader.class);
    player = mock(Player.class);

    when(player.getName()).thenReturn("TestHero");
    when(player.isInDungeon()).thenReturn(true);

    gameConfig = new GameConfig();
    environment = new MockEnvironment();
  }

  @Test
  @DisplayName("DEBUG-01: 正式環境 (debug=false) 下封禁 battle test/木樁 與 battle charge/充能，防止刷怪刷資源")
  void testBattleDebugCommandsBlockedInProduction() {
    gameConfig.getDebug().setEnabled(false);
    environment.setActiveProfiles("prod");

    BattleCommand cmd = new BattleCommand(battleService, dungeonManager);
    cmd.setGameConfig(gameConfig);
    cmd.setEnvironment(environment);

    // 1. 嘗試木樁測試戰鬥
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> cmd.execute("dummy"));
    verify(player).reply(contains("玄鐵試道傀儡演武僅於除錯環境開放，當前已受天道法則封禁"));
    verify(battleService, never()).startTrainingBattle(any(), any());

    // 2. 嘗試充能法門
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> cmd.execute("charge"));
    verify(player).reply(contains("陣法靈威充能法門僅於除錯環境開放，當前已受天道法則封禁"));
    verify(battleService, never()).chargeCombatResources(any(), any());
  }

  @Test
  @DisplayName("DEBUG-01: 除錯模式開啟 (debug=true) 時正常允許木樁與充能指令")
  void testBattleDebugCommandsAllowedWhenDebugEnabled() {
    gameConfig.getDebug().setEnabled(true);

    DungeonPosition pos = new DungeonPosition("test_floor", 0, 0, GridDirection.NORTH, 10, 10);
    when(dungeonManager.getPlayerPosition("TestHero")).thenReturn(pos);

    BattleCommand cmd = new BattleCommand(battleService, dungeonManager);
    cmd.setGameConfig(gameConfig);
    cmd.setEnvironment(environment);

    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> cmd.execute("dummy"));
    verify(battleService).startTrainingBattle(player, pos);

    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> cmd.execute("charge"));
    verify(battleService).chargeCombatResources(player, pos);
  }

  @Test
  @DisplayName("DEBUG-01: 正式環境 (debug=false) 下封禁 dungeon dummy/test/testmob")
  void testDungeonDummyBlockedInProduction() {
    gameConfig.getDebug().setEnabled(false);
    environment.setActiveProfiles("prod");

    DungeonPosition pos = new DungeonPosition("test_floor", 0, 0, GridDirection.NORTH, 10, 10);
    when(dungeonManager.getOrCreatePosition("TestHero", null)).thenReturn(pos);
    when(dungeonManager.getFloor("test_floor")).thenReturn(com.example.htmlmud.domain.dungeon.model.DungeonFloor.builder().id("test_floor").build());

    DungeonCommand cmd = new DungeonCommand(
        dungeonManager, dungeonNavigator, partyService, battleService,
        roomMovementService, broadcastService, templateReader);
    cmd.setGameConfig(gameConfig);
    cmd.setEnvironment(environment);

    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> cmd.execute("dummy"));
    verify(player).reply(contains("試道演武傀儡僅於除錯環境開放，當前已受天地法則封禁"));
    verify(battleService, never()).startTrainingBattle(any(), any());
  }

  @Test
  @DisplayName("DEBUG-01: 除錯模式開啟時正常執行 dungeon dummy")
  void testDungeonDummyAllowedWhenDebugEnabled() {
    gameConfig.getDebug().setEnabled(true);

    DungeonPosition pos = new DungeonPosition("test_floor", 0, 0, GridDirection.NORTH, 10, 10);
    when(dungeonManager.getOrCreatePosition("TestHero", null)).thenReturn(pos);
    when(dungeonManager.getFloor("test_floor")).thenReturn(com.example.htmlmud.domain.dungeon.model.DungeonFloor.builder().id("test_floor").build());

    DungeonCommand cmd = new DungeonCommand(
        dungeonManager, dungeonNavigator, partyService, battleService,
        roomMovementService, broadcastService, templateReader);
    cmd.setGameConfig(gameConfig);
    cmd.setEnvironment(environment);

    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> cmd.execute("dummy"));
    verify(battleService).startTrainingBattle(player, pos);
  }
}
