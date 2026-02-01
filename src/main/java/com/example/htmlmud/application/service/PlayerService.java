package com.example.htmlmud.application.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.command.CommandDispatcher;
import com.example.htmlmud.domain.service.SkillService;
import com.example.htmlmud.infra.persistence.service.PlayerPersistenceService;
import com.example.htmlmud.infra.util.MessageUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlayerService {

  @Getter
  private final MessageUtil messageUtil;

  @Getter
  private final ObjectProvider<LivingService> livingServiceProvider;

  @Getter
  private final ObjectMapper objectMapper;

  @Getter
  private final CommandDispatcher commandDispatcher;

  @Getter
  private final AuthService authService;

  @Getter
  private final PlayerPersistenceService playerPersistenceService;

  @Getter
  private final SkillService skillService;

  @Getter
  private final ObjectProvider<WorldManager> worldManagerProvider;

}
