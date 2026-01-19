package com.example.htmlmud.service.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.model.PlayerRecord;
import com.example.htmlmud.domain.model.RoomStateRecord;
import com.example.htmlmud.infra.persistence.entity.RoomEntity;
import com.example.htmlmud.infra.persistence.repository.RoomRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomPersistenceService {

  private final RoomRepository roomRepository;

  // 1. 緩衝佇列 (Thread-Safe)
  // LinkedBlockingQueue 是最適合生產者-消費者模式的結構
  private final BlockingQueue<RoomStateRecord> saveQueue = new LinkedBlockingQueue<>();

  // 控制迴圈的旗標
  private volatile boolean running = true;


  /**
   * 【對外 API】非同步存檔 Actor 呼叫這個方法時，幾乎是瞬間完成的。
   */
  public void saveAsync(RoomStateRecord record) {
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
    Thread.ofVirtual().name("db-writer-room").start(this::processQueue);
  }

  /**
   * 3. 消費者迴圈 (批次寫入邏輯)
   */
  private void processQueue() {
    log.info("Write-Behind DB Writer started.");

    // 用來暫存批次資料的 List
    List<RoomStateRecord> batch = new ArrayList<>();

    while (running) {
      try {
        // A. 從佇列取出一筆 (如果空的，這裡會阻塞等待，節省 CPU)
        // 使用 poll 設定超時，這樣我們可以定期檢查 running 狀態或處理剩餘批次
        RoomStateRecord record = saveQueue.poll(1, java.util.concurrent.TimeUnit.SECONDS);

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
  private void flushBatch(List<RoomStateRecord> batch) {
    if (batch.isEmpty())
      return;

    for (RoomStateRecord rec : batch) {
      // 直接用 CharacterRepo 查 (查出來的物件本來就沒有密碼)
      roomRepository.findById(rec.id()).ifPresent(entity -> {

        // entity.setId(rec.id());
        // entity.setDroppedItems(rec.items());
        // entity.setItems(rec.items());

        // 存檔
        roomRepository.save(entity);
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
    List<RoomStateRecord> remaining = new ArrayList<>();
    saveQueue.drainTo(remaining);

    if (!remaining.isEmpty()) {
      log.info("Flushing remaining {} records...", remaining.size());
      flushBatch(remaining);
    }

    log.info("PersistenceService shutdown complete.");
  }

}
