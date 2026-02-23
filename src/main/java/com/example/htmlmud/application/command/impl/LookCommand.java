package com.example.htmlmud.application.command.impl;

import java.io.IOException;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.application.command.annotation.CommandAlias;
import com.example.htmlmud.application.command.parser.TargetSelector;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.enums.Direction;
import com.example.htmlmud.protocol.MudMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component // 註冊為 Spring Bean
@RequiredArgsConstructor
@CommandAlias({"l", "see", "ls"}) // 支援 l, see, ls
public class LookCommand implements PlayerCommand {

  private final ObjectMapper objectMapper;


  @Override
  public String getKey() {
    return "look";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    Room room = self.getCurrentRoom();

    Object target = TargetSelector.findTarget(self, room, args);
    switch (target) {
      case Room r -> {
        self.reply(r.lookAtRoom(self));
      }
      case Direction d -> {
        self.reply(room.lookDirection(self, d)); // 這裡是看特定方向
      }
      case Player p -> {
        self.getOutput().sendJson(p.lookAtMe(p));
      }
      case Mob m -> {
        self.getOutput().sendJson(m.lookAtMe());
      }
      case GameItem i -> {
        self.getOutput().sendJson(i.lookAtMe());
      }
      case null -> {
        self.reply("你要看什麼？這裡找不到 '" + args + "'。");
      }
      default -> {
        self.reply("你盯著它看，但看不出什麼端倪。");
      }
    };
  }
}
