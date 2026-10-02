package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.entity.RoomStateRecord;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.service.RoomService;
import com.example.htmlmud.infra.persistence.entity.RoomStateEntity;
import com.example.htmlmud.infra.persistence.repository.RoomStateRepository;
import com.example.htmlmud.infra.persistence.service.RoomPersistenceService;

/**
 * 房間狀態與地面掉落物持久化閉環 (ROOM-01) 專屬驗收測試
 */
@SpringBootTest
public class RoomDroppedItemPersistenceTest {

  @Autowired
  private RoomPersistenceService roomPersistenceService;

  @Autowired
  private RoomService roomService;

  @Autowired
  private RoomStateRepository roomStateRepository;

  @Test
  @DisplayName("ROOM-01: 掉落物快照寫入、批次持久化與跨重載讀回閉環驗證")
  void testRoomDroppedItemsPersistenceAndLoad() {
    String zoneId = "test_zone";
    String roomId = "test_chamber";
    String roomFullId = zoneId + ":" + roomId;

    GameItem sword = new GameItem();
    sword.setId("i-sword-999");
    sword.setName("玄鐵重劍");
    sword.setType(ItemType.WEAPON);
    sword.setAliases(List.of("sword", "重劍"));

    GameItem potion = new GameItem();
    potion.setId("i-pot-888");
    potion.setName("大還丹");
    potion.setType(ItemType.CONSUMABLE);
    potion.setAliases(List.of("potion", "丹藥"));

    List<GameItem> droppedItems = new ArrayList<>(List.of(sword, potion));

    // 1. 透過 RoomService.record 觸發快照
    roomService.record(roomFullId, droppedItems);

    // 2. 觸發立即排空持久化
    roomPersistenceService.flushImmediately();

    // 3. 驗證資料庫實體已正確寫入
    var optEntity = roomStateRepository.findByRoomIdAndZoneId(roomId, zoneId);
    assertThat(optEntity).isPresent();
    RoomStateEntity entity = optEntity.get();
    assertThat(entity.getDroppedItems()).isNotNull();
    assertThat(entity.getDroppedItems()).hasSize(2);
    assertThat(entity.getDroppedItems().get(0).getName()).isEqualTo("玄鐵重劍");
    assertThat(entity.getDroppedItems().get(1).getName()).isEqualTo("大還丹");

    // 4. 驗證透過 RoomPersistenceService.loadDroppedItems 成功讀回
    var loaded = roomPersistenceService.loadDroppedItems(zoneId, roomId);
    assertThat(loaded).isPresent();
    assertThat(loaded.get()).hasSize(2);
    assertThat(loaded.get().stream().map(GameItem::getName)).containsExactly("玄鐵重劍", "大還丹");

    // 5. 拾取所有物品後記錄空清單，驗證能正確覆蓋舊有資料 (清除掉落物)
    roomService.record(roomFullId, Collections.emptyList());
    roomPersistenceService.flushImmediately();

    var emptyLoaded = roomPersistenceService.loadDroppedItems(zoneId, roomId);
    assertThat(emptyLoaded).isPresent();
    assertThat(emptyLoaded.get()).isEmpty();
  }
}
