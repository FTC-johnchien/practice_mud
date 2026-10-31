# 📚 《深淵仙途》四層級工程文檔導覽體系 (Engineering Documentation Map)

> **核心定位**：  
> 本專案為**「純單機第一人稱地牢探索與爽快砍殺刷寶 RPG (Single-Player Hack-and-Slash DRPG)」**。  
> 堅決貫徹 **85% 爽快砍殺刷寶與戰術構築 vs 15% 非侵入式背景世界觀** 之黃金權重。

---

## ⚖️ 三大層級遞進治理與使用者審批鐵律 (Mandatory Cascading Governance & User Approval Rules)

> [!IMPORTANT]
> **全專案必須無條件嚴格遵守的三大層級遞進與審批原則：**
> 1. **`1_requirements` (L1) 為最主要規範**：當 L1 內容發生任何異動時，必須檢視現有 L2 文件是否涵蓋 L1 異動後的內容。**若有需要進行新增或修改，AI 必須提供建議並由使用者決定是否新增/修改 L2 文件。**
> 2. **`2_specifications` (L2) 為 L1 內容的完整模組規劃**：當 L2 內容發生任何異動時，必須檢視現有 L3 文件是否涵蓋 L2 異動後的內容。**若有需要進行新增或修改，AI 必須提供建議並由使用者決定是否新增/修改 L3 文件。**
> 3. **`3_task_lists` (L3) 為 L2 規格的完整實作項目清單**：記錄所有模組之落實現況、待辦與驗收標準。

---

## 🏛️ 文檔四層級結構總覽

```text
docs/
├── 1_requirements/              # Level 1: 需求與願景層 (Why & What)
│   ├── WORLD_AND_LORE_BIBLE.md   # 世界觀聖經：森羅星卵鞘、工程師、賽博巨企、擬態修仙
│   ├── CYBERPUNK_CULTIVATION_SPEC.md # 賽博特工能力與克蘇魯修真技能映射需求規格
│   ├── UI_UX_REQUIREMENTS.md     # 畫面佈局：PC優先/手機自適應、敘事文字視窗、走格與戰鬥
│   └── GAMEPLAY_CORE_LOOPS.md    # 核心循環：營地中樞選單/地牢走格遠征閉環、道基五維體系
│
├── 2_specifications/            # Level 2: 模組架構與技術規格層 (How & Architecture)
│   ├── ARCHITECTURE_SUMMARY.md   # 頂層技術架構總覽 (Java 25 虛擬執行緒、Spring Boot)
│   ├── ARCHITECTURAL_DECISIONS.md # 既定架構裁決與外部 AI 審查豁免基準
│   ├── MODULES_MAP.md            # 全功能模組拼圖與拓撲邊界 (後端9+1、前端6)
│   ├── backend/                  # 後端 9+1 大業務模組技術規格手冊 (M1 ~ M9)
│   │   ├── M1_TOWN_HUB_AND_FACILITIES.md
│   │   ├── M2_DRPG_DUNGEON.md
│   │   ├── M3_COMBAT_ENGINE.md
│   │   ├── M3_1_BUFF_AND_STATUS_EFFECTS.md
│   │   ├── M4_PARTY_AND_FORMATION.md
│   │   ├── M5_SKILL_AND_PROGRESSION.md
│   │   ├── M6_ITEM_AND_ECONOMY.md
│   │   ├── M7_DATA_DRIVEN_TEMPLATES.md
│   │   ├── M8_SAVE_AND_PERSISTENCE.md
│   │   └── M9_STATE_PIPELINE_AND_EVENTS.md
│   ├── frontend/                 # 前端 6 大表現模組技術規格手冊 (FE1 ~ FE6)
│   │   ├── FE1_CORE_STATE_AND_PIPELINE.md
│   │   ├── FE2_DRPG_VIEWPORT_RENDERER.md
│   │   ├── FE3_BATTLE_ARENA_AND_FX.md
│   │   ├── FE4_TOWN_HUB_AND_ACTION_BAR.md
│   │   ├── FE5_PARTY_EQUIPMENT_AND_MODALS.md
│   │   ├── FE6_CSS_TOKENS_AND_THEMES.md
│   │   ├── FRONTEND_LAYOUT_GUIDE.md
│   │   └── UI_PAGE_STRUCTURE.md
│   ├── skills/                   # 四大淵源 17 專精技能樹與合體技規格
│   │   ├── README.md
│   │   ├── SYNERGY_AND_COMBOS.md
│   │   ├── 1_cultivator/         # 修真者 (TREE_01 ~ TREE_05)
│   │   ├── 2_engineer/           # 造物主/工程師 (TREE_06 ~ TREE_09)
│   │   ├── 3_high_tech/          # 賽博巨企 (TREE_10 ~ TREE_13)
│   │   └── 4_cthulhu/            # 克蘇魯外神 (TREE_14 ~ TREE_17)
│   └── data_schemas/             # 資料驅動拓撲與 JSON Schema 模板
│       ├── DUNGEON_SCHEMA.md
│       ├── ITEM_SCHEMA.md
│       └── SKILL_SCHEMA.md
│
├── 3_task_lists/                # Level 3: 模組實作清單與路線圖層 (1:1 鏡像對齊 Level 2)
│   ├── README.md                # 📊 全模組 1:1 實作進度儀表板 (進度中樞與完成度總表)
│   ├── FUTURE_IMPROVEMENTS.md   # 宏觀演進戰略總表 (優先序矩陣與演進路徑)
│   ├── CODE_CONVERGENCE_AND_REFACTORING_TASKS.md # 跨模組代碼收斂與重構任務清單
│   ├── backend/                 # 後端 10 大模組實作與驗收清單 (M1~M9, M3.1)
│   ├── frontend/                # 前端 6 大表現模組實作與驗收清單 (FE1~FE6)
│   └── data_schemas/            # 資料架構契約實作與驗收清單 (Dungeon, Item, Skill)
│
├── 4_active_tasks/              # Level 4: 當前焦點與微任務看板層 (Current Iteration & Sprint)
│   ├── ACTIVE_TASKS.md           # 當前單一焦點微任務看板 (每次對話 1 Micro-Task)
│   └── SPRINT_GOAL.md            # 當前衝刺目標對齊聲明 (防偏離校準錨點)
│
├── 0_references/                # 參考素材與外部審查建議庫 (Raw References & Non-normative Reviews)
│   ├── Claude_Review.md          # Claude 審查建議與執行步驟拆解
│   ├── Codex_Review.md           # Codex 審查建議報告
│   ├── Copilot_Review.md         # Copilot 審查建議報告
│   └── 背景.txt / 畫面佈局.txt... # 原始小說世界觀與企劃素材
│
└── 0_archive/                   # 封存歷史參考資料 (Historical References & Legacy Manuals)
    ├── alignment_history/        # 歷史四層級深度對齊審查清單封存庫
    └── future_improvements_history.md # 歷史已完成落地之架構改善庫 (Phase 0~25)
```

---

## 📖 各層級閱讀與維護指引

### 🔹 Level 1: Requirements (需求與願景層)
- **定位**：定義遊戲「為什麼做」與「做成什麼樣」。
- **維護原則**：任何新功能或數值機制，必須先在需求層找到世界觀與體驗支撐，嚴禁無源之水式的機制堆砌。**遵循單一真相源 (SSoT)，嚴禁在此層出現具體程式 ID、JSON 欄位名與戰鬥平衡數值**。
- **核心文件**：
  - [世界觀聖經 (WORLD_AND_LORE_BIBLE.md)](./1_requirements/WORLD_AND_LORE_BIBLE.md)
  - [賽博修仙技能規格 (CYBERPUNK_CULTIVATION_SPEC.md)](./1_requirements/CYBERPUNK_CULTIVATION_SPEC.md)
  - [UI/UX 佈局需求 (UI_UX_REQUIREMENTS.md)](./1_requirements/UI_UX_REQUIREMENTS.md)
  - [核心遊戲循環 (GAMEPLAY_CORE_LOOPS.md)](./1_requirements/GAMEPLAY_CORE_LOOPS.md)

### 🔹 Level 2: Module Specifications (模組架構與規格層)
- **定位**：定義各模組「如何設計、邊界在哪、契約為何」。
- **維護原則**：變更 API、DTO、狀態機或欄位時，必須無條件同步對應手冊；遵循 100% 資料驅動、純函數式結算與表現層分離。**Schema 範例必須能通過真實反序列化驗證，嚴禁與代碼脫節**。
- **核心文件**：
  - [全模組地圖 (MODULES_MAP.md)](./2_specifications/MODULES_MAP.md)
  - [架構總覽 (ARCHITECTURE_SUMMARY.md)](./2_specifications/ARCHITECTURE_SUMMARY.md)
  - [架構裁決與審查豁免基準 (ARCHITECTURAL_DECISIONS.md)](./2_specifications/ARCHITECTURAL_DECISIONS.md)
  - [後端手冊庫 (backend/)](./2_specifications/backend/)
  - [前端手冊庫 (frontend/)](./2_specifications/frontend/)
  - [專精技能樹與合體技 (skills/)](./2_specifications/skills/)
  - [資料契約庫 (data_schemas/)](./2_specifications/data_schemas/)

### 🔹 Level 3: Task Lists (模組實作清單與路線圖層)
- **定位**：記錄 Level 2 所有架構模組的 1:1 完整實作進度 (`[x]` 既有代碼與測試驗證、`[ ]` 待辦項目與驗收標準) 與宏觀演進路線。
- **鏡像對齊規範**：本目錄嚴格與 `docs/2_specifications/` 保持 1:1 模組鏡像結構。當 L1 需求變更時同步更新 L2 規格；當 L2 模組規格異動時，L3 對應模組檔案必須同步更新其驗收項目。
- **核心文件與模組目錄**：
  - [📊 全模組實作進度儀表板 (README.md)](./3_task_lists/README.md)（全專案 19 大模組進度看板與完成度矩陣）
  - [🧭 宏觀演進戰略總表 (FUTURE_IMPROVEMENTS.md)](./3_task_lists/FUTURE_IMPROVEMENTS.md)（演進路徑與優先序總表）
  - [🔧 代碼收斂與重構清單 (CODE_CONVERGENCE_AND_REFACTORING_TASKS.md)](./3_task_lists/CODE_CONVERGENCE_AND_REFACTORING_TASKS.md)（跨模組代碼收斂與重構任務清單）
  - [🏛️ 後端模組任務清單 (backend/)](./3_task_lists/backend/)（10 大後端模組 M1~M9、M3.1 實作清單）
  - [🎨 前端模組任務清單 (frontend/)](./3_task_lists/frontend/)（6 大前端模組 FE1~FE6 實作清單）
  - [📜 資料契約任務清單 (data_schemas/)](./3_task_lists/data_schemas/)（3 大 Schema 契約實作清單）
- **歷史與封存指針**：
  - [四層級深度對齊審查清單封存庫 (alignment_history/)](./0_archive/alignment_history/)（歷史七大審查與治理清單）
  - [已完成改善歷史庫 (future_improvements_history.md)](./0_archive/future_improvements_history.md)（Phase 0~25 落地歸檔）
  - [專案版本交付記錄 (CHANGELOG.md)](../CHANGELOG.md)（專案根目錄標準規範）

### 🔹 Level 4: Active Tasks (當前微任務與看板層)
- **定位**：微觀敏捷執行層，遵循單一焦點原則，每次對話僅聚焦一個 Micro-Task。
- **維護原則**：**任務卡嚴禁自創或重複抄寫數值！僅能做 Canonical ID 指針引用；嚴格落實查證優先 (Evidence-First)，禁止在實體代碼/資料尚未吻合前提前打勾。**
- **完成定義 (DoD)**：代碼實作 $\to$ 測試 100% 通過 $\to$ 文檔全域同步 $\to$ 繁體中文標準 $\to$ 防漂移自檢。
- **核心文件**：
  - [當前微任務看板 (ACTIVE_TASKS.md)](./4_active_tasks/ACTIVE_TASKS.md)（唯一權威看板，杜絕多處複製漂移）
  - [當前衝刺對齊目標 (SPRINT_GOAL.md)](./4_active_tasks/SPRINT_GOAL.md)

### 🔹 Level 0: References & Archive (素材、審查與封存層)
- **定位**：儲備原始小說設定、畫面草稿、外部 AI 審查建議與歷史架構封存庫。
- **維護原則**：本層文件**不具備直接架構約束力**。外部審查建議（如 `Claude_Review.md`、`Copilot_Review.md`）僅作為參考，未經正式裁決並移入 L2/L3 前不得當作已核准規格。
- **核心目錄**：
  - `docs/0_references/`：原始世界觀小說、企劃稿件、外部 AI 審查建議報告。
  - `docs/0_archive/`：歷史除役之舊模組手冊與舊待辦庫。

---

## 🛠️ 文件健康度與自動化檢驗工具 (Documentation Tooling)

為防止文檔隨迭代演進產生斷鏈或路徑失效，專案提供輕量自動化掃描腳本：
- **執行命令**：`powershell -ExecutionPolicy Bypass -File .\scripts\check-doc-links.ps1`
- **檢驗內容**：自動遍歷 `docs/`（排除 `0_archive/`）下所有 Markdown 內部檔案連結與 `file:///` 絕對路徑，確保目標檔案 100% 存在於檔案系統中。
- **治理紅線**：任何文件重構或搬移完成後，必須保證此腳本回傳 Exit Code 0（PASS）。
