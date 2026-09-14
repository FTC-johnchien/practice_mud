package com.example.htmlmud.infra.persistence.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.model.entity.RoomStateRecord;
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

    // 只寫入去重後的資料
    for (RoomStateRecord rec : latestRecords.values()) {
      roomRepository.findByRoomIdAndZoneId(rec.roomId(), rec.zoneId()).ifPresent(entity -> {
        roomRepository.save(entity);
      });
    }
  }
}
