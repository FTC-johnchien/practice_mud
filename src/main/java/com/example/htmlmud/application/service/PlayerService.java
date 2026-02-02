package com.example.htmlmud.application.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.command.CommandDispatcher;
import com.example.htmlmud.domain.service.SkillService;
import com.example.htmlmud.infra.persistence.service.PlayerPersistenceService;
import com.example.htmlmud.infra.server.MudWebSocketHandler;
import com.example.htmlmud.infra.util.MessageUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Service
@RequiredArgsConstructor
public class PlayerService {

  private final MessageUtil messageUtil;

  private final ObjectProvider<LivingService> livingServiceProvider;

  private final ObjectMapper objectMapper;

  private final CommandDispatcher commandDispatcher;

  private final AuthService authService;

  private final PlayerPersistenceService playerPersistenceService;

  private final SkillService skillService;

  private final ObjectProvider<WorldManager> worldManagerProvider;

  private final ObjectProvider<MudWebSocketHandler> mudWebSocketHandlerProvider;

}
