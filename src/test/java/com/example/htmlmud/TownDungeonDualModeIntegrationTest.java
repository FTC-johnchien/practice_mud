package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.htmlmud.application.command.impl.DungeonCommand;
import com.example.htmlmud.application.command.impl.MoveCommand;
import com.example.htmlmud.application.command.impl.RecruitCommand;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.dto.DrpgStateDto;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.save.service.SaveGameService;
import com.example.htmlmud.domain.service.GameStateBroadcastService;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.WorldManager;

import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
class TownDungeonDualModeIntegrationTest {

  @Autowired
  private SaveGameService saveGameService;

  @Autowired
  private PartyService partyService;

  @Autowired
  private RecruitCommand recruitCommand;

  @Autowired
  private MoveCommand moveCommand;

  @Autowired
  private DungeonCommand dungeonCommand;

  @Autowired
  private com.example.htmlmud.application.command.impl.SaveCommand saveCommand;

  @Autowired
  private com.example.htmlmud.application.command.impl.LookCommand lookCommand;

  @Autowired
  private DungeonManager dungeonManager;

  @Autowired
  private GameStateBroadcastService broadcastService;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private Player player;
  private List<String> capturedMessages;

  @BeforeEach
  void setUp() {
    capturedMessages = new java.util.concurrent.CopyOnWriteArrayList<>();
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

    player = Player.createSinglePlayer(mockOutput, worldManager, playerService, "雲中子");
    player.setCurrentRoomId("newbie_village:inn");
    player.setInDungeon(false);
    player.start();
    worldManager.addLivingActor(player);
    partyService.resetParty("雲中子", "雲中子");
  }

  @Test
  @DisplayName("驗證開闢新道途為單人 1 人隊伍，並可於客棧招募鐵牛與凌霜")
  void testSoloStartAndRecruitFlow() {
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      Party soloParty = saveGameService.createNewGame(player.getName(), "雲中子", "formation_four_symbols");
      assertThat(soloParty.size()).isEqualTo(1);
      assertThat(soloParty.getMembers().get(0).getName()).isEqualTo("雲中子");

      // 招募 鐵牛
      recruitCommand.execute("tie_niu");
      assertThat(soloParty.size()).isEqualTo(2);
      assertThat(soloParty.getMember(1).getName()).isEqualTo("鐵牛");

      // 招募 凌霜
      recruitCommand.execute("ling_shuang");
      assertThat(soloParty.size()).isEqualTo(3);
      assertThat(soloParty.getMember(2).getName()).isEqualTo("凌霜");

      // 重複招募應被阻擋
      recruitCommand.execute("tie_niu");
      assertThat(soloParty.size()).isEqualTo(3);

      // 請離鐵牛
      recruitCommand.execute("dismiss m-iron");
      assertThat(soloParty.size()).isEqualTo(2);
      assertThat(soloParty.getMembers().stream().anyMatch(m -> "m-iron".equals(m.getId()))).isFalse();

      // 請離主角應被阻擋
      recruitCommand.execute("dismiss m-leader");
      assertThat(soloParty.size()).isEqualTo(2);
    });
  }

  @Test
  @DisplayName("驗證城鎮模式下 GameStateBroadcastService 推送 TOWN 視圖與 NPC 能力標籤")
  void testTownStateBroadcast() {
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      player.setInDungeon(false);
      player.setCurrentRoomId("newbie_village:inn");

      broadcastService.broadcastState(player);

      assertThat(capturedMessages).isNotEmpty();
      String lastJson = capturedMessages.get(capturedMessages.size() - 1);
      assertThat(lastJson).contains("\"type\":\"DRPG_STATE\"");
      assertThat(lastJson).contains("\"mode\":\"TOWN\"");
      assertThat(lastJson).contains("村莊客棧");
      // 客棧應有福伯、鐵牛、凌霜等 NPC 與能力標籤
      assertThat(lastJson).contains("福伯");
      assertThat(lastJson).contains("鐵牛");
      assertThat(lastJson).contains("凌霜");
      assertThat(lastJson).contains("TALK");
      assertThat(lastJson).contains("RECRUIT");
    });
  }

  @Test
  @DisplayName("驗證踏入墨竹礦坑地牢後切換為 DUNGEON 視圖，離開後切回 TOWN 視圖")
  void testEnterAndLeaveDungeonDualMode() {
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      // 踏入墨竹礦坑 B1F
      dungeonCommand.execute("enter mozhu_mines_b1f");
      assertTrue(player.isInDungeon(), "踏入地牢後 inDungeon 應為 true");

      capturedMessages.clear();
      broadcastService.broadcastState(player);
      assertThat(capturedMessages).isNotEmpty();
      String dungeonJson = capturedMessages.get(capturedMessages.size() - 1);
      assertThat(dungeonJson).contains("\"mode\":\"DUNGEON\"");
      assertThat(dungeonJson).contains("mozhu_mines_b1f");

      // 於起點階梯 (0, 0) 撤離地牢
      dungeonCommand.execute("leave");
      assertFalse(player.isInDungeon(), "離開地牢後 inDungeon 應為 false");
      assertThat(player.getCurrentRoomId()).isEqualTo("mozhu_mines:mine_entrance");

      capturedMessages.clear();
      broadcastService.broadcastState(player);
      assertThat(capturedMessages).isNotEmpty();
      String townJson = capturedMessages.get(capturedMessages.size() - 1);
      assertThat(townJson).contains("\"mode\":\"TOWN\"");
      assertThat(townJson).contains("mozhu_mines:mine_entrance");
    });
  }

  @Test
  @DisplayName("驗證新手村至礦坑之 4 正交方向拓撲移動 (西與東)")
  void testOrthogonalMovementBetweenTowns() {
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      player.setCurrentRoomId("newbie_village:north_of_the_village");
      player.setInDungeon(false);

      // 向西前往礦坑入口
      moveCommand.execute("west");
      assertThat(player.getCurrentRoomId()).isEqualTo("mozhu_mines:mine_entrance");

      // 向東返回新手村北
      moveCommand.execute("east");
      assertThat(player.getCurrentRoomId()).isEqualTo("newbie_village:north_of_the_village");
    });
  }

  @Test
  @DisplayName("驗證靜默存檔查詢不洗版文字日誌，以及城鎮模式下移動與察看輸出簡約大氣")
  void testQuietSavesAndCleanTownLookAndMoveLog() {
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      player.setCurrentRoomId("newbie_village:inn");
      player.setInDungeon(false);

      // 1. 測試 saves quiet: 僅廣播 SAVE_SLOTS，不輸出 ASCII 存檔表格
      capturedMessages.clear();
      saveCommand.execute("quiet");
      assertThat(capturedMessages).anyMatch(msg -> msg.contains("\"type\":\"SAVE_SLOTS\""));
      assertThat(capturedMessages).noneMatch(msg -> msg.contains("📜【仙道命冊・單機存檔槽位】"));

      // 2. 測試城鎮模式下 look: 廣播 TOWN 視圖，抑制 raw 房間文本
      capturedMessages.clear();
      lookCommand.execute("");
      assertThat(capturedMessages).anyMatch(msg -> msg.contains("\"mode\":\"TOWN\""));
      assertThat(capturedMessages).noneMatch(msg -> msg.contains("=== 村莊客棧 ==="));

      // 3. 測試城鎮模式下移動: 輸出簡約抵達提示，單人時顯示主角名，並廣播 TOWN 視圖與富能力標籤
      capturedMessages.clear();
      moveCommand.execute("east");
      assertThat(player.getCurrentRoomId()).isEqualTo("newbie_village:square");
      assertThat(capturedMessages).anyMatch(msg -> msg.contains("🚶 【" + player.getName() + "】向") && msg.contains("前行，抵達【中央廣場】。"));
      assertThat(capturedMessages).anyMatch(msg -> msg.contains("\"mode\":\"TOWN\""));
      // 確保沒有舊 MUD 原始出口資訊洗版
      assertThat(capturedMessages).noneMatch(msg -> msg.contains("=== 中央廣場 ==="));
    });
  }
}
