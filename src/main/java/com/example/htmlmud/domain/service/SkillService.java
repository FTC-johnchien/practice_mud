package com.example.htmlmud.domain.service;

import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.SkillCategory;
import com.example.htmlmud.domain.model.map.RaceTemplate;
import com.example.htmlmud.domain.model.map.SkillTemplate;
import com.example.htmlmud.domain.model.skill.dto.ActiveSkillResult;
import com.example.htmlmud.infra.persistence.entity.SkillEntry;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import com.example.htmlmud.infra.util.RandomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillService {

  private final ObjectMapper objectMapper;
  private final TemplateRepository templateRepo;


  // --- 核心方法：取得當前該用的技能 ---
  public SkillTemplate getEffectiveSkill(Living self, SkillCategory category) {
    // 1. 檢查玩家有沒有在這個分類上「掛載」特殊武功
    String skillId = self.getEnabledSkillId(category);

    // 2. 如果有掛載，且真的學過，回傳該武功
    if (skillId != null && self.getLearnedSkills().containsKey(skillId)) {
      return templateRepo.getSkill(skillId);
    }

    // 3. 【關鍵】如果沒掛載 (或沒學過)，回傳系統預設的「基礎技能」
    // 例如：SWORD -> basic_sword, UNARMED -> basic_fist
    return templateRepo.getDefaultSkill(category);
  }

  // 取得當前使用的攻擊招式 TODO skillId可能為null
  // public CombatAction getCombatAction(LivingActor self, SkillCategory category) {
  // String skillId = self.getEnabledSkillId(category);
  // SkillTemplate tpl = templateRepo.getSkill(skillId);

  // // 從 actions 列表中隨機挑一個 (或者依等級循序挑選)
  // List<CombatAction> actions = tpl.getActions();
  // return actions.get(ThreadLocalRandom.current().nextInt(actions.size()));
  // }

  // --- 玩家指令介面 ---
  public void enableSkill(Living self, SkillCategory category, String skillId) {
    // 檢查是否學過
    if (!self.getLearnedSkills().containsKey(skillId)) {
      throw new MudException("你還沒學會這項技能。");
    }

    // 檢查該技能是否支援此分類 (從 JSON data 讀取)
    SkillTemplate tpl = templateRepo.getSkill(skillId);
    if (!tpl.getTags().contains(category.name())) {
      throw new MudException("這個技能不能用在這個用途上。");
    }

    self.getEnabledSkills().put(category, skillId);
  }

  /**
   * 【核心方法】取得當前生效的技能
   *
   * @param weaponType 武器類型 (UNARMED, SWORD...)
   */
  public ActiveSkillResult getCombatSkill(Living self, SkillCategory category) {
    String skillId = null;
    SkillEntry entry = null;

    try {
      log.info("{}", objectMapper.writeValueAsString(category));
    } catch (Exception e) {
      e.printStackTrace();
    }


    // 1. 檢查是否主動 Enable (例如太極拳)
    if (self.getEnabledSkills().containsKey(category)) {
      skillId = self.getEnabledSkills().get(category);
      entry = self.getLearnedSkills().get(skillId);

      // 防呆：如果 enable 了但其實沒學過 (理論上 enable 指令會擋，但做個保險)
      if (entry == null) {
        self.getEnabledSkills().remove(category); // 移除錯誤設定
        skillId = null; // 進入 Fallback
      }
    }

    // 2. 如果沒有 Enable，使用預設技能 (Fallback)
    if (skillId == null) {
      // 從設定檔讀取：SWORD -> basic_sword
      skillId = templateRepo.getDefaultSkillId(category);

      // 嘗試從玩家已學列表取得
      entry = self.getLearnedSkills().get(skillId);

      // 3. 【亂揮機制】如果連基礎技能都沒學過？
      if (entry == null) {
        // 創建一個臨時的 Level 1 技能物件，但不存入 learnedSkills (直到獲得經驗)
        // 或者：直接讓玩家學會 Lv 1 (比較簡單)
        entry = new SkillEntry(skillId);
        // 這裡標記它是 "Temporary" 或直接呼叫 learnSkill 讓玩家學會
        this.learnSkill(self, skillId, 1);

      }
    }

    // 回傳技能模板 + 當前等級資料
    return new ActiveSkillResult(templateRepo.getSkill(skillId), entry);
  }

  public void learnSkill(Living self, String skillId, int level) {
    self.getLearnedSkills().put(skillId, new SkillEntry(skillId, level));
    // player.send("你在戰鬥中領悟了 " + skillId + " 的皮毛！");
  }



  /**
   * 取得當前自動攻擊要用的技能 由子類別決定邏輯
   */
  public ActiveSkillResult getAutoAttackSkill(Living self) {
    // 玩家的邏輯：委託給 SkillManager 處理
    // SkillManager 會處理 Enable, Learned, Auto-Learn
    // return getCombatSkill(self, self.getMainHandType());


    String skillId = resolveAutoAttackSkillId(self);
    log.info("name:{} skillId:{}", self.getName(), skillId);

    // 1. 取得技能模板
    SkillTemplate skillTemplate = templateRepo.getSkill(skillId);
    if (skillTemplate == null) {
      skillTemplate = templateRepo.getSkill("mob_hit"); // 系統保底
    }

    // 2. 【關鍵】動態捏造技能狀態
    // 怪物的技能等級 = 怪物自身等級 (或是怪物等級 * 1.2 之類的強度調整)
    SkillEntry virtualEntry = new SkillEntry(skillId);
    log.info("name:{} getLevel:{}", self.getName(), self.getLevel());
    virtualEntry.setLevel(self.getLevel());

    // 標記為虛擬/臨時，這樣戰鬥結算時不會給它加經驗值
    virtualEntry.setTemporary(true);

    return new ActiveSkillResult(skillTemplate, virtualEntry);
  }


  /**
   * 決定要用哪一招 (優先級判定)
   */
  private String resolveAutoAttackSkillId(Living self) {

    // 赤手空拳預設 UNARMED
    SkillCategory category = SkillCategory.UNARMED;

    // 1. 如果有拿武器，尋找是否有enable該武器的skill
    GameItem weapon = self.getMainHandWeapon();
    try {
      log.info("name:{} weapon:{}", self.getName(), objectMapper.writeValueAsString(weapon));
    } catch (Exception e) {
      log.info(e.getMessage());
    }
    if (weapon != null) {
      category = weapon.getWeaponSkillCategory();
    }

    return resolveCombatSkillId(self, category);
  }

  /**
   * 決定要用哪一招 (優先級判定)
   */
  private String resolveCombatSkillId(Living self, SkillCategory category) {

    // 先找 enabled 的技能
    String skillId = self.getEnabledSkillId(category);
    // log.info("resolveCombatSkillId name:{} skillId:{}", self.getName(), skillId);

    if (skillId != null) {

      // 防呆：如果 enable 了但其實沒學過 (理論上 enable 指令會擋，但做個保險)
      if (self.getLearnedSkills().get(skillId) == null) {
        self.getEnabledSkills().remove(category); // 移除錯誤設定
        skillId = null;
      } else {
        return skillId;
      }
    }

    // 檢查種族是否有設定 nature attack (Race Default)
    // log.info("resolveCombatSkillId name:{} race:{}", self.getName(), self.getState().getRace());
    RaceTemplate race = templateRepo.getRaceTemplates().get(self.getState().getRace());
    // log.info("resolveCombatSkillId name:{} race:{}", self.getName(), race);
    switch (category) {
      case DODGE -> {
        if (race != null && race.combat() != null && race.combat().naturalDodge() != null) {
          return race.combat().naturalDodge();
        }
        return "mob_dodge"; // 系統預設
      }
      case PARRY -> {
        if (race != null && race.combat() != null && race.combat().naturalParry() != null) {
          return race.combat().naturalParry();
        }
        return "mob_parry"; // 系統預設
      }
      case FORCE -> {
        if (race != null && race.combat() != null && race.combat().naturalForce() != null) {
          return race.combat().naturalForce();
        }
      }
      case MAGIC -> {
      }
      case MEDICAL -> {
      }
      // 武器類
      default -> {
        if (race != null && race.combat() != null && race.combat().naturalAttacks() != null) {

          // 權重隨機抽選 (Weighted Random)
          return RandomUtil.pickWeighted(race.combat().naturalAttacks()).getId();
        }

        // 真的都沒有，就用普通的撞擊/揮拳
        return "mob_hit";
      }
    }

    return null;
  }
}
