package com.example.htmlmud.infra.persistence.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.model.entity.RoomStateRecord;
import com.example.htmlmud.infra.persistence.entity.RoomStateEntity;
import com.example.htmlmud.infra.persistence.repository.RoomStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomPersistenceService extends AbstractAsyncBatchPersistenceService<RoomStateRecord> {

  private final RoomStateRepository roomRepository;

  @Override
  protected String getWorkerThreadName() {
    return "db-writer-room";
  }

  @Override
  protected void flushBatch(List<RoomStateRecord> batch) {
    if (batch.isEmpty()) {
      return;
    }

    // 使用 Map 去重，只保留每個 Room 的最新狀態
    Map<String, RoomStateRecord> latestRecords = new HashMap<>();
    for (RoomStateRecord rec : batch) {
      String key = rec.zoneId() + ":" + rec.roomId();
      latestRecords.put(key, rec);
    }

    // 寫入去重後的資料 (存在則更新，不存在則新增)
    for (RoomStateRecord rec : latestRecords.values()) {
      try {
        RoomStateEntity entity = roomRepository.findByRoomIdAndZoneId(rec.roomId(), rec.zoneId())
            .orElseGet(() -> {
              RoomStateEntity newEntity = new RoomStateEntity();
              newEntity.setRoomId(rec.roomId());
              newEntity.setZoneId(rec.zoneId());
              return newEntity;
            });

        entity.setDroppedItems(rec.droppedItems() != null ? new java.util.ArrayList<>(rec.droppedItems()) : new java.util.ArrayList<>());
        roomRepository.save(entity);
      } catch (Exception e) {
        log.error("Failed to persist room state for {}:{}", rec.zoneId(), rec.roomId(), e);
      }
    }
  }

  /**
   * 讀取特定房間歷史儲存的地面掉落物清單
   */
  public java.util.Optional<List<com.example.htmlmud.domain.model.entity.GameItem>> loadDroppedItems(String zoneId, String roomId) {
    return roomRepository.findByRoomIdAndZoneId(roomId, zoneId)
        .map(entity -> entity.getDroppedItems() != null ? entity.getDroppedItems() : java.util.Collections.emptyList());
  }
}
