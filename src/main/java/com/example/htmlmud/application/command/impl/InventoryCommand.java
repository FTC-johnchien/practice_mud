package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.service.PartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@CommandAlias({"bag", "inv", "item", "items", "行囊", "背包"})
public class InventoryCommand implements PlayerCommand {

  private final PartyService partyService;
  private final DungeonManager dungeonManager;
  private final DrpgBattleService battleService;

  @Override
  public String getKey() {
    return "inventory";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    String input = args != null ? args.trim() : "";
    DungeonPosition pos = dungeonManager.getPlayerPosition(self.getName());
    Party party = partyService.getOrCreateParty(self.getName());

    if (input.startsWith("use ")) {
      String sub = input.substring(4).trim();
      String[] parts = sub.split("\\s+");
      if (parts.length >= 2) {
        try {
          int memberIdx = Integer.parseInt(parts[1]);
          battleService.useItem(self, parts[0], memberIdx, pos);
          return;
        } catch (NumberFormatException ignored) {}
      }
      self.reply("用法: item use <物品序號或ID> <隊員編號0-5>");
      return;
    }

    if (input.startsWith("equip ")) {
      String sub = input.substring(6).trim();
      String[] parts = sub.split("\\s+");
      if (parts.length >= 2) {
        try {
          int memberIdx = Integer.parseInt(parts[1]);
          battleService.equipItem(self, parts[0], memberIdx, pos);
          return;
        } catch (NumberFormatException ignored) {}
      }
      self.reply("用法: item equip <物品序號或ID> <隊員編號0-5>");
      return;
    }

    if (input.startsWith("unequip ")) {
      String sub = input.substring(8).trim();
      String[] parts = sub.split("\\s+");
      if (parts.length >= 2) {
        try {
          int memberIdx = Integer.parseInt(parts[1]);
          battleService.unequipItem(self, parts[0], memberIdx, pos);
          return;
        } catch (NumberFormatException ignored) {}
      }
      self.reply("用法: item unequip <部位> <隊員編號0-5> (支援 weapon, shield, armor, head, feet, acc1, acc2)");
      return;
    }

    // 預設列出行囊內容
    StringBuilder sb = new StringBuilder();
    sb.append("\n\u001B[1;36m═══════════════════【隊伍公共行囊】═══════════════════\u001B[0m\n");
    var slots = party.getInventory().getSlots();
    sb.append(" 容量負載: ").append(slots.size()).append(" / ").append(party.getInventory().getCapacity()).append(" 格\n");
    sb.append("──────────────────────────────────────────────────\n");
    if (slots.isEmpty()) {
      sb.append("  (行囊空空如也，可探索墓塚或擊殺妖邪獲取戰利品)\n");
    } else {
      for (int i = 0; i < slots.size(); i++) {
        PartyItemSlot s = slots.get(i);
        sb.append(String.format(" [%2d] %s %-14s x%d", (i + 1), s.getIcon(), s.getName(), s.getCount()));
        if (s.isWeapon()) {
          sb.append(" [武器: 攻 +").append(s.getBonusMinDamage()).append("~").append(s.getBonusMaxDamage()).append("]");
        } else if (s.isArmor()) {
          sb.append(" [防具: 防 +").append(s.getBonusDefense()).append(", 氣血 +").append(s.getBonusHp()).append("]");
        } else if ("RESTORE_SAN".equals(s.getEffectType())) {
          sb.append(" [清心符: 回復 ").append(s.getEffectValue()).append(" SAN，解走火入魔]");
        } else if ("HEAL_HP".equals(s.getEffectType())) {
          sb.append(" [靈丹: 回復 ").append(s.getEffectValue()).append(" HP]");
        } else if ("LEARN_SKILL".equals(s.getEffectType())) {
          sb.append(" \u001B[1;35m[血肉道種: 可領悟【").append(s.getGrantedSkillName()).append("】]\u001B[0m");
        }
        sb.append("\n");
      }
    }
    sb.append("──────────────────────────────────────────────────\n");
    sb.append(" 操作指令:\n");
    sb.append("   item use <序號> <隊員0-5>   - 對隊員使用丹藥/符咒/道種\n");
    sb.append("   item equip <序號> <隊員0-5> - 為隊員穿戴武器或防具\n");
    sb.append("   item unequip <weapon/armor> <隊員0-5> - 卸下裝備放回行囊\n");
    sb.append("\u001B[1;36m══════════════════════════════════════════════════\u001B[0m\n");

    self.reply(sb.toString());
    battleService.pushDrpgState(self, pos, battleService.getBattle(self.getName()));
  }

  @Override
  public String getDescription() {
    return "隊伍公共行囊與法寶丹藥管理 (支援 bag/inv/use/equip/unequip)";
  }
}
