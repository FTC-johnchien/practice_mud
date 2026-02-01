package com.example.htmlmud.infra.server;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.actor.impl.Player;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionRegistry {

  private final Map<String, Player> sessions = new ConcurrentHashMap<>();

  public void register(WebSocketSession session, Player actor) {
    sessions.put(session.getId(), actor);
  }

  public Player remove(String sessionId) {
    return sessions.remove(sessionId);
  }

  public Player get(String sessionId) {
    return sessions.get(sessionId);
  }

  // 額外功能：踢掉某個 Session (例如重複登入踢人)
  // 或是統計當前連線數 (含 Guest)
  public int getConnectionCount() {
    return sessions.size();
  }

}
