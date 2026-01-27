package com.example.htmlmud.domain.actor.behavior;

import com.example.htmlmud.domain.actor.impl.LivingActor;
import com.example.htmlmud.domain.actor.impl.MobActor;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.protocol.ActorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
// 攻擊行為
public class AggressiveBehavior implements MobBehavior {

  @Override
  public void onEnter(MobActor actor) {
    log.info("AggressiveBehavior onEnter()");

    // 進場時自動scanForEnemies
    // AgroScan()
  }

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

      default -> log.warn("AggressiveBehavior 收到無法處理的訊息: {} {}", self.getName(), msg);
    }

    return next;
  }


  @Override
  public void onPlayerEnter(MobActor self, PlayerActor player) {
    // 主動怪邏輯：看到玩家就攻擊
    // 發送戰鬥指令給 BattleSystem
    self.sayToRoom("滾出去！" + player.getName());
    self.attack(player);
  }

  @Override
  public void onInteract(MobActor self, PlayerActor player, String command) {
    self.sayToRoom("吼！！！(它看起來不想跟你說話)");
    self.attack(player);
  }

  @Override
  public void onDamaged(MobActor self, LivingActor attacker) {
    self.sayToRoom("吼吼吼！！！(它看起來想殺死你)");
    self.attack(attacker);
  }
}
