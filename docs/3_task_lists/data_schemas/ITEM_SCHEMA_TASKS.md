# ⚔️ ITEM_SCHEMA: 裝備與隨機詞綴資料架構實作進度清單 (Item Schema Tasks)

> **文檔層級**：Level 3 — Task Lists (資料契約進度清單)  
> **對應規格**：👉 [`docs/2_specifications/data_schemas/ITEM_SCHEMA.md`](../../2_specifications/data_schemas/ITEM_SCHEMA.md)  
> **核心職責**：驗證 5+2 裝備部位、稀有度階級、詞綴池範本 (`affixPool`) 與掉落實例 (`affixes`) 之結構契約。  
> **當前完成度**：95% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心資料檔案**：
  - `src/main/resources/data/global/items/quest/quest_items.json`
  - `src/main/resources/data/global/items/weapons/`
- **核心實體與載入模型**：
  - [`ItemTemplate.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/template/model/ItemTemplate.java)
  - [`ItemAffix.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/item/ItemAffix.java)
- **關聯驗證測試**：
  - [`CyberSkillAndItemAffixTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/CyberSkillAndItemAffixTest.java)（100% 綠燈）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **SCHEMA-ITEM-DECOUPLING (模板與實例結構解耦)**
  - Schema 分離 Item Template（靜態範本與 `affixPool`）與 Item Instance（具體持有態與 `affixes` 清單）。
- [x] **SCHEMA-SLOT-OPTIONAL (非裝備槽位鬆綁)**
  - 任務道具、消耗品等 `itemType` 允許 `slot` 為選填或 null。
- [x] **SCHEMA-TRACKER-CHIP (加密追蹤晶片資料建立)**
  - 正式建立 `item_chip_qianxing_tracker` 定義，具備 `speed` 與 `poiseBreakPower` 屬性。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task ITEM-SCHEMA-1 (VS-01 掉落驗收): 首領掉落與隨機詞綴產出實測**
  - **目標**：驗證戰鬥掉落時，`DrpgRewardService` 產出之裝備 100% 吻合 `ITEM_SCHEMA` 實例規範。
- [ ] **Task ITEM-SCHEMA-2 (LENSES-SCHEMA-VALIDATION): 物品多階靈識認知透鏡資料契約驗證**
  - **目標**：驗證 `items.json` 與各類別裝備/素材 JSON 中 `lenses` 陣列（`minProgress`, `name`, `desc`）結構契約，確保資料合法性。
  - **驗收標準**：`DataIntegrityValidator` 啟動期檢驗 `lenses` 格式正確，無階級進度重疊或逆序破窗。
