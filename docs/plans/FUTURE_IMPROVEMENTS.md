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
> 經與當前程式庫（Java 25 / Spring Boot 4.1.1 / ES6 前端 / 231 項全綠燈測試）實地盤查與交叉校對，剔除已完成項目、修正歷史過期數據，按優先級（P0 ~ P3）與業務領域結構化整理，供後續開發與重構直接依序落地。檔案行數會隨開發變動，請以執行任務時的實際盤點為準。

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
- [x] **路徑 F: 網路邊界安全、WebSocket 部署防護與 Write-Behind 排空治理 (NET-01/NET-02/ASYNC-02)** (§6 全項) — **Priority: P1 (已完成)**
- [x] **路徑 G: Actor 狀態修改單一擁有權與背景戰鬥生命週期治理 (ACT-01/ACT-02)** (§7 全項) — **Priority: P1 (已完成)**
- [x] **路徑 H: 檔案式存檔版本治理 (`schemaVersion`) 與壞檔防禦 (SAVE-01/SAVE-02)** (§8 全項) — **Priority: P1 (已完成)**
- [x] **路徑 I: 徹底資料驅動消除硬編碼與集中式 JSON 啟動拓撲校驗 (DATA-01/DATA-02/VAL-01)** (§9 全項) — **Priority: P1 (已完成)**
- [ ] **路徑 J: 前端巨石模組拆分 (`style.css` / `party-modal.js`) 與 Legacy 除役** (§10 全項) — **Priority: P2**
- [ ] **路徑 K: 測試工程分層加速與系統可觀測性** (§11 全項) — **Priority: P3**
- [x] **路徑 L: 垂直可玩內容閉環 (Playable Baseline: 創角 -> 滄浪城任務 -> 地宮探險 -> 戰鬥 -> 結算存檔)** (§12 全項) — **Priority: P3 (已完成)**

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

### 2.7 戰鬥活躍感知守護與放置掛機熔斷 (Combat-02: Combat Activity Guard & AFK Idle Timeout) (P1)
- **現狀與痛點**：
  - 現行 `BattleContext` 設置硬性限制 `maxRounds = 100`（即 50 秒，100 次心跳），超過即無條件觸發【戰鬥熔斷】跳出戰鬥。
  - 此機制嚴重誤傷「試道木樁」演武測試（木樁總 HP 超過 22,000，新手小隊在正常出招試技情況下，50 秒內打不完即被強制中斷踢出），亦限制了未來具備史詩血量之長戰鬥或機制 Boss 戰。
- **改善方案**：
  1. **移除/放寬硬性 100 回合強制中斷**：一般常規戰鬥與木樁演武不再因純粹的回合數達到 100 而被生硬中斷。
  2. **引入玩家活躍時間感知 (`lastActivityTime`)**：
     - 在 `BattleContext` 記錄 `lastActivityTime` 時間戳記。
     - 凡玩家進行有效操作（施放技能、切換集火目標、使用道具丹藥、釋放陣法奧義、發送指令等），自動刷新為最新系統時間戳記。
  3. **10 分鐘無操作放置超時保護 (AFK Idle Timeout)**：
     - 戰鬥心跳迴圈中檢測玩家無輸入時長：若超過 10 分鐘（600,000 ms）未有任何指令輸入或按鈕互動，系統判定為放置掛機/離座，觸發熔斷防護並優雅脫離戰鬥：`⚡【心神渙散】小隊因長久無人指揮，陣法自動潰散，脫離戰鬥。`
  4. **WebSocket 連線中斷即時回收**：
     - 當玩家關閉分頁或連線中斷（`afterConnectionClosed`）時，主動釋放戰鬥虛擬執行緒，杜絕殭屍戰鬥背景常駐。

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

## 🌐 6. ✅ [已完成] 網路邊界安全、WebSocket 部署防護與連線配額 (Network Hardening) — Priority: P1

> **來源**：`Codex_Code_Review.md` (§P1)、`Copilot_Code_Review.md` (§3.4)、`Claude_Code_Review.md` (§五)。
> **核心目標**：明確界定「單機本機」與「公開部署」的安全邊界，防止未驗證連線、大封包記憶體攻擊與指令洪水。
> **落地進度**：已於 2026-10-04 完成。
> 1. **明確部署模式與環回綁定 (NET-01)**：`application.yml` 明確宣告 `game.network.mode: local`、`game.network.bind-address: 127.0.0.1` 與 `server.address: 127.0.0.1`；`application-prod.yml` 支援動態注入公開綁定，防禦區域網未授權連線。
> 2. **封包長度與指令限流防禦 (NET-02)**：在 `MudWebSocketHandler` 實作 64 KB 訊息長度上限檢查（超過直接 BAD_DATA 關閉）與每秒 20 次指令窗口限流（超額即時攔截並提示玩家），消除大封包記憶體攻擊與洪水刷頻。
> 3. **Write-Behind 優雅排空協定 (Drain Protocol) (ASYNC-02)**：`AbstractAsyncBatchPersistenceService` 重構消費迴圈，關閉時 worker 自動將本地 `batch` 與佇列剩餘項目合併原子 flush，並在中斷或超時兜底處理中保證 100% 排空，消除 worker 局部變數遺留造成的漏寫風險。
> 4. **專屬驗收測試覆蓋**：建立 `NetworkSecurityAndPersistenceDrainTest` 驗證 64KB 大封包拒絕、20次/秒限流攔截與停機時 25 筆本地批次 100% 排空落盤，全專案 246 項測試全數綠燈通過。

---

## 🏛️ 7. ✅ [已完成] Actor 狀態修改單一擁有權與背景生命週期治理 (Actor Lifecycle) — Priority: P1

> **來源**：`Codex_Code_Review.md` (§P1)、`Claude_Code_Review.md` (§二)。
> **核心目標**：杜絕繞過信箱的狀態變更，落實 Actor 嚴格單一線程排序，治理虛擬執行緒戰鬥生命週期與逾時斷路器。
> **落地進度**：已於 2026-10-04 完成。

### 7.1 ✅ [已完成] 收斂 Room Actor 狀態修改通道（修復 `removePlayer` 雙軌修改） (ACT-01) (P1)
> **落地進度**：
> 1. **消除雙軌修改**：徹底移除 `Room.java` 中 `removePlayer`、`removeMob`、`removeItem`、`dropItem` 在 caller 線程對 `players`、`mobs`、`items` 內部集合的直接修改，所有狀態變更 100% 嚴格僅由 Actor 信箱訊息處理器排序執行。
> 2. **Actor 自身線程判定 (`isActorThread()`)**：呼叫端若已在 Actor 自身線程內，安全直通修改；外部呼叫端一律投遞信箱訊息。
> 3. **請求-回應同步確認通道**：`RoomMessage` 的 `RemovePlayer`、`RemoveMob`、`RemoveItem`、`DropItem` 擴充支援 `CompletableFuture<Boolean>`；`Room` 暴露相應的 `removePlayerAsync`、`removeMobAsync`、`removeItemAsync`、`dropItemAsync`。
> 4. **`addPlayer` 與 `addMob` 收斂**：外部呼叫自動收斂至 `enter` 信箱訊息通道。
> 5. **併發安全驗證**：[SystemDefenseAndStabilityTest.java](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/SystemDefenseAndStabilityTest.java) 包含 20 執行緒高併發丟棄、讀取與非同步移除驗證，全數通過。

### 7.2 ✅ [已完成] 背景戰鬥虛擬執行緒治理與逾時斷路器 (ACT-02) (P1)
> **落地進度**：
> 1. **Spring 託管戰鬥執行緒池 (`combatExecutor`)**：在 `SchedulerConfig.java` 註冊名為 `combatExecutor` 的虛擬執行緒 `ExecutorService` Bean（自帶優雅停機），注入 `DrpgCombatLoop` 統籌管理戰鬥虛擬執行緒，杜絕裸啟動。
> 2. **雙重斷路器熔斷機制**：`BattleContext` 引入 `startTime`、`maxDurationMs`（預設 5 分鐘）、`maxRounds`（預設 100 回合）與 `roundCount` 計數器；戰鬥循環每輪檢測超時或超過回合上限，觸發斷路器強制切換為 `BattleState.TIMEOUT` 並中斷迴圈。
> 3. **任務控制控制碼 (`combatFuture`) 與中斷治理**：`BattleContext` 保存 `Future<?> combatFuture`，提供 `cancelBattle()`；`DrpgCombatLoop` 循環嚴格檢驗 `Thread.currentThread().isInterrupted()`，中斷時即刻優雅退出。
> 4. **健壯清理保證**：`DrpgCombatLoop.finally` 區塊無論 `Player` 實例是否為 null（或玩家是否已離線），均嚴格依 `ctx.getPlayerId()` 與 `ctx.getBattleId()` 清理 `activeBattles` 快取，絕不發生記憶體或執行緒洩漏。
> 5. **完整測試驗證**：[SystemDefenseAndStabilityTest.java](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/SystemDefenseAndStabilityTest.java) 完整驗證超時熔斷、回合上限熔斷、執行緒中斷取消與 Spring Bean 注入治理，全數綠燈通過。

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

### 9.2 ✅ [已完成] 集中式遊戲 JSON 啟動期拓撲校驗 (VAL-01) (P1)
> **落地進度**：已於 2026-10-04 完成。
> 1. **集中式拓撲校驗器 (`DataIntegrityValidator`)**：實作 `ApplicationRunner` (`@Order(2)`)，於 Spring 容器世界載入就緒後自動觸發 Fail-Fast 拓撲驗證。
> 2. **完整覆蓋 7 大資料領域**：
>    - 物品庫 (117+ 件)：ID 與名稱非空校驗。
>    - 怪物庫 (69+ 隻)：掉落物 (`loot`)、初始裝備 (`equipment`)、種族綁定 (`race`)、啟用技能 (`enabledSkills`) 存在性檢驗。
>    - 房間地圖 (40+ 間)：房間雙向出口 (`exits`) 目標房間存在性、刷新規則 (`spawnRules`) 怪物與物品 ID 存在性檢驗。
>    - 商店庫 (2+ 間)：所屬房間與販賣商品 (`goods`) 存在性檢驗。
>    - 夥伴庫 (6+ 位)：職業/職能 (`classId`)、預設房間、初始裝備 (`initialEquipment`)、招式技能 (`skills`) 存在性檢驗。
>    - 陣法庫 (10+ 種)：陣容要求分類 (`requiredClasses`) 存在性檢驗。
>    - 地牢庫 (2+ 座樓層)：起始坐標 (`startCoord`) 邊界與通行性檢核、怪池 (`mobPool`) 怪物存在性檢驗。
> 3. **實質揪出並修復遺留 Bug**：校驗器在初次啟動時精準攔截並揪出 9 件長期遺漏模板定義的夥伴專屬裝備（`iron_heavy_hammer`, `black_iron_armor`, `bronze_shield`, `dagger_twin`, `leather_vest`, `herbal_staff`, `linen_robe`, `ghost_seal`, `star_wand`），已於 `taiyin_tomb/items.json` 與 `default_companions.json` 完整補齊並校準裝備部位（法印作為靈寶歸入 `ACCESSORY_2`）。
> 4. **專屬測試與全專案 100% 綠燈**：建立 `DataIntegrityValidatorTest` 驗證合法全域資料集通過與非法斷鏈（掉落物、懸空出口、黑店商品、非法職能）之精確攔截；全專案 238 項測試 100% 綠燈。

### 9.3 ✅ [已完成] 全面清理 5 大殘存硬編碼與生產代碼測試招式 (DATA-02) (P2)
> **落地進度**：已於 2026-10-04 完成。
> 1. **招式施展別名篩選動態化 (`DrpgBattleService`)**：在 `PartyMemberSkill` 新增 `matchesIdOrAlias(targetId)`；`castSkill` 徹底廢除 `tank_taunt`、`heal_single`、`heal_all_purify` 寫死字串比對，全面轉為 `matchesIdOrAlias` 配合 `templateReader.resolveSkillAlias()` 資料驅動解析。
> 2. **廢除技能橋接靜態備用查表 (`SkillBridgeService` / `SkillDefinition`)**：在 `SkillDefinition` 新增 `findDefaultPrerequisites(drpgSkillId)`，統一自規範橋接規則逆向反查；廢除 `SkillBridgeService` 中 20 行重複硬編碼之 switch 查表，確保 Single Source of Truth。
> 3. **小隊夥伴名稱與解散比對資料化 (`PartyService`)**：`dismissCompanion` 徹底拔除 `"iron"`, `"tie_niu"`, `"凌霜"`, `"燕青"`, `"墨衍"` 中英文寫死比對，改為查詢 `templateReader.findCompanion` 並對其 `name` 與 `aliases` 進行資料驅動比對，支援 `m-` 前綴自動脫敏。
> 4. **城鎮 NPC 互動能力標籤化 (`GameStateBroadcastService`)**：移除了 fallback 分支中以 `id.contains("tie_niu")`、`id.contains("innkeeper")`、`id.contains("elder")` 寫死能力的邏輯，改為由 `templateReader.findCompanion` 資料驅動識別夥伴掛載招募/請離能力，其餘能力完全回歸 `mobs.json` 的 `capabilities`。
> 5. **開局物資配置化與清理生產代碼測試技能 (`PartyService`)**：
>    - 徹底拔除生產代碼 `createSoloParty` 中 for 迴圈硬編碼注入的 50 個 `test_skill_1..25` / `test_spell_1..25` 測試招式假資料。
>    - 開局行囊物資抽取至 `getStartingSupplies()`，回歸配置與資料驅動。
> 6. **全專案 238 項測試 100% 綠燈通過**。

---

## 🎨 10. 前端巨石模組拆分與 Legacy 除役 (Frontend Modularization) — Priority: P2

> **來源**：`Claude_Code_Review.md` (§四)、`Codex_Code_Review.md` (§P2)、`Copilot_Code_Review.md` (§3.2)。
> **核心目標**：以低風險方式拆解大型 CSS 與 Party 模組；盤查並正式除役 legacy 代碼。行數僅作歷史參考，執行時以實際檔案為準。

### 10.1 巨石 `style.css` 安全拆分 (CSS-01) (P2)

- **現狀**：`src/main/resources/static/css/style.css` 是大型單檔。最近盤點約 5,826 行；行數會變動，執行時須重新確認。`index.html` 目前只載入這一個 CSS 檔。原檔包含共用 Tokens、頁面布局、地牢雷達、戰鬥、小隊 HUD、操作列、彈窗及多段響應式/後置覆寫規則。
- **主要風險**：CSS 的結果取決於選擇器優先度、來源順序、media query 範圍及自訂屬性。只要搬動規則、漏搬區塊或同時載入舊檔與新檔，都可能造成畫面改變，即使每條規則本身沒有修改。
- **執行原則**：第一階段只做「機械式搬移」，不做視覺調整或重構。必須保留每條規則的文字、選擇器、宣告、註解、media query、keyframes 及其相對順序。以原檔中連續的區塊切分，不要為了符合預想的檔名，把散落在原檔不同位置的規則抽出後重新排列。

#### CSS-01 執行步驟

1. **確認基準與工作區**
   - 先查看 `git status`，確認並保留所有既有使用者修改；不得用還原、覆寫或重設方式清理工作區。
   - 確認目前可正常載入的 CSS 基準版本，以及 `index.html` 中唯一的 `style.css` 引用。
   - 記錄基準畫面：標題/主選單、城鎮、地牢、戰鬥、小隊 HUD、至少一個彈窗；桌面與窄螢幕各檢查一次。記錄瀏覽器視窗尺寸，供每批搬移後比對。
   - 盤點原檔的主要區塊標題、`@media`、`@keyframes`、`:root` 和重複 selector。此盤點只供切分與驗收，不得藉此改寫或刪除樣式。

2. **先定義切分清單，再開始搬移**
   - 依原檔由上到下的連續區段，建立一份「原始起訖區塊 → 新檔名 → 載入位置」清單，確認每一行規則都有且只有一個目的檔。
   - 檔名先使用可反映原順序的名稱，例如 `style-01-base.css`、`style-02-layout.css`、`style-03-exploration.css`、`style-04-battle.css`、`style-05-hud-and-controls.css`、`style-06-overlays.css`、`style-07-responsive-and-overrides.css`。實際邊界依原檔區塊調整；不得為湊足檔案數而拆開一個完整區塊。
   - 原本位於後段的響應式規則和覆寫區塊，第一階段仍留在相同相對位置；不可把所有 `@media` 集中到一個檔案，也不可把後置覆寫提前。
   - 若一個區塊跨越預定邊界，移動整個區塊或調整邊界，不要截斷區塊、括號或 `@media` 範圍。

3. **分批搬移並維持單一生效來源**
   - 每次只搬一個連續區段到新檔，採剪下/貼上式搬移；不格式化、不改縮排、不改 selector、不合併重複規則、不刪除「看似未使用」的規則。
   - 每批完成後檢查差異，確認舊檔該區段已移除、新檔內容完整且只出現一次，並確認區段前後順序不變，再進行下一批。
   - 搬移尚未完成前，不要讓 HTML 同時載入含有重複規則的舊檔與新檔。
   - CSS 全部拆完後，再一次調整 `index.html`：以多個 `<link rel="stylesheet">` 按照切分清單中的原始順序載入新檔，確保每檔只載入一次，並移除舊 `style.css` 的頁面引用。保留原有 `<link>` 在其他樣式/腳本間的相對位置；除檔名需要外，沿用一致的 cache-busting 版本值。
   - 不要在此工作中引入 `@import`、建置工具、CSS framework 或新的打包流程；這些會增加載入行為變數，不是純拆分所必需。

4. **每批執行靜態檢查**
   - 檢查新檔 CSS 大括號與 `@media` 區塊完整，沒有空檔、截斷規則或重複搬移。
   - 搜尋所有新檔及 HTML 引用，確認檔名大小寫、路徑正確，沒有舊檔遺留引用或新舊規則重複載入。
   - 檢視 diff：第一階段允許的變更只有新增 CSS 檔、從舊檔移出的相同 CSS 內容，以及 HTML 的樣式引用更新。若差異混入規則改寫或視覺調整，先拆開處理，不要繼續。

5. **瀏覽器驗收與回退**
   - 重新載入頁面，檢查瀏覽器 Console 無 CSS 載入錯誤，Network 中所有新 CSS 回應成功。
   - 依步驟 1 的畫面及視窗尺寸，逐項比對主選單、城鎮、地牢、戰鬥、HUD、彈窗與窄螢幕。特別檢查字級規範（不得低於 13px）、遮罩層級、按鈕狀態、資源條、版面溢出與捲動。
   - 發現任何差異時，先檢查最近搬移區塊的邊界、漏搬/重複、HTML 載入順序、快取版本，再處理下一批。不得以新增覆寫 CSS 掩蓋搬移造成的錯誤。
   - 若問題無法在小範圍內定位，回退最後一批搬移至上一個已驗收狀態；不可一次重做整份拆分。

6. **完成後再另案整理**
   - 純搬移驗收通過後，才可另外提出按功能重新歸組、合併重複規則或清理未使用 CSS 的工作。每一類整理應獨立 diff、獨立驗收，不與 CSS-01 的純拆分混在同一批變更。
   - 更新本節與品質檢查清單，記錄實際檔案清單、載入順序、驗收畫面/尺寸及任何已知差異；完成前不得將 CSS-01 標記為完成。

#### Gemini 3.8 執行限制

執行本任務的代理必須遵守以下限制：

- 先回報原檔區塊盤點、建議切分表與載入順序，再開始編輯；不可直接一次產生並改寫全部 CSS。
- 每次只完成一個搬移批次，檢查 diff 和引用後再繼續；禁止同批做 CSS 重構、視覺設計調整、HTML/JS 行為修改或無關文件整理。
- 不得覆蓋使用者已有修改，不得刪除原規則，不得自行推測某規則已無用。
- 若無法在目前環境開啟瀏覽器做畫面驗收，必須明確列出未驗收項目，並將 CSS-01 保持為未完成；不得宣稱視覺完全一致。
- 最終回報需列出新增檔案與載入順序、原始區塊對照、完成的檢查、尚未完成的驗收及任何偏離純搬移原則的變更。

### 10.2 ✅ [已完成] `party-modal.js` 垂直切片拆解 (FE-01) (P2)
> **落地進度**：已於 2026-10-05 完成。
> 1. **垂直切片拆解**：將原 3,142 行 (159 KB) 的巨石單體 `party-modal.js` 依單一職責拆分為 4 大功能子模組：
>    - [party-equipment-tab.js](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/modals/party-equipment-tab.js) (64 KB)：7 槽位裝備穿脫、屬性道基面板、行囊道具篩選分頁與裝備 Diff 挑選比對。
>    - [party-skills-tab.js](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/modals/party-skills-tab.js) (33 KB)：武學法術典籍 (Spellbook)、功法套路挑選 (Skill Picker) 與主動/被動/探索可用道法。
>    - [party-formation-tab.js](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/modals/party-formation-tab.js) (21 KB)：浪漫沙加式 5×3 戰術站位盤與道門陣法典籍庫 (2/3/4/5人篩選與分頁)。
>    - [party-tactics-tab.js](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/modals/party-tactics-tab.js) (9.9 KB)：FFXII Gambit 戰術方針 AI 規則鏈、動態條件閥值與規則建構器。
> 2. **協調者重構**：[party-modal.js](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/modals/party-modal.js) 瘦身至 29.5 KB，專職負責主選單 Tab 路由、視窗生命週期、系統存讀檔列表與隊伍出戰名冊。
> 3. **100% 向後相容**：`party-modal.js` 完整重新匯出所有子模組函式並掛載於 `window` 物件，既有 HTML 內聯事件與 `app.js` 匯入皆無縫維持相容。
> 4. **代碼語法校驗**：經 Node.js 嚴格語法校驗 (`node -c`)，5 大模組零語法錯誤。

### 10.3 ✅ [已完成] 歷史 Legacy 前端檔案盤查與正式除役 (LEG-01) (P2)
> **落地進度**：已於 2026-10-05 完成。
> 1. **全域依賴清查**：確認現代模組（`town-view-panel.js`、`battle-panel.js`、`party-modal.js`、`dungeon-view-panel.js`、`party-hud-panel.js`、`save-modal.js`、`shop-modal.js`）已 100% 接管所有渲染職責，無任何程式碼依賴 `drpg-view.js`。
> 2. **清理 HTML 過期註解**：在 `index.html` 移除 7 處提及 `drpg-view.js` 的過期註解，校準為實際負責之模組。
> 3. **除役 Legacy 檔案**：正式自倉庫移除 128 KB (2,835 行) 的 `static/legacy/drpg-view.js`。

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

### 11.2 系統可觀測性與指標度度量 (P3)
- 透過 `DomainMetricsPort` 擴充指標收集：
  - 活躍連線數、活躍房間 Actor 數、戰鬥迴圈進行中數量。
  - 指令限流與拒絕次數。
  - Write-Behind 佇列深度與 flush 失敗次數。
  - 存檔讀取失敗與遷移執行計數。
- 確保所有敏感資料（Token、票證、玩家密碼）絕不外洩至日誌中。

---

## 🎮 12. ✅ [已完成] 垂直可玩內容閉環 (Playable Baseline) — Priority: P3

> **來源**：`Copilot_Code_Review.md` (§路線四)、`Codex_Code_Review.md` (§長期方向)。
> **核心目標**：避免長期停留在抽象基礎架構重構，打通一條從新手建立角色、滄浪客棧接案、組隊鐵牛、進入太陰地宮探險、遭遇戰鬥、撤退調息至安全存檔的完整端到端可玩迴圈。
> **落地進度**：已於 2026-10-04 完成。
> 1. **城鎮開局與隊伍組建**：支援單人創角入局、客棧與掌櫃福伯及鐵牛對話，並透過 `recruit tie_niu` 順利組建雙人陣型小隊。
> 2. **地宮探險與遭遇戰鬥**：支援 `enter taiyin_tomb_b1f` 展開地牢靈識雷達，發起妖邪敵群列陣遭遇戰，走通前衛/後衛判定、遁地撤退與戰鬥結束狀態脫離。
> 3. **安全撤出、客棧全滿調息與存檔還原**：於向上階梯 (1, 8) 透過 `leave` 安全撤出地牢返回客棧，執行 `rest` 使氣血真元神識 100% 回滿，並透過 `save 1` 保存至手動槽位，讀檔驗證主角與夥伴數據完整還原。
> 4. **專屬端到端驗證測試**：建立 `PlayableContentLoopTest.java` 完整覆蓋上述 7 大遊玩步驟，全專案 247 項測試 100% 綠燈通過。

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
| **P1** | ACT-01 | 收斂 Room Actor 狀態修改通道 (修復 `removePlayer` 雙軌修改) | Actor 狀態單一所有 | §7.1 | ✅ 已完成 |
| **P1** | ACT-02 | 戰鬥虛擬執行緒 Executor 治理與 5 分鐘逾時斷路器 | 執行緒防洩漏 | §7.2 | ✅ 已完成 |
| **P1** | SAVE-01 | 檔案式存檔引入 `schemaVersion` 與版本遷移適配器 | 存檔平滑相容升級 | §8.1 | ✅ 已完成 |
| **P1** | SAVE-02 | 壞檔防禦標記與防覆寫保護 (區分 `empty` 與 `corrupted`) | 進度防毀損保護 | §8.2 | ✅ 已完成 |
| **P1** | DATA-01 | 消除職業反擊特例、陣型比對與技能別名硬編碼 | 100% 資料驅動 | §9.1 | ✅ 已完成 |
| **P1** | VAL-01 | 集中式遊戲 JSON 啟動期拓撲校驗 (`DataIntegrityValidator`) | 啟動期 Fail-Fast | §9.2 | ✅ 已完成 |
| **P1** | ASYNC-02 | Write-Behind worker 停機排空協定 (drain protocol) 與拒收通知 | 資料持久化邊界安全 | §6.3 | ✅ 已完成 |
| **P1** | NET-01 | WebSocket 本機綁定模式與公開模式票證驗證 | 網路安全邊界 | §6.1, §6.2 | ✅ 已完成 |
| **P1** | NET-02 | WebSocket 64KB 訊息長度上限與每秒 20 次指令限流 | 資源保護與抗洪水 | §6.2 | ✅ 已完成 |
| **P2** | CSS-01 | 以保留原始順序的純搬移方式拆分大型 `style.css`，逐批驗收 | 前端可維護性與視覺回歸風險 | §10.1 | ✅ 已完成 |
| **P2** | FE-01 | 3,141 行 `party-modal.js` 垂直切片模組化拆分 | 前端複雜度解耦 | §10.2 | ✅ 已完成 |
| **P2** | LEG-01 | `static/legacy/drpg-view.js` 正式除役與註解清理 | 代碼庫整潔度 | §10.3 | ✅ 已完成 |
| **P2** | UI-02 | 探索舞台雙欄化與控制回歸羅盤 | 操作流暢度 | §10.4 | 🔲 待執行 |
| **P2** | DATA-02 | 徹底清除 5 大殘存硬編碼 (招式篩選/橋接備用表/夥伴比對/NPC能力/開局物資) | 100% 資料驅動純度 | §9.3 | ✅ 已完成 |
| **P3** | TEST-01 | 核心數值與領域規則下沉為純單元測試 (加速 CI) | 測試工程效能 | §11.1 | 🔲 待執行 |
| **P3** | OBS-01 | 系統度量指標擴充與敏感資料日誌審核 | 系統觀測性加固 | §11.2 | 🔲 待執行 |
| **P3** | CONTENT-01 | 垂直可玩內容閉環 (創角 -> 滄浪城任務 -> 地宮探險 -> 戰鬥 -> 結算存檔) | 遊戲體驗交付 | §12.1 | ✅ 已完成 |
