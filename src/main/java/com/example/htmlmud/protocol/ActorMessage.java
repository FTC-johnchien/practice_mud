package com.example.htmlmud.protocol;

import java.util.concurrent.CompletableFuture;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.actor.LivingActor;
import com.example.htmlmud.domain.model.EquipmentSlot;
import com.example.htmlmud.domain.model.GameItem;

public sealed interface ActorMessage
    permits ActorMessage.Tick, ActorMessage.Command, ActorMessage.Die, ActorMessage.SendText,
    ActorMessage.Attacked, ActorMessage.Equip, ActorMessage.Unequip {

  /**
   * 心跳訊息
   *
   * @param tickCount 全域累計的 Tick 次數 (用來取餘數判斷頻率)
   * @param timestamp 當前時間戳
   */
  record Tick(long tickCount, long timestamp) implements ActorMessage {
  }

  record Command(String traceId, GameCommand command) implements ActorMessage {
  }

  record Die(String killerId) implements ActorMessage {
  }

  record SendText(WebSocketSession session, String content) implements ActorMessage {
  }

  record Attacked(LivingActor actor) implements ActorMessage {
  }

  record Equip(GameItem item, CompletableFuture<String> future) implements ActorMessage {
  }

  record Unequip(EquipmentSlot slot, CompletableFuture<String> future) implements ActorMessage {
  }

}
