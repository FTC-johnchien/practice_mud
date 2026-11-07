# 📦 後端 M6: 物品、背包與經濟模組深度手冊 (Item, Inventory & Economy Manual)

> **模組編號**：M6  
> **系統類型**：後端核心業務模組  
> **核心定位**：管理小隊共用儲物袋、裝備槽位穿脫、貨棧買賣交易、消耗品使用與靈物品質模型。

---

## 🎒 一、大一統小隊行囊架構 (Unified Party Inventory)

專案確立以 **40 格小隊共用儲物袋 (`PartyInventory`)** 作為全遊戲唯一的道具真實載體，所有物資拾取、購買與戰利品全隊共享：

```
┌────────────────────────────────────────────────────────┐
│                   40 格小隊共用行囊                     │
│                    (PartyInventory)                    │
├────────────────────────────────────────────────────────┤
│ • 全隊 5 人共用，地牢掉落物與任務獎勵直接入庫          │
│ • 支援最大堆疊 (靈石/丹藥 99 堆疊，裝備單件獨立)         │
│ • 5 位隊員換裝皆直接從小隊行囊中存取，零假帳雙寫       │
│ • 拾取 (get)、丟棄 (drop)、商店買賣 (buy) 直接操作行囊 │
└────────────────────────────────────────────────────────┘
```

---

## 🛡️ 二、裝備槽位與純資料驅動契約 (EquipmentSlot 5+2 結構)

嚴格遵循專案第一紅線，前端與後端換裝邏輯**完全拋棄中文名稱推測**，全面依賴 [`EquipmentSlot`](../../../src/main/java/com/example/htmlmud/item/EquipmentSlot.java) 枚舉（JSON / DTO 序列化欄位為 `equipSlot`）：

```java
public enum EquipmentSlot {
  // === 5 大基礎防禦與兵刃部位 ===
  MAIN_HAND,    // 主手兵刃 (飛劍、巨錘、長槍、法杖)
  OFF_HAND,     // 副手防具 (法印、靈盾、劍鞘)
  HEAD,         // 頭部防具 (道冠、兜鍪、面具)
  BODY,         // 身軀防具 (道袍、玄甲、法衣)
  FEET,         // 靴履防具 (踏雲靴、芒鞋、鐵靴)

  // === 2 大擴充靈寶飾品部位 (雙飾品體系) ===
  ACCESSORY_1,  // 本命法寶 / 飾品一 (玉佩、寶珠、靈幡、護符)
  ACCESSORY_2   // 輔佐靈寶 / 飾品二 (戒指、手鐲、魂印、法印)
}
```

### 換裝流水線 (`party-equipment-tab.js` ➔ `PartyCommand`)
1. **前端依槽位精準過濾**：僅顯示 `item.equipSlot == targetSlot` 的小隊行囊物品，防範防具誤戴在武器欄，杜絕字串推測。
2. **替換舊裝機制**：將舊裝備安全卸下放回小隊 40 格行囊，新裝備穿上並即時重算五維道基與抗性加成。

---

## 🛒 三、貨棧買賣與小隊共享金庫經濟體系 (Unified Party Economy & Coin)

專案確立以 **小隊共享金庫 (`Party.coin`)** 作為全隊單一真相源（SSOT）：

```
┌────────────────────────────────────────────────────────┐
│                   全隊大一統共享經濟體系                │
│             Party.coin (全隊共享靈石金庫 SSOT)           │
├────────────────────────────────────────────────────────┤
│ • 商店購買 (buy)  ➔ 檢核小隊金庫 ➔ 扣款 ➔ 入庫 40格行囊  │
│ • 行囊出售 (sell) ➔ 從 40 格行囊扣除 ➔ 半價靈石入金庫   │
│ • 戰鬥大捷繳獲    ➔ 妖獸掉落靈石直接入帳小隊金庫        │
│ • 地牢探索寶箱    ➔ 寶箱暗格靈石直接入帳小隊金庫        │
│ • 隊長零延遲鏡像  ➔ leader.stats.coin 與金庫保持雙向同步│
└────────────────────────────────────────────────────────┘
```

1. **資料驅動查詢**：根據當前房間 ID，向 `TemplateReader.findShopByRoomId(roomId)` 獲取商店範本。
2. **小隊金庫出納流水線 (`ShopCommand.java`)**：
   - **購買 (buy)**：檢驗小隊金庫靈石餘額是否足夠；小隊 40 格行囊未滿時入庫，扣減小隊金庫並同步隊長個人數值。支援批次購買（如 `buy 1 5` 或 `buy bread 3`）。
   - **出售 (sell)**：支援回收小隊公共行囊中的多餘法寶與素材（如 `sell 1 2` 或 `sell herb 1`），以半價回收換取靈石直接入帳小隊金庫。
3. **戰鬥與探索結算 (`DrpgRewardService.java` & `DungeonCommand.java`)**：
   - 戰鬥大捷時，依據擊倒怪物品階計算繳獲靈石，直接注入 `Party.addCoin(rewardCoin)`。
   - 開啟地牢寶箱時，暗格繳獲靈石直接入庫小隊金庫。
4. **靈藥消耗效果**：
   - **辟邪清心靈茶**：飲用回復 25 點道心（SAN 值）與 20 點真元。
   - **百草金創藥膏**：塗抹迅速回復 60 點氣血。

---

## 🔍 四、多階靈識認知透鏡【待深入討論／長線演進藍圖】

現行 `CognitiveProxyService` 已實裝於營地六大功能設施之認知轉譯；道具級之多階認知透鏡（`lenses`）與 DTO `glitch` 標籤規劃為長線擴充藍圖：

```json
{
  "id": "item_bio_keratin_chip",
  "type": "MATERIAL",
  "value": 150,
  "lenses": [
    { "minProgress": 0,  "name": "一團怪物的血肉", "desc": "自地底礦脈妖物剝離的黏稠血肉，隱隱散發血腥溫熱。" },
    { "minProgress": 30, "name": "幾丁質活性骨晶", "desc": "表面硬化且密布神經節點，握持時使前世靈骨隱隱發燙。" },
    { "minProgress": 70, "name": "【強殖角質晶片·試作型】", "desc": "工程師二代仿生兵器組件，可鑲嵌解鎖戰技。" }
  ]
}
```

---

## 🔗 五、代表類別與資料設定檔

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **小隊背包** | `com.example.htmlmud.party.PartyInventory` | 40 格小隊儲物袋堆疊、存取與序列化 |
| **行囊槽位載體** | `com.example.htmlmud.party.PartyItemSlot` | 40 格行囊與裝備欄之實體載體，持有詞綴與削韌力 |
| **詞綴生成服務** | `com.example.htmlmud.item.ItemAffixService` | 隨機詞綴骰算、詞綴描述建構與削韌加成 |
| **物品實例** | `com.example.htmlmud.domain.model.entity.ItemInstance` | 靈物實體實例、基礎模板綁定 |
| **槽位枚舉** | `com.example.htmlmud.item.EquipmentSlot` | 裝備欄位嚴格枚舉契約 (5+2 結構) |
| **經濟服務** | `com.example.htmlmud.item.UnifiedPartyEconomyService` | 小隊共享金庫出納、買入、賣出與結算 |
| **靈識轉譯代理** | `com.example.htmlmud.town.CognitiveProxyService` | 依據晶片加載度動態過濾營地設施之認知呈現 |
| **資料設定** | `src/main/resources/data/global/items/weapons/weapons.json` | 全域裝備與法寶屬性模板 |
| **資料設定** | `src/main/resources/data/zones/*/shops.json` | 區域商店商品、價格與無限庫存宣告 |

---

## 🚀 六、未來架構演進規劃 (Roadmap)

### 1. 🎯 現代遊戲機制深化
- **隨機詞綴法寶生成（`ItemAffixService`）**【現行已驗證】：掉落法寶具備隨機前綴與後綴，直接影響削韌力與屬性。
- **城鎮鐵匠鋪重鑄與銘文洗練**【已決議待實作】：於百煉堂開放消耗靈石重鑄隨機詞條。
- **動態供需經濟與修仙跑商**【待深入討論／長線演進藍圖】：不同區域商店貨棧引入供需差價與跑商套利。
