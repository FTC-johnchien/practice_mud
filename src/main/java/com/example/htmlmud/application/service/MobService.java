package com.example.htmlmud.application.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MobService {

  @Getter
  private final ObjectProvider<LivingService> livingServiceProvider;

}
