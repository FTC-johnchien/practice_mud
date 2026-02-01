package com.example.htmlmud.application.service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.command.CommandRequest;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.protocol.GameCommand;

@Service
public class CommandQueueService {

  // 執行緒安全的佇列
  private final Queue<CommandRequest> queue = new ConcurrentLinkedQueue<>();

  /**
   * [生產者端] 由 WebSocketHandler 呼叫 這裡動作極快，幾乎不消耗時間，不會阻塞網路執行緒
   */
  public void push(Player player, GameCommand cmd) {
    queue.offer(new CommandRequest(player, cmd));
  }

  /**
   * [消費者端] 由 GameLoop 呼叫 一次取出所有累積的指令
   */
  public CommandRequest poll() {
    return queue.poll(); // 如果佇列是空的，回傳 null
  }

  public boolean isEmpty() {
    return queue.isEmpty();
  }
}
