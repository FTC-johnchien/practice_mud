package com.example.htmlmud.domain.event;

import java.time.Instant;

public sealed interface PlayerEvents extends DomainEvent
    permits PlayerEvents.LoggedIn, PlayerEvents.LoggedOut, PlayerEvents.LevelUp {

  // 1. 玩家登入事件
  record LoggedIn(String playerId, String username, String ipAddress, Instant occurredOn)
      implements PlayerEvents {
    public LoggedIn(String playerId, String username, String ipAddress) {
      this(playerId, username, ipAddress, DomainEvent.now());
    }
  }

  // 2. 玩家登出事件
  record LoggedOut(String playerId, Instant occurredOn) implements PlayerEvents {
    public LoggedOut(String playerId) {
      this(playerId, DomainEvent.now());
    }
  }

  // 3. 玩家升級
  record LevelUp(String playerId, int newLevel, Instant occurredOn) implements PlayerEvents {
    public LevelUp(String playerId, int newLevel) {
      this(playerId, newLevel, DomainEvent.now());
    }
  }
}
