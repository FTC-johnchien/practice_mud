package com.example.htmlmud.domain.actor.behavior;

import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.protocol.ActorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
// 被動行為 (一般 NPC)
public class PassiveBehavior implements MobBehavior {

  @Override
  public MobBehavior handle(Mob mob, ActorMessage.MobMessage msg) {
    MobBehavior next = null;
    switch (msg) {
      case ActorMessage.OnPlayerEnter(var playerId) -> {
        if (mob != null && mob.getCurrentRoom() != null) {
          Player player = mob.getCurrentRoom().findLiving(playerId)
              .filter(l -> l instanceof Player)
              .map(l -> (Player) l)
              .orElse(null);
          if (player != null && player.isValid()) {
            onPlayerEnter(mob, player);
          }
        }
      }
      case ActorMessage.OnPlayerFlee(var playerId, var direction) -> {
      }
      case ActorMessage.OnInteract(var playerId, var command) -> {
        if (mob != null && mob.getCurrentRoom() != null) {
          Player player = mob.getCurrentRoom().findLiving(playerId)
              .filter(l -> l instanceof Player)
              .map(l -> (Player) l)
              .orElse(null);
          if (player != null && player.isValid()) {
            onInteract(mob, player, command);
          }
        }
      }
      case ActorMessage.AgroScan() -> {
      }
      case ActorMessage.RandomMove() -> {
      }
      case ActorMessage.Respawn() -> {
        if (mob != null) {
          mob.getAggroTable().clear();
        }
      }

      default -> log.warn("PassiveBehavior 收到無法處理的訊息: {} {}", mob != null ? mob.getName() : "null", msg);
    }

    return next;
  }


  // 在 PassiveBehavior (一般 NPC) 的 onTick
  @Override
  public void onTick(Mob mob) {
    if (mob == null || !mob.isValid() || mob.isInCombat()) return;

    // 10% 機率說夢話或閒聊
    if (Math.random() < 0.1) {
      if (mob.getTemplate() != null && mob.getTemplate().dialogues() != null && !mob.getTemplate().dialogues().isEmpty()) {
        mob.sayToRoom(mob.getTemplate().dialogues().iterator().next());
      } else {
        mob.sayToRoom("今天天氣真好...");
      }
    }
  }

  @Override
  public void onPlayerEnter(Mob mob, Player player) {
  }

  @Override
  public void onInteract(Mob mob, Player player, String command) {
    if (mob != null) {
      if (mob.getTemplate() != null && mob.getTemplate().dialogues() != null && !mob.getTemplate().dialogues().isEmpty()) {
        String dialog = mob.getTemplate().dialogues().iterator().next();
        mob.sayToRoom(dialog);
      } else {
        mob.sayToRoom("有什麼事嗎，少俠？");
      }
    }
  }

  @Override
  public void onDamaged(Mob mob, Living attacker) {
    if (mob != null) {
      mob.sayToRoom("吼吼~！！！(它看起來想殺死你)");
      mob.attack(attacker);
    }
  }
}
