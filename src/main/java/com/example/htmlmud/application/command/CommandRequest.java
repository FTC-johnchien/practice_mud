package com.example.htmlmud.application.command;

import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.protocol.GameCommand;
import lombok.Getter;

@Getter
public class CommandRequest {
  private final Player player; // 誰發出的
  private final GameCommand command; // 解析後的指令物件
  private final long timestamp; // 收到時間 (用於防止惡意洗頻或計算延遲)

  public CommandRequest(Player player, GameCommand command) {
    this.player = player;
    this.command = command;
    this.timestamp = System.currentTimeMillis();
  }

  // Getters...
}
