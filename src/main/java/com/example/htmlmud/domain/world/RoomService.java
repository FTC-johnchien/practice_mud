package com.example.htmlmud.domain.world;

import com.example.htmlmud.domain.service.TargetSelector;
import com.example.htmlmud.domain.service.MessageUtil;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.port.WorldEntityFactoryPort;
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
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.protocol.MudMessage;
import com.example.htmlmud.protocol.util.AnsiColor;
import com.example.htmlmud.protocol.util.ColorText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

  private final TargetSelector targetSelector;

  private final WorldEntityFactoryPort worldFactory;

  private final WorldManager manager;

  private final TemplateReader templateReader;

  private final com.example.htmlmud.infra.persistence.service.RoomPersistenceService roomPersistenceService;



  public ZoneTemplate getZoneTemplate(String zoneId) {
    return templateReader.findZone(zoneId).orElse(null);
  }

  public RoomTemplate getRoomTemplate(String roomId) {
    return templateReader.findRoom(roomId).orElse(null);
  }

  public void enter(Room room, List<Mob> mobs, Living actor, Direction direction) {
    String livingName = null;
    switch (actor) {
      case Player player -> {
        livingName = player.getNickname();
        room.setProtagonist(player);
        mobs.forEach(m -> m.onPlayerEnter(player.getId()));
      }
      case Mob mob -> {
        if (room.hasFlag(com.example.htmlmud.domain.model.enums.RoomFlag.NO_MOB)) {
          log.info("房間 {} 禁止怪物進入，阻止 Mob {}", room.getId(), mob.getId());
          return;
        }
        livingName = mob.getName();
        if (!mobs.contains(mob)) {
          mobs.add(mob);
        }
      }
    }

    // 設定 currentRoomId
    actor.setCurrentRoomId(room.getId());

    // 房間廣播訊息
    String arriveMsg = direction != null
        ? ColorText.wrap(AnsiColor.YELLOW, livingName + " 從 " + direction.getDisplayName() + " 過來了。")
        : ColorText.wrap(AnsiColor.YELLOW, livingName + " 出現在此處。");
    broadcastToOthers(room, actor.getId(), arriveMsg);
  }

  @Deprecated
  public void enter(Room room, List<Player> players, List<Mob> mobs, Living actor, Direction direction) {
    enter(room, mobs, actor, direction);
  }

  public void leave(Room room, List<Mob> mobs, Living actor, Direction direction) {
    String livingName = null;
    switch (actor) {
      case Player player -> {
        if (room.getProtagonist() == player || (room.getProtagonist() != null && player.getId().equals(room.getProtagonist().getId()))) {
          room.clearProtagonist();
        }
        livingName = player.getNickname();
      }
      case Mob mob -> {
        mobs.remove(mob);
        livingName = mob.getName();
      }
    }

    String leaveMsg =
        ColorText.wrap(AnsiColor.YELLOW, livingName + " 往 " + direction.getDisplayName() + " 離開了。");

    broadcast(room, mobs, actor.getId(), null, leaveMsg);
  }

  @Deprecated
  public void leave(Room room, List<Player> players, List<Mob> mobs, Living actor, Direction direction) {
    leave(room, mobs, actor, direction);
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

  public void say(Room room, String sourceId, String content) {
    Player protagonist = room.getProtagonist();
    String name = (protagonist != null && protagonist.getId().equals(sourceId))
        ? protagonist.getName()
        : "有人";

    broadcastToOthers(room, sourceId, name + ": " + content);
  }

  @Deprecated
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
    if (room.getZoneTemplate() != null && room.getZoneTemplate().respawnTime() > 0
        && tickCount % room.getZoneTemplate().respawnTime() == 0) {
      checkSpawnRule(); // 檢查是否有怪物死掉很久該重生了
    }

    // === 2. 轉發給 Actor ===
    // 過濾掉 "完全沒事做且沒玩家在場" 的怪物
    if (room.hasProtagonist() || room.getMobs().stream().anyMatch(Living::isInCombat)) {
      for (Mob mob : room.getMobs()) {
        mob.tick(tickCount, timestamp);
      }

      Player protagonist = room.getProtagonist();
      if (protagonist != null) {
        protagonist.tick(tickCount, timestamp);
      }
    }
  }

  public void broadcast(Room room, List<Mob> mobs, String actorId, String targetId,
      String message) {
    Player protagonist = room.getProtagonist();
    if (protagonist == null) {
      return;
    }
    Living actor = null;
    if (actorId != null) {
      if (actorId.equals(protagonist.getId())) {
        actor = protagonist;
      } else {
        actor = mobs.stream().filter(m -> m.getId().equals(actorId)).findFirst().orElse(null);
      }
      if (actor == null) {
        actor = manager.findLivingActor(actorId).orElse(null);
      }
    }
    Living target = null;
    if (targetId != null) {
      if (targetId.equals(protagonist.getId())) {
        target = protagonist;
      } else {
        target = mobs.stream().filter(m -> m.getId().equals(targetId)).findFirst().orElse(null);
      }
      if (target == null) {
        target = manager.findLivingActor(targetId).orElse(null);
      }
    }
    MessageUtil.send(message, actor, target, protagonist);
  }

  @Deprecated
  public void broadcast(List<Player> players, List<Mob> mobs, String actorId, String targetId,
      String message) {
    Living actor = manager.findLivingActor(actorId).orElse(null);
    Living target = null;
    if (targetId != null) {
      target = mobs.stream().filter(m -> m.getId().equals(targetId)).findFirst().orElse(null);
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

  public void broadcastJson(Room room, MudMessage<Object> message) {
    Player protagonist = room.getProtagonist();
    if (protagonist != null) {
      protagonist.sendJson(message);
    }
  }

  @Deprecated
  public void broadcastJson(List<Player> players, MudMessage<Object> message) {
    for (Player player : players) {
      player.sendJson(message);
    }
  }

  public void broadcastToOthers(Room room, String actorId, String message) {
    Player protagonist = room.getProtagonist();
    if (protagonist == null) {
      return;
    }
    // 單機模式下，若事件源即為主角本人（如自己丟下物品），排他性廣播直接略過
    if (actorId != null && actorId.equals(protagonist.getId())) {
      return;
    }
    Living actor = manager.findLivingActor(actorId).orElse(null);
    MessageUtil.send(message, actor, protagonist);
  }

  @Deprecated
  public void broadcastToOthers(List<Player> players, String actorId, String message) {
    Living actor = manager.findLivingActor(actorId).orElse(null);
    players.stream().filter(p -> !p.getId().equals(actorId))
        .forEach(p -> MessageUtil.send(message, actor, p));
  }

  public Living findLiving(Room room, List<Mob> mobs, String livingId) {
    Player protagonist = room.getProtagonist();
    if (protagonist != null && protagonist.getId().equals(livingId)) {
      return protagonist;
    }
    return mobs.stream().filter(living -> living.getId().equals(livingId)).findFirst().orElse(null);
  }

  @Deprecated
  public Living findLiving(List<Player> players, List<Mob> mobs, String livingId) {
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

    // 單機模式下房內無其他玩家，直接略過 otherPlayers 計算
    List<Player> otherPlayers = List.of();
    List<String> otherPlayerNames = List.of();

    // 取得房間內的物品
    List<GameItem> sortedItems = sortedItems(items);


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
    if (roomPersistenceService != null) {
      roomPersistenceService.saveAsync(record);
    }
  }

  /**
   * 房間初次載入時的生怪與掉落物邏輯
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

    // 若資料庫有歷史持久化的掉落物狀態，載入並還原 (以存檔狀態覆蓋初始靜態掉落)
    if (roomPersistenceService != null && room.getTemplate() != null) {
      String zoneId = room.getTemplate().zoneId();
      String roomId = room.getTemplate().id();
      roomPersistenceService.loadDroppedItems(zoneId, roomId).ifPresent(persistedItems -> {
        items.clear();
        items.addAll(persistedItems);
      });
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

    // Key 是 "前綴 + 名字 + 狀態"
    Map<String, Long> groups = mobs.stream()
        .collect(groupingBy(
            m -> {
              String prefix = (m.getTemplate().rank() != null) ? m.getTemplate().rank().getPrefix() : "";
              return prefix + m.getName() + "(" + m.getTemplate().aliases().get(0) + ") " + getHealthStatus(m);
            },
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
    if (room.hasFlag(com.example.htmlmud.domain.model.enums.RoomFlag.NO_MOB)) {
      return;
    }

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
    String zoneId = "";
    String roomId = (roomTemplateId != null) ? roomTemplateId : "";
    if (roomTemplateId != null && roomTemplateId.contains(":")) {
      String[] args = roomTemplateId.split(":", 2);
      zoneId = args[0];
      roomId = args[1];
    } else if (roomTemplateId != null) {
      RoomTemplate tpl = getRoomTemplate(roomTemplateId);
      if (tpl != null && tpl.zoneId() != null) {
        zoneId = tpl.zoneId();
      }
    }
    return new RoomStateRecord(roomId, zoneId, new ArrayList<>(items));
  }

}
