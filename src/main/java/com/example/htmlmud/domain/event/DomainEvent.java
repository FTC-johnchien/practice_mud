package com.example.htmlmud.domain.event;

import java.time.Instant;
import com.example.htmlmud.domain.actor.PlayerActor;

/**
 * 領域事件的根介面 使用 sealed 限制只有特定的 record 可以實作它
 */
public sealed interface DomainEvent permits DomainEvent.SessionEvent, DomainEvent.SystemEvent,
    DomainEvent.WorldEvent, PlayerEvents, MobEvents {

  // 所有事件都必須有的 metadata
  Instant occurredOn();

  // 預設實作，方便取得當下時間
  static Instant now() {
    return Instant.now();
  }

  /**
   * 連線相關事件
   */
  sealed interface SessionEvent extends DomainEvent
      permits SessionEvent.Established, SessionEvent.MessageReceived, SessionEvent.Closed {

    String sessionId();

    record Established(String sessionId, Instant occurredOn) implements SessionEvent {
    }

    record MessageReceived(String sessionId, String message, Instant occurredOn)
        implements SessionEvent {
    }

    record Closed(String sessionId, String reason, int statusCode, Instant occurredOn)
        implements SessionEvent {
    }
  }

  /**
   * 系統指令與認證事件
   */
  sealed interface SystemEvent extends DomainEvent permits SystemEvent.Register, SystemEvent.Login,
      SystemEvent.Authenticate, SystemEvent.Logout {

    String sessionId();

    record Register(String sessionId, String[] words, Instant occurredOn) implements SystemEvent {
    }

    record Login(String sessionId, String[] words, Instant occurredOn) implements SystemEvent {
    }

    record Authenticate(String sessionId, String[] words, Instant occurredOn)
        implements SystemEvent {
    }

    record Logout(String sessionId, PlayerActor player, Instant occurredOn) implements SystemEvent {
    }
  }

  /**
   * 遊戲世界運行事件
   */
  sealed interface WorldEvent extends DomainEvent permits WorldEvent.Tick {

    record Tick(long gameTickTime, Instant occurredOn) implements WorldEvent {
    }
  }

}
