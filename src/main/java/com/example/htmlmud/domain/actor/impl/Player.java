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
import com.example.htmlmud.application.service.PlayerService;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.actor.behavior.GuestBehavior;
import com.example.htmlmud.domain.actor.behavior.PlayerBehavior;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.Direction;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.LivingState;
import com.example.htmlmud.domain.model.PlayerRecord;
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
  private volatile long lastDisconnectTime = 0;

  @Setter
  private String nickname;

  @Setter
  private String lookDescription;

  // 當前的行為腦
  @Setter
  private PlayerBehavior currentBehavior;



  private Player(WebSocketSession session, String id, String name, LivingState state,
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
        new Player(session, playerId, name, new LivingState(), worldManager, playerService);
    actor.become(new GuestBehavior(playerService.getAuthService(), worldManager));
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
        doCommand(traceId, cmd);
      }
      case ActorMessage.SendText(var session, var content) -> {
        doSendText(session, content);
      }
      case ActorMessage.SaveData() -> {
        save();
      }
      case ActorMessage.GainExp(var amount) -> {
        // TODO
        this.state.exp += amount;
        // if (this.state.exp >= this.state.nextLevelExp) {
        // this.levelUp();
        // }
      }
      case ActorMessage.QuestUpdate(var questId, var status) -> {
        // TODO
      }
      case ActorMessage.Reconnect(var session) -> {
        onReconnect(session);
      }

      default -> log.warn("handlePlayerMessage 收到無法處理的訊息: {} {}", this.id, msg);
    }
  }

  private void doCommand(String traceId, GameCommand cmd) {
    // A. 設定 MDC (給 Log 看)
    MDC.put("traceId", traceId);
    // MDC.put("actorName", this.name);

    try {
      // B. 設定 ScopedValue (給 Service 邏輯看)
      ScopedValue.where(MudContext.CURRENT_PLAYER, this).where(MudContext.TRACE_ID, traceId)
          .run(() -> {
            // C. 委派給當前 Behavior 處理
            PlayerBehavior next = currentBehavior.handle(this, cmd);

            // D. 狀態切換
            if (next != null) {
              become(next);
            }
          });
    } finally {
      // E. 清理 MDC
      MDC.clear();
    }
  }

  private void doSendText(WebSocketSession session, String msg) {
    if (session != null && session.isOpen()) {

      // 處理 $N 代名詞
      msg = service.getMessageUtil().format(msg, this);

      try {
        String json =
            service.getObjectMapper().writeValueAsString(Map.of("type", "TEXT", "content", msg));

        // 這裡才是真正寫入 WebSocket 的地方
        // 因為是在 handleMessage 內執行，保證了 Thread-Safe
        session.sendMessage(new TextMessage(json));
      } catch (IOException e) {
        log.error("Failed to send message to player {}", id, e);
      }
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
  protected void doDie(String killerId) {
    reply("$N已經死亡！即將在重生點復活...");
    super.doDie(killerId);


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

  // 狀態切換方法
  private void become(PlayerBehavior nextBehavior) {
    this.currentBehavior = nextBehavior;
    this.currentBehavior.onEnter(this); // 觸發進場事件
    log.info("{} 切換行為模式至 {}", this.name, nextBehavior.getClass().getSimpleName());
  }

  private void levelUp() {
    // 從 Context 取得當前 Trace ID
    String currentTraceId = MudContext.traceId();

    // 發送事件
    // publisher.publishEvent(
    // new DomainEvent.PlayerLevelUpEvent(currentTraceId, this.objectId.id(), this.state.level));
  }



  private PlayerRecord toRecord() {
    // 您必須確保 LivingState 有實作 deepCopy，否則會發生併發修改例外
    return new PlayerRecord(this.id, // ID
        this.name, // Username
        this.nickname, // Nickname
        this.currentRoomId, // Room
        this.state.deepCopy(), // 【關鍵】深層複製 State
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
    this.state = record.state();
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
    update.put("hp", state.hp);
    update.put("maxHp", state.maxHp);
    update.put("mp", state.mp);
    update.put("maxMp", state.maxMp);
    update.put("energy", state.getCombatResource("charge")); // 0~100

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

    // 更新玩家 state
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
    getState().setHp(1);
    getState().setMp(0);
    getState().setStamina(0);
    // 更新玩家 state
    sendStatUpdate();
    room.enter(this, Direction.UP);

    // 自動 Look 直接調用 LookCommand 執行邏輯 (讓玩家看到新環境)
    // service.getCommandDispatcher().dispatch(this, "look");

    // 對房間廣播你復活了
    room.broadcastToOthers(id, "$N復活了，又是一尾活龍！");
  }



  // 【當 WebSocket 斷線時呼叫此方法】
  public void handleDisconnect() {
    log.warn("{} 斷線，進入緩衝狀態...", name);
    this.lastDisconnectTime = System.currentTimeMillis();
    connectionState = ConnectionState.LINK_DEAD;

    // 廣播給房間其他人 (沉浸式體驗)
    getCurrentRoom().broadcastToOthers(id, nickname + " 眼神突然變得呆滯，似乎失去了靈魂。");

    // 【關鍵】啟動一個「死神 VT」
    startDeathTimer(this.lastDisconnectTime);
  }

  // 【當玩家重新連線時呼叫此方法】
  public void onReconnect(WebSocketSession newSession) {

    // 1. 關閉舊連線 (如果還開著)
    if (this.session != null && this.session.isOpen()) {
      try {
        this.session.close();
      } catch (IOException ignored) {
      }
    }

    // 2. 換上新連線 (必須先更新欄位，確保後續訊息發往正確的連線)
    // 更新斷線時間戳記，這樣之前的死神 VT 醒來後會發現時間對不上，就不會執行殺人
    this.lastDisconnectTime = System.currentTimeMillis();
    connectionState = ConnectionState.IN_GAME;
    service.getMudWebSocketHandlerProvider().getObject().promoteToPlayer(newSession, this);

    // 3. 重發當前環境資訊
    log.warn("{} 重新連線成功！", name);

    // 發送歡迎回來的訊息
    doSendText(this.session, "\u001B[33m[系統] 連線已恢復。\u001B[0m");
    getCurrentRoom().broadcastToOthers(id, nickname + " 的眼神恢復了光采。");
    sendStatUpdate();
    service.getCommandDispatcher().dispatch(this, "look");
  }

  private void startDeathTimer(long disconnectTimestamp) {
    // 啟動一個虛擬執行緒，成本極低
    Thread.ofVirtual().name("Reaper-" + name).start(() -> {
      try {
        // 設定緩衝時間：例如 10 分鐘 (600,000 ms)
        // 這裡直接 sleep，不會佔用系統資源
        Thread.sleep(10 * 60 * 1000);

        // --- 10 分鐘後醒來 ---

        // 檢查 1: 玩家是否還在斷線狀態？
        // 檢查 2: 這是當初那次斷線嗎？(防止玩家重連後又斷線，舊的計時器殺錯)
        if (connectionState == ConnectionState.LINK_DEAD
            && this.lastDisconnectTime == disconnectTimestamp) {
          log.warn("緩衝時間已過，強制清理玩家: {}", name);
          this.forceLogout();
        } else {
          log.info("玩家已重連，死神計時器取消: {}", name);
        }

      } catch (InterruptedException e) {
        // 計時器被中斷
      }
    });
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
    if (session != null)
      try {
        session.close();
      } catch (Exception e) {
      }
  }
}
