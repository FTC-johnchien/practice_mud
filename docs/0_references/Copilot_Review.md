# Copilot 文件與程式一致性審查

> 審查日期：2026-11-02
> 審查範圍：`docs/1_requirements/`、`docs/2_specifications/`、`docs/3_task_lists/` 下全部 68 份 Markdown 文件，以及文件所涉及的程式碼、JSON 與測試。
> 排除原則：`docs/2_specifications/ARCHITECTURAL_DECISIONS.md` 中已明確決議的事項不列為待修問題。
> 判定原則：依文件標示的【現行已驗證】、【已決議待實作】、【候選提案】與【歷史／已除役】區分現況；明確列為未來工作的項目，不當作已實作缺陷。

## 審查結論

已確認兩項需要釐清或補齊的落差：一項是需求與任務清單／現行前端功能對「文字命令輸入」的定位不一致；另一項是前端規格描述了離線快取與斷線指令排隊，但程式現況未見相應實作，且規格未清楚標示其狀態。

除此之外，本次沒有列出缺乏明確程式或文件證據的疑慮。技能樹擴充已在任務清單標為未完成，因此不列為程式與文件不符。

## 發現事項

### 1. 零指令輸入需求與開發者終端現況／任務標示不一致

- **狀態：**規格與實作定位待釐清
- **信心度：**高
- **證據：**
  - [UI_UX_REQUIREMENTS.md](../1_requirements/UI_UX_REQUIREMENTS.md#L14-L15) 要求移除文字命令輸入，改以按鈕、卡片與快捷鍵操作。
  - [FE1_CORE_STATE_TASKS.md](../3_task_lists/frontend/FE1_CORE_STATE_TASKS.md#L26-L27) 將保留開發者終端列為已完成。
  - [index.html](../../src/main/resources/static/index.html#L619) 提供命令輸入框與終端操作入口；[app.js](../../src/main/resources/static/js/app.js#L132) 讀取並送出輸入指令。
- **建議：**先決定終端是否為僅限開發環境的例外。若是，請在需求及任務清單標明開發用途，並確保正式遊戲介面不顯示；若不是，則應修訂需求或移除終端，避免「禁止命令輸入」與「保留命令輸入」並存。

### 2. 離線快取與斷線指令排隊缺少明確狀態標籤

- **狀態：**規格描述了能力，程式未見對應實作；是否為未來提案尚不明
- **信心度：**高
- **證據：**
  - [FE1_CORE_STATE_AND_PIPELINE.md](../2_specifications/frontend/FE1_CORE_STATE_AND_PIPELINE.md#L125-L128) 描述 Service Worker、離線快取及斷線時排隊指令。
  - [websocket.js](../../src/main/resources/static/js/network/websocket.js#L120) 在 WebSocket 未連線時回報失敗，未見指令排隊處理；靜態資源亦未見 Service Worker 註冊或實作。
- **建議：**若這是當前承諾，補上 Service Worker、斷線指令佇列及相應驗收測試；若是候選或遠期構想，請在規格明確標為「候選提案」或「已決議待實作」，並在任務清單建立對應項目。

## 正面觀察

- [M5_SKILL_AND_PROGRESSION_TASKS.md](../3_task_lists/backend/M5_SKILL_AND_PROGRESSION_TASKS.md) 將 17 棵技能樹的擴充列為未完成待辦，與目前功能落地範圍有區隔；不應將尚未完成的內容誤判為已交付功能故障。
- 文件連結檢查通過：464 個 Markdown 連結、76 個程式路徑及 9 個 Schema JSON 區塊均通過檢查。

## 驗證限制

Maven 測試未能執行，原因是目前環境未正確設定 `JAVA_HOME`。因此本報告不宣稱測試套件通過；本次程式一致性判斷以檢視程式碼、JSON、測試與任務狀態為依據。
