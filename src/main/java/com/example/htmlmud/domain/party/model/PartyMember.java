package com.example.htmlmud.domain.party.model;

import com.example.htmlmud.domain.model.entity.LivingStats;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

  public String getSanityStatus() {
    double ratio = (double) currentSan / maxSan;
    if (ratio >= 0.8) {
      return "【道心澄澈】";
    } else if (ratio >= 0.5) {
      return "【心神恍惚】";
    } else if (ratio >= 0.2) {
      return "【狂亂囈語】";
    } else {
      return "【走火入魔】";
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
}
