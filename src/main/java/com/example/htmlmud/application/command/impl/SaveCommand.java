package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.save.service.SaveGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@CommandAlias({"saves", "slot", "slots"})
public class SaveCommand implements PlayerCommand {

  private final SaveGameService saveGameService;

  @Override
  public String getKey() {
    return "save";
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();
    String input = (args != null) ? args.trim() : "";

    if (input.isEmpty() || input.equalsIgnoreCase("list")) {
      saveGameService.listSaveSlots(player);
      return;
    }

    if (input.equalsIgnoreCase("quiet") || input.equalsIgnoreCase("silent")
        || input.equalsIgnoreCase("json") || input.equalsIgnoreCase("list quiet")) {
      saveGameService.broadcastSaveSlots(player);
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
      saveGameService.handleLoad(player, tokens[1]);
      return;
    }

    // 支援 save new <name> [formationId]
    if ("new".equals(sub)) {
      String rest = (tokens.length > 1) ? tokens[1] : "";
      saveGameService.handleNew(player, rest);
      return;
    }

    // 支援 save del <slot>
    if ("del".equals(sub) || "delete".equals(sub)) {
      if (tokens.length < 2) {
        player.reply("請指定欲刪除之存檔槽位 (1~5)，例如: save del 2");
        return;
      }
      saveGameService.handleDelete(player, tokens[1]);
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

      saveGameService.broadcastSaveSlots(player);
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

  @Deprecated
  public void handleLoad(Player player, String slotStr) {
    saveGameService.handleLoad(player, slotStr);
  }

  @Deprecated
  public void handleNew(Player player, String args) {
    saveGameService.handleNew(player, args);
  }

  @Deprecated
  public void handleDelete(Player player, String slotStr) {
    saveGameService.handleDelete(player, slotStr);
  }

  @Deprecated
  public void broadcastSaveSlots(Player player) {
    saveGameService.broadcastSaveSlots(player);
  }

  @Override
  public String getDescription() {
    return "單機存檔管理 (save, load, new, saves)";
  }
}
