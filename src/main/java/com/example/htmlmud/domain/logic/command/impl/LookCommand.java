package com.example.htmlmud.domain.logic.command.impl;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.actor.MobActor;
import com.example.htmlmud.domain.actor.PlayerActor;
import com.example.htmlmud.domain.actor.RoomActor;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.domain.logic.command.PlayerCommand;
import com.example.htmlmud.domain.logic.command.annotation.CommandAlias;
import com.example.htmlmud.domain.model.MobKind;
import com.example.htmlmud.infra.util.AnsiColor;
import com.example.htmlmud.infra.util.ColorText;
import com.example.htmlmud.service.world.WorldManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component // 註冊為 Spring Bean
@RequiredArgsConstructor
@CommandAlias({"l", "see", "ls"}) // 支援 l, see, ls
public class LookCommand implements PlayerCommand {

  @Override
  public String getKey() {
    return "look";
  }

  @Override
  public void execute(PlayerActor actor, String args) {
    // 1. 取得玩家當前位置 ID
    Integer roomId = actor.getCurrentRoomId();

    // 2. 查詢房間資料 (使用 WorldManager)
    RoomActor roomActor = actor.getWorldManager().getRoomActor(roomId);

    if (roomActor == null) {
      actor.reply("你處於一片虛空之中... (RoomID: " + roomId + " 不存在)");
      return;
    }

    // 3. 產生房間描述
    String roomDescription = buildRoomDescription(actor, roomActor);



    // 4. 回傳給玩家
    actor.reply(roomDescription);
  }

  private String buildRoomDescription(PlayerActor actor, RoomActor room) {
    StringBuilder sb = new StringBuilder();

    // 標題 (亮白色)
    sb.append(
        ColorText.wrap(AnsiColor.BRIGHT_WHITE, "=== " + room.getTemplate().name() + " ===\r\n"));

    // 描述 (預設色/灰色)
    sb.append(ColorText.wrap(AnsiColor.LIGHT_GREY, room.getTemplate().description()))
        .append("\r\n");

    // 出口 (黃色)
    sb.append(ColorText.wrap(AnsiColor.YELLOW, "[出口]: "));
    if (room.getTemplate().exits().isEmpty()) {
      sb.append("無");
    } else {
      sb.append(String.join(", ", room.getTemplate().exits().keySet()));
    }
    sb.append("\r\n");

    // 取得房間內的生物 (玩家與怪物，皆為 Set)
    Set<PlayerActor> players = room.getPlayers();
    Set<MobActor> mobs = room.getMobs();
    log.info("players: {}, mobs: {}", players.size(), mobs.size());


    // 1. 篩選出 其他玩家 (亮藍色，排除自己)
    List<String> otherPlayerNames = players.stream().filter(p -> !p.getId().equals(actor.getId()))
        .map(p -> ColorText.wrap(AnsiColor.BRIGHT_BLUE, p.getNickname())).toList();

    // 2. 篩選出 NPC (綠色顯示)
    List<String> npcNames = mobs.stream().filter(m -> m.getTemplate().kind() == MobKind.FRIENDLY)
        .map(m -> ColorText.wrap(AnsiColor.GREEN, m.getTemplate().name() + " [NPC]")).toList();

    // 3. 篩選出 怪物 (紅色顯示)
    List<String> monsterNames = mobs.stream().filter(
        m -> m.getTemplate().kind() == MobKind.AGGRESSIVE || m.getTemplate().kind() == MobKind.BOSS)
        .map(m -> ColorText.wrap(AnsiColor.RED, m.getTemplate().name())) // 紅色代表危險
        .toList();

    if (!otherPlayerNames.isEmpty()) {
      sb.append(ColorText.wrap(AnsiColor.BRIGHT_MAGENTA, "[玩家]: "))
          .append(String.join(", ", otherPlayerNames)).append("\r\n");
    }
    if (!npcNames.isEmpty()) {
      sb.append(ColorText.wrap(AnsiColor.GREEN, "[人物]: ")).append(String.join(", ", npcNames))
          .append("\r\n");
    }
    if (!monsterNames.isEmpty()) {
      sb.append(ColorText.wrap(AnsiColor.RED, "[怪物]: ")).append(String.join(", ", monsterNames))
          .append("\r\n");
    }

    return sb.toString();
  }
}
