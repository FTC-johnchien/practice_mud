package com.example.htmlmud.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.model.entity.SkillEntry;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * 技能熟練度經驗服務適配器 (向下相容)，已統一收斂委託至 {@link XpProgressionService}。
 */
@Slf4j
@Service
public class XpService {

  private final XpProgressionService xpProgressionService;

  @Autowired
  public XpService(XpProgressionService xpProgressionService) {
    this.xpProgressionService = xpProgressionService != null ? xpProgressionService : new XpProgressionService();
  }

  public XpService() {
    this(new XpProgressionService());
  }

  public long getRequiredXp(SkillEntry userSkill, SkillTemplate template) {
    return xpProgressionService.calculateSkillRequiredExp(userSkill, template);
  }

  public XpProgressionService.SkillLevelUpResult awardSkillExp(SkillEntry entry, SkillTemplate template, long gain) {
    return xpProgressionService.awardSkillExp(entry, template, gain);
  }
}

