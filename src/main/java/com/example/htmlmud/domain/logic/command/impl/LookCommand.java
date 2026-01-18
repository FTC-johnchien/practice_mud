package com.example.htmlmud.domain.logic.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.actor.PlayerActor;
import com.example.htmlmud.domain.actor.RoomActor;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.domain.logic.command.PlayerCommand;
import com.example.htmlmud.domain.logic.command.annotation.CommandAlias;
import com.example.htmlmud.infra.util.AnsiColor;
import com.example.htmlmud.infra.util.ColorText;
import lombok.RequiredArgsConstructor;

@Component // 註冊為 Spring Bean
@RequiredArgsConstructor
@CommandAlias({"l", "see", "ls"}) // 支援 l, see, ls
public class LookCommand implements PlayerCommand {

  // 注入聚合服務，獲取 WorldManager
  private final GameServices services;

  @Override
  public String getKey() {
    return "look";
  }

  @Override
  public void execute(PlayerActor actor, String args) {
    // 1. 取得玩家當前位置 ID
    Integer roomId = actor.getCurrentRoomId();

    // 2. 查詢房間資料 (使用 WorldManager)
    RoomActor roomActor = services.worldManager().getRoomActor(roomId);

    if (roomActor == null) {
      actor.reply("你處於一片虛空之中... (RoomID: " + roomId + " 不存在)");
      return;
    }

    // 3. 組合輸出 (StringBuilder)
    StringBuilder sb = new StringBuilder();

    // 標題 (亮白色)
    sb.append(ColorText.wrap(AnsiColor.BRIGHT_WHITE,
        "=== " + roomActor.getTemplate().title() + " ===\r\n"));

    // 描述 (預設色/灰色)
    sb.append(ColorText.wrap(AnsiColor.LIGHT_GREY, roomActor.getTemplate().description()))
        .append("\r\n");

    // 出口 (黃色)
    sb.append(ColorText.wrap(AnsiColor.YELLOW, "[出口]: "));
    if (roomActor.getTemplate().exits().isEmpty()) {
      sb.append("無");
    } else {
      // ex: North, South, East
      sb.append(String.join(", ", roomActor.getTemplate().exits().keySet()));
    }
    sb.append("\r\n");

    // 房間內的其他生物 (不包含自己) (紅色)
    // 這裡需要 WorldManager 支援 "getActorsInRoom"
    sb.append(ColorText.wrap(AnsiColor.RED, "[生物]: "));
    // var others = gameServices.worldManager().getActorsInRoom(roomId).stream()
    // .filter(a -> !a.getObjectId().equals(actor.getObjectId())) // 過濾自己
    // .map(a -> a.getState().name) // 假設 State 有 name 欄位
    // .toList();

    // if (others.isEmpty()) {
    // sb.append("無");
    // } else {
    // sb.append(String.join(", ", others));
    // }

    // 4. 回傳給玩家
    actor.reply(sb.toString());
  }
}
