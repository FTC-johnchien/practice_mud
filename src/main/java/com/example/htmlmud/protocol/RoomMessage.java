package com.example.htmlmud.protocol;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.entity.RoomStateRecord;
import com.example.htmlmud.domain.model.enums.Direction;

/**
 * 定義所有發送給 RoomActor 的內部訊息協定 使用 Sealed Interface 限制訊息類型，配合 switch pattern matching
 */
public sealed interface RoomMessage
    permits RoomMessage.Enter, RoomMessage.Leave, RoomMessage.TryPickItem, RoomMessage.Say,
    RoomMessage.Tick, RoomMessage.Broadcast, RoomMessage.BroadcastToOthers, RoomMessage.FindLiving,
    RoomMessage.GetLivings, RoomMessage.RemoveLiving, RoomMessage.GetPlayers, RoomMessage.GetMobs,
    RoomMessage.GetItems, RoomMessage.ToRecord, RoomMessage.RemovePlayer, RoomMessage.RemoveMob,
    RoomMessage.RemoveItem, RoomMessage.DropItem {

  record Tick(long tickCount, long timestamp) implements RoomMessage {
  }

  /**
   * LivingActor進入房間
   *
   * @param Living 實例
   * @param future 用於通知移動完成 (可選)
   */
  record Enter(Living actor, Direction direction, CompletableFuture<Void> future)
      implements RoomMessage {
  }

  /**
   * LivingActor離開房間
   *
   * @param actorId 離開的 Actor ID
   */
  record Leave(Living actor, Direction direction) implements RoomMessage {
  }

  /**
   * 查看房間 (Look 指令)
   *
   * @param playerId 發出指令的玩家 ID (用於過濾"自己看到自己")
   * @param result 用於回傳房間描述字串
   */
  // record Look(String playerId, CompletableFuture<String> result) implements RoomMessage {
  // }

  /**
   * 說話/廣播
   *
   * @param sourcePlayerId 說話者 ID
   * @param content 內容
   */
  record Say(String sourcePlayerId, String content) implements RoomMessage {
  }

  record TryPickItem(String itemId, Player picker, CompletableFuture<GameItem> future)
      implements RoomMessage {
  }

  record Broadcast(String sourceId, String targetId, String message) implements RoomMessage {
  }

  record BroadcastToOthers(String sourceId, String message) implements RoomMessage {
  }

  record FindLiving(String livingId, CompletableFuture<Living> future) implements RoomMessage {
  }

  record GetLivings(CompletableFuture<List<Living>> future) implements RoomMessage {
  }

  record RemoveLiving(String livingId) implements RoomMessage {
  }

  record GetPlayers(CompletableFuture<List<Player>> future) implements RoomMessage {
  }

  record RemovePlayer(String playerId) implements RoomMessage {
  }

  record GetMobs(CompletableFuture<List<Mob>> future) implements RoomMessage {
  }

  record RemoveMob(String mobId) implements RoomMessage {
  }

  record GetItems(CompletableFuture<List<GameItem>> future) implements RoomMessage {
  }

  record RemoveItem(String itemId) implements RoomMessage {
  }

  record DropItem(GameItem item) implements RoomMessage {
  }

  record ToRecord(CompletableFuture<RoomStateRecord> future) implements RoomMessage {
  }

}
