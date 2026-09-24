package com.example.htmlmud.domain.actor.core;

/**
 * 領域實體訊息輸出抽象埠口 (Message Output Port)
 * 徹底與具體傳輸技術 (WebSocket / TCP / CLI) 解耦。
 */
public interface MessageOutput {

  /**
   * 發送結構化資料 (如 STAT_UPDATE, TEXT, BATTLE_STATE 等)
   */
  void sendJson(Object payload);

  /**
   * 關閉連線輸出端
   */
  void close();

  /**
   * 取得底層傳輸連線實例 (以 Object 抽象泛型返回，避免污染領域層)
   */
  Object getSession();
}
