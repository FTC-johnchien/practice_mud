package com.example.htmlmud.config;

import java.util.Arrays;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.example.htmlmud.infra.server.MudWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final MudWebSocketHandler mudWebSocketHandler;

  private final String allowedOrigins;

  public WebSocketConfig(MudWebSocketHandler mudWebSocketHandler,
      @Value("${app.websocket.allowed-origins:http://localhost:8080}") String allowedOrigins) {
    this.mudWebSocketHandler = mudWebSocketHandler;
    this.allowedOrigins = allowedOrigins;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    String[] origins = Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .filter(origin -> !origin.isEmpty())
        .toArray(String[]::new);
    registry.addHandler(mudWebSocketHandler, "/ws").setAllowedOrigins(origins);
  }
}
