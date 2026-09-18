package com.example.htmlmud.application.command.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.template.ShopTemplate;
import com.example.htmlmud.domain.model.template.ShopTemplate.ShopItemTemplate;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.service.GameStateBroadcastService;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@CommandAlias({"buy", "list", "store"})
public class ShopCommand implements PlayerCommand {

  private final PartyService partyService;
  private final GameStateBroadcastService broadcastService;

  private final Map<String, Integer> shopStockTracker = new ConcurrentHashMap<>();

  public record ShopCatalogDto(
      String type,
      String shopId,
      String shopName,
      int playerCoin,
      List<ShopCatalogItemDto> goods
  ) {
    public record ShopCatalogItemDto(
        int index,
        String id,
        String templateId,
        String name,
        int price,
        int stock,
        String description
    ) {}
  }

  public int getStock(String shopId, ShopItemTemplate item) {
    if (item.getEffectiveStock() < 0) return -1;
    String stockKey = shopId + ":" + item.id();
    return shopStockTracker.computeIfAbsent(stockKey, k -> item.getEffectiveStock());
  }

  public synchronized void deductStock(String shopId, ShopItemTemplate item, int count) {
    if (item.getEffectiveStock() < 0) return;
    String stockKey = shopId + ":" + item.id();
    int current = shopStockTracker.getOrDefault(stockKey, item.getEffectiveStock());
    shopStockTracker.put(stockKey, Math.max(0, current - count));
  }

  public void resetShopStock(String shopId) {
    shopStockTracker.keySet().removeIf(k -> k.startsWith(shopId + ":"));
  }

  @Override
  public String getKey() {
    return "shop";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    String roomId = self.getCurrentRoomId();

    // 動態自 TemplateRepository 解析當前房間的商店，若無則嘗試新手村客棧作為保底
    Optional<ShopTemplate> shopOpt = TemplateRepository.findShopByRoomId(roomId);
    if (shopOpt.isEmpty()) {
      if (roomId != null && roomId.contains("inn")) {
        shopOpt = TemplateRepository.findShop("newbie_village:inn_shop");
      }
    }

    if (shopOpt.isEmpty()) {
      self.reply("此處荒郊野嶺，並無商販或客棧掌櫃可以交易。（請前往新手村客棧尋找福伯）");
      return;
    }

    ShopTemplate shop = shopOpt.get();
    List<ShopItemTemplate> goods = shop.goods();

    if (args == null || args.isBlank() || "list".equalsIgnoreCase(args.trim())) {
      showShopList(self, shop);
      return;
    }

    String input = args.trim();
    if (input.toLowerCase().startsWith("buy ")) {
      input = input.substring(4).trim();
    }

    // 解析購買數量 (支援: "1 5" 或 "tea 3" 或 "1")
    int count = 1;
    String itemKeyword = input;
    String[] tokens = input.split("\\s+");
    if (tokens.length >= 2) {
      try {
        int parsedCount = Integer.parseInt(tokens[tokens.length - 1]);
        if (parsedCount > 0) {
          count = parsedCount;
          // 前面的 tokens 為商品識別名稱/編號
          itemKeyword = input.substring(0, input.lastIndexOf(tokens[tokens.length - 1])).trim();
        }
      } catch (NumberFormatException ignored) {
        // 最後一個 token 不是數字，則整串當做商品名稱
      }
    }

    ShopItemTemplate selected = null;
    try {
      int idx = Integer.parseInt(itemKeyword);
      selected = goods.stream().filter(i -> i.index() == idx).findFirst().orElse(null);
    } catch (NumberFormatException ignored) {
      String nameOrId = itemKeyword.toLowerCase();
      selected = goods.stream().filter(i -> (i.id() != null && i.id().equalsIgnoreCase(nameOrId))
          || (i.templateId() != null && i.templateId().equalsIgnoreCase(nameOrId))
          || i.getEffectiveName().toLowerCase().contains(nameOrId)).findFirst().orElse(null);
    }

    if (selected == null) {
      self.reply("掌櫃福伯擦著汗道：「客官，小店沒有這件貨物，請對照貨架清單輸入代號或編號！」");
      showShopList(self, shop);
      return;
    }

    // 檢核限量庫存
    int availableStock = getStock(shop.id(), selected);
    if (availableStock == 0) {
      self.reply("掌櫃福伯抱歉地躬身道：「客官來得不巧，小店的【" + selected.getEffectiveName() + "】已全數售罄，尚在等待進貨呢！」");
      return;
    }
    if (availableStock > 0 && availableStock < count) {
      self.reply("掌櫃福伯抱歉地笑道：「客官，小店【" + selected.getEffectiveName() + "】庫存吃緊，當前僅剩 "
          + availableStock + " 件，無法滿足 " + count + " 件之需！」");
      return;
    }

    int effectivePrice = selected.getEffectivePrice();
    int currentCoin = self.getStats().getCoin();
    int totalPrice = effectivePrice * count;
    if (currentCoin < totalPrice) {
      self.reply("掌櫃福伯抱歉地笑道：「客官身上的靈石/盤纏不夠呢！（需要 " + totalPrice
          + " 靈石，您當前僅有 " + currentCoin + " 靈石）」");
      return;
    }

    Party party = partyService.getOrCreateParty(self.getName());
    String templateIdToGive = selected.templateId() != null ? selected.templateId() : selected.id();
    boolean added = party.getInventory().addItem(templateIdToGive, count);
    if (!added) {
      self.reply("小隊行囊空間已滿，無法再裝入新物品！");
      return;
    }

    // 扣減金幣與庫存
    self.getStats().setCoin(currentCoin - totalPrice);
    deductStock(shop.id(), selected, count);

    int remainingStock = getStock(shop.id(), selected);
    String stockNotice = remainingStock >= 0 ? "（小店剩餘庫存: " + remainingStock + "）" : "（供應充足）";

    self.reply("💰【購買成功】你花費了 " + totalPrice + " 靈石購入了 " + count + " 件【" + selected.getEffectiveName() + "】！\n"
        + "物品已安全收納入【隊伍行囊】。" + stockNotice + "（剩餘靈石/盤纏: " + self.getStats().getCoin() + " 靈石）");

    broadcastService.broadcastState(self);
    sendShopCatalog(self, shop);
  }

  private void showShopList(Player self, ShopTemplate shop) {
    if (self.getOutput() != null) {
      sendShopCatalog(self, shop);
      self.reply("🏪 已開啟【" + shop.name() + "】交易櫃檯，掌櫃正笑吟吟地候著您。（可於彈出視窗點選購買或按 Esc 關閉）");
      return;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("🛒【").append(shop.name()).append("】\n");
    sb.append("掌櫃笑吟吟地招呼：「客官要進墨竹山吧？帶足乾糧和清心靈茶才是保命上策！」\n");
    sb.append("----------------------------------------------------------------------\n");
    for (ShopItemTemplate item : shop.goods()) {
      int s = getStock(shop.id(), item);
      String stockLabel = s >= 0 ? ("庫存: " + s) : "充足";
      sb.append(String.format(" [%d] %-10s - %-14s 價格: %2d 靈石 [%s] (%s)\n",
          item.index(), item.id(), item.getEffectiveName(), item.getEffectivePrice(), stockLabel, item.getEffectiveDescription()));
    }
    sb.append("----------------------------------------------------------------------\n");
    sb.append("💰 您當前持有靈石/盤纏: ").append(self.getStats().getCoin()).append(" 靈石\n");
    sb.append("💡 輸入「buy <商品> [數量]」（例如 buy 1 5 或 buy tea 2）即可購入！");
    self.reply(sb.toString());
  }

  private void sendShopCatalog(Player self, ShopTemplate shop) {
    if (self.getOutput() == null) return;
    List<ShopCatalogDto.ShopCatalogItemDto> dtos = shop.goods().stream()
        .map(g -> new ShopCatalogDto.ShopCatalogItemDto(
            g.index(),
            g.id(),
            g.templateId() != null ? g.templateId() : g.id(),
            g.getEffectiveName(),
            g.getEffectivePrice(),
            getStock(shop.id(), g),
            g.getEffectiveDescription()))
        .toList();
    ShopCatalogDto catalogDto = new ShopCatalogDto("SHOP_CATALOG", shop.id(), shop.name(), self.getStats().getCoin(), dtos);
    self.getOutput().sendJson(catalogDto);
  }

  @Override
  public String getDescription() {
    return "貨棧買賣 (查看貨架商品或購買乾糧與靈藥)";
  }
}
