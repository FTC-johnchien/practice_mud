package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.htmlmud.application.command.impl.KillCommand;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.service.CombatService;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.WorldManager;
import com.example.htmlmud.infra.server.WorldPulse;
import org.springframework.web.socket.WebSocketSession;

@SpringBootTest
@ActiveProfiles("test")
class CombatFlowTest {

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private CombatService combatService;

  @Autowired
  private WorldPulse worldPulse;

  @Autowired
  private KillCommand killCommand;

  private Player player;
  private List<String> capturedMessages;

  @BeforeEach
  void setUp() {
    capturedMessages = new CopyOnWriteArrayList<>();
    MessageOutput mockOutput = new MessageOutput() {
      @Override
      public void sendJson(Object payload) {
        if (payload != null) {
          capturedMessages.add(payload.toString());
        }
      }

      @Override
      public void close() {}

      @Override
      public WebSocketSession getSession() {
        return null;
      }
    };

    player = Player.createSinglePlayer(mockOutput, worldManager, playerService, "測試道友");
    player.setCurrentRoomId("newbie_village:forest_entrance");
    player.setInDungeon(false);
    player.start();
    worldManager.addLivingActor(player);

    Room room = worldManager.getRoomActor("newbie_village:forest_entrance");
    room.enter(player, com.example.htmlmud.domain.model.enums.Direction.SOUTH);
  }

  @Test
  @DisplayName("驗證攻擊巨大野鼠時，野鼠會反擊並產生攻擊戰鬥日誌")
  void testWildRatCombatCounterAttack() throws Exception {
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      Room room = worldManager.getRoomActor("newbie_village:forest_entrance");
      List<Mob> mobs = room.getMobs();
      System.out.println("Room mobs: " + mobs.stream().map(Mob::getName).toList());
      assertThat(mobs).isNotEmpty();

      // 發起攻擊
      killCommand.execute("wild rat");
    });

    // 模擬 40 個心跳 (4秒)
    for (int i = 0; i < 40; i++) {
      worldPulse.pulse();
      Thread.sleep(100);
    }

    System.out.println("=== 捕獲到的所有戰鬥訊息 (" + capturedMessages.size() + ") ===");
    for (String msg : capturedMessages) {
      System.out.println(msg);
    }

    boolean ratAttacked = capturedMessages.stream().anyMatch(m -> m.contains("野鼠") && (m.contains("咬") || m.contains("抓") || m.contains("撞") || m.contains("傷害")));
    System.out.println("ratAttacked: " + ratAttacked);
    assertThat(ratAttacked).isTrue();

    // 驗證訊息中不再包含出戲的時間戳 [xx.xxx]
    boolean hasTimestamp = capturedMessages.stream().anyMatch(m -> m.matches(".*\\[\\d{2}\\.\\d{3}\\].*"));
    assertThat(hasTimestamp).as("戰鬥日誌中不應存在出戲的時間戳前綴").isFalse();
  }
}
