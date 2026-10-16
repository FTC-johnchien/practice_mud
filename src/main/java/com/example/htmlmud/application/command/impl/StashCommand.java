package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
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

/**
 * UNIFY-01 (雙層背包模型調度指令)
 * 提供個人隨身行囊 (Personal Inventory) 與小隊公共倉庫 (Party Storage) 之間的物品流轉與調度。
 */
@Slf4j
@Component
@CommandAlias({"stash", "store", "存入", "withdraw", "take", "取出"})
@RequiredArgsConstructor
public class StashCommand implements PlayerCommand {

  private final PartyService partyService;
  private final TemplateReader templateReader;
  private final TargetSelector targetSelector;
  private final GameStateBroadcastService broadcastService;

  @Override
  public String getKey() {
    return "stash";
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();
    Party party = partyService.getOrCreateParty(player.getName());

    if (args == null || args.isBlank()) {
      showUsage(player, party);
      return;
    }

    String trimmed = args.trim();
    String lower = trimmed.toLowerCase();

    // 判斷動作模式：withdraw / take / 取出
    if (lower.startsWith("withdraw ") || lower.startsWith("take ") || lower.startsWith("取出 ")) {
      String sub = trimmed.substring(trimmed.indexOf(' ') + 1).trim();
      handleWithdraw(player, party, sub);
    } else if (lower.startsWith("stash ") || lower.startsWith("store ") || lower.startsWith("存入 ")) {
      String sub = trimmed.substring(trimmed.indexOf(' ') + 1).trim();
      handleStash(player, party, sub);
    } else {
      // 若當前指令上下文為 withdraw / take，或未指定動作詞
      // 優先檢查個人背包是否有該物品，有則執行 stash，否則若小隊倉庫有則執行 withdraw
      GameItem inPersonal = targetSelector.selectItem(player.getInventory(), trimmed);
      if (inPersonal != null) {
        handleStash(player, party, trimmed);
      } else {
        handleWithdraw(player, party, trimmed);
      }
    }
  }

  private void handleStash(Player player, Party party, String keyword) {
    GameItem item = targetSelector.selectItem(player.getInventory(), keyword);
    if (item == null) {
      player.reply("$N隨身行囊中沒有 '" + keyword + "'！");
      return;
    }

    if (party.getInventory().getSlots().size() >= party.getInventory().getCapacity()) {
      player.reply("⚠️ 小隊儲物倉庫已滿載（上限 " + party.getInventory().getCapacity() + " 格），無法再存入物品！");
      return;
    }

    player.getInventory().remove(item);
    party.getInventory().addGameItem(item);

    player.reply("📥 你將【" + item.getDisplayName() + "】存入了【小隊儲物倉庫】！（隨身負載: "
        + player.getInventory().size() + "/" + GetCommand.PERSONAL_INVENTORY_CAPACITY
        + " | 倉庫負載: " + party.getInventory().getSlots().size() + "/" + party.getInventory().getCapacity() + "）");

    if (broadcastService != null) {
      broadcastService.broadcastState(player);
    }
  }

  private void handleWithdraw(Player player, Party party, String keyword) {
    if (player.getInventory().size() >= GetCommand.PERSONAL_INVENTORY_CAPACITY) {
      player.reply("⚠️ 個人隨身行囊已滿載（上限 " + GetCommand.PERSONAL_INVENTORY_CAPACITY + " 格），請先使用或將多餘物品存入倉庫！");
      return;
    }

    PartyItemSlot slot = findPartySlot(party, keyword);
    if (slot == null) {
      player.reply("小隊儲物倉庫中沒有 '" + keyword + "'！");
      return;
    }

    party.getInventory().removeItem(slot.getSlotId(), 1);
    GameItem item = slot.toGameItem(templateReader);
    item.setAmount(1);
    player.getInventory().add(item);

    player.reply("📤 你從小隊倉庫取出了【" + slot.getName() + "】，放入【個人隨身行囊】！（隨身負載: "
        + player.getInventory().size() + "/" + GetCommand.PERSONAL_INVENTORY_CAPACITY
        + " | 倉庫負載: " + party.getInventory().getSlots().size() + "/" + party.getInventory().getCapacity() + "）");

    if (broadcastService != null) {
      broadcastService.broadcastState(player);
    }
  }

  private PartyItemSlot findPartySlot(Party party, String keyword) {
    if (party == null || party.getInventory() == null) return null;
    var slots = party.getInventory().getSlots();
    if (slots.isEmpty()) return null;

    // 嘗試按序號 (1-based)
    try {
      int idx = Integer.parseInt(keyword);
      if (idx >= 1 && idx <= slots.size()) {
        return slots.get(idx - 1);
      }
    } catch (NumberFormatException ignored) {}

    return slots.stream()
        .filter(s -> s.getSlotId().equalsIgnoreCase(keyword)
            || s.getItemId().equalsIgnoreCase(keyword)
            || s.getName().contains(keyword))
        .findFirst()
        .orElse(null);
  }

  private void showUsage(Player player, Party party) {
    int pSize = player.getInventory().size();
    int sSize = party.getInventory().getSlots().size();
    int sCap = party.getInventory().getCapacity();

    player.reply("\n\u001B[1;36m═══════════════【行囊與倉庫物品調度】═══════════════\u001B[0m\n"
        + " 💡【行囊大一統】：小隊行囊已全面共用！拾取之物品皆直接入庫，無需繁瑣轉存。\n"
        + " 目前負載: 小隊行囊 [" + sSize + "/" + sCap + "] 格 | 隨身相容視圖 [" + pSize + "/" + GetCommand.PERSONAL_INVENTORY_CAPACITY + "] 格\n"
        + " 調度指令（相容支援）:\n"
        + "   stash <物品名稱>     - 將隨身行囊物品存入小隊倉庫 (別名 store / 存入)\n"
        + "   withdraw <序號/名稱>  - 從小隊倉庫取出物品至隨身行囊 (別名 take / 取出)\n"
        + "\u001B[1;36m════════════════════════════════════════════════════\u001B[0m");
  }

  @Override
  public String getDescription() {
    return "在個人隨身行囊與小隊儲物倉庫之間調度物品 (支援 stash/withdraw/store/take)";
  }
}
