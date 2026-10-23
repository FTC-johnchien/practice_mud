package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketSession;

import com.example.htmlmud.application.command.impl.ShopCommand;
import com.example.htmlmud.application.command.impl.ShopCommand.ShopCatalogDto;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.DrpgRewardService;
import com.example.htmlmud.domain.dungeon.dto.BattleVictoryDto;
import com.example.htmlmud.domain.model.enums.MobRank;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.template.TemplateReader;
import com.example.htmlmud.domain.actor.service.PlayerService;
import com.example.htmlmud.domain.world.WorldManager;

/**
 * SP-09: 小隊共享金庫與經濟體系大一統整合測試 (Unified Party Economy & Coin Tests)
 */
@SpringBootTest
@ActiveProfiles("test")
class UnifiedPartyEconomyTest {

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private PartyService partyService;

  @Autowired
  private ShopCommand shopCommand;

  @Autowired
  private DrpgRewardService drpgRewardService;

  @Autowired
  private TemplateReader templateReader;

  private Player testPlayer;
  private final List<Object> jsonMessages = new CopyOnWriteArrayList<>();

  @BeforeEach
  void setUp() {
    jsonMessages.clear();

    MessageOutput mockOutput = new MessageOutput() {
      @Override
      public void sendJson(Object payload) {
        if (payload != null) {
          jsonMessages.add(payload);
        }
      }

      @Override
      public void close() {}

      @Override
      public WebSocketSession getSession() {
        return null;
      }
    };

    testPlayer = Player.createSinglePlayer(mockOutput, worldManager, playerService, "玄陽道人");
    testPlayer.setCurrentRoomId("newbie_village:inn");
    partyService.clearCache();
  }

  @AfterEach
  void tearDown() {
    partyService.clearCache();
    if (testPlayer != null) {
      testPlayer.stop();
    }
  }

  @Test
  @DisplayName("SP-09: 驗證小隊金庫單一真相源與隊長 stats 零延遲鏡像同步")
  void testPartyTreasurySSOTAndLeaderMirroring() {
    Party party = partyService.getOrCreateParty(testPlayer);
    assertThat(party).isNotNull();

    // 1. 驗證初始金庫依配置給予 100 靈石
    assertThat(party.getCoin()).isEqualTo(100);
    assertThat(party.getLeader()).isNotNull();
    assertThat(party.getLeader().getStats().getCoin()).isEqualTo(100);

    // 2. 驗證增減出納
    party.addCoin(50);
    assertThat(party.getCoin()).isEqualTo(150);
    assertThat(party.getLeader().getStats().getCoin()).isEqualTo(150);

    boolean deducted = party.deductCoin(70);
    assertThat(deducted).isTrue();
    assertThat(party.getCoin()).isEqualTo(80);
    assertThat(party.getLeader().getStats().getCoin()).isEqualTo(80);

    // 3. 驗證透支防禦
    boolean overdraft = party.deductCoin(200);
    assertThat(overdraft).isFalse();
    assertThat(party.getCoin()).isEqualTo(80);
    assertThat(party.getLeader().getStats().getCoin()).isEqualTo(80);

    // 4. 驗證直接設定
    party.setCoin(300);
    assertThat(party.getCoin()).isEqualTo(300);
    assertThat(party.getLeader().getStats().getCoin()).isEqualTo(300);
  }

  @Test
  @DisplayName("SP-09: 驗證商店購買由小隊共享金庫出納，公共行囊入庫，隊長個人 stats 同步扣款")
  void testShopBuyDeductsFromPartyTreasuryAndSyncsStats() {
    Party party = partyService.getOrCreateParty(testPlayer);
    party.setCoin(50);
    testPlayer.getStats().setCoin(50);

    ScopedValue.where(MudContext.CURRENT_PLAYER, testPlayer).run(() -> {
      // 1. 查看商店貨架清單
      shopCommand.execute("list");

      Optional<ShopCatalogDto> catalogOpt = jsonMessages.stream()
          .filter(ShopCatalogDto.class::isInstance)
          .map(ShopCatalogDto.class::cast)
          .findFirst();
      assertThat(catalogOpt).isPresent();
      assertThat(catalogOpt.get().playerCoin()).isEqualTo(50);

      // 2. 執行購買 2 份乾糧 (編號 1 商品，單價 2 靈石)
      shopCommand.execute("buy 1 2");

      // 3. 檢驗小隊金庫扣款：50 - (2 * 2) = 46 靈石
      assertThat(party.getCoin()).isEqualTo(46);
      assertThat(testPlayer.getStats().getCoin()).isEqualTo(46);

      // 4. 檢驗小隊 40 格公共行囊入庫
      assertThat(party.getInventory().getSlots()).isNotEmpty();
      boolean hasPurchasedItem = party.getInventory().getSlots().stream()
          .anyMatch(s -> s.getCount() >= 2);
      assertThat(hasPurchasedItem).isTrue();
    });
  }

  @Test
  @DisplayName("SP-09: 驗證商店回收出售小隊公共行囊物品，獲取靈石直接入帳小隊金庫")
  void testShopSellRecyclesPartyInventoryToTreasury() {
    Party party = partyService.getOrCreateParty(testPlayer);
    party.setCoin(10);
    testPlayer.getStats().setCoin(10);

    // 預先放入小隊公共行囊：止血草 x5 (假設原價 2 靈石，半價回收單價 1 靈石)
    party.getInventory().addItem("newbie_village:herb_healing", 5);
    int initialCount = party.getInventory().getItemCount("newbie_village:herb_healing");
    assertThat(initialCount).isGreaterThanOrEqualTo(5);

    ScopedValue.where(MudContext.CURRENT_PLAYER, testPlayer).run(() -> {
      // 1. 出售 2 件草藥
      shopCommand.execute("sell newbie_village:herb_healing 2");

      // 2. 檢驗小隊公共行囊扣除 2 件
      assertThat(party.getInventory().getItemCount("newbie_village:herb_healing")).isEqualTo(initialCount - 2);

      // 3. 檢驗小隊金庫入帳 (獲得靈石，總額 >= 12)
      assertThat(party.getCoin()).isGreaterThanOrEqualTo(12);
      assertThat(testPlayer.getStats().getCoin()).isEqualTo(party.getCoin());
    });
  }

  @Test
  @DisplayName("SP-09: 驗證戰鬥大捷斬除妖邪，繳獲靈石直接入帳小隊金庫，並推送 BattleVictoryDto")
  void testBattleVictoryAwardsCoinDirectlyToPartyTreasury() {
    Party party = partyService.getOrCreateParty(testPlayer);
    party.setCoin(100);
    testPlayer.getStats().setCoin(100);

    // 建立遭遇戰敵人：1 隻 NORMAL 妖獸 (8 靈石) + 1 隻 ELITE 妖首 (25 靈石)
    BattleEnemy enemy1 = BattleEnemy.builder()
        .id("mob-1")
        .name("狂暴黑狼")
        .rank(MobRank.NORMAL)
        .hp(0)
        .maxHp(100)
        .alive(false)
        .xp(50)
        .build();

    BattleEnemy enemy2 = BattleEnemy.builder()
        .id("mob-2")
        .name("幽煞狼王")
        .rank(MobRank.ELITE)
        .hp(0)
        .maxHp(200)
        .alive(false)
        .xp(100)
        .build();

    BattleContext ctx = BattleContext.builder()
        .battleId("battle-economy-test")
        .party(party)
        .enemies(List.of(enemy1, enemy2))
        .build();

    // 結算戰鬥大捷
    drpgRewardService.resolveVictory(testPlayer, ctx, null, null, null);

    // 檢驗小隊金庫獲得靈石獎勵：100 + (8 + 25) = 133 靈石
    assertThat(party.getCoin()).isEqualTo(133);
    assertThat(testPlayer.getStats().getCoin()).isEqualTo(133);

    // 檢驗推送 BattleVictoryDto 且包含繳獲靈石 33
    Optional<BattleVictoryDto> victoryDtoOpt = jsonMessages.stream()
        .filter(BattleVictoryDto.class::isInstance)
        .map(BattleVictoryDto.class::cast)
        .findFirst();
    assertThat(victoryDtoOpt).isPresent();
    assertThat(victoryDtoOpt.get().coinsGained()).isEqualTo(33);
  }
}
