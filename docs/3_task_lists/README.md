# 📊 模組實作進度總覽看板 (Module Implementation Dashboard)

> **文檔層級**：Level 3 — Task Lists (實作進度與待辦管理中樞)  
> **體系定位**：**核心模組 1:1 鏡像、跨模組總綱與 17 專精技能樹採聚合映射**。任何 Level 2 規格變更，皆在此處對應的模組清單中紀錄進度與追加待辦；Level 4 看板微任務均由本清單抽取。  
> **全局測試基準**：全專案 395 項單元與整合測試（2026-10-29 驗證基準，實時狀態以執行 `.\test.ps1` 為準）100% 綠燈守護。

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
| **M1** | 營地機能設施中樞 | [`M1_TOWN_HUB_AND_FACILITIES.md`](../2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md) | [**`M1_TOWN_HUB_TASKS.md`**](backend/M1_TOWN_HUB_TASKS.md) | 85% 🟢 | `TownHubServiceTest`<br>`CognitiveProxyServiceTest` |
| **M2** | DRPG 網格地牢探索 | [`M2_DRPG_DUNGEON.md`](../2_specifications/backend/M2_DRPG_DUNGEON.md) | [**`M2_DRPG_DUNGEON_TASKS.md`**](backend/M2_DRPG_DUNGEON_TASKS.md) | 95% 🟢 | `DungeonDefeatSettlementTest`<br>`DungeonTorchAndTrapTest` |
| **M3** | 戰鬥引擎與規則結算 | [`M3_COMBAT_ENGINE.md`](../2_specifications/backend/M3_COMBAT_ENGINE.md) | [**`M3_COMBAT_ENGINE_TASKS.md`**](backend/M3_COMBAT_ENGINE_TASKS.md) | 95% 🟢 | `BossPhaseAndPoiseStaggerTest`<br>`CrossComboAndFormationResonanceTest` |
| **M3.1** | 狀態效果與 Buff 引擎 | [`M3_1_BUFF_AND_STATUS_EFFECTS.md`](../2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md) | [**`M3_1_BUFF_AND_STATUS_TASKS.md`**](backend/M3_1_BUFF_AND_STATUS_TASKS.md) | 95% 🟢 | `ElementalReactionAndDetonateTest` |
| **M4** | 冒險小隊與古仙陣法 | [`M4_PARTY_AND_FORMATION.md`](../2_specifications/backend/M4_PARTY_AND_FORMATION.md) | [**`M4_PARTY_AND_FORMATION_TASKS.md`**](backend/M4_PARTY_AND_FORMATION_TASKS.md) | 95% 🟢 | `CompanionGambitEngineTest`<br>`PartyInventoryAndMadnessTest` (行囊與精神防護) |
| **M5** | 技能體系與修為突破 | [`M5_SKILL_AND_PROGRESSION.md`](../2_specifications/backend/M5_SKILL_AND_PROGRESSION.md) | [**`M5_SKILL_AND_PROGRESSION_TASKS.md`**](backend/M5_SKILL_AND_PROGRESSION_TASKS.md) | 85% 🟢 | `CyberSkillAndItemAffixTest`<br>`XpProgressionServiceTest` |
| **M6** | 物品裝備與經濟體系 | [`M6_ITEM_AND_ECONOMY.md`](../2_specifications/backend/M6_ITEM_AND_ECONOMY.md) | [**`M6_ITEM_AND_ECONOMY_TASKS.md`**](backend/M6_ITEM_AND_ECONOMY_TASKS.md) | 95% 🟢 | `UnifiedPartyEconomyTest`<br>`UnifiedPartyEconomyServiceTest` |
| **M7** | 資料範本與看門狗 | [`M7_DATA_DRIVEN_TEMPLATES.md`](../2_specifications/backend/M7_DATA_DRIVEN_TEMPLATES.md) | [**`M7_DATA_DRIVEN_TEMPLATES_TASKS.md`**](backend/M7_DATA_DRIVEN_TEMPLATES_TASKS.md) | 100% 🟢 | `ArchitectureGuardrailsTest`<br>`WorldDataIntegrityTest` |
| **M8** | 存檔與持久化管線 | [`M8_SAVE_AND_PERSISTENCE.md`](../2_specifications/backend/M8_SAVE_AND_PERSISTENCE.md) | [**`M8_SAVE_AND_PERSISTENCE_TASKS.md`**](backend/M8_SAVE_AND_PERSISTENCE_TASKS.md) | 95% 🟢 | `AsyncPersistenceLifecycleTest` |
| **M9** | 狀態管線與事件協定 | [`M9_STATE_PIPELINE_AND_EVENTS.md`](../2_specifications/backend/M9_STATE_PIPELINE_AND_EVENTS.md) | [**`M9_STATE_PIPELINE_TASKS.md`**](backend/M9_STATE_PIPELINE_TASKS.md) | 90% 🟢 | `SessionTakeoverAndSingletonProtagonistTest` |

---

### 🎨 二、前端原生表現模組 (Frontend Vanilla Modules FE1 ~ FE6 & Guides)

| 模組代碼 | 模組名稱 | Level 2 規格手冊 | Level 3 實作進度清單 | 完成度 | 核心真實代碼 / 樣式真值源 |
| :---: | :--- | :--- | :--- | :---: | :--- |
| **FE1** | 核心狀態與管線 | [`FE1_CORE_STATE_AND_PIPELINE.md`](../2_specifications/frontend/FE1_CORE_STATE_AND_PIPELINE.md) | [**`FE1_CORE_STATE_TASKS.md`**](frontend/FE1_CORE_STATE_TASKS.md) | 90% 🟢 | `app.js`, `core/state-store.js`, `websocket.js` |
| **FE2** | DRPG 視口與雷達 | [`FE2_DRPG_VIEWPORT_RENDERER.md`](../2_specifications/frontend/FE2_DRPG_VIEWPORT_RENDERER.md) | [**`FE2_DRPG_VIEWPORT_TASKS.md`**](frontend/FE2_DRPG_VIEWPORT_TASKS.md) | 90% 🟢 | `panels/dungeon-panel.js` |
| **FE3** | 戰鬥擂台與特效 | [`FE3_BATTLE_ARENA_AND_FX.md`](../2_specifications/frontend/FE3_BATTLE_ARENA_AND_FX.md) | [**`FE3_BATTLE_ARENA_TASKS.md`**](frontend/FE3_BATTLE_ARENA_TASKS.md) | 95% 🟢 | `panels/battle-panel.js`, `fx/battle-fx-director.js`, `fx/floating-text.js` |
| **FE4** | 營地設施與動作列 | [`FE4_TOWN_HUB_AND_ACTION_BAR.md`](../2_specifications/frontend/FE4_TOWN_HUB_AND_ACTION_BAR.md) | [**`FE4_TOWN_HUB_TASKS.md`**](frontend/FE4_TOWN_HUB_TASKS.md) | 85% 🟢 | `panels/town-panel.js`, `index.html`（舊動作列面板檔【歷史／已除役】） |
| **FE5** | 小隊裝備與彈窗 | [`FE5_PARTY_EQUIPMENT_AND_MODALS.md`](../2_specifications/frontend/FE5_PARTY_EQUIPMENT_AND_MODALS.md) | [**`FE5_PARTY_EQUIPMENT_TASKS.md`**](frontend/FE5_PARTY_EQUIPMENT_TASKS.md) | 90% 🟢 | `modals/party-modal.js`, `modals/save-modal.js`, `modals/victory-modal.js` |
| **FE6** | 七層樣式串流 | [`FE6_CSS_TOKENS_AND_THEMES.md`](../2_specifications/frontend/FE6_CSS_TOKENS_AND_THEMES.md) | [**`FE6_CSS_TOKENS_TASKS.md`**](frontend/FE6_CSS_TOKENS_TASKS.md) | 95% 🟢 | `style-01-tokens-and-shell.css` ~ `style-07-battle-fx.css` |
| **GUIDE** | 前端佈局與視圖切換全景手冊 | [`FRONTEND_LAYOUT_GUIDE.md`](../2_specifications/frontend/FRONTEND_LAYOUT_GUIDE.md) | [**`FE1_CORE_STATE_TASKS.md`**](frontend/FE1_CORE_STATE_TASKS.md)<br>[**`FE4_TOWN_HUB_TASKS.md`**](frontend/FE4_TOWN_HUB_TASKS.md) | 95% 🟢 | `index.html`, `app.js` (左右雙主區佈局與換邊機制) |
| **PAGE** | 頁面結構與視圖規範手冊 | [`UI_PAGE_STRUCTURE.md`](../2_specifications/frontend/UI_PAGE_STRUCTURE.md) | [**`FE1_CORE_STATE_TASKS.md`**](frontend/FE1_CORE_STATE_TASKS.md) | 95% 🟢 | `core/constants.js`, `core/ui-utils.js` |

---

### 📜 三、資料架構契約與 17 專精技能樹體系 (Data Schemas & Skill Trees)

| 契約代碼 | 契約名稱 | Level 2 規格手冊 | Level 3 實作進度清單 | 完成度 | 核心靜態資料真值源 |
| :---: | :--- | :--- | :--- | :---: | :--- |
| **DUNGEON** | 地牢資料架構 | [`DUNGEON_SCHEMA.md`](../2_specifications/data_schemas/DUNGEON_SCHEMA.md) | [**`DUNGEON_SCHEMA_TASKS.md`**](data_schemas/DUNGEON_SCHEMA_TASKS.md) | 95% 🟢 | `data/dungeons/*.json` |
| **ITEM** | 裝備與詞綴架構 | [`ITEM_SCHEMA.md`](../2_specifications/data_schemas/ITEM_SCHEMA.md) | [**`ITEM_SCHEMA_TASKS.md`**](data_schemas/ITEM_SCHEMA_TASKS.md) | 95% 🟢 | `data/global/items/`, `quest_items.json` |
| **SKILL** | 賽博戰技架構 | [`SKILL_SCHEMA.md`](../2_specifications/data_schemas/SKILL_SCHEMA.md) | [**`SKILL_SCHEMA_TASKS.md`**](data_schemas/SKILL_SCHEMA_TASKS.md) | 90% 🟢 | `data/party/party_skills.json` |
| **SKILL_TREES** | 四大淵源 17 專精樹與合體技體系 | [`2_specifications/skills/`](../2_specifications/skills/README.md)（含 `SYNERGY_AND_COMBOS.md` 與 `TREE_01` ~ `TREE_17` 共 19 檔） | [**`SKILL_SCHEMA_TASKS.md`**](data_schemas/SKILL_SCHEMA_TASKS.md)<br>[**`M5_SKILL_AND_PROGRESSION_TASKS.md`**](backend/M5_SKILL_AND_PROGRESSION_TASKS.md) | 90% 🟢 | `party/party_skills.json`, `party/synergy_combos.json` |

---

## 🧭 四、Level 1 需求核心主題 → Level 2 模組規劃 → Level 3 實作清單 權威追溯矩陣 (End-to-End Requirements Traceability Matrix)

本矩陣確保 Level 1 四大最高規範之所有核心系統與需求，均具備具體的 Level 2 規格模組承接，並對應到 Level 3 實作任務與測試驗證：

| # | Level 1 需求核心主題 | 權威來源 (SSoT) | 承接之 Level 2 規格模組與章節 | 對應之 Level 3 實作任務清單 | 關鍵測試與防禦依據 |
|---|---|---|---|---|---|
| **R1** | **世界觀序章與特工晶片加載**<br>（黑山羊卵鞘、千星特工、85/15非侵入敘事） | [`WORLD_AND_LORE_BIBLE.md`](../1_requirements/WORLD_AND_LORE_BIBLE.md)<br>§1~§5 | [`M1_TOWN_HUB_AND_FACILITIES.md`](../2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md) §7<br>[`FE4_TOWN_HUB_AND_ACTION_BAR.md`](../2_specifications/frontend/FE4_TOWN_HUB_AND_ACTION_BAR.md) §2 | [`M1_TOWN_HUB_TASKS.md`](backend/M1_TOWN_HUB_TASKS.md) (Task M1-3, M1-4)<br>[`FE4_TOWN_HUB_TASKS.md`](frontend/FE4_TOWN_HUB_TASKS.md) (Task FE4-2) | `CognitiveProxyServiceTest` |
| **R2** | **純單機 DRPG 典範轉移**<br>（廢除 MUD 指令與房間移動，改純選單與按鈕） | [`GAMEPLAY_CORE_LOOPS.md`](../1_requirements/GAMEPLAY_CORE_LOOPS.md) §5<br>[`UI_UX_REQUIREMENTS.md`](../1_requirements/UI_UX_REQUIREMENTS.md) §1 | [`M1_TOWN_HUB_AND_FACILITIES.md`](../2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md) §1<br>[`M2_DRPG_DUNGEON.md`](../2_specifications/backend/M2_DRPG_DUNGEON.md) §1<br>[`FE1_CORE_STATE_AND_PIPELINE.md`](../2_specifications/frontend/FE1_CORE_STATE_AND_PIPELINE.md) §1 | [`M1_TOWN_HUB_TASKS.md`](backend/M1_TOWN_HUB_TASKS.md) (M1-BROADCAST-PURGE)<br>[`M2_DRPG_DUNGEON_TASKS.md`](backend/M2_DRPG_DUNGEON_TASKS.md) (M2-PURE-SP)<br>[`FE1_CORE_STATE_TASKS.md`](frontend/FE1_CORE_STATE_TASKS.md) | `ArchitectureGuardrailsTest`<br>`SessionTakeoverAndSingletonProtagonistTest` |
| **R3** | **安全營地六大機能設施與休整**<br>（百煉堂/藏經閣/演武場/外門集市/客棧/礦道） | [`GAMEPLAY_CORE_LOOPS.md`](../1_requirements/GAMEPLAY_CORE_LOOPS.md) §2<br>§5 (調息休整) | [`M1_TOWN_HUB_AND_FACILITIES.md`](../2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md) §2<br>[`FE4_TOWN_HUB_AND_ACTION_BAR.md`](../2_specifications/frontend/FE4_TOWN_HUB_AND_ACTION_BAR.md) §2 | [`M1_TOWN_HUB_TASKS.md`](backend/M1_TOWN_HUB_TASKS.md) (M1-CAMP-REST, Task M1-1, M1-2)<br>[`FE4_TOWN_HUB_TASKS.md`](frontend/FE4_TOWN_HUB_TASKS.md) (Task FE4-1, FE4-3) | `TownHubServiceTest`<br>`TownFacilityCommandTest` |
| **R4** | **第一人稱迷霧走格探索、火把與機關**<br>（3D透視視口、火把暗度暈影、陷阱寶箱撤離） | [`GAMEPLAY_CORE_LOOPS.md`](../1_requirements/GAMEPLAY_CORE_LOOPS.md) §3<br>[`UI_UX_REQUIREMENTS.md`](../1_requirements/UI_UX_REQUIREMENTS.md) §2, §4 | [`M2_DRPG_DUNGEON.md`](../2_specifications/backend/M2_DRPG_DUNGEON.md) §2, §3<br>[`FE2_DRPG_VIEWPORT_RENDERER.md`](../2_specifications/frontend/FE2_DRPG_VIEWPORT_RENDERER.md) §2<br>[`DUNGEON_SCHEMA.md`](../2_specifications/data_schemas/DUNGEON_SCHEMA.md) §2 | [`M2_DRPG_DUNGEON_TASKS.md`](backend/M2_DRPG_DUNGEON_TASKS.md)<br>[`FE2_DRPG_VIEWPORT_TASKS.md`](frontend/FE2_DRPG_VIEWPORT_TASKS.md)<br>[`DUNGEON_SCHEMA_TASKS.md`](data_schemas/DUNGEON_SCHEMA_TASKS.md) | `DungeonTorchAndTrapTest`<br>`MozhuMinesDungeonIntegrationTest` |
| **R5** | **半即時爽快戰鬥、削韌破防與連攜合擊**<br>（圓桌演算法、Poise Break、打斷、FCT） | [`GAMEPLAY_CORE_LOOPS.md`](../1_requirements/GAMEPLAY_CORE_LOOPS.md) §4<br>[`UI_UX_REQUIREMENTS.md`](../1_requirements/UI_UX_REQUIREMENTS.md) §4 | [`M3_COMBAT_ENGINE.md`](../2_specifications/backend/M3_COMBAT_ENGINE.md) §1~§4<br>[`M3_1_BUFF_AND_STATUS_EFFECTS.md`](../2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md) §1~§3<br>[`FE3_BATTLE_ARENA_AND_FX.md`](../2_specifications/frontend/FE3_BATTLE_ARENA_AND_FX.md) §1~§3 | [`M3_COMBAT_ENGINE_TASKS.md`](backend/M3_COMBAT_ENGINE_TASKS.md)<br>[`M3_1_BUFF_AND_STATUS_TASKS.md`](backend/M3_1_BUFF_AND_STATUS_TASKS.md)<br>[`FE3_BATTLE_ARENA_TASKS.md`](frontend/FE3_BATTLE_ARENA_TASKS.md) | `BossPhaseAndPoiseStaggerTest`<br>`CrossComboAndFormationResonanceTest` |
| **R6** | **四大淵源十七專精樹與合體技體系**<br>（修仙/工程/科技/外神，Tier 1~4 元嬰大絕招、法則反應引擎、16雙星、8三才、5領域、5人終結技） | [`CYBERPUNK_CULTIVATION_SPEC.md`](../1_requirements/CYBERPUNK_CULTIVATION_SPEC.md) §1~§4 | [`M5_SKILL_AND_PROGRESSION.md`](../2_specifications/backend/M5_SKILL_AND_PROGRESSION.md) §1~§5<br>[`SKILL_SCHEMA.md`](../2_specifications/data_schemas/SKILL_SCHEMA.md) §1~§5<br>[`2_specifications/skills/`](../2_specifications/skills/README.md) (19 檔) | [`M5_SKILL_AND_PROGRESSION_TASKS.md`](backend/M5_SKILL_AND_PROGRESSION_TASKS.md) (Task M5-4, M5-5, M5-6, M5-7)<br>[`SKILL_SCHEMA_TASKS.md`](data_schemas/SKILL_SCHEMA_TASKS.md) (Task SKILL-SCHEMA-3) | `CyberSkillAndItemAffixTest`<br>`WorldDataIntegrityTest` |
| **R7** | **5 人小隊名冊、四大道統門派夥伴招募與陣法 Gambit**<br>（正統道門/天工妖修/巨企仙朝/天道星靈四大來源、孔位加成、半即時 AI） | [`CYBERPUNK_CULTIVATION_SPEC.md`](../1_requirements/CYBERPUNK_CULTIVATION_SPEC.md) §1<br>[`GAMEPLAY_CORE_LOOPS.md`](../1_requirements/GAMEPLAY_CORE_LOOPS.md) §2, §4 | [`M4_PARTY_AND_FORMATION.md`](../2_specifications/backend/M4_PARTY_AND_FORMATION.md) §1~§3<br>[`FE5_PARTY_EQUIPMENT_AND_MODALS.md`](../2_specifications/frontend/FE5_PARTY_EQUIPMENT_AND_MODALS.md) §1 | [`M4_PARTY_AND_FORMATION_TASKS.md`](backend/M4_PARTY_AND_FORMATION_TASKS.md) (Task M4-2)<br>[`FE5_PARTY_EQUIPMENT_TASKS.md`](frontend/FE5_PARTY_EQUIPMENT_TASKS.md) | `CompanionGambitEngineTest`<br>`PartyFormationTest` |
| **R8** | **40 格小隊公共背包、裝備詞綴與刷寶**<br>（7 裝備槽位、隨機詞綴、多階稀有度） | [`GAMEPLAY_CORE_LOOPS.md`](../1_requirements/GAMEPLAY_CORE_LOOPS.md) §5<br>[`UI_UX_REQUIREMENTS.md`](../1_requirements/UI_UX_REQUIREMENTS.md) §1 | [`M6_ITEM_AND_ECONOMY.md`](../2_specifications/backend/M6_ITEM_AND_ECONOMY.md) §1~§4<br>[`ITEM_SCHEMA.md`](../2_specifications/data_schemas/ITEM_SCHEMA.md) §1~§3<br>[`FE5_PARTY_EQUIPMENT_AND_MODALS.md`](../2_specifications/frontend/FE5_PARTY_EQUIPMENT_AND_MODALS.md) §1 | [`M6_ITEM_AND_ECONOMY_TASKS.md`](backend/M6_ITEM_AND_ECONOMY_TASKS.md)<br>[`ITEM_SCHEMA_TASKS.md`](data_schemas/ITEM_SCHEMA_TASKS.md)<br>[`FE5_PARTY_EQUIPMENT_TASKS.md`](frontend/FE5_PARTY_EQUIPMENT_TASKS.md) | `UnifiedPartyEconomyTest`<br>`UnifiedPartyEconomyServiceTest` |
| **R9** | **SAN 精神護盾機制與【精神萎靡】輕度弱化**<br>（除役狂亂叛變，保留摸黑扣除與單一弱化） | [`GAMEPLAY_CORE_LOOPS.md`](../1_requirements/GAMEPLAY_CORE_LOOPS.md) §5 | [`M1_TOWN_HUB_AND_FACILITIES.md`](../2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md) §2<br>[`M2_DRPG_DUNGEON.md`](../2_specifications/backend/M2_DRPG_DUNGEON.md) §2<br>[`M3_1_BUFF_AND_STATUS_EFFECTS.md`](../2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md) §2 | [`M1_TOWN_HUB_TASKS.md`](backend/M1_TOWN_HUB_TASKS.md) (M1-CAMP-REST)<br>[`M2_DRPG_DUNGEON_TASKS.md`](backend/M2_DRPG_DUNGEON_TASKS.md)<br>[`FUTURE_IMPROVEMENTS.md`](FUTURE_IMPROVEMENTS.md) (COMBAT-DATA-DOC-01) | `PartyInventoryAndMadnessTest`<br>`TownFacilityCommandTest` |
| **R10** | **單機本機多槽位存檔持久化**<br>（schemaVersion 3、Slot 1~5 + AutoSave、地牢存檔安全鎖） | [`GAMEPLAY_CORE_LOOPS.md`](../1_requirements/GAMEPLAY_CORE_LOOPS.md) §5 | [`M8_SAVE_AND_PERSISTENCE.md`](../2_specifications/backend/M8_SAVE_AND_PERSISTENCE.md) §1~§3<br>[`FE5_PARTY_EQUIPMENT_AND_MODALS.md`](../2_specifications/frontend/FE5_PARTY_EQUIPMENT_AND_MODALS.md) §2 | [`M8_SAVE_AND_PERSISTENCE_TASKS.md`](backend/M8_SAVE_AND_PERSISTENCE_TASKS.md)<br>[`FE5_PARTY_EQUIPMENT_TASKS.md`](frontend/FE5_PARTY_EQUIPMENT_TASKS.md) | `SaveGameServiceTest`<br>`AsyncPersistenceLifecycleTest` |
| **R11** | **左右雙主區佈局（支援換邊）與文字日誌**<br>（PC-First、唯讀文字情境敘事日誌） | [`UI_UX_REQUIREMENTS.md`](../1_requirements/UI_UX_REQUIREMENTS.md) §1~§4 | [`FE1_CORE_STATE_AND_PIPELINE.md`](../2_specifications/frontend/FE1_CORE_STATE_AND_PIPELINE.md) §2<br>[`FRONTEND_LAYOUT_GUIDE.md`](../2_specifications/frontend/FRONTEND_LAYOUT_GUIDE.md) §1~§3<br>[`FE6_CSS_TOKENS_AND_THEMES.md`](../2_specifications/frontend/FE6_CSS_TOKENS_AND_THEMES.md) §1~§3 | [`FE1_CORE_STATE_TASKS.md`](frontend/FE1_CORE_STATE_TASKS.md)<br>[`FE6_CSS_TOKENS_TASKS.md`](frontend/FE6_CSS_TOKENS_TASKS.md) | 前端組件自適應驗證 |

---

## 🧭 五、全域演進中樞與歷史封存庫指針

- **全域架構裁決與審查豁免基準**：👉 [**`ARCHITECTURAL_DECISIONS.md`**](../2_specifications/ARCHITECTURAL_DECISIONS.md)（已拍板定案之核心設計裁決、治理基準與外部 AI 審查豁免清單）
- **文件與程式碼一致性對齊清單**：👉 [**`CODE_DOCS_ALIGNMENT_TASKS.md`**](CODE_DOCS_ALIGNMENT_TASKS.md)（對齊 Level 2 模組手冊、資料配置與現行代碼之專項治理任務庫）
- **代碼模組化收斂與重構清單**：👉 [**`CODE_CONVERGENCE_AND_REFACTORING_TASKS.md`**](CODE_CONVERGENCE_AND_REFACTORING_TASKS.md)（對齊 Level 2 模組之漸進式收斂與安全重構工程專案）
- **宏觀演進總覽**：👉 [`FUTURE_IMPROVEMENTS.md`](FUTURE_IMPROVEMENTS.md)（跨領域優先序矩陣與長程路線圖）
- **當前衝刺看板**：👉 [`../4_active_tasks/ACTIVE_TASKS.md`](../4_active_tasks/ACTIVE_TASKS.md)（唯一微觀焦點，當前執行：Milestone VS-01）
- **歷史審查與除役歸檔**：👉 [`../0_archive/alignment_history/`](../0_archive/alignment_history/)（四層架構大對齊、心魔背刺除役等已完成任務庫）
