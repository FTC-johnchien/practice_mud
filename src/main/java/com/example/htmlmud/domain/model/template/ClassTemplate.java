package com.example.htmlmud.domain.model.template;

import java.util.List;
import java.util.Map;
import com.example.htmlmud.domain.party.model.CombatResourceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClassTemplate(
    String id,
    String name,
    String description,
    CombatResourceType resourceType,
    Map<String, Integer> baseStats,
    ClassGrowth growth,
    ClassProficiencies proficiencies,
    Map<String, Object> traits,
    Map<String, Integer> skills
) {
  public ClassTemplate {
    if (baseStats == null) baseStats = Map.of();
    if (traits == null) traits = Map.of();
    if (skills == null) skills = Map.of();
    if (growth == null) growth = new ClassGrowth(20, 5, Map.of());
    if (proficiencies == null) proficiencies = new ClassProficiencies(List.of(), List.of());
  }

  public com.example.htmlmud.domain.model.enums.ClassType toClassType() {
    return com.example.htmlmud.domain.model.enums.ClassType.fromId(this.id);
  }

  public int getHpPerLevel() {
    return growth != null ? growth.hpPerLevel() : 20;
  }

  public int getMpPerLevel() {
    return growth != null ? growth.mpPerLevel() : 5;
  }

  public double getStatWeight(String statKey) {
    if (growth == null || growth.statWeights() == null || statKey == null) {
      return 0.0;
    }
    return growth.statWeights().getOrDefault(statKey.toUpperCase(), 0.0);
  }

  public boolean hasTrait(String traitKey) {
    if (traits == null || traitKey == null) return false;
    Object val = traits.get(traitKey);
    if (val instanceof Boolean b) return b;
    if (val != null) {
      String s = val.toString().trim();
      return "true".equalsIgnoreCase(s) || "1".equals(s);
    }
    return false;
  }

  public double getTraitDouble(String traitKey, double defaultValue) {
    if (traits == null || traitKey == null) return defaultValue;
    Object val = traits.get(traitKey);
    if (val instanceof Number n) return n.doubleValue();
    if (val != null) {
      try {
        return Double.parseDouble(val.toString());
      } catch (NumberFormatException ignored) {}
    }
    return defaultValue;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ClassGrowth(
      int hpPerLevel,
      int mpPerLevel,
      Map<String, Double> statWeights
  ) {
    public ClassGrowth {
      if (statWeights == null) statWeights = Map.of();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ClassProficiencies(
      List<String> armor,
      List<String> weapon
  ) {
    public ClassProficiencies {
      if (armor == null) armor = List.of();
      if (weapon == null) weapon = List.of();
    }
  }
}
