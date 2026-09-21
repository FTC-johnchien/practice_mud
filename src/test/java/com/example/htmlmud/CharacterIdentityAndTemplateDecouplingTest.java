package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.dungeon.model.Direction;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.vo.CharacterId;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.TemplateCatalog;
import com.example.htmlmud.domain.service.WorldManager;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import com.example.htmlmud.domain.actor.core.MessageOutput;

@SpringBootTest
@ActiveProfiles("test")
class CharacterIdentityAndTemplateDecouplingTest {

  @Autowired
  private PartyService partyService;

  @Autowired
  private DungeonManager dungeonManager;

  @Autowired
  private TemplateRepository templateRepository;

  @Autowired
  private TemplateCatalog templateCatalog;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  private final MessageOutput dummyOutput = new MessageOutput() {
    @Override
    public void sendJson(Object payload) {}

    @Override
    public void close() {}

    @Override
    public org.springframework.web.socket.WebSocketSession getSession() {
      return null;
    }
  };

  @Nested
  @DisplayName("一、CharacterId 強型別值物件測試")
  class CharacterIdTests {

    @Test
    @DisplayName("應拒絕空字串或 null")
    void rejectsInvalidValues() {
      assertThatThrownBy(() -> new CharacterId(""))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new CharacterId(null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("of() 工廠方法應正常去除空白並包裝")
    void factoryOfWorks() {
      CharacterId id = CharacterId.of("  hero-101  ");
      assertThat(id.value()).isEqualTo("hero-101");
      assertThat(id.toString()).isEqualTo("hero-101");
    }

    @Test
    @DisplayName("generate() 應產生帶前綴的唯一識別碼")
    void generatePrefix() {
      CharacterId id1 = CharacterId.generate("player");
      CharacterId id2 = CharacterId.generate("player");
      assertThat(id1.value()).startsWith("player-");
      assertThat(id2.value()).startsWith("player-");
      assertThat(id1.value()).isNotEqualTo(id2.value());
    }

    @Test
    @DisplayName("fromPlayer 應消除 p-single 假象並以有效 ID 或名稱做唯一標識")
    void fromPlayerResolvesProperly() {
      Player singlePlayer = Player.createSinglePlayer(dummyOutput, worldManager, playerService, "雲遊真人");
      assertThat(singlePlayer.getId()).isNotEqualTo("p-single");
      assertThat(singlePlayer.getCharacterId().value()).isEqualTo("雲遊真人");

      Player customIdPlayer = Player.createSinglePlayer(dummyOutput, worldManager, playerService, "天機子", "p-session-999");
      assertThat(customIdPlayer.getId()).isEqualTo("p-session-999");
      assertThat(customIdPlayer.getCharacterId().value()).isEqualTo("p-session-999");
    }
  }

  @Nested
  @DisplayName("二、多玩家會話隔離與雙向別名映射測試")
  class SessionBoundaryAndAliasTests {

    @Test
    @DisplayName("多名單機玩家實例應擁有各自獨立的 CharacterId，不再發生碰撞")
    void distinctPlayersHaveDistinctIdentities() {
      Player p1 = Player.createSinglePlayer(dummyOutput, worldManager, playerService, "青鋒道長", "p-session-1");
      Player p2 = Player.createSinglePlayer(dummyOutput, worldManager, playerService, "烈陽仙子", "p-session-2");

      assertThat(p1.getId()).isNotEqualTo(p2.getId());
      assertThat(p1.getCharacterId()).isNotEqualTo(p2.getCharacterId());
    }

    @Test
    @DisplayName("PartyService 雙向別名：以 ID 建立隊伍後，以角色名或 Player 查詢皆回傳同一實例")
    void partyServiceDualAliasResolution() {
      String sessionId = "p-sync-test-session";
      String protagonistName = "太虛散人";

      Party party = partyService.resetParty(sessionId, protagonistName);
      party.setFormationEnergy(77);

      // 以 sessionId 查
      Party byId = partyService.getOrCreateParty(sessionId);
      // 以 protagonistName 查
      Party byName = partyService.getOrCreateParty(protagonistName);

      // 以 Player 物件查
      Player player = Player.createSinglePlayer(dummyOutput, worldManager, playerService, protagonistName, sessionId);
      Party byPlayer = partyService.getOrCreateParty(player);

      assertThat(byId).isSameAs(party);
      assertThat(byName).isSameAs(party);
      assertThat(byPlayer).isSameAs(party);
      assertThat(byName.getFormationEnergy()).isEqualTo(77);
    }

    @Test
    @DisplayName("DungeonManager 多載方法：支援以 CharacterId 與 Player 儲存及查詢坐標")
    void dungeonManagerSupportsCharacterIdAndPlayer() {
      Player player = Player.createSinglePlayer(dummyOutput, worldManager, playerService, "風靈子", "p-dungeon-test");
      DungeonPosition pos = new DungeonPosition("mozhu_mines_b1f", 3, 5, Direction.EAST, 10, 10);

      dungeonManager.setPlayerPosition(player.getCharacterId(), pos);

      DungeonPosition readByPlayer = dungeonManager.getPlayerPosition(player);
      DungeonPosition readById = dungeonManager.getPlayerPosition(player.getCharacterId());
      DungeonPosition readByStr = dungeonManager.getPlayerPosition("p-dungeon-test");

      assertThat(readByPlayer).isSameAs(pos);
      assertThat(readById).isSameAs(pos);
      assertThat(readByStr).isSameAs(pos);
      assertThat(readByPlayer.getX()).isEqualTo(3);
      assertThat(readByPlayer.getY()).isEqualTo(5);
    }
  }

  @Nested
  @DisplayName("三、TemplateRepository Spring Bean 化與解耦測試")
  class TemplateDecouplingTests {

    @Test
    @DisplayName("TemplateRepository 應作為 Spring Bean 成功注入，且其實例方法與靜態方法皆回傳同源資料")
    void templateRepositoryBeanAndStaticConsistency() {
      assertThat(templateRepository).isNotNull();

      var ironSwordOpt = templateRepository.getItem("iron_sword");
      assertThat(ironSwordOpt).isPresent();

      var staticIronSwordOpt = TemplateRepository.findItem("iron_sword");
      assertThat(staticIronSwordOpt).isPresent();
      assertThat(staticIronSwordOpt.get().name()).isEqualTo(ironSwordOpt.get().name());
    }

    @Test
    @DisplayName("TemplateCatalog 透過注入的 TemplateRepository 能正確解析原型")
    void templateCatalogResolvesThroughInjectedRepository() {
      ItemTemplate ironSword = templateCatalog.resolveItem("iron_sword");
      assertThat(ironSword).isNotNull();
      assertThat(ironSword.name()).isEqualTo("精鋼劍");

      var bronzeSwordOpt = templateCatalog.findItem("bronze_sword");
      assertThat(bronzeSwordOpt).isPresent();
    }
  }
}
