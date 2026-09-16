package com.example.htmlmud.domain.save.model;

import java.util.HashSet;
import java.util.Set;
import com.example.htmlmud.domain.party.model.Party;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaveData {
  private int slotId; // 0 = autosave, 1..5 = manual saves
  private String title;
  private String playerId;
  private String protagonistName;
  private String floorId;
  private int floorX;
  private int floorY;
  private String floorFacing;
  private boolean[][] visitedTiles;
  @Builder.Default
  private Set<String> openedChests = new HashSet<>();
  private Party party;
  private String savedAt;
  private long playtimeSeconds;
}
