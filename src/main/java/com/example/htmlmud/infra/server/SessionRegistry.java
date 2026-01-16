package com.example.htmlmud.infra.server;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionRegistry {

  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

  public void register(WebSocketSession session) {
    sessions.put(session.getId(), session);
  }

  public void unregister(String sessionId) {
    sessions.remove(sessionId);
  }

  public WebSocketSession get(String sessionId) {
    return sessions.get(sessionId);
  }
}
