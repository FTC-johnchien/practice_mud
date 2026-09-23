package com.example.htmlmud.domain.service;

import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.model.enums.Direction;
import com.example.htmlmud.domain.model.template.RoomExit;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.service.PartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 處理 MUD 房間與城鎮地圖拓撲移動之領域服務 (Room & Town Movement Service)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomMovementService {

  private final WorldManager worldManager;
  private final GameStateBroadcastService broadcastService;
  private final PartyService partyService;

  public boolean move(Player player, Direction dir) {
    if (player == null || dir == null) {
      if (player != null) player.reply("你要往哪個方向移動？");
      return false;
    }

    Room currentRoom = player.getCurrentRoom();
    if (currentRoom == null || currentRoom.getTemplate() == null) {
      player.reply("你身處未知虛空中，無法辨認方向。");
      return false;
    }

    RoomExit exit = currentRoom.getTemplate().exits().get(dir.getFullName());
    if (exit == null) {
      player.reply("往 " + dir.getDisplayName() + " 沒有出路。");
      return false;
    }

    String targetRoomId = exit.targetRoomId();
    Room targetRoom = worldManager.getRoomActor(targetRoomId);
    if (targetRoom == null) {
      player.reply("前方房間 " + targetRoomId + " 施工中，無法前往。");
      return false;
    }

    // 離場與進場
    currentRoom.leave(player, dir);
    targetRoom.enter(player, dir.opposite());

    // 狀態更新與呈現
    if (!player.isInDungeon()) {
      Party party = (partyService != null) ? partyService.getOrCreateParty(player) : null;
      boolean isSolo = (party == null || party.getMembers() == null || party.getMembers().size() <= 1);
      String traveler = isSolo ? "【" + player.getName() + "】" : "小隊";
      player.reply("\u001B[1;32m🚶 " + traveler + "向" + dir.getDisplayName() + "前行，抵達【" + targetRoom.getTemplate().name() + "】。\u001B[0m");
      if (broadcastService != null) {
        broadcastService.broadcastState(player);
      }
    } else {
      player.reply(targetRoom.lookAtRoom(player));
      if (broadcastService != null) {
        broadcastService.broadcastState(player);
      }
    }
    return true;
  }
}
