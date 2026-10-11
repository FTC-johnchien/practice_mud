package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.domain.service.GameStateBroadcastService;
import com.example.htmlmud.domain.service.TargetSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@CommandAlias("drop")
@RequiredArgsConstructor
public class DropCommand implements PlayerCommand {

  private final TargetSelector targetSelector;
  private final PartyService partyService;
  private final TemplateReader templateReader;
  private final GameStateBroadcastService broadcastService;

  @Override
  public String getKey() {
    return "drop";
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();

    if (args == null || args.isBlank()) {
      player.reply("$N要丟下什麼？（支援: drop <物品名稱> 或 drop stash <倉庫物品>）");
      return;
    }

    String trimmed = args.trim();
    Room room = player.getCurrentRoom();
    if (room == null) {
      player.reply("你不在任何房間中。");
      return;
    }

    Party party = partyService.getOrCreateParty(player.getName());

    // 1. 支援 drop stash <物品> / drop storage <物品> (丟棄小隊倉庫中的物品)
    if (trimmed.startsWith("stash ") || trimmed.startsWith("storage ") || trimmed.startsWith("party ")) {
      String sub = trimmed.substring(trimmed.indexOf(' ') + 1).trim();
      PartyItemSlot slot = findPartySlot(party, sub);
      if (slot == null) {
        player.reply("小隊儲物倉庫中沒有 '" + sub + "'。");
        return;
      }

      party.getInventory().removeItem(slot.getSlotId(), 1);
      GameItem droppedItem = slot.toGameItem(templateReader);
      droppedItem.setAmount(1);
      room.dropItem(droppedItem);

      player.reply("🗑️ $N從小隊倉庫丟下了 " + slot.getName());
      room.broadcastToOthers(player.getId(), player.getNickname() + " 從小隊倉庫丟下了 " + slot.getName());
      if (broadcastService != null) {
        broadcastService.broadcastState(player);
      }
      return;
    }

    // 2. 預設自個人隨身行囊丟棄
    GameItem target = targetSelector.selectItem(player.getInventory(), trimmed);
    if (target != null) {
      player.getInventory().remove(target);
      room.dropItem(target);

      player.reply("🗑️ $N從隨身行囊丟下了 " + target.getDisplayName());
      room.broadcastToOthers(player.getId(), player.getNickname() + " 丟下了 " + target.getDisplayName());
      if (broadcastService != null) {
        broadcastService.broadcastState(player);
      }
      return;
    }

    // 3. 若個人隨身行囊沒有，但小隊倉庫有，給予貼心引導提示
    PartyItemSlot inStorage = findPartySlot(party, trimmed);
    if (inStorage != null) {
      player.reply("$N隨身行囊裡沒有 '" + trimmed + "'。（小隊倉庫中存有【" + inStorage.getName()
          + "】，若欲丟棄倉庫物品，請輸入: drop stash " + trimmed + "）");
    } else {
      player.reply("$N隨身行囊與小隊倉庫裡皆沒有 '" + trimmed + "'。");
    }
  }

  private PartyItemSlot findPartySlot(Party party, String keyword) {
    if (party == null || party.getInventory() == null) return null;
    var slots = party.getInventory().getSlots();
    if (slots.isEmpty()) return null;

    // 嘗試按數字序號 (1-based)
    try {
      int idx = Integer.parseInt(keyword);
      if (idx >= 1 && idx <= slots.size()) {
        return slots.get(idx - 1);
      }
    } catch (NumberFormatException ignored) {}

    // 嘗試按 slotId 或 itemId 或 名稱
    return slots.stream()
        .filter(s -> s.getSlotId().equalsIgnoreCase(keyword)
            || s.getItemId().equalsIgnoreCase(keyword)
            || s.getName().contains(keyword))
        .findFirst()
        .orElse(null);
  }

  @Override
  public String getDescription() {
    return "丟下隨身行囊或小隊倉庫之物品 (支援 drop <物品> 或 drop stash <物品>)";
  }
}
