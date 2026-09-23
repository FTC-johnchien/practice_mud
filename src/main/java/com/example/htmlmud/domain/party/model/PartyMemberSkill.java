package com.example.htmlmud.domain.party.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 隊員專屬主動戰鬥技能
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyMemberSkill {
  private String id;
  private String name;
  private String icon;
  private String description;
  private ResourceType costType;
  private int costValue;
  @Builder.Default
  private int spCost = 0;
  @Builder.Default
  private int mpCost = 0;
  @Builder.Default
  private int hpCost = 0;
  private com.example.htmlmud.domain.model.enums.SkillType skillType;
  private String category; // WEAPON, CLASS, SPELL, COMBO, FORMATION, PASSIVE
  private long cooldownMs;
  @Builder.Default
  private double damageMultiplier = 1.0;
  @Builder.Default
  private boolean aoe = false;
  @Builder.Default
  private boolean heal = false;
  @Builder.Default
  private boolean taunt = false;
  @Builder.Default
  private boolean stun = false;
  @Builder.Default
  private int stunDurationSeconds = 0;
  @Builder.Default
  private int healAmount = 0;
  @Builder.Default
  private int sanRestore = 0;
  @Builder.Default
  private java.util.List<String> allowedWeapons = java.util.List.of();

  @Builder.Default
  private boolean synergy = false;
  @Builder.Default
  private java.util.List<String> requiredClasses = java.util.List.of();
  @Builder.Default
  private int minPartyAlive = 1;
  private String requiredFormation;
  @Builder.Default
  private int formationEnergyCost = 0;

  public boolean isWeaponAllowed(String weaponTypeStr) {
    if (allowedWeapons == null || allowedWeapons.isEmpty()) return true;
    if (weaponTypeStr == null) return false;
    for (String w : allowedWeapons) {
      if (w.equalsIgnoreCase(weaponTypeStr)) return true;
    }
    return false;
  }

  public static PartyMemberSkill fromSkillTemplate(com.example.htmlmud.domain.model.template.SkillTemplate tpl) {
    if (tpl == null) return null;

    ResourceType costType = ResourceType.MP;
    int costValue = 0;
    int sp = 0;
    int mp = 0;
    int hp = 0;

    if (tpl.getCosts() != null) {
      sp = tpl.getCosts().sp();
      mp = tpl.getCosts().mp();
      hp = tpl.getCosts().hp();

      if (mp > 0) {
        costType = ResourceType.MP;
        costValue = mp;
      } else if (sp > 0) {
        costType = ResourceType.SP;
        costValue = sp;
      } else if (hp > 0) {
        costType = ResourceType.HP;
        costValue = hp;
      }
    }
    if (tpl.getTags() != null) {
      if (tpl.getTags().contains("SP")) {
        costType = ResourceType.SP;
        costValue = sp;
      } else if (tpl.getTags().contains("COMBO")) {
        costType = ResourceType.COMBO;
        costValue = (tpl.getCosts() != null && tpl.getCosts().charge() > 0) ? tpl.getCosts().charge() : 2;
      } else if (tpl.getTags().contains("RAGE")) {
        costType = ResourceType.RAGE;
        costValue = (tpl.getCosts() != null && tpl.getCosts().stamina() > 0) ? tpl.getCosts().stamina() : sp;
      } else if (tpl.getTags().contains("FORCE")) {
        costType = ResourceType.FORCE;
      }
    }

    double dmgMult = 1.0;
    if (tpl.getMechanics() != null && tpl.getMechanics().scaleFactor() > 0) {
      dmgMult = tpl.getMechanics().scaleFactor();
    }

    boolean isAoe = tpl.getTags() != null && tpl.getTags().contains("AOE");
    boolean isHeal = tpl.getTags() != null && tpl.getTags().contains("HEAL");
    boolean isTaunt = tpl.getTags() != null && tpl.getTags().contains("TAUNT");
    boolean isStun = tpl.getTags() != null && (tpl.getTags().contains("STUN") || tpl.getTags().contains("SEAL"));
    int healAmt = (isHeal && tpl.getMechanics() != null) ? tpl.getMechanics().damage() : 0;
    int sanVal = (tpl.getTags() != null && tpl.getTags().contains("SAN")) ? 15 : 0;

    java.util.List<String> weapons = java.util.List.of();
    if (tpl.getUsage() != null && tpl.getUsage().allowedWeapons() != null) {
      weapons = tpl.getUsage().allowedWeapons().stream().map(Enum::name).toList();
    }

    String category = "CLASS";
    if (tpl.getType() == com.example.htmlmud.domain.model.enums.SkillType.PASSIVE) {
      category = "PASSIVE";
    } else if (tpl.getType() == com.example.htmlmud.domain.model.enums.SkillType.COMBO) {
      category = "COMBO";
    } else if (tpl.getType() == com.example.htmlmud.domain.model.enums.SkillType.FORMATION) {
      category = "FORMATION";
    } else if (!weapons.isEmpty()) {
      category = "WEAPON";
    } else if (costType == ResourceType.MP || (tpl.getTags() != null && tpl.getTags().contains("MAGIC"))) {
      category = "SPELL";
    }

    String icon = "⚡";
    if (isHeal) icon = "🌿";
    else if (isTaunt) icon = "🛡️";
    else if (isStun) icon = "📜";
    else if (tpl.getSchool() != null && tpl.getSchool().equalsIgnoreCase("SWORD")) icon = "🗡️";
    else if (tpl.getSchool() != null && tpl.getSchool().equalsIgnoreCase("DAGGER")) icon = "⚡";

    return PartyMemberSkill.builder()
        .id(tpl.getId())
        .name(tpl.getName())
        .icon(icon)
        .description(tpl.getDescription())
        .skillType(tpl.getType())
        .category(category)
        .costType(costType)
        .costValue(costValue)
        .spCost(sp)
        .mpCost(mp)
        .hpCost(hp)
        .cooldownMs(5000L)
        .damageMultiplier(dmgMult)
        .aoe(isAoe)
        .heal(isHeal)
        .taunt(isTaunt)
        .stun(isStun)
        .stunDurationSeconds(isStun ? 4 : 0)
        .healAmount(healAmt)
        .sanRestore(sanVal)
        .allowedWeapons(weapons)
        .build();
  }
}
