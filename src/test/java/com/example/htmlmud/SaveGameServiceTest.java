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
}
