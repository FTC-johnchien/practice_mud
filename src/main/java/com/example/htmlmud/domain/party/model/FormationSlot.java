package com.example.htmlmud.domain.party.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormationSlot {
  private int slotIndex;
  private String slotName;
  @Builder.Default
  private RowPosition requiredRow = RowPosition.ANY;
  @Builder.Default
  private double attackMultiplier = 1.0;
  @Builder.Default
  private double defenseMultiplier = 1.0;
  @Builder.Default
  private double speedMultiplier = 1.0;
  @Builder.Default
  private int sanResistanceBonus = 0;
  private String specialBonusDesc;
}
