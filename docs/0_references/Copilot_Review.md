# Copilot 專案程式碼與 Level 3 任務清單對齊審查

> **審查日期：**2026-11-08（Asia/Taipei）  
> **審查範圍：**以目前工作區為準，盤點 `docs/3_task_lists/` 全部 21 份 Markdown，交叉核對其程式、測試、前端資源與 JSON 引用；並核對專案根目錄 README 的文件導覽與測試基準。  
> **掃描規模：**296 個生產 Java 檔、95 個測試 Java 檔、29 個前端 JS、7 個 CSS、125 個資料 JSON。  
> **判定原則：**未勾選項目及明確標為「待深入討論／長線演進藍圖」者視為待辦，不當作已交付功能故障；本報告只把實際現況和清單描述不一致之處列為發現。

## 審查結論

整體架構與任務追蹤**大致對齊**。`docs/3_task_lists/` 的程式路徑與 Markdown 連結有效，Schema 範例 JSON 語法正確；目前 Maven 測試套件也全數通過。本輪發現的差異集中在進度／測試數字及部分待辦文字未隨既有實作更新，未發現會阻斷建置或測試的問題。

另確認先前審查提及的 M3.1 友方破防救援規格差異已收斂：目前 L2 與 L3 都描述 3～4 秒力竭、援護瞬間拉滿架勢並解除狀態，因此不再列為發現。

## 發現事項

### 1. M8 總覽完成度與模組清單不一致

- **嚴重度：低（文件治理）**
- [docs/3_task_lists/README.md](../3_task_lists/README.md) 將 M8 列為 **95%**；[M8_SAVE_AND_PERSISTENCE_TASKS.md](../3_task_lists/backend/M8_SAVE_AND_PERSISTENCE_TASKS.md) 標示 **100%**，且其列出的 M8 工作項目均已勾選完成、未列待辦。
- **建議：**若 M8 範圍已完成，將總覽同步為 100%；若仍有 5% 範圍，應在 M8 清單列出對應項目及驗收條件。

### 2. 全專案測試數字過期且互相矛盾

- **嚴重度：低（文件治理）**
- [docs/3_task_lists/README.md](../3_task_lists/README.md) 宣稱 **416+** 項；[M5_SKILL_AND_PROGRESSION_TASKS.md](../3_task_lists/backend/M5_SKILL_AND_PROGRESSION_TASKS.md) 與 [M8_SAVE_AND_PERSISTENCE_TASKS.md](../3_task_lists/backend/M8_SAVE_AND_PERSISTENCE_TASKS.md) 也沿用 416+。
- 專案根目錄 [README.md](../../README.md) 則分別出現 **376 Passed**、**373 項**兩種舊數字。
- 本次執行 `.\test.ps1 test` 成功；Surefire 報告彙總為 **407 項測試、0 失敗、0 錯誤、0 略過（87 個測試套件）**。
- **建議：**將動態測試數字集中於一個可更新來源，或在文件中標示「歷史基準」及日期；避免 README、任務總覽和模組歷史敘述被理解為目前測試數。

### 3. M4-1 將已存在的加點功能仍描述為待實作

- **嚴重度：低（待辦範圍需收斂）**
- [M4_PARTY_AND_FORMATION_TASKS.md](../3_task_lists/backend/M4_PARTY_AND_FORMATION_TASKS.md) 的 M4-1 將後端加點 Action 列為目標；目前 `PartyCommand` 已派發 `stat add` 並呼叫 `XpProgressionService.allocateStatPoint(PartyMember, ...)`，且 [DualTrackPotentialPointsTest.java](../../src/test/java/com/example/htmlmud/DualTrackPotentialPointsTest.java) 覆蓋 Party 指令加點。
- 該任務仍有尚待驗收的範圍，例如隊長初始 5 點、DTO／隊員面板同步及端到端驗收。
- **建議：**保留未完成狀態，但將已具備的加點命令列為現況，待辦限定於初始點數、投影同步和 E2E 驗收。

### 4. M6-1 的首領晶片掉落資料已配置，待辦描述未區分剩餘驗收

- **嚴重度：低（待辦範圍需收斂）**
- [M6_ITEM_AND_ECONOMY_TASKS.md](../3_task_lists/backend/M6_ITEM_AND_ECONOMY_TASKS.md) 將首領保證掉落追蹤晶片列為待辦；目前 [mobs.json](../../src/main/resources/data/zones/mozhu_mines/mobs.json) 的 `boss_song_tianheng` 掉落表已包含 `global:item_chip_qianxing_tracker`，機率為 `1.0`。
- 隨機詞綴結算、獎勵流程及前端結算呈現仍屬需驗收範圍，因此現有證據不足以把整個 M6-1 標為完成。
- **建議：**把已落地的掉落資料標記完成，將待辦改為實際尚缺的掉落流程、詞綴結算與結算視窗端到端驗收，避免重複追蹤已完成的資料配置。

### 5. 字級完成聲明和 CSS 例外的涵蓋範圍不清楚

- **嚴重度：低（文件範圍表述）**
- [FUTURE_IMPROVEMENTS.md](../3_task_lists/FUTURE_IMPROVEMENTS.md) 將 UI-01「字級 ≥13px」標為已完成；[FE6_CSS_TOKENS_TASKS.md](../3_task_lists/frontend/FE6_CSS_TOKENS_TASKS.md) 則說明 13px 是全域基準 Token，歷史緊湊標籤的 9–12px 尚待漸進收斂。實際 [style-01-tokens-and-shell.css](../../src/main/resources/static/css/style-01-tokens-and-shell.css) 與 [style-02-combat-arena.css](../../src/main/resources/static/css/style-02-combat-arena.css) 仍有 11–12px 宣告。
- **建議：**把 UI-01 的完成範圍表述為「建立 13px 基準 Token，特定舊標籤例外另列待收斂」，或明確列出允許小字的元件範圍；目前 FE6 已有例外說明，應同步到總表。

### 6. 根目錄 README 的文件樹列出不存在於 `docs/3_task_lists/` 的檔案

- **嚴重度：低（文件導覽）**
- [README.md](../../README.md) 的目錄樹把 `future_improvements_history.md` 和 `CHANGELOG.md` 列在 `docs/3_task_lists/` 下；目前任務目錄中都沒有這兩個檔案，歷史改善文件位於 `docs/0_archive/`，`CHANGELOG.md` 位於專案根目錄。
- **建議：**修正目錄樹的實際位置，或註明為概念性導覽，避免讀者依圖尋找不存在的路徑。

## 對齊良好的項目

- M1–M9、M3.1、FE1–FE6 與三份 Schema 任務清單均存在；總覽列出的模組與清單完成度除 M8 外一致。
- M9 的結構化 Action Protocol 與 `MudWebSocketHandler` 更名仍標為待辦，現行程式仍使用 `MudWebSocketHandler`，狀態一致。
- M1 NPC 對話樹、M5 技能樹擴充、FE2 Canvas 視角及 FE4 版面提案均明確標為待辦／長期提案；未誤列為已交付。
- M3.1 友方破防救援的 L2 行為定義與 L3 驗收條件目前一致。

## 驗證結果與範圍限制

- `scripts/check-doc-links.ps1`：通過；檢查 **476** 個 Markdown 連結、**130** 個反引號檔案路徑及 **14** 個 Schema JSON 區塊。
- `.\test.ps1 test`：通過；**407 項測試，0 失敗、0 錯誤、0 略過**。
- 本次依完整任務清單、其引用、程式檔案盤點與相關功能路徑進行交叉核對，並執行專案文件檢查及測試；這不等同於對 296 個生產 Java 類別逐行作人工功能審計。測試通過亦不代表所有待辦或所有文件陳述均已由自動化測試驗證。

## 建議處理順序

1. 同步 M8 總覽完成度。
2. 統一測試數字及其更新方式。
3. 收斂 M4-1、M6-1 待辦文字，只保留尚未完成的驗收範圍。
4. 明確界定 13px 字級基準與舊標籤例外。
5. 修正根目錄 README 文件樹中的歷史文件及 Changelog 位置。
