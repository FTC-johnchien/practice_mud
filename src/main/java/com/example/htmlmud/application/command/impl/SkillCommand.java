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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@CommandAlias({"cast"})
public class SkillCommand implements PlayerCommand {

  private final DrpgBattleService battleService;
  private final DungeonManager dungeonManager;

  @Override
  public String getKey() {
    return "skill";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    String input = args != null ? args.trim() : "";
    DungeonPosition pos = dungeonManager.getPlayerPosition(self.getName());

    // 格式支援：
    // 1. "skill cast 0 sword_pierce 1" 或 "cast 0 sword_pierce 1"
    // 2. "0 sword_pierce"
    if (input.startsWith("cast ")) {
      input = input.substring(5).trim();
    }

    String[] parts = input.split("\\s+");
    if (parts.length >= 2) {
      try {
        int memberIdx = Integer.parseInt(parts[0]);
        String skillId = parts[1];
        int targetIdx = parts.length >= 3 ? Integer.parseInt(parts[2]) : -1;
        battleService.castSkill(self, memberIdx, skillId, targetIdx, pos);
      } catch (NumberFormatException e) {
        self.reply("指令格式錯誤！正確用法：skill cast <隊員編號0-5> <技能ID> [目標編號]");
      }
    } else {
      self.reply("請指定隊員編號與技能ID！例如：skill cast 0 sword_pierce");
    }
  }
}
