package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@CommandAlias({"newgame"})
public class NewGameCommand implements PlayerCommand {

  private final SaveCommand saveCommand;

  @Override
  public String getKey() {
    return "new";
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();
    saveCommand.handleNew(player, args);
  }

  @Override
  public String getDescription() {
    return "開闢全新單機冒險進度 (例如: new [主角姓名])";
  }
}
