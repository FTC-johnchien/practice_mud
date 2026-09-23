package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.htmlmud.application.command.impl.DismissCommand;
import com.example.htmlmud.application.command.impl.DungeonCommand;
import com.example.htmlmud.application.command.impl.LoadCommand;
import com.example.htmlmud.application.command.impl.LookCommand;
import com.example.htmlmud.application.command.impl.MoveCommand;
import com.example.htmlmud.application.command.impl.NewGameCommand;
import com.example.htmlmud.application.command.impl.PartyCommand;
import com.example.htmlmud.application.command.impl.RecruitCommand;
import com.example.htmlmud.application.command.impl.SaveCommand;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.model.GridDirection;
import com.example.htmlmud.domain.model.enums.Direction;
import com.example.htmlmud.domain.party.model.CombatResourceType;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.save.service.SaveGameService;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.RoomMovementService;
import com.example.htmlmud.domain.service.WorldManager;

@SpringBootTest
class CleanArchitectureDecouplingTest {

  @Autowired
  private PartyService partyService;

  @Autowired
  private SaveGameService saveGameService;

  @Autowired
  private RoomMovementService roomMovementService;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private LoadCommand loadCommand;

  @Autowired
  private NewGameCommand newGameCommand;

  @Autowired
  private SaveCommand saveCommand;

  @Autowired
  private MoveCommand moveCommand;

  @Autowired
  private DungeonCommand dungeonCommand;

  @Autowired
  private RecruitCommand recruitCommand;

  @Autowired
  private DismissCommand dismissCommand;

  @Autowired
  private PartyCommand partyCommand;

  private Player testPlayer;

  @BeforeEach
  void setUp() {
    MessageOutput mockOutput = new MessageOutput() {
      @Override public void sendJson(Object payload) {}
      @Override public void close() {}
      @Override public org.springframework.web.socket.WebSocketSession getSession() { return null; }
    };
    testPlayer = Player.createSinglePlayer(mockOutput, worldManager, playerService, "雲隱真人");
    testPlayer.setCurrentRoomId("newbie_village:inn");
    testPlayer.start();
    worldManager.addLivingActor(testPlayer);
  }

  @Test
  @DisplayName("1. 反射驗證：徹底消除指令之間的直接相互注入耦合")
  void testNoInterCommandCouplingViaReflection() {
    // LoadCommand 不得包含 SaveCommand 欄位
    boolean loadHasSave = Arrays.stream(LoadCommand.class.getDeclaredFields())
        .anyMatch(f -> f.getType().equals(SaveCommand.class));
    assertFalse(loadHasSave, "LoadCommand 不應直接依賴 SaveCommand！");

    // NewGameCommand 不得包含 SaveCommand 欄位
    boolean newHasSave = Arrays.stream(NewGameCommand.class.getDeclaredFields())
        .anyMatch(f -> f.getType().equals(SaveCommand.class));
    assertFalse(newHasSave, "NewGameCommand 不應直接依賴 SaveCommand！");

    // MoveCommand 不得包含 LookCommand 欄位
    boolean moveHasLook = Arrays.stream(MoveCommand.class.getDeclaredFields())
        .anyMatch(f -> f.getType().equals(LookCommand.class));
    assertFalse(moveHasLook, "MoveCommand 不應直接依賴 LookCommand！");

    // DungeonCommand 不得包含 MoveCommand 欄位
    boolean dungeonHasMove = Arrays.stream(DungeonCommand.class.getDeclaredFields())
        .anyMatch(f -> f.getType().equals(MoveCommand.class));
    assertFalse(dungeonHasMove, "DungeonCommand 不應直接依賴 MoveCommand！");

    // SaveCommand 不得包含 DungeonCommand 欄位
    boolean saveHasDungeon = Arrays.stream(SaveCommand.class.getDeclaredFields())
        .anyMatch(f -> f.getType().equals(DungeonCommand.class));
    assertFalse(saveHasDungeon, "SaveCommand 不應直接依賴 DungeonCommand！");
  }

  @Test
  @DisplayName("2. Enum 隔離與轉接：GridDirection 與 CombatResourceType 獨立無歧義")
  void testEnumDisambiguationAndBridges() {
    // 網格方向轉向與正交映射
    assertEquals(GridDirection.EAST, GridDirection.NORTH.turnRight());
    assertEquals(GridDirection.WEST, GridDirection.NORTH.turnLeft());
    assertEquals(GridDirection.SOUTH, GridDirection.NORTH.getOpposite());
    assertEquals(Direction.NORTH, GridDirection.NORTH.toMudDirection());
    assertEquals(GridDirection.SOUTH, GridDirection.fromMudDirection(Direction.SOUTH));

    // 戰鬥招式資源類型獨立
    assertEquals(CombatResourceType.SP, CombatResourceType.fromString("sp"));
    assertEquals(CombatResourceType.SP, CombatResourceType.fromString("stamina"));
    assertEquals(CombatResourceType.MP, CombatResourceType.fromString("mana"));
    assertEquals(CombatResourceType.HP, CombatResourceType.fromString("life"));
    assertEquals(CombatResourceType.RAGE, CombatResourceType.fromString("rage"));
    assertEquals(CombatResourceType.COMBO, CombatResourceType.fromString("combo"));
  }

  @Test
  @DisplayName("3. 業務收斂：PartyService 統一處理招募與請離")
  void testUnifiedPartyRecruitAndDismiss() {
    Party party = partyService.resetParty(testPlayer.getName(), "雲隱真人");
    assertEquals(1, party.size());

    // 招募夥伴
    boolean recruitOk = partyService.recruitCompanionForPlayer(testPlayer, "tie_niu");
    assertTrue(recruitOk, "招募鐵牛應成功");
    assertEquals(2, party.size());

    // 重複招募應防禦
    boolean reRecruit = partyService.recruitCompanionForPlayer(testPlayer, "tie_niu");
    assertFalse(reRecruit, "重複招募同一人應失敗");
    assertEquals(2, party.size());

    // 隊長不可請離
    boolean dismissLeader = partyService.dismissCompanionForPlayer(testPlayer, "雲隱真人");
    assertFalse(dismissLeader, "主角隊長不可被請離");
    assertEquals(2, party.size());

    // 請離鐵牛
    boolean dismissOk = partyService.dismissCompanionForPlayer(testPlayer, "tie_niu");
    assertTrue(dismissOk, "請離鐵牛應成功");
    assertEquals(1, party.size());
  }

  @Test
  @DisplayName("4. 服務解耦：RoomMovementService 獨立負責房間移動與離進場")
  void testRoomMovementService() {
    testPlayer.setCurrentRoomId("newbie_village:inn");
    var innRoom = worldManager.getRoomActor("newbie_village:inn");
    assertNotNull(innRoom);
    innRoom.enter(testPlayer, Direction.NORTH);

    // 往東移動到村中廣場
    boolean moved = roomMovementService.move(testPlayer, Direction.EAST);
    assertTrue(moved, "往東移動應成功");
    assertEquals("newbie_village:square", testPlayer.getCurrentRoom().getId());

    // 往西移動回客棧
    boolean movedBack = roomMovementService.move(testPlayer, Direction.WEST);
    assertTrue(movedBack, "往西移動回客棧應成功");
    assertEquals("newbie_village:inn", testPlayer.getCurrentRoom().getId());
  }

  @Test
  @DisplayName("5. 指令多重入口測試：Recruit/Dismiss/PartyCommand 指令調用一致性")
  void testCommandFacadeConsistency() {
    ScopedValue.where(MudContext.CURRENT_PLAYER, testPlayer).run(() -> {
      Party party = partyService.resetParty(testPlayer.getName(), "雲隱真人");

      // 透過 RecruitCommand 招募
      recruitCommand.execute("ling_shuang");
      assertEquals(2, party.size());

      // 透過 DismissCommand 請離
      dismissCommand.execute("ling_shuang");
      assertEquals(1, party.size());

      // 透過 PartyCommand 招募
      partyCommand.execute("recruit ling_shuang");
      assertEquals(2, party.size());

      // 透過 PartyCommand 請離
      partyCommand.execute("dismiss ling_shuang");
      assertEquals(1, party.size());
    });
  }
}
