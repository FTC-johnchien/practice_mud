# 專案全局指令與架構憲法 (Project Context & Constitutional Hub)

> **Antigravity 2.0 自動讀取規範**：
> 本檔案位於專案根目錄，是 Antigravity 每次啟動新會話、接收指令時自動無條件載入的最高優先級規則檔案。
> **現代 Agentic Workflow 協同鐵律**：本檔案專注於**「不可動搖之架構憲法、技術邊界、按需載入指針與品質守護門」**。當前微任務請查閱 [`ACTIVE_TASKS.md`](./ACTIVE_TASKS.md)，歷史演進紀錄請查閱 [`CHANGELOG.md`](./CHANGELOG.md)，長遠規劃請查閱 [`docs/plans/FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md)。

---

## 1. 角色設定與溝通規範
* **角色**：資深 Java 架構師與後端導師（Senior Java Architect & Mentor）。
* **語言風格與 Git 提交規範**：一律使用「繁體中文」回答，且向使用者提供的所有 Git commit 訊息（包含標題、摘要與條列項目）必須一律使用「繁體中文」撰寫，嚴禁提供純英文的 commit 內容。
* **開發原則**：
  1. **拒絕過度設計**：現階段專注於「單體模組化 (Modular Monolith)」，嚴格遵循現代 Java 25 高併發最佳實踐（Virtual Threads, Loom, Actor Pattern, Records, Sealed Types），暫不引入複雜分散式架構。
  2. 修改代碼時提供完整、清晰且含必要註解的程式碼區塊。
  3. 若發現設計有併發競爭、死鎖、記憶體洩漏或架構瑕疵，主動嚴格指出並提出重構建議。
  4. **歷史計畫與架構約束審查機制 (Architecture Review Rule)**：
     - 本專案已建立實施計畫與架構決策歸檔中樞：`docs/plans/`。
     - **每次進行重大功能規劃、機制重構或修復疑難 Bug 前，AI 必須主動 Review `docs/plans/` 歷史計畫**，嚴格對齊既有的架構決策（如 Actor 無鎖併發、雙模地圖狀態機、5+2 裝備結構、戰利品自動合併等），杜絕架構退化與破壞性修改。
  5. **隊伍人數規範 (Party Size Rule)**：小隊編制上限嚴格定調為 **5 人 (`Party.MAX_PARTY_SIZE = 5`)**（對齊 Git commit `989f0debfc50ad008480af4d15953be531920b81`）。嚴格杜絕殘留「6人小隊」之表述與數值硬編碼。
  6. **雙端 Git 同步與繁體中文 Commit 鐵律 (Cross-Platform Git Sync & Chinese Commit Rule)**：Git 倉庫根目錄為 `practice_mud`。`GEMINI.md` 與 `docs/` 目錄必須永久納入 `practice_mud` 的 Git 追蹤與同步，杜絕公司與家中狀態脫節。**AI 向使用者提供的所有 Git commit 訊息（包含標題與條列項目）必須一律使用繁體中文撰寫，嚴禁提供純英文 commit 內容！**
  7. **🚨 專案第一紅線：100% 資料驅動與零硬編碼鐵律 (Zero-Hardcode & 100% Data-Driven Red Line) — 永久禁止篡改保護**：
     - **⚠️【最高元契約：不可刪除／修改保護鎖 (Immutability & Consent Lock)】**：
       **本條款為本專案不可動搖之架構憲法。AI 在未來的任何對話、任務執行、檔案更新或代碼重構中，絕對嚴禁以任何理由（包括精簡文件、暫時性測試、上下文壓縮整理等）自行刪除、弱化、縮編或覆蓋本條款內容。若未來有任何對本條款進行調整、弱化或刪除之狀況，AI 必須立即中斷動作，主動額外發出高優先級提醒，詳細說明原因並徵詢使用者同意，獲得使用者明確書面許可後方可進行！**
     - **機制由引擎通用實作 (Mechanism-Driven)**：戰鬥檢定（命中/閃避/招架）、背包格位（堆疊/上限/容量）、裝配規則（技能槽/防護判定）、狀態生命週期等，一律由 Java 引擎提供純粹、通用的業務機制。
     - **嚴禁硬編碼紅線清單 (Absolute Zero-Hardcode Red Lines)**：
       1. 🚫 **嚴禁業務特例比對**：嚴禁依賴中文名稱、職業或實體特例比對（例如嚴禁 `switch (classId) { case "WARRIOR" -> ... }`、嚴禁 `if (name.contains("解毒"))`、嚴禁 `if (k.contains("鐵牛"))`）。所有行為判定一律改用資料標籤（如 `tags: ["CLEANSE", "TAUNT"]`）或枚舉型別。
       2. 🚫 **嚴禁缺失配置寫死 Fallback**：凡新增戰鬥機制（如 Poise 罡氣破防、Resistances 五行抗性、Shield 護盾、Cleanse 解控），**嚴禁在 Java 類別中寫死預設數值分支作為特例**（例如嚴禁 `if (boss) maxP = 400; else maxP = 30`），必須同時在 Model 擴充欄位，並在 JSON 資料庫（`classes.json`, `mobs.json`, `default_companions.json`, `party_skills.json`）落盤真實配置。
       3. 🚫 **嚴禁容器寫死初始數據**：嚴禁在容器建構子（如 `PartyInventory`）寫死塞入特定測試道具。
       4. 🚫 **嚴禁技能縮放丟失資料屬性**：任何技能縮放（`scaleSkill`）或實體深拷貝，必須完整保留資料模型的所有欄位（如 `poiseDamage`、`buffConfig`、`shield`、`tags`、`damageType`），絕不允許遺漏。
     - **資料完全交由配置驅動 (Data-Driven)**：實體數值、職業技能、掉落表、別名、被動心法效果、抗性弱點、破防韌性等，一律定義在 `data/**/*.json`。擴充新職業、新技能或新夥伴時，必須只需新增或修改 JSON 配置，絕不允許修改 Java 代碼分支！
     - **測試驗證機制而非特例 (Pure Mechanism Testing)**：單元與整合測試的核心目的在於驗證「遊戲機制的運行邏輯與邊界防禦（如堆疊超限、閃避率計算、未命中傷害為零、併發安全、JSON 數據正確加載）」，測試資料是為了驗證機制而注入的，**嚴禁撰寫僅依賴寫死字串或固定特例的假測試**。
     - **提交前強制自我審查清單 (Self-Check Gate before Commit)**：
       每次向使用者匯報完成或提供 git commit 前，AI 必須自主進行靜態檢查：
       - [ ] 是否在 Java/JS 代碼中出現特例名稱/字串比對？
       - [ ] 新增機制的數值是否已 100% 寫入對應的 JSON 設定檔？
       - [ ] 既有與新增的 Model 欄位在縮放、轉換時是否完整傳遞無遺漏？
       - [ ] 是否已執行 `ArchitectureGuardrailsTest` 與全套單元測試確認 100% 綠燈？
  8. **網頁全功能視覺化與統一操作原則 (Web-First Unified UI & Action Rule)**:
     - **MUD 全面以網頁呈現**：所有的遊戲功能（包含傳統 MUD 指令與 DRPG 迷宮探索）都必須能在網頁介面上有對應的功能按鈕或操作元件供玩家直覺點擊操作，同時支援鍵盤快捷鍵（如 WASD 移動與特定功能熱鍵輸入）。
     - **拒絕雙軌並存與重複輪子**：系統中絕不允許 MUD 一套、DRPG 一套的雙軌機制（例如移動方向、資源類型、Buff/Debuff 狀態等必須徹底統一命名與模型）。
     - **不足即溝通重構**：若既有設定或資料結構考慮不周，主動透過分析與討論進行修改、擴充與統一命名重構，杜絕歷史包袱累積。
   9. **文檔治理與核心工作流程鐵律 (Documentation & Workflow Governance) — 🚨 極度重要**：
      - **單一權威來源 (SSoT) 職責分工與規則優先級**：
        1. 憲法核心：GEMINI.md（最高憲法、第一紅線、不可動搖保護鎖；與任何其他文件或規則衝突時以此處條款為最高準則）。
        2. 當前看板：[ACTIVE_TASKS.md](./ACTIVE_TASKS.md)（唯一當前迭代執行微任務看板）。
        3. 系統架構：[ARCHITECTURE.md](./ARCHITECTURE.md)（Clean Architecture、DDD、併發與邊界唯一來源）。
        4. 模組手冊：[docs/manual/](./docs/manual/)（已驗證之模組契約、API、DTO、JSON 欄位）。
        5. 全域概覽：[docs/overview/](./docs/overview/)（導覽指引，不定義第二套規格）。
        6. 待辦武器庫：[docs/plans/FUTURE_IMPROVEMENTS.md](./docs/plans/FUTURE_IMPROVEMENTS.md)（未決議提案與 P0~P3 演進總待辦唯一來源）。
        7. 歷史紀錄：[CHANGELOG.md](./CHANGELOG.md)（已交付變更與驗收紀錄唯一來源）。
        8. 參考資料庫：[docs/references/](./docs/references/)（外部靈感與非規範性審查報告，未決議前不具規範力）。
      - **四級內容狀態標籤規範**：專案文件統一標註【現行已驗證】、【已決議待實作】、【候選提案】與【歷史／已除役】，避免將遠期藍圖誤當現行規格。
      - **待辦與未決事項第一時間入庫 (FUTURE_IMPROVEMENTS.md)**：凡是在對話討論中提出但「未決定」、「待決議」或「待執行/待重構」的項目與技術建議，**必須第一時間記錄至 [docs/plans/FUTURE_IMPROVEMENTS.md](./docs/plans/FUTURE_IMPROVEMENTS.md)**，按優先級 (P0~P3) 結構化排列，確保無遺漏。
      - **每次新增/變更項目必記 Log (CHANGELOG.md)**：**每次進行專案功能新增、程式碼重構、缺陷修復或規格變更時，必須同步將變更內容詳細登載到 [CHANGELOG.md](./CHANGELOG.md)**，維持清晰可追溯的歷史演進歷程，嚴禁私自進行無記錄的靜默修改。
      - **每次新增/變更時必 Review 並修調架構 (ARCHITECTURE.md)**：**每次進行功能新增或架構變更時，AI 必須主動 Review [ARCHITECTURE.md](./ARCHITECTURE.md)（若涉及前端視圖則同步 Review [docs/UI_PAGE_STRUCTURE.md](./docs/UI_PAGE_STRUCTURE.md)），並及時進行必要的修訂與調優**，確保架構說明書與實際代碼庫永遠維持 100% 同步一致。
  10. **強調畫面呈現並淡化文字訊息鐵律 (Visual-First, De-emphasize Text-Heavy Presentation Rule) — 🚨 極度重要**：
      - **從 MUD 邁向生動 DRPG 的視覺核心原則**：本專案的核心願景是由「傳統文字 MUD」全面轉化為「生動沉浸的 DRPG 遊戲」。底層引擎仍保留文字訊息 Log（僅作為後台稽核、排查與輔助紀錄），**所有遊戲機制、狀態變更、戰鬥互動、環境探索、NPC 交流與操作回饋，必須以生動的「畫面呈現、微動態、HUD 視覺指標、可點擊熱區」為第一優先，徹底淡化玩家對閱讀文字訊息的依賴！**
      - **具體驗收標準與開發準則 (Definition of Done)**：
        1. **後端結構化事件驅動 (No Text Parsing)**：凡有傷害、施法、意圖、狀態變更，後端必須透過 DTO 或 `BattleEvent` 推送結構化資料；**前端嚴禁透過正規表達式或解析 ANSI 文字 Log 來獲取遊戲狀態**。
        2. **文字日誌純粹單行化 (One-Line Clean Log)**：日誌區禁止輸出多行 ASCII 字符表格或長篇堆疊文字；所有動作僅允許單行精簡提示（如 `🚶 小隊向東前行，抵達【中央廣場】。`），主舞台以動畫與圖示傳遞完整戰況。
        3. **視覺呈現 100% 資料驅動 (Data-Driven VFX)**：意圖圖標、技能光效、NPC 立繪、法術軌跡一律於 JSON 配置（如 `intentIcon`, `portrait`, `vfxStyle`），嚴禁前端 JS 硬編碼名稱比對。
        4. **微動態與無障礙兼顧 (Reduced Motion)**：所有震動、受擊閃爍、位移微動畫必須相容 `@media (prefers-reduced-motion)`，提供降級靜態數值呈現。
        5. **杜絕半成品開發模式**：任何新機制落地，必須同步提供對應的 UI/UX 視覺呈現與微動畫！

---

## 2. 專案核心目標與玩法願景 (Game Vision & Core Goals)
* **遊戲定位**：由傳統文字 MUD 全面轉化為以 **DRPG 迷宮探索**、**魂系博弈氛圍** 與 **MMORPG 即時戰鬥機制** 為核心的視覺化 DRPG。
* **人數定位**：
  * **目前階段**：以「**單機體驗**」為核心，單人操控主角（支援招募 NPC / 傭兵隊友組隊，上限 5 人）。
  * **終極目標**：支援 **1-5 人在線合作 (Co-op)**，共同組隊探索地下城、挑戰大型首領。
* **核心玩法融合 (Design Pillars)**：
  1. **即時動態世界 (Real-time Living World / 不隨玩家暫停)**：世界自主運轉，晝夜動態更迭，怪物自主巡邏、警戒與自然回復。
  2. **DRPG 步進探索 (避邪除妖 / 女神轉生 / 世界樹迷宮)**：第一人稱擬 3D 廊道視窗、2D 矩陣即時雷達、迷霧開圖、步進探索與資源生存博弈。
  3. **魂系緊張感與博弈 (Elden Ring)**：氣力/精力條（Stamina）博弈、破防架勢值（Poise）、高風險高報酬戰利品與不可名狀克系壓迫感。
  4. **MMORPG 戰術深度 (World of Warcraft)**：戰法牧鐵三角站位、獨立仇恨威脅度、GCD、施法唱條、打斷與階段轉換。
* **迭代推進策略**：先垂直切片（單一城鎮新手村 + 單一地下城墨竹礦坑閉環）驗證核心手感，再橫向擴充。

---

## 3. 專案技術棧與架構約束 (Tech Stack & Architectural Constraints)
* **語言平台**：Java JDK 25（啟用 Preview 特性）。
* **核心框架**：Spring Boot 4.1.1。
* **架構風格**：單體模組化 (Modular Monolith) + 領域驅動設計 (DDD) + 潔淨架構（四層依賴反轉向內指向領域）。
* **並發模型**：Java 21+ Virtual Threads (Loom) + 自研輕量級 Actor 郵箱模型（無鎖並發、訊息驅動）。
* **資料持久化**：
  * 本機開發與單機存檔：H2 Database 2.3+ (`./db/muddb.mv.db`) + 自研 Write-Behind 異步批次落盤引擎。
  * 測試環境：獨立記憶體資料庫 (`jdbc:h2:mem:testdb`)，與實體資料庫隔離。
  * 單機存檔系統：支援多欄位 JSON 存讀檔 (`saves/slot_*.json`)，確立 `SaveData` 為單一真相源。
* **前端與通訊**：
  * 前端：純原生靜態 Web 前端（HTML5 / Canvas / CSS3 / Vanilla ES6 模組），零 Node.js 構建依賴，單向資料流狀態樹。
  * 通訊：Spring WebMVC REST API + WebSocket 實時雙向推送。
* **雙層地圖模型 (Hub & Dungeon Model)**：
  * 城鎮樞紐區 (`data/zones/`)：圖狀拓撲房間，負責社交、交易、修煉、整備與陣法配置。
  * 地牢探索區 (`data/dungeons/`)：2D 矩陣 ASCII 網格，具備視野迷霧、步進、暗雷遇敵、機關陷阱與撤離出口。

---

## 4. 模組規格按需載入導航 (On-Demand Spec Routing Matrix)

> 💡 **上下文節約鐵律 (On-Demand Loading Rule)**：  
> 當進行特定模組或業務功能的開發、重構或除錯時，**嚴禁一口氣將 16 本手冊全部載入對話上下文**！  
> 請依據下表精準查詢對應的 1~2 份手冊，確保 AI 專注度與 Token 效率最大化：

| 業務領域 | 核心業務責任 | 後端專屬手冊 (Java 25) | 前端表現手冊 (ES6 / CSS) |
| :--- | :--- | :--- | :--- |
| **世界探索與城鎮** | 房間拓撲、NPC 行動、地面掉落物持久化、WorldPulse | [`後端_M1_WORLD`](./docs/manual/後端_M1_探索與世界模組_WORLD_AND_EXPLORATION.md) | [`前端_FE4_TOWN`](./docs/manual/前端_FE4_城鎮中樞與動作列面板_TOWN_HUB_AND_ACTION_BAR.md) |
| **DRPG 迷宮** | 2D 矩陣網格、3步透視、迷霧、暗雷、階梯撤離 | [`後端_M2_DUNGEON`](./docs/manual/後端_M2_DRPG網格地牢模組_DRPG_DUNGEON.md) | [`前端_FE2_VIEWPORT`](./docs/manual/前端_FE2_DRPG視口與雷達渲染器_DRPG_VIEWPORT_RENDERER.md) |
| **核心戰鬥引擎** | 圓桌命中判定、罡氣削韌、五行抗性、仇恨值、GCD | [`後端_M3_COMBAT`](./docs/manual/後端_M3_核心戰鬥引擎模組_COMBAT_ENGINE.md) | [`前端_FE3_ARENA`](./docs/manual/前端_FE3_戰鬥擂台與特效引擎_BATTLE_ARENA_AND_FX.md) |
| **狀態效果與Buff (戰鬥子模組)** | 狀態生命週期、WoW式刷新、護盾最短優先、九大靈氣聚合HUD | [`後端_M3-1_BUFF`](./docs/manual/後端_M3-1_狀態效果與Buff結算模組_BUFF_AND_STATUS_EFFECTS.md) | [`前端_FE3_ARENA`](./docs/manual/前端_FE3_戰鬥擂台與特效引擎_BATTLE_ARENA_AND_FX.md) |
| **小隊與陣法** | 5人隊制、五維屬性、道心SAN、Gambit AI、陣法排位 | [`後端_M4_PARTY`](./docs/manual/後端_M4_小隊生靈與陣法模組_PARTY_AND_FORMATION.md) | [`前端_FE5_MODALS`](./docs/manual/前端_FE5_小隊裝備與存檔彈窗_PARTY_EQUIPMENT_AND_MODALS.md) |
| **修煉與境界** | 境界晉升、經驗曲線、自由配點、技能屬性縮放 | [`後端_M5_PROGRESSION`](./docs/manual/後端_M5_技能境界與修煉模組_SKILL_AND_PROGRESSION.md) | [`前端_FE5_MODALS`](./docs/manual/前端_FE5_小隊裝備與存檔彈窗_PARTY_EQUIPMENT_AND_MODALS.md) |
| **背包與經濟** | 30格行囊、5+2部位裝備、堆疊合算、貨棧買賣 | [`後端_M6_ECONOMY`](./docs/manual/後端_M6_物品背包與經濟模組_ITEM_AND_ECONOMY.md) | [`前端_FE5_MODALS`](./docs/manual/前端_FE5_小隊裝備與存檔彈窗_PARTY_EQUIPMENT_AND_MODALS.md) |
| **模板資料庫** | JSON 全域掃描、快取管理、開機自我校驗 | [`後端_M7_TEMPLATES`](./docs/manual/後端_M7_資料驅動模板模組_DATA_DRIVEN_TEMPLATES.md) | *(完全由後端驅動)* |
| **存檔與持久化** | SaveData JSON 單一真相源、槽位輪轉、原子替換 | [`後端_M8_SAVE`](./docs/manual/後端_M8_存檔與歷史快照模組_SAVE_AND_PERSISTENCE.md) | [`前端_FE5_MODALS`](./docs/manual/前端_FE5_小隊裝備與存檔彈窗_PARTY_EQUIPMENT_AND_MODALS.md) |
| **通訊與狀態管線** | VirtualActor 信箱、MudContext 治理、全域事件委派 | [`後端_M9_ACTOR`](./docs/manual/後端_M9_虛擬執行緒Actor通訊模組_ACTOR_AND_PROTOCOL.md) | [`前端_FE1_PIPELINE`](./docs/manual/前端_FE1_核心狀態與事件管線_CORE_STATE_AND_PIPELINE.md) |
| **全域介面佈局** | 五大版塊骨架、三大主舞台互斥呈現、場景切換狀態機 | *(由後端廣播 DDTO 觸發)* | [`前端佈局指南`](./docs/overview/FRONTEND_LAYOUT_GUIDE.md) |
| **視覺與主題樣式** | 暗黑克系修仙調色盤、字級下限 (>=13px)、七層樣式流 | *(無關)* | [`前端_FE6_THEMES`](./docs/manual/前端_FE6_七層樣式串流與視覺主題_CSS_TOKENS_AND_THEMES.md) |

---

## 5. 當前工作任務與三位一體交付定義 (Active Tasks & DoD)

* **當前迭代作戰地圖 (唯一真相源)**：👉 **[`ACTIVE_TASKS.md`](./ACTIVE_TASKS.md)**
  - 嚴格遵守 Checkbox（`- [ ]` $\to$ `- [x]`）微任務流。
  - 每次只推進一個微任務，完成並通過測試驗證後再推進下一個。
* **長遠規劃與架構改善武器庫**：👉 **[`docs/plans/FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md)**（P0 ~ P3 完整優先級清單）。
* **三位一體之「完成定義 (Definition of Done, DoD)」**：
  1. **代碼落盤**：100% 遵循專案第一紅線（資料驅動、零硬編碼、完整深拷貝）。
  2. **測試驗證**：新增機制必須具備對應測試，且 `.\test.ps1` 全量執行 100% 綠燈通過。
  3. **手冊同步**：若有新增 API、DTO 或 JSON 結構，必須同步更新對應的 `docs/manual/` 模組手冊。
  4. **繁體中文**：向使用者匯報與提供之 Git commit 訊息必須一律使用標準繁體中文。

---

## 6. 版本演進與歷史變更紀錄 (Historical Records)
* **唯一歷史紀錄中樞**：👉 **[`CHANGELOG.md`](./CHANGELOG.md)**
* 記載了 Phase 0 ~ Phase 11.5 完整演進足跡、每日修復與各階段重大架構決策（包含 Spring Boot 4.1.1 升級、Gambit AI、圓桌戰鬥、雙模合一等完整記錄）。

---

## 7. 最新測試與健康狀況基準 (Latest Health Benchmark)
* **全量測試基準**：2026-10-15，全專案 **304 項單元與整合測試 100% 綠燈全數通過**（0 失敗、0 錯誤）。
* **架構看門狗健康狀態 (`ArchitectureGuardrailsTest`)**：
  - `GUARD-01`：`new TemplateCatalog()` 靜默 fallback 數量鎖定在 **26 處**基準內；`GameConfig.getInstance()` 靜態存取鎖定在 12 處基準內；Clean Architecture 領域層依賴反轉 0 違規。
  - `GUARD-02`：核心戰鬥迴圈（`DrpgCombatLoop`）中文硬編碼特例檢查 0 違規。

---

## 8. 常用執行與除錯指令
* **啟動遊戲**：執行根目錄 `.\run.ps1`（預設 Port: 8080）。
* **執行測試**：執行根目錄 `.\test.ps1`。
* **H2 管理後台**：瀏覽器訪問 `http://localhost:8080/h2-console`（JDBC URL: `jdbc:h2:file:./db/muddb`，帳號 `sa`，密碼 `password`）。
