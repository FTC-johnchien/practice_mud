package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@CommandAlias({"裝備", "穿戴", "unequip", "卸下", "eq", "equipment"})
public class EquipCommand implements PlayerCommand {

  private final DrpgBattleService battleService;
  private final DungeonManager dungeonManager;
  private final PartyService partyService;

  @Override
  public String getKey() {
    return "equip";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    String input = args != null ? args.trim() : "";
    DungeonPosition pos = dungeonManager.getPlayerPosition(self.getName());

    // 判斷是否透過 unequip/卸下 別名觸發
    String rawInput = MudContext.rawInput();
    boolean isUnequip = rawInput != null
        && (rawInput.trim().startsWith("unequip") || rawInput.trim().startsWith("卸下"));

    if (isUnequip) {
      String[] parts = input.split("\\s+");
      if (parts.length >= 2) {
        try {
          int memberIdx = Integer.parseInt(parts[1]);
          battleService.unequipItem(self, parts[0], memberIdx, pos);
          return;
        } catch (NumberFormatException ignored) {}
      } else if (parts.length == 1 && !parts[0].isEmpty()) {
        battleService.unequipItem(self, parts[0], 0, pos);
        return;
      }
      self.reply("用法: unequip <部位> [隊員編號0-5] (支援 weapon, shield, armor/body, head, feet, acc1, acc2)");
      return;
    }

    if (input.isEmpty() || input.equalsIgnoreCase("list") || input.equalsIgnoreCase("show")) {
      self.reply(formatEquipment(self));
      return;
    }

    String[] parts = input.split("\\s+");
    if (parts.length >= 2) {
      try {
        int memberIdx = Integer.parseInt(parts[1]);
        battleService.equipItem(self, parts[0], memberIdx, pos);
        return;
      } catch (NumberFormatException ignored) {}
    } else if (parts.length == 1 && !parts[0].isEmpty()) {
      battleService.equipItem(self, parts[0], 0, pos);
      return;
    }
    self.reply("用法: equip <物品序號/ID> [隊員編號0-5]");
  }

  private String formatEquipment(Player player) {
    Party party = partyService.getOrCreateParty(player.getName());
    PartyMember leader = (party != null && !party.getMembers().isEmpty()) ? party.getMembers().get(0) : null;
    var eq = (leader != null) ? leader.getEquipment() : java.util.Map.<EquipmentSlot, PartyItemSlot>of();

    StringBuilder sb = new StringBuilder();
    sb.append("\n\u001B[1;36m═════════════【道途法寶・裝備佩掛】═════════════\u001B[0m\n");
    sb.append("  主角道號：").append(player.getName())
      .append("  防禦力：").append(player.getDefense())
      .append("  攻擊力：").append(player.getMinDamage()).append("~").append(player.getMaxDamage()).append("\n");
    sb.append("──────────────────────────────────────────────\n");
    sb.append("  ⚔️ 【主手武器】: ").append(formatSlot(eq.get(EquipmentSlot.MAIN_HAND))).append("\n");
    sb.append("  🛡️ 【副手裝備】: ").append(formatSlot(eq.get(EquipmentSlot.OFF_HAND))).append("\n");
    sb.append("  👑 【頭部防具】: ").append(formatSlot(eq.get(EquipmentSlot.HEAD))).append("\n");
    sb.append("  🥋 【身軀防具】: ").append(formatSlot(eq.get(EquipmentSlot.BODY))).append("\n");
    sb.append("  👢 【踏雲靴履】: ").append(formatSlot(eq.get(EquipmentSlot.FEET))).append("\n");
    sb.append("  💍 【靈寶飾品】: ").append(formatSlot(eq.get(EquipmentSlot.ACCESSORY_1))).append("\n");
    sb.append("  📿 【本命法寶】: ").append(formatSlot(eq.get(EquipmentSlot.ACCESSORY_2))).append("\n");
    sb.append("──────────────────────────────────────────────\n");
    sb.append("  穿戴指令: equip <行囊或倉庫物品序號/名稱> [隊員0-5]\n");
    sb.append("  卸下指令: unequip <部位> [隊員0-5] (部位: weapon, shield, armor, head, feet, acc1, acc2)\n");
    sb.append("\u001B[1;36m══════════════════════════════════════════════\u001B[0m\n");
    return sb.toString();
  }

  private String formatSlot(PartyItemSlot slot) {
    if (slot == null) {
      return "\u001B[1;30m(空)\u001B[0m";
    }
    StringBuilder desc = new StringBuilder();
    desc.append(slot.getName());
    if (slot.isWeapon()) {
      desc.append(" [攻 +").append(slot.getBonusMinDamage()).append("~").append(slot.getBonusMaxDamage()).append("]");
    } else if (slot.isArmor() || slot.isShield()) {
      desc.append(" [防 +").append(slot.getBonusDefense());
      if (slot.getBonusHp() > 0) desc.append(", 氣血 +").append(slot.getBonusHp());
      desc.append("]");
    } else if (slot.getBonusSan() > 0 || slot.getBonusDefense() > 0) {
      desc.append(" [");
      if (slot.getBonusSan() > 0) desc.append("道心 +").append(slot.getBonusSan()).append(" ");
      if (slot.getBonusDefense() > 0) desc.append("防 +").append(slot.getBonusDefense());
      desc.append("]");
    }
    return desc.toString();
  }

  @Override
  public String getDescription() {
    return "檢視、穿戴或卸下武器法寶 (支援 equip / unequip / eq / equipment)";
  }
}
