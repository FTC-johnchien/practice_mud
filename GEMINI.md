# 專案全局指令與進度中樞 (Project Context & Progress Hub)

> **Antigravity 2.0 自動讀取規範**：
> 本檔案位於專案根目錄，是 Antigravity 2.0 每次啟動新會話、接收指令時自動無條件載入的最高優先級規則檔案。
> **跨會話／雙帳號／跨端維護鐵律**：每次 AI 完成功能開發、Bug 修復、架構調整或測試驗證後，**必須主動更新本檔案**（同步更新「當前進度」、「待辦事項」與「最新測試結果」），以確保公司與家中透過 GitHub 同步時無縫接軌。

---

## 1. 角色設定與溝通規範
* **角色**：資深 Java 架構師與後端導師（Senior Java Architect & Mentor）。
* **語言風格**：一律使用「繁體中文」回答。
* **開發原則**：
  1. **拒絕過度設計**：現階段專注於「單體模組化 (Modular Monolith)」，嚴格遵循現代 Java 25 高併發最佳實踐（Virtual Threads, Loom, Actor Pattern, Records, Sealed Types），暫不引入複雜分散式架構。
  2. 修改代碼時提供完整、清晰且含必要註解的程式碼區塊。
  3. 若發現設計有併發競爭、死鎖、記憶體洩漏或架構瑕疵，主動嚴格指出並提出重構建議。

---

## 2. 專案核心目標與玩法願景 (Game Vision & Core Goals)
* **遊戲定位**：融合 **DRPG 迷宮探索**、**魂系博弈氛圍** 與 **MMORPG 戰鬥機制** 的混合型 MUD。
* **人數定位**：
  * **目前階段**：以「**單機體驗**」為核心，單人操控主角（支援招募 NPC / 傭兵隊友組隊）。
  * **終極目標**：支援 **1-5 人在線合作 (Co-op)**，共同組隊探索地下城、挑戰大型首領。
* **核心玩法融合 (Design Pillars)**：
  1. **即時動態世界 (Real-time Living World / 不隨玩家暫停)**：
     - **世界自主運轉**：時間不因玩家停止操作而停滯。
     - **宏觀環境**：晝夜交替（時辰更迭、夜晚刷新隱藏強怪/怪物狂暴化）、動態氣候（濃霧遮雷達、陰風耗 San 值）。
     - **中觀生態**：怪物具備自主遊蕩巡邏（Patrol）、視野警戒與主動索敵，脫戰後緩慢自然回復。
     - **微觀節奏**：動作冷卻（GCD）、施法唱條、即時 DoT 扣血與精力回復。
  2. **DRPG 步進探索 (避邪除妖 / 女神轉生 / 世界樹迷宮)**：
     - 2D 矩陣 ASCII / 網格迷宮、迷霧開圖、步進探索。
     - 資源枯竭生存壓力（San 值、火把/精力消耗、背包負重），回城整備與深入地牢的抉擇。
  3. **魂系緊張感與博弈 (Elden Ring)**：
     - 克蘇魯/暗黑修仙碎片化敘事與壓迫感。
     - 氣力/精力條（Stamina）博弈、破防架勢值（Poise）、高風險高報酬戰利品。
     - 「原地發呆即死亡」的高壓探索博弈（安全區僅限客棧/城鎮或特定結界營火）。
  4. **MMORPG 戰術深度 (World of Warcraft)**：
     - 戰法牧鐵三角（Tank / Healer / DPS）站位與協同。
     - 仇恨值系統（Threat / Aggro Management）。
     - 全域冷卻 (GCD)、施法唱條、打斷機制、首領階段轉換。
* **迭代推進策略**：
  * **垂直切片優先 (Vertical Slice MVP)**：先打造「**單一城鎮 (新手村) + 單一地下城 (墨竹礦坑)**」的完整閉環（整備 $\to$ 探索 $\to$ 戰鬥 $\to$ 摸金 $\to$ 回城結算），驗證核心樂趣後再橫向擴充。

---

## 3. 專案技術棧 (Tech Stack)
* **專案名稱**：`htmlmud`
* **語言平台**：Java JDK 25（啟用 Preview 特性）。
* **核心框架**：Spring Boot 3.5.10。
* **架構風格**：單體模組化 (Modular Monolith) + 領域驅動設計 (DDD)。
* **並發模型**：Java 21+ Virtual Threads (Loom) + 自研輕量級 Actor 郵箱模型（無鎖並發、訊息驅動）。
* **資料持久化**：
  * 本機開發與單機存檔：H2 Database 2.3+ (`./db/muddb.mv.db`) + 自研 Write-Behind 異步批次落盤引擎。
  * 測試環境：獨立記憶體資料庫 (`jdbc:h2:mem:testdb`)，與實體資料庫隔離。
  * 單機存檔系統：支援多欄位 JSON 存讀檔 (`saves/slot_*.json`)。
* **前端與通訊**：
  * 前端：靜態 Web 前端（HTML5 / Canvas / Vanilla JS），整合 DRPG 2D 矩陣即時雷達與 MUD 文本輸出。
  * 通訊：Spring WebMVC REST API + WebSocket 實時雙向推送。

---

## 4. 架構設計規範 (Architecture Overview)
1. **雙層地圖模型 (Hub & Dungeon Model)**：
   * **城鎮樞紐區 (`practice_mud/src/main/resources/data/zones/`)**：圖狀拓撲房間，負責 NPC、買賣、鍛造、打坐回血、任務接取、傭兵招募與陣法配置。
   * **地牢探索區 (`practice_mud/src/main/resources/data/dungeons/`)**：2D 矩陣 ASCII 網格，具備視野迷霧、步進、暗雷隨機遇敵、機關陷阱與 San 值/精力消耗。
2. **領域實體安全封裝**：
   * `Living` 戰鬥狀態全數透過封裝方法（如 `enterCombat`、`exitCombat`、`setNextAttackTime`）維護，嚴格杜絕多執行緒直接賦值。
3. **Write-Behind 批次持久化**：
   * 繼承 `AbstractAsyncBatchPersistenceService<T>`，具備虛擬執行緒背景寫入、Buffer 滿即寫/逾時 Flush，以及 `@PreDestroy` 優雅停機（Graceful Shutdown）。

---

## 5. 當前系統狀態與進度 (Current Progress)
* [x] **四強核心區域資料庫**：
  * `newbie_village`（新手村）：客棧與暗道拓撲健全。
  * `mozhu_mines`（墨竹礦坑）：8 洞穴、精英怪、專屬掉落表與任務。
  * `snow`（雪亭鎮）：鐵匠鋪裝備完備。
  * `silverleaf`（銀葉村）：舊資料枚舉升級、飾品槽位支援。
* [x] **技能與種族庫**：
  * 35 門武學法術標準 JSON（含 10 種怪物天生攻擊）。
  * 5 大標準種族（human, wolf, rat, humanoid, undead）。
* [x] **單機垂直切片閉環 (Vertical Slice MVP - 新手村 + 墨竹礦坑)**：
  * **墨竹礦坑 2D DRPG 地牢 (`mozhu_mines_b1f.json`)**：10x10 ASCII 網格，具備視野迷霧、地牢步進、怪物池隨機暗雷、陷阱、理智古碑、寶箱與首領祭壇。
  * **首領戰與戰利品/任務事件聯動**：擊殺畸變大師兄·宋天衡觸發 `MobEvents.MobDead`，廣播現代唯物思維錨點格式化劇情，掉落【黑曜骨鐮】與【長老黑話玉牌】。
  * **新手村整備樞紐 (`newbie_village`)**：新增客棧掌櫃·福伯 (`innkeeper`) 與新手村長·白石老人 (`village_elder`)，具備沉浸式對話與道途指引。
  * **貨棧與經濟系統 (`ShopCommand`)**：支援 `shop` / `buy` / `list`，以盤纏靈石購入村莊烤麵包、辟邪清心靈茶、百草金創藥膏與採礦鐵鎬，直接收納入隊伍行囊。
  * **客棧安全區調息 (`RestCommand`)**：於客棧安歇全隊氣血、真元、道心全數回滿且靈壓不增；於地牢調息則承擔 +15 靈壓警戒風險。
  * **地牢多樓層與進出動態切換 (`DungeonManager` / `DungeonCommand`)**：支援階梯動態出入，可從礦坑地表直接進入地下 B1F，並於階梯撤離重返地表。
* [x] **單機垂直切片體驗深度優化與關鍵修復 (Vertical Slice UX & Bugfixes)**：
  * **起點與地牢位置重置問題**：單機模式起點修正為新手村客棧 (`newbie_village:inn`)，進場即看福伯與客棧介紹；重構全部指令動態解析當前地牢樓層，根除各項操作強制將玩家瞬移回太陰古塚的惡性 Bug；地牢階梯撤離支援返回對應地表房間。
  * **5+2 裝備槽位視覺化**：
    * 底部隊員 HUD 卡片即時展示主手、副手、頭部、身軀、靴履、飾品1、飾品2等 7 大部位（已穿戴亮色標籤帶 `✕` 卸下鍵，未穿戴虛線槽位標籤）。
    * 文字端 `PartyService.formatPartyStatus` 支援完整 7 大槽位排版輸出。
  * **道具掉落、寶箱拾取與背包堆疊修正**：
    * 補全地牢開寶箱 `TREASURE` 格子道具自動存入隊伍行囊與滿溢提示。
    * `PartyInventory` 支援區域前綴去綴比對 (`isSameItemId`)，消耗品正確合併堆疊（上限 99），非堆疊武器防具獨立佔位。
  * **「小隊 (Party)」互動模態面板實裝**：
    * 點擊底部 `👥 小隊 (P)` 按鈕或鍵盤按下 `P` 鍵展開 `#party-modal` 視窗。
    * 支援即時預覽 6 名隊員道號、前/後衛站位切換、HP/MP/SAN 數值，以及各成員專屬 5+2 裝備部位卡片、屬性加成檢視、一鍵卸下與挑選穿戴。
* [x] **城鎮與地牢雙模一體化、正交四向導航與能力標籤 (Hub & Dungeon Dual Mode, 4-Direction & Capabilities)**：
  * **雙模架構整合 (`GameStateBroadcastService`)**：統一城鎮拓撲與 DRPG 地牢狀態廣播（`mode: "TOWN"` 與 `"DUNGEON"`）。
  * **正交四向 (N, S, W, E) 簡化**：捨棄複雜斜向方位，全面採用北、南、西、東 4 方向；前端 WASD 與方向鍵無縫支援雙模移動。
  * **能力標籤 (Capabilities Chips) 取代常駐輸入框**：隱藏常駐對話輸入框，免除鍵盤焦點衝突；NPC 與設施自動渲染互動能力標籤（交談、買賣、安歇、招募、請離、踏入地牢、拾取）；保留 `~` 終端控制台用於除錯。
* [x] **城鎮主舞台重構與文字日誌純淨化 (Town Main Stage & Clean Event Logs)**：
  * **根除存檔槽位洗版 Bug**：在 `SaveCommand` 實裝 `quiet` / `silent` / `json` 靜默模式，僅向前端推送 `SAVE_SLOTS` JSON 結構，不向終端噴發 20 行 ASCII 字符表格；修正 `InGameBehavior`、`index.html` 與 `drpg-view.js` 連線時的重複呼叫。
  * **城鎮互動焦點移入主要視窗 (Town Main Stage)**：全面釋放左側空間，將城鎮主舞台升級為中央主要視窗（佔 60% 寬度），整合格調高雅的道途橫幅、環境描述卡、正交 4 向羅盤、地面靈物與雙欄卡片式 NPC 互動能力標籤（名字自傳不截斷、大尺寸能力標籤點擊）。
  * **抑制舊 MUD 原始文字廣播**：城鎮移動與察看時全面抑制傳統 MUD 的 `=== 房間名 ===`、`[出口]:`、`這裡有：` 原始冗長文字，改為輸出極簡優雅的單行行進提示（`🚶 小隊向東方前行，抵達【中央廣場】。`）；右側 40% 視窗專注記錄 NPC 對話、貨棧購買與冒險歷程。
* [x] **城鎮生靈右側直排佈局、明確功能能力標籤、單人/小隊動態稱謂與角色狀態全覽 (Town 2-Column Body, Explicit Capability Chips, Solo/Party Distinction & Full Character Stats)**：
  * **城鎮主舞台重構為雙欄佈局 (免除下拉捲動)**：
    * 左欄固定 250px 方位羅盤與特殊出路。
    * 右欄單欄直排「👥 當前環境人物與生靈」與「📦 地面散落靈物」，帶有生靈計數角標與專屬平滑捲動條，即便同房間湧入數十名 NPC/怪/物品，主舞台仍穩定固定高度，徹底根除畫面下拉捲動問題。
  * **明確功能能力標籤 (根除全部按鈕叫「互動」之 Bug)**：
    * 後端 `TownCapabilityDto` 傳輸 `(type, label, command, icon)`，前端 `normalizeTownCapability` 智慧雙向正規化。
    * 清晰標示 `[💬 交談]`、`[🛒 貨棧買賣]`、`[🤝 招募入隊]`、`[👋 請離隊友]`、`[🛏️ 客棧安歇]`、`[📜 任務指引]`、`[⚔️ 拔劍迎擊]`。
    * 智慧判斷夥伴是否已在隊伍中，客棧中已入隊者動態呈現 `[👋 請離隊友]`，未入隊者呈現 `[🤝 招募入隊]`。
  * **單人與組隊行進提示動態稱謂**：
    * `MoveCommand` 動態檢查隊伍成員數。單人開局時提示 `🚶 【雲中子】向東方前行，抵達【中央廣場】。`；招募夥伴入隊後自動升級為 `🚶 小隊向東方前行，抵達【中央廣場】。`。
  * **角色狀態（屬性/裝備/技能/職業/門派）全面可視化**：
    * 小隊面板 (`P` 鍵 / 點擊 `👥 小隊`) 完整展示：隊員道號、前/後衛站位切換、門派職業、氣血/真元/怒氣/連擊點、SAN 值/狂亂度、7 大部位裝備數值加成，以及已修習的所有武學與道門道術清單。
  * **戰鬥引擎異常根除與正統武學拳腳修復 (Combat Engine Bugfixes & Martial Arts Fist)**：
    * **野鼠無法還擊 Bug 根除**：定位 `newbie_village/mobs.json` 中 `wild_rat` 缺少 `race` 屬性，觸發 `TemplateRepository.findRace(null)` 時因 `ConcurrentHashMap` 禁止 null key 拋出 NPE，導致怪物攻擊回合虛擬執行緒瞬間崩潰。已修復 NPE 防禦、為野鼠補齊 `"race": "rat"` 並加固執行緒例外日誌，野鼠已能正常發動利爪抓擊與撕咬反擊並造成實質傷害。
    * **玩家赤手空拳張開血盆大口咬人修復**：修正 `LivingStats` 預設種族為 `"human"`（原為不存在的 `"DRAGON"`）；清理 `races.json` 中人族天生攻擊混入的野獸 `mob_hit`；於 `SkillService` 建立空手保底機制（未裝備武器時 100% 套用 `basic_fist` 基本拳腳：直拳突刺、側身鞭腿、上勾拳、手刀），徹底回歸正統武俠拳腳體驗。
    * **戰鬥流整合測試實裝 (`CombatFlowTest`)**：自動化驗證玩家主動發起攻擊、基本拳腳招式輸出、怪物受擊仇恨反擊以及即時傷害扣血之完整閉環。

---

## 6. 最新測試與健康狀況 (Latest Test Results)
* **測試時間**：2026-09-17
* **測試指令**：`.\test.ps1`（或 `mvnw test`）
* **測試項目**：
  * `CombatFlowTest`：玩家空手基本拳腳招式判定、野鼠即時反擊與傷害結算驗證。
  * `HtmlmudApplicationTests`：Spring Boot 啟動與資料庫配置驗證。
  * `MozhuMinesIntegrationTest`：礦坑場景動態載入、Boss 戰鬥與任務掉落驗證。
  * `MozhuMinesDungeonIntegrationTest`：墨竹礦坑 10x10 DRPG 地牢載入、迷霧開圖、步進與首領祭壇觸發驗證。
  * `NewbieToMozhuLoopIntegrationTest`：新手村客棧整備購藥 -> 啟程前往礦坑 -> B1F 步進探索使用靈藥 -> 首領討伐掉落 -> 撤離回村安歇之完整閉環驗證。
  * `TownDungeonDualModeIntegrationTest`：單人開局、客棧招募/請離夥伴、雙模狀態廣播、4 正交方向拓撲移動、靜默存檔查詢與簡約城鎮行進日誌驗證。
  * `PartyEquipmentIntegrationTest` / `PartyFormationTest`：隊伍陣法與裝備協同驗證。
  * `PartyInventoryAndMadnessTest`：DRPG 地牢步進、San 值/狂亂度、消耗品去綴堆疊與 5+2 裝備輸出測試。
  * `TaiyinTombDungeonTest`：太陰古塚步進與暗雷測試。
  * `SaveGameServiceTest`：單機多槽位 JSON 存讀檔驗證。
  * `WorldDataIntegrityTest`：4 大區域載入、出口拓撲無懸空、自然攻擊與技能映射、Bug 迴歸測試。
* **結果**：`Tests run: 53, Failures: 0, Errors: 0, Skipped: 0` -> **BUILD SUCCESS (53 項測試全數綠燈通過)**

---

## 7. 待辦事項與演進藍圖 (TODOs & Roadmap)
### 階段一：垂直切片閉環 (Single Town + Single Dungeon MVP)
- [x] **打通新手村與墨竹礦坑的完整單機遊玩循環 (垂直切片 MVP 完成)**：
  - 城鎮接取指引/客棧購買靈茶乾糧 $\to$ 出發進入墨竹礦坑 $\to$ DRPG 步進探索消耗 San 值 $\to$ 遭遇戰鬥/討伐首領 $\to$ 拾取戰利品撤離回村安歇。
- [x] **城鎮/地牢雙模操作整合與全視覺化能力標籤**：
  - 城鎮正交四向羅盤 + NPC/設施互動標籤（交談/購買/安歇/招募）；地牢 2D ASCII 即時雷達與步進探索。
- [x] **單人開局與隊伍/傭兵招募雏形**：
  - 新手村客棧招募力士·鐵牛（坦克）與醫修·凌霜（治療），支援單人開局組隊。
- [ ] **戰鬥狀態機 (FSM) 與 GCD 機制**：
  - 導入全域冷卻時間 (GCD)、施法前搖/唱條與後搖。
  - 加入初步仇恨（Threat/Aggro）基礎結構，坦克嘲諷與補師治療仇恨機制。

### 階段二：戰鬥深度與動態機制 (Combat Depth & Scripting)
- [ ] **精力/氣力條 (Stamina) 與破防處決**：
  - 攻擊與閃避消耗精力，精力枯竭時受到暴擊增傷。
- [ ] **動態腳本 (Lua / GraalVM JS) 與怪物行為樹 (Behavior Tree)**：
  - NPC 對話、陷阱解謎腳本化。
  - Boss 具備轉階段與狂暴機制。

### 階段三：多人在線合作 (1-5 Players Online Co-op)
- [ ] **組隊系統與副本房局實例 (Dungeon Instance Actor)**：
  - 多人組隊進入同一個地下城，同步迷霧與隊員位置。
  - WebSocket 廣播戰鬥動作與即時血量更新。
- [ ] **通訊優化與背壓控制 (Protobuf / Backpressure)**。

---

## 8. 常用執行與除錯指令
* **啟動遊戲**：執行根目錄 `.\run.ps1`（預設 Port: 8080）。
* **執行測試**：執行根目錄 `.\test.ps1`。
* **H2 管理後台**：瀏覽器訪問 `http://localhost:8080/h2-console`（JDBC URL: `jdbc:h2:file:./db/muddb`，帳號 `sa`，密碼 `password`）。
