package com.example.htmlmud.domain.model.definition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.example.htmlmud.domain.model.enums.SkillType;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import lombok.Builder;

/**
 * 技能規範化模型 (Canonical Skill Definition)
 * 統一 MUD 世界模式與 DRPG 小隊模式之技能定義，透過 Facets 切分規則並支援資料驅動橋接。
 */
@Builder(toBuilder = true)
public record SkillDefinition(
    String id,
    String name,
    String description,
    String icon,
    SkillType type,
    String school,
    Set<String> tags,
    List<String> allowedWeapons,
    MudSkillRules mudRules,
    DrpgSkillRules drpgRules,
    List<SkillBridgeRule> bridgeRules
) {

  public boolean hasMudRules() {
    return mudRules != null;
  }

  public boolean hasDrpgRules() {
    return drpgRules != null;
  }

  public boolean hasBridgeRules() {
    return bridgeRules != null && !bridgeRules.isEmpty();
  }

  /**
   * 轉換為 DRPG 小隊技能實例 PartyMemberSkill
   */
  public PartyMemberSkill toPartyMemberSkill() {
    PartyMemberSkill.PartyMemberSkillBuilder builder = PartyMemberSkill.builder()
        .id(id)
        .name(name)
        .icon(icon != null ? icon : "⚡")
        .description(description)
        .skillType(type)
        .tags(tags != null ? new ArrayList<>(tags) : List.of())
        .allowedWeapons(allowedWeapons != null ? new ArrayList<>(allowedWeapons) : List.of());

    if (drpgRules != null) {
      builder.costType(drpgRules.costType())
          .costValue(drpgRules.costValue())
          .spCost(drpgRules.spCost())
          .mpCost(drpgRules.mpCost())
          .hpCost(drpgRules.hpCost())
          .cooldownMs(drpgRules.cooldownMs())
          .damageMultiplier(drpgRules.damageMultiplier())
          .aoe(drpgRules.aoe())
          .heal(drpgRules.heal())
          .taunt(drpgRules.taunt())
          .stun(drpgRules.stun())
          .stunDurationSeconds(drpgRules.stunDurationSeconds())
          .healAmount(drpgRules.healAmount())
          .sanRestore(drpgRules.sanRestore())
          .shield(drpgRules.shield())
          .buff(drpgRules.buff())
          .defense(drpgRules.defense())
          .buffConfig(drpgRules.buffConfig())
          .gcdMs(drpgRules.gcdMs())
          .triggersGcd(drpgRules.triggersGcd())
          .castTimeMs(drpgRules.castTimeMs())
          .interruptible(drpgRules.interruptible())
          .threatBonus(drpgRules.threatBonus())
          .synergy(drpgRules.synergy())
          .requiredClasses(drpgRules.requiredClasses() != null ? drpgRules.requiredClasses() : List.of())
          .minPartyAlive(drpgRules.minPartyAlive())
          .requiredFormation(drpgRules.requiredFormation())
          .formationEnergyCost(drpgRules.formationEnergyCost());
    }

    return builder.build();
  }

  /**
   * 由 MUD SkillTemplate 建立 SkillDefinition
   */
  public static SkillDefinition fromSkillTemplate(SkillTemplate tpl) {
    if (tpl == null) return null;

    MudSkillRules mud = MudSkillRules.builder()
        .learning(tpl.getLearning())
        .usage(tpl.getUsage())
        .costs(tpl.getCosts())
        .scaling(tpl.getScaling())
        .mechanics(tpl.getMechanics())
        .synergies(tpl.getSynergies())
        .moves(tpl.getMoves())
        .comboDefaults(tpl.getComboDefaults())
        .combo(tpl.getCombo())
        .counterDefaults(tpl.getCounterDefaults())
        .counter(tpl.getCounter())
        .messages(tpl.getMessages())
        .buff(tpl.getBuff())
        .build();

    // 透過 PartyMemberSkill.fromSkillTemplate 提取預設 DRPG 切面
    PartyMemberSkill pms = PartyMemberSkill.fromSkillTemplate(tpl);
    DrpgSkillRules drpg = null;
    if (pms != null) {
      drpg = DrpgSkillRules.builder()
          .costType(pms.getCostType())
          .costValue(pms.getCostValue())
          .spCost(pms.getSpCost())
          .mpCost(pms.getMpCost())
          .hpCost(pms.getHpCost())
          .cooldownMs(pms.getCooldownMs())
          .damageMultiplier(pms.getDamageMultiplier())
          .aoe(pms.isAoe())
          .heal(pms.isHeal())
          .taunt(pms.isTaunt())
          .stun(pms.isStun())
          .stunDurationSeconds(pms.getStunDurationSeconds())
          .healAmount(pms.getHealAmount())
          .sanRestore(pms.getSanRestore())
          .shield(pms.isShield())
          .buff(pms.isBuff())
          .defense(pms.isDefense())
          .buffConfig(pms.getBuffConfig())
          .gcdMs(pms.getGcdMs())
          .triggersGcd(pms.isTriggersGcd())
          .castTimeMs(pms.getCastTimeMs())
          .interruptible(pms.isInterruptible())
          .threatBonus(pms.getThreatBonus())
          .synergy(pms.isSynergy())
          .requiredClasses(pms.getRequiredClasses())
          .build();
    }

    List<String> allowedWeapons = List.of();
    if (tpl.getUsage() != null && tpl.getUsage().allowedWeapons() != null) {
      allowedWeapons = tpl.getUsage().allowedWeapons().stream()
          .filter(java.util.Objects::nonNull)
          .map(Enum::name)
          .toList();
    }

    List<SkillBridgeRule> bridges = (tpl.getBridges() != null && !tpl.getBridges().isEmpty())
        ? tpl.getBridges()
        : resolveDefaultBridges(tpl.getId());

    return SkillDefinition.builder()
        .id(tpl.getId())
        .name(tpl.getName())
        .description(tpl.getDescription())
        .icon(pms != null ? pms.getIcon() : "⚡")
        .type(tpl.getType())
        .school(tpl.getSchool())
        .tags(tpl.getTags() != null ? new HashSet<>(tpl.getTags()) : Set.of())
        .allowedWeapons(allowedWeapons)
        .mudRules(mud)
        .drpgRules(drpg)
        .bridgeRules(bridges)
        .build();
  }

  /**
   * 由 DRPG PartyMemberSkill 建立 SkillDefinition
   */
  public static SkillDefinition fromPartyMemberSkill(PartyMemberSkill pms) {
    if (pms == null) return null;

    DrpgSkillRules drpg = DrpgSkillRules.builder()
        .costType(pms.getCostType())
        .costValue(pms.getCostValue())
        .spCost(pms.getSpCost())
        .mpCost(pms.getMpCost())
        .hpCost(pms.getHpCost())
        .cooldownMs(pms.getCooldownMs())
        .damageMultiplier(pms.getDamageMultiplier())
        .aoe(pms.isAoe())
        .heal(pms.isHeal())
        .taunt(pms.isTaunt())
        .stun(pms.isStun())
        .stunDurationSeconds(pms.getStunDurationSeconds())
        .healAmount(pms.getHealAmount())
        .sanRestore(pms.getSanRestore())
        .shield(pms.isShield())
        .buff(pms.isBuff())
        .defense(pms.isDefense())
        .buffConfig(pms.getBuffConfig())
        .gcdMs(pms.getGcdMs())
        .triggersGcd(pms.isTriggersGcd())
        .castTimeMs(pms.getCastTimeMs())
        .interruptible(pms.isInterruptible())
        .threatBonus(pms.getThreatBonus())
        .synergy(pms.isSynergy())
        .requiredClasses(pms.getRequiredClasses())
        .minPartyAlive(pms.getMinPartyAlive())
        .requiredFormation(pms.getRequiredFormation())
        .formationEnergyCost(pms.getFormationEnergyCost())
        .build();

    return SkillDefinition.builder()
        .id(pms.getId())
        .name(pms.getName())
        .description(pms.getDescription())
        .icon(pms.getIcon())
        .type(pms.getSkillType() != null ? pms.getSkillType() : SkillType.ACTIVE)
        .tags(pms.getTags() != null ? new HashSet<>(pms.getTags()) : Set.of())
        .allowedWeapons(pms.getAllowedWeapons() != null ? pms.getAllowedWeapons() : List.of())
        .drpgRules(drpg)
        .bridgeRules(List.of())
        .build();
  }

  /**
   * 預設標準 MUD 到 DRPG 橋接規則解析 (提供開箱即用之資料驅動保底)
   */
  public static List<SkillBridgeRule> resolveDefaultBridges(String mudSkillId) {
    if (mudSkillId == null) return List.of();
    String id = mudSkillId.toLowerCase();

    return switch (id) {
      case "basic_sword" -> List.of(
          SkillBridgeRule.of("sword_pierce", 1),
          SkillBridgeRule.of("sword_storm", 5)
      );
      case "taiji_sword", "taiyin_sword", "tianjian_sword" -> List.of(
          SkillBridgeRule.of("sword_pierce", 1),
          SkillBridgeRule.of("sword_storm", 1)
      );
      case "basic_blade" -> List.of(
          SkillBridgeRule.of("sword_pierce", 1),
          SkillBridgeRule.of("tank_smash", 3)
      );
      case "badao_blade", "storm_blade" -> List.of(
          SkillBridgeRule.of("sword_pierce", 1),
          SkillBridgeRule.of("tank_smash", 1)
      );
      case "basic_axe", "mountain_split_axe", "basic_blunt" -> List.of(
          SkillBridgeRule.of("tank_smash", 1)
      );
      case "basic_parry", "iron_cloth" -> List.of(
          SkillBridgeRule.of("tank_taunt", 1)
      );
      case "basic_dagger" -> List.of(
          SkillBridgeRule.of("rogue_shadow_strike", 1),
          SkillBridgeRule.of("rogue_seven_star", 5)
      );
      case "shadow_strike" -> List.of(
          SkillBridgeRule.of("rogue_shadow_strike", 1),
          SkillBridgeRule.of("rogue_seven_star", 1)
      );
      case "basic_first_aid" -> List.of(
          SkillBridgeRule.of("heal_single", 1),
          SkillBridgeRule.of("heal_all_purify", 5)
      );
      case "divine_healing" -> List.of(
          SkillBridgeRule.of("heal_single", 1),
          SkillBridgeRule.of("heal_all_purify", 1)
      );
      case "basic_magic" -> List.of(
          SkillBridgeRule.of("taoist_seal", 1),
          SkillBridgeRule.of("taoist_thunder", 5)
      );
      case "thunder_strike", "fireball", "ice_spear" -> List.of(
          SkillBridgeRule.of("taoist_seal", 1),
          SkillBridgeRule.of("taoist_thunder", 1)
      );
      case "chaos_magic", "violet_mist_force" -> List.of(
          SkillBridgeRule.of("star_warp", 1),
          SkillBridgeRule.of("star_meteor", 5)
      );
      case "zen_trance" -> List.of(
          SkillBridgeRule.of("star_warp", 1),
          SkillBridgeRule.of("star_meteor", 1)
      );
      default -> List.of();
    };
  }

  private static final List<String> CANONICAL_MUD_SKILLS = List.of(
      "basic_sword", "taiji_sword", "taiyin_sword", "tianjian_sword",
      "basic_blade", "badao_blade", "storm_blade",
      "basic_axe", "mountain_split_axe", "basic_blunt",
      "basic_parry", "iron_cloth",
      "basic_dagger", "shadow_strike",
      "basic_first_aid", "divine_healing",
      "basic_magic", "thunder_strike", "fireball", "ice_spear",
      "chaos_magic", "violet_mist_force", "zen_trance"
  );

  /**
   * 根據預設規範橋接規則，反查解鎖特定 DRPG 技能所需之 MUD 前置武學 (Single Source of Truth)
   */
  public static List<String> findDefaultPrerequisites(String drpgSkillId) {
    if (drpgSkillId == null) return List.of();
    List<String> list = new ArrayList<>();
    for (String mId : CANONICAL_MUD_SKILLS) {
      for (var rule : resolveDefaultBridges(mId)) {
        if (drpgSkillId.equalsIgnoreCase(rule.targetDrpgSkillId())) {
          String label = mId + (rule.requiredLevel() > 1 ? " (Lv." + rule.requiredLevel() + ")" : "");
          if (!list.contains(label)) {
            list.add(label);
          }
        }
      }
    }
    return list;
  }
}

