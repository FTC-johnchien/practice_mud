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
@CommandAlias({"hire", "join", "招募"})
public class RecruitCommand implements PlayerCommand {

  private final PartyService partyService;

  @Override
  public String getKey() {
    return "recruit";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    Party party = partyService.getOrCreateParty(self);

    if (args == null || args.isBlank()) {
      self.reply("【結識與招募指令】\n"
          + "  recruit <人物名稱/ID> - 邀請同道知己加入問道旅團 (上限 " + Party.MAX_PARTY_SIZE + " 人)\n"
          + "  dismiss <隊員名稱/ID> - 請離隊員返回客棧安歇\n"
          + "當前旅團人數: " + party.size() + "/" + Party.MAX_PARTY_SIZE);
      return;
    }

    String trimmed = args.trim();
    String[] parts = trimmed.split("\\s+");
    String verb = parts[0].toLowerCase();
    String target = parts.length > 1 ? parts[1] : parts[0];

    // 如果使用者打的是 dismiss 或 fire
    if (verb.equals("dismiss") || verb.equals("fire") || verb.equals("請離")) {
      partyService.dismissCompanionForPlayer(self, target);
      return;
    }

    // 預設為招募
    partyService.recruitCompanionForPlayer(self, target);
  }

  @Override
  public String getDescription() {
    return "結納與請離同道夥伴 (recruit <名>, dismiss <名>)";
  }
}
