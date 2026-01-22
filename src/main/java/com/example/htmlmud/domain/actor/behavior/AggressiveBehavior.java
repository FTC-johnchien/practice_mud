package com.example.htmlmud.domain.actor.behavior;

import com.example.htmlmud.domain.actor.LivingActor;
import com.example.htmlmud.domain.actor.impl.MobActor;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
// 攻擊行為
public class AggressiveBehavior implements MobBehavior {

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
