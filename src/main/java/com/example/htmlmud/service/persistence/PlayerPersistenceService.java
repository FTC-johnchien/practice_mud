package com.example.htmlmud.service.persistence;

import com.example.htmlmud.domain.model.PlayerRecord;
import com.example.htmlmud.infra.mapper.PlayerMapper;
import com.example.htmlmud.infra.persistence.entity.PlayerEntity;
import com.example.htmlmud.infra.persistence.repository.PlayerRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerPersistenceService {

  private final PlayerMapper mapper; // 注入 MapStruct
  private final PlayerRepository playerRepository;

  // 1. 緩衝佇列 (Thread-Safe)
  // LinkedBlockingQueue 是最適合生產者-消費者模式的結構
  private final BlockingQueue<PlayerRecord> saveQueue = new LinkedBlockingQueue<>();

  // 控制迴圈的旗標
  private volatile boolean running = true;

  /**
   * 【對外 API】非同步存檔 Actor 呼叫這個方法時，幾乎是瞬間完成的。
   */
  public void saveAsync(PlayerRecord record) {
    if (record == null)
      return;

    // 丟入佇列，如果不滿就立刻返回，不會阻塞
    if (!saveQueue.offer(record)) {
      log.error("存檔佇列已滿！可能資料庫寫入過慢，資料遺失風險: {}", record.id());
    }
  }

  /**
   * 2. 啟動背景消費者執行緒 使用 @PostConstruct 在 Bean 建立後自動執行
   */
  @PostConstruct
  public void init() {
    // 啟動一個虛擬執行緒來專門處理存檔
    Thread.ofVirtual().name("db-writer").start(this::processQueue);
  }

  /**
   * 3. 消費者迴圈 (批次寫入邏輯)
   */
  private void processQueue() {
    log.info("Write-Behind DB Writer started.");

    // 用來暫存批次資料的 List
    List<PlayerRecord> batch = new ArrayList<>();

    while (running) {
      try {
        // A. 從佇列取出一筆 (如果空的，這裡會阻塞等待，節省 CPU)
        // 使用 poll 設定超時，這樣我們可以定期檢查 running 狀態或處理剩餘批次
        PlayerRecord record = saveQueue.poll(1, java.util.concurrent.TimeUnit.SECONDS);

        if (record != null) {
          batch.add(record);
        }

        // B. 檢查是否需要寫入 DB (滿足數量 或 佇列沒東西了但還有殘存資料)
        // 條件：累積滿 50 筆 OR (佇列空了 且 手上還有資料)
        if (batch.size() >= 50 || (record == null && !batch.isEmpty())) {
          flushBatch(batch);
        }

        // C. 額外優化：如果佇列裡還有很多，一口氣全部撈出來 (Drain)
        // 這能大幅提升高負載時的吞吐量
        if (!saveQueue.isEmpty() && batch.size() < 100) {
          saveQueue.drainTo(batch, 100 - batch.size());
          flushBatch(batch); // 再寫一次
        }

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("DB Writer thread interrupted.");
        break;
      } catch (Exception e) {
        log.error("DB Writer loop error", e);
      }
    }
  }

  /**
   * 4. 實際寫入資料庫
   */
  private void flushBatch(List<PlayerRecord> batch) {
    if (batch.isEmpty())
      return;

    for (PlayerRecord rec : batch) {
      // 直接用 CharacterRepo 查 (查出來的物件本來就沒有密碼)
      playerRepository.findById(rec.id()).ifPresent(entity -> {

        // Record -> Entity (MapStruct 自動更新)
        // 這行程式碼取代了原本手寫的 entity.setNickname(), entity.setState()...
        mapper.updateEntityFromRecord(rec, entity);

        // 存檔
        playerRepository.save(entity);
      });
    }
  }

  /**
   * 5. 優雅關機 (Graceful Shutdown) 當 Spring Boot 關閉時，確保佇列裡的資料都寫完
   */
  @PreDestroy
  public void shutdown() {
    log.info("Shutting down PersistenceService...");
    running = false; // 停止迴圈讀取

    // 把佇列中剩下的全部寫完
    List<PlayerRecord> remaining = new ArrayList<>();
    saveQueue.drainTo(remaining);

    if (!remaining.isEmpty()) {
      log.info("Flushing remaining {} records...", remaining.size());
      flushBatch(remaining);
    }

    log.info("PersistenceService shutdown complete.");
  }

  // --- 輔助方法：DTO 轉換 ---
  // 將 Record 轉回 Entity 準備存檔
  private PlayerEntity toEntity(PlayerRecord r) {
    // 注意：這裡我們建立一個新的 Entity 物件，只填入 ID 和要更新的欄位
    // Hibernate save() 檢查 ID 存在會執行 merge/update
    return PlayerEntity.builder().id(r.id()).name(r.name()) // 雖然通常不改 username，但 JPA 需要
        .displayName(r.displayName()).currentRoomId(r.currentRoomId()).state(r.state()) // 更新 JSON
                                                                                        // 狀態
        // .inventory(r.inventory()) // 未來加入
        // .lastLoginAt(LocalDateTime.now()) // 順便更新最後活動時間
        // 密碼等敏感欄位如果是 null，要小心不要覆蓋掉 DB 裡的舊資料
        // 實務上建議：如果是 null，先查 DB 再填，或者使用 @DynamicUpdate
        // 但簡單起見，這裡假設 Record 包含了所有資料
        .build();
  }
}
