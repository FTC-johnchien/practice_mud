package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.example.htmlmud.infra.server.MudWebSocketHandler;
import com.example.htmlmud.infra.server.SessionRegistry;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.WorldManager;
import com.example.htmlmud.application.service.GameCommandService;
import com.example.htmlmud.infra.monitor.GameMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;

/**
 * 驗收測試：WebSocket 網路防禦 (NET-01/NET-02) 與持久化停機排空協定 (ASYNC-02)
 */
public class NetworkSecurityAndPersistenceDrainTest {

  @Test
  @DisplayName("NET-02: 驗證超過 64KB 封包會被直接拒絕並關閉連線")
  void testOversizedWebSocketMessageRejected() throws Exception {
    PlayerService playerService = Mockito.mock(PlayerService.class);
    WorldManager worldManager = Mockito.mock(WorldManager.class);
    SessionRegistry sessionRegistry = new SessionRegistry();
    GameMetrics gameMetrics = Mockito.mock(GameMetrics.class);
    GameCommandService gameCommandService = Mockito.mock(GameCommandService.class);
    ObjectMapper objectMapper = new ObjectMapper();
    var saveGameService = Mockito.mock(com.example.htmlmud.domain.save.service.SaveGameService.class);

    MudWebSocketHandler handler = new MudWebSocketHandler(
        playerService, worldManager, sessionRegistry, gameMetrics, gameCommandService, objectMapper, saveGameService);

    WebSocketSession session = Mockito.mock(WebSocketSession.class);
    Mockito.when(session.getId()).thenReturn("ws-test-1");

    handler.afterConnectionEstablished(session);

    // 構造超過 64 KB (如 65 KB) 的文字封包
    String oversizedPayload = "A".repeat(65 * 1024);
    TextMessage message = new TextMessage(oversizedPayload);

    handler.handleMessage(session, message);

    ArgumentCaptor<CloseStatus> statusCaptor = ArgumentCaptor.forClass(CloseStatus.class);
    Mockito.verify(session).close(statusCaptor.capture());
    assertThat(statusCaptor.getValue().getCode())
        .as("關閉狀態碼應為 BAD_DATA (1007)")
        .isEqualTo(CloseStatus.BAD_DATA.getCode());
  }

  @Test
  @DisplayName("NET-02: 驗證指令速率限制 (每秒超過 20 次指令觸發限流)")
  void testCommandRateLimitingPerSecond() throws Exception {
    PlayerService playerService = Mockito.mock(PlayerService.class);
    @SuppressWarnings("unchecked")
    org.springframework.beans.factory.ObjectProvider<com.example.htmlmud.domain.service.LivingService> livingProvider =
        Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
    Mockito.when(playerService.getLivingServiceProvider()).thenReturn(livingProvider);

    WorldManager worldManager = Mockito.mock(WorldManager.class);
    SessionRegistry sessionRegistry = new SessionRegistry();
    GameMetrics gameMetrics = Mockito.mock(GameMetrics.class);
    GameCommandService gameCommandService = Mockito.mock(GameCommandService.class);
    ObjectMapper objectMapper = new ObjectMapper();
    var saveGameService = Mockito.mock(com.example.htmlmud.domain.save.service.SaveGameService.class);

    MudWebSocketHandler handler = new MudWebSocketHandler(
        playerService, worldManager, sessionRegistry, gameMetrics, gameCommandService, objectMapper, saveGameService);

    WebSocketSession session = Mockito.mock(WebSocketSession.class);
    Mockito.when(session.getId()).thenReturn("ws-test-2");

    handler.afterConnectionEstablished(session);

    Player player = sessionRegistry.get(session.getId());
    assertThat(player).isNotNull();

    // 模擬 1 秒內連續發送 25 次合法大小的指令
    TextMessage message = new TextMessage("look");
    for (int i = 0; i < 25; i++) {
      handler.handleMessage(session, message);
    }

    // 前 20 次應正常進入 commandService，超過 20 次應被攔截
    Mockito.verify(gameCommandService, Mockito.atMost(20)).execute(Mockito.any());
  }

  @Test
  @DisplayName("ASYNC-02: 驗證 Write-Behind 服務在關機時完整排空佇列與 worker 本地批次 (Drain Protocol)")
  void testWriteBehindDrainProtocolOnShutdown() throws Exception {
    AtomicInteger flushedCount = new AtomicInteger(0);

    var service = new com.example.htmlmud.infra.persistence.service.AbstractAsyncBatchPersistenceService<String>(1000) {
      @Override
      protected String getWorkerThreadName() {
        return "test-drain-worker";
      }

      @Override
      protected void flushBatch(java.util.List<String> batch) {
        flushedCount.addAndGet(batch.size());
      }
    };

    service.init();

    // 寫入 25 筆資料 (小於單批 50 筆，會暫存在 worker 的本地 batch 或 saveQueue)
    for (int i = 0; i < 25; i++) {
      service.saveAsync("item-" + i);
    }

    // 立即觸發 shutdown
    service.shutdown();

    // 驗證 25 筆資料全部在 shutdown 期間被原子排空與寫入，無任何遺失
    assertThat(flushedCount.get())
        .as("25 筆資料在 shutdown 時應 100% 透過 Drain Protocol 排空落盤")
        .isEqualTo(25);
  }
}
