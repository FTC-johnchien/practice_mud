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
  private RowPosition assignedRow = RowPosition.ANY;
  @Builder.Default
  private double attackMultiplier = 1.0;
  @Builder.Default
  private double defenseMultiplier = 1.0;
  @Builder.Default
  private double speedMultiplier = 1.0;
  @Builder.Default
  private int sanResistanceBonus = 0;
  @Builder.Default
  private double threatMultiplier = 1.0;
  @Builder.Default
  private int gridX = 2;
  @Builder.Default
  private int gridY = 1;
  private String specialBonusDesc;

  public RowPosition getAssignedRow() {
    if (assignedRow != null && assignedRow != RowPosition.ANY) return assignedRow;
    if (requiredRow != null && requiredRow != RowPosition.ANY) return requiredRow;
    return assignedRow != null ? assignedRow : (requiredRow != null ? requiredRow : RowPosition.ANY);
  }
}
