package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.application.command.annotation.CommandAlias;
import com.example.htmlmud.application.command.parser.TargetSelector;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.entity.GameItem;
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
    Player self = MudContext.currentPlayer();
    Room room = self.getCurrentRoom();

    Object target = TargetSelector.findTarget(self, room, args);
    String result = switch (target) {
      case Room r -> r.lookAtRoom(self);
      case Direction d -> room.lookDirection(self, d); // 這裡是看特定方向
      case Player p -> lookAtSelf(p);
      case Mob m -> m.lookAtTarget();
      case GameItem i -> i.lookAtTarget();
      case null -> "你要看什麼？這裡找不到 '" + args + "'。";
      default -> "你盯著它看，但看不出什麼端倪。";
    };

    self.reply(result);

    // // 1. 如果沒有參數 -> 看當前房間
    // if (args == null || args.trim().isEmpty()) {
    // self.reply(room.lookAtRoom(self));
    // return;
    // }

    // String targetName = args.trim().toLowerCase();

    // // 2. 判斷是否為「方向」 (look north)
    // Direction dir = Direction.parse(targetName);
    // if (dir != null) {
    // room.lookDirection(self, dir);
    // return;
    // }

    // // 3. 判斷是否為「自己」 (look me)
    // if (targetName.equals("me") || targetName.equals("self")) {
    // room.lookAtSelf(self);
    // return;
    // }

    // // 4. 看具體目標 (生物或物品)
    // room.lookAtTarget(self, targetName);
  }

  private String lookAtSelf(Player player) {
    return "你看自己幹嘛 (TODO)";
  }

}
