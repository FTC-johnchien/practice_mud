package com.example.htmlmud.application.listener;

import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.service.WorldManager;
import com.example.htmlmud.protocol.RoomMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerLoginListener {

  private final WorldManager worldManager;

  @Autowired
  private ObjectMapper objectMapper;

  // @EventListener
  // public void onPlayerLogin(PlayerLoginEvent event) {
  // var entity = event.getPlayerEntity();
  // var session = event.getSession();

  // log.info("處理玩家登入事件: {}", entity.getUsername());
  /*
   * try { // 1. 建立 Actor (這裡是 Domain Logic) PlayerActor playerActor = new
   * PlayerActor(entity.getId(), session, entity.state, objectMapper);
   *
   * // 2. 設定上次位置 playerActor.setCurrentRoomId(entity.getCurrentRoomId());
   *
   * // 3. 綁定 Session (讓 WebSocketHandler 知道這個 session 有主人了) session.getAttributes().put("actor",
   * playerActor);
   *
   * // 4. 歡迎訊息 playerActor.sendText("\u001B[32m登入成功！歡迎回到 MUD 世界 (Event Driven)。\u001B[0m");
   *
   * // 5. 進入房間邏輯 var room = worldManager.getRoomActor(playerActor.getCurrentRoomId()); var future =
   * new CompletableFuture<Void>();
   *
   * // 讓玩家進入房間 // room.receive(new RoomMessage.PlayerEnter(playerActor, future));
   *
   * // 進入後自動 Look // future.thenRun(() -> playerActor.send("look"));
   *
   * } catch (Exception e) { log.error("玩家登入後續處理失敗", e); try { session.sendMessage(new
   * TextMessage("系統錯誤：無法進入世界")); } catch (Exception ignored) { } }
   */
  // }
}
