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
import com.example.htmlmud.infra.monitor.GameMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MudWebSocketHandler extends TextWebSocketHandler {
  private final PlayerService playerService;
  private final WorldManager worldManager;
  private final SessionRegistry sessionRegistry;
  private final GameMetrics gameMetrics;
  private final GameCommandService gameCommandService;


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
    Player player = sessionRegistry.get(session.getId());
    if (player != null) {

      // 檢查玩家是否可以動作
      if (!player.isValid() || player.getGcdEndTimestamp() > System.currentTimeMillis()) {
        player.reply("$N目前無法動作!");
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
    player.setSession(newSession);

    // 3. 更新 sessionRegistry，之後的訊息直接灌給 Player
    sessionRegistry.register(newSession, player);

    // 4. 【賜死 Guest】 舊的 Guest 任務完成，請他下台
    oldGuest.stop(); // 停止 Guest 的 VT，釋放資源

    log.info("Session 權限已移交給玩家: {}", player.getName());
  }
}
