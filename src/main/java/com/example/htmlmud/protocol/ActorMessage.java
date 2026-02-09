package com.example.htmlmud.protocol;

import java.util.concurrent.CompletableFuture;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;


public sealed interface ActorMessage
    permits ActorMessage.LivingMessage, ActorMessage.PlayerMessage, ActorMessage.MobMessage {



  sealed interface LivingMessage extends ActorMessage permits Tick, OnAttacked, OnDamage, onDeath,
      onHeal, Say, BuffEffect, Equip, Unequip, OnMessage {
  }
  /**
   * 心跳訊息
   *
   * @param tickCount 全域累計的 Tick 次數 (用來取餘數判斷頻率)
   * @param timestamp 當前時間戳
   */
  record Tick(long tickCount, long timestamp) implements LivingMessage {
  }
  record OnAttacked(String attackerId) implements LivingMessage {
  }
  record OnDamage(int amount, String attackerId) implements LivingMessage {
  }
  record onDeath(String killerId) implements LivingMessage {
  }
  record onHeal(int amount) implements LivingMessage {
  }
  record Say(String content) implements LivingMessage {
  }
  record BuffEffect(String effectId) implements LivingMessage {
  }
  record Equip(GameItem item, CompletableFuture<Boolean> future) implements LivingMessage {
  }
  record Unequip(EquipmentSlot slot, CompletableFuture<Boolean> future) implements LivingMessage {
  }
  record OnMessage(Living self, ActorMessage msg) implements LivingMessage {
  }



  sealed interface PlayerMessage extends ActorMessage
      permits Command, SendText, GainExp, SaveData, QuestUpdate, Reconnect, Disconnect {
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
  record Reconnect(WebSocketSession session) implements PlayerMessage {
  }
  record Disconnect() implements PlayerMessage {
  }



  sealed interface MobMessage extends ActorMessage
      permits OnPlayerEnter, OnPlayerFlee, OnInteract, AgroScan, RandomMove, Respawn {
  }
  record OnPlayerEnter(String playerId) implements MobMessage {
  }
  record OnPlayerFlee(String playerId, String direction) implements MobMessage {
  }
  record OnInteract(String playerId, String command) implements MobMessage {
  }
  // scanForEnemies
  record AgroScan() implements MobMessage {
  }
  record RandomMove() implements MobMessage {
  }
  record Respawn() implements MobMessage {
  }

}
