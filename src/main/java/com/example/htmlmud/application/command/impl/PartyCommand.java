package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.dto.DrpgStateDto;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.dungeon.service.DungeonNavigator;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.party.service.PartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@CommandAlias({"team", "coven"})
public class PartyCommand implements PlayerCommand {

  private final PartyService partyService;
  private final DungeonManager dungeonManager;
  private final DungeonNavigator dungeonNavigator;
  private final com.example.htmlmud.domain.dungeon.battle.DrpgBattleService battleService;
  private final com.example.htmlmud.domain.service.GameStateBroadcastService broadcastService;

  public Party getOrCreateParty(String playerName) {
    return partyService.getOrCreateParty(playerName);
  }

  @Override
  public String getKey() {
    return "party";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    Party party = getOrCreateParty(self.getName());
    String input = args != null ? args.trim() : "";

    if (input.isEmpty() || input.equals("status") || input.equals("list") || input.equals("detail")) {
      self.reply(partyService.formatPartyStatus(party));
      broadcastDrpgState(self);
      return;
    }

    String[] parts = input.split("\\s+");
    String subCmd = parts[0].toLowerCase();

    switch (subCmd) {
      case "energy" -> {
        if (parts.length > 1) {
          try {
            int amount = Integer.parseInt(parts[1]);
            party.addFormationEnergy(amount);
            self.reply("【小隊靈威】小隊陣法靈威值已調整，當前為: [" + party.getFormationEnergy() + "/100]");
            broadcastDrpgState(self);
          } catch (NumberFormatException e) {
            self.reply("請輸入正確的數值，例如: party energy 50");
          }
        }
      }
      case "add" -> {
        if (parts.length < 2) {
          self.reply("用法: party add <夥伴姓名> [稱號定位] [front/back]");
          return;
        }
        if (party.size() >= Party.MAX_PARTY_SIZE) {
          self.reply("【小隊已滿】目前編制已達上限 " + Party.MAX_PARTY_SIZE + " 人！");
          return;
        }
        String name = parts[1];
        String role = parts.length > 2 ? parts[2] : "浪跡散修";
        RowPosition row = (parts.length > 3 && parts[3].equalsIgnoreCase("front")) ? RowPosition.FRONT : RowPosition.BACK;

        LivingStats s = new LivingStats();
        s.setHp(100);
        s.setMaxHp(100);
        s.setMp(50);
        s.setMaxMp(50);

        party.addMember(PartyMember.builder()
            .id("m-" + System.currentTimeMillis())
            .name(name)
            .roleTitle(role)
            .row(row)
            .stats(s)
            .baseMinDamage(10)
            .baseMaxDamage(20)
            .baseDefense(5)
            .currentSan(100)
            .maxSan(100)
            .build());
        self.reply("【招募成功】道友「" + name + "」(" + role + ") 正式加入小隊！\n" + partyService.formatPartyStatus(party));
        broadcastDrpgState(self);
      }
      case "recruit", "hire", "join" -> {
        if (parts.length < 2) {
          self.reply("用法: party recruit <夥伴ID或姓名>");
          return;
        }
        String target = parts[1];
        handleRecruit(self, party, target);
      }
      case "dismiss", "fire", "remove" -> {
        if (parts.length < 2) {
          self.reply("用法: party dismiss <夥伴ID或姓名>");
          return;
        }
        String target = parts[1];
        handleDismiss(self, party, target);
      }
      case "enable" -> {
        if (parts.length < 3) {
          self.reply("用法: party enable <隊員編號: 0~5> <技能ID>");
          return;
        }
        try {
          int mIdx = Integer.parseInt(parts[1]);
          if (mIdx < 0 || mIdx >= party.getMembers().size()) {
            self.reply("隊員編號超出範圍 (0 ~ " + (party.getMembers().size() - 1) + ")！");
            return;
          }
          String skillId = parts[2].trim();
          PartyMember m = party.getMembers().get(mIdx);
          var skillOpt = com.example.htmlmud.infra.persistence.repository.TemplateRepository.findSkill(skillId);
          if (skillOpt.isEmpty()) {
            self.reply("找不到指定的武學或道術: " + skillId);
            return;
          }
          var skill = skillOpt.get();
          var cat = PartyMember.resolveSkillCategory(skill);
          m.enableSkill(cat, skillId);
          self.reply("【武學掛載】已將成員「" + m.getName() + "」的 [" + cat + "] 分類主修武學設定為【" + skill.getName() + "】！");
          broadcastDrpgState(self);
        } catch (NumberFormatException e) {
          self.reply("隊員編號格式錯誤，請輸入數字 (0~5)。");
        }
      }
      default -> {
        self.reply("【小隊編制指令】\n"
            + "  party                    - 檢視 5 人小隊成員、血量、真元、道心(SAN)與陣位加成\n"
            + "  party recruit <名/ID>    - 邀請同道知己加入問道旅團 (上限 " + Party.MAX_PARTY_SIZE + " 人)\n"
            + "  party dismiss <名/ID>    - 請離隊員返回客棧安歇\n"
            + "  party enable <idx> <武學> - 設定隊員主修武學套路\n"
            + "  party energy <數值>       - 注入陣法靈威值 (測試大招專用)");
      }
    }
  }

  private void handleRecruit(Player self, Party party, String target) {
    if (party.size() >= Party.MAX_PARTY_SIZE) {
      self.reply("【旅團滿編】問道旅團已達上限 " + Party.MAX_PARTY_SIZE + " 人，無法再結納更多隊友！");
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
        self.reply("此地並未見到能結納為同道的「" + target + "」。(可招募夥伴：鐵牛 tie_niu、凌霜 ling_shuang)");
      }
    }
    broadcastDrpgState(self);
  }

  private void handleDismiss(Player self, Party party, String target) {
    boolean success = partyService.dismissCompanion(party, target);
    if (success) {
      self.reply("\u001B[1;33m👋【道別】已將隊員「" + target + "」請離隊伍，其已抱拳告辭返回客棧安歇。\u001B[0m");
    } else {
      self.reply("無法請離「" + target + "」（隊長不可離隊，或隊伍中查無此人）。");
    }
    broadcastDrpgState(self);
  }

  private void broadcastDrpgState(Player player) {
    if (player == null) return;
    broadcastService.broadcastState(player);
  }

  @Override
  public String getDescription() {
    return "檢視或管理 5 人冒險者隊伍 (Party)";
  }
}
