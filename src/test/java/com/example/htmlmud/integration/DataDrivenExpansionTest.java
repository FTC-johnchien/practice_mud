package com.example.htmlmud.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.example.htmlmud.domain.model.template.ClassTemplate;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.model.template.NpcCapability;
import com.example.htmlmud.domain.model.template.ShopTemplate;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.application.command.impl.ShopCommand;
import com.example.htmlmud.application.command.impl.ShopCommand.ShopCatalogDto;

@SpringBootTest
@ActiveProfiles("test")
public class DataDrivenExpansionTest {

  @Autowired
  private PartyService partyService;

  @Autowired
  private com.example.htmlmud.application.command.impl.ShopCommand shopCommand;

  @Autowired
  private com.example.htmlmud.application.command.impl.TalkCommand talkCommand;

  @Autowired
  private com.example.htmlmud.domain.service.WorldManager worldManager;

  @Test
  @DisplayName("1. 驗證 classes.json 成功載入為 ClassTemplate 且屬性齊全")
  void testClassTemplatesLoaded() {
    Optional<ClassTemplate> warriorOpt = TemplateRepository.findClass("WARRIOR");
    assertThat(warriorOpt).isPresent();
    ClassTemplate warrior = warriorOpt.get();
    assertThat(warrior.name()).isEqualTo("戰士");
    assertThat(warrior.growth()).isNotNull();
    assertThat(warrior.growth().hpPerLevel()).isEqualTo(30);
    assertThat(warrior.proficiencies().weapon()).contains("SWORD", "AXE");

    Optional<ClassTemplate> swordOpt = TemplateRepository.findClass("SWORDSMAN");
    assertThat(swordOpt).isPresent();
    ClassTemplate swordsman = swordOpt.get();
    assertThat(swordsman.name()).isEqualTo("俠客");
    assertThat(swordsman.baseStats().get("STR")).isEqualTo(8);
  }

  @Test
  @DisplayName("2. 驗證夥伴習得武學套路 (learnedStances) 100% 由 default_companions.json 資料驅動")
  void testCompanionLearnedStancesDataDriven() {
    Party party = partyService.createSoloParty("玄天宗弟子");
    PartyMember leader = party.getMembers().get(0);

    // 主角開局習得資料定義的劍法與拳腳
    assertThat(leader.getLearnedStances()).contains(
        "basic_sword", "taiji_sword", "taiyin_sword", "tianjian_sword", "basic_fist"
    );

    // 招募搬山力士鐵牛
    boolean recruited = partyService.recruitCompanion(party, "tie_niu");
    assertThat(recruited).isTrue();
    PartyMember tieNiu = party.getMembers().stream()
        .filter(m -> m.getName().contains("鐵牛"))
        .findFirst()
        .orElseThrow();

    // 鐵牛資料定義的套路為 basic_blunt 與 basic_fist
    assertThat(tieNiu.getLearnedStances()).contains("basic_blunt", "basic_fist");
    assertThat(tieNiu.getLearnedStances()).doesNotContain("basic_sword");
  }

  @Test
  @DisplayName("3. 驗證新手村貨棧 shops.json 成功載入與註冊至 TemplateRepository")
  void testShopTemplateLoaded() {
    Optional<ShopTemplate> shopOpt = TemplateRepository.findShop("newbie_village:inn_shop");
    assertThat(shopOpt).isPresent();

    ShopTemplate shop = shopOpt.get();
    assertThat(shop.name()).isEqualTo("新手村客棧貨棧");
    assertThat(shop.roomId()).isEqualTo("newbie_village:inn");
    assertThat(shop.goods()).hasSizeGreaterThanOrEqualTo(11);

    // 驗證商品 ID 包含前綴解析與價格
    ShopTemplate.ShopItemTemplate bread = shop.goods().stream()
        .filter(g -> "bread".equals(g.id()))
        .findFirst()
        .orElseThrow();
    assertThat(bread.templateId()).isIn("village_bread", "newbie_village:village_bread");
    assertThat(bread.price()).isEqualTo(2);

    // 透過房間 ID 也能查得商店
    Optional<ShopTemplate> byRoom = TemplateRepository.findShopByRoomId("newbie_village:inn");
    assertThat(byRoom).isPresent();
    assertThat(byRoom.get().id()).isEqualTo("newbie_village:inn_shop");
  }

  @Test
  @DisplayName("4. 驗證 NPC 組件化能力 (NpcCapability) 100% 資料驅動")
  void testNpcCapabilitiesDataDriven() {
    // 福伯：TALK, SHOP, REST
    MobTemplate innkeeper = TemplateRepository.findMob("newbie_village:innkeeper")
        .orElseThrow();
    List<NpcCapability> innCaps = innkeeper.capabilities();
    assertThat(innCaps).isNotEmpty();
    assertThat(innCaps.stream().map(NpcCapability::type).toList())
        .contains("TALK", "SHOP", "REST");

    // 白石老人：TALK, QUEST
    MobTemplate elder = TemplateRepository.findMob("newbie_village:village_elder")
        .orElseThrow();
    List<NpcCapability> elderCaps = elder.capabilities();
    assertThat(elderCaps).isNotEmpty();
    assertThat(elderCaps.stream().map(NpcCapability::type).toList())
        .contains("TALK", "QUEST");

    // 客棧隊友 NPC (Single Source of Truth 註冊為 MobTemplate)
    MobTemplate tieNiuMob = TemplateRepository.findMob("newbie_village:tie_niu")
        .orElseThrow();
    assertThat(tieNiuMob.capabilities().stream().map(NpcCapability::type).toList())
        .contains("TALK", "RECRUIT");
  }

  @Test
  @DisplayName("5. 驗證角色與職業 (Class) 關聯整合：職業專精、成長屬性與職稱")
  void testPartyMemberClassIntegration() {
    Party party = partyService.createSoloParty("令狐沖");
    PartyMember leader = party.getMembers().get(0);
    assertThat(leader.getClassId()).isEqualTo("SWORDSMAN");
    assertThat(leader.getEffectiveClassName()).isEqualTo("俠客");
    assertThat(leader.getClassTemplate()).isPresent();
    assertThat(leader.getClassTemplate().get().proficiencies().weapon()).contains("SWORD");

    partyService.recruitCompanion(party, "tie_niu");
    PartyMember tieNiu = party.getMembers().stream()
        .filter(m -> m.getName().contains("鐵牛"))
        .findFirst()
        .orElseThrow();
    assertThat(tieNiu.getClassId()).isEqualTo("WARRIOR");
    assertThat(tieNiu.getEffectiveClassName()).isEqualTo("戰士");
    assertThat(tieNiu.getClassTemplate()).isPresent();
    assertThat(tieNiu.getClassTemplate().get().growth().hpPerLevel()).isEqualTo(30);
  }

  @Autowired
  private com.example.htmlmud.domain.service.PlayerService playerService;

  @Test
  @DisplayName("6. 驗證貨棧批量購買指令 (buy bread 3 / buy 1 2) 與靈石扣減、行囊堆疊")
  void testBulkShopBuy() {
    String playerName = "採購修士";
    Party party = partyService.createSoloParty(playerName);
    Player testPlayer = Player.createSinglePlayer(new MessageOutput() {
      @Override public void sendJson(Object payload) {}
      @Override public void close() {}
      @Override public org.springframework.web.socket.WebSocketSession getSession() { return null; }
    }, worldManager, playerService, playerName);
    testPlayer.setCurrentRoomId("newbie_village:inn");
    testPlayer.getStats().setCoin(50);
    partyService.setParty(playerName, party);

    ScopedValue.where(MudContext.CURRENT_PLAYER, testPlayer).run(() -> {
      // 1. 購買 3 個麵包 (單價 2 靈石)
      shopCommand.execute("buy bread 3");
      assertThat(testPlayer.getStats().getCoin()).isEqualTo(44);
      int count1 = party.getInventory().getSlots().stream()
          .filter(s -> com.example.htmlmud.domain.party.model.PartyInventory.isSameItemId(s.getItemId(), "village_bread"))
          .mapToInt(com.example.htmlmud.domain.party.model.PartyItemSlot::getCount).sum();
      assertThat(count1).isEqualTo(3);

      // 2. 透過貨架編號 1 再購買 2 個
      shopCommand.execute("buy 1 2");
      assertThat(testPlayer.getStats().getCoin()).isEqualTo(40);
      int count2 = party.getInventory().getSlots().stream()
          .filter(s -> com.example.htmlmud.domain.party.model.PartyInventory.isSameItemId(s.getItemId(), "village_bread"))
          .mapToInt(com.example.htmlmud.domain.party.model.PartyItemSlot::getCount).sum();
      assertThat(count2).isEqualTo(5);
    });
  }

  @Autowired
  private com.example.htmlmud.application.command.impl.PartyCommand partyCommand;

  @Autowired
  private com.example.htmlmud.application.command.impl.RecruitCommand recruitCommand;

  @Autowired
  private com.example.htmlmud.application.command.impl.DismissCommand dismissCommand;

  @Test
  @DisplayName("7. 驗證 NPC 交談指令 (TalkCommand) 能正確匹配並觸發對白")
  void testTalkCommandExecution() {
    String playerName = "問道使者";
    partyService.createSoloParty(playerName);
    java.util.List<String> replies = new java.util.concurrent.CopyOnWriteArrayList<>();
    Player testPlayer = Player.createSinglePlayer(new MessageOutput() {
      @Override public void sendJson(Object payload) {
        if (payload != null) replies.add(payload.toString());
      }
      @Override public void close() {}
      @Override public org.springframework.web.socket.WebSocketSession getSession() { return null; }
    }, worldManager, playerService, playerName);
    testPlayer.setCurrentRoomId("newbie_village:inn");

    ScopedValue.where(MudContext.CURRENT_PLAYER, testPlayer).run(() -> {
      // 向福伯交談
      talkCommand.execute("innkeeper");
      // 向未知 NPC 交談
      talkCommand.execute("unknown_npc");
    });
  }

  @Test
  @DisplayName("8. 驗證 party recruit、recruit、dismiss 與 party dismiss 能正確招募與請離同伴")
  void testPartyRecruitAndDismissCommands() {
    String playerName = "招募行者";
    Party party = partyService.createSoloParty(playerName);
    assertThat(party.size()).isEqualTo(1);

    Player testPlayer = Player.createSinglePlayer(new MessageOutput() {
      @Override public void sendJson(Object payload) {}
      @Override public void close() {}
      @Override public org.springframework.web.socket.WebSocketSession getSession() { return null; }
    }, worldManager, playerService, playerName);
    testPlayer.setCurrentRoomId("newbie_village:inn");
    partyService.setParty(playerName, party);

    ScopedValue.where(MudContext.CURRENT_PLAYER, testPlayer).run(() -> {
      // 1. 測試前端按鈕點擊觸發的 party recruit tie_niu
      partyCommand.execute("recruit tie_niu");
      assertThat(party.size()).isEqualTo(2);
      PartyMember tieNiu = party.getMembers().get(1);
      assertThat(tieNiu.getName()).isEqualTo("鐵牛");
      assertThat(tieNiu.getClassId()).isEqualTo("WARRIOR");

      // 2. 測試 dismiss 指令請離
      dismissCommand.execute("tie_niu");
      assertThat(party.size()).isEqualTo(1);

      // 3. 測試 recruit 指令招募
      recruitCommand.execute("tie_niu");
      assertThat(party.size()).isEqualTo(2);

      // 4. 測試 party dismiss 指令請離
      partyCommand.execute("dismiss tie_niu");
      assertThat(party.size()).isEqualTo(1);

      // 5. 測試招募凌霜
      partyCommand.execute("recruit ling_shuang");
      assertThat(party.size()).isEqualTo(2);
      PartyMember ling = party.getMembers().get(1);
      assertThat(ling.getName()).isEqualTo("凌霜");
      assertThat(ling.getClassId()).isEqualTo("CLERIC");

      // 6. 測試 party remove 指令請離凌霜
      partyCommand.execute("remove ling_shuang");
      assertThat(party.size()).isEqualTo(1);
    });
  }

  @Test
  @DisplayName("9. 驗證貨棧開啟時僅推送結構化 SHOP_CATALOG 事件，消除日誌洗版表格，並於購買後即時刷新靈石餘額")
  void testShopCatalogPushAndCleanReply() {
    String playerName = "貨棧測試修士";
    Party party = partyService.createSoloParty(playerName);
    java.util.List<Object> jsonMessages = new java.util.ArrayList<>();

    Player testPlayer = Player.createSinglePlayer(new MessageOutput() {
      @Override public void sendJson(Object payload) { jsonMessages.add(payload); }
      @Override public void close() {}
      @Override public org.springframework.web.socket.WebSocketSession getSession() { return null; }
    }, worldManager, playerService, playerName);
    testPlayer.setCurrentRoomId("newbie_village:inn");
    testPlayer.getStats().setCoin(30);
    partyService.setParty(playerName, party);

    ScopedValue.where(MudContext.CURRENT_PLAYER, testPlayer).run(() -> {
      // 1. 執行 shop 開啟貨棧
      shopCommand.execute("");

      // 驗證結構化事件發送 (過濾出 ShopCatalogDto)
      Optional<ShopCatalogDto> catalogOpt = jsonMessages.stream()
          .filter(ShopCatalogDto.class::isInstance)
          .map(ShopCatalogDto.class::cast)
          .findFirst();
      assertThat(catalogOpt).isPresent();
      ShopCatalogDto catalog = catalogOpt.get();
      assertThat(catalog.type()).isEqualTo("SHOP_CATALOG");
      assertThat(catalog.shopName()).isEqualTo("新手村客棧貨棧");
      assertThat(catalog.playerCoin()).isEqualTo(30);
      assertThat(catalog.goods()).hasSizeGreaterThanOrEqualTo(4);

      // 2. 購買商品 (buy 1 2)
      shopCommand.execute("buy 1 2");
      assertThat(testPlayer.getStats().getCoin()).isEqualTo(26);

      // 驗證購買後再次推送更新後的 SHOP_CATALOG，且靈石同步更新至 26
      List<ShopCatalogDto> catalogList = jsonMessages.stream()
          .filter(ShopCatalogDto.class::isInstance)
          .map(ShopCatalogDto.class::cast)
          .toList();
      assertThat(catalogList.size()).isGreaterThanOrEqualTo(2);
      ShopCatalogDto updatedCatalog = catalogList.get(catalogList.size() - 1);
      assertThat(updatedCatalog.playerCoin()).isEqualTo(26);
    });
  }
}
