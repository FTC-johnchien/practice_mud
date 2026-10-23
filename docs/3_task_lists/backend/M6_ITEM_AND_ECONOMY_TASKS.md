# 💎 M6: 物品裝備與經濟體系模組實作進度清單 (Item & Economy Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/backend/M6_ITEM_AND_ECONOMY.md`](../../2_specifications/backend/M6_ITEM_AND_ECONOMY.md)  
> **核心職責**：管理小隊 5+2 裝備部位（主副手/頭/身/足/雙飾品）、裝備稀有度、隨機道韻前綴/後綴詞綴池與小隊共享金庫經濟。  
> **當前完成度**：95% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心資料配置**：
  - `src/main/resources/data/global/items/`（分流目錄：weapons, armors, accessories, consumables, materials, currencies, quest）
  - `src/main/resources/data/zones/*/shops.json`（區域商店交易貨品）
- **核心領域實體與服務**：
  - [`ItemTemplate.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/template/model/ItemTemplate.java)（物品範本）
  - [`ItemDefinition.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/item/ItemDefinition.java)（物品不可變規範模型）
  - [`ItemInstance.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/item/ItemInstance.java)（運行期道具實體）
  - [`ItemAffix.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/item/ItemAffix.java)（隨機詞綴實體：PREFIX / SUFFIX，動態屬性累加）
  - [`PartyItemSlot.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/party/model/PartyItemSlot.java)（掛載詞綴之裝備插槽與動態命名）
  - [`EquipmentSlot.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/item/EquipmentSlot.java)（5+2 裝備槽位枚舉）
  - [`ItemFactory.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/item/ItemFactory.java)（集中化道具工廠）
  - [`UnifiedPartyEconomyService.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/item/UnifiedPartyEconomyService.java)（小隊大一統共享經濟服務）
- **關聯驗證測試**：
  - [`CyberSkillAndItemAffixTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/CyberSkillAndItemAffixTest.java)（詞綴池掛載與合成名稱 100% 綠燈）
  - [`UnifiedPartyEconomyTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/UnifiedPartyEconomyTest.java)（共享金庫與交易 100% 綠燈）
  - [`UnifiedPartyEconomyServiceTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/UnifiedPartyEconomyServiceTest.java)（3/3 綠燈）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **M6-PACKAGE-BY-FEATURE (模組 M6 套件獨立封裝)**
  - 依循依賴拓撲完成 M6 套件封裝，建立 `com.example.htmlmud.item` 獨立套件，將 `ItemDefinition`, `ItemInstance`, `ItemAffix`, `EquipmentSlot`, `ItemFactory` 遷移至新套件，並實作 `UnifiedPartyEconomyService` 統整金庫出納。
- [x] **M6-5PLUS2-SLOTS (5+2 裝備槽位體系)**
  - 確立 7 大部位：`MAIN_HAND`, `OFF_HAND`, `HEAD`, `BODY`, `FEET`, `ACCESSORY_1`, `ACCESSORY_2`。
- [x] **M6-AFFIX-SYSTEM (隨機道韻詞綴池與動態數值)**
  - 實作 `ItemAffix`：掉落時隨機抽取前後綴（如「超頻透鏡之」），名稱動態格式化，屬性動態累加至小隊五維。
- [x] **M6-UNIFIED-ECONOMY (小隊共享經濟體系)**
  - 金幣全域由小隊統一託管，商店購買直接從小隊行囊扣款，消除個人錢包割裂。
- [x] **M6-POISE-BREAK-POWER (削韌破防強度裝備屬性)**
  - 武器裝備支援 `poiseBreakPower`，戰鬥中直接增強隊員對敵方罡氣的破防打擊效力。
- [x] **M6-QUEST-CHIP-DATA (千星加密追蹤晶片資料實裝)**
  - 在 `src/main/resources/data/global/items/quest/quest_items.json` 正式建立 `item_chip_qianxing_tracker` 定義。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task M6-1 (VS-01-LOOT-CHIP / ACTIVE-TASK-4): 首領掉落任務晶片與隨機詞綴結算驗收**
  - **目標**：首領擊敗後由 `DrpgRewardService` 必定掉落 `item_chip_qianxing_tracker`，其餘武器隨機掛載前後綴詞條。
  - **驗收標準**：`victory-modal.js` 正確展示金色晶片與詞綴裝備，測試全綠。
- [ ] **Task M6-2 (WORKSHOP-REFORGE): 百煉坊裝備分解與道韻洗練重鑄**
  - **目標**：在營地工坊消耗靈石與煞晶，洗練裝備前後綴詞條數值或替換詞綴。
  - **驗收標準**：重鑄後數值在 `affixPool` 定義範圍內合理浮動，存讀檔一致。
- [ ] **Task M6-3 (COGNITIVE-ITEM-LENSES): 物品與素材之多階靈識認知代理過濾實作**
  - **目標**：在 `ItemTemplate` 與 `GameItem` 支援 `lenses` 陣列解析，對接 `CognitiveProxyService.filterItem(item, chipLoadingRate)`，使地牢掉落素材依晶片加載度由「一團怪物的血肉」動態解密為「活性神經幾丁質」及「仿生兵器晶片」。
  - **驗收標準**：單元測試驗證不同晶片加載度下，物品 DTO 投影精準輸出對應階級名稱、描述與 `glitch` 標籤。
