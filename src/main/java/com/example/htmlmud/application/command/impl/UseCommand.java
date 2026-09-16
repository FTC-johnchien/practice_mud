package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@CommandAlias({"服用", "用"})
public class UseCommand implements PlayerCommand {

  private final DrpgBattleService battleService;
  private final DungeonManager dungeonManager;

  @Override
  public String getKey() {
    return "use";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    String input = args != null ? args.trim() : "";
    DungeonPosition pos = dungeonManager.getOrCreatePosition(self.getName(), "taiyin_tomb_b1f");

    String[] parts = input.split("\\s+");
    if (parts.length >= 2) {
      try {
        int memberIdx = Integer.parseInt(parts[1]);
        battleService.useItem(self, parts[0], memberIdx, pos);
        return;
      } catch (NumberFormatException ignored) {}
    } else if (parts.length == 1 && !parts[0].isEmpty()) {
      // 預設對隊長 (0) 使用
      battleService.useItem(self, parts[0], 0, pos);
      return;
    }
    self.reply("用法: use <物品序號/ID> [隊員編號0-5]");
  }

  @Override
  public String getDescription() {
    return "使用行囊中之丹藥、清心符或血肉道核 (use <物品序號/ID> <隊員編號0-5>)";
  }
}
