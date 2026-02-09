package com.example.htmlmud.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.command.parser.TargetSelector;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.entity.RoomStateRecord;
import com.example.htmlmud.domain.model.enums.Direction;
import com.example.htmlmud.domain.model.template.RoomTemplate;
import com.example.htmlmud.domain.model.template.SpawnRule;
import com.example.htmlmud.domain.model.template.ZoneTemplate;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import com.example.htmlmud.infra.util.AnsiColor;
import com.example.htmlmud.infra.util.ColorText;
import com.example.htmlmud.infra.util.MessageUtil;
import com.example.htmlmud.protocol.RoomMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

  private final MessageUtil messageUtil;

  private final TemplateRepository templateRepo;

  private final TargetSelector targetSelector;

  private final WorldFactory worldFactory;

  private final WorldManager manager;



  public ZoneTemplate getZoneTemplate(String zoneId) {
    return templateRepo.findZone(zoneId).orElse(null);
  }

  public RoomTemplate getRoomTemplate(String roomId) {
    return templateRepo.findRoom(roomId).orElse(null);
  }

  public void doEnter(Room room, List<Player> players, List<Mob> mobs, Living actor,
      Direction direction, CompletableFuture<Void> future) {

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
    doBroadcastToOthers(players, actor.getId(), arriveMsg);

    future.complete(null);
  }

  public void doLeave(Room room, List<Player> players, List<Mob> mobs, Living actor,
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

    doBroadcast(players, mobs, actor.getId(), null, leaveMsg);
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

  public void doSay(List<Player> players, String sourceId, String content) {
    Player speaker =
        players.stream().filter(p -> p.getId().equals(sourceId)).findFirst().orElse(null);
    String name = (speaker != null) ? speaker.getName() : "有人";

    doBroadcastToOthers(players, sourceId, name + ": " + content);
  }

  public void doTryPickItem(List<GameItem> items, String args, Player picker,
      CompletableFuture<GameItem> future) {
    log.info("tryPickItem: {}", args);

    GameItem targetItem = targetSelector.selectItem(items, args);
    if (targetItem == null) {
      future.complete(null);
      return;
    }

    items.remove(targetItem);
    future.complete(targetItem);
  }

  // 處理邏輯
  public void doTick(Room room, List<Player> players, List<Mob> mobs, long tickCount,
      long timestamp) {
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

  public void doBroadcast(List<Player> players, List<Mob> mobs, String actorId, String targetId,
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
      messageUtil.send(message, actor, target, player);
    }
  }

  public void doBroadcastToOthers(List<Player> players, String actorId, String message) {
    Living actor = manager.findLivingActor(actorId).orElse(null);
    for (Player player : players) {
      if (player.getId().equals(actorId)) {
        continue;
      }
      messageUtil.send(message, actor, player);
    }
  }

  public void doFindLiving(List<Player> players, List<Mob> mobs, String livingId,
      CompletableFuture<Living> future) {

    // 先找 mobs
    Living living = mobs.stream().filter(m -> m.getId().equals(livingId)).findFirst().orElse(null);
    if (living != null) {
      future.complete(living);
      return;
    }

    // 再找 players
    living = players.stream().filter(p -> p.getId().equals(livingId)).findFirst().orElse(null);
    if (living != null) {
      future.complete(living);
      return;
    }

    // 都找不到就回傳 null
    future.complete(null);
  }

  public void toRecord(String roomTemplateId, List<GameItem> items,
      CompletableFuture<RoomStateRecord> future) {
    try {
      RoomStateRecord record = toRecord(roomTemplateId, items);
      future.complete(record);
    } catch (Exception e) {
      log.error("toRecord error", e);
      future.completeExceptionally(e);
    }
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



  /**
   * 需等待處理完成的事件 (room資料的異動或取得可能被異動的資料)
   */
  public void enter(Room room, Living actor, Direction direction) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    room.send(new RoomMessage.Enter(actor, direction, future));
    try {
      future.orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("Room enter 失敗 roomId:{}", room.getId(), e);
      actor.reply("一股未知的力量阻擋了$N的前進!");
    }
  }

  public Optional<GameItem> tryPickItem(Room room, String args, Player picker) {
    CompletableFuture<GameItem> future = new CompletableFuture<>();
    room.send(new RoomMessage.TryPickItem(args, picker, future));
    try {
      GameItem item = future.orTimeout(1, TimeUnit.SECONDS).join();
      if (item != null) {
        return Optional.of(item);
      }
    } catch (Exception e) {
      log.error("Room tryPickItem 失敗 roomId:{}", room.getId(), e);
    }

    return Optional.empty();
  }

  public Optional<Living> findLiving(Room room, String livingId) {
    CompletableFuture<Living> future = new CompletableFuture<>();
    room.send(new RoomMessage.FindLiving(livingId, future));
    try {
      Living living = future.orTimeout(1, TimeUnit.SECONDS).join();
      if (living != null) {
        return Optional.of(living);
      }
    } catch (Exception e) {
      log.error("Room 取得 Living 列表失敗", e);
    }

    return Optional.empty();
  }

  public List<Living> getLivings(Room room) {
    CompletableFuture<List<Living>> future = new CompletableFuture<>();
    room.send(new RoomMessage.GetLivings(future));
    try {
      return future.orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("Room 取得 Living 列表失敗", e);
    }
    return new ArrayList<>();
  }

  public List<Player> getPlayers(Room room) {
    CompletableFuture<List<Player>> future = new CompletableFuture<>();
    room.send(new RoomMessage.GetPlayers(future));
    try {
      return future.orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("Room 取得 Player 列表失敗", e);
    }
    return new ArrayList<>();
  }

  public List<Mob> getMobs(Room room) {
    CompletableFuture<List<Mob>> future = new CompletableFuture<>();
    room.send(new RoomMessage.GetMobs(future));
    try {
      return future.orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("Room 取得 Mob 列表失敗", e);
    }
    return new ArrayList<>();
  }

  public List<GameItem> getItems(Room room) {
    CompletableFuture<List<GameItem>> future = new CompletableFuture<>();
    room.send(new RoomMessage.GetItems(future));
    try {
      return future.orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("Room 取得 Mob 列表失敗", e);
    }
    return new ArrayList<>();
  }

  public void record(Room room) {
    CompletableFuture<RoomStateRecord> future = new CompletableFuture<>();
    room.send(new RoomMessage.ToRecord(future));
    try {
      future.orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("Room record 失敗 roomId:{}", room.getId(), e);
    }
  }



  // ---------------------------------------------------------------------------------------------



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
