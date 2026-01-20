package com.example.htmlmud.service.world;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.MobActor;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.domain.model.map.MobTemplate;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldFactory {

  private final ObjectProvider<GameServices> servicesProvider;

  private final TemplateRepository templateRepo;

  public MobActor createMob(String templateId) {
    // 1. 查 Template (Record)
    MobTemplate tpl = templateRepo.findMob(templateId).orElse(null);
    if (tpl == null) {
      log.error("MobTemplate ID not found: " + templateId);
      return null;
    }

    // 2. new Actor (State 自動生成)
    MobActor mob = new MobActor(tpl, servicesProvider.getObject());

    // 3. 【關鍵】在外部呼叫 start
    mob.start();

    return mob;
  }
}
