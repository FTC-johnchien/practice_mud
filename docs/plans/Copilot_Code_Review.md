# Copilot Code Review

> 審查日期：2026-10-02
> 範圍：`GEMINI.md`、`ARCHITECTURE.md`、`docs/UI_PAGE_STRUCTURE.md`、`docs/plans/FUTURE_IMPROVEMENTS.md`，以及目前工作樹中的 Java、前端與測試程式。以下先列可由程式碼直接確認的問題，再列文件治理與後續改善建議。這是跨模組抽查，不代表已逐行形式化審計所有檔案。

## Findings

### P1：房間掉落物持久化目前沒有實際寫入

[`RoomService.record`](../../src/main/java/com/example/htmlmud/domain/service/RoomService.java) 會建立 `RoomStateRecord`，但方法只有 TODO，沒有將 record 交給 [`RoomPersistenceService`](../../src/main/java/com/example/htmlmud/infra/persistence/service/RoomPersistenceService.java) 或任何 repository；搜尋到的 `saveAsync` 呼叫也沒有房間服務的呼叫點。即使把佇列接上，`RoomPersistenceService.flushBatch` 目前只載入並 `save(entity)`，沒有把 `record.droppedItems()` 套用到 entity。若房間掉落物原本預期跨重啟保留，現況會直接遺失。

**建議**：由 `RoomService` 注入房間持久化 port/service，將 snapshot 排入佇列；在 flush 中明確套用記錄資料並提交。補一個端到端測試：丟棄物品、觸發記錄、重載房間後仍可取回，另測空清單確實清除舊掉落物。

### P1：圓桌防禦切片總和可超過 100%，高配置會吞掉後續結果

[`DefenseResolver`](../../src/main/java/com/example/htmlmud/domain/dungeon/battle/DefenseResolver.java) 分別計算 Miss、Dodge、Parry、Block、Crit 上限後直接累加累積邊界，未限制總機率。以測試附近的屬性（防守者三項屬性 30、攻擊者 DEX 20），同時啟用 Dodge、Parry 並裝備盾牌時，按程式公式約為 Miss 10% + Dodge 28% + Parry 39% + Block 29% + Crit 9% = 115%。此時 `blockLimit > 1`，所有剩餘骰值都會先命中 Block 分支，Crit 與普通 Hit 不再可能；目前 [`OneRollCombatTableTest`](../../src/test/java/com/example/htmlmud/OneRollCombatTableTest.java) 將切片分開測，未覆蓋組合上限。

**建議**：明確定義超額機率的遊戲規則（例如依優先序只分配剩餘機率，或正規化權重），保證各區間非負且總和不大於 1。新增同時啟用多種被動與盾牌的邊界測試，驗證各結果可達與區間總和。

### P1：Write-behind 佇列無界，關閉期間仍可能接收並遺失資料

[`AbstractAsyncBatchPersistenceService`](../../src/main/java/com/example/htmlmud/infra/persistence/service/AbstractAsyncBatchPersistenceService.java) 使用無界 `LinkedBlockingQueue`，因此 `offer()` 幾乎不會回傳 `false`，現有「佇列已滿」告警無法形成背壓；資料庫長時間慢於寫入時，佇列會持續吃記憶體。`shutdown()` 設定 `running = false` 後直接 drain queue，沒有阻止新的 `saveAsync`，也沒有等待 worker 結束；關閉與提交競爭時，新資料可能留在佇列而未 flush，worker 正在 flush 的批次也可能與 shutdown flush 重疊。

**建議**：改用有界佇列並定義滿載策略（拒收並回報、依 entity key 合併最新狀態，或阻塞限時）；關閉時先停止接收、喚醒 worker、排空佇列並等待 worker join，最後才回報關閉完成。為 flush 失敗加上可觀測的重試/死信策略，並測試佇列滿載、flush 失敗及關閉競態。

### P1（公開部署前）：WebSocket 連線目前沒有驗證身分或連線配額

[`WebSocketConfig`](../../src/main/java/com/example/htmlmud/config/WebSocketConfig.java) 只限制 Origin；[`MudWebSocketHandler.afterConnectionEstablished`](../../src/main/java/com/example/htmlmud/infra/server/MudWebSocketHandler.java) 對每個連線直接建立並啟動新的 `Player` Actor，未檢查已驗證的 Principal，也未見連線數、訊息大小或指令速率限制。Origin 是瀏覽器來源限制，不是身分驗證；若服務對外開放，任何可連線的客戶端都能消耗 Actor/記憶體資源。單機本機遊玩若刻意不做帳號驗證，應明確限制僅本機使用；多人或公開部署前則需先補認證、授權與配額。

## 文件與架構一致性

- [`GEMINI.md`](../../GEMINI.md) 將隊伍上限定為 5 人，但 [`ARCHITECTURE.md`](../../ARCHITECTURE.md) 的前端模組樹仍描述「6 人小隊 HUD」；同一文件稍後又正確寫 5 人。應全文以 `Party.MAX_PARTY_SIZE` 為準。
- [`FUTURE_IMPROVEMENTS.md`](FUTURE_IMPROVEMENTS.md) 內部完成狀態互相矛盾：§2.1 與 §3.1 標為已完成，但「推薦實施路徑」與執行優先序仍把 GameConfig / One-Roll 列為未完成；§2.3、§2.4 仍說職業與商店資料未串接，而 [`GEMINI.md`](../../GEMINI.md) 的進度已記錄 classes、shops 與 learned stances 資料化完成。建議用一份狀態表作為單一來源，每次里程碑只更新該處，再由各文件引用。
- [`docs/UI_PAGE_STRUCTURE.md`](../UI_PAGE_STRUCTURE.md) 與 [`FUTURE_IMPROVEMENTS.md`](FUTURE_IMPROVEMENTS.md) 宣稱主要視圖的 XSS 整治已完成、前端禁止 inline handlers；但目前主頁 [`index.html`](../../src/main/resources/static/index.html)、`party-modal.js`、`shop-modal.js`、`skill-drawer.js` 等仍有大量 `onclick` / `onchange` / `onkeypress`。此外 `party-modal.js` 將 Gambit DTO 的 `conditionLabel`、`targetLabel`、`skillName` 直接插入 `innerHTML`。這至少違反已文件化的 CSP/事件委派規範；若這些欄位未來可由使用者或不受信任資料提供，也會形成 HTML 注入面。應先完成輸出編碼與事件委派，再把「已完成」標記為真。
- `ARCHITECTURE.md` 的目錄樹與目前實際位置有落差，例如 `VirtualActor` 位於 `domain/actor/core/`，而文件列在 `domain/actor/`；前端實際位於 `src/main/resources/static/js/`。建議定期以實際目錄校正架構圖，避免將過時路徑當作新增功能 SOP。

## 建議處理順序

1. 修復並測試房間掉落物持久化，避免玩家狀態資料靜默遺失。
2. 修正圓桌機率分配，補齊多防禦切片同時啟用的測試。
3. 為 Write-behind 加入有界背壓與可靠關閉流程。
4. 對外部署前補 WebSocket 身分驗證、授權、連線/訊息配額；若仍為本機單機版，將其列為明確部署限制。
5. 清理前端剩餘 inline handlers 與未轉義插值，之後再更新 UI 安全完成狀態。
6. 統一四份文件的隊伍上限、完成狀態與實際目錄路徑；將已完成項目從待辦總表移除。

## 驗證紀錄與限制

- `mvnw test` 使用到系統 Java 8，在 Surefire 載入 JUnit 前因 `UnsupportedClassVersionError` 失敗，實際執行測試數為 0。
- `test.ps1 test` 啟動時顯示使用 Java 25.0.4.1，但本次執行未取得完整的 Surefire 結束摘要，因此不能據此宣稱測試通過。需在 JDK 25 環境重新執行並確認 `BUILD SUCCESS`。
- 上述圓桌問題由程式公式與現有測試配置推導；建議將組合配置測試納入修復驗收。
