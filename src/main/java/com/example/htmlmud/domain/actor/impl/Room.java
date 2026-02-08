package com.example.htmlmud.domain.actor.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  // @Getter
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
        roomService.doEnter(this, players, mobs, actor, direction, future);
      }
      case RoomMessage.Leave(var actor, var direction) -> {
        roomService.doLeave(this, players, mobs, actor, direction);
      }
      case RoomMessage.Say(var sourceId, var content) -> {
        roomService.doSay(players, sourceId, content);
      }
      case RoomMessage.TryPickItem(var args, var picker, var future) -> {
        roomService.doTryPickItem(items, args, picker, future);
      }
      case RoomMessage.Tick(var tickCount, var timestamp) -> {
        roomService.doTick(this, players, mobs, tickCount, timestamp);
      }
      case RoomMessage.Broadcast(var sourceId, var targetId, var message) -> {
        roomService.doBroadcast(players, mobs, sourceId, targetId, message);
      }
      case RoomMessage.BroadcastToOthers(var sourceId, var message) -> {
        roomService.doBroadcastToOthers(players, sourceId, message);
      }
      case RoomMessage.FindLiving(var livingId, var future) -> {
        roomService.doFindLiving(players, mobs, livingId, future);
      }
      case RoomMessage.GetLivings(var future) -> {
        List<Living> livings = new ArrayList<>(players.size() + mobs.size());
        livings.addAll(players);
        livings.addAll(mobs);
        future.complete(livings);
      }
      case RoomMessage.RemoveLiving(var livingId) -> {
        players.removeIf(player -> player.getId().equals(livingId));
        mobs.removeIf(mob -> mob.getId().equals(livingId));
      }
      case RoomMessage.GetPlayers(var future) -> {
        future.complete(List.copyOf(players));
      }
      case RoomMessage.RemovePlayer(var playerId) -> {
        players.removeIf(player -> player.getId().equals(playerId));
      }
      case RoomMessage.GetMobs(var future) -> {
        future.complete(List.copyOf(mobs));
      }
      case RoomMessage.RemoveMob(var mobId) -> {
        mobs.removeIf(mob -> mob.getId().equals(mobId));
      }
      case RoomMessage.GetItems(var future) -> {
        future.complete(List.copyOf(items));
      }
      case RoomMessage.RemoveItem(var itemId) -> {
        items.removeIf(item -> item.getId().equals(itemId));
      }
      case RoomMessage.DropItem(var item) -> {
        items.add(item);
      }
      case RoomMessage.ToRecord(var future) -> {
        roomService.toRecord(this.getTemplate().id(), items, future);
      }

    }
  }
  // ---------------------------------------------------------------------------------------------



  // 公開給外部呼叫的方法 --------------------------------------------------------------------------
  public void enter(Living actor, Direction direction) {
    roomService.enter(this, actor, direction);
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
    return roomService.tryPickItem(this, args, picker);
  }

  public void broadcast(String actorId, String targetId, String message) {
    this.send(new RoomMessage.Broadcast(actorId, targetId, message));
  }

  public void broadcastToOthers(String actorId, String message) {
    this.send(new RoomMessage.BroadcastToOthers(actorId, message));
  }

  public Optional<Living> findLiving(String livingId) {
    return roomService.findLiving(this, livingId);
  }

  public List<Living> getLivings() {
    return roomService.getLivings(this);
  }

  public List<Player> getPlayers() {
    return roomService.getPlayers(this);
  }

  public List<Mob> getMobs() {
    return roomService.getMobs(this);
  }

  public List<GameItem> getItems() {
    return roomService.getItems(this);
  }

  public void record() {
    roomService.record(this);
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



  // ---------------------------------------------------------------------------------------------



  // --- 物品操作邏輯 ---

  // --- 輔助方法 (保持不變) ---



  // ---------------------------------------------------------------------------------------------



  private void removeItem(GameItem item) {
    items.remove(item);
    // 標記為 Dirty (需要存檔)
    // WorldManager.markDirty(this.template.id());
  }

}
