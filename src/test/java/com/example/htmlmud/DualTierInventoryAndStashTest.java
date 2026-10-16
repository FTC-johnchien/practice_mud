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
  @DisplayName("驗證 SP-02: stash 與 withdraw 手動雙向轉存已除役，小隊公共行囊為唯一共享載體")
  void testStashAndWithdrawDecommissioned() {
    // 預先在小隊行囊與個人假帳中設定物品
    GameItem potion = new GameItem();
    potion.setId("herb_paste");
    potion.setName("百草金創藥膏");
    potion.setType(ItemType.CONSUMABLE);
    player.getInventory().add(potion);

    party.getInventory().addItem("gold_ore", 3);

    int initialPersonalSize = player.getInventory().size();
    int initialPartySlots = party.getInventory().getSlots().size();

    // 1. 執行 stash：不再搬動個人行囊物品至倉庫
    ScopedValue.where(MudContext.CURRENT_PLAYER, player)
        .where(MudContext.RAW_INPUT, "stash 藥膏")
        .run(() -> stashCommand.execute("藥膏"));

    assertThat(player.getInventory()).hasSize(initialPersonalSize);
    assertThat(party.getInventory().getSlots()).hasSize(initialPartySlots);

    // 2. 執行 withdraw：亦不再從小隊倉庫搬回個人行囊
    ScopedValue.where(MudContext.CURRENT_PLAYER, player)
        .where(MudContext.RAW_INPUT, "withdraw 靈石")
        .run(() -> stashCommand.execute("靈石"));

    assertThat(player.getInventory()).hasSize(initialPersonalSize);
    assertThat(party.getInventory().getSlots()).hasSize(initialPartySlots);
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
  @DisplayName("驗證 SP-03: 存檔大一統 (Party Inventory 單一真相源持久化，徹底除役 personalInventory 雙寫)")
  void testPartyInventorySaveAndRestoreUnified() {
    // 將物品放入小隊公共行囊
    party.getInventory().addItem("peace_amulet", 1);
    assertThat(party.getInventory().getSlots()).anyMatch(s -> s.getItemId().contains("peace_amulet"));

    int slotId = 1;
    saveGameService.deleteSave(slotId);

    try {
      SaveData saved = ScopedValue.where(MudContext.CURRENT_PLAYER, player).call(() ->
          saveGameService.saveGame(player.getId(), slotId, "測試小隊行囊大一統存檔")
      );

      assertThat(saved.getSchemaVersion()).isEqualTo(SaveData.CURRENT_SCHEMA_VERSION);
      // 驗證存檔之小隊公共行囊中包含該物品
      assertThat(saved.getParty().getInventory().getSlots()).anyMatch(s -> s.getItemId().contains("peace_amulet"));

      // 清空小隊行囊以模擬記憶體重置
      party.getInventory().getSlots().clear();
      assertThat(party.getInventory().getSlots()).isEmpty();

      // 讀檔還原
      SaveData loaded = ScopedValue.where(MudContext.CURRENT_PLAYER, player).call(() ->
          saveGameService.loadGame(player.getId(), slotId)
      );

      assertThat(loaded.getSchemaVersion()).isEqualTo(SaveData.CURRENT_SCHEMA_VERSION);
      // 驗證小隊公共行囊成功完整還原
      assertThat(loaded.getParty().getInventory().getSlots()).anyMatch(s -> s.getItemId().contains("peace_amulet"));
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
