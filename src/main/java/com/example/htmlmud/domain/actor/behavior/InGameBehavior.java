package com.example.htmlmud.domain.actor.behavior;

import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.protocol.GameCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class InGameBehavior implements PlayerBehavior {

  @Override
  public void onEnter() {
    log.info("InGameBehavior onEnter()");
    Player actor = MudContext.currentPlayer();

    // 進場時自動看一次房間
    actor.getService().getCommandDispatcher().dispatch("look");
  }

  @Override
  public PlayerBehavior handle(GameCommand cmd) {
    // log.info("InGameBehavior handle()");
    Player actor = MudContext.currentPlayer();

    // 目前只處理文字輸入 (Input)
    if (cmd instanceof GameCommand.Input(var text)) {
      // 【關鍵】將文字交給 Dispatcher
      actor.getService().getCommandDispatcher().dispatch(text);
    }
    // else if (cmd instanceof GameCommand.Logout) {
    // actor.reply("登出中...");
    // actor.handleDisconnect();
    // // ... 切換回 GuestBehavior ...
    // }

    return null; // 保持 InGame 狀態
  }
}
