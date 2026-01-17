package com.example.htmlmud.infra.server;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.example.htmlmud.domain.actor.PlayerActor;
import com.example.htmlmud.domain.actor.RoomActor;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.context.MudKeys;
import com.example.htmlmud.domain.event.DomainEvent.SessionEvent;
import com.example.htmlmud.domain.event.DomainEvent.SystemEvent;
import com.example.htmlmud.domain.model.json.LivingState;
import com.example.htmlmud.protocol.ActorMessage;
import com.example.htmlmud.protocol.ConnectionState;
import com.example.htmlmud.protocol.GameCommand;
import com.example.htmlmud.protocol.RoomMessage;
import com.example.htmlmud.service.PlayerService;
import com.example.htmlmud.service.auth.AuthService;
import com.example.htmlmud.service.world.WorldManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MudWebSocketHandler extends TextWebSocketHandler {

  private static final String DELIMITER_REGEX = "[ \t,.:;?!\"']+";

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  @Autowired
  private SessionRegistry sessionRegistry;

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    String sessionId = session.getId();

    // 放入一個空的Actor維持一致性 (Guest階段)
    PlayerActor actor = new PlayerActor(sessionId, new LivingState(), session, objectMapper);
    session.getAttributes().put(MudKeys.PLAYER_ID, sessionId);
    sessionRegistry.register(session);

    worldManager.addPlayer(actor);

    eventPublisher.publishEvent(new SessionEvent.Established(sessionId, Instant.now()));
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    String playerId = (String) session.getAttributes().get(MudKeys.PLAYER_ID);
    if (playerId == null) {
      session.close(CloseStatus.NOT_ACCEPTABLE);
      return;
    }
    PlayerActor actor = worldManager.getPlayer(playerId);
    if (actor == null) {
      session.close(CloseStatus.NOT_ACCEPTABLE);
      return;
    }

    // 解析 JSON
    GameCommand cmd = objectMapper.readValue(message.getPayload(), GameCommand.class);
    // 1. 使用 Pattern Matching 直接取出 text，避免使用 cmd.toString()
    if (!(cmd instanceof GameCommand.Input(var text))) {
      return;
    }

    String[] input = parseInput(text);

    // 這次的 trace id
    String traceId = UUID.randomUUID().toString().substring(0, 8);

    // 在處理開始前
    MDC.put("traceId", traceId);
    try {
      ScopedValue.where(MudContext.TRACE_ID, traceId).where(MudContext.CURRENT_PLAYER, actor)
          .run(() -> {
            switch (actor.getConnectionState()) {
              case CONNECTED -> eventPublisher.publishEvent(
                  new SystemEvent.Authenticate(session.getId(), input[0], Instant.now()));
              case REGISTER_USERNAME -> eventPublisher.publishEvent(
                  new SystemEvent.RegisterUsername(session.getId(), input[0], Instant.now()));
              case REGISTER_PASSWORD -> eventPublisher.publishEvent(
                  new SystemEvent.RegisterPassword(session.getId(), input[0], Instant.now()));
              case LOGIN_PASSWORD -> eventPublisher
                  .publishEvent(new SystemEvent.Login(session.getId(), input[0], Instant.now()));
              case IN_GAME -> actor.send(new ActorMessage(traceId, cmd));
            }
          });
    } finally {
      // 務必在結束後清除，避免執行緒池污染
      MDC.clear();
    }



    // 收到訊息，使用 ScopedValue 綁定 TraceID (用於 Handler 內的 Log)

    // ScopedValue.where(MudContext.TRACE_ID, traceId).run(() -> {

    // ScopedValue.where(MudContext.TRACE_ID, traceId)
    /*
     * // 1. 嘗試從 Session 取得 Actor PlayerActor actor = (PlayerActor)
     * session.getAttributes().get(MudKeys.PLAYER_ID);
     *
     * if (actor != null) { // [情境 A]: 已登入 -> 轉發給 Actor (Game Logic) actor.send(input); } else { //
     * [情境 B]: 未登入 -> 處理認證指令 (Auth Logic) handleGuestCommand(session, input); }
     *
     * try { log.info("[Trace:{}] Payload: {}", traceId, message.getPayload());
     *
     * // 解析 JSON GameCommand cmd = objectMapper.readValue(message.getPayload(), GameCommand.class);
     *
     * // 2. 檢查 Session 裡面有沒有綁定 Player ID String playerId = (String)
     * session.getAttributes().get(MudKeys.PLAYER_ID);
     *
     * PlayerActor actor = null; if (playerId != null) { // A. 如果已登入：直接從世界管理器找人 actor =
     * worldManager.getPlayer(playerId); }
     *
     * // 3. 考慮斷線重連的狀態 處理特殊情況：如果是登入指令 if (actor == null && cmd instanceof GameCommand.Login(var
     * user, var pass)) { // 執行登入邏輯 (驗證帳密) // 假設驗證成功，並取得/建立了 Actor actor = loginService.login(user,
     * pass, session);
     *
     * // 【關鍵】把 ID 寫入 Session，下次 Input 就知道他是誰了 session.getAttributes().put(SessionKeys.PLAYER_ID,
     * actor.getObjectId().id());
     *
     * // 【關鍵】把 Session 塞給 Actor，讓他能回話 actor.attachSession(session); }
     *
     * // 4. 投遞訊息 if (actor != null) { // 這裡我們把 TraceId 和指令包進去 // 注意：這時候還沒建立 ScopedValue，是進到
     * Actor.handleMessage 才建立 actor.send(new ActorMessage(traceId, cmd)); } else { // 未登入且輸入非登入指令
     * session.sendMessage(new TextMessage("請先登入 (LOGIN user pass)")); }
     *
     *
     *
     * // // 1. 驗證帳密 // PlayerEntity playerEntity = authService.login(parts[1], parts[2]);
     *
     * // // 2. 發布事件！ (我不負責初始化 Actor，我只負責大喊"他登入了") // eventPublisher.publishEvent(new
     * PlayerLoginEvent(this, playerEntity, session));
     *
     * // 投遞給 Actor // PlayerActor actor = activeActors.get(session.getId()); // log.info("",
     * actor.getName()); // GameObjectId objectId = GameObjectId.mob(1);
     *
     *
     * LivingState state = new LivingState(); state.name = "john"; state.displayName = "約翰2";
     * state.hp = 100; state.maxHp = 100; PlayerActor actor = new PlayerActor("2", session, state,
     * objectMapper); if (actor != null) { int startRoomId = 1001;
     * actor.setCurrentRoomId(startRoomId);
     * log.info("1 -----------------------------------------------------"); RoomActor room =
     * worldManager.getRoomActor(startRoomId); log.info("{}", room.getTemplate().description());
     * CompletableFuture<String> enterFuture = new CompletableFuture<>();
     *
     * room.send(new RoomMessage.Look(actor.getId(), enterFuture)); // room.send(new
     * RoomMessage.PlayerEnter, enterFuture));
     *
     * // 進入完成後，自動 Look 讓玩家知道自己在哪 // ActorMessage envelope2 = new ActorMessage(traceId, cmd); // new
     * RoomMessage.Look(actor.getId(), null); enterFuture.thenRun(() -> { // 這裡模擬發送 look 指令 try {
     * log.info("{}", enterFuture.get()); actor.sendText(enterFuture.get()); } catch
     * (InterruptedException e) { // TODO Auto-generated catch block e.printStackTrace(); } catch
     * (ExecutionException e) { // TODO Auto-generated catch block e.printStackTrace(); } ; //
     * actor.send(new RoomMessage.Look(actor.getId(), null)); });
     *
     * log.info("2 -----------------------------------------------------"); // 將 Actor 存起來 (Session
     * Attributes 或 Map) 以便後續使用 session.getAttributes().put("actor", actor);
     *
     * ActorMessage envelope = new ActorMessage(traceId, cmd); actor.send(envelope); } else {
     * log.warn("Actor not found for session: {}", session.getId()); } } catch (Exception e) {
     * log.error("[Trace:{}] Error processing message", MudContext.TRACE_ID.get(), e); }
     */
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    sessionRegistry.unregister(session.getId());
    eventPublisher.publishEvent(new SessionEvent.Closed(session.getId(), status.getReason(),
        status.getCode(), Instant.now()));
  }

  /**
   * 將輸入字串切割成單字列表，忽略大小寫及空白
   *
   * @param input 輸入字串
   * @return 單字列表
   */
  private String[] parseInput(String input) {
    if (input == null || input.trim().isEmpty()) {
      return new String[0];
    }

    String trimInput = input.trim();

    String[] words = Arrays.stream(trimInput.split(DELIMITER_REGEX)).filter(word -> !word.isEmpty())
        .toArray(String[]::new);

    return words;
  }

}
