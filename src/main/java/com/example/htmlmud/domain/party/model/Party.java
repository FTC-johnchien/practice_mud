package com.example.htmlmud.domain.party.model;

import java.util.ArrayList;
import java.util.List;
import com.example.htmlmud.domain.model.entity.LivingStats;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Party {
  public static final int MAX_PARTY_SIZE = 6;

  private String id;
  @Builder.Default
  private String partyName = "問道除穢小隊";
  @Builder.Default
  private List<PartyMember> members = new ArrayList<>();
  private FormationTemplate equippedFormation;
  @Builder.Default
  private int formationEnergy = 0;
  @Builder.Default
  private int maxFormationEnergy = 100;

  public boolean addMember(PartyMember member) {
    if (member == null || members.size() >= MAX_PARTY_SIZE) {
      return false;
    }
    // 若成員尚未指定站位，優先依據陣法孔位需求指派，否則預設前3位前衛、後3位後衛
    if (member.getRow() == null) {
      if (equippedFormation != null) {
        FormationSlot slot = equippedFormation.getSlot(members.size());
        if (slot != null && slot.getRequiredRow() != RowPosition.ANY) {
          member.setRow(slot.getRequiredRow());
        }
      }
      if (member.getRow() == null) {
        member.setRow(members.size() < 3 ? RowPosition.FRONT : RowPosition.BACK);
      }
    }
    members.add(member);
    return true;
  }

  public boolean removeMember(String memberId) {
    if (memberId == null) return false;
    return members.removeIf(m -> memberId.equals(m.getId()));
  }

  public PartyMember getMember(int index) {
    if (index >= 0 && index < members.size()) {
      return members.get(index);
    }
    return null;
  }

  public int size() {
    return members.size();
  }

  public void addFormationEnergy(int amount) {
    this.formationEnergy = Math.max(0, Math.min(this.maxFormationEnergy, this.formationEnergy + amount));
  }

  public boolean canCastUltimate() {
    if (equippedFormation == null || equippedFormation.getUltimateSkill() == null) {
      return false;
    }
    return formationEnergy >= equippedFormation.getUltimateSkill().getEnergyCost();
  }

  public void consumeFormationEnergy() {
    if (equippedFormation != null && equippedFormation.getUltimateSkill() != null) {
      this.formationEnergy = Math.max(0, this.formationEnergy - equippedFormation.getUltimateSkill().getEnergyCost());
      // 若陣法大招伴隨 SAN 侵蝕代價，扣除全隊 SAN 值
      int sanCost = equippedFormation.getUltimateSkill().getSanCost();
      if (sanCost > 0) {
        for (PartyMember member : members) {
          member.consumeSan(sanCost);
        }
      }
    }
  }

  public List<PartyMember> getFrontRow() {
    return members.stream().filter(m -> m.getRow() == RowPosition.FRONT && m.isAlive()).toList();
  }

  public List<PartyMember> getBackRow() {
    return members.stream().filter(m -> m.getRow() == RowPosition.BACK && m.isAlive()).toList();
  }

  public boolean isAllDead() {
    return members.stream().noneMatch(PartyMember::isAlive);
  }

  public EffectiveCombatStats calculateEffectiveStats(int memberIndex) {
    PartyMember member = getMember(memberIndex);
    if (member == null) {
      return null;
    }
    LivingStats base = member.getStats();
    int hp = base != null ? base.getHp() : 100;
    int maxHp = base != null ? base.getMaxHp() : 100;
    int mp = base != null ? base.getMp() : 50;
    int maxMp = base != null ? base.getMaxMp() : 50;
    int san = member.getCurrentSan();
    int maxSan = member.getMaxSan();

    int minDmg = member.getBaseMinDamage();
    int maxDmg = member.getBaseMaxDamage();
    int def = member.getBaseDefense();

    // 套用陣法孔位倍率
    if (equippedFormation != null) {
      FormationSlot slot = equippedFormation.getSlot(memberIndex);
      if (slot != null) {
        minDmg = (int) Math.round(minDmg * slot.getAttackMultiplier());
        maxDmg = (int) Math.round(maxDmg * slot.getAttackMultiplier());
        def = (int) Math.round(def * slot.getDefenseMultiplier());
      }
    }

    return new EffectiveCombatStats(minDmg, maxDmg, def, hp, maxHp, mp, maxMp, san, maxSan);
  }
}
