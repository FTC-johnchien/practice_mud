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
    if (req.equals("WARRIOR") || req.equals("TANK") || req.equals("戰") || req.equals("戰士")) {
      return actual.equals("WARRIOR") || actual.equals("SWORDSMAN")
          || (member.getRoleTitle() != null && (member.getRoleTitle().contains("體修") || member.getRoleTitle().contains("力士")));
    }
    if (req.equals("MAGE") || req.equals("WIZARD") || req.equals("法") || req.equals("法師")) {
      return actual.equals("MAGE") || actual.equals("TAOIST")
          || (member.getRoleTitle() != null && (member.getRoleTitle().contains("符修") || member.getRoleTitle().contains("法修")));
    }
    if (req.equals("CLERIC") || req.equals("HEALER") || req.equals("牧") || req.equals("牧師")) {
      return actual.equals("CLERIC")
          || (member.getRoleTitle() != null && (member.getRoleTitle().contains("丹修") || member.getRoleTitle().contains("靈醫") || member.getRoleTitle().contains("醫仙")));
    }
    if (req.equals("ROGUE") || req.equals("RANGER") || req.equals("遊俠") || req.equals("刺客")) {
      return actual.equals("ROGUE")
          || (member.getRoleTitle() != null && member.getRoleTitle().contains("遊俠"));
    }
    return actual.contains(req);
  }

  public static String formatClassName(String raw) {
    if (raw == null) return "";
    return switch (raw.toUpperCase()) {
      case "WARRIOR", "TANK", "戰", "戰士" -> "戰士(體修/肉盾)";
      case "MAGE", "WIZARD", "法", "法師" -> "法師(符修/法修)";
      case "CLERIC", "HEALER", "牧", "牧師" -> "牧師(丹修/靈醫)";
      case "ROGUE", "RANGER", "遊俠", "刺客" -> "遊俠(刺客)";
      default -> raw;
    };
  }
}
