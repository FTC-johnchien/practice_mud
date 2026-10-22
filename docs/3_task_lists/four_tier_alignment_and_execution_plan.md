# 🎯 四層級文件穿透性對齊與純單機 DRPG 典範治理執行清單
# (Four-Tier Alignment & Execution Plan)

> **文檔層級**：Level 3 — Task Lists (演進路線與專案治理清單)  
> **制定日期**：2026-10-22  
> **核心目標**：基於四層級（Requirements / Specifications / Task Lists / Active Tasks）深度審查結論，全面推行**「方案 A」**，建立純單機 Hack-and-slash DRPG 100% 資料契約閉環，徹底清剿所有歷史殘存之 MUD 概念。

---

## 📌 一、審查結論與治理依據 (Audit Summary)

經過對全專案現行 32 份活躍規格文件的穿透性檢驗，確診以下關鍵落差並啟動閉環治理：
1. **垂直傳遞性斷層 (Traceability Gap)**：Level 1 定義之賽博修仙四大流派與晶片超頻，在 Level 2 缺乏正式的 Schema 契約支持。
2. **範疇認知矛盾 (Scope Conflict)**：`data_schemas/` 與 `M7` 殘留歷史多人 MUD 檔案（`game-config-sample.json`、`rooms.json`），與單機選單營地架構衝突。
3. **無效索引清除 (Broken Reference Cleanup)**：`docs/README.md` 曾存在未實裝的虛構指針。

---

## 📋 二、詳細可執行任務清單 (Actionable Task List)

### 🔹 Phase 1: 規格層資料架構實裝與 MUD 殘留除役 (Data-Driven Schemas & Purge)

- [x] **Task 1.1 (LEGACY-PURGE): 物理清理 `data_schemas/` 中的歷史多人 MUD 檔案**
  - **執行動作**：將 `docs/2_specifications/data_schemas/` 內殘留之 `game-config-sample.json`（含 JavaMUD / motd / startRoomId）、`game-config-architecture.txt`、`skill-template.json` 全數物理刪除（歷史版本已完好備份於 `docs/0_archive/templates/`）。
  - **交付產出**：消除規範層與多人 MUD 的歷史關聯。

- [x] **Task 1.2 (DATA-SCHEMAS-INIT): 正式建立純單機 DRPG 三大核心資料架構契約**
  - **執行動作**：在 `docs/2_specifications/data_schemas/` 正式建立 3 份純單機 DRPG 規格手冊（每份精準控制在 80~100 行，嚴格遵循 < 300 行規範）：
    - [x] [`DUNGEON_SCHEMA.md`](../2_specifications/data_schemas/DUNGEON_SCHEMA.md)：定義 10x10 地牢網格、外神卵鞘地貌 Legend、暗雷遇敵率與事件點。
    - [x] [`ITEM_SCHEMA.md`](../2_specifications/data_schemas/ITEM_SCHEMA.md)：定義 5+2 裝備部位、七階稀有度色彩、隨機詞綴池（Prefix / Suffix）。
    - [x] [`SKILL_SCHEMA.md`](../2_specifications/data_schemas/SKILL_SCHEMA.md)：對齊 Level 1 賽博修仙四大流派（黑客/生化/機械/靈能）、超頻階級、削韌破防、Primer/Detonator。
  - **交付產出**：補齊 Level 1 $\to$ Level 2 賽博特工戰技與裝備詞綴的正式架構契約。

- [x] **Task 1.3 (M7-ALIGN): 校準 `M7_DATA_DRIVEN_TEMPLATES.md` 範例檔案樹**
  - **執行動作**：將 `backend/M7_DATA_DRIVEN_TEMPLATES.md` 範例樹中過時的 `newbie_village/rooms.json` 替換為 `facilities.json`。
  - **交付產出**：徹底消除 M7 模組手冊與 M1 營地選單中樞手冊之間的微小矛盾。

---

### 🔹 Phase 2: 演進清單與微任務看板量化對齊 (Roadmap & Active Task Alignment)

- [ ] **Task 2.1 (TOWN-FACILITY-ROADMAP): 補齊營地機能設施中長程演進規劃**
  - **執行動作**：在 [`content_and_combat_roadmap.md`](content_and_combat_roadmap.md) 中為 M1 規劃的五大營地機能設施（裝備工坊分解重鑄、特工晶片實驗室、道場天賦加點、醫療站基因強殖）排定階段性演進節奏。
  - **驗收標準**：營地設施之擴充藍圖與五大核心目標（角色養成、裝備收集）完全契合。

- [ ] **Task 2.2 (VS-01-DOD-QUANTIFY): Milestone VS-01 驗收標準精準量化**
  - **執行動作**：在 [`ACTIVE_TASKS.md`](../4_active_tasks/ACTIVE_TASKS.md) 中將 Milestone VS-01（第一章序幕垂直切片）的驗收標準與全新 Schema 深度綁定，明確標記增量測試驗收守護（守護既有 373 項測試全綠）。
  - **驗收標準**：Task 1 ~ Task 4 具備明確的資料範本與斷言依據。

---

### 🔹 Phase 3: 全域清查與專案品質守護 (Continuous Inspection)

- [ ] **Task 3.1 (CUSTOM-INSPECTION): 使用者指示之特定領域深入清查**
  - **執行動作**：依據使用者後續指定之清查項目（如特定代碼模組、設定資料完整性、或特定前後端對齊），執行嚴謹審查並擬定處置方案。
  - **驗收標準**：全域無死角對齊純單機 Hack-and-slash DRPG 專案第一紅線。

---

## 🛡️ 三、完成定義 (Definition of Done, DoD)

1. **零 MUD 殘留**：`docs/` 現行生效目錄中絕不出現任何房間位移、文字命令行或多人會話術語。
2. **契約先行 (Spec-Driven)**：所有資料結構必有 Schema 規格對應，實作代碼嚴禁憑空發明 JSON 欄位。
3. **綠燈守護 (Zero Regression)**：任何調整均不影響全量測試通過率（維持 373+ 測試 100% 綠燈）。
4. **極致輕量 (Strict Conciseness)**：除 CHANGELOG 與 history 外，所有文件嚴格壓制在 300 行以內。
