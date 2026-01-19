package com.example.htmlmud.domain.context;

import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.ApplicationEventPublisher;
import com.example.htmlmud.service.PlayerService;
import com.example.htmlmud.service.auth.AuthService;
import com.example.htmlmud.service.persistence.PlayerPersistenceService;
import com.example.htmlmud.service.world.WorldManager;
import com.fasterxml.jackson.databind.ObjectMapper;

public record GameServices(AuthService authService, PlayerService playerService,
    PlayerPersistenceService playerPersistenceService, WorldManager worldManager,
    ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher,
    ScheduledExecutorService scheduler
// 未來如果要加 ItemService, SkillService，加在這裡就好
// ItemService itemService
) {

}
