# Copilot 文件與程式一致性審查

> 審查日期：2026-11-07
> 審查範圍：目前工作區 `practice_mud/` 專案；`docs/2_specifications/`（43 份 Markdown）與 `docs/3_task_lists/`（21 份 Markdown），並依文件所指向的 Java、前端資源、JSON 與測試交叉核對。
> 判定原則：以目前檔案為準。依文件中的「已決議待實作」、「待深入討論／長線演進藍圖」等狀態區分已交付功能與待辦；明確列為待辦或未來提案的內容不當作已實作缺陷。

## 審查結論

整體架構對照與文件引用大致相符。本次確認 **1 項規格與任務清單之間的行為定義不一致**：友方隊員破防後的硬直時間，以及隊友救援時的罡氣回復量不同。此項功能在任務清單中明確標示為待實作，因此目前屬於待實作前的規格歧異，不是已交付功能故障。

先前報告提到的 Service Worker／PWA 未實作疑慮，本次重新核對後不列為缺陷：相關內容位於規格的長期演進藍圖／待討論段落。文字指令終端的舊疑慮亦未沿用；FE1 任務描述為移除玩家操作用輸入框、保留開發者終端，與目前前端入口相符。

## 發現事項

### 1. 友方破防救援的硬直時間與救援效果定義不一致

- **狀態：**Level 2 規格與 Level 3 待辦內容不一致；功能尚未實作
- **信心度：**高
- **證據：**
  - [M3_1_BUFF_AND_STATUS_EFFECTS.md](../2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md#L138-L143) §2 描述友方角色破防後陷入 **3～4 秒**力竭，隊友援護可**瞬間拉滿架勢條並解除**力竭。
  - [M3_1_BUFF_AND_STATUS_TASKS.md](../3_task_lists/backend/M3_1_BUFF_AND_STATUS_TASKS.md#L42-L43) Task M3_1-2 則要求 **6 秒**硬直，且救援為**回復 50% 罡氣**並中斷硬直。
  - 任務標題帶有「【已決議待實作】」，且仍為未勾選項目；目前原始碼搜尋未找到友方破防救援機制，已知破防測試 [BossPhaseAndPoiseStaggerTest.java](../../src/test/java/com/example/htmlmud/BossPhaseAndPoiseStaggerTest.java#L39-L40) 驗證的是破防暴擊，不是友方救援。
- **建議：**實作前先決定一套權威行為（硬直時間、救援回復比例及解除規則），再同步修改 L2 規格與 L3 任務／驗收標準，避免依據不同文件實作。

## 已核對且不列為缺陷的事項

- FE1 的 Service Worker／離線快取位於 [FE1_CORE_STATE_AND_PIPELINE.md](../2_specifications/frontend/FE1_CORE_STATE_AND_PIPELINE.md#L151-L155) 的未來演進規劃；PWA 位於 [FRONTEND_LAYOUT_GUIDE.md](../2_specifications/frontend/FRONTEND_LAYOUT_GUIDE.md#L209-L215) 的待討論藍圖。程式沒有找到相關實作，但文件狀態清楚，故不視為現行規格與程式不符。
- [FE1_CORE_STATE_TASKS.md](../3_task_lists/frontend/FE1_CORE_STATE_TASKS.md#L26-L27) 說明下架玩家文字輸入框並保留開發者終端；目前 [index.html](../../src/main/resources/static/index.html#L582-L586) 有開發者終端入口，與該任務描述一致。
- M3 的普通怪物護體罡氣階級閘門已由 [M3_COMBAT_ENGINE.md](../2_specifications/backend/M3_COMBAT_ENGINE.md#L83-L89) 標明現行程式仍套用減傷、由 Task M3-3 追蹤修正；這是已揭露的待辦，不是未標示的落差。
- 技能樹擴充、NPC 對話樹、裝備重鑄等功能在相關任務清單中仍標示未完成或長期提案，不應誤判為已交付功能失效。

## 驗證結果

- 文件檢查器 `scripts/check-doc-links.ps1`：通過；檢查 **476** 個 Markdown 連結、**130** 個反引號程式路徑及 **14** 個 Schema JSON 區塊。
- Maven 測試：執行 `.\test.ps1 test`，**416 項測試通過，0 失敗、0 錯誤、0 略過**。
- 測試通過表示現有測試套件成功，不代表所有文件描述都已具備自動化測試；本報告的文件歧異仍需依建議統一。
