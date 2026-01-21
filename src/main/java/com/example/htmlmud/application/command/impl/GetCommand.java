package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.application.command.annotation.CommandAlias;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.domain.actor.impl.RoomActor;
import com.example.htmlmud.protocol.RoomMessage;
import lombok.RequiredArgsConstructor;

@Component
@CommandAlias("get")
@RequiredArgsConstructor
public class GetCommand implements PlayerCommand {

  private final WorldManager worldManager;

  @Override
  public String getKey() {
    return "get";
  }

  @Override
  public void execute(PlayerActor actor, String args) {
    // 1. 基本檢查
    if (args.isEmpty()) {
      actor.reply("你要撿什麼？");
      return;
    }

    // 2. 找到房間 Actor
    String roomId = actor.getCurrentRoomId();
    RoomActor room = worldManager.getRoomActor(roomId);

    // 3. 發送請求 (這是非同步的)
    // 注意：這裡有兩種做法
    // 方法 A: 發後不理 (Fire-and-Forget)，結果由 ActorMessage 回傳處理
    // 方法 B: 使用 CompletableFuture 等待結果 (較直觀，但在 Actor 模型中要小心死鎖)

    // 這裡示範更符合 Actor 精神的 "Ask Pattern" (或是透過回呼)
    // 為了簡單起見，我們假設 RoomActor 有一個同步的安全方法 (如果都在同一台機器)
    // 或者發送一個 "AttemptPick" 訊息

    room.send(new RoomMessage.TryPickItem(args, actor));
  }
}
