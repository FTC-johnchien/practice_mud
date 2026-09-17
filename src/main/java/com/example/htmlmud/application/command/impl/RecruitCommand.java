package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.service.GameStateBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@CommandAlias({"hire", "join", "dismiss", "fire"})
public class RecruitCommand implements PlayerCommand {

  private final PartyService partyService;
  private final GameStateBroadcastService broadcastService;

  @Override
  public String getKey() {
    return "recruit";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    Party party = partyService.getOrCreateParty(self.getName());

    if (args == null || args.isBlank()) {
      self.reply("【結識與招募指令】\n"
          + "  recruit <人物名稱/ID> - 邀請同道知己加入問道旅團 (上限 6 人)\n"
          + "  dismiss <隊員名稱/ID> - 請離隊員返回客棧安歇\n"
          + "當前旅團人數: " + party.size() + "/6");
      return;
    }

    String trimmed = args.trim();
    String[] parts = trimmed.split("\\s+");
    String verb = parts[0].toLowerCase();
    String target = parts.length > 1 ? parts[1] : parts[0];

    // 如果使用者打的是 dismiss 或 fire
    if (verb.equals("dismiss") || verb.equals("fire") || verb.equals("請離")) {
      handleDismiss(self, party, target);
      return;
    }

    // 預設為招募
    handleRecruit(self, party, target);
  }

  private void handleRecruit(Player self, Party party, String target) {
    if (party.size() >= Party.MAX_PARTY_SIZE) {
      self.reply("【旅團滿編】問道旅團已達上限 6 人，無法再結納更多隊友！");
      return;
    }

    boolean success = partyService.recruitCompanion(party, target);
    if (success) {
      PartyMember added = party.getMembers().get(party.getMembers().size() - 1);
      self.reply("\u001B[1;32m🤝【結識同道】「" + added.getName() + "」爽朗抱拳應允，正式加入【" + party.getPartyName() + "】！\u001B[0m\n"
          + "【" + added.getName() + "】(" + added.getRoleTitle() + ") 當前站位: " + (added.getRow() == com.example.htmlmud.domain.party.model.RowPosition.FRONT ? "前衛" : "後衛"));
    } else {
      boolean alreadyIn = party.getMembers().stream().anyMatch(m -> m.getName().equalsIgnoreCase(target) || m.getId().equalsIgnoreCase(target));
      if (alreadyIn) {
        self.reply("【同道同行】「" + target + "」已身處隊伍之中，與你生死與共。");
      } else {
        self.reply("此地並未見到能結納為同道的「" + target + "」。(可招募夥伴：鐵牛 iron、凌霜 ling)");
      }
    }
    broadcastService.broadcastState(self);
  }

  private void handleDismiss(Player self, Party party, String target) {
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
    return "結納與請離同道夥伴 (recruit <名>, dismiss <名>)";
  }
}
