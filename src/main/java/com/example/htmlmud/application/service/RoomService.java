package com.example.htmlmud.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.command.parser.TargetSelector;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.domain.actor.impl.LivingActor;
import com.example.htmlmud.domain.actor.impl.MobActor;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.domain.actor.impl.RoomActor;
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.RoomStateRecord;
import com.example.htmlmud.domain.model.map.SpawnRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

  @Getter
  private final ObjectMapper objectMapper;

  @Getter
  private final TargetSelector targetSelector;

  @Getter
  private final WorldFactory worldFactory;

  public void doEnter(RoomActor roomActor, LivingActor actor, CompletableFuture<Void> future) {
    try {
      enter(roomActor, actor);
      future.complete(null);
    } catch (Exception e) {
      log.error("doEnter error", e);
      future.completeExceptionally(e);
    }
  }

  public void doLeave(RoomActor roomActor, String actorId) {
    LivingActor actor = findActor(roomActor, actorId);
    if (actor == null) {
      return;
    }

    switch (actor) {
      case PlayerActor player -> roomActor.getPlayers().remove(player);
      case MobActor mob -> roomActor.getMobs().remove(mob);
    }

    doBroadcast(roomActor, actor.getName() + " 離開了。");
  }

  @Deprecated
  public void doLook(RoomActor roomActor, String playerId, CompletableFuture<String> future) {
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

  public void doSay(RoomActor roomActor, String sourceId, String content) {
    PlayerActor speaker = roomActor.getPlayers().stream().filter(p -> p.getId().equals(sourceId))
        .findFirst().orElse(null);
    String name = (speaker != null) ? speaker.getName() : "有人";
    doBroadcast(roomActor, name + ": " + content);
  }

  public void doTryPickItem(RoomActor roomActor, String args, PlayerActor picker,
      CompletableFuture<GameItem> future) {
    try {
      GameItem item = tryPickItem(roomActor, args, picker);
      future.complete(item);
    } catch (Exception e) {
      log.error("doTryPickItem error", e);
      future.completeExceptionally(e);
      // future.completeExceptionally(new MudException("你的背包太重了，撿不起來！"));
    }
  }

  // 處理邏輯
  public void doTick(RoomActor roomActor, long tickCount, long timestamp) {
    // log.info("{} tickCount: {}", id, tickCount);

    // === 1. World/Zone 層級邏輯 (例如：每 60 秒檢查一次重生) ===
    if (tickCount % roomActor.getZoneTemplate().respawnRate() == 0) {
      checkSpawnRule(); // 檢查是否有怪物死掉很久該重生了
    }

    // === 2. 轉發給 Actor ===
    // 過濾掉 "完全沒事做且沒玩家在場" 的怪物
    if (!roomActor.getPlayers().isEmpty()
        || roomActor.getMobs().stream().anyMatch(m -> m.getState().isInCombat())) {
      // ActorMessage.Tick msg = new ActorMessage.Tick(tickCount, timestamp);

      for (MobActor mob : roomActor.getMobs()) {
        mob.tick(tickCount, timestamp);
      }

      for (PlayerActor player : roomActor.getPlayers()) {
        // log.info("send player");
        player.tick(tickCount, timestamp);
      }
    }
  }

  public void doBroadcast(RoomActor roomActor, String message) {
    roomActor.getPlayers().forEach(p -> p.reply(message));
  }

  public void doBroadcastToOthers(RoomActor roomActor, String actorId, String message) {
    roomActor.getPlayers().stream().filter(p -> !p.getId().equals(actorId))
        .forEach(p -> p.reply(message));
  }

  public void doFindActor(RoomActor roomActor, String actorId,
      CompletableFuture<LivingActor> future) {
    try {
      LivingActor actor = findActor(roomActor, actorId);
      if (actor == null) {
        future.complete(null);
        return;
      }
      future.complete(actor);
    } catch (Exception e) {
      log.error("doFindActor error", e);
      future.completeExceptionally(e);
    }
  }

  // 產生快照
  public void getPlayersSnapshot(RoomActor roomActor, CompletableFuture<List<PlayerActor>> future) {
    try {
      // 排序規則：name
      List<PlayerActor> sortedPlayers = roomActor.getPlayers().stream()
          .sorted(Comparator.comparing(PlayerActor::getName)).toList();
      future.complete(sortedPlayers);
    } catch (Exception e) {
      log.error("getPlayersSnapshot error", e);
      future.completeExceptionally(e);
    }
  }

  /**
   * 獲取有序快照 排序規則：先根據進入時間 (老鳥在前)，如果時間一樣，再比對 ID
   */
  public void getMobsSnapshot(RoomActor roomActor, CompletableFuture<List<MobActor>> future) {
    try {
      // 排序規則：名稱 -> 進入時間(ArrayList有序，不用這段) -> ID
      List<MobActor> sortedMobs =
          roomActor
              .getMobs().stream().sorted(Comparator
                  .comparing((MobActor m) -> m.getTemplate().name()).thenComparing(MobActor::getId))
              .toList();
      future.complete(sortedMobs);
    } catch (Exception e) {
      log.error("getMobsSnapshot error", e);
      future.completeExceptionally(e);
    }
  }

  public void getItemsSnapshot(RoomActor roomActor, CompletableFuture<List<GameItem>> future) {
    try {
      // 排序規則：名稱 -> 進入時間(ArrayList有序，不用這段) -> ID
      List<GameItem> sortedItems =
          roomActor
              .getItems().stream().sorted(Comparator
                  .comparing((GameItem i) -> i.getTemplate().name()).thenComparing(GameItem::getId))
              .toList();
      future.complete(sortedItems);
    } catch (Exception e) {
      log.error("getItemsSnapshot error", e);
      future.completeExceptionally(e);
    }
  }

  /**
   * 房間初次載入時的生怪邏輯
   */
  public void spawnInitial(RoomActor roomActor) {
    List<SpawnRule> mobSpawnRules = roomActor.getTemplate().mobSpawnRules();
    List<SpawnRule> itemSpawnRules = roomActor.getTemplate().itemSpawnRules();

    if (mobSpawnRules != null && !mobSpawnRules.isEmpty()) {
      for (SpawnRule rule : mobSpawnRules) {
        // 處理機率 (例如：稀有怪只有 10% 機率出現)
        if (Math.random() > rule.respawnChance()) {
          continue;
        }

        // 根據數量生成
        for (int i = 0; i < rule.count(); i++) {
          spawnOneMob(roomActor, rule);
        }
      }
    }

    if (itemSpawnRules != null && !itemSpawnRules.isEmpty()) {
      for (SpawnRule rule : itemSpawnRules) {
        // 處理機率 (例如：稀有怪只有 10% 機率出現)
        if (Math.random() > rule.respawnChance()) {
          continue;
        }

        // 根據數量生成
        for (int i = 0; i < rule.count(); i++) {
          spawnOneItem(roomActor, rule);
        }
      }
    }
  }

  public void dropItem(RoomActor roomActor, LivingActor actor, GameItem item,
      CompletableFuture<String> future) {

  }

  public void toRecord(RoomActor roomActor, CompletableFuture<RoomStateRecord> future) {
    try {
      RoomStateRecord record = toRecord(roomActor);
      future.complete(record);
    } catch (Exception e) {
      log.error("toRecord error", e);
      future.completeExceptionally(e);
    }
  }



  // ---------------------------------------------------------------------------------------------



  private void checkSpawnRule() {

  }

  private void spawnOneMob(RoomActor roomActor, SpawnRule rule) {
    // 1. 呼叫工廠產生 MobActor (這裡會給予 UUID)
    MobActor mob = worldFactory.createMob(rule.id());
    try {
      log.info("{}: {} - {}", mob.getId(), mob.getName(),
          objectMapper.writeValueAsString(mob.getTemplate()));
    } catch (Exception e) {
      e.printStackTrace();
    }

    // // 2. 設定位置
    mob.setCurrentRoomId(roomActor.getId());

    // // 3. 加入房間列表
    roomActor.getMobs().add(mob);

    // // 4. 啟動怪物的 AI
    mob.start();

    log.info("Spawned {} in room {}", mob.getTemplate().name(), roomActor.getId());
  }

  private void spawnOneItem(RoomActor roomActor, SpawnRule rule) {
    GameItem item = worldFactory.createItem(rule.id());
    if (item == null) {
      return;
    }

    roomActor.getItems().add(item);
    log.info("Spawned {} in room {}", item.getDisplayName(), roomActor.getId());
  }



  private boolean enter(RoomActor roomActor, LivingActor actor) {
    switch (actor) {
      case PlayerActor player -> {
        if (!roomActor.getPlayers().contains(player)) {
          roomActor.getPlayers().add(player);
        }
      }
      case MobActor mob -> {
        if (!roomActor.getMobs().contains(mob)) {
          roomActor.getMobs().add(mob);
        }
      }
    }

    actor.setCurrentRoomId(roomActor.getId());
    doBroadcastToOthers(roomActor, actor.getId(), actor.getName() + " 走了進來。");
    return true;
  }

  private GameItem tryPickItem(RoomActor roomActor, String args, PlayerActor picker) {
    log.info("tryPickItem: {}", args);

    GameItem targetItem = targetSelector.selectItem(roomActor.getItems(), args);
    if (targetItem == null) {
      return null;
    }

    roomActor.getItems().remove(targetItem);

    return targetItem;
  }

  private LivingActor findActor(RoomActor roomActor, String actorId) {

    // 先檢查mob
    for (MobActor mob : roomActor.getMobs()) {
      if (mob.getId().equals(actorId)) {
        return mob;
      }
    }

    for (PlayerActor player : roomActor.getPlayers()) {
      if (player.getId().equals(actorId)) {
        return player;
      }
    }

    return null;
  }

  private RoomStateRecord toRecord(RoomActor roomActor) {
    String[] args = roomActor.getTemplate().id().split(":");
    String zoneId = args[0];
    String roomId = args[1];
    return new RoomStateRecord(roomId, zoneId, new ArrayList<>(roomActor.getItems()));
  }
}
