package com.example.htmlmud.infra.persistence.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.model.entity.PlayerRecord;
import com.example.htmlmud.infra.mapper.PlayerMapper;
import com.example.htmlmud.infra.persistence.repository.CharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerPersistenceService extends AbstractAsyncBatchPersistenceService<PlayerRecord> {

  private final PlayerMapper mapper;
  private final CharacterRepository playerRepository;

  @Override
  protected String getWorkerThreadName() {
    return "db-writer-player";
  }

  @Override
  protected void flushBatch(List<PlayerRecord> batch) {
    if (batch.isEmpty()) {
      return;
    }

    // 使用 Map 去重，只保留每個 Player 的最新狀態
    Map<String, PlayerRecord> latestRecords = new HashMap<>();
    for (PlayerRecord rec : batch) {
      latestRecords.put(rec.id(), rec);
    }

    // 只寫入去重後的資料
    for (PlayerRecord rec : latestRecords.values()) {
      playerRepository.findById(rec.id()).ifPresent(entity -> {
        mapper.updateEntityFromRecord(rec, entity);
        playerRepository.save(entity);
      });
    }
  }
}
