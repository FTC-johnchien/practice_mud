package com.example.htmlmud.domain.actor.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import com.example.htmlmud.domain.actor.core.VirtualActor; // 引用您的基礎類別
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.enums.Direction;
import com.example.htmlmud.domain.model.template.RoomTemplate;
import com.example.htmlmud.domain.model.template.ZoneTemplate;
import com.example.htmlmud.domain.service.RoomService;
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

  // 房間內的玩家 (Runtime State)
  private final List<Player> players = new ArrayList<>();

  private final List<Mob> mobs = new ArrayList<>();

  private final List<GameItem> items = new ArrayList<>(); // 地上的物品

  // 戶籍名冊：記錄這個房間生出來且還活著的怪物 ID
  // Key: TemplateID (ex: "snow_guard"), Value: Set of Instance UUIDs
  private Map<String, Set<String>> trackedMobs = new HashMap<>();


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
        future.complete(roomService.tryPickItem(items, args, picker));
      }
      case RoomMessage.Tick(var tickCount, var timestamp) -> {
        roomService.tick(this, tickCount, timestamp);
      }
      case RoomMessage.Broadcast(var sourceId, var targetId, var message) -> {
        roomService.broadcast(players, mobs, sourceId, targetId, message);
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
      case RoomMessage.RemoveLiving(var livingId) -> {
        players.removeIf(player -> player.getId().equals(livingId));
        mobs.removeIf(mob -> mob.getId().equals(livingId));
      }
      case RoomMessage.GetPlayers(var future) -> {
        future.complete(players.stream().filter(Player::isValid).toList());
      }
      case RoomMessage.RemovePlayer(var playerId) -> {
        players.removeIf(player -> player.getId().equals(playerId));
      }
      case RoomMessage.GetMobs(var future) -> {
        future.complete(mobs.stream().filter(Mob::isValid).toList());
      }
      case RoomMessage.RemoveMob(var mobId) -> {
        mobs.removeIf(mob -> mob.getId().equals(mobId));
      }
      case RoomMessage.RemoveItem(var itemId) -> {
        items.removeIf(item -> item.getId().equals(itemId));
        // 標記為 Dirty (需要存檔)
        // WorldManager.markDirty(this.template.id());
      }
      case RoomMessage.DropItem(var item) -> {
        items.add(item);
      }
      case RoomMessage.Record() -> {
        roomService.record(this.getTemplate().id(), items);
      }
      case RoomMessage.LookAtRoom(var playerId, var future) -> {
        future.complete(roomService.handleLookAtRoom(this, players, mobs, items, playerId));
      }

    }
  }



  // ---------------------------------------------------------------------------------------------



  // 公開給外部呼叫的方法 --------------------------------------------------------------------------



  public void enter(Living actor, Direction direction) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    this.send(new RoomMessage.Enter(actor, direction, future));
    try {
      future.orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("Room enter 失敗 roomId:{}", id, e);
      actor.reply("一股未知的力量阻擋了$N的前進!");
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

  public void broadcastToOthers(String actorId, String message) {
    this.send(new RoomMessage.BroadcastToOthers(actorId, message));
  }

  public Optional<Living> findLiving(String livingId) {
    CompletableFuture<Living> future = new CompletableFuture<>();
    this.send(new RoomMessage.FindLiving(livingId, future));
    try {
      Living living = future.orTimeout(1, TimeUnit.SECONDS).join();
      if (living != null) {
        return Optional.of(living);
      }
    } catch (Exception e) {
      log.error("Room 取得 Living 列表失敗 roomId:{}", id, e);
    }

    return Optional.empty();
  }

  public List<Living> getLivings() {
    CompletableFuture<List<Living>> future = new CompletableFuture<>();
    this.send(new RoomMessage.GetLivings(future));
    try {
      return future.orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("Room 取得 Living 列表失敗 roomId:{}", id, e);
    }
    return new ArrayList<>();
  }

  public List<Player> getPlayers() {
    CompletableFuture<List<Player>> future = new CompletableFuture<>();
    this.send(new RoomMessage.GetPlayers(future));
    try {
      return future.orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("Room 取得 Player 列表失敗 roomId:{}", id, e);
    }
    return new ArrayList<>();
  }

  public List<Mob> getMobs() {
    CompletableFuture<List<Mob>> future = new CompletableFuture<>();
    this.send(new RoomMessage.GetMobs(future));
    try {
      return future.orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("Room 取得 Mob 列表失敗 roomId:{}", id, e);
    }
    return new ArrayList<>();
  }

  public void record() {
    this.send(new RoomMessage.Record());
  }

  public void removeLiving(String livingId) {
    this.send(new RoomMessage.RemoveLiving(livingId));
  }

  public void removePlayer(String playerId) {
    this.send(new RoomMessage.RemovePlayer(playerId));
  }

  public void removeMob(String mobId) {
    this.send(new RoomMessage.RemoveMob(mobId));
  }

  public void removeItem(String itemId) {
    this.send(new RoomMessage.RemoveItem(itemId));
  }

  public void dropItem(GameItem item) {
    this.send(new RoomMessage.DropItem(item));
  }

  public void lookAtRoom(Player player) {
    CompletableFuture<String> future = new CompletableFuture<>();
    this.send(new RoomMessage.LookAtRoom(player.getId(), future));
    try {
      String roomDesc = future.orTimeout(1, TimeUnit.SECONDS).join();
      player.reply(roomDesc);
    } catch (Exception e) {
      log.error("Room lookAtRoom 失敗 roomId:{}", this.getId(), e);
    }
  }

  public void lookDirection(Player player, Direction dir) {

  }

  public void lookAtSelf(Player player) {

  }

  public void lookAtTarget(Player player, String targetName) {

  }

}
