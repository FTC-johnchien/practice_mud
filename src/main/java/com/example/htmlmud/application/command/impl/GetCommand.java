package com.example.htmlmud.application.command.impl;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.application.command.annotation.CommandAlias;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.entity.GameItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@CommandAlias("get")
@RequiredArgsConstructor
public class GetCommand implements PlayerCommand {

  @Override
  public String getKey() {
    return "get";
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();

    // 基本檢查
    if (args.isEmpty()) {
      player.reply("$N要撿什麼？");
      return;
    }

    Room room = player.getCurrentRoom();
    Optional<GameItem> opt = room.tryPickItem(args, player);
    if (opt.isPresent()) {
      GameItem pickItem = opt.get();
      player.getInventory().add(pickItem);
      player.reply("$N撿起了 " + pickItem.getDisplayName());

      // 房間通知
      room.broadcastToOthers(player.getId(),
          player.getNickname() + " 撿起了 " + pickItem.getDisplayName());
    } else {
      player.reply("這裡沒有 '" + args + "'。");
    }
  }
}
