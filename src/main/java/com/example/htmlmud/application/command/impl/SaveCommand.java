package com.example.htmlmud.application.command.impl;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.save.dto.SaveSlotDto;
import com.example.htmlmud.domain.save.service.SaveGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@CommandAlias({"saves", "slot", "slots"})
public class SaveCommand implements PlayerCommand {

  private final SaveGameService saveGameService;
  private final DungeonManager dungeonManager;
  private final DungeonCommand dungeonCommand;
  private final com.example.htmlmud.domain.service.GameStateBroadcastService broadcastService;

  @Override
  public String getKey() {
    return "save";
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();
    String input = (args != null) ? args.trim() : "";

    if (input.isEmpty() || input.equalsIgnoreCase("list")) {
      listSaveSlots(player);
      return;
    }

    if (input.equalsIgnoreCase("quiet") || input.equalsIgnoreCase("silent")
        || input.equalsIgnoreCase("json") || input.equalsIgnoreCase("list quiet")) {
      broadcastSaveSlots(player);
      return;
    }

    String[] tokens = input.split("\\s+", 2);
    String sub = tokens[0].toLowerCase();

    // 支援 save load <slot>
    if ("load".equals(sub)) {
      if (tokens.length < 2) {
        player.reply("請指定欲載入之存檔槽位 (0~5)，例如: save load 1");
        return;
      }
      handleLoad(player, tokens[1]);
      return;
    }

    // 支援 save new <name> [formationId]
    if ("new".equals(sub)) {
      String rest = (tokens.length > 1) ? tokens[1] : "";
      handleNew(player, rest);
      return;
    }

    // 支援 save del <slot>
    if ("del".equals(sub) || "delete".equals(sub)) {
      if (tokens.length < 2) {
        player.reply("請指定欲刪除之存檔槽位 (1~5)，例如: save del 2");
        return;
      }
      handleDelete(player, tokens[1]);
      return;
    }

    // 預設為存檔至指定槽位: save <1..5> [自訂標題]
    try {
      int slotId = Integer.parseInt(sub);
      if (slotId < 1 || slotId > SaveGameService.TOTAL_MANUAL_SLOTS) {
        player.reply("⚠️ 手動存檔槽位僅限 1 ~ " + SaveGameService.TOTAL_MANUAL_SLOTS + " (0 為自動存檔專用)！");
        return;
      }
      String customTitle = (tokens.length > 1) ? tokens[1] : null;
      var data = saveGameService.saveGame(player.getName(), slotId, customTitle, player.isInDungeon(), player.getCurrentRoomId());
      player.reply("\n\u001B[1;32m💾【道印封存】進度已成功烙印至【存檔槽位 " + slotId + "】！\n標題: "
          + data.getTitle() + " | 時間: " + data.getSavedAt() + "\u001B[0m\n");

      broadcastSaveSlots(player);
    } catch (NumberFormatException e) {
      player.reply("⚠️ 無效的存檔指令格式！使用範例：\n"
          + "  save                  - 列出所有存檔槽位\n"
          + "  save 1 [自訂標題]      - 儲存進度至槽位 1\n"
          + "  load 1                - 載入槽位 1 進度\n"
          + "  new [主角姓名]         - 開闢全新修仙道途\n"
          + "  save del 1            - 刪除槽位 1 進度");
    } catch (Exception e) {
      player.reply("❌ 存檔失敗: " + e.getMessage());
    }
  }

  public void handleLoad(Player player, String slotStr) {
    try {
      int slotId = Integer.parseInt(slotStr.trim());
      var data = saveGameService.loadGame(player.getName(), slotId);
      if (data.getProtagonistName() != null && !data.getProtagonistName().isBlank()) {
        player.setName(data.getProtagonistName());
      }
      player.reply("\n\u001B[1;36m📂【道途重臨】已成功讀取【存檔槽位 " + slotId + "】！\n進度標題: "
          + data.getTitle() + " | 主角: " + data.getProtagonistName() + "\u001B[0m\n");

      player.setInDungeon(data.isInDungeon());
      if (data.getCurrentRoomId() != null && !data.getCurrentRoomId().isBlank()) {
        player.setCurrentRoomId(data.getCurrentRoomId());
      }
      broadcastService.broadcastState(player);
      broadcastSaveSlots(player);
    } catch (NumberFormatException e) {
      player.reply("⚠️ 存檔槽位必須為數字 (0~5)！");
    } catch (Exception e) {
      player.reply("❌ 讀檔失敗: " + e.getMessage());
    }
  }

  public void handleNew(Player player, String args) {
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
      saveGameService.createNewGame(player.getName(), protagonistName, formationId);
      player.reply("\n\u001B[1;33m✨【新途啟程】道心初定！主角【" + protagonistName + "】踏入【新手村客棧】！\u001B[0m\n");

      player.setInDungeon(false);
      player.setCurrentRoomId("newbie_village:inn");
      broadcastService.broadcastState(player);
      broadcastSaveSlots(player);
    } catch (Exception e) {
      player.reply("❌ 開闢新遊戲失敗: " + e.getMessage());
    }
  }

  public void handleDelete(Player player, String slotStr) {
    try {
      int slotId = Integer.parseInt(slotStr.trim());
      boolean deleted = saveGameService.deleteSave(slotId);
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

  private void listSaveSlots(Player player) {
    List<SaveSlotDto> slots = saveGameService.listSaveSlots();
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

  public void broadcastSaveSlots(Player player) {
    if (player == null) return;
    List<SaveSlotDto> slots = saveGameService.listSaveSlots();
    player.sendJson(Map.of("type", "SAVE_SLOTS", "slots", slots));
  }

  @Override
  public String getDescription() {
    return "單機存檔管理 (save, load, new, saves)";
  }
}
