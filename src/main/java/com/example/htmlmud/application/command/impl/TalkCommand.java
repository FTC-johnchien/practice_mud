package com.example.htmlmud.application.command.impl;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@CommandAlias({"ask", "chat", "speak", "talkto"})
public class TalkCommand implements PlayerCommand {

  private final PartyService partyService;

  @Override
  public String getKey() {
    return "talk";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    Room room = self.getCurrentRoom();

    if (room == null) {
      self.reply("你處於虛空中，四周空無一人。");
      return;
    }

    String target = (args != null) ? args.trim() : "";

    // 1. 如果未指定目標，且房間只有一個生靈，則預設與該生靈交談
    if (target.isBlank()) {
      List<Living> others = room.getLivings().stream()
          .filter(l -> l.isValid() && !l.getId().equals(self.getId()))
          .toList();
      if (others.size() == 1) {
        talkWithLiving(self, others.get(0));
        return;
      }
      self.reply("你想與誰交談？（請輸入：talk <人物名稱/ID>，例如 talk innkeeper 或 talk tie_niu）");
      return;
    }

    // 2. 優先在房間內尋找生物 (依 ID、名稱、別名比對)
    String lowerTarget = target.toLowerCase();
    Optional<Living> foundLiving = room.getLivings().stream()
        .filter(l -> l.isValid() && isMatch(l, lowerTarget))
        .findFirst();

    if (foundLiving.isPresent()) {
      talkWithLiving(self, foundLiving.get());
      return;
    }

    // 3. 檢查是否是已在小隊中的隊友 (PartyMember)
    Party party = partyService.getOrCreateParty(self.getName());
    if (party != null) {
      Optional<PartyMember> memberOpt = party.getMembers().stream()
          .filter(m -> isMatchMember(m, lowerTarget))
          .findFirst();
      if (memberOpt.isPresent()) {
        talkWithPartyMember(self, memberOpt.get());
        return;
      }
    }

    self.reply("環顧四周，這裡並沒有「" + target + "」，無法與之交談。");
  }

  private boolean isMatch(Living living, String target) {
    if (living.getId().equalsIgnoreCase(target)) return true;
    if (living.getName().toLowerCase().contains(target)) return true;
    if (living.getAliases() != null) {
      for (String a : living.getAliases()) {
        if (a.equalsIgnoreCase(target) || target.contains(a.toLowerCase())) return true;
      }
    }
    if (living instanceof Mob mob && mob.getTemplate() != null) {
      if (mob.getTemplate().id().equalsIgnoreCase(target)) return true;
      if (mob.getTemplate().aliases() != null) {
        for (String a : mob.getTemplate().aliases()) {
          if (a.equalsIgnoreCase(target) || target.contains(a.toLowerCase())) return true;
        }
      }
    }
    return false;
  }

  private boolean isMatchMember(PartyMember member, String target) {
    if (member.getId().equalsIgnoreCase(target)) return true;
    if (member.getName().toLowerCase().contains(target)) return true;
    if (member.getRoleTitle() != null && member.getRoleTitle().toLowerCase().contains(target)) return true;
    return false;
  }

  private void talkWithLiving(Player self, Living target) {
    if (target instanceof Mob mob && mob.getTemplate() != null) {
      List<String> dialogues = mob.getTemplate().dialogues();
      if (dialogues != null && !dialogues.isEmpty()) {
        int idx = ThreadLocalRandom.current().nextInt(dialogues.size());
        String line = dialogues.get(idx);
        self.reply("💬【" + mob.getName() + "】笑瞇瞇地說道：「" + line + "」");
        return;
      }
      self.reply("💬【" + mob.getName() + "】向你拱手作揖：「道友同行問道，若有差遣儘管吩咐！」");
      return;
    }

    if (target instanceof Player otherPlayer) {
      self.reply("💬【" + otherPlayer.getName() + "】朝你微微頷首：「道友仙途順遂，幸會！」");
      return;
    }

    self.reply("💬【" + target.getName() + "】神情專注地注視著四周。");
  }

  private void talkWithPartyMember(Player self, PartyMember member) {
    self.reply("💬【" + member.getName() + "】（" + member.getRoleTitle() + "）堅定地對你說道：「隊長，隨時聽候差遣！願與隊長共克難關！」");
  }

  @Override
  public String getDescription() {
    return "與當前環境中的人物、商販、長老或小隊成員交談";
  }
}
