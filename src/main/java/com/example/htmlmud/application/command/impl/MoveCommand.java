package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.enums.Direction;
import com.example.htmlmud.domain.model.template.RoomExit;
import com.example.htmlmud.domain.service.WorldManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MoveCommand implements PlayerCommand {

  private final WorldManager worldManager;
  private final LookCommand lookCommand;
  private final com.example.htmlmud.domain.service.GameStateBroadcastService broadcastService;
  private final com.example.htmlmud.domain.party.service.PartyService partyService;


  @Override
  public String getKey() {
    return "move"; // 主鍵是 move，但我們會註冊 alias (n, s, e, w)
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();

    // 1. 解析方向
    // 玩家可能輸入 "move north" 或者直接輸入 "north" (由 Dispatcher 轉發)
    Direction dir = Direction.parse(args);

    if (dir == null) {
      player.reply("你要往哪個方向移動？");
      return;
    }

    // 2. 取得當前房間
    Room currentRoom = player.getCurrentRoom();

    // 3. 檢查出口
    // 假設 Room.exits 是 Map<String, Integer> (key 是 direction full name)
    // Integer nextRoomId = currentRoom.getTemplate().exits().get(dir.getFullName());
    RoomExit exit = currentRoom.getTemplate().exits().get(dir.getFullName());
    if (exit == null) {
      player.reply("往 " + dir.getDisplayName() + " 沒有出路。");
      return;
    }

    // TODO 檢查出口限制


    // 檢查要去的房間是否存在
    String targetRoomId = exit.targetRoomId();
    Room targetRoom = worldManager.getRoomActor(targetRoomId);

    if (targetRoom == null) {
      player.reply("前方房間 " + targetRoomId + " 施工中，無法前往。");
      return;
    }


    // TODO 是否限制移動 守衛/鎖/魔法.........


    // --- 檢查成功，開始處理移動流程 ---

    // 4. 舊房間廣播 (離場)
    currentRoom.leave(player, dir);

    // 6. 新房間廣播 (進場)
    // 計算反方向 (例如往北走，新房間的人會看到你從南方來)

    targetRoom.enter(player, dir.opposite());

    // 7. 自動更新環境狀態
    if (!player.isInDungeon()) {
      // 城鎮模式下具備圖形化主舞台，日誌中僅需簡潔行進提示，免除原始 MUD 房間文字洗版
      com.example.htmlmud.domain.party.model.Party party = (partyService != null) ? partyService.getOrCreateParty(player.getName()) : null;
      boolean isSolo = (party == null || party.getMembers() == null || party.getMembers().size() <= 1);
      String traveler = isSolo ? "【" + player.getName() + "】" : "小隊";
      player.reply("\u001B[1;32m🚶 " + traveler + "向" + dir.getDisplayName() + "前行，抵達【" + targetRoom.getTemplate().name() + "】。\u001B[0m");
      broadcastService.broadcastState(player);
    } else {
      lookCommand.execute("");
    }
  }
}
