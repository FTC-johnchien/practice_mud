package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketSession;

import com.example.htmlmud.application.command.impl.RestCommand;
import com.example.htmlmud.config.GameConfig;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.enums.RoomFlag;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.save.service.SaveGameService;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.world.WorldManager;
import com.example.htmlmud.domain.actor.core.MessageOutput;

@SpringBootTest
@ActiveProfiles("test")
class DataCoreIntegrityTest {

  @Autowired
  private GameConfig gameConfig;

  @Autowired
  private RestCommand restCommand;

  @Autowired
  private SaveGameService saveGameService;

  @Autowired
  private PartyService partyService;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private WorldManager worldManager;

  private MessageOutput createMockOutput() {
    return new MessageOutput() {
      @Override
      public void sendJson(Object payload) {}

      @Override
      public void close() {}

      @Override
      public WebSocketSession getSession() {
        return null;
      }
    };
  }

  @Test
  @DisplayName("DATA-CORE-01: 驗證新手村客棧與雪亭客棧房間資料具備 SAFE_ZONE 標籤")
  void testInnRoomsHaveSafeZoneFlag() {
    Room newbieInn = worldManager.getRoomActor("newbie_village:inn");
    assertThat(newbieInn).isNotNull();
    assertThat(newbieInn.hasFlag(RoomFlag.SAFE_ZONE)).isTrue();

    Room snowInn = worldManager.getRoomActor("snow:snow_inn");
    assertThat(snowInn).isNotNull();
    assertThat(snowInn.hasFlag(RoomFlag.SAFE_ZONE)).isTrue();
  }

  @Test
  @DisplayName("DATA-CORE-01: RestCommand 依據 RoomFlag.SAFE_ZONE 執行完全回復")
  void testRestCommandSafeZoneFullRecovery() {
    Player player = Player.createSinglePlayer(createMockOutput(), worldManager, playerService, "safe_tester");
    player.setCurrentRoomId("newbie_village:inn");

    Party party = partyService.getOrCreateParty(player);
    PartyMember member = party.getMembers().get(0);
    member.getStats().setHp(10);
    member.consumeSan(50);
    member.getStats().setMp(5);

    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      restCommand.execute("");
    });

    assertThat(member.getStats().getHp()).isEqualTo(member.getStats().getMaxHp());
    assertThat(member.getCurrentSan()).isEqualTo(100);
    assertThat(member.getStats().getMp()).isEqualTo(member.getStats().getMaxMp());
  }

  @Test
  @DisplayName("DATA-CORE-01: SaveGameService 開闢新遊戲與預設存檔由 GameConfig 提供出生點")
  void testSaveGameServiceSpawnRoomFromGameConfig() {
    String expectedSpawn = gameConfig.getDefaults().getPlayer().getSpawnRoomId();
    assertThat(expectedSpawn).isNotBlank();

    Player newPlayer = Player.createSinglePlayer(createMockOutput(), worldManager, playerService, "spawn_tester");
    saveGameService.handleNew(newPlayer, "李逍遙");

    assertThat(newPlayer.getCurrentRoomId()).isEqualTo(expectedSpawn);
    assertThat(newPlayer.isInDungeon()).isFalse();
  }

  @org.junit.jupiter.api.AfterEach
  void tearDown() {
    partyService.clearCache();
  }
}

