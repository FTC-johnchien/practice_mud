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

      default -> log.warn("MerchantBehavior 收到無法處理的訊息: {} {}", npc.getName(), msg);
    }

    return next;
  }

  @Override
  public void onPlayerEnter(Mob npc, Player player) {
    // 禮貌性問候
    npc.sayToRoom("歡迎光臨！需要買點什麼嗎？(輸入 'list' 查看商品)");
  }

  @Override
  public void onInteract(Mob npc, Player player, String command) {
    if ("list".equalsIgnoreCase(command)) {
      // 顯示商品列表
      // ShopService.showList(player, shopId);
    } else if (command.startsWith("buy")) {
      // 處理購買
    } else {
      // 隨機講一句話
      String dialog = npc.getTemplate().dialogues().iterator().next();
      npc.sayToRoom(dialog);
    }
  }

  @Override
  public void onDamaged(Mob npc, Living attacker) {
    // 守衛邏輯：如果被打，可能呼叫警衛，或者單純不理會(因為無敵)
    npc.sayToRoom("衛兵！有人在鬧事！");
    // Spawn guards...
  }
}
