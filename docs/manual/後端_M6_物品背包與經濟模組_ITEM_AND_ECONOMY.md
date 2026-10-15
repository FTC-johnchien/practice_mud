# 📦 後端 M6: 物品、背包與經濟模組深度手冊 (Item, Inventory & Economy Manual)

> **模組編號**：M6  
> **系統類型**：後端核心業務模組  
> **核心定位**：管理小隊共用儲物袋、裝備槽位穿脫、貨棧買賣交易、消耗品使用與靈物品質模型。

---

## 🎒 一、雙層背包模型架構

遊戲採用「個人速用背包 + 小隊共用儲物袋」的雙層結構：

```
┌────────────────────────────────────────────────────────┐
│                   30 格小隊共用儲物袋                   │
│                    (PartyInventory)                    │
├────────────────────────────────────────────────────────┤
│ • 全隊 5 人共用，地牢掉落物直接入庫                    │
│ • 支援最大堆疊 (靈石/丹藥 99 堆疊，裝備單件獨立)         │
│ • 任何隊員換裝皆可直接從儲物袋中調取裝備               │
└────────────────────────────────────────────────────────┘
                           ▲
                           │ stash 存入 / withdraw 取出
                           ▼
┌────────────────────────────────────────────────────────┐
│                   個人隨身背包 (6 格)                   │
├────────────────────────────────────────────────────────┤
│ • 個人速用乾糧、回春靈藥、任務信物                     │
│ • 個人裝備穿戴欄位 (武器、頭盔、胸甲、靴子、飾品)       │
└────────────────────────────────────────────────────────┘
```

---

## 🛡️ 二、裝備槽位與純資料驅動契約

嚴格遵循專案第一紅線，前端與後端換裝邏輯**完全拋棄中文名稱推測**，全面依賴 `EquipSlot` 枚舉：

```java
public enum EquipSlot {
  MAIN_HAND,  // 主手兵刃 (飛劍、巨錘、長槍、法杖)
  OFF_HAND,   // 副手防具 (法印、靈盾、劍鞘)
  HEAD,       // 頭部 (道冠、兜鍪、面具)
  BODY,       // 胸部 (道袍、玄甲、法衣)
  FEET,       // 足部 (踏雲靴、芒鞋)
  ACCESSORY,  // 飾品 (玉佩、手鐲、寶珠)
  RING        // 戒指 (儲物戒、煞靈戒)
}
```

### 換裝流水線 (`party-equipment-tab.js` ➔ `PartyCommand`)
1. **前端依槽位過濾**：僅顯示 `item.equipSlot == targetSlot` 的儲物袋物品，防範防具誤戴在武器欄。
2. **替換舊裝機制**：將舊裝備安全卸下放回小隊儲物袋，新裝備穿上並即時重算五維道基加成。

---

## 🛒 三、貨棧買賣與經濟系統 (Shop System)

城鎮中的客棧與商販提供貨棧交易服務（[`ShopCommand.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/application/command/impl/ShopCommand.java)）：

1. **資料驅動查詢**：
   - 根據當前房間 ID，向 `TemplateReader.findShopByRoomId(roomId)` 獲取商店範本。
   - 徹底拔除舊有的 `roomId.contains("inn")` 硬編碼降級。
2. **購買交易檢驗**：
   - 檢驗玩家碎銀/靈石餘額是否足夠。
   - 檢驗小隊儲物袋是否有空餘格位（未滿 30 格）。
   - 支援批次購買（如 `buy bread 5`）。
3. **靈藥消耗效果**：
   - **辟邪清心靈茶**：飲用回復 25 點道心（SAN 值）與 20 點真元。
   - **百草金創藥膏**：塗抹迅速回復 60 點氣血。

---

## 🔗 四、代表類別與資料設定檔

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **小隊背包** | `com.example.htmlmud.domain.party.model.PartyInventory` | 30 格小隊儲物袋堆疊、存取與序列化 |
| **物品實體** | `com.example.htmlmud.domain.model.GameItem` | 靈物實例、耐久度、詞綴與五維加成 |
| **槽位枚舉** | `com.example.htmlmud.domain.model.EquipSlot` | 裝備欄位嚴格枚舉契約 |
| **商店指令** | `com.example.htmlmud.application.command.impl.ShopCommand` | 貨棧清單列出、買入、賣出指令處理 |
| **拾取丟棄** | `com.example.htmlmud.application.command.impl.DropCommand` | 地面物品拾取（`get`）與丟棄（`drop`） |
| **資料設定** | `src/main/resources/data/items.json` | 全域裝備、法寶、丹藥與素材屬性模板 |
| **資料設定** | `src/main/resources/data/zones/*/shops.json` | 區域商店商品、價格與無限庫存宣告 |

---

## 🚀 五、未來架構演進與現代化擴充規劃 (Future Roadmap & Scalability)

為使物品、背包與經濟模組具備暗黑刷寶、修仙百藝與拍賣交易的耐玩深度，確立以下四大演進方向：

### 1. 🎯 現代遊戲機制深化 (Modern Gameplay & Depth)
- **隨機詞綴法寶生成與洗練 (Affix & Refinement System)**：
  地牢擊敗首領或開棺摸金時，掉落之法寶導入隨機詞綴（前綴如「破煞之·物理穿透+15%」，後綴如「幽冥之·受擊回藍+5」）。城鎮鐵匠鋪開放「重鑄與銘文洗練」，消耗靈石與煞石精華洗練詞條，打造極品本命法寶。
- **動態供需經濟與修仙跑商 (Dynamic Market & Leyline Trade)**：
  不同區域商店貨棧引入供需差價（如桃源村盛產靈米但缺丹藥，雪亭鎮急缺禦寒靈符）。玩家可在城鎮間行商套利，靈石物價隨全服消耗量動態微幅波動，增添生活修仙樂趣。

### 2. 🛡️ 穩定性與狀態防禦 (Robustness & Resilience)
- **交易原子性兩階段事務 (Two-Phase Commit Transaction)**：
  在進行小隊共用儲物袋存取、裝備替換或商店大額買賣時，採用嚴格的兩階段驗證（`ValidateInventorySpace` ➔ `DeductCurrency` ➔ `CommitTransfer`），若任何環節異常自動回滾（Rollback），徹底消滅「物品丟失」或「複製刷物」漏洞。
- **背包格位邊界保護**：為小隊儲物袋擴充滿載溢出緩衝區（Mailbox Overfill Buffer），若滿 30 格時戰鬥大捷掉落稀有法寶，自動暫存於臨時寄存箱，防範珍貴掉落物被強制銷毀。

### 3. ⚡ 效能與記憶體最佳化 (Performance & Concurrency)
- **物品原型享元架構 (Flyweight Item Model)**：
  靜態資料（圖標、重量、基礎描述、槽位枚舉）一律由 `ItemTemplate` 共享單例。動態實例（`GameItem`）僅記錄 `templateId`、自訂詞綴、耐久度與強化等級，記憶體開銷降至原本的 10%，序列化更加輕盈。

### 4. 🔌 可擴充性與模組化 (Extensibility & Modding)
- **宗門古寶套裝效果引擎 (Set Bonus Engine)**：
  透過 JSON 宣告套裝組合（如「太陰玄冥 4 件套：2 件提高 15% 坎水抗性，4 件使破防敵人受暴擊率 +30%」）。系統自動掃描隊員身上穿戴的套裝標籤並動態激活共鳴，大幅提升配裝策略維度。
