package com.example.htmlmud.domain.port;

/**
 * 領域層指令轉發輸出埠口 (Output Port)
 * 允許領域主體在需要時 (如復活後的 look) 觸發命令執行，由 Application 層的 CommandDispatcher 實作。
 */
public interface CommandDispatcherPort {

  /**
   * 轉發並執行 MUD / DRPG 指令行
   */
  void dispatch(String commandLine);
}
