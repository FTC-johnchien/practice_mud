package com.example.htmlmud.domain.model.template;

import java.util.List;
import java.util.Map;
import com.example.htmlmud.domain.party.model.ResourceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClassTemplate(
    String id,
    String name,
    String description,
    ResourceType resourceType,
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
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ClassGrowth(
      int hpPerLevel,
      int mpPerLevel,
      Map<String, Double> statWeights
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ClassProficiencies(
      List<String> armor,
      List<String> weapon
  ) {}
}
