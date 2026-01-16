package com.example.htmlmud.service.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.context.MudKeys;
import com.example.htmlmud.domain.event.DomainEvent.SystemEvent;
import com.example.htmlmud.infra.server.SessionRegistry;
import com.example.htmlmud.protocol.ConnectionState;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SystemEventListener {

  @Autowired
  private SessionRegistry sessionRegistry;

  @EventListener
  public void onRegister(SystemEvent.Register event) {

  }

  @EventListener
  public void onLogin(SystemEvent.Login event) {}

  @EventListener
  public void onAuthenticate(SystemEvent.Authenticate event) {
    WebSocketSession session = sessionRegistry.get(event.sessionId());
    String input = event.words()[0];


    if ("new".equalsIgnoreCase(input)) {
      session.sendMessage(new TextMessage("請輸入您想使用的帳號名稱:"));
      session.getAttributes().put(MudKeys.CONNECTION_STATE, ConnectionState.REGISTER_USERNAME);
    } else {
      // 暫存帳號
      session.getAttributes().put("temp_user", input);
      session.sendMessage(new TextMessage("請輸入密碼:"));
      // 告訴前端：切換輸入模式為密碼 (透過自定義協議，例如 JSON {type: "PWD_MODE"})
      session.getAttributes().put("state", ConnectionState.REGISTER_PASSWORD);
    }
  }

  @EventListener
  public void onLogout(SystemEvent.Logout event) {}

}
