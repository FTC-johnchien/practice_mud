package com.example.htmlmud.infra.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.example.htmlmud.domain.actor.PlayerActor;
import com.example.htmlmud.domain.context.NetworkContext;
import com.example.htmlmud.protocol.ActorMessage;
import com.example.htmlmud.protocol.GameCommand;
import com.example.htmlmud.service.PlayerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MudWebSocketHandler extends TextWebSocketHandler {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PlayerService playerService;

  // 簡單的 Actor 註冊表 (未來可以用 Caffeine Cache 或 Service 管理)
  private final Map<Long, PlayerActor> activeActors = new ConcurrentHashMap<>();

  public MudWebSocketHandler(PlayerService playerService) {
    this.playerService = playerService;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    // 1. 連線建立，啟動 Actor
    Long dbId = System.currentTimeMillis();
    var actor = new PlayerActor(dbId, session, null, objectMapper, playerService);
    actor.start();
    activeActors.put(dbId, actor);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    // 直接問它是不是虛擬的 印出詳細資訊
    // boolean isVirtual = Thread.currentThread().isVirtual();
    // log.info("--- [WS Handler] ---");
    // log.info("Thread Name : {}", current.getName());
    // log.info("Is Virtual : {}", isVirtual);
    // log.info("String Rep : {}", current.toString());

    // 2. 收到訊息，使用 ScopedValue 綁定 TraceID (用於 Handler 內的 Log)
    String traceId = UUID.randomUUID().toString().substring(0, 8);
    try {
      log.info("[Trace:{}] Payload: {}", traceId, message.getPayload());

      // 解析 JSON
      GameCommand cmd = objectMapper.readValue(message.getPayload(), GameCommand.class);

      // 投遞給 Actor
      PlayerActor actor = activeActors.get(session.getId());
      if (actor != null) {
        ActorMessage envelope = new ActorMessage(traceId, cmd);
        actor.send(envelope);
      } else {
        log.warn("Actor not found for session: {}", session.getId());
      }
    } catch (Exception e) {
      log.error("[Trace:{}] Error processing message", NetworkContext.TRACE_ID.get(), e);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    PlayerActor actor = activeActors.remove(session.getId());
    if (actor != null) {
      actor.stop();
    }
    log.info("Connection closed: {}", session.getId());
  }

}
