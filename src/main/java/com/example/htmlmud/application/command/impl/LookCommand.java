package com.example.htmlmud.application.command.impl;

import java.util.List;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.application.command.annotation.CommandAlias;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.model.GameItem;
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

  @Override
  public String getKey() {
    return "look";
  }

  @Override
  public void execute(Player actor, String args) {

    // 查詢房間資料 (使用 WorldManager)
    Room room = actor.getCurrentRoom();

    if (room == null) {
      actor.reply("你處於一片虛空之中... (玩家: " + actor.getName() + " 的 currentRoom 不存在)");
      return;
    }

    // 3. 產生房間描述
    String roomDescription = buildRoomDescription(actor, room);

    // 4. 回傳給玩家
    actor.reply(roomDescription);
  }

  private String buildRoomDescription(Player actor, Room room) {
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
    List<Player> players = room.getPlayers();
    List<Mob> mobs = room.getMobs();
    List<GameItem> items = room.getItems();
    log.info("players: {}, mobs: {} items: {}", players.size(), mobs.size(), items.size());
    players.stream().forEach(p -> log.info("player: {}:{}", p.getId(), p.getName()));


    // 1. 篩選出 其他玩家 (亮藍色，排除自己)
    List<String> otherPlayerNames = players.stream().filter(p -> !p.getId().equals(actor.getId()))
        .map(p -> ColorText.player(p.getNickname() + "(" + p.getName() + ")")).toList();

    // 2. 篩選出 NPC (綠色顯示)
    List<String> npcNames =
        mobs.stream().filter(m -> m.getTemplate().kind() == MobKind.FRIENDLY)
            .map(m -> ColorText
                .npc(m.getTemplate().name() + "(" + m.getTemplate().aliases().get(0) + ")"))
            .toList();

    // 3. 篩選出 怪物 (紅色顯示)
    List<String> monsterNames = mobs.stream().filter(
        m -> m.getTemplate().kind() == MobKind.AGGRESSIVE || m.getTemplate().kind() == MobKind.BOSS)
        .map(m -> ColorText
            .mob(m.getTemplate().name() + "(" + m.getTemplate().aliases().get(0) + ")")) // 紅色代表危險
        .toList();

    // items
    List<String> itemNames = items.stream()
        .map(i -> ColorText.item(i.getDisplayName() + "(" + i.getTemplate().aliases().get(0) + ")"))
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
    if (!itemNames.isEmpty()) {
      sb.append(ColorText.wrap(AnsiColor.YELLOW, "[物品]: ")).append(String.join(", ", itemNames))
          .append("\r\n");
    }

    return sb.toString();
  }
}
