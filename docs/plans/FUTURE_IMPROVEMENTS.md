# 專案後續架構改善與演化藍圖 (Consolidated Future Improvements & Evolution Roadmap)

> **文件定位與目的**：
> 本文件作為專案所有後續架構演進、機制補完、安全加固與重構任務的**單一權威來源 (Single Source of Truth, SSoT)**。
> 完整收斂並整合了歷史架構規劃與三大模型之深度代碼審查報告（`Claude_Code_Review.md`、`Copilot_Code_Review.md` 與 `Codex_Code_Review.md`）：
> 1. **戰鬥與成長機制** (圓桌判定、魂系精力破防、主角自由潛能點、MUD 生靈閉環)
> 2. **全實體資料驅動** (去除職業/陣型/反擊/別名硬編碼、集中式 JSON 啟動校驗)
> 3. **共用規範模型與清潔架構** (Canonical Model、Domain 依賴反向、Actor 狀態邊界單一擁有)
> 4. **連線安全與部署防禦** (WebSocket 身分票證、訊息長度與速率限制、部署模式明確化)
> 5. **存檔版本治理與防損壞** (SaveData `schemaVersion`、遷移適配器、損壞槽位可識別)
> 6. **前端模組化與代碼收斂** (巨石 `style.css` 拆分、`party-modal.js` 職責切分、legacy 模組除役)
> 7. **測試工程分層加速** (領域純單元測試 vs Spring 整合測試、快取隔離)
>
> 經與當前程式庫（Java 25 / Spring Boot 4.1.1 / ES6 前端 / 231 項全綠燈測試）實地盤查與交叉校對，剔除已完成項目、修正歷史過期數據（如修正 CSS 5,810 行、party-modal 3,141 行），按優先級（P0 ~ P3）與業務領域結構化整理，供後續開發與重構直接依序落地。

---

## 📌 歷史演進指針 (Historical Records)

> 💡 **已完成里程碑與歷史變更**：
> 本專案所有已落地之里程碑（Phase 0 ~ Phase 11.5 主選單重構）、Bug 修復與架構升級歷程，已全數移轉至專門文件維護：
> 👉 **請參閱 [`CHANGELOG.md`](../../CHANGELOG.md)**。
> 本文件僅專注於**「未決定議題、待執行架構任務與後續演化路線圖」**。

---

## 🧭 後續推薦實施路徑 (Next Evolution Paths)

- [x] **路徑 0: 緊急程式與安全漏洞修復 (Hotfixes & Hardening)** (§0 全項)
- [x] **路徑 A: 共用 Canonical Model 徹底解耦 (Phase 2 ~ 5)** (§1 全項)
- [x] **路徑 B: 戰鬥圓桌判定、盾牌格擋、魂系精力破防與主角雙軌升級** (§2 全項)
- [x] **路徑 C: MUD 端生靈 AI、升級閉環、Buff 引擎與規則補齊** (§3 全項)
- [x] **路徑 D: UI/UX 全域佈局重構、三種資訊密度、字級規範 (>= 13px) 與雙軌資源契約** (§4 全項)
- [x] **路徑 E: 領域層反向依賴反轉 (Clean Architecture Phase 10)** (§5.1)
- [ ] **路徑 F: 網路邊界安全、WebSocket 部署防護與連線配額** (§6 全項) — **Priority: P1**
- [ ] **路徑 G: Actor 狀態修改單一擁有權與背景戰鬥生命週期治理** (§7 全項) — **Priority: P1**
- [ ] **路徑 H: 檔案式存檔版本治理 (`schemaVersion`) 與壞檔防禦** (§8 全項) — **Priority: P1**
- [ ] **路徑 I: 徹底資料驅動消除硬編碼與集中式 JSON 啟動拓撲校驗** (§9 全項) — **Priority: P1**
- [ ] **路徑 J: 前端巨石模組拆分 (`style.css` / `party-modal.js`) 與 Legacy 除役** (§10 全項) — **Priority: P2**
- [ ] **路徑 K: 測試工程分層加速與系統可觀測性** (§11 全項) — **Priority: P3**

---

## 🚨 0. 緊急核心修復與安全性加固 (Critical Hotfixes & Hardening) — Priority: P0 (已完成)

### 0.1 ✅ [已完成] 房間狀態與地面掉落物持久化閉環 (P0)
> **落地進度**：已於 2026-10-02 完成。
> 1. `RoomService` 注入 `RoomPersistenceService`，於物品丟棄、拾取或快照時非同步排入寫入佇列。
> 2. `RoomPersistenceService.flushBatch()` 實作新舊實體 upsert，完整保存 `droppedItems`。
> 3. `RoomService.spawnInitial()` 於房間初次載入時自動讀取歷史持久化狀態還原。
> 4. `GameItem` 輔助方法補齊 `@JsonIgnore` 與空指針防禦，修復 Hibernate/Jackson 序列化失敗漏洞。
> 5. 新增專屬整合測試 `RoomDroppedItemPersistenceTest` 100% 綠燈通過。

### 0.2 ✅ [已完成] 戰鬥圓桌防禦切片累積總機率收斂 (P0)
> **落地進度**：已於 2026-10-02 完成。
> 1. `DefenseResolver` 導入總防禦率上限（預設 $75\%$），超過時依比例縮放防禦切片（Miss / Dodge / Parry / Block）。
> 2. 致命一擊 (Crit) 僅分配剩餘機率，並保留至少 $5\%$ 作為普通命中 (Normal Hit) 窗口，消除複合高防禦下的機率湮滅 Bug。
> 3. `GameConfig.Combat` 新增 `maxTotalDefenseChance` 與 `minNormalHitChance` 設定與單元測試。
> 4. `OneRollCombatTableTest` 新增複合防禦邊界與 3,000 次蒙地卡羅隨機模擬測試 100% 綠燈通過。

### 0.3 ✅ [已完成] Write-Behind 持久化佇列有界化與可靠關閉 (P0)
> **落地進度**：已於 2026-10-02 完成。
> 1. `AbstractAsyncBatchPersistenceService` 佇列設為有界容量 (10,000)，滿載時回報背壓日誌。
> 2. 引入 `AtomicBoolean accepting` 狀態，關閉時拒收新資料防止漏寫。
> 3. 優雅關閉時等待 worker 虛擬執行緒 `join(3s)`，並同步排空剩餘資料，消除併發 flush 競態。
> 4. 提供 `flushImmediately()` 支援測試與關鍵節點即時排空。

### 0.4 ✅ [已完成] 前端資料輸出轉義與 CSP 事件委派整頓 (P0)
> **落地進度**：已於 2026-10-02 完成。
> 1. `party-modal.js` Gambit 戰術方針動態插值（`conditionLabel`、`targetLabel`、`skillName`、`valDisplay`）全面導入 `escapeHtml()` 轉義防禦。
> 2. `index.html` 移除全數 inline `onclick` 與 `on*=` 屬性，全面改為 `data-action` 屬性。
> 3. `app.js` 實作全局集中式事件委派 (`initEventDelegation()`)，支援背景遮罩關閉、選單跳轉、快捷指令發送，徹底滿足嚴格 Content Security Policy (CSP) 規範。

---

## 🏛️ 1. 共用 Canonical Model 徹底解耦 (Canonical Model Decoupling) — Priority: P1 (已完成)

### 1.1 ✅ [已完成] Phase 2: Canonical Item Model (`ItemDefinition` + `ItemInstance` + `ItemView`) (P1)
> **落地進度**：已於 2026-10-02 完成。不可變 `ItemDefinition` record、`ItemInstance` 實體、`ItemView` 投影與 Spring 託管之 `ItemFactory`，`CanonicalItemModelTest` 全數通過。

### 1.2 ✅ [已完成] Phase 3: 統一技能定義 Single `SkillDefinition` (MUD / DRPG Facets) (P1)
> **落地進度**：已於 2026-10-03 完成。`SkillDefinition`、`MudSkillRules`、`DrpgSkillRules`、`SkillBridgeRule`，徹底廢除寫死之 switch/if-else，`CanonicalSkillModelTest` 通過。

### 1.3 ✅ [已完成] Phase 4: 徹底移除 `TemplateRepository` 靜態呼叫 (DI Migration) (P1)
> **落地進度**：已於 2026-10-03 完成。`TemplateRepository` 重構為純 Spring `@Component` 實作 `TemplateReader`，移除靜態 Map 與單例，全整合測試注入實例。

### 1.4 ✅ [已完成] Phase 5: 基於 Outcome 的角色同步機制 (Outcome-based Synchronization) (P1)
> **落地進度**：已於 2026-10-03 完成。開戰前產生不可變 `BattleParticipantSnapshot`；戰後產生不可變 `BattleOutcome` 由內建 LRU 冪等快取的 `BattleOutcomeApplier` 單向安全套用，徹底根除雙向拷貝與併發競態風險，`OutcomeBasedCharacterSyncTest` 通過。

---

## ⚔️ 2. 戰鬥圓桌判定、格擋與成長機制 (Combat & Progression) — Priority: P1 / P2 (已完成)

### 2.1 ✅ [已完成] 全域數值集中管理 — `GameConfig` (P1)
> **落地進度**：集中管理經驗、戰鬥、精力、冷卻等常數於 `GameConfig.java` 與 `application.yml`。

### 2.2 ✅ [已完成] 經驗值公式 3 套矛盾實作收斂 (XP-01) (P1)
> **落地進度**：統一由 `XpProgressionService` 集中管理角色等級曲線 ($60L^{1.6} + 120L$) 與技能曲線 ($50 \times \text{lv}^2 \times \text{diff}$)，打通溢出經驗保留與全狀態回滿。

### 2.3 ✅ [已完成] 一次擲骰圓桌判定 (One-Roll Combat Table) 收斂 (P1)
> **落地進度**：`DefenseResolver` 統一雙軌單次擲骰圓桌判定：$\text{[Miss]} \to \text{[Dodge]} \to \text{[Parry]} \to \text{[Block]} \to \text{[Crit]} \to \text{[Normal Hit]}$，導入 75% 防禦上限與 5% 保底命中。

### 2.4 ✅ [已完成] 獨立盾牌格擋 (Block) 與破招反擊 (Riposte) (P2)
> **落地進度**：副手裝備盾牌參與圓桌格擋減傷；招架成功觸發無消耗破招反擊，MUD 與 DRPG 雙軌致死判斷閉環，`ShieldBlockAndRiposteTest` 通過。

### 2.5 ✅ [已完成] 魂系精力 (Stamina) 消耗與架勢破防 (Poise Break) (P2)
> **落地進度**：閃避扣 2 點、招架扣 3 點精力；精力歸零進入架勢破防（閃避/招架歸零、承受傷害 +20%），脫戰自然回復，`StaminaAndPoiseBreakTest` 通過。

### 2.6 ✅ [已完成] 主角自由潛能點 (Potential Points) 雙軌升級 (P2)
> **落地進度**：夥伴升級純依模板自動成長；主角/隊長每級獲得自由潛能點 (+2~3 點)；實作 MUD `StatCommand`（`score`/`status`/`加點`）與雙向即時同步，`DualTrackPotentialPointsTest` 通過。

---

## 🧟 3. MUD 端生靈 AI、升級閉環與規則補齊 (MUD Mechanics) — Priority: P1 / P2 (已完成)

### 3.1 ✅ [已完成] Mob 行為與反應補完 (MUD-01) (P1)
> **落地進度**：`Mob.sayToRoom` 廣播、主動怪進場撲咬、被動怪閒話、商人求救與仇恨同步閉環。

### 3.2 ✅ [已完成] MUD 端角色擊殺升級突破閉環 (MUD-02) (P1)
> **落地進度**：怪死計算動態修為掉落，調用 `XpProgressionService.awardExp` 觸發升級突破與潛能點入帳。

### 3.3 ✅ [已完成] MUD 端 Buff / Debuff 引擎串接 (P1)
> **落地進度**：`Living` 實作 `Buffable`，支援多重護盾 Shortest-Duration-First 吸收，每 500ms 週期心跳結算 HoT/DoT 與 DoT 致死結算。

### 3.4 ✅ [已完成] MUD 規則與戰鬥細節補齊 (P2)
> **落地進度**：`LivingPosture` 姿勢檢定（阻擋非站立移動）、`RoomFlag`（SAFE_ZONE 禁武、NO_MAGIC 禁魔、NO_MOB、HIGH_REGEN）、`DamageType` 抗性增減、`MobRank` 精英/首領倍率全數生效。

---

## 🖥️ 4. UI/UX 全域佈局與資源契約 (UI Layout & Density) — Priority: P1 / P2 (已完成)

### 4.1 ✅ [已完成] 三種資訊密度分區與字級規範 (>= 13px) (P1)
> **落地進度**：全域審查消除 `< 12px` 樣式，遵循 `--font-xs: 13px`；探索密度雷達 26x26px、戰鬥密度三層甲板、管理密度 1600x900 規格。

### 4.2 ✅ [已完成] 共用頁面 Shell 重構 (Global Layout) (P1)
> **落地進度**：頂部 Global Header、中央 Context Viewport、底部常駐 5 人小隊軌道與情境動態操作列 (Context Action Bar)。

### 4.3 ✅ [已完成] HP / MP / SP / SAN 雙軌資源顯示契約 (P2)
> **落地進度**：`DrpgStateDto` 支援雙軌資源，近戰法術雙修條件各自獨立判定，前端 HUD 雙軌同步呈現，`DualTrackResourceContractTest` 通過。

---

## 📐 5. 清潔架構與領域依賴反向 (Clean Architecture) — Priority: P1 (已完成)

### 5.1 ✅ [已完成] Domain 層反向依賴反轉 — Phase 10 (Clean Architecture) (P1)
> **落地進度**：
> 1. 提取 `PlayerPersistencePort`、`DomainMetricsPort`、`TemplateRegistryPort` 至 `domain.port`。
> 2. `RandomUtil`、`IdUtils`、`RoomDescriptionDeserializer` 移入 `domain.util` / `domain.model.template.json`。
> 3. `domain` 套件對外部 `infra` 依賴達成 **0 違規**！全專案 231 項自動化測試 100% 綠燈通過。

---

## 🌐 6. 網路邊界安全、WebSocket 部署防護與連線配額 (Network Hardening) — Priority: P1

> **來源**：`Codex_Code_Review.md` (§P1)、`Copilot_Code_Review.md` (§3.4)、`Claude_Code_Review.md` (§五)。
> **核心目標**：明確界定「單機本機」與「公開部署」的安全邊界，防止未驗證連線、大封包記憶體攻擊與指令洪水。

### 6.1 明確定義部署模式與本機綁定預設 (P1)
- **現存缺陷**：
  `config/WebSocketConfig.java` 僅依賴 Origin 白名單註冊 `/ws`。Origin 白名單僅能防禦瀏覽器跨站連線，無法阻擋非瀏覽器客戶端（如手寫指令碼或爬蟲）。
- **改善方案**：
  1. 於 `application.yml` 提供明確的部署設定項：
     ```yaml
     game:
       network:
         mode: local # local | public
         bind-address: 127.0.0.1
     ```
  2. 若為 `local` 模式，伺服器嚴格僅監聽 `127.0.0.1` 環回介面，杜絕外部區域網入侵。

### 6.2 公開模式 WebSocket 握手票證驗證與連線配額 (P1)
- **現存缺陷**：
  `MudWebSocketHandler.afterConnectionEstablished()` 對所有進入連線直接建立並註冊玩家 Actor，缺乏身分鑑權。
- **改善方案**：
  1. **握手鑑權 (Handshake Auth)**：實作 `HandshakeInterceptor`，公開模式下要求客戶端在連線 URL 帶入短期 Ticket/Token（如 `/ws?ticket=xxx`），由伺服器驗證通過後才允許升級為 WebSocket。
  2. **封包長度上限**：在 `WebSocketConfiguration` 配置 `ServletWebSocketHandlerRegistry` 與 `WebSocketSessionDecorator`，強制限制單一文字訊息長度最大為 **64 KB**，超額直接關閉連線。
  3. **指令速率限制 (Rate Limiting)**：在 Handler 實作滑動窗口或令牌桶算法，限制每連線每秒最多處理 **20 次指令**，超額回傳警告訊息並短暫暫停處理。
  4. **單 IP 連線配額**：限制單一 IP 最多建立 3 條活躍連線，防止資源耗盡攻擊。

---

## 🏛️ 7. Actor 狀態修改單一擁有權與背景生命週期治理 (Actor Lifecycle) — Priority: P1

> **來源**：`Codex_Code_Review.md` (§P1)、`Claude_Code_Review.md` (§二)。
> **核心目標**：杜絕繞過信箱的狀態變更，落實 Actor 嚴格單一線程排序，治理虛擬執行緒戰鬥生命週期與逾時斷路器。

### 7.1 收斂 Room Actor 狀態修改通道（修復 `removePlayer` 雙軌修改） (P1)
- **現存缺陷**：
  [Room.java#L342-L346](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/actor/impl/Room.java#L342-L346) 的 `removePlayer(String playerId)` 在呼叫者線程直接調用 `players.removeIf(...)`，隨後又向佇列送出 `RoomMessage.RemovePlayer`。即使 `players` 為 `CopyOnWriteArrayList`，仍破壞了 Actor 作為狀態變更唯一排序者的契約。
- **改善方案**：
  1. 移除 `Room.removePlayer()` 與 `removeMob()` 中 caller 線程的直接 `removeIf` 呼叫。
  2. 房間內所有成員新增、移出、掉落物撿取與丟棄，**100% 嚴格僅在信箱訊息處理器中執行**。
  3. 若呼叫端需要確認移出完成，統一採用帶 `CompletableFuture<Boolean>` 的請求-回應信箱訊息。
  4. 補齊高併發進出房間與掉落物變更的專屬併發測試。

### 7.2 背景戰鬥虛擬執行緒治理與逾時斷路器 (P1)
- **現存缺陷**：
  [DrpgCombatLoop.java#L85](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java#L85) 開戰時以非管理的 `Thread.ofVirtual().start(...)` 裸啟動，缺乏全域戰鬥併發限制、伺服器關機時的任務盤點與逾時斷路器。若戰鬥雙方陷入無限回血/無攻擊能力死迴圈，執行緒將無限期佔用資源。
- **改善方案**：
  1. 引入 Spring 託管的 `ExecutorService`（如 `Executors.newVirtualThreadPerTaskExecutor()`）統籌戰鬥任務。
  2. 在 `BattleContext` 設置最大戰鬥時間（預設 5 分鐘）與回合上限（預設 100 回合）。
  3. 超過上限時觸發斷路器，判定平局/脫戰並安全清理 `activeBattles` 快取，產生日誌。
  4. 連線斷開時依據設計原則判定戰鬥行為（如依預設 Gambit 持續結算或自動暫停），避免野執行緒持續運行。

---

## 🛡️ 8. ✅ [已完成] 檔案式存檔版本治理 (`schemaVersion`) 與壞檔防禦 (Save File Resilience) — Priority: P1

> **來源**：`Codex_Code_Review.md` (§P1)、`Copilot_Code_Review.md` (§3.5)、`Claude_Code_Review.md` (§三)。
> **落地進度**：已於 2026-10-03 完成。
> 1. **資料模型版本治理 (`CURRENT_SCHEMA_VERSION = 1` / `SaveMigrationPipeline`)**：
>    - [SaveData.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/save/model/SaveData.java) 引入 `CURRENT_SCHEMA_VERSION = 1` 與 `schemaVersion` 欄位（`@Builder.Default`）。
>    - 實作 [SaveMigrationPipeline.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/save/migration/SaveMigrationPipeline.java) 與 `SaveMigration` 介面，內建 v0（歷史 legacy 無版本存檔）平滑遷移升級至 v1 的安全補齊管線。
> 2. **壞檔狀態明確標記與防禦 (`SaveSlotDto` / `SaveCorruptedException`)**：
>    - [SaveSlotDto.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/save/dto/SaveSlotDto.java) 新增 `boolean corrupted` 欄位。
>    - [SaveGameService.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/save/service/SaveGameService.java) 的 `readSlotSummary()` 在 JSON 格式不合法或解析例外時，嚴格回傳 `empty: false, corrupted: true, title: "... (存檔損壞)"`，杜絕壞檔被誤判為空槽而遭意外覆寫。
>    - `loadGame()` 遇到損壞存檔時嚴格拋出 [SaveCorruptedException.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/save/exception/SaveCorruptedException.java)，阻止殘破資料注入記憶體。
> 3. **原子性自動 `.bak` 備份與還原機制 (`restoreBackup` / `save restore`)**：
>    - 存檔寫入或覆寫時，自動將前代舊存檔備份為 `autosave.json.bak` / `slot_X.json.bak`。
>    - `SaveGameService` 提供 `hasBackup(slotId)` 與 `restoreBackup(slotId)`；指令行支援 `save restore <slotId>`；刪除存檔時同步安全清理備份。
> 4. **前端防禦與 UI 視覺適配 (`drpg-view.js` / `style.css`)**：
>    - `drpgState.latestSaveSlot` 排除壞檔，避免「繼續遊戲」嘗試載入損壞資料。
>    - 壞檔卡片呈現醒目警示樣式（`.is-corrupted`, `.corrupted-tag`），禁用載入按鈕，並提供「🔄 還原備份」、「💾 重新覆寫」與「🗑️ 刪除」。
> 5. **完整單元測試覆蓋 (`SaveGameServiceTest.java`)**：
>    - 覆蓋單機存檔全生命週期、壞檔偵測與防禦攔截、v0 舊檔自動遷移至 v1、以及 `.bak` 備份自動生成與時空還原驗證。

---

## 🏛️ 9. 徹底資料驅動消除硬編碼與集中式 JSON 啟動拓撲校驗 (Data-Driven Purity) — Priority: P1

> **來源**：`Claude_Code_Review.md` (§一)、`Copilot_Code_Review.md` (§3.3)、`Codex_Code_Review.md` (§P2)。
> **核心目標**：拔除底層代碼中殘留的職業名稱、陣法字串比對與技能別名映射；建立啟動期跨檔案資料校驗器。

### 9.1 ✅ [已完成] 消除職業特例與反擊特性硬編碼 (DATA-01) (P1)
> **落地進度**：已於 2026-10-03 完成。
> 1. **職業特質資料化 (`ClassTemplate` / `classes.json`)**：在 `classes.json` 為俠客配置 `"canRiposte": true` 與通用 `traits`；`ClassTemplate` 支援 `hasTrait(...)` 與 `getTraitDouble(...)`；`DefenseResolver` 徹底廢除 `"SWORDSMAN"` 字串硬編碼，全面轉為資料驅動檢定。
> 2. **職能角色標準化 (`RoleCategory` / `FormationTemplate`)**：引入標準枚舉 `RoleCategory`（`TANK`, `MELEE_DPS`, `RANGED_DPS`, `MAGIC_DPS`, `HEALER`），支援 `fromClassOrRole` 與 `parseRequirement`；`FormationTemplate` 的 `satisfiesClassRequirement` 與中文名稱格式化全面依賴職能枚舉比對，徹底移除寫死之 switch 中英文字串比對。
> 3. **技能別名集中治理 (`party_skills.json` / `TemplateCatalog`)**：在 `PartyMemberSkill` 與 `party_skills.json` 引入 `aliases` 陣列（支援 `tank_taunt` ⇄ `class_warrior_taunt`、`heal_single` ⇄ `class_cleric_heal` 等）；`TemplateRepository` 建立雙向別名索引表；`TemplateReader` 與 `TemplateCatalog` 統一暴露 `resolveSkillAlias`；`PartyMember` 移除寫死別名 switch，冷卻時間判定與技能查詢皆完全資料驅動。
> 4. **全專案 231 項測試 100% 綠燈通過**，包含 MUD/DRPG 雙軌防禦、陣法職能比對、三態資源消耗與冷卻別名查詢。

### 9.2 集中式遊戲 JSON 啟動期拓撲校驗 (`DataIntegrityValidator`) (P1)
- **現存缺陷**：
  跨檔案資料關聯（如怪物掉落物是否存在於 items、房間出口目標是否存在於 rooms、陣法技能是否存在於 skills）目前分散於各單元測試，缺乏啟動期的強制 Fail-Fast 攔截。
- **改善方案**：
  1. 建立 `DataIntegrityValidator` 標註 `@Component`，於 Spring 啟動完成後執行：
     - 校驗所有物品 ID、技能 ID、怪物 ID 唯一性。
     - 校驗怪物的掉落物 ID、商店商品 ID 均存在於 `ItemDefinition`。
     - 校驗房間雙向出口與地牢樓層座標合法性。
  2. 開發模式下發現斷鏈直接拋出 `DataValidationException` 阻止啟動；正式模式回報嚴重警報記錄，防止執行期 NullPointerException。

### 9.3 夥伴初始套路與貨棧商品全資料驅動 (P2)
- 擴充 `default_companions.json` 增加 `"learnedStances": [...]` 欄位，移除 `PartyService` 代碼保底給予。
- 將 `ShopCommand.INN_GOODS` 寫死的商品列表移至各區域 `shops.json`。
- 開局行囊預設道具由代碼給予改由開局設定檔驅動。

---

## 🎨 10. 前端巨石模組拆分與 Legacy 除役 (Frontend Modularization) — Priority: P2

> **來源**：`Claude_Code_Review.md` (§四)、`Codex_Code_Review.md` (§P2)、`Copilot_Code_Review.md` (§3.2)。
> **核心目標**：拆解 5,810 行巨石 CSS 與 3,141 行 Party 模組；盤查並正式除役 legacy 代碼。

### 10.1 巨石 `style.css` 模組化拆分 (5,810 行拆分) (P2)
- **現狀**：[style.css](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/css/style.css) 目前達 5,810 行（120 KB），維護成本極高。
- **改善方案**：依據職責與 UI 規範拆分為 5 大模組，於 `index.html` 按順序載入：
  1. `base.css`：全域 Design Tokens、變數、字型重置、排版基線（嚴格確保字級 $\ge 13\text{px}$）。
  2. `town.css`：城鎮主舞台、NPC 卡片網格、羅盤方向控制。
  3. `battle.css`：戰鬥競技場、5x5 敵方陣列、戰備甲板、交鋒微光效果。
  4. `menu.css`：1600x900 命冊主選單、分頁控制、裝備與 Gambit 介面。
  5. `hud.css`：底部 5 人小隊軌道、四維資源條（HP/MP/SP/SAN）、Toast 提示。

### 10.2 `party-modal.js` 垂直切片拆解 (3,141 行拆解) (P2)
- **現狀**：[party-modal.js](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/modals/party-modal.js) 達 3,141 行（159 KB），承載了隊員卡片、裝備穿脫、技能典籍、Gambit 戰術方針與陣法切換等多重責任。
- **改善方案**：拆解為清晰的職責子模組：
  - `party-equipment-tab.js`：裝備槽位切換與穿脫。
  - `party-tactics-tab.js`：Gambit 戰術方針設定與驗證。
  - `party-formation-tab.js`：陣型選取與角色站位拖曳。
  - `party-skills-tab.js`：技能熟練度展示與快捷鍵配置。
  - `party-modal.js` 僅作為外層選單標籤頁協調者。

### 10.3 歷史 Legacy 前端檔案盤查與正式除役 (P2)
- **現狀**：`static/legacy/drpg-view.js` 仍保留 126 KB (2,835 行) 的舊代碼，且 `index.html` 中仍有 7 處註解提及此檔案。
- **改善方案**：
  1. 確認現代模組（`town-view-panel.js`、`battle-panel.js`、`party-modal.js`）已 100% 接管所有渲染職責。
  2. 清理 `index.html` 中提及 `drpg-view.js` 的過期註解。
  3. 將 `static/legacy/` 目錄正式封存或從生產部署資源中排除，消除維護者的認知混淆。

### 10.4 探索舞台雙欄化與控制回歸羅盤 (P2)
- 左欄佔比約 60%（場景資訊 + 3x3 方向羅盤 + NPC 網格卡片 + 地面物品）。
- 右欄佔比約 40%（沉浸式 MUD 故事日誌與戰報，純粹自動捲動）。
- 方向鍵、探查等控制收納於羅盤周邊，杜絕 footer 重複佔位。

---

## ⚡ 11. 測試工程分層加速與可觀測性加固 (Testing & Observability) — Priority: P3

> **來源**：`Claude_Code_Review.md` (§六)、`Copilot_Code_Review.md` (§3.6)、`Codex_Code_Review.md` (§P3)。
> **核心目標**：測試架構下沉為純單元測試提高執行效率；補齊關鍵指標與稽核日誌。

### 11.1 測試套件分層與純單元測試下沉 (P3)
- **現狀**：專案有 61 個測試檔案、231 項測試，其中有 41 個測試類標註了重量級 `@SpringBootTest`，整套測試需耗時 25+ 秒，且需手動排空 Party 快取。
- **改善方案**：
  1. **測試分層**：
     - **Fast Unit Tests**：純演算法與領域模型（`DefenseResolver`、`XpProgressionService`、`FormationEngine`、`RandomUtil`），改寫為純單元測試（不啟動 Spring Context），反饋時間縮短至 3~5 秒。
     - **Spring Integration Tests**：資料庫持久化、Actor 訊息信箱、WebSocket 通訊保留 `@SpringBootTest`。
  2. **快取自動隔離**：建立 `BaseIntegrationTest`，於 `@AfterEach` 自動重置各類別單例與暫存快取，杜絕跨測試污染。

### 11.2 系統可觀測性與指標度量 (P3)
- 透過 `DomainMetricsPort` 擴充指標收集：
  - 活躍連線數、活躍房間 Actor 數、戰鬥迴圈進行中數量。
  - 指令限流與拒絕次數。
  - Write-Behind 佇列深度與 flush 失敗次數。
  - 存檔讀取失敗與遷移執行計數。
- 確保所有敏感資料（Token、票證、玩家密碼）絕不外洩至日誌中。

---

## 📋 改善項目實施優先序總表 (Execution Priority Matrix)

| 優先序 | 類別代碼 | 改善任務簡述 | 核心影響領域 | 對應章節 | 狀態 |
| :---: | :---: | :--- | :--- | :---: | :---: |
| **P0** | ROOM-01 | 房間狀態與地面掉落物持久化閉環 (`RoomService.record`) | 世界狀態防遺失 | §0.1 | ✅ 已完成 |
| **P0** | COMB-00 | 圓桌戰鬥防禦切片累積總機率收斂 (消除 Crit/Hit 湮滅) | 核心戰鬥數學 | §0.2 | ✅ 已完成 |
| **P0** | ASYNC-01 | Write-Behind 佇列有界背壓與優雅關閉無競態 | 系統併發安全 | §0.3 | ✅ 已完成 |
| **P0** | SEC-01 | 前端未轉義動態插值修補與 53 個 inline 事件委派改造 | 前端安全性與 CSP | §0.4 | ✅ 已完成 |
| **P1** | CANON-01 | Phase 2: Canonical Item (`Definition`+`Instance`+`View`) | 物品單一真相源 | §1.1 | ✅ 已完成 |
| **P1** | CANON-02 | Phase 3: Single `SkillDefinition` (MUD/DRPG Facets) | 技能定義合併 | §1.2 | ✅ 已完成 |
| **P1** | CANON-03 | Phase 4: 徹底移除 `TemplateRepository` 靜態呼叫 (DI) | 測試與架構隔離 | §1.3 | ✅ 已完成 |
| **P1** | CANON-04 | Phase 5: 基於 Snapshot / Outcome 的角色戰鬥同步 | 併發與資料一致性 | §1.4 | ✅ 已完成 |
| **P1** | COMB-01 | 圓桌單次擲骰收斂、Stamina 精力破防、主角潛能點加點 | 戰鬥深度與成長 | §2.3~2.6 | ✅ 已完成 |
| **P1** | MUD-01 | MUD 怪物 AI、擊殺升級閉環、Buff 引擎與規則補齊 | 世界規則與 AI | §3.1~3.4 | ✅ 已完成 |
| **P1** | UI-01 | 全域佈局重構、三種資訊密度 (字級 $\ge 13\text{px}$) 與雙軌契約 | 介面佈局與資源呈現 | §4.1~4.3 | ✅ 已完成 |
| **P1** | ARCH-01 | Domain 層反向依賴反轉 (Phase 10 Clean Architecture) | 六角形純淨架構 | §5.1 | ✅ 已完成 |
| **P1** | ACT-01 | 收斂 Room Actor 狀態修改通道 (修復 `removePlayer` 雙軌修改) | Actor 狀態單一所有 | §7.1 | 🔲 待執行 |
| **P1** | ACT-02 | 戰鬥虛擬執行緒 Executor 治理與 5 分鐘逾時斷路器 | 執行緒防洩漏 | §7.2 | 🔲 待執行 |
| **P1** | SAVE-01 | 檔案式存檔引入 `schemaVersion` 與版本遷移適配器 | 存檔平滑相容升級 | §8.1 | 🔲 待執行 |
| **P1** | SAVE-02 | 壞檔防禦標記與防覆寫保護 (區分 `empty` 與 `corrupted`) | 進度防毀損保護 | §8.2 | 🔲 待執行 |
| **P1** | DATA-01 | 消除職業反擊特例、陣型比對與技能別名硬編碼 | 100% 資料驅動 | §9.1 | 🔲 待執行 |
| **P1** | VAL-01 | 集中式遊戲 JSON 啟動期拓撲校驗 (`DataIntegrityValidator`) | 啟動期 Fail-Fast | §9.2 | 🔲 待執行 |
| **P2** | CSS-01 | 5,810 行巨石 `style.css` 拆分為 5 大模組 | 前端可維護性 | §10.1 | 🔲 待執行 |
| **P2** | FE-01 | 3,141 行 `party-modal.js` 垂直切片模組化拆分 | 前端複雜度解耦 | §10.2 | 🔲 待執行 |
| **P2** | LEG-01 | `static/legacy/drpg-view.js` 正式除役與註解清理 | 代碼庫整潔度 | §10.3 | 🔲 待執行 |
| **P2** | UI-02 | 探索舞台雙欄化與控制回歸羅盤 | 操作流暢度 | §10.4 | 🔲 待執行 |
| **P2** | DATA-02 | 夥伴套路、初始道具、貨棧商品移入 JSON | 資料配置徹底化 | §9.3 | 🔲 待執行 |
| **P3** | TEST-01 | 核心數值與領域規則下沉為純單元測試 (加速 CI) | 測試工程效能 | §11.1 | 🔲 待執行 |
| **P3** | OBS-01 | 系統度量指標擴充與敏感資料日誌審核 | 系統觀測性加固 | §11.2 | 🔲 待執行 |
| **P4** | NET-01 | WebSocket 本機綁定模式與公開模式票證驗證 | 網路安全邊界 | §6.1, §6.2 | 🔲 待執行 |
| **P4** | NET-02 | WebSocket 64KB 訊息長度上限與每秒 20 次指令限流 | 資源保護與抗洪水 | §6.2 | 🔲 待執行 |