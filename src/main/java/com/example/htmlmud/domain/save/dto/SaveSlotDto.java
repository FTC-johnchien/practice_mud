package com.example.htmlmud.domain.save.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class SaveSlotDto {
  private int slotId; // 0 = autosave, 1..5 = manual
  private boolean empty;
  private boolean corrupted;
  private String title;
  private String protagonistName;
  private String floorId;
  private String floorName;
  private int memberCount;
  private List<String> memberNames;
  private String formationName;
  private String savedAt;

  public SaveSlotDto() {}

  public SaveSlotDto(int slotId, boolean empty, boolean corrupted, String title, String protagonistName,
      String floorId, String floorName, int memberCount, List<String> memberNames, String formationName, String savedAt) {
    this.slotId = slotId;
    this.empty = empty;
    this.corrupted = corrupted;
    this.title = title;
    this.protagonistName = protagonistName;
    this.floorId = floorId;
    this.floorName = floorName;
    this.memberCount = memberCount;
    this.memberNames = memberNames;
    this.formationName = formationName;
    this.savedAt = savedAt;
  }
}
