package com.example.htmlmud.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.SkillCategory;
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
  private final TemplateRepository tmplateRepo;

  // 已學會的技能與等級: "taichi_fist" -> Lv 50
  private Map<String, Integer> learnedSkills = new HashMap<>();

  // 當前激發的技能: "UNARMED" -> "taichi_fist"
  // 這代表玩家雖然學了十種拳法，但他現在是用太極拳在打
  private Map<SkillCategory, String> enabledSkills = new HashMap<>();

  // --- 核心方法：取得當前該用的技能 ---
  public SkillTemplate getEffectiveSkill(SkillCategory category) {
    // 1. 檢查玩家有沒有在這個分類上「掛載」特殊武功
    String skillId = enabledSkills.get(category);

    // 2. 如果有掛載，且真的學過，回傳該武功
    if (skillId != null && learnedSkills.containsKey(skillId)) {
      return tmplateRepo.getSkill(skillId);
    }

    // 3. 【關鍵】如果沒掛載 (或沒學過)，回傳系統預設的「基礎技能」
    // 例如：SWORD -> basic_sword, UNARMED -> basic_fist
    return tmplateRepo.getDefaultSkill(category);
  }

  // 取得當前使用的攻擊招式
  public CombatAction getCombatAction(SkillType type) {
    String skillId = enabledSkills.getOrDefault(type, "basic_fist"); // 沒激發就用基本拳腳
    SkillTemplate tpl = tmplateRepo.getSkill(skillId);

    // 從 actions 列表中隨機挑一個 (或者依等級循序挑選)
    List<CombatAction> actions = tpl.getActions();
    return actions.get(ThreadLocalRandom.current().nextInt(actions.size()));
  }

  // --- 玩家指令介面 ---
  public void enableSkill(SkillCategory category, String skillId) {
    // 檢查是否學過
    if (!learnedSkills.containsKey(skillId)) {
      throw new MudException("你還沒學會這項技能。");
    }

    // 檢查該技能是否支援此分類 (從 JSON data 讀取)
    SkillTemplate tpl = tmplateRepo.getSkill(skillId);
    if (!tpl.getTags().contains(category.name())) {
      throw new MudException("這個技能不能用在這個用途上。");
    }

    enabledSkills.put(category, skillId);
  }
}
