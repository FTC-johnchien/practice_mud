package com.example.htmlmud.domain.port;

/**
 * 領域層指標度量埠 (Driven/Output Port)
 * 遵循整潔架構 (Clean Architecture)，戰鬥與系統運算透過此介面記錄計數，不直接耦合監控實作。
 */
public interface DomainMetricsPort {

  /**
   * 增加玩家指令計數
   */
  void incrementPlayerCommand();

  /**
   * 增加系統任務計數 (如戰鬥動作、AI 運算)
   */
  void incrementSystemTask();

  /**
   * 累加心跳耗時 (奈秒)
   */
  void addPulseDurationNanos(long nanos);
}
