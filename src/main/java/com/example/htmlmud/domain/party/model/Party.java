package com.example.htmlmud.domain.party.model;

import java.util.ArrayList;
import java.util.List;
import com.example.htmlmud.domain.dungeon.battle.ActiveBuff;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.BuffCategory;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.party.service.PartyService;
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
  public static final int MAX_PARTY_SIZE = 5;

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
  private boolean formationBroken = false;
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
          .costValue(target.getResourceType() == CombatResourceType.COMBO ? 2 : (target.getResourceType() == CombatResourceType.RAGE ? 30 : 35))
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

    if (!slot.isEquipment()) {
      return "該物品並非法寶裝備，無法穿戴！";
    }

    // 確定要穿戴的目標槽位
    com.example.htmlmud.domain.model.enums.EquipmentSlot targetSlot = slot.getEquipSlot();
    if (targetSlot == null) {
      if (slot.isWeapon()) targetSlot = com.example.htmlmud.domain.model.enums.EquipmentSlot.MAIN_HAND;
      else if (slot.isShield()) targetSlot = com.example.htmlmud.domain.model.enums.EquipmentSlot.OFF_HAND;
      else if (slot.isAccessory()) targetSlot = com.example.htmlmud.domain.model.enums.EquipmentSlot.ACCESSORY_1;
      else if (slot.isArmor()) targetSlot = com.example.htmlmud.domain.model.enums.EquipmentSlot.BODY;
    }

    // 若為飾品，智慧判斷 ACCESSORY_1 或 ACCESSORY_2 是否為空
    if (targetSlot != null && targetSlot.isAccessory()) {
      if (target.getEquipment().get(com.example.htmlmud.domain.model.enums.EquipmentSlot.ACCESSORY_1) == null) {
        targetSlot = com.example.htmlmud.domain.model.enums.EquipmentSlot.ACCESSORY_1;
      } else if (target.getEquipment().get(com.example.htmlmud.domain.model.enums.EquipmentSlot.ACCESSORY_2) == null) {
        targetSlot = com.example.htmlmud.domain.model.enums.EquipmentSlot.ACCESSORY_2;
      } else {
        targetSlot = com.example.htmlmud.domain.model.enums.EquipmentSlot.ACCESSORY_1;
      }
    }

    if (targetSlot == null) {
      targetSlot = com.example.htmlmud.domain.model.enums.EquipmentSlot.BODY;
    }

    getInventory().removeItem(slotId, 1);
    PartyItemSlot old = target.equip(targetSlot, slot);
    if (old != null) {
      getInventory().addSlot(old);
    }

    return switch (targetSlot) {
      case MAIN_HAND -> "⚔️ " + target.getName() + " 裝備了武器【" + slot.getName() + "】！攻擊力顯著提升！";
      case OFF_HAND -> "🛡️ " + target.getName() + " 裝備了副手【" + slot.getName() + "】！防禦加成提升！";
      case HEAD -> "👑 " + target.getName() + " 穿戴了頭部防具【" + slot.getName() + "】！防禦與屬性提升！";
      case BODY -> "🥋 " + target.getName() + " 穿戴了防具【" + slot.getName() + "】！防禦與氣血提升！";
      case FEET -> "👢 " + target.getName() + " 穿戴了靴履【" + slot.getName() + "】！身法敏捷提升！";
      case ACCESSORY_1, ACCESSORY_2 -> "💍 " + target.getName() + " 佩戴了法寶【" + slot.getName() + "】(" + targetSlot.getDisplayName() + ")！靈韻道心增幅！";
    };
  }

  public String unequipItemFromMember(String slotType, int memberIdx) {
    if (memberIdx < 0 || memberIdx >= members.size()) {
      return "無效的隊員編號！";
    }
    PartyMember target = members.get(memberIdx);
    if (slotType == null || slotType.trim().isEmpty()) {
      return "請指定要卸下的裝備部位！";
    }

    String key = slotType.trim().toLowerCase();
    com.example.htmlmud.domain.model.enums.EquipmentSlot targetSlot = switch (key) {
      case "weapon", "main_hand", "main" -> com.example.htmlmud.domain.model.enums.EquipmentSlot.MAIN_HAND;
      case "shield", "off_hand", "off" -> com.example.htmlmud.domain.model.enums.EquipmentSlot.OFF_HAND;
      case "armor", "body", "chest" -> com.example.htmlmud.domain.model.enums.EquipmentSlot.BODY;
      case "head", "helm", "helmet" -> com.example.htmlmud.domain.model.enums.EquipmentSlot.HEAD;
      case "feet", "boots", "shoes", "legs" -> com.example.htmlmud.domain.model.enums.EquipmentSlot.FEET;
      case "acc1", "accessory_1", "ring" -> com.example.htmlmud.domain.model.enums.EquipmentSlot.ACCESSORY_1;
      case "acc2", "accessory_2", "trinket" -> com.example.htmlmud.domain.model.enums.EquipmentSlot.ACCESSORY_2;
      default -> {
        try {
          yield com.example.htmlmud.domain.model.enums.EquipmentSlot.valueOf(slotType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
          yield null;
        }
      }
    };

    if (targetSlot == null) {
      return "無效的裝備部位 (支援 weapon, shield, armor, head, feet, acc1, acc2)！";
    }

    PartyItemSlot old = target.unequip(targetSlot);
    if (old != null) {
      getInventory().addSlot(old);
      if (targetSlot == com.example.htmlmud.domain.model.enums.EquipmentSlot.MAIN_HAND) {
        return "🗡️ " + target.getName() + " 卸下了武器【" + old.getName() + "】。";
      } else if (targetSlot == com.example.htmlmud.domain.model.enums.EquipmentSlot.BODY) {
        return "🥋 " + target.getName() + " 卸下了防具【" + old.getName() + "】。";
      } else {
        String icon = (old.getIcon() != null) ? old.getIcon() : "📦";
        return icon + " " + target.getName() + " 卸下了" + targetSlot.getDisplayName() + "【" + old.getName() + "】。";
      }
    }

    if (targetSlot == com.example.htmlmud.domain.model.enums.EquipmentSlot.MAIN_HAND) {
      return target.getName() + " 未裝備任何武器！";
    } else if (targetSlot == com.example.htmlmud.domain.model.enums.EquipmentSlot.BODY) {
      return target.getName() + " 未穿戴任何防具！";
    }
    return target.getName() + " 的【" + targetSlot.getDisplayName() + "】部位未穿戴任何裝備！";
  }

  public void setEquippedFormation(FormationTemplate equippedFormation) {
    this.equippedFormation = equippedFormation;
    this.formationBroken = false;
    alignFormationRows();
    refreshFormation();
  }

  public FormationSlot getSlotForMember(int memberIndex) {
    if (equippedFormation == null || formationBroken || members == null || memberIndex < 0 || memberIndex >= members.size()) {
      return null;
    }
    PartyMember target = members.get(memberIndex);
    if (target == null || !target.isAlive()) return null;
    int aliveIdx = 0;
    for (int i = 0; i < memberIndex; i++) {
      PartyMember prev = members.get(i);
      if (prev != null && prev.isAlive()) {
        aliveIdx++;
      }
    }
    return equippedFormation.getSlot(aliveIdx);
  }

  public void alignFormationRows() {
    if (members == null || members.isEmpty() || equippedFormation == null || formationBroken) return;
    for (int i = 0; i < members.size(); i++) {
      PartyMember m = members.get(i);
      if (m == null || !m.isAlive()) continue;
      FormationSlot slot = getSlotForMember(i);
      if (slot != null && slot.getAssignedRow() != null && slot.getAssignedRow() != RowPosition.ANY) {
        m.setRow(slot.getAssignedRow());
      }
    }
  }

  public void refreshFormation() {
    if (members == null || members.isEmpty()) return;
    for (int i = 0; i < members.size(); i++) {
      PartyMember m = members.get(i);
      if (m == null) continue;
      for (int j = 0; j < 10; j++) {
        m.removeBuff("buff_formation_slot_" + j);
      }
      if (equippedFormation != null && !formationBroken && m.isAlive()) {
        FormationSlot slot = getSlotForMember(i);
        if (slot != null) {
          ActiveBuff formBuff = ActiveBuff.builder()
              .id("buff_formation_slot_" + slot.getSlotIndex())
              .name(slot.getSlotName())
              .category(BuffCategory.STAT_MODIFIER)
              .durationTicks(-1)
              .remainingTicks(-1)
              .description(slot.getSpecialBonusDesc())
              .build();
          m.addBuff(formBuff);
        }
      }
    }
  }

  public String validateAndAlignFormation(PartyService partyService) {
    if (members == null || members.isEmpty()) return null;
    int currentSize = members.size();
    boolean needsFallback = false;
    String reason = null;

    if (equippedFormation == null) {
      needsFallback = true;
    } else if (equippedFormation.getRequiredPartySize() > 0 && equippedFormation.getRequiredPartySize() != currentSize) {
      needsFallback = true;
      reason = "隊伍人數變更為 " + currentSize + " 人（原陣法為 " + equippedFormation.getRequiredPartySize() + " 人陣）";
    } else {
      List<String> missing = equippedFormation.checkClassRequirements(this);
      if (!missing.isEmpty()) {
        needsFallback = true;
        reason = "隊伍成員調整後缺少必要職業: " + String.join("、", missing);
      }
    }

    if (needsFallback) {
      FormationTemplate fallback = (partyService != null) ? partyService.getBasicFormationForPartySize(currentSize) : null;
      if (fallback != null) {
        this.equippedFormation = fallback;
        alignFormationRows();
        refreshFormation();
        return reason != null
            ? "⚠️【陣法自動退回】因" + reason + "，陣法自動退回為 " + currentSize + " 人基本陣法【" + fallback.getName() + "】！"
            : "【陣法適配】隊伍已自動結成 " + currentSize + " 人基本陣法【" + fallback.getName() + "】！";
      }
    } else {
      refreshFormation();
    }
    return null;
  }

  public boolean addMember(PartyMember member) {
    if (member == null || members.size() >= MAX_PARTY_SIZE) {
      return false;
    }
    // 若成員尚未指定站位，優先依據陣法孔位需求指派，否則預設前3位前衛、後3位後衛
    if (member.getRow() == null) {
      if (equippedFormation != null) {
        FormationSlot slot = equippedFormation.getSlot(members.size());
        if (slot != null && slot.getAssignedRow() != RowPosition.ANY) {
          member.setRow(slot.getAssignedRow());
        }
      }
      if (member.getRow() == null) {
        member.setRow(members.size() < 3 ? RowPosition.FRONT : RowPosition.BACK);
      }
    }
    members.add(member);
    refreshFormation();
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
  public List<PartyMember> getAliveMembers() {
    if (members == null) return List.of();
    return members.stream().filter(PartyMember::isAlive).toList();
  }

  @JsonIgnore
  public int getAliveCount() {
    if (members == null) return 0;
    return (int) members.stream().filter(PartyMember::isAlive).count();
  }

  @JsonIgnore
  public boolean canCastUltimate() {
    if (formationBroken || equippedFormation == null || equippedFormation.getUltimateSkill() == null) {
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
  public List<PartyMember> getMiddleRow() {
    return members.stream().filter(m -> m.getRow() == RowPosition.MIDDLE && m.isAlive()).toList();
  }

  @JsonIgnore
  public List<PartyMember> getBackRow() {
    return members.stream().filter(m -> m.getRow() == RowPosition.BACK && m.isAlive()).toList();
  }

  @JsonIgnore
  public PartyMember getLeader() {
    if (members == null || members.isEmpty()) return null;
    return members.stream()
        .filter(PartyMember::isLeader)
        .findFirst()
        .orElse(members.get(0));
  }

  @JsonIgnore
  public boolean isAllDead() {
    return members.stream().noneMatch(PartyMember::isAlive);
  }

  public boolean swapMembers(int idx1, int idx2) {
    if (members == null || idx1 < 0 || idx1 >= members.size() || idx2 < 0 || idx2 >= members.size() || idx1 == idx2) {
      return false;
    }
    PartyMember temp = members.get(idx1);
    members.set(idx1, members.get(idx2));
    members.set(idx2, temp);
    refreshFormation();
    return true;
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
    if (equippedFormation != null && !formationBroken) {
      FormationSlot slot = getSlotForMember(memberIndex);
      if (slot != null) {
        minDmg = (int) Math.round(minDmg * slot.getAttackMultiplier());
        maxDmg = (int) Math.round(maxDmg * slot.getAttackMultiplier());
        def = (int) Math.round(def * slot.getDefenseMultiplier());
      }
    }

    return new EffectiveCombatStats(minDmg, maxDmg, def, hp, maxHp, mp, maxMp, san, maxSan);
  }
}
