package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.application.command.annotation.CommandAlias;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.enums.Direction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component // 註冊為 Spring Bean
@RequiredArgsConstructor
@CommandAlias({"l", "see", "ls"}) // 支援 l, see, ls
public class LookCommand implements PlayerCommand {

  @Override
  public String getKey() {
    return "look";
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();

    // 1. 如果沒有參數 -> 看當前房間
    if (args == null || args.trim().isEmpty()) {
      player.reply(player.getCurrentRoom().lookAtRoom(player));
      return;
    }

    String targetName = args.trim().toLowerCase();

    // 2. 判斷是否為「方向」 (look north)
    Direction dir = Direction.parse(targetName);
    if (dir != null) {
      player.getCurrentRoom().lookDirection(player, dir);
      return;
    }

    // 3. 判斷是否為「自己」 (look me)
    if (targetName.equals("me") || targetName.equals("self")) {
      player.getCurrentRoom().lookAtSelf(player);
      return;
    }

    // 4. 看具體目標 (生物或物品)
    player.getCurrentRoom().lookAtTarget(player, targetName);
  }
}
