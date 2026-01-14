package com.example.htmlmud.domain.actor;

import java.io.IOException;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.actor.core.VirtualActor;
import com.example.htmlmud.protocol.ActorMessage;
import com.example.htmlmud.protocol.GameCommand;
import com.example.htmlmud.service.PlayerService;
import lombok.extern.slf4j.Slf4j;

// PlayerActor 處理的訊息類型就是 GameCommand
@Slf4j
public class PlayerActor extends VirtualActor<ActorMessage> {
  // 定義連線狀態
  private enum State {
    CONNECTED, // 剛連線：等待輸入帳號 或 'new'
    REGISTER_USERNAME, // 註冊中：等待輸入新帳號
    REGISTER_PASSWORD, // 註冊中：等待輸入新密碼
    LOGIN_PASSWORD, // 登入中：等待輸入密碼
    IN_GAME // 遊戲中：正常遊玩
  }

  private final WebSocketSession session;
  private final PlayerService playerService;

  // Actor 內部狀態 (State Machine Context)
  private State state = State.CONNECTED;
  private String tempUsername; // 暫存正在處理的帳號名
  private String playerName = "Unassigned";

  public PlayerActor(String sessionId, WebSocketSession session, PlayerService playerService) {
    super("player-" + sessionId);
    this.session = session; // 在建構時就存起來了
    this.playerService = playerService;
  }

  @Override
  public void start() {
    super.start();
    // 連線歡迎詞
    reply("歡迎光臨 Html Mud 世界！");
    reply("請輸入登入帳號 或 輸入 'new' 進行註冊：");
  }

  @Override
  protected void handleMessage(ActorMessage msg) {
    // 這裡已經是 Single Thread 環境，完全不用 synchronized
    String traceId = msg.traceId(); // 拿到 Trace ID 了！
    GameCommand cmd = msg.command(); // 拿到實際指令

    // 只處理字串輸入 (Input)
    if (cmd instanceof GameCommand.Input(var text)) {
      String cleanText = text.trim();
      log.info("[Trace:{}] [State:{}] Input: {}", traceId, state, cleanText);

      // 根據當前狀態分流
      switch (state) {
        case CONNECTED -> handleConnected(cleanText);
        case REGISTER_USERNAME -> handleRegisterUsername(cleanText);
        case REGISTER_PASSWORD -> handleRegisterPassword(cleanText);
        case LOGIN_PASSWORD -> handleLoginPassword(cleanText);
        case IN_GAME -> handleGameLogic(cleanText, traceId);
      }
    }

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

  // --- 狀態處理邏輯 ---

  // 1. 剛連線
  private void handleConnected(String input) {
    if ("new".equalsIgnoreCase(input)) {
      state = State.REGISTER_USERNAME;
      reply("【註冊流程】請輸入您想使用的帳號名稱：");
    } else {
      // 視為嘗試登入
      if (playerService.exists(input)) {
        this.tempUsername = input;
        state = State.LOGIN_PASSWORD;
        reply("帳號存在，請輸入密碼：");
      } else {
        reply("找不到帳號 '%s'。請重新輸入，或輸入 'new' 註冊。".formatted(input));
      }
    }
  }

  // 2. 註冊 - 檢查帳號
  private void handleRegisterUsername(String input) {
    if (input.length() < 3) {
      reply("帳號長度需大於 3 個字元，請重試：");
      return;
    }
    if (playerService.isReservedWord(input)) {
      reply("'%s' 是系統保留字，請換一個：".formatted(input));
      return;
    }
    if (playerService.exists(input)) {
      reply("'%s' 已經被註冊了，請換一個：".formatted(input));
      return;
    }

    this.tempUsername = input;
    state = State.REGISTER_PASSWORD;
    reply("帳號 '%s' 可用。請設定您的密碼：".formatted(input));
  }

  // 3. 註冊 - 設定密碼
  private void handleRegisterPassword(String input) {
    if (input.length() < 3) {
      reply("密碼太短，請重試：");
      return;
    }

    playerService.register(tempUsername, input);
    log.info("User registered: {}", tempUsername);

    reply("註冊成功！已自動為您登入。");
    enterGame();
  }

  // 4. 登入 - 驗證密碼
  private void handleLoginPassword(String input) {
    if (playerService.verifyPassword(tempUsername, input)) {
      reply("登入成功！歡迎回來，%s".formatted(tempUsername));
      enterGame();
    } else {
      reply("密碼錯誤，請重新輸入：");
    }
  }

  // 5. 進入遊戲 (轉場)
  private void enterGame() {
    state = State.IN_GAME;
    // 未來這裡可以加入 WorldManager.joinRoom()
    handleGameLogic("look", "system-init"); // 自動看一次環境
  }

  // 6. 遊戲中邏輯 (Look, Kill, Move...)
  private void handleGameLogic(String input, String traceId) {
    String[] parts = handleStringInput(input);
    String action = parts[0].toLowerCase();

    switch (action) {
      case "look" -> reply("你看見四週一片漆黑，遠方似乎有微弱的火光。");
      case "help" -> reply("可用指令: look, say, quit");
      case "say" -> reply("你說道: " + (parts.length > 1 ? input.substring(4) : "..."));
      case "quit" -> {
        reply("再見！");
        // 這裡可以做 disconnect 邏輯
      }
      case "kill" -> {
        if (parts.length > 1) {
          reply("你揮劍砍向了 %s！".formatted(parts[1]));
        } else {
          reply("你要殺誰？");
        }
      }
      default -> {
        log.info("[Trace:{}] Unknown command: {}", traceId, action);
        reply("我不懂 '%s' 是什麼意思。".formatted(action));
      }
    }
  }

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

  private void reply(String msg) {
    // MVP 階段：直接回傳 JSON 包裹的字串
    // 這樣前端只要解析 { "type": "TEXT", "content": "..." } 即可
    // 未來要加血條更新時，可以改傳 { "type": "UPDATE", "hp": 100 }
    // String jsonResponse = """
    // { "type": "TEXT", "content": "%s" }
    // """.formatted(msg);

    // 這裡呼叫的是 session 的方法
    // 這與當初是哪個 Thread 收進來的完全無關
    try {
      if (session.isOpen()) {
        session.sendMessage(new TextMessage(msg));
      }
    } catch (IOException e) {
      log.error("Failed to send message to client", e);
    }
  }

  // --- 這裡實現您的 "純字串" 邏輯 ---
  private String[] handleStringInput(String text) {
    log.info("Player {} input: {}", playerName, text);

    // 簡單的傳統 MUD 解析方式
    // 未來這裡可以抽換成更高級的 Parser，但介面(Input text)不用變
    return text.trim().split("\\s+");
  }

}
