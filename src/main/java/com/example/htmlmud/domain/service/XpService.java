package com.example.htmlmud.domain.service;

import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.infra.persistence.entity.SkillEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class XpService {

  public long getRequiredXp(SkillEntry userSkill, SkillTemplate template) {
    int lv = userSkill.getLevel();
    double difficulty = template.getMechanics().learningDifficulty(); // 預設 1.0

    // 基礎公式 (平方曲線)
    long baseReq = 50 * (long) Math.pow(lv, 2);

    // 乘上技能個別的難度係數
    return (long) (baseReq * difficulty);
  }
}
