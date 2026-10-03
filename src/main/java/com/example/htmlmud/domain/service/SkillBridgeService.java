package com.example.htmlmud.domain.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.entity.SkillEntry;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.repository.TemplateReader;
import lombok.extern.slf4j.Slf4j;

/**
 * 負責 MUD 敘事招式 (SkillEntry) 與 DRPG 回合戰術技能 (PartyMemberSkill) 的語意橋接與動態數值縮放。
 */
@Slf4j
@Service
public class SkillBridgeService {

  private final TemplateReader templateReader;
  private final XpProgressionService xpProgressionService;

  @Autowired
  public SkillBridgeService(TemplateReader templateReader, XpProgressionService xpProgressionService) {
    this.templateReader = templateReader != null ? templateReader : new TemplateCatalog();
    this.xpProgressionService = xpProgressionService != null ? xpProgressionService : new XpProgressionService();
  }

  public SkillBridgeService(TemplateReader templateReader) {
    this(templateReader, new XpProgressionService());
  }

  public SkillBridgeService() {
    this(new TemplateCatalog(), new XpProgressionService());
  }

  /**
   * 同步玩家的 MUD 技能與屬性至 DRPG 小隊成員 (隊長)
   */
  public void syncSkills(Player player, PartyMember member) {
    if (player == null || member == null) return;

    Map<String, SkillEntry> mudSkills = player.getLearnedSkills();
    LivingStats stats = player.getStats();

    // 1. 若玩家尚未修練任何 MUD 技能，保留成員既有技能並進行基礎縮放
    if (mudSkills == null || mudSkills.isEmpty()) {
      if (member.getSkills() != null && !member.getSkills().isEmpty()) {
        List<PartyMemberSkill> scaled = new ArrayList<>();
        for (PartyMemberSkill base : member.getSkills()) {
          scaled.add(scaleSkill(base, 1, stats));
        }
        member.setSkills(scaled);
      }
      return;
    }

    // 2. 依據 MUD 技能映射對應的 DRPG 戰術技能與最高等級
    Map<String, Integer> targetDrpgSkills = new LinkedHashMap<>();

    for (Map.Entry<String, SkillEntry> entry : mudSkills.entrySet()) {
      String mudId = entry.getKey();
      int lvl = Math.max(1, entry.getValue().getLevel());

      List<String> drpgIds = mapMudSkillToDrpgSkills(mudId, lvl);
      for (String dId : drpgIds) {
        targetDrpgSkills.merge(dId, lvl, Math::max);
      }
    }

    // 3. 保留成員身上原先具有但未被 MUD 映射覆蓋的技能 (如道具學習的血肉道核)
    if (member.getSkills() != null) {
      for (PartyMemberSkill existing : member.getSkills()) {
        targetDrpgSkills.putIfAbsent(existing.getId(), 1);
      }
    }

    // 4. 實例化並縮放 DRPG 技能
    List<PartyMemberSkill> resolvedSkills = new ArrayList<>();
    for (Map.Entry<String, Integer> target : targetDrpgSkills.entrySet()) {
      String drpgId = target.getKey();
      int lvl = target.getValue();

      var opt = templateReader.findPartySkill(drpgId);
      if (opt.isPresent()) {
        PartyMemberSkill scaled = scaleSkill(opt.get(), lvl, stats);
        resolvedSkills.add(scaled);
      } else {
        // 若找不到模板，嘗試保留既有成員實例
        if (member.getSkills() != null) {
          member.getSkills().stream()
              .filter(s -> s.getId().equalsIgnoreCase(drpgId))
              .findFirst()
              .ifPresent(s -> resolvedSkills.add(scaleSkill(s, lvl, stats)));
        }
      }
    }

    if (!resolvedSkills.isEmpty()) {
      member.setSkills(resolvedSkills);
      log.debug("Synced and scaled {} DRPG skills for member [{}] from player [{}]",
          resolvedSkills.size(), member.getName(), player.getName());
    }
  }

  /**
   * 根據 MUD 技能 ID 與等級，動態求得對應解鎖的 DRPG 戰術技能清單 (完全由 SkillDefinition 與 SkillBridgeRule 資料驅動)
   */
  public List<String> mapMudSkillToDrpgSkills(String mudSkillId, int level) {
    List<String> results = new ArrayList<>();
    if (mudSkillId == null) return results;

    // 1. 優先由 TemplateReader 查取 SkillTemplate 轉換為 Canonical SkillDefinition
    var skillOpt = templateReader.findSkill(mudSkillId);
    List<com.example.htmlmud.domain.model.definition.SkillBridgeRule> rules = null;
    if (skillOpt.isPresent()) {
      var def = skillOpt.get().toDefinition();
      if (def != null && def.hasBridgeRules()) {
        rules = def.bridgeRules();
      }
    }

    // 2. 若未載入模板，則自 SkillDefinition 預設規則庫讀取
    if (rules == null || rules.isEmpty()) {
      rules = com.example.htmlmud.domain.model.definition.SkillDefinition.resolveDefaultBridges(mudSkillId);
    }

    // 3. 依等級檢定判定解鎖
    for (var rule : rules) {
      if (level >= rule.requiredLevel()) {
        if (!results.contains(rule.targetDrpgSkillId())) {
          results.add(rule.targetDrpgSkillId());
        }
      }
    }

    return results;
  }

  /**
   * 根據 MUD 熟練度等級與角色核心屬性，動態縮放 DRPG 技能威力與 CD
   */
  public PartyMemberSkill scaleSkill(PartyMemberSkill base, int mudSkillLevel, LivingStats stats) {
    if (base == null) return null;
    int lvl = Math.max(1, mudSkillLevel);

    // 計算屬性補正：法術/治療看 INT+WIS，物理/戰術看 STR+DEX
    double statBonus = 0.0;
    if (stats != null) {
      if (base.isHeal() || base.getId().startsWith("taoist_") || base.getId().startsWith("star_")) {
        statBonus = (stats.getIntelligence() + stats.getWis()) * 0.005;
      } else {
        statBonus = (stats.getStr() + stats.getDex()) * 0.005;
      }
    }

    // 傷害倍率：每等級提升 5%，加上屬性補正
    double newMultiplier = Math.round(base.getDamageMultiplier() * (1.0 + (lvl - 1) * 0.05 + statBonus) * 100.0) / 100.0;

    // 冷卻時間：每等級縮減 100ms (下限 1000ms)
    long newCd = Math.max(1000L, base.getCooldownMs() - (long) (lvl - 1) * 100L);

    // 治療與道心平復增益
    int newHeal = (int) Math.round(base.getHealAmount() * (1.0 + (lvl - 1) * 0.08 + statBonus));
    int newSan = (int) Math.round(base.getSanRestore() * (1.0 + (lvl - 1) * 0.05));

    String descSuffix = (lvl > 1) ? " 【道法 Lv." + lvl + " 強化】" : "";

    return PartyMemberSkill.builder()
        .id(base.getId())
        .name(base.getName())
        .icon(base.getIcon())
        .description(base.getDescription() + descSuffix)
        .costType(base.getCostType())
        .costValue(base.getCostValue())
        .cooldownMs(newCd)
        .damageMultiplier(newMultiplier)
        .aoe(base.isAoe())
        .heal(base.isHeal())
        .taunt(base.isTaunt())
        .stun(base.isStun())
        .stunDurationSeconds(base.getStunDurationSeconds())
        .healAmount(newHeal)
        .sanRestore(newSan)
        .allowedWeapons(base.getAllowedWeapons() != null ? new ArrayList<>(base.getAllowedWeapons()) : List.of())
        .build();
  }

  /**
   * 戰鬥勝利結算時，為玩家當前修練之 MUD 技能提供戰鬥修為回饋
   */
  public void awardCombatSkillXp(Player player, long totalCombatXp) {
    if (player == null || totalCombatXp <= 0) return;
    Map<String, SkillEntry> skills = player.getLearnedSkills();
    if (skills == null || skills.isEmpty()) return;

    long skillGain = Math.max(5, totalCombatXp / 4);

    for (Map.Entry<String, SkillEntry> entry : skills.entrySet()) {
      SkillEntry se = entry.getValue();
      com.example.htmlmud.domain.model.template.SkillTemplate tmpl =
          templateReader != null ? templateReader.findSkill(entry.getKey()).orElse(null) : null;
      var res = xpProgressionService.awardSkillExp(se, tmpl, skillGain);
      if (res.leveledUp() && player.isValid()) {
        player.reply("\u001B[1;36m💡【武學頓悟】在太陰死鬥中歷經磨礪，你的技能【" + res.skillName() + "】突破精進至 Lv." + res.newLevel() + "！\u001B[0m");
      }
    }
  }

  /**
   * 反查解鎖特定 DRPG 技能所需之 MUD 前置武學 (供 UI 或指引呈現)
   */
  public Set<String> getPrerequisiteMudSkills(String drpgSkillId) {
    Set<String> prereqs = new LinkedHashSet<>();
    if (drpgSkillId == null) return prereqs;

    Map<String, com.example.htmlmud.domain.model.template.SkillTemplate> allSkills = templateReader.getAllSkills();
    if (allSkills != null && !allSkills.isEmpty()) {
      for (var entry : allSkills.entrySet()) {
        var def = entry.getValue().toDefinition();
        if (def != null && def.bridgeRules() != null) {
          for (var rule : def.bridgeRules()) {
            if (drpgSkillId.equalsIgnoreCase(rule.targetDrpgSkillId())) {
              String label = entry.getValue().getId() + (rule.requiredLevel() > 1 ? " (Lv." + rule.requiredLevel() + ")" : "");
              prereqs.add(label);
            }
          }
        }
      }
    }

    if (prereqs.isEmpty()) {
      // 容錯備用查表：統一自 SkillDefinition 逆向反查規範橋接規則 (完全消除硬編碼 switch)
      prereqs.addAll(com.example.htmlmud.domain.model.definition.SkillDefinition.findDefaultPrerequisites(drpgSkillId));
    }
    return prereqs;
  }
}
