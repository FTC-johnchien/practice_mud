package com.example.htmlmud.domain.actor.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.example.htmlmud.application.service.RoomService;
import com.example.htmlmud.domain.actor.core.VirtualActor; // 引用您的基礎類別
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.RoomStateRecord;
import com.example.htmlmud.domain.model.map.RoomTemplate;
import com.example.htmlmud.domain.model.map.ZoneTemplate;
import com.example.htmlmud.protocol.RoomMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
// 1. 繼承 VirtualActor，並指定泛型為 RoomMessage
public class RoomActor extends VirtualActor<RoomMessage> {

  @Getter
  private final String id;

  @Getter
  private final RoomTemplate template;

  @Getter
  private final ZoneTemplate zoneTemplate;

  private final RoomService roomService;


  // 房間內的玩家 (Runtime State)
  @Getter
  private final List<PlayerActor> players = new ArrayList<>();

  @Getter
  private final List<MobActor> mobs = new ArrayList<>();

  @Getter
  private final List<GameItem> items = new ArrayList<>(); // 地上的物品


  public RoomActor(RoomTemplate template, ZoneTemplate zoneTemplate, RoomService roomService) {
    super("room-" + template.id());
    this.roomService = roomService;

    this.id = template.id();
    this.template = template;
    this.zoneTemplate = zoneTemplate;

    roomService.spawnInitial(this);

  }

  // --- 實作父類別的抽象方法 ---
  @Override
  protected void handleMessage(RoomMessage msg) {
    // 這裡的邏輯跟之前一模一樣，但不需要自己寫 loop 和 try-catch 了
    switch (msg) {
      case RoomMessage.Enter(var actor, var future) -> {
        roomService.doEnter(this, actor, future);
      }
      case RoomMessage.Leave(var actorId) -> {
        roomService.doLeave(this, actorId);
      }
      // 交由 LookCommand 實作
      // case RoomMessage.Look(var playerId, var future) -> {
      // roomService.doLook(this, playerId, future);
      // }
      case RoomMessage.Say(var sourceId, var content) -> {
        roomService.doSay(this, sourceId, content);
      }
      case RoomMessage.TryPickItem(var args, var picker, var future) -> {
        roomService.doTryPickItem(this, args, picker, future);
      }
      case RoomMessage.Tick(var tickCount, var timestamp) -> {
        roomService.doTick(this, tickCount, timestamp);
      }
      case RoomMessage.Broadcast(var message) -> {
        roomService.doBroadcast(this, message);
      }
      case RoomMessage.BroadcastToOthers(var sourceId, var message) -> {
        roomService.doBroadcastToOthers(this, sourceId, message);
      }
      case RoomMessage.FindActor(var actorId, var future) -> {
        roomService.doFindActor(this, actorId, future);
      }
      case RoomMessage.GetPlayers(var future) -> {
        roomService.getPlayersSnapshot(this, future);
      }
      case RoomMessage.GetMobs(var future) -> {
        roomService.getMobsSnapshot(this, future);
      }
      case RoomMessage.GetItems(var future) -> {
        roomService.getItemsSnapshot(this, future);
      }
      case RoomMessage.ToRecord(var future) -> {
        roomService.toRecord(this, future);
      }

    }
  }
  // ---------------------------------------------------------------------------------------------



  // 公開給外部呼叫的方法 --------------------------------------------------------------------------
  public void enter(LivingActor actor, CompletableFuture<Void> future) {
    this.send(new RoomMessage.Enter(actor, future));
  }

  public void leave(String actorId) {
    this.send(new RoomMessage.Leave(actorId));
  }

  // public void look(String playerId, CompletableFuture<String> future) {
  // this.send(new RoomMessage.Look(playerId, future));
  // }

  public void say(String sourceId, String content) {
    this.send(new RoomMessage.Say(sourceId, content));
  }

  public void tick(long tickCount, long timestamp) {
    this.send(new RoomMessage.Tick(tickCount, timestamp));
  }

  public void tryPickItem(String args, PlayerActor picker, CompletableFuture<GameItem> future) {
    this.send(new RoomMessage.TryPickItem(args, picker, future));
  }

  public void broadcast(String message) {
    this.send(new RoomMessage.Broadcast(message));
  }

  public void broadcastToOthers(String actorId, String message) {
    this.send(new RoomMessage.BroadcastToOthers(actorId, message));
  }

  public void findActor(String actorId, CompletableFuture<LivingActor> future) {
    this.send(new RoomMessage.FindActor(actorId, future));
  }

  public void getPlayers(CompletableFuture<List<PlayerActor>> future) {
    this.send(new RoomMessage.GetPlayers(future));
  }

  public void getMobs(CompletableFuture<List<MobActor>> future) {
    this.send(new RoomMessage.GetMobs(future));
  }

  public void getItems(CompletableFuture<List<GameItem>> future) {
    this.send(new RoomMessage.GetItems(future));
  }

  public void toRecord(CompletableFuture<RoomStateRecord> future) {
    this.send(new RoomMessage.ToRecord(future));
  }

  // ---------------------------------------------------------------------------------------------



  // --- 物品操作邏輯 ---

  // --- 輔助方法 (保持不變) ---



  // ---------------------------------------------------------------------------------------------



  private void dropItem(GameItem item) {
    // 標記為 Dirty (需要存檔)
    item.setDirty(true);
    items.add(item);

    // WorldManager.markDirty(this.template.id());
  }

  private void removeItem(GameItem item) {
    items.remove(item);
    // 標記為 Dirty (需要存檔)
    // WorldManager.markDirty(this.template.id());
  }

}
