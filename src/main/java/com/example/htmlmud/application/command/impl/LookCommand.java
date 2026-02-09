package com.example.htmlmud.application.command.impl;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.application.command.annotation.CommandAlias;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.enums.Direction;
import com.example.htmlmud.domain.model.enums.MobKind;
import com.example.htmlmud.domain.service.RoomService;
import com.example.htmlmud.infra.util.AnsiColor;
import com.example.htmlmud.infra.util.ColorText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component // 註冊為 Spring Bean
@RequiredArgsConstructor
@CommandAlias({"l", "see", "ls"}) // 支援 l, see, ls
public class LookCommand implements PlayerCommand {

  private final RoomService roomService;

  @Override
  public String getKey() {
    return "look";
  }

  @Override
  public void execute(Player player, String args) {

    // 1. 如果沒有參數 -> 看當前房間
    if (args == null || args.trim().isEmpty()) {
      roomService.lookAtRoom(player);
      return;
    }

    String targetName = args.trim().toLowerCase();

    // 2. 判斷是否為「方向」 (look north)
    Direction dir = Direction.parse(targetName);
    if (dir != null) {
      roomService.lookDirection(player, dir);
      return;
    }

    // 3. 判斷是否為「自己」 (look me)
    if (targetName.equals("me") || targetName.equals("self")) {
      roomService.lookAtSelf(player);
      return;
    }

    // 4. 看具體目標 (生物或物品)
    roomService.lookAtTarget(player, targetName);

    // 查詢房間資料 (使用 WorldManager)
    Room room = player.getCurrentRoom();

    // 3. 產生房間描述
    String roomDescription = buildRoomDescription(player, room);

    // 4. 回傳給玩家
    player.reply(roomDescription);
  }

  private String buildRoomDescription(Player player, Room room) {
    StringBuilder sb = new StringBuilder();

    // 標題 (亮白色)
    sb.append(ColorText.room("=== " + room.getTemplate().name() + " ===")).append("\r\n");

    // 描述 (預設色/灰色)
    sb.append(ColorText.roomDesc(room.getTemplate().description())).append("\r\n");

    // 出口 (黃色)
    sb.append(ColorText.exit("[出口]: "));
    if (room.getTemplate().exits() == null || room.getTemplate().exits().isEmpty()) {
      sb.append("無");
    } else {
      sb.append(String.join(", ", room.getTemplate().exits().keySet()));
    }
    sb.append("\r\n");

    // 取得房間內的玩家
    List<Player> otherPlayers = sortedPlayers(
        room.getPlayers().stream().filter(p -> !p.getId().equals(player.getId())).toList());

    // 取得房間內的怪物
    List<Mob> mobs = room.getMobs();
    // mobs = sortedMobs(mobs);

    // 取得房間內的物品
    List<GameItem> items = sortedItems(room.getItems());

    log.info("otherPlayers: {}, mobs: {} items: {}", otherPlayers.size(), mobs.size(),
        items.size());


    // 1. 篩選出 其他玩家 (亮藍色，排除自己)
    List<String> otherPlayerNames =
        otherPlayers.stream().filter(p -> !p.getId().equals(player.getId()))
            .map(p -> ColorText.player(p.getName() + "(" + p.getAliases().get(0) + ")")).toList();


    // 2. 篩選出 NPC (綠色顯示)
    List<Mob> npcs = mobs.stream().filter(m -> m.getTemplate().kind() == MobKind.FRIENDLY).toList();
    npcs = sortedMobs(npcs);

    // 3. 篩選出 怪物 (紅色顯示)
    List<Mob> monsters = mobs.stream().filter(
        m -> m.getTemplate().kind() == MobKind.AGGRESSIVE || m.getTemplate().kind() == MobKind.BOSS)
        .toList();
    monsters = sortedMobs(monsters);

    // items
    List<String> itemNames = items.stream()
        .map(i -> ColorText.item(i.getDisplayName() + "(" + i.getAliases().get(0) + ")")).toList();

    sb.append(AnsiColor.YELLOW).append("這裡有：\n").append(AnsiColor.RESET);
    if (!otherPlayerNames.isEmpty()) {
      sb.append(ColorText.wrap(AnsiColor.BRIGHT_MAGENTA, "[玩家]: "))
          .append(String.join(", ", otherPlayerNames)).append("\r\n");
    }
    if (!npcs.isEmpty()) {
      sb.append(getMobDescription(npcs, AnsiColor.GREEN)).append("\r\n");
      // sb.append(ColorText.npc(getMobDescription(npcs))).append("\r\n");
      // sb.append(ColorText.npc("[人物]: ")).append(String.join(", ", npcNames)).append("\r\n");
    }
    if (!monsters.isEmpty()) {
      sb.append(getMobDescription(monsters, AnsiColor.RED)).append("\r\n");
      // sb.append(ColorText.wrap(AnsiColor.RED, getMobDescription(monsters))).append("\r\n");
      // sb.append(ColorText.wrap(AnsiColor.RED, "[怪物]: ")).append(String.join(", ", monsterNames))
      // .append("\r\n");
    }
    if (!itemNames.isEmpty()) {
      sb.append(ColorText.wrap(AnsiColor.YELLOW, "[物品]: ")).append(String.join(", ", itemNames))
          .append("\r\n");
    }

    return sb.toString();
  }

  // public String getMobDescription(List<Mob> mobs) {
  // if (mobs.isEmpty())
  // return "";

  // // 1. 根據 "名稱(別名)" 分組並計數
  // Map<String, Long> counts = mobs.stream().collect(
  // groupingBy(m -> m.getName() + "(" + m.getTemplate().aliases().get(0) + ")", counting()));


  // // Map<String, Long> counts = mobs.stream().collect(Collectors.groupingBy(
  // // (Mob m) -> m.getName() + "(" + m.getTemplate().aliases()[0] + ")", Collectors.counting()));

  // StringBuilder sb = new StringBuilder();

  // // 2. 組合字串
  // for (Map.Entry<String, Long> entry : counts.entrySet()) {
  // long count = entry.getValue();
  // String displayName = entry.getKey(); // 這裡已經是 "名稱(別名)"
  // if (count > 1) {
  // sb.append(String.format(" (x%d) %s\n", count, displayName));
  // } else {
  // sb.append(String.format(" %s\n", displayName));
  // }
  // }
  // return sb.toString();
  // }

  // 定義一個簡單的狀態判斷
  public String getHealthStatus(Mob mob) {
    double pct = (double) mob.getStats().getHp() / mob.getStats().getMaxHp();
    if (pct >= 0.85)
      return AnsiColor.GREEN + "[狀態佳]" + AnsiColor.RESET;
    if (pct > 0.5)
      return AnsiColor.YELLOW + "[受輕傷]" + AnsiColor.RESET;
    if (pct > 0.2)
      return AnsiColor.RED + "[受重傷]" + AnsiColor.RESET;

    return AnsiColor.RED_BOLD + "[瀕　死]" + AnsiColor.RESET;
  }

  public String getMobDescription(List<Mob> mobs, AnsiColor color) {

    // Key 是 "狀態 + 名字"
    Map<String, Long> groups = mobs.stream()
        .collect(groupingBy(
            m -> m.getName() + "(" + m.getTemplate().aliases().get(0) + ") " + getHealthStatus(m),
            counting()));

    // 輸出
    StringBuilder sb = new StringBuilder();
    groups.forEach((key, count) -> {
      if (count > 1) {
        sb.append("(x").append(count).append(") ");
      }

      sb.append(ColorText.wrap(color, key)).append("\r\n");
    });
    return sb.toString();
  }

  // 排序規則：name
  public List<Player> sortedPlayers(List<Player> players) {
    List<Player> sortedPlayers =
        players.stream().sorted(Comparator.comparing(Player::getName)).toList();
    return sortedPlayers;
  }

  // 排序規則：名稱 -> ID
  public List<Mob> sortedMobs(List<Mob> mobs) {
    List<Mob> sortedMobs = mobs.stream()
        .sorted(Comparator.comparing((Mob m) -> m.getTemplate().name()).thenComparing(Mob::getId))
        .toList();
    return sortedMobs;
  }

  // 排序規則：名稱 -> ID
  private List<GameItem> sortedItems(List<GameItem> items) {
    List<GameItem> sortedItems = items.stream()
        .sorted(Comparator.comparing((GameItem i) -> i.getName()).thenComparing(GameItem::getId))
        .toList();
    return sortedItems;
  }
}
