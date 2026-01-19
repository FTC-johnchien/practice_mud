package com.example.htmlmud.domain.actor.behavior;

import com.example.htmlmud.domain.actor.MobActor;
import com.example.htmlmud.domain.actor.PlayerActor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
// 被動行為 (一般 NPC)
public class PassiveBehavior implements MobBehavior {

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

}
