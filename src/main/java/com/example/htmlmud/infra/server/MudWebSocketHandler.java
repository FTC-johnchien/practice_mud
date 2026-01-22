package com.example.htmlmud.infra.server;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.protocol.ActorMessage;
import com.example.htmlmud.protocol.GameCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MudWebSocketHandler extends TextWebSocketHandler {
  private final GameServices gameServices;
  private final WorldManager worldManager;
  private final SessionRegistry sessionRegistry;

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    // 包裝 Session
    // 這會自動加上 Lock，確保同時寫入時會排隊，不會噴出 IllegalStateException
    // 參數說明：session, sendTimeLimit(ms), bufferSizeLimit(bytes)
    WebSocketSession concurrentSession =
        new ConcurrentWebSocketSessionDecorator(session, 1000, 64 * 1024);

    try {

      // Guest階段 使用工廠方法建立 Guest Actor (ID=0)
      // 將必要的 Service 注入給 Actor，讓 Actor 擁有處理業務的能力
      PlayerActor actor = PlayerActor.createGuest(concurrentSession, worldManager, gameServices);

      // 啟動 Actor 的虛擬執行緒 (Virtual Thread)
      actor.start();

      // 註冊到網路層 SessionRegistry
      sessionRegistry.register(concurrentSession, actor);

      log.info("連線建立: {} (Guest Actor Created)", concurrentSession.getId());
      // eventPublisher.publishEvent(new SessionEvent.Established(session.getId(), Instant.now()));
    } catch (Exception e) {
      log.error("連線初始化失敗", e);
      try {
        concurrentSession.close();
      } catch (Exception ignored) {
      }
    }
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

    // A. 產生 Trace ID (所有 Log 追蹤的源頭)
    // 使用短 UUID 方便閱讀，實務上可用完整的 UUID
    String traceId = UUID.randomUUID().toString().substring(0, 8);

    try {
      PlayerActor actor = sessionRegistry.get(session.getId());
      if (actor != null) {
        // C. 解析指令 (JSON -> Record)
        GameCommand cmd =
            gameServices.objectMapper().readValue(message.getPayload(), GameCommand.class);

        // D. 裝入信封並投遞
        // 這裡不綁定 ScopedValue，因為要跨執行緒傳遞
        actor.send(new ActorMessage.Command(traceId, cmd));
      } else {
        // 找不到 Actor，通常代表連線異常或已被踢除
        log.warn("[{}] 收到訊息但找不到 Actor，關閉連線: {}", traceId, session.getId());
        session.close();
      }
    } catch (Exception e) {
      // JSON 解析失敗或其他錯誤
      log.error("[{}] 訊息處理錯誤: {}", traceId, e.getMessage());
      // 選擇性：回傳錯誤訊息給 Client
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

    // 從 Registry 移除並取得 Actor
    PlayerActor actor = sessionRegistry.remove(session.getId());

    if (actor != null) {
      log.info("連線關閉: {} (Actor: {})", session.getId(), actor.getId());

      // 通知 Actor 執行清理邏輯
      // (如果是 Guest 則直接停止，如果是正式玩家則觸發存檔與從 WorldManager 移除)
      actor.handleDisconnect();
    }
  }
}
