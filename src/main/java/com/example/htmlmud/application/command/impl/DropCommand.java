package com.example.htmlmud.application.command.impl;

import java.util.Optional;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.service.TargetSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@CommandAlias("drop")
@RequiredArgsConstructor
public class DropCommand implements PlayerCommand {

  private final TargetSelector targetSelector; // 注入工具


  @Override
  public String getKey() {
    return "drop";
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();

    // 基本檢查
    if (args.isEmpty()) {
      player.reply("$N要丟什麼？");
      return;
    }

    // 檢查背包是否有要丟的物品
    GameItem target = targetSelector.selectItem(player.getInventory(), args);
    if (target == null) {
      player.reply("$N背包裡沒有 '" + args + "'。");
      return;
    }

    // 先取得房間, 避免空指针
    Room room = player.getCurrentRoom();

    // 將物品移出背包
    player.getInventory().remove(target);
    // 將物品丟至房間
    room.dropItem(target);

    Optional<GameItem> opt = room.tryPickItem(args, player);
    if (opt.isPresent()) {
      GameItem pickItem = opt.get();
      player.getInventory().add(pickItem);
      player.reply("$N丟下了 " + pickItem.getDisplayName());
    } else {
      player.reply("這裡沒有 '" + args + "'。");
    }
  }

}
