package com.example.htmlmud.domain.party.model;

import java.util.EnumMap;
import java.util.Map;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.domain.service.TemplateCatalog;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PartyMember {
  private String id;
  private String name;
  private String roleTitle;
  private String classId;
  private RowPosition row;
  private LivingStats stats;
  @Builder.Default
  private int baseMinDamage = 10;
  @Builder.Default
  private int baseMaxDamage = 20;
  @Builder.Default
  private int baseDefense = 5;
  @Builder.Default
  private int currentSan = 100;
  @Builder.Default
  private int maxSan = 100;
  @Builder.Default
  private boolean alive = true;
  @Builder.Default
  private CombatResourceType resourceType = CombatResourceType.MP;

  @Deprecated
  public ResourceType getLegacyResourceType() {
    return ResourceType.fromCombatResourceType(resourceType);
  }

  @Deprecated
  public void setLegacyResourceType(ResourceType type) {
    this.resourceType = type != null ? type.toCombatResourceType() : null;
  }

  public void setResourceType(CombatResourceType type) {
    this.resourceType = type;
  }
  @Builder.Default
  private int currentSp = 0;
  @Builder.Default
  private int maxSp = 100;
  @Builder.Default
  private int currentRage = 0;
  @Builder.Default
  private int maxRage = 100;
  @Builder.Default
  private int currentCombo = 0;
  @Builder.Default
  private int maxCombo = 5;
  @Builder.Default
  private java.util.List<String> quickSkillIds = new java.util.ArrayList<>();
  @Builder.Default
  private long attackIntervalMs = 2000;
  @Builder.Default
  private long nextAttackTime = 0;
  @Builder.Default
  private java.util.Map<String, Long> cooldownUntil = new java.util.concurrent.ConcurrentHashMap<>();
  @Builder.Default
  private java.util.List<PartyMemberSkill> skills = new java.util.ArrayList<>();
  @Builder.Default
  private java.util.Map<com.example.htmlmud.domain.model.enums.SkillCategory, String> enabledSkills = new java.util.concurrent.ConcurrentHashMap<>();
  @Builder.Default
  private java.util.Set<String> learnedStances = java.util.concurrent.ConcurrentHashMap.newKeySet();

  public enum MadnessState {
    SANE,         // 正常
    CHAOS,        // 第一階段：走火入魔混亂期 (SAN == 0, 計數中)
    SEALED,       // 封印鎮魔狀態 (計數暫停, 定身無法行動)
    ABERRATION,   // 第二階段：徹底異變古神眷族 (HP x10, 敵對狂暴)
    DEAD_MEAT     // 血肉崩潰 (第一階段自殘至死, 永久陣亡)
  }

  @Builder.Default
  private MadnessState madnessState = MadnessState.SANE;
  @Builder.Default
  private int aberrationCounter = 0;
  @Builder.Default
  private int maxAberrationCounter = 100;

  // 7 大部位裝備槽位 (全部位容器)
  @Builder.Default
  private Map<EquipmentSlot, PartyItemSlot> equipment = new EnumMap<>(EquipmentSlot.class);

  // 仇恨值 (Threat / Aggro)
  @Builder.Default
  private int threat = 0;

  // 戰術方針規則清單 (Tactics / Gambit Rules)
  @Builder.Default
  private java.util.List<TacticsRule> tactics = new java.util.ArrayList<>();

  @JsonIgnore
  private transient TemplateReader templateReader;

  public void setTemplateReader(TemplateReader templateReader) {
    this.templateReader = templateReader;
  }

  @JsonIgnore
  public int getEffectiveMinDamage() {
    int bonus = (equipment != null) ? equipment.values().stream().mapToInt(PartyItemSlot::getBonusMinDamage).sum() : 0;
    return baseMinDamage + bonus;
  }

  @JsonIgnore
  public int getEffectiveMaxDamage() {
    int bonus = (equipment != null) ? equipment.values().stream().mapToInt(PartyItemSlot::getBonusMaxDamage).sum() : 0;
    return baseMaxDamage + bonus;
  }

  @JsonIgnore
  public int getEffectiveDefense() {
    int bonus = (equipment != null) ? equipment.values().stream().mapToInt(PartyItemSlot::getBonusDefense).sum() : 0;
    return baseDefense + bonus;
  }

  /**
   * 通用穿戴方法：換下同槽位舊裝備，動態更新氣血/道心加成
   */
  public PartyItemSlot equip(EquipmentSlot slot, PartyItemSlot item) {
    if (slot == null) return null;
    if (equipment == null) {
      equipment = new EnumMap<>(EquipmentSlot.class);
    }
    PartyItemSlot old = unequip(slot);
    if (item != null) {
      this.equipment.put(slot, item);
      if (item.getBonusHp() > 0 && stats != null) {
        stats.setMaxHp(stats.getMaxHp() + item.getBonusHp());
        stats.setHp(stats.getHp() + item.getBonusHp());
      }
      if (item.getBonusSan() > 0) {
        this.maxSan += item.getBonusSan();
        this.currentSan += item.getBonusSan();
      }
    }
    return old;
  }

  /**
   * 通用卸下方法：從槽位移除並扣減相應屬性加成
   */
  public PartyItemSlot unequip(EquipmentSlot slot) {
    if (slot == null || equipment == null) return null;
    PartyItemSlot old = this.equipment.remove(slot);
    if (old != null) {
      if (old.getBonusHp() > 0 && stats != null) {
        stats.setMaxHp(Math.max(1, stats.getMaxHp() - old.getBonusHp()));
        stats.setHp(Math.min(stats.getHp(), stats.getMaxHp()));
      }
      if (old.getBonusSan() > 0) {
        this.maxSan = Math.max(1, this.maxSan - old.getBonusSan());
        this.currentSan = Math.min(this.currentSan, this.maxSan);
      }
    }
    return old;
  }

  // --- 向下相容代理方法 ---

  public PartyItemSlot getEquippedWeapon() {
    return equipment != null ? equipment.get(EquipmentSlot.MAIN_HAND) : null;
  }

  public PartyItemSlot getEquippedArmor() {
    return equipment != null ? equipment.get(EquipmentSlot.BODY) : null;
  }

  public PartyItemSlot equipWeapon(PartyItemSlot weapon) {
    return equip(EquipmentSlot.MAIN_HAND, weapon);
  }

  public PartyItemSlot equipArmor(PartyItemSlot armor) {
    return equip(EquipmentSlot.BODY, armor);
  }

  public PartyItemSlot unequipWeapon() {
    return unequip(EquipmentSlot.MAIN_HAND);
  }

  public PartyItemSlot unequipArmor() {
    return unequip(EquipmentSlot.BODY);
  }

  public boolean learnSkill(PartyMemberSkill newSkill) {
    if (newSkill == null) return false;
    if (skills == null) {
      skills = new java.util.ArrayList<>();
    } else if (!(skills instanceof java.util.ArrayList)) {
      skills = new java.util.ArrayList<>(skills);
    }
    for (PartyMemberSkill s : skills) {
      if (s.getId().equalsIgnoreCase(newSkill.getId())) {
        return false; // 已掌握
      }
    }
    skills.add(newSkill);
    return true;
  }

  public String getSanityStatus() {
    if (madnessState == MadnessState.ABERRATION) {
      return "【不可名狀畸變】";
    }
    if (madnessState == MadnessState.SEALED) {
      return "【鎮魔封印中】";
    }
    if (madnessState == MadnessState.CHAOS) {
      return "【走火入魔 " + aberrationCounter + "%】";
    }
    double ratio = (double) currentSan / maxSan;
    if (ratio >= 0.8) {
      return "【道心澄澈】";
    } else if (ratio >= 0.5) {
      return "【心神恍惚】";
    } else if (ratio >= 0.2) {
      return "【狂亂囈語】";
    } else {
      return "【心魔滋生】";
    }
  }

  public void consumeSan(int amount) {
    this.currentSan = Math.max(0, this.currentSan - amount);
  }

  public void restoreSan(int amount) {
    this.currentSan = Math.min(this.maxSan, this.currentSan + amount);
  }

  public void setCurrentSp(int currentSp) {
    this.currentSp = Math.min(this.maxSp, Math.max(0, currentSp));
    this.currentRage = this.currentSp;
    this.currentCombo = Math.min(this.maxCombo, this.currentSp / 20);
  }

  public void setCurrentRage(int currentRage) {
    this.currentRage = Math.min(this.maxRage, Math.max(0, currentRage));
    this.currentSp = this.currentRage;
    this.currentCombo = Math.min(this.maxCombo, this.currentSp / 20);
  }

  public void setCurrentCombo(int currentCombo) {
    this.currentCombo = Math.min(this.maxCombo, Math.max(0, currentCombo));
    this.currentSp = Math.min(this.maxSp, this.currentCombo * 20);
    this.currentRage = this.currentSp;
  }

  public void gainSp(int amount) {
    this.currentSp = Math.min(this.maxSp, Math.max(0, this.currentSp + amount));
    this.currentRage = this.currentSp;
    this.currentCombo = Math.min(this.maxCombo, this.currentSp / 20);
  }

  public boolean consumeSp(int amount) {
    if (this.currentSp >= amount) {
      this.currentSp -= amount;
      this.currentRage = this.currentSp;
      this.currentCombo = Math.min(this.maxCombo, this.currentSp / 20);
      return true;
    }
    return false;
  }

  public void gainRage(int amount) {
    this.currentRage = Math.min(this.maxRage, this.currentRage + amount);
    this.currentSp = this.currentRage;
    this.currentCombo = Math.min(this.maxCombo, this.currentSp / 20);
  }

  public boolean consumeRage(int amount) {
    if (this.currentRage >= amount) {
      this.currentRage -= amount;
      this.currentSp = this.currentRage;
      this.currentCombo = Math.min(this.maxCombo, this.currentSp / 20);
      return true;
    }
    return false;
  }

  public void gainCombo(int amount) {
    this.currentCombo = Math.min(this.maxCombo, this.currentCombo + amount);
    this.currentSp = Math.min(this.maxSp, this.currentCombo * 20);
    this.currentRage = this.currentSp;
  }

  public boolean consumeCombo(int amount) {
    if (this.currentCombo >= amount) {
      this.currentCombo -= amount;
      this.currentSp = Math.min(this.maxSp, this.currentCombo * 20);
      this.currentRage = this.currentSp;
      return true;
    }
    return false;
  }

  public boolean consumeMp(int amount) {
    if (this.stats != null && this.stats.getMp() >= amount) {
      this.stats.setMp(this.stats.getMp() - amount);
      return true;
    }
    return false;
  }

  public boolean isLeader() {
    return id != null && (id.equals("m-leader") || id.contains("leader"));
  }

  public void setLeader(boolean leader) {
    if (leader) {
      if (this.id == null || !this.id.contains("leader")) {
        this.id = "m-leader";
      }
    } else {
      if (this.id != null && this.id.contains("leader")) {
        this.id = "m-companion";
      }
    }
  }

  public int getLevel() {
    return stats != null ? stats.getLevel() : 1;
  }

  public int getExp() {
    return stats != null ? stats.getExp() : 0;
  }

  public long getNextLevelExp() {
    return stats != null ? stats.getNextLevelExp() : 180;
  }

  public int getFreeStatPoints() {
    return stats != null ? stats.getFreeStatPoints() : 0;
  }

  public int getStr() {
    return stats != null ? stats.getStr() : 5;
  }

  public int getCon() {
    return stats != null ? stats.getCon() : 5;
  }

  public int getDex() {
    return stats != null ? stats.getDex() : 5;
  }

  public int getIntelligence() {
    return stats != null ? stats.getIntelligence() : 5;
  }

  public int getWis() {
    return stats != null ? stats.getWis() : 5;
  }

  public void restoreMp(int amount) {
    if (this.stats != null) {
      this.stats.setMp(Math.min(this.stats.getMaxMp(), this.stats.getMp() + amount));
    }
  }

  public boolean isOnCooldown(String skillId) {
    return cooldownUntil.getOrDefault(skillId, 0L) > System.currentTimeMillis();
  }

  public long getRemainingCooldownMs(String skillId) {
    return Math.max(0, cooldownUntil.getOrDefault(skillId, 0L) - System.currentTimeMillis());
  }

  public void setCooldown(String skillId, long cdMs) {
    cooldownUntil.put(skillId, System.currentTimeMillis() + cdMs);
  }

  public void resetCooldowns() {
    if (cooldownUntil != null) {
      cooldownUntil.clear();
    }
  }

  public void takeDamage(int damage) {
    if (stats != null) {
      stats.setHp(Math.max(0, stats.getHp() - damage));
      if (stats.getHp() <= 0) {
        this.alive = false;
      }
    }
    // 力士受傷增加怒氣
    if (this.resourceType == CombatResourceType.RAGE) {
      gainRage(15);
    }
  }

  public void heal(int amount) {
    if (stats != null) {
      stats.setHp(Math.min(stats.getMaxHp(), stats.getHp() + amount));
      if (stats.getHp() > 0) {
        this.alive = true;
      }
    }
  }

  public void addThreat(int amount) {
    this.threat = Math.max(0, this.threat + amount);
  }

  public void resetThreat() {
    this.threat = 0;
  }

  public com.example.htmlmud.domain.model.enums.WeaponType getMainHandWeaponType() {
    PartyItemSlot weapon = getEquippedWeapon();
    if (weapon == null || weapon.getSubType() == null) {
      return com.example.htmlmud.domain.model.enums.WeaponType.UNARMED;
    }
    String sub = weapon.getSubType().toUpperCase();
    try {
      return com.example.htmlmud.domain.model.enums.WeaponType.valueOf(sub);
    } catch (Exception ignored) {}

    if (sub.contains("SWORD")) return com.example.htmlmud.domain.model.enums.WeaponType.SWORD;
    if (sub.contains("BLADE")) return com.example.htmlmud.domain.model.enums.WeaponType.BLADE;
    if (sub.contains("HAMMER") || sub.contains("BLUNT") || sub.contains("MACE") || sub.contains("MAUL")) {
      return com.example.htmlmud.domain.model.enums.WeaponType.BLUNT;
    }
    if (sub.contains("DAGGER") || sub.contains("KNIFE")) return com.example.htmlmud.domain.model.enums.WeaponType.DAGGER;
    if (sub.contains("STAFF") || sub.contains("WAND") || sub.contains("ROD")) return com.example.htmlmud.domain.model.enums.WeaponType.STAFF;
    if (sub.contains("BOW")) return com.example.htmlmud.domain.model.enums.WeaponType.BOW;
    if (sub.contains("AXE") || sub.contains("PICKAXE")) return com.example.htmlmud.domain.model.enums.WeaponType.AXE;
    if (sub.contains("SPEAR") || sub.contains("POLEARM") || sub.contains("SCYTHE")) return com.example.htmlmud.domain.model.enums.WeaponType.POLEARM;

    return com.example.htmlmud.domain.model.enums.WeaponType.UNARMED;
  }

  public String getBasicSkillId() {
    com.example.htmlmud.domain.model.enums.WeaponType wt = getMainHandWeaponType();
    return switch (wt) {
      case SWORD -> "basic_sword";
      case BLADE -> "basic_blade";
      case BLUNT, HAMMER, MACE, MAUL, CLUB, FLAIL -> "basic_blunt";
      case DAGGER, DIRK, KNIFE, STILETTO -> "basic_dagger";
      case STAFF, WAND, ROD, SCEPTER -> "basic_magic_staff";
      case BOW, CROSSBOW -> "basic_archery";
      case AXE, POLEAXE -> "basic_axe";
      case POLEARM, HALBERD, SPEAR, JAVELIN -> "basic_polearm";
      case WHIP, CHAIN, ROPE -> "basic_whip";
      case DART, SHURIKEN, STONE -> "basic_throwing";
      default -> "basic_fist";
    };
  }

  public String getEffectiveBasicSkillId() {
    com.example.htmlmud.domain.model.enums.WeaponType wt = getMainHandWeaponType();
    com.example.htmlmud.domain.model.enums.SkillCategory cat = toSkillCategory(wt);
    if (enabledSkills != null && enabledSkills.containsKey(cat)) {
      String customSkill = enabledSkills.get(cat);
      if (customSkill != null && !customSkill.isBlank()) {
        return customSkill;
      }
    }
    return getBasicSkillId();
  }

  public com.example.htmlmud.domain.model.template.SkillTemplate getEnabledBasicSkill() {
    String skillId = getEffectiveBasicSkillId();
    return getTemplateReader().findSkill(skillId).orElse(null);
  }

  public void enableSkill(com.example.htmlmud.domain.model.enums.SkillCategory category, String skillId) {
    if (enabledSkills == null) {
      enabledSkills = new java.util.concurrent.ConcurrentHashMap<>();
    }
    if (skillId == null || skillId.isBlank()) {
      enabledSkills.remove(category);
    } else {
      enabledSkills.put(category, skillId);
    }
  }

  public void learnStance(String skillId) {
    if (learnedStances == null) {
      learnedStances = java.util.concurrent.ConcurrentHashMap.newKeySet();
    }
    if (skillId != null && !skillId.isBlank()) {
      learnedStances.add(skillId);
    }
  }

  public java.util.Set<String> getLearnedStances() {
    if (learnedStances == null) {
      learnedStances = java.util.concurrent.ConcurrentHashMap.newKeySet();
    }
    return learnedStances;
  }

  public static com.example.htmlmud.domain.model.enums.SkillCategory toSkillCategory(com.example.htmlmud.domain.model.enums.WeaponType wt) {
    if (wt == null) return com.example.htmlmud.domain.model.enums.SkillCategory.UNARMED;
    return switch (wt) {
      case SWORD -> com.example.htmlmud.domain.model.enums.SkillCategory.SWORD;
      case BLADE -> com.example.htmlmud.domain.model.enums.SkillCategory.BLADE;
      case BLUNT, HAMMER, MACE, MAUL, CLUB, FLAIL -> com.example.htmlmud.domain.model.enums.SkillCategory.HAMMER;
      case DAGGER, DIRK, KNIFE, STILETTO -> com.example.htmlmud.domain.model.enums.SkillCategory.DAGGER;
      case STAFF -> com.example.htmlmud.domain.model.enums.SkillCategory.STAFF;
      case WAND, ROD, SCEPTER -> com.example.htmlmud.domain.model.enums.SkillCategory.WAND;
      case BOW, CROSSBOW -> com.example.htmlmud.domain.model.enums.SkillCategory.BOW;
      case AXE, POLEAXE -> com.example.htmlmud.domain.model.enums.SkillCategory.AXE;
      case POLEARM, HALBERD -> com.example.htmlmud.domain.model.enums.SkillCategory.POLEARM;
      case SPEAR -> com.example.htmlmud.domain.model.enums.SkillCategory.SPEAR;
      case WHIP, CHAIN, ROPE -> com.example.htmlmud.domain.model.enums.SkillCategory.WHIP;
      case DART, SHURIKEN, STONE, JAVELIN -> com.example.htmlmud.domain.model.enums.SkillCategory.DAGGER;
      default -> com.example.htmlmud.domain.model.enums.SkillCategory.UNARMED;
    };
  }

  public static com.example.htmlmud.domain.model.enums.SkillCategory resolveSkillCategory(com.example.htmlmud.domain.model.template.SkillTemplate template) {
    if (template == null) return com.example.htmlmud.domain.model.enums.SkillCategory.UNARMED;

    // 1. 優先依據標籤判定被動心法 (DODGE, PARRY, FORCE)
    if (template.getTags() != null) {
      if (template.getTags().contains("DODGE") || template.getTags().contains("EVASION")) {
        return com.example.htmlmud.domain.model.enums.SkillCategory.DODGE;
      }
      if (template.getTags().contains("PARRY")) {
        return com.example.htmlmud.domain.model.enums.SkillCategory.PARRY;
      }
      if (template.getTags().contains("FORCE")) {
        return com.example.htmlmud.domain.model.enums.SkillCategory.FORCE;
      }
    }

    if (template.getType() == com.example.htmlmud.domain.model.enums.SkillType.REACTIVE) {
      if (template.getId() != null && template.getId().contains("dodge")) {
        return com.example.htmlmud.domain.model.enums.SkillCategory.DODGE;
      }
      if (template.getId() != null && template.getId().contains("parry")) {
        return com.example.htmlmud.domain.model.enums.SkillCategory.PARRY;
      }
    }
    if (template.getType() == com.example.htmlmud.domain.model.enums.SkillType.MAGIC) {
      return com.example.htmlmud.domain.model.enums.SkillCategory.MAGIC;
    }
    if (template.getUsage() != null && template.getUsage().allowedWeapons() != null) {
      for (var wt : template.getUsage().allowedWeapons()) {
        if (wt != null) {
          return toSkillCategory(wt);
        }
      }
    }
    if (template.getType() == com.example.htmlmud.domain.model.enums.SkillType.UNARMED) {
      return com.example.htmlmud.domain.model.enums.SkillCategory.UNARMED;
    }
    return com.example.htmlmud.domain.model.enums.SkillCategory.FORCE;
  }

  public String getEnabledPassiveSkillId(com.example.htmlmud.domain.model.enums.SkillCategory category) {
    return enabledSkills != null ? enabledSkills.get(category) : null;
  }

  public com.example.htmlmud.domain.model.template.SkillTemplate getEnabledPassive(com.example.htmlmud.domain.model.enums.SkillCategory category) {
    String id = getEnabledPassiveSkillId(category);
    return (id != null && getTemplateReader() != null) ? getTemplateReader().findSkill(id).orElse(null) : null;
  }

  public com.example.htmlmud.domain.model.config.MoveAction getRandomBasicMove() {
    var skill = getEnabledBasicSkill();
    if (skill != null && skill.getMoves() != null && !skill.getMoves().isEmpty()) {
      int idx = java.util.concurrent.ThreadLocalRandom.current().nextInt(skill.getMoves().size());
      return skill.getMoves().get(idx);
    }
    return null;
  }

  public boolean isSkillUsable(PartyMemberSkill skill) {
    if (skill == null) return false;
    if (skill.getAllowedWeapons() == null || skill.getAllowedWeapons().isEmpty()) {
      return true;
    }
    var wt = getMainHandWeaponType();
    return skill.isWeaponAllowed(wt.name()) || skill.isWeaponAllowed(wt.getDescription());
  }

  @JsonIgnore
  public java.util.Optional<com.example.htmlmud.domain.model.template.ClassTemplate> getClassTemplate() {
    if (this.classId == null || this.classId.isBlank()) {
      return java.util.Optional.empty();
    }
    return getTemplateReader().findClass(this.classId);
  }

  private TemplateReader getTemplateReader() {
    if (templateReader == null) {
      templateReader = new TemplateCatalog();
    }
    return templateReader;
  }

  @JsonIgnore
  public String getEffectiveClassName() {
    return getClassTemplate().map(com.example.htmlmud.domain.model.template.ClassTemplate::name).orElse(this.roleTitle);
  }

  public java.util.List<TacticsRule> getTactics() {
    if (tactics == null) {
      tactics = new java.util.ArrayList<>();
    }
    return tactics;
  }

  public void addTacticsRule(TacticsRule rule) {
    getTactics().add(rule);
    tactics.sort(java.util.Comparator.comparingInt(TacticsRule::getPriority));
  }

  public void clearTactics() {
    getTactics().clear();
  }

  public void resetTactics() {
    clearTactics();
    initDefaultTactics();
  }

  public void initDefaultTactics() {
    getTactics().clear();
    if (skills == null || skills.isEmpty()) return;

    int p = 1;
    // 1. 治療招式預設規則
    for (PartyMemberSkill s : skills) {
      if (s.isHeal()) {
        addTacticsRule(TacticsRule.builder()
            .priority(p++)
            .condition(TacticsCondition.ALLY_HP_LESS_THAN)
            .conditionValue(45)
            .target(TacticsTarget.LOWEST_HP_ALLY)
            .skillId(s.getId())
            .enabled(true)
            .build());
      }
    }

    // 2. 嘲諷招式預設規則
    for (PartyMemberSkill s : skills) {
      if (s.isTaunt()) {
        addTacticsRule(TacticsRule.builder()
            .priority(p++)
            .condition(TacticsCondition.ENEMY_COUNT_GTE)
            .conditionValue(2)
            .target(TacticsTarget.ALL_ENEMIES)
            .skillId(s.getId())
            .enabled(true)
            .build());
      }
    }

    // 3. 輸出招式預設規則
    for (PartyMemberSkill s : skills) {
      if (!s.isHeal() && !s.isTaunt()) {
        TacticsCondition cond = (s.getCostType() == CombatResourceType.SP || s.getCostType() == CombatResourceType.RAGE || s.getCostType() == CombatResourceType.COMBO)
            ? TacticsCondition.RESOURCE_GTE
            : TacticsCondition.ALWAYS;
        int val = (s.getCostType() == CombatResourceType.SP || s.getCostType() == CombatResourceType.RAGE) ? Math.max(30, s.getCostValue())
            : (s.getCostType() == CombatResourceType.COMBO) ? Math.max(3, s.getCostValue()) : 0;

        addTacticsRule(TacticsRule.builder()
            .priority(p++)
            .condition(cond)
            .conditionValue(val)
            .target(s.isAoe() ? TacticsTarget.ALL_ENEMIES : TacticsTarget.CURRENT_ENEMY)
            .skillId(s.getId())
            .enabled(true)
            .build());
      }
    }
  }
}
