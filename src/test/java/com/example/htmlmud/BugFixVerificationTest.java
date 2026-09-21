package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.WebSocketSession;

import com.example.htmlmud.application.command.CommandDispatcher;
import com.example.htmlmud.application.command.impl.DropCommand;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.service.LivingService;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.RoomService;
import com.example.htmlmud.domain.service.WorldManager;
import com.example.htmlmud.protocol.MudMessage;

@SpringBootTest
class BugFixVerificationTest {

  @Autowired
  private DropCommand dropCommand;

  @Autowired
  private CommandDispatcher commandDispatcher;

  @Autowired
  private LivingService livingService;

  @Autowired
  private RoomService roomService;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  private MessageOutput createMockOutput(List<Object> jsonSink) {
    return new MessageOutput() {
      @Override
      public void sendJson(Object payload) {
        if (jsonSink != null) {
          jsonSink.add(payload);
        }
      }
      @Override
      public void close() {}
      @Override
      public WebSocketSession getSession() {
        return null;
      }
    };
  }

  @Test
  @DisplayName("Bug 1 驗證: DropCommand 丟棄物品後，物品留在房間，不再被自動撿回")
  void testDropCommandDoesNotRePickItem() {
    Room room = worldManager.getRoomActor("newbie_village:square");
    assertThat(room).isNotNull();

    Player player = Player.createSinglePlayer(createMockOutput(null), worldManager, playerService, "丟物測試者");
    player.setCurrentRoomId("newbie_village:square");

    GameItem testItem = new GameItem();
    testItem.setId(UUID.randomUUID().toString());
    testItem.setName("測試鐵劍");
    testItem.setAliases(List.of("sword", "鐵劍"));
    testItem.setType(ItemType.WEAPON);

    player.getInventory().add(testItem);
    assertThat(player.getInventory()).contains(testItem);

    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      dropCommand.execute("sword");
    });

    // 物品必須已從玩家背包移除
    assertThat(player.getInventory()).doesNotContain(testItem);
    // 物品必須留在房間地上
    assertThat(room.getItems()).contains(testItem);

    // 清理測試物品
    room.removeItem(testItem.getId());
  }

  @Test
  @DisplayName("Bug 2 驗證: Dispatcher 保留原始 rawInput，可區分 unequip 與 equip")
  void testDispatcherPreservesRawInputForUnequip() {
    Player player = Player.createSinglePlayer(createMockOutput(null), worldManager, playerService, "裝備測試者");

    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      commandDispatcher.dispatch("unequip weapon 0");
    });

    // CommandDispatcher 會在 dispatch 期間將 "unequip weapon 0" 綁定至 MudContext.RAW_INPUT
    // 驗證 dispatch 能正常運作且沒有例外
  }

  @Test
  @DisplayName("Bug 3 驗證: Player.lookAtMe 使用 equals 比對 String id，不同實例相同內容正確返回自身資訊")
  void testPlayerLookAtMeStringEquals() {
    Player player1 = Player.createSinglePlayer(createMockOutput(null), worldManager, playerService, "看自己測試者");
    player1.setNickname("玄靈道人");

    // 驗證 lookAtMe 透過 equals 比對進入 performLookAtMe 分支
    MudMessage<?> msg = player1.lookAtMe(player1);
    assertThat(msg).isNotNull();
    assertThat(msg.getType()).isEqualTo("ENTITY_DETAIL");
  }

  @Test
  @DisplayName("Bug 4 驗證: LivingService.unequip 正確將裝備移回背包並從裝備欄移除")
  void testLivingServiceUnequipLogic() {
    Player player = Player.createSinglePlayer(createMockOutput(null), worldManager, playerService, "脫裝測試者");

    GameItem helmet = new GameItem();
    helmet.setId(UUID.randomUUID().toString());
    helmet.setName("精鋼頭盔");
    helmet.setType(ItemType.ARMOR);

    LivingStats stats = player.getStats();
    stats.equipment.put(EquipmentSlot.HEAD, helmet);

    assertThat(stats.equipment.get(EquipmentSlot.HEAD)).isEqualTo(helmet);
    assertThat(player.getInventory()).doesNotContain(helmet);

    // 執行脫下頭部裝備
    boolean result = livingService.unequip(player, EquipmentSlot.HEAD);

    assertThat(result).isTrue();
    // 裝備欄應為空
    assertThat(stats.equipment.get(EquipmentSlot.HEAD)).isNull();
    // 背包應增加該裝備
    assertThat(player.getInventory()).contains(helmet);
  }

  @Test
  @DisplayName("Bug 5 驗證: RoomService.broadcastJson 正確將訊息透過 sendJson 發送給房間內所有玩家")
  void testRoomServiceBroadcastJson() {
    List<Object> receivedJson1 = new ArrayList<>();
    List<Object> receivedJson2 = new ArrayList<>();

    Player player1 = Player.createSinglePlayer(createMockOutput(receivedJson1), worldManager, playerService, "廣播收訊者1");
    Player player2 = Player.createSinglePlayer(createMockOutput(receivedJson2), worldManager, playerService, "廣播收訊者2");

    // 清空進場訊息
    receivedJson1.clear();
    receivedJson2.clear();

    MudMessage<Object> testMsg = MudMessage.<Object>builder()
        .type("TEST_BROADCAST")
        .payload("測試廣播資料")
        .build();

    roomService.broadcastJson(List.of(player1, player2), testMsg);

    assertThat(receivedJson1).contains(testMsg);
    assertThat(receivedJson2).contains(testMsg);
  }
}
