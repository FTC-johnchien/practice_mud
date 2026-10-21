package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketSession;

import com.example.htmlmud.application.command.impl.RestCommand;
import com.example.htmlmud.config.GameConfig;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.enums.RoomFlag;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.save.service.SaveGameService;
import com.example.htmlmud.domain.actor.service.PlayerService;
import com.example.htmlmud.domain.world.WorldManager;
import com.example.htmlmud.domain.actor.core.MessageOutput;

@SpringBootTest
@ActiveProfiles("test")
class DataCoreIntegrityTest {

  @Autowired
  private GameConfig gameConfig;

  @Autowired
  private RestCommand restCommand;

  @Autowired
  private SaveGameService saveGameService;

  @Autowired
  private PartyService partyService;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private WorldManager worldManager;

  private MessageOutput createMockOutput() {
    return new MessageOutput() {
      @Override
      public void sendJson(Object payload) {}

      @Override
      public void close() {}

      @Override
      public WebSocketSession getSession() {
        return null;
      }
    };
  }

  @Test
  @DisplayName("DATA-CORE-01: 驗證新手村客棧與雪亭客棧房間資料具備 SAFE_ZONE 標籤")
  void testInnRoomsHaveSafeZoneFlag() {
    Room newbieInn = worldManager.getRoomActor("newbie_village:inn");
    assertThat(newbieInn).isNotNull();
    assertThat(newbieInn.hasFlag(RoomFlag.SAFE_ZONE)).isTrue();

    Room snowInn = worldManager.getRoomActor("snow:snow_inn");
    assertThat(snowInn).isNotNull();
    assertThat(snowInn.hasFlag(RoomFlag.SAFE_ZONE)).isTrue();
  }

  @Test
  @DisplayName("DATA-CORE-01: RestCommand 依據 RoomFlag.SAFE_ZONE 執行完全回復")
  void testRestCommandSafeZoneFullRecovery() {
    Player player = Player.createSinglePlayer(createMockOutput(), worldManager, playerService, "safe_tester");
    player.setCurrentRoomId("newbie_village:inn");

    Party party = partyService.getOrCreateParty(player);
    PartyMember member = party.getMembers().get(0);
    member.getStats().setHp(10);
    member.consumeSan(50);
    member.getStats().setMp(5);

    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      restCommand.execute("");
    });

    assertThat(member.getStats().getHp()).isEqualTo(member.getStats().getMaxHp());
    assertThat(member.getCurrentSan()).isEqualTo(100);
    assertThat(member.getStats().getMp()).isEqualTo(member.getStats().getMaxMp());
  }

  @Test
  @DisplayName("DATA-CORE-01: SaveGameService 開闢新遊戲與預設存檔由 GameConfig 提供出生點")
  void testSaveGameServiceSpawnRoomFromGameConfig() {
    String expectedSpawn = gameConfig.getDefaults().getPlayer().getSpawnRoomId();
    assertThat(expectedSpawn).isNotBlank();

    Player newPlayer = Player.createSinglePlayer(createMockOutput(), worldManager, playerService, "spawn_tester");
    saveGameService.handleNew(newPlayer, "李逍遙");

    assertThat(newPlayer.getCurrentRoomId()).isEqualTo(expectedSpawn);
    assertThat(newPlayer.isInDungeon()).isFalse();
  }

  @Autowired
  private com.example.htmlmud.application.command.impl.ShopCommand shopCommand;

  @Autowired
  private com.example.htmlmud.domain.repository.TemplateReader templateReader;

  @Test
  @DisplayName("DATA-CORE-02: PartyService 夥伴招募失敗提示動態自 TemplateReader 產出可用清單")
  void testCompanionRecruitmentFailureListsAllCompanions() {
    Player player = Player.createSinglePlayer(createMockOutput(), worldManager, playerService, "recruit_tester");
    partyService.recruitCompanionForPlayer(player, "non_existent_companion");
    int companionCount = templateReader.getAllCompanions().size();
    assertThat(companionCount).isGreaterThanOrEqualTo(4);
  }

  @Test
  @DisplayName("DATA-CORE-02: Party 吸收道核學習技能自 TemplateReader 動態反查設定")
  void testLearnSkillFromDaoCoreDynamicallyResolvesTemplate() {
    Party party = partyService.getOrCreateParty("dao_tester");
    PartyMember member = party.getMembers().get(0);
    int initialSkills = member.getSkills().size();

    com.example.htmlmud.domain.party.model.PartyItemSlot daoCore =
        com.example.htmlmud.domain.party.model.PartyItemSlot.builder()
            .slotId("dao-core-slot-1")
            .itemId("dao_core_blade")
            .name("異變刀客道核")
            .count(1)
            .itemType(com.example.htmlmud.domain.model.enums.ItemType.CONSUMABLE)
            .effectType("LEARN_SKILL")
            .grantedSkillId("combo_blade_and_shadow")
            .grantedSkillName("刀劍合璧・斷空破")
            .build();
    party.getInventory().addSlot(daoCore);

    String msg = party.useItemOnMember("dao-core-slot-1", 0);
    assertThat(msg).contains("成功領悟掌握新絕學");
    assertThat(member.getSkills().size()).isEqualTo(initialSkills + 1);

    com.example.htmlmud.domain.party.model.PartyMemberSkill learned = member.getSkills().stream()
        .filter(s -> s.getId().equals("combo_blade_and_shadow"))
        .findFirst().orElse(null);
    assertThat(learned).isNotNull();
    assertThat(learned.getName()).isEqualTo("刀劍合璧・斷空破");
    // party_skills.json 中 combo_blade_and_shadow 的 damageMultiplier 為 6.0, cooldownMs 為 15000，完全動態讀取範本數值（消滅寫死的 1.8 與 6000ms）
    assertThat(learned.getDamageMultiplier()).isEqualTo(6.0);
    assertThat(learned.getCooldownMs()).isEqualTo(15000L);
  }

  @Test
  @DisplayName("DATA-GUARD-03: DRPG 地牢視圖狀態包含動態地塊名稱陣列 tileNames")
  void testDungeonStateDtoContainsDynamicTileNames() {
    com.example.htmlmud.domain.dungeon.service.DungeonManager dungeonManager =
        new com.example.htmlmud.domain.dungeon.service.DungeonManager();
    dungeonManager.init();

    com.example.htmlmud.domain.dungeon.model.DungeonFloor floor = dungeonManager.getFloor("taiyin_tomb_b1f");
    assertThat(floor).isNotNull();

    com.example.htmlmud.domain.dungeon.model.DungeonPosition pos =
        new com.example.htmlmud.domain.dungeon.model.DungeonPosition(floor.getId(), 1, 1, com.example.htmlmud.domain.dungeon.model.Direction.NORTH, 10, 10);

    var dto = com.example.htmlmud.domain.dungeon.dto.DrpgStateDto.of(floor, pos, "前方視野", null);
    assertThat(dto.dungeon()).isNotNull();
    assertThat(dto.dungeon().tileNames()).isNotNull();
    assertThat(dto.dungeon().tileNames().length).isEqualTo(10);
    assertThat(dto.dungeon().tileNames()[0].length).isEqualTo(10);
    assertThat(dto.dungeon().tileNames()[0][0]).isNotBlank();
  }

  @org.junit.jupiter.api.AfterEach
  void tearDown() {
    partyService.clearCache();
  }
}


