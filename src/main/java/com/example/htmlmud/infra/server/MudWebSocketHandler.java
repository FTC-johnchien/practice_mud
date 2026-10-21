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
import com.example.htmlmud.domain.actor.service.PlayerService;
import com.example.htmlmud.domain.world.WorldManager;
import com.example.htmlmud.domain.port.ClientSessionManagerPort;
import com.example.htmlmud.infra.monitor.GameMetrics;
import com.example.htmlmud.protocol.WebSocketOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MudWebSocketHandler extends TextWebSocketHandler implements ClientSessionManagerPort {
  private final PlayerService playerService;
  private final WorldManager worldManager;
  private final SessionRegistry sessionRegistry;
  private final GameMetrics gameMetrics;
  private final GameCommandService gameCommandService;
  private final ObjectMapper objectMapper;
  private final com.example.htmlmud.domain.save.service.SaveGameService saveGameService;

  public MudWebSocketHandler(
      PlayerService playerService,
      WorldManager worldManager,
      SessionRegistry sessionRegistry,
      GameMetrics gameMetrics,
      GameCommandService gameCommandService,
      ObjectMapper objectMapper,
      com.example.htmlmud.domain.save.service.SaveGameService saveGameService) {
    this.playerService = playerService;
    this.worldManager = worldManager;
    this.sessionRegistry = sessionRegistry;
    this.gameMetrics = gameMetrics;
    this.gameCommandService = gameCommandService;
    this.objectMapper = objectMapper;
    this.saveGameService = saveGameService;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    try {
      // 1. 檢查是否已存在運作中的單機唯一主角 (Singleton Protagonist)
      if (sessionRegistry.hasActiveProtagonist()) {
        Player protagonist = sessionRegistry.getProtagonist();
        WebSocketSession oldSession = sessionRegistry.getActiveSession();

        // 多開連線安全接管 (Session Takeover)：關閉舊連線防範多分頁相互干擾
        if (oldSession != null && oldSession.isOpen() && !oldSession.getId().equals(session.getId())) {
          log.info("偵測到新連線 {}，接管現有主角會話，關閉舊連線 {}", session.getId(), oldSession.getId());
          try {
            oldSession.close(new CloseStatus(4001, "已在另一個視窗或分頁接管會話 (Session Taken Over)"));
          } catch (Exception ex) {
            log.debug("關閉舊連線異常 (可忽略): {}", ex.getMessage());
          }
          sessionRegistry.remove(oldSession.getId());
        }

        // 將主角的輸出通道無縫切換至新連線
        WebSocketOutput newOutput = new WebSocketOutput(session, objectMapper);
        protagonist.setOutput(newOutput);
        protagonist.setConnectionState(com.example.htmlmud.protocol.ConnectionState.IN_GAME);
        protagonist.setLastDisconnectTime(System.currentTimeMillis());

        // 更新註冊表映射
        sessionRegistry.register(session, protagonist);

        log.info("單機唯一主角會話已由新連線 {} 接管: {}", session.getId(), protagonist.getName());

        // 推送接管提示與即時全景狀態
        protagonist.reply("💡 【會話接管】已恢復太陰萬劫修仙進度，世界已無縫同步！");
        protagonist.sendStatUpdate();
        try {
          ScopedValue.where(com.example.htmlmud.domain.context.MudContext.CURRENT_PLAYER, protagonist).run(() -> {
            gameCommandService.execute(new GameRequest(protagonist, "look", "WEB"));
          });
        } catch (Exception ex) {
          log.warn("會話接管時推送全景環境失敗: {}", ex.getMessage());
        }
        return;
      }

      // 2. 首次連線或主角尚未啟動：初始化單機唯一主角實例
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

      String playerId = (initialName != null && !initialName.isBlank()) ? initialName : "玄靈子";
      Player self = Player.createSinglePlayer(new WebSocketOutput(session, objectMapper), worldManager,
          playerService, initialName, playerId);

      // 啟動 Actor 的虛擬執行緒 (Virtual Thread)
      self.start();

      // 註冊到網路層 SessionRegistry
      sessionRegistry.register(session, self);

      log.info("單機主角 Actor 創生並綁定連線: session={}, player={}", session.getId(), playerId);
    } catch (Exception e) {
      log.error("連線初始化失敗", e);
      try {
        session.close();
      } catch (Exception ignored) {
      }
    }
  }

  public static final int MAX_TEXT_MESSAGE_SIZE = 64 * 1024; // 64 KB
  public static final int MAX_COMMANDS_PER_SECOND = 20;

  // 記錄 session 每秒指令窗口: sessionId -> [windowStartTimestamp, count]
  private final java.util.Map<String, long[]> rateLimitMap = new java.util.concurrent.ConcurrentHashMap<>();

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    // 1. 訊息長度上限檢查 (64 KB)
    if (message.getPayloadLength() > MAX_TEXT_MESSAGE_SIZE) {
      log.warn("WebSocket 封包超過上限 ({} bytes > 64KB)，強制關閉連線: {}", message.getPayloadLength(), session.getId());
      session.close(new CloseStatus(CloseStatus.BAD_DATA.getCode(), "Payload too large (max 64KB)"));
      return;
    }

    // 2. 指令頻率限制檢查 (每秒最多 20 次指令)
    long now = System.currentTimeMillis();
    long[] rateData = rateLimitMap.compute(session.getId(), (k, v) -> {
      if (v == null || now - v[0] >= 1000) {
        return new long[] {now, 1};
      }
      v[1]++;
      return v;
    });

    if (rateData[1] > MAX_COMMANDS_PER_SECOND) {
      Player player = sessionRegistry.get(session.getId());
      if (player != null) {
        player.reply("操作過於頻繁，請稍候再試！(Rate limit exceeded)");
      }
      log.warn("WebSocket 指令頻率超限 ({} cmds/s)，暫停處理: {}", rateData[1], session.getId());
      return;
    }

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
    rateLimitMap.remove(session.getId());
    log.info("WebSocket 連線關閉: session={}, status={}", session.getId(), status);

    // 檢查被關閉的 session 是否仍為主角當前活躍連線
    WebSocketSession currentActiveSession = sessionRegistry.getActiveSession();
    boolean isCurrentActive = (currentActiveSession != null && currentActiveSession.getId().equals(session.getId()));

    // 從 Registry 移除此 session
    Player actor = sessionRegistry.remove(session.getId());

    if (actor != null && isCurrentActive) {
      log.info("主角當前活躍連線關閉: {} (Actor: {})", session.getId(), actor.getId());
      // 觸發自動存檔落盤，防止進度遺失
      try {
        actor.saveData();
      } catch (Exception ex) {
        log.warn("連線中斷自動存檔失敗: {}", ex.getMessage());
      }
      actor.disconnect();
    } else {
      log.info("已由新會話接管或非當前活躍連線關閉，主角繼續保持運作: {}", session.getId());
    }
  }

  // --- 關鍵方法：切換負責人 (Handover) ---
  // 這個方法會被 LoginService 呼叫
  public void promoteToPlayer(WebSocketSession newSession, Player player) {
    Player oldGuest = sessionRegistry.get(newSession.getId());

    player.setOutput(new WebSocketOutput(newSession, objectMapper));
    sessionRegistry.register(newSession, player);

    if (oldGuest != null && oldGuest != player) {
      oldGuest.stop(); // 停止 Guest 的 VT，釋放資源
    }

    log.info("Session 權限已移交給玩家: {}", player.getName());
  }

  @Override
  public void promoteToPlayer(Object rawSession, Player player) {
    if (rawSession instanceof WebSocketSession session) {
      promoteToPlayer(session, player);
    }
  }
}
