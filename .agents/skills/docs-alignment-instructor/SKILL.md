---
name: docs-alignment-instructor
description: >-
  【僅限指導員模式・執行模型嚴禁讀取或啟用】當使用者詢問「下一步要做什麼」、「下一步」、「目前進度」、「請驗收」或要求接手指導員角色時，必須立即讀取本技能以執行《深淵仙途》四層級文件對齊闖關控場。注意：若使用者在提示詞中明確要求「讀取 DOCS_ALIGNMENT_ACTIVE_TASKS.md 並執行 Step N」，代表當前對話為執行模型（gemini 3.8），絕對禁止讀取或啟用本技能！
---

# 🎓 四層級文件對齊：指導員（Instructor）自動接手與單步控場手順

當使用者輸入 **「下一步要做什麼」**（或詢問進度、要求驗收）時，請自動依序執行以下標準程序，無需讓使用者重貼背景說明：

## 1. 定位專案根目錄與載入主控狀態
1. 確認當前目錄是否為 Git 倉庫根目錄 `practice_mud`（若工作區位於外層目錄如 `my_practice`，執行指令時請先 `Set-Location practice_mud`）。
2. 讀取以下兩份檔案掌握全局進度與步驟定義：
   - 指導員主控計劃與檢查表：[DOCS_ALIGNMENT_MASTER_PLAN.md](../../../docs/3_task_lists/DOCS_ALIGNMENT_MASTER_PLAN.md)
   - `gemini 3.8` 執行任務卡：[DOCS_ALIGNMENT_ACTIVE_TASKS.md](../../../docs/4_active_tasks/DOCS_ALIGNMENT_ACTIVE_TASKS.md)
3. 在 `practice_mud` 根目錄執行 `git status --porcelain -u` 與前置檢查：
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\verify-doc-alignment-gates.ps1 -Step 0
   ```

## 2. 智慧判斷當前情境並回應「下一步要做什麼」

對照 `DOCS_ALIGNMENT_MASTER_PLAN.md`「三、闖關簽核進度總表」中第一個尚未勾選 `[ ]` 的關卡 `Gate N`（對應 `Step N`），以及 `git status` 的變更檔案：

### 情境 A：發現 `Step N` 的目標檔案已有變更、但 `Gate N` 尚未勾選 `[x]`
代表 `gemini 3.8` 剛執行完 `Step N` 並停機等待指導員驗收！請立即自動執行驗收：
1. 執行該關卡自動驗收並封存快照：
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\verify-doc-alignment-gates.ps1 -Step <N> -RecordSnapshot
   ```
2. 執行 `git diff` 對照 `DOCS_ALIGNMENT_MASTER_PLAN.md` 第四節 `Gate N` 檢核清單進行人工語意抽查（確認未誤刪有效規格、未偷改 `scripts/verify-doc-alignment-gates.ps1`）。
3. **分流處置**：
   - **若驗收失敗**：列出具體缺失檔案與原因，並直接產出一段**「退回 `gemini 3.8` 修正 `Step N` 的指令」**供使用者複製貼上。
   - **若驗收 100% 通過**：立即編輯 `docs/3_task_lists/DOCS_ALIGNMENT_MASTER_PLAN.md` 將 `Gate N` 與其子項勾選為 `[x]`，向使用者回報驗收通過，並直接提供**「派發給 `gemini 3.8` 執行 `Step N+1` 的標準指令」**（附帶若需換電腦下班時的繁體中文 `git commit` 建議指令）。

### 情境 B：工作區尚無 `Step N` 的業務文件變更（例如剛開工，或換電腦 `git pull` 後接手）
1. 用 1~2 句話簡報目前簽核進度（例如：「目前已簽核完成至 `Gate K`，接下來準備進行 `Step N`」）。
2. 直接產出以下格式之**單步派工指令**，讓使用者可直接複製貼給 `gemini 3.8`：
   ```text
   請在 practice_mud 目錄下讀取 docs/4_active_tasks/DOCS_ALIGNMENT_ACTIVE_TASKS.md，嚴格遵守頂部「單步停機鎖」與「五條安全紅線」（嚴禁讀取 .agents/skills/ 與 DOCS_ALIGNMENT_MASTER_PLAN.md）。
   現在請你【只執行 Step <N>】。
   完成 Step <N> 後請貼出修改檔案清單與自檢結果，並立即停下等待指導員簽核，嚴禁自行執行 Step <N+1>！
   ```

### 情境 C：`Gate 1 ~ Gate 8` 已全數勾選 `[x]`
主動提醒使用者進入「收尾封存（Post-Gate）」步驟：在獲得確認後，將 `DOCS_ALIGNMENT_MASTER_PLAN.md` 與 `DOCS_ALIGNMENT_ACTIVE_TASKS.md` 歸檔至 `docs/0_archive/alignment_history/`、清除 `scripts/.gate_snapshots/`，恢復 `docs/4_active_tasks/ACTIVE_TASKS.md` 常態開發。

