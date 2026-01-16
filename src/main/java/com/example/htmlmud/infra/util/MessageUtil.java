package com.example.htmlmud.infra.util;

import java.io.IOException;
import java.util.Map;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.context.MudContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageUtil {

  public static void sendText(String text) {
    try {
      WebSocketSession session = MudContext.WEB_SOCKET_SESSION.get();
      if (session.isOpen()) {
        // 使用 Map 來建立結構，Jackson 會自動處理所有特殊字元的轉義
        String json = MudContext.OBJECT_MAPPER.get()
            .writeValueAsString(Map.of("type", "TEXT", "content", text));

        session.sendMessage(new TextMessage(json));
      }
    } catch (IOException e) {
      log.error("[Trace:{}] Error in session sendText", MudContext.TRACE_ID.get(), e);
    }
  }

}
