# 💾 M8: 存檔與持久化管線模組實作進度清單 (Save & Persistence Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/backend/M8_SAVE_AND_PERSISTENCE.md`](../../2_specifications/backend/M8_SAVE_AND_PERSISTENCE.md)  
> **核心職責**：管理小隊狀態存讀檔 (schemaVersion 3)、異步批量落盤 (Write-Behind)、讀寫鎖生命週期原子關閉與無競態保障。  
> **當前完成度**：95% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心服務與模型**：
  - [`SaveGameService.java`](../../../src/main/java/com/example/htmlmud/save/SaveGameService.java)（存讀檔核心入口）
  - [`SaveData.java`](../../../src/main/java/com/example/htmlmud/save/SaveData.java)（存檔根物件，封裝小隊、行囊與進度）
  - [`AbstractAsyncBatchPersistenceService.java`](../../../src/main/java/com/example/htmlmud/infra/persistence/service/AbstractAsyncBatchPersistenceService.java)（`ReentrantReadWriteLock` 異步落盤引擎）
- **存檔存放路徑**：
  - `saves/slot_*.json`、`saves/autosave.json`
- **關聯驗證測試**：
  - [`AsyncPersistenceLifecycleTest.java`](../../../src/test/java/com/example/htmlmud/AsyncPersistenceLifecycleTest.java)（高併發關閉無滯留落盤 100% 綠燈）
  - [`UnifiedCharacterCoreTest.java`](../../../src/test/java/com/example/htmlmud/UnifiedCharacterCoreTest.java)（存檔僅存小隊狀態 schemaVersion 3 100% 綠燈）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **M8-PKG-ENCAPSULATION (Package-by-Feature 獨立封裝 / Phase 3 Step R3.4)**
  - 建立一級套件 `com.example.htmlmud.save`，將 `SaveGameService`、`SaveData`、`SaveSlotDto`、`SaveMigration`、`SaveMigrationPipeline`、`SaveCorruptedException` 統一收斂至獨立套件（完成時歷史基準 416 項 `@Test` 綠燈，實時狀態以 `.\test.ps1` 為準）。
- [x] **M8-SCHEMA-V3 (純單機小隊存檔)**
  - 存檔徹底拔除 `personalInventory` 雙寫假帳，只存 40 格小隊公共行囊與成員真實裝備。
- [x] **M8-ASYNC-RWLOCK (停機生命週期原子化)**
  - 導入 `ReentrantReadWriteLock`，停機時以 WriteLock 原子化切斷接收管道，杜絕關閉排空後的競態漏盤；提供明確 `SubmitResult` 與背壓度量。
- [x] **M8-AUTO-SAVE-TRIGGER (關鍵節點自動存檔)**
  - 進入營地休整、切換地牢樓層、首領戰鬥勝利結算後觸發自動存檔（`autosave.json`）。
- [x] **Task M8-2 (IN-DUNGEON-SAVE-LOCK-AND-WIPE): 地牢手動存檔限制與滅團自動存檔實作**
  - 依據 L2 規格，地牢非撤離點狀態下（`inDungeon = true`）鎖定手動存檔；當全隊覆滅回營時，保留行囊內所有戰利品與資產，全隊 -20 SAN、回復 30% HP，並強制觸發原子性自動存檔至 `autosave.json`。
  - 單元測試驗證地牢中存檔阻斷、撤離點存檔成功、滅團強制存檔保護 100% 綠燈通過。
- [x] **Task M8-1 (MULTI-SLOT-MANAGEMENT): 多存檔槽位管理 (Slot 1~5 + AutoSave) 與遷移恢復**
  - 後端 `SaveGameService` 支援多槽位（`slot_1` ~ `slot_5` 與 `autosave.json`）寫入、讀取與 `save restore` 壞檔備份還原；`SaveMigrationPipeline` 支援 `v0 -> v1 -> v2 -> v3` 平滑遷移；前端 `save-modal.js` 完整渲染各槽位狀態與覆蓋防誤觸二次確認。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task M8-3 (PLAYTIME-TRACKING-AND-ROADMAP)【待深入討論／長線演進藍圖】：`playtimeSeconds` 遊玩時數真實累計與 L2 §六長線持久化演進**
  - **目標**：
    1. 補齊 `SaveGameService.saveGame(...)` 對 `SaveData.playtimeSeconds` 的真實會話遊玩秒數計算與跨讀檔累加（目前 `SaveData` 雖定義欄位且 `V0ToV1Migration` 預設補 `0`，但 `saveGame` 寫入時尚未累加實際遊玩秒數）。
    2. 承接 [`M8_SAVE_AND_PERSISTENCE.md`](../../2_specifications/backend/M8_SAVE_AND_PERSISTENCE.md) §六之長線演進規劃（太陰萬劫·鐵人一命模式、存檔匯出協議、Zstandard 串流壓縮）。
  - **驗收標準**：存讀檔後 `playtimeSeconds` 精確反映累計遊玩時間，單元測試綠燈。
