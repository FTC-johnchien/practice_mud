package com.example.htmlmud.save;

import java.util.HashSet;
import java.util.Set;
import com.example.htmlmud.party.Party;
import java.util.ArrayList;
import java.util.List;
import com.example.htmlmud.party.PartyItemSlot;
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
  public static final int CURRENT_SCHEMA_VERSION = 3;

  @Builder.Default
  private int schemaVersion = CURRENT_SCHEMA_VERSION;

  /**
   * 歷史相容欄位 (v2 雙層背包過渡期產物)。
   * 在 SP-03 (存檔大一統) 中已徹底除役，所有道具由 party.inventory 唯一管理。
   * 此處僅保留用於反序列化舊存檔或向下相容，若為空則不輸出至 JSON。
   */
  @Deprecated
  @Builder.Default
  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
  private List<PartyItemSlot> personalInventory = new ArrayList<>();
  private int slotId; // 0 = autosave, 1..5 = manual saves
  private String title;
  private String playerId;
  private String protagonistName;
  private boolean inDungeon;
  private String currentRoomId;
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
