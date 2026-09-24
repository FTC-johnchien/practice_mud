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
  4. **歷史計畫與架構約束審查機制 (Architecture Review Rule)**：
     - 本專案已建立實施計畫與架構決策歸檔中樞：`docs/plans/`。
     - **每次進行重大功能規劃、機制重構或修復疑難 Bug 前，AI 必須主動 Review `docs/plans/` 歷史計畫**，嚴格對齊既有的架構決策（如 Actor 無鎖併發、雙模地圖狀態機、5+2 裝備結構、戰利品自動合併等），杜絕架構退化與破壞性修改。
     - 完成重要開發後，同步在 `docs/plans/` 歸檔新的 Implementation Plan。
  5. **隊伍人數規範 (Party Size Rule)**：小隊編制上限嚴格定調為 **5 人 (`Party.MAX_PARTY_SIZE = 5`)**（對齊 Git commit `989f0debfc50ad008480af4d15953be531920b81`）。嚴格杜絕殘留「6人小隊」之表述與數值硬編碼。
  6. **雙端 Git 同步鐵律 (Cross-Platform Git Sync)**：Git 倉庫根目錄為 `practice_mud`。`GEMINI.md` 與 `docs/` 目錄必須永久納入 `practice_mud` 的 Git 追蹤與同步，杜絕公司與家中狀態脫節。
  7. **核心架構鐵律：機制運行 + 資料驅動 + 純粹測試 (Mechanism, Data-Driven & Pure Testing Pillars)**：
     - **機制由引擎通用實作 (Mechanism-Driven)**：戰鬥檢定（命中/閃避/招架）、背包格位（堆疊/上限/容量）、裝配規則（技能槽/防護判定）等，由 Java 引擎提供純粹、通用的業務機制。**嚴禁在 Java 代碼中硬編碼特定實體、職業、技能名稱或字串特例**（例如嚴禁 `switch (classId) { case "WARRIOR" -> ... }`、嚴禁 `if (k.contains("鐵牛"))`、嚴禁在容器建構子寫死塞入特定測試道具）。
     - **資料完全交由配置驅動 (Data-Driven)**：實體數值、職業技能、掉落表、別名、被動心法效果等，一律定義在 `data/**/*.json`。擴充新職業、新技能或新夥伴時，必須只需新增或修改 JSON 配置，絕不允許修改 Java 代碼分支！
     - **測試驗證機制而非特例 (Pure Mechanism Testing)**：單元與整合測試的核心目的在於驗證「遊戲機制的運行邏輯與邊界防禦（如堆疊超限、閃避率計算、未命中傷害為零、併發安全）」，測試資料是為了驗證機制而注入的，**嚴禁撰寫僅依賴寫死字串或固定特例的假測試**。

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
* **核心框架**：Spring Boot 4.1.1。
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
* [x] **Items 全域化與商店/地圖本地化參數 (Global Items & Localized Shop Overrides)**：
  * **物品原型 (Prototype) 全域收斂 (`data/global/items/`)**：
    * 建立兵刃 (`weapons/`)、防具 (`armors/`)、飾品 (`accessories/`)、消耗品 (`consumables/`)、素材 (`materials/`)、貨幣 (`currencies/`) 與任務信物 (`quest/`) 七大子目錄。
    * 支援 Spring 通配符 `classpath:data/global/items/**/*.json` 遞迴自動掃描與純淨 ID 註冊。
  * **智慧雙向容錯查詢 (`TemplateRepository.findItem`)**：
    * 輸入帶前綴 `newbie_village:healing_salve` 自動剝除前綴查得全域 `healing_salve`。
    * 輸入純 ID `black_obsidian_scythe` 若無直接匹配，自動回退查找區域特有物品。確保所有既有 90 項測試與存檔 100% 零破壞。
  * **商店本地化參數與實例覆寫 (`ShopTemplate.ShopItemTemplate`)**：
    * 支援本地指定價格 (`price`)、價格倍率 (`priceMultiplier`) 與限量庫存 (`stock`)。
    * 實例若未填寫名稱、說明或定價，自動繼承全域物品原型數值與描述 (`getEffectivePrice()`, `getEffectiveName()`, `getEffectiveDescription()`)。
  * **動態限量庫存扣減與售罄狀態**：
    * `ShopCommand` 整合線程安全 `shopStockTracker`，結算時檢核庫存並動態扣減；缺貨或售罄時給予沉浸式掌櫃回絕提示。
    * 前端 `drpg-view.js` 商店彈窗即時展示庫存標籤（`庫存: N` 或 `充足`），並於庫存為 0 時動態禁用按鈕標註 `❌ 售罄`。
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
  * **戰鬥交鋒主舞台化、敵群彈性陣列與精準集火鎖定 (Battle Main Stage, Multi-Enemy & Target Focus Lock)**：
    * **戰鬥主舞台化 (Battle Arena Main Stage)**：將戰鬥交鋒面板提升為中央主舞台（佔 60% 寬度），右側 40% 為 100% 全高度戰鬥情報日誌，徹底根除怪物卡片將戰鬥日誌壓扁的痛點；支援 `⇄ 換邊` 功能。
    * **敵群 1~7 體彈性陣列佈局**：採用自適應 CSS Grid 雙排陣列，支援 1 至 7 隻（甚至更多）敵怪同場對峙；每隻怪具備前/後衛徽章、血條、編號、眩暈狀態標籤。
    * **集火目標鎖定與轉火閉環**：修復 `BattleContext.getFrontTargetEnemy()` 原先固定索敵 index 0 的 Bug，改為嚴格優先遵循玩家指定的 `selectedTargetIndex`；新增 `testTargetSwitchingAndFocusFire` 單元測試；前端卡片點擊時即時賦予金紅色 `🎯 集火鎖定` 動態光環與對峙線提示。
  * **武器普攻動態綁定、出戲時間戳拔除與角色狀態 (C) 全面視覺化 (Weapon Auto-Attack, Clean Combat Logs & Character Info Modal)**：
    * **持劍施展劍法與真實傷害結算**：修復 MUD 探索戰鬥中 `Player` 未關聯 `PartyMember` 裝備導致誤判徒手拳腳與 4 點抓癢傷害之問題；`SkillService` 與 `CombatService` 依據隊長裝備動態解析普攻套路（手持青銅古劍自動施展 `basic_sword`：順勢一劈、中宮直刺、斜劍穿花），並正確套用武器 14~22 傷害與武器名稱。
    * **拔除出戲毫秒時間戳記**：徹底移除戰鬥日誌前綴冗餘的 `[11.339]` 時間戳，還原純淨、流暢的武學交鋒沉浸體驗；`CombatFlowTest` 新增自動化正則斷言，杜絕時間戳迴歸。
    * **角色狀態介面升級 (Character Info / C 鍵支援)**：
      - 底欄按鈕正式由 `👥 小隊 (P)` 升級為 `👤 狀態 (C)`，鍵盤熱鍵同時相容 WoW 玩家肌肉記憶的 `C` 鍵與 MUD/DRPG 習慣的 `P` 鍵。
      - 彈窗升級為 `👤 角色狀態・小隊成員與 5+2 裝備一覽`。
    * **普攻套路 (Auto-Attack Stance) 與主動絕技分層**：
      - 每名隊員卡片清楚標示【🗡️ 當前武器普攻套路】（如 `【基礎劍法】 [✔ 每輪戰鬥自動施展]`）與【⚡ 修習主動絕技與道術】。
      - `PartyMember` 支援 `enabledSkills` (`Map<SkillCategory, String>`) 與 `party enable <idx> <skillId>` 指令，實現武器專修武學切換與空手自動回歸機制。
      - 新增 `WeaponSkillBindingTest`，自動化驗證空手、穿戴長劍、自訂武學掛載、卸除武器回退之完整狀態流。
  * **集火目標鎖定同步修復、多武器普攻切換、多種高級劍法專修與非人種族天生攻擊系統 (Target Sync, Multi-Weapon, Advanced Sword Stances & Race Natural Attacks)**：
    * **集火目標鎖定與轉火同步修正**：修復前端在收到戰鬥狀態廣播時 `isSelectedTarget` 丟失之問題，優化為即時檢查 `battle.selectedTargetIndex === e.index || e.isTarget` 並配合樂觀更新，確保玩家點擊任意目標時金紅色光環與對峙線精準常駐。
    * **多武器種類自動普攻動態綁定與貨棧供給**：
      - 新增 6 款標準武器於新手村物品庫與貨棧：百辟精鋼刀（`BLADE`）、開山大斧（`AXE`）、齊眉熟銅棍（`STAFF`）、八棱玄鐵錘（`HAMMER`）、無影短匕（`DAGGER`）、穿雲桑木弓（`BOW`）。
      - 行囊開局自帶鋼刀與長槍，裝備任一武器即可自動無縫切換為對應門派基礎武學套路（刀法、棍法、鈍器、匕首、弓箭、拳腳）。
    * **角色狀態介面互動切換高級武學套路 (Interactive Stance Switching)**：
      - 擴充三套高級劍法標準 JSON：【武當太極劍法】（`taiji_sword`，白鶴亮翅、順水推舟、仙人指路、流星趕月）、【太陰幽冥劍法】（`taiyin_sword`，陰魂泣血、白骨穿心、幽冥磷火、太陰蝕日）、【天劍飛仙術】（`tianjian_sword`，青鋒掠影、劍氣破霄、萬流歸宗、天外飛仙）。
      - 主角預設精通多門劍法與拳腳，狀態彈窗（`C` 鍵）支援一鍵點擊切換當前主力普攻套路（`party enable <idx> <skillId>`），並高亮標記當前主力套路。
    * **非人種族天然攻擊體系 (Non-Human Race Natural Attacks)**：
      - `BattleEnemy` 攜帶種族屬性，戰鬥循環中依種族天然攻擊池（`naturalAttacks` 權重）動態抽選招式（如野鼠隨機施展【試探抓擊】、【十字撕裂】、【快速輕咬】、【碎骨咬合】；龍族隨機施展爪擊、撕咬、甩尾、重擊與吐息），徹底告別死板的文字廣播。
    * **整合測試升級 (`MultiWeaponAndStanceTest`)**：自動化驗證持劍多劍法切換、多武器普攻映射、卸武空手拳腳回退，以及非人怪物種族攻擊池抽取。
  * **生靈死亡清理、主角道號同步、地面靈物拾取修復、技能抽屜防閃爍與 WoW 經典典籍 (Mob Removal, Protagonist Sync, Item Pickup, Anti-Flicker & WoW Spellbook)**：
    * **怪物陣亡即時隱退**：在 `LivingService.onDeath` 注入狀態廣播並於擊殺時立即調用 `room.removeMob(mob.getId())`，根除怪物已死但仍殘留在畫面生靈清單與迎擊按鈕之問題。
    * **主角道號全生命週期同步**：修復單機模式連線時 Actor 預設名稱為「道友」之問題，改為讀取當前啟動槽位之主角名（預設「玄靈子」），並於 `SaveCommand` 建立與讀取新檔時強制同步更新 `player.setName(protagonistName)`。
    * **地面動態靈物與屍體拾取（UUID 匹配 & 行囊同步）**：修復 `TargetSelector.isMatchItem` 支援比對 UUID 與 Template ID，並於 `GetCommand` 撿起物品時同步收納至隊伍行囊 `Party.getInventory()`，同時向房間廣播更新狀態即刻消除地面拾取標籤。
    * **戰鬥技能抽屜懸停防閃爍與點擊防丟失**：定位 500ms 戰鬥心跳推送導致前端 DOM 樹銷毀重建（引發 hover 閃爍與點擊吞噬）之問題；在 `renderSkillDrawer` 與 `renderBattlePartyQuickBar` 引進 In-place DOM 局部更新，按鈕節點永久常駐，懸停流暢無痕且 100% 響應點擊。
    * **WoW 經典風格修仙武學典籍 (Spellbook Component)**：
      - 狀態面板（`C` / `P` 鍵）全面升級為魔獸世界經典法術書架構：
        1. **右側垂直 Tab 標籤列**：清晰劃分【🗡️ 兵刃套路】、【⚡ 門派絕技】、【☯️ 陣法奧義】。
        2. **左側雙欄網格卡片書頁**：38x38 典雅浮雕圖示、武學名稱、消耗類型、修為心法描述與操作按鈕（啟用主力/快捷施展/參悟中標籤）。
        3. **底部分頁控制器**：支援 `◀ 上一頁`、`下一頁 ▶`，每頁穩定展示 4 門武學，無論習得多少神功絕技皆永不擠壓變形。
    * **怪物死亡消散、戰利品儲物袋 (Loot Box) 一鍵搜刮與去屍體化 (Loot Pouch & No-Corpse Clutter)**：
      - **無掉落直接消散**：怪物氣血歸零時若掉落表（Loot Table）未擲骰出任何物品，怪物倒地化作一縷青煙消散，地面**不產生任何殘留實體或空屍體按鈕**，主舞台地面乾淨清爽。
      - **有掉落生成【散落的儲物袋 / 戰利品寶箱】**：普通怪物死亡化為 `【散落的儲物袋】`（`ItemType.CONTAINER`），精英/首領怪死亡化為 `【戰利品寶箱】`，掉落物全數收納於容器中。
      - **同場戰鬥戰利品自動合併 (Loot Auto-Merge)**：當同房間內連續擊殺多隻普通怪時，後續掉落物**自動歸攏合併進同一個【散落的儲物袋】**，按鈕動態呈現件數提示（如 `[👝 搜刮 【散落的儲物袋】 (內含 4 件靈物)]`），地面始終保持單一儲物袋不洗版。
      - **一鍵搜刮 (Loot All) 與容器自動消散**：玩家點擊 `[👝 搜刮 【散落的儲物袋】]` 或 `[📦 開啟 戰利品寶箱]`（支援 `get`/`loot`/`open`），後端自動將內部所有道具取出並納入小隊背包 `PartyInventory`，條列輸出獲得物品清單；容器本體搜刮完畢後即刻隨風消散，**徹底杜絕將整具怪物屍體塞入背包之窘境**。
      - **地面靈物視覺標籤升級**：`drpg-view.js` 與 `style.css` 針對儲物袋與寶箱賦予專屬琥珀金光 (`.is-loot-pouch`) 與紫晶靈光 (`.is-chest`)，清晰標示 `[👝 搜刮]` 與 `[📦 開啟]`。
    * **新增自動化整合測試 (`ItemPickupAndEntitySyncTest`)**：自動化驗證 UUID 物品匹配、儲物袋多怪掉落自動合併、一鍵搜刮入行囊、容器即刻銷毀消散、空屍體禁止入包、開局主角道號一致性。
  * **全實體資料驅動 (Data-Driven) 架構審查與關聯模型歸檔 (`docs/plans/`)**：
    - 完成對專案內所有 Zones、Rooms、Mobs、NPCs、Items、Skills、Races、Classes、Dungeons 的全面 Review。
    - 確認 95% 以上核心實體完全由 `resources/data/` 動態載入，客棧夥伴採 Single Source of Truth 自動註冊為城鎮生靈，戰利品與天然攻擊池全數資料驅動。
    - 繪製完整 Mermaid 實體關係圖與 Runtime 生命週期圖，歸檔於 [`docs/plans/2026-09-18_data_driven_architecture_and_entity_relations.md`](./docs/plans/2026-09-18_data_driven_architecture_and_entity_relations.md)。
  * **四大核心實體資料驅動升級與 NPC 組件化能力實裝 (Full Data-Driven & NpcCapability)**：
    - **職業資料驅動 (`classes.json` $\to$ `ClassTemplate`)**：建立 `ClassTemplate` 模型（包含成長、屬性權重、武器防具專精），擴充 `ResourceType` 支援 `MP`, `MANA`, `RAGE`, `COMBO`, `ENERGY`, `FORCE` 彈性自適應反序列化，`WorldManager` 啟動自動解析載入 6 大職業。
    - **夥伴初始武學套路資料化 (`learnedStances`)**：`CompanionTemplate` 擴充 `learnedStances` 欄位並配置主角與客棧夥伴專屬開局套路，`PartyService` 移除寫死 `if-else` 改為 100% 資料驅動。
    - **貨棧商店清單資料化 (`shops.json` $\to$ `ShopTemplate`)**：建立 `ShopTemplate` 模型與新手村 `shops.json`，`ShopCommand` 移除寫死貨物陣列，動態透過 `TemplateRepository.findShopByRoomId(roomId)` 提供交易服務。
    - **NPC 分類與組件化能力模式 (`NpcCapability`)**：建立 `NpcCapability` 模型，在 `mobs.json` 與 `default_companions.json` 聲明能力清單（交談、買賣、安歇、招募、任務）；`GameStateBroadcastService` 捨棄 hardcode 檢查，100% 依據資料動態渲染前端大尺寸能力標籤，並智慧相容隊友入隊/請離狀態切換。
  * **貨棧互動購買介面、職業系統實裝與交談指令修復 (Shop UI Buy Buttons, Class Binding & Talk Command)**：
    - **貨棧買賣商品清單卡片與批量購買**：
      - 後端 `ShopCommand` 擴充支援 `buy <id|index> [count]` 批量購買（支援名稱與貨架序號，自動驗證靈石與小隊背包容量並堆疊收納）。
      - 新增 `ShopCatalogDto`，當點選 `[🛒 貨棧買賣]` 時推送 `SHOP_CATALOG` 結構化事件。
      - 前端 `drpg-view.js` 與 `mud-core.js` 於右側日誌渲染修仙水墨風格商品櫃檯卡片，每件商品內建 `[-] [數量 1] [+]` 步進器與 `[🛒 購買]` 按鈕，點擊即時發送指令並動態反饋。
    - **`classes.json` 與角色實體完整關聯整合**：
      - `default_companions.json` 與 `CompanionTemplate` 為 6 位夥伴明確配置 `classId`（`SWORDSMAN` 俠客, `WARRIOR` 戰士, `ROGUE` 刺客, `CLERIC` 醫修, `MAGE` 法師）。
      - `PartyMember` 實體綁定 `classId`，提供 `getClassTemplate()` 動態關聯職業模板成長係數與專精武器。
      - `DrpgStateDto` 傳輸 `classId`、`className`、`classDescription`；前端 HUD 隊員卡片與小隊狀態面板（`C` 鍵）清晰展示職業專精徽章。
    - **NPC 與隊友沉浸式交談指令實裝 (`TalkCommand`)**：
      - 實裝 `TalkCommand`（別名 `ask`, `chat`, `speak`, `talkto`），解析 `talk <target>`。
      - 依序搜尋當前房間生靈與小隊成員，從資料庫中動態隨機抽選 NPC/夥伴專屬 `dialogues` 台詞並格式化輸出沉浸式問答對白。
  * **小隊招募與請離雙向指令修復與能力按鈕打通 (Party Recruit & Dismiss Bugfixes)**：
    - **`PartyCommand` 支援 `recruit` / `hire` / `dismiss` / `fire` 子指令**：
      - 修復前端點擊 `[🤝 招募入隊]` 發送 `party recruit <id>` 時被導向 `default` 並顯示指令列表的 Bug。
      - `PartyCommand` 擴充子命令處理，直接調用 `partyService.recruitCompanion(party, target)` 與 `dismissCompanion(party, target)`，招募成功立即向隊長回饋入隊宣言並廣播即時更新前端狀態。
    - **獨立 `DismissCommand` 與 `RecruitCommand` 語意解耦**：
      - 新增獨立的 `DismissCommand`（支援 `dismiss` / `fire` / `請離`），根除 `RecruitCommand` 誤把 `dismiss <id>` 當作 `recruit` 執行的別名參數切割缺陷。
      - `RecruitCommand`（支援 `recruit` / `hire` / `join` / `招募`）與 `DismissCommand` 互不干擾。
  * **貨棧交易視窗獨立模態化與純文字日誌洗版根除 (Shop Modal Popup & Clean Event Log)**：
    - **貨棧交易改為專屬獨立彈窗 (`#shop-modal`)**：
      - 前端 `index.html` 與 `style.css` 建立精緻暗金水墨風格的貨棧買賣彈出視窗（`#shop-modal`）。
      - 視窗頂部即時顯示貨棧招牌、當前盤纏靈石計數（`💰 盤纏靈石: XX 靈石`）與右上角 `✕ 關閉 (Esc)` 按鈕。
      - 包含掌櫃親切招呼語、多欄網格商品卡片（每件商品包含編號、名稱、單價、功效說明、`[-] [數量] [+]` 步進器與 `[🛒 購買]` 快捷鍵）以及底部小隊行囊提示。
      - 支援鍵盤 `Esc` 鍵即刻關閉，點擊視窗外部半透明背景無縫返回主畫面。
    - **根除 20 行純文字 ASCII 表格洗版**：
      - 後端 `ShopCommand.java` 重構 `showShopList`：連線環境僅推送結構化 `SHOP_CATALOG` 事件並回傳單行極簡開啟提示（`🏪 已開啟【新手村客棧貨棧】交易櫃檯，掌櫃正笑吟吟地候著您。`），不再向日誌噴發 20 行重複純文字表格。
      - 購買成功後再次推送更新後的 `SHOP_CATALOG`，前端 `openShopModal` 智慧判斷僅局部刷新靈石數額，不重刷或重置已輸入的購買數量。
    - **自動化測試擴充 (`DataDrivenExpansionTest`)**：
      - 新增 Test 9 `testShopCatalogPushAndCleanReply`，自動化驗證 `shop` 指令觸發 `SHOP_CATALOG` 結構化事件推送、日誌純淨無表格洗版、以及購買商品後靈石即時同步刷新。
  * **戰鬥邏輯、傷害公式、防禦博弈 (Parry/Dodge) 與屬性成長架構規範歸檔 (`docs/plans/`)**：
    - 完成傷害計算、身法閃避（`Dodge`）、兵刃招架（`Parry`）、盾牌格擋（`Block`）、暴擊與一次擲骰圓桌判定（One-Roll Combat Table）詳細設計。
    - 結合魂系氣力條（`Stamina`）博弈：閃避扣 2 精力、招架扣 3 精力，精力歸零陷入架勢崩潰（Poise Break）並受到 +20% 易傷。
    - 確立角色升級混合雙軌制：客棧夥伴 100% 依據 `classes.json` 的 `growth` 自動成長（避免 5 人小隊微操疲勞）；主角享有專屬特權，每級獲得基礎成長 + 2 點自由潛能點（`Potential`）自由配點。
    - 完整架構規範歸檔於 [`docs/plans/2026-09-18_combat_logic_damage_parry_dodge_and_growth.md`](./docs/plans/2026-09-18_combat_logic_damage_parry_dodge_and_growth.md) 並同步更新 Plan 索引。
  * **多種族專屬 Parry / Dodge 防禦技能庫與生靈生成自動綁定 (Race Natural Defense Skills & Auto-Binding)**：
    - **6 大種族防禦技能專屬化 (100% Data-Driven)**：
      - 鼠類 (`rat`)：【狡鼠閃避】（`mob_rat_dodge`：急停鼠竄、縮身遁隙、打滾鑽縫）與【尖齒偏架】（`mob_rat_parry`：尖牙偏迎、前爪格偏、細尾抽干擾）。
      - 龍族 (`dragon`)：【真龍騰挪】（`mob_dragon_dodge`：龍翼震空、騰雲掠影、神龍擺尾）與【太古龍鱗格擋】（`mob_dragon_parry`：逆鱗震刃、龍爪截架、金石硬撼）。
      - 不死族 (`undead`)：【死體錯位】（`mob_undead_dodge`：骨節錯位、腐肉滑卸）與【枯骨封架】（`mob_undead_parry`：白骨硬架、死氣纏刃）。
      - 猛獸/狼族 (`beast`/`wolf`)：【野性閃避】（`mob_beast_dodge`）與【野獸格擋】（`mob_beast_parry`）。
      - 類人族 (`humanoid`)：【本能躲閃】（`mob_basic_dodge`）與【粗暴格擋】（`mob_basic_parry`）。
      - 人類 (`human`)：【身法閃避】（`basic_dodge`）與【基礎招架】（`basic_parry`）。
    - **生靈生成與戰鬥怪動態綁定**：
      - `WorldFactory.createMob` 依怪物種族自動註冊並激活對應的 `naturalDodge` 與 `naturalParry` 至 `enabledSkills` 與 `learnedSkills`。
      - `BattleEnemy.fromTemplate` 自動映射種族防禦技能 ID，使 DRPG 戰鬥怪具備真實種族防禦反制力。
      - `PartyService` 招募冒險夥伴時自動綁定基礎身法與招架。
    - **小隊人數上限全面統一為 5 人**：
      - 對齊 Git commit `989f0debfc50ad008480af4d15953be531920b81`，全面根除代碼與文檔中殘留的「6 人」描述，動態綁定 `Party.MAX_PARTY_SIZE = 5`。
    - **跨端 Git 同步機制加固**：
      - 將 `GEMINI.md` 與 `docs/plans/` 完整納入 `practice_mud` Git 倉庫版本追蹤，確保公司與家中無縫接軌。
    - **自動化測試升級 (`RaceSkillsBindingTest`)**：
      - 新增 5 項測試，自動化覆蓋 7 大種族定義完整性、野鼠/不死族/龍族技能綁定、戰鬥怪技能映射以及隊員防禦技能開局啟用。
  * **2026-09-18（Skills 技能資料整理與武器分類治理）**：
    - **技能二級目錄體系規範化**：
      - 將 `resources/data/global/skills/` 徹底重構為二級目錄架構：`weapons/` (刀、劍、槍、鈍器、短兵、弓弩、暗器、斧、鞭)、`martial_arts/` (拳腳空手、身法、招架)、`cultivation/` (內功心法、打坐禪定)、`spells/` (五行道術、輔助醫道)、`corrupted/` (克蘇魯墮落) 與 `mobs/` (怪物專屬防禦)。
      - 舊有 59 個技能零遺失平移，保持原始技能 ID 完全相容。
    - **新增 8 個門派高階武學與五行道術**：
      - 刀法：霸刀門・霸刀歸一斬 (`badao_blade`)、狂風門・狂風絕息刀 (`storm_blade`)。
      - 槍法：天策府・破陣遊龍槍 (`dragon_spear`)。
      - 棍杖：少林寺・瘋魔杖法 (`mad_demon_staff`)。
      - 短兵：暗影閣・無影幽冥刺 (`shadow_strike`)。
      - 斧鉞：巨力門・開山裂地斧 (`mountain_split_axe`)。
      - 道術：神霄派・九天應元雷訣 (`thunder_strike` - `LIGHTNING`)、太陰門・玄冰聚煞引 (`ice_spear` - `ICE`)。
      - 全域技能擴充至 67 個，枚舉（`WeaponType`, `DamageType`）安全校驗。
    - **程式碼加固與夥伴門派武學套路初始化**：
      - `TemplateRepository` 擴充 `getAllSkills()` 查詢方法。
      - `PartyMember.getMainHandWeaponType()` 支援 `PICKAXE` -> `AXE`、`SCYTHE` -> `POLEARM` 副武器歸類。
      - `PartyMember.resolveSkillCategory()` 增強被動招架/身法（`REACTIVE`）與道術（`MAGIC`）之安全降級，防止 NPE。
      - `default_companions.json` 為鐵牛、燕青、墨道人、芷若注入門派特色武學至 `learnedStances`。
    - **自動化測試升級 (`SkillsTaxonomyAndWeaponBindingTest`)**：
      - 新增 4 項專屬測試，覆蓋全域技能多層目錄遞迴加載、9 大武器種類招式映射、夥伴門派特色武學及高階技能屬性校驗。
  * **2026-09-18（Spring Boot 4.1.1 升級與階段一核心手感優化：同伴戰術 AI、地牢伏擊調息、動態滅團重生點）**：
    - **Spring Boot 4.1.1 框架升級**：
      - 將專案由 Spring Boot 3.5.10 成功平滑升級至最新 Spring Boot 4.1.1（Jakarta EE 11 / Tomcat 11.0.24 / Hibernate 7.4.5）。
      - 清理 `application.yml` 中已過時的 `database-platform: org.hibernate.dialect.H2Dialect` 與顯式配置 `open-in-view: false`，消除啟動警示。
    - **同伴戰術 AI（Gambit / Auto-Tactics）實裝**：
      - 在 `DrpgBattleService.java` 的戰鬥循環中引入 `tryTriggerCompanionTactics`。
      - 醫修（凌霜）：任一隊員血量 $\le 45\%$ 時自動施展【神聖治癒/甘露靈泉】急救，扣除真元並進入冷卻。
      - 力士（鐵牛）：敵群 $\ge 2$ 或面對首領時，自動施展【金剛怒目/獅子吼】全場嘲諷（仇恨 +600，嘲諷 5 秒）。
      - 輸出/法師/刺客：資源充沛（怒氣 $\ge 40$、連擊點 $\ge 3$、真元 $\ge 35\%$）時自動打出爆發絕技。
      - 主角保留 100% 手動出招主導權，大幅釋放玩家在即時戰鬥中微操 5 人的認知超載。
    - **地牢調息夜襲伏擊（Ambush）與安全節點（Safe Nodes）機制**：
      - `RestCommand.java` 引入地牢安全判定：墨家非攻殘碑（`EVENT`）與向上階梯（`STAIRS_UP`）為絕對安全節點，調息 100% 成功且不增加靈壓。
      - 危險廊道調息進行暗骰：基礎 $25\% + (\text{Danger} \times 0.5\%)$。觸發伏擊時立即中斷調息並遭遇妖邪突襲開戰，徹底封堵原地刷滿血魔道心的作弊漏洞。
    - **滅團重生坐標動態化**：
      - `DrpgBattleService.resolveDefeat` 移除寫死 `(1, 1)`，改為動態讀取當前樓層 `floor.getStartCoord()`，墨竹礦坑 B1F 精確退回起點 `(1, 8)`。
    - **新增自動化整合測試 (`PhaseOneMechanicsTest`)**：
      - 新增 4 項專屬測試，自動化驗證動態重生坐標、安全節點調息免靈壓、醫修殘血自動急救、力士主動嘲諷。
  * **2026-09-18（模組化隊友戰術 AI 指針系統 / Gambit System 實裝）**：
    - **戰術規則引擎 (Tactics Rule Engine)**：
      - 新增 `TacticsCondition`（條件：`ALLY_HP_LESS_THAN`, `SELF_HP_LESS_THAN`, `ENEMY_COUNT_GTE`, `ENEMY_IS_BOSS`, `RESOURCE_GTE`, `ALWAYS`）。
      - 新增 `TacticsTarget`（目標：`LOWEST_HP_ALLY`, `SELF`, `CURRENT_ENEMY`, `ALL_ENEMIES`, `ALL_ALLIES`）。
      - 新增 `TacticsRule`（優先級 `priority`、條件、閥值、目標、技能ID、啟用開關、沉浸式文本格式化）。
    - **成員實體與戰鬥求值整合**：
      - `PartyMember` 具備 `tactics` 規則鏈，支援按優先級排序、重置職業預設、清空與新增規則。
      - `DrpgBattleService.tryTriggerCompanionTactics` 動態循序求值，支援雙補師（Healer A 於 60% 觸發小急救、Healer B 於 35% 觸發起死回生）、雙坦克（Tank 1 群怪嘲諷、Tank 2 首領單嘲）等高度客製化戰術博弈。
    - **玩家交互指令 (`PartyCommand`)**：
      - 實裝 `party tactics <隊員編號>`：檢視當前規則鏈清單與中文說明。
      - 實裝 `party tactics <隊員編號> clear`：清空規則。
      - 實裝 `party tactics <隊員編號> reset`：恢復職業預設戰術。
      - 實裝 `party tactics <隊員編號> add <優先級> <條件> <數值> <目標> <技能ID>`：新增或覆蓋規則。
    - **新增自動化整合測試 (`CustomCompanionTacticsTest`)**：
      - 新增 3 項測試，驗證雙補師血量閥值分流、雙坦群嘲與首領單嘲分工、CLI 指令互動與狀態連動。
  * **2026-09-18（前端城鎮環境人物渲染拋錯修復 / Town NPC Card Render Bugfix）**：
    - **問題定位**：`drpg-view.js` 在渲染城鎮右欄人物清單（`town-npcs-list`）時，於第 346 行直接存取未宣告的 `header` 變數（`header.innerHTML = ...`），導致瀏覽器拋出 `Uncaught ReferenceError: header is not defined`。此異常在第一位 NPC 即刻中斷渲染循環，造成計數角標顯示 `(3)` 但下方生靈卡片與能力按鈕完全空白。
    - **修復方案**：補齊 `const header = document.createElement('div'); header.className = 'npc-header';`，並重新同步靜態資源，徹底恢復客棧掌櫃福伯、力士鐵牛、醫修凌霜等生靈卡片與能力標籤（交談/買賣/招募/安歇）正常渲染。
  * **2026-09-18（五大 UI/UX 與遊戲機制缺陷徹底修復：貨棧庫存即時扣減、城鎮狀態跳轉地牢根除、戰場 5-Slot 網格、WASD 焦點解鎖、現代 RPG 狀態與同伴戰術 AI 全視覺化）**：
    - **缺陷 1：貨棧購買後彈窗內商品庫存即時扣減與售罄狀態聯動**：
      - 定位 `drpg-view.js:openShopModal` 當彈窗開啟中僅更新頂部靈石數額並直接 `return`，跳過了各行商品庫存更新邏輯。
      - 重構為動態遍歷商品，即時更新 `.shop-item-stock` 文本與顏色（剩餘庫存藍色 `#38bdf8`，售罄紅色 `#ef4444`）、動態限制數量選擇器 `max` 屬性，並於庫存為 0 時自動切換按鈕為 `❌ 售罄` 並禁用點擊。
    - **缺陷 2：客棧中卸下裝備或戰鬥遁地錯誤跳轉地牢雷達與 WASD 鎖死根除**：
      - 定位 `DrpgBattleService.pushDrpgState` 硬編碼發送 `mode: "DUNGEON"` 與 `mozhu_mines_b1f`，導致在客棧（`newbie_village:inn`）中卸除裝備或戰鬥遁地時覆蓋了城鎮模式，客戶端切入地牢雷達，且因客棧無北方出口導致按 W 無效（按 `>` 發送 `east` 方能走進廣場重置城鎮）。
      - 在 `DrpgBattleService` 引入 `stateBroadcaster` 鉤子，由 `GameStateBroadcastService` 集中判定玩家所在房間類型（`broadcastTownState` vs `broadcastDungeonState`），並將戰鬥視圖注入城鎮 DTO，徹底根除偽地牢模式切換。
    - **缺陷 3：戰場主舞台我方與敵方 5 槽位網格化（固定 1/5 寬度）與怪物體型支援**：
      - 將 `.battle-enemies-container` 與 `.battle-party-quick-bar` 的 CSS Grid 由 `auto-fit` 重構為 `repeat(5, minmax(0, 1fr))`，確保單人或 2 人隊伍、1~2 隻小怪時各自穩定佔用單排 1/5 槽位，杜絕 100% / 50% 膨脹變形。
      - 敵怪卡片支援 `colSpan` 與 `rowSpan` 體型屬性（預設 1 格，Boss 與首領自動跨 2 格或多格），保留未來巨型怪物的視覺震撼感。
    - **缺陷 4：WASD 偶發性無作用與表單/模態視窗焦點鎖定修復**：
      - 在關閉角色狀態、貨棧、存檔等模態視窗時，主動調用 `document.activeElement.blur()`，解除按鈕焦點殘留。
      - 在鍵盤事件監聽器中新增輸入框型態檢查（`INPUT`, `TEXTAREA`, `SELECT`），當玩家在輸入框打字時避免按鍵被吞噬或誤發遊戲移動，關閉彈窗後立刻恢復全局 WASD 與熱鍵響應。
    - **缺陷 5：現代 RPG 單人角色狀態頁籤、WoW 法術書防擠壓與同伴戰術方針 (Gambit AI) 全視覺化面板**：
      - 依現代 RPG（如柏德之門、女神異聞錄、FF12）標準重構 `#party-modal`：
        1. **頂部隊員切換列 (`party-modal-member-tabs`)**：直觀呈現 `#1 玄靈子`、`#2 鐵牛`、`#3 凌霜` 等隊員頁籤，自帶血條與職業徽章，點擊秒切。
        2. **子分頁切換列 (`party-modal-sub-tabs`)**：劃分【🛡️ 屬性與裝備】、【📖 武學法術】、【🎯 戰術方針 (Gambit AI)】三大子頁面。
        3. **【🛡️ 屬性與裝備】**：單人滿版卡片，完整展示氣血、真元/怒氣/連擊、道心 SAN 與 7 大裝備槽位（卸下/挑選）。
        4. **【📖 武學法術】**：WoW 風格典籍獨佔滿版寬度，給予 `.wow-spellbook-page` 增加 `min-width: 0`，給予 `.wow-spellbook-tabs` 增加 `flex-shrink: 0`，徹底解決鐵牛「門派絕技」撐爆容器擠丟右側標籤的 Bug。
        5. **【🎯 戰術方針 (Gambit AI)】**：
           - 主角（隊長）：展示「👑 隊長手操模式」卡片，說明主角擁有 100% 即時戰略決策權。
           - 同伴（鐵牛、凌霜等）：完整可視化展示當前戰術規則鏈清單（#1, #2...），帶有條件、閥值、目標與武學標籤。支援一鍵點擊【🟢 啟用中 / ⚪ 已停用】（`party tactics <idx> toggle <priority>`）、一鍵【🗑️ 刪除】（`party tactics <idx> delete <priority>`）、【➕ 新增方針】（展開規則構建表單）、【🔄 重置門派預設】與【🗑️ 清空方針】。
      - `PartyCommand` 擴充 `toggle` 與 `delete` CLI 子指令，並於 `CustomCompanionTacticsTest` 補齊自動化測試。
  * **2026-09-18（四大戰鬥站位、全隊陣法架構、怒氣積累機制與 WASD 焦點防鎖定優化）**：
    - **站位切換支援與廣播聯動**：
      - `PartyCommand` 與 `FormationCommand` 新增 `switch` / `row` 子指令（`party switch <idx> [front|back]`），支援即時切換隊員前衛/後衛戰鬥站位，並即時廣播 DRPG 狀態更新。
      - 前端個人狀態卡片與全隊站位盤全面更新為 `send('party switch ' + idx)`，徽章與站位狀態無延遲同步。
    - **全隊陣法奧義從個人武學典籍抽離為獨立面板**：
      - 個人武學典籍 (`renderMemberSpellbook`) 移除了每個隊員重複出現的「☯️ 陣法奧義」分頁，專注於「🗡️ 兵刃套路」與「⚡ 門派絕技」，並附有一鍵跳轉全隊陣法指引。
      - 在角色狀態面板頂部隊員列右側新增【☯️ 全隊陣法奧義】專屬分頁 (`renderTeamFormationView`)，集結呈現：
        1. 當前啟用陣法光環與名稱（四象辟邪陣防禦減傷 vs 玄陰噬魂陣暴擊弒魂）。
        2. 靈威充能條（0~100）與全隊終極奧義施展按鈕。
        3. 5 人隊伍前後排站位調配盤（視覺化呈現前衛 Front Row 與後衛 Back Row，提供一鍵調至前衛/後衛）。
        4. 道門陣法典籍庫（可一鍵切換結成《四象辟邪陣》或《玄陰噬魂陣》）。
    - **怒氣積累機制修復（鐵牛普攻獲取怒氣）**：
      - 定位 `DrpgBattleService` 普攻判定中漏掉了怒氣生成，導致力士在面對木樁未受傷時怒氣始終為 0。
      - 補齊 `if (member.getResourceType() == ResourceType.RAGE) { member.gainRage(15); }`，鐵牛每次揮舞重錘普攻皆可獲取 15 點怒氣，確保戰術方針正常觸發。
    - **戰鬥脫離與 WASD 輸入焦點防鎖定**：
      - `mud-ui.js:handleEnter()` 發送指令後立即呼叫 `cmdInput.blur()`，杜絕玩家手動輸入 `party` 等指令後文字框隱性佔據焦點導致 WASD 被吞噬。
      - 新增全域 `pointerdown` 監聽器，點擊非輸入框遊戲區域自動釋放文字焦點。
      - 城鎮模式中若玩家朝無出路方向按鍵（如客棧中僅有東向出口，按 W/A/S），主動於日誌輸出前路不通提示與可用出口清單，提供明確視覺回饋。
  * **2026-09-18（階段一：戰鬥經驗值結算與 1~1000 級成長曲線、主角自由配點與隊友職業自適應成長實裝）**：
    - **1~1000 級平滑冪次經驗需求曲線**：
      - 實裝 `XpProgressionService.calculateNextLevelExp(level)`：公式 $\text{NextLevelExp}(L) = \lfloor 60 \times L^{1.6} + 120 \times L \rfloor$。
      - 兼具等級 1~20 的平滑過渡（Lv.1 需求 180 EXP，升級節奏緊湊）與高等級防數值溢出（Lv.1000 約 400 萬 EXP，在 long 與 int 範圍內，拒絕階乘與指數爆炸）。
    - **隊友職業自適應成長 (Class-Adaptive Growth) 與離散 Delta 計算**：
      - 全 6 大職業成長範本（`classes.json`）平衡為每級 5 點基礎屬性預算（如戰士 CON 3.0, STR 1.5, DEX 0.5；劍客 DEX 2.5, STR 1.5, CON 1.0；法師 INT 3.5, WIS 1.0, CON 0.5）。
      - 採用無狀態離散取整算法：$\Delta = \lfloor \text{newLevel} \times w \rfloor - \lfloor (\text{newLevel} - 1) \times w \rfloor$，消除浮點數累加誤差。
      - 同伴升級依據職業模板自動提升 HP、MP 與基礎 5 維（STR/CON/DEX/INT/WIS），無自由分配點數，免除玩家操控 5 人隊伍時的微操疲勞。
    - **主角專屬特權與自由分配點數 (Free Stat Allocation)**：
      - 主角（隊長）升級除了享受職業基礎成長外，額外獲贈 **+2 自由修為點數 (`freeStatPoints`)**。
      - 支援 CLI 指令：`party stat`（檢視當前等級、經驗值條與未分配點數）與 `party stat add <str|con|dex|int|wis> [點數]`（自由分配至 5 維屬性，體質 CON 加點額外提升生命上限 HP +10，悟性 INT 加點額外提升真元上限 MP +8）。
      - 狀態即時與 `Player` 實體及存檔系統雙向同步。
    - **戰鬥大捷經驗結算與突破境界廣播**：
      - `DrpgBattleService.resolveVictory`：累計敵群擊殺經驗值，全員平分並為所有存活隊友發放修為。
      - 支援跨多等級爆發跳級（Multi-level jump）與升級氣血/真元回滿。
      - 戰後日誌輸出金光灌頂突破播報（`【金光灌頂】隊員「鐵牛」突破境界！(Lv.1 ➔ Lv.2) 基礎屬性成長: CON+3, STR+2, DEX+1`）。
    - **自動化測試全量覆蓋 (`XpProgressionServiceTest`)**：
      - 涵蓋經驗公式曲線平滑度、同伴職業自適應成長、主角 +2 點數獲取與手動分配、多等級跳級等 4 項測試全數通過。
  * **2026-09-18（階段二：角色狀態面板與 HUD 視覺化修為條與 [+] 自由加點 UI 實裝）**：
    - **角色狀態面板 (C 鍵) 修為境界條與五維屬性網格可視化**：
      - 前端 `drpg-view.js` 重構 `renderPartyModal()` 的屬性裝備 (`EQUIP`) 子分頁：
        1. **境界修為卡片 (`.party-detail-exp-card`)**：展示當前境界等級（`Lv.X`）、數值進度 (`EXP: 当前 / 晋升需求`) 與藍金色漸層修為能量條 (`.party-detail-exp-fill`)。
        2. **自由分配點數橫幅 (`.free-points-banner`)**：主角若有可用自由點數，展示微光閃爍的金色尊榮橫幅 `⭐ 道胎未定・造化充盈：尚有 N 點自由修為點數！`；若無點數或為隊員，則溫馨提示成長模式規則。
        3. **五維先天道基網格 (`.party-detail-stats-grid`)**：精細排版力量 (STR)、根骨 (CON)、靈巧 (DEX)、悟性 (INT)、定力 (WIS) 之數值、英文代號與武學影響領域。
        4. **主角專屬 `[+1]` 點擊配點按鈕 (`.stat-add-btn`)**：主角若有自由點數，五維右側點亮加點按鈕，滑鼠點擊直接調用 `send('party stat add <stat> 1')`，享受無縫即時加點與氣血/真元上限連動提升。
    - **底部 HUD 隊員卡片與頁籤等級視覺化**：
      - 底部小隊卡片標題列新增金色境界等級徽章（`.member-level-badge`：`Lv.X`）。
      - 若主角擁有未分配自由點數，底部卡片即時亮起微光金標 `[+N點]`（`.hud-free-points-pill`），點擊可直接開啟配點視窗。
      - 隊員切換頁籤（`#party-modal-member-tabs`）同步顯示各隊員等級與加點提示角標。
    - **後端雙向資料同步加固 (`PartyCommand.java`)**：
      - 加點成功後，除基礎 5 維外，同步將氣血 HP、氣血上限 MaxHP、真元 MP、真元上限 MaxMP 完整鏡像同步至 `Player.getStats()`，確保持久化與內存無任何數值漂移。
  * **2026-09-21 ~ 2026-09-22（MUD & DRPG 深度架構整合 Phase 0 ~ 3 完工）**：
    - **Phase 0 (核心邏輯 Bug 修復)**：修復 `DropCommand` 異常 re-add 背包邏輯、`EquipCommand` 移除內嵌 unequip 呼叫、`Player.lookAtMe` 修正為 `.equals()`、`LivingService.unequip` 恢復裝備解除邏輯、`RoomService.broadcastJson` 實作完成。
    - **Phase 1 (單一真相源 Single Source of Truth)**：全面廢棄舊全域 `items.json`，改以 `data/global/items/**/*.json` 分類目錄唯一收斂；實作 `PartyItemSlot` 與 `GameItem` / `ItemTemplate` 雙向無損轉換；建立 `CharacterSyncService` 維護世界主體 `Player` 與小隊隊長 `PartyMember` 雙向屬性同步。
    - **Phase 2 (穩定角色識別與生命週期)**：導入強型別值物件 `CharacterId` 徹底淘汰寫死 `"p-single"`；`PartyService` 與 `DungeonManager` 實作 Dual-Index Alias 雙向容錯索引；`TemplateRepository` 轉型為 Spring Bean 託管元件。
    - **Phase 3 (技能語意橋接與戰鬥服務解耦)**：建立 `SkillBridgeService` 實現 MUD 熟練度與 DRPG 戰術技能之動態等級縮放與解鎖；`DrpgBattleService` 解構為輕量 Facade，拆解出 `DrpgCombatLoop`、`DrpgEnemyTacticsService` 與 `DrpgRewardService`。
  * **2026-09-23（Phase 8 - 清潔架構與指令解耦完工，Commit: `e3f9896`）**：
    - **Enum 碰撞消除**：DRPG 網格方向轉為 `GridDirection`，小隊戰鬥資源轉為 `CombatResourceType`。
    - **指令責任收斂**：招募/離隊邏輯統一收斂至 `PartyService`。
    - **指令間完全解耦**：新增 `RoomMovementService` 解耦移動/觀察，擴充 `SaveGameService` 解耦存讀檔。
  * **2026-09-24（Phase 8.5 - 關鍵缺陷修復與機制純化衝刺完工）**：
    - **戰鬥 Miss Sentinel 短路免傷**：修復 `CombatService` 當 `calculateDamage()` 返回 `-1` (Miss) 時仍累加技能傷害的嚴重穿透漏洞，未命中立即短路免傷。
    - **背包堆疊上限與容器純化**：`PartyInventory` 實作 `maxStack` 分槽堆疊防禦；**徹底拔除建構子中寫死塞入的測試道具**，容器回歸純粹機制。
    - **資料驅動純化與消除 Hardcode**：**徹底拔除 `PartyService` 中硬編碼的職業技能 `switch (cId)` 與中文別名**，改由 `data/**/*.json` 配置完全驅動。
    - **房間戰利品袋併發安全**：`LivingService` 針對 `room` 與 `targetPouch` 導入同步保護，杜絕多怪同時死亡掉寶時的競態條件與異常。
    - **存檔原子化寫入與槽位防禦**：`SaveGameService` 導入 `.tmp` 暫存檔原子替換 (`ATOMIC_MOVE`) 與 `0..5` 槽位邊界防禦。
    - **構建版本統一**：`pom.xml` 中 Lombok 依賴與 annotationProcessor 版本一致化為 `1.18.48`。
    - **機制驗證測試**：新增 `MechanismPurityAndBugfixTest`，全專案自動化測試全綠通過。

---

## 6. 最新測試與健康狀況 (Latest Test Results)
* **測試時間**：2026-09-24
* **測試指令**：`.\test.ps1`（或 `mvnw test`）
* **測試項目**：涵蓋既有 156 項測試，以及 Phase 8.5 機制純化與缺陷修復套件（`MechanismPurityAndBugfixTest` 共 4 項新測試）。
* **結果**：`Tests run: 160, Failures: 0, Errors: 0, Skipped: 0` -> **BUILD SUCCESS (160 項測試全數綠燈通過，0 失敗、0 錯誤)**

---

## 7. 待辦事項與演進藍圖 (TODOs & Roadmap)
> **架構改善總清單**：完整之後續架構改進、安全防護 (P0/P1) 與併發演化藍圖，請優先參照永久存檔：👉 **[`docs/plans/FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md)**。

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
