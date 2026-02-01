package com.example.htmlmud.domain.actor.behavior;

import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.protocol.ActorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
// 攻擊行為
public class AggressiveBehavior implements MobBehavior {

  @Override
  public void onEnter(Mob actor) {
    log.info("AggressiveBehavior onEnter()");

    // 進場時自動scanForEnemies
    // AgroScan()
  }

  @Override
  public MobBehavior handle(Mob self, ActorMessage.MobMessage msg) {
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
  public void onPlayerEnter(Mob self, Player player) {
    // 主動怪邏輯：看到玩家就攻擊
    // 發送戰鬥指令給 BattleSystem
    self.sayToRoom("滾出去！" + player.getName());
    self.attack(player);
  }

  @Override
  public void onInteract(Mob self, Player player, String command) {
    self.sayToRoom("吼！！！(它看起來不想跟你說話)");
    self.attack(player);
  }

  @Override
  public void onDamaged(Mob self, Living attacker) {
    self.sayToRoom("吼吼吼！！！(它看起來想殺死你)");
    self.attack(attacker);
  }
}
