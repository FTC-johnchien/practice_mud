package com.example.htmlmud.domain.actor.behavior;

import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.protocol.ActorMessage;

public interface MobBehavior {

  default void onEnter(Mob actor) {}

  MobBehavior handle(Mob self, ActorMessage.MobMessage msg);

  // 定期心跳 (例如每秒一次)：決定是否移動、回血、索敵
  default void onTick(Mob self) {}

  // 當玩家進入視野
  default void onPlayerEnter(Mob self, Player player) {}

  // 當玩家與其互動 (例如輸入 "talk guard")
  default void onInteract(Mob self, Player player, String command) {}

  // 當受傷時 (仇恨值處理)
  default void onDamaged(Mob self, Living attacker) {}

  // 處理通用訊息
  default void onMessage(Mob self, ActorMessage msg) {}
}
