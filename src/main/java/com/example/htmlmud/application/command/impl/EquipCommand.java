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
@CommandAlias({"裝備", "穿戴", "unequip", "卸下"})
public class EquipCommand implements PlayerCommand {

  private final DrpgBattleService battleService;
  private final DungeonManager dungeonManager;

  @Override
  public String getKey() {
    return "equip";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    String input = args != null ? args.trim() : "";
    DungeonPosition pos = dungeonManager.getPlayerPosition(self.getName());

    if (input.startsWith("unequip ") || input.startsWith("卸下 ")) {
      String sub = input.replaceFirst("^(unequip|卸下)\\s+", "");
      String[] parts = sub.split("\\s+");
      if (parts.length >= 2) {
        try {
          int memberIdx = Integer.parseInt(parts[1]);
          battleService.unequipItem(self, parts[0], memberIdx, pos);
          return;
        } catch (NumberFormatException ignored) {}
      }
      self.reply("用法: unequip <部位> <隊員編號0-5> (支援 weapon, shield, armor, head, feet, acc1, acc2)");
      return;
    }

    String[] parts = input.split("\\s+");
    if (parts.length >= 2) {
      try {
        int memberIdx = Integer.parseInt(parts[1]);
        battleService.equipItem(self, parts[0], memberIdx, pos);
        return;
      } catch (NumberFormatException ignored) {}
    } else if (parts.length == 1 && !parts[0].isEmpty()) {
      battleService.equipItem(self, parts[0], 0, pos);
      return;
    }
    self.reply("用法: equip <物品序號/ID> [隊員編號0-5]");
  }

  @Override
  public String getDescription() {
    return "為隊員穿戴或卸下武器防具 (equip <物品> <隊員0-5>)";
  }
}
