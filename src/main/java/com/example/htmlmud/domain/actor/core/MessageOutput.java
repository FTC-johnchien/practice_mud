package com.example.htmlmud.domain.actor.core;

import org.springframework.web.socket.WebSocketSession;

public interface MessageOutput {
  // void sendMessage(String text);

  void sendJson(Object payload); // 處理 STAT_UPDATE 等 JSON

  void close(); // 關閉連線

  WebSocketSession getSession();

}
