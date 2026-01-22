package com.example.htmlmud.domain.actor.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.actor.LivingActor;
import com.example.htmlmud.domain.actor.behavior.GuestBehavior;
import com.example.htmlmud.domain.actor.behavior.InGameBehavior;
import com.example.htmlmud.domain.actor.behavior.PlayerBehavior;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.LivingState;
import com.example.htmlmud.domain.model.PlayerRecord;
import com.example.htmlmud.protocol.ActorMessage;
import com.example.htmlmud.protocol.ConnectionState;
import com.example.htmlmud.protocol.GameCommand;
import com.example.htmlmud.protocol.RoomMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

// PlayerActor 處理的訊息類型就是 GameCommand
@Slf4j
public class PlayerActor extends LivingActor {

  @Getter
  private WebSocketSession session;

  @Getter
  private final WorldManager manager;

  @Getter
  private String characterId;

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
    String playerId = "player-" + session.getId().substring(0, 8);
    PlayerActor actor =
        new PlayerActor(session, playerId, name, new LivingState(), worldManager, gameServices);
    actor.become(new GuestBehavior(worldManager.getAuthService()));
    actor.inventory = new ArrayList<>();
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
      case ActorMessage.Command(String traceId, GameCommand command) -> {
        // A. 設定 MDC (給 Log 看)
        MDC.put("traceId", traceId);
        MDC.put("actorName", this.name);

        try {
          // B. 設定 ScopedValue (給 Service 邏輯看)
          ScopedValue.where(MudContext.CURRENT_PLAYER, this).where(MudContext.TRACE_ID, traceId)
              .run(() -> {
                // C. 委派給當前 Behavior 處理
                PlayerBehavior next = currentBehavior.handle(this, command);

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

      case ActorMessage.Tick(var tickCount, var timestamp) -> {
        log.info("player tickCount: {}", tickCount);
        tick();
      }

      case ActorMessage.Die(var killerId) -> {
        onDeath(killerId);
      }

      case ActorMessage.SendText(var content) -> {
        performSendText(content);
      }

    }
    /*
     * // 這裡已經是 Single Thread 環境，完全不用 synchronized // 使用 ScopedValue 重新綁定從信封拿到的 traceId
     * ScopedValue.where(MudContext.TRACE_ID, msg.traceId()).run(() -> { GameCommand cmd =
     * msg.command();
     *
     * // 只處理字串輸入 (Input) if (cmd instanceof GameCommand.Input(var text)) { String cleanText =
     * text.trim(); log.info("[State:{}] Input: {}", state, cleanText);
     *
     * // 根據當前狀態分流 // switch (connectionState) { // case CONNECTED -> handleConnected(cleanText); //
     * case CREATING_USER -> handleRegisterUsername(cleanText); // case CREATING_PASS ->
     * handleRegisterPassword(cleanText); // case ENTERING_PASS -> handleLoginPassword(cleanText);
     * // case PLAYING -> handleGameLogic(cleanText, msg.traceId()); // } handleGameLogic(cleanText,
     * msg.traceId()); } });
     */
    // 這裡是由 Actor 的 Virtual Thread 執行的
    // Actor 直接操作 session 物件發送資料
    // try {
    // switch (cmd) {
    // case GameCommand.Login(var user, var pass) -> {
    // log.info("[Trace:{}] Processing Login: {}", traceId, user);
    // handleLogin(user, pass);
    // }

    // // 核心：所有遊戲指令都在這裡處理
    // case GameCommand.Input(var text) -> {
    // log.info("[Trace:{}] Processing Input: {}", traceId, text);
    // handleStringInput(text);
    // }
    // // case GameCommand.Chat(var content) -> {
    // // log.debug("Chat from {}: {}", playerName, content);

    // // reply("%s 說: %s".formatted(playerName, content));
    // // }
    // // case GameCommand.Move(var dir) -> {
    // // reply("你往 %s 移動了一步。".formatted(dir));
    // // }
    // }
    // } catch (Exception e) {
    // log.error("[Trace:{}] Error", traceId, e);
    // }
  }

  @Override
  protected void onDeath(String killerId) {
    reply("你已經死亡！即將在重生點復活...");
    // 玩家死亡邏輯：掉經驗、傳送回城
  }

  public void sendText(String text) {
    reply(text);
  }

  // 狀態切換方法
  private void become(PlayerBehavior nextBehavior) {
    this.currentBehavior = nextBehavior;
    this.currentBehavior.onEnter(this); // 觸發進場事件
    log.info("{} 切換行為模式至 {}", this.name, nextBehavior.getClass().getSimpleName());
  }

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

  // --- 狀態處理邏輯 ---
  /*
   * // 1. 剛連線 private void handleConnected(String input) { if ("new".equalsIgnoreCase(input)) {
   * connectionState = ConnectionState.CREATING_USER; reply("【註冊流程】請輸入您想使用的帳號名稱："); } else { //
   * 視為嘗試登入 // if (playerService.exists(input)) { // this.tempUsername = input; // state =
   * State.LOGIN_PASSWORD; // reply("帳號存在，請輸入密碼："); // } else { //
   * reply("找不到帳號 '%s'。請重新輸入，或輸入 'new' 註冊。".formatted(input)); // } } }
   *
   * // 2. 註冊 - 檢查帳號 private void handleRegisterUsername(String input) { if (input.length() < 3) {
   * reply("帳號長度需大於 3 個字元，請重試："); return; } // if (playerService.isReservedWord(input)) { //
   * reply("'%s' 是系統保留字，請換一個：".formatted(input)); // return; // } // if
   * (playerService.exists(input)) { // reply("'%s' 已經被註冊了，請換一個：".formatted(input)); // return; // }
   *
   * this.tempUsername = input; connectionState = ConnectionState.ENTERING_PASS;
   * reply("帳號 '%s' 可用。請設定您的密碼：".formatted(input)); }
   *
   * // 3. 註冊 - 設定密碼 private void handleRegisterPassword(String input) { if (input.length() < 3) {
   * reply("密碼太短，請重試："); return; }
   *
   * // playerService.register(tempUsername, input); // log.info("User registered: {}",
   * tempUsername);
   *
   * reply("註冊成功！已自動為您登入。"); enterGame(); }
   *
   * // 4. 登入 - 驗證密碼 private void handleLoginPassword(String input) { // if
   * (playerService.verifyPassword(tempUsername, input)) { //
   * reply("登入成功！歡迎回來，%s".formatted(tempUsername)); // enterGame(); // } else { //
   * reply("密碼錯誤，請重新輸入："); // } }
   *
   * // 5. 進入遊戲 (轉場) private void enterGame() { connectionState = ConnectionState.PLAYING; //
   * 未來這裡可以加入 WorldManager.joinRoom() handleGameLogic("look", "system-init"); // 自動看一次環境 }
   */
  // 6. 遊戲中邏輯 (Look, Kill, Move...)
  // private void handleGameLogic(String input, String traceId) {
  // String[] parts = handleStringInput(input);
  // String action = parts[0].toLowerCase();

  // switch (action) {
  // case "look" -> {
  // // 範例：組合一個多彩的房間描述
  // StringBuilder sb = new StringBuilder();
  // sb.append(ColorText.wrap(AnsiColor.BRIGHT_WHITE, AnsiColor.BOLD, "=== 新手村廣場 ===\n"));
  // sb.append(ColorText.wrap(AnsiColor.LIGHT_GREY, "這是一個鋪著石板的廣場，四週有些破舊。\n"));
  // sb.append("這裡有一個 ").append(ColorText.npc("村長")).append(" 站在噴泉旁。\n");
  // sb.append("地上掉落了一把 ").append(ColorText.item("生鏽的鐵劍")).append("。\n");
  // sb.append("往北可以看到陰森的 ").append(ColorText.wrap(AnsiColor.DARK_GREY, "黑暗森林")).append("。");

  // reply(sb.toString());
  // } // reply("你看見四週一片漆黑，遠方似乎有微弱的火光。");
  // case "help" -> reply("可用指令: look, say, quit");
  // case "say" -> reply("你說道: " + (parts.length > 1 ? input.substring(4) : "..."));
  // case "quit" -> {
  // reply("再見！");
  // // 這裡可以做 disconnect 邏輯
  // }
  // case "kill" -> {
  // if (parts.length > 1) {
  // reply("你揮劍砍向了 %s！".formatted(parts[1]));
  // } else {
  // reply("你要殺誰？");
  // }
  // }
  // default -> {
  // log.info("Unknown command: {}", action);
  // reply("我不懂 '%s' 是什麼意思。".formatted(action));
  // }
  // }
  // }

  // private void handleLogin(String user, String pass) {
  // log.info("Player logged in: {}", user);

  // reply("歡迎進入 MUD 世界，%s。現在時間：%s".formatted(user, java.time.Instant.now()));
  // // // 1. 查 DB (透過 Service)
  // // PlayerEntity entity = playerRepository.findByUsername(cmd.username());

  // // // 2. 比對密碼 (使用 BCrypt)
  // // if (entity != null && passwordEncoder.matches(cmd.password(), entity.getPasswordHash())) {
  // // this.isLoggedIn = true;
  // // this.playerId = entity.getId();
  // // this.state = loadState(entity); // 載入玩家資料
  // // send("登入成功！");
  // // } else {
  // // send("帳號或密碼錯誤。");
  // // // 可以增加失敗計數器，防止爆破
  // // }
  // }
  // 在 PlayerActor.java 中
  protected void levelUp() {
    // 從 Context 取得當前 Trace ID
    String currentTraceId = MudContext.traceId();

    // 發送事件
    // publisher.publishEvent(
    // new DomainEvent.PlayerLevelUpEvent(currentTraceId, this.objectId.id(), this.state.level));
  }

  // 將原本的 sendText 改名或設為私有，避免外部誤用
  private void performSendText(String msg) {
    if (session != null && session.isOpen()) {
      try {
        // 這裡才是真正寫入 WebSocket 的地方
        // 因為是在 handleMessage 內執行，保證了 Thread-Safe
        String json =
            services.objectMapper().writeValueAsString(Map.of("type", "TEXT", "content", msg));

        session.sendMessage(new TextMessage(json));
      } catch (IOException e) {
        log.error("Failed to send message to player {}", id, e);
      }
    }
  }

  public void reply(String msg) {
    this.send(new ActorMessage.SendText(msg));
  }

  // --- 這裡實現您的 "純字串" 邏輯 ---
  private String[] handleStringInput(String text) {
    log.info("Player {} input: {}", name, text);

    // 簡單的傳統 MUD 解析方式
    // 未來這裡可以抽換成更高級的 Parser，但介面(Input text)不用變
    return text.trim().split("\\s+");
  }

  // 供子類別 (PlayerActor) 呼叫，用來切換數據
  protected void swapIdentity(String newId, LivingState newState) {
    this.id = newId;
    this.state = newState;
  }

  // 供 GuestBehavior 呼叫：變身為正式玩家
  public void upgradeIdentity(PlayerRecord record) {
    this.fromRecord(record);
    this.become(new InGameBehavior());
    this.characterId = record.name();
    this.name = record.name();
    if (record.nickname() != null) {
      this.name = record.nickname();
    }

    // 將玩家放入房間
    RoomActor room = manager.getRoomActor(this.getCurrentRoomId());
    room.addPlayer(this);

    log.info("Actor 載入玩家資料 當前狀態: {} {}", this.name, this.nickname);
  }

  public void replaceSession(WebSocketSession newSession) {
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
    this.sendText("\u001B[33m[系統] 連線已恢復。\u001B[0m");
    this.services.commandDispatcher().dispatch(this, "look");
  }

  private void handleLoginSuccess(PlayerRecord record) {
    // 使用父類別的 protected 方法變身
    swapIdentity(record.id(), record.state());

    // 變身後，才加入世界管理
    // WorldManager.joinWorld(this);

    reply("登入成功！");
  }

  // 觸發存檔的輔助方法
  public void save() {
    manager.getPlayerPersistenceService().saveAsync(this.toRecord());
  }


  public PlayerRecord toRecord() {
    // 您必須確保 LivingState 有實作 deepCopy，否則會發生併發修改例外
    return new PlayerRecord(this.id, // ID
        this.name, // Username
        this.nickname, // Nickname
        this.currentRoomId, // Room
        this.state.deepCopy(), // 【關鍵】深層複製 State
        new ArrayList<GameItem>(this.inventory) // Inventory
    );
  }

  public void fromRecord(PlayerRecord record) {
    // 1. 更新基礎資料
    this.name = record.name();
    this.nickname = record.nickname();
    this.currentRoomId = record.currentRoomId();
    this.inventory = record.inventory();


    // 2. 更新狀態 (直接替換引用)
    // 因為 Record 是從 DB 讀出來的新物件，這裡的 state 是全新的，可以直接拿來用
    this.swapIdentity(record.id(), record.state());

    // 3. 標記為已登入
    this.id = record.id();
  }
}
