# 專案架構與系統規格說明書 (System Architecture & Project Specifications)

> **文件定位**：  
> 本文件作為專案宏觀架構、後端領域驅動設計 (DDD)、並發模型、目錄劃分與資料標準的**單一權威來源 (Single Source of Truth)**。  
> 遵循「每次進行項目新增或變更時，必須主動 Review 本文件並進行必要修調」的架構治理鐵律。  
> *(註：Web 前端視圖與 7 大模組化規範請參閱專用規格書：👉 [`docs/UI_PAGE_STRUCTURE.md`](./docs/UI_PAGE_STRUCTURE.md))*

---

## 📌 目錄 (Table of Contents)

1. [核心架構哲學與三大開發鐵律](#1-核心架構哲學與三大開發鐵律)
2. [實體目錄與原始碼分層結構](#2-實體目錄與原始碼分層結構)
3. [領域驅動設計 (DDD) 與後端分層架構](#3-領域驅動設計-ddd-與後端分層架構)
4. [虛擬執行緒與 Actor 無鎖並發模型](#4-虛擬執行緒與-actor-無鎖並發模型)
5. [資料驅動與雙層地圖模型 (Hub & Dungeon Model)](#5-資料驅動與雙層地圖模型-hub--dungeon-model)
6. [MUD 與 DRPG 共用整合架構 (Unified Integration)](#6-mud-與-drpg-共用整合架構-unified-integration)
7. [新增遊戲內容之標準作業程序 (SOP)](#7-新增遊戲內容之標準作業程序-sop)
8. [文檔治理與關聯索引指針](#8-文檔治理與關聯索引指針)

---

## 1. 核心架構哲學與三大開發鐵律

### 鐵律一：機制運行 + 資料驅動 + 純粹測試 (Mechanism, Data-Driven & Pure Testing)
1. **機制由引擎通用實作 (Mechanism-Driven)**：
   - 戰鬥檢定（命中/閃避/招架/格擋）、背包格位（堆疊/上限/容量）、裝配規則（技能槽/防護判定）等，由 Java 引擎提供純粹、通用的業務機制。
   - **嚴禁在 Java 代碼中硬編碼特定實體、職業、技能名稱或字串特例**（例如嚴禁 `switch (classId) { case "WARRIOR" -> ... }`、嚴禁 `if (k.contains("鐵牛"))`、嚴禁在容器建構子寫死塞入特定測試道具）。
2. **資料完全交由配置驅動 (Data-Driven)**：
   - 實體數值、職業技能、掉落表、別名、被動心法效果等，一律定義在 `src/main/resources/data/**/*.json`。
   - 擴充新職業、新技能或新夥伴時，必須只需新增或修改 JSON 配置，絕不允許修改 Java 代碼分支。
3. **測試驗證機制而非特例 (Pure Mechanism Testing)**：
   - 單元與整合測試的核心目的在於驗證「遊戲機制的運行邏輯與邊界防禦（如堆疊超限、閃避率計算、未命中傷害為零、併發安全）」，測試資料是為了驗證機制而注入的，**嚴禁撰寫僅依賴寫死字串或固定特例的假測試**。

### 鐵律二：拒絕過度設計，擁抱現代 Java
- 現階段專注於**單體模組化 (Modular Monolith)**，嚴格遵循 Java 21+ / 25 現代特性（Virtual Threads, Loom, Actor Pattern, Records, Sealed Types, Pattern Matching）。
- 暫不引入複雜分散式架構與重量級消息佇列，以輕量、高吞吐與易於本機除錯為最高準則。

### 鐵律三：單一真相源與嚴格邊界
- 靜態資料只有一份 Canonical Definition；可變執行期實體以 Instance / State 表達，嚴禁回寫 Template。
- 前端傳輸模型是 Projection / DTO，絕不能成為遊戲規則的來源。

---

## 2. 實體目錄與原始碼分層結構

```
practice_mud/
├── pom.xml                       # Maven 建構配置 (Java 25, Spring Boot 4.1.1)
├── run.bat / run.ps1             # 遊戲啟動腳本 (Port 8080)
├── test.bat / test.ps1           # 自動化測試腳本
├── GEMINI.md                     # 📜【專案憲法】最高元契約、第一紅線、技術邊界與按需載入矩陣
├── ACTIVE_TASKS.md               # 🎯【作戰看板】當前迭代單一真相源，以 [ ] 微任務推進 (AI Agentic Workflow)
├── README.md                     # 🚀【專案首頁】四層體系全景導覽地圖與極速開始指南
├── ARCHITECTURE.md               # 🏗️【本文件】系統與後端架構說明書
├── CHANGELOG.md                  # 📜【歷史紀錄中樞】歷史版本演進與變更紀錄
├── saves/                        # 本機 JSON 遊戲存檔 (slot_*.json, autosave.json)
├── db/                           # 本機 H2 資料庫檔案 (./db/muddb.mv.db，Git 忽略)
├── docs/                         # 設計文檔與規格中樞 (四層體系)
│   ├── overview/                 # 🚀【簡要概覽層】MODULES_MAP, FRONTEND_LAYOUT_GUIDE, QUICKSTART
│   ├── manual/                   # 🔬【深入手冊層】後端 M1~M9 (含 M3-1 戰鬥子模組)、前端 FE-1~FE-6 深度技術手冊
│   ├── plans/                    # 📈【長遠演化層】FUTURE_IMPROVEMENTS.md (P0~P3 全局待辦)
│   ├── references/               # 📚【原始靈感與參考素材庫】世界觀文本、概念圖與素材儲備
│   ├── templates/                # 靜態資源 JSON 規範範本 (如技能、配置草稿)
│   └── UI_PAGE_STRUCTURE.md      # 前端共用常數、工具庫與歷史視圖規範
└── src/
    ├── main/
    │   ├── java/com/example/htmlmud/ # 核心 Java 原始碼 (DDD 架構分層)
    │   └── resources/
    │       ├── application.yml       # Spring Boot 核心配置
    │       ├── logback-spring.xml    # 日誌配置
    │       ├── data/                 # 遊戲靜態數值與地圖資料庫 (Data-Driven JSON)
    │       └── static/               # 前端靜態資源 (HTML5 / Vanilla CSS / ES6 JS)
    └── test/                         # 單元與整合測試套件 (獨立記憶體資料庫)
```

---

## 3. 領域驅動設計 (DDD) 與後端分層架構

後端 Java 原始碼（`com.example.htmlmud`）依領域驅動設計劃分為清晰的四層架構：

```
com.example.htmlmud/
├── HtmlmudApplication.java             # Spring Boot 進入點
│
├── config/                              # === 基礎設施配置 ===
│   ├── GameConfig.java                  # 全域遊戲數值與規則設定檔 (集中管理常數)
│   ├── AsyncConfig.java                 # 非同步執行配置
│   ├── GuiBridge.java                   # GUI 單機模式橋接
│   ├── JacksonConfig.java              # JSON 序列化配置
│   ├── SchedulerConfig.java            # 排程器配置
│   ├── SecurityConfig.java             # 安全配置
│   └── WebSocketConfig.java            # WebSocket 端點配置
│
├── domain/                              # === 核心領域層 (Domain Layer) ===
│   ├── actor/                           # Actor 模型核心
│   │   ├── core/                        #   Actor 核心基類與通訊緩衝
│   │   │   ├── VirtualActor.java        #     虛擬執行緒 Actor 抽象基類
│   │   │   ├── RoomMessageBuffer.java   #     房間訊息緩衝
│   │   │   ├── MessageFragment.java     #     訊息片段
│   │   │   └── MessageOutput.java       #     訊息輸出介面
│   │   ├── impl/                        #   Actor 實體 (Living, Player, Mob, Room)
│   │   └── behavior/                    #   行為模式 (PlayerBehavior, MobBehavior, GuestBehavior)
│   │
│   ├── combat/                          # 戰鬥核心規則與呈現投影 (UNIFY-03 SSoT)
│   │   ├── CombatRules.java             #   純函式戰鬥演算法 (基礎傷害/五行抗性/一次擲骰圓桌判定)
│   │   ├── CombatPresentation.java      #   雙模呈現投影 (MUD 文字敘事 + Web 結構化事件)
│   │   └── Combatant.java               #   大一統戰鬥參與者介面 (繼承 Buffable)
│   │
│   ├── service/                         # 領域服務 (無狀態業務邏輯)
│   │   ├── WorldManager.java            #   世界管理器 (房間管理與生命週期)
│   │   ├── CombatService.java           #   MUD 即時戰鬥引擎
│   │   ├── LivingService.java           #   生靈通用服務 (裝備/受傷/死亡)
│   │   ├── PlayerService.java           #   玩家專屬服務 (登入/斷線/復活)
│   │   ├── RoomService.java             #   房間服務 (進出/廣播/觀察)
│   │   ├── RoomMovementService.java     #   房間移動服務
│   │   ├── SkillService.java            #   技能解算服務
│   │   ├── SkillBridgeService.java      #   MUD↔DRPG 技能語意橋接
│   │   ├── XpProgressionService.java    #   經驗值與等級成長
│   │   ├── CharacterSyncService.java    #   角色狀態同步服務
│   │   └── TemplateCatalog.java         #   模板目錄 (TemplateReader 實作)
│   │
│   ├── event/                           # 領域事件骨幹 (EVENT-01)
│   │   ├── MobEvents.java               #   怪物事件 (MobDead)
│   │   └── PlayerEvents.java            #   玩家事件 (LevelUp, ItemAcquired)
│   │
│   ├── port/                            # 六角形架構輸出埠 (Phase 10 Output Ports)
│   │   ├── CommandDispatcherPort.java   #   指令分派埠
│   │   └── ClientSessionManagerPort.java # 客戶端會話管理埠
│   │
│   ├── validation/                      # 啟動期拓撲校驗與看門狗
│   │   └── DataIntegrityValidator.java  #   資料完整性校驗器
│   │
│   ├── repository/                      # 領域唯讀介面
│   │   └── TemplateReader.java          #   模板唯讀抽象契約
│   │
│   ├── factory/                         # 領域工廠
│   │   └── ItemFactory.java             #   標準物品工廠 (ItemInstance / PartyItemSlot 生成)
│   │
│   ├── model/                           # 領域模型實體與值物件
│   │   ├── definition/                  #   不可變標準定義 (ItemDefinition)
│   │   ├── entity/                      #   可變實體 (LivingStats, GameItem, ItemInstance, SkillEntry)
│   │   ├── view/                        #   只讀合成視圖 (ItemView)
│   │   ├── template/                    #   不可變定義模板 (Record 封裝)
│   │   │   ├── ItemTemplate.java        #     物品模板
│   │   │   ├── MobTemplate.java         #     怪物模板
│   │   │   ├── RoomTemplate.java        #     房間模板
│   │   │   ├── SkillTemplate.java       #     技能模板
│   │   │   ├── RaceTemplate.java        #     種族模板
│   │   │   └── ZoneTemplate.java        #     區域模板
│   │   └── enums/                       #   領域枚舉 (DamageType, EquipmentSlot, Posture 等)
│   │
│   ├── dungeon/                         # DRPG 地牢領域子域
│   │   ├── manager/DungeonManager.java  #   地牢管理器
│   │   ├── service/DungeonFloorLoader.java # 地牢地圖載入器
│   │   ├── model/                       #   地牢地圖模型 (DungeonFloor, DungeonTile)
│   │   ├── battle/                      #   DRPG 戰鬥子系統
│   │   │   ├── DrpgBattleService.java   #     DRPG 戰鬥門面
│   │   │   ├── DrpgCombatLoop.java      #     戰鬥心跳迴圈
│   │   │   ├── CombatFsmService.java    #     戰鬥狀態機 (FSM)
│   │   │   ├── DefenseResolver.java     #     一次擲骰圓桌防禦檢定 (Miss/Dodge/Parry/Block/Crit/Hit)
│   │   │   ├── BuffSettlementService.java #   Buff/Debuff 結算引擎
│   │   │   └── ThreatTable.java         #     仇恨管理表
│   │   └── tactics/                     #   戰術與方針 (GambitEvaluator)
│   │
│   ├── party/                           # 小隊領域子域
│   │   ├── model/                       #   小隊模型 (Party, PartyMember, FormationTemplate)
│   │   └── service/PartyService.java    #   小隊管理服務
│   │
│   └── save/                            # 存檔領域子域
│       ├── model/SaveData.java          #   存檔聚合根模型 (schemaVersion v2)
│       ├── dto/SaveSlotDto.java         #   存檔槽位 DTO
│       └── service/SaveGameService.java #   存讀檔與版本平滑遷移服務
│
├── application/                         # === 應用層 (Application Layer) ===
│   ├── command/                         # 指令處理模式 (Command Pattern)
│   │   ├── GameCommand.java             #   指令介面
│   │   ├── CommandDispatcher.java       #   指令分派器
│   │   ├── parser/CommandParser.java    #   指令解析器
│   │   └── impl/                        #   具體指令 (Look, Move, Kill, Cast, Shop 等)
│   ├── dto/                             # 資料傳輸物件 (DRPG_STATE, SaveSlotsDto 等)
│   └── factory/WorldFactory.java        # 世界實體建立工廠
│
├── infra/                               # === 基礎設施層 (Infrastructure Layer) ===
│   ├── persistence/                     # 資料庫持久化 (JPA Repository & Service)
│   │   ├── service/AbstractAsyncBatchPersistenceService.java # Write-Behind 基類
│   │   └── repository/TemplateRepository.java # 模板唯讀快取
│   ├── server/                          # 網路伺服器與脈衝核心
│   │   ├── MudWebSocketHandler.java     #   核心 WebSocket 處理器 (限流/長度檢查)
│   │   ├── SessionRegistry.java         #   會話連線登錄註冊表
│   │   ├── WorldPulse.java              #   戰鬥與世界 100ms 心跳脈衝
│   │   └── WorldInitializer.java        #   開機資料庫預載入器
│   └── util/AnsiColor.java              # ANSI 文本色彩轉義
│
├── protocol/                            # === 通訊協定層 (Protocol Layer) ===
│   ├── ActorMessage.java                #   Actor 郵箱訊息協定
│   ├── MessageFactory.java              #   MUD 訊息工廠
│   ├── MudMessage.java                  #   雙向通訊封包模型
│   └── WebSocketOutput.java             #   WebSocket 輸出配接器
│
└── static/                              # === 前端 Web 視圖模組 (Frontend Architecture) ===
    ├── css/                             # 模組化樣式庫 (Cascade 串流加載)
    │   ├── style-01-tokens-and-shell.css #   全域 Tokens 與基礎骨架
    │   ├── style-02-combat-arena.css    #   戰鬥主舞台與 5x5 敵陣
    │   ├── style-03-battle-actions.css  #   戰備指令台與冷卻唱條
    │   ├── style-04-main-menu.css       #   1600x900 滿版主選單外框
    │   ├── style-05-town-and-hub.css    #   城鎮生活舞台與羅盤
    │   ├── style-06-shop-and-responsive.css # 貨棧交易彈窗與響應式
    │   └── style-07-battle-fx.css       #   戰鬥 VFX 特效、護盾流光與呼吸邊框
    └── js/
        ├── core/                        # 前端核心中樞 (Core Engine)
        │   ├── constants.js             #   全域常數中樞 (字級 >= 13px、快捷鍵、日誌上限)
        │   ├── ui-utils.js              #   純函式工具庫 (escapeHtml 全域防禦、百分比換算)
        │   ├── state-store.js           #   全域響應式狀態管理 (StateStore)
        │   ├── event-bus.js             #   解耦事件匯流排 (EventBus)
        │   ├── cmd-dispatcher.js        #   指令分派與 WebSocket 封包發送
        │   └── buff-hud-helper.js       #   Buff 聚合圖示 HUD 與 Tooltip 輔助器
        ├── fx/                          # 戰鬥視覺特效引擎 (VFX)
        │   ├── battle-fx-director.js    #   演出指揮中心
        │   ├── floating-text.js         #   正交色彩浮動數字
        │   ├── fx-presets.js            #   武器與法術粒子預設庫
        │   └── fx-geometry.js           #   命中幾何座標運算
        ├── panels/                      # 視圖面板模組 (Panels)
        │   ├── town-panel.js            #   城鎮探索、NPC 互動與羅盤
        │   ├── dungeon-panel.js         #   地牢迷宮雷達、靈壓與視野
        │   ├── battle-panel.js          #   戰鬥主舞台、5x5 敵陣與指揮台
        │   ├── party-hud-panel.js       #   5 人小隊 HUD 狀態列與三色能量條
        │   └── message-log-panel.js     #   情報文字日誌與 ANSI 色碼解析
        └── modals/                      # 彈窗與子功能頁籤模組 (Modals & Subtabs)
            ├── party-modal.js           #   小隊主選單 Coordinator 與路由
            ├── party-equipment-tab.js   #   5+2 裝備穿脫與數值 Diff 比對
            ├── party-skills-tab.js      #   WoW 風格武學法術書與被動心法
            ├── party-formation-tab.js   #   戰術站位盤與道門陣法庫
            ├── party-tactics-tab.js     #   Gambit 規則鏈配置
            ├── shop-modal.js            #   城鎮貨棧交易彈窗
            ├── save-modal.js            #   存讀檔管理與標題畫面
            ├── victory-modal.js         #   戰鬥大捷翻牌結算彈窗
            ├── skill-drawer.js          #   隊員技能盤抽屜
            └── bag-drawer.js            #   隊伍公共行囊抽屜
```

---

## 4. 虛擬執行緒與 Actor 無鎖並發模型

1. **Virtual Threads (Project Loom)**：
   - 專案全面啟用 Java 21+ 虛擬執行緒。每個 Actor（玩家、怪物、房間、地牢迴圈）均在獨立的虛擬執行緒排程中運作，杜絕傳統執行緒池飽和瓶頸。
2. **Actor 郵箱模型 (Actor Pattern)**：
   - 每個實體（`Player`、`Mob`、`Room`）繼承自 [`VirtualActor`](./src/main/java/com/example/htmlmud/domain/actor/core/VirtualActor.java)，持有獨立的訊息佇列（Mailbox）。
   - **無鎖併發安全**：實體內部狀態（如坐標、HP、戰鬥目標）僅能在自身的 Actor 訊息處理循環中修改，禁止外部執行緒直接賦值。
   - **廣播緩衝防死鎖**：跨房間或全地圖廣播時，使用 `RoomMessageBuffer` 進行非同步解耦，消除 Actor 之間互相等待造成的死鎖。
3. **Write-Behind 批次非同步持久化**：
   - 存檔服務繼承 [`AbstractAsyncBatchPersistenceService<T>`](./src/main/java/com/example/htmlmud/infra/persistence/service/AbstractAsyncBatchPersistenceService.java)。
   - 雙觸發機制：緩衝區滿（50~100 筆）立即寫入，或超時（500ms）定期 Flush。
   - 具備 `@PreDestroy` 優雅停機（Graceful Shutdown），確保伺服器關閉時資料 100% 完整落盤。

---

## 5. 資料驅動與雙層地圖模型 (Hub & Dungeon Model)

所有遊戲靜態內容均存放於 `src/main/resources/data/`：

```
resources/data/
├── global/                        # 全域共用資料
│   ├── races.json                 #   種族設定 (human, wolf, rat, humanoid, undead)
│   ├── classes.json               #   門派/職業設定
│   ├── weapon_config.json         #   武器分類設定
│   ├── items/                     #   全域物品原型 (weapons, armors, consumables, misc)
│   └── skills/                    #   武學法術 JSON (按門派與派系分類)
├── zones/                         # 城鎮樞紐區 (圖狀拓撲房間)
│   ├── newbie_village/            #   新手村 (客棧、暗道、長老)
│   ├── mozhu_mines/               #   墨竹礦坑 (地表與外圍)
│   ├── snow/                      #   雪亭鎮 (鐵匠鋪)
│   ├── silverleaf/                #   銀葉村
│   └── taiyin_tomb/               #   太陰古塚入口
├── companions/                    # 同伴模板 (Single Source of Truth 轉化客棧 Mob)
├── party/                         # 小隊主動技能模板
├── formations/                    # 道門陣法模板 (站位加成與奧義大招)
└── dungeons/                      # 地牢探索區 (DRPG 2D 矩陣迷宮)
```

### 雙層地圖運作機制：
- **A. 城鎮樞紐區 (`data/zones/`)**：圖狀拓撲房間，負責 NPC 對話、交易買賣、打坐回復、招募隊員與陣法配置。
- **B. 地牢探索區 (`data/dungeons/`)**：2D ASCII 矩陣網格，具備視野迷霧、步進導航、暗雷隨機遇敵、機關陷阱與 SAN/精力博弈。
- **C. 資源命名空間 (Namespace)**：全域物品標記 `global:<id>`，區域特有物品標記 `<zone>:<id>`。開機自動執行完整性校驗 (`validate()`)，杜絕懸空出口與缺漏引用。

---

## 6. MUD 與 DRPG 共用整合架構 (Unified Integration)

1. **物品單一真相源**：所有物品集中於 `data/` 定義，透過 Adapter 投影至 MUD `GameItem` 或 DRPG 行囊，嚴禁兩套重複規則。
2. **穩定角色識別**：以強型別值物件 `CharacterId` 作為跨系統唯一識別，配合雙重索引別名 (Dual-Index Alias) 杜絕狀態分裂。
3. **技能語意橋接 (`SkillBridgeService`)**：MUD 武學等級與心法即時投影為 DRPG 戰術技能，戰後成果透過 `awardCombatSkillXp` 回饋 MUD 熟練度。
4. **小隊編制規範**：小隊出戰人數上限嚴格定調為 **5 人 (`Party.MAX_PARTY_SIZE = 5`)**。
5. **一次擲骰圓桌判定 (`DefenseResolver`)**：
   - 防禦結算淘汰連續 `if-else`，採用單一隨機浮點數 $[0.0, 1.0)$ 依累積區間結算：
     $$\text{[Miss]} \to \text{[Dodge]} \to \text{[Parry]} \to \text{[Block]} \to \text{[Crit]} \to \text{[Normal Hit]}$$
   - 確保身法閃避、兵刃招架、副手盾牌格擋與致命重創擁有確定且無順序偏倚的機率分佈。

---

## 7. 新增遊戲內容之標準作業程序 (SOP)

### 7.1 新增一門武學技能 (Skill)
1. 確定技能派系（如 `sword`, `blade`, `dodge`, `parry`, `force`, `common`）。
2. 在 `src/main/resources/data/global/skills/<派系>/` 下新增 `<skill_id>.json`。
3. 確保包含完整 `mechanics`（傷害倍率、消耗類型、命中與防禦檢定）與沉浸招式文本 (`moves`)。
4. 執行 `./test.ps1` 驗證資料載入與語法正確。

### 7.2 新增一個地牢迷宮樓層 (Dungeon Floor)
1. 在 `src/main/resources/data/dungeons/` 新增 `<floor_id>.json`。
2. 規劃 2D `layout` ASCII 矩陣與 `legend` 圖例標記（`#` 牆、`.` 路、`$` 寶箱、`!` 首領）。
3. 在 `encounters.mobs` 引用標準怪物 ID，設定遭遇率與探索事件。

---

## 8. 文檔治理與關聯索引指針

本專案文檔體系採取嚴格的「分層收斂、各司其職」治理原則：

| 文檔層級 | 檔案路徑 | 定位與職責 |
| :--- | :--- | :--- |
| **憲法核心** | [`GEMINI.md`](./GEMINI.md) | **【專案最高元契約】** 100% 資料驅動第一紅線、不可刪除保護鎖、按需載入矩陣 |
| **當前作戰看板** | [`ACTIVE_TASKS.md`](./ACTIVE_TASKS.md) | **【當前迭代唯一真相源】** Checkbox 微任務清單、DoD 三位一體交付標準 |
| **宏觀架構說明** | [`ARCHITECTURE.md`](./ARCHITECTURE.md) | **【後端與系統設計唯一來源】** 潔淨架構、DDD 模型、Actor 併發、SOP |
| **模組全景與佈局** | [`docs/overview/`](./docs/overview/) | **【高階概覽】** `MODULES_MAP.md`、`FRONTEND_LAYOUT_GUIDE.md`、`QUICKSTART.md` |
| **16 本模組技術手冊** | [`docs/manual/`](./docs/manual/) | **【前後端模組唯一來源】** 後端 M1~M9 (含 M3-1 戰鬥子模組)、前端 FE-1~FE-6 深度手冊與未來演進規劃 |
| **未來演化藍圖** | [`docs/plans/FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md) | **【長期待辦唯一來源】** 未決議項目、P0~P3 架構改善總庫 |
| **版本變更紀錄** | [`CHANGELOG.md`](./CHANGELOG.md) | **【過去歷程唯一來源】** 各 Phase 歷史落地、重構與 Bug 修復紀錄 |

---

## 9. 🚀 系統架構演進與現代化擴充願景 (System Architecture Roadmap)

為使本專案由「單體單機原型」平滑演進至「百人同服、萬人線上的高併發高可用現代遊戲伺服器」，確立以下四大宏觀架構演進支柱：

### 1. 現代架構規劃 (Modern Enterprise & Gaming Architecture)
- **領域事件驅動架構 (Domain Event-Driven Architecture, EDA)**：
  - 全面解耦服務間的同步呼叫（如戰鬥呼叫背包、升級呼叫廣播）。
  - 引進基於虛擬執行緒的輕量領域事件匯流排（`DomainEventBus`），各模組以發布-訂閱（Pub-Sub）模式非同步處理後續副作用，保證核心業務邏輯的純粹性與毫秒級響應。
- **多人地宮實例 Actor 容器 (Multi-Tenant Instance Actor)**：
  - 將地牢探索與戰鬥解構為獨立生命週期的 `DungeonInstanceActor`。
  - 每個探索隊伍擁有專屬的計算沙盒，迷霧矩陣與戰況狀態完全隔離；即便單一副本遭遇不可預期的運算異常，亦具備自動崩潰重啟與隔離能力，絕不波及全伺服器。

### 2. 穩定性與併發防禦 (Zero-Trust Stability & Concurrency Guard)
- **虛擬執行緒 (Loom) 載體排程防禦 (Carrier Thread Exhaustion Defense)**：
  - 嚴格監控 `synchronized` 關鍵字導致的虛擬執行緒釘選（Pinning）問題，全面改用 JUC `ReentrantLock`、`ReentrantReadWriteLock` 與 `StampedLock`。
  - 對重度 I/O 操作與 CPU 密集型數值解算進行執行緒池隔離，防止底層 ForkJoinPool 載體耗盡。
- **架構看門狗 CI 門禁 (Automated ArchUnit & AST Gate)**：
  - 擴充 `ArchitectureGuardrailsTest`：在 CI 管線中自動化掃描 Clean Architecture 反向依賴、禁止直接讀取內部私有狀態、強行校驗 100% 資料驅動紅線，杜絕架構腐化。

### 3. 系統效能調優 (Performance & Extreme Throughput)
- **雙軌傳輸協定支援 (Protobuf / JSON Binary Pipeline)**：
  - 戰鬥與探索狀態廣播支援 Protobuf 二進位序列化，將高頻 WebSocket 訊息吞吐量提升 300%，頻寬開銷降低 60% 以上。
- **異步批次持久化強一致性保證**：
  - 固化 `AbstractAsyncBatchPersistenceService` 的讀寫鎖生命週期與 `flushImmediately` 度量計數契約，杜絕任何高併發場景下的停機資料遺失與競態。

### 4. 模組化與多語言動態擴充 (Modular Subpackages & Polyglot Scripting)
- **領域層 Service 包結構模組化收斂**：
  - 將 `com.example.htmlmud.domain.service` 下平鋪的 10+ 個服務拆解至獨立子包（`domain.world`、`domain.combat`、`domain.party`、`domain.progression`、`domain.economy`），對外僅開放 Facade 介面與 Output Ports。
- **GraalVM 多語言動態腳本引擎 (Polyglot Dynamic Scripting)**：
  - 引入 GraalVM 執行時期，支援使用 JavaScript 或 Python 編寫複雜的 NPC 劇情對話、地牢機關解謎與首領行為樹（Behavior Tree），支援伺服器不停機熱更載入（Hot-Reload）。
