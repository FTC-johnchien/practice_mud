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
public class DropCommand implements PlayerCommand {

  private final TargetSelector targetSelector;
  private final PartyService partyService;
  private final TemplateReader templateReader;
  private final GameStateBroadcastService broadcastService;

  @org.springframework.beans.factory.annotation.Autowired
  public DropCommand(TargetSelector targetSelector,
                     PartyService partyService,
                     TemplateReader templateReader,
                     GameStateBroadcastService broadcastService) {
    this.targetSelector = targetSelector;
    this.partyService = partyService;
    this.templateReader = templateReader;
    this.broadcastService = broadcastService;
  }

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

    // 1. 若輸入帶有 stash / storage / party 前綴，去除前綴後統一處理
    String keyword = trimmed;
    if (trimmed.startsWith("stash ") || trimmed.startsWith("storage ") || trimmed.startsWith("party ")) {
      keyword = trimmed.substring(trimmed.indexOf(' ') + 1).trim();
    }

    // 2. 優先自大一統【小隊行囊】中尋找並丟棄
    PartyItemSlot partySlot = findPartySlot(party, keyword);
    if (partySlot != null) {
      party.getInventory().removeItem(partySlot.getSlotId(), 1);
      GameItem droppedItem = partySlot.toGameItem(templateReader);
      if (droppedItem != null) {
        droppedItem.setAmount(1);
        room.dropItem(droppedItem);
      }
      // 同步移除 player.getInventory() 中的對應物品 (若存在)
      if (player.getInventory() != null) {
        var optItem = targetSelector.selectItem(player.getInventory(), keyword);
        if (optItem != null) {
          player.getInventory().remove(optItem);
        }
      }

      player.reply("🗑️ $N從小隊行囊丟下了 " + partySlot.getName());
      room.broadcastToOthers(player.getId(), player.getNickname() + " 從小隊行囊丟下了 " + partySlot.getName());
      if (broadcastService != null) {
        broadcastService.broadcastState(player);
      }
      return;
    }

    // 3. 兜底自個人隨身背包視圖丟棄 (相容舊單元測試)
    GameItem target = targetSelector.selectItem(player.getInventory(), keyword);
    if (target != null) {
      player.getInventory().remove(target);
      room.dropItem(target);

      player.reply("🗑️ $N從行囊丟下了 " + target.getDisplayName());
      room.broadcastToOthers(player.getId(), player.getNickname() + " 丟下了 " + target.getDisplayName());
      if (broadcastService != null) {
        broadcastService.broadcastState(player);
      }
      return;
    }

    player.reply("$N行囊裡沒有 '" + keyword + "'。");
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
