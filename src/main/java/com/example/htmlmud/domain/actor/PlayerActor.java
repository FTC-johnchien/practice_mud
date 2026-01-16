package com.example.htmlmud.domain.actor;

import java.io.IOException;
import java.util.Map;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.logic.command.CommandDispatcher;
import com.example.htmlmud.domain.model.GameObjectId;
import com.example.htmlmud.domain.model.PlayerRecord;
import com.example.htmlmud.domain.model.json.LivingState;
import com.example.htmlmud.infra.util.AnsiColor;
import com.example.htmlmud.infra.util.ColorText;
import com.example.htmlmud.protocol.ActorMessage;
import com.example.htmlmud.protocol.ConnectionState;
import com.example.htmlmud.protocol.GameCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

// PlayerActor 處理的訊息類型就是 GameCommand
@Slf4j
public class PlayerActor extends LivingActor {

  private final WebSocketSession session;
  // private PlayerService playerService;


  private PlayerRecord currentData; // 記憶體中的最新狀態
  private boolean isDirty = false;

  // Actor 內部狀態 (State Machine Context)
  private ConnectionState state = ConnectionState.CONNECTED;
  private String tempUsername; // 暫存正在處理的帳號名
  private String playerName = "Unassigned";

  private final ObjectMapper objectMapper;

  private CommandDispatcher commandDispatcher;

  public PlayerActor(String id, WebSocketSession session, LivingState state,
      ObjectMapper objectMapper) {
    super(id, state);
    this.session = session;
    this.objectMapper = objectMapper;
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
    // 使用 ScopedValue 重新綁定從信封拿到的 traceId
    ScopedValue.where(MudContext.TRACE_ID, msg.traceId()).run(() -> {
      GameCommand cmd = msg.command();

      // 只處理字串輸入 (Input)
      if (cmd instanceof GameCommand.Input(var text)) {
        String cleanText = text.trim();
        log.info("[State:{}] Input: {}", state, cleanText);

        // 根據當前狀態分流
        switch (state) {
          case CONNECTED -> handleConnected(cleanText);
          case REGISTER_USERNAME -> handleRegisterUsername(cleanText);
          case REGISTER_PASSWORD -> handleRegisterPassword(cleanText);
          case LOGIN_PASSWORD -> handleLoginPassword(cleanText);
          case PLAYING -> handleGameLogic(cleanText, msg.traceId());
        }
      }
    });

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
  protected void onDeath(GameObjectId killerId) {
    reply("你已經死亡！即將在重生點復活...");
    // 玩家死亡邏輯：掉經驗、傳送回城
  }

  public void sendText(String text) {
    reply(text);
  }

  // --- 狀態處理邏輯 ---

  // 1. 剛連線
  private void handleConnected(String input) {
    if ("new".equalsIgnoreCase(input)) {
      state = ConnectionState.REGISTER_USERNAME;
      reply("【註冊流程】請輸入您想使用的帳號名稱：");
    } else {
      // 視為嘗試登入
      // if (playerService.exists(input)) {
      // this.tempUsername = input;
      // state = State.LOGIN_PASSWORD;
      // reply("帳號存在，請輸入密碼：");
      // } else {
      // reply("找不到帳號 '%s'。請重新輸入，或輸入 'new' 註冊。".formatted(input));
      // }
    }
  }

  // 2. 註冊 - 檢查帳號
  private void handleRegisterUsername(String input) {
    if (input.length() < 3) {
      reply("帳號長度需大於 3 個字元，請重試：");
      return;
    }
    // if (playerService.isReservedWord(input)) {
    // reply("'%s' 是系統保留字，請換一個：".formatted(input));
    // return;
    // }
    // if (playerService.exists(input)) {
    // reply("'%s' 已經被註冊了，請換一個：".formatted(input));
    // return;
    // }

    this.tempUsername = input;
    state = ConnectionState.REGISTER_PASSWORD;
    reply("帳號 '%s' 可用。請設定您的密碼：".formatted(input));
  }

  // 3. 註冊 - 設定密碼
  private void handleRegisterPassword(String input) {
    if (input.length() < 3) {
      reply("密碼太短，請重試：");
      return;
    }

    // playerService.register(tempUsername, input);
    // log.info("User registered: {}", tempUsername);

    reply("註冊成功！已自動為您登入。");
    enterGame();
  }

  // 4. 登入 - 驗證密碼
  private void handleLoginPassword(String input) {
    // if (playerService.verifyPassword(tempUsername, input)) {
    // reply("登入成功！歡迎回來，%s".formatted(tempUsername));
    // enterGame();
    // } else {
    // reply("密碼錯誤，請重新輸入：");
    // }
  }

  // 5. 進入遊戲 (轉場)
  private void enterGame() {
    state = ConnectionState.PLAYING;
    // 未來這裡可以加入 WorldManager.joinRoom()
    handleGameLogic("look", "system-init"); // 自動看一次環境
  }

  // 6. 遊戲中邏輯 (Look, Kill, Move...)
  private void handleGameLogic(String input, String traceId) {
    String[] parts = handleStringInput(input);
    String action = parts[0].toLowerCase();

    switch (action) {
      case "look" -> {
        // 範例：組合一個多彩的房間描述
        StringBuilder sb = new StringBuilder();
        sb.append(ColorText.wrap(AnsiColor.BRIGHT_WHITE, AnsiColor.BOLD, "=== 新手村廣場 ===\n"));
        sb.append(ColorText.wrap(AnsiColor.LIGHT_GREY, "這是一個鋪著石板的廣場，四週有些破舊。\n"));
        sb.append("這裡有一個 ").append(ColorText.npc("村長")).append(" 站在噴泉旁。\n");
        sb.append("地上掉落了一把 ").append(ColorText.item("生鏽的鐵劍")).append("。\n");
        sb.append("往北可以看到陰森的 ").append(ColorText.wrap(AnsiColor.DARK_GREY, "黑暗森林")).append("。");

        reply(sb.toString());
      } // reply("你看見四週一片漆黑，遠方似乎有微弱的火光。");
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
    try {
      if (session.isOpen()) {
        // 使用 Map 來建立結構，Jackson 會自動處理所有特殊字元的轉義
        String json = objectMapper.writeValueAsString(Map.of("type", "TEXT", "content", msg));

        session.sendMessage(new TextMessage(json));
      }
    } catch (IOException e) {
      log.error("Reply failed", e);
    }
  }

  // --- 這裡實現您的 "純字串" 邏輯 ---
  private String[] handleStringInput(String text) {
    log.info("Player {} input: {}", playerName, text);

    // 簡單的傳統 MUD 解析方式
    // 未來這裡可以抽換成更高級的 Parser，但介面(Input text)不用變
    return text.trim().split("\\s+");
  }

  // 【關鍵】當玩家換瀏覽器重連時，呼叫這個方法
  public void attachSession(WebSocketSession newSession) {
    // 1. 關閉舊連線 (如果還開著)
    if (this.session != null && this.session.isOpen()) {
      try {
        this.session.close();
      } catch (Exception e) {
      }
    }

    // 2. 換上新連線
    // this.session = newSession;

    // 3. 可以在這裡主動推播：「歡迎回來，連線已恢復」
    reply("=== 連線已恢復 ===");
    // 自動執行一次 Look，讓玩家知道自己在哪
    handleGameLogic("look", "user-relink");
  }
}
