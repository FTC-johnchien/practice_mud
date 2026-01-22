package com.example.htmlmud.domain.context;

import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.ApplicationEventPublisher;
import com.example.htmlmud.application.command.CommandDispatcher;
import com.example.htmlmud.domain.service.CombatService;
import com.fasterxml.jackson.databind.ObjectMapper;

public record GameServices(

    ObjectMapper objectMapper,

    CombatService combatService,

    CommandDispatcher commandDispatcher,

    ScheduledExecutorService scheduler,

    ApplicationEventPublisher eventPublisher

) {

}
