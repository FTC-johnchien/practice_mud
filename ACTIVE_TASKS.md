# 🎯 ACTIVE_TASKS (當前迭代微任務看板)

> **模式規範**：遵循 Spec-Driven Development 與 AI Agentic Workflow 最佳實踐。  
> **單一焦點原則**：每次對話僅推進 **1 個 Micro-Task**，完成後進行測試驗證與手冊同步，並勾選為 `[x]`。  
> **長遠演進參照**：宏觀藍圖與全局待辦已收錄於 👉 [`docs/plans/FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md)。

---

## 📌 當前焦點里程碑：Milestone 2 — 核心雙軌實體大一統：Player 與 PartyMember 實體合一與單一真相源治理 (F1-01 / M1 / M4)

- **對應模組手冊**：
  - 後端探索：[`後端_M1_探索與世界模組_WORLD_AND_EXPLORATION.md`](./docs/manual/後端_M1_探索與世界模組_WORLD_AND_EXPLORATION.md)
  - 後端小隊：[`後端_M4_小隊生靈與陣法模組_PARTY_AND_FORMATION.md`](./docs/manual/後端_M4_小隊生靈與陣法模組_PARTY_AND_FORMATION.md)
  - 長遠藍圖：[`FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md) §22.1 F1-01
- **核心目標**：徹底消除多人架構殘留之「Player (MUD 空間代理) vs PartyMember (DRPG 角色)」雙軌並立與兩套背包、兩套裝備痛點。以小隊為唯一真實資料源（Single Source of Truth），廢止脆弱的 `CharacterSyncService` 雙向拷貝與超時等待，實現純單機極致純粹之角色與行囊體系。

### 📋 微任務清單 (Micro-Tasks)

- [x] **Task 1: 行囊大一統 (Unified Party Inventory)**
  - 消除「個人隨身行囊 12 格」與「小隊公共倉庫 40 格」割裂。
  - `GetCommand`、`DropCommand`、`InventoryCommand`、`UseCommand` 全面直接操作 `party.getInventory()`。
  - 廢除強制性 `stash/withdraw` 轉存，`Player.getInventory()` 委派/同步小隊行囊視圖。
- [x] **Task 2: 裝備與核心屬性大一統 (Unified Equipment & Stats)**
  - `Player` 之 `LivingStats.equipment` 與 `party.getLeader().getEquipment()` 保持一致引用或直接委派。
  - 徹底廢止 `syncEquipmentFromPartyToPlayer` 內的 `CompletableFuture` 5 秒超時等待與 Mailbox 轉發。
  - `EquipCommand` 直接穿戴至小隊成員槽位，即時生效。
- [ ] **Task 3: 廢止 CharacterSyncService 淺拷貝與戰鬥結果雙向同步 (Decommission Sync Copies)**
  - `DrpgRewardService` 直接將經驗值、升級、修為與戰利品入帳至 `Party` 與 `PartyMember` 實體，消除 `syncFromPartyToPlayer` 逆向覆蓋。
  - 簡化 `SkillBridgeService` 與 `BattleOutcomeApplier`，徹底終結「兩本假帳」。
- [ ] **Task 4: 測試驅動驗證與舊測試重構 (Test-Driven Verification)**
  - 盤查並重構 `CharacterSyncServiceTest`、`CanonicalEquipmentProjectionTest`、`OutcomeBasedCharacterSyncTest`、`DualTrackPotentialPointsTest` 等測試，對齊單一真相源規範。
  - 執行全量 `.\test.ps1`，確保全專案所有測試 100% 綠燈全數通過。
- [ ] **Task 5: 模組手冊同步與 Git Commit 提交流程 (Documentation & Commit)**
  - 同步更新 `後端_M1` 與 `後端_M4` 手冊，記錄實體大一統架構圖與單一真相源流向。
  - 由使用者審查後執行繁體中文 Git Commit。

---

## 🔮 後續備選里程碑 (Upcoming Milestones)

### 📌 Milestone 3 — 後端領域層 Package 結構按模組收斂 (M1/M4/M5/M6)
- **目標**：在大一統實體穩定後，解決 `com.example.htmlmud.domain.service` 平鋪問題，拆分為 `domain.world`、`domain.party`、`domain.progression`、`domain.economy`。

### 📌 Milestone 4 — 前端歷史樣式巨石除役 (FE-6)
- **目標**：完成 `static/css/style.css`（122 KB）歷史巨石安全移除，徹底切換至 `style-01` ~ `style-07` 七層樣式流。

---

## 🛡️ 三位一體之「完成定義 (Definition of Done, DoD)」

任何一個 Micro-Task 標記為 `[x]` 前，必須無條件通過以下檢驗：
1. **代碼實作**：100% 遵循「專案第一紅線（資料驅動、零硬編碼、深拷貝完整性）」。
2. **測試驗證**：新增機制必須具備對應測試，且 `.\test.ps1` 全量執行 100% 綠燈全數通過。
3. **文檔同步**：若涉及 API、DTO、配置結構變更，同步更新對應的 `docs/manual/` 模組手冊。
4. **繁體中文**：所有註解、日誌、Commit 訊息一律使用標準繁體中文。
