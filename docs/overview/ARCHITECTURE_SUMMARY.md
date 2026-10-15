# 🏛️ 高階架構與核心準則 (Architecture Summary & Rules)

> **閱讀時間**：約 4 分鐘  
> 本文件提煉本專案的分層架構設計、核心職責邊界，以及不可篡改之「專案第一紅線」。

---

## 🏗️ 一、潔淨架構四層依賴反轉 (Clean Architecture)

本專案嚴格貫徹 Clean Architecture（潔淨架構）與 Hexagonal Architecture（六角架構）的核心思想：**依賴永遠只能向內指向領域核心**。

```
┌─────────────────────────────────────────────────────────────┐
│ 4. 框架與外部設施層 (Frameworks & Drivers)                   │
│    • Spring Boot 4.1.1, WebSocket, Jackson, H2 JPA (棄用中) │
└──────────────────────────────┬──────────────────────────────┘
                               │ (實作 / 注入)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. 介面適配器層 (Interface Adapters)                         │
│    • MudWebSocketHandler, DrpgStateDto, TemplateRepository  │
└──────────────────────────────┬──────────────────────────────┘
                               │ (呼叫)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. 應用業務編排層 (Application Layer)                        │
│    • CommandDispatcher, LivingService, SaveGameService       │
└──────────────────────────────┬──────────────────────────────┘
                               │ (依賴)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 1. 核心領域實體與純規則層 (Domain Core - Pure Java)           │
│    • CombatRules, Poise, Resistance, LivingStats, Player    │
│    • TemplateReader (埠), Buffable (契約)                   │
│    • 絕無任何 Spring / 資料庫依賴，純乾淨業務邏輯            │
└─────────────────────────────────────────────────────────────┘
```

1. **核心領域層 (`domain`)**：
   - 包含實體（`Player`, `Party`, `GameItem`）與純函式規則（`CombatRules`, `FormationEngine`）。
   - 嚴禁依賴 Spring、資料庫實體或外部傳輸協定，確保單元測試毫秒級執行。
2. **應用層 (`application`)**：
   - 負責玩家輸入指令的調度（`CommandDispatcher`）、事件監聽解算（`MozhuMinesQuestListener`）。
3. **介面適配器與基礎設施層 (`adapter`, `infra`)**：
   - 負責將內部領域物件序列化為前端專用 DTO（`DrpgStateDto`），並實作 `TemplateReader` 等輸出埠。

---

## 🚨 二、不可逾越之「專案第一紅線」 (The Golden Redlines)

本專案於 `GEMINI.md` 與系統級規則中確立了**最高元契約**，任何重構與功能新增皆必須 100% 遵守：

### 1. 100% 資料驅動與零硬編碼鐵律 (Zero Hardcoding)
- **嚴禁使用中文名稱做業務特例比對**：
  - ❌ 禁止：`if (item.getName().contains("甲"))` 或 `if ("boss_song_tianheng".equals(mobId))`。
  - ✅ 正確：全面依賴資料庫/JSON 枚舉與標籤，如 `item.getEquipSlot() == EquipSlot.BODY`，或透過 `quests.json` 資料驅動查詢。
- **所有遊戲數值完全由 JSON 驅動**：怪物數值、物品屬性、技能威力、地牢地塊全部由 JSON 設定檔定義，Java 程式碼僅負責通用公式解算。

### 2. 深拷貝不可遺漏屬性鐵律 (Complete Deep-Copy)
- 在任何進行實體快照、存檔保存或屬性比對之處（如 `LivingStats.deepCopy()`），**嚴禁遺漏任何成員屬性**（包含抗性 `resistances`、已學技能 `learnedSkills`、戰鬥資源 `combatResources` 等）。
- 任何遺漏均視為破壞系統可逆性與快照完整性的重大架構事故。

### 3. 單機存檔單一真相源 (SaveData JSON as Single Truth)
- 單機運行模式下，`saves/slot_*.json` 為唯一單一真相源。
- 歷史 H2 JPA 實體（`CharacterEntity.inventory` 等）已全數標記為 `@Deprecated`，業務層一律透過 `SaveGameService` 操作。

### 4. 繁體中文 Commit 規範
- 所有 Git commit 訊息必須使用**繁體中文**，並嚴格遵循 Conventional Commits 格式（如 `feat(...)`、`refactor(...)`），禁止純英文提交。

---

## 🛡️ 三、架構看門狗測試 (Architecture Guardrails)

專案配備了強大的架構看門狗測試（[`ArchitectureGuardrailsTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/domain/validation/ArchitectureGuardrailsTest.java)），每次執行 `.\test.ps1` 都會自動執行靜態代碼稽核：
1. **靜態單例封殺**：全面禁止生產代碼直接調用 `GameConfig.getInstance()`，強制由 Spring 建構子依賴注入。
2. **空 Catalog 棘輪監控**：即時監控 `new TemplateCatalog()` 的呼叫次數，門檻已自 35 處嚴格收斂至 **26 處**，任何新增的靜默 fallback 都會直接導致編譯測試失敗！
3. **深拷貝完整性檢驗**：反射比對原始物件與深拷貝物件的所有欄位，確保 100% 完美克隆。

---

## 🚀 四、未來高階架構現代化演進規劃 (High-Level Architecture Roadmap)

為因應分散式高併發與多人在線合作的長線目標，規劃以下高階架構演化路徑：

### 1. 現代架構規劃 (Modern Architectural Patterns)
- **領域事件溯源 (Event Sourcing & Auditability)**：
  - 核心戰鬥結算與角色成長逐步引入輕量事件溯源：狀態變更皆由 `EntityDamagedEvent`、`ItemAcquiredEvent`、`LevelAdvancedEvent` 事件序列所驅動，具備 100% 戰況重播與作弊審計回溯能力。
- **多人地宮實例 Actor 隔離 (Multi-Tenant Instance Actor)**：
  - 由單機全域狀態升級為多實例容器，每個地下城樓層動態生成獨立的 `DungeonInstanceActor`，隊伍間運算完全隔離，單一副本崩潰不影響其他隊伍。

### 2. 穩定性與安全邊界防禦 (Zero-Trust Security & Boundary Guard)
- **WebSocket 票證驗證與速率限制 (Ticket Auth & Token Bucket Rate Limiting)**：
  - 登入握手時由 REST API 發放短期一次性簽名票證，連線後由內建令牌桶限制玩家輸入頻率（如指令上限 20 次/秒），徹底杜絕惡意腳本轟炸與連線劫持。
- **看門狗棘輪全面清零 (Zero-Tolerance Ratchet Gate)**：
  - 持續推進領域層重構，將當前 26 處 `new TemplateCatalog()` 的棘輪上限逐步清零，全面消除代碼庫中任何潛在的靜默 fallback。

### 3. 極限效能調優 (Extreme Performance & Throughput)
- **Protobuf / JSON 雙軌傳輸協定支援 (Binary Protocol Option)**：
  - 針對高頻戰鬥廣播（200ms Tick），支援客戶端協商切換為 Protobuf 二進位串流，封包體積降低 60%，網路序列化延遲壓低至 1ms 內。
- **異步批次持久化強一致性保證**：
  - 透過 `flushLock` 互斥鎖與 `flushedCount >= acceptedCount` 完成度量契約，確保在任何伺服器重啟或高併發停機瞬間，零資料遺失、零競態崩潰。

### 4. 領域模型可擴充性 (Extensibility & Clean Subpackages)
- **領域層 Service 包結構按業務模組拆分 (Domain Subpackages)**：
  - 將目前平鋪在 `domain.service` 的 10+ 個服務，按業務領域重構至子包（`domain.world`、`domain.combat`、`domain.party`、`domain.progression`、`domain.economy`），各子包對外僅暴露精簡的 Facade 介面。
- **動態腳本熱載入引擎 (GraalVM Polyglot Engine)**：
  - 引進 GraalVM JavaScript / Python 動態腳本，讓複雜的 NPC 對話樹、任務解謎與副本機關支援免重編譯熱載入。
