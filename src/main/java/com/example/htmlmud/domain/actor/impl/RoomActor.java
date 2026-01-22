package com.example.htmlmud.domain.actor.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.domain.actor.LivingActor;
import com.example.htmlmud.domain.actor.core.VirtualActor; // 引用您的基礎類別
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.RoomStateRecord;
import com.example.htmlmud.domain.model.map.RoomTemplate;
import com.example.htmlmud.domain.model.map.SpawnRule;
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

  private final List<SpawnRule> mobSpawnRules;

  private final List<SpawnRule> itemSpawnRules;

  private final WorldFactory worldFactory;


  // 房間內的玩家 (Runtime State)
  @Getter
  private final List<PlayerActor> players = new ArrayList<>();

  @Getter
  private final List<MobActor> mobs = new ArrayList<>();

  @Getter
  private final Set<GameItem> items = ConcurrentHashMap.newKeySet(); // 地上的物品


  public RoomActor(RoomTemplate template, ZoneTemplate zoneTemplate, WorldFactory worldFactory) {
    super("room-" + template.id());
    this.id = template.id();
    this.template = template;
    this.zoneTemplate = zoneTemplate;
    // 從 Template 複製規則 (因為這是固定的)
    this.mobSpawnRules = template.mobSpawnRules();
    this.itemSpawnRules = template.itemSpawnRules();
    this.worldFactory = worldFactory;

    // 初始生怪
    spawnInitial("mob", mobSpawnRules);

    // 初始物品
    spawnInitial("item", itemSpawnRules);

  }

  // --- 實作父類別的抽象方法 ---

  @Override
  protected void handleMessage(RoomMessage msg) {
    // 這裡的邏輯跟之前一模一樣，但不需要自己寫 loop 和 try-catch 了
    switch (msg) {
      case RoomMessage.PlayerEnter(var player, var future) -> {
        log.info("Player {} entered room", player.getId());
        player.markEnterRoom();
        if (!players.contains(player)) {
          players.add(player);
        }
        broadcastToOthers(player.getId(), "看到 " + player.getNickname() + " 走了進來。");
        log.debug("Player {} entered room {}", player.getNickname(), template.id());
        if (future != null)
          future.complete(null);
      }

      case RoomMessage.PlayerLeave(var playerId) -> {
        players.stream().filter(p -> p.getId().equals(playerId)).findFirst().ifPresent(p -> {
          if (players.remove(p))
            broadcastToOthers(playerId, p.getNickname() + " 離開了。");
        });
      }

      case RoomMessage.Look(var playerId, var future) -> {
        log.info("Player {} Look room", playerId);
        StringBuilder sb = new StringBuilder();
        sb.append("\u001B[1;36m").append(template.name()).append("\u001B[0m\r\n");
        sb.append(template.description()).append("\r\n");

        if (template.exits() != null && !template.exits().isEmpty()) {
          sb.append("\u001B[33m[出口]: ").append(String.join(", ", template.exits().keySet()))
              .append("\u001B[0m\r\n");
        }

        StringBuilder others = new StringBuilder();
        players.stream().filter(p -> !p.getId().equals(playerId))
            .forEach(p -> others.append(p.getNickname()).append(" "));

        if (!others.isEmpty()) {
          sb.append("\u001B[35m[這裡有]: \u001B[0m").append(others).append("\r\n");
        }
        future.complete(sb.toString());
      }

      case RoomMessage.Say(var sourceId, var content) -> {
        PlayerActor speaker =
            players.stream().filter(p -> p.getId().equals(sourceId)).findFirst().orElse(null);
        String name = (speaker != null) ? speaker.getNickname() : "有人";
        broadcast(name + ": " + content);
      }

      case RoomMessage.TryPickItem(var itemId, var picker) -> {
        // 【關鍵併發控制】
        // var item = items.stream().filter(i -> i.id.equals(itemId)).findFirst();
        // if (item.isPresent()) {
        // items.remove(item.get());
        // 回覆給 Command: 成功
        // picker.send(new ActorMessage("...", new InternalCommand.PickSuccess(item.get())));
        // } else {
        // 回覆給 Command: 失敗
        // picker.send(new ActorMessage("...", new InternalCommand.PickFail("東西不在了")));
        // }
      }
    }
  }

  // --- 物品操作邏輯 ---

  // public Optional<Item> pickItem(String keyword) {
  // 簡單搜尋邏輯
  // var found = items.stream().filter(i -> isMatch(i, keyword)) // 需實作 isMatch
  // .findFirst();

  // found.ifPresent(items::remove);
  // return found; // 這裡回傳後，記得要標記 Dirty
  // }

  // --- 輔助方法 (保持不變) ---

  public void broadcast(String message) {
    players.forEach(p -> p.sendText(message));
  }

  private void broadcastToOthers(String sourceId, String message) {
    players.stream().filter(p -> !p.getId().equals(sourceId)).forEach(p -> p.sendText(message));
  }

  // 產生快照 (只存變動的部分)
  public RoomStateRecord toRecord() {
    String[] args = template.id().split(":");
    String zoneId = args[0];
    String roomId = args[1];
    return new RoomStateRecord(roomId, zoneId, new ArrayList<>(items));
  }

  public void addPlayer(PlayerActor player) {
    player.markEnterRoom();
    players.add(player);
  }

  public void removePlayer(PlayerActor player) {
    players.remove(player);
  }

  public List<PlayerActor> getPlayersSnapshot() {
    List<PlayerActor> players = new ArrayList<>(this.players);
    players.sort(Comparator.comparing(PlayerActor::getNickname));
    return players;
  }

  /**
   * 房間的週期性邏輯更新
   */
  public void tick() {
    // 1. 驅動房間內的 Mobs
    for (MobActor mob : mobs) {
      mob.tick();
    }

    // 2. 驅動房間內的 Players (如果需要自動回血/DoT)
    for (PlayerActor player : players) {
      player.tick();
    }

    // 3. 處理房間本身的邏輯 (例如生怪)
    checkSpawn();

    // 未來可以在這裡處理：怪物重生計時、物品腐爛、環境效果等
  }

  public void dropItem(GameItem item) {
    items.add(item);
    // 標記為 Dirty (需要存檔)
    // WorldManager.markDirty(this.template.id());
  }

  public void removeItem(GameItem item) {
    items.remove(item);
    // 標記為 Dirty (需要存檔)
    // WorldManager.markDirty(this.template.id());
  }

  public List<GameItem> getItemsSnapshot() {
    return new ArrayList<>(items);
  }



  /**
   * 處理怪物進入
   */
  public void addMob(MobActor mob) {
    // 標記時間
    mob.markEnterRoom();
    if (!mobs.contains(mob)) {
      mobs.add(mob);
    }
  }

  /**
   * 尋找怪物
   */
  public MobActor findMob(String mobId) {
    return mobs.stream().filter(m -> m.getId().equals(mobId)).findFirst().orElse(null);
  }

  /**
   * 處理怪物離開
   */
  public void removeMob(MobActor mob) {
    mobs.remove(mob);
  }

  /**
   * 獲取有序快照 排序規則：先根據進入時間 (老鳥在前)，如果時間一樣，再比對 ID
   */
  public List<MobActor> getMobsSnapshot() {
    // 1. 複製 (O(N))
    List<MobActor> snapshot = new ArrayList<>(this.mobs);

    // 2. 排序 (O(N log N))
    // 排序規則：名稱 -> 進入時間 -> ID
    snapshot.sort(Comparator.comparing((MobActor m) -> m.getTemplate().name())
        .thenComparingLong(MobActor::getLastEnterRoomTime).thenComparing(MobActor::getId));

    return snapshot;
  }

  private void checkSpawn() {

  }

  /**
   * 房間初次載入時的生怪邏輯
   */
  private void spawnInitial(String type, List<SpawnRule> rules) {
    if (rules == null)
      return;

    for (SpawnRule rule : rules) {
      // 處理機率 (例如：稀有怪只有 10% 機率出現)
      if (Math.random() > rule.respawnChance()) {
        continue;
      }

      // 根據數量生成
      for (int i = 0; i < rule.count(); i++) {
        switch (type) {
          case "mob":
            spawnOneMob(rule);
            break;
          case "item":
            spawnOneItem(rule);
            break;
        }
      }
    }
  }

  private void spawnOneMob(SpawnRule rule) {
    // 1. 呼叫工廠產生 MobActor (這裡會給予 UUID)
    MobActor mob = worldFactory.createMob(rule.id());

    // // 2. 設定位置
    mob.setCurrentRoomId(this.id);

    // // 3. 加入房間列表
    this.mobs.add(mob);

    // // 4. 啟動怪物的 AI
    mob.start();

    log.debug("Spawned {} in room {}", mob.getTemplate().name(), this.id);
  }

  private void spawnOneItem(SpawnRule rule) {
    GameItem item = worldFactory.createItem(rule.id());

  }

  public LivingActor findActor(String actorId) {

    // 先檢查mob
    for (MobActor mob : mobs) {
      if (mob.getId().equals(actorId)) {
        return mob;
      }
    }

    for (PlayerActor player : players) {
      if (player.getId().equals(actorId)) {
        return player;
      }
    }

    return null;
  }
}
