package com.example.htmlmud.domain.actor.behavior;

import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.protocol.ActorMessage;

public interface MobBehavior {

  MobBehavior handle(Mob mob, ActorMessage.MobMessage msg);

  default void onEnter(Mob actor) {}

  // 定期心跳 (例如每秒一次)：決定是否移動、回血、索敵
  default void onTick(Mob mob) {}

  // 當玩家進入視野
  default void onPlayerEnter(Mob mob, Player player) {}

  // 當玩家與其互動 (例如輸入 "talk guard")
  default void onInteract(Mob mob, Player player, String command) {}

  // 被攻擊時 (仇恨值處理)
  default void onAttacked(Mob mob, Living attacker) {}

  // 當受傷時 (仇恨值處理)
  default void onDamaged(Mob mob, Living attacker) {}

  // 處理通用訊息
  default void onMessage(Mob mob, ActorMessage msg) {}
}
