# 📋 第二階段：程式碼與 L2 / L3 / L4 文件深度對齊專項任務清單 (Code & Docs Alignment Tasks v2)

> **文檔層級**：Level 3 — Task Lists（專項對齊治理與實質缺口收斂執行清單）  
> **架構與豁免基準**：👉 [`ARCHITECTURAL_DECISIONS.md`](../2_specifications/ARCHITECTURAL_DECISIONS.md) 及 [`.agents/rules/documentation_alignment_guardrails.md`](../../.agents/rules/documentation_alignment_guardrails.md)  
> **專案總表指針**：👉 [`docs/3_task_lists/README.md`](README.md)  
> **全量驗證防線**：每完成一項任務必須同時通過 `.\scripts\check-doc-links.ps1` 與 `.\test.ps1`（403+ 測試全綠）。

---

## 🤖 零、給執行模型（Gemini 3.8）的單步執行守則與邊界聲明

> [!IMPORTANT]
> **專項定位與強制執行紀律（每次開始前必讀）**：
> 1. **與常規玩法看板區隔**：[`ACTIVE_TASKS.md`](../4_active_tasks/ACTIVE_TASKS.md) 為 `Milestone VS-01` 玩法垂直切片看板；本清單為**「第二階段全量稽核後之代碼缺口修補與 L2/L3/L4 契約對齊專項清單」**（前次第一階段 [`CODE_DOCS_ALIGNMENT_TASKS.md`](../0_archive/alignment_history/CODE_DOCS_ALIGNMENT_TASKS.md) 已結案封存）。
> 2. **嚴格依序單步執行**：請依 **Phase 1 (P0) ➔ Phase 2 (P1 開發) ➔ Phase 3 (P1 規格誠實化) ➔ Phase 4 (P2 清單同步)** 順序推進，每次對話預設執行 **1 個 `[ ]` 任務**。
> 3. **三大拍板裁決（已獲專案擁有者核准）**：
>    - **P0 最優先**：優先修復「清單已勾選 `[x]` 但執行期未接通」之斷層與實質程式碼 Bug（`ALIGN2-01` ~ `ALIGN2-05`）。
>    - **營地中樞排入開發**：正式將後端 `TownHubDto` 接通前端六大設施卡牌並移除 MUD 東南西北出口羅盤（`ALIGN2-06`）。
>    - **L2 規格誠實化校準**：針對短期不重寫之協定（WebSocket `INPUT`）、Schema 欄位與未實作視覺功能（如 `#viewport-canvas`、鐵匠重鑄），**一律以現行真實程式碼為準修正 L2 文件，並將理想設計移入或標註 `【待深入討論／長線演進藍圖】`**（`ALIGN2-08` ~ `ALIGN2-10`）。
> 4. **完工勾選紅線（DoD）**：任何任務標記 `[x]` 前，必須先讀檔查證實體代碼、補齊真實行為測試（禁止手算純數學假測試），並在終端機完整執行全量驗證（**嚴禁只跑單一 `-Dtest` 即宣稱完工**）：
>    - `powershell -ExecutionPolicy Bypass -File .\test.ps1`（確認 409+ 測試 `Failures: 0, Errors: 0`）
>    - `powershell -ExecutionPolicy Bypass -File .\scripts\check-doc-links.ps1`（確認 Exit Code `0`）
>    全部 12 項完成後，將本檔封存至 [`docs/0_archive/alignment_history/`](../0_archive/alignment_history/)。

---

## 🚦 一、任務總覽與執行優先序矩陣 (Execution Matrix)

| 階段 / 優先級 | 任務代碼 | 任務名稱 | 核心目標檔案 / 範疇 |
| :---: | :---: | :--- | :--- |
| **Phase 1 · P0** | **ALIGN2-01** | 打通裝備隨機詞綴與 `poiseBreakPower` 之 DTO 投影與戰鬥削韌結算 | `DrpgStateDto.java`, `PartyItemSlot.java`, `DrpgCombatLoop.java` |
| **Phase 1 · P0** | **ALIGN2-02** | 修復地牢「🔥 點火」按鈕、傳送門地牢 ID 斷鏈與 4 種補血消耗品失效 | `dungeon-panel.js`, `facilities.json`, `Party.java`, `consumables.json` |
| **Phase 1 · P0** | **ALIGN2-03** | 修復 `DamageType`（`METAL`/`CHAOS`/`FORCE`）、5P 萬象終結技死分支與 5 人陣破陣條件 | `DamageType.java`, `FormationFinisherClassifier.java`, `FormationEngine.java` |
| **Phase 1 · P0** | **ALIGN2-04** | 消除 `mud-core.js` 會話接管 `TypeError` 並統一導流日誌至 `message-log-panel.js` | `mud-core.js`, `mud-ui.js`, `message-log-panel.js`, `index.html` |
| **Phase 1 · P0** | **ALIGN2-05** | 徹底收斂 Option A 精神萎靡一致性（更新遊戲指南、恢復 `SAN=0` 合擊資格、主角萎靡） | `index.html`, `ComboResolver.java`, `DrpgCombatLoop.java`, `PartyMember.java` |
| **Phase 2 · P1** | **ALIGN2-06** | 營地中樞（`TownHubDto`）正式接線 `DrpgStateDto` 與前端六大設施卡牌（移除十字出口） | `DrpgStateDto.java`, `GameStateBroadcastService.java`, `town-panel.js`, `index.html` |
| **Phase 2 · P1** | **ALIGN2-07** | 清除剩餘 7 個 JS 模組之 69 處內聯 `onclick` 與補齊覆蓋存檔二次確認 | `party-modal.js`, `party-skills-tab.js`, `battle-panel.js`, `save-modal.js` 等 |
| **Phase 3 · P1** | **ALIGN2-08** | 三大 Data Schema 與專精樹說明對齊真實載入器，並修正 `KEY_ITEM` 篩選與無效欄位 | `ITEM_SCHEMA.md`, `SKILL_SCHEMA.md`, `DUNGEON_SCHEMA.md`, `quest_items.json` |
| **Phase 3 · P1** | **ALIGN2-09** | 後端規格手冊（`M1`~`M9`、`MODULES_MAP`、`ARCHITECTURE_SUMMARY`）誠實化校準 | `docs/2_specifications/backend/*.md`, `MODULES_MAP.md` |
| **Phase 3 · P1** | **ALIGN2-10** | 前端規格手冊（`FE1`~`FE6`、`FRONTEND_LAYOUT_GUIDE`、`UI_PAGE_STRUCTURE`）誠實化校準 | `docs/2_specifications/frontend/*.md` |
| **Phase 4 · P2** | **ALIGN2-11** | 同步 L3 全模組任務清單與總表（清理 `CyberSchool`、幽靈檔案指針與測試基準） | `docs/3_task_lists/README.md`, `FUTURE_IMPROVEMENTS.md`, `*_TASKS.md` |
| **Phase 4 · P2** | **ALIGN2-12** | 校準 L4 衝刺看板（`ACTIVE_TASKS.md`、`SPRINT_GOAL.md`）真實剩餘範圍與 v3.0 定位 | `docs/4_active_tasks/ACTIVE_TASKS.md`, `SPRINT_GOAL.md` |

---

## 🔴 二、Phase 1：P0 實質功能損壞修復與假完工閉環（最高優先）

### - [x] Task ALIGN2-01 (EQUIP-DTO-AND-COMBAT-POISE): 打通裝備隨機詞綴與 `poiseBreakPower` 之 DTO 投影與戰鬥削韌結算（100% 驗收結案）
- **核心落點**：`DrpgStateDto.java`、`PartyItemSlot.java`、`Party.java`、`PartyMember.java`、`DrpgCombatLoop.java`、`DrpgRewardService.java`、`DrpgBattleService.java`、`ComboResolver.java`、`EquipCommand.java`、`party-equipment-tab.js` 及對應測試。
- **結案摘要**：✅ 完成 DTO 詞綴／削韌投影與 40 格預設行囊、`PartyItemSlot` `@JsonProperty("name")` 純基礎名持久化 + `getDisplayNameWithAffixes()` 展示分離、普攻／技能／合擊／5P 終結技真實裝備削韌結算，以及 `activeBattles.remove(key, ctx)` 原子條件移除修復虛擬執行緒競態。

---

### - [x] Task ALIGN2-02 (DUNGEON-TORCH-PORTAL-CONSUMABLES): 修復地牢「🔥 點火」按鈕、傳送門地牢 ID 斷鏈與 4 種補血消耗品失效（100% 驗收結案）
- **核心落點**：`dungeon-panel.js`、`app.js`、`facilities.json`、`DungeonCommand.java`、`Party.java`、`PartyItemSlot.java`、`InventoryCommand.java`、`consumables.json`、`bag-drawer.js`、`party-equipment-tab.js` 及 `DungeonTorchAndTrapTest`、`UnifiedPartyEconomyTest`、`DungeonDefeatSettlementTest`。
- **結案摘要**：✅ 移除無效 `window.cmd` 改用 `data-action="dungeon-torch"` 並限定地牢探索態 `[T]` 快捷鍵；對齊 `"mozhu_mines_b1f"` 與別名防呆；打通 4 種補血藥與 `purify_tea`（25 SAN + 20 MP）雙回復；以 `awaitCapturedMessage` 根除 `VirtualActor` 非同步信箱測試競態（409 測全綠）。

---

### - [x] Task ALIGN2-03 (COMBAT-DAMAGETYPE-AND-FORMATION-BUGS): 修復 `DamageType`、5P 萬象終結技死分支與 5 人陣破陣條件（100% 驗收結案）
- **核心落點**：`DamageType.java`、`PartyMemberSkill.java`、`DrpgCombatLoop.java`、`FormationFinisherClassifier.java`、`FormationTemplate.java`、`FormationEngine.java`、`Party.java`、`formations.json`、`DataIntegrityValidator.java`、`M3_COMBAT_ENGINE.md` 及對應測試（`CrossComboAndFormationResonanceTest`、`FormationFinisherClassifierTest`、`PartyFormationTest`）。
- **結案摘要**：✅ 修正 `DamageType`（`METAL` 轉入 `MAGIC` 支援抗性逆流回血，新增 `FORCE`/`CHAOS`）；打通 `SkillTemplate.damageType` 投影與啟動期 Fail-Fast 校驗；移除屬性硬編碼猜測並保留合法標籤解析；修復 5P 萬象混元終結技判定死分支；解耦陣法最低存活人數 `minAliveCount` 與陣亡孔位自動遞補，並實裝必要職業陣亡破陣機制（411 測全綠，check-doc-links 0 錯誤）。

---

### - [x] Task ALIGN2-04 (LEGACY-JS-CRASH-AND-LOG-ROUTING): 消除 `mud-core.js` 會話接管 `TypeError` 並統一導流日誌至 `message-log-panel.js`（100% 驗收結案）
- **核心落點**：`index.html`、`mud-core.js`、`mud-ui.js`、`message-log-panel.js`、`app.js`。
- **結案摘要**：✅ 移除 `mud-core.js` / `mud-ui.js` 對 `#hp-val`、`#mp-val`、`#cmd-input` 之無效 DOM 操作；所有文字、實體、戰報全面統一導流至 `message-log-panel.js` 之 `appendLog()`；修正 `app.js` 指向 `#dev-cmd-input` 並移除 `who` 指令殘留（411 測試全綠）。

---

### - [x] Task ALIGN2-05 (OPTION-A-SAN-RESIDUAL-PURGE): 徹底收斂 Option A 精神萎靡一致性（100% 驗收結案）
- **核心落點**：`index.html`、`bag-drawer.js`、`ComboResolver.java`、`DrpgCombatLoop.java`、`Party.java`、`PartyMember.java`、`DrpgStateDto.java`、`PartyInventoryAndMadnessTest.java`。
- **結案摘要**：✅ 遊戲指南 `#guide-modal` 第三條全面改寫為 Option A 精神防護罩與精神萎靡說明；`Party.java` 與 `bag-drawer.js` 解走火入魔更新為解精神萎靡；`ComboResolver` 恢復 `SAN=0` 隊員參與合體技資格；`DrpgCombatLoop` 讓主角與隊友一致套用 `SAN=0` 萎靡 `-15%` 減傷（414 測試全綠）。

---

## 🟠 三、Phase 2：P1 營地中樞前端接線與 CSP 安全收斂（排入開發）

### - [x] Task ALIGN2-06 (TOWN-HUB-FRONTEND-INTEGRATION): 營地中樞（`TownHubDto`）正式接線 `DrpgStateDto` 與前端六大設施卡牌（100% 驗收結案）
- **核心落點**：`DrpgStateDto.java`、`GameStateBroadcastService.java`、`TownHubService.java`、`TownHubDto.java`、`TownFacilityCommand.java`、`index.html`、`town-panel.js`、`app.js`、`style-05-town-and-hub.css`、`DrpgStateDtoTest.java`。
- **結案摘要**：✅ `TownHubDto` 正式接通 `DrpgStateDto.ofTown` 並隨主角 `chipRate` 動態投影三階透鏡名描述與 `glitch`；移除 MUD 東南西北 `exits` 十字出口羅盤；前端以現代卡牌網格渲染六大設施（百煉堂/藏經閣/演武道場/外門集市/回春客棧/坑道傳送門）；`app.js` 接通事件委派（415 測試全綠）。

---

### - [x] Task ALIGN2-07 (FE-CSP-ONCLICK-ZERO): 清除剩餘 7 個 JS 模組之動態內聯 `onclick` 並補齊覆蓋存檔二次確認（100% 驗收結案）
- **核心落點**：`party-modal.js`、`party-skills-tab.js`、`party-formation-tab.js`、`battle-panel.js`、`skill-drawer.js`、`shop-modal.js`、`save-modal.js`、`party-equipment-tab.js`。
- **結案摘要**：✅ 7 個 JS 模組全量 69 處內聯 `onclick` 清理完畢，全面改用 `data-action` 事件委派；`party-modal` 補齊 `escapeHtml`；統一穿戴指令為 `equip`；`save-modal` 加入覆蓋存檔二次確認（全專案 `onclick=` 0 處，415 測試全綠）。

---

## 📘 四、Phase 3：P1 L2 規格手冊與三大 Schema 誠實化校準（以現行代碼為準）

### - [x] Task ALIGN2-08 (L2-DATA-SCHEMAS-HONEST-SYNC): 三大 Data Schema 與專精樹說明對齊真實載入器（100% 驗收結案）
- **核心落點**：`ITEM_SCHEMA.md`、`SKILL_SCHEMA.md`、`DUNGEON_SCHEMA.md`、`skills/README.md`、`SYNERGY_AND_COMBOS.md`、`quest_items.json`、`party-equipment-tab.js`、`CyberSkillAndItemAffixTest.java`。
- **結案摘要**：✅ 三大 Schema 與專精樹手冊 100% 誠實對齊 Java 真實載入契約（`ItemTemplate`、`PartyItemSlot`、`PartyMemberSkill`、`DungeonFloorJson`、`DungeonTile.TileType`），未實作結構標註長線演進藍圖；正名「千星聯合科技」並補正 `attackSpeed=8` 與 `LEGENDARY` 品質；前端打通 `KEY_ITEM` 任務信物篩選；補齊晶片契約單測（416 測全綠，check-doc-links 0 錯誤）。

---

### - [x] Task ALIGN2-09 (L2-BACKEND-SPECS-HONEST-SYNC): 後端規格手冊（`M1`~`M9`、`MODULES_MAP`、`ARCHITECTURE_SUMMARY`）誠實化校準（100% 驗收結案）
- **核心落點**：`docs/2_specifications/backend/*.md`、`MODULES_MAP.md`、`ARCHITECTURE_SUMMARY.md`。
- **結案摘要**：✅ 12 份 L2 後端手冊 100% 誠實對齊 Java 真實載入契約與架構；誠實記錄 `GameCommand.Input` 傳入協定與 `DrpgStateDto` 真實 Record 欄位；消滅虛構類別與不存在路徑；修正圓桌判定上限、火把光照/黑暗扣 SAN/動態危險槽、全滅資產保全與 `save restore` 壞檔還原；未實作功能一律標註長線演進藍圖（416 測全綠，check-doc-links 0 錯誤）。

---

### - [x] Task ALIGN2-10 (L2-FRONTEND-SPECS-HONEST-SYNC): 前端規格手冊（`FE1`~`FE6` 及佈局指南）誠實化校準（100% 驗收結案）
- **核心落點**：`docs/2_specifications/frontend/*.md`（共 8 份文件）。
- **結案摘要**：✅ 8 份 L2 前端手冊 100% 誠實對齊現行真實 JS/CSS/HTML 架構；消滅不存在的 `viewMode` 與虛構類別方法；如實記錄 `INPUT` 封包協定、`mode` 狀態鍵、三態按鍵映射與全域事件委派；將 `#viewport-canvas`、常駐普攻 ATB、NPC 分支對話彈窗標註為長線演進藍圖；校準全滅全額資產保全契約；完整補齊 10 個彈窗模組與真實 CSS Token/斷點（416 測全綠，check-doc-links 0 錯誤）。

---

## 🟡 五、Phase 4：P2 L3 任務清單與 L4 衝刺看板全域同步收尾

### - [x] Task ALIGN2-11 (L3-TASK-LISTS-SYNC): 同步 L3 全模組任務清單與總表（100% 驗收結案）
- **核心落點**：`docs/3_task_lists/README.md`、`FUTURE_IMPROVEMENTS.md`、`backend/*_TASKS.md`、`data_schemas/*_TASKS.md`、`frontend/*_TASKS.md`（共 10 份清單）。
- **結案摘要**：✅ L3 總表與 10 份模組任務清單 100% 誠實對齊；移除不存在之 `synergy_combos.json` 幽靈指針；過期枚舉 `CyberSchool` / `cyberSchool` 全面更新為 `OriginSchool` + 17 專精樹 `treeId`；補齊 `ItemAffixService`、`PartyItemSlot`、`ActiveBuff`、`BuffSettlementService` 等核心指針並移除失效 `speed` 描述；校準 `M8-1`、`FE4`、`FE5` 真實完成度；`FUTURE_IMPROVEMENTS` 清理 `[ ]` 競合待辦與基因補完字眼（416 測全綠，check-doc-links 0 錯誤）。

---

### - [x] Task ALIGN2-12 (L4-SPRINT-BOARD-SYNC): 校準 L4 衝刺看板（`ACTIVE_TASKS.md`、`SPRINT_GOAL.md`）真實剩餘範圍與 v3.0 定位（100% 驗收結案）
- **核心落點**：[`docs/4_active_tasks/ACTIVE_TASKS.md`](../4_active_tasks/ACTIVE_TASKS.md)、[`docs/4_active_tasks/SPRINT_GOAL.md`](../4_active_tasks/SPRINT_GOAL.md)、[`docs/3_task_lists/README.md`](README.md)。
- **結案摘要**：✅ L4 看板與衝刺目標 100% 誠實對齊；`ACTIVE_TASKS.md` 補齊 `Milestone ALIGN-V2` 完成項、修正 `Milestone VS-03` 千星軌道與古代工程師重工深井、Task 1~4 移除幽靈檔案 `party-overview-tab.js` 與 L4 硬編碼數值雙寫，標明 `ItemAffixService` 與必掉晶片已實裝並收斂剩餘驗收焦點；`SPRINT_GOAL.md` 移除音效描述與舊技能名，同步五大核心支柱與 v3.0 敘事；`docs/3_task_lists/README.md` 更新 `R1` 描述並將本清單正式標記為已 100% 完工結案（416 測全綠，check-doc-links 0 錯誤）。
