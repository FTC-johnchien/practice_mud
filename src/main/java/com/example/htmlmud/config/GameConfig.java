package com.example.htmlmud.config;

import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.htmlmud.application.command.CommandDispatcher;
import com.example.htmlmud.application.service.AuthService;
import com.example.htmlmud.domain.context.GameServices;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class GameConfig {
  @Bean
  public GameServices gameServices(

      ObjectMapper objectMapper,

      ApplicationEventPublisher eventPublisher,

      CommandDispatcher commandDispatcher,

      ScheduledExecutorService scheduler) {
    return new GameServices(

        objectMapper,

        eventPublisher,

        commandDispatcher,

        scheduler);
  }
}
