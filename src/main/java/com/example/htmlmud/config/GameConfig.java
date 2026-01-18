package com.example.htmlmud.config;

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
  public GameServices gameServices(AuthService as, PlayerService ps, PlayerPersistenceService pps,
      WorldManager wm, ObjectMapper om, ApplicationEventPublisher pub) {
    return new GameServices(as, ps, pps, wm, om, pub);
  }
}
