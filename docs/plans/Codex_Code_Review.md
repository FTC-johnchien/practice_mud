# Codex Code Review

> 審查日期：2026-10-02  
> 範圍：`GEMINI.md`、`ARCHITECTURE.md`、`docs/UI_PAGE_STRUCTURE.md`、`docs/plans/Copilot_Code_Review.md`、`docs/plans/FUTURE_IMPROVEMENTS.md`，以及專案目前可見的 Java、前端、設定、資料與測試結構。這是以風險為導向的整體審查，並非逐行形式化稽核。

## 優先處理的程式問題

### P1：房間狀態與地面掉落物沒有持久化

[`RoomService.record()`](../../src/main/java/com/example/htmlmud/domain/service/RoomService.java) 會建立 `RoomStateRecord`，但方法內仍只有 TODO，沒有把記錄送入持久化佇列。即使接上佇列，[`RoomPersistenceService.flushBatch()`](../../src/main/java/com/example/htmlmud/infra/persistence/service/RoomPersistenceService.java) 目前也只載入並儲存既有 entity，沒有將 record 的掉落物狀態套用到 entity。因此玩家丟棄或拾取物品後，房間狀態無法依此流程在重啟後還原。

**建議**：定義房間狀態的寫入與清空語意，將不可變 snapshot 交給持久化層；flush 時完整映射欄位，並以房間複合鍵更新或建立資料。驗收需涵蓋新增、更新、清空和重啟後讀回。

### P1：圓桌戰鬥結果區間沒有總機率上限

[`DefenseResolver`](../../src/main/java/com/example/htmlmud/domain/dungeon/battle/DefenseResolver.java) 分別計算 Miss、Dodge、Parry、Block、Crit，再直接累加成邊界；各切片各自有上限，總和卻未限制在 1。當隊員同時裝備閃避、招架被動與盾牌且屬性較高時，`blockLimit` 或 `critLimit` 可能超過 1，後續結果區間便消失，導致普通命中或暴擊永遠抽不到。

**建議**：先決定總和超額時的規則（依優先序分配剩餘機率，或正規化權重），再集中建立互斥且總和為 1 的區間；檢查高屬性、所有防禦同時啟用及骰值邊界。

### P1：Write-behind 持久化佇列無界，關閉期間存在資料競態

[`AbstractAsyncBatchPersistenceService`](../../src/main/java/com/example/htmlmud/infra/persistence/service/AbstractAsyncBatchPersistenceService.java) 使用無界 `LinkedBlockingQueue`，因此 `offer()` 幾乎不會拒絕資料，現有佇列滿載告警不能提供實際背壓。若資料庫長期慢於事件產生速度，佇列可無限增長。`shutdown()` 設定 `running = false` 後直接 drain，卻未停止新的 `saveAsync()`，也未等待 worker 停止；關閉和提交競爭時資料可能漏寫，worker 與 shutdown 也可能同時 flush。

**建議**：使用有界佇列並明確定義滿載策略；關閉時先拒絕新資料、通知 worker 排空並等待完成，再回報關閉成功。flush 失敗需保留或重試批次，避免目前 catch 後清空 batch 的資料遺失風險。

### P1（公開部署前）：WebSocket 建立遊戲 Actor 前未做身分驗證與資源限制

[`MudWebSocketHandler.afterConnectionEstablished()`](../../src/main/java/com/example/htmlmud/infra/server/MudWebSocketHandler.java) 為每個連線建立並啟動 Player Actor；[`WebSocketConfig`](../../src/main/java/com/example/htmlmud/config/WebSocketConfig.java) 設定 Origin 白名單，但 Origin 限制本身不代表已驗證使用者。程式路徑中未見連線配額、訊息大小限制或指令速率限制。若服務可由非本機使用者連線，未授權連線可消耗 Actor 與記憶體資源；多人模式也需要明確的玩家身份與存檔授權邊界。

**建議**：若產品維持本機單人模式，將服務綁定本機介面並明確記載部署限制；對外提供服務前，加入 WebSocket 握手身份驗證、每身份連線配額、訊息大小與指令頻率限制，以及存檔存取授權。

### P1：前端安全完成狀態與實際輸出仍不一致

`FUTURE_IMPROVEMENTS.md` 與 `UI_PAGE_STRUCTURE.md` 將 XSS 整治和禁止 inline handlers 描述為已完成或規範，但 [`party-modal.js`](../../src/main/resources/static/js/modals/party-modal.js) 仍把 Gambit 的 `conditionLabel`、`targetLabel`、`skillName` 直接插入 `innerHTML`；`index.html`、`party-modal.js`、`shop-modal.js`、`skill-drawer.js`、`battle-panel.js` 等也仍有大量 inline `onclick`。其中帶資料的 HTML 拼接若來源未受信任，會有 HTML 注入風險；inline handler 也阻礙採用嚴格 CSP。

**建議**：逐一追蹤插值資料來源，對文字節點使用 `textContent` 或一致的 HTML escaping；把事件改為事件委派，並在完成實際掃描前撤回「已完成」標記。需特別驗證 ANSI 轉 HTML 的輸出是否在轉換前妥善編碼。

## 架構與資料一致性

### P1：模板資料存在靜態全域狀態與建構子 fallback

[`TemplateRepository`](../../src/main/java/com/example/htmlmud/infra/persistence/repository/TemplateRepository.java) 仍維護多組 static map/static 查詢入口；[`PartyService`](../../src/main/java/com/example/htmlmud/domain/party/service/PartyService.java) 仍有 `new TemplateCatalog()` fallback。這會讓 Spring 管理生命週期與測試依賴注入不完整，也使模板快取狀態跨測試或應用上下文共享。這與 `FUTURE_IMPROVEMENTS.md` 所列的 DI 遷移問題一致。

**建議**：以單一注入的 `TemplateReader`/port 取代靜態查詢與無參建構子 fallback；將快取生命週期限定於應用上下文，使用可替換的記憶體實作隔離單元測試。

### P1：文件中的規則和完成狀態互相矛盾

- `GEMINI.md` 明確要求隊伍上限為 5 人，但進度段落仍稱小隊面板支援 6 名隊員；`ARCHITECTURE.md` 的前端樹也寫「6 人小隊 HUD」，正文才寫 5 人。
- `FUTURE_IMPROVEMENTS.md` 的推薦路徑仍把 GameConfig 與 One-Roll 列為未完成，但各自章節已標為完成；部分「待辦」狀態也與 `GEMINI.md` 的近期進度不同。
- `Copilot_Code_Review.md` 以「目前工作樹」為基準，但其中 Java 8 測試失敗、JDK 25 執行未完成等驗證敘述是該報告當時的紀錄，不能視為本次狀態的測試結果。

**建議**：以 `CHANGELOG.md` 記里程碑、以 `FUTURE_IMPROVEMENTS.md` 記未完成工作；清理舊數字與重複完成狀態，並為每份審查報告加上明確時間與當時版本/工作樹註記。

### P2：架構文件的目錄圖和實際原始碼位置未完全同步

`ARCHITECTURE.md` 將 `VirtualActor` 等類別放在舊目錄位置，前端模組樹列出的模組亦與實際 `static/js` 目錄不完全相符。文件同時自稱單一權威來源，過時路徑會讓後續修改依錯誤結構進行。

**建議**：以目前目錄重建目錄樹，僅保留穩定的責任邊界而非逐檔複製清單；可加入簡單文件檢查流程，避免新增或搬移模組後長期失配。

### P2：遊戲資料格式缺少可見的統一啟動期驗證入口

專案有大量 JSON 模板與資料完整性測試，但整體結構仍依賴多種 loader/adapter 在執行時解析。資料欄位缺漏、引用不存在的技能/物品 ID 或互相矛盾的數值，可能要到特定遊戲流程才會顯現。

**建議**：集中建立資料載入驗證報告，在啟動或 CI 階段檢查必要欄位、ID 唯一性、跨檔引用、數值範圍與拓撲引用；錯誤需帶出檔名、JSON 路徑與資料 ID。

## 測試與工程流程建議

- 現有測試套件涵蓋戰鬥、資料、存檔、併發與整合流程，方向良好；優先補足上述持久化、佇列關閉競態、機率區間組合及不可信前端資料案例。
- `pom.xml` 設定 Java 25 與 Spring Boot 4.1.1；本次未執行測試或建置，因此本報告不宣稱專案目前可成功編譯或測試通過。建議在固定 JDK 25 的 CI 執行完整測試並保存 Surefire 摘要，避免不同本機 Java 版本造成結果歧異。
- 報告撰寫時工作樹已有大量修改、刪除與新增檔案（包含被審查文件及程式碼）。本報告按目前可見內容評估，沒有嘗試還原、整理或覆寫那些既有變更。

## 建議執行順序

1. 修正房間狀態持久化及 Write-behind 關閉/失敗時的資料保全。
2. 定義並限制戰鬥圓桌各結果的總機率。
3. 完成前端資料輸出轉義與事件委派，再更新 XSS/CSP 完成狀態。
4. 明確本機單人與公開/多人部署的身份驗證、配額及存檔授權邊界。
5. 清理模板靜態全域存取，收斂到依賴注入的讀取介面。
6. 統一文件中的隊伍容量、路線完成狀態與目錄說明，將高價值未決項目納入 Roadmap。
