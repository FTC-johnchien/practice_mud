package com.example.htmlmud.domain.actor.behavior;

import com.example.htmlmud.domain.actor.impl.LivingActor;
import com.example.htmlmud.domain.actor.impl.MobActor;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.protocol.ActorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
// 商店行為
public class MerchantBehavior implements MobBehavior {

  private final int shopId;

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

      default -> log.warn("MerchantBehavior 收到無法處理的訊息: {} {}", self.getName(), msg);
    }

    return next;
  }

  @Override
  public void onPlayerEnter(MobActor self, PlayerActor player) {
    // 禮貌性問候
    self.sayToRoom("歡迎光臨！需要買點什麼嗎？(輸入 'list' 查看商品)");
  }

  @Override
  public void onInteract(MobActor self, PlayerActor player, String command) {
    if ("list".equalsIgnoreCase(command)) {
      // 顯示商品列表
      // ShopService.showList(player, shopId);
    } else if (command.startsWith("buy")) {
      // 處理購買
    } else {
      // 隨機講一句話
      String dialog = self.getTemplate().dialogues().iterator().next();
      self.sayToRoom(dialog);
    }
  }

  @Override
  public void onDamaged(MobActor self, LivingActor attacker) {
    // 守衛邏輯：如果被打，可能呼叫警衛，或者單純不理會(因為無敵)
    self.sayToRoom("衛兵！有人在鬧事！");
    // Spawn guards...
  }
}
