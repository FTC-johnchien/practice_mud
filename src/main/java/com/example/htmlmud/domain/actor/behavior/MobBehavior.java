package com.example.htmlmud.domain.actor.behavior;

import com.example.htmlmud.domain.actor.impl.LivingActor;
import com.example.htmlmud.domain.actor.impl.MobActor;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.protocol.ActorMessage;

public interface MobBehavior {

  default void onEnter(MobActor actor) {}

  MobBehavior handle(MobActor self, ActorMessage.MobMessage msg);

  // 定期心跳 (例如每秒一次)：決定是否移動、回血、索敵
  default void onTick(MobActor self) {}

  // 當玩家進入視野
  default void onPlayerEnter(MobActor self, PlayerActor player) {}

  // 當玩家與其互動 (例如輸入 "talk guard")
  default void onInteract(MobActor self, PlayerActor player, String command) {}

  // 當受傷時 (仇恨值處理)
  default void onDamaged(MobActor self, LivingActor attacker) {}

  // 處理通用訊息
  default void onMessage(MobActor self, ActorMessage msg) {}
}
