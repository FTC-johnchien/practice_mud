package com.example.htmlmud.domain.service;

import java.io.IOException;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.application.command.CommandDispatcher;
import com.example.htmlmud.application.service.AuthService;
import com.example.htmlmud.domain.actor.behavior.PlayerBehavior;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.infra.persistence.service.PlayerPersistenceService;
import com.example.htmlmud.infra.server.MudWebSocketHandler;
import com.example.htmlmud.infra.util.MessageUtil;
import com.example.htmlmud.protocol.ConnectionState;
import com.example.htmlmud.protocol.GameCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class PlayerService {

  private final MessageUtil messageUtil;

  private final ObjectProvider<LivingService> livingServiceProvider;

  private final ObjectMapper objectMapper;

  private final CommandDispatcher commandDispatcher;

  private final AuthService authService;

  private final PlayerPersistenceService playerPersistenceService;

  private final SkillService skillService;

  private final ObjectProvider<WorldManager> worldManagerProvider;

  private final ObjectProvider<MudWebSocketHandler> mudWebSocketHandlerProvider;



  public void handleInput(Player player, String traceId, GameCommand cmd) {
    // A. 設定 MDC (給 Log 看)
    MDC.put("traceId", traceId);
    // MDC.put("actorName", this.name);

    try {
      // B. 設定 ScopedValue (給 Service 邏輯看)
      ScopedValue.where(MudContext.CURRENT_PLAYER, player).where(MudContext.TRACE_ID, traceId)
          .run(() -> {
            // C. 委派給當前 Behavior 處理
            PlayerBehavior next = player.getCurrentBehavior().handle(cmd);

            // D. 狀態切換
            if (next != null) {
              become(player, next);
            }
          });
    } finally {
      // E. 清理 MDC
      MDC.clear();
    }
  }

  // 狀態切換方法
  public void become(Player player, PlayerBehavior nextBehavior) {
    player.setCurrentBehavior(nextBehavior);
    nextBehavior.onEnter(); // 觸發進場事件
    log.info("{} 切換行為模式至 {}", player.getName(), nextBehavior.getClass().getSimpleName());
  }

  public void handleSendText(Player player, WebSocketSession session, String msg) {
    if (session != null && session.isOpen()) {

      // 處理 $N 代名詞
      msg = messageUtil.format(msg, player);

      try {
        String json = objectMapper.writeValueAsString(Map.of("type", "TEXT", "content", msg));

        // 這裡才是真正寫入 WebSocket 的地方
        // 因為是在 handleMessage 內執行，保證了 Thread-Safe
        session.sendMessage(new TextMessage(json));
      } catch (IOException e) {
        log.error("Failed to send message to player {}", player.getId(), e);
      }
    }
  }

  // 【當玩家重新連線時呼叫此方法】
  public void handleReconnect(Player player, WebSocketSession newSession) {

    // 1. 關閉舊連線 (如果還開著)
    if (player.getSession() != null && player.getSession().isOpen()) {
      try {
        player.getSession().close();
      } catch (IOException ignored) {
      }
    }

    // 2. 換上新連線 (必須先更新欄位，確保後續訊息發往正確的連線)
    // 更新斷線時間戳記，這樣之前的死神 VT 醒來後會發現時間對不上，就不會執行殺人
    player.setLastDisconnectTime(System.currentTimeMillis());
    player.setConnectionState(ConnectionState.IN_GAME);

    mudWebSocketHandlerProvider.getObject().promoteToPlayer(newSession, player);

    // 3. 重發當前環境資訊
    log.warn("{} 重新連線成功！", player.getName());

    // 發送歡迎回來的訊息
    handleSendText(player, player.getSession(), "\u001B[33m[系統] 連線已恢復。\u001B[0m");
    player.sendStatUpdate();
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      commandDispatcher.dispatch("look");
    });
    player.getCurrentRoom().broadcastToOthers(player.getId(), "$N的眼神恢復了光采。");
  }

  // 【當 WebSocket 斷線時呼叫此方法】
  public void handleDisconnect(Player player) {
    log.warn("{} 斷線，進入緩衝狀態...", player.getName());
    long disconnectTimestamp = System.currentTimeMillis();

    player.setLastDisconnectTime(disconnectTimestamp);
    player.setConnectionState(ConnectionState.LINK_DEAD);

    // 廣播給房間其他人 (沉浸式體驗)
    player.getCurrentRoom().broadcastToOthers(player.getId(), "$N眼神突然變得呆滯，似乎失去了靈魂。");

    // 【關鍵】啟動一個「死神 VT」
    startDeathTimer(player, disconnectTimestamp);
  }



  private void startDeathTimer(Player player, long disconnectTimestamp) {
    // 啟動一個虛擬執行緒，成本極低
    Thread.ofVirtual().name("Reaper-" + player.getName()).start(() -> {
      try {
        // 設定緩衝時間：例如 10 分鐘 (600,000 ms)
        // 這裡直接 sleep，不會佔用系統資源
        Thread.sleep(10 * 60 * 1000);

        // --- 10 分鐘後醒來 ---

        // 檢查 1: 玩家是否還在斷線狀態？
        // 檢查 2: 這是當初那次斷線嗎？(防止玩家重連後又斷線，舊的計時器殺錯)
        if (player.getConnectionState() == ConnectionState.LINK_DEAD
            && disconnectTimestamp == player.getLastDisconnectTime()) {
          log.warn("緩衝時間已過，強制清理玩家: {}", player.getName());
          player.forceLogout();
        } else {
          log.info("玩家已重連，死神計時器取消: {}", player.getName());
        }

      } catch (InterruptedException ignored) {
        // 計時器被中斷
      }
    });
  }

}
