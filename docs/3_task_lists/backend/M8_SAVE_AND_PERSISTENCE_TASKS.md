# 💾 M8: 存檔與持久化管線模組實作進度清單 (Save & Persistence Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/backend/M8_SAVE_AND_PERSISTENCE.md`](../../2_specifications/backend/M8_SAVE_AND_PERSISTENCE.md)  
> **核心職責**：管理小隊狀態存讀檔 (schemaVersion 3)、異步批量落盤 (Write-Behind)、讀寫鎖生命週期原子關閉與無競態保障。  
> **當前完成度**：90% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心服務與模型**：
  - [`SaveGameService.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/save/service/SaveGameService.java)（存讀檔核心入口）
  - [`SaveData.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/save/model/SaveData.java)（存檔根物件，封裝小隊、行囊與進度）
  - [`AbstractAsyncBatchPersistenceService.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/infra/persistence/service/AbstractAsyncBatchPersistenceService.java)（`ReentrantReadWriteLock` 異步落盤引擎）
- **存檔存放路徑**：
  - `saves/slot_*.json`
- **關聯驗證測試**：
  - [`AsyncPersistenceLifecycleTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/AsyncPersistenceLifecycleTest.java)（高併發關閉無滯留落盤 100% 綠燈）
  - [`UnifiedCharacterCoreTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/UnifiedCharacterCoreTest.java)（存檔僅存小隊狀態 schemaVersion 3 100% 綠燈）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **M8-SCHEMA-V3 (純單機小隊存檔)**
  - 存檔徹底拔除 `personalInventory` 雙寫假帳，只存 40 格小隊公共行囊與成員真實裝備。
- [x] **M8-ASYNC-RWLOCK (停機生命週期原子化)**
  - 導入 `ReentrantReadWriteLock`，停機時以 WriteLock 原子化切斷接收管道，杜絕關閉排空後的競態漏盤；提供明確 `SubmitResult` 與背壓度量。
- [x] **M8-AUTO-SAVE-TRIGGER (關鍵節點自動存檔)**
  - 進入營地休整、切換地牢樓層、首領戰鬥勝利結算後觸發自動存檔。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task M8-1 (MULTI-SLOT-MANAGEMENT): 前端多存檔槽位管理 (Slot 1~5 + AutoSave)**
  - **目標**：支援玩家手動建立、覆蓋、刪除 5 個手動獨立槽位（`slot_1` ~ `slot_5`）與自動存檔（`slot_auto`），前端存檔面板呈現各槽位角色修為與遊戲時長。
  - **驗收標準**：多存檔切換正常，存檔損毀具備 `.bak` 自動備份回滾機制。
- [ ] **Task M8-2 (IN-DUNGEON-SAVE-LOCK-AND-WIPE): 地牢手動存檔限制與滅團自動存檔實作**
  - **目標**：依據 L2 規格，地牢非撤離點狀態下（`inDungeon = true`）鎖定手動存檔；當全隊覆滅回營時，扣除暫存戰利品並強制觸發原子性自動存檔至 `slot_auto.json`。
  - **驗收標準**：單元測試驗證地牢中存檔阻斷、撤離點存檔成功、滅團強制存檔保護。
