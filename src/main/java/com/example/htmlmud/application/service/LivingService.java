package com.example.htmlmud.application.service;

import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.service.CombatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivingService {

  @Getter
  protected final ObjectMapper objectMapper;

  @Getter
  protected final CombatService combatService;

}
