package com.example.htmlmud.domain.actor;

import com.example.htmlmud.domain.actor.core.VirtualActor; // 引用您的基礎類別
import com.example.htmlmud.domain.model.map.Room;
import com.example.htmlmud.protocol.RoomMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
// 1. 繼承 VirtualActor，並指定泛型為 RoomMessage
public class RoomActor extends VirtualActor<RoomMessage> {

  @Getter
  private final Room template;

  // 房間內的玩家 (Runtime State)
  private final Map<String, PlayerActor> players = new ConcurrentHashMap<>();

  public RoomActor(Room template) {
    // 2. 傳入 Actor 名稱給父類別 (方便 Log 排查)
    super("room-" + template.id());
    this.template = template;

    // 3. 啟動 Actor (這會呼叫父類別的 start -> runLoop)
    this.start();
  }

  // --- 實作父類別的抽象方法 ---

  @Override
  protected void handleMessage(RoomMessage msg) {
    // 這裡的邏輯跟之前一模一樣，但不需要自己寫 loop 和 try-catch 了
    switch (msg) {
      case RoomMessage.PlayerEnter(var player, var future) -> {
        log.info("Player {} entered room", player.getId());
        players.put(player.getId(), player);
        broadcastToOthers(player.getId(), "看到 " + player.getDisplayName() + " 走了進來。");
        log.debug("Player {} entered room {}", player.getDisplayName(), template.id());
        if (future != null)
          future.complete(null);
      }

      case RoomMessage.PlayerLeave(var playerId) -> {
        PlayerActor p = players.remove(playerId);
        if (p != null) {
          broadcastToOthers(playerId, p.getDisplayName() + " 離開了。");
        }
      }

      case RoomMessage.Look(var playerId, var future) -> {
        log.info("Player {} Look room", playerId);
        StringBuilder sb = new StringBuilder();
        sb.append("\u001B[1;36m").append(template.title()).append("\u001B[0m\r\n");
        sb.append(template.description()).append("\r\n");

        if (template.exits() != null && !template.exits().isEmpty()) {
          sb.append("\u001B[33m[出口]: ").append(String.join(", ", template.exits().keySet()))
              .append("\u001B[0m\r\n");
        }

        StringBuilder others = new StringBuilder();
        players.values().stream().filter(p -> !p.getId().equals(playerId))
            .forEach(p -> others.append(p.getDisplayName()).append(" "));

        if (!others.isEmpty()) {
          sb.append("\u001B[35m[這裡有]: \u001B[0m").append(others).append("\r\n");
        }
        future.complete(sb.toString());
      }

      case RoomMessage.Say(var sourceId, var content) -> {
        PlayerActor speaker = players.get(sourceId);
        String name = (speaker != null) ? speaker.getDisplayName() : "有人";
        broadcast(name + ": " + content);
      }
    }
  }

  // --- 輔助方法 (保持不變) ---

  private void broadcast(String message) {
    players.values().forEach(p -> p.sendText(message));
  }

  private void broadcastToOthers(String sourceId, String message) {
    players.values().stream().filter(p -> !p.getId().equals(sourceId))
        .forEach(p -> p.sendText(message));
  }
}
