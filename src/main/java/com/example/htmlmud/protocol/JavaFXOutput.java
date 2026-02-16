package com.example.htmlmud.protocol;

import java.io.IOException;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.infra.gui.MudGuiLauncher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JavaFXOutput implements MessageOutput {

  private final ObjectMapper objectMapper;

  public JavaFXOutput(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void sendJson(Object payload) {
    // 將物件轉為 JSON 字串後丟給 JS 的 updateStats 函數
    try {
      MudGuiLauncher.executeJavaScript(
          "handleServerMessage(" + objectMapper.writeValueAsString(payload) + ")");
    } catch (IOException e) {
      log.error("sendJson {}", e.getMessage(), e);
    }
  }

  @Override
  public void close() {}

  @Override
  public WebSocketSession getSession() {
    return null;
  }
}
