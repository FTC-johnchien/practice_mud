package com.example.htmlmud.infra.server;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.example.htmlmud.application.service.CommandQueueService;
import com.example.htmlmud.application.service.PlayerService;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.protocol.GameCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MudWebSocketHandler extends TextWebSocketHandler {
  private final PlayerService playerService;
  private final WorldManager worldManager;
  private final SessionRegistry sessionRegistry;
  private final CommandQueueService commandQueueService;

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
      Player actor = Player.createGuest(concurrentSession, worldManager, playerService);

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
    try {
      Player actor = sessionRegistry.get(session.getId());
      if (actor != null) {
        // C. 解析指令 (JSON -> Record)
        GameCommand cmd =
            playerService.getObjectMapper().readValue(message.getPayload(), GameCommand.class);

        // D. 統一丟進佇列，讓 ServerEngine 決定何時執行
        commandQueueService.push(actor, cmd);
      } else {
        log.warn("收到訊息但找不到 Actor，關閉連線: {}", session.getId());
        session.close();
      }
    } catch (Exception e) {
      log.error("訊息處理錯誤: {}", e.getMessage());
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

    // 從 Registry 移除並取得 Actor
    Player actor = sessionRegistry.remove(session.getId());

    if (actor != null) {
      log.info("連線關閉: {} (Actor: {})", session.getId(), actor.getId());

      // 通知 Actor 執行清理邏輯
      // (如果是 Guest 則直接停止，如果是正式玩家則觸發存檔與從 WorldManager 移除)
      actor.handleDisconnect();
    }
  }
}
