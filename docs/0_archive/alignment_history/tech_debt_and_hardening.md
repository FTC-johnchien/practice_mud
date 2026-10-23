# 🛡️ 底層技術債治理、硬編碼清剿與系統加固藍圖 (Tech Debt & Hardening)

> **文件定位與目的**：
> 本文件專注於守護專案第一紅線（100% 資料驅動、零硬編碼），包含**全域硬編碼清剿清單、測試分層加速、Buff 契約護欄與單機語意純化**。  
> 宏觀演進總表請參閱 👉 [`FUTURE_IMPROVEMENTS.md`](FUTURE_IMPROVEMENTS.md)。

---

## 📌 治理專案索引 (Navigation)
1. [測試工程分層加速與可觀測性加固 (§11)](#11-測試工程分層加速與可觀測性加固-testing--observability--priority-p3)
2. [全專案功能與設定資料硬編碼實地盤查與清理藍圖 (§16)](#16-全專案功能與設定資料硬編碼實地盤查與清理藍圖-hardcoded-features--data-audit--purge-blueprint--priority-p1--p2)
3. [Buff 契約與可靠性護欄 (§19)](#19-2026-10-13-copilot-審查後續buff-契約與可靠性護欄-review-follow-ups)
4. [單機語意純化與全域硬編碼大清剿 (§25)](#25-單機語意純化與全域硬編碼大清剿-single-player-purity--anti-hardcoding-audit--priority-p1)

---

## ⚡ 11. 測試工程分層加速與可觀測性加固 (Testing & Observability) — Priority: P3

> **來源**：歷史檢視（Claude／Codex）；本次測試執行限制與治理建議見 [`docs/0_references/Copilot_Review.md`](../0_references/Copilot_Review.md) §五、§四。
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



## 🔍 16. 全專案功能與設定資料硬編碼實地盤查與清理藍圖 (Hardcoded Features & Data Audit & Purge Blueprint) — Priority: P1 / P2

> **核心目標**：徹底盤查代碼庫中殘留的業務 ID、地牢出入口、掉落物備用清單、技能屬性推算、客棧/商店台詞等硬編碼（Hardcoding），推動全系統 100% 回歸配置化與資料驅動。

### 16.1 地牢與地圖系統硬編碼盤查 (DATA-03) (P1)
- **現狀問題**：
  1. **地牢出口房間硬編碼**：[`DungeonCommand.java:L144-150`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/application/command/impl/DungeonCommand.java#L144-L150) 中，玩家在向上階梯執行 `leave` 撤離時，返回的房間透過 Java `if-else` 寫死（`mozhu_mines_b1f` 返回 `mozhu_mines:mine_entrance`；`taiyin_tomb_b1f` 返回 `newbie_village:inn`）。[`DungeonFloor.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/model/DungeonFloor.java) 缺乏 `entranceRoomId` 與 `exitRoomId` 欄位。
  2. **怪物與掉落物備用池寫死**：[`DungeonCommand.java:L34-48`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/application/command/impl/DungeonCommand.java#L34-L48) 靜態常數寫死 `TOMB_MOBS`（4 種太陰怪）與 `TOMB_ITEMS`（6 種太陰古塚道具），當樓層或寶箱未設定掉落時直接 fallback 至太陰資料。
  3. **地牢切換別名硬編碼**：[`DungeonCommand.java:L74-83`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/application/command/impl/DungeonCommand.java#L74-L83) 中，`switch taiyin` / `switch mozhu` 以字串比對直接映射為指定樓層。
  4. **陷阱數值與提示文字硬編碼**：[`DungeonCommand.java:L295-298`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/application/command/impl/DungeonCommand.java#L295-L298) 陷阱扣除固定 15 HP、5 SAN，且提示寫死「菌絲煞氣噴湧」（太陰古塚踩陷阱亦出現礦坑菌絲文字）。
  5. **[已完成] 殘留 65 行手刻地圖生成器**：原 `DungeonManager.java` 中的 `buildTombB1F()` 已徹底物理刪除，全系統 100% 由 `resources/data/dungeons/*.json` 資料驅動。 ✅
  6. **區域載入清單退回硬編碼**：`WorldManager.java`（歷史代碼，已除役）於動態掃描失敗時曾退回硬編碼清單 `List.of("newbie_village", "mozhu_mines", "snow", "silverleaf", "taiyin_tomb")`。
- **改善方案**：
  - 在 `DungeonFloor` 與 `dungeons/*.json` 擴充 `entranceRoomId`、`exitRoomId`、`defaultLootPool`、`trapDamage`、`trapSanCost`、`trapMessage`。
  - 徹底移除 `DungeonManager.buildTombB1F()` 手刻代碼，100% 由 JSON 檔案驅動（已完成 ✅）。

### 16.2 戰鬥與技能數值硬編碼盤查 (DATA-04 / COMB-03) (P1)
- **現狀問題**：
  1. **試道傀儡 200+ 行手刻寫死**：[`DrpgBattleService.java:L193-400`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgBattleService.java#L193-L400) 的 `startTrainingBattle()` 用 200 多行代碼手刻 1x1 ~ 3x3 傀儡與陣樁的所有屬性與排位。
  2. **[CANCELLED] 走火入魔 CHAOS 異變機制**：原隊員背刺（40% 機率）與畸變 Boss 走火入魔機制已依專案核心決策徹底除役取消，相關代碼在純單機單體化演進中已徹底拔除。
  3. **首領專屬掉落物寫死代碼注入**：[`DrpgRewardService.java:L189-214`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgRewardService.java#L189-L214) 擊殺首領 `boss_song_tianheng` 時，直接在代碼中手動建構並塞入戰利品 `【黑曜骨鐮】` 與 `【長老黑話玉牌】`，而非透過怪物模板本身的 `loot` 掉落表。
  4. **技能橋接規則寫死 switch 查表**：[`SkillDefinition.java:L225-285`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/progression/SkillDefinition.java#L225-L285) 透過 60 行 `switch-case` 寫死 MUD 技能映射至 DRPG 技能的對照關係，並維護靜態字串陣列 `CANONICAL_MUD_SKILLS`。
  5. **屬性補正依賴 ID 前綴字串比對**：`SkillBridgeService.java`（已徹底物理刪除除役）曾靠 `base.getId().startsWith("taoist_") || base.getId().startsWith("star_")` 判定是否為法術補正，現由技能定義與流派標籤統一驅動。
  6. **戰鬥時限政策與閒置熔斷衝突**：[`BattleContext.java:L45, L67`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/BattleContext.java#L45-L67) `maxDurationMs` 固定 5 分鐘熔斷，即使玩家持續積極操作仍會逾時中斷，未與 10 分鐘無操作 AFK 閒置熔斷解耦。
- **改善方案**：
  - 將試道傀儡與大型敵怪移入 `mobs.json`，在 `MobTemplate` 支援 `width`、`height`、`rowIdx`、`colIdx`。
  - 異變背刺規則已取消除役；常規首領掉落全數回歸 `mobs.json` 的 `loot` 陣列。
  - 技能橋接規則下沉至 `skills.json` 的 `bridgeRules`，技能模板標註 `scaleStat`（STR/DEX/INT/WIS）。
  - 解開 5 分鐘固定戰鬥時限，全面改為依賴 `lastActivityTime` 的 10 分鐘 AFK 閒置熔斷。

### 16.3 養成、城鎮、商店與物品硬編碼盤查 (TOWN-02 / CONF-01) (P2)
- **現狀問題**：
  1. **客棧安全區判定靠字串比對**：[`RestCommand.java:L47`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/application/command/impl/RestCommand.java#L47) 以 `currentRoomId.contains("inn")` 判斷是否為客棧回滿狀態，而非檢查房間標籤 `RoomFlag.SAFE_ZONE` 或 `RoomTemplate.canRest`；野外調息值（HP 30, SAN 10, MP 20）與伏擊機率公式（`25 + danger * 0.5`）寫死。
  2. **商店台詞與身份全域寫死**：[`ShopCommand.java:L133-190`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/application/command/impl/ShopCommand.java#L133-L190) 台詞全寫死「掌櫃福伯擦著汗道」、「客官要進墨竹山吧？帶足乾糧和清心靈茶才是保命上策！」，所有城鎮的所有商店皆變成了「福伯」且在賣墨竹山補給；庫存僅放記憶體且無 `sell` 典當收購功能。
  3. **開局物資與預設心法寫死**：[`PartyService.java:L550-557`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/party/PartyService.java#L550-L557) 開局物資寫死 `taiyin_pill: 3`, `purify_talisman: 2` 等；L328-335 缺少心法預設補齊 `basic_dodge` 等；L519 提示訊息寫死可招募夥伴字串。
  4. **道核領悟技能數值寫死**：[`Party.java:L59-68`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/party/Party.java#L59-L68) 使用道核領悟技能時，技能 CD (6000ms)、倍率 (1.8)、資源消耗 (2/30/35) 寫死，未由技能定義反查。
  5. **重生點與開新遊戲預設房間寫死**：[`SaveGameService.java:L191, L461`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/save/service/SaveGameService.java#L191) 存檔與開闢新遊戲預設房間寫死 `"newbie_village:inn"`，未統一由 `GameConfig.getDefaults().getPlayer().getSpawnRoomId()` 驅動。
- **改善方案**：
  - 客棧改以 `RoomFlag.SAFE_ZONE` 判定；商店台詞移入 `ShopTemplate` 或 `MobTemplate`。
  - 開局物資移入 `application.yml`（`game.defaults.player.starting-supplies`）。
  - 全域重生房間統一讀取 `GameConfig`。

### 16.4 前端視圖與任務邏輯硬編碼 (FE-02 / CODE-01) (P2 / P3)
- **現狀問題**：
  1. **地牢 Tile 懸停文字固定為太陰地貌**：[`dungeon-panel.js:L67-97`](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/panels/dungeon-panel.js#L67-L97) 符號 `#` 寫死「青岡石壁」，`+` 寫死「封印石門」，`$` 寫死「古仙棺槨寶箱」，`^` 寫死「深淵黏液陷阱」。在墨竹礦坑探索時懸停仍顯示太陰古塚的地貌名。
  2. **裝備槽位以中文名稱硬編碼模糊推測**：[`party-equipment-tab.js:L54-61`](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/modals/party-equipment-tab.js#L54-L61) 判斷裝備槽位時，大量使用 `name.includes('冠') || name.includes('盔')`、`name.includes('靴')`、`name.includes('戒')` 等中文關鍵字比對作為 fallback。
  3. **任務事件為單一特例 Java 監聽器**：[`MozhuMinesQuestListener.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/application/listener/MozhuMinesQuestListener.java) 以硬編碼監聽 `boss_song_tianheng` 並輸出 20 行台詞，全專案無通用任務資料模型；[`QuestEventListener.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/application/listener/QuestEventListener.java) 僅有 `System.out.println`。
  4. **殘留死代碼**：`PlayerLoginListener.java`（全檔案曾皆為註解代碼，已徹底物理刪除清理）。
- **改善方案**：
  - 前端地塊 Tooltip 改由後端 DTO 攜帶動態 `tileName`。
  - 裝備嚴格依據 `equipSlot` 判定，啟動拓撲校驗器確保所有裝備皆有正確部位。
  - 清理註解死代碼。

---



## 🧭 19. 2026-10-13 Copilot 審查後續：Buff 契約與可靠性護欄 (Review Follow-ups)

> **來源**：[`docs/0_references/Copilot_Review.md`](../0_references/Copilot_Review.md)。以下項目是本次靜態審查發現，尚未代表已決議的行為設計或已完成修復；實作前仍須以現行程式碼、部署設定與測試再次驗收。

### 19.1 ASYNC-03：持久化提交與停機生命週期原子化 — ✅ 已完成 (2026-10-13)

- **讀寫鎖生命週期原子協定 (`ReentrantReadWriteLock`)**：在 [`AbstractAsyncBatchPersistenceService`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/infra/persistence/service/AbstractAsyncBatchPersistenceService.java) 導入讀寫鎖機制。提交請求以 ReadLock 併發防護；停機時以 WriteLock 原子化切斷接收管道，杜絕「先檢查通過、關閉排空結束、再入列隊列無人處理」的致命競態。
- **明確結果契約與有界背壓 (`SubmitResult`)**：新增 `submitAsync(T record)` 返回 `SubmitResult`（`ACCEPTED`, `REJECTED_SHUTTING_DOWN`, `REJECTED_QUEUE_FULL`, `REJECTED_NULL`），佇列滿載與關閉時不再靜默拒收，提供可觀測度量（`submittedCount`, `acceptedCount`, `flushedCount`, `rejectedQueueFullCount`, `rejectedShuttingDownCount`）。
- **高併發交錯競態證明與專屬整合測試**：新增 [`AsyncPersistenceLifecycleTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/AsyncPersistenceLifecycleTest.java)，以 `CountDownLatch` 在關閉瞬間併發 500 筆寫入，證明「每筆提交不是 100% 落盤就是明確回傳 REJECTED，佇列停機後 100% 乾淨清零無任何滯留」，全專案 299 項測試保持 100% 綠燈。

### 19.2 NET-01-VERIFY：WebSocket 身分與部署模式重新驗收 — P1

- 對照 `MudWebSocketHandler`、WebSocket 設定與實際公開模式，確認在建立玩家 Actor／派發遊戲指令前完成身份驗證。
- 本機模式需證明只接受 loopback；公開模式需證明缺失、錯誤或過期票證會被拒絕，合法票證會綁定正確 session。
- 在部署模式及拒絕路徑測試完成前，不把此項標示為公開部署已驗收。

### 19.3 BUFF-01：Buff 契約與共用結算切片 — ✅ 已完成 (2026-10-13)

- **權威手冊**：參閱 [`M3_1_BUFF_AND_STATUS_EFFECTS.md`](../2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md)，歷史規劃書已全面升格為正式模組手冊並收斂除役。
- **Phase 1 核心生命週期與數值機制**：實裝 `PERMANENT_DURATION = -1` 常駐陣法不消退、WoW 階梯短時間護盾優先吸收、罡氣破防條（Poise Gauge）削韌減傷與失衡大招打斷、五行法術抗性 $>100\%$ 逆流回血。
- **Phase 2 控場門控與打斷**：收斂 `Buffable.isControlled()` 與 `isSilenced()`，打斷普通攻擊、技能吟唱與 1500ms 意圖預告。
- **Phase 3 視覺投影與屬性圖示聚合 HUD**：前後端 DTO 串接 `ActiveBuffDto`，前端 `buff-hud-helper.js` 實現極簡 `[🧪 54/s x3]`、`[🛡️ 120]` 聚合 HUD 與 Tooltip，1600x900 角色道基詳細面板補齊「☯️ 道身加持與狀態」。
- **Phase 4 Gambit AI 反制與自動化測試**：擴充 `TacticsCondition`（`ALLY_HAS_DEBUFF`, `ALLY_IS_CONTROLLED`, `SELF_LACKS_SHIELD`）、AI 防呆（時效 $> 3$ 秒不重施）、淨化驅散 Cleanse；新增 `BuffDebuffComprehensiveTest` 驗收 6 大核心場景，全專案 295 項測試 100% 綠燈。

### 19.4 FALLBACK-02：生產 fallback 與架構護欄精確化 — P2

- 對生產程式碼中的 `TemplateCatalog` 與靜態 `GameConfig` fallback 逐項核對，將必要依賴改為必填注入，將測試建構限制在明確 fixture。
- 更新 `ArchitectureGuardrailsTest`，禁止未核准的生產 fallback，並提供具體檔案／行號診斷；如保留個別例外，須明列理由與測試。
- 將 FALLBACK-01 的完成定義補上零違規護欄證據，避免寬鬆數量上限掩蓋回歸。

### 19.5 WEB-STATE-01：瀏覽器儲存失敗降級 — P2

- 將 `localStorage` 存取集中至安全邊界，對可選 UI 偏好提供可診斷的預設值降級，避免模組匯入失敗；核心遊戲狀態錯誤不得被靜默吞掉。
- 測試 `getItem`／`setItem` 拋出例外時，前端仍可啟動且偏好狀態符合預期。

### 19.6 DOC-STATE-01：文件現況與驗收證據同步 — P3

- 將 `GEMINI.md`、`UI_PAGE_STRUCTURE.md` 的「現況」與 `CHANGELOG.md`、本文件的歷史／待辦資訊對齊，測試摘要記錄最後成功日期、數量及後續失敗的執行嘗試。
- 每項標為「已完成」的高風險護欄，應連結實際測試、架構檢查或部署條件；失效或寬鬆的檢查需重新開啟驗收。
- 將文件中的本機絕對 `file:///` 連結改為相對連結，確保跨端 Git 同步後仍可閱讀。



## 🧹 25. 單機語意純化與全域硬編碼大清剿 (Single-Player Purity & Anti-Hardcoding Audit) — Priority: P1

> **專題背景**：
> 於 2026-10-20 完成 SP-09 後進行之全域代碼與文檔地毯式盤查，針對專案中殘留之**非單機多人語意**與違反專案第一紅線之**寫死字串/特例**進行深度建檔與系統性清剿。

### 25.1 非單機（Multiplayer）設計與殘留代碼清單

1. **排除自己的「其他玩家廣播」**：
   - 涉及：`DropCommand.java:L87`, `GetCommand.java:L77`, `Mob.java:L355`, `PlayerService.java:L146`。
   - 現狀：丟棄/拾取/說話時調用 `room.broadcastToOthers(player.getId(), ...)`。單機房間僅有主角一人，向不存在的其他玩家發送排他性廣播在架構上是偽操作。
   - 處置：收斂為純粹的單機事件通知或主角直接感知。
2. **RoomService 廢棄多玩家重載**：
   - 涉及：`RoomService.java:L257, L277`。
   - 現狀：保留 `@Deprecated` 之 `broadcastJson(List<Player>, ...)` 與 `broadcastToOthers(List<Player>, ...)`。
   - 處置：徹底物理刪除該批過期方法。
3. **WorldManager 殘留注釋死代碼**：
   - 涉及：`WorldManager.java:L548-570` 底部 20 行 `createItem` 註解代碼。
   - 處置：安全清除。
4. **文檔測試指標滯後**：
   - 涉及：`README.md:L6, L34` 標記 315 Passed，現行實測已達 331 項。
   - 處置：校準為 331+ 項全綠燈。

### 25.2 寫死代碼（Hardcoded Code / 第一紅線違規）地毯式清單

1. **商店掌櫃名稱與進店台詞全域寫死**：
   - 涉及：`ShopCommand.java:L147, L155, L159, L168, L220, L245, L268, L296`。
   - 現狀：所有城鎮所有商店台詞皆寫死「掌櫃福伯擦著汗道」、「客官要進墨竹山吧？」。
   - 處置：依 `ShopTemplate.npcId` 向 `TemplateReader` 反查 NPC 真實姓名，改為資料驅動動態台詞。
2. **客棧與安全區以 `contains("inn")` 字串寫死比對**：
   - 涉及：`RestCommand.java:L47`, `GameStateBroadcastService.java:L88`。
   - 現狀：直接判斷房間 ID 是否包含 `"inn"` 作為客棧與安全區判定。
   - 處置：全面改由 `RoomTemplate` 之 `RoomFlag.SAFE_ZONE` 或 `flags` 集合驅動。
3. **開新遊戲、重生與地牢撤離房間寫死**：
   - 涉及：`SaveGameService.java:L207, L516`, `DungeonCommand.java:L154`, `DataIntegrityValidator.java:L353`。
   - 現狀：硬編碼 `"newbie_village:inn"` 或 `"mozhu_mines:boss_song_tianheng"`。
   - 處置：統一讀取 `GameConfig.getDefaults().getPlayer().getSpawnRoomId()`，地牢 exitRoomId 納入 Fail-Fast 啟動校驗。
4. **吸收道核技能數值與夥伴招募列表寫死**：
   - 涉及：`Party.java:L96-98`, `PartyService.java:L540`。
   - 現狀：吸收道核硬編碼 `cooldownMs(6000)`、`damageMultiplier(1.8)`；夥伴提示手寫四人名稱。
   - 處置：由 `TemplateReader` 動態反查技能範本；夥伴清單自 `getAllCompanions()` 動態產出。
5. **靜默 Fallback 棘輪殘留**：
   - 現狀：26 處 `new TemplateCatalog()` 與 5 處 `GameConfig.getInstance()`。
   - 處置：改為 Spring 建構子依賴注入，並收緊看門狗測試閾值。
6. **前端地牢懸停文字依字元符號寫死**：
   - 涉及：`dungeon-panel.js:L67-107`。
   - 現狀：`cell.title` 依符號寫死「玄岩石壁」、「石門」等固定太陰地貌名稱。
   - 處置：優先從 DTO 當前地塊名稱動態注入。

### 25.3 清剿微任務執行映射 (Clean Tasks: SP-PURITY-01 ~ DATA-GUARD-03)

| 任務編號 | 核心收斂內容 | 優先級 | 涉及模組 | 狀態 |
| :--- | :--- | :---: | :--- | :---: |
| **SP-PURITY-01** | 房間廣播通道收斂為單機通知，刪除 `@Deprecated` 多玩家方法與死代碼，對齊測試指標 | **P1** | `Room`, `RoomService`, `WorldManager` | **已完成 (331 測試全綠 ✅)** |
| **DATA-CORE-01** | 客棧安全區改為 `RoomFlag.SAFE_ZONE`，全域出生點與地牢撤離統一 `GameConfig` 驅動 | **P1** | `RestCommand`, `SaveGameService`, `DungeonCommand` | **已完成 (335 測試全綠 ✅)** |
| **DATA-CORE-02** | 商店掌櫃動態稱謂與台詞，道核領悟反查技能範本，夥伴招募提示動態產出 | **P1** | `ShopCommand`, `Party`, `PartyService` | **已完成 (337 測試全綠 ✅)** |
| **DATA-GUARD-03** | 前端地塊動態 Tooltip 支援，清理 `GameConfig.getInstance()`，收緊看門狗棘輪 | **P1** | `dungeon-panel.js`, `Living`, `Player`, `Guardrails` | **已完成 (338 測試全綠 ✅)** |







---

