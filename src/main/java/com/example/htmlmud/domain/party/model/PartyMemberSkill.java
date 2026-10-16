package com.example.htmlmud.domain.party.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 隊員專屬主動戰鬥技能 (升級實作大一統規範技能 UnifiedSkill)
 * 同時具備修煉境界 (道法等級/熟練度/升級門檻) 與回合戰術 (CD/消耗/傷害倍率/打斷標籤)
 */
@Data
@Builder
@AllArgsConstructor
public class PartyMemberSkill implements UnifiedSkill {

  public PartyMemberSkill() {
    super();
  }

  private String id;
  private String name;
  private String icon;
  private String description;
  private CombatResourceType costType;

  // === SP-06: 大一統修煉境界屬性 (Level 1~10 & Mastery XP) ===
  @Builder.Default
  private int level = 1;
  @Builder.Default
  private long xp = 0L;
  @Builder.Default
  private long maxXp = 100L;
  @Builder.Default
  private double masteryMultiplier = 1.0;

  // === SP-06: 敘事文本 (招式動態播報) ===
  @Builder.Default
  private java.util.List<String> moves = new java.util.ArrayList<>();

  @Deprecated
  public ResourceType getLegacyCostType() {
    return ResourceType.fromCombatResourceType(costType);
  }

  @Deprecated
  public void setLegacyCostType(ResourceType legacyType) {
    this.costType = legacyType != null ? legacyType.toCombatResourceType() : null;
  }
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
  private boolean shield = false;
  @Builder.Default
  private boolean buff = false;
  @Builder.Default
  private boolean defense = false;
  private com.example.htmlmud.domain.model.config.BuffConfig buffConfig;
  @Builder.Default
  private java.util.List<String> tags = java.util.List.of();
  @Builder.Default
  private java.util.List<String> allowedWeapons = java.util.List.of();
  @Builder.Default
  private java.util.List<String> aliases = java.util.List.of();

  // Phase 11: 戰鬥狀態機、GCD 與施法唱條屬性
  @Builder.Default
  private long gcdMs = 1200L;
  @Builder.Default
  private boolean triggersGcd = true;
  @Builder.Default
  private long castTimeMs = 0L;
  @Builder.Default
  private boolean interruptible = true;
  @Builder.Default
  private int threatBonus = 0;
  @Builder.Default
  private String damageType = "PHYSICAL";
  private String fxKey;
  @Builder.Default
  private int poiseDamage = 0;

  public boolean isInstant() {
    return castTimeMs <= 0;
  }

  public boolean isShield() {
    return shield || (tags != null && tags.contains("SHIELD"));
  }

  public boolean isBuff() {
    return buff || (tags != null && tags.contains("BUFF"));
  }

  public boolean isDefense() {
    return defense || (tags != null && tags.contains("DEFENSE"));
  }

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

  /**
   * 判定技能是否匹配指定 ID 或別名 (完全資料驅動)
   */
  public boolean matchesIdOrAlias(String targetId) {
    if (targetId == null) return false;
    if (id != null && id.equalsIgnoreCase(targetId)) return true;
    if (aliases != null) {
      for (String a : aliases) {
        if (targetId.equalsIgnoreCase(a)) return true;
      }
    }
    return false;
  }

  // === SP-06: UnifiedSkill 修煉境界與動態縮放實作 ===
  public static final int MAX_SKILL_LEVEL = 10;

  @Override
  public boolean isMaxLevel() {
    return level >= MAX_SKILL_LEVEL;
  }

  @Override
  public double getProficiencyProgress() {
    if (isMaxLevel()) return 1.0;
    if (maxXp <= 0) return 0.0;
    return Math.min(1.0, Math.max(0.0, (double) xp / maxXp));
  }

  @Override
  public void addXp(long amount) {
    if (amount <= 0 || isMaxLevel()) return;
    this.xp += amount;
    while (this.xp >= this.maxXp && this.level < MAX_SKILL_LEVEL) {
      levelUp();
    }
  }

  @Override
  public void levelUp() {
    if (this.level >= MAX_SKILL_LEVEL) {
      this.level = MAX_SKILL_LEVEL;
      return;
    }
    this.xp = Math.max(0, this.xp - this.maxXp);
    this.level++;
    this.maxXp = calculateRequiredXpForLevel(this.level);
    this.masteryMultiplier = 1.0 + (this.level - 1) * 0.05;
  }

  public static long calculateRequiredXpForLevel(int lvl) {
    if (lvl <= 1) return 100L;
    return (long) Math.floor(100.0 * Math.pow(lvl, 1.4));
  }

  @Override
  public double getEffectiveDamageMultiplier() {
    int lvl = Math.max(1, this.level);
    double raw = this.damageMultiplier * (1.0 + (lvl - 1) * 0.05);
    return Math.round((raw + 1e-9) * 100.0) / 100.0;
  }

  @Override
  public long getEffectiveCooldownMs() {
    int lvl = Math.max(1, this.level);
    return Math.max(1000L, this.cooldownMs - (long) (lvl - 1) * 100L);
  }

  @Override
  public int getEffectiveHealAmount() {
    int lvl = Math.max(1, this.level);
    return (int) Math.round(this.healAmount * (1.0 + (lvl - 1) * 0.08));
  }

  @Override
  public int getEffectiveSanRestore() {
    int lvl = Math.max(1, this.level);
    return (int) Math.round(this.sanRestore * (1.0 + (lvl - 1) * 0.05));
  }

  public static PartyMemberSkill fromSkillTemplate(com.example.htmlmud.domain.model.template.SkillTemplate tpl) {
    if (tpl == null) return null;

    CombatResourceType costType = CombatResourceType.MP;
    int costValue = 0;
    int sp = 0;
    int mp = 0;
    int hp = 0;

    if (tpl.getCosts() != null) {
      sp = tpl.getCosts().sp();
      mp = tpl.getCosts().mp();
      hp = tpl.getCosts().hp();

      if (mp > 0) {
        costType = CombatResourceType.MP;
        costValue = mp;
      } else if (sp > 0) {
        costType = CombatResourceType.SP;
        costValue = sp;
      } else if (hp > 0) {
        costType = CombatResourceType.HP;
        costValue = hp;
      }
    }
    if (tpl.getTags() != null) {
      if (tpl.getTags().contains("SP")) {
        costType = CombatResourceType.SP;
        costValue = sp;
      } else if (tpl.getTags().contains("COMBO")) {
        costType = CombatResourceType.COMBO;
        costValue = (tpl.getCosts() != null && tpl.getCosts().charge() > 0) ? tpl.getCosts().charge() : 2;
      } else if (tpl.getTags().contains("RAGE")) {
        costType = CombatResourceType.RAGE;
        costValue = (tpl.getCosts() != null && tpl.getCosts().stamina() > 0) ? tpl.getCosts().stamina() : sp;
      } else if (tpl.getTags().contains("FORCE")) {
        costType = CombatResourceType.FORCE;
      }
    }

    double dmgMult = 1.0;
    if (tpl.getMechanics() != null && tpl.getMechanics().scaleFactor() > 0) {
      dmgMult = tpl.getMechanics().scaleFactor();
    }

    boolean isAoe = tpl.getTags() != null && tpl.getTags().contains("AOE");
    boolean isHeal = tpl.getTags() != null && tpl.getTags().contains("HEAL");
    boolean isTaunt = tpl.getTags() != null && tpl.getTags().contains("TAUNT");
    boolean isShield = tpl.getTags() != null && tpl.getTags().contains("SHIELD");
    boolean isBuff = tpl.getTags() != null && tpl.getTags().contains("BUFF");
    boolean isDefense = tpl.getTags() != null && tpl.getTags().contains("DEFENSE");
    boolean isStun = tpl.getTags() != null && (tpl.getTags().contains("STUN") || tpl.getTags().contains("SEAL"));
    int healAmt = (isHeal && tpl.getMechanics() != null) ? tpl.getMechanics().damage() : 0;
    int sanVal = (tpl.getTags() != null && tpl.getTags().contains("SAN")) ? 15 : 0;
    java.util.List<String> rawTags = tpl.getTags() != null ? new java.util.ArrayList<>(tpl.getTags()) : java.util.List.of();

    java.util.List<String> weapons = java.util.List.of();
    if (tpl.getUsage() != null && tpl.getUsage().allowedWeapons() != null) {
      weapons = tpl.getUsage().allowedWeapons().stream()
          .filter(java.util.Objects::nonNull)
          .map(Enum::name)
          .toList();
    }

    java.util.List<String> moveNames = new java.util.ArrayList<>();
    if (tpl.getMoves() != null) {
      for (var m : tpl.getMoves()) {
        if (m != null && m.name() != null && !m.name().isBlank()) {
          moveNames.add(m.name());
        }
      }
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
    } else if (costType == CombatResourceType.MP || (tpl.getTags() != null && tpl.getTags().contains("MAGIC"))) {
      category = "SPELL";
    }

    String icon = "⚡";
    if (isHeal) icon = "🌿";
    else if (isTaunt || isShield || isDefense) icon = "🛡️";
    else if (isBuff) icon = "✨";
    else if (isStun) icon = "📜";
    else if (tpl.getSchool() != null && tpl.getSchool().equalsIgnoreCase("SWORD")) icon = "🗡️";
    else if (tpl.getSchool() != null && tpl.getSchool().equalsIgnoreCase("DAGGER")) icon = "⚡";

    long gcdTime = 1200L;
    boolean trigGcd = true;
    if (rawTags.contains("OFF_GCD") || rawTags.contains("INSTANT_DEFENSE")) {
      trigGcd = false;
      gcdTime = 0L;
    }

    long castTime = 0L;
    if (tpl.getTiming() != null && tpl.getTiming().castTime() > 0) {
      castTime = (long) tpl.getTiming().castTime();
    } else if (rawTags.contains("CAST_1S")) {
      castTime = 1000L;
    } else if (rawTags.contains("CAST_1_5S")) {
      castTime = 1500L;
    } else if (rawTags.contains("CAST_2S")) {
      castTime = 2000L;
    } else if (rawTags.contains("CAST_3S")) {
      castTime = 3000L;
    }

    boolean isInterrupt = (tpl.getTiming() != null) ? tpl.getTiming().interruptible() : !rawTags.contains("UNINTERRUPTIBLE");

    int threatVal = isTaunt ? 600 : (rawTags.contains("HIGH_THREAT") ? 300 : 0);

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
        .shield(isShield)
        .buff(isBuff)
        .defense(isDefense)
        .buffConfig(tpl.getBuff())
        .tags(rawTags)
        .stun(isStun)
        .stunDurationSeconds(isStun ? 4 : 0)
        .healAmount(healAmt)
        .sanRestore(sanVal)
        .allowedWeapons(weapons)
        .moves(moveNames)
        .gcdMs(gcdTime)
        .triggersGcd(trigGcd)
        .castTimeMs(castTime)
        .interruptible(isInterrupt)
        .threatBonus(threatVal)
        .build();
  }
}
