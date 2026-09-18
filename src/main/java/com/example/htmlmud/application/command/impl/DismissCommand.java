package com.example.htmlmud.application.command.impl;

import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.service.GameStateBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@CommandAlias({"fire", "請離"})
public class DismissCommand implements PlayerCommand {

  private final PartyService partyService;
  private final GameStateBroadcastService broadcastService;

  @Override
  public String getKey() {
    return "dismiss";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    Party party = partyService.getOrCreateParty(self.getName());

    if (args == null || args.isBlank()) {
      self.reply("【請離隊員指令】\n"
          + "  dismiss <隊員名稱/ID> - 請離隊員返回客棧安歇\n"
          + "當前旅團人數: " + party.size() + "/6");
      return;
    }

    String target = args.trim().split("\\s+")[0];
    boolean success = partyService.dismissCompanion(party, target);
    if (success) {
      self.reply("\u001B[1;33m👋【道別】已將隊員「" + target + "」請離隊伍，其已抱拳告辭返回客棧安歇。\u001B[0m");
    } else {
      self.reply("無法請離「" + target + "」（隊長不可離隊，或隊伍中查無此人）。");
    }
    broadcastService.broadcastState(self);
  }

  @Override
  public String getDescription() {
    return "請離隊員返回客棧安歇 (dismiss <名/ID>)";
  }
}
