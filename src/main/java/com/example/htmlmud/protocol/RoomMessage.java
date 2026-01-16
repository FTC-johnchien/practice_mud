package com.example.htmlmud.protocol;

// 修正引用路徑：指向 domain.actor
import com.example.htmlmud.domain.actor.PlayerActor;
import java.util.concurrent.CompletableFuture;

/**
 * 定義所有發送給 RoomActor 的內部訊息協定 使用 Sealed Interface 限制訊息類型，配合 switch pattern matching
 */
public sealed interface RoomMessage
    permits RoomMessage.PlayerEnter, RoomMessage.PlayerLeave, RoomMessage.Look, RoomMessage.Say {

  /**
   * 玩家進入房間
   * 
   * @param player 玩家 Actor 實例
   * @param future 用於通知移動完成 (可選)
   */
  record PlayerEnter(PlayerActor player, CompletableFuture<Void> future) implements RoomMessage {
  }

  /**
   * 玩家離開房間
   * 
   * @param playerId 離開的玩家 ID
   */
  record PlayerLeave(String playerId) implements RoomMessage {
  }

  /**
   * 查看房間 (Look 指令)
   * 
   * @param playerId 發出指令的玩家 ID (用於過濾"自己看到自己")
   * @param result 用於回傳房間描述字串
   */
  record Look(String playerId, CompletableFuture<String> result) implements RoomMessage {
  }

  /**
   * 說話/廣播
   * 
   * @param sourcePlayerId 說話者 ID
   * @param content 內容
   */
  record Say(String sourcePlayerId, String content) implements RoomMessage {
  }
}
