# 專案歷史架構改善與演進紀錄庫 (Consolidated Future Improvements History)

> **文件定位與目的**：
> 本文件收錄《深淵仙途》專案自創立以來所有**已落地、已驗收並全數通過單元與整合測試（376+ 測試全綠燈）**之架構改善、模型解耦、性能調優、存檔加固與單機單體化戰役。  
> 未來待實施之動態演化任務與純單機 DRPG 規劃，請參閱現行 👉 [**`FUTURE_IMPROVEMENTS.md`**](../3_task_lists/FUTURE_IMPROVEMENTS.md)。

---

## 📌 歷史演進指針 (Navigation)
- 👉 最新版本發布與詳細版本紀錄：請參閱 [**`CHANGELOG.md`**](../../CHANGELOG.md)。
- 👉 當前開發單一焦點與微任務：請參閱 [**`../4_active_tasks/ACTIVE_TASKS.md`**](../4_active_tasks/ACTIVE_TASKS.md)。

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
  - 原本的 `maxRounds = 100` 硬性限制已放寬；目前 `BattleContext.maxRounds` 為 50,000。
  - **目前實作核對（2026-10-05）**：`BattleContext.maxDurationMs` 仍預設 300,000ms，`isTimedOut()` 會在戰鬥開始 5 分鐘後逾時，並不因玩家持續操作而延長；此行為須與 10 分鐘閒置熔斷設計一併決議。
- **改善方案**：
  1. **明確定義回合與絕對時限政策**：保留足夠大的回合上限作為極端安全閥；一般常規戰鬥與木樁演武不應因固定 5 分鐘而被生硬中斷，若需絕對時限，應依戰鬥模式設定並與閒置時限分離。
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

> **來源**：歷史檢視（Codex／Claude）；最新 WebSocket 與持久化風險見 [`docs/references/Copilot_Review.md`](../references/Copilot_Review.md) §二 #1–2。
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

> **來源**：歷史檢視（Codex／Claude）；最新 Write-Behind 風險與驗收建議見 [`docs/references/Copilot_Review.md`](../references/Copilot_Review.md) §二 #1、§四。
> **落地進度**：已於 2026-10-03 完成。
> 1. **資料模型版本治理 (`CURRENT_SCHEMA_VERSION = 1` / `SaveMigrationPipeline`)**：
>    - [SaveData.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/save/model/SaveData.java) 引入 `CURRENT_SCHEMA_VERSION = 1` 與 `schemaVersion` 欄位（`@Builder.Default`）。
>    - 實作 [SaveMigrationPipeline.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/save/migration/SaveMigrationPipeline.java) 與 `SaveMigration` 介面，內建 v0 $\to$ v1（補齊槽位與裝備結構）與 v1 $\to$ v2（補齊九大抗性矩陣與罡氣上限）的平滑自動遷移管線。
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

> **來源**：歷史檢視（Claude／Codex）；最新 fallback 護欄發現見 [`docs/references/Copilot_Review.md`](../references/Copilot_Review.md) §二 #3。
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

> **來源**：歷史檢視（Claude／Codex）；前端模組現況與文件治理建議見 [`docs/references/Copilot_Review.md`](../references/Copilot_Review.md) §三。
> **核心目標**：以低風險方式拆解大型 CSS 與 Party 模組；盤查並正式除役 legacy 代碼。行數僅作歷史參考，執行時以實際檔案為準。

### 10.1 ✅ [已完成] 巨石 `style.css` 安全物理拆分 (CSS-01) (P2)
> **落地進度**：已於 2026-10-05 完成安全物理切分。
> 1. **分模組連續映射**：依 cascade 原始由上至下連續順序切分為 6 個獨立模組（無 `@import`、無規則改寫）：
>    - `style-01-tokens-and-shell.css` (24 KB)：CSS 變數、共用 Token、基底重置與 Shell 骨架。
>    - `style-02-combat-arena.css` (23.7 KB)：戰鬥 5x5 敵陣、5x3 戰陣甲板與交鋒微光。
>    - `style-03-battle-actions.css` (24.7 KB)：戰鬥情境動作列、技能快捷槽位與 CD 倒數。
>    - `style-04-main-menu.css` (21.9 KB)：1600x900 主選單外框、8 大 Tab 頁面結構。
>    - `style-05-town-and-hub.css` (15 KB)：城鎮生活視圖、3x3 方位羅盤與 NPC 卡片。
>    - `style-06-shop-and-responsive.css` (12.3 KB)：商店交易彈窗與全域 `@media` 響應式佈局。
> 2. **載入管道更新**：`index.html` 按照切分順序以 6 個 `<link rel="stylesheet">` 引入並加上 cache-busting (`?v=1.8.0`)。
> 3. **資產維護注意事項**：原根目錄 120 KB 的 `style.css` 目前作為備份基準保留在倉庫中，待全面回歸確認無誤後再行封存或除役，避免誤傷仍被 legacy 視圖參考的資產。

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

### 10.4 ✅ [已完成] 探索舞台雙欄化與控制回歸羅盤 (P2)
- 左欄佔比精準 60% (`flex: 3;` 場景資訊 + 3x3 方向羅盤 + NPC 網格卡片 + 地面物品)。
- 右欄佔比精準 40% (`flex: 2;` 沉浸式 MUD 故事日誌與戰報，純粹自動捲動)。
- 方向鍵、探查、調息、行囊、選單等控制完整收納於羅盤 3x3 隅角周邊，地牢雷達與前方探查緊湊並列，杜絕 footer 重複佔位。
- 底部情境列純化為輕量狀態引導列，戰鬥模式無縫切換專屬指令。

---


## 🎮 12. ✅ [已完成] 垂直可玩內容閉環 (Playable Baseline) — Priority: P3

> **來源**：歷史檢視（Codex）；單機切片與長期演進方向見最新 [`docs/references/Copilot_Review.md`](../references/Copilot_Review.md) §四。
> **核心目標**：避免長期停留在抽象基礎架構重構，打通一條從新手建立角色、滄浪客棧接案、組隊鐵牛、進入太陰地宮探險、遭遇戰鬥、撤退調息至安全存檔的完整端到端可玩迴圈。
> **落地進度**：已於 2026-10-04 完成。
> 1. **城鎮開局與隊伍組建**：支援單人創角入局、客棧與掌櫃福伯及鐵牛對話，並透過 `recruit tie_niu` 順利組建雙人陣型小隊。
> 2. **地宮探險與遭遇戰鬥**：支援 `enter taiyin_tomb_b1f` 展開地牢靈識雷達，發起妖邪敵群列陣遭遇戰，走通前衛/後衛判定、遁地撤退與戰鬥結束狀態脫離。
> 3. **安全撤出、客棧全滿調息與存檔還原**：於向上階梯 (1, 8) 透過 `leave` 安全撤出地牢返回客棧，執行 `rest` 使氣血真元神識 100% 回滿，並透過 `save 1` 保存至手動槽位，讀檔驗證主角與夥伴數據完整還原。
> 4. **專屬端到端驗證測試**：建立 `PlayableContentLoopTest.java` 完整覆蓋上述 7 大遊玩步驟，全專案 247 項測試 100% 綠燈通過。

---


## 💥 14. ✅ [已完成] 戰鬥區塊視覺特效與動態回饋 (Battle Presentation & VFX) — Priority: P1 / P2

> **來源**：2026-10-05 戰鬥表現綜合評估，收斂 Claude / Copilot / Codex 三方提案並全面落地，歷史評估稿已收斂除役。
> **核心目標**：解決戰鬥只有文字日誌顯示的問題；在戰鬥區塊引入結構化戰鬥事件、覆蓋式特效層 (FX Layer)、浮動傷害數值與受擊/防禦動態回饋，並嚴格遵循「事件事實後端結算、前端僅負責投影演繹、不更改數值平衡」三大原則。
> **落地進度**：已於 2026-10-08 全數完成。
> 1. 後端 `DrpgCombatLoop` 與 `ComboResolver` 全面派發 `BattleEvent`（含普攻、主動單體/AOE、治療、護盾、Buff、連攜技），並於 `DrpgBattleService` 實裝主動技能即時排空推進（Instant Flush）。
> 2. 前端新增 `fx-presets.js` 武器與法術風格預設庫，以及 `battle-fx-director.js` 動畫控制器。
> 3. CSS 新增 9 種兵刃刀光動畫、6 系元素法術光效、全場震動（`.arena-shake`）與橫幅公告（`.fx-skill-banner`）。

### 14.1 ✅ [已完成] 結構化戰鬥事件傳輸管道 (COMBAT-FX-EVENT-01) (P1)
- **現狀**：`DrpgCombatLoop` 結算時僅產出 ANSI 戰鬥 log，前端 `BattleViewDto` 僅為狀態快照，無法得知當前 tick「誰攻擊了誰、用了何種武器/屬性、判定為命中/暴擊/閃避/格擋、實扣多少血量」。
- **改善方向**：
  1. **雙軌傳遞**：保留訊息區現有文字 log；後端新增不可變 `BattleEvent`（包含 `battleId`、單調遞增 `seq`、`actor`、`target`、`kind`、`weaponType`、`damageType`、`shape` 及 `hits` 陣列）。
  2. **獨立訊息路由**：透過 WebSocket 訊息 `BATTLE_EVENTS` 傳輸，與快照 `DRPG_STATE` 職責分離，避免狀態快照污染與事件遺失。
  3. **結算點即時產生**：隊員普攻、敵方攻擊（取自 `DefenseResolver.DefenseOutcome`）、主動技能與反擊即刻建立事件；手動技能結算立即 flush，避免等待 500ms 心跳。

### 14.2 ✅ [已完成] 覆蓋式特效層與防禦判定反饋 MVP (COMBAT-FX-MVP-02) (P1)
- **現狀**：敵人陣列重繪時會直接摧毀 DOM 節點，直接在卡片上掛載動畫會被 full rebuild 機制打斷；目前無任何擊中/閃避/格擋/浮字。
- **改善方向**：
  1. **獨立 FX Layer**：在 `#battle-arena-panel` 內注入獨立覆蓋層 `<div id="battle-fx-layer">`，設置 `pointer-events: none` 與 `aria-hidden="true"`，杜絕遮擋玩家點擊互動。
  2. **浮動數字正交編碼**：數字顏色代表「傷害屬性」（物理白、火橘、冰藍、雷黃、毒綠、治療綠），字級/描邊/前綴代表「判定結果」（普通命中、暴擊放大+暴擊標記、格擋-N、閃避/招架/未命中文字標籤）；完全閃避不顯示令人困惑的 `-0`。
  3. **防禦判定微動畫**：
     - 普通命中 (`react-hit`)：短暫閃白 60ms + 輕震 3px。
     - 致命一擊 (`react-crit`)：紅閃 2 次 + 抖動 6px + 戰場輕震。
     - 招架 (`react-parry`) / 格擋 (`react-block`)：金屬火花 / 盾牌浮現。
     - 閃避 (`react-dodge`)：卡片側移殘影後復位。
  4. **無障礙支援**：完整支援 `@media (prefers-reduced-motion)`，關閉震動位移並保留靜態數字。

### 14.3 ✅ [已完成] 武器與法術風格預設庫 (COMBAT-FX-PRESETS-03) (P2)
- **改善方向**：
  1. 武器組別粗粒度分群：斬擊類 (劍/刀)、穿刺類 (槍/匕首)、鈍擊類 (槌/杖)、投射類 (弓弩)、施法類 (法杖/符籙)、徒手類，不為每個武器枚舉重複造輪子。
  2. 法術通用粒子骨架：火/冰/雷/暗/聖等屬性共用動畫結構，僅切換色彩光暈與音畫提示。
  3. 攻擊範圍演繹：單體、全體、一整排（以 hits 命中清單為準，畫出範圍掃擊帶），錯開 60ms 依序跳字。

### 14.4 遊戲機制與數值平衡獨立化決策 (COMBAT-FX-RULE-04) (P2)
- **改善方向**：
  1. **我方暴擊/未命中規則**：維持既有平衡，不因導入視覺特效而順便改寫我方傷害公式；待表現層穩定後，作為獨立機制任務（含蒙地卡羅測試）評估落地。
  2. **一整排目標判定**：目標規則完全由後端結算與排位推進決定，前端禁止自行猜測畫面一整排，以 DTO 中的 `hits` 為唯一依據。

---


## 🛡️ 20. 2026-10-14 全局架構審查後續與深拷貝加固 (Review Follow-ups: Deep-Copy Hardening, Fallback Ratchet & Dual-Persistence)

> **來源**：2026-10-14 全專案靜態程式碼與規劃文件交叉 Review。

### 20.1 DEEPCOPY-01：LivingStats.deepCopy() 屬性完整拷貝防衛 — ✅ 已完成 (2026-10-14)
- **問題現況**：`LivingStats.deepCopy()` 原先僅複製等級、五維與氣血，遺漏了 `resistances`、`equipment`、`learnedSkills`、`enabledSkills`、`combatResources`，違反專案第一紅線中「深拷貝不得遺漏任何屬性」之架構準則，導致 `Player.toRecord()` 或測試進行快照時數值丟失。
- **加固方案**：在 `LivingStats.deepCopy()` 中補齊集合欄位的淺防禦性拷貝（`new HashMap<>(...)`），確保快照資料 100% 完整無遺漏。

### 20.2 FALLBACK-02：持續推進生產碼 new TemplateCatalog() 歸零 — ✅ 階段性完成 (2026-10-15 收斂至 26 處)
- **問題現況**：`ArchitectureGuardrailsTest` 中 `testTemplateCatalogFallbackRatchet` 原先鎖定基準為 33 處。
- **演進路徑**：已完成第一批 POJO 與 Service 建構子收斂（`PartyInventory`、`SkillBridgeService`、`DrpgEnemyTacticsService`、`XpProgressionService`、`DrpgTemplateAdapter`、`MudTemplateAdapter`），將棘輪門檻自 33 嚴格收斂至 26 處，持續推進歸零。

### 20.3 PERSIST-01：H2 CharacterEntity.inventory 雙持久化漸進棄用 — ✅ 已完成 (2026-10-15)
- **問題現況**：`CharacterEntity` 仍持有 `List<GameItem> inventory` 映射至 H2 的 `inventory_json`。
- **對齊方針**：依據 ADR-01 決策，單機模式以 `SaveData` JSON 為唯一單一真相源。已將 `CharacterEntity.inventory` 標記為 `@Deprecated`，業務層已全面由 `SaveGameService` 接管，解除雙套持久化維護負擔。


## 🛡️ 21. 2026-10-15 全專案綜合審查後續與紅線治理藍圖 (Consistency, Over-Engineering, Hardcode & Cleanup Action Plan)

> **來源**：2026-10-15 全專案深入審查（文件間進度一致性、代碼與規劃一致性、過度設計、寫死代碼與改善建議）。
> 全套單元與整合測試保持 **304 項 100% 綠燈全數通過**。

### 21.1 DOC-CONSISTENCY-01：文件現況、測試數據與目錄架構全面同步 — ✅ 已完成 (2026-10-15)
- **問題現況**：
  1. `FUTURE_IMPROVEMENTS.md` 前言殘留「250 項全綠燈測試」，落後於實際的 304 項。
  2. `UI_PAGE_STRUCTURE.md` 第 4 節「現況深度盤點」依然把已被 Phase UI-1~5 解決的項目（`index.html` inline onclick、`style.css` 巨石、`party-modal.js` 巨石）列為高危紅燈，產生嚴重內部自我矛盾。
  3. `ARCHITECTURE.md` 第 3 節原始碼目錄樹遺漏 `com.example.htmlmud.domain.combat`（純函式 `CombatRules`、`CombatPresentation`、`Combatant`），亦遺漏 `domain/port`、`domain/event` 等子包，前端結構樹漏列 `buff-hud-helper.js`、`victory-modal.js` 與 `style-07-battle-fx.css`。
  4. `UI_PAGE_STRUCTURE.md` 第 3 節宣稱存在的 `static/js/panels/header-panel.js` 與 `footer-bar.js` 在代碼庫中並不存在。
- **加固方案**：全面修訂三份核心文件，消除過期文字與矛盾，維持架構說明書與實際代碼庫 100% 同步。

### 21.2 EQUIP-SLOT-CLEAN-01：清除裝備槽位中文名稱硬編碼推測 — 🚨 紅線治理 ✅ 已完成 (2026-10-15)
- **問題現況**：[`static/js/modals/party-equipment-tab.js:L54-L61`](../../src/main/resources/static/js/modals/party-equipment-tab.js#L54-L61) 在判定裝備槽位時，依然保留 `name.includes('冠') || name.includes('盔') || name.includes('帽')`、`name.includes('甲') || name.includes('袍') || name.includes('衣')`、`name.includes('靴') || name.includes('履')`、`name.includes('戒') || name.includes('佩') || name.includes('鐲') || name.includes('珠')` 等字串比對作為 fallback，明確違反 `GEMINI.md` 專案第一紅線（嚴禁中文名稱業務特例推測）。
- **加固方案**：徹底拔除所有 `name.includes(...)` 中文名稱推測，全面嚴格依賴後端 `ItemTemplate` 的 `equipSlot` 枚舉。由 `DataIntegrityValidator` 確保所有裝備資料在啟動時皆有明確槽位定義。

### 21.3 DATA-03-REVISIT：地牢出入口 exitRoomId 與地塊 Tooltip 100% 配置化 — ✅ 已完成 (2026-10-15)
- **問題現況**：
  1. [`DungeonCommand.java:L152-L158`](../../src/main/java/com/example/htmlmud/application/command/impl/DungeonCommand.java#L152-L158) 仍寫死 `if ("mozhu_mines_b1f".equals(floor.getId()))` 導向 `mine_entrance`，其餘導向 `newbie_village:inn`。
  2. [`static/js/panels/dungeon-panel.js:L67-L97`](../../src/main/resources/static/js/panels/dungeon-panel.js#L67-L97) 地塊懸停提示寫死為太陰古塚地貌（「青岡石壁」、「古仙棺槨寶箱」），在墨竹礦坑等不同地城中顯示失真。
- **加固方案**：
  1. 在 `DungeonFloor` 模型、`DungeonFloorLoader` 與對應 JSON（`mozhu_mines_b1f.json`、`taiyin_tomb_b1f.json`）補齊 `exitRoomId` 欄位，重構 `DungeonCommand.java` 撤離邏輯為 `floor.getExitRoomId()` 動態導向。
  2. 前端 `dungeon-panel.js` 的 `populateTileAppearance` 替換為通用地牢圖塊提示並補全向上階梯（`<`）圖標提示。

### 21.4 SHOP-FALLBACK-01：移除 ShopCommand 客棧名稱比對與偽商店 Fallback — ✅ 已完成 (2026-10-15)
- **問題現況**：[`ShopCommand.java:L81-L83`](../../src/main/java/com/example/htmlmud/application/command/impl/ShopCommand.java#L81-L83) 包含 `if (roomId != null && roomId.contains("inn")) shopOpt = templateReader.findShop("newbie_village:inn_shop");`，導致任意包含 inn 的房間若無商店會被強制導流至新手村客棧商店。
- **加固方案**：移除客棧名稱硬編碼比對，商店完全依據 `TemplateReader.findShopByRoomId(roomId)` 提供服務，無商店時給予明確提示。

### 21.5 DEADCODE-01：清理死代碼、殘留內聯屬性與歷史巨石檔案 — ✅ 已完成 (2026-10-15)
- **問題現況**：
  1. [`PlayerLoginListener.java`](../../src/main/java/com/example/htmlmud/application/listener/PlayerLoginListener.java) 全檔案 100% 為註解代碼。
  2. [`QuestEventListener.java`](../../src/main/java/com/example/htmlmud/application/listener/QuestEventListener.java) 包含 `System.out.println` 未完成代碼。
  3. [`index.html:L37-L61`](../../src/main/resources/static/index.html#L37-L61) 殘留 4 處內聯 `onclick` 屬性。
- **加固方案**：刪除死代碼類別 `PlayerLoginListener.java`，改用標準 SLF4J Logger 於 `QuestEventListener`，清除 `index.html` 內聯 `onclick` 屬性。

### 21.6 FALLBACK-02：持續推進生產環境 new TemplateCatalog() 歸零 — ✅ 階段性完成 (2026-10-15 基準收斂至 26 處)
- **問題現況**：`ArchitectureGuardrailsTest` 的 `new TemplateCatalog()` 棘輪鎖定在 33 處。
- **加固方案**：重構 `PartyInventory`、`SkillBridgeService`、`DrpgEnemyTacticsService`、`XpProgressionService`、`DrpgTemplateAdapter`、`MudTemplateAdapter` 建構子，棘輪看門狗上限自 33 成功收斂至 26 處（縮減 21.2%）。

### 21.7 QUEST-DATA-01：任務劇情資料模型化與特例監聽器解耦 — ✅ 已完成 (2026-10-15)
- **問題現況**：[`MozhuMinesQuestListener.java`](../../src/main/java/com/example/htmlmud/application/listener/MozhuMinesQuestListener.java) 以特例監聽 `boss_song_tianheng` 並在 Java 代碼硬編碼輸出 25 行台詞，全專案無通用任務資料模型。
- **加固方案**：建立 `QuestTemplate` 領域模型與 `quests.json` 資料設定檔，擴充 `TemplateReader.findQuestByTriggerMob()` 與 `TemplateRepository` 自動加載機制；重構 `MozhuMinesQuestListener` 為通用資料驅動型監聽器，消除全部硬編碼劇情台詞與首領特例。

### 21.8 PERSIST-01：H2 CharacterEntity.inventory 雙持久化收斂 — ✅ 已完成 (2026-10-15)
- **問題現況**：單機模式以 `SaveData` JSON 為唯一單一真相源，但 `CharacterEntity` 仍維護 `inventory_json`。
- **加固方案**：標記 `CharacterEntity.inventory` 為 `@Deprecated`，確立以 `SaveGameService` 為核心，消除維護雙套持久化實體的認知負擔。

---


## 🗡️ 24. 純單機單體化演進戰略：多人機制、雙軌架構與膠水代碼全域收斂藍圖 (Single-Player Monolith & Dual-Track Convergence Masterplan) — Priority: P0 ~ P2（單人轉型核心戰役）

> **戰略定位與決策背景**：  
> 專案已正式決策由多人 MUD 架構全面轉向**「純單機單體暗黑克蘇魯修仙 DRPG (Single-Player Dark Xianxia DRPG Monolith)」**。  
> 歷史上由於「MUD 文字探索」與「網格 3D/DRPG 地牢」平行拼裝，累積了嚴重的雙重實體、兩套背包、兩套技能、兩套戰鬥與兩套存檔，以及依附其上的各種翻譯膠水代碼（如 `CharacterSyncService`、`SkillBridgeService`、`StashCommand`、`TellCommand`）。  
> 愈晚處理，後續新功能的開發與包袱愈重。本章節將所有殘留點系統化解構為**四大執行階段 (Phase 0 ~ Phase 3)**，明確各步驟之涉及類別、改動方針、非目標與驗收標準（DoD），作為專案單人化收斂之唯一權威依據（SSoT），供後續依序逐一落地。

### 核心演進哲學 (Guiding Principles)
1. **小隊即世界 (The Party is the Single Source of Truth)**：小隊公共行囊為全遊戲唯一背包；隊員實體擁有裝備與戰鬥屬性；主角即隊長。
2. **零膠水橋接 (Zero Glue Code & Zero Dual-Track)**：徹底消滅在 MUD 與 DRPG 之間倒買倒賣的翻譯器與雙寫鏡像。
3. **單一主角與單一戰鬥引擎 (Singleton Protagonist & Unified Combat Engine)**：城鎮與地牢共享同一套小隊與戰鬥規則。

---

### 24.1 階段 0：P0 級速勝清理 — 廢止明顯多人指令與轉存假帳 (Fast Wins & Immediate Decommission)

#### 1. SP-01: 多人密語指令除役與單機世界化 (Decommission TellCommand) — Priority: P0 [已完成 ✅]
- **問題現狀**：目前仍註冊 `tell` / `whisper` / `t` 指令，代碼依賴 `worldManager.findPlayerByName(targetName)` 在世界中尋找其他玩家並發送私聊，是典型的多人殘留死代碼。
- **涉及檔案**：
  - [`TellCommand.java`](../../src/main/java/com/example/htmlmud/application/command/impl/TellCommand.java)
  - [`WorldManager.java`](../../src/main/java/com/example/htmlmud/domain/world/WorldManager.java)
- **改動方針**：
  1. 徹底刪除 [`TellCommand.java`](../../src/main/java/com/example/htmlmud/application/command/impl/TellCommand.java)。
  2. 自 [`WorldManager.java`](../../src/main/java/com/example/htmlmud/domain/world/WorldManager.java) 中移除 `findPlayerByName(String targetName)` 方法。
  3. 自指令別名與幫助文件中移除 `tell` 相關說明。
- **非目標**：不影響與 NPC 對話的 `TalkCommand`。
- **驗收標準 (DoD)**：
  - 輸入 `tell` 指令提示無此命令；全專案測試無失效引用，315 項測試 100% 綠燈。[已達成 ✅]

#### 2. SP-02: 雙層背包轉存指令廢止 (Decommission Stash Dual-Transfer) — Priority: P0 [已完成 ✅]
- **問題現狀**：雖然 `get` 與 `drop` 已操作小隊行囊，但 [`StashCommand.java`](../../src/main/java/com/example/htmlmud/application/command/impl/StashCommand.java) 內部仍保留將道具自 `player.getInventory()` 存入/取出到 `party.getInventory()` 的雙向轉存邏輯。玩家若輸入 `stash` / `withdraw`，道具會掉入底層已廢止的個人隨身假帳背包中。
- **涉及檔案**：
  - [`StashCommand.java`](../../src/main/java/com/example/htmlmud/application/command/impl/StashCommand.java)
  - [`Living.java`](../../src/main/java/com/example/htmlmud/domain/actor/impl/Living.java)
  - [`DualTierInventoryAndStashTest.java`](../../src/test/java/com/example/htmlmud/DualTierInventoryAndStashTest.java)
- **改動方針**：
  1. 廢除 `handleStash` 與 `handleWithdraw` 跨背包搬移業務邏輯。
  2. 將 `StashCommand` 簡化為純粹導向 `InventoryCommand` 或友情回應用戶：「道途公共行囊已全面大一統（40格公共儲物袋），所有拾取與獲取物資皆自動納入全隊共享，無需手動轉存！」。
  3. 將 [`Living.java`](../../src/main/java/com/example/htmlmud/domain/actor/impl/Living.java) 中的 `inventory` 限制為僅供 `Mob` 掉落物保管使用，禁止 `Player` 依賴此集合。
- **驗收標準 (DoD)**：
  - 玩家輸入 `stash` / `withdraw` 不會發生任何雙軌物品劃撥；小隊行囊為全專案道具流轉唯一真實載體；測試 100% 綠燈。[已達成 ✅]

---

### 24.2 階段 1：P1 級核心實體與存檔單一真相源 (Single Source of Truth & Save Convergence)

#### 1. SP-03: 存檔雙寫徹底根除與結構純化 (Decommission Personal Inventory in Saves) — Priority: P1 [已完成 ✅]
- **問題現狀**：[`SaveGameService.java`](../../src/main/java/com/example/htmlmud/domain/save/service/SaveGameService.java) 在存檔時，既序列化了整個 `party`（小隊 40 格公共行囊），又額外將 `player.getInventory()` 序列化為 `personalInventory` 寫入 JSON；讀檔時又重新還原並呼叫 `syncEquipmentFromPartyToPlayer` 進行投影，維護成本高且存在假帳隱患。
- **涉及檔案**：
  - [`SaveGameService.java`](../../src/main/java/com/example/htmlmud/domain/save/service/SaveGameService.java)
  - [`SaveData.java`](../../src/main/java/com/example/htmlmud/domain/save/model/SaveData.java)
  - [`SaveMigrationPipeline.java`](../../src/main/java/com/example/htmlmud/domain/save/migration/SaveMigrationPipeline.java)
- **改動方針**：
  1. 存檔寫入：移除 `personalInventory` 提取與序列化邏輯，小隊公共行囊為道具唯一保存源。
  2. 存檔載入：移除 `onlinePlayer.getInventory()` 還原流程，僅還原小隊實體與其裝備。
  3. 版本遷移：在 `SaveMigrationPipeline` 中將舊存檔的 `personalInventory` 自動合併進 `party.inventory`，確保相容舊有存檔。
- **驗收標準 (DoD)**：
  - 存檔 JSON 檔案結構不再冗餘記錄 `personalInventory`；舊檔載入時物品安全合流，無道具遺失；測試 100% 綠燈。[已達成 ✅ (316 測試全綠)]

#### 2. SP-04: 房間實體單一主角化與多人廣播除役 (Single Protagonist Room & Broadcast Simplification) — Priority: P1
- **問題現狀**：[`Room.java`](../../src/main/java/com/example/htmlmud/domain/actor/impl/Room.java) 維護 `List<Player> players = new CopyOnWriteArrayList<>()`，[`RoomService.java`](../../src/main/java/com/example/htmlmud/domain/world/RoomService.java) 與 [`LivingService.java`](../../src/main/java/com/example/htmlmud/domain/service/LivingService.java) 處處依賴遍歷房間所有玩家，並維護 `broadcastToOthers`「排除自己發送給其他人」邏輯。
- **涉及檔案**：
  - [`Room.java`](../../src/main/java/com/example/htmlmud/domain/actor/impl/Room.java)
  - [`RoomService.java`](../../src/main/java/com/example/htmlmud/domain/world/RoomService.java)
  - [`LivingService.java`](../../src/main/java/com/example/htmlmud/domain/service/LivingService.java)
- **改動方針**：
  1. 將 `Room` 內部代表玩家的並發清單 `List<Player> players` 重構為單一引用 `Player protagonist`（當主角進入時賦值，離開時置 null）。
  2. 除役 `broadcastToOthers` 冗餘方法，簡化 `broadcast` 為直接向房內主角推送。
  3. 簡化生靈 Buff 結算跳字：若房內有主角則直接送達，不再進行多重 foreach 遍歷。
- **驗收標準 (DoD)**：
  - 消除房間對多玩家並發集合的鎖與記憶體開銷；房間探索與事件通知邏輯大幅精簡；測試 100% 綠燈。

#### 3. SP-05: WebSocket 單一主角綁定與多開會話接管 (Singleton Protagonist & Session Takeover) — Priority: P1
- **問題現狀**：目前每次 WebSocket 連入，[`MudWebSocketHandler.java`](../../src/main/java/com/example/htmlmud/pipeline/MudWebSocketHandler.java) 就分配一個 `p-<sessionId>` 並建立全新 `Player` 實體。若瀏覽器開多個分頁，世界中會同時存在多個「主角」，造成存檔槽位覆寫衝突。
- **涉及檔案**：
  - [`MudWebSocketHandler.java`](../../src/main/java/com/example/htmlmud/pipeline/MudWebSocketHandler.java)
  - [`SessionRegistry.java`](../../src/main/java/com/example/htmlmud/infra/server/SessionRegistry.java)
- **改動方針**：
  1. 引入「單機單一主角實例（Singleton Protagonist）」模型：世界只維持單一主角生命週期。
  2. 當新分頁連入時，自動接管現有主角的 `WebSocketOutput` 並向前端同步最新全景狀態，而非盲目創生新的獨立 Player。
- **驗收標準 (DoD)**：
  - 多分頁連入時共享同一個主角進度；消除存檔競爭條件；測試 100% 綠燈。

---

### 24.3 階段 2：P2 級技能體系大一統與橋接膠水除役 (Skill Unified Architecture)

#### 1. SP-06: 技能模型合一 (Skill Model Unification) — Priority: P2
- **問題現狀**：技能體系存在兩套定義：MUD 端的 `SkillEntry`（修煉熟練度、境界等級）與 DRPG 端的 `PartyMemberSkill`（CD、傷害倍率、消耗、打斷標籤），導致新增一門技能需維護兩份範本資料。
- **涉及檔案**：
  - [`SkillEntry.java`](../../src/main/java/com/example/htmlmud/domain/model/entity/SkillEntry.java)
  - [`PartyMemberSkill.java`](../../src/main/java/com/example/htmlmud/party/PartyMemberSkill.java)
  - [`skills.json`](../../src/main/resources/data/skills/skills.json)
- **改動方針**：
  1. 建立大一統規範技能實體 `UnifiedSkill`（或擴充升級 `PartyMemberSkill`），同時包含：
     - **戰術數值**：CD、傷害倍率、真元/精力消耗、施法時間、打斷反制標籤（STUN / SILENCE / CONTROL）。
     - **修煉境界**：熟練度（Mastery）、道法等級（Level 1~10）、屬性補正係數。
     - **敘事文本**：動態戰鬥播報範本、招式意境描述。
  2. 主角與 4 名隊友均統一持有此技能模型。
- **驗收標準 (DoD)**：
  - 全遊戲唯一技能資料源，徹底消除 `skills.json` 與 DRPG 戰術技能重複定義。

#### 2. SP-07: 移除 `SkillBridgeService` 膠水橋接 — Priority: P2
- **問題現狀**：目前依賴 [`SkillBridgeService.java`](../../src/main/java/com/example/htmlmud/domain/service/SkillBridgeService.java) 進行字串映射、動態縮放與熟練度回饋，代碼超過 260 行且容易產生數值飄移。
- **涉及檔案**：
  - [`SkillBridgeService.java`](../../src/main/java/com/example/htmlmud/domain/service/SkillBridgeService.java)
  - [`CharacterSyncService.java`](../../src/main/java/com/example/htmlmud/domain/service/CharacterSyncService.java)
  - [`DrpgRewardService.java`](../../src/main/java/com/example/htmlmud/combat/DrpgRewardService.java)
- **改動方針**：
  1. 在技能模型合一後，戰鬥直接讀取隊員自身的統一技能物件，無需戰前映射翻譯。
  2. 戰鬥獲勝後，熟練度直接原地記入小隊成員技能中，無需反向回饋。
  3. 徹底刪除 [`SkillBridgeService.java`](../../src/main/java/com/example/htmlmud/domain/service/SkillBridgeService.java)。
- **驗收標準 (DoD)**：
  - 成功刪除 `SkillBridgeService` 類別，零編譯錯誤，全量 315 測試綠燈。

---

### 24.4 階段 3：P2 級戰鬥引擎雙軌大一統與共享經濟 (Combat & Economy Unification)

#### 1. SP-08: 戰鬥引擎收斂與 5 人小隊全域協同 (Combat Engine Unification) — Priority: P2 (已完成 ✅)
- **問題現狀**：原本城鎮與野外採用 MUD [`CombatService.java`](../../src/main/java/com/example/htmlmud/domain/service/CombatService.java)（主角一人單挑，隊友看戲）；地牢採用 DRPG [`DrpgBattleService.java`](../../src/main/java/com/example/htmlmud/combat/DrpgBattleService.java)（5人小隊滿編戰術戰鬥），戰鬥規則與體驗嚴重分裂。
- **涉及檔案**：
  - [`KillCommand.java`](../../src/main/java/com/example/htmlmud/application/command/impl/KillCommand.java)
  - [`CombatService.java`](../../src/main/java/com/example/htmlmud/domain/service/CombatService.java)
  - [`DrpgBattleService.java`](../../src/main/java/com/example/htmlmud/combat/DrpgBattleService.java)
  - [`FieldCombatPartyUnificationTest.java`](../../src/test/java/com/example/htmlmud/FieldCombatPartyUnificationTest.java)
- **落地成果**：
  1. `DrpgBattleService` 實裝 `startFieldBattle(Player, Mob)`，野外遭遇戰由 5 人小隊全員依照站位、陣法與戰術 AI 協同迎敵。
  2. `BattleContext` 新增 `onBattleEnd` 回呼，大捷時掉落物直入 40 格小隊公共行囊，隊員獲得修為經驗與熟練度原地增長，發布任務事件並同步移除/標記大世界房間 Mob。
  3. `KillCommand` 與 `CombatService.startCombat` 全面委派至小隊戰鬥引擎，`TargetSelector` 自動過濾死亡怪防禦性防護。
  4. 新增 `FieldCombatPartyUnificationTest`，全專案 327 項測試 100% 綠燈零回歸破壞。

#### 2. SP-09: 全隊共享經濟與小隊資產化 (Unified Party Economy & Coin) — Priority: P2
- **問題現狀**：金幣/靈石目前分散於各成員的 `LivingStats.coin`，但道具則放入小隊公共行囊；商店扣款扣主角個人，缺少小隊公共金庫概念。
- **涉及檔案**：
  - [`Party.java`](../../src/main/java/com/example/htmlmud/party/Party.java)
  - [`ShopCommand.java`](../../src/main/java/com/example/htmlmud/application/command/impl/ShopCommand.java)
  - [`DrpgRewardService.java`](../../src/main/java/com/example/htmlmud/combat/DrpgRewardService.java)
  - [`DungeonCommand.java`](../../src/main/java/com/example/htmlmud/application/command/impl/DungeonCommand.java)
  - [`DrpgStateDto.java`](../../src/main/java/com/example/htmlmud/dungeon/DrpgStateDto.java)
  - [`UnifiedPartyEconomyTest.java`](../../src/test/java/com/example/htmlmud/UnifiedPartyEconomyTest.java)
- **改動方針**：
  1. 在 `Party` 實體中增設 `coin` 欄位作為全隊共享靈石金庫。
  2. 商店購買與出售（`ShopCommand`）、野外地牢掉落獎勵、開寶箱獲取金幣全面直接入帳至小隊金庫。
  3. 主角個人 `LivingStats.coin` 與隊長小隊金庫保持單向關聯與 0ms 零延遲鏡像同步。
- **落地成果**：
  1. `Party.java` 實裝 `coin`、`addCoin`、`deductCoin`、`setCoin` 與 `syncLeaderCoin`。
  2. `ShopCommand.java` 採小隊金庫出納，新增行囊回收出售換取靈石機制。
  3. `DrpgRewardService.java` 戰鬥大捷繳獲妖獸靈石直接存入小隊金庫，並透過 `BattleVictoryDto` 推送前端。
  4. `DungeonCommand.java` 地牢寶箱暗格繳獲靈石入帳小隊金庫。
  5. 前端面板（頂部靈石導航列、隊伍行囊抽屜、大捷視窗）全域統一顯示小隊共享靈石。
  6. 新增 `UnifiedPartyEconomyTest.java`，全專案 331 項測試 100% 綠燈零回歸通過。

---

### 24.5 執行步驟順序與看板里程碑映射 (Execution Sequence & Milestone Mapping)

| 順序 | 步驟編號 | 核心收斂任務 | 優先級 | 預估風險 | 實裝狀態 |
| :---: | :--- :--- | :---: | :---: | :---: |
| **Step 1** | **SP-01** | 刪除 `TellCommand` 多人私聊與 `findPlayerByName` | **P0** | 極低 | **已完成 (315綠燈) ✅** |
| **Step 2** | **SP-02** | 廢止 `StashCommand` 雙向轉存，除役個人背包假帳 | **P0** | 低 | **已完成 (315綠燈) ✅** |
| **Step 3** | **SP-03** | 存檔寫入與載入廢止 `personalInventory` 雙寫 | **P1** | 中（需兼顧舊存檔遷移） | **已完成 (316綠燈) ✅** |
| **Step 4** | **SP-04** | `Room` 房間實體改為單一主角引用，除役多玩家廣播遍歷 | **P1** | 中 | **已完成 (316綠燈) ✅** |
| **Step 5** | **SP-05** | WebSocket 多開連線自動接管唯一主角，杜絕存檔覆寫競爭 | **P1** | 中 | **已完成 (320綠燈) ✅** |
| **Step 6** | **SP-06** | 技能模型合一 (`UnifiedSkill`)，消除 MUD/DRPG 技能定義割裂 | **P2** | 中高 | **已完成 (324綠燈) ✅** |
| **Step 7** | **SP-07** | 移除 `SkillBridgeService` 翻譯縮放膠水代碼 | **P2** | 中 | **已完成 (324綠燈) ✅** |
| **Step 8** | **SP-08** | 戰鬥引擎大一統，野外戰鬥全面對齊 5 人小隊協同作戰 | **P2** | 高 | **已完成 (327綠燈) ✅** |
| **Step 9** | **SP-09** | 小隊共享金庫與經濟體系大一統 | **P2** | 低 | **已完成 (331綠燈) ✅** |

---

