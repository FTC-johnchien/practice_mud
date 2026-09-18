package com.example.htmlmud.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.htmlmud.application.command.impl.ShopCommand;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.template.ShopTemplate;
import com.example.htmlmud.domain.model.template.ShopTemplate.ShopItemTemplate;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;

@SpringBootTest
@ActiveProfiles("test")
public class GlobalItemsAndShopOverridesTest {

  @Autowired
  private ShopCommand shopCommand;

  @Test
  @DisplayName("1. 驗證全域物品庫 (Global Items) 遞迴自動掃描載入")
  void testGlobalItemsLoaded() {
    // 消耗品類
    Optional<ItemTemplate> bread = TemplateRepository.findItem("village_bread");
    assertThat(bread).isPresent();
    assertThat(bread.get().name()).contains("烤麵包");
    assertThat(bread.get().isStackable()).isTrue();

    Optional<ItemTemplate> salve = TemplateRepository.findItem("healing_salve");
    assertThat(salve).isPresent();
    assertThat(salve.get().value()).isEqualTo(8);

    // 兵刃類
    Optional<ItemTemplate> blade = TemplateRepository.findItem("steel_blade");
    assertThat(blade).isPresent();
    assertThat(blade.get().subType()).isEqualTo("BLADE");

    Optional<ItemTemplate> spear = TemplateRepository.findItem("standard_spear");
    assertThat(spear).isPresent();
    assertThat(spear.get().equipmentProp().minDamage()).isEqualTo(15);

    // 防具與飾品
    Optional<ItemTemplate> armor = TemplateRepository.findItem("rusty_armor");
    assertThat(armor).isPresent();

    Optional<ItemTemplate> ring = TemplateRepository.findItem("rusty_ring");
    assertThat(ring).isPresent();

    // 貨幣與材料
    Optional<ItemTemplate> coin = TemplateRepository.findItem("copper_coin");
    assertThat(coin).isPresent();
    assertThat(coin.get().type().name()).isEqualTo("CURRENCY");
  }

  @Test
  @DisplayName("2. 驗證帶有 zoneId 前綴與純 ID 的雙向智慧容錯查詢")
  void testBidirectionalIdLookup() {
    // 2.1 帶前綴查詢只在全域定義的物品 (驗證前綴剝離回退查找全域庫)
    Optional<ItemTemplate> prefixedSword = TemplateRepository.findItem("newbie_village:iron_sword");
    assertThat(prefixedSword).isPresent();
    assertThat(prefixedSword.get().id()).isEqualTo("iron_sword");

    // 2.2 帶前綴查詢既有消耗品，驗證名稱與藥效皆正確解析
    Optional<ItemTemplate> prefixedSalve = TemplateRepository.findItem("newbie_village:healing_salve");
    assertThat(prefixedSalve).isPresent();
    assertThat(prefixedSalve.get().name()).contains("金創藥");

    // 2.3 不帶前綴查詢區域特有物品 (驗證後綴反向匹配)
    Optional<ItemTemplate> scythe = TemplateRepository.findItem("black_obsidian_scythe");
    assertThat(scythe).isPresent();
    assertThat(scythe.get().name()).contains("骨鐮");
  }

  @Test
  @DisplayName("3. 驗證商店本地化實例參數 (定價覆寫、價格倍率與名稱說明繼承)")
  void testShopItemOverrides() {
    // 3.1 直接覆寫價格
    ShopItemTemplate customPriceItem = ShopItemTemplate.builder()
        .index(1)
        .id("test_salve")
        .templateId("healing_salve")
        .price(18) // 原品價值 8，商店覆寫為 18
        .stock(5)
        .build();

    assertThat(customPriceItem.getEffectivePrice()).isEqualTo(18);
    assertThat(customPriceItem.getEffectiveName()).isEqualTo("百草金創藥膏");
    assertThat(customPriceItem.getEffectiveStock()).isEqualTo(5);
    assertThat(customPriceItem.getEffectiveDescription()).contains("塗抹傷口可迅速癒合");

    // 3.2 價格倍率 (priceMultiplier) 浮動定價
    ShopItemTemplate multiplierItem = ShopItemTemplate.builder()
        .index(2)
        .id("test_bread")
        .templateId("village_bread") // 原品價值 2
        .priceMultiplier(2.5) // 2 * 2.5 = 5
        .build();

    assertThat(multiplierItem.getEffectivePrice()).isEqualTo(5);
    assertThat(multiplierItem.getEffectiveName()).isEqualTo("村莊烤麵包");
    assertThat(multiplierItem.getEffectiveStock()).isEqualTo(-1); // 預設無窮

    // 3.3 完全未指定 price 與 multiplier，自動回退到物品原型基礎價值
    ShopItemTemplate defaultItem = ShopItemTemplate.builder()
        .index(3)
        .id("test_blade")
        .templateId("steel_blade") // 原品價值 15
        .build();

    assertThat(defaultItem.getEffectivePrice()).isEqualTo(15);
    assertThat(defaultItem.getEffectiveName()).isEqualTo("百辟精鋼刀");

    // 3.4 覆寫名稱與說明
    ShopItemTemplate customNameItem = ShopItemTemplate.builder()
        .index(4)
        .id("test_special_spear")
        .templateId("standard_spear")
        .name("【福伯親傳】祖傳長槍")
        .description("福伯年輕時闖蕩江湖所用的老槍，殺氣凜然。")
        .price(88)
        .build();

    assertThat(customNameItem.getEffectiveName()).isEqualTo("【福伯親傳】祖傳長槍");
    assertThat(customNameItem.getEffectiveDescription()).contains("福伯年輕時闖蕩江湖");
    assertThat(customNameItem.getEffectivePrice()).isEqualTo(88);
  }

  @Test
  @DisplayName("4. 驗證商店限量庫存管理與動態扣減")
  void testShopStockManagement() {
    ShopItemTemplate limitedItem = ShopItemTemplate.builder()
        .index(1)
        .id("limited_pill")
        .templateId("taiyin_pill")
        .price(30)
        .stock(3) // 限量 3 顆
        .build();

    String shopId = "test_village_shop";
    shopCommand.resetShopStock(shopId);

    // 初始庫存為 3
    assertThat(shopCommand.getStock(shopId, limitedItem)).isEqualTo(3);

    // 購買 2 顆後剩餘 1
    shopCommand.deductStock(shopId, limitedItem, 2);
    assertThat(shopCommand.getStock(shopId, limitedItem)).isEqualTo(1);

    // 再購買 1 顆後售罄 (0)
    shopCommand.deductStock(shopId, limitedItem, 1);
    assertThat(shopCommand.getStock(shopId, limitedItem)).isEqualTo(0);

    // 無限供應商品 stock 為 -1
    ShopItemTemplate infiniteItem = ShopItemTemplate.builder()
        .index(2)
        .id("inf_bread")
        .templateId("village_bread")
        .stock(-1)
        .build();

    assertThat(shopCommand.getStock(shopId, infiniteItem)).isEqualTo(-1);
    shopCommand.deductStock(shopId, infiniteItem, 999);
    assertThat(shopCommand.getStock(shopId, infiniteItem)).isEqualTo(-1);
  }

  @Test
  @DisplayName("5. 驗證真實 newbie_village/shops.json 客棧貨棧商品載入與限量參數")
  void testRealShopTemplateLoaded() {
    Optional<ShopTemplate> innShopOpt = TemplateRepository.findShop("inn_shop");
    if (innShopOpt.isEmpty()) {
      innShopOpt = TemplateRepository.findShop("newbie_village:inn_shop");
    }
    assertThat(innShopOpt).isPresent();
    ShopTemplate innShop = innShopOpt.get();

    // 驗證麵包為無窮 (-1)
    ShopItemTemplate bread = innShop.goods().stream()
        .filter(g -> "bread".equalsIgnoreCase(g.id()))
        .findFirst().orElseThrow();
    assertThat(bread.getEffectiveStock()).isEqualTo(-1);
    assertThat(bread.getEffectivePrice()).isEqualTo(2);

    // 驗證金創藥膏限量 10 盒
    ShopItemTemplate salve = innShop.goods().stream()
        .filter(g -> "salve".equalsIgnoreCase(g.id()))
        .findFirst().orElseThrow();
    assertThat(salve.getEffectiveStock()).isEqualTo(10);
    assertThat(salve.getEffectivePrice()).isEqualTo(8);

    // 驗證鐵鎬商品載入、限量 5 把且關聯全域物品
    ShopItemTemplate pickaxe = innShop.goods().stream()
        .filter(g -> "pickaxe".equalsIgnoreCase(g.id()))
        .findFirst().orElseThrow();
    assertThat(pickaxe.templateId()).isIn("miner_pickaxe", "newbie_village:miner_pickaxe");
    assertThat(pickaxe.getEffectiveStock()).isEqualTo(5);
    assertThat(pickaxe.getEffectivePrice()).isEqualTo(25);
    assertThat(TemplateRepository.findItem(pickaxe.templateId())).isPresent();
    assertThat(TemplateRepository.findItem(pickaxe.templateId()).get().name()).contains("鶴嘴鋤");
  }
}
