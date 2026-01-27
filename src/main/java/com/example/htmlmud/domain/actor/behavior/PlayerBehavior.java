package com.example.htmlmud.domain.actor.behavior;

import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.protocol.GameCommand;

public interface PlayerBehavior {

  // 當切換到這個狀態時觸發 (Optional)
  default void onEnter(PlayerActor actor) {}

  // 處理指令
  // 回傳值：如果是 null 代表狀態不變；如果回傳新的 Behavior，代表狀態切換 (Become)
  PlayerBehavior handle(PlayerActor actor, GameCommand cmd);

}
