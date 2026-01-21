package com.example.htmlmud.infra.server;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionRegistry {

  private final Map<String, PlayerActor> sessions = new ConcurrentHashMap<>();

  public void register(WebSocketSession session, PlayerActor actor) {
    sessions.put(session.getId(), actor);
  }

  public PlayerActor remove(String sessionId) {
    return sessions.remove(sessionId);
  }

  public PlayerActor get(String sessionId) {
    return sessions.get(sessionId);
  }

  // 額外功能：踢掉某個 Session (例如重複登入踢人)
  // 或是統計當前連線數 (含 Guest)
  public int getConnectionCount() {
    return sessions.size();
  }

}
