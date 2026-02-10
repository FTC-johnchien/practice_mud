package com.example.htmlmud.domain.actor.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.MDC;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.actor.behavior.GuestBehavior;
import com.example.htmlmud.domain.actor.behavior.PlayerBehavior;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.entity.PlayerRecord;
import com.example.htmlmud.domain.model.enums.Direction;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.WorldManager;
import com.example.htmlmud.protocol.ActorMessage;
import com.example.htmlmud.protocol.ConnectionState;
import com.example.htmlmud.protocol.GameCommand;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

// PlayerActor 處理的訊息類型就是 GameCommand
@Getter
@Slf4j
public final class Player extends Living {

  @Setter
  private WebSocketSession session;

  private final PlayerService service;

  private final WorldManager manager;

  // Actor 內部狀態 (State Machine Context)
  @Setter
  private ConnectionState connectionState = ConnectionState.CONNECTED;



  // 用來比對「這是哪一次的斷線」，防止舊的計時器殺錯人
  @Setter
  private volatile long lastDisconnectTime = 0;

  @Setter
  private String nickname;

  @Setter
  private String lookDescription;

  // 當前的行為腦
  @Setter
  private PlayerBehavior currentBehavior;



  private Player(WebSocketSession session, String id, String name, LivingStats state,
      WorldManager worldManager, PlayerService playerService) {
    super(id, name, state, playerService.getLivingServiceProvider().getObject());
    this.session = session;
    this.service = playerService;
    this.manager = worldManager;
  }

  // 工廠方法 初始設定為 GuestBehavior
  public static Player createGuest(WebSocketSession session, WorldManager worldManager,
      PlayerService playerService) {
    String name = "GUEST";
    String uuid = UUID.randomUUID().toString();
    String playerId = "p-" + uuid.substring(0, 8) + uuid.substring(9, 11);
    Player actor =
        new Player(session, playerId, name, new LivingStats(), worldManager, playerService);
    playerService.become(actor, new GuestBehavior(playerService.getAuthService(), worldManager));
    return actor;
  }

  @Override
  public void start() {
    super.start();
    // 連線歡迎詞
    reply("歡迎光臨 Html Mud 世界！");
    reply("請輸入 帳號 進行登入 或 輸入 'new' 進行註冊：");
  }

  @Override
  protected void handleMessage(ActorMessage msg) {
    try {
      switch (msg) {
        // 1. 先攔截我專屬的訊息
        case ActorMessage.PlayerMessage playerMsg -> handlePlayerMessage(playerMsg);

        // 2. 其他的 (LivingMessage 或 MobMessage?) 丟給父類別處理
        // 父類別會處理 LivingMessage，並忽略 MobMessage
        default -> super.handleMessage(msg);
      }
    } catch (Exception e) {
      reply(e.getMessage());
    }
  }

  private void handlePlayerMessage(ActorMessage.PlayerMessage msg) {
    switch (msg) {
      case ActorMessage.Command(var traceId, var cmd) -> {
        service.handleInput(this, traceId, cmd);
      }
      case ActorMessage.SendText(var session, var content) -> {
        service.handleSendText(this, session, content);
      }
      case ActorMessage.Reconnect(var session) -> {
        service.handleReconnect(this, session);
      }
      case ActorMessage.Disconnect() -> {
        service.handleDisconnect(this);
      }



      case ActorMessage.SaveData() -> {
        save();
      }
      case ActorMessage.GainExp(var amount) -> {
        // TODO
        this.stats.exp += amount;
        // if (this.stats.exp >= this.stats.nextLevelExp) {
        // this.levelUp();
        // }
      }
      case ActorMessage.QuestUpdate(var questId, var status) -> {
        // TODO
      }


      default -> log.warn("handlePlayerMessage 收到無法處理的訊息: {} {}", this.id, msg);
    }
  }

  // 觸發存檔的輔助方法
  private void save() {
    service.getPlayerPersistenceService().saveAsync(this.toRecord());
  }



  // @Override
  // protected void doTick(long tickCount, long time) {
  // log.info("player tickCount: {}", tickCount);
  // super.doTick(tickCount, time);
  // }

  @Override
  protected void handleOnDeath(String killerId) {
    reply("$N已經死亡！即將在重生點復活...");
    super.handleOnDeath(killerId);


    // 玩家死亡邏輯：掉經驗、傳送回城
  }



  // ---------------------------------------------------------------------------------------------



  public void addToInventory(GameItem newItem) {
    // 1. 如果是可堆疊的
    if (newItem.isStackable()) {
      // 找找看背包裡有沒有一樣的物品 (比對 TemplateID)
      Optional<GameItem> existing = this.inventory.stream()
          .filter(i -> i.getTemplate().id().equals(newItem.getTemplate().id())).findFirst();

      if (existing.isPresent()) {
        // 2. 找到了：直接加數量 (不會產生新 Record)
        GameItem stack = existing.get();
        stack.setAmount(stack.getAmount() + newItem.getAmount());

        // newItem 物件可以丟棄了
        return;
      }
    }

    // 3. 不可堆疊，或是背包還沒有：直接加入清單
    this.inventory.add(newItem);
  }

  private void levelUp() {
    // 從 Context 取得當前 Trace ID
    String currentTraceId = MudContext.traceId();

    // 發送事件
    // publisher.publishEvent(
    // new DomainEvent.PlayerLevelUpEvent(currentTraceId, this.objectId.id(), this.stats.level));
  }



  private PlayerRecord toRecord() {
    // 您必須確保 LivingState 有實作 deepCopy，否則會發生併發修改例外
    return new PlayerRecord(this.id, // ID
        this.name, // Username
        this.nickname, // Nickname
        this.currentRoomId, // Room
        this.stats.deepCopy(), // 【關鍵】深層複製 stats
        new ArrayList<GameItem>(this.inventory) // Inventory
    );
  }

  // 只想提供給 GuestBehavior 使用
  public void fromRecord(GuestBehavior behavior, PlayerRecord record) {
    // 玩家基礎資料(from資料庫)
    this.id = record.id();
    this.name = record.name();
    this.nickname = record.nickname();
    if (this.nickname == null) {
      this.nickname = record.name();
    }
    this.aliases = new ArrayList<>(List.of(record.name()));
    this.stats = record.stats();
    this.currentRoomId = record.currentRoomId();
    this.inventory = record.inventory();
    if (this.inventory == null) {
      this.inventory = new ArrayList<>();
    }
  }

  /**
   * 檢查是否受到 GCD 限制
   */
  public boolean isOnGcd() {
    return System.currentTimeMillis() < gcdEndTimestamp;
  }

  /**
   * 觸發 GCD
   *
   * @param duration 如果是 0，則使用預設值
   */
  public void triggerGcd(int duration) {
    // int actualDuration = (duration > 0) ? duration : GameConfig.DEFAULT_GCD;
    int actualDuration = (duration > 0) ? duration : 1500;
    this.gcdEndTimestamp = System.currentTimeMillis() + actualDuration;
  }

  // Java 範例：發送狀態更新
  @Override
  public void sendStatUpdate() {
    Map<String, Object> update = new HashMap<>();
    update.put("type", "STAT_UPDATE");
    update.put("hp", stats.hp);
    update.put("maxHp", stats.maxHp);
    update.put("mp", stats.mp);
    update.put("maxMp", stats.maxMp);
    update.put("energy", stats.getCombatResource("charge")); // 0~100

    // 轉成 JSON 字串發送給前端
    try {
      String json = service.getObjectMapper().writeValueAsString(update);
      session.sendMessage(new TextMessage(json));
    } catch (Exception e) {
      log.error("發送狀態更新失敗: {}", this.getName(), e);
    }
  }

  @Override
  protected void performRemoveFromRoom(Room room) {
    room.removePlayer(id);
  }

  @Override
  public void processDeath() {
    log.info("processDeath");

    // 更新玩家 stats
    sendStatUpdate();

    // 先暫停 2秒
    try {
      Thread.sleep(2000);
    } catch (InterruptedException ignored) {
    }

    // 取得死亡的房間 (可能死亡時被移出房間)

    getCurrentRoom().removePlayer(id);

    // 取出 currentRoom 區域的重生點/安全點 或是固定地點墳場 (如果有的話)
    // setCurrentRoomId("newbie_village:cemetery");
    setCurrentRoomId("newbie_village:cemetery");
    Room room = getCurrentRoom();

    // 復活玩家
    markValid();
    getStats().setHp(1);
    getStats().setMp(0);
    getStats().setStamina(0);
    // 更新玩家 stats
    sendStatUpdate();
    room.enter(this, Direction.UP);

    // 自動 Look 直接調用 LookCommand 執行邏輯 (讓玩家看到新環境)
    // service.getCommandDispatcher().dispatch(this, "look");

    // 對房間廣播你復活了
    room.broadcastToOthers(id, "$N復活了，又是一尾活龍！");
  }



  // 強制登出程序
  public void forceLogout() {
    // 1. 存檔
    // saveToDb();

    // // 2. 從房間移除
    // if (currentRoom != null) {
    // currentRoom.removePlayer(this);
    // currentRoom.broadcast(name + " 的身影慢慢消失在空氣中。");
    // }

    // // 3. 從全域管理器移除 (Map<String, PlayerActor>)
    // PlayerManager.remove(this.name);

    // 4. 終止 Actor 迴圈
    stop();

    // 5. 關閉 Socket (保險起見)
    if (session != null) {
      try {
        session.close();
      } catch (Exception e) {
      }
    }
  }



  // 公開給外部呼叫的方法 --------------------------------------------------------------------------



  public void reconnect(WebSocketSession newSession) {
    this.send(new ActorMessage.Reconnect(newSession));
  }

  public void disconnect() {
    this.send(new ActorMessage.Disconnect());
  }
}
