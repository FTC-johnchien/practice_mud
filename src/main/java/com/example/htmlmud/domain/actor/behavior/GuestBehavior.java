package com.example.htmlmud.domain.actor.behavior;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.MDC;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.application.service.AuthService;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.domain.actor.impl.RoomActor;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.context.MudKeys;
import com.example.htmlmud.domain.model.PlayerRecord;
import com.example.htmlmud.infra.persistence.entity.UserEntity;
import com.example.htmlmud.protocol.ConnectionState;
import com.example.htmlmud.protocol.GameCommand;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
// @RequiredArgsConstructor
public class GuestBehavior implements PlayerBehavior {

  private static final int MAX_AUTH_RETRIES = 5;

  private final AuthService authService;

  @Setter
  private String tempUsername; // 暫存正在處理的帳號名

  @Setter
  private String tempPassword; // 暫存正在處理的密碼

  public GuestBehavior(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public PlayerBehavior handle(PlayerActor actor, GameCommand cmd) {
    // 目前只處理文字輸入 (Input)
    PlayerBehavior next = null;
    if (cmd instanceof GameCommand.Input(var text)) {
      switch (actor.getConnectionState()) {
        case CONNECTED -> doConnected(actor, text);
        case CREATING_USERNAME -> doCreatingUsername(actor, text);
        case CREATING_PASSWORD -> doCreatingPassword(actor, text);
        case ENTERING_PASSWORD -> {
          next = doEnterPassword(actor, text);
        }
        case ENTERING_CHAR_NAME -> doEnteringCharName(actor, text);
        case ENTERING_CHAR_GENDER -> {
        }
        case ENTERING_CHAR_RACE -> {
        }
        case ENTERING_CHAR_CLASS -> {
        }
        case ENTERING_CHAR_ATTRIBUTES -> {
        }
        case IN_GAME -> {
        }
      }
    }

    return next;
  }

  private void doConnected(PlayerActor actor, String input) {
    log.info("doConnected");

    if ("new".equalsIgnoreCase(input.trim())) {
      doRegister(actor);
    } else {
      doEnterUsername(actor, input);
    }
  }

  private void doRegister(PlayerActor actor) {
    log.info("doRegister");

    WebSocketSession session = actor.getSession();
    String msg = "【註冊流程】\r\n只能包含英文字母（不分大小寫），不允許數字、空格或特殊符號。\r\n長度必須在 4 到 20 個字元之間。\r\n請輸入您想使用的帳號名稱:";
    try {
      // 告訴前端：切換輸入模式為帳號 (透過自定義協議，例如 JSON {type: "USER_MODE"})
      String json = actor.getServices().objectMapper()
          .writeValueAsString(Map.of("type", "USER_MODE", "content", msg));
      session.sendMessage(new TextMessage(json));

      session.getAttributes().put(MudKeys.AUTH_RETRY_COUNT_KEY, 0); // 重置計數
      actor.setConnectionState(ConnectionState.CREATING_USERNAME);
    } catch (IOException e) {
      log.error("doRegister {}", e.getMessage(), e);
    }
  }

  public void doCreatingUsername(PlayerActor actor, String input) {
    log.info("doCreatingUsername");
    WebSocketSession session = actor.getSession();

    // 取得目前的重試次數
    int retryCount = (int) Optional
        .ofNullable(session.getAttributes().get(MudKeys.AUTH_RETRY_COUNT_KEY)).orElse(0);

    String errorReason = authService.validateUsername(input);

    if (errorReason != null) {
      retryCount++;
      session.getAttributes().put(MudKeys.AUTH_RETRY_COUNT_KEY, retryCount);

      if (retryCount >= MAX_AUTH_RETRIES) {
        log.warn("連線 {} 註冊帳號失敗次數過多 ({})，強制中斷。最後輸入: {}", session.getId(), retryCount, input);
        actor.reply("嘗試次數過多，連線即將關閉。");
        try {
          session.close();
        } catch (IOException ignored) {
        }

        return;
      }

      log.info("連線 {} 註冊帳號失敗: {} (剩餘次數: {})", session.getId(), errorReason,
          MAX_AUTH_RETRIES - retryCount);
      actor.reply(errorReason + " (剩餘嘗試次數: " + (MAX_AUTH_RETRIES - retryCount) + ")");
      return;
    }

    // 驗證成功，重置計數
    session.getAttributes().put(MudKeys.AUTH_RETRY_COUNT_KEY, 0);

    // 暫存帳號
    this.tempUsername = input;

    // String msg = "請輸入密碼:";
    String msg = "【註冊流程】\r\n只能包含英文字母與數字且長度必須在 6 到 32 個字元之間。\r\n請輸入密碼:";
    try {
      // 告訴前端：切換輸入模式為密碼 (透過自定義協議，例如 JSON {type: "PWD_MODE"})
      String json = actor.getServices().objectMapper()
          .writeValueAsString(Map.of("type", "PWD_MODE", "content", msg));
      session.sendMessage(new TextMessage(json));

      actor.setConnectionState(ConnectionState.CREATING_PASSWORD);
    } catch (IOException e) {
      log.error("doCreatingUsername {}", e.getMessage(), e);
    }
  }

  public void doCreatingPassword(PlayerActor actor, String input) {
    log.info("doCreatingPassword");

    WebSocketSession session = actor.getSession();

    // 取得目前的重試次數
    int retryCount = (int) Optional
        .ofNullable(session.getAttributes().get(MudKeys.AUTH_RETRY_COUNT_KEY)).orElse(0);

    String errorReason = authService.validatePassword(input);

    if (errorReason != null) {
      retryCount++;
      session.getAttributes().put(MudKeys.AUTH_RETRY_COUNT_KEY, retryCount);

      if (retryCount >= MAX_AUTH_RETRIES) {
        log.warn("連線 {} 註冊密碼失敗次數過多 ({})，強制中斷。最後輸入: {}", session.getId(), retryCount, input);
        actor.reply("嘗試次數過多，連線即將關閉。");
        try {
          session.close();
        } catch (IOException ignored) {
        }

        return;
      }

      log.info("連線 {} 註冊密碼失敗: {} (剩餘次數: {})", session.getId(), errorReason,
          MAX_AUTH_RETRIES - retryCount);
      actor.reply(errorReason + " (剩餘嘗試次數: " + (MAX_AUTH_RETRIES - retryCount) + ")");
      return;
    }

    // 驗證成功，移除計數
    session.getAttributes().remove(MudKeys.AUTH_RETRY_COUNT_KEY);

    // 暫存密碼
    this.tempPassword = input;

    // 新增玩家帳密
    authService.register(tempUsername, tempPassword);
    log.info("玩家註冊成功: {}", tempUsername);

    this.tempUsername = null;
    this.tempPassword = null;

    // String msg = "請輸入密碼:";
    String msg = "【註冊流程】\r\n註冊完成，請重新登入。";
    // 告訴前端：切換輸入模式為密碼 (透過自定義協議，例如 JSON {type: "PWD_MODE"})
    actor.reply(msg);

    actor.setConnectionState(ConnectionState.CONNECTED);
  }

  private void doEnterUsername(PlayerActor actor, String input) {
    log.info("doLoginUsername");

    WebSocketSession session = actor.getSession();

    // 取得目前的重試次數
    int retryCount = (int) Optional
        .ofNullable(session.getAttributes().get(MudKeys.AUTH_RETRY_COUNT_KEY)).orElse(0);

    String errorReason = authService.validateUsername(input);

    // 如果格式正確，檢查資料庫是否存在該帳號
    if (errorReason == null && !authService.exists(input)) {
      errorReason = "帳號不存在。";
    }

    if (errorReason != null) {
      retryCount++;
      session.getAttributes().put(MudKeys.AUTH_RETRY_COUNT_KEY, retryCount);

      if (retryCount >= MAX_AUTH_RETRIES) {
        log.warn("連線 {} 登入帳號失敗次數過多 ({})，強制中斷。最後輸入: {}", session.getId(), retryCount, input);
        actor.reply("嘗試次數過多，連線即將關閉。");
        try {
          session.close();
        } catch (IOException ignored) {
        }

        return;
      }

      log.info("連線 {} 登入帳號失敗: {} (剩餘次數: {})", session.getId(), errorReason,
          MAX_AUTH_RETRIES - retryCount);
      actor.reply(errorReason + " (剩餘嘗試次數: " + (MAX_AUTH_RETRIES - retryCount) + ")");
      return;
    }

    // 驗證成功，重置計數
    session.getAttributes().put(MudKeys.AUTH_RETRY_COUNT_KEY, 0);

    // 暫存帳號
    this.tempUsername = input;

    String msg = "請輸入密碼:";
    try {
      // 告訴前端：切換輸入模式為密碼 (透過自定義協議，例如 JSON {type: "PWD_MODE"})
      String json = actor.getServices().objectMapper()
          .writeValueAsString(Map.of("type", "PWD_MODE", "content", msg));
      session.sendMessage(new TextMessage(json));

      actor.setConnectionState(ConnectionState.ENTERING_PASSWORD);
    } catch (IOException e) {
      log.error("msg");
    }
  }

  private PlayerBehavior doEnterPassword(PlayerActor actor, String input) {
    log.info("doEnterPassword");

    WebSocketSession session = actor.getSession();

    // 取得目前的重試次數
    int retryCount = (int) Optional
        .ofNullable(session.getAttributes().get(MudKeys.AUTH_RETRY_COUNT_KEY)).orElse(0);

    String errorReason = authService.validatePassword(input);

    if (errorReason != null) {
      retryCount++;
      session.getAttributes().put(MudKeys.AUTH_RETRY_COUNT_KEY, retryCount);

      if (retryCount >= MAX_AUTH_RETRIES) {
        log.warn("連線 {} 登入密碼失敗次數過多 ({})，強制中斷。最後輸入: {}", session.getId(), retryCount, input);
        actor.reply("嘗試次數過多，連線即將關閉。");
        try {
          session.close();
        } catch (IOException ignored) {
        }

        return null;
      }

      log.info("連線 {} 登入密碼失敗: {} (剩餘次數: {})", session.getId(), errorReason,
          MAX_AUTH_RETRIES - retryCount);
      actor.reply(errorReason + " (剩餘嘗試次數: " + (MAX_AUTH_RETRIES - retryCount) + ")");
      return null;
    }

    // 驗證成功，移除計數
    session.getAttributes().remove(MudKeys.AUTH_RETRY_COUNT_KEY);

    // 暫存密碼
    this.tempPassword = input;
    // log.info("玩家登入: {} {}", this.tempUsername, this.tempPassword);

    // 登入玩家帳密
    PlayerRecord record = authService.login(tempUsername, tempPassword);

    // 變更為正式玩家身份
    return upgradeIdentity(actor, record);
  }

  private void doEnteringCharName(PlayerActor actor, String input) {
    log.info("doEnteringCharName");

    WebSocketSession session = actor.getSession();
    String msg =
        "【角色設定流程】\r\n只能包含英文字母（不分大小寫），不允許數字、空格或特殊符號。\r\n長度必須在 2 到 20 個字元之間。\r\n請輸入您想使用的角色名稱:";
    try {
      // 告訴前端：切換輸入模式為帳號 (透過自定義協議，例如 JSON {type: "USER_MODE"})
      String json = actor.getServices().objectMapper()
          .writeValueAsString(Map.of("type", "USER_MODE", "content", msg));
      session.sendMessage(new TextMessage(json));

      session.getAttributes().put(MudKeys.AUTH_RETRY_COUNT_KEY, 0); // 重置計數
      actor.setConnectionState(ConnectionState.ENTERING_CHAR_NAME);
    } catch (IOException e) {
      log.error("doRegister {}", e.getMessage(), e);
    }
  }


  // 供 GuestBehavior 呼叫：由GUEST變更為正式玩家
  private PlayerBehavior upgradeIdentity(PlayerActor actor, PlayerRecord record) {

    // 資料載入
    actor.fromRecord(this, record);
    actor.setConnectionState(ConnectionState.IN_GAME);

    // 裝備?

    // 讓玩家進入資料紀錄的房間
    RoomActor room = actor.getManager().getRoomActor(actor.getCurrentRoomId());
    CompletableFuture<Void> future = new CompletableFuture<>();
    room.enter(actor, future);
    try {
      future.orTimeout(1, java.util.concurrent.TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("enterRoom", e);
    }

    log.info("Actor 載入玩家資料 當前狀態: {} {}", actor.getName(), actor.getNickname());
    return new InGameBehavior();
  }

}
