# 🎓 《深淵仙途》四層級文件對齊：指導員完整主控計劃與闖關檢核表

> **文檔層級**：Level 3 — 架構治理主控計劃與指導員簽核檢查表（Master Plan & Instructor Gate Checklist）  
> **跨電腦 Git 同步設計**：本檔存放於 Git 追蹤之 `docs/3_task_lists/`，不依賴本機 `%USERPROFILE%\.gemini\` 暫存目錄。無論在**公司電腦**或**家裡電腦**，只要執行 `git pull`，指導員 AI 讀取本檔即可 100% 掌握背景裁決、當前闖關進度與驗收標準。  
> **關聯執行組件**：
> - 🧠 **指導員自動喚醒技能（`gemini 3.8` 禁讀）**：[`SKILL.md`](../../.agents/skills/docs-alignment-instructor/SKILL.md)
> - 🤖 **`gemini 3.8` 執行任務卡**：[`DOCS_ALIGNMENT_ACTIVE_TASKS.md`](../4_active_tasks/DOCS_ALIGNMENT_ACTIVE_TASKS.md)
> - 🛡️ **指導員自動驗收閘門腳本**：[`verify-doc-alignment-gates.ps1`](../../scripts/verify-doc-alignment-gates.ps1)（快照目錄：`scripts/.gate_snapshots/`）
> - 📑 **全量審查問題溯源報告**：[`docs_hierarchy_review_report.md`](../0_references/docs_hierarchy_review_report.md)

---

## 🔄 一、公司 ↔ 家裡跨電腦無縫交接協定（Cross-PC Handover SOP）

1. **為什麼指導員主控檔放在 `docs/3_task_lists/`？**
   - 依據 [`documentation_alignment_guardrails.md`](../../.agents/rules/documentation_alignment_guardrails.md) §1，`docs/3_task_lists/` 專職承載「宏觀路線圖與架構治理清單」（與既有 [`CODE_CONVERGENCE_AND_REFACTORING_TASKS.md`](./CODE_CONVERGENCE_AND_REFACTORING_TASKS.md) 同級）。
   - 將「執行步驟卡」放 `docs/4_active_tasks/` 給 `gemini 3.8` 專注執行、將「主控計劃＋檢核表＋自動腳本」放 `docs/3_task_lists/` 與 `scripts/` 給指導員控場，既避免單檔超過 350 行紅線，也能防止執行模型看到驗收正規表達式而「湊字作弊」。
2. **換電腦時的標準交接步驟**：
   - **離開當前電腦前**：指導員完成當前關卡簽核並更新本檔下方 `Gate` 勾選框 `[x]` 後，將變更 `git add .` → `git commit`（繁體中文訊息）→ `git push`。
   - **抵達另一台電腦後**：先執行 `git pull`，直接對指導員輸入 **「下一步要做什麼」** 即可（已內建專屬按需載入技能 [`docs-alignment-instructor`](../../.agents/skills/docs-alignment-instructor/SKILL.md)，指導員會自動讀取本檔與檢核腳本接手進度，而執行模型不會載入該技能）。
3. **指導員單步控場迴圈（每關必守 4 步）**：
   - **① 單步派工**：每次只授權 `gemini 3.8` 執行**單一 Step**（嚴禁一次派多步）。
   - **② 腳本硬檢核**：待 `gemini 3.8` 回報停機後，指導員在 `practice_mud` 根目錄執行：
     ```powershell
     powershell -ExecutionPolicy Bypass -File .\scripts\verify-doc-alignment-gates.ps1 -Step <N> -RecordSnapshot
     ```
   - **③ 語意人工抽檢**：指導員用 `git diff` 依下方第 四 節各關清單抽查語意（確認無刪減有效規格、無假湊關鍵字、無動到指導員腳本 `git diff -- scripts/verify-doc-alignment-gates.ps1`）。
   - **④ 簽核或退回**：全數通過才將本檔對應 Gate 改為 `[x]` 並下達 `Step N+1` 派工令；若有任一缺失，列出具體檔案與修正要求退回 `gemini 3.8` 重修本關。

---

## 📌 二、使用者拍板之 6 大設計裁決與不可篡改基準

### 2.1 六大核心裁決（指導員語意審查權威依據）
1. **D1（PC 桌機與手機版面）**：列為低優先延後專題討論。將現行 [`index.html`](../../src/main/resources/static/index.html) 的「**左右雙主區（左主舞台切換＋左下 5 人小隊卡；右全高可摺疊日誌，支援 `⮃ 換邊`）**」標註為 `【現行已驗證】`；將 [`UI_UX_REQUIREMENTS.md`](../1_requirements/UI_UX_REQUIREMENTS.md) 的「**PC 上三欄＋底日誌**」與「**手機直向底部 5 分頁常駐 Tab Bar**」標註為 `【待深入討論／已決議待實作】`。清除明顯筆誤（`FE2` 左右寫反、`FE4` 左側日誌、`FE6` 四方出口 `.exit-btn`），並於 [`FUTURE_IMPROVEMENTS.md`](./FUTURE_IMPROVEMENTS.md) 與 `FE4_TASKS` 立項追蹤。
2. **D2（SAN 精神護盾規則）**：嚴格貫徹 L1 Option A——僅保留 `SAN = 0` 時觸發單一輕度 Debuff `【精神萎靡】`（傷害 `-15%`，枚舉 `WEAKENED`）。移除 `M4` 的 `SAN <= 25 (-10%)` 多餘階層、清理 `M3` / `FE5` 舊門檻，並刪除 `MODULES_MAP` 與 `Task M2-2` 的「低 SAN 幻覺／偽裝寶箱怪」。
3. **D3（破防數值與 Boss/菁英限定範疇）**：以 [`M3_1_BUFF_AND_STATUS_EFFECTS.md`](../2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md) 為準——**僅限 Boss 與特定菁英怪具備護體罡氣減傷（未破防僅受 `25%~33%` 傷害，實作 `1/3`；破防持續 `6~8 秒`，實作 `7 秒 / 14 Ticks`，期間恢復 `100%` 受擊並享 `1.5x` 暴擊）**；普通雜兵（`NORMAL`）無護體減傷。針對現行 [`BattleEnemy.java`](../../src/main/java/com/example/htmlmud/combat/BattleEnemy.java) 普通怪仍預設 `poise=30` 折減 `1/3` 傷害的代碼落差，於 `M3_COMBAT_ENGINE_TASKS.md` 新增 `Task M3-3 (POISE-RANK-GATE)` 追蹤（本次不動 Java 代碼）。
4. **D4（技能契約統一與孤兒/違憲任務清理）**：
   - **4-1**：[`SKILL_SCHEMA.md`](../2_specifications/data_schemas/SKILL_SCHEMA.md) 的 `treeId` 對齊 `TREE_01 ~ TREE_17` 完整名（如 `tree_time_overclock`），合體技與領域對齊 [`SYNERGY_AND_COMBOS.md`](../2_specifications/skills/SYNERGY_AND_COMBOS.md)（`combo_water_fire_taiji` 倍率 `2.5`、削韌 `60`，註記 `party_skills.json` 主鍵 `combo_water_fire_taichi` 以 `aliases` 相容；`domain_quad_celestial_blade` 消耗 `100`）。
   - **4-2**：`TREE_01 ~ TREE_17` 表格第 7 欄表頭由 `連攜角色` 改為 `戰術／連攜定位`（修 `TREE_09` 錯字 `前討` → `前搖`），並在 `SKILL_SCHEMA.md` 明定僅 `PRIMER`/`DETONATOR` 填 `comboRole`，其餘填 `tags`（`comboRole` 填 `NONE`）。
   - **4-3**：刪除 `M5_TASKS` 舊重疊 `Task M5-1 (32+ 賽博天賦樹)`，更新 `M5_TASKS` 與 `SKILL_SCHEMA_TASKS` 頁首為 17 專精樹架構。
   - **4-4**：刪除無規格依據之 `Task M3-2 (兵刃換彈)`，並刪除違反 [`GEMINI.md`](../../GEMINI.md) 紅線之 `M7` §6.1 與 `Task M7-1 (動態熱重載)`。
5. **D5（主角強殖基因覺醒定位）**：正式併入第 14 樹 [`TREE_14_CHITIN_BLADE.md`](../2_specifications/skills/4_cthulhu/TREE_14_CHITIN_BLADE.md) 與主角【萬象核心插槽 (`Core Socket`)】體系，無獨立第二條基因進度條。
6. **D6（看門狗腳本三重升級）**：升級 [`check-doc-links.ps1`](../../scripts/check-doc-links.ps1)，涵蓋根目錄 `GEMINI.md` 連結、反引號實體路徑存在性校驗、`data_schemas/*.md` JSON 區塊語法驗證。

### 2.2 不可篡改保護鎖跨平台雜湊基準（LF 正規化 SHA-256）
- [`documentation_alignment_guardrails.md`](../../.agents/rules/documentation_alignment_guardrails.md) 全文：`72BA889D4C5D7E40925C41827D129952F99CDA48068C951393A26F5FC8C8CE2B`
- [`GEMINI.md`](../../GEMINI.md) 第 21~37 行「第 7 條第一紅線保護鎖」區段：`A8E94EE97003F8A1148B0EEFB73C232B12061C5A9D0F69E7AB5042624371CBD4`
- *註：檢核腳本在計算 SHA-256 前會先將 `\r\n` 正規化為 `\n`，確保公司與家裡電腦即使 Git `core.autocrlf` 設定不同亦 100% 相容。*

---

## 📊 三、闖關簽核進度總表（Master Gate Progress Tracker）

> 💡 **進度同步規則**：每通過一關，指導員請即時將下方對應關卡更新為 `[x]` 並記錄日期與檢核備註，隨 Git 同步至另一台電腦。

- [x] **Gate 0（Pre-Flight）**：雙檔拆分就緒、審查報告相對路徑化、全域保護鎖基準驗證通過（2026-10-29）
- [ ] **Gate 1（Step 1）**：升級 [`check-doc-links.ps1`](../../scripts/check-doc-links.ps1) 三重檢查並通過 TDD 紅燈基準測試
- [ ] **Gate 2（Step 2）**：Level 1 需求層（3 檔）與全域導覽 [`docs/README.md`](../README.md) 校準完成
- [ ] **Gate 3（Step 3）**：Level 2 資料架構（`ITEM_SCHEMA`、`SKILL_SCHEMA`）與 `TREE_01 ~ TREE_17`（17 檔）契約統一
- [ ] **Gate 4（Step 4）**：Level 2 頂層架構（`ARCHITECTURE_SUMMARY`、`MODULES_MAP`）與後端 `M1 ~ M9`（10 檔）校準完成
- [ ] **Gate 5（Step 5）**：Level 2 前端規格手冊（`FRONTEND_LAYOUT_GUIDE`、`UI_PAGE_STRUCTURE`、`FE1 ~ FE6` 共 8 檔）校準完成
- [ ] **Gate 6（Step 6）**：Level 3 任務清單與總覽看板（`3_task_lists/` 下 22 份業務清單）1:1 收斂完成
- [ ] **Gate 7（Step 7）**：Level 4 看板、[`GEMINI.md`](../../GEMINI.md) 斷鏈修復、[`CHANGELOG.md`](../../CHANGELOG.md) 登載與全專案看門狗 100% 綠燈總驗收
- [ ] **收尾封存（Post-Gate）**：將本主控計劃與 [`DOCS_ALIGNMENT_ACTIVE_TASKS.md`](../4_active_tasks/DOCS_ALIGNMENT_ACTIVE_TASKS.md) 移入 `docs/0_archive/alignment_history/`，並清除 `scripts/.gate_snapshots/`

---

## 🔍 四、逐關詳細檢核表與語意審查標準（Gate 1 ~ Gate 7 Checklists）

### 🛡️ Gate 1 檢核清單（對應 Step 1：看門狗腳本升級）
- **授權修改範圍**：僅限 [`scripts/check-doc-links.ps1`](../../scripts/check-doc-links.ps1)（1 檔）。
- [ ] **自動閘門**：`.\scripts\verify-doc-alignment-gates.ps1 -Step 1 -RecordSnapshot` 回傳 `ExitCode = 0`。
- [ ] **檢查 A 語意審查**：掃描涵蓋 `docs/**/*.md`（排除 `0_archive`、`0_references`）與根目錄 `GEMINI.md`，且已先濾除單行反引號內文字再比對連結。
- [ ] **檢查 B 語意審查**：正確解析 `src/`、`static/`、`js/`、`css/`、`data/`、`scripts/` 實體路徑，排除 `0_archive`、`0_references` 與 `DOCS_ALIGNMENT_*.md`，且豁免四級規劃/除役標籤所在行。
- [ ] **檢查 C 語意審查**：對 `docs/2_specifications/data_schemas/*.md` 所有 JSON 區塊執行 `ConvertFrom-Json` 並精準抓出目前 `ITEM_SCHEMA.md` 漏逗號錯誤（TDD 紅燈必現）。
- [ ] **編碼防呆**：PowerShell 5.1 執行無亂碼解析錯誤（ASCII 訊息或 UTF-8 BOM）。

### 🧭 Gate 2 檢核清單（對應 Step 2：L1 需求層與 `docs/README.md`）
- **授權新增修改範圍**：[`WORLD_AND_LORE_BIBLE.md`](../1_requirements/WORLD_AND_LORE_BIBLE.md)、[`GAMEPLAY_CORE_LOOPS.md`](../1_requirements/GAMEPLAY_CORE_LOOPS.md)、[`UI_UX_REQUIREMENTS.md`](../1_requirements/UI_UX_REQUIREMENTS.md)、[`docs/README.md`](../README.md)。
- [ ] **自動閘門**：`.\scripts\verify-doc-alignment-gates.ps1 -Step 2 -RecordSnapshot` 回傳 `ExitCode = 0`。
- [ ] **L1 純淨度與裁決核對**：`GAMEPLAY_CORE_LOOPS.md` 已無具體 Java 類別名，已寫入 Boss/菁英限定護體減傷（D3）、強殖基因併入第 14 樹與萬象核心插槽（D5）、五維屬性鍵對照（`CON`、`ESS/INT`、`SPI/WIS`、`STR`、`DEX`）。
- [ ] **版面過渡與導覽**：`UI_UX_REQUIREMENTS.md` §2/§3 已標 `【待深入討論／演進目標】` 並移除 `.torch-darkness-vignette`；`docs/README.md` 已補入 `skills/` 19 檔與 `CODE_CONVERGENCE_AND_REFACTORING_TASKS.md`。

### 📜 Gate 3 檢核清單（對應 Step 3：Data Schemas 與 17 專精技能樹）
- **授權新增修改範圍**：`docs/2_specifications/data_schemas/*.md`、`docs/2_specifications/skills/**/*.md`。
- [ ] **自動閘門**：`.\scripts\verify-doc-alignment-gates.ps1 -Step 3 -RecordSnapshot` 回傳 `ExitCode = 0`（含 9 個 JSON 區塊 `ConvertFrom-Json` 全綠）。
- [ ] **契約對齊核對**：`SKILL_SCHEMA.md` 簡寫 `treeId` 已清零，合體技 `combo_water_fire_taiji`（`2.5x` / `poiseDamage: 60` + `taichi` 別名說明）與 `domain_quad_celestial_blade`（消耗 `100`）對齊 `SYNERGY_AND_COMBOS.md`；`comboRole` 與 `tags` 映射規則明確。
- [ ] **17 樹批次核對**：`TREE_01 ~ TREE_17` 第 7 欄表頭全改為 `戰術／連攜定位`，`TREE_09` `前討` 已改為 `前搖`，表格列數與技能數值無誤刪。

### ⚙️ Gate 4 檢核清單（對應 Step 4：頂層架構與後端 `M1 ~ M9`）
- **授權新增修改範圍**：[`ARCHITECTURE_SUMMARY.md`](../2_specifications/ARCHITECTURE_SUMMARY.md)、[`MODULES_MAP.md`](../2_specifications/MODULES_MAP.md)、`docs/2_specifications/backend/*.md`。
- [ ] **自動閘門**：`.\scripts\verify-doc-alignment-gates.ps1 -Step 4 -RecordSnapshot` 回傳 `ExitCode = 0`。
- [ ] **關鍵規格逐項抽檢**：
  - `M1` 頁首與 Mermaid 圖對齊 6 大仙俠設施；`M2` `dangerRate` 為 `15` 且章節號修復；
  - `M3` 與 `M3_1` 破防統一為 Boss/菁英限定未破防 `25%~33%`（`1/3`）受擊、破防 `6~8 秒`（`7 秒`）且附 `BattleEnemy.java` 待收斂註記（D3）；
  - `M3`、`M4`、`MODULES_MAP` 完全符合 SAN Option A（無 `<=25`、無 `<30%` 畸變、無偽裝寶箱怪）（D2）；
  - `M5` 載明強殖基因併入 `tree_chitin_blade`（D5）；`M6` 移除「全服消耗量」；`M7` 刪除 `WatchService` 熱重載（D4-4）；`M8` 改為 `autosave.json`；`M9` 改為 `"TOWN"` 與真實 `pipeline.*` 類別；全體 Tick 統一為 `500ms`。

### 🎨 Gate 5 檢核清單（對應 Step 5：前端 8 份規格手冊）
- **授權新增修改範圍**：`docs/2_specifications/frontend/*.md`（8 檔）。
- [ ] **自動閘門**：`.\scripts\verify-doc-alignment-gates.ps1 -Step 5 -RecordSnapshot` 回傳 `ExitCode = 0`。
- [ ] **版面與路徑核對**：`FRONTEND_LAYOUT_GUIDE` 與 `UI_PAGE_STRUCTURE` 清楚區分 `【現行已驗證】` 左右雙主區與 `【待深入討論／已決議待實作】` 三欄＋手機 5 Tab Bar，且修正 `40 格公共行囊`、移除文字指令；`FE1`~`FE6` 無虛構 JS/CSS 路徑、`FE5` 對齊 SAN=0、`FE6` 移除 `.exit-btn` 並引用 `style-07-battle-fx.css`。

### 📋 Gate 6 檢核清單（對應 Step 6：Level 3 任務清單收斂）
- **授權新增修改範圍**：`docs/3_task_lists/**/*.md`（**嚴禁 `gemini 3.8` 修改本檔 `DOCS_ALIGNMENT_MASTER_PLAN.md`**）。
- [ ] **自動閘門**：`.\scripts\verify-doc-alignment-gates.ps1 -Step 6 -RecordSnapshot` 回傳 `ExitCode = 0`。
- [ ] **任務增刪與雙向同步核對**：
  - 已刪除：`M2_TASKS` `Task M2-2`、`M3_TASKS` `Task M3-2`、`M5_TASKS` `Task M5-1`、`M7_TASKS` `Task M7-1`；
  - 已勾選 `[x]`：`M3_TASKS` `Task M3-1`、`SKILL_SCHEMA_TASKS` `Task SKILL-SCHEMA-1`、`M8_TASKS` `Task M8-2`；
  - 已新增：`M3_TASKS` `Task M3-3 (POISE-RANK-GATE)`、`M3_1_TASKS` 救援/DR/`valueFormula`、`M6_TASKS` 裝備數值強化 `UPGRADE`、`FE4_TASKS` 與 `FUTURE_IMPROVEMENTS` 的版面討論項（`UI-LAYOUT-DISCUSS`）；
  - `FE1 ~ FE6` 清單與 [`3_task_lists/README.md`](./README.md) 真實 CSS/JS 路徑全數正確，`FUTURE_IMPROVEMENTS.md` 無懸空 `§` 章節或重複衝突狀態。

### 🎯 Gate 7 終極總驗收清單（對應 Step 7：L4、`GEMINI.md`、`CHANGELOG.md`）
- **授權新增修改範圍**：[`SPRINT_GOAL.md`](../4_active_tasks/SPRINT_GOAL.md)、[`ACTIVE_TASKS.md`](../4_active_tasks/ACTIVE_TASKS.md)、[`GEMINI.md`](../../GEMINI.md)、[`CHANGELOG.md`](../../CHANGELOG.md)。
- [ ] **自動閘門與看門狗雙綠燈**：`.\scripts\verify-doc-alignment-gates.ps1 -Step 7 -RecordSnapshot` 回傳 `ExitCode = 0`（內含升級版 `check-doc-links.ps1` 全專案 0 斷鏈、0 虛構路徑、0 JSON 語法錯誤）。
- [ ] **`GEMINI.md` 最小差異鐵律**：執行 `git diff -- GEMINI.md` 人工逐行確認**僅第 5、50、58 行連結與第 160~162 行測試基準變更**，第 21~37 行保護鎖一字未動。
- [ ] **L4 與變更日誌核對**：`SPRINT_GOAL.md` 對齊 17 專精樹、萬象核心插槽與 Option C 分階前提；`ACTIVE_TASKS.md` 測試基準表述統一；`CHANGELOG.md` 最上方已新增 `2026-10-29` 繁體中文完整紀錄。

