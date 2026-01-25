package com.example.htmlmud.protocol;

import java.util.concurrent.CompletableFuture;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.actor.impl.LivingActor;
import com.example.htmlmud.domain.actor.impl.MobActor;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.domain.model.EquipmentSlot;
import com.example.htmlmud.domain.model.GameItem;


public sealed interface ActorMessage
    permits ActorMessage.LivingMessage, ActorMessage.PlayerMessage, ActorMessage.MobMessage {



  sealed interface LivingMessage extends ActorMessage
      permits Tick, OnAttacked, OnDamage, Die, Heal, Say, BuffEffect, Equip, Unequip, onMessage {
  }
  /**
   * 心跳訊息
   *
   * @param tickCount 全域累計的 Tick 次數 (用來取餘數判斷頻率)
   * @param timestamp 當前時間戳
   */
  record Tick(long tickCount, long timestamp) implements LivingMessage {
  }
  record OnAttacked(LivingActor actor) implements LivingMessage {
  }
  record OnDamage(int amount, String attackerId) implements LivingMessage {
  }
  record Die(String killerId) implements LivingMessage {
  }
  record Heal(int amount) implements LivingMessage {
  }
  record Say(String content) implements LivingMessage {
  }
  record BuffEffect(String effectId) implements LivingMessage {
  }
  record Equip(GameItem item, CompletableFuture<String> future) implements LivingMessage {
  }
  record Unequip(EquipmentSlot slot, CompletableFuture<String> future) implements LivingMessage {
  }
  record onMessage(LivingActor self, ActorMessage msg) implements LivingMessage {
  }



  sealed interface PlayerMessage extends ActorMessage
      permits Command, SendText, GainExp, SaveData, QuestUpdate {
  }
  record Command(String traceId, GameCommand command) implements PlayerMessage {
  }
  record SendText(WebSocketSession session, String content) implements PlayerMessage {
  }
  record GainExp(int amount) implements PlayerMessage {
  }
  record SaveData() implements PlayerMessage {
  }
  record QuestUpdate(String questId, String status) implements PlayerMessage {
  }



  sealed interface MobMessage extends ActorMessage
      permits OnPlayerEnter, onInteract, AgroScan, RandomMove, Respawn {
  }
  record OnPlayerEnter(String actorId) implements MobMessage {
  }
  record onInteract(MobActor self, PlayerActor player, String command) implements MobMessage {
  }
  record AgroScan() implements MobMessage {
  }
  record RandomMove() implements MobMessage {
  }
  record Respawn() implements MobMessage {
  }

}
