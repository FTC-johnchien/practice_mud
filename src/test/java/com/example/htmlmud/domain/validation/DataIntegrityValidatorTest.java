package com.example.htmlmud.domain.validation;

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
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.template.CompanionTemplate;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.model.template.RoomExit;
import com.example.htmlmud.domain.model.template.RoomTemplate;
import com.example.htmlmud.domain.model.template.ShopTemplate;
import com.example.htmlmud.domain.party.model.FormationTemplate;
import com.example.htmlmud.domain.repository.TemplateReader;

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
}
