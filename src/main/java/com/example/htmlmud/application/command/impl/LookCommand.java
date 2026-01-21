package com.example.htmlmud.application.command.impl;

import java.util.List;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.application.command.annotation.CommandAlias;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.actor.impl.MobActor;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.domain.actor.impl.RoomActor;
import com.example.htmlmud.domain.model.MobKind;
import com.example.htmlmud.infra.util.AnsiColor;
import com.example.htmlmud.infra.util.ColorText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component // 註冊為 Spring Bean
@RequiredArgsConstructor
@CommandAlias({"l", "see", "ls"}) // 支援 l, see, ls
public class LookCommand implements PlayerCommand {

  private final WorldManager worldManager;

  @Override
  public String getKey() {
    return "look";
  }

  @Override
  public void execute(PlayerActor actor, String args) {
    // 1. 取得玩家當前位置 ID
    String roomId = actor.getCurrentRoomId();

    log.info("args:{}", args);

    // 2. 查詢房間資料 (使用 WorldManager)
    RoomActor roomActor = worldManager.getRoomActor(roomId);

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
    sb.append(ColorText.room("=== " + room.getTemplate().name() + " ===")).append("\r\n");

    // 描述 (預設色/灰色)
    sb.append(ColorText.roomDesc(room.getTemplate().description())).append("\r\n");

    // 出口 (黃色)
    sb.append(ColorText.exit("[出口]: "));
    if (room.getTemplate().exits().isEmpty()) {
      sb.append("無");
    } else {
      sb.append(String.join(", ", room.getTemplate().exits().keySet()));
    }
    sb.append("\r\n");

    // 取得房間內的生物 (玩家與怪物，經過排序)
    List<PlayerActor> players = room.getPlayersSnapshot();
    List<MobActor> mobs = room.getMobsSnapshot();
    log.info("players: {}, mobs: {}", players.size(), mobs.size());


    // 1. 篩選出 其他玩家 (亮藍色，排除自己)
    List<String> otherPlayerNames = players.stream().filter(p -> !p.getId().equals(actor.getId()))
        .map(p -> ColorText.player(p.getNickname() + "(" + p.getName() + ")")).toList();

    // 2. 篩選出 NPC (綠色顯示)
    List<String> npcNames = mobs.stream().filter(m -> m.getTemplate().kind() == MobKind.FRIENDLY)
        .map(m -> ColorText.npc(m.getTemplate().name() + "(" + m.getTemplate().aliases()[0] + ")"))
        .toList();

    // 3. 篩選出 怪物 (紅色顯示)
    List<String> monsterNames = mobs.stream().filter(
        m -> m.getTemplate().kind() == MobKind.AGGRESSIVE || m.getTemplate().kind() == MobKind.BOSS)
        .map(m -> ColorText.mob(m.getTemplate().name() + "(" + m.getTemplate().aliases()[0] + ")")) // 紅色代表危險
        .toList();

    if (!otherPlayerNames.isEmpty()) {
      sb.append(ColorText.wrap(AnsiColor.BRIGHT_MAGENTA, "[玩家]: "))
          .append(String.join(", ", otherPlayerNames)).append("\r\n");
    }
    if (!npcNames.isEmpty()) {
      sb.append(ColorText.npc("[人物]: ")).append(String.join(", ", npcNames)).append("\r\n");
    }
    if (!monsterNames.isEmpty()) {
      sb.append(ColorText.wrap(AnsiColor.RED, "[怪物]: ")).append(String.join(", ", monsterNames))
          .append("\r\n");
    }

    return sb.toString();
  }
}
