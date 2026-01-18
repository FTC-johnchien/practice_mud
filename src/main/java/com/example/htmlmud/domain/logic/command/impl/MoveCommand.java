package com.example.htmlmud.domain.logic.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.actor.PlayerActor;
import com.example.htmlmud.domain.actor.RoomActor;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.domain.logic.command.PlayerCommand;
import com.example.htmlmud.domain.model.Direction;
import com.example.htmlmud.domain.model.map.RoomExit;
import com.example.htmlmud.infra.util.AnsiColor;
import com.example.htmlmud.infra.util.ColorText;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MoveCommand implements PlayerCommand {

  private final GameServices services;

  @Override
  public String getKey() {
    return "move"; // 主鍵是 move，但我們會註冊 alias (n, s, e, w)
  }

  @Override
  public void execute(PlayerActor actor, String args) {
    // 1. 解析方向
    // 玩家可能輸入 "move north" 或者直接輸入 "north" (由 Dispatcher 轉發)
    Direction dir = Direction.parse(args);

    if (dir == null) {
      actor.reply("你要往哪個方向移動？");
      return;
    }

    // 2. 取得當前房間
    Integer currentRoomId = actor.getCurrentRoomId();
    RoomActor currentRoom = services.worldManager().getRoomActor(currentRoomId);

    if (currentRoom == null) {
      actor.reply("你在一片虛空中，無法移動。");
      return;
    }

    // 3. 檢查出口
    // 假設 Room.exits 是 Map<String, Integer> (key 是 direction full name)
    // Integer nextRoomId = currentRoom.getTemplate().exits().get(dir.getFullName());
    RoomExit exit = currentRoom.getTemplate().exits().get(dir.getFullName());


    if (exit == null) {
      actor.reply("往 " + dir.getDisplayName() + " 沒有出路。");
      return;
    }

    // --- 移動成功，開始處理流程 ---

    // 4. 舊房間廣播 (離場)
    String leaveMsg = ColorText.wrap(AnsiColor.YELLOW,
        actor.getDisplayName() + " 往 " + dir.getDisplayName() + " 離開了。");
    services.worldManager().broadcastToRoom(currentRoomId, leaveMsg, actor.getId());

    // 5. 更新玩家位置
    // 注意：這裡只改記憶體，State Pattern + Write-Behind 會負責存檔
    int targetRoomId = exit.targetRoomId();
    actor.setCurrentRoomId(targetRoomId);

    // 6. 新房間廣播 (進場)
    // 計算反方向 (例如往北走，新房間的人會看到你從南方來)
    String arriveMsg = ColorText.wrap(AnsiColor.YELLOW,
        actor.getDisplayName() + " 從 " + dir.opposite().getDisplayName() + " 過來了。");
    services.worldManager().broadcastToRoom(targetRoomId, arriveMsg, actor.getId());

    // 7. 自動 Look (讓玩家看到新環境)
    // 直接調用 Dispatcher 執行 look 指令
    actor.getDispatcher().dispatch(actor, "look");
  }
}
