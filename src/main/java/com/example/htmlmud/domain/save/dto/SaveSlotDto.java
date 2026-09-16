package com.example.htmlmud.domain.save.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveSlotDto {
  private int slotId; // 0 = autosave, 1..5 = manual
  private boolean empty;
  private String title;
  private String protagonistName;
  private String floorId;
  private String floorName;
  private int memberCount;
  private List<String> memberNames;
  private String formationName;
  private String savedAt;
}
