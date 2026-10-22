# 📚 《道祖是克蘇魯》四層級工程文檔導覽體系 (Engineering Documentation Map)

> **架構定位**：  
> 本專案嚴格遵循現代軟體工程與高沉浸 RPG 開發規範，將全域技術與業務文檔劃分為 **4 大遞進層級 (Requirements $\to$ Module Specifications $\to$ Task Lists $\to$ Active Tasks)**。  
> 全專案的所有系統設計與代碼實作，無條件服務於 **五大核心目標**：
> 1. **世界觀的介紹 (World & Lore)**：克蘇魯卵鞘、工程師採掘遺產、賽博巨企掠奪、特工晶片 × 異形強殖基因。
> 2. **戰鬥爽快 (Impactful Combat)**：半即時 ATB、罡氣削韌 (Poise)、反制打斷 (Interrupt)、滿屏跳字與強烈打擊感。
> 3. **技能組合 (Skill Synergy)**：跨隊員連攜合擊、元素反應與 DoT 引爆、Gambit 自動戰術鏈。
> 4. **裝備收集 (Loot Progression)**：妖獸煞石、工程師零件、巨企晶片，豐富隨機詞綴與注靈強化。
> 5. **角色養成 (Character Progression)**：道基五維加點、境界突破、特工晶片加載度、強殖基因覺醒。

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
│   └── data_schemas/             # 資料驅動拓撲與 JSON Schema 模板
│       ├── DUNGEON_SCHEMA.md
│       ├── ITEM_SCHEMA.md
│       └── SKILL_SCHEMA.md
│
├── 3_task_lists/                # Level 3: 全局任務清單與演進路線層 (Roadmap & Backlog)
│   ├── FUTURE_IMPROVEMENTS.md    # 宏觀演進戰略總表 (~150行，導覽中樞與優先序總表)
│   ├── content_and_combat_roadmap.md # 戰鬥爽快、視覺呈現與五大目標內容拓展藍圖
│   ├── tech_debt_and_hardening.md # 底層技術債治理、硬編碼清剿與系統加固藍圖
│   ├── four_tier_alignment_and_execution_plan.md # 四層級穿透性對齊與純單機典範治理清單
│   ├── codebase_four_tier_alignment_plan.md # 程式碼與四層級文檔全域對齊執行清單
│   ├── codebase_audit_and_alignment_tasks.md # 程式碼與文件一致性全域審查待辦清單
│   └── decommission_audit_and_remediation_tasks.md # 未竟除役項目實地查驗與代碼治理清單
│
├── 4_active_tasks/              # Level 4: 當前焦點與微任務看板層 (Current Iteration & Sprint)
│   ├── ACTIVE_TASKS.md           # 當前單一焦點微任務看板 (每次對話 1 Micro-Task)
│   └── SPRINT_GOAL.md            # 當前衝刺目標對齊聲明 (防偏離校準錨點)
│
└── 0_archive/                   # 封存歷史參考資料 (Historical References & Legacy Manuals)
    └── future_improvements_history.md # 歷史已完成落地之架構改善庫 (Phase 0~25)
```

---

## 📖 各層級閱讀與維護指引

### 🔹 Level 1: Requirements (需求與願景層)
- **定位**：定義遊戲「為什麼做」與「做成什麼樣」。
- **維護原則**：任何新功能或數值機制，必須先在需求層找到世界觀與體驗支撐，嚴禁無源之水式的機制堆砌。
- **核心文件**：
  - [世界觀聖經 (WORLD_AND_LORE_BIBLE.md)](./1_requirements/WORLD_AND_LORE_BIBLE.md)
  - [賽博修仙技能規格 (CYBERPUNK_CULTIVATION_SPEC.md)](./1_requirements/CYBERPUNK_CULTIVATION_SPEC.md)
  - [UI/UX 佈局需求 (UI_UX_REQUIREMENTS.md)](./1_requirements/UI_UX_REQUIREMENTS.md)
  - [核心遊戲循環 (GAMEPLAY_CORE_LOOPS.md)](./1_requirements/GAMEPLAY_CORE_LOOPS.md)

### 🔹 Level 2: Module Specifications (模組架構與規格層)
- **定位**：定義各模組「如何設計、邊界在哪、契約為何」。
- **維護原則**：變更 API、DTO、狀態機或欄位時，必須無條件同步對應手冊；遵循 100% 資料驅動、純函數式結算與表現層分離。
- **核心文件**：
  - [全模組地圖 (MODULES_MAP.md)](./2_specifications/MODULES_MAP.md)
  - [架構總覽 (ARCHITECTURE_SUMMARY.md)](./2_specifications/ARCHITECTURE_SUMMARY.md)
  - [後端手冊庫 (backend/)](./2_specifications/backend/)
  - [前端手冊庫 (frontend/)](./2_specifications/frontend/)

### 🔹 Level 3: Task Lists (全局待辦與路線圖層)
- **定位**：記錄宏觀 Roadmap、演進戰線、候選功能儲備。
- **核心文件**：
  - [全局演進戰略總表 (FUTURE_IMPROVEMENTS.md)](./3_task_lists/FUTURE_IMPROVEMENTS.md)（~140行，導覽中樞）
  - [戰鬥爽快與內容藍圖 (content_and_combat_roadmap.md)](./3_task_lists/content_and_combat_roadmap.md)（去文字化、五大核心目標中長程拓展）
  - [底層技術債與加固藍圖 (tech_debt_and_hardening.md)](./3_task_lists/tech_debt_and_hardening.md)（硬編碼清剿、測試加速、護欄加固）
  - [四層級對齊與治理清單 (four_tier_alignment_and_execution_plan.md)](./3_task_lists/four_tier_alignment_and_execution_plan.md)（資料架構契約、零 MUD 殘留）
  - [代碼全域對齊執行清單 (codebase_four_tier_alignment_plan.md)](./3_task_lists/codebase_four_tier_alignment_plan.md)（戰鬥/技能/裝備/資料/前端落地）
  - [全域審查待辦清單 (codebase_audit_and_alignment_tasks.md)](./3_task_lists/codebase_audit_and_alignment_tasks.md)（營地中樞服務化/路徑校準/存檔與協定）
  - [未竟除役查驗治理清單 (decommission_audit_and_remediation_tasks.md)](./3_task_lists/decommission_audit_and_remediation_tasks.md)（背刺魔化/指令框代碼清剿）
- **歷史與封存指針**：
  - [專案版本交付記錄 (CHANGELOG.md)](../CHANGELOG.md)（專案根目錄標準規範）
  - [已完成改善歷史庫 (future_improvements_history.md)](./0_archive/future_improvements_history.md)（Phase 0~25 落地歸檔）

### 🔹 Level 4: Active Tasks (當前微任務與看板層)
- **定位**：微觀敏捷執行層，遵循單一焦點原則，每次對話僅聚焦一個 Micro-Task。
- **完成定義 (DoD)**：代碼實作 $\to$ 測試 100% 通過 $\to$ 文檔全域同步 $\to$ 繁體中文標準。
- **核心文件**：
  - [當前微任務看板 (ACTIVE_TASKS.md)](./4_active_tasks/ACTIVE_TASKS.md)（唯一權威看板，杜絕多處複製漂移）
  - [當前衝刺對齊目標 (SPRINT_GOAL.md)](./4_active_tasks/SPRINT_GOAL.md)
