package com.example.htmlmud.domain.actor.behavior;

import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.protocol.ActorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
// 商店行為
public class MerchantBehavior implements MobBehavior {

  private final int shopId;

  @Override
  public MobBehavior handle(Mob npc, ActorMessage.MobMessage msg) {
    MobBehavior next = null;
    switch (msg) {
      case ActorMessage.OnPlayerEnter(var playerId) -> {
        if (npc != null && npc.getCurrentRoom() != null) {
          Player player = npc.getCurrentRoom().findLiving(playerId)
              .filter(l -> l instanceof Player)
              .map(l -> (Player) l)
              .orElse(null);
          if (player != null && player.isValid()) {
            onPlayerEnter(npc, player);
          }
        }
      }
      case ActorMessage.OnPlayerFlee(var playerId, var direction) -> {
      }
      case ActorMessage.OnInteract(var playerId, var command) -> {
        if (npc != null && npc.getCurrentRoom() != null) {
          Player player = npc.getCurrentRoom().findLiving(playerId)
              .filter(l -> l instanceof Player)
              .map(l -> (Player) l)
              .orElse(null);
          if (player != null && player.isValid()) {
            onInteract(npc, player, command);
          }
        }
      }
      case ActorMessage.AgroScan() -> {
      }
      case ActorMessage.RandomMove() -> {
      }
      case ActorMessage.Respawn() -> {
        if (npc != null) {
          npc.getAggroTable().clear();
        }
      }

      default -> log.warn("MerchantBehavior 收到無法處理的訊息: {} {}", npc != null ? npc.getName() : "null", msg);
    }

    return next;
  }

  @Override
  public void onPlayerEnter(Mob npc, Player player) {
    // 禮貌性問候
    if (npc != null) {
      npc.sayToRoom("歡迎光臨！需要買點什麼嗎？(輸入 'list' 查看商品)");
    }
  }

  @Override
  public void onInteract(Mob npc, Player player, String command) {
    if (npc == null) return;
    if ("list".equalsIgnoreCase(command)) {
      npc.sayToRoom("客官請看貨架，所有好物均在此列。");
    } else if (command != null && command.startsWith("buy")) {
      npc.sayToRoom("多謝惠顧，貨真價實！");
    } else {
      // 隨機或預設講一句話 (防空保護)
      if (npc.getTemplate() != null && npc.getTemplate().dialogues() != null && !npc.getTemplate().dialogues().isEmpty()) {
        String dialog = npc.getTemplate().dialogues().iterator().next();
        npc.sayToRoom(dialog);
      } else {
        npc.sayToRoom("本店誠信經營，童叟無欺。");
      }
    }
  }

  @Override
  public void onDamaged(Mob npc, Living attacker) {
    if (npc != null) {
      npc.sayToRoom("衛兵！有人在鬧事！");
    }
  }
}
