# 🏛️ 高階架構與核心準則 (Architecture Summary & Rules)

> **閱讀時間**：約 4 分鐘  
> 本文件提煉本專案的分層架構設計、核心職責邊界，以及不可篡改之「專案第一紅線」。

---

## 🏗️ 一、潔淨架構與一級業務套件結構 (Clean Architecture & Package Layout)

本專案貫徹 Clean Architecture（潔淨架構）與模組化單體（Modular Monolith）的核心思想：**依賴永遠只能向內指向領域核心**。

後端程式碼已收斂為【現行已驗證】之 9 大一級業務模組套件與過渡/支援套件（`com.example.htmlmud.*`）：

```text
src/main/java/com/example/htmlmud/
├── town/           # M1: 營地中樞與功能設施 (TownHubService, CognitiveProxyService, TownHubDto)
├── dungeon/        # M2: DRPG 網格地牢 (DungeonManager, DungeonFloor, DungeonNavigator, DungeonTile)
├── combat/         # M3/M3.1: 戰鬥引擎與狀態 (CombatRules, DrpgCombatLoop, DrpgBattleService, ComboResolver, BuffSettlementService)
├── party/          # M4: 小隊、生靈與陣法 (PartyService, Party, PartyMember, FormationEngine, GambitRule)
├── progression/    # M5: 成長與技能樹 (XpProgressionService, DrpgSkillRules, SkillDefinition)
├── item/           # M6: 物品與經濟 (UnifiedPartyEconomyService, ItemFactory, ItemInstance, EquipmentSlot)
├── template/       # M7: 資料驅動模板與校驗 (TemplateCatalog, TemplateRepository, TemplateReader, DataIntegrityValidator)
├── save/           # M8: 存檔持久化與遷移 (SaveGameService, SaveData, SaveSlotDto, SaveMigrationPipeline)
├── pipeline/       # M9: 狀態與通訊管線 (CommandDispatcher, GameStateBroadcastService, MudWebSocketHandler, SessionRegistry)
└── (過渡/支援層)    # domain, application, infra, config, protocol 等通用基底與歷史相容層
```

```
┌─────────────────────────────────────────────────────────────┐
│ 4. 框架與外部設施層 (Frameworks & Drivers)                   │
│    • Spring Boot 4.1.1, WebSocket, Jackson, H2 JPA (除役中) │
└──────────────────────────────┬──────────────────────────────┘
                               │ (實作 / 注入)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. 介面適配器層 (Interface Adapters)                         │
│    • MudWebSocketHandler, GameStateBroadcastService         │
└──────────────────────────────┬──────────────────────────────┘
                               │ (呼叫)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. 應用業務編排層 (Application & Service Layer)              │
│    • CommandDispatcher, TownHubService, DrpgBattleService   │
└──────────────────────────────┬──────────────────────────────┘
                               │ (依賴)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 1. 核心領域實體與純規則層 (Domain Core - Pure Java)           │
│    • CombatRules, FormationEngine, BuffSettlementService    │
│    • TemplateReader (埠), Buffable (契約)                   │
│    • 絕無任何 Spring / 資料庫依賴，純乾淨業務邏輯            │
└─────────────────────────────────────────────────────────────┘
```

1. **核心領域層 (`domain`, `combat`, `party` 等純業務核心)**：
   - 包含實體（`Player`, `Party`, `GameItem`）與純函式規則（`CombatRules`, `FormationEngine`）。
   - 嚴禁依賴 Spring、資料庫實體或外部傳輸協定，確保單元測試毫秒級執行。
2. **應用層與服務編排 (`town`, `dungeon`, `progression`, `pipeline`)**：
   - 負責玩家輸入指令的調度（`CommandDispatcher`）、事件監聽解算與業務狀態機推進。
3. **介面適配器與基礎設施層 (`template`, `save`, `infra`)**：
   - 負責將內部領域物件序列化為前端專用 DTO（`DrpgStateDto`），實作 `TemplateReader` 等輸出埠與檔案式原子存檔。

---

## 🚨 二、不可逾越之「專案第一紅線」 (The Golden Redlines)

本專案於系統級規範中確立了**最高元契約**，任何重構與功能新增皆必須 100% 遵守：

### 0. 純單機 85/15 刷寶黃金權重與三大層級遞進治理鐵律
- **85/15 黃金權重原則**：
  - 本專案定位為**「純單機第一人稱地牢探索與爽快砍殺刷寶 RPG (Single-Player Hack-and-Slash DRPG)」**。
  - **85% 核心體驗**：半即時 500ms ATB 砍殺、罡氣削韌、反制打斷、詞綴刷寶、5人小隊戰術構築。
  - **15% 背景世界觀**：克蘇魯卵鞘、工程師基岩、特工晶片覺醒，所有劇情與敘事**一律為非侵入式背景文字**，絕不可打斷戰鬥節奏或強制彈窗阻斷操作。
- **三大層級遞進治理與使用者審批鐵律**：
  1. `1_requirements` (L1) 發生任何異動，必須檢視現有 L2 文件是否涵蓋。若需新增或修改，AI 必須提供建議並由使用者決定是否新增/修改 L2 文件。
  2. `2_specifications` (L2) 發生任何異動，必須檢視現有 L3 文件是否涵蓋。若需新增或修改，AI 必須提供建議並由使用者決定是否新增/修改 L3 文件。
  3. `3_task_lists` (L3) 為 L2 規格的完整實作項目清單。

### 1. 100% 資料驅動與零硬編碼鐵律 (Zero Hardcoding)
- **嚴禁使用中文名稱做業務特例比對**：
  - ❌ 禁止：`if (item.getName().contains("甲"))` 或 `if ("boss_song_tianheng".equals(mobId))`。
  - ✅ 正確：全面依賴資料庫/JSON 枚舉與標籤，如 `item.getEquipSlot() == EquipmentSlot.BODY`，或透過 `quests.json` 資料驅動查詢。
- **所有遊戲數值完全由 JSON 驅動**：怪物數值、物品屬性、技能威力、地牢地塊全部由 JSON 設定檔定義，Java 程式碼僅負責通用公式解算。

### 2. 深拷貝不可遺漏屬性鐵律 (Complete Deep-Copy)
- 在任何進行實體快照、存檔保存或屬性比對之處（如 `LivingStats.deepCopy()`），**嚴禁遺漏任何成員屬性**（包含抗性 `resistances`、已學技能 `learnedSkills`、戰鬥資源 `combatResources` 等）。
- 任何遺漏均視為破壞系統可逆性與快照完整性的重大架構事故。

### 3. 單機存檔單一真相源 (SaveData JSON as Single Truth)
- 單機運行模式下，`saves/autosave.json` 與 `saves/slot_*.json` 為唯一單一真相源。
- 歷史 H2 JPA 實體（`CharacterEntity.inventory` 等）已全數標記為 `@Deprecated`，業務層一律透過 `SaveGameService` 操作。

### 4. 繁體中文 Commit 規範
- 所有 Git commit 訊息必須使用**繁體中文**，並嚴格遵循 Conventional Commits 格式（如 `feat(...)`、`refactor(...)`），禁止純英文提交。

---

## 🛡️ 三、架構看門狗測試 (Architecture Guardrails)

專案配備了強大的架構看門狗測試（[`ArchitectureGuardrailsTest.java`](../../src/test/java/com/example/htmlmud/domain/validation/ArchitectureGuardrailsTest.java)），每次執行 `.\test.ps1` 都會自動執行靜態代碼稽核：
1. **靜態單例封殺**：全面禁止生產代碼直接調用 `GameConfig.getInstance()`，強制由 Spring 建構子依賴注入。
2. **空 Catalog 棘輪監控**：即時監控 `new TemplateCatalog()` 的呼叫次數，門檻已自 35 處嚴格收斂至 **0 處**（全域已完成依賴注入改造），任何新增的靜默 fallback 都會直接導致編譯測試失敗！
3. **深拷貝完整性檢驗**：反射比對原始物件與深拷貝物件的所有欄位，確保 100% 完美克隆。

---

## 🎯 四、純單機單體架構深度演進規劃 (Single-Player Monolith Roadmap)

本專案專注於**「純單機單體 (Single-Player Modular Monolith)」**，圍繞單人小隊沉浸體驗進行深度演進，以極簡、高效與靜態資料驅動為核心架構：

### 1. 單機事件與沉浸探索 (Single-Player Exploration & Dynamic World)
- **輕量領域事件 (Domain Events)**：
  - 核心戰鬥結算與角色成長引入以虛擬執行緒為基礎的本機輕量事件：狀態變更由 `EntityDamagedEvent`、`ItemAcquiredEvent`、`LevelAdvancedEvent` 事件序列所驅動，具備 100% 戰況重播與本機稽核能力。
- **DRPG 迷宮步進探索手感**：
  - 2D 矩陣即時雷達、迷霧開圖、暗雷隨機遭遇、陷阱機關解謎、階梯撤離與地牢生存博弈。

### 2. 穩定性與單機存檔安全 (Local Save Resilience & Guardrails)
- **檔案式存檔可靠性與壞檔防禦**：
  - 以 `SaveData` JSON 為唯一單一真相源，多槽位原子替換、自動 `.bak` 備份與 `schemaVersion` 平滑遷移管線（v0 $\to$ v1 $\to$ v2 $\to$ v3）。
- **看門狗棘輪全面清零 (Zero-Tolerance Ratchet Gate)**：
  - 領域層已完成重構，`new TemplateCatalog()` 私建目錄已全面清零，消除代碼庫中任何潛在的靜默 fallback。

### 3. 本機極速效能與單體無鎖並發 (Local High Performance)
- **單機虛擬執行緒 Actor 模型**：
  - 延續基於 Java 25 Virtual Threads 的輕量級 Actor 信箱模型，本機 WebSocket 採用簡潔標準的 JSON 傳輸，確保 500ms ATB 戰鬥與探索毫秒級響應。
- **異步批次持久化強一致性保證**：
  - 透過 `flushLock` 互斥鎖與 `flushedCount >= acceptedCount` 完成度量契約，確保在任何伺服器重啟或高併發停機瞬間，零資料遺失、零競態崩潰。

### 4. 領域模型清晰化與架構邊界收斂 (Clean Architecture & Scope Convergence)
- **一級業務套件架構邊界收斂【現行已驗證】**：
  - 後端已全面收斂至 9 大核心業務套件（`town`、`dungeon`、`combat`、`party`、`progression`、`item`、`template`、`save`、`pipeline`），歷史 `domain.service` 已完成模組化邊界遷移，各套件內部職責內聚，對外僅依賴公開服務與 DTO 契約。
- **🛡️ 避免過度設計守則**：
  - 架構全面採用純粹的 Java 25 虛擬執行緒機制與靜態 JSON 資料驅動，杜絕引入跨服通訊（Redis/gRPC）、Protobuf 二進位串流與動態腳本熱載入引擎等複雜度。
