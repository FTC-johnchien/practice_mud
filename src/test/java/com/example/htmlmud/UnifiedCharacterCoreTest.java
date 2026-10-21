package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.application.command.impl.DropCommand;
import com.example.htmlmud.application.command.impl.EquipCommand;
import com.example.htmlmud.application.command.impl.GetCommand;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.actor.service.PlayerService;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.save.service.SaveGameService;
import com.example.htmlmud.domain.world.WorldManager;
import com.example.htmlmud.protocol.MessageFactory;
import com.example.htmlmud.protocol.MudMessage;

/**
 * Milestone F1-01 核心大一統驗收測試：
 * 驗證 Player 降級為純空間代理 (Spatial Proxy)，
 * PartyMember 與 Party.inventory 為唯一真相源 (SSoT)，徹底消滅實體與背包雙寫假帳。
 */
@SpringBootTest
class UnifiedCharacterCoreTest {

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private PartyService partyService;

  @Autowired
  private GetCommand getCommand;

  @Autowired
  private DropCommand dropCommand;

  @Autowired
  private EquipCommand equipCommand;

  @Autowired
  private SaveGameService saveGameService;

  @Autowired
  private DrpgBattleService battleService;

  @Autowired
  private WorldFactory worldFactory;

  private Player player;
  private Party party;
  private Room room;

  @BeforeEach
  void setUp() {
    partyService.clearCache();
    if (battleService != null) {
      battleService.getActiveBattles().clear();
    }

    String playerName = "大一統真君-" + UUID.randomUUID().toString().substring(0, 6);
    player = Player.createSinglePlayer(new com.example.htmlmud.domain.actor.core.MessageOutput() {
      @Override
      public void sendJson(Object payload) {}
      @Override
      public void close() {}
      @Override
      public org.springframework.web.socket.WebSocketSession getSession() {
        return null;
      }
    }, worldManager, playerService, playerName);

    party = partyService.getOrCreateParty(playerName);
    party.getInventory().getSlots().clear();

    room = worldManager.getRoomActor("newbie_village:inn");
    if (room != null) {
      for (GameItem it : new ArrayList<>(room.getItems())) {
        room.removeItem(it.getId());
      }
    }
  }

  @AfterEach
  void tearDown() {
    partyService.clearCache();
    if (battleService != null) {
      battleService.getActiveBattles().clear();
    }
  }

  @Test
  @DisplayName("F1-01 驗證: 撿拾物品與丟棄物品 100% 作用於小隊公共行囊，Player 個人假帳零雙寫")
  void testPickupAndDropOperateSolelyOnPartyInventory() {
    // 1. 在房間地上放置一把精鋼刀
    GameItem blade = worldFactory.createItem("steel_blade");
    if (blade == null) {
      blade = new GameItem();
      blade.setId("steel_blade");
      blade.setName("百辟精鋼刀");
      blade.setType(ItemType.WEAPON);
    }
    room.dropItem(blade);

    // 2. Player 執行 get steel_blade
    GameItem targetBlade = blade;
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      getCommand.execute(targetBlade.getId());
    });

    // 斷言：小隊公共行囊已獲得該物品 (SSoT)
    assertThat(party.getInventory().getSlots())
        .anyMatch(s -> s.getItemId().contains("steel_blade") || s.getName().contains("鋼刀"));

    // 斷言：Player 個人背包維持為空，徹底消滅雙寫假帳
    assertThat(player.getInventory()).noneMatch(s -> s.getId().contains("steel_blade") || s.getName().contains("鋼刀"));

    // 3. Player 執行 drop 鋼刀
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      dropCommand.execute("鋼刀");
    });

    // 斷言：小隊公共行囊已扣減
    assertThat(party.getInventory().getSlots())
        .noneMatch(s -> s.getItemId().contains("steel_blade") || s.getName().contains("鋼刀"));

    // 斷言：房間地面已出現丟下的物品
    assertThat(room.getItems())
        .anyMatch(it -> it.getId().contains("steel_blade") || it.getName().contains("鋼刀"));
  }

  @Test
  @DisplayName("F1-01 驗證: Player.getMainHandWeapon 與 getOffHandEquip 動態委派隊長真實槽位 (SSoT)")
  void testEquipmentDelegationToPartyLeader() {
    PartyMember leader = party.getLeader();
    assertThat(leader).isNotNull();

    // 1. 初始時，Player 委派反映隊長初始佩掛之青銅古劍
    assertThat(player.getMainHandWeapon()).isNotNull();
    assertThat(player.getMainHandWeapon().getName()).contains("古劍");

    // 2. 隊長穿戴百辟精鋼刀
    PartyItemSlot bladeSlot = PartyItemSlot.builder()
        .slotId("test-blade-slot")
        .itemId("steel_blade")
        .name("百辟精鋼刀")
        .itemType(ItemType.WEAPON)
        .equipSlot(EquipmentSlot.MAIN_HAND)
        .count(1)
        .build();
    leader.getEquipment().put(EquipmentSlot.MAIN_HAND, bladeSlot);

    // 斷言：Player.getMainHandWeapon 自動反映隊長主手裝備，無需額外雙寫
    assertThat(player.getMainHandWeapon()).isNotNull();
    assertThat(player.getMainHandWeapon().getName()).contains("鋼刀");

    // 3. 隊長穿戴雪亭靈盾 (副手)
    PartyItemSlot shieldSlot = PartyItemSlot.builder()
        .slotId("test-shield-slot")
        .itemId("snow_shield")
        .name("雪亭玄靈盾")
        .itemType(ItemType.SHIELD)
        .equipSlot(EquipmentSlot.OFF_HAND)
        .count(1)
        .build();
    leader.getEquipment().put(EquipmentSlot.OFF_HAND, shieldSlot);

    // 斷言：Player.getOffHandEquip 自動反映隊長副手裝備
    assertThat(player.getOffHandEquip()).isNotNull();
    assertThat(player.getOffHandEquip().getName()).contains("盾");
  }

  @Test
  @DisplayName("F1-01 驗證: MessageFactory.playerDetail 裝備清單直接投影隊長真實裝備")
  void testPlayerDetailReflectsLeaderEquipment() {
    PartyMember leader = party.getLeader();
    assertThat(leader).isNotNull();

    PartyItemSlot armorSlot = PartyItemSlot.builder()
        .slotId("test-armor-slot")
        .itemId("yin_robe")
        .name("太陰玄煞道袍")
        .itemType(ItemType.ARMOR)
        .equipSlot(EquipmentSlot.BODY)
        .count(1)
        .build();
    leader.getEquipment().put(EquipmentSlot.BODY, armorSlot);

    MudMessage<?> detailMsg = MessageFactory.playerDetail(player);
    assertThat(detailMsg).isNotNull();
    assertThat(detailMsg.getType()).isEqualTo("ENTITY_DETAIL");

    @SuppressWarnings("unchecked")
    var payload = (java.util.Map<String, Object>) detailMsg.getPayload();
    assertThat(payload).isNotNull();

    @SuppressWarnings("unchecked")
    var items = (List<String>) payload.get("items");
    assertThat(items).isNotNull();
    assertThat(items).contains("太陰玄煞道袍");
  }

  @Test
  @DisplayName("F1-01 驗證: 戰鬥穿戴法寶直接從小隊公共行囊扣除並完成隊長佩掛")
  void testBattleEquipDirectlyFromPartyInventory() {
    // 預先在小隊公共行囊放入百辟精鋼刀
    PartyItemSlot bladeSlot = PartyItemSlot.builder()
        .slotId("slot-blade-01")
        .itemId("steel_blade")
        .name("百辟精鋼刀")
        .itemType(ItemType.WEAPON)
        .equipSlot(EquipmentSlot.MAIN_HAND)
        .count(1)
        .build();
    party.getInventory().addSlot(bladeSlot);

    assertThat(party.getInventory().getSlots()).hasSize(1);
    assertThat(party.getLeader().getEquipment().get(EquipmentSlot.MAIN_HAND)).isNotNull();

    // 執行 equip slot-blade-01
    battleService.equipItem(player, "slot-blade-01", 0, null);

    // 斷言：小隊公共行囊已移除該武器槽位
    assertThat(party.getInventory().getSlots()).noneMatch(s -> s.getSlotId().equals("slot-blade-01"));

    // 斷言：隊長已穿戴該武器
    assertThat(party.getLeader().getEquipment().get(EquipmentSlot.MAIN_HAND)).isNotNull();
    assertThat(party.getLeader().getEquipment().get(EquipmentSlot.MAIN_HAND).getName()).contains("鋼刀");

    // 斷言：Player 空間代理動態反映隊長武器
    assertThat(player.getMainHandWeapon()).isNotNull();
    assertThat(player.getMainHandWeapon().getName()).contains("鋼刀");
  }

  @Test
  @DisplayName("F1-01 驗證: 存檔與讀檔管線 100% 透過 Party 持久化小隊行囊與隊員裝備，無個人假帳")
  void testSaveAndLoadPreservesUnifiedCore() {
    // 在小隊行囊放置 5 個辟邪清心符
    party.getInventory().addItem("purify_talisman", 5);

    // 隊長佩掛鋼刀
    PartyItemSlot bladeSlot = PartyItemSlot.builder()
        .slotId("save-blade-slot")
        .itemId("steel_blade")
        .name("百辟精鋼刀")
        .itemType(ItemType.WEAPON)
        .equipSlot(EquipmentSlot.MAIN_HAND)
        .count(1)
        .build();
    party.getLeader().getEquipment().put(EquipmentSlot.MAIN_HAND, bladeSlot);

    // 執行存檔至 slot 2
    var saveData = saveGameService.saveGame(player.getName(), 2, "大一統驗收存檔測試");
    assertThat(saveData).isNotNull();
    assertThat(saveData.getSchemaVersion()).isEqualTo(3);

    // 清除記憶體中的小隊快取
    partyService.clearCache();

    // 讀取存檔
    var loadedData = saveGameService.loadGame(player.getName(), 2);
    assertThat(loadedData).isNotNull();

    Party reloadedParty = partyService.getOrCreateParty(player.getName());
    assertThat(reloadedParty).isNotNull();

    // 驗證行囊物品完整恢復
    assertThat(reloadedParty.getInventory().getSlots())
        .anyMatch(s -> s.getItemId().contains("purify_talisman") && s.getCount() == 5);

    // 驗證隊長裝備完整恢復
    assertThat(reloadedParty.getLeader().getEquipment().get(EquipmentSlot.MAIN_HAND)).isNotNull();
    assertThat(reloadedParty.getLeader().getEquipment().get(EquipmentSlot.MAIN_HAND).getName()).contains("鋼刀");

    // 清理存檔槽位
    saveGameService.deleteSave(2);
  }
}
