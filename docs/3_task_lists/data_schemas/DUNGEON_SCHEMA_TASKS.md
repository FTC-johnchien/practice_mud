# 🗺️ DUNGEON_SCHEMA: 地牢資料架構實作進度清單 (Dungeon Schema Tasks)

> **文檔層級**：Level 3 — Task Lists (資料契約進度清單)  
> **對應規格**：👉 [`docs/2_specifications/data_schemas/DUNGEON_SCHEMA.md`](../../2_specifications/data_schemas/DUNGEON_SCHEMA.md)  
> **核心職責**：驗證 10x10 地牢網格 JSON 結構、圖例 Legend、暗雷遇敵率與事件點配置之有效性。  
> **當前完成度**：95% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心資料檔案**：
  - `src/main/resources/data/dungeons/mozhu_mines_b1f.json`
  - `src/main/resources/data/dungeons/taiyin_tomb_b1f.json`
- **契約驗證載入器**：
  - [`DungeonFloorLoader.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/dungeon/DungeonFloorLoader.java)
- **關聯驗證測試**：
  - [`WorldDataIntegrityTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/WorldDataIntegrityTest.java)（100% 綠燈）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **SCHEMA-DUNGEON-ALIGN (地牢契約與載入器對齊)**
  - 補齊別名相容映射（`floorId` $\leftrightarrow$ `id`、`startCoord` $\leftrightarrow$ `startPosition`、`encounters.dangerRate` $\leftrightarrow$ `encounterRate`）。
- [x] **SCHEMA-DUNGEON-LEGEND (圖例枚舉擴充)**
  - Schema 支援 `WALL`, `FLOOR`, `DOOR`, `TREASURE`, `CHEST`, `EVENT`, `TRAP`, `STAIRS_UP`, `STAIRS_DOWN`, `ELITE`, `BOSS`。
- [x] **SCHEMA-DUNGEON-EXIT (撤離營地出口契約)**
  - 規範 `exitRoomId` 語意為撤離返回之營地設施，實體資料庫精準對應。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task DUNGEON-SCHEMA-1 (VS-01 地理重塑): 墨竹山礦坑地質文本深度化**
  - **目標**：在 `mozhu_mines_b1f.json` 中深化外神卵塊、硬化偽地殼與腐朽枕木坑道描寫，通過 `DataIntegrityValidator` 驗證。
