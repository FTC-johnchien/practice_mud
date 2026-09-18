# Items 全域化與商店/地圖本地化參數 (數量/售價覆寫) 實施計畫與架構決策

本文件記錄 2026-09-18 實施的生靈與物品階層治理，將原本碎片化分散在各區域（`zones/*/items.json`）的物品**原型（Prototype）**提升至全域層級（`data/global/items/`），並讓商店（Shop）與地圖房間（Room/SpawnRule）等本地實例僅需指定引用 ID 與本地化參數（數量、限購庫存、地區浮動售價覆寫）。

---

## 1. 架構設計與目錄劃分

### 1.1 全域物品原型分類 (`data/global/items/`)
參照技能（`skills/`）的成功分類模式，將物品按大類分門別類存放在 `data/global/items/` 下，支援 `classpath:data/global/items/**/*.json` 遞迴通配載入：

```text
data/global/items/
├── weapons/          # 兵刃 (劍、刀、槍、棍、斧、錘、匕首、弓、法杖)
├── armors/           # 防具 (重鎧、布袍、輕甲、盾牌)
├── accessories/      # 飾品 (戒指、玉佩、項鍊)
├── consumables/      # 消耗品 (丹藥、靈茶、乾糧、符籙、靈石碎片)
├── materials/        # 鍛造、煉丹與戰利品素材 (毛皮、尾巴、礦石)
├── currencies/       # 流通貨幣 (銅錢、銀兩、靈石)
└── quest/            # 任務信物與特殊鑰匙 (長老玉牌、村長信物、小屋鑰匙)
```

### 1.2 本地化實例覆寫機制 (Prototype-Instance Pattern)
商店與地圖不再重新定義物品的數值屬性，而是專注於「商業流通」與「世界擺放」參數：

```mermaid
flowchart LR
    subgraph Global [全域物品原型 (Prototype)]
        P1["village_bread<br/>Type: CONSUMABLE, Value: 2"]
        P2["healing_salve<br/>Type: CONSUMABLE, Value: 8"]
        P3["steel_blade<br/>Type: WEAPON, Value: 15"]
    end

    subgraph ShopInstance [商店商品配置 (ShopItemTemplate)]
        S1["商品 1: bread<br/>templateId: village_bread<br/>price: 2 (原價)<br/>stock: -1 (無限供應)"]
        S2["商品 2: salve<br/>templateId: healing_salve<br/>price: 8 (原價)<br/>stock: 10 (限量10盒)"]
        S3["商品 3: blade<br/>templateId: steel_blade<br/>priceMultiplier: 1.2 (邊境溢價20%)<br/>stock: 3 (精良限量)"]
    end

    P1 --> S1
    P2 --> S2
    P3 --> S3
```

---

## 2. 核心代碼改造重點

### 2.1 全域載入與雙向相容查找
* **`WorldManager.java`**：
  - 新增 `loadGlobalItemData()`：透過 Spring `resourceResolver.getResources("classpath:data/global/items/**/*.json")` 掃描註冊。
  - 支援單一物件與物件陣列兩種 JSON 結構，全域物品以純淨 ID 註冊（如 `healing_salve`、`steel_blade`）。
  - 保留 `zones/*/items.json` 作為區域特有物品載入，維護既有地圖向下相容。
* **`TemplateRepository.java`**：
  - 升級 `findItem(String id)` 的雙向容錯：
    1. 精確比對 `id`。
    2. 若 `id` 含有 `:`（例如傳入 `newbie_village:healing_salve`），自動剝離前綴並查詢全域 `healing_salve`。
    3. 若 `id` 不含 `:`，在庫中無直接匹配時，尋找以 `":" + id` 結尾的項目。
  - **保證既有所有 85 項單元測試與存檔零破壞、零回歸**。

### 2.2 商店模板與實例計算擴充
* **`ShopTemplate.java`**（擴充 `ShopItemTemplate`）：
  - 新增欄位：
    - `Integer stock`：限量庫存（`-1` 或 `null` 為無限供應；`>= 0` 為限量剩餘）。
    - `Integer price`：本地指定定價（若未填，回退至 `priceMultiplier` 或物品基礎價值 `ItemTemplate.value()`）。
    - `Double priceMultiplier`：價格倍率（例如 `1.5` 為 1.5 倍物價）。
  - 新增業務方法：
    - `int getEffectivePrice()`：動態解析最終結算價格。
    - `String getEffectiveName()`：若無自訂商品名，自動繼承 `ItemTemplate.name()`。
    - `String getEffectiveDescription()`：若無自訂說明，自動繼承 `ItemTemplate.description()`。
    - `int getEffectiveStock()`：回傳有效庫存。
* **`ShopCommand.java`**：
  - 購買結算使用 `getEffectivePrice()`。
  - 支援限量檢核與動態扣減：使用線程安全 `shopStockTracker` 維護實例庫存，庫存不足或售罄時拒絕交易並給出掌櫃客氣回絕提示。
  - `ShopCatalogDto` 傳遞 `stock` 資訊給前端。
* **`drpg-view.js`**：
  - 彈窗介面渲染庫存狀態（`庫存: N` 或 `充足`）。
  - 庫存為 0 時按鈕標註 `❌ 售罄` 並禁用。

---

## 3. 測試驗證
透過 `GlobalItemsAndShopOverridesTest.java` 完成 5 大面向驗證：
1. 全域物品庫遞迴掃描載入正確性。
2. 帶有 zoneId 前綴與純 ID 的雙向智慧容錯查詢。
3. 商店本地化實例參數（定價覆寫、價格倍率、原型名稱說明繼承）。
4. 商店限量庫存管理與動態扣減。
5. 真實 `newbie_village/shops.json` 客棧貨棧商品載入與限量參數。

執行 `.\test.ps1`，90 項測試全數綠燈通過。
