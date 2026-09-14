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
  private List<FormationSlot> slots = new ArrayList<>();
  private FormationSkill ultimateSkill;

  public FormationSlot getSlot(int index) {
    if (slots != null && index >= 0 && index < slots.size()) {
      return slots.get(index);
    }
    return FormationSlot.builder().slotIndex(index).slotName("普通站位").build();
  }
}
