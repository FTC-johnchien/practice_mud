package com.example.htmlmud.domain.event;

import java.time.Clock;
import org.springframework.context.ApplicationEvent;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.infra.persistence.entity.PlayerEntity;

// 繼承 ApplicationEvent 是 Spring 的標準做法
public class PlayerLoginEvent extends ApplicationEvent {

  private final PlayerEntity playerEntity;
  private final WebSocketSession session;

  public PlayerLoginEvent(Object source, PlayerEntity playerEntity, WebSocketSession session) {
    super(source, Clock.systemUTC());
    this.playerEntity = playerEntity;
    this.session = session;
  }

  public PlayerEntity getPlayerEntity() {
    return playerEntity;
  }

  public WebSocketSession getSession() {
    return session;
  }
}
