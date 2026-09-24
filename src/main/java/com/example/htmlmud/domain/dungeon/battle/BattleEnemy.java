package com.example.htmlmud.domain.dungeon.battle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import com.example.htmlmud.domain.model.enums.BuffCategory;
import java.util.Comparator;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleEnemy implements Buffable {
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
  private long attackIntervalMs = 2200;
  @Builder.Default
  private long nextAttackTime = 0;
  @Builder.Default
  private long stunnedUntil = 0;
  @Builder.Default
  private boolean alive = true;
  @Builder.Default
  private int xp = 30;
  private String dropItemId;
  private String droppedDaoSkillId;
  private String droppedDaoSkillName;
  private String droppedDaoMemberName;

  @Builder.Default
  private Map<EquipmentSlot, String> equipment = new HashMap<>();
  @Builder.Default
  private List<String> skills = new ArrayList<>();
  private String dodgeSkillId;
  private String parrySkillId;

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

    return BattleEnemy.builder()
        .id(id)
        .templateId(tpl.id())
        .race(tpl.race())
        .name(displayName)
        .rank(rank)
        .isUnique(isUnique)
        .hp(hp)
        .maxHp(hp)
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
        .dropItemId(dropId)
        .alive(true)
        .xp(tpl.expReward() > 0 ? tpl.expReward() : 30)
        .build();
  }

  public static BattleEnemy fromTemplate(String id, MobTemplate tpl, RowPosition row, String dropId) {
    return fromTemplate(id, tpl, row, dropId, new TemplateCatalog());
  }

  @Builder.Default
  private List<ActiveBuff> activeBuffs = new ArrayList<>();

  public void takeDamage(int dmg) {
    int remainingDmg = absorbShieldDamage(dmg);
    this.hp = Math.max(0, this.hp - remainingDmg);
    if (this.hp <= 0) {
      this.alive = false;
    }
  }

  @Override
  public int absorbShieldDamage(int incomingDmg) {
    int remaining = incomingDmg;
    if (activeBuffs != null && !activeBuffs.isEmpty()) {
      var shields = activeBuffs.stream()
          .filter(b -> b.getCategory() == BuffCategory.SHIELD && b.getValue() > 0 && !b.isExpired())
          .sorted(Comparator.comparingInt(ActiveBuff::getRemainingTicks))
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
    } else {
      activeBuffs.add(newBuff);
    }
  }

  @Override
  public void removeBuff(String buffId) {
    if (buffId == null || activeBuffs == null) return;
    activeBuffs.removeIf(b -> b.getId().equalsIgnoreCase(buffId));
  }

  public boolean isStunned() {
    return stunnedUntil > System.currentTimeMillis();
  }

  public void applyStun(long durationMs) {
    this.stunnedUntil = Math.max(this.stunnedUntil, System.currentTimeMillis() + durationMs);
  }
}
