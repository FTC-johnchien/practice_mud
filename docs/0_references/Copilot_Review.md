# 文件規劃與治理整體審閱

> **檢視日期：** 2026-10-30
> **檢視範圍：** `docs/` 下 73 份 Markdown；排除 `docs/0_archive/` 與 `docs/0_references/`。
> **目的：** 提出讓文件更清楚、可靠、有效率，並支援現代單機遊戲規劃與內容擴充的建議。
> **性質：** 非規範性審閱；本文件不會自行變更需求、規格、數值或任務狀態。

## 總結

文件已具備值得保留的基礎：需求、模組規格、實作清單與當前工作分層；遊戲定位明確採單機模組化架構；資料驅動、啟動校驗、存檔版本遷移與任務追溯也已有相應設計。〔[docs/README.md](../README.md#L19)、[M7_DATA_DRIVEN_TEMPLATES.md](../2_specifications/backend/M7_DATA_DRIVEN_TEMPLATES.md#L91)、[M8_SAVE_AND_PERSISTENCE.md](../2_specifications/backend/M8_SAVE_AND_PERSISTENCE.md#L45)、[3_task_lists/README.md](../3_task_lists/README.md#L64)〕

主要風險不是缺少更多規劃文件，而是現有文件之間有些契約、狀態與任務責任尚未收斂。建議先改善「規格能否互相對接、現在該做什麼、如何驗收」，再擴大內容量；避免以新增文件或流程取代修正既有權威來源。

## 優先建議

### P0｜收斂跨模組契約，並明確區分規格目標與實作現況

目前少數關鍵契約在不同文件使用不同名稱或表達方式：

- 物品資料以 `slot` 描述裝備位置，M6 與 FE5 則使用 `equipSlot`。〔[ITEM_SCHEMA.md](../2_specifications/data_schemas/ITEM_SCHEMA.md#L30)、[M6_ITEM_AND_ECONOMY.md](../2_specifications/backend/M6_ITEM_AND_ECONOMY.md#L29)、[FE5_PARTY_EQUIPMENT_AND_MODALS.md](../2_specifications/frontend/FE5_PARTY_EQUIPMENT_AND_MODALS.md#L53)〕
- FE5 的裝備動作名稱與 M9 所列動作契約不同（`EQUIP_ITEM`／`EQUIP`）。〔[FE5_PARTY_EQUIPMENT_AND_MODALS.md](../2_specifications/frontend/FE5_PARTY_EQUIPMENT_AND_MODALS.md#L53)、[M9_STATE_PIPELINE_AND_EVENTS.md](../2_specifications/backend/M9_STATE_PIPELINE_AND_EVENTS.md#L40)〕
- FE2 的操作示例仍以 `step w`、`turn a` 文字命令呈現，容易被誤讀為遊戲狀態仍依賴命令字串，而非結構化動作契約。〔[FE2_DRPG_VIEWPORT_RENDERER.md](../2_specifications/frontend/FE2_DRPG_VIEWPORT_RENDERER.md#L93)、[M9_STATE_PIPELINE_AND_EVENTS.md](../2_specifications/backend/M9_STATE_PIPELINE_AND_EVENTS.md#L104)〕
- M3 的規格目標與文件註記的現況／待辦尚未完全一致；應明確標示未完成項目，而不要讓讀者誤以為目標行為已落地。〔[M3_COMBAT_ENGINE.md](../2_specifications/backend/M3_COMBAT_ENGINE.md#L78)〕

**建議做法**

1. 為 JSON 欄位、前後端動作、事件與 DTO 欄位各指定唯一權威契約來源；跨文件以連結引用，不重複維護另一份相同清單。
2. 逐項決定採用的 canonical 名稱；若需保留舊名稱，列出相容別名、轉換位置與移除條件。
3. 統一文件狀態標籤，清楚區分「現行已驗證」、「規格已決議但未實作」、「候選提案」與「已除役」。
4. 讓既有資料載入／整合驗證涵蓋這些契約；文件的範例應能被實際解析或由測試核對。

**效益與成本：** 可降低前後端整合、資料遷移與實作歧義；需先盤點程式與 JSON 的實際契約，並以相容遷移避免一次改名造成舊資料失效。

### P1｜建立唯一工作入口，消除看板與狀態數字漂移

根目錄文件將 `ACTIVE_TASKS.md` 定義為唯一當前看板，但另有文件對齊專用的 active task 文件；規劃文件、看板及模組任務清單的責任邊界需要更明確。測試總數也曾以不同數字或不同表述並存，降低進度資訊可信度。〔[docs/README.md](../README.md#L126)、[DOCS_ALIGNMENT_MASTER_PLAN.md](../3_task_lists/DOCS_ALIGNMENT_MASTER_PLAN.md#L64)、[DOCS_ALIGNMENT_ACTIVE_TASKS.md](../4_active_tasks/DOCS_ALIGNMENT_ACTIVE_TASKS.md#L4)、[ACTIVE_TASKS.md](../4_active_tasks/ACTIVE_TASKS.md#L15)、[3_task_lists/README.md](../3_task_lists/README.md#L5)〕

**建議做法**

- 保留四層文件治理，但明定 L4 是「目前選定工作的入口」，不是第二份任務資料庫；治理工作與遊戲功能工作都經由同一入口切換，完成後將狀態與歷史移回對應來源。
- 模組任務清單維護任務的完整說明與驗收；L4 只引用任務 ID、當前焦點與阻塞，不複製整份工作內容。
- 測試數量若非必要，不在多份文件手動維護；改記錄可重現的測試命令與結果來源。若需保留里程碑數字，明確標成歷史快照並附日期。
- 為「進行中／已完成／封存」定義明確的轉換條件，避免待辦、提案與現況混在同一清單。

**效益與成本：** 降低重複更新與過期狀態，讓每次工作都能快速定位；初期需整理重複任務並確認各文件現有狀態。

### P1｜將任務拆成可獨立驗收的垂直增量，守住領域單一真相源

部分當前任務橫跨領域模型、Action、DTO、前端與測試，難以判定單一任務的完成條件。地牢與 Schema 任務也有相近工作重複列管的情況。另 M4 將小隊成員／小隊狀態描述為權威資料來源，任務規劃應避免重新引入角色狀態雙寫。〔[ACTIVE_TASKS.md](../4_active_tasks/ACTIVE_TASKS.md#L30)、[M4_PARTY_AND_FORMATION.md](../2_specifications/backend/M4_PARTY_AND_FORMATION.md#L88)、[M4_PARTY_AND_FORMATION_TASKS.md](../3_task_lists/backend/M4_PARTY_AND_FORMATION_TASKS.md#L41)、[M2_DRPG_DUNGEON_TASKS.md](../3_task_lists/backend/M2_DRPG_DUNGEON_TASKS.md#L49)、[DUNGEON_SCHEMA_TASKS.md](../3_task_lists/data_schemas/DUNGEON_SCHEMA_TASKS.md#L35)〕

**建議做法**

- 每項任務以一個可觀察的結果為核心，列出範圍、前置條件、驗收方式與負責的權威文件。
- 依實際依賴拆分資料／Schema、後端規則、前端呈現及端到端驗收；只有跨層功能確實需要時才保留一張垂直整合任務，並連結子項。
- 為相同交付物指定唯一任務來源，其他清單以任務 ID 引用，避免重複追蹤。
- 對主角、小隊、裝備與背包等狀態，先在任務驗收中寫清「唯一寫入來源」及「投影／同步方向」，再開發 UI 或 API。

**效益與成本：** 進度更容易驗收、回歸失敗也較容易定位；需要一次性拆卡，且跨層工作的完成狀態會分布在相依子項。

### P1｜以「可玩的章節閉環」定義垂直切片，而非功能清單

世界觀文件已描述序章與地牢經歷，但當前切片的任務重點偏向地圖、HUD、技能與掉落；開始探索、首領遭遇、撤離或失敗、回營結算與存檔等端到端完成條件，應在同一處明確串起來。〔[WORLD_AND_LORE_BIBLE.md](../1_requirements/WORLD_AND_LORE_BIBLE.md#L83)、[ACTIVE_TASKS.md](../4_active_tasks/ACTIVE_TASKS.md#L22)、[M1_TOWN_HUB_TASKS.md](../3_task_lists/backend/M1_TOWN_HUB_TASKS.md#L56)〕

規劃內容也需確認符合既有體驗取向：85/15 的遊戲體驗比例、Sprint 中較強的敘事文字安排，以及「不另設基因進度條」與新增基因槽構想，可能造成方向或範圍上的歧義。地點名稱與認知透鏡門檻的示例亦有差異。〔[GAMEPLAY_CORE_LOOPS.md](../1_requirements/GAMEPLAY_CORE_LOOPS.md#L107)、[SPRINT_GOAL.md](../4_active_tasks/SPRINT_GOAL.md#L20)、[WORLD_AND_LORE_BIBLE.md](../1_requirements/WORLD_AND_LORE_BIBLE.md#L118)、[DUNGEON_SCHEMA.md](../2_specifications/data_schemas/DUNGEON_SCHEMA.md#L97)、[M6_ITEM_AND_ECONOMY.md](../2_specifications/backend/M6_ITEM_AND_ECONOMY.md#L94)、[ITEM_SCHEMA.md](../2_specifications/data_schemas/ITEM_SCHEMA.md#L152)〕

**建議做法**

1. 為首個可玩切片定義一條玩家路徑：新遊戲／回營 → 整備 → 進入地牢 → 探索與戰鬥 → 獲得戰利品 → 撤離或失敗結算 → 回營 → 存檔與重新載入。
2. 為每個節點列出玩家可觀察的畫面、成功／失敗條件及至少一項端到端驗收；後台日誌不能取代必要的遊戲內視覺回饋。
3. 對體驗比例、基因槽、技能數量、地名與門檻等不一致事項，回到權威需求文件作決議；涉及 L1/L2 規格變更時依現有審批規則先取得使用者決定。
4. 把非必要敘事做成可忽略的環境或物品資訊，避免切片為了展示設定而阻塞核心遊玩循環。

**效益與成本：** 能更早驗證遊戲是否「真的可玩」，並及早發現系統整合與體驗問題；相較於單點功能清單，端到端切片短期需要較多協調和測試。

### P1｜統一資料 Schema 方言，補足跨欄位與引用資料驗證

M7 提及 Draft 2020-12，而部分 Schema 範例仍宣告 draft-07；地牢版面除了基本型別，還需要驗證矩陣尺寸、行長、符號圖例與引用 ID 等語意約束。不同驗證器或只依賴 JSON Schema 的結構檢查，可能留下無效遊戲內容。〔[M7_DATA_DRIVEN_TEMPLATES.md](../2_specifications/backend/M7_DATA_DRIVEN_TEMPLATES.md#L121)、[DUNGEON_SCHEMA.md](../2_specifications/data_schemas/DUNGEON_SCHEMA.md#L21)、[ITEM_SCHEMA.md](../2_specifications/data_schemas/ITEM_SCHEMA.md#L23)、[SKILL_SCHEMA.md](../2_specifications/data_schemas/SKILL_SCHEMA.md#L33)〕

**建議做法**

- 統一 Schema draft 版本與實際使用的驗證器，並確認既有 JSON 均可通過。
- 將可由 Schema 表達的型別、必填欄位與範圍檢查，和需讀取多筆資料才能判斷的語意檢查分開；例如地圖列寬一致、符號有對應圖例、技能／物品引用 ID 存在、門檻順序合理，應由同一套內容驗證流程負責。
- 在文件中記錄校驗入口、錯誤定位方式及資料遷移策略；持續沿用既有啟動校驗，不必為此引入熱重載或額外服務。

**效益與成本：** 讓內容作者能在執行遊戲前發現資料錯誤，降低新地圖、技能與物品的擴充風險；需盤點舊資料並選擇是否採相容或版本遷移。

### P2｜以小規模可玩內容驗證技能與組合，再擴大內容承諾

技能規劃已涵蓋 17 棵專精樹及多種合體／連攜組合，而實作清單仍包含大範圍資料擴充與動態解算器工作。這類內容不只要驗證資料能載入，也要驗證組合在不同隊伍、敵人與資源條件下是否有趣且可平衡。〔[skills/README.md](../2_specifications/skills/README.md#L12)、[SYNERGY_AND_COMBOS.md](../2_specifications/skills/SYNERGY_AND_COMBOS.md#L14)、[M5_SKILL_AND_PROGRESSION_TASKS.md](../3_task_lists/backend/M5_SKILL_AND_PROGRESSION_TASKS.md#L49)〕

**建議做法**

- 保留完整專精樹作長期內容藍圖，但將「架構支援數量」與「首個可玩版本實裝數量」分開標示。
- 先選能代表不同戰術角色的少量技能與組合，驗證消耗、冷卻、反制、可讀性及隊伍配置差異，再決定擴充順序。
- 將載入／觸發正確性測試與遊戲平衡／體驗驗收分開，避免資料格式通過就被視為內容完成。
- 對尚未決定或未驗證的組合明確標示為提案，不把完整規劃清單誤當成本迭代的交付承諾。

**效益與成本：** 降低內容膨脹與全局返工；部分已撰寫內容需重新標示成熟度，短期看起來的「已規劃數量」可能下降。

## 建議的落地順序

1. **契約盤點：** 以一份短期核對清單確認 canonical 欄位、動作名稱、Schema 版本及現況標籤；不先大規模重寫文件。
2. **看板收斂：** 明定 L4 唯一入口與任務引用規則，合併重複追蹤，清理手工複製的過期測試數字。
3. **切片驗收：** 為當前垂直切片補上完整玩家路徑與端到端 DoD，並處理需使用者決策的體驗方向差異。
4. **內容護欄：** 在既有資料載入與文件連結檢查流程中補契約和語意驗證，再逐步擴充技能／地圖內容。

## 文件維護的輕量 DoD 建議

每次新增或改動規劃文件時，至少檢查：

- 文件的層級、權威範圍、狀態與上游來源是否清楚。
- API、JSON 欄位、ID、數值與遊戲規則是否引用唯一來源，而非複製成第二套真相。
- 需求、規格、任務與驗收是否能以連結或穩定任務 ID 追溯。
- 範例是否有效、相對連結是否通過現有文件檢查，以及完成狀態是否有可核對依據。
- 若內容尚未決議或尚未驗證，是否明確標示，而非寫成現行承諾。

本報告提供改進方向而非自動批准的規格變更；任何涉及需求或架構規格的實質調整，仍應遵守現有文件治理與使用者審批流程。
