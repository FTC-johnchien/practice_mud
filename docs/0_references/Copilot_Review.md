# 文件對齊審查報告

> **檢視日期：** 2026-10-23  
> **檢視範圍：** `docs/1_requirements/`、`docs/2_specifications/`、`docs/3_task_lists/`  
> **審查基準：** 需求文件是唯一上游規範；規格須完整承接需求；任務清單須承接規格並可追蹤進度。  
> **檢視方式：** 靜態核對文件內容、模組對照、任務涵蓋及文件連結；未檢查程式碼，也未重新執行測試。

## 結論

**目前僅部分對齊，尚未形成可靠的端到端追溯。** 主要後端 9 個模組、前端 FE1–FE6 與 3 份資料 Schema 均有對應任務清單，未發現這些主模組缺少任務檔；但對照表連結多處失效，部分需求沒有明確連到規格模組，且 M1 任務清單未追蹤規格中的全部主要交付內容。另外，去除 MUD／魔化的需求方向與部分規格及進度文件中的舊語意尚未完全收斂。

## 發現

### 1. 高：去除 MUD 的需求與規格中的 MUD 架構描述互相衝突

- **需求依據：** [GAMEPLAY_CORE_LOOPS.md](../1_requirements/GAMEPLAY_CORE_LOOPS.md) 第 5、97–98 行明確要求純單機 DRPG、零 MUD 指令並除役 MUD 機制；[UI_UX_REQUIREMENTS.md](../1_requirements/UI_UX_REQUIREMENTS.md) 第 5 行也要求除役 MUD 指令輸入及文字房間移動。
- **規格現況：** [ARCHITECTURE_SUMMARY.md](../2_specifications/ARCHITECTURE_SUMMARY.md) 第 21 行仍列出 `MudWebSocketHandler`；[M3_1_BUFF_AND_STATUS_EFFECTS.md](../2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md) 第 35 行仍以「MUD 生靈」描述 Actor。
- **影響：** 實作者無法判斷這些是仍有效的架構要求，還是尚未清理的歷史內容。
- **建議：** 依需求決定是否移除相關描述；若只是歷史背景，明確標示為已除役／非現行設計，避免被當成實作要求。

### 2. 高：Level 3 儀表板的規格連結使用不存在的檔名

[3_task_lists/README.md](../3_task_lists/README.md) 第 15–19 行的後端規格連結指向舊檔名，與實際檔案不符，包括：

| 儀表板目前指向 | 實際規格檔案 |
|---|---|
| `M1_TOWN_HUB.md` | [M1_TOWN_HUB_AND_FACILITIES.md](../2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md) |
| `M3_1_BUFF_STATUS.md` | [M3_1_BUFF_AND_STATUS_EFFECTS.md](../2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md) |
| `M4_PARTY_FORMATION.md` | [M4_PARTY_AND_FORMATION.md](../2_specifications/backend/M4_PARTY_AND_FORMATION.md) |
| `M5_SKILL_PROGRESS.md` | [M5_SKILL_AND_PROGRESSION.md](../2_specifications/backend/M5_SKILL_AND_PROGRESSION.md) |
| `M6_ITEM_ECONOMY.md` | [M6_ITEM_AND_ECONOMY.md](../2_specifications/backend/M6_ITEM_AND_ECONOMY.md) |
| `M7_DATA_TEMPLATES.md` | [M7_DATA_DRIVEN_TEMPLATES.md](../2_specifications/backend/M7_DATA_DRIVEN_TEMPLATES.md) |
| `M8_SAVE_PERSIST.md` | [M8_SAVE_AND_PERSISTENCE.md](../2_specifications/backend/M8_SAVE_AND_PERSISTENCE.md) |
| `M9_STATE_PIPELINE.md` | [M9_STATE_PIPELINE_AND_EVENTS.md](../2_specifications/backend/M9_STATE_PIPELINE_AND_EVENTS.md) |

同一區段的 M2 連結 `../2_specifications/backend/M2_DRPG_DUNGEON.md` 與現行檔案名稱相符，不列為失效連結。

**影響：** 儀表板作為模組追蹤入口時，無法可靠地導向正確規格；即使任務檔存在，也會中斷規格到任務的查找路徑。應逐項改為實際路徑，並驗證所有相對連結。

### 3. 中：除核心玩法外，需求文件缺少明確的需求至模組追溯

[GAMEPLAY_CORE_LOOPS.md](../1_requirements/GAMEPLAY_CORE_LOOPS.md) 第 11–18 行有「涉及系統與模組」對照；但 [CYBERPUNK_CULTIVATION_SPEC.md](../1_requirements/CYBERPUNK_CULTIVATION_SPEC.md)、[WORLD_AND_LORE_BIBLE.md](../1_requirements/WORLD_AND_LORE_BIBLE.md) 與 [UI_UX_REQUIREMENTS.md](../1_requirements/UI_UX_REQUIREMENTS.md) 未提供相應的模組追溯表。[MODULES_MAP.md](../2_specifications/MODULES_MAP.md) 有模組總覽，但沒有逐項把這些需求連到負責規格模組。

**影響：** 目前無法只靠文件確認技能、世界觀及 UI/UX 的每項主要要求都已分配給規格模組；這是可追溯性缺口，不等同於已證明相關功能完全沒有規格。

**建議：** 在需求文件或共用追溯矩陣中，為每項主要需求列出負責的規格模組及對應任務清單，並在需求變更時更新映射。

### 4. 中：M1 任務清單沒有覆蓋規格中的全部主要交付內容

[M1_TOWN_HUB_AND_FACILITIES.md](../2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md) 第 19–39、61–80、82–132 行包含五項設施、NPC 對話樹、`TownHubDto`、設施互動及對話系統等內容；但 [M1_TOWN_HUB_TASKS.md](../3_task_lists/backend/M1_TOWN_HUB_TASKS.md) 的已完成與待辦項目未明確拆列 NPC 對話及設施互動流程。

**影響：** 這些規格交付物沒有可供追蹤的完成狀態與驗收項目，無法確認是否已實作或仍待處理。

**建議：** 依 M1 規格補列對話資料／流程、設施互動及其驗收工作；若某項已完成，補上可核對的完成依據。

### 5. 中：前端視圖拆分與任務看板的模組拆分未建立對照

[UI_PAGE_STRUCTURE.md](../2_specifications/frontend/UI_PAGE_STRUCTURE.md) 的「7 大核心視圖模組職責矩陣」列出七種視圖；[3_task_lists/README.md](../3_task_lists/README.md) 的前端看板則只列 FE1–FE6。

**影響：** 目前沒有明確映射可判定七種視圖如何分配到六個 FE 模組，也無法辨識是否有視圖責任未被任務清單承接。這不直接證明少了一份任務檔，但模組分解的追溯關係不完整。

**建議：** 在 `UI_PAGE_STRUCTURE.md` 或任務 README 增加七個視圖至 FE1–FE6 的對照，並標明一個 FE 模組涵蓋多個視圖的情況。

### 6. 低：除役／歷史語意仍混在現行任務與測試索引中

[3_task_lists/README.md](../3_task_lists/README.md) 第 19 行仍以 `PartyInventoryAndMadnessTest` 作為關鍵測試名稱；[M3_COMBAT_ENGINE_TASKS.md](../3_task_lists/backend/M3_COMBAT_ENGINE_TASKS.md) 第 39–40 行則保留 `M3-PURGE-BETRAYAL` 除役工作。核心需求 [GAMEPLAY_CORE_LOOPS.md](../1_requirements/GAMEPLAY_CORE_LOOPS.md) 第 97–98 行已要求取消魔化機制。

**影響：** 這些名稱可能是舊測試或除役工作的歷史紀錄，不足以證明現行玩法仍有魔化／背刺；但文件未清楚區分歷史項目與現行驗收，容易造成誤讀。

**建議：** 保留必要歷史紀錄時加上「已除役／歷史」標記，並確認現行測試索引描述的是需求仍要求的行為，而非已取消的機制。

## 已確認的對齊部分

- 9 個後端模組、FE1–FE6 六個前端模組，以及 DUNGEON、ITEM、SKILL 三份 Schema，皆可找到對應的 Level 3 任務清單。
- [ARCHITECTURE_SUMMARY.md](../2_specifications/ARCHITECTURE_SUMMARY.md)、[MODULES_MAP.md](../2_specifications/MODULES_MAP.md)、[FRONTEND_LAYOUT_GUIDE.md](../2_specifications/frontend/FRONTEND_LAYOUT_GUIDE.md) 屬架構／導覽類文件；未發現它們各自構成缺少任務檔的獨立交付模組。

## 建議修正順序

1. 先釐清並統一 MUD／魔化等與 canonical requirements 衝突的現行語意。
2. 修正 [3_task_lists/README.md](../3_task_lists/README.md) 的失效連結，並驗證整份文件的相對連結。
3. 建立需求 → 規格模組 → 任務清單的追溯矩陣，補上 M1 目前未追蹤的交付內容。
4. 對齊七種 UI 視圖與 FE1–FE6 的責任分配，並清楚標註除役項目的歷史狀態。
