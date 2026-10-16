package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.service.PartyService;
import lombok.extern.slf4j.Slf4j;

/**
 * SP-02 (行囊大一統：除役個人隨身假帳雙向轉存)
 * <p>
 * 在純單機單體暗黑克蘇魯修仙架構中，所有物資統一收納於【小隊公共行囊】（Party Inventory），
 * 不再提供或維護個人隨身假帳背包（player.inventory）與倉庫之間的搬移邏輯。
 * 玩家輸入 stash / withdraw / store / take 時，一律提供友善導引並引導檢視小隊行囊。
 */
@Slf4j
@Component
@CommandAlias({"stash", "store", "存入", "withdraw", "take", "取出"})
public class StashCommand implements PlayerCommand {

  private final PartyService partyService;

  public StashCommand(PartyService partyService) {
    this.partyService = partyService;
  }

  @Override
  public String getKey() {
    return "stash";
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();
    Party party = partyService.getOrCreateParty(player.getName());

    int size = (party != null && party.getInventory() != null)
        ? party.getInventory().getSlots().size()
        : 0;
    int cap = (party != null && party.getInventory() != null)
        ? party.getInventory().getCapacity()
        : 40;

    player.reply("\n\u001B[1;36m═══════════════【道途行囊已全面大一統】═══════════════\u001B[0m\n"
        + " 💡【共享提示】：小隊道途行囊已全面大一統（40格公共儲物袋）！\n"
        + " 所有拾取、購買與戰利品皆自動納入全隊共享，無需手動存入或取出。\n"
        + " 目前小隊行囊負載: [" + size + "/" + cap + "] 格。\n"
        + " 請直接輸入 \u001B[1;32minventory\u001B[0m (或 \u001B[1;32mi\u001B[0m) 檢視行囊，或使用 \u001B[1;32mitem equip / use\u001B[0m 調配道具。\n"
        + "\u001B[1;36m════════════════════════════════════════════════════\u001B[0m");
  }

  @Override
  public String getDescription() {
    return "小隊公共行囊大一統導引 (除役舊版個人假帳轉存，所有物資全隊共用)";
  }
}
