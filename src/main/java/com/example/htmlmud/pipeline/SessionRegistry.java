package com.example.htmlmud.pipeline;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.actor.impl.Player;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 網路層會話註冊表 (SessionRegistry)
 * 支援單機單一主角模型 (Singleton Protagonist) 與多開會話安全接管
 */
@Component
public class SessionRegistry {

  private final Map<String, Player> sessions = new ConcurrentHashMap<>();
  private volatile Player protagonist;
  private volatile WebSocketSession activeSession;

  public void register(WebSocketSession session, Player actor) {
    sessions.put(session.getId(), actor);
    this.activeSession = session;
    this.protagonist = actor;
  }

  public Player remove(String sessionId) {
    Player removed = sessions.remove(sessionId);
    if (activeSession != null && activeSession.getId().equals(sessionId)) {
      activeSession = null;
    }
    return removed;
  }

  public Player get(String sessionId) {
    if (sessionId == null) {
      return protagonist;
    }
    Player player = sessions.get(sessionId);
    if (player != null) {
      return player;
    }
    return protagonist;
  }

  public Player getProtagonist() {
    return protagonist;
  }

  public void setProtagonist(Player protagonist) {
    this.protagonist = protagonist;
  }

  public WebSocketSession getActiveSession() {
    return activeSession;
  }

  public void setActiveSession(WebSocketSession activeSession) {
    this.activeSession = activeSession;
  }

  public boolean hasActiveProtagonist() {
    return protagonist != null && protagonist.isRunning();
  }

  public int getConnectionCount() {
    return sessions.size();
  }

  public void clear() {
    sessions.clear();
    protagonist = null;
    activeSession = null;
  }
}
