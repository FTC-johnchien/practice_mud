package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.save.service.SaveGameService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LoadCommand implements PlayerCommand {

  private final SaveGameService saveGameService;

  @Override
  public String getKey() {
    return "load";
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();
    if (args == null || args.isBlank()) {
      saveGameService.listSaveSlots(player);
      return;
    }
    saveGameService.handleLoad(player, args.trim());
  }

  @Override
  public String getDescription() {
    return "讀取指定存檔槽位 (例如: load 1, load 0)";
  }
}
