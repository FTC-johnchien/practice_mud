package com.example.htmlmud.domain.port;

import com.example.htmlmud.domain.model.entity.PlayerRecord;

/**
 * 玩家資料持久化埠 (Driven/Output Port)
 * 遵循整潔架構 (Clean Architecture)，由領域層定義介面，基礎設施層負責具體實作。
 */
public interface PlayerPersistencePort {

  /**
   * 異步提交玩家存檔資料至寫入佇列 (Write-Behind)
   *
   * @param record 玩家核心持久化資料快照
   */
  void saveAsync(PlayerRecord record);
}
