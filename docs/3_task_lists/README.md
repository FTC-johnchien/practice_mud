# 📊 模組實作進度總覽看板 (Module Implementation Dashboard)

> **文檔層級**：Level 3 — Task Lists (實作進度與待辦管理中樞)  
> **體系定位**：**1:1 鏡像對齊 Level 2 模組架構手冊**。任何 Level 2 規格變更，皆在此處對應的模組清單中紀錄進度與追加待辦；Level 4 看板微任務均由本清單抽取。  
> **全局測試基準**：全專案 376+ 項單元與整合測試 100% 綠燈守護（`.\test.ps1`）。

> [!IMPORTANT]
> **三大層級遞進治理與使用者審批鐵律 (Mandatory Cascading Governance & User Approval Rules)**：
> 1. **`1_requirements` (L1) 為最主要規範**：當 L1 內容發生任何異動時，必須檢視現有 L2 文件是否涵蓋 L1 異動後的內容。**若有需要進行新增或修改，AI 必須提供建議並由使用者決定是否新增/修改 L2 文件。**
> 2. **`2_specifications` (L2) 為 L1 內容的完整模組規劃**：當 L2 內容發生任何異動時，必須檢視現有 L3 文件是否涵蓋 L2 異動後的內容。**若有需要進行新增或修改，AI 必須提供建議並由使用者決定是否新增/修改 L3 文件。**
> 3. **`3_task_lists` (L3) 為 L2 規格的完整實作項目清單**。

---

## 🏛️ 全模組 1:1 實作進度對照表 (Module Traceability Matrix)

### 🔹 一、後端核心領域模組 (Backend Modules M1 ~ M9)

| 模組代碼 | 模組名稱 | Level 2 規格手冊 | Level 3 實作進度清單 | 完成度 | 關鍵測試保護 |
| :---: | :--- | :--- | :--- | :---: | :--- |
| **M1** | 營地機能設施中樞 | [`M1_TOWN_HUB_AND_FACILITIES.md`](../2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md) | [**`M1_TOWN_HUB_TASKS.md`**](backend/M1_TOWN_HUB_TASKS.md) | 80% 🟢 | `WorldDataIntegrityTest` |
| **M2** | DRPG 網格地牢探索 | [`M2_DRPG_DUNGEON.md`](../2_specifications/backend/M2_DRPG_DUNGEON.md) | [**`M2_DRPG_DUNGEON_TASKS.md`**](backend/M2_DRPG_DUNGEON_TASKS.md) | 90% 🟢 | `DungeonTorchAndTrapTest` |
| **M3** | 戰鬥引擎與規則結算 | [`M3_COMBAT_ENGINE.md`](../2_specifications/backend/M3_COMBAT_ENGINE.md) | [**`M3_COMBAT_ENGINE_TASKS.md`**](backend/M3_COMBAT_ENGINE_TASKS.md) | 95% 🟢 | `BossPhaseAndPoiseStaggerTest`<br>`CrossComboAndFormationResonanceTest` |
| **M3.1** | 狀態效果與 Buff 引擎 | [`M3_1_BUFF_AND_STATUS_EFFECTS.md`](../2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md) | [**`M3_1_BUFF_AND_STATUS_TASKS.md`**](backend/M3_1_BUFF_AND_STATUS_TASKS.md) | 95% 🟢 | `ElementalReactionAndDetonateTest` |
| **M4** | 冒險小隊與古仙陣法 | [`M4_PARTY_AND_FORMATION.md`](../2_specifications/backend/M4_PARTY_AND_FORMATION.md) | [**`M4_PARTY_AND_FORMATION_TASKS.md`**](backend/M4_PARTY_AND_FORMATION_TASKS.md) | 95% 🟢 | `CompanionGambitEngineTest`<br>`PartyInventoryAndMadnessTest` (行囊與精神防護) |
| **M5** | 技能體系與修為突破 | [`M5_SKILL_AND_PROGRESSION.md`](../2_specifications/backend/M5_SKILL_AND_PROGRESSION.md) | [**`M5_SKILL_AND_PROGRESSION_TASKS.md`**](backend/M5_SKILL_AND_PROGRESSION_TASKS.md) | 90% 🟢 | `CyberSkillAndItemAffixTest`<br>`XpProgressionServiceTest` |
| **M6** | 物品裝備與經濟體系 | [`M6_ITEM_AND_ECONOMY.md`](../2_specifications/backend/M6_ITEM_AND_ECONOMY.md) | [**`M6_ITEM_AND_ECONOMY_TASKS.md`**](backend/M6_ITEM_AND_ECONOMY_TASKS.md) | 90% 🟢 | `UnifiedPartyEconomyTest` |
| **M7** | 資料範本與看門狗 | [`M7_DATA_DRIVEN_TEMPLATES.md`](../2_specifications/backend/M7_DATA_DRIVEN_TEMPLATES.md) | [**`M7_DATA_DRIVEN_TEMPLATES_TASKS.md`**](backend/M7_DATA_DRIVEN_TEMPLATES_TASKS.md) | 95% 🟢 | `ArchitectureGuardrailsTest` |
| **M8** | 存檔與持久化管線 | [`M8_SAVE_AND_PERSISTENCE.md`](../2_specifications/backend/M8_SAVE_AND_PERSISTENCE.md) | [**`M8_SAVE_AND_PERSISTENCE_TASKS.md`**](backend/M8_SAVE_AND_PERSISTENCE_TASKS.md) | 90% 🟢 | `AsyncPersistenceLifecycleTest` |
| **M9** | 狀態管線與事件協定 | [`M9_STATE_PIPELINE_AND_EVENTS.md`](../2_specifications/backend/M9_STATE_PIPELINE_AND_EVENTS.md) | [**`M9_STATE_PIPELINE_TASKS.md`**](backend/M9_STATE_PIPELINE_TASKS.md) | 90% 🟢 | `SessionTakeoverAndSingletonProtagonistTest` |

---

### 🎨 二、前端原生表現模組 (Frontend Vanilla Modules FE1 ~ FE6)

| 模組代碼 | 模組名稱 | Level 2 規格手冊 | Level 3 實作進度清單 | 完成度 | 核心 JavaScript 真相源 |
| :---: | :--- | :--- | :--- | :---: | :--- |
| **FE1** | 核心狀態與管線 | [`FE1_CORE_STATE_AND_PIPELINE.md`](../2_specifications/frontend/FE1_CORE_STATE_AND_PIPELINE.md) | [**`FE1_CORE_STATE_TASKS.md`**](frontend/FE1_CORE_STATE_TASKS.md) | 90% 🟢 | `app.js`, `store.js`, `websocket.js` |
| **FE2** | DRPG 視口與雷達 | [`FE2_DRPG_VIEWPORT_RENDERER.md`](../2_specifications/frontend/FE2_DRPG_VIEWPORT_RENDERER.md) | [**`FE2_DRPG_VIEWPORT_TASKS.md`**](frontend/FE2_DRPG_VIEWPORT_TASKS.md) | 90% 🟢 | `dungeon-panel.js` |
| **FE3** | 戰鬥擂台與特效 | [`FE3_BATTLE_ARENA_AND_FX.md`](../2_specifications/frontend/FE3_BATTLE_ARENA_AND_FX.md) | [**`FE3_BATTLE_ARENA_TASKS.md`**](frontend/FE3_BATTLE_ARENA_TASKS.md) | 95% 🟢 | `battle-panel.js`, `battle-fx-director.js`, `floating-text.js` |
| **FE4** | 營地設施與動作列 | [`FE4_TOWN_HUB_AND_ACTION_BAR.md`](../2_specifications/frontend/FE4_TOWN_HUB_AND_ACTION_BAR.md) | [**`FE4_TOWN_HUB_TASKS.md`**](frontend/FE4_TOWN_HUB_TASKS.md) | 85% 🟢 | `town-panel.js`, `action-bar-panel.js` |
| **FE5** | 小隊裝備與彈窗 | [`FE5_PARTY_EQUIPMENT_AND_MODALS.md`](../2_specifications/frontend/FE5_PARTY_EQUIPMENT_AND_MODALS.md) | [**`FE5_PARTY_EQUIPMENT_TASKS.md`**](frontend/FE5_PARTY_EQUIPMENT_TASKS.md) | 90% 🟢 | `party-modal.js`, `save-modal.js`, `victory-modal.js` |
| **FE6** | 七層樣式串流 | [`FE6_CSS_TOKENS_AND_THEMES.md`](../2_specifications/frontend/FE6_CSS_TOKENS_AND_THEMES.md) | [**`FE6_CSS_TOKENS_TASKS.md`**](frontend/FE6_CSS_TOKENS_TASKS.md) | 95% 🟢 | `style-01` ~ `style-07.css` (巨石已除役) |

---

### 📜 三、資料架構契約模組 (Data Schemas)

| 契約代碼 | 契約名稱 | Level 2 規格手冊 | Level 3 實作進度清單 | 完成度 | 核心靜態資料真值源 |
| :---: | :--- | :--- | :--- | :---: | :--- |
| **DUNGEON** | 地牢資料架構 | [`DUNGEON_SCHEMA.md`](../2_specifications/data_schemas/DUNGEON_SCHEMA.md) | [**`DUNGEON_SCHEMA_TASKS.md`**](data_schemas/DUNGEON_SCHEMA_TASKS.md) | 95% 🟢 | `data/dungeons/*.json` |
| **ITEM** | 裝備與詞綴架構 | [`ITEM_SCHEMA.md`](../2_specifications/data_schemas/ITEM_SCHEMA.md) | [**`ITEM_SCHEMA_TASKS.md`**](data_schemas/ITEM_SCHEMA_TASKS.md) | 95% 🟢 | `data/global/items/`, `quest_items.json` |
| **SKILL** | 賽博戰技架構 | [`SKILL_SCHEMA.md`](../2_specifications/data_schemas/SKILL_SCHEMA.md) | [**`SKILL_SCHEMA_TASKS.md`**](data_schemas/SKILL_SCHEMA_TASKS.md) | 95% 🟢 | `data/party/party_skills.json` |

---

## 🧭 四、全域演進中樞與歷史封存庫指針

- **代碼模組化收斂與重構清單**：👉 [**`CODE_CONVERGENCE_AND_REFACTORING_TASKS.md`**](CODE_CONVERGENCE_AND_REFACTORING_TASKS.md)（對齊 Level 2 模組之漸進式收斂與安全重構工程專案）
- **宏觀演進總覽**：👉 [`FUTURE_IMPROVEMENTS.md`](FUTURE_IMPROVEMENTS.md)（跨領域優先序矩陣與長程路線圖）
- **當前衝刺看板**：👉 [`../4_active_tasks/ACTIVE_TASKS.md`](../4_active_tasks/ACTIVE_TASKS.md)（唯一微觀焦點，當前執行：Milestone VS-01）
- **歷史審查與除役歸檔**：👉 [`../0_archive/alignment_history/`](../0_archive/alignment_history/)（四層架構大對齊、心魔背刺除役等已完成任務庫）
