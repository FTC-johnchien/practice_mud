package com.example.htmlmud.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.domain.exception.DataValidationException;
import com.example.htmlmud.domain.model.config.LootEntry;
import com.example.htmlmud.item.EquipmentSlot;
import com.example.htmlmud.template.model.CompanionTemplate;
import com.example.htmlmud.template.model.ItemTemplate;
import com.example.htmlmud.template.model.MobTemplate;
import com.example.htmlmud.template.model.RoomExit;
import com.example.htmlmud.template.model.RoomTemplate;
import com.example.htmlmud.template.model.ShopTemplate;
import com.example.htmlmud.dungeon.DungeonManager;
import com.example.htmlmud.party.FormationTemplate;
import com.example.htmlmud.template.TemplateReader;

@SpringBootTest
class DataIntegrityValidatorTest {

  @Autowired
  private DataIntegrityValidator liveValidator;

  @Test
  @DisplayName("VAL-01: 現有全域遊戲資料集拓撲校驗 100% 通過 (真實容器環境)")
  void testLiveGameDataIntegrityPasses() {
    DataIntegrityValidator.ValidationReport report = liveValidator.validate();
    assertThat(report.isValid()).isTrue();
    assertThat(report.errorCount()).isZero();
    assertThat(report.getItemsChecked()).isGreaterThan(100);
    assertThat(report.getMobsChecked()).isGreaterThan(50);
    assertThat(report.getRoomsChecked()).isGreaterThan(30);
    assertThat(report.getCompanionsChecked()).isGreaterThanOrEqualTo(6);
    assertThat(report.getFormationsChecked()).isGreaterThanOrEqualTo(10);
    assertThat(report.getDungeonsChecked()).isGreaterThanOrEqualTo(2);
    assertThat(report.getSkillsChecked()).isGreaterThan(0);
    assertThat(report.getSuppliesChecked()).isGreaterThan(0);
  }

  @Test
  @DisplayName("GUARD-01: 模擬地牢地塊掉落物斷鏈，在 failFast=true 下精確攔截")
  void testDungeonTileBrokenDropsIntercepted() {
    TemplateReader mockReader = mock(TemplateReader.class);
    DungeonManager mockDungeonManager = mock(DungeonManager.class);
    when(mockDungeonManager.validateDungeons(any(), any(), any())).thenCallRealMethod();
    DataIntegrityValidator validator = new DataIntegrityValidator(mockReader);
    validator.setDungeonManager(mockDungeonManager);
    validator.setFailFast(true);

    com.example.htmlmud.dungeon.DungeonFloor floor =
        com.example.htmlmud.dungeon.DungeonFloor.builder()
            .id("test_floor")
            .width(2)
            .height(2)
            .startCoord(new com.example.htmlmud.dungeon.GridCoord(0, 0))
            .tiles(new com.example.htmlmud.dungeon.DungeonTile[][] {
                {
                    com.example.htmlmud.dungeon.DungeonTile.floor("起點"),
                    com.example.htmlmud.dungeon.DungeonTile.builder()
                        .type(com.example.htmlmud.dungeon.DungeonTile.TileType.TREASURE)
                        .drops(List.of("ghost_drop_item"))
                        .build()
                },
                {
                    com.example.htmlmud.dungeon.DungeonTile.wall(),
                    com.example.htmlmud.dungeon.DungeonTile.wall()
                }
            })
            .build();

    when(mockDungeonManager.getAllFloorIds()).thenReturn(List.of("test_floor"));
    when(mockDungeonManager.getFloor("test_floor")).thenReturn(floor);
    when(mockReader.findItem("ghost_drop_item")).thenReturn(Optional.empty());

    assertThatThrownBy(validator::validate)
        .isInstanceOf(DataValidationException.class)
        .hasMessageContaining("掉落物 [ghost_drop_item] 不存在於物品庫");
  }

  @Test
  @DisplayName("GUARD-01: 模擬技能橋接目標 DRPG 技能不存在，精準報告錯誤")
  void testSkillBrokenBridgeRuleIntercepted() {
    TemplateReader mockReader = mock(TemplateReader.class);
    DataIntegrityValidator validator = new DataIntegrityValidator(mockReader);
    validator.setFailFast(false);

    com.example.htmlmud.template.model.SkillTemplate brokenSkill =
        new com.example.htmlmud.template.model.SkillTemplate();
    brokenSkill.setId("custom_sword");
    brokenSkill.setName("自創劍法");
    brokenSkill.setBridges(List.of(com.example.htmlmud.progression.SkillBridgeRule.of("non_existent_drpg_skill", 1)));

    when(mockReader.getAllSkills()).thenReturn(Map.of("custom_sword", brokenSkill));
    when(mockReader.findPartySkill("non_existent_drpg_skill")).thenReturn(Optional.empty());
    when(mockReader.findSkill("non_existent_drpg_skill")).thenReturn(Optional.empty());

    DataIntegrityValidator.ValidationReport report = validator.validate();
    assertThat(report.isValid()).isFalse();
    assertThat(report.getErrors()).anyMatch(e -> e.contains("橋接目標 DRPG 技能 [non_existent_drpg_skill] 不存在於技能庫"));
  }

  @Test
  @DisplayName("GUARD-01: 模擬開局物資不存在於物品庫，精準報告錯誤")
  void testBrokenStartingSuppliesIntercepted() {
    TemplateReader mockReader = mock(TemplateReader.class);
    com.example.htmlmud.party.PartyService mockPartyService =
        mock(com.example.htmlmud.party.PartyService.class);
    DataIntegrityValidator validator = new DataIntegrityValidator(mockReader);
    validator.setPartyService(mockPartyService);
    validator.setFailFast(false);

    when(mockPartyService.getStartingSupplies()).thenReturn(Map.of("ghost_pill", 5));
    when(mockReader.findItem("ghost_pill")).thenReturn(Optional.empty());

    DataIntegrityValidator.ValidationReport report = validator.validate();
    assertThat(report.isValid()).isFalse();
    assertThat(report.getErrors()).anyMatch(e -> e.contains("開局初始物資 [ghost_pill] 不存在於物品庫"));
  }

  @Test
  @DisplayName("VAL-01: 模擬怪物掉落物斷鏈，在 failFast=true 下精確攔截並拋出 DataValidationException")
  void testMobBrokenLootIntercepted() {
    TemplateReader mockReader = mock(TemplateReader.class);
    DataIntegrityValidator validator = new DataIntegrityValidator(mockReader);
    validator.setFailFast(true);

    MobTemplate brokenMob = MobTemplate.builder()
        .id("test_mob")
        .name("測試妖獸")
        .loot(List.of(new LootEntry("non_existent_item", 0.5, 1, 1)))
        .build();

    when(mockReader.getAllMobs()).thenReturn(Map.of("test_mob", brokenMob));
    when(mockReader.findItem("non_existent_item")).thenReturn(Optional.empty());

    assertThatThrownBy(validator::validate)
        .isInstanceOf(DataValidationException.class)
        .hasMessageContaining("怪物 [test_mob] 掉落物 [non_existent_item] 不存在於物品庫");
  }

  @Test
  @DisplayName("VAL-01: 模擬房間懸空出口與商店幽靈商品，在 failFast=false 下返回完整錯誤報告")
  void testBrokenRoomExitsAndShopGoodsReported() {
    TemplateReader mockReader = mock(TemplateReader.class);
    DataIntegrityValidator validator = new DataIntegrityValidator(mockReader);
    validator.setFailFast(false);

    RoomTemplate brokenRoom = RoomTemplate.builder()
        .id("broken_room")
        .zoneId("zone1")
        .name("斷頭路")
        .exits(Map.of("north", RoomExit.builder().targetRoomId("zone1:nowhere").build()))
        .build();

    ShopTemplate brokenShop = ShopTemplate.builder()
        .id("ghost_shop")
        .name("黑店")
        .roomId("zone1:broken_room")
        .goods(List.of(ShopTemplate.ShopItemTemplate.builder().templateId("ghost_item").build()))
        .build();

    when(mockReader.getAllRooms()).thenReturn(Map.of("broken_room", brokenRoom));
    when(mockReader.findRoom("zone1:nowhere")).thenReturn(Optional.empty());
    when(mockReader.findRoom("nowhere")).thenReturn(Optional.empty());
    when(mockReader.findRoom("zone1:broken_room")).thenReturn(Optional.of(brokenRoom));

    when(mockReader.getAllShops()).thenReturn(Map.of("ghost_shop", brokenShop));
    when(mockReader.findItem("ghost_item")).thenReturn(Optional.empty());

    DataIntegrityValidator.ValidationReport report = validator.validate();
    assertThat(report.isValid()).isFalse();
    assertThat(report.errorCount()).isEqualTo(2);
    assertThat(report.getErrors()).anyMatch(e -> e.contains("指向不存在之房間 [zone1:nowhere]"));
    assertThat(report.getErrors()).anyMatch(e -> e.contains("商品 [ghost_item] 不存在於物品庫"));
  }

  @Test
  @DisplayName("VAL-01: 模擬夥伴初始裝備缺失與陣法非法職業需求，精準捕獲異常")
  void testCompanionAndFormationIntegrity() {
    TemplateReader mockReader = mock(TemplateReader.class);
    DataIntegrityValidator validator = new DataIntegrityValidator(mockReader);
    validator.setFailFast(false);

    CompanionTemplate brokenComp = CompanionTemplate.builder()
        .id("broken_hero")
        .name("斷刀客")
        .classId("UNKNOWN_CLASS")
        .initialEquipment(Map.of(EquipmentSlot.MAIN_HAND, "missing_sword"))
        .build();

    FormationTemplate brokenFormation = FormationTemplate.builder()
        .id("broken_formation")
        .name("殘缺殘陣")
        .requiredClasses(List.of("INVALID_CLASS_REQUIREMENT"))
        .build();

    when(mockReader.getAllCompanions()).thenReturn(Map.of("broken_hero", brokenComp));
    when(mockReader.findItem("missing_sword")).thenReturn(Optional.empty());
    when(mockReader.findClass("UNKNOWN_CLASS")).thenReturn(Optional.empty());

    when(mockReader.getAllFormations()).thenReturn(Map.of("broken_formation", brokenFormation));
    when(mockReader.findClass("INVALID_CLASS_REQUIREMENT")).thenReturn(Optional.empty());

    DataIntegrityValidator.ValidationReport report = validator.validate();
    assertThat(report.isValid()).isFalse();
    assertThat(report.getErrors()).anyMatch(e -> e.contains("夥伴 [broken_hero] 指定之職業/職能 [UNKNOWN_CLASS] 無效"));
    assertThat(report.getErrors()).anyMatch(e -> e.contains("夥伴 [broken_hero] 初始裝備 [MAIN_HAND: missing_sword] 不存在"));
    assertThat(report.getErrors()).anyMatch(e -> e.contains("陣法 [broken_formation] 要求之職業/職能分類 [INVALID_CLASS_REQUIREMENT] 無效"));
  }

  @Test
  @DisplayName("DATA-CORE-01: 模擬地牢 exitRoomId 不存在與首領地塊缺 eventId/怪物不存在，精確捕獲異常")
  void testDungeonExitRoomAndBossIntegrity() {
    TemplateReader mockReader = mock(TemplateReader.class);
    DungeonManager mockDungeonManager = mock(DungeonManager.class);
    when(mockDungeonManager.validateDungeons(any(), any(), any())).thenCallRealMethod();
    DataIntegrityValidator validator = new DataIntegrityValidator(mockReader);
    validator.setDungeonManager(mockDungeonManager);
    validator.setFailFast(false);

    com.example.htmlmud.dungeon.DungeonFloor floor =
        com.example.htmlmud.dungeon.DungeonFloor.builder()
            .id("broken_floor")
            .exitRoomId("zone:missing_exit_room")
            .width(2)
            .height(2)
            .startCoord(new com.example.htmlmud.dungeon.GridCoord(0, 0))
            .tiles(new com.example.htmlmud.dungeon.DungeonTile[][] {
                {
                    com.example.htmlmud.dungeon.DungeonTile.floor("起點"),
                    com.example.htmlmud.dungeon.DungeonTile.builder()
                        .type(com.example.htmlmud.dungeon.DungeonTile.TileType.BOSS)
                        .eventId("missing_boss_mob")
                        .build()
                },
                {
                    com.example.htmlmud.dungeon.DungeonTile.wall(),
                    com.example.htmlmud.dungeon.DungeonTile.wall()
                }
            })
            .build();

    when(mockDungeonManager.getAllFloorIds()).thenReturn(List.of("broken_floor"));
    when(mockDungeonManager.getFloor("broken_floor")).thenReturn(floor);
    when(mockReader.findRoom("zone:missing_exit_room")).thenReturn(Optional.empty());
    when(mockReader.findMob("missing_boss_mob")).thenReturn(Optional.empty());

    DataIntegrityValidator.ValidationReport report = validator.validate();
    assertThat(report.isValid()).isFalse();
    assertThat(report.getErrors()).anyMatch(e -> e.contains("撤退出口房間 [zone:missing_exit_room] 不存在於房間庫"));
    assertThat(report.getErrors()).anyMatch(e -> e.contains("首領地塊 (1, 0) 指定首領怪物 [missing_boss_mob] 不存在於怪物庫"));
  }
}

