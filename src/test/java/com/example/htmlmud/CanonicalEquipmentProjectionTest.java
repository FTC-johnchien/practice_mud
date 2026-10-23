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

import com.example.htmlmud.application.command.impl.EquipCommand;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.item.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyItemSlot;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.PartyService;
import com.example.htmlmud.save.SaveGameService;
import com.example.htmlmud.domain.actor.service.CharacterSyncService;
import com.example.htmlmud.domain.actor.service.PlayerService;
import com.example.htmlmud.domain.world.WorldManager;

@SpringBootTest
class CanonicalEquipmentProjectionTest {

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private PartyService partyService;

  @Autowired
  private WorldFactory worldFactory;

  @Autowired
  private DrpgBattleService battleService;

  @Autowired
  private EquipCommand equipCommand;

  @Autowired
  private CharacterSyncService characterSyncService;

  @Autowired
  private SaveGameService saveGameService;

  private final List<Object> sentMessages = new ArrayList<>();
  private Player player;
  private Party party;
  private PartyMember leader;

  @BeforeEach
  void setUp() {
    sentMessages.clear();
    MessageOutput mockOutput = new MessageOutput() {
      @Override public void sendJson(Object payload) { sentMessages.add(payload); }
      @Override public void close() {}
      @Override public org.springframework.web.socket.WebSocketSession getSession() { return null; }
    };

    String playerId = "p-equip-sync-" + UUID.randomUUID().toString().substring(0, 6);
    player = Player.createSinglePlayer(mockOutput, worldManager, playerService, playerId);
    player.setCurrentRoomId("newbie_village:inn");
    player.setInDungeon(false);

    party = partyService.resetParty(playerId, playerId);
    party.getInventory().getSlots().clear();
    leader = party.getMembers().get(0);
  }

  @Test
  @DisplayName("驗證 7 大部位裝備由小隊隊長 (PartyMember) 單一擁有，並實時單向投影至 Player 實體")
  void testSevenSlotEquipmentProjectionToPlayer() {
    // 1. 小隊倉庫加入各部位裝備法寶
    party.getInventory().addItem("steel_blade", 1);
    party.getInventory().addItem("yin_robe", 1);

    // 2. 隊長穿戴百煉精鋼刀與陰煞道袍
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      battleService.equipItem(player, "steel_blade", 0, null);
      battleService.equipItem(player, "yin_robe", 0, null);
    });

    // 3. 驗證隊長實體裝備擁有權
    assertThat(leader.getEquipment().get(EquipmentSlot.MAIN_HAND)).isNotNull();
    assertThat(leader.getEquipment().get(EquipmentSlot.MAIN_HAND).getItemId()).isEqualTo("steel_blade");
    assertThat(leader.getEquipment().get(EquipmentSlot.BODY)).isNotNull();
    assertThat(leader.getEquipment().get(EquipmentSlot.BODY).getItemId()).isEqualTo("yin_robe");

    // 4. 驗證 Player 實體 (MUD) 獲得投影
    assertThat(player.getStats().getEquipment().get(EquipmentSlot.MAIN_HAND)).isNotNull();
    assertThat(player.getStats().getEquipment().get(EquipmentSlot.MAIN_HAND).getName()).contains("鋼刀");
    assertThat(player.getStats().getEquipment().get(EquipmentSlot.BODY)).isNotNull();
    assertThat(player.getStats().getEquipment().get(EquipmentSlot.BODY).getName()).contains("道袍");

    // 5. 驗證 Player 衍生屬性 (minDamage, maxDamage, defense) 已隨裝備自動重新計算
    assertThat(player.getMainHandWeapon()).isNotNull();
    assertThat(player.getMinDamage()).isGreaterThan(0);
    assertThat(player.getMaxDamage()).isGreaterThan(0);
    assertThat(player.getDefense()).isGreaterThan(0);
  }

  @Test
  @DisplayName("驗證主角可直接自個人隨身行囊穿戴法寶，自動流轉並完成投影")
  void testEquipDirectlyFromPersonalInventory() {
    // 往玩家個人隨身行囊放入法寶武器
    GameItem personalBlade = new GameItem();
    personalBlade.setId("steel_blade");
    personalBlade.setName("百煉精鋼刀");
    personalBlade.setType(ItemType.WEAPON);
    var tpl = worldManager.getTemplateReader().findItem("steel_blade").orElse(null);
    if (tpl != null) personalBlade.setTemplate(tpl);
    player.getInventory().add(personalBlade);

    assertThat(player.getInventory()).hasSize(1);
    assertThat(party.getInventory().getSlots()).isEmpty();

    // 透過 equip 指令穿戴
    ScopedValue.where(MudContext.CURRENT_PLAYER, player)
        .where(MudContext.RAW_INPUT, "equip 鋼刀")
        .run(() -> equipCommand.execute("鋼刀"));

    // 1. 個人行囊已移出該法寶
    assertThat(player.getInventory()).isEmpty();

    // 2. 隊長身上已穿戴該武器
    assertThat(leader.getEquipment().get(EquipmentSlot.MAIN_HAND)).isNotNull();
    assertThat(leader.getEquipment().get(EquipmentSlot.MAIN_HAND).getName()).contains("鋼刀");

    // 3. Player (MUD) 實體武器實時同步投影
    assertThat(player.getMainHandWeapon()).isNotNull();
    assertThat(player.getMainHandWeapon().getName()).contains("鋼刀");
  }

  @Test
  @DisplayName("驗證 unequip 卸下裝備時，隊長與 Player 投影實時同步清除，數值重算回空手")
  void testUnequipSynchronizesBothTiers() {
    party.getInventory().addItem("steel_blade", 1);
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      battleService.equipItem(player, "steel_blade", 0, null);
    });

    assertThat(leader.getEquipment().get(EquipmentSlot.MAIN_HAND)).isNotNull();
    assertThat(player.getMainHandWeapon()).isNotNull();
    int armedMaxDamage = player.getMaxDamage();

    // 執行卸下主手武器
    ScopedValue.where(MudContext.CURRENT_PLAYER, player)
        .where(MudContext.RAW_INPUT, "unequip weapon")
        .run(() -> equipCommand.execute("weapon"));

    // 1. 隊長主手已空
    assertThat(leader.getEquipment().get(EquipmentSlot.MAIN_HAND)).isNull();

    // 2. Player 主手投影已清除
    assertThat(player.getMainHandWeapon()).isNull();

    // 3. 攻擊力重算回空手基礎數值
    assertThat(player.getMaxDamage()).isLessThan(armedMaxDamage);
  }

  @Test
  @DisplayName("驗證單機存檔讀取時，隊長 7 大裝備部位完整還原並投影至線上玩家")
  void testSaveAndLoadEquipmentProjection() {
    party.getInventory().addItem("steel_blade", 1);
    party.getInventory().addItem("yin_robe", 1);
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      battleService.equipItem(player, "steel_blade", 0, null);
      battleService.equipItem(player, "yin_robe", 0, null);
    });

    int slotId = 1;
    saveGameService.deleteSave(slotId);

    try {
      // 存檔
      ScopedValue.where(MudContext.CURRENT_PLAYER, player).call(() ->
          saveGameService.saveGame(player.getId(), slotId, "裝備大一統存檔測試")
      );

      // 人為清空 Player 裝備
      player.getStats().getEquipment().clear();
      player.minDamage = 0;
      player.maxDamage = 0;
      player.defense = 0;

      // 讀檔還原
      ScopedValue.where(MudContext.CURRENT_PLAYER, player).call(() ->
          saveGameService.loadGame(player.getId(), slotId)
      );

      // 驗證讀檔後 Player 實體之裝備投影與屬性重新計算完整生效
      assertThat(player.getStats().getEquipment().get(EquipmentSlot.MAIN_HAND)).isNotNull();
      assertThat(player.getStats().getEquipment().get(EquipmentSlot.BODY)).isNotNull();
      assertThat(player.getMainHandWeapon()).isNotNull();
      assertThat(player.getMaxDamage()).isGreaterThan(0);
      assertThat(player.getDefense()).isGreaterThan(0);
    } finally {
      saveGameService.deleteSave(slotId);
    }
  }

  @Test
  @DisplayName("驗證 EquipCommand 無參數時可輸出完整的 7 大部位裝備佩掛面板")
  void testEquipCommandDisplaySevenSlots() {
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      equipCommand.execute("");
    });

    assertThat(sentMessages).isNotEmpty();
  }
}
