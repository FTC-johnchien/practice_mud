# 專案後續架構改善與演化藍圖 (Consolidated Future Improvements & Evolution Roadmap)

> **文件定位與目的**：
> 本文件整合了歷史上所有架構與代碼審查（Claude Opus、Codex、Copilot、Gemini 及架構小組 Code Review）之核心精華，經過實作進度比對，剔除已完成項目（Phase 0~3），系統化歸納所有**尚未實施、具備高度參考價值**之改善項目。
> **AI / 開發者守則**：啟動新階段重構、安全加固、併發調整或功能擴展前，必須主動檢索本文件對齊架構決策。

---

## 📌 進度現況對齊 (Completed Milestones Status)

在推進後續改進前，確認以下項目**已於前期完成並全數通過 134 項自動化測試**：

| 已完成里程碑 | 涵蓋範疇與關鍵落實元件 | 狀態 |
| :--- | :--- | :--- |
| **Phase 0: 核心邏輯修復** | 1. `DropCommand` 移除無效 re-add 邏輯<br>2. `EquipCommand` 移除內嵌 unequip 呼叫<br>3. `Player.lookAtMe` 修正為 `.equals()`<br>4. `LivingService.unequip` 恢復裝備解除邏輯<br>5. `RoomService.broadcastJson` 實作完成 | ✅ 已完成 |
| **Phase 1: 物品與屬性單一真相源** | 1. 廢棄 `data/global/items.json`，改以 `data/global/items/**/*.json` 分類目錄唯一收斂<br>2. `PartyItemSlot` 與 `GameItem` / `ItemTemplate` 實現雙向無損轉換<br>3. `CharacterSyncService` 維護世界主體 `Player` 與小隊隊長 `PartyMember` 雙向同步 | ✅ 已完成 |
| **Phase 2: 穩定角色識別與生命週期** | 1. 導入 `CharacterId` 強型別值物件，徹底廢除單機寫死 `"p-single"`<br>2. `PartyService` 與 `DungeonManager` 實作 Dual-Index Alias 雙向容錯索引（ID/Name）<br>3. `TemplateRepository` 轉型為 Spring Bean 託管元件 | ✅ 已完成 |
| **Phase 3: 技能語意橋接與戰鬥解耦** | 1. `SkillBridgeService` 建立 MUD 熟練度與 DRPG 戰術招式之執行期語意映射與動態等級縮放<br>2. `DrpgBattleService` 解耦為輕量 Facade，拆分 `DrpgCombatLoop`、`DrpgEnemyTacticsService`、`DrpgRewardService` | ✅ 已完成 |
| **Phase 4: 併發安全與 Actor 隱形地雷加固** | 1. `VirtualActor` 引入 `isActorThread()` 判定與內外分流，消除自身 `join()` 永久死鎖<br>2. `VirtualActor.runLoop()` 實作單訊息例外隔離防護，免疫 RuntimeException 中毒停止<br>3. `RoomMessageBuffer` 廢除 per-room 執行緒池，改以共用 Daemon 排程器統一處理碎片 Flush<br>4. `LivingStats` 強化非負邊界防禦與 `clampToMax()` 數值夾緊，消除序列化順序覆蓋問題 | ✅ 已完成 |
| **Phase 5: 前端組件模組化重構** | 1. 拆解 3,120 行巨石 `drpg-view.js` 為原生 ES6 Modules (`core/`, `panels/`, `modals/`)<br>2. 對齊畫面佈局 6 大區塊（主舞台、方位、戰鬥、隊伍HUD、功能抽屜、訊息日誌）<br>3. 支援全域快捷鍵與彈窗 In-place 更新防閃爍 | ✅ 已完成 |
| **Phase 6: 三層技能架構與小隊合擊系統** | 1. 規範單一真相源：武器技能與職業特性技能統整為標準 `SkillTemplate`（`data/global/skills/**`）<br>2. 職業技能不限武器（`allowedWeapons: []`），如戰士嘲諷/鐵壁、牧師治療/驅散、盜賊潛伏/煙霧<br>3. `party_skills.json` 轉型為多角色小隊合擊技能（Party Combo/Synergy），支援職業組合、人數、陣法與複合能量要求<br>4. 引擎動態適配器、夥伴職業技能自動掛載與 146 項全域測試通過 | ✅ 已完成 |
| **Phase 7: 全域統一技能與戰鬥動能系統** | 1. 數值資源大收斂為 HP/MP/SP 三槽模型，戰鬥中動態獲取戰氣勢能（普通命中+15、受擊+10）<br>2. 多人合擊解算器（`ComboResolver`）解算組合條件、排除異常失控成員並原子化扣除複合資源<br>3. 前端小隊 HUD 三色條規範化，`skill-drawer.js` 實現 WoW 常用列 + DQ/FF 5 大分類 Tab 抽屜<br>4. 白皮書載入，全專案 151 項自動化測試 100% 通過 | ✅ 已完成 |


---

## 🚨 1. 安全加固規範 (Security Hardening) - Priority: P0 / P1

### 1.1 H2 Console 本機與生產環境防護 (P0)
- **現狀問題**：`src/main/resources/application.yml` 中配置 `spring.h2.console.settings.web-allow-others: true`，且預設連線密碼為弱密碼 `password`。若部署於外部網路，攻擊者可透過 H2 Console 執行任意 SQL 或 JNDI/RCE 漏洞。
- **改善方案**：
  1. 將 `web-allow-others` 設為 `false`，僅允許本機 `127.0.0.1` 訪問。
  2. 區分 `application-dev.yml` 與 `application-prod.yml`，生產環境徹底停用 H2 Console (`enabled: false`)。
  3. 資料庫帳號密碼改由環境變數讀取。

### 1.2 前端 DOM XSS 防禦與字串轉義 (P0)
- **現狀問題**：`drpg-view.js` 與 `mud-core.js` 中存在多處將使用者輸入（如主角名稱、自訂稱號）、伺服器返回之 NPC 對話、物品說明直接以樣板字串拼接注入 `innerHTML`。
- **改善方案**：
  1. 在前端全域建立標準 HTML 轉義工具：
     ```javascript
     function escapeHtml(str) {
         if (!str) return '';
         return String(str)
             .replace(/&/g, '&amp;')
             .replace(/</g, '&lt;')
             .replace(/>/g, '&gt;')
             .replace(/"/g, '&quot;')
             .replace(/'/g, '&#039;');
     }
     ```
  2. 全面審查 `renderTownMainStage`、`renderPartyModal`、`renderInventory`、`updateStats`，將字串拼接改為 `escapeHtml()` 或使用 `element.textContent`。

### 1.3 存檔槽位使用者空間隔離 (P1)
- **現狀問題**：`SaveCommand` 與 `SaveGameService` 目前將存檔寫入全域路徑 `saves/slot_*.json`。在未來多人或多帳號情境下，任何連線使用者皆可讀取或覆寫其他玩家的存檔。
- **改善方案**：
  1. 存檔目錄按帳號/角色 ID 隔離：`saves/{accountId}/slot_{slotIndex}.json`。
  2. 存檔路徑檢核檔名白名單，防範目錄遍歷攻擊 (`Path Traversal: ../../`)。

### 1.4 WebSocket CORS 來源限制 (P1)
- **現狀問題**：`WebSocketConfig.java` 採用 `.setAllowedOrigins("*")`，任何第三方惡意站點皆可在使用者瀏覽器中建立跨站 WebSocket 連線劫持遊戲會話 (CSWSH)。
- **改善方案**：改為讀取配置之白名單域名，本機開發限定 `http://localhost:8080`、`http://127.0.0.1:8080`。

### 1.5 身份驗證防禦與密碼加密 Bean 注入 (P1)
- **現狀問題**：
  1. `AuthService.register()` 存在驗證方法 `validateUsername()` 與 `validatePassword()` 但未在註冊流程中呼叫。
  2. `AuthService` 自行 `new BCryptPasswordEncoder()`，未依循 Spring IoC 注入 `PasswordEncoder` Bean。
- **改善方案**：
  1. 於註冊流程前置調用校驗邏輯，限制帳號密碼長度與合法字元。
  2. 在 `SecurityConfig` 宣告 `@Bean public PasswordEncoder passwordEncoder()`，並於 `AuthService` 建構子注入。

---

## 🏛️ 2. 清潔架構與領域邊界 (Clean Architecture & Decoupling) - Priority: P1

### 2.1 領域層反向依賴反轉 (Domain Dependency Inversion)
- **現狀問題**：
  - `LivingService`、`PlayerService`、`GuestBehavior`、`RoomService` 等 Domain 層類別，直接 `import` 了 Application / Infrastructure 層的元件（如 `AuthService`、`CommandDispatcher`、`WebSocketSession`、`MudWebSocketHandler`）。
- **改善方案**：
  - 依循 DDD 原則，Domain 層不可依賴外層。
  - 提取 Output Ports（輸出介面）：例如 `SessionMessageSender`、`SessionContext`、`GameEventPublisher`，由 Infrastructure 層實作這些介面並注入 Domain 服務。

### 2.2 命名衝突 Enum 拆分與語意收斂
- **現狀問題**：
  1. **方向衝突**：`net.mud.world.Direction`（MUD 拓撲 10 方向：北、南、東、西、上、下、東北、西北、東南、西南）與 `net.mud.dungeon.Direction`（DRPG 2D 矩陣 4 方向：NORTH, SOUTH, EAST, WEST）同名。
  2. **資源類型衝突**：存在兩套 `ResourceType` 定義。
- **改善方案**：
  - 將 MUD 方向更名為 `WorldDirection` 或 `MudDirection`。
  - 將 DRPG 方向更名為 `GridDirection`。
  - 於共用層提供 `DirectionAdapter` 處理正交方向轉換 (`WorldDirection.NORTH ↔ GridDirection.NORTH`)。

### 2.3 指令冗餘消除與責任收斂
- **現狀問題**：部分玩家指令存在重複實作：
  - `dismiss`：同時散落於 `DismissCommand` 與 `PartyCommand.handleDismiss`。
  - `use`：同時存在於 `UseCommand` 與 `InventoryCommand`。
  - `seal`：同時存在於 `SealCommand`、`BattleCommand` 與 `InventoryCommand`。
- **改善方案**：
  - 統一以 Facade/Service 處理核心業務（如 `PartyService.dismissMember`、`ItemUsageService.useItem`）。
  - 單一指令類別專職解析語法，多別名映射至同一個 Command Bean，杜絕複製貼上程式碼。

### 2.4 指令間直接耦合解構 (Decouple Inter-Command Invocations)
- **現狀問題**：`LoadCommand` 直接注入並呼叫 `SaveCommand`，`MoveCommand` 直接注入並呼叫 `LookCommand`。
- **改善方案**：
  - 指令不應依賴其他指令。
  - 將共同邏輯（如存檔查詢、房間視角渲染）抽入 `PlayerPresentationService` 或 `SaveGameService`，指令各自調用底層服務。

---

## ⚡ 3. 併發安全與 Actor 執行緒健全度 (Concurrency & Actor Integrity) - Priority: P1 【✅ 已完成】

> **完成狀態**：已全數實作防禦機制，並建立 `ConcurrencyAndActorSafetyTest` 驗證套件，全數通過 141 項測試。

### 3.1 VirtualActor 自死鎖防禦 (Self-Deadlock Prevention)
- **現狀問題**：`Living.equip()`、`Living.unequip()`、`Living.lookAtMe()` 中調用 `future.join()` 等待郵箱執行結果。若呼叫者恰好就在該 Actor 自身的虛擬執行緒內，將導致自身等待自身完成任務的永久死鎖。
- **改善方案**：
  ```java
  public CompletableFuture<Void> runInActorThread(Runnable task) {
      if (Thread.currentThread() == this.actorThread) {
          task.run();
          return CompletableFuture.completedFuture(null);
      }
      // 否則投遞至郵箱隊列
      ...
  }
  ```

### 3.2 RoomMessageBuffer 執行緒池洩漏修復
- **現狀問題**：`RoomMessageBuffer` 為每個房間建立 `ScheduledExecutorService`，房間生命週期結束或系統重啟時未統一釋放，造成虛擬/平臺執行緒資源洩漏。
- **改善方案**：
  1. 改由 Spring 管理的全域排程執行緒池統一派發定時 Flush。
  2. 或實作 `@PreDestroy` / `AutoCloseable`，在容器關閉時遍歷關閉所有緩衝區排程器。

### 3.3 VirtualActor 例外中毒防護 (Poisoning / Failure Recovery)
- **現狀問題**：`VirtualActor.runLoop()` 中若發生未捕捉的 `RuntimeException` 或 `Error`，虛擬執行緒將直接死亡，Actor 永遠失去回應能力（Mailbox 阻塞）。
- **改善方案**：在事件處理迴圈外圍加上 `try-catch(Throwable t)`，記錄錯誤日誌並觸發失敗恢復機制（Supervisor Strategy），維持郵箱迴圈運轉。

### 3.4 LivingStats 欄位封裝與執行緒邊界
- **現狀問題**：`LivingStats` 部分數值欄位為 `public`，允許外部直接修改，破壞了狀態變更必須通過 Actor 訊息的約束。
- **改善方案**：所有屬性改為 `private`，提供具備邊界防禦（Clamped: 0 ~ Max）的修改方法，狀態變更統一由 `Living` / Actor 驅動。

---

## 📦 4. 資料驅動架構演進與死代碼清理 (Data-Driven & Code Health) - Priority: P2

### 4.1 Canonical Data Model 與雙模式適配器深化
- **架構原則**：
  1. **資料層唯一 (Canonical Schema)**：地圖、物品、技能、種族、職業維持單一 JSON 定義。
  2. **角色職責分層 (Role Separation)**：
     - `Living` = 世界實體 (World Entity/Actor，負責房間存在、移動、世界對話與生命週期)。
     - `PartyMember` / `BattleUnit` = 戰鬥投影 (Combat Projection/Card，負責陣型、SAN、怒氣、連擊與 DRPG 戰術)。
     - **嚴禁使用繼承將兩者強行綁死**（即嚴禁 `PartyMember extends Living`），兩者透過 `CharacterSyncService` 與共用屬性保持同步。
  3. **適配器工廠 (Adapters)**：以 `MudTemplateAdapter` 與 `DrpgTemplateAdapter` 分別將共用模板轉化為 MUD 實體與 DRPG 戰鬥單位。

### 4.2 資料檔鍵值不一致修正
- **現狀問題**：`newbie_village/rooms.json` 中的暗道開門鑰匙 ID 為 `village_elder_key`，但物品庫或關聯標註曾出現 `village_elder_house_key`。
- **改善方案**：統一使用 `village_elder_house_key`，並在 `WorldDataIntegrityTest` 新增房間鎖定鑰匙關聯校驗。

### 4.3 歷史死代碼與未調用方法清理
- **清理標的**：
  1. 註解掉的 `PlayerLoginListener`
  2. `LivingStateService.processLevelUp`（已由 `XpProgressionService` 取代）
  3. `TaichiHitPerform`
  4. `WorldManager.startPersistenceWorker()` 未被調用之死方法
  5. `singleplayer_v2.html` 殘留之 `alert('CORPSE_DETAIL')` 測試碼

### 4.4 構建與環境配置補強
- **Git 忽略**：`.gitignore` 目前僅忽略 `autosave.json`，需補齊 `/saves/*.json`。
- **腳本路徑相容性**：`run.bat` / `test.bat` 若存在寫死路徑，應對齊 `run.ps1` / `test.ps1` 之動態目錄偵測機制。
- **日誌檔案輸出**：檢視 `logback-spring.xml`，重啟輪轉日誌檔案輸出（RollingFileAppender）。

---

## 🎨 5. 前端現代化與組件模組化 (Frontend Architecture) - Priority: P2

### 5.1 巨石 JS / CSS 檔案模組化拆分 【JS 部分 ✅ 已完成】
- **現狀問題**：
  - `drpg-view.js` 原單檔超過 3,120 行。
  - `style.css` 單檔超過 4,180 行。
  - 造成維護困難與修改時的高回歸風險。
- **改善方案與落地成果**：
  - 改用原生 ES6 Modules 拆分（已完成落地並建立 10 個核心模組）：
    - `js/core/event-bus.js`：輕量事件總線。
    - `js/core/state-store.js`：全域狀態快照儲存中樞。
    - `js/core/cmd-dispatcher.js`：步進防抖與方向路由派發器。
    - `js/panels/town-panel.js`：城鎮主舞台、方位羅盤、生靈清單與物品拾取。
    - `js/panels/dungeon-panel.js`：10x10 地牢雷達迷霧、視口切換與前方探查。
    - `js/panels/party-hud-panel.js`：6 人小隊狀態 HUD 與陣法靈威。
    - `js/panels/battle-panel.js`：戰鬥主舞台、敵怪雙排陣列、集火鎖定與戰鬥控制列。
    - `js/panels/message-log-panel.js`：文字日誌終端滾動。
    - `js/modals/`：技能抽屜、公共行囊、裝備法術書、貨棧交易與 5+1 存檔管理。
    - `js/app.js`：應用主入口整合與全域快捷鍵。
  - CSS 按元件拆分（`base.css`, `town.css`, `battle.css`, `modal.css`，待後續跟進）。

### 5.2 前端異常防禦與無障礙優化
- **DOM ID 存取保護**：`mud-core.js` 中 `updateStats()` 避免無條件存取 `#hp-val`、`#mp-val`，加入 optional chaining 或 null 檢查防範 `TypeError`。
- **無障礙 (ARIA)**：所有彈窗補全 `role="dialog"`、`aria-modal="true"` 與 ESC 鍵關閉支援。
- **外部 CDN 資源安全**：靜態引用的 CDN 資源（如 `ansi_up`）補上 Subresource Integrity (`integrity`) 驗證碼。

---

## 🧪 6. 測試覆蓋率與品質提升 (Test Coverage & Quality) - Priority: P2 / P3

### 6.1 補全關鍵元件測試缺口
- **指令層**：針對目前尚未獨立測試之 11 個指令（如 `DropCommand`、`EquipCommand`、`UnequipCommand`、`LookCommand` 等）與 `CommandDispatcher` 補齊測試。
- **通訊與安全層**：補齊 `MudWebSocketHandler`、`AuthService`、`SecurityConfig` 測試。
- **併發與 Actor**：建立 `VirtualActorTest` 針對 Mailbox 循序性、高併發訊息吞吐與死鎖防禦進行壓力測試。

### 6.2 測試品質重構
- **杜絕假測試與無斷言測試**：審查並修正 `ItemPickupAndEntitySyncTest` 與 `DataDrivenExpansionTest` 中未調用真實服務或缺乏有效 `assert` 的測試方法。
- **移除硬編碼等待**：消除測試代碼中的 `Thread.sleep()`，改用 `Awaitility` 輪詢條件或虛擬時鐘 mock。
- **輕量化測試 context**：針對純邏輯測試，將 `@SpringBootTest` 降級為純單元測試（POJO Unit Test），加速 Maven 測試建置時間。
- **靜態測試隔離**：在動態修改 `TemplateRepository` 的測試類別加上 `@AfterEach` 清理靜態註冊表，防止測試間相互污染。

---

## 📋 改善項目實施優先序總表 (Execution Priority Matrix)

| 優先序 | 項目類別 | 改善任務簡述 | 預期效益 |
| :---: | :--- | :--- | :--- |
| **P0** | 安全加固 | 1. 關閉 H2 Console 外部存取<br>2. 建立全域 `escapeHtml()` 杜絕前端 XSS | 消除嚴重安全漏洞與 RCE/XSS 風險 |
| **P1** | 安全與架構 | 1. 存檔槽位使用者空間隔離<br>2. 限制 WebSocket 來源<br>3. 補齊 AuthService 註冊校驗與密碼加密 Bean<br>4. VirtualActor 自死鎖防禦<br>5. 依賴反轉（Domain 不直接 import Web/Auth） | 提升多端健全度、架構分層嚴謹度與執行緒安全 |
| **P1** | 領域模型 | 1. 消除 Direction/ResourceType 命名衝突<br>2. 消除指令重複實作與指令間直接耦合 | 提升代碼清晰度，避免語意混淆 |
| **P2** | 代碼與資料健康 | 1. 修復 `rooms.json` 鑰匙 ID 殘留問題<br>2. 清理歷史 dead code（註解 Listener、舊升級方法）<br>3. 修正 `.gitignore` 與執行腳本路徑相容性 | 保持專案整潔度與可持續維護性 |
| **P2** | 前端工程 | 1. `drpg-view.js` 與 `style.css` 元件模組化拆分<br>2. DOM 存取防禦與 CDN 資源 SRI | 降低單檔複雜度，提升前端修改穩定性 |
| **P2** | 測試工程 | 1. 補齊 CommandDispatcher 與未覆蓋指令測試<br>2. 以 Awaitility 取代 Thread.sleep<br>3. 修正假測試與增加靜態表清理 | 確保回歸防護網百分之百可信 |
