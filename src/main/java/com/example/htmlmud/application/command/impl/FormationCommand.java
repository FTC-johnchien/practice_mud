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
import com.example.htmlmud.domain.party.model.FormationSkill;
import com.example.htmlmud.domain.party.model.FormationTemplate;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.service.PartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@CommandAlias({"zhenfa", "zf"})
public class FormationCommand implements PlayerCommand {

  private final PartyService partyService;
  private final com.example.htmlmud.domain.party.service.FormationEngine formationEngine;
  private final DungeonManager dungeonManager;
  private final DungeonNavigator dungeonNavigator;
  private final com.example.htmlmud.domain.dungeon.battle.DrpgBattleService battleService;
  private final com.example.htmlmud.domain.service.GameStateBroadcastService broadcastService;

  @Override
  public String getKey() {
    return "formation";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    Party party = partyService.getOrCreateParty(self.getName());
    String input = args != null ? args.trim() : "";

    if (input.isEmpty() || input.equals("toggle")) {
      toggleFormation(self, party);
      return;
    }

    String[] parts = input.split("\\s+");
    String subCmd = parts[0].toLowerCase();

    switch (subCmd) {
      case "switch", "row" -> {
        if (parts.length >= 2) {
          try {
            int mIdx = Integer.parseInt(parts[1]);
            if (mIdx >= 0 && mIdx < party.getMembers().size()) {
              var m = party.getMembers().get(mIdx);
              com.example.htmlmud.domain.party.model.RowPosition cur = m.getRow() != null
                  ? m.getRow() : com.example.htmlmud.domain.party.model.RowPosition.FRONT;
              var next = switch (cur) {
                case FRONT -> com.example.htmlmud.domain.party.model.RowPosition.MIDDLE;
                case MIDDLE -> com.example.htmlmud.domain.party.model.RowPosition.BACK;
                case BACK -> com.example.htmlmud.domain.party.model.RowPosition.FRONT;
                default -> com.example.htmlmud.domain.party.model.RowPosition.FRONT;
              };
              m.setRow(next);
              String rowName = switch (next) {
                case FRONT -> "前衛";
                case MIDDLE -> "中衛";
                case BACK -> "後衛";
                default -> "前衛";
              };
              self.reply("【站位變更】已將隊員「" + m.getName() + "」的戰鬥站位切換為【" + rowName + "】！");
              broadcastDrpgState(self);
              return;
            } else {
              self.reply("隊員編號超出範圍 (0 ~ " + (party.getMembers().size() - 1) + ")！");
              return;
            }
          } catch (NumberFormatException ignored) {}
        }
        toggleFormation(self, party);
      }
      case "info", "status" -> {
        if (party.getEquippedFormation() != null) {
          StringBuilder sb = new StringBuilder();
          if (party.isFormationBroken()) {
            sb.append("⚠️【陣法狀態：已崩解】\n因隊伍成員減員，原【")
                .append(party.getEquippedFormation().getName())
                .append("】已崩散失效！請使用 formation list 查看並切換符合當前存活人數的陣法！\n\n");
          }
          sb.append(partyService.formatFormationDetails(party.getEquippedFormation()));
          self.reply(sb.toString());
        } else {
          self.reply("當前未裝備任何道門陣法。可使用 formation list 查看可用陣法。");
        }
        broadcastDrpgState(self);
      }
      case "list" -> {
        int effSize = formationEngine.getEffectivePartySize(party);
        var formations = partyService.getFormationsForPartySize(effSize);
        if (formations.isEmpty()) {
          self.reply("目前存活人數（" + effSize + "人）無對應的陣法。");
          return;
        }
        StringBuilder sb = new StringBuilder();
        if (effSize < party.size()) {
          sb.append("=== 【").append(party.size()).append("人隊伍 (存活 ").append(effSize).append(" 人)】可用道門陣法列表 ===\n");
        } else {
          sb.append("=== 【").append(effSize).append("人隊伍】可用道門陣法列表 ===\n");
        }
        int idx = 1;
        for (var ft : formations) {
          boolean isCurrent = party.getEquippedFormation() != null && ft.getId().equals(party.getEquippedFormation().getId());
          var missing = formationEngine.checkClassRequirements(party, ft);
          String status;
          if (isCurrent && party.isFormationBroken()) {
            status = " [⚠️ 已崩解 - 需重結]";
          } else if (isCurrent) {
            status = " [✔ 運轉中]";
          } else if (missing.isEmpty()) {
            status = " [可結成]";
          } else {
            status = " [不可選 - 缺少職業: " + String.join(", ", missing) + "]";
          }
          sb.append(idx++).append(". ").append(ft.getId()).append(" - 《").append(ft.getName()).append("》").append(status).append("\n");
          sb.append("   說明: ").append(ft.getDescription()).append("\n");
        }
        sb.append("切換陣法語法: formation equip <陣法ID>\n");
        self.reply(sb.toString());
      }
      case "equip" -> {
        if (parts.length < 2) {
          self.reply("用法: formation equip <陣法ID>");
          return;
        }
        String formId = parts[1];
        FormationTemplate ft = partyService.getFormation(formId);
        if (ft == null) {
          self.reply("查無此陣法 ID，請使用 formation list 查看。");
          return;
        }
        int effSize = formationEngine.getEffectivePartySize(party);
        if (ft.getRequiredPartySize() != effSize) {
          self.reply("【人數不符】無法結成【" + ft.getName() + "】！此陣法需要 " + ft.getRequiredPartySize() + " 人，當前隊伍存活人數為 " + effSize + " 人。");
          return;
        }
        var missing = formationEngine.checkClassRequirements(party, ft);
        if (!missing.isEmpty()) {
          self.reply("【隊伍組成不符】無法結成【" + ft.getName() + "】！缺少必要職業: " + String.join(", ", missing) + "。");
          return;
        }
        formationEngine.applyFormation(party, ft);
        self.reply("【變換陣法】小隊已成功結成【" + ft.getName() + "】！\n"
            + partyService.formatFormationDetails(ft));
        broadcastDrpgState(self);
      }
      case "cast", "ult" -> {
        if (party.isFormationBroken()) {
          self.reply("【陣法已崩解】當前陣法因人員減員已崩散，無法引動奧義！請使用 formation equip 切換符合當前存活人數的陣法。");
          return;
        }
        if (battleService.isInBattle(self.getName())) {
          DungeonPosition pos = dungeonManager.getOrCreatePosition(self.getName(), "taiyin_tomb_b1f");
          battleService.castPartyUltimate(self, pos);
          return;
        }
        if (!party.canCastUltimate()) {
          self.reply("【靈威不足】陣法靈威未滿！當前靈威: [" + party.getFormationEnergy() + "/100]，需要 100 點靈威。");
          return;
        }
        FormationSkill ult = party.getEquippedFormation().getUltimateSkill();
        party.consumeFormationEnergy();

        StringBuilder sb = new StringBuilder();
        sb.append("★【陣法奧義爆發！】★\n");
        sb.append("全隊道法共鳴，陣眼光華沖霄，祭出陣法大招【").append(ult.getName()).append("】！\n");
        sb.append(ult.getDescription()).append("\n");
        if (ult.getSanCost() > 0) {
          sb.append("⚠️【星骸反噬】因引動不可名狀之禁忌威能，全隊道心遭受侵蝕，理智(SAN) -")
              .append(ult.getSanCost()).append(" 點！\n");
        }
        sb.append("陣法靈威已耗盡，重歸平靜。\n");
        self.reply(sb.toString());
        broadcastDrpgState(self);
      }
      default -> {
        self.reply("【道門陣法指令】\n"
            + "  formation               - 檢視當前陣法孔位倍率與專屬大招\n"
            + "  formation list          - 列出符合當前隊伍人數的所有陣法\n"
            + "  formation equip <ID>    - 切換結成指定陣法 (如 formation_five_elements)\n"
            + "  formation cast          - 釋放小隊陣法專屬奧義大招 (需 100 靈威)");
      }
    }
  }

  private void toggleFormation(Player self, Party party) {
    int effSize = formationEngine.getEffectivePartySize(party);
    var eligible = partyService.getFormationsForPartySize(effSize).stream()
        .filter(f -> formationEngine.isFormationEligible(party, f))
        .toList();
    if (eligible.isEmpty()) {
      if (party.getEquippedFormation() != null) {
        String status = party.isFormationBroken() ? "【已崩解】" : "【運轉中】";
        self.reply("當前陣法" + status + "【" + party.getEquippedFormation().getName() + "】。目前存活人數（" + effSize + "人）無其他可切換的有效陣法。");
      } else {
        self.reply("目前隊伍組成無法結成任何陣法。");
      }
      broadcastDrpgState(self);
      return;
    }
    FormationTemplate current = party.getEquippedFormation();
    int curIdx = -1;
    if (current != null && !party.isFormationBroken()) {
      for (int i = 0; i < eligible.size(); i++) {
        if (eligible.get(i).getId().equals(current.getId())) {
          curIdx = i;
          break;
        }
      }
    }
    int nextIdx = (curIdx + 1) % eligible.size();
    FormationTemplate next = eligible.get(nextIdx);
    formationEngine.applyFormation(party, next);
    self.reply("【變換道門陣法】小隊結成【" + next.getName() + "】！\n"
        + partyService.formatFormationDetails(next));
    broadcastDrpgState(self);
  }

  private void broadcastDrpgState(Player player) {
    if (player == null) return;
    broadcastService.broadcastState(player);
  }

  @Override
  public String getDescription() {
    return "道門陣法系統 (對標結魂書，支援陣位加成與陣法奧義大招)";
  }
}
