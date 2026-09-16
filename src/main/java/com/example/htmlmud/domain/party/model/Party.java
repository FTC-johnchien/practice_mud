package com.example.htmlmud.domain.party.model;

import java.util.ArrayList;
import java.util.List;
import com.example.htmlmud.domain.model.entity.LivingStats;
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
  @Builder.Default
  private PartyInventory inventory = new PartyInventory();

  public PartyInventory getInventory() {
    if (this.inventory == null) {
      this.inventory = new PartyInventory();
    }
    return this.inventory;
  }

  public String useItemOnMember(String slotId, int memberIdx) {
    if (memberIdx < 0 || memberIdx >= members.size()) {
      return "無效的隊員編號！";
    }
    PartyMember target = members.get(memberIdx);
    PartyItemSlot slot = getInventory().getItem(slotId);
    if (slot == null) {
      return "行囊中無此物品！";
    }

    if ("LEARN_SKILL".equals(slot.getEffectType()) && slot.getGrantedSkillId() != null) {
      // 吸收道種遺物，習得技能
      PartyMemberSkill newSkill = PartyMemberSkill.builder()
          .id(slot.getGrantedSkillId())
          .name(slot.getGrantedSkillName() != null ? slot.getGrantedSkillName() : "道種絕學")
          .icon("🧬")
          .description("自異變隊友道核中領悟之殘篇秘法")
          .costType(target.getResourceType())
          .costValue(target.getResourceType() == ResourceType.COMBO ? 2 : (target.getResourceType() == ResourceType.RAGE ? 30 : 35))
          .cooldownMs(6000)
          .damageMultiplier(1.8)
          .build();

      boolean learned = target.learnSkill(newSkill);
      if (!learned) {
        return target.getName() + " 早已領悟掌握該項道法！";
      }
      getInventory().removeItem(slotId, 1);
      return "✨ " + target.getName() + " 煉化吸收了【" + slot.getName() + "】，神魂共鳴，成功領悟掌握新絕學：【" + newSkill.getName() + "】！";
    }

    if (slot.getItemType() == com.example.htmlmud.domain.model.enums.ItemType.CONSUMABLE) {
      if ("HEAL_HP".equals(slot.getEffectType())) {
        target.heal(slot.getEffectValue());
        getInventory().removeItem(slotId, 1);
        return "🌿 為 " + target.getName() + " 服用【" + slot.getName() + "】，氣血恢復 " + slot.getEffectValue() + " 點！";
      } else if ("RESTORE_SAN".equals(slot.getEffectType())) {
        target.restoreSan(slot.getEffectValue());
        // 若隊員處於走火入魔期 (CHAOS)，恢復後若 SAN > 0 則解除走火入魔！
        if (target.getMadnessState() == PartyMember.MadnessState.CHAOS && target.getCurrentSan() > 0) {
          target.setMadnessState(PartyMember.MadnessState.SANE);
          target.setAberrationCounter(0);
          getInventory().removeItem(slotId, 1);
          return "✨ 為 " + target.getName() + " 服用【" + slot.getName() + "】，心魔被太華清氣驅散，走火入魔狀態解除！道心恢復至 " + target.getCurrentSan() + " 點！";
        }
        getInventory().removeItem(slotId, 1);
        return "✨ 為 " + target.getName() + " 使用【" + slot.getName() + "】，道心平復，恢復 " + slot.getEffectValue() + " 點 SAN！";
      } else if ("RESTORE_MP".equals(slot.getEffectType())) {
        target.restoreMp(slot.getEffectValue());
        getInventory().removeItem(slotId, 1);
        return "🔮 為 " + target.getName() + " 服用【" + slot.getName() + "】，真元恢復 " + slot.getEffectValue() + " 點！";
      }
    }

    return "該物品無法如此使用！";
  }

  public String equipItemOnMember(String slotId, int memberIdx) {
    if (memberIdx < 0 || memberIdx >= members.size()) {
      return "無效的隊員編號！";
    }
    PartyMember target = members.get(memberIdx);
    PartyItemSlot slot = getInventory().getItem(slotId);
    if (slot == null) {
      return "行囊中無此物品！";
    }

    if (slot.isWeapon()) {
      getInventory().removeItem(slotId, 1);
      PartyItemSlot old = target.equipWeapon(slot);
      if (old != null) {
        getInventory().addSlot(old);
      }
      return "⚔️ " + target.getName() + " 裝備了武器【" + slot.getName() + "】！攻擊力顯著提升！";
    } else if (slot.isArmor()) {
      getInventory().removeItem(slotId, 1);
      PartyItemSlot old = target.equipArmor(slot);
      if (old != null) {
        getInventory().addSlot(old);
      }
      return "🥋 " + target.getName() + " 穿戴了防具【" + slot.getName() + "】！防禦與氣血提升！";
    }

    return "該物品並非法寶裝備，無法穿戴！";
  }

  public String unequipItemFromMember(String slotType, int memberIdx) {
    if (memberIdx < 0 || memberIdx >= members.size()) {
      return "無效的隊員編號！";
    }
    PartyMember target = members.get(memberIdx);
    if ("weapon".equalsIgnoreCase(slotType)) {
      PartyItemSlot old = target.unequipWeapon();
      if (old != null) {
        getInventory().addSlot(old);
        return "🗡️ " + target.getName() + " 卸下了武器【" + old.getName() + "】。";
      }
      return target.getName() + " 未裝備任何武器！";
    } else if ("armor".equalsIgnoreCase(slotType)) {
      PartyItemSlot old = target.unequipArmor();
      if (old != null) {
        getInventory().addSlot(old);
        return "🥋 " + target.getName() + " 卸下了防具【" + old.getName() + "】。";
      }
      return target.getName() + " 未穿戴任何防具！";
    }
    return "無效的裝備部位 (支援 weapon, armor)！";
  }

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

  @JsonIgnore
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

  @JsonIgnore
  public List<PartyMember> getFrontRow() {
    return members.stream().filter(m -> m.getRow() == RowPosition.FRONT && m.isAlive()).toList();
  }

  @JsonIgnore
  public List<PartyMember> getBackRow() {
    return members.stream().filter(m -> m.getRow() == RowPosition.BACK && m.isAlive()).toList();
  }

  @JsonIgnore
  public boolean isAllDead() {
    return members.stream().noneMatch(PartyMember::isAlive);
  }

  @JsonIgnore
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
