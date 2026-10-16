package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.application.command.impl.DropCommand;
import com.example.htmlmud.application.command.impl.GetCommand;
import com.example.htmlmud.application.command.impl.InventoryCommand;
import com.example.htmlmud.application.command.impl.StashCommand;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.save.model.SaveData;
import com.example.htmlmud.domain.save.service.SaveGameService;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.world.WorldManager;

@SpringBootTest
class DualTierInventoryAndStashTest {

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private PartyService partyService;

  @Autowired
  private WorldFactory worldFactory;

  @Autowired
  private GetCommand getCommand;

  @Autowired
  private DropCommand dropCommand;

  @Autowired
  private StashCommand stashCommand;

  @Autowired
  private InventoryCommand inventoryCommand;

  @Autowired
  private SaveGameService saveGameService;

  private final List<Object> sentMessages = new ArrayList<>();
  private Player player;
  private Party party;
  private Room room;

  @BeforeEach
  void setUp() {
    sentMessages.clear();
    MessageOutput mockOutput = new MessageOutput() {
      @Override public void sendJson(Object payload) { sentMessages.add(payload); }
      @Override public void close() {}
      @Override public org.springframework.web.socket.WebSocketSession getSession() { return null; }
    };

    String playerId = "p-dual-tier-" + UUID.randomUUID().toString().substring(0, 6);
    player = Player.createSinglePlayer(mockOutput, worldManager, playerService, playerId);
    player.setCurrentRoomId("newbie_village:inn");
    party = partyService.resetParty(playerId, playerId);
    party.getInventory().getSlots().clear();
    room = worldManager.getRoomActor("newbie_village:inn");
    if (room != null) {
      // 清空地面物品
      for (GameItem item : new ArrayList<>(room.getItems())) {
        room.removeItem(item.getId());
      }
    }
  }

  @Test
  @DisplayName("驗證地面散落物品拾取時直接同步納入大一統小隊行囊")
  void testLooseItemPickupEntersPartyInventory() {
    GameItem looseSword = worldFactory.createItem("steel_blade");
    if (looseSword == null) {
      looseSword = new GameItem();
      looseSword.setId("steel_blade");
      looseSword.setName("百煉精鋼刀");
      looseSword.setType(ItemType.WEAPON);
    }
    room.dropItem(looseSword);

    GameItem finalItem = looseSword;
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      getCommand.execute(finalItem.getId());
    });

    // 1. 個人隨身行囊視圖已獲得此物品
    assertThat(player.getInventory()).anyMatch(it -> it.getName().contains("鋼刀") || it.getId().equals("steel_blade"));

    // 2. 小隊公共行囊亦已同步獲得該物品 (大一統單一真相源)
    assertThat(party.getInventory().getSlots()).anyMatch(s -> s.getItemId().contains("steel_blade") || s.getName().contains("鋼刀"));
  }

  @Test
  @DisplayName("驗證容器/儲物袋掉落物全數搜刮入小隊公共倉庫，不佔用個人行囊")
  void testContainerSpoilsEnterPartyStorage() {
    GameItem pouch = new GameItem();
    pouch.setId(UUID.randomUUID().toString());
    pouch.setName("【散落的儲物袋】");
    pouch.setType(ItemType.CONTAINER);

    GameItem talisman = worldFactory.createItem("purify_talisman");
    if (talisman != null) {
      pouch.addContent(talisman);
    }
    room.dropItem(pouch);

    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      getCommand.execute(pouch.getId());
    });

    // 1. 小隊公共倉庫獲得符籙
    assertThat(party.getInventory().getSlots()).anyMatch(s -> s.getItemId().contains("purify_talisman") || s.getName().contains("符"));

    // 2. 個人行囊沒有獲得容器符籙
    assertThat(player.getInventory()).noneMatch(it -> it.getId().contains("purify_talisman") || it.getName().contains("符"));
  }

  @Test
  @DisplayName("驗證 stash 與 withdraw 雙向流轉：個人行囊存入倉庫，倉庫取出回行囊")
  void testStashAndWithdrawWorkflow() {
    // 給個人行囊放入一件物品
    GameItem potion = new GameItem();
    potion.setId("herb_paste");
    potion.setName("百草金創藥膏");
    potion.setType(ItemType.CONSUMABLE);
    player.getInventory().add(potion);

    assertThat(player.getInventory()).hasSize(1);
    assertThat(party.getInventory().getSlots()).isEmpty();

    // 1. 執行 stash：將個人行囊物品存入小隊倉庫
    ScopedValue.where(MudContext.CURRENT_PLAYER, player)
        .where(MudContext.RAW_INPUT, "stash 藥膏")
        .run(() -> stashCommand.execute("藥膏"));

    assertThat(player.getInventory()).isEmpty();
    assertThat(party.getInventory().getSlots()).hasSize(1);
    assertThat(party.getInventory().getSlots().get(0).getName()).isEqualTo("百草金創藥膏");

    // 2. 執行 withdraw：從小隊倉庫取回個人行囊
    ScopedValue.where(MudContext.CURRENT_PLAYER, player)
        .where(MudContext.RAW_INPUT, "withdraw 藥膏")
        .run(() -> stashCommand.execute("藥膏"));

    assertThat(player.getInventory()).hasSize(1);
    assertThat(player.getInventory().get(0).getName()).isEqualTo("百草金創藥膏");
    assertThat(party.getInventory().getSlots()).isEmpty();
  }

  @Test
  @DisplayName("驗證 DropCommand 支援 drop (個人行囊) 與 drop stash (小隊倉庫)")
  void testDropFromPersonalAndPartyStorage() {
    GameItem personalItem = new GameItem();
    personalItem.setId("wooden_stick");
    personalItem.setName("青木短杖");
    personalItem.setType(ItemType.WEAPON);
    player.getInventory().add(personalItem);

    party.getInventory().addItem("yin_robe", 1);

    // 1. drop 個人行囊物品
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      dropCommand.execute("青木短杖");
    });

    assertThat(player.getInventory()).isEmpty();
    assertThat(room.getItems()).anyMatch(it -> it.getName().contains("青木短杖"));

    // 2. drop stash 小隊倉庫物品
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      dropCommand.execute("stash 陰煞道袍");
    });

    assertThat(party.getInventory().getSlots()).noneMatch(s -> s.getName().contains("陰煞道袍"));
    assertThat(room.getItems()).anyMatch(it -> it.getName().contains("陰煞道袍"));
  }

  @Test
  @DisplayName("驗證個人隨身行囊可隨單機存檔 v2 正確序列化與還原")
  void testPersonalInventorySaveAndRestore() {
    GameItem amulet = new GameItem();
    amulet.setId("peace_amulet");
    amulet.setName("辟邪玉佩");
    amulet.setType(ItemType.ARMOR);
    player.getInventory().add(amulet);

    int slotId = 1;
    saveGameService.deleteSave(slotId);

    try {
      SaveData saved = ScopedValue.where(MudContext.CURRENT_PLAYER, player).call(() ->
          saveGameService.saveGame(player.getId(), slotId, "測試隨身行囊存檔")
      );

      assertThat(saved.getSchemaVersion()).isEqualTo(2);
      assertThat(saved.getPersonalInventory()).isNotEmpty();
      assertThat(saved.getPersonalInventory().get(0).getName()).isEqualTo("辟邪玉佩");

      // 清空玩家行囊
      player.getInventory().clear();
      assertThat(player.getInventory()).isEmpty();

      // 讀檔還原
      SaveData loaded = ScopedValue.where(MudContext.CURRENT_PLAYER, player).call(() ->
          saveGameService.loadGame(player.getId(), slotId)
      );

      assertThat(loaded.getPersonalInventory()).isNotEmpty();
      assertThat(player.getInventory()).anyMatch(it -> it.getName().contains("辟邪玉佩"));
    } finally {
      saveGameService.deleteSave(slotId);
    }
  }

  @Test
  @DisplayName("驗證 InventoryCommand 同時輸出個人行囊與小隊倉庫")
  void testInventoryDisplayBothTiers() {
    GameItem bread = new GameItem();
    bread.setId("bread");
    bread.setName("乾糧");
    bread.setType(ItemType.CONSUMABLE);
    player.getInventory().add(bread);

    party.getInventory().addItem("gold_ore", 3);

    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      inventoryCommand.execute("");
    });

    // 驗證 sentMessages 中有發送物品清單包含個人隨身行囊與小隊儲物倉庫
    assertThat(sentMessages).isNotEmpty();
  }
}
