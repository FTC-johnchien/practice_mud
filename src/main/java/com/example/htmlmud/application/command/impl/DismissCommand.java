package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.service.PartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@CommandAlias({"fire", "請離"})
public class DismissCommand implements PlayerCommand {

  private final PartyService partyService;

  @Override
  public String getKey() {
    return "dismiss";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    Party party = partyService.getOrCreateParty(self);

    if (args == null || args.isBlank()) {
      self.reply("【請離隊員指令】\n"
          + "  dismiss <隊員名稱/ID> - 請離隊員返回客棧安歇\n"
          + "當前旅團人數: " + party.size() + "/" + Party.MAX_PARTY_SIZE);
      return;
    }

    String target = args.trim().split("\\s+")[0];
    partyService.dismissCompanionForPlayer(self, target);
  }

  @Override
  public String getDescription() {
    return "請離隊員返回客棧安歇 (dismiss <名/ID>)";
  }
}
