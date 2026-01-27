package com.example.htmlmud.domain.actor.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.actor.behavior.GuestBehavior;
import com.example.htmlmud.domain.actor.behavior.PlayerBehavior;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.domain.context.MudContext;
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
@Slf4j
public final class PlayerActor extends LivingActor {

  @Getter
  private WebSocketSession session;

  @Getter
  private final WorldManager manager;

  @Getter
  @Setter
  private List<String> aliases;

  @Getter
  @Setter
  private String nickname;

  @Getter
  @Setter
  private String lookDescription;


  // Actor 內部狀態 (State Machine Context)
  @Getter
  @Setter
  private ConnectionState connectionState = ConnectionState.CONNECTED;

  // 當前的行為腦
  @Setter
  private PlayerBehavior currentBehavior;

  private boolean isDirty = false;



  private PlayerActor(WebSocketSession session, String id, String name, LivingState state,
      WorldManager worldManager, GameServices services) {
    super(id, name, state, services);
    this.session = session;
    this.manager = worldManager;
  }

  // 工廠方法 初始設定為 GuestBehavior
  public static PlayerActor createGuest(WebSocketSession session, WorldManager worldManager,
      GameServices gameServices) {
    String name = "GUEST";
    String uuid = UUID.randomUUID().toString();
    String playerId = "p-" + uuid.substring(0, 8) + uuid.substring(9, 11);
    PlayerActor actor =
        new PlayerActor(session, playerId, name, new LivingState(), worldManager, gameServices);
    actor.become(new GuestBehavior(worldManager.getAuthService()));
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
    switch (msg) {
      // 1. 先攔截我專屬的訊息
      case ActorMessage.PlayerMessage playerMsg -> handlePlayerMessage(playerMsg);

      // 2. 其他的 (LivingMessage 或 MobMessage?) 丟給父類別處理
      // 父類別會處理 LivingMessage，並忽略 MobMessage
      default -> super.handleMessage(msg);
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
      try {
        String json =
            services.objectMapper().writeValueAsString(Map.of("type", "TEXT", "content", msg));

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
    manager.getPlayerPersistenceService().saveAsync(this.toRecord());
  }



  // @Override
  // protected void doTick(long tickCount, long time) {
  // log.info("player tickCount: {}", tickCount);
  // super.doTick(tickCount, time);
  // }

  @Override
  protected void doDie(LivingActor attacker) {
    this.stop();
    reply("你已經死亡！即將在重生點復活...");
    super.doDie(attacker);


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



  // MudWebSocketHandler 在 afterConnectionClosed 時呼叫此方法
  public void handleDisconnect() {
    // 1. 停止 Actor 迴圈
    this.stop();

    // 2. 如果已經登入並存在於世界中，從世界移除
    // 注意：這裡可以做「離線保護」或「延遲登出」
    // 但最簡單的做法是直接移除
    // worldManager.removePlayer(this.getId());

    // 3. 觸發存檔 (Write-Behind)
    // persistenceService.saveAsync(this.toRecord());
  }



  // 未整理也
  private void replaceSession(WebSocketSession newSession) {
    // 1. 關閉舊連線 (如果還開著)
    if (this.session != null && this.session.isOpen()) {
      try {
        this.session.close();
      } catch (IOException ignored) {
      }
    }

    // 2. 換上新連線
    this.session = newSession;

    // 3. 重發當前環境資訊
    this.doSendText(session, "\u001B[33m[系統] 連線已恢復。\u001B[0m");
    this.services.commandDispatcher().dispatch(this, "look");
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

}
