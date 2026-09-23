# Copilot 專案 Review：現況、風險與建議實作順序

> Review 日期：2026-09-23
>
> 本文件是對目前工作區程式碼、測試、`Codex Code Review.md`、`FUTURE_IMPROVEMENTS.md`、`README.md` 及既有實施計畫的獨立核對。它不是要求一次完成所有重構的需求單。判斷分為「已證實問題」、「條件式風險」與「架構改善方向」，避免把設計偏好誤列為現存漏洞。

## 1. 結論摘要

目前專案的核心玩法與資料驅動方向已相當完整，Actor 併發防護也有實作及測試支持。`Codex Code Review.md` 對大型 Canonical Model 重構的成本判斷大致正確；但本次核對發現一個更根本的邊界問題：系統目前同時存在帳號登入模型與匿名單機 WebSocket 模型，且 WebSocket 建立連線時直接建立 `IN_GAME` 玩家。這讓 Auth 並不是實際的身份邊界，也使固定全域存檔槽位在多連線部署下成為真實的資料隔離風險。

最應先處理的不是全面改名或移除 static API，而是：

1. 明確決定產品是「單機匿名」還是「帳號式多人／多工作階段」。
2. 在決定前先封住存檔 service 的槽位、擁有者與輸入邊界。
3. 修補明確可達的前端動態 HTML 注入點，尤其是玩家名稱、存檔資料與 NPC／物品名稱。
4. 讓登入流程真正接入 WebSocket，或明確移除未使用的 Auth 路徑；不要讓兩套身份模型並存。
5. 之後才以小批次方式收斂物品 fallback、TemplateReader DI 和角色戰鬥結算同步。

## 2. 已證實的高優先問題

### P1-1：WebSocket 目前繞過 Auth，身份不是安全邊界

**證據：** [MudWebSocketHandler.java](../../src/main/java/com/example/htmlmud/infra/server/MudWebSocketHandler.java) 在 `afterConnectionEstablished` 直接呼叫 `Player.createSinglePlayer(...)`，並將玩家視為可進入遊戲的 Actor。它沒有要求登入，也沒有把已驗證的 account／character identity 放入 session。`promoteToPlayer` 雖然存在，但目前不是連線入口的必要流程。

**影響：** `AuthService` 和資料庫帳號存在，但 WebSocket 使用者仍可直接進入遊戲；`player.getName()` 不能當作可靠的使用者身份。若同一個服務接受多個連線，這會直接影響存檔授權、角色擁有權與後續交易／社交功能。

**建議：**先做產品決策：

- 若目前就是單機匿名模式，刪除或隔離未接通的登入流程，並在文件與部署設定中明確標示單機邊界。
- 若目標是帳號模式，先建立 authenticated session／WebSocket handshake identity，再由 identity 建立 Player；不要用玩家顯示名稱作為 account key。

**原因與取捨：**這是所有存檔隔離設計的前置條件。直接先搬存檔路徑只能改善檔名，不會自動產生授權。完整接入認證會涉及前端登入、session 綁定、重連與測試，成本高於單純修設定，但不應以「未來才多人」掩蓋目前模型互相矛盾的事實。

### P1-2：存檔是全域共享，service 層沒有擁有者檢查

**證據：** [SaveGameService.java](../../src/main/java/com/example/htmlmud/domain/save/service/SaveGameService.java) 固定使用 `saves/autosave.json` 與 `saves/slot_N.json`。`playerId` 只寫入 JSON；`loadGame`、`listSaveSlots`、`readSlotSummary`、`deleteSave` 沒有核對檔案內的 `playerId`，刪除甚至不接收 player identity。命令層的 [SaveCommand.java](../../src/main/java/com/example/htmlmud/application/command/impl/SaveCommand.java) 也以 `player.getName()` 作為存檔 ID。

**影響：**在多連線部署下，玩家可能讀取、覆寫或刪除其他連線的槽位。即使目前產品宣稱單機，service API 仍沒有把這個邊界寫成可驗證的契約。

**建議：**

1. 在 `SaveGameService` 內集中驗證 `slotId`：autosave 為 0，手動槽位為 1 到 5；所有 public API 都必須檢查，不只依賴 command。
2. 以已驗證的 account／character ID 建立 `saves/{ownerId}/slot_N.json`，並保留舊路徑讀取一次的 migration fallback。
3. save、load、list、summary、delete 全部接收 owner identity；讀取後再次核對 `SaveData.playerId`。
4. 用 `Path.resolve`、`normalize` 與 root containment 檢查建立路徑，即使目前槽位是整數也把檔案邊界固定在 service。
5. 補兩個 owner 的測試：A 不能讀、列出、覆寫或刪除 B 的存檔；舊單機存檔仍可遷移。

**優點：**把授權集中在 domain service，避免未來新增 HTTP／WebSocket／CLI 呼叫點時繞過保護。**代價：**需要處理舊存檔搬遷與匿名模式的 owner policy；若仍是單機，應先採用固定 local owner，而不是假裝已有帳號。

### P1-3：前端動態 HTML 有可達的未轉義資料

**證據：** [drpg-view.js](../../src/main/resources/static/js/drpg-view.js) 的 NPC header、特殊出口、地面物品、地牢樓層資訊、存檔資料等區塊把資料插入 `innerHTML`。`SaveCommand.handleNew` 可直接設定主角名稱，該名稱會進入存檔與 `renderSaveSlots`。`mud-core.js` 也把 ANSI 轉換結果交給 `innerHTML`。

**影響：**這不是「所有 innerHTML 都一定可利用」，但玩家名稱、存檔欄位及可由資料檔／伺服器控制的 NPC、物品、敵人文字，已具備需要修補的輸出路徑。`ansi_up` 的轉換結果也應由測試確認是否會 escape 原始文字，不能只假設安全。

**建議：**按 HTML context 逐點修補：

- 純文字節點使用 `textContent`／`innerText`，不要以模板字串建 HTML。
- 必須保留 markup 的地方，只讓固定 markup 由程式建立，動態值以 text node 填入。
- 不要把動態值放入 `style`、URL、HTML attribute 或 class；必要時採白名單映射。
- ANSI 顯示先確認 library 的 escape 行為，再以惡意 `<img ...>`、引號與事件屬性建立回歸測試。
- `SaveCommand.handleNew` 與其他玩家輸入仍要做 server-side 長度及控制字元限制；輸入驗證不能取代輸出 escaping。

**優點：**可以從高風險輸入點逐步交付，回歸面小。**缺點：**不能只新增一個全域 `escapeHtml()` 就宣稱所有 context 安全；錯誤使用在 attribute／URL 仍可能有問題。

## 3. 已存在但應收斂的風險

### P2-1：登入流程缺少有效的重試限制與枚舉防護

[GuestBehavior.java](../../src/main/java/com/example/htmlmud/domain/actor/behavior/GuestBehavior.java) 中註冊與登入的 username/password validation、`MAX_AUTH_RETRIES` 和 session retry counter 大多是註解；登入錯誤會區分「帳號不存在」與「密碼錯誤」。`AuthService.register()` 本身已正確呼叫驗證方法並注入 `PasswordEncoder`，不應重做 Codex 已確認完成的部分。

若 Auth 要對外使用，應加入以 account／IP／session 為維度的 rate limit 或 backoff、有限重試、統一登入錯誤訊息與 audit log。若仍是單機匿名模式，則先移除這條未接通流程，避免維護兩個互相矛盾的狀態機。

### P2-2：`PartyInventory` 找不到模板時會猜測物品

[PartyInventory.java](../../src/main/java/com/example/htmlmud/domain/party/model/PartyInventory.java) 的 `createFromTemplate` 會依 ID 是否包含 `pill`、`talisman`、`sword`、`robe` 建立 fallback，否則建立通用「古仙法物」。

這會把資料 ID 錯誤轉成看似成功的錯誤物品，掩蓋 JSON／掉落／商店關聯問題。建議先改成可觀測的 unknown-item 或明確 domain failure，補未知 ID 的負面測試，再把轉換責任集中到 factory／mapper。完整 `ItemDefinition + ItemInstance + ItemView` 仍應依 [2026-09-22_shared_canonical_model_execution_plan.md](./2026-09-22_shared_canonical_model_execution_plan.md) 分階段做，不宜一次替換所有背包與存檔類別。

### P2-3：角色同步名稱容易造成「完整同步」的誤解

[CharacterSyncService.java](../../src/main/java/com/example/htmlmud/domain/service/CharacterSyncService.java) 只複製 level、XP、HP/MP、五維與 free stat points，沒有同步 coin、stamina、equipment、learned/enabled skills 等狀態。這不一定是 bug，因為其中一些可能是模式專屬狀態；但文件將它描述為雙向單一真相源，容易讓後續開發者錯誤假設所有角色狀態都已同步。

建議先建立欄位 ownership 表：世界位置與持久角色由 Player 擁有；陣型、SAN、怒氣、cooldown 由 DRPG battle projection 擁有；戰鬥結果由明確 `BattleOutcome` 套用。對 HP/MP、XP、掉落物、技能進度各做一條 outcome 垂直切片，確認 idempotency 後再刪除整份 stats copy。

### P2-4：模板讀取仍有 static singleton、static maps 與 no-arg fallback

[TemplateRepository.java](../../src/main/java/com/example/htmlmud/infra/persistence/repository/TemplateRepository.java) 同時是 Spring component、static singleton 與 static API；[TemplateCatalog.java](../../src/main/java/com/example/htmlmud/domain/service/TemplateCatalog.java) 支援無參數建構；[PartyInventory.java](../../src/main/java/com/example/htmlmud/domain/party/model/PartyInventory.java) 等類別仍自行建立 `TemplateCatalog`。

這是測試隔離、初始化順序與資料來源追蹤的架構債，不是立即功能漏洞。建議遵循既有 execution plan：先用 `TemplateReader` 注入 production service，再建 `InMemoryTemplateReader` 純單元測試，逐批移除 `TemplateRepository` static call，最後才刪相容 facade。不要先刪 static API 或 no-arg constructor，否則會同時碰到 Jackson、舊存檔、fixture 與 Spring wiring。

## 4. 併發、資料與工程品質核對

### 4.1 Actor 防護目前可視為已完成，但測試仍可改善

[VirtualActor.java](../../src/main/java/com/example/htmlmud/domain/actor/core/VirtualActor.java) 已有 actor thread 判定與單訊息 `catch (Throwable)`；[RoomMessageBuffer.java](../../src/main/java/com/example/htmlmud/domain/actor/core/RoomMessageBuffer.java) 使用共享 daemon scheduler；[ConcurrencyAndActorSafetyTest.java](../../src/test/java/com/example/htmlmud/ConcurrencyAndActorSafetyTest.java) 也覆蓋相關行為。這些不應再列為原始 P1 待修。

但測試仍用 `Thread.sleep(150)` 等待 flush。建議改用可觀測的 future、latch 或 Awaitility；同時補 actor stop、scheduler shutdown、mailbox backlog 與重複 disconnect 的測試。這是提升可信度，不是重做已完成的修復。

### 4.2 測試數量不是品質指標，部分測試需要更接近 production path

`ItemPickupAndEntitySyncTest` 有一段直接手動把第二份掉落物加入 pouch，驗證的是資料結構而非實際 LivingService merge 流程；`DataDrivenExpansionTest.testTalkCommandExecution()` 主要執行 command，缺少回覆或狀態 assertion；大量 `@SpringBootTest` 讓純邏輯測試依賴完整 context。

建議：

- 為每個高風險 command/service 測試寫出可失敗的行為 assertion。
- 將純 mapper、公式、value object、parser 測試降級為 POJO unit test。
- 保留少量真正的 Spring integration test，測 session、repository、template loading 與完整遊戲流程。
- 對 dynamic `TemplateRepository` 測試建立獨立 reader 或明確 `@AfterEach` 清理。
- 文件不要固定宣稱 134 或 141 項測試；以 Maven／CI 實際報告為準。

### 4.3 建置腳本與日誌設定是低成本工程改善

[run.bat](../../run.bat) 與 [test.bat](../../test.bat) 仍含固定 `C:\Workspace\DevTools` 路徑，雖然有 Maven wrapper fallback；PowerShell 腳本則同時支援系統工具與 wrapper。建議讓 `.bat` 與 `.ps1` 共用一致的偵測順序，並在啟動時檢查 Java major version。

[logback-spring.xml](../../src/main/resources/logback-spring.xml) 已定義 RollingFileAppender，但 root 的 FILE appender 被註解，因此實際只輸出 console。若 production 需要故障追蹤，應依 profile 啟用檔案輸出、設定合理的 history 與敏感資料遮罩；若刻意不寫檔，應刪除未啟用的設定以免造成錯誤期待。

`application-dev.yml` 已將 H2 console 限制為本機，`application-prod.yml` 已停用 H2；WebSocket origin 也已改為白名單設定。這些不是目前應再次實作的 P0，但 production 啟動時應對缺少 `APP_WEBSOCKET_ALLOWED_ORIGINS`、`DB_PASSWORD` 等必要環境變數做明確 fail-fast／部署檢查。

## 5. 不建議現在直接做的工作

1. **全面改名 Direction／ResourceType：**除非已有實際誤用或 API 混淆，否則會牽涉 JSON、存檔與前端序列化，收益低於相容成本。
2. **一次完成 Canonical Item／Skill Model：**物品、技能、戰鬥、存檔與 DTO 同時變動，回歸面過大；先用未知 item failure、mapper 與一個消耗品／技能做垂直試點。
3. **全面 ports-and-adapters 化：**先處理真正阻礙測試、產生循環依賴或造成身份邊界不清的依賴；形式上的介面數量不是目標。
4. **只按檔案行數拆分前端：**先修輸出安全與 DOM 建構，再按穩定責任邊界抽出 WebSocket client、town view、battle view；每次拆分都要跑瀏覽器 smoke test。
5. **以 Thread.sleep 全面替換為新套件：**先改造成 flaky 或拖慢 suite 的等待；遊戲節奏本身的延遲不應與測試等待混為一談。

## 6. 建議實作順序

### Phase 0：定義執行模型與建立基準

**目標：**決定匿名單機或 authenticated account 模式，並保存目前可回歸的基準。

- 固定 JDK 25、Maven wrapper 與 profile 的 CI 建置。
- 執行完整 `mvnw test`，保存測試數與失敗報告，不把數字硬編進文件。
- 為 WebSocket 建立一個連線 smoke test，確認連線後的 state、identity 與可用 command。
- 寫下 session identity、player ID、display name 的責任與生命週期。

**完成條件：**身份模型有一個明確入口；匿名模式若保留，文件寫明它不是多人安全模型。

### Phase 1：封住身份、存檔與前端輸出邊界

**目標：**先處理實際可造成資料越權或 DOM 注入的問題。

- 在 SaveGameService 集中檢查 slot 範圍、owner、舊檔 migration 與 root containment。
- 依 Phase 0 的 identity 決定是否分 owner directory；補跨 owner save/load/list/delete 測試。
- 逐點修補 drpg view 的動態文字，優先主角名稱、存檔資料、NPC、物品、敵人與樓層名稱。
- 確認 ANSI 轉 HTML 的 escaping 行為，補輸入型回歸測試。
- 若啟用帳號，恢復有效重試限制、統一登入錯誤訊息與 rate limit；若不啟用，移除未接通的 Auth flow。

**完成條件：**未驗證 caller 無法操作其他 owner 的存檔；明確不可信文字不再直接進入 HTML 結構；WebSocket smoke test 使用正確 identity。

### Phase 2：資料完整性與 fallback 收斂

**目標：**讓錯誤資料立即可見，不再靜默產生假物品。

- 為 template ID、房間出口、鎖匙、掉落、商店商品與技能 mapping 建立跨檔完整性測試。
- 將 `PartyInventory.createFromTemplate` 的 substring fallback 改成 unknown-item 或可診斷的 domain failure。
- 把 PartyItemSlot／GameItem 的轉換集中到 mapper，保留舊存檔讀取相容性。
- 把 loot pouch integration test 改為呼叫實際掉落合併服務，而非手動重演 merge。

**完成條件：**未知 ID 會明確失敗並帶出 ID；資料完整性測試能指出檔案與關聯名稱；舊存檔仍能載入。

### Phase 3：漸進式 TemplateReader DI

**目標：**改善測試隔離與資料來源可追蹤性。

- 盤點 `TemplateRepository.`、`getInstance()`、`new TemplateCatalog()` 的 production 呼叫點。
- 先遷移 factory、service、adapter，再處理 DTO assembler。
- 建立 test-only `InMemoryTemplateReader`，讓純邏輯測試不依賴 static registry 或完整 Spring context。
- 最後才移除 static compatibility API，並用架構檢查禁止新增 static caller。

**完成條件：**production code 只透過 `TemplateReader` 取得模板；測試可獨立建立資料；兩次不同順序的測試執行不互相污染。

### Phase 4：明確角色 ownership 與 BattleOutcome

**目標：**避免雙向整份 stats 複製造成狀態覆寫。

- 先列出 Player、PartyMember、battle state 的欄位 owner。
- 建立 immutable battle snapshot 與 outcome，先改 XP 或 HP/MP 一條路徑。
- 在 actor thread 套用 outcome，加入 battle ID／idempotency key，防止重送重複發獎。
- 逐條補勝利、失敗、逃跑、死亡、掉落與技能進度的 integration test。

**完成條件：**每個同步欄位都有明確 owner 和測試；戰鬥服務不再直接任意修改 Player；完成全部路徑後才刪除舊 copy API。

### Phase 5：依實際摩擦決定大型重構

只有當前面階段顯示收益足夠時，才評估：Canonical Item／Skill Model、Domain output ports、enum rename、前端 ES modules 與 CSS 拆分。每一項都應有單獨 plan、相容策略、回退界線與 focused test，不要合併成一次大改。

## 7. 文件與架構紀錄建議

- `FUTURE_IMPROVEMENTS.md` 應移除或標示已完成的 H2、Origin、Auth、Actor、Room scheduler 與新手村鑰匙項目；目前它們仍以「現狀問題」描述，會誘導後續 AI 重做。
- 將 WebSocket identity／匿名單機邊界新增為明確 ADR，因為它控制存檔、Auth、session 與未來多人化的所有決策。
- 將「已證實缺陷」「條件式風險」「架構偏好」分欄記錄，並為每個待辦增加觸發條件、完成條件、相容風險與測試名稱。
- `README.md` 的規則可以保留，但計畫索引應補上本 review，並在重大實作完成後同步更新狀態，不只更新測試數字。

## 8. 最終判斷

Codex review 對「不要一次做大型模型重構」的主張是合理的；本次 review 的主要補充是把身份／存檔邊界提升到第一順位，因為目前 WebSocket 入口尚未真正使用 Auth。Actor 防護與 H2／Origin 基本設定不需重做；前端 XSS、存檔 owner、登入狀態機、物品 fallback 與測試真實性則應依上述 Phase 0 到 Phase 2 優先處理。

最重要的工程原則是：先讓身份、資料 owner、輸出 context 和失敗行為可觀測，再進行 Canonical Model 或 Clean Architecture 的大型整理。這樣每個後續重構都有可驗證的邊界，也保留舊存檔與既有玩法的回退空間。
