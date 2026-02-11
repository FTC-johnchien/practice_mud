package com.example.htmlmud.application.command;

public interface PlayerCommand {

  // 該指令的觸發關鍵字，例如 "look", "l"
  String getKey();

  // 執行邏輯
  // actor: 誰發出的指令 (操作者 Context)
  // args: 指令參數 (例如 'look north' 的 'north')
  void execute(String args);

  // 描述 (給 help 指令用)
  default String getDescription() {
    return "無描述";
  }
}
