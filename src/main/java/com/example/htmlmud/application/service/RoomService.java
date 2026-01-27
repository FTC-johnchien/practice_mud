package com.example.htmlmud.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.command.parser.TargetSelector;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.domain.actor.impl.LivingActor;
import com.example.htmlmud.domain.actor.impl.MobActor;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.domain.actor.impl.RoomActor;
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.Direction;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.RoomStateRecord;
import com.example.htmlmud.domain.model.map.SpawnRule;
import com.example.htmlmud.infra.util.AnsiColor;
import com.example.htmlmud.infra.util.ColorText;
import com.example.htmlmud.infra.util.MessageFormatter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

  private final ObjectMapper objectMapper;

  private final TargetSelector targetSelector;

  private final WorldFactory worldFactory;

  public void doEnter(RoomActor self, LivingActor actor, Direction direction,
      CompletableFuture<Void> future) {
    // enter
    enter(self, actor, direction);

    // 當actor為玩家時，要將room里的mobs丟出playerEnter的message
    switch (actor) {
      case PlayerActor player -> {
        self.getMobs().forEach(m -> m.onPlayerEnter(player.getId()));
      }
      // case MobActor mob -> {}
      default -> {
      }
    }
    future.complete(null);
  }

  public void doLeave(RoomActor self, LivingActor actor, Direction direction) {
    String livingName = null;
    switch (actor) {
      case PlayerActor player -> {
        self.getPlayers().remove(player);
        livingName = player.getNickname();
      }
      case MobActor mob -> {
        self.getMobs().remove(mob);
        livingName = mob.getName();
      }
    }

    String leaveMsg =
        ColorText.wrap(AnsiColor.YELLOW, livingName + " 往 " + direction.getDisplayName() + " 離開了。");

    doBroadcast(self, leaveMsg);
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

    doBroadcastToOthers(roomActor, sourceId, name + ": " + content);
    // doBroadcast(roomActor, name + ": " + content);
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
    if (tickCount % roomActor.getZoneTemplate().respawnTime() == 0) {
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

  public void doCombatBroadcast(RoomActor self, LivingActor source, LivingActor target,
      String messageTemplate) {
    if (source == null) {
      throw new MudException("source is null");
    }
    if (target == null) {
      throw new MudException("target is null");
    }

    // 1. 取得房間內所有人 (包含做動作的人自己)
    List<LivingActor> audiences = new ArrayList<>();
    audiences.addAll(self.getPlayers());
    // audiences.addAll(self.getMobs()); // 怪物通常不需要收訊息，除非有 AI 反應

    // 2. 對每個人發送「客製化」的訊息
    for (LivingActor receiver : audiences) {
      String finalMsg = MessageFormatter.format(messageTemplate, source, target, receiver);
      receiver.reply(finalMsg);
    }
  }

  public void doFindActor(RoomActor self, String actorId, CompletableFuture<LivingActor> future) {
    Optional<LivingActor> opt = findActor(self, actorId);
    if (opt.isEmpty()) {
      future.completeExceptionally(
          new MudException("ActorId:" + actorId + " not found in roomId:" + self.getId()));
      return;
    }

    future.complete(opt.get());
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
    List<SpawnRule> spawnRules = roomActor.getTemplate().spawnRules();
    if (spawnRules != null && !spawnRules.isEmpty()) {
      for (SpawnRule rule : spawnRules) {
        for (int i = 0; i < rule.count(); i++) {
          switch (rule.type()) {
            case "MOB" -> spawnOneMob(roomActor, rule);
            case "ITEM" -> spawnOneItem(roomActor, rule);
          }
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

    // 處理機率 (例如：稀有怪只有 10% 機率出現)
    if (Math.random() > rule.rate()) {
      return;
    }

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
    // 處理機率 (例如：稀有怪只有 10% 機率出現)
    if (Math.random() > rule.rate()) {
      return;
    }

    GameItem item = worldFactory.createItem(rule.id());
    if (item == null) {
      return;
    }

    roomActor.getItems().add(item);
    log.info("Spawned {} in room {}", item.getDisplayName(), roomActor.getId());
  }



  private void enter(RoomActor self, LivingActor actor, Direction direction) {
    String livingName = null;
    switch (actor) {
      case PlayerActor player -> {
        livingName = player.getNickname();
        if (!self.getPlayers().contains(player)) {
          self.getPlayers().add(player);
        }
      }
      case MobActor mob -> {
        livingName = mob.getName();
        if (!self.getMobs().contains(mob)) {
          self.getMobs().add(mob);
        }
      }
    }

    actor.setCurrentRoomId(self.getId());

    String arriveMsg =
        ColorText.wrap(AnsiColor.YELLOW, livingName + " 從 " + direction.getDisplayName() + " 過來了。");
    doBroadcastToOthers(self, actor.getId(), arriveMsg);
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

  private Optional<LivingActor> findActor(RoomActor self, String actorId) {

    // 先檢查mob
    for (MobActor mob : self.getMobs()) {
      if (mob.getId().equals(actorId)) {
        return Optional.of(mob);
      }
    }

    for (PlayerActor player : self.getPlayers()) {
      if (player.getId().equals(actorId)) {
        return Optional.of(player);
      }
    }

    log.warn("ActorId:" + actorId + " not found in roomId:" + self.getId());
    return Optional.empty();
  }

  private RoomStateRecord toRecord(RoomActor roomActor) {
    String[] args = roomActor.getTemplate().id().split(":");
    String zoneId = args[0];
    String roomId = args[1];
    return new RoomStateRecord(roomId, zoneId, new ArrayList<>(roomActor.getItems()));
  }
}
