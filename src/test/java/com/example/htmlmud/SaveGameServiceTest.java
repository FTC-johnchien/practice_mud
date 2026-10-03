package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.save.dto.SaveSlotDto;
import com.example.htmlmud.domain.save.model.SaveData;
import com.example.htmlmud.domain.save.service.SaveGameService;

@SpringBootTest
class SaveGameServiceTest {

  @Autowired
  private SaveGameService saveGameService;

  @Autowired
  private PartyService partyService;

  @Autowired
  private DungeonManager dungeonManager;

  private final String testPlayerId = "test_player";

  @BeforeEach
  void setUp() {
    saveGameService.init();
    // 清除測試槽位 1
    saveGameService.deleteSave(1);
    saveGameService.deleteSave(0);
  }

  @AfterEach
  void tearDown() {
    saveGameService.deleteSave(1);
    saveGameService.deleteSave(0);
  }

  @Test
  @DisplayName("驗證單機存檔、槽位列表查詢、讀檔還原與刪除完整流程")
  void testSaveAndLoadCycle() {
    // 1. 初始化小隊與地牢坐標
    Party party = partyService.resetParty(testPlayerId, "玄靈真人");
    PartyMember leader = party.getMembers().get(0);
    leader.takeDamage(25);
    leader.consumeSan(10);

    // 往背包加入一件法寶
    party.getInventory().addItem("taiyin_tomb:yin_robe", 1);

    // 設定特定坐標與開啟寶箱
    DungeonPosition pos = dungeonManager.getOrCreatePosition(testPlayerId, "taiyin_tomb_b1f");
    pos.setCoord(4, 4);
    pos.markChestOpened(3, 5);

    // 2. 儲存至槽位 1
    SaveData saved = saveGameService.saveGame(testPlayerId, 1, "玄靈真人太陰古塚突破");
    assertNotNull(saved);
    assertThat(saved.getSlotId()).isEqualTo(1);
    assertThat(saved.getFloorX()).isEqualTo(4);
    assertThat(saved.getFloorY()).isEqualTo(4);
    assertThat(saved.getOpenedChests()).contains("3,5");

    // 3. 檢查槽位列表
    List<SaveSlotDto> slots = saveGameService.listSaveSlots();
    assertThat(slots).hasSize(6); // 0 (autosave) + 1..5 (manual)

    SaveSlotDto slot1 = slots.stream().filter(s -> s.getSlotId() == 1).findFirst().orElseThrow();
    assertFalse(slot1.isEmpty());
    assertThat(slot1.getTitle()).isEqualTo("玄靈真人太陰古塚突破");
    assertThat(slot1.getProtagonistName()).isEqualTo("玄靈真人");

    SaveSlotDto slot2 = slots.stream().filter(s -> s.getSlotId() == 2).findFirst().orElseThrow();
    assertTrue(slot2.isEmpty(), "槽位 2 應為空");

    // 4. 重置記憶體狀態，模擬重啟遊戲後讀檔
    partyService.resetParty(testPlayerId, "他人");
    pos.setCoord(1, 8);

    // 5. 讀取槽位 1
    SaveData loaded = saveGameService.loadGame(testPlayerId, 1);
    assertNotNull(loaded);
    assertThat(loaded.getTitle()).isEqualTo("玄靈真人太陰古塚突破");

    // 驗證小隊與隊員狀態完美還原
    Party restoredParty = partyService.getOrCreateParty(testPlayerId);
    PartyMember restoredLeader = restoredParty.getMembers().get(0);
    assertThat(restoredLeader.getName()).isEqualTo("玄靈真人");
    assertThat(restoredLeader.getStats().getHp()).isEqualTo(leader.getStats().getHp());
    assertThat(restoredLeader.getCurrentSan()).isEqualTo(leader.getCurrentSan());

    // 驗證背包法寶還原
    assertThat(restoredParty.getInventory().getSlots()).anyMatch(i -> i.getName().contains("陰煞"));

    // 驗證地牢坐標與開箱狀態還原
    DungeonPosition restoredPos = dungeonManager.getOrCreatePosition(testPlayerId, "taiyin_tomb_b1f");
    assertThat(restoredPos.getX()).isEqualTo(4);
    assertThat(restoredPos.getY()).isEqualTo(4);
    assertTrue(restoredPos.isChestOpened(3, 5));

    // 6. 測試開闢新道途 createNewGame
    Party freshParty = saveGameService.createNewGame(testPlayerId, "李逍遙", "formation_four_symbols");
    assertThat(freshParty.getMembers().get(0).getName()).isEqualTo("李逍遙");
    DungeonPosition freshPos = dungeonManager.getOrCreatePosition(testPlayerId, "taiyin_tomb_b1f");
    assertThat(freshPos.getX()).isEqualTo(1);
    assertThat(freshPos.getY()).isEqualTo(8);

    // 7. 測試刪除槽位 1
    boolean deleted = saveGameService.deleteSave(1);
    assertTrue(deleted);
    SaveSlotDto slot1After = saveGameService.readSlotSummary(1, "存檔槽位 1");
    assertTrue(slot1After.isEmpty());
  }

  @Test
  @DisplayName("驗證存檔損壞時防禦機制：卡片標記 corrupted 且拒絕直接加載")
  void testCorruptedSaveDetectionAndDefense() throws Exception {
    int slotId = 2;
    java.io.File saveFile = new java.io.File("saves", "slot_" + slotId + ".json");
    if (!saveFile.getParentFile().exists()) {
      saveFile.getParentFile().mkdirs();
    }
    java.nio.file.Files.writeString(saveFile.toPath(), "{ invalid json content: corrupt! @@@");

    try {
      // 1. 槽位清單與摘要檢查：應標記 empty = false, corrupted = true
      SaveSlotDto summary = saveGameService.readSlotSummary(slotId, "存檔槽位 " + slotId);
      assertFalse(summary.isEmpty(), "損壞存檔不可誤判為空");
      assertTrue(summary.isCorrupted(), "損壞存檔應明確標記 corrupted=true");
      assertThat(summary.getTitle()).contains("存檔損壞");

      // 2. 嘗試 loadGame 應嚴格拋出 SaveCorruptedException
      org.junit.jupiter.api.Assertions.assertThrows(
          com.example.htmlmud.domain.save.exception.SaveCorruptedException.class,
          () -> saveGameService.loadGame(testPlayerId, slotId),
          "加載壞檔應拋出 SaveCorruptedException"
      );
    } finally {
      saveGameService.deleteSave(slotId);
    }
  }

  @Test
  @DisplayName("驗證舊版本存檔 (v0 無 schemaVersion) 能平滑遷移至 v1 最新版本")
  void testSaveMigrationFromLegacyV0ToV1() throws Exception {
    int slotId = 3;
    java.io.File saveFile = new java.io.File("saves", "slot_" + slotId + ".json");
    if (!saveFile.getParentFile().exists()) {
      saveFile.getParentFile().mkdirs();
    }

    String legacyJson = """
        {
          "slotId": 3,
          "title": "太陰舊版存檔",
          "playerId": "test_player",
          "protagonistName": "古修殘魂",
          "floorId": "mozhu_mines_b1f",
          "floorX": 2,
          "floorY": 2
        }
        """;
    java.nio.file.Files.writeString(saveFile.toPath(), legacyJson);

    try {
      SaveSlotDto summary = saveGameService.readSlotSummary(slotId, "存檔槽位 " + slotId);
      assertFalse(summary.isEmpty());
      assertFalse(summary.isCorrupted());
      assertThat(summary.getTitle()).isEqualTo("太陰舊版存檔");
      assertThat(summary.getProtagonistName()).isEqualTo("古修殘魂");

      SaveData loaded = saveGameService.loadGame(testPlayerId, slotId);
      assertNotNull(loaded);
      assertThat(loaded.getSchemaVersion()).isEqualTo(SaveData.CURRENT_SCHEMA_VERSION);
      assertThat(loaded.getProtagonistName()).isEqualTo("古修殘魂");
    } finally {
      saveGameService.deleteSave(slotId);
    }
  }

  @Test
  @DisplayName("驗證存檔自動 .bak 備份機制與還原功能")
  void testBackupCreationAndRestore() {
    int slotId = 4;
    try {
      // 首次存檔：尚無前代檔案，不應有備份
      SaveData firstSave = saveGameService.saveGame(testPlayerId, slotId, "第一代進度");
      assertNotNull(firstSave);
      assertFalse(saveGameService.hasBackup(slotId));

      // 第二次覆寫存檔：應自動將「第一代進度」存入 .bak
      SaveData secondSave = saveGameService.saveGame(testPlayerId, slotId, "第二代進度");
      assertNotNull(secondSave);
      assertTrue(saveGameService.hasBackup(slotId));

      // 模擬人為破壞主存檔
      java.io.File mainFile = new java.io.File("saves", "slot_" + slotId + ".json");
      mainFile.delete();

      // 執行備份還原
      boolean restored = saveGameService.restoreBackup(slotId);
      assertTrue(restored, "備份還原應成功");

      // 驗證讀檔成功且內容為第一代進度
      SaveData loaded = saveGameService.loadGame(testPlayerId, slotId);
      assertNotNull(loaded);
      assertThat(loaded.getTitle()).isEqualTo("第一代進度");
    } finally {
      saveGameService.deleteSave(slotId);
    }
  }
}
