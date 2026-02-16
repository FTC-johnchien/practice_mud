package com.example.htmlmud.application.listener;

import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.event.DomainEvent.SessionEvent;
import com.example.htmlmud.infra.server.SessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SessionEventListener {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private SessionRegistry sessionRegistry;

  @EventListener
  public void onEstablished(SessionEvent.Established event) throws IOException {
    log.info("系統收到連線建立事件: {}", event.sessionId());

    Player actor = sessionRegistry.get(event.sessionId());
    // WebSocketSession session = actor.getSession();
    // WebSocketSession session = sessionRegistry.get(event.sessionId());

    // String welcomeMsg = """
    // \u001B[36m
    // ==========================================
    // 歡迎來到 HTML MUD (Java 25 Edition)
    // ==========================================
    // \u001B[0m
    // 請輸入以下指令開始:
    // 1. 註冊: \u001B[33mregister <帳號> <密碼>\u001B[0m
    // 2. 登入: \u001B[33mlogin <帳號> <密碼>\u001B[0m
    // """;
    String welcomeMsg = "歡迎來到 HTML MUD 世界！\r\n請輸入帳號 (或輸入 new 註冊)：";
    // if (session != null && session.isOpen()) {
    // String json = objectMapper.writeValueAsString(Map.of("type", "TEXT", "content", welcomeMsg));

    // session.sendMessage(new TextMessage(json));
    // }
  }

  @EventListener
  public void onMessageReceived(SessionEvent.MessageReceived event) {
    System.out.println("onMessageReceived");
  }

  @EventListener
  public void onClosed(SessionEvent.Closed event) {
    log.info("系統收到連線關閉事件: {} CloseStatus:{}", event.sessionId(), event.reason());
  }

}
