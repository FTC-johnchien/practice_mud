package com.example.htmlmud.domain.party.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormationSkill {
  private String id;
  private String name;
  private String description;
  @Builder.Default
  private int energyCost = 100;
  @Builder.Default
  private int sanCost = 0; // 克蘇魯代價：全隊道心侵蝕
  private String effectType; // "AOE_DAMAGE", "AOE_HEAL", "AOE_PURIFY"
  private int basePower;
}
