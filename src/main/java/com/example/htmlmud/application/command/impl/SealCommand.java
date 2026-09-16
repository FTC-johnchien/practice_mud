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
@CommandAlias({"封印", "鎮魔"})
public class SealCommand implements PlayerCommand {

  private final DrpgBattleService battleService;
  private final DungeonManager dungeonManager;

  @Override
  public String getKey() {
    return "seal";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    String input = args != null ? args.trim() : "";
    DungeonPosition pos = dungeonManager.getOrCreatePosition(self.getName(), "taiyin_tomb_b1f");

    if (input.isEmpty()) {
      self.reply("請指定封印目標！用法: seal <隊員編號0-5 或 敵方畸變體編號>");
      return;
    }

    try {
      int idx = Integer.parseInt(input);
      battleService.sealTarget(self, idx, pos);
    } catch (NumberFormatException e) {
      self.reply("目標編號格式不正確！用法: seal <隊員編號0-5>");
    }
  }

  @Override
  public String getDescription() {
    return "施展太上鎮魔咒封印走火入魔隊友或戰場畸變體 (seal <編號>)";
  }
}
