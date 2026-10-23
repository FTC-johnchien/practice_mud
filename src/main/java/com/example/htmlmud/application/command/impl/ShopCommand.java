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
import com.example.htmlmud.template.model.ShopTemplate;
import com.example.htmlmud.template.model.ShopTemplate.ShopItemTemplate;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyItemSlot;
import com.example.htmlmud.party.PartyService;
import com.example.htmlmud.pipeline.GameStateBroadcastService;
import com.example.htmlmud.template.TemplateReader;
import lombok.extern.slf4j.Slf4j;

/**
 * 貨棧買賣與小隊共享金庫出納命令 (SP-09)。
 * 購買消耗小隊金庫、物品入庫小隊公共行囊；出售小隊行囊物品回收靈石入庫小隊金庫。
 * 隊長個人 LivingStats.coin 與小隊金庫保持單一真相源零延遲鏡像同步。
 */
@Slf4j
@Component
@CommandAlias({"buy", "list", "store", "sell"})
public class ShopCommand implements PlayerCommand {

  private final PartyService partyService;
  private final GameStateBroadcastService broadcastService;
  private final TemplateReader templateReader;

  private final Map<String, Integer> shopStockTracker = new ConcurrentHashMap<>();

  public ShopCommand(
      PartyService partyService,
      GameStateBroadcastService broadcastService,
      TemplateReader templateReader) {
    this.partyService = partyService;
    this.broadcastService = broadcastService;
    this.templateReader = templateReader;
  }

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

    // 依當前房間查詢商店配置
    Optional<ShopTemplate> shopOpt = templateReader.findShopByRoomId(roomId);
    if (shopOpt.isEmpty()) {
      self.reply("此處荒郊野嶺，並無商販或客棧掌櫃可以交易。（請前往設有貨棧之處）");
      return;
    }

    ShopTemplate shop = shopOpt.get();
    Party party = partyService.getOrCreateParty(self.getName());
    syncPlayerAndPartyCoin(self, party);

    if (args == null || args.isBlank() || "list".equalsIgnoreCase(args.trim())) {
      showShopList(self, shop, party);
      return;
    }

    String input = args.trim();
    if (input.toLowerCase().startsWith("sell")) {
      handleSell(self, shop, party, input);
      return;
    }

    if (input.toLowerCase().startsWith("buy ")) {
      input = input.substring(4).trim();
    }

    List<ShopItemTemplate> goods = shop.goods();

    // 解析購買數量 (支援: "1 5" 或 "tea 3" 或 "1")
    int count = 1;
    String itemKeyword = input;
    String[] tokens = input.split("\\s+");
    if (tokens.length >= 2) {
      try {
        int parsedCount = Integer.parseInt(tokens[tokens.length - 1]);
        if (parsedCount > 0) {
          count = parsedCount;
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
          || i.getEffectiveName(templateReader).toLowerCase().contains(nameOrId)).findFirst().orElse(null);
    }

    if (selected == null) {
      self.reply("【" + getNpcName(shop) + "】道：「客官，小店沒有這件貨物，請對照貨架清單輸入代號或編號！」");
      showShopList(self, shop, party);
      return;
    }

    // 檢核限量庫存
    int availableStock = getStock(shop.id(), selected);
    if (availableStock == 0) {
      self.reply("【" + getNpcName(shop) + "】抱歉地躬身道：「客官來得不巧，小店的【" + selected.getEffectiveName(templateReader) + "】已全數售罄，尚在等待進貨呢！」");
      return;
    }
    if (availableStock > 0 && availableStock < count) {
      self.reply("【" + getNpcName(shop) + "】抱歉地笑道：「客官，小店【" + selected.getEffectiveName(templateReader) + "】庫存吃緊，當前僅剩 "
          + availableStock + " 件，無法滿足 " + count + " 件之需！」");
      return;
    }

    int effectivePrice = selected.getEffectivePrice(templateReader);
    int currentCoin = party.getCoin();
    int totalPrice = effectivePrice * count;
    if (currentCoin < totalPrice) {
      self.reply("【" + getNpcName(shop) + "】抱歉地笑道：「客官身上的靈石/盤纏不夠呢！（需要 " + totalPrice
          + " 靈石，小隊金庫當前僅有 " + currentCoin + " 靈石）」");
      return;
    }

    String templateIdToGive = selected.templateId() != null ? selected.templateId() : selected.id();
    boolean added = party.getInventory().addItem(templateIdToGive, count);
    if (!added) {
      self.reply("小隊行囊空間已滿，無法再裝入新物品！");
      return;
    }

    // 扣減金幣與庫存 (小隊金庫為單一真相源，並同步隊長個人數值)
    party.deductCoin(totalPrice);
    if (self.getStats() != null) {
      self.getStats().setCoin(party.getCoin());
    }
    deductStock(shop.id(), selected, count);

    int remainingStock = getStock(shop.id(), selected);
    String stockNotice = remainingStock >= 0 ? "（小店剩餘庫存: " + remainingStock + "）" : "（供應充足）";

    self.reply("💰【購買成功】你花費了 " + totalPrice + " 靈石購入了 " + count + " 件【" + selected.getEffectiveName(templateReader) + "】！\n"
        + "物品已安全收納入【隊伍行囊】。" + stockNotice + "（小隊金庫剩餘: " + party.getCoin() + " 靈石）");

    broadcastService.broadcastState(self);
    sendShopCatalog(self, shop, party);
  }

  private void handleSell(Player self, ShopTemplate shop, Party party, String input) {
    String rest = input.toLowerCase().startsWith("sell ") ? input.substring(5).trim() : "";
    if (rest.isBlank() || "list".equalsIgnoreCase(rest)) {
      showSellList(self, shop, party);
      return;
    }

    // 解析出售數量 (支援: "1 5" 或 "herb 2" 或 "1")
    int count = 1;
    String itemKeyword = rest;
    String[] tokens = rest.split("\\s+");
    if (tokens.length >= 2) {
      try {
        int parsedCount = Integer.parseInt(tokens[tokens.length - 1]);
        if (parsedCount > 0) {
          count = parsedCount;
          itemKeyword = rest.substring(0, rest.lastIndexOf(tokens[tokens.length - 1])).trim();
        }
      } catch (NumberFormatException ignored) {}
    }

    List<PartyItemSlot> slots = party.getInventory().getSlots();
    if (slots == null || slots.isEmpty()) {
      self.reply("【" + getNpcName(shop) + "】客氣道：「客官的小隊公共行囊中空無一物，暫無可供出售的法寶或素材！」");
      return;
    }

    PartyItemSlot selectedSlot = null;
    try {
      int idx = Integer.parseInt(itemKeyword);
      if (idx >= 1 && idx <= slots.size()) {
        selectedSlot = slots.get(idx - 1);
      }
    } catch (NumberFormatException ignored) {
      String kw = itemKeyword.toLowerCase();
      selectedSlot = slots.stream().filter(s ->
          (s.getItemId() != null && s.getItemId().equalsIgnoreCase(kw)) ||
          (s.getName() != null && s.getName().toLowerCase().contains(kw)) ||
          (s.getSlotId() != null && s.getSlotId().equalsIgnoreCase(kw))
      ).findFirst().orElse(null);
    }

    if (selectedSlot == null) {
      self.reply("【" + getNpcName(shop) + "】搖頭笑道：「客官小隊行囊中並無此件物品，請輸入「sell」查看小隊可回收之物！」");
      return;
    }

    if (selectedSlot.getCount() < count) {
      self.reply("【" + getNpcName(shop) + "】抱歉地笑道：「客官小隊行囊中的【" + selectedSlot.getName() + "】僅存 "
          + selectedSlot.getCount() + " 件，無法出售 " + count + " 件之多！」");
      return;
    }

    // 計算回收價 (以物品原價之半價回收，若低於 2 則以 1 靈石回收)
    int basePrice = 2;
    var itemOpt = templateReader.findItem(selectedSlot.getItemId());
    if (itemOpt.isPresent()) {
      basePrice = itemOpt.get().value();
    }
    int unitSellPrice = Math.max(1, basePrice / 2);
    int totalEarn = unitSellPrice * count;

    party.getInventory().removeItem(selectedSlot.getSlotId(), count);
    party.addCoin(totalEarn);
    if (self.getStats() != null) {
      self.getStats().setCoin(party.getCoin());
    }

    self.reply("💰【出售成功】小隊公共行囊出售了 " + count + " 件【" + selectedSlot.getName() + "】，換得 "
        + totalEarn + " 靈石！\n（小隊金庫剩餘: " + party.getCoin() + " 靈石）");

    broadcastService.broadcastState(self);
    sendShopCatalog(self, shop, party);
  }

  private void showSellList(Player self, ShopTemplate shop, Party party) {
    List<PartyItemSlot> slots = party.getInventory().getSlots();
    StringBuilder sb = new StringBuilder();
    sb.append("🛒【小隊行囊回收櫃檯】\n");
    sb.append("【").append(getNpcName(shop)).append("】笑吟吟地道：「客官若有多餘的妖獸素材或行囊雜物，小店童叟無欺，皆可按半價靈石回收！」\n");
    sb.append("----------------------------------------------------------------------\n");
    if (slots == null || slots.isEmpty()) {
      sb.append(" (小隊公共行囊中空空如也，暫無可出售物品)\n");
    } else {
      for (int i = 0; i < slots.size(); i++) {
        PartyItemSlot s = slots.get(i);
        int basePrice = 2;
        var itemOpt = templateReader.findItem(s.getItemId());
        if (itemOpt.isPresent()) basePrice = itemOpt.get().value();
        int unitPrice = Math.max(1, basePrice / 2);
        sb.append(String.format(" [%d] %-14s (數量: %2d) - 回收單價: %2d 靈石 (總值: %d 靈石)\n",
            i + 1, s.getName(), s.getCount(), unitPrice, unitPrice * s.getCount()));
      }
    }
    sb.append("----------------------------------------------------------------------\n");
    sb.append("💰 小隊金庫當前持有: ").append(party.getCoin()).append(" 靈石\n");
    sb.append("💡 輸入「sell <編號/名稱> [數量]」（例如 sell 1 2 或 sell herb 1）即可出售！");
    self.reply(sb.toString());
  }

  private void showShopList(Player self, ShopTemplate shop, Party party) {
    if (self.getOutput() != null) {
      sendShopCatalog(self, shop, party);
      self.reply("🏪 已開啟【" + shop.name() + "】交易櫃檯，【" + getNpcName(shop) + "】正候著您。（可於彈出視窗點選購買或按 Esc 關閉）");
      return;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("🛒【").append(shop.name()).append("】\n");
    sb.append("【").append(getNpcName(shop)).append("】招呼道：「").append(getNpcGreeting(shop)).append("」\n");
    sb.append("----------------------------------------------------------------------\n");
    for (ShopItemTemplate item : shop.goods()) {
      int s = getStock(shop.id(), item);
      String stockLabel = s >= 0 ? ("庫存: " + s) : "充足";
      sb.append(String.format(" [%d] %-10s - %-14s 價格: %2d 靈石 [%s] (%s)\n",
          item.index(), item.id(), item.getEffectiveName(templateReader), item.getEffectivePrice(templateReader), stockLabel, item.getEffectiveDescription(templateReader)));
    }
    sb.append("----------------------------------------------------------------------\n");
    sb.append("💰 小隊金庫持有靈石/盤纏: ").append(party.getCoin()).append(" 靈石\n");
    sb.append("💡 輸入「buy <商品> [數量]」購入，或「sell [物品]」出售行囊多餘物資！");
    self.reply(sb.toString());
  }

  private void sendShopCatalog(Player self, ShopTemplate shop, Party party) {
    if (self.getOutput() == null) return;
    List<ShopCatalogDto.ShopCatalogItemDto> dtos = shop.goods().stream()
        .map(g -> new ShopCatalogDto.ShopCatalogItemDto(
            g.index(),
            g.id(),
            g.templateId() != null ? g.templateId() : g.id(),
            g.getEffectiveName(templateReader),
            g.getEffectivePrice(templateReader),
            getStock(shop.id(), g),
            g.getEffectiveDescription(templateReader)))
        .toList();
    ShopCatalogDto catalogDto = new ShopCatalogDto("SHOP_CATALOG", shop.id(), shop.name(), party.getCoin(), dtos);
    self.getOutput().sendJson(catalogDto);
  }

  private void syncPlayerAndPartyCoin(Player self, Party party) {
    if (self.getStats() != null && self.getStats().getCoin() != party.getCoin()) {
      party.setCoin(self.getStats().getCoin());
    }
  }

  private String getNpcName(ShopTemplate shop) {
    if (shop != null && shop.npcId() != null && !shop.npcId().isBlank()) {
      return templateReader.findMob(shop.npcId())
          .map(com.example.htmlmud.template.model.MobTemplate::name)
          .orElse(shop.npcId());
    }
    return "掌櫃";
  }

  private String getNpcGreeting(ShopTemplate shop) {
    if (shop != null && shop.npcId() != null && !shop.npcId().isBlank()) {
      var mobOpt = templateReader.findMob(shop.npcId());
      if (mobOpt.isPresent()) {
        java.util.List<String> dialogues = mobOpt.get().dialogues();
        if (dialogues != null && !dialogues.isEmpty()) {
          return dialogues.get(0);
        }
      }
    }
    return "客官請進，看看小店有些什麼所需之物！";
  }

  @Override
  public String getDescription() {
    return "貨棧買賣 (查看貨架商品、購買物資或出售小隊行囊物品)";
  }
}

