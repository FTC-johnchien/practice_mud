package com.example.htmlmud.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.model.SkillType;
import com.example.htmlmud.domain.model.map.CombatAction;
import com.example.htmlmud.domain.model.map.SkillTemplate;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillManager {
  // 已學會的技能與等級: "taichi_fist" -> Lv 50
  private Map<String, Integer> learnedSkills = new HashMap<>();

  // 當前激發的技能: "UNARMED" -> "taichi_fist"
  // 這代表玩家雖然學了十種拳法，但他現在是用太極拳在打
  private Map<SkillType, String> enabledSkills = new HashMap<>();

  // 取得當前使用的攻擊招式
  public CombatAction getCombatAction(SkillType type) {
    String skillId = enabledSkills.getOrDefault(type, "basic_fist"); // 沒激發就用基本拳腳
    SkillTemplate tpl = TemplateRepository.getSkill(skillId);

    // 從 actions 列表中隨機挑一個 (或者依等級循序挑選)
    List<CombatAction> actions = tpl.actions();
    return actions.get(ThreadLocalRandom.current().nextInt(actions.size()));
  }
}
