package com.example.htmlmud.domain.party.model;

import java.util.EnumMap;
import java.util.Map;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
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
  private ResourceType resourceType = ResourceType.MP;
  @Builder.Default
  private int currentRage = 0;
  @Builder.Default
  private int maxRage = 100;
  @Builder.Default
  private int currentCombo = 0;
  @Builder.Default
  private int maxCombo = 5;
  @Builder.Default
  private long attackIntervalMs = 2000;
  @Builder.Default
  private long nextAttackTime = 0;
  @Builder.Default
  private java.util.Map<String, Long> cooldownUntil = new java.util.concurrent.ConcurrentHashMap<>();
  @Builder.Default
  private java.util.List<PartyMemberSkill> skills = new java.util.ArrayList<>();

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

  public void gainRage(int amount) {
    this.currentRage = Math.min(this.maxRage, this.currentRage + amount);
  }

  public boolean consumeRage(int amount) {
    if (this.currentRage >= amount) {
      this.currentRage -= amount;
      return true;
    }
    return false;
  }

  public void gainCombo(int amount) {
    this.currentCombo = Math.min(this.maxCombo, this.currentCombo + amount);
  }

  public boolean consumeCombo(int amount) {
    if (this.currentCombo >= amount) {
      this.currentCombo -= amount;
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

  public void takeDamage(int damage) {
    if (stats != null) {
      stats.setHp(Math.max(0, stats.getHp() - damage));
      if (stats.getHp() <= 0) {
        this.alive = false;
      }
    }
    // 力士受傷增加怒氣
    if (this.resourceType == ResourceType.RAGE) {
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
    if (sub.contains("AXE")) return com.example.htmlmud.domain.model.enums.WeaponType.AXE;
    if (sub.contains("SPEAR") || sub.contains("POLEARM")) return com.example.htmlmud.domain.model.enums.WeaponType.POLEARM;

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

  public com.example.htmlmud.domain.model.template.SkillTemplate getEnabledBasicSkill() {
    String skillId = getBasicSkillId();
    return com.example.htmlmud.infra.persistence.repository.TemplateRepository.findSkill(skillId).orElse(null);
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
}
