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

    if (input.isEmpty()) {
      self.reply("【小隊編制】已同步 6 人小隊狀態與陣法靈威。");
      broadcastDrpgState(self);
      return;
    }

    if (input.equals("status") || input.equals("list") || input.equals("detail")) {
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
      case "remove" -> {
        if (parts.length < 2) {
          self.reply("用法: party remove <夥伴姓名>");
          return;
        }
        String name = parts[1];
        PartyMember target = party.getMembers().stream()
            .filter(m -> m.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
        if (target != null) {
          if (target.getId().equals("m-leader")) {
            self.reply("隊長不可離隊！");
            return;
          }
          party.removeMember(target.getId());
          self.reply("道友「" + name + "」已離開小隊。\n" + partyService.formatPartyStatus(party));
          broadcastDrpgState(self);
        } else {
          self.reply("小隊中找不到名為「" + name + "」的成員。");
        }
      }
      default -> {
        self.reply("【小隊編制指令】\n"
            + "  party               - 檢視 6 人小隊成員、血量、真元、道心(SAN)與陣位加成\n"
            + "  party add <名> <定位> - 結識並招募新夥伴入隊 (最多 6 人)\n"
            + "  party remove <名>    - 請離隊員\n"
            + "  party energy <數值>  - 注入陣法靈威值 (測試大招專用)");
      }
    }
  }

  private void broadcastDrpgState(Player player) {
    if (player == null) return;
    DungeonFloor floor = dungeonManager.getFloor("taiyin_tomb_b1f");
    DungeonPosition pos = dungeonManager.getOrCreatePosition(player.getName(), "taiyin_tomb_b1f");
    String inspect = dungeonNavigator.inspectForward(floor, pos);
    Party party = partyService.getOrCreateParty(player.getName());
    var battleView = battleService.createBattleView(player.getName());
    player.sendJson(DrpgStateDto.of(floor, pos, inspect, party, battleView));
  }

  @Override
  public String getDescription() {
    return "檢視或管理 6 人冒險者小隊 (Party)";
  }
}
