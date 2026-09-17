package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.application.command.impl.DungeonCommand;
import com.example.htmlmud.application.command.impl.RestCommand;
import com.example.htmlmud.application.command.impl.ShopCommand;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.dungeon.service.DungeonNavigator;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.WorldManager;

@SpringBootTest
class NewbieToMozhuLoopIntegrationTest {

  @Autowired
  private ShopCommand shopCommand;

  @Autowired
  private RestCommand restCommand;

  @Autowired
  private DungeonCommand dungeonCommand;

  @Autowired
  private DungeonManager dungeonManager;

  @Autowired
  private DungeonNavigator dungeonNavigator;

  @Autowired
  private DrpgBattleService battleService;

  @Autowired
  private PartyService partyService;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  private Player player;

  @BeforeEach
  void setUp() {
    MessageOutput mockOutput = new MessageOutput() {
      @Override
      public void sendJson(Object payload) {}

      @Override
      public void close() {}

      @Override
      public WebSocketSession getSession() {
        return null;
      }
    };
    player = Player.createSinglePlayer(mockOutput, worldManager, playerService, "求道修士");
    player.setCurrentRoomId("newbie_village:inn");
    player.getStats().setCoin(100);
    player.start();
    worldManager.addLivingActor(player);
  }

  @Test
  @DisplayName("驗證新手村整備 -> 進入墨竹礦坑 -> DRPG 探索戰鬥 -> 安全撤出回村之完整閉環")
  void testCompleteVillageToDungeonLoop() {
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      Party party = partyService.getOrCreateParty(player.getName());

      // ==========================================
      // 階段 1：城鎮客棧整備 (Town Prep & Shopping)
      // ==========================================
      // 1.1 查看貨架
      shopCommand.execute("list");

      // 1.2 購買村莊烤麵包 (價格 2 靈石)
      shopCommand.execute("buy 1");
      assertThat(player.getStats().getCoin()).isEqualTo(98);

      // 1.3 購買辟邪清心靈茶 (價格 5 靈石)
      shopCommand.execute("buy tea");
      assertThat(player.getStats().getCoin()).isEqualTo(93);

      // 1.4 購買百草金創藥膏 (價格 8 靈石)
      shopCommand.execute("buy salve");
      assertThat(player.getStats().getCoin()).isEqualTo(85);

      // 驗證購買物已存入隊伍行囊
      var invSlots = party.getInventory().getSlots();
      assertThat(invSlots).anyMatch(s -> s.getItemId().contains("village_bread"));
      assertThat(invSlots).anyMatch(s -> s.getItemId().contains("purify_tea"));
      assertThat(invSlots).anyMatch(s -> s.getItemId().contains("healing_salve"));

      // 1.5 模擬隊員受創與 San 值耗損後在客棧安歇 (Rest)
      PartyMember protagonist = party.getMembers().get(0);
      protagonist.getStats().setHp(50);
      protagonist.setCurrentSan(30);
      restCommand.execute("");

      // 客棧安歇應全滿回復且不累積靈壓警戒
      assertThat(protagonist.getStats().getHp()).isEqualTo(protagonist.getStats().getMaxHp());
      assertThat(protagonist.getCurrentSan()).isEqualTo(protagonist.getMaxSan());

      // ==========================================
      // 階段 2：出發前往墨竹礦坑 (Travel to Mines)
      // ==========================================
      // 從客棧走向礦坑入口
      player.setCurrentRoomId("mozhu_mines:mine_entrance");

      // ==========================================
      // 階段 3：踏入墨竹礦坑 B1F (Dungeon Crawling)
      // ==========================================
      dungeonCommand.execute("enter mozhu_mines_b1f");
      DungeonPosition pos = dungeonManager.getOrCreatePosition(player.getName(), null);
      assertThat(pos.getFloorId()).isEqualTo("mozhu_mines_b1f");
      assertThat(pos.getX()).isEqualTo(1);
      assertThat(pos.getY()).isEqualTo(8);

      DungeonFloor floor = dungeonManager.getFloor("mozhu_mines_b1f");
      assertNotNull(floor);

      // 3.1 步進探索 (1, 8) -> (1, 7)
      var step = dungeonNavigator.moveForward(floor, pos);
      assertTrue(step.success());
      assertThat(pos.getY()).isEqualTo(7);

      // 3.2 使用購入的清心靈茶回復理智
      PartyItemSlot teaSlot = party.getInventory().getSlots().stream()
          .filter(s -> s.getItemId().contains("purify_tea"))
          .findFirst().orElse(null);
      assertNotNull(teaSlot);

      protagonist.setCurrentSan(70);
      String useMsg = party.useItemOnMember(teaSlot.getSlotId(), 0);
      assertThat(useMsg).contains("道心平復");
      assertThat(protagonist.getCurrentSan()).isEqualTo(95);

      // ==========================================
      // 階段 4：首領祭壇討伐與戰利品結算 (Boss Slay)
      // ==========================================
      battleService.startBossBattle(player, pos, "mozhu_mines:boss_song_tianheng");
      assertTrue(battleService.isInBattle(player.getName()));

      // 擊敗首領並結算
      var ctx = battleService.getActiveBattle(player.getName());
      assertNotNull(ctx);
      ctx.getEnemies().forEach(e -> {
        e.setHp(0);
        e.setAlive(false);
      });
      battleService.resolveVictory(player, ctx, pos);
      assertThat(battleService.isInBattle(player.getName())).isFalse();

      // 驗證首領戰利品已掉落入行囊
      assertThat(party.getInventory().getSlots())
          .anyMatch(s -> s.getItemId().contains("black_obsidian_scythe"));

      // ==========================================
      // 階段 5：重返地表撤離點與結算回村 (Extraction)
      // ==========================================
      // 走回起點階梯 (1, 8) 撤離
      pos.setCoord(1, 8);
      dungeonCommand.execute("leave");

      // 玩家順利重返地表礦坑入口
      assertThat(player.getCurrentRoomId()).isEqualTo("mozhu_mines:mine_entrance");

      // 返回客棧再次整備
      player.setCurrentRoomId("newbie_village:inn");
      restCommand.execute("");
      assertThat(protagonist.getStats().getHp()).isEqualTo(protagonist.getStats().getMaxHp());
    });
  }
}
