package com.example.htmlmud.domain.logic.command;

import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.PlayerActor;
import com.example.htmlmud.domain.actor.RoomActor;
import com.example.htmlmud.domain.model.map.RoomExit;
import com.example.htmlmud.protocol.RoomMessage;
import com.example.htmlmud.service.world.WorldManager;

@Service
public class CommandDispatcher {

  private final WorldManager worldManager;

  public CommandDispatcher(WorldManager worldManager) {
    this.worldManager = worldManager;
  }

  public void dispatch(PlayerActor player, String input) {
    String[] parts = input.trim().split("\\s+", 2);
    String command = parts[0].toLowerCase();
    String argument = (parts.length > 1) ? parts[1] : "";

    switch (command) {
      case "look", "l" -> handleLook(player);
      case "move", "m", "n", "s", "e", "w" -> handleMove(player, command, argument); // 支援簡寫
      case "say" -> handleSay(player, argument);
      default -> player.sendText("你是誰？你想去哪？(未知的指令)");
    }
  }

  // --- 實作 Look 指令 ---
  private void handleLook(PlayerActor player) {
    int roomId = player.getCurrentRoomId();
    RoomActor room = worldManager.getRoomActor(roomId);

    // 1. 建立 Future 接收結果
    CompletableFuture<String> future = new CompletableFuture<>();

    // 2. 發送訊息給房間 (非同步)
    room.send(new RoomMessage.Look(player.getId(), future));

    // 3. 當房間回傳結果時，傳送給玩家 (這段會在 Platform Thread 或 VT 上執行)
    future.thenAccept(description -> player.sendText(description));
  }

  // --- 實作 Move 指令 ---
  private void handleMove(PlayerActor player, String cmd, String arg) {
    // 處理方向縮寫
    String direction = switch (cmd) {
      case "n" -> "north";
      case "s" -> "south";
      case "e" -> "east";
      case "w" -> "west";
      default -> (arg.isEmpty() ? cmd : arg); // 如果是 "move north"
    };

    // 1. 檢查當前房間是否有這個出口
    // 注意：這裡我們直接讀取 Template，因為這是不變的，不需要問 RoomActor (效能優化)
    RoomActor currentRoom = worldManager.getRoomActor(player.getCurrentRoomId());
    RoomExit exit = currentRoom.getTemplate().exits().get(direction);

    if (exit == null) {
      player.sendText("那個方向沒有路。");
      return;
    }

    // TODO: 檢查門是否上鎖 (ex: if (exit.locked()) ...)

    // 2. 執行移動邏輯 (兩階段提交：先離開，再進入)
    int oldRoomId = player.getCurrentRoomId();
    int newRoomId = exit.targetRoomId();

    // 2a. 通知舊房間：玩家離開
    currentRoom.send(new RoomMessage.PlayerLeave(player.getId()));

    // 2b. 更新玩家狀態
    player.setCurrentRoomId(newRoomId);

    // 2c. 通知新房間：玩家進入
    RoomActor newRoom = worldManager.getRoomActor(newRoomId);
    CompletableFuture<Void> enterFuture = new CompletableFuture<>();
    newRoom.send(new RoomMessage.PlayerEnter(player, enterFuture));

    // 2d. 進入完成後，自動執行 Look
    enterFuture.thenRun(() -> handleLook(player));
  }

  private void handleSay(PlayerActor player, String content) {
    RoomActor room = worldManager.getRoomActor(player.getCurrentRoomId());
    room.send(new RoomMessage.Say(player.getId(), content));
  }
}
