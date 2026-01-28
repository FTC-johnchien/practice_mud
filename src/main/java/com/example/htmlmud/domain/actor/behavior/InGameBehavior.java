package com.example.htmlmud.domain.actor.behavior;

import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.protocol.GameCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class InGameBehavior implements PlayerBehavior {
  // 這裡可以注入 CommandHandlerRegistry

  @Override
  public void onEnter(PlayerActor actor) {
    log.info("InGameBehavior onEnter()");

    // 進場時自動看一次房間
    actor.getPlayerService().getCommandDispatcher().dispatch(actor, "look");
  }

  @Override
  public PlayerBehavior handle(PlayerActor actor, GameCommand cmd) {
    // log.info("InGameBehavior handle()");

    // 目前只處理文字輸入 (Input)
    if (cmd instanceof GameCommand.Input(var text)) {
      // 【關鍵】將文字交給 Dispatcher
      actor.getPlayerService().getCommandDispatcher().dispatch(actor, text);
    }
    // else if (cmd instanceof GameCommand.Logout) {
    // actor.reply("登出中...");
    // actor.handleDisconnect();
    // // ... 切換回 GuestBehavior ...
    // }

    return null; // 保持 InGame 狀態
  }
}
