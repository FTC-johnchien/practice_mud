package com.example.htmlmud.domain.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@Getter
@RequiredArgsConstructor
public class MobService {

  private final ObjectProvider<LivingService> livingServiceProvider;

  private final TemplateRepository templateRepo;

}
