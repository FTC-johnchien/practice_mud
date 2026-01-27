package com.example.htmlmud.domain.actor.behavior;

import com.example.htmlmud.domain.actor.impl.LivingActor;
import com.example.htmlmud.domain.actor.impl.MobActor;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.protocol.ActorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
// 被動行為 (一般 NPC)
public class PassiveBehavior implements MobBehavior {

  @Override
  public MobBehavior handle(MobActor self, ActorMessage.MobMessage msg) {
    MobBehavior next = null;
    switch (msg) {
      case ActorMessage.OnPlayerEnter(var playerId) -> {
      }
      case ActorMessage.OnPlayerFlee(var playerId, var direction) -> {
      }
      case ActorMessage.OnInteract(var playerId, var command) -> {
      }
      case ActorMessage.AgroScan() -> {
      }
      case ActorMessage.RandomMove() -> {
      }
      case ActorMessage.Respawn() -> {
      }

      default -> log.warn("PassiveBehavior 收到無法處理的訊息: {} {}", self.getName(), msg);
    }

    return next;
  }


  // 在 PassiveBehavior (一般 NPC) 的 onTick
  public void onTick(MobActor self) {
    // 5% 機率隨機移動
    if (Math.random() < 0.05) {
      // 隨機選一個出口移動
      // self.moveTo(randomExit);
    }

    // 10% 機率說夢話
    if (Math.random() < 0.1) {
      self.sayToRoom("今天天氣真好...");
    }
  }

  @Override
  public void onPlayerEnter(MobActor self, PlayerActor player) {

  }

  @Override
  public void onDamaged(MobActor self, LivingActor attacker) {
    self.sayToRoom("吼吼~！！！(它看起來想殺死你)");
    self.attack(attacker);
  }
}
