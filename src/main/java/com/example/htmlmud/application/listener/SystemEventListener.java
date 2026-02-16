package com.example.htmlmud.application.listener;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudKeys;
import com.example.htmlmud.domain.event.DomainEvent.SystemEvent;
import com.example.htmlmud.domain.service.WorldManager;
import com.example.htmlmud.infra.persistence.repository.UserRepository;
import com.example.htmlmud.infra.server.SessionRegistry;
import com.example.htmlmud.protocol.ConnectionState;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SystemEventListener {

  private static final int MAX_AUTH_RETRIES = 5;

  // 基礎保留字（非指令類的關鍵字）
  private final Set<String> reservedWords = new HashSet<>(
      Set.of("new", "quit", "exit", "wizard", "admin", "system", "root", "guest", "player"));

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private SessionRegistry sessionRegistry;

  @Autowired
  private UserRepository userRepository;



  @EventListener
  public void onRegisterUsername(SystemEvent.RegisterUsername event) throws Exception {
    log.info("onRegisterUsername");
    Player actor = sessionRegistry.get(event.sessionId());
    // WebSocketSession session = actor.getSession();

    // WebSocketSession session = sessionRegistry.get(event.sessionId());
    // PlayerActor actor = worldManager.getPlayer(session);

    // 取得目前的重試次數
    // int retryCount = (int) Optional
    // .ofNullable(session.getAttributes().get(MudKeys.AUTH_RETRY_COUNT_KEY)).orElse(0);

    // String errorReason = validateUsername(event.input());

    // if (errorReason != null) {
    // retryCount++;
    // session.getAttributes().put(MudKeys.AUTH_RETRY_COUNT_KEY, retryCount);

    // if (retryCount >= MAX_AUTH_RETRIES) {
    // log.warn("連線 {} 註冊帳號失敗次數過多 ({})，強制中斷。最後輸入: {}", event.sessionId(), retryCount,
    // event.input());
    // actor.reply("嘗試次數過多，連線即將關閉。");
    // session.close();
    // return;
    // }

    // log.info("連線 {} 註冊帳號失敗: {} (剩餘次數: {})", event.sessionId(), errorReason,
    // MAX_AUTH_RETRIES - retryCount);
    // actor.reply(errorReason + " (剩餘嘗試次數: " + (MAX_AUTH_RETRIES - retryCount) + ")");
    // return;
    // }

    // 驗證成功，重置計數
    // session.getAttributes().put(MudKeys.AUTH_RETRY_COUNT_KEY, 0);

    // 暫存帳號
    // actor.setTempUsername(event.input());

    // String msg = "請輸入密碼:";
    String msg = "【註冊流程】\r\n只能包含英文字母與數字且長度必須在 6 到 32 個字元之間。\r\n請輸入密碼:";
    // 告訴前端：切換輸入模式為密碼 (透過自定義協議，例如 JSON {type: "PWD_MODE"})
    // String json = objectMapper.writeValueAsString(Map.of("type", "PWD_MODE", "content", msg));
    // session.sendMessage(new TextMessage(json));

    actor.setConnectionState(ConnectionState.CREATING_PASSWORD);
  }

  @EventListener
  public void onRegisterPassword(SystemEvent.RegisterPassword event) throws IOException {
    log.info("onRegisterPassword");

    Player actor = sessionRegistry.get(event.sessionId());
    // WebSocketSession session = actor.getSession();
    // WebSocketSession session = sessionRegistry.get(event.sessionId());
    // PlayerActor actor = worldManager.getPlayer(session);

    // 取得目前的重試次數
    // int retryCount = (int) Optional
    // .ofNullable(session.getAttributes().get(MudKeys.AUTH_RETRY_COUNT_KEY)).orElse(0);

    // String errorReason = validatePassword(event.input());

    // if (errorReason != null) {
    // retryCount++;
    // session.getAttributes().put(MudKeys.AUTH_RETRY_COUNT_KEY, retryCount);

    // if (retryCount >= MAX_AUTH_RETRIES) {
    // log.warn("連線 {} 註冊密碼失敗次數過多 ({})，強制中斷。最後輸入: {}", event.sessionId(), retryCount,
    // event.input());
    // actor.reply("嘗試次數過多，連線即將關閉。");
    // session.close();
    // return;
    // }

    // log.info("連線 {} 註冊密碼失敗: {} (剩餘次數: {})", event.sessionId(), errorReason,
    // MAX_AUTH_RETRIES - retryCount);
    // actor.reply(errorReason + " (剩餘嘗試次數: " + (MAX_AUTH_RETRIES - retryCount) + ")");
    // return;
    // }

    // 驗證成功，移除計數
    // session.getAttributes().remove(MudKeys.AUTH_RETRY_COUNT_KEY);

    // 暫存密碼
    // actor.setTempPassword(event.input());

    // 新增玩家帳密


    // String msg = "請輸入密碼:";
    String msg = "【註冊流程】\r\n註冊完成，請重新登入。";
    // 告訴前端：切換輸入模式為密碼 (透過自定義協議，例如 JSON {type: "PWD_MODE"})
    actor.reply(msg);

    actor.setConnectionState(ConnectionState.CONNECTED);
  }

  @EventListener
  public void onLogin(SystemEvent.Login event) {
    log.info("onLogin");
  }

  @EventListener
  public void onAuthenticate(SystemEvent.Authenticate event) throws IOException {
    log.info("onAuthenticate");
    Player actor = sessionRegistry.get(event.sessionId());
    // WebSocketSession session = actor.getSession();
    // WebSocketSession session = sessionRegistry.get(event.sessionId());
    // PlayerActor actor = worldManager.getPlayer(session);

    // if ("new".equalsIgnoreCase(event.input())) {
    // doRegister(session, actor);
    // } else {
    // doLoginUsername(session, actor, event);
    // }
  }

  @EventListener
  public void onLogout(SystemEvent.Logout event) {}


  private void doRegister(WebSocketSession session, Player actor) throws IOException {
    log.info("doRegister");

    String msg = "【註冊流程】\r\n只能包含英文字母（不分大小寫），不允許數字、空格或特殊符號。\r\n長度必須在 4 到 20 個字元之間。\r\n請輸入您想使用的帳號名稱:";
    // 告訴前端：切換輸入模式為帳號 (透過自定義協議，例如 JSON {type: "USER_MODE"})
    String json = objectMapper.writeValueAsString(Map.of("type", "USER_MODE", "content", msg));
    session.sendMessage(new TextMessage(json));

    session.getAttributes().put(MudKeys.AUTH_RETRY_COUNT_KEY, 0); // 重置計數
    actor.setConnectionState(ConnectionState.CREATING_USERNAME);
  }

  private void doLoginUsername(WebSocketSession session, Player actor,
      SystemEvent.Authenticate event) throws IOException {
    log.info("doLoginUsername");

    // 取得目前的重試次數
    int retryCount = (int) Optional
        .ofNullable(session.getAttributes().get(MudKeys.AUTH_RETRY_COUNT_KEY)).orElse(0);

    String errorReason = validateUsername(event.input());

    // 如果格式正確，檢查資料庫是否存在該帳號
    if (errorReason == null && !userRepository.existsByUsername(event.input())) {
      errorReason = "帳號不存在。";
    }

    if (errorReason != null) {
      retryCount++;
      session.getAttributes().put(MudKeys.AUTH_RETRY_COUNT_KEY, retryCount);

      if (retryCount >= MAX_AUTH_RETRIES) {
        log.warn("連線 {} 驗證失敗次數過多 ({})，強制中斷。最後輸入: {}", event.sessionId(), retryCount, event.input());
        actor.reply("嘗試次數過多，連線即將關閉。");
        session.close();
        return;
      }

      log.info("連線 {} 驗證失敗: {} (剩餘次數: {})", event.sessionId(), errorReason,
          MAX_AUTH_RETRIES - retryCount);
      actor.reply(errorReason + " (剩餘嘗試次數: " + (MAX_AUTH_RETRIES - retryCount) + ")");
      return;
    }

    // 驗證成功，重置計數
    session.getAttributes().put(MudKeys.AUTH_RETRY_COUNT_KEY, 0);

    // 暫存帳號
    // actor.setTempUsername(event.input());

    String msg = "請輸入密碼:";
    // 告訴前端：切換輸入模式為密碼 (透過自定義協議，例如 JSON {type: "PWD_MODE"})
    String json = objectMapper.writeValueAsString(Map.of("type", "PWD_MODE", "content", msg));
    session.sendMessage(new TextMessage(json));

    actor.setConnectionState(ConnectionState.ENTERING_PASSWORD);
  }


  /**
   * 驗證用戶名是否合法
   *
   * @return 錯誤訊息，若合法則回傳 null
   */
  private String validateUsername(String input) {
    if (input == null || input.isEmpty()) {
      return "帳號名稱不能為空。";
    }
    if (!input.matches("^[a-zA-Z]+$")) {
      log.info("{}", input);
      return "帳號名稱只能包含英文字母（不分大小寫），不允許數字、空格或特殊符號。";
    }
    if (input.length() < 4 || input.length() > 20) {
      return "帳號名稱長度必須在 4 到 20 個字元之間。";
    }
    if (reservedWords.contains(input.toLowerCase())) {
      return "「" + input + "」是系統保留字或指令，請選擇其他名稱。";
    }
    return null;
  }

  /**
   * 驗證密碼是否合法
   *
   * @param input 輸入的密碼
   * @return 錯誤訊息，若合法則回傳 null
   */
  private String validatePassword(String input) {
    if (input == null || input.isEmpty()) {
      return "密碼不能為空。";
    }
    if (!input.matches("^[a-zA-Z0-9]+$")) {
      return "密碼只能包含英文字母與數字。";
    }
    if (input.length() < 6 || input.length() > 32) {
      return "密碼長度必須在 6 到 32 個字元之間。";
    }
    return null;
  }

}
