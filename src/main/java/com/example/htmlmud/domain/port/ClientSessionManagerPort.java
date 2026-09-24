package com.example.htmlmud.domain.port;

import com.example.htmlmud.domain.actor.impl.Player;

/**
 * 客戶端網路連線交接管理輸出埠口 (Output Port)
 * 遵循依賴反轉原則，由網路層 MudWebSocketHandler 實作。
 */
public interface ClientSessionManagerPort {

  /**
   * 將連線移交給登入/重連後的正式玩家 Actor
   */
  void promoteToPlayer(Object rawSession, Player player);
}
