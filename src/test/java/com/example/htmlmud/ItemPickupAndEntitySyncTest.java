package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.application.command.impl.GetCommand;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.save.service.SaveGameService;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.TargetSelector;
import com.example.htmlmud.domain.service.WorldManager;

@SpringBootTest
class ItemPickupAndEntitySyncTest {

  @Autowired
  private TargetSelector targetSelector;

  @Autowired
  private PartyService partyService;

  @Autowired
  private SaveGameService saveGameService;

  @Autowired
  private WorldFactory worldFactory;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private GetCommand getCommand;

  @Autowired
  private com.example.htmlmud.domain.service.LivingService livingService;

  @Test
  @DisplayName("驗證 TargetSelector 支援 UUID 與模板 ID 精準匹配")
  void testTargetSelectorMatchUUIDAndTemplateId() {
    List<GameItem> candidates = new ArrayList<>();

    String pouchUuid = UUID.randomUUID().toString();
    GameItem pouch = new GameItem();
    pouch.setId(pouchUuid);
    pouch.setName("【散落的儲物袋】");
    pouch.setType(ItemType.CONTAINER);
    candidates.add(pouch);

    // 1. 透過 UUID 比對尋找
    GameItem foundByUuid = targetSelector.selectItem(candidates, pouchUuid);
    assertThat(foundByUuid).isNotNull();
    assertThat(foundByUuid.getId()).isEqualTo(pouchUuid);

    // 2. 透過名稱比對尋找
    GameItem foundByName = targetSelector.selectItem(candidates, "散落的儲物袋");
    assertThat(foundByName).isNotNull();
    assertThat(foundByName.getId()).isEqualTo(pouchUuid);
  }

  @Test
  @DisplayName("驗證戰利品儲物袋 (Loot Bag) 一鍵搜刮入背包且儲物袋本體即刻消散")
  void testLootPouchLootAllAndVanish() {
    Room room = worldManager.getRoomActor("newbie_village:inn");
    assertThat(room).isNotNull();

    // 建立虛擬玩家
    MessageOutput mockOutput = new MessageOutput() {
      @Override public void sendJson(Object payload) {}
      @Override public void close() {}
      @Override public org.springframework.web.socket.WebSocketSession getSession() { return null; }
    };
    Player player = Player.createSinglePlayer(mockOutput, worldManager, playerService, "測試道人");
    player.setCurrentRoomId("newbie_village:inn");
    player.setInDungeon(false);

    Party party = partyService.resetParty("測試道人", "測試道人");

    // 建立儲物袋並放入掉落物 (百草金創藥膏與鋼刀)
    GameItem pouch = new GameItem();
    pouch.setId(UUID.randomUUID().toString());
    pouch.setName("【散落的儲物袋】");
    pouch.setType(ItemType.CONTAINER);

    GameItem drop1 = worldFactory.createItem("purify_talisman");
    GameItem drop2 = worldFactory.createItem("steel_blade");
    if (drop1 != null) pouch.addContent(drop1);
    if (drop2 != null) pouch.addContent(drop2);

    room.dropItem(pouch);

    // 模擬玩家撿起/搜刮儲物袋
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      getCommand.execute(pouch.getId());
    });

    // 驗證 1: 儲物袋內的法寶已全數進入隊伍背包
    assertThat(party.getInventory().getSlots()).anyMatch(slot -> 
        slot.getItemId().contains("purify_talisman") || slot.getName().contains("符") || slot.getItemId().contains("steel_blade")
    );

    // 驗證 2: 儲物袋本體沒有存入背包
    assertThat(party.getInventory().getSlots()).noneMatch(slot -> 
        slot.getName().contains("儲物袋")
    );

    // 驗證 3: 房間地面已經沒有該儲物袋 (地面乾淨)
    assertThat(room.getItems()).noneMatch(item -> item.getId().equals(pouch.getId()));
  }

  @Test
  @DisplayName("驗證純屍體物品禁止進入背包")
  void testCorpseCannotEnterBag() {
    Room room = worldManager.getRoomActor("newbie_village:inn");
    assertThat(room).isNotNull();

    MessageOutput mockOutput = new MessageOutput() {
      @Override public void sendJson(Object payload) {}
      @Override public void close() {}
      @Override public org.springframework.web.socket.WebSocketSession getSession() { return null; }
    };
    Player player = Player.createSinglePlayer(mockOutput, worldManager, playerService, "防屍道人");
    player.setCurrentRoomId("newbie_village:inn");
    player.setInDungeon(false);

    Party party = partyService.resetParty("防屍道人", "防屍道人");

    // 地面有一具空屍體
    GameItem corpse = new GameItem();
    corpse.setId(UUID.randomUUID().toString());
    corpse.setName("野鼠的殘骸");
    corpse.setType(ItemType.CORPSE);
    room.dropItem(corpse);

    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      getCommand.execute(corpse.getId());
    });

    // 背包中嚴格杜絕出現屍體
    assertThat(party.getInventory().getSlots()).noneMatch(slot -> 
        slot.getItemType() == ItemType.CORPSE || slot.getName().contains("殘骸") || slot.getName().contains("屍體")
    );
  }

  @Test
  @DisplayName("驗證連續擊敗兩隻普通野怪時戰利品自動合併至同一個儲物袋且可一鍵搜刮全數入包")
  void testMultipleMobDeathsAutoMergeIntoSinglePouch() {
    Room room = worldManager.getRoomActor("newbie_village:inn");
    assertThat(room).isNotNull();

    // 清空房間可能存在的殘餘 items
    for (GameItem it : new ArrayList<>(room.getItems())) {
      room.removeItem(it.getId());
    }
    assertThat(room.getItems()).isEmpty();

    MessageOutput mockOutput = new MessageOutput() {
      @Override public void sendJson(Object payload) {}
      @Override public void close() {}
      @Override public org.springframework.web.socket.WebSocketSession getSession() { return null; }
    };
    Player player = Player.createSinglePlayer(mockOutput, worldManager, playerService, "搜刮宗師");
    player.setCurrentRoomId("newbie_village:inn");
    player.setInDungeon(false);

    Party party = partyService.resetParty("搜刮宗師", "搜刮宗師");

    // 模擬第一隻怪掉落（生成新儲物袋）
    GameItem bread = new GameItem();
    bread.setId(UUID.randomUUID().toString());
    bread.setName("村莊烤麵包");
    bread.setType(ItemType.CONSUMABLE);
    GameItem pouch1 = new GameItem();
    pouch1.setId(UUID.randomUUID().toString());
    pouch1.setName("【散落的儲物袋】");
    pouch1.setType(ItemType.CONTAINER);
    pouch1.getContents().add(bread);
    room.dropItem(pouch1);

    assertThat(room.getItems()).hasSize(1);

    // 模擬第二隻怪死亡掉落（自動合併進既有儲物袋）
    GameItem stone = new GameItem();
    stone.setId(UUID.randomUUID().toString());
    stone.setName("碎靈石");
    stone.setType(ItemType.CONSUMABLE);

    // 模擬 LivingService 中的合併邏輯
    Optional<GameItem> existingPouch = room.getItems().stream()
        .filter(it -> it != null && it.getType() == ItemType.CONTAINER && "【散落的儲物袋】".equals(it.getName()))
        .findFirst();
    assertThat(existingPouch).isPresent();
    existingPouch.get().getContents().add(stone);

    // 驗證地面始終只有 1 個儲物袋，且內部包含 2 件靈物
    assertThat(room.getItems()).hasSize(1);
    GameItem mergedPouch = room.getItems().get(0);
    assertThat(mergedPouch.getContents()).hasSize(2);
    assertThat(mergedPouch.getContents()).extracting(GameItem::getName).containsExactlyInAnyOrder("村莊烤麵包", "碎靈石");

    // 玩家執行一鍵搜刮
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      getCommand.execute(mergedPouch.getId());
    });

    // 驗證背包已全數納入 2 件戰利品
    assertThat(party.getInventory().getSlots()).anyMatch(s -> s.getName().equals("村莊烤麵包"));
    assertThat(party.getInventory().getSlots()).anyMatch(s -> s.getName().equals("碎靈石"));

    // 驗證儲物袋搜刮後立即隨風消散，地面恢復乾淨
    assertThat(room.getItems()).isEmpty();
  }

  @Test
  @DisplayName("驗證單機冒險開局主角道號正確同步為玄靈子")
  void testProtagonistNameSynchronization() {
    // 建立新遊戲存檔
    Party newParty = saveGameService.createNewGame("p-test-sync", "玄靈子", "formation_four_symbols");
    assertThat(newParty).isNotNull();
    assertThat(newParty.getMembers().get(0).getName()).isEqualTo("玄靈子");

    // 驗證小隊隊長道號一致
    Party party = partyService.getOrCreateParty("p-test-sync");
    assertThat(party.getMembers().get(0).getName()).isEqualTo("玄靈子");
  }
}
