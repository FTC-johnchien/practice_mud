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

  @Autowired
  public SkillBridgeService(TemplateReader templateReader) {
    this.templateReader = templateReader != null ? templateReader : new TemplateCatalog();
  }

  public SkillBridgeService() {
    this(new TemplateCatalog());
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
   * 根據 MUD 技能 ID 與等級，動態求得對應解鎖的 DRPG 戰術技能清單
   */
  public List<String> mapMudSkillToDrpgSkills(String mudSkillId, int level) {
    List<String> results = new ArrayList<>();
    if (mudSkillId == null) return results;
    String id = mudSkillId.toLowerCase();

    // 劍系
    if (id.equals("basic_sword")) {
      results.add("sword_pierce");
      if (level >= 5) results.add("sword_storm");
    } else if (id.equals("taiji_sword") || id.equals("taiyin_sword") || id.equals("tianjian_sword")) {
      results.add("sword_pierce");
      results.add("sword_storm");
    }
    // 刀法 / 重兵系
    else if (id.equals("basic_blade") || id.equals("badao_blade") || id.equals("storm_blade")) {
      results.add("sword_pierce");
      if (level >= 3) results.add("tank_smash");
    } else if (id.equals("basic_axe") || id.equals("mountain_split_axe") || id.equals("basic_blunt")) {
      results.add("tank_smash");
    }
    // 守禦 / 嘲諷系
    else if (id.equals("basic_parry") || id.equals("iron_cloth")) {
      results.add("tank_taunt");
    }
    // 刺術 / 敏捷系
    else if (id.equals("basic_dagger")) {
      results.add("rogue_shadow_strike");
      if (level >= 5) results.add("rogue_seven_star");
    } else if (id.equals("shadow_strike")) {
      results.add("rogue_shadow_strike");
      results.add("rogue_seven_star");
    }
    // 醫道 / 復原系
    else if (id.equals("basic_first_aid")) {
      results.add("heal_single");
      if (level >= 5) results.add("heal_all_purify");
    } else if (id.equals("divine_healing")) {
      results.add("heal_single");
      results.add("heal_all_purify");
    }
    // 法術 / 符策系
    else if (id.equals("basic_magic")) {
      results.add("taoist_seal");
      if (level >= 5) results.add("taoist_thunder");
    } else if (id.equals("thunder_strike") || id.equals("fireball") || id.equals("ice_spear")) {
      results.add("taoist_seal");
      results.add("taoist_thunder");
    }
    // 心法 / 星宿系
    else if (id.equals("chaos_magic") || id.equals("violet_mist_force")) {
      results.add("star_warp");
      if (level >= 5) results.add("star_meteor");
    } else if (id.equals("zen_trance")) {
      results.add("star_warp");
      results.add("star_meteor");
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
      se.addXp(skillGain);

      // 簡化升級檢定：每級所需熟練度為 level * 100
      long needXp = Math.max(100L, se.getLevel() * 100L);
      if (se.getXp() >= needXp) {
        se.levelUp();
        if (player.isValid()) {
          player.reply("\u001B[1;36m💡【武學頓悟】在太陰死鬥中歷經磨礪，你的技能【" + entry.getKey() + "】突破精進至 Lv." + se.getLevel() + "！\u001B[0m");
        }
      }
    }
  }

  /**
   * 反查解鎖特定 DRPG 技能所需之 MUD 前置武學 (供 UI 或指引呈現)
   */
  public Set<String> getPrerequisiteMudSkills(String drpgSkillId) {
    Set<String> prereqs = new LinkedHashSet<>();
    if (drpgSkillId == null) return prereqs;

    switch (drpgSkillId.toLowerCase()) {
      case "sword_pierce" -> prereqs.addAll(List.of("basic_sword", "basic_blade", "taiji_sword"));
      case "sword_storm" -> prereqs.addAll(List.of("basic_sword (Lv.5)", "taiji_sword", "taiyin_sword", "tianjian_sword"));
      case "tank_smash" -> prereqs.addAll(List.of("basic_axe", "mountain_split_axe", "basic_blunt", "basic_blade (Lv.3)"));
      case "tank_taunt" -> prereqs.addAll(List.of("basic_parry", "iron_cloth"));
      case "rogue_shadow_strike" -> prereqs.addAll(List.of("basic_dagger", "shadow_strike"));
      case "rogue_seven_star" -> prereqs.addAll(List.of("basic_dagger (Lv.5)", "shadow_strike"));
      case "heal_single" -> prereqs.addAll(List.of("basic_first_aid", "divine_healing"));
      case "heal_all_purify" -> prereqs.addAll(List.of("basic_first_aid (Lv.5)", "divine_healing"));
      case "taoist_seal" -> prereqs.addAll(List.of("basic_magic", "thunder_strike"));
      case "taoist_thunder" -> prereqs.addAll(List.of("basic_magic (Lv.5)", "thunder_strike"));
      case "star_warp" -> prereqs.addAll(List.of("chaos_magic", "violet_mist_force", "zen_trance"));
      case "star_meteor" -> prereqs.addAll(List.of("zen_trance", "chaos_magic (Lv.5)"));
      default -> {}
    }
    return prereqs;
  }
}
