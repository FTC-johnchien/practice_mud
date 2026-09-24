package com.example.htmlmud.infra.server;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.example.htmlmud.application.dto.GameRequest;
import com.example.htmlmud.application.service.GameCommandService;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.WorldManager;
import com.example.htmlmud.domain.port.ClientSessionManagerPort;
import com.example.htmlmud.infra.monitor.GameMetrics;
import com.example.htmlmud.protocol.WebSocketOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MudWebSocketHandler extends TextWebSocketHandler implements ClientSessionManagerPort {
  private final PlayerService playerService;
  private final WorldManager worldManager;
  private final SessionRegistry sessionRegistry;
  private final GameMetrics gameMetrics;
  private final GameCommandService gameCommandService;
  private final ObjectMapper objectMapper;
  private final com.example.htmlmud.domain.save.service.SaveGameService saveGameService;

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    try {
      String initialName = "玄靈子";
      try {
        var slots = saveGameService.listSaveSlots();
        for (var slot : slots) {
          if (!slot.isEmpty() && slot.getProtagonistName() != null && !slot.getProtagonistName().isBlank()) {
            initialName = slot.getProtagonistName();
            break;
          }
        }
      } catch (Exception ignored) {}

      // 單機/Web模式：建立玩家 Actor，為每個 WebSocket session 分配唯一穩定的 playerId
      String playerId = "p-" + session.getId();
      Player self = Player.createSinglePlayer(new WebSocketOutput(session, objectMapper), worldManager,
          playerService, initialName, playerId);

      // 啟動 Actor 的虛擬執行緒 (Virtual Thread)
      self.start();

      // 註冊到網路層 SessionRegistry
      sessionRegistry.register(session, self);

      log.info("單機連線建立 (Single Player Actor Created): {}", session.getId());
      // eventPublisher.publishEvent(new SessionEvent.Established(session.getId(), Instant.now()));
    } catch (Exception e) {
      log.error("連線初始化失敗", e);
      try {
        session.close();
      } catch (Exception ignored) {
      }
    }
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    Player player = sessionRegistry.get(session.getId());
    if (player != null) {

      // 檢查玩家是否可以動作
      if (!player.isValid() || player.getGcdEndTimestamp() > System.currentTimeMillis()) {
        player.reply("你目前無法動作!");
        return;
      }

      gameCommandService.execute(new GameRequest(player, message.getPayload(), "WEB"));

      // 增加指令計數 (來自玩家的輸入)
      gameMetrics.incrementPlayerCommand();
    } else {
      // 找不到 Actor，通常代表連線異常或已被踢除
      log.warn("收到訊息但找不到 Actor，關閉連線: {}", session.getId());
      session.close();
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    log.info("afterConnectionClosed");
    // 從 Registry 移除並取得 Actor
    Player actor = sessionRegistry.remove(session.getId());

    if (actor != null) {
      log.info("連線關閉: {} (Actor: {})", session.getId(), actor.getId());

      // 通知 Actor 執行清理邏輯
      // (如果是 Guest 則直接停止，如果是正式玩家則觸發存檔與從 WorldManager 移除)
      actor.disconnect();
    }
  }

  // --- 關鍵方法：切換負責人 (Handover) ---
  // 這個方法會被 LoginService 呼叫
  public void promoteToPlayer(WebSocketSession newSession, Player player) {

    // 1. 取得登入產生的 Guest Actor
    Player oldGuest = sessionRegistry.get(newSession.getId());

    // 2. 讓 Player 接管 Session
    player.setOutput(new WebSocketOutput(newSession, objectMapper));

    // 3. 更新 sessionRegistry，之後的訊息直接灌給 Player
    sessionRegistry.register(newSession, player);

    // 4. 【賜死 Guest】 舊的 Guest 任務完成，請他下台
    oldGuest.stop(); // 停止 Guest 的 VT，釋放資源

    log.info("Session 權限已移交給玩家: {}", player.getName());
  }

  @Override
  public void promoteToPlayer(Object rawSession, Player player) {
    if (rawSession instanceof WebSocketSession session) {
      promoteToPlayer(session, player);
    }
  }
}
