package com.example.htmlmud.protocol;

import java.io.IOException;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketOutput implements MessageOutput {
  private final WebSocketSession session;
  private final ObjectMapper objectMapper;

  public WebSocketOutput(WebSocketSession session, ObjectMapper objectMapper) {
    this.session = session;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sendJson(Object payload) {
    try {
      session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    } catch (IOException e) {
      log.error("sendJson {}", e.getMessage(), e);
    }
  }

  @Override
  public void close() {
    if (session == null || !session.isOpen()) {
      return;
    }

    try {
      session.close();
    } catch (IOException e) {
      log.error("WebSocketOutput close {}", e.getMessage(), e);
    }
  }

  @Override
  public WebSocketSession getSession() {
    return session;
  }
}
