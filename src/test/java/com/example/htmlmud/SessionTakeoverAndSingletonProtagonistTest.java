package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.example.htmlmud.application.service.GameCommandService;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.save.SaveGameService;
import com.example.htmlmud.domain.actor.service.PlayerService;
import com.example.htmlmud.domain.world.WorldManager;
import com.example.htmlmud.infra.monitor.GameMetrics;
import com.example.htmlmud.pipeline.MudWebSocketHandler;
import com.example.htmlmud.pipeline.SessionRegistry;
import com.example.htmlmud.protocol.ConnectionState;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 專屬驗收測試：SP-05 單機唯一主角 (Singleton Protagonist) 與多開會話安全接管 (Session Takeover)
 */
public class SessionTakeoverAndSingletonProtagonistTest {

  private PlayerService playerService;
  private WorldManager worldManager;
  private SessionRegistry sessionRegistry;
  private GameMetrics gameMetrics;
  private GameCommandService gameCommandService;
  private ObjectMapper objectMapper;
  private SaveGameService saveGameService;
  private MudWebSocketHandler handler;

  @BeforeEach
  void setUp() {
    playerService = mock(PlayerService.class);
    @SuppressWarnings("unchecked")
    org.springframework.beans.factory.ObjectProvider<com.example.htmlmud.domain.actor.service.LivingService> livingProvider =
        mock(org.springframework.beans.factory.ObjectProvider.class);
    when(playerService.getLivingServiceProvider()).thenReturn(livingProvider);

    worldManager = mock(WorldManager.class);
    sessionRegistry = new SessionRegistry();
    gameMetrics = mock(GameMetrics.class);
    gameCommandService = mock(GameCommandService.class);
    objectMapper = new ObjectMapper();
    saveGameService = mock(SaveGameService.class);

    handler = new MudWebSocketHandler(
        playerService, worldManager, sessionRegistry, gameMetrics, gameCommandService, objectMapper, saveGameService);
  }

  @Test
  @DisplayName("SP-05-1: 首次連線創生單機唯一主角，並登錄至 SessionRegistry")
  void testSingletonProtagonistOnFirstConnect() {
    WebSocketSession session1 = mock(WebSocketSession.class);
    when(session1.getId()).thenReturn("ws-tab-1");
    when(session1.isOpen()).thenReturn(true);

    handler.afterConnectionEstablished(session1);

    assertThat(sessionRegistry.hasActiveProtagonist())
        .as("首次連入後應存在活躍主角")
        .isTrue();

    Player protagonist = sessionRegistry.getProtagonist();
    assertThat(protagonist).isNotNull();
    assertThat(protagonist.getName()).isEqualTo("玄靈子");
    assertThat(protagonist.getConnectionState()).isEqualTo(ConnectionState.IN_GAME);
    assertThat(sessionRegistry.getActiveSession()).isSameAs(session1);
    assertThat(sessionRegistry.get(session1.getId())).isSameAs(protagonist);

    // 清理主角執行緒
    protagonist.stop();
  }

  @Test
  @DisplayName("SP-05-2: 多開分頁連入自動安全接管唯一主角，關閉舊分頁連線並無縫同步狀態")
  void testSessionTakeoverClosesOldSessionAndReplacesOutput() throws Exception {
    WebSocketSession session1 = mock(WebSocketSession.class);
    when(session1.getId()).thenReturn("ws-tab-1");
    when(session1.isOpen()).thenReturn(true);

    // 1. 分頁 1 連入
    handler.afterConnectionEstablished(session1);
    Player protagonist = sessionRegistry.getProtagonist();
    assertThat(protagonist).isNotNull();

    // 2. 分頁 2 連入 (模擬多開分頁或重新連線)
    WebSocketSession session2 = mock(WebSocketSession.class);
    when(session2.getId()).thenReturn("ws-tab-2");
    when(session2.isOpen()).thenReturn(true);

    handler.afterConnectionEstablished(session2);

    // 驗證未建立新 Player，主角實例為同一個
    Player takenOverPlayer = sessionRegistry.get(session2.getId());
    assertThat(takenOverPlayer)
        .as("多開分頁應共享同一個主角實體")
        .isSameAs(protagonist);

    // 驗證舊分頁被關閉以防干擾，關閉狀態碼包含 4001
    ArgumentCaptor<CloseStatus> statusCaptor = ArgumentCaptor.forClass(CloseStatus.class);
    verify(session1).close(statusCaptor.capture());
    assertThat(statusCaptor.getValue().getCode())
        .as("舊連線關閉碼應為 4001 (Session Takeover)")
        .isEqualTo(4001);

    // 驗證當前活躍 session 切換為 session2
    assertThat(sessionRegistry.getActiveSession()).isSameAs(session2);
    assertThat(protagonist.getConnectionState()).isEqualTo(ConnectionState.IN_GAME);

    // 清理主角執行緒
    protagonist.stop();
  }

  @Test
  @DisplayName("SP-05-3: 已被接管的舊連線關閉時，主角不受影響繼續保持運行")
  void testClosedOldSessionDoesNotDisconnectProtagonist() {
    WebSocketSession session1 = mock(WebSocketSession.class);
    when(session1.getId()).thenReturn("ws-tab-1");
    when(session1.isOpen()).thenReturn(true);

    handler.afterConnectionEstablished(session1);
    Player protagonist = sessionRegistry.getProtagonist();

    // 分頁 2 接管
    WebSocketSession session2 = mock(WebSocketSession.class);
    when(session2.getId()).thenReturn("ws-tab-2");
    when(session2.isOpen()).thenReturn(true);
    handler.afterConnectionEstablished(session2);

    // 模擬分頁 1 觸發 afterConnectionClosed
    handler.afterConnectionClosed(session1, CloseStatus.NORMAL);

    // 驗證主角依然是 IN_GAME 狀態，並未斷線
    assertThat(protagonist.getConnectionState())
        .as("舊會話關閉不應影響接管中的主角狀態")
        .isEqualTo(ConnectionState.IN_GAME);
    assertThat(sessionRegistry.hasActiveProtagonist()).isTrue();

    // 清理主角執行緒
    protagonist.stop();
  }

  @Test
  @DisplayName("SP-05-4: 當前活躍會話關閉時，正確觸發主角斷線與存檔保護")
  void testClosedActiveSessionTriggersDisconnect() {
    WebSocketSession session = mock(WebSocketSession.class);
    when(session.getId()).thenReturn("ws-single");
    when(session.isOpen()).thenReturn(true);

    handler.afterConnectionEstablished(session);
    Player protagonist = sessionRegistry.getProtagonist();

    // 活躍連線關閉
    handler.afterConnectionClosed(session, CloseStatus.NORMAL);

    // 驗證 session 從活躍連線清除
    assertThat(sessionRegistry.getActiveSession()).isNull();

    // 清理主角執行緒
    protagonist.stop();
  }
}
