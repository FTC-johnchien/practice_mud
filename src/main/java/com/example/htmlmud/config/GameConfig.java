package com.example.htmlmud.config;

import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.domain.logic.command.CommandDispatcher;
import com.example.htmlmud.service.PlayerService;
import com.example.htmlmud.service.auth.AuthService;
import com.example.htmlmud.service.persistence.PlayerPersistenceService;
import com.example.htmlmud.service.world.WorldManager;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class GameConfig {
  @Bean
  public GameServices gameServices(

      AuthService authService,

      PlayerService playerService,

      PlayerPersistenceService dispatcher,

      WorldManager worldManager,

      ObjectMapper objectMapper,

      ApplicationEventPublisher eventPublisher,

      CommandDispatcher commandDispatcher,

      ScheduledExecutorService scheduler) {
    return new GameServices(

        authService,

        playerService,

        dispatcher,

        worldManager,

        objectMapper,

        eventPublisher,

        commandDispatcher,

        scheduler);
  }
}
