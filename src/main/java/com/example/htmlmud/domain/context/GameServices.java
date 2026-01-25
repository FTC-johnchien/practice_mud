package com.example.htmlmud.domain.context;

import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.ApplicationEventPublisher;
import com.example.htmlmud.application.command.CommandDispatcher;
import com.example.htmlmud.application.command.parser.TargetSelector;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.service.CombatService;
import com.fasterxml.jackson.databind.ObjectMapper;

public record GameServices(

    ObjectMapper objectMapper,

    WorldManager worldManager,

    CombatService combatService,

    CommandDispatcher commandDispatcher,

    ScheduledExecutorService scheduler,

    ApplicationEventPublisher eventPublisher,

    TargetSelector targetSelector

) {

}
