package com.example.htmlmud.domain.dungeon.battle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import com.example.htmlmud.domain.model.enums.DamageType;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.MobRank;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.domain.service.TemplateCatalog;
import com.example.htmlmud.domain.party.model.RowPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.example.htmlmud.domain.combat.Combatant;
import com.example.htmlmud.domain.model.enums.BuffCategory;
import java.util.Comparator;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleEnemy implements Combatant, Buffable {
  private String id;
  private String templateId;
  private String race;
  private String name;
  @Builder.Default
  private MobRank rank = MobRank.NORMAL;
  private boolean isUnique;
  private int hp;
  private int maxHp;
  @Builder.Default
  private int minDamage = 12;
  @Builder.Default
  private int maxDamage = 20;
  @Builder.Default
  private int defense = 5;
  @Builder.Default
  private int dex = 10;
  @Builder.Default
  private RowPosition row = RowPosition.FRONT;
    @Builder.Default
    private boolean isLarge = false;
    @Builder.Default
    private int size = 1;
    @Builder.Default
    private String slot = "CENTER";
    @Builder.Default
    private int rowIdx = 1;
    @Builder.Default
    private int colIdx = 2;
    @Builder.Default
    private int width = 1;
    @Builder.Default
    private int height = 1;
  @Builder.Default
  private long attackIntervalMs = 2200;
  @Builder.Default
  private long nextAttackTime = 0;
  @Builder.Default
  private long stunnedUntil = 0;
  @Builder.Default
  private boolean alive = true;
  @Builder.Default
  private int poise = 30;
  @Builder.Default
  private int maxPoise = 30;
  @Builder.Default
  private boolean poiseBroken = false;
  @Builder.Default
  private int poiseBrokenTicksRemaining = 0;
  @Builder.Default
  private int xp = 30;
  @Builder.Default
  private Map<String, Double> resistances = new HashMap<>();
  @Builder.Default
  private List<String> dropItemIds = new ArrayList<>();
  private String dropItemId;
  private String droppedDaoSkillId;
  private String droppedDaoSkillName;
  private String droppedDaoMemberName;

  // Phase 11: 獨立威脅表 (Threat Table) 與當前鎖定目標
  @Builder.Default
  private Map<String, Integer> threatTable = new java.util.concurrent.ConcurrentHashMap<>();
  private String targetMemberId;
  private String targetMemberName;
  private String tauntedByMemberId;
  @Builder.Default
  private long tauntedUntil = 0;

  @Builder.Default
  private Map<EquipmentSlot, String> equipment = new HashMap<>();
  @Builder.Default
  private List<String> skills = new ArrayList<>();
  private String dodgeSkillId;
  private String parrySkillId;

  // Telegraph / Intent 欄位 (ENEMY-INTENT-01)
  private String currentIntentIcon;
  private String currentIntentName;
  @Builder.Default
  private String currentIntentType = "PHYSICAL";
  @Builder.Default
  private String intentTargetScope = "SINGLE";
  @Builder.Default
  private boolean isCasting = false;
  @Builder.Default
  private long castStartTime = 0;
  @Builder.Default
  private long castDurationMs = 0;
  @Builder.Default
  private boolean interruptible = true;

  public void startIntent(String icon, String name, String type, String scope, long durationMs, boolean interruptible) {
    this.currentIntentIcon = icon;
    this.currentIntentName = name;
    this.currentIntentType = (type != null && !type.isBlank()) ? type : "PHYSICAL";
    this.intentTargetScope = (scope != null && !scope.isBlank()) ? scope : "SINGLE";
    this.castDurationMs = Math.max(0, durationMs);
    this.castStartTime = System.currentTimeMillis();
    this.isCasting = this.castDurationMs > 0;
    this.interruptible = interruptible;
  }

  public void startIntent(String icon, String name, String type, String scope, long durationMs) {
    startIntent(icon, name, type, scope, durationMs, this.interruptible);
  }

  public void clearIntent() {
    this.currentIntentIcon = null;
    this.currentIntentName = null;
    this.currentIntentType = "PHYSICAL";
    this.intentTargetScope = "SINGLE";
    this.isCasting = false;
    this.castDurationMs = 0;
    this.castStartTime = 0;
    this.interruptible = true;
  }

  public long getCastRemainingMs() {
    if (!isCasting || castDurationMs <= 0) return 0;
    long elapsed = System.currentTimeMillis() - castStartTime;
    return Math.max(0, castDurationMs - elapsed);
  }

  public void setCastRemainingMs(long remainingMs) {
    if (this.castDurationMs > 0) {
      this.castStartTime = System.currentTimeMillis() - (this.castDurationMs - Math.max(0, remainingMs));
    }
  }

  public String getCurrentAction() {
    return currentIntentName != null ? currentIntentName : "";
  }

  public void setCurrentAction(String action) {
    this.currentIntentName = action;
  }

  public int getActionProgressPercent() {
    if (!isCasting || castDurationMs <= 0) return 0;
    long elapsed = System.currentTimeMillis() - castStartTime;
    return (int) Math.min(100, Math.max(0, Math.round(((double) elapsed / castDurationMs) * 100.0)));
  }

  public boolean isInterruptible() {
    return interruptible;
  }

  public boolean getInterruptible() {
    return interruptible;
  }

  private String lastInterruptReason;

  /**
   * 觸發施法反制打斷：重置蓄力、破防失衡、進入力竭狀態
   */
  public boolean triggerInterrupt(String reason, long stunDurationMs) {
    if (!isCasting || !interruptible) {
      return false;
    }
    clearIntent();
    triggerPoiseBreak();
    if (stunDurationMs > 0) {
      this.stunnedUntil = Math.max(this.stunnedUntil, System.currentTimeMillis() + stunDurationMs);
    }
    this.nextAttackTime = System.currentTimeMillis() + Math.max(1500L, stunDurationMs);
    this.lastInterruptReason = reason != null ? reason : "反制打斷";
    return true;
  }

  public String popLastInterruptReason() {
    String r = this.lastInterruptReason;
    this.lastInterruptReason = null;
    return r;
  }

  public static BattleEnemy fromTemplate(String id, MobTemplate tpl, RowPosition row, String dropId,
      TemplateReader templateReader) {
    if (tpl == null) return null;
    int hp = tpl.maxHp() > 0 ? tpl.maxHp() : 100;
    int minD = tpl.minDamage() > 0 ? tpl.minDamage() : 10;
    int maxD = tpl.maxDamage() > 0 ? tpl.maxDamage() : 18;
    int def = tpl.defense();
    int dex = tpl.dex() > 0 ? tpl.dex() : 10;
    long attackInterval = tpl.attackSpeed() > 0 ? tpl.attackSpeed() : 2200;

    Map<EquipmentSlot, String> equipMap = new HashMap<>();
    if (tpl.equipment() != null) {
      for (var entry : tpl.equipment().entrySet()) {
        try {
          EquipmentSlot slot = EquipmentSlot.valueOf(entry.getKey().toUpperCase());
          equipMap.put(slot, entry.getValue());
          var itOpt = templateReader.findItem(entry.getValue());
          if (itOpt.isPresent()) {
            var it = itOpt.get();
            if (it.equipmentProp() != null) {
              minD += it.equipmentProp().minDamage();
              maxD += it.equipmentProp().maxDamage();
              def += it.equipmentProp().defense();
            }
          }
        } catch (Exception ignored) {}
      }
    }

    List<String> mobSkills = new ArrayList<>();
    String dodgeSkill = null;
    String parrySkill = null;

    if (tpl.enabledSkills() != null) {
      mobSkills.addAll(tpl.enabledSkills().values());
      dodgeSkill = tpl.enabledSkills().get(SkillCategory.DODGE);
      parrySkill = tpl.enabledSkills().get(SkillCategory.PARRY);
    }

    // 依據種族自動補齊天然防禦技能 (Dodge, Parry)
    String raceId = tpl.race();
    if (raceId != null) {
      var raceOpt = templateReader.findRace(raceId);
      if (raceOpt.isPresent() && raceOpt.get().combat() != null) {
        var raceCombat = raceOpt.get().combat();
        if (dodgeSkill == null && raceCombat.naturalDodge() != null) {
          dodgeSkill = raceCombat.naturalDodge();
          if (!mobSkills.contains(dodgeSkill)) mobSkills.add(dodgeSkill);
        }
        if (parrySkill == null && raceCombat.naturalParry() != null) {
          parrySkill = raceCombat.naturalParry();
          if (!mobSkills.contains(parrySkill)) mobSkills.add(parrySkill);
        }
      }
    }

    MobRank rank = tpl.rank() != null ? tpl.rank() : MobRank.NORMAL;
    boolean isUnique = tpl.isUnique();
    String displayName = (rank != MobRank.NORMAL ? rank.getPrefix() : "") + tpl.name();

    List<String> dropList = new ArrayList<>();
    if (tpl.loot() != null && !tpl.loot().isEmpty()) {
      for (com.example.htmlmud.domain.model.config.LootEntry entry : tpl.loot()) {
        if (ThreadLocalRandom.current().nextDouble() <= entry.chance()) {
          dropList.add(entry.itemId());
        }
      }
    } else if (dropId != null && !dropId.isBlank()) {
      dropList.add(dropId);
    }

    int maxP = tpl.maxPoise() > 0 ? tpl.maxPoise() : (tpl.poise() > 0 ? tpl.poise() : 30);
    if (tpl.maxPoise() <= 0 && tpl.poise() <= 0) {
      if (rank == MobRank.BOSS) {
        maxP = 400;
      } else if (rank == MobRank.ELITE) {
        maxP = 100;
      }
    }

    Map<String, Double> resMap = new HashMap<>();
    if (tpl.resistances() != null) {
      resMap.putAll(tpl.resistances());
    }

    long configuredCastDuration = tpl.castTimeMs() > 0 ? tpl.castTimeMs() : 0L;
    boolean isInterruptible = tpl.interruptible() != null ? tpl.interruptible() : true;

    return BattleEnemy.builder()
        .id(id)
        .templateId(tpl.id())
        .race(tpl.race())
        .name(displayName)
        .rank(rank)
        .isUnique(isUnique)
        .hp(hp)
        .maxHp(hp)
        .poise(maxP)
        .maxPoise(maxP)
        .resistances(resMap)
        .minDamage(minD)
        .maxDamage(maxD)
        .defense(def)
        .dex(dex)
        .row(row != null ? row : RowPosition.FRONT)
        .attackIntervalMs(attackInterval)
        .equipment(equipMap)
        .skills(mobSkills)
        .dodgeSkillId(dodgeSkill)
        .parrySkillId(parrySkill)
        .dropItemId(dropList.isEmpty() ? dropId : dropList.get(0))
        .dropItemIds(dropList)
        .alive(true)
        .xp(tpl.expReward() > 0 ? tpl.expReward() : 30)
        .castDurationMs(configuredCastDuration)
        .interruptible(isInterruptible)
        .build();
  }

  public double getResistance(DamageType type) {
    if (type == null || resistances == null) return 0.0;
    return resistances.getOrDefault(type.name(), 0.0);
  }

  public void heal(int amount) {
    if (amount <= 0 || !isAlive()) return;
    this.hp = Math.min(this.maxHp, this.hp + amount);
  }

  public List<String> getDropItemIds() {
    if (dropItemIds == null) {
      dropItemIds = new ArrayList<>();
    }
    if (dropItemIds.isEmpty() && dropItemId != null && !dropItemId.isBlank()) {
      dropItemIds.add(dropItemId);
    }
    return dropItemIds;
  }

  public static BattleEnemy fromTemplate(String id, MobTemplate tpl, RowPosition row, String dropId) {
    return fromTemplate(id, tpl, row, dropId, new TemplateCatalog());
  }

  @Builder.Default
  private List<ActiveBuff> activeBuffs = new ArrayList<>();

  public void takeDamage(int dmg) {
    int remainingDmg = absorbShieldDamage(dmg);
    if (remainingDmg > 0) {
      if (!poiseBroken) {
        // 未破防階段 (罡氣護體)：傷害折減為 1/3 (約 33% 輸出)
        remainingDmg = Math.max(1, (int) Math.round(remainingDmg / 3.0));
        // 受擊削韌：扣減破防條
        applyPoiseDamage(Math.max(10, dmg / 2));
      }
      this.hp = Math.max(0, this.hp - remainingDmg);
      if (this.hp <= 0) {
        this.alive = false;
      }
    }
  }

  @Override
  public void applyPoiseDamage(int amount) {
    if (poiseBroken || poise <= 0) return;
    this.poise = Math.max(0, this.poise - amount);
    if (this.poise == 0) {
      triggerPoiseBreak();
    }
  }

  public void triggerPoiseBreak() {
    this.poiseBroken = true;
    this.poise = 0;
    this.poiseBrokenTicksRemaining = 14; // 7 秒 (14 Ticks) 力竭期
    clearIntent(); // 打斷大招與施法意圖
  }

  @Override
  public void recoverPoise() {
    this.poiseBroken = false;
    this.poise = this.maxPoise;
    this.poiseBrokenTicksRemaining = 0;
  }

  @Override
  public String processPoiseTick() {
    if (poiseBroken) {
      poiseBrokenTicksRemaining--;
      if (poiseBrokenTicksRemaining <= 0) {
        recoverPoise();
        return "⚡【罡氣重聚】" + name + " 罡氣復甦重整旗鼓，重新獲得霸體護身！";
      }
    }
    return null;
  }

  @Override
  public int absorbShieldDamage(int incomingDmg) {
    int remaining = incomingDmg;
    if (activeBuffs != null && !activeBuffs.isEmpty()) {
      var shields = activeBuffs.stream()
          .filter(b -> b.getCategory() == BuffCategory.SHIELD && b.getValue() > 0 && !b.isExpired())
          .sorted(Comparator.comparingInt(b -> b.isPermanent() ? Integer.MAX_VALUE : b.getRemainingTicks()))
          .toList();

      for (ActiveBuff shield : shields) {
        if (remaining <= 0) break;
        int absorb = Math.min(remaining, shield.getValue());
        shield.setValue(shield.getValue() - absorb);
        remaining -= absorb;
      }
      activeBuffs.removeIf(ActiveBuff::isExpired);
    }
    return remaining;
  }

  @Override
  public int getTotalShield() {
    return (activeBuffs != null)
        ? activeBuffs.stream()
            .filter(b -> b.getCategory() == BuffCategory.SHIELD && !b.isExpired())
            .mapToInt(ActiveBuff::getValue)
            .sum()
        : 0;
  }

  @Override
  public List<ActiveBuff> getActiveBuffs() {
    if (activeBuffs == null) {
      activeBuffs = new ArrayList<>();
    }
    return activeBuffs;
  }

  @Override
  public boolean hasActiveBuff(String buffId) {
    if (buffId == null || activeBuffs == null || activeBuffs.isEmpty()) return false;
    return activeBuffs.stream().anyMatch(b -> b.getId().equalsIgnoreCase(buffId) && !b.isExpired());
  }

  @Override
  public ActiveBuff getActiveBuff(String buffId) {
    if (buffId == null || activeBuffs == null) return null;
    return activeBuffs.stream()
        .filter(b -> b.getId().equalsIgnoreCase(buffId) && !b.isExpired())
        .findFirst()
        .orElse(null);
  }

  @Override
  public void addBuff(ActiveBuff newBuff) {
    if (newBuff == null) return;
    if (activeBuffs == null) {
      activeBuffs = new ArrayList<>();
    }
    ActiveBuff existing = getActiveBuff(newBuff.getId());
    if (existing != null) {
      existing.setDurationTicks(newBuff.getDurationTicks());
      existing.setRemainingTicks(newBuff.getDurationTicks());
      if (newBuff.getCategory() == BuffCategory.SHIELD) {
        existing.setValue(Math.max(existing.getValue(), newBuff.getValue()));
      } else if (newBuff.getMaxStacks() > 1) {
        existing.setStacks(Math.min(existing.getMaxStacks(), existing.getStacks() + 1));
      }
      existing.resetTickCounter();
    } else {
      activeBuffs.add(newBuff);
    }
  }

  @Override
  public void removeBuff(String buffId) {
    if (buffId == null || activeBuffs == null) return;
    activeBuffs.removeIf(b -> b.getId().equalsIgnoreCase(buffId));
  }

  @Override
  public boolean isControlled() {
    if (!poiseBroken) {
      return false; // 霸體護身：未破防時免疫硬控
    }
    return Combatant.super.isControlled();
  }

  public boolean isStunned() {
    if (!poiseBroken) {
      return false; // 霸體護身：未破防時免疫暈眩
    }
    return (stunnedUntil > System.currentTimeMillis()) || isControlled();
  }

  public void applyStun(long durationMs) {
    if (!poiseBroken) return; // 霸體護身：未破防時免疫眩暈施加
    this.stunnedUntil = Math.max(this.stunnedUntil, System.currentTimeMillis() + durationMs);
  }

  public boolean isTaunted() {
    return tauntedByMemberId != null && tauntedUntil > System.currentTimeMillis();
  }

  public void setTaunt(String memberId, long durationMs) {
    this.tauntedByMemberId = memberId;
    this.tauntedUntil = System.currentTimeMillis() + durationMs;
    this.targetMemberId = memberId;
    // 仇恨同步：將嘲諷者的仇恨拉至當前最高仇恨 + 100，鞏固仇恨
    int top = getTopThreat();
    int current = getThreat(memberId);
    threatTable.put(memberId, Math.max(top + 100, current + 100));
  }

  public void addThreat(String memberId, int amount) {
    if (memberId == null || amount <= 0) return;
    threatTable.merge(memberId, amount, Integer::sum);
  }

  public int getThreat(String memberId) {
    return memberId != null ? threatTable.getOrDefault(memberId, 0) : 0;
  }

  public int getTopThreat() {
    return threatTable.values().stream().mapToInt(Integer::intValue).max().orElse(0);
  }

  public void resetThreat(String memberId) {
    if (memberId != null) threatTable.remove(memberId);
  }

  public void clearThreat() {
    threatTable.clear();
  }

  @Override
  public int getEffectiveMinDamage() {
    int base = this.minDamage;
    double atkPct = getStatModifierSum("ATK_PCT");
    if (atkPct != 0) {
      base = (int) Math.round(base * (1.0 + atkPct));
    }
    return Math.max(1, base);
  }

  @Override
  public int getEffectiveMaxDamage() {
    int base = this.maxDamage;
    double atkPct = getStatModifierSum("ATK_PCT");
    if (atkPct != 0) {
      base = (int) Math.round(base * (1.0 + atkPct));
    }
    return Math.max(1, base);
  }

  @Override
  public int getEffectiveDefense() {
    int base = this.defense;
    double defPct = getStatModifierSum("DEF_PCT");
    if (defPct != 0) {
      base = (int) Math.round(base * (1.0 + defPct));
    }
    return Math.max(0, base);
  }

  public long getEffectiveAttackIntervalMs() {
    double speedPct = getStatModifierSum("SPEED_PCT");
    if (speedPct != 0) {
      return Math.max(500, (long) Math.round(this.attackIntervalMs * Math.max(0.2, 1.0 - speedPct)));
    }
    return this.attackIntervalMs;
  }

  @Override
  public int getEffectiveDex() {
    return this.dex;
  }

  @Override
  public int getEffectiveStr() {
    return 10;
  }

  @Override
  public int getEffectiveCon() {
    return 10;
  }
}

