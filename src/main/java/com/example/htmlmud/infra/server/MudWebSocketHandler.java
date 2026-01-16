package com.example.htmlmud.infra.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.example.htmlmud.domain.actor.PlayerActor;
import com.example.htmlmud.domain.actor.RoomActor;
import com.example.htmlmud.domain.context.NetworkContext;
import com.example.htmlmud.domain.event.PlayerLoginEvent;
import com.example.htmlmud.domain.model.json.LivingState;
import com.example.htmlmud.infra.persistence.entity.PlayerEntity;
import com.example.htmlmud.protocol.ActorMessage;
import com.example.htmlmud.protocol.GameCommand;
import com.example.htmlmud.protocol.RoomMessage;
import com.example.htmlmud.service.PlayerService;
import com.example.htmlmud.service.auth.AuthService;
import com.example.htmlmud.service.world.WorldManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MudWebSocketHandler extends TextWebSocketHandler {

  @Autowired
  private AuthService authService;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  public MudWebSocketHandler() {
    // this.objectMapper = objectMapper;
    // this.playerService = playerService;
    // this.worldManager = worldManager;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    log.info("玩家連線成功: {}", session.getId());
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

      // // 1. 驗證帳密
      // PlayerEntity playerEntity = authService.login(parts[1], parts[2]);

      // // 2. 發布事件！ (我不負責初始化 Actor，我只負責大喊"他登入了")
      // eventPublisher.publishEvent(new PlayerLoginEvent(this, playerEntity, session));

      // 投遞給 Actor
      // PlayerActor actor = activeActors.get(session.getId());
      // log.info("", actor.getName());
      // GameObjectId objectId = GameObjectId.mob(1);
      LivingState state = new LivingState();
      state.name = "john";
      state.displayName = "約翰2";
      state.hp = 100;
      state.maxHp = 100;
      PlayerActor actor = new PlayerActor("2", session, state, objectMapper);
      if (actor != null) {
        int startRoomId = 1001;
        actor.setCurrentRoomId(startRoomId);
        log.info("1 -----------------------------------------------------");
        RoomActor room = worldManager.getRoomActor(startRoomId);
        log.info("{}", room.getTemplate().description());
        CompletableFuture<String> enterFuture = new CompletableFuture<>();

        room.send(new RoomMessage.Look(actor.getId(), enterFuture));
        // room.send(new RoomMessage.PlayerEnter, enterFuture));

        // 進入完成後，自動 Look 讓玩家知道自己在哪
        // ActorMessage envelope2 = new ActorMessage(traceId, cmd);
        // new RoomMessage.Look(actor.getId(), null);
        enterFuture.thenRun(() -> {
          // 這裡模擬發送 look 指令
          try {
            log.info("{}", enterFuture.get());
            actor.sendText(enterFuture.get());
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          } ;
          // actor.send(new RoomMessage.Look(actor.getId(), null));
        });

        log.info("2 -----------------------------------------------------");
        // 將 Actor 存起來 (Session Attributes 或 Map) 以便後續使用
        session.getAttributes().put("actor", actor);

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
    log.info("Connection closed: {}", session.getId());
  }

}
