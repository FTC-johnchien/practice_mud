package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.model.Direction;
import com.example.htmlmud.domain.model.map.RoomExit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MoveCommand implements PlayerCommand {

  private final WorldManager worldManager;

  private final LookCommand lookCommand;


  @Override
  public String getKey() {
    return "move"; // 主鍵是 move，但我們會註冊 alias (n, s, e, w)
  }

  @Override
  public void execute(Player actor, String args) {
    // 1. 解析方向
    // 玩家可能輸入 "move north" 或者直接輸入 "north" (由 Dispatcher 轉發)
    Direction dir = Direction.parse(args);

    if (dir == null) {
      actor.reply("你要往哪個方向移動？");
      return;
    }

    // 2. 取得當前房間
    Room currentRoom = actor.getCurrentRoom();

    // 3. 檢查出口
    // 假設 Room.exits 是 Map<String, Integer> (key 是 direction full name)
    // Integer nextRoomId = currentRoom.getTemplate().exits().get(dir.getFullName());
    RoomExit exit = currentRoom.getTemplate().exits().get(dir.getFullName());
    if (exit == null) {
      actor.reply("往 " + dir.getDisplayName() + " 沒有出路。");
      return;
    }

    // TODO 檢查出口限制


    // 檢查要去的房間是否存在
    String targetRoomId = exit.targetRoomId();
    Room targetRoom = worldManager.getRoomActor(targetRoomId);

    if (targetRoom == null) {
      actor.reply("前方房間 " + targetRoomId + " 施工中，無法前往。");
      return;
    }


    // TODO 是否限制移動 守衛/鎖/魔法.........


    // --- 檢查成功，開始處理移動流程 ---

    // 4. 舊房間廣播 (離場)
    currentRoom.leave(actor, dir);

    // 6. 新房間廣播 (進場)
    // 計算反方向 (例如往北走，新房間的人會看到你從南方來)

    targetRoom.enter(actor, dir.opposite());

    // 7. 自動 Look (讓玩家看到新環境)
    // 直接調用 LookCommand 執行邏輯
    lookCommand.execute(actor, "");
  }
}
