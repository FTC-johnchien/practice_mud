package com.example.htmlmud.domain.service;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.command.parser.TargetSelector;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.entity.RoomStateRecord;
import com.example.htmlmud.domain.model.enums.Direction;
import com.example.htmlmud.domain.model.enums.MobKind;
import com.example.htmlmud.domain.model.template.RoomExit;
import com.example.htmlmud.domain.model.template.RoomTemplate;
import com.example.htmlmud.domain.model.template.SpawnRule;
import com.example.htmlmud.domain.model.template.ZoneTemplate;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import com.example.htmlmud.infra.util.AnsiColor;
import com.example.htmlmud.infra.util.ColorText;
import com.example.htmlmud.infra.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

  private final TargetSelector targetSelector;

  private final WorldFactory worldFactory;

  private final WorldManager manager;



  public ZoneTemplate getZoneTemplate(String zoneId) {
    return TemplateRepository.findZone(zoneId).orElse(null);
  }

  public RoomTemplate getRoomTemplate(String roomId) {
    return TemplateRepository.findRoom(roomId).orElse(null);
  }

  public void enter(Room room, List<Player> players, List<Mob> mobs, Living actor,
      Direction direction) {

    String livingName = null;
    // 當actor為玩家時，要將room里的mobs丟出playerEnter的message
    switch (actor) {
      case Player player -> {
        livingName = player.getNickname();
        if (!players.contains(player)) {
          players.add(player);
        }
        mobs.forEach(m -> m.onPlayerEnter(player.getId()));
      }
      case Mob mob -> {
        livingName = mob.getName();
        if (!mobs.contains(mob)) {
          mobs.add(mob);
        }
      }
    }

    // 設定 currentRoomId
    actor.setCurrentRoomId(room.getId());

    // 房間廣播訊息
    String arriveMsg =
        ColorText.wrap(AnsiColor.YELLOW, livingName + " 從 " + direction.getDisplayName() + " 過來了。");
    broadcastToOthers(players, actor.getId(), arriveMsg);
  }

  public void leave(Room room, List<Player> players, List<Mob> mobs, Living actor,
      Direction direction) {
    String livingName = null;
    switch (actor) {
      case Player player -> {
        players.remove(player);
        livingName = player.getNickname();
      }
      case Mob mob -> {
        mobs.remove(mob);
        livingName = mob.getName();
      }
    }

    String leaveMsg =
        ColorText.wrap(AnsiColor.YELLOW, livingName + " 往 " + direction.getDisplayName() + " 離開了。");

    broadcast(players, mobs, actor.getId(), null, leaveMsg);
  }

  @Deprecated
  public void doLook(Room roomActor, String playerId, CompletableFuture<String> future) {
    log.info("Player {} Look room", playerId);

    // StringBuilder sb = new StringBuilder();
    // sb.append("\u001B[1;36m").append(template.name()).append("\u001B[0m\r\n");
    // sb.append(template.description()).append("\r\n");

    // if (template.exits() != null && !template.exits().isEmpty()) {
    // sb.append("\u001B[33m[出口]: ").append(String.join(", ", template.exits().keySet()))
    // .append("\u001B[0m\r\n");
    // }

    // StringBuilder others = new StringBuilder();
    // players.stream().filter(p -> !p.getId().equals(playerId))
    // .forEach(p -> others.append(p.getNickname()).append(" "));

    // if (!others.isEmpty()) {
    // sb.append("\u001B[35m[這裡有]: \u001B[0m").append(others).append("\r\n");
    // }
    // future.complete(sb.toString());
    future.complete(null);
  }

  public void say(List<Player> players, String sourceId, String content) {
    Player speaker =
        players.stream().filter(p -> p.getId().equals(sourceId)).findFirst().orElse(null);
    String name = (speaker != null) ? speaker.getName() : "有人";

    broadcastToOthers(players, sourceId, name + ": " + content);
  }

  public GameItem tryPickItem(List<GameItem> items, String args, Player picker) {
    log.info("tryPickItem: {}", args);

    GameItem targetItem = targetSelector.selectItem(items, args);
    if (targetItem == null) {
      return null;
    }

    items.remove(targetItem);
    return targetItem;
  }

  // 處理邏輯
  public void tick(Room room, long tickCount, long timestamp) {
    // log.info("{} tickCount: {}", id, tickCount);

    // === 1. World/Zone 層級邏輯 (例如：每 60 秒檢查一次重生) ===
    if (tickCount % room.getZoneTemplate().respawnTime() == 0) {
      checkSpawnRule(); // 檢查是否有怪物死掉很久該重生了
    }

    // === 2. 轉發給 Actor ===
    // 過濾掉 "完全沒事做且沒玩家在場" 的怪物
    // if (!players.isEmpty() || mobs.stream().anyMatch(m -> m.isInCombat())) {
    // ActorMessage.Tick msg = new ActorMessage.Tick(tickCount, timestamp);

    // for (Mob mob : mobs) {
    // mob.tick(tickCount, timestamp);
    // }

    // for (Player player : players) {
    // // log.info("send player");
    // player.tick(tickCount, timestamp);
    // }
    // }
  }

  public void broadcast(List<Player> players, List<Mob> mobs, String actorId, String targetId,
      String message) {
    Living actor = manager.findLivingActor(actorId).orElse(null);
    Living target = null;

    if (targetId != null) {

      // 先找房間內的mobs
      target = mobs.stream().filter(m -> m.getId().equals(targetId)).findFirst().orElse(null);

      // 再找 players
      if (target == null) {
        target = players.stream().filter(p -> p.getId().equals(targetId)).findFirst().orElse(null);
      }

      if (target == null) {
        target = manager.findLivingActor(targetId).orElse(null);
      }
    }

    for (Player player : players) {
      MessageUtil.send(message, actor, target, player);
    }
  }

  public void broadcastToOthers(List<Player> players, String actorId, String message) {
    Living actor = manager.findLivingActor(actorId).orElse(null);
    players.stream().filter(p -> !p.getId().equals(actorId))
        .forEach(p -> MessageUtil.send(message, actor, p));
  }

  public Living findLiving(List<Player> players, List<Mob> mobs, String livingId) {
    // 結合兩個列表並搜尋第一個匹配項
    return Stream.concat(players.stream(), mobs.stream())
        .filter(living -> living.getId().equals(livingId)).findFirst().orElse(null);
  }

  public String lookAtRoom(Room room, List<Player> players, List<Mob> mobs, List<GameItem> items,
      String playerId) {
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
    List<Player> otherPlayers =
        sortedPlayers(players.stream().filter(p -> !p.getId().equals(playerId)).toList());

    // 取得房間內的怪物
    // List<Mob> mobs = room.getMobs();
    // mobs = sortedMobs(mobs);

    // 取得房間內的物品
    List<GameItem> sortedItems = sortedItems(items);

    log.info("otherPlayers: {}, mobs: {} items: {}", otherPlayers.size(), mobs.size(),
        sortedItems.size());


    // 1. 篩選出 其他玩家 (亮藍色，排除自己)
    List<String> otherPlayerNames = otherPlayers.stream().filter(p -> !p.getId().equals(playerId))
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
    List<String> itemNames = sortedItems.stream()
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

  public String lookDirection(Room room, Player player, Direction dir) {
    RoomExit exit = room.getTemplate().exits().get(dir.getFullName());
    if (exit == null) {
      return "那裡沒有路。";
    }

    // 這裡可以進階：透過 WorldManager 取得隔壁房間的簡要資訊
    return "你往 " + dir.getDisplayName() + " 方看去，隱約看到通往隔壁的出口。";
  }



  public void record(String roomTemplateId, List<GameItem> items) {
    RoomStateRecord record = toRecord(roomTemplateId, items);
    // TODO 還沒紀錄
    // 丟給 save 的 queue 就不管了



  }



  /**
   * 房間初次載入時的生怪邏輯
   */
  public void spawnInitial(Room room, List<Mob> mobs, List<GameItem> items) {
    List<SpawnRule> spawnRules = room.getTemplate().spawnRules();
    if (spawnRules != null && !spawnRules.isEmpty()) {
      for (SpawnRule rule : spawnRules) {
        for (int i = 0; i < rule.count(); i++) {
          switch (rule.type()) {
            case "MOB" -> spawnOneMob(room, mobs, rule);
            case "ITEM" -> spawnOneItem(room, items, rule);
          }
        }
      }
    }
  }



  // ---------------------------------------------------------------------------------------------



  // 定義一個簡單的狀態判斷
  private String getHealthStatus(Mob mob) {
    double pct = (double) mob.getStats().getHp() / mob.getStats().getMaxHp();
    if (pct >= 0.85)
      return AnsiColor.GREEN + "[狀態佳]" + AnsiColor.RESET;
    if (pct > 0.5)
      return AnsiColor.YELLOW + "[受輕傷]" + AnsiColor.RESET;
    if (pct > 0.2)
      return AnsiColor.RED + "[受重傷]" + AnsiColor.RESET;

    return AnsiColor.RED_BOLD + "[瀕　死]" + AnsiColor.RESET;
  }

  private String getMobDescription(List<Mob> mobs, AnsiColor color) {

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
  private List<Player> sortedPlayers(List<Player> players) {
    List<Player> sortedPlayers =
        players.stream().sorted(Comparator.comparing(Player::getName)).toList();
    return sortedPlayers;
  }

  // 排序規則：名稱 -> ID
  private List<Mob> sortedMobs(List<Mob> mobs) {
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



  private void checkSpawnRule() {

  }

  private void spawnOneMob(Room room, List<Mob> mobs, SpawnRule rule) {

    // 處理機率 (例如：稀有怪只有 10% 機率出現)
    if (Math.random() > rule.rate()) {
      return;
    }

    // 1. 呼叫工廠產生 MobActor (這裡會給予 UUID)
    Mob mob = worldFactory.createMob(rule.id());
    try {
      // log.info("{}: {} - {}", mob.getId(), mob.getName(),
      // objectMapper.writeValueAsString(mob.getTemplate()));
    } catch (Exception e) {
      e.printStackTrace();
    }

    // // 2. 設定位置
    mob.setCurrentRoomId(room.getId());

    // // 3. 加入房間列表
    mobs.add(mob);

    log.info("Spawned {} in room {}", mob.getTemplate().name(), room.getId());
  }

  private void spawnOneItem(Room room, List<GameItem> items, SpawnRule rule) {
    // 處理機率 (例如：稀有怪只有 10% 機率出現)
    if (Math.random() > rule.rate()) {
      return;
    }

    GameItem item = worldFactory.createItem(rule.id());
    if (item == null) {
      return;
    }

    items.add(item);
    log.info("Spawned {} in room {}", item.getDisplayName(), room.getId());
  }



  private RoomStateRecord toRecord(String roomTemplateId, List<GameItem> items) {
    String[] args = roomTemplateId.split(":");
    String zoneId = args[0];
    String roomId = args[1];
    return new RoomStateRecord(roomId, zoneId, new ArrayList<>(items));
  }

}
