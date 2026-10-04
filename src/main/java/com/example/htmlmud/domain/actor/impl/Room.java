package com.example.htmlmud.domain.actor.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import com.example.htmlmud.domain.actor.core.RoomMessageBuffer;
import com.example.htmlmud.domain.actor.core.VirtualActor; // 引用您的基礎類別
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.enums.Direction;
import com.example.htmlmud.domain.model.template.RoomTemplate;
import com.example.htmlmud.domain.model.template.ZoneTemplate;
import com.example.htmlmud.domain.service.RoomService;
import com.example.htmlmud.protocol.MudMessage;
import com.example.htmlmud.protocol.RoomMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
// 1. 繼承 VirtualActor，並指定泛型為 RoomMessage
public class Room extends VirtualActor<RoomMessage> {

  private final RoomService roomService;

  @Getter
  private final String id;

  @Getter
  private final RoomTemplate template;

  @Getter
  private final ZoneTemplate zoneTemplate;

  // 房間內的生物與物品 (Runtime State) - 使用 CopyOnWriteArrayList 支援無鎖快照讀取
  private final List<Player> players = new CopyOnWriteArrayList<>();

  private final List<Mob> mobs = new CopyOnWriteArrayList<>();

  private final List<GameItem> items = new CopyOnWriteArrayList<>(); // 地上的物品

  // 戶籍名冊：記錄這個房間生出來且還活著的怪物 ID
  // Key: TemplateID (ex: "snow_guard"), Value: Set of Instance UUIDs
  private Map<String, Set<String>> trackedMobs = new HashMap<>();

  @Getter
  private RoomMessageBuffer buffer = new RoomMessageBuffer(this);



  private final Set<String> dynamicFlags = new java.util.concurrent.CopyOnWriteArraySet<>();

  public Room(String id, RoomService roomService) {
    super("room-" + id);
    this.roomService = roomService;

    this.id = id;
    this.template = roomService.getRoomTemplate(id);
    if (this.template == null) {
      log.error("roomTemplate is null id:{}", id);
      throw new MudException("此處空間被不明的力量給破碎了");
    }

    this.zoneTemplate = roomService.getZoneTemplate(template.zoneId());
    if (this.zoneTemplate == null) {
      // log.error("zoneTemplate is null id:{}", template.zoneId());
      throw new MudException("zoneTemplate is null id:" + template.zoneId());
    }

    roomService.spawnInitial(this, mobs, items);
  }

  public Room(String id, String name, String description, RoomService roomService) {
    super("room-" + id);
    this.roomService = roomService;
    this.id = id;
    this.template = RoomTemplate.builder()
        .id(id)
        .name(name)
        .description(description)
        .flags(new java.util.HashSet<>())
        .build();
    this.zoneTemplate = null;
  }

  public void addFlag(com.example.htmlmud.domain.model.enums.RoomFlag flag) {
    if (flag != null) {
      dynamicFlags.add(flag.name());
    }
  }

  public void addFlag(String flag) {
    if (flag != null) {
      dynamicFlags.add(flag);
    }
  }

  public void removeFlag(String flag) {
    if (flag != null) {
      dynamicFlags.remove(flag);
    }
  }

  public void addPlayer(Player player) {
    if (player == null) {
      return;
    }
    if (isActorThread()) {
      if (!players.contains(player)) {
        players.add(player);
        player.setCurrentRoomId(this.id);
      }
      return;
    }
    enter(player, null);
  }

  public void addMob(Mob mob) {
    if (mob == null) {
      return;
    }
    if (isActorThread()) {
      if (!mobs.contains(mob)) {
        mobs.add(mob);
        mob.setCurrentRoomId(this.id);
      }
      return;
    }
    enter(mob, null);
  }



  // --- 實作父類別的抽象方法 ---
  @Override
  protected void handleMessage(RoomMessage msg) {
    // 這裡的邏輯跟之前一模一樣，但不需要自己寫 loop 和 try-catch 了
    switch (msg) {
      case RoomMessage.Enter(var actor, var direction, var future) -> {
        roomService.enter(this, players, mobs, actor, direction);
        future.complete(null);
      }
      case RoomMessage.Leave(var actor, var direction) -> {
        roomService.leave(this, players, mobs, actor, direction);
      }
      case RoomMessage.Say(var sourceId, var content) -> {
        roomService.say(players, sourceId, content);
      }
      case RoomMessage.TryPickItem(var args, var picker, var future) -> {
        GameItem picked = roomService.tryPickItem(items, args, picker);
        if (picked != null) {
          roomService.record(this.getId(), items);
        }
        future.complete(picked);
      }
      case RoomMessage.Tick(var tickCount, var timestamp) -> {
        roomService.tick(this, tickCount, timestamp);
      }
      case RoomMessage.Broadcast(var sourceId, var targetId, var message) -> {
        roomService.broadcast(players, mobs, sourceId, targetId, message);
      }
      case RoomMessage.BroadcastJson(var message) -> {
        roomService.broadcastJson(players, message);
      }
      case RoomMessage.BroadcastToOthers(var sourceId, var message) -> {
        roomService.broadcastToOthers(players, sourceId, message);
      }
      case RoomMessage.FindLiving(var livingId, var future) -> {
        future.complete(roomService.findLiving(players, mobs, livingId));
      }
      case RoomMessage.GetLivings(var future) -> {
        future.complete(
            Stream.concat(players.stream(), mobs.stream()).filter(Living::isValid).toList());
      }
      case RoomMessage.GetPlayers(var future) -> {
        future.complete(players.stream().filter(Player::isValid).toList());
      }
      case RoomMessage.RemovePlayer(var playerId, var future) -> {
        boolean removed = false;
        if (playerId != null) {
          removed = players.removeIf(player -> player.getId().equals(playerId));
        }
        if (future != null) {
          future.complete(removed);
        }
      }
      case RoomMessage.GetMobs(var future) -> {
        future.complete(mobs.stream().filter(Mob::isValid).toList());
      }
      case RoomMessage.RemoveMob(var mobId, var future) -> {
        boolean removed = false;
        if (mobId != null) {
          removed = mobs.removeIf(mob -> mob.getId().equals(mobId));
        }
        if (future != null) {
          future.complete(removed);
        }
      }
      case RoomMessage.GetItems(var future) -> {
        future.complete(items);
      }
      case RoomMessage.RemoveItem(var itemId, var future) -> {
        boolean removed = false;
        if (itemId != null) {
          removed = items.removeIf(item -> item.getId().equals(itemId));
          if (removed) {
            roomService.record(this.getId(), items);
          }
        }
        if (future != null) {
          future.complete(removed);
        }
      }
      case RoomMessage.DropItem(var item, var future) -> {
        boolean added = false;
        if (item != null && !items.contains(item)) {
          items.add(item);
          roomService.record(this.getId(), items);
          added = true;
        }
        if (future != null) {
          future.complete(added);
        }
      }
      case RoomMessage.Record() -> {
        roomService.record(this.getId(), items);
      }
      case RoomMessage.LookAtRoom(var playerId, var future) -> {
        future.complete(roomService.lookAtRoom(this, players, mobs, items, playerId));
      }
      case RoomMessage.LookDirection(var player, var dir, var future) -> {
        future.complete(roomService.lookDirection(this, player, dir));
      }

    }
  }



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  // 公開給外部呼叫的方法 --------------------------------------------------------------------------



  public void enter(Living actor, Direction direction) {
    if (isActorThread()) {
      roomService.enter(this, players, mobs, actor, direction);
      return;
    }
    CompletableFuture<Void> future = new CompletableFuture<>();
    this.send(new RoomMessage.Enter(actor, direction, future));
    try {
      future.orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("Room enter 失敗 roomId:{}", id, e);
      if (actor instanceof Player player) {
        player.reply("一股未知的力量阻擋了你的前進!");
      }
    }
  }

  public void leave(Living actor, Direction direction) {
    this.send(new RoomMessage.Leave(actor, direction));
  }

  public void say(String sourceId, String content) {
    this.send(new RoomMessage.Say(sourceId, content));
  }

  public void tick(long tickCount, long timestamp) {
    this.send(new RoomMessage.Tick(tickCount, timestamp));
  }

  public Optional<GameItem> tryPickItem(String args, Player picker) {
    if (isActorThread()) {
      GameItem item = roomService.tryPickItem(items, args, picker);
      if (item != null) {
        roomService.record(this.getId(), items);
      }
      return Optional.ofNullable(item);
    }
    CompletableFuture<GameItem> future = new CompletableFuture<>();
    this.send(new RoomMessage.TryPickItem(args, picker, future));
    try {
      GameItem item = future.orTimeout(1, TimeUnit.SECONDS).join();
      if (item != null) {
        return Optional.of(item);
      }
    } catch (Exception e) {
      log.error("Room tryPickItem 失敗 roomId:{}", id, e);
    }

    return Optional.empty();
  }

  public void broadcast(String actorId, String targetId, String message) {
    this.send(new RoomMessage.Broadcast(actorId, targetId, message));
  }

  public void broadcastJson(MudMessage<Object> message) {
    this.send(new RoomMessage.BroadcastJson(message));
  }

  public void broadcastToOthers(String actorId, String message) {
    this.send(new RoomMessage.BroadcastToOthers(actorId, message));
  }

  public Optional<Living> findLiving(String livingId) {
    if (livingId == null) return Optional.empty();
    return Stream.concat(players.stream(), mobs.stream())
        .filter(l -> l.isValid() && livingId.equals(l.getId()))
        .findFirst();
  }

  public List<Living> getLivings() {
    return Stream.concat(players.stream(), mobs.stream())
        .filter(Living::isValid)
        .toList();
  }

  public List<Player> getPlayers() {
    return players.stream().filter(Player::isValid).toList();
  }

  public List<Mob> getMobs() {
    return mobs.stream().filter(Mob::isValid).toList();
  }

  public List<GameItem> getItems() {
    return Collections.unmodifiableList(items);
  }

  public boolean hasFlag(com.example.htmlmud.domain.model.enums.RoomFlag flag) {
    if (flag == null) {
      return false;
    }
    String flagName = flag.name();
    if (dynamicFlags.stream().anyMatch(f -> f != null && (f.equalsIgnoreCase(flagName) || ("SAFE".equalsIgnoreCase(f) && flag == com.example.htmlmud.domain.model.enums.RoomFlag.SAFE_ZONE)))) {
      return true;
    }
    if (template == null || template.flags() == null) {
      return false;
    }
    return template.flags().stream()
        .anyMatch(f -> f.equalsIgnoreCase(flagName) || ("SAFE".equalsIgnoreCase(f) && flag == com.example.htmlmud.domain.model.enums.RoomFlag.SAFE_ZONE));
  }

  public boolean hasFlag(String flagStr) {
    if (flagStr == null) {
      return false;
    }
    if (dynamicFlags.stream().anyMatch(f -> f != null && f.equalsIgnoreCase(flagStr))) {
      return true;
    }
    if (template == null || template.flags() == null) {
      return false;
    }
    return template.flags().stream().anyMatch(f -> f.equalsIgnoreCase(flagStr));
  }

  public void record() {
    this.send(new RoomMessage.Record());
  }

  public CompletableFuture<Boolean> removePlayerAsync(String playerId) {
    if (playerId == null) {
      return CompletableFuture.completedFuture(false);
    }
    if (isActorThread()) {
      boolean removed = players.removeIf(player -> player.getId().equals(playerId));
      return CompletableFuture.completedFuture(removed);
    }
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    this.send(new RoomMessage.RemovePlayer(playerId, future));
    return future;
  }

  public void removePlayer(String playerId) {
    if (playerId == null) {
      return;
    }
    if (isActorThread()) {
      players.removeIf(player -> player.getId().equals(playerId));
      return;
    }
    this.send(new RoomMessage.RemovePlayer(playerId));
  }

  public CompletableFuture<Boolean> removeMobAsync(String mobId) {
    if (mobId == null) {
      return CompletableFuture.completedFuture(false);
    }
    if (isActorThread()) {
      boolean removed = mobs.removeIf(mob -> mob.getId().equals(mobId));
      return CompletableFuture.completedFuture(removed);
    }
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    this.send(new RoomMessage.RemoveMob(mobId, future));
    return future;
  }

  public void removeMob(String mobId) {
    if (mobId == null) {
      return;
    }
    if (isActorThread()) {
      mobs.removeIf(mob -> mob.getId().equals(mobId));
      return;
    }
    this.send(new RoomMessage.RemoveMob(mobId));
  }

  public CompletableFuture<Boolean> removeItemAsync(String itemId) {
    if (itemId == null) {
      return CompletableFuture.completedFuture(false);
    }
    if (isActorThread()) {
      boolean removed = items.removeIf(item -> item.getId().equals(itemId));
      if (removed) {
        roomService.record(this.getId(), items);
      }
      return CompletableFuture.completedFuture(removed);
    }
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    this.send(new RoomMessage.RemoveItem(itemId, future));
    return future;
  }

  public void removeItem(String itemId) {
    if (itemId == null) {
      return;
    }
    try {
      removeItemAsync(itemId).orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("Room removeItem 失敗 roomId:{}", id, e);
    }
  }

  public CompletableFuture<Boolean> dropItemAsync(GameItem item) {
    if (item == null) {
      return CompletableFuture.completedFuture(false);
    }
    if (isActorThread()) {
      boolean added = false;
      if (!items.contains(item)) {
        items.add(item);
        roomService.record(this.getId(), items);
        added = true;
      }
      return CompletableFuture.completedFuture(added);
    }
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    this.send(new RoomMessage.DropItem(item, future));
    return future;
  }

  public void dropItem(GameItem item) {
    if (item == null) {
      return;
    }
    try {
      dropItemAsync(item).orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("Room dropItem 失敗 roomId:{}", id, e);
    }
  }

  public String lookAtRoom(Player player) {
    if (isActorThread()) {
      return roomService.lookAtRoom(this, players, mobs, items, player.getId());
    }
    CompletableFuture<String> future = new CompletableFuture<>();
    this.send(new RoomMessage.LookAtRoom(player.getId(), future));
    try {
      return future.orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("Room lookAtRoom 失敗 roomId:{}", this.getId(), e);
      throw new MudException("這個房間發生空間破碎!!!");
    }
  }

  public String lookDirection(Player player, Direction dir) {
    if (isActorThread()) {
      return roomService.lookDirection(this, player, dir);
    }
    CompletableFuture<String> future = new CompletableFuture<>();
    this.send(new RoomMessage.LookDirection(player, dir, future));
    try {
      return future.orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("Room lookDirection 失敗 roomId:{}", this.getId(), e);
      throw new MudException("那個方向發生空間破碎!!!");
    }
  }

}
