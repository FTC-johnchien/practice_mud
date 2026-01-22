package com.example.htmlmud.config;

import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.htmlmud.application.command.CommandDispatcher;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.domain.service.CombatService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class GameConfig {
  @Bean
  public GameServices gameServices(

      ObjectMapper objectMapper,

      CombatService combatService,

      CommandDispatcher commandDispatcher,

      ScheduledExecutorService scheduler,

      ApplicationEventPublisher eventPublisher

  ) {
    return new GameServices(

        objectMapper,

        combatService,

        commandDispatcher,

        scheduler,

        eventPublisher

    );
  }
}
