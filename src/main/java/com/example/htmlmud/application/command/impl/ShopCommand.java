package com.example.htmlmud.application.command.impl;

import java.util.List;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.service.PartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@CommandAlias({"buy", "list", "store"})
public class ShopCommand implements PlayerCommand {

  private final PartyService partyService;

  public record ShopItem(int index, String id, String templateId, String name, int price, String description) {}

  private static final List<ShopItem> INN_GOODS = List.of(
      new ShopItem(1, "bread", "newbie_village:village_bread", "村莊烤麵包", 2, "乾糧，食用回復 20 點氣血"),
      new ShopItem(2, "tea", "newbie_village:purify_tea", "辟邪清心靈茶", 5, "靈茶，飲用回復 25 點道心 (SAN) 與 20 點真元"),
      new ShopItem(3, "salve", "newbie_village:healing_salve", "百草金創藥膏", 8, "靈膏，塗抹迅速回復 60 點氣血"),
      new ShopItem(4, "pickaxe", "mozhu_mines:miner_pickaxe", "雜役採礦鐵鎬", 15, "耐用鐵鎬，可於礦坑挖掘靈石或破障")
  );

  @Override
  public String getKey() {
    return "shop";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    String roomId = self.getCurrentRoomId();

    boolean isInn = roomId != null && roomId.contains("inn");
    if (!isInn) {
      self.reply("此處荒郊野嶺，並無商販或客棧掌櫃可以交易。（請前往新手村客棧尋找福伯）");
      return;
    }

    if (args == null || args.isBlank() || "list".equalsIgnoreCase(args.trim())) {
      showShopList(self);
      return;
    }

    String input = args.trim();
    if (input.toLowerCase().startsWith("buy ")) {
      input = input.substring(4).trim();
    }

    ShopItem selected = null;
    try {
      int idx = Integer.parseInt(input);
      selected = INN_GOODS.stream().filter(i -> i.index() == idx).findFirst().orElse(null);
    } catch (NumberFormatException ignored) {
      String nameOrId = input.toLowerCase();
      selected = INN_GOODS.stream().filter(i -> i.id().equalsIgnoreCase(nameOrId)
          || i.templateId().equalsIgnoreCase(nameOrId)
          || i.name().contains(nameOrId)).findFirst().orElse(null);
    }

    if (selected == null) {
      self.reply("掌櫃福伯擦著汗道：「客官，小店沒有這件貨物，請對照貨架清單輸入代號或編號！」");
      showShopList(self);
      return;
    }

    int currentCoin = self.getStats().getCoin();
    if (currentCoin < selected.price()) {
      self.reply("掌櫃福伯抱歉地笑道：「客官身上的靈石/盤纏不夠呢！（需要 " + selected.price()
          + " 靈石，您當前僅有 " + currentCoin + " 靈石）」");
      return;
    }

    Party party = partyService.getOrCreateParty(self.getName());
    boolean added = party.getInventory().addItem(selected.templateId(), 1);
    if (!added) {
      self.reply("小隊行囊空間已滿，無法再裝入新物品！");
      return;
    }

    self.getStats().setCoin(currentCoin - selected.price());
    self.reply("💰【購買成功】你花費了 " + selected.price() + " 靈石購入了【" + selected.name() + "】！\n"
        + "物品已安全收納入【隊伍行囊】。（剩餘靈石/盤纏: " + self.getStats().getCoin() + " 靈石）");
  }

  private void showShopList(Player self) {
    StringBuilder sb = new StringBuilder();
    sb.append("🛒【村莊客棧·福伯的百寶貨架】\n");
    sb.append("福伯笑吟吟地招呼：「客官要進墨竹山吧？帶足乾糧和清心靈茶才是保命上策！」\n");
    sb.append("----------------------------------------------------------------------\n");
    for (ShopItem item : INN_GOODS) {
      sb.append(String.format(" [%d] %-10s - %-14s 價格: %2d 靈石 (%s)\n",
          item.index(), item.id(), item.name(), item.price(), item.description()));
    }
    sb.append("----------------------------------------------------------------------\n");
    sb.append("💰 您當前持有靈石/盤纏: ").append(self.getStats().getCoin()).append(" 靈石\n");
    sb.append("💡 輸入「buy <商品代號或編號>」（例如 buy 1 或 buy tea）即可購入並存入隊伍行囊！");
    self.reply(sb.toString());
  }

  @Override
  public String getDescription() {
    return "客棧貨棧 (查看貨架商品或購買乾糧與靈藥)";
  }
}
