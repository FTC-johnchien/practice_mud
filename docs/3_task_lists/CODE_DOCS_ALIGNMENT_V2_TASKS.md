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
> 4. **完工勾選紅線（DoD）**：任何任務標記 `[x]` 前，必須先 `read_file` 查證實體代碼、補齊對應測試，並實跑 `.\scripts\check-doc-links.ps1` 與 `.\test.ps1` 取得 Exit Code `0`。全部 12 項完成後，將本檔封存至 [`docs/0_archive/alignment_history/`](../0_archive/alignment_history/)。

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

### - [x] Task ALIGN2-01 (EQUIP-DTO-AND-COMBAT-POISE): 打通裝備隨機詞綴與 `poiseBreakPower` 之 DTO 投影與戰鬥削韌結算（二審 100% 已落地，2 項衍生問題已根除結案）
- **目標檔案**：
  - `src/main/java/com/example/htmlmud/dungeon/DrpgStateDto.java`
  - `src/main/java/com/example/htmlmud/party/PartyItemSlot.java`
  - `src/main/java/com/example/htmlmud/party/Party.java`
  - `src/main/java/com/example/htmlmud/party/PartyMember.java`
  - `src/main/java/com/example/htmlmud/combat/DrpgCombatLoop.java`
  - `src/main/java/com/example/htmlmud/combat/DrpgRewardService.java`
  - `src/main/java/com/example/htmlmud/combat/DrpgBattleService.java`
  - `src/main/java/com/example/htmlmud/combat/ComboResolver.java`
  - `src/main/resources/static/js/modals/party-equipment-tab.js`
  - `src/test/java/com/example/htmlmud/CyberSkillAndItemAffixTest.java`
  - `src/test/java/com/example/htmlmud/DrpgStateDtoTest.java`
  - `src/test/java/com/example/htmlmud/DrpgBattleServiceTest.java`
- **已驗收通過項目（勿回退）**：
  - ✅ DTO 詞綴／削韌投影與 40 格空背包預設、`PartyItemSlot` `@JsonProperty("name")` + 16 個衍生 getter `@JsonIgnore` 與 `ObjectMapper` 雙重往返測試、`ComboResolver` 合擊與 5P 終結技真實戰鬥削韌測試、`party-equipment-tab.js` 三處削韌與詞綴顯示皆已到位。
- **指導員二審待補項目（完成以下 2 點並確保 `.\test.ps1` 0 失敗後勾選 `[x]`）**：
  1. **補齊 `getName()` 改回純基礎名稱後之掉落／穿戴展示名稱（Side-effect 修正）**：因 `PartyItemSlot.getName()` 現回傳純基礎名，導致 `DrpgRewardService.java` L256（戰鬥結算掉落卡牌 `dropName = equipSlot.getName()`）、`Party.java` L224–230（穿戴裝備回饋訊息）與 `src/main/java/com/example/htmlmud/application/command/impl/EquipCommand.java` L129 漏顯示隨機前後綴（如掉出神兵只顯示白板名）。請將上述展示端改呼叫 `getDisplayNameWithAffixes()`。
  2. **根治 `DrpgBattleServiceTest` 虛擬執行緒競態紅燈（實跑 `.\test.ps1` 於 L103 失敗）**：上一場測試 `cancel(true)` 後，舊虛擬執行緒非同步進入 `DrpgCombatLoop.java` L560–568 `finally`（及 `DrpgRewardService.java` L123–124 / L341–342、`DrpgBattleService.java` L958），以單參數 `activeBattles.remove(player.getName())` **無條件誤刪了下一場測試剛建立的新 `BattleContext`**（單純在 `DrpgBattleServiceTest` 加長 `sleep` 無法修復已被誤刪的 `ctx`）。請將上述移除呼叫全面改為雙參數原子條件移除 `activeBattles.remove(key, ctx)`，確保舊戰鬥迴圈收尾絕不覆蓋新戰鬥。
- **驗收標準**：戰鬥掉落卡牌與穿戴訊息正確顯示含詞綴全名；連續執行 `.\test.ps1` 與 `.\scripts\check-doc-links.ps1` 100% 綠燈（0 Failures）。

---

### - [x] Task ALIGN2-02 (DUNGEON-TORCH-PORTAL-CONSUMABLES): 修復地牢「🔥 點火」按鈕、傳送門地牢 ID 斷鏈與 4 種補血消耗品失效（複審 100% 已落地結案）
- **目標檔案**：
  - `src/main/resources/static/js/panels/dungeon-panel.js`
  - `src/main/resources/static/js/app.js`
  - `src/main/resources/data/zones/newbie_village/facilities.json`
  - `src/main/java/com/example/htmlmud/application/command/impl/DungeonCommand.java`
  - `src/main/java/com/example/htmlmud/party/Party.java`
  - `src/main/resources/data/global/items/consumables/consumables.json`
  - `src/test/java/com/example/htmlmud/DungeonTorchAndTrapTest.java`
  - `src/test/java/com/example/htmlmud/UnifiedPartyEconomyTest.java`
  - `src/test/java/com/example/htmlmud/DungeonDefeatSettlementTest.java`
- **已驗收通過項目（勿回退）**：
  - ✅ `dungeon-panel.js` 已改用 `data-action="dungeon-torch"` 移除無效 `window.cmd`；`facilities.json` 與 `DungeonCommand` 已對齊 `"mozhu_mines_b1f"` 並補上別名防呆；`consumables.json`、`Party.useItemOnMember`、`InventoryCommand`、`bag-drawer.js` 與 `party-equipment-tab.js` 已完整打通 4 種補血道具與 `purify_tea`（25 SAN + 20 MP）雙回復及單元測試。
- **指導員複審待補項目（完成以下 2 點並確保全量 `.\test.ps1` 0 失敗後勾選 `[x]`）**：
  1. **限定 `[T]` 點火快捷鍵僅於地牢模式觸發**：`src/main/resources/static/js/app.js` L617–619 目前未判斷模式，導致玩家在城鎮（`TOWN`）按 `T` 鍵也會發送 `'dungeon torch'` 並收到無效導航提示。請加上 `if (store.get('mode') === 'DUNGEON')` 保護（僅在地牢非戰鬥探索態觸發點火）。
  2. **修復全量 `.\test.ps1` 中 `DungeonDefeatSettlementTest` 之虛擬執行緒信箱競態紅燈**：連續兩輪實跑全量 `.\test.ps1`（409 測）均於 `DungeonDefeatSettlementTest.java` L196 失敗——原因是 `saveCommand.execute("1")` 在走廊阻擋存檔時無磁碟 I/O 耗時，`player.reply(...)` 投遞至 `VirtualActor` 非同步信箱尚未寫入 `capturedMessages` 主執行緒便立即斷言（待 AssertJ 印出錯誤時訊息才剛抵達）。請為 `DungeonDefeatSettlementTest.java` L196、L208、L221 之 `capturedMessages` 斷言封裝短暫輪詢等待輔助方法（如最多等待 500ms），確保全量 `.\test.ps1` 100% 穩定通過。
- **驗收標準**：僅地牢探索態按 `T` 觸發點火；4 種補藥與清心茶全數生效；全量 `.\test.ps1`（409 測）與 `.\scripts\check-doc-links.ps1` 0 失敗。

---

### - [ ] Task ALIGN2-03 (COMBAT-DAMAGETYPE-AND-FORMATION-BUGS): 修復 `DamageType`、5P 萬象終結技死分支與 5 人陣破陣條件
- **目標檔案**：
  - `src/main/java/com/example/htmlmud/domain/model/enums/DamageType.java`
  - `src/main/java/com/example/htmlmud/combat/DrpgCombatLoop.java`
  - `src/main/java/com/example/htmlmud/party/PartyMemberSkill.java`
  - `src/main/java/com/example/htmlmud/combat/FormationFinisherClassifier.java`
  - `src/main/java/com/example/htmlmud/party/FormationTemplate.java`
  - `src/main/java/com/example/htmlmud/party/FormationEngine.java`
  - `src/main/java/com/example/htmlmud/party/Party.java`
  - `src/main/resources/data/formations/formations.json`
  - `src/main/java/com/example/htmlmud/template/DataIntegrityValidator.java`
  - `docs/2_specifications/backend/M3_COMBAT_ENGINE.md`
  - `src/test/java/com/example/htmlmud/FormationFinisherClassifierTest.java`
  - `src/test/java/com/example/htmlmud/CrossComboAndFormationResonanceTest.java`
  - `src/test/java/com/example/htmlmud/PartyFormationTest.java`
- **指導員派工前預審地雷清單（實作時務必遵守）**：
  1. **`DamageType`、技能屬性校驗與硬編碼猜字清理**：
     - `DamageType.java` L13 將 `METAL(PHYSICAL)` 移至道法系改為 `METAL(MAGIC)`（使 `CombatRules.resolveResistance` L347 不再攔截金系逆流回血），並新增 `FORCE(MAGIC)` 與 `CHAOS(DARK)`；同步更新 [`M3_COMBAT_ENGINE.md`](../2_specifications/backend/M3_COMBAT_ENGINE.md) L118–170 繼承樹說明。
     - `PartyMemberSkill.fromSkillTemplate`（L469 附近）補齊從 `tpl.getMechanics().damageType()` 映射 `.damageType(...)`。
     - `DrpgCombatLoop.resolveSkillDamageType`（L668–696）：**刪除 L684–691 以中英文名稱包含 `"炎"`/`"火"`/`"魔"` 猜測屬性的硬編碼 fallback**，但**保留 `skill.getTags()` 標籤解析**（可升級為比對合法 `DamageType` 名稱，確保 `BattlePresentationEventsTest` L142 等以 `tags` 標註屬性的測試相容）。
     - `DataIntegrityValidator.validateSkills`（L353–362）對所有 `PartyMemberSkill.getDamageType()` 與 `FormationFinisherClassifier.getAllSpecs()` 執行 `DamageType.valueOf(...)` 校驗（非法值寫入 `errors`）。
  2. **`FormationFinisherClassifier.java` L266–270 萬象混元死分支（防誤傷 `testFourTrioFinishers` 地雷！）**：
     - ⚠️ **嚴禁直接只刪內層 `if (distinctSchools.size() == 4)`**！因為 `FormationFinisherClassifierTest.testFourTrioFinishers`（L138–155）測試的 5 人三才隊（如 `"UNIVERSAL", "CULTIVATOR", "CULTIVATOR", "CTHULHU", "HIGH_TECH"`）擁有 `alive.size() == 5`、`distinctSchools.size() == 3`、`universalCount == 1`，但其非萬象成員為 `2+1+1`（`nonUniversalCount == 4`，刻意重複修仙並排除工程師），必須落入後方 `TRIO_*` 分支！
     - 正確條件：設 `int nonUniversalCount = alive.size() - universalCount;`，僅當 `distinctSchools.size() == 4 || (distinctSchools.size() == 3 && nonUniversalCount == 3 && universalCount > 0)`（即 3 名非萬象隊員各屬不同門派 `1+1+1` 無重複，且有 `UNIVERSAL` 補足第 4 門派，如 4 人存活隊伍或雙 `UNIVERSAL` 5 人隊）時回傳 `ALL_UNIVERSAL`。
  3. **`FormationTemplate`、`Party.isFormationActive` 與 `FormationEngine.checkAndBreakFormation` 三處同步解耦（防既有測試衝突地雷！）**：
     - 在 `FormationTemplate.java` 新增 `@Builder.Default private int minAliveCount = 0;` 與輔助方法 `getEffectiveMinAliveCount()`（若 `minAliveCount > 0` 回傳該值，否則回傳 `Math.min(requiredPartySize, 3)`；並在 `formations.json` 為 2 人陣設 `2`、3~5 人陣設 `3`）。
     - ⚠️ **保持編隊選陣門檻不變**：`FormationTemplate.isEligibleForParty`（檢查 `party.size() == requiredPartySize`）與 `FormationEngine.isFormationEligible`（檢查 `effSize == requiredPartySize`）**維持原判**，確保 `PartyFormationTest` L167（遣散隊員自動降級陣法）與 L250（死 1 人手動切換 4 人陣）不回歸。
     - ⚠️ **同步更新 `Party.java` L294–301 `isFormationActive()`**：必須同時檢查「編制總人數 `members.size() >= reqSize`」（確保 `PartyFormationTest` L362 之「3 人隊伍強裝 4 人陣 `isFormationActive() == false`」繼續通過）且「存活人數 `effSize >= ft.getEffectiveMinAliveCount() && effSize <= reqSize`」！
     - **更新 `FormationEngine.checkAndBreakFormation`（L131–143）**：將人數破陣條件改為 `alive < ft.getEffectiveMinAliveCount()`；且當隊員陣亡但**未達破陣門檻**（如 5 人陣剩 4 或 3 人存活且必要職業仍在）時，**必須呼叫 `party.alignFormationRows(); party.refreshFormation();`** 即時將存活隊員自動遞補至 `Slot 0..alive-1` 並清除死者孔位 Buff！
     - **同步演進 `PartyFormationTest.testFormationEngineBreakOnMemberDeath`（L217–246）**：原測試在 5 人五行陣死第 1 人時即斷言破陣；請改為驗證「死第 1~2 人（剩 4、3 人）時**不破陣且自動遞補孔位與 Buff**，死第 3 人（剩 2 人 `< 3`）時才觸發**陣法崩解、靈威歸零與 Buff 清空**」，並額外補測「鳳天舞之陣唯一必要職業（如法師墨衍）陣亡時，縱使剩 4 人仍立即破陣」。
- **驗收標準**：金系 `METAL` 抗性 `>1.0` 逆流回血、`CHAOS`/`FORCE` 真實技能傷害屬性結算、`1+1+1+UNIVERSAL` 萬象混元終結技（且不破壞 4 組三才鼎立測試）、5 人陣死 1~2 人存活孔位與 Buff 自動遞補、低於 `minAliveCount` 或缺必要職業破陣全數通過測試；全量 `.\test.ps1` 與 `.\scripts\check-doc-links.ps1` 0 失敗。

---

### - [ ] Task ALIGN2-04 (LEGACY-JS-CRASH-AND-LOG-ROUTING): 消除 `mud-core.js` 會話接管 `TypeError` 並統一導流日誌至 `message-log-panel.js`
- **目標檔案**：
  - `src/main/resources/static/index.html`
  - `src/main/resources/static/js/mud-core.js`
  - `src/main/resources/static/js/mud-ui.js`
  - `src/main/resources/static/js/panels/message-log-panel.js`
  - `src/main/resources/static/js/app.js`
- **現況問題**：
  1. 會話接管時後端發送 `STAT_UPDATE`，`mud-core.js` L11–23 `updateStats` 直接寫入已移除的 `#hp-val` / `#hp-bar` / `#mp-val` / `#mp-bar`，拋出 `Uncaught TypeError`。
  2. `mud-core.js` L28–48 `appendHtml` 直接把裸 `<div>` 塞進 `#log`，繞過 `message-log-panel.js` 的 `appendLog()`，導致【戰鬥／劇情／系統】日誌分頁過濾與頂部跑馬燈失效。
  3. `mud-core.js`、`mud-ui.js`、`app.js` L360 仍查詢不存在的 `#cmd-input`，且 `index.html` L619 開發者輸入框 placeholder 仍殘留多人指令 `who`。
- **實作步驟**：
  1. 安全收斂（或直接整合除役）`mud-core.js` 與 `mud-ui.js`：移除對 `#hp-val`、`#mp-val`、`#cmd-input` 之無效 DOM 操作；將所有文字／實體／屍體訊息輸出統一委派給 `message-log-panel.js` 的 `appendLog`（保留 ANSI 色碼安全轉換）。
  2. 修正 `app.js` L360 指向 `#dev-cmd-input`，並移除 `index.html` L619 placeholder 中的 `who`。
- **驗收標準**：重開分頁接管會話時瀏覽器 Console 零 `TypeError`；伺服器推送之戰鬥、劇情與系統訊息均可被日誌分頁正常篩選。

---

### - [ ] Task ALIGN2-05 (OPTION-A-SAN-RESIDUAL-PURGE): 徹底收斂 Option A 精神萎靡一致性
- **目標檔案**：
  - `src/main/resources/static/index.html`
  - `src/main/resources/static/js/modals/bag-drawer.js`
  - `src/main/java/com/example/htmlmud/combat/ComboResolver.java`
  - `src/main/java/com/example/htmlmud/combat/DrpgCombatLoop.java`
  - `src/main/java/com/example/htmlmud/party/Party.java`
  - `src/main/java/com/example/htmlmud/party/PartyMember.java`
  - `src/main/java/com/example/htmlmud/dungeon/DrpgStateDto.java`
  - `src/test/java/com/example/htmlmud/PartyInventoryAndMadnessTest.java`
- **現況問題**：
  1. `index.html` L164–165（`#guide-modal` 第三條）仍向玩家展示已廢止之「40% 背刺隊友、施符鎮魔、異變為敵方 Boss 剖取道核」；`bag-drawer.js` L75 仍顯示 `(解走火入魔)`。
  2. `ComboResolver.isHealthyCombatant`（L110–113）禁止 `SAN=0` 隊員參與合體技（違反 Option A 絕不中斷連攜心流裁決）；`DrpgCombatLoop` L231–238 以 `if (!isLeader)` 跳過主角，導致主角 `SAN=0` 不會進入 `-15%` 精神萎靡減傷。
  3. `Party.java` L165、`PartyMember.getSanityStatus()` L368–375 與 `DrpgStateDto.java` L169 仍殘留 `"走火入魔狀態解除"`、`"【狂亂囈語】"` 與 `sanLevel = "MADNESS"`。
- **實作步驟**：
  1. 將 `index.html` `#guide-modal` 第三條改寫為 Option A 精神防護罩說明（黑暗探索損耗 SAN，`SAN=0` 附加【精神萎靡】全傷害 `-15%`，服用辟邪丹茶或回營調息即癒）；將 `bag-drawer.js` L75 與 `Party.java` L165 文案更新為解除【精神萎靡】。
  2. 移除 `ComboResolver.isHealthyCombatant` 對 `MadnessState.CHAOS` 的阻擋（存活隊員皆可參與合擊），並讓 `DrpgCombatLoop` 對主角與隊友一致套用 `SAN=0` 精神萎靡 `-15%` 減傷。
  3. 同步更新 `PartyMember.getSanityStatus()` 與 `DrpgStateDto` 之 `sanLevel` 字串（如 `"WEAKENED"`，並確認前端樣式相容），補充 `PartyInventoryAndMadnessTest` 驗證主角萎靡與 `SAN=0` 合擊不中斷。
- **驗收標準**：遊戲內指南彈窗、行囊提示、戰鬥減傷與合體技判定 100% 符合 [`ARCHITECTURAL_DECISIONS.md`](../2_specifications/ARCHITECTURAL_DECISIONS.md) 裁決一（Option A）。

---

## 🟠 三、Phase 2：P1 營地中樞前端接線與 CSP 安全收斂（排入開發）

### - [ ] Task ALIGN2-06 (TOWN-HUB-FRONTEND-INTEGRATION): 營地中樞（`TownHubDto`）正式接線 `DrpgStateDto` 與前端六大設施卡牌
- **目標檔案**：
  - `src/main/java/com/example/htmlmud/dungeon/DrpgStateDto.java`
  - `src/main/java/com/example/htmlmud/pipeline/GameStateBroadcastService.java`
  - `src/main/java/com/example/htmlmud/town/TownHubService.java`
  - `src/main/java/com/example/htmlmud/town/TownHubDto.java`
  - `src/main/java/com/example/htmlmud/application/command/impl/TownFacilityCommand.java`
  - `src/main/resources/static/index.html`
  - `src/main/resources/static/js/panels/town-panel.js`
  - `src/main/resources/static/js/app.js`
  - `src/main/resources/static/css/style-05-town-and-hub.css`
  - `src/test/java/com/example/htmlmud/DrpgStateDtoTest.java`
- **現況問題**：
  後端雖已建置 `TownHubService`、`CognitiveProxyService` 與 `facilities.json` 六大設施（百煉堂、靈台藏經閣、雜役演武道場、宗門外門集市、回春客棧／通鋪柴房、廢棄礦脈坑道），但 `DrpgStateDto.ofTown()` 僅推送傳統 MUD 房間 `exits`，前端 `index.html`（L346–365）與 `town-panel.js` 仍渲染東南西北十字移動羅盤，且完全未接收 `TOWN_HUB` / `OPEN_FACILITY` / `CAMP_REST_RESULT` 訊息。
- **實作步驟**：
  1. 在 `DrpgStateDto`（或 `TownViewDto` 擴充整合 `TownHubDto`）於城鎮模式下隨狀態一併投影 `TownHubDto`（依據主角 `player.getChipRate()` 經 `TownHubService.getTownHub(...)` 動態產出三階認知透鏡名稱、描述、`glitch` 標記與 `chipRate`）。
  2. 重構 `index.html` 營地視圖與 `town-panel.js`：**移除東南西北十字 `exits` 出口方塊**，改以現代 DRPG 六大設施卡牌網格渲染 `facilities`（支援 `glitch: true` 視覺微光擾動樣式於 `style-05-town-and-hub.css`，以及駐留夥伴／NPC 互動列表）。
  3. 在 `app.js` 全域 `data-action` 委派接通點擊設施卡牌之對應行為（如：回春客棧觸發 `town_facility rest` 一鍵調息、宗門外門集市開啟商店、雜役演武道場開啟小隊編隊／戰術／招募面板、靈台藏經閣與百煉堂開啟技能／裝備面板或對應設施回饋、廢棄礦脈坑道選擇進入 `mozhu_mines_b1f` / `taiyin_tomb_b1f`），並處理後端回傳之 `OPEN_FACILITY` / `CAMP_REST_RESULT` 事件。
  4. 更新整合測試確保單機開局與招募、商店、休整、進地牢全流程無縫閉環（注意檢查 `TownDungeonDualModeIntegrationTest` 等既有測試相容性）。
- **驗收標準**：城鎮介面不再出現 MUD 十字方位出口，完整呈現六大仙俠設施卡牌，且隨 `chipRate`（`<30%` / `30%~70%` / `>70%`）正確切換認知透鏡文案與 Glitch 特效；`.\test.ps1` 全綠。

---

### - [ ] Task ALIGN2-07 (FE-CSP-ONCLICK-ZERO): 清除剩餘 7 個 JS 模組之動態內聯 `onclick` 並補齊覆蓋存檔二次確認
- **目標檔案**：
  - `src/main/resources/static/js/modals/party-modal.js`
  - `src/main/resources/static/js/modals/party-skills-tab.js`
  - `src/main/resources/static/js/modals/party-formation-tab.js`
  - `src/main/resources/static/js/panels/battle-panel.js`
  - `src/main/resources/static/js/modals/skill-drawer.js`
  - `src/main/resources/static/js/modals/shop-modal.js`
  - `src/main/resources/static/js/modals/save-modal.js`
  - `src/main/resources/static/js/modals/party-equipment-tab.js`
- **現況問題**：
  1. 上述前 6 個模組（加上 `ALIGN2-02` 處理的 `dungeon-panel.js`）在 HTML 模板字串中仍殘留共 69 處 `onclick="..."`，違反 `FE1` 與 `UI_PAGE_STRUCTURE` 嚴格 CSP 零內聯事件鐵律；且 `party-modal.js` L289–290 `${mem.name}` / `${mem.roleTitle}` 漏做 `escapeHtml` 轉義。
  2. `party-equipment-tab.js` 穿戴裝備同時混用 `equip ...`（L264）與 `item equip ...`（L732）兩種前綴。
  3. `save-modal.js` L160–169 `triggerSaveSlot` 覆蓋已有舊存檔之槽位時，缺少 [`FE5`](../2_specifications/frontend/FE5_PARTY_EQUIPMENT_AND_MODALS.md) 規範之二次確認 `confirm()` 提示。
- **實作步驟**：
  1. 將 `party-modal.js`、`party-skills-tab.js`、`party-formation-tab.js`、`battle-panel.js`、`skill-drawer.js`、`shop-modal.js` 內所有字串拼接之 `onclick="..."` 全面改寫為 `data-action="..."` 搭配容器級事件委派，並補齊 `party-modal.js` 的 `escapeHtml()`。
  2. 統一 `party-equipment-tab.js` 穿戴指令格式；在 `save-modal.js` 檢測目標槽位已存在存檔時先彈出 `window.confirm` 防誤觸確認。
- **驗收標準**：以正則搜尋 `src/main/resources/static/` 全目錄之 `onclick=` 出現次數為 **`0`**；所有彈窗、技能切換、陣法裝備、商店買賣與存檔覆蓋確認運作正常。

---

## 📘 四、Phase 3：P1 L2 規格手冊與三大 Schema 誠實化校準（以現行代碼為準）

### - [ ] Task ALIGN2-08 (L2-DATA-SCHEMAS-HONEST-SYNC): 三大 Data Schema 與專精樹說明對齊真實載入器
- **目標檔案**：
  - [`docs/2_specifications/data_schemas/ITEM_SCHEMA.md`](../2_specifications/data_schemas/ITEM_SCHEMA.md)
  - [`docs/2_specifications/data_schemas/SKILL_SCHEMA.md`](../2_specifications/data_schemas/SKILL_SCHEMA.md)
  - [`docs/2_specifications/data_schemas/DUNGEON_SCHEMA.md`](../2_specifications/data_schemas/DUNGEON_SCHEMA.md)
  - [`docs/2_specifications/skills/README.md`](../2_specifications/skills/README.md)
  - [`docs/2_specifications/skills/SYNERGY_AND_COMBOS.md`](../2_specifications/skills/SYNERGY_AND_COMBOS.md)
  - `src/main/resources/data/global/items/quest/quest_items.json`
  - `src/main/resources/static/js/modals/party-equipment-tab.js`
- **對齊步驟**：
  1. **`ITEM_SCHEMA.md` & `quest_items.json`**：將現行契約欄位修正對齊 `ItemTemplate.java` 與 `EquipmentProp.java`（頂層 `"type"` 枚舉含 `KEY_ITEM`、屬性區塊 `"stats"` 欄位 `minDamage, maxDamage, defense, attackSpeed, poiseBreakPower`）；將尚未實作之物品級 `"lenses"`、`"affixPool"` 移入標註 `【待深入討論／長線演進藍圖】` 區段。同步修正 `quest_items.json` 中 `item_chip_qianxing_tracker`：移除無效 `"speed": 8`（或改配有效欄位）、將品質提升為 `"LEGENDARY"`（支援 `VS-01` 金色翻牌）、並將舊稱「千星殖民巨企」正名為 **「千星聯合科技」**（同步修正 `ITEM_SCHEMA.md` L172–175 範例文字）；修正 `party-equipment-tab.js` L160/165/270 任務物品篩選器相容 `'KEY_ITEM'`。
  2. **`SKILL_SCHEMA.md` & `skills/`**：將主動技能 Schema 修正對齊 `PartyMemberSkill.java` 真實欄位（`originSchool`、`treeId`、`aoe/heal/shield/buff`、`costType/spCost/mpCost/hpCost`、`damageType`、`tags`、`comboRole`），將巢狀 `costs` 與 `targetType` 重構草案標註為 `【待深入討論／長線演進藍圖】`；在 `skills/README.md` 與 `SYNERGY_AND_COMBOS.md` 對齊 `FormationFinisherClassifier` 真實終結技 ID（如 `finisher_all_universal`），並聲明具體平衡數值以 `src/main/resources/data/party/party_skills.json` 為唯一生效 SSoT（消除 `TREE_10/11/14/17` 靜態數值漂移疑慮）。
  3. **`DUNGEON_SCHEMA.md`**：對齊 `DungeonFloorLoader.java` 與 `DungeonTile.TileType` 真實枚舉（補入 `SPRING`，將尚未支援之 `CHEST`/`ELITE`、樓層別名與結構化 `checkType`/`checkDc` 明確標註 `【已決議待實作】` 或補齊最小必要枚舉支援以杜絕靜默降級）。
- **驗收標準**：三大 Schema 文件內所有未加藍圖標籤之 JSON 範例，100% 可被現行 Java 載入器無損反序列化；`.\scripts\check-doc-links.ps1` Check C 100% `PASS`。

---

### - [ ] Task ALIGN2-09 (L2-BACKEND-SPECS-HONEST-SYNC): 後端規格手冊（`M1`~`M9`、`MODULES_MAP`、`ARCHITECTURE_SUMMARY`）誠實化校準
- **目標檔案**：
  - [`docs/2_specifications/ARCHITECTURE_SUMMARY.md`](../2_specifications/ARCHITECTURE_SUMMARY.md)
  - [`docs/2_specifications/MODULES_MAP.md`](../2_specifications/MODULES_MAP.md)
  - [`docs/2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md`](../2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md)
  - [`docs/2_specifications/backend/M2_DRPG_DUNGEON.md`](../2_specifications/backend/M2_DRPG_DUNGEON.md)
  - [`docs/2_specifications/backend/M3_COMBAT_ENGINE.md`](../2_specifications/backend/M3_COMBAT_ENGINE.md)
  - [`docs/2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md`](../2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md)
  - [`docs/2_specifications/backend/M4_PARTY_AND_FORMATION.md`](../2_specifications/backend/M4_PARTY_AND_FORMATION.md)
  - [`docs/2_specifications/backend/M5_SKILL_AND_PROGRESSION.md`](../2_specifications/backend/M5_SKILL_AND_PROGRESSION.md)
  - [`docs/2_specifications/backend/M6_ITEM_AND_ECONOMY.md`](../2_specifications/backend/M6_ITEM_AND_ECONOMY.md)
  - [`docs/2_specifications/backend/M7_DATA_DRIVEN_TEMPLATES.md`](../2_specifications/backend/M7_DATA_DRIVEN_TEMPLATES.md)
  - [`docs/2_specifications/backend/M8_SAVE_AND_PERSISTENCE.md`](../2_specifications/backend/M8_SAVE_AND_PERSISTENCE.md)
  - [`docs/2_specifications/backend/M9_STATE_PIPELINE_AND_EVENTS.md`](../2_specifications/backend/M9_STATE_PIPELINE_AND_EVENTS.md)
- **對齊步驟**：
  1. **`M9` & `M1`**：將 `M9` 現行傳入協定更正為真實 `GameCommand.Input(String text)`（`{"type": "INPUT", "text": "..."}` 經 `CommandDispatcher` 分發），將 9 大結構化 `ActionCommand` JSON 移入末章 `【待深入討論／長線演進藍圖】`；將 `M9` 的 `DrpgStateDto` 與 `M1` §四 的 `TownHubDto` 簽章更新為與 `ALIGN2-06` 完工後之真實 Java Record 1:1 吻合（移除不存在的 `atmosphericAudio`、`FacilityInfoDto` 等）。
  2. **`M2` ~ `M8` 常數與狀態標籤校準**：
     - `M2`：對齊真實火把回滿 `100`、黑暗扣 SAN 數值、累加危險槽遇敵、靈泉固定回復量、`Set<GridCoord> openedChests` 與全滅結算／存檔點規則。
     - `M3` & `M3_1`：對齊 `CombatRules` 一元圓桌真實上限與暴擊公式；移除未實作之土+金【金石固元】與木+火傳染上的錯誤 `【現行已驗證】` 標籤（改標 `【待深入討論／長線演進藍圖】`）；修正 `M3_1` Phase 1 狀態 ID（如 `buff_spring_heal`）與 `com.example.htmlmud.combat.*` 真實套件路徑。
     - `M4` & `M5`：對齊 `XpProgressionService` 真實五維換算、升級經驗公式、技能等級 `+5%` 倍率、燕青預設站位，以及 `TacticsRule`（`GambitRule` 為工廠輔助類）說明。
     - `M6` & `M7` & `M8`：將 `M6` 尚未實作的「鐵匠鋪重鑄洗練」由 `【現行已驗證】` 改標 `【已決議待實作】`；校準 `M7` `DataIntegrityValidator` 真實校驗範圍；校準 `M8` 壞檔拋出 `SaveCorruptedException` 提示 `save restore` 及 `v0 -> v1 -> v2 -> v3` 真實遷移鏈。
  3. **`MODULES_MAP` & `ARCHITECTURE_SUMMARY`**：全面清查並修正虛構／舊類別名（`CommandDispatcher`、`MudWebSocketHandler`、`SaveMigrationPipeline`、`PlayerEvents` 等），移除不存在的 `buffs.json` 狀態定義檔引用。
- **驗收標準**：L2 後端 12 份手冊無任何虛構現役類別、無誤標 `【現行已驗證】` 之未實作功能，各檔行數維持在規範上限（`<= 400` 行）內；`.\scripts\check-doc-links.ps1` 全綠。

---

### - [ ] Task ALIGN2-10 (L2-FRONTEND-SPECS-HONEST-SYNC): 前端規格手冊（`FE1`~`FE6` 及佈局指南）誠實化校準
- **目標檔案**：
  - [`docs/2_specifications/frontend/FE1_CORE_STATE_AND_PIPELINE.md`](../2_specifications/frontend/FE1_CORE_STATE_AND_PIPELINE.md)
  - [`docs/2_specifications/frontend/FE2_DRPG_VIEWPORT_RENDERER.md`](../2_specifications/frontend/FE2_DRPG_VIEWPORT_RENDERER.md)
  - [`docs/2_specifications/frontend/FE3_BATTLE_ARENA_AND_FX.md`](../2_specifications/frontend/FE3_BATTLE_ARENA_AND_FX.md)
  - [`docs/2_specifications/frontend/FE4_TOWN_HUB_AND_ACTION_BAR.md`](../2_specifications/frontend/FE4_TOWN_HUB_AND_ACTION_BAR.md)
  - [`docs/2_specifications/frontend/FE5_PARTY_EQUIPMENT_AND_MODALS.md`](../2_specifications/frontend/FE5_PARTY_EQUIPMENT_AND_MODALS.md)
  - [`docs/2_specifications/frontend/FE6_CSS_TOKENS_AND_THEMES.md`](../2_specifications/frontend/FE6_CSS_TOKENS_AND_THEMES.md)
  - [`docs/2_specifications/frontend/FRONTEND_LAYOUT_GUIDE.md`](../2_specifications/frontend/FRONTEND_LAYOUT_GUIDE.md)
  - [`docs/2_specifications/frontend/UI_PAGE_STRUCTURE.md`](../2_specifications/frontend/UI_PAGE_STRUCTURE.md)
- **對齊步驟**：
  1. **`FE1` & 佈局指南**：對齊 `state-store.js` 真實狀態鍵名（`mode, lastTown, lastDungeon, lastParty, lastBattle`）與 `subscribe(key, handler)` 雙參數 API；對齊真實 DOM 容器 ID（`#log`、營地容器 ID）與真實 `data-action` 清單；移除末章殘留之「音效資源／Web Audio」描述（對齊裁決五）。
  2. **`FE2` & `FE3`**：將尚未實作之「第一人稱 3 步透視畫布 `#viewport-canvas`」、「敵方常駐 ATB 進度條」、「Buff 環形倒數光圈」與「30 節點循環 DOM Pool」移至或標註為 `【待深入討論／長線演進藍圖】`，正文如實記錄現行 2D 迷霧網格 `#drpg-grid` + `#drpg-forward-inspect`、施法詠唱條 `.enemy-cast-track` 與 `MAX_ALIVE_TEXTS = 24` 飄字機制；補齊 `static/js/fx/*` 四檔案代表清單。
  3. **`FE4` & `FE5`**：對齊 `ALIGN2-06` 完工後之真實營地卡牌結構與動作名稱；修正探索與戰鬥快捷鍵表對齊 `app.js`（`L` 觀察、`T` 火把／鎖定、戰鬥 `1~5` 選隊員、`Space` 開戰、`U` 陣法奧義）；補齊 `static/js/modals/` 下全部 10 個彈窗／分頁模組與真實 `export` 函式名，將陣法 UI 描述更新為典籍卡牌＋5×3 戰陣盤。
  4. **`FE6` & `UI_PAGE_STRUCTURE`**：將 `:root` 色譜變數與代表 CSS 類別更新為 `style-01` ~ `style-07` 真實名稱（如 `--bg-color`、`.game-container`、`.ftext`、`.arena-shake`），如實說明現行 `@media` 斷點位置與字級規範現況。
- **驗收標準**：前端 8 份規格所列之模組路徑、`export` 函式、DOM ID、快捷鍵與 CSS Token 100% 可在 `src/main/resources/static/` 查證對應；`.\scripts\check-doc-links.ps1` 全綠。

---

## 🟡 五、Phase 4：P2 L3 任務清單與 L4 衝刺看板全域同步收尾

### - [ ] Task ALIGN2-11 (L3-TASK-LISTS-SYNC): 同步 L3 全模組任務清單與總表
- **目標檔案**：
  - [`docs/3_task_lists/README.md`](README.md)
  - [`docs/3_task_lists/FUTURE_IMPROVEMENTS.md`](FUTURE_IMPROVEMENTS.md)
  - [`docs/3_task_lists/backend/M3_1_BUFF_AND_STATUS_TASKS.md`](backend/M3_1_BUFF_AND_STATUS_TASKS.md)
  - [`docs/3_task_lists/backend/M5_SKILL_AND_PROGRESSION_TASKS.md`](backend/M5_SKILL_AND_PROGRESSION_TASKS.md)
  - [`docs/3_task_lists/backend/M6_ITEM_AND_ECONOMY_TASKS.md`](backend/M6_ITEM_AND_ECONOMY_TASKS.md)
  - [`docs/3_task_lists/backend/M8_SAVE_AND_PERSISTENCE_TASKS.md`](backend/M8_SAVE_AND_PERSISTENCE_TASKS.md)
  - [`docs/3_task_lists/data_schemas/ITEM_SCHEMA_TASKS.md`](data_schemas/ITEM_SCHEMA_TASKS.md)
  - [`docs/3_task_lists/data_schemas/SKILL_SCHEMA_TASKS.md`](data_schemas/SKILL_SCHEMA_TASKS.md)
  - [`docs/3_task_lists/frontend/FE4_TOWN_HUB_TASKS.md`](frontend/FE4_TOWN_HUB_TASKS.md)
  - [`docs/3_task_lists/frontend/FE5_PARTY_EQUIPMENT_TASKS.md`](frontend/FE5_PARTY_EQUIPMENT_TASKS.md)
- **對齊步驟**：
  1. 在 `docs/3_task_lists/README.md` L56 移除不存在的 `synergy_combos.json` 引用，更新全域測試基準說明（`403+` 項測試，以 `.\test.ps1` 實跑為準），並校準 R1 追溯列用詞（「千星聯合科技」及序章原型定位）。
  2. 在 `M5_SKILL_AND_PROGRESSION_TASKS.md` 與 `SKILL_SCHEMA_TASKS.md` 將已廢除之 `CyberSchool` / `cyberSchool` 更新為 `OriginSchool` + 17 專精樹 `treeId`，同步 `SKILL-SCHEMA-3` 實際剩餘範圍（被動道骨與高階終結技）。
  3. 補齊各模組清單遺漏之核心 SSoT 檔案指針（`M6` / `ITEM_SCHEMA` 補列 `ItemAffixService.java` 與 `CyberSkillAndItemAffixTest.java`，移除失效 `speed` 描述；`M3_1` 補列 `BuffSettlementService.java`、`ActiveBuff.java` 並更新單測計數；校準 `FE4`、`FE5` 與 `M8-1` 真實完成狀態）。
  4. 更新 `FUTURE_IMPROVEMENTS.md`：將已完工之路徑 M、路徑 Y、`COMBAT-REACT-01`、`FE-02` 標為已完成（並移除與 `ACTIVE_TASKS.md` 競合之 `[ ]` 核取方塊），刪除已否決的獨立「基因補完」進度條字眼。
- **驗收標準**：L3 全數 22+ 份檔案無幽靈路徑、無過期枚舉、無雙頭競爭待辦；`.\scripts\check-doc-links.ps1` 全綠。

---

### - [ ] Task ALIGN2-12 (L4-SPRINT-BOARD-SYNC): 校準 L4 衝刺看板（`ACTIVE_TASKS.md`、`SPRINT_GOAL.md`）真實剩餘範圍與 v3.0 定位
- **目標檔案**：
  - [`docs/4_active_tasks/ACTIVE_TASKS.md`](../4_active_tasks/ACTIVE_TASKS.md)
  - [`docs/4_active_tasks/SPRINT_GOAL.md`](../4_active_tasks/SPRINT_GOAL.md)
- **對齊步驟**：
  1. **同步 L1 v3.0 與主角開局 `【待深入討論】` 聲明**：在 `ACTIVE_TASKS.md` 與 `SPRINT_GOAL.md` 明確加註「遊戲玩家主角 ≠ 小說參考主角，玩家正式出身與第一章劇情列為 `【待深入討論】`；現行 `Milestone VS-01` 為垂直切片機制驗證基準」；將 `ACTIVE_TASKS.md` L221 `Milestone VS-03` 舊設定「千星殖民巨艦墜毀核心・虛數靈能流派」修正對齊 v3.0（如千星聯合科技「銜尾蛇號」／古代工程師深井核心與現行四大淵源專精樹），並修正 `SPRINT_GOAL.md` L45 舊技能名稱為現行 `party_skills.json` 真實名稱。
  2. **校準 `Milestone VS-01` 四項微任務之真實狀態、檔案指針與 SSoT 規範**：
     - `Task 1 (VS-01-LORE-GEOLOGY)`：核對 `mozhu_mines_b1f.json` 已落地內容，標明已完成部分與僅剩之進場第一人稱敘事驗收（若已全數滿足 DoD 則實證勾選並同步 `M2-1` / `DUNGEON-SCHEMA-1`）。
     - `Task 2 (VS-01-AGENT-HUD)`：修正錯誤檔案指針（移除不存在的 `party-overview-tab.js`，改指真實 `src/main/resources/static/js/modals/party-equipment-tab.js` 與 `party-modal.js`），明確聚焦剩餘缺口（隊長初始 5 點自由潛能點、HUD 特工晶片加載進度條顯示）。
     - `Task 3 (VS-01-CYBER-SKILLS)`：移除硬編碼抄寫之具體削韌數值與未實作效果宣稱（改為純 Canonical ID 指針引用，遵守守護鐵律 §1），或補齊 `combo_chitin_kinetic_superconductor` 對應整合測試。
     - `Task 4 (VS-01-LOOT-CHIP)`：反映 `boss_song_tianheng` 必掉晶片與 `ItemAffixService` 已落地之現況，聚焦剩餘之首領擊殺掉落與 `victory-modal.js` 金色傳說翻牌端到端驗收測試。
- **驗收標準**：`ACTIVE_TASKS.md` 與 `SPRINT_GOAL.md` 100% 符合 [`.agents/rules/documentation_alignment_guardrails.md`](../../.agents/rules/documentation_alignment_guardrails.md) 提交前自檢清單五項檢核；`.\scripts\check-doc-links.ps1` 與 `.\test.ps1` 全數通過。
