package com.example.htmlmud.domain.save.service;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.dungeon.model.GridDirection;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.model.GridCoord;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.save.dto.SaveSlotDto;
import com.example.htmlmud.domain.save.model.SaveData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaveGameService {

  public static final int TOTAL_MANUAL_SLOTS = 5;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final ObjectMapper objectMapper;
  private final PartyService partyService;
  private final DungeonManager dungeonManager;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.example.htmlmud.domain.service.GameStateBroadcastService broadcastService;

  private File savesDir;

  @PostConstruct
  public void init() {
    savesDir = new File("saves");
    if (!savesDir.exists()) {
      boolean created = savesDir.mkdirs();
      log.info("Initialized saves directory: {} (created={})", savesDir.getAbsolutePath(), created);
    }
  }

  private File getSaveFile(int slotId) {
    if (slotId < 0 || slotId > TOTAL_MANUAL_SLOTS) {
      throw new IllegalArgumentException("無效的存檔槽位: " + slotId + " (合法範圍: 0.." + TOTAL_MANUAL_SLOTS + ")");
    }
    String filename = (slotId == 0) ? "autosave.json" : "slot_" + slotId + ".json";
    return new File(savesDir, filename);
  }

  public List<SaveSlotDto> listSaveSlots() {
    List<SaveSlotDto> dtos = new ArrayList<>();

    // 0: Autosave
    dtos.add(readSlotSummary(0, "自動存檔"));

    // 1..5: Manual save slots
    for (int i = 1; i <= TOTAL_MANUAL_SLOTS; i++) {
      dtos.add(readSlotSummary(i, "存檔槽位 " + i));
    }

    return dtos;
  }

  public SaveSlotDto readSlotSummary(int slotId, String defaultTitle) {
    File file = getSaveFile(slotId);
    if (!file.exists()) {
      return SaveSlotDto.builder()
          .slotId(slotId)
          .empty(true)
          .title(defaultTitle + " (空)")
          .build();
    }

    try {
      SaveData data = objectMapper.readValue(file, SaveData.class);
      DungeonFloor floor = dungeonManager.getFloor(data.getFloorId());
      String floorName = floor != null ? floor.getName() : data.getFloorId();

      List<String> memberNames = new ArrayList<>();
      String formationName = "四象辟邪陣";
      if (data.getParty() != null) {
        if (data.getParty().getMembers() != null) {
          for (PartyMember m : data.getParty().getMembers()) {
            memberNames.add(m.getName());
          }
        }
        if (data.getParty().getEquippedFormation() != null) {
          formationName = data.getParty().getEquippedFormation().getName();
        }
      }

      return SaveSlotDto.builder()
          .slotId(slotId)
          .empty(false)
          .title(data.getTitle() != null ? data.getTitle() : defaultTitle)
          .protagonistName(data.getProtagonistName())
          .floorId(data.getFloorId())
          .floorName(floorName)
          .memberCount(memberNames.size())
          .memberNames(memberNames)
          .formationName(formationName)
          .savedAt(data.getSavedAt())
          .build();
    } catch (Exception e) {
      log.error("Failed to read save slot summary from {}: {}", file.getName(), e.getMessage());
      return SaveSlotDto.builder()
          .slotId(slotId)
          .empty(true)
          .title(defaultTitle + " (損壞)")
          .build();
    }
  }

  public SaveData saveGame(String playerId, int slotId, String customTitle) {
    return saveGame(playerId, slotId, customTitle, false, "newbie_village:inn");
  }

  public SaveData saveGame(String playerId, int slotId, String customTitle, boolean inDungeon, String currentRoomId) {
    DungeonPosition currentPos = dungeonManager.getOrCreatePosition(playerId, null);
    String floorId = (currentPos != null && currentPos.getFloorId() != null)
        ? currentPos.getFloorId()
        : "mozhu_mines_b1f";
    Party party = partyService.getOrCreateParty(playerId);
    DungeonPosition pos = dungeonManager.getOrCreatePosition(playerId, floorId);

    String protagonistName = playerId;
    if (party != null && !party.getMembers().isEmpty()) {
      protagonistName = party.getMembers().get(0).getName();
    }

    String title = customTitle;
    if (title == null || title.isBlank()) {
      title = (slotId == 0) ? "探索進度 (自動存檔)" : protagonistName + "的修仙道途 (第" + slotId + "槽)";
    }

    Set<String> openedChests = new HashSet<>();
    if (pos.getOpenedChests() != null) {
      for (GridCoord c : pos.getOpenedChests()) {
        openedChests.add(c.x() + "," + c.y());
      }
    }

    SaveData saveData = SaveData.builder()
        .slotId(slotId)
        .title(title)
        .playerId(playerId)
        .protagonistName(protagonistName)
        .inDungeon(inDungeon)
        .currentRoomId(currentRoomId)
        .floorId(pos.getFloorId())
        .floorX(pos.getX())
        .floorY(pos.getY())
        .floorFacing(pos.getFacing().name())
        .visitedTiles(pos.getVisited())
        .openedChests(openedChests)
        .party(party)
        .savedAt(LocalDateTime.now().format(DATE_FORMATTER))
        .build();

    File file = getSaveFile(slotId);
    File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
    try {
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile, saveData);
      try {
        java.nio.file.Files.move(tempFile.toPath(), file.toPath(),
            java.nio.file.StandardCopyOption.ATOMIC_MOVE,
            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      } catch (java.nio.file.AtomicMoveNotSupportedException e) {
        java.nio.file.Files.move(tempFile.toPath(), file.toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
      log.info("Successfully atomically saved game to slot {} [{}]: {}", slotId, file.getAbsolutePath(), title);
      return saveData;
    } catch (Exception e) {
      if (tempFile.exists()) {
        tempFile.delete();
      }
      log.error("Failed to save game to slot {}: {}", slotId, e.getMessage(), e);
      throw new RuntimeException("存檔失敗: " + e.getMessage(), e);
    }
  }

  public SaveData loadGame(String playerId, int slotId) {
    File file = getSaveFile(slotId);
    if (!file.exists()) {
      throw new IllegalArgumentException("存檔槽位 " + slotId + " 不存在！");
    }

    try {
      SaveData data = objectMapper.readValue(file, SaveData.class);

      // 1. 還原小隊 Party
      if (data.getParty() != null) {
        // 若缺少陣法參照，從 registry 補全
        if (data.getParty().getEquippedFormation() == null) {
          data.getParty().setEquippedFormation(partyService.getFormation("formation_four_symbols"));
        }
        partyService.setParty(playerId, data.getParty());
      }

      // 2. 還原地牢坐標與迷霧狀態
      String floorId = (data.getFloorId() != null) ? data.getFloorId() : "taiyin_tomb_b1f";
      DungeonFloor floor = dungeonManager.getFloor(floorId);
      int w = (floor != null) ? floor.getWidth() : 10;
      int h = (floor != null) ? floor.getHeight() : 10;

      GridDirection facing = GridDirection.NORTH;
      if (data.getFloorFacing() != null) {
        try {
          facing = GridDirection.valueOf(data.getFloorFacing().toUpperCase());
        } catch (Exception ignored) {}
      }

      DungeonPosition pos = new DungeonPosition(floorId, data.getFloorX(), data.getFloorY(), facing, w, h);
      if (data.getVisitedTiles() != null) {
        pos.setVisited(data.getVisitedTiles());
      }
      if (data.getOpenedChests() != null) {
        for (String chestStr : data.getOpenedChests()) {
          String[] parts = chestStr.split(",");
          if (parts.length == 2) {
            try {
              int cx = Integer.parseInt(parts[0].trim());
              int cy = Integer.parseInt(parts[1].trim());
              pos.markChestOpened(cx, cy);
            } catch (Exception ignored) {}
          }
        }
      }

      dungeonManager.setPlayerPosition(playerId, pos);
      log.info("Successfully loaded game from slot {} for player {}: {}", slotId, playerId, data.getTitle());
      return data;
    } catch (Exception e) {
      log.error("Failed to load game from slot {}: {}", slotId, e.getMessage(), e);
      throw new RuntimeException("讀檔失敗: " + e.getMessage(), e);
    }
  }

  public Party createNewGame(String playerId, String protagonistName, String formationId) {
    String pName = (protagonistName != null && !protagonistName.isBlank()) ? protagonistName.trim() : "玄靈子";
    Party party = partyService.resetParty(playerId, pName);

    if (formationId != null && !formationId.isBlank()) {
      var form = partyService.getFormation(formationId);
      if (form != null) {
        party.setEquippedFormation(form);
      }
    }

    // 重置地牢坐標為入口 (預設進入垂直切片 MVP 之 墨竹礦坑 B1F)
    String defaultFloorId = "mozhu_mines_b1f";
    DungeonFloor floor = dungeonManager.getFloor(defaultFloorId);
    if (floor == null) {
      floor = dungeonManager.getFloor("taiyin_tomb_b1f");
      if (floor != null) defaultFloorId = "taiyin_tomb_b1f";
    }
    int startX = (floor != null && floor.getStartCoord() != null) ? floor.getStartCoord().x() : 1;
    int startY = (floor != null && floor.getStartCoord() != null) ? floor.getStartCoord().y() : 1;
    GridDirection facing = (floor != null && floor.getStartFacing() != null) ? floor.getStartFacing() : GridDirection.NORTH;
    int w = (floor != null) ? floor.getWidth() : 10;
    int h = (floor != null) ? floor.getHeight() : 10;

    DungeonPosition pos = new DungeonPosition(defaultFloorId, startX, startY, facing, w, h);
    dungeonManager.setPlayerPosition(playerId, pos);

    // 自動儲存初始狀態至 slot 0 (Autosave)
    try {
      saveGame(playerId, 0, pName + " 踏入修仙道途 (新手村與墨竹礦坑)");
    } catch (Exception ignored) {}

    log.info("Created new single-player adventure for {} (protagonist: {})", playerId, pName);
    return party;
  }

  public boolean deleteSave(int slotId) {
    File file = getSaveFile(slotId);
    if (file.exists()) {
      boolean deleted = file.delete();
      log.info("Deleted save slot {}: {}", slotId, deleted);
      return deleted;
    }
    return false;
  }

  public void handleLoad(com.example.htmlmud.domain.actor.impl.Player player, String slotStr) {
    if (player == null) return;
    try {
      int slotId = Integer.parseInt(slotStr.trim());
      var data = loadGame(player.getName(), slotId);
      if (data.getProtagonistName() != null && !data.getProtagonistName().isBlank()) {
        player.setName(data.getProtagonistName());
      }
      player.reply("\n\u001B[1;36m📂【道途重臨】已成功讀取【存檔槽位 " + slotId + "】！\n進度標題: "
          + data.getTitle() + " | 主角: " + data.getProtagonistName() + "\u001B[0m\n");

      player.setInDungeon(data.isInDungeon());
      if (data.getCurrentRoomId() != null && !data.getCurrentRoomId().isBlank()) {
        player.setCurrentRoomId(data.getCurrentRoomId());
      }
      if (broadcastService != null) {
        broadcastService.broadcastState(player);
      }
      broadcastSaveSlots(player);
    } catch (NumberFormatException e) {
      player.reply("⚠️ 存檔槽位必須為數字 (0~5)！");
    } catch (Exception e) {
      player.reply("❌ 讀檔失敗: " + e.getMessage());
    }
  }

  public void handleNew(com.example.htmlmud.domain.actor.impl.Player player, String args) {
    if (player == null) return;
    try {
      String protagonistName = "玄靈子";
      String formationId = "formation_four_symbols";
      if (args != null && !args.isBlank()) {
        String[] tokens = args.trim().split("\\s+");
        if (tokens.length > 0 && !tokens[0].isBlank()) {
          protagonistName = tokens[0];
        }
        if (tokens.length > 1 && !tokens[1].isBlank()) {
          formationId = tokens[1];
        }
      }
      player.setName(protagonistName);
      createNewGame(player.getName(), protagonistName, formationId);
      player.reply("\n\u001B[1;33m✨【新途啟程】道心初定！主角【" + protagonistName + "】踏入【新手村客棧】！\u001B[0m\n");

      player.setInDungeon(false);
      player.setCurrentRoomId("newbie_village:inn");
      if (broadcastService != null) {
        broadcastService.broadcastState(player);
      }
      broadcastSaveSlots(player);
    } catch (Exception e) {
      player.reply("❌ 開闢新遊戲失敗: " + e.getMessage());
    }
  }

  public void handleDelete(com.example.htmlmud.domain.actor.impl.Player player, String slotStr) {
    if (player == null) return;
    try {
      int slotId = Integer.parseInt(slotStr.trim());
      boolean deleted = deleteSave(slotId);
      if (deleted) {
        player.reply("🗑️ 已成功刪除【存檔槽位 " + slotId + "】。");
      } else {
        player.reply("⚠️ 槽位 " + slotId + " 本就是空槽位或無法刪除。");
      }
      broadcastSaveSlots(player);
    } catch (Exception e) {
      player.reply("❌ 刪除存檔失敗: " + e.getMessage());
    }
  }

  public void listSaveSlots(com.example.htmlmud.domain.actor.impl.Player player) {
    if (player == null) return;
    List<SaveSlotDto> slots = listSaveSlots();
    StringBuilder sb = new StringBuilder();
    sb.append("=== 📜【仙道命冊・單機存檔槽位】===\n");
    for (SaveSlotDto s : slots) {
      String slotName = (s.getSlotId() == 0) ? "[自動存檔]" : "[槽位 " + s.getSlotId() + "]";
      if (s.isEmpty()) {
        sb.append(String.format(" %-10s -- (空無道痕) --\n", slotName));
      } else {
        sb.append(String.format(" %-10s %-20s | 主角: %-6s | %s | %s\n",
            slotName,
            s.getTitle(),
            s.getProtagonistName(),
            s.getFloorName(),
            s.getSavedAt() != null ? s.getSavedAt() : ""));
      }
    }
    sb.append("------------------------------------------\n");
    sb.append("提示: 輸入 'save <1-5>' 儲存，'load <0-5>' 讀檔，'new [姓名]' 新開局\n");
    player.reply(sb.toString());

    broadcastSaveSlots(player);
  }

  public void broadcastSaveSlots(com.example.htmlmud.domain.actor.impl.Player player) {
    if (player == null) return;
    List<SaveSlotDto> slots = listSaveSlots();
    player.sendJson(java.util.Map.of("type", "SAVE_SLOTS", "slots", slots));
  }
}
