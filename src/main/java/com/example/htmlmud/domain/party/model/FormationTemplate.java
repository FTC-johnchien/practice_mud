package com.example.htmlmud.domain.party.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormationTemplate {
  private String id;
  private String name;
  private String description;
  private String passiveAura;
  @Builder.Default
  private int requiredPartySize = 5;
  @Builder.Default
  private boolean basic = false;
  @Builder.Default
  private List<String> requiredClasses = new ArrayList<>();
  @Builder.Default
  private List<FormationSlot> slots = new ArrayList<>();
  private FormationSkill ultimateSkill;

  public FormationSlot getSlot(int index) {
    if (slots != null && index >= 0 && index < slots.size()) {
      return slots.get(index);
    }
    return FormationSlot.builder().slotIndex(index).slotName("普通站位").build();
  }

  public boolean isEligibleForParty(Party party) {
    if (party == null) return false;
    if (requiredPartySize > 0 && party.size() != requiredPartySize) {
      return false;
    }
    return checkClassRequirements(party).isEmpty();
  }

  public List<String> checkClassRequirements(Party party) {
    if (party == null || requiredClasses == null || requiredClasses.isEmpty()) {
      return List.of();
    }
    List<String> missing = new ArrayList<>();
    for (String req : requiredClasses) {
      boolean hasClass = party.getMembers().stream().anyMatch(m -> satisfiesClassRequirement(m, req));
      if (!hasClass) {
        missing.add(formatClassName(req));
      }
    }
    return missing;
  }

  public static boolean satisfiesClassRequirement(PartyMember member, String reqClass) {
    if (member == null || reqClass == null) return false;
    String actual = (member.getClassId() != null) ? member.getClassId().toUpperCase() : "";
    String req = reqClass.trim().toUpperCase();
    if (actual.equals(req)) return true;

    com.example.htmlmud.domain.model.enums.RoleCategory reqCategory =
        com.example.htmlmud.domain.model.enums.RoleCategory.parseRequirement(req);
    if (reqCategory != null) {
      com.example.htmlmud.domain.model.enums.RoleCategory memberCategory =
          com.example.htmlmud.domain.model.enums.RoleCategory.fromClassOrRole(member.getClassId(), member.getRoleTitle());
      if (memberCategory == reqCategory) return true;
      // 容錯：若需求為 TANK，SWORDSMAN 作為前排亦可相容
      if (reqCategory == com.example.htmlmud.domain.model.enums.RoleCategory.TANK && "SWORDSMAN".equals(actual)) {
        return true;
      }
    }

    return actual.contains(req);
  }

  public static String formatClassName(String raw) {
    if (raw == null) return "";
    com.example.htmlmud.domain.model.enums.RoleCategory cat =
        com.example.htmlmud.domain.model.enums.RoleCategory.parseRequirement(raw);
    if (cat != null) {
      return cat.getDisplayName() + "(" + cat.getRoleDescription() + ")";
    }
    return raw;
  }
}
