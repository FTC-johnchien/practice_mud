package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.enums.Direction;
import com.example.htmlmud.domain.service.RoomMovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MoveCommand implements PlayerCommand {

  private final RoomMovementService roomMovementService;

  @Override
  public String getKey() {
    return "move";
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();
    Direction dir = Direction.parse(args);
    if (dir == null) {
      player.reply("你要往哪個方向移動？");
      return;
    }
    roomMovementService.move(player, dir);
  }

  @Override
  public String getDescription() {
    return "在房間與地圖之間移動 (move <方向> 或直接輸入 n/s/e/w)";
  }
}
