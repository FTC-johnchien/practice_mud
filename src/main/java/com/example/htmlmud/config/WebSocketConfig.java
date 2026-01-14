package com.example.htmlmud.config;

import com.example.htmlmud.infra.server.MudWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final MudWebSocketHandler mudWebSocketHandler;

  public WebSocketConfig(MudWebSocketHandler mudWebSocketHandler) {
    this.mudWebSocketHandler = mudWebSocketHandler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(mudWebSocketHandler, "/ws").setAllowedOrigins("*");
  }
}
