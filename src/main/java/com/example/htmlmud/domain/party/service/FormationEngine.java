package com.example.htmlmud.domain.party.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.dungeon.battle.ActiveBuff;
import com.example.htmlmud.domain.model.enums.BuffCategory;
import com.example.htmlmud.domain.party.model.FormationSlot;
import com.example.htmlmud.domain.party.model.FormationTemplate;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.RowPosition;
import lombok.extern.slf4j.Slf4j;

/**
 * 專職陣法引擎 (Tactical Formation Engine)：
 * 負責陣法運算、存活人員孔位自動排位映射 (Auto-mapping)、完整性檢驗、戰鬥破陣 (Formation Break) 與仇恨/倍率調度。
 */
@Slf4j
@Service
public class FormationEngine {

  /**
   * 取得隊伍在當前狀態下的「有效陣法人數」：
   * 若隊伍中有成員陣亡（存活人數 > 0 且小於總人數），以存活人數為準；否則以隊伍總人數為準。
   */
  public int getEffectivePartySize(Party party) {
    if (party == null || party.getMembers() == null || party.getMembers().isEmpty()) {
      return 0;
    }
    int alive = party.getAliveCount();
    return alive > 0 ? alive : party.size();
  }

  /**
   * 自動排位映射 (Auto-mapping)：
   * 將陣法的各孔位 (Slot 0..N-1) 依序分配給「存活的隊員」。
   * 陣亡隊員不佔用孔位（映射為 null）。
   *
   * @return Map<Integer, FormationSlot> key 為隊伍成員在 party.getMembers() 中的 index
   */
  public Map<Integer, FormationSlot> resolveSlotAssignments(Party party, FormationTemplate formation) {
    if (party == null || party.getMembers() == null || formation == null) {
      return Collections.emptyMap();
    }
    Map<Integer, FormationSlot> assignments = new HashMap<>();
    List<PartyMember> members = party.getMembers();
    int slotIdx = 0;

    for (int i = 0; i < members.size(); i++) {
      PartyMember m = members.get(i);
      if (m != null && m.isAlive()) {
        if (slotIdx < formation.getSlots().size()) {
          assignments.put(i, formation.getSlot(slotIdx));
          slotIdx++;
        }
      }
    }
    return assignments;
  }

  /**
   * 取得指定成員在陣法中的孔位設定 (考慮存活自動遞補)
   */
  public FormationSlot getSlotForMember(Party party, int memberIndex) {
    if (party == null || party.getEquippedFormation() == null || party.isFormationBroken()) {
      return null;
    }
    return party.getSlotForMember(memberIndex);
  }

  /**
   * 檢驗陣法是否符合隊伍目前存活與職業配置
   */
  public boolean isFormationEligible(Party party, FormationTemplate formation) {
    if (party == null || formation == null) return false;
    int effSize = getEffectivePartySize(party);
    if (formation.getRequiredPartySize() > 0 && formation.getRequiredPartySize() != effSize) {
      return false;
    }
    return checkClassRequirements(party, formation).isEmpty();
  }

  /**
   * 檢查當前「存活成員」是否滿足陣法職業要求
   */
  public List<String> checkClassRequirements(Party party, FormationTemplate formation) {
    if (party == null || formation == null || formation.getRequiredClasses() == null || formation.getRequiredClasses().isEmpty()) {
      return Collections.emptyList();
    }
    List<PartyMember> livingMembers = party.getMembers().stream()
        .filter(PartyMember::isAlive)
        .toList();
    if (livingMembers.isEmpty()) {
      livingMembers = party.getMembers();
    }

    List<String> missing = new ArrayList<>();
    for (String req : formation.getRequiredClasses()) {
      boolean hasClass = livingMembers.stream().anyMatch(m -> FormationTemplate.satisfiesClassRequirement(m, req));
      if (!hasClass) {
        missing.add(FormationTemplate.formatClassName(req));
      }
    }
    return missing;
  }

  /**
   * 結成/套用陣法：
   * 1. 根據存活成員自動映射孔位
   * 2. 更新隊員站位 (RowPosition)
   * 3. 刷新/派發 ActiveBuff
   */
  public void applyFormation(Party party, FormationTemplate formation) {
    if (party == null) return;
    party.setEquippedFormation(formation);
  }

  /**
   * 檢查當前陣法完整性；若人數或職業不滿足，觸發破陣 (Break)
   *
   * @param party 隊伍
   * @param triggerReason 觸發原因 (如 "隊員 鐵牛 力竭倒下")
   * @return 若破陣，傳回系統通知訊息；若未破陣則傳回 null
   */
  public String checkAndBreakFormation(Party party, String triggerReason) {
    if (party == null || party.getEquippedFormation() == null || party.isFormationBroken()) {
      return null;
    }
    FormationTemplate ft = party.getEquippedFormation();
    int alive = party.getAliveCount();

    // 1. 人數檢驗 (若存活人數小於陣法要求人數)
    if (ft.getRequiredPartySize() > 0 && alive != ft.getRequiredPartySize()) {
      return breakFormation(party, "因" + triggerReason + "，隊伍存活人數變為 " + alive + " 人，無法維持【" + ft.getName() + "】（需 " + ft.getRequiredPartySize() + " 人），陣法崩解！陣法加成與靈威皆已消散！");
    }

    // 2. 職業檢驗 (若陣亡後缺少維持陣法之必要職業)
    List<String> missing = checkClassRequirements(party, ft);
    if (!missing.isEmpty()) {
      return breakFormation(party, "因" + triggerReason + "，隊伍存活成員中缺少維持陣法之必要職業（" + String.join("、", missing) + "），【" + ft.getName() + "】陣法崩解！陣法加成與靈威皆已消散！");
    }

    return null;
  }

  /**
   * 執行破陣操作：清除 Buff、靈威歸零、設置 broken 標記
   */
  public String breakFormation(Party party, String reasonText) {
    if (party == null) return null;
    party.setFormationBroken(true);
    party.setFormationEnergy(0);
    party.refreshFormation();
    return reasonText;
  }

  /**
   * 取得指定成員在陣法中的仇恨倍率
   */
  public double getThreatMultiplier(Party party, PartyMember member) {
    if (party == null || party.isFormationBroken() || party.getEquippedFormation() == null || member == null || !member.isAlive()) {
      return 1.0;
    }
    int idx = party.getMembers().indexOf(member);
    if (idx < 0) return 1.0;
    FormationSlot slot = getSlotForMember(party, idx);
    return (slot != null && slot.getThreatMultiplier() > 0) ? slot.getThreatMultiplier() : 1.0;
  }

  public void clearAllFormationBuffs(Party party) {
    if (party == null || party.getMembers() == null) return;
    for (PartyMember m : party.getMembers()) {
      clearMemberFormationBuffs(m);
    }
  }

  private void clearMemberFormationBuffs(PartyMember m) {
    if (m == null || m.getActiveBuffs() == null) return;
    for (int j = 0; j < 10; j++) {
      m.removeBuff("buff_formation_slot_" + j);
    }
  }
}
