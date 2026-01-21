package com.example.htmlmud.domain.context;

import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.ApplicationEventPublisher;
import com.example.htmlmud.application.command.CommandDispatcher;
import com.fasterxml.jackson.databind.ObjectMapper;

public record GameServices(

    ObjectMapper objectMapper,

    ApplicationEventPublisher eventPublisher,

    CommandDispatcher commandDispatcher,

    ScheduledExecutorService scheduler

) {

}
