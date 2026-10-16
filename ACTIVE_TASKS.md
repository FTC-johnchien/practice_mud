# 🎯 ACTIVE_TASKS (當前迭代微任務看板)

> **模式規範**：遵循 Spec-Driven Development 與 AI Agentic Workflow 最佳實踐。  
> **單一焦點原則**：每次對話僅推進 **1 個 Micro-Task**，完成後進行測試驗證與手冊同步，並勾選為 `[x]`。  
> **長遠演進參照**：宏觀藍圖與全局待辦已收錄於 👉 [`docs/plans/FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md)。

---

## 📌 當前焦點里程碑：Milestone 3 — 後端領域層 Package 結構按模組收斂 (M1/M4/M5/M6)

- **對應模組手冊**：
  - 後端探索：[`後端_M1_探索與世界模組_WORLD_AND_EXPLORATION.md`](./docs/manual/後端_M1_探索與世界模組_WORLD_AND_EXPLORATION.md)
  - 後端小隊：[`後端_M4_小隊生靈與陣法模組_PARTY_AND_FORMATION.md`](./docs/manual/後端_M4_小隊生靈與陣法模組_PARTY_AND_FORMATION.md)
  - 後端技能：[`後端_M5_技能境界與修煉模組_SKILL_AND_PROGRESSION.md`](./docs/manual/後端_M5_技能境界與修煉模組_SKILL_AND_PROGRESSION.md)
  - 後端戰鬥：[`後端_M3_核心戰鬥引擎模組_COMBAT_ENGINE.md`](./docs/manual/後端_M3_核心戰鬥引擎模組_COMBAT_ENGINE.md)
  - 長遠藍圖：[`FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md) §22.2 M1~M6
- **核心目標**：解決歷史上 `com.example.htmlmud.domain.service` 平鋪 22 個服務類別之雜糅問題。遵循領域驅動設計（DDD）與模組化規格，將平鋪的 service 依照核心領域進行分拆與收斂，建立職責清晰的高內聚子 package，消除架構模糊地帶，保持全量 315 項測試 100% 綠燈。

### 📋 微任務清單 (Micro-Tasks)

- [x] **Task 1: 收斂 M1 探索與世界模組 (`domain.world`)**
  - 建立 `com.example.htmlmud.domain.world` 子 package。
  - 將 `WorldManager`、`RoomService`、`RoomMovementService`、`GameStateBroadcastService` 遷移至 `domain.world`。
  - 批次更新全專案 48 個關聯檔案的 import，並修復時間抖動脆弱斷言，全量 315 項測試 100% 綠燈。
- [ ] **Task 2: 收斂 M5 技能修為與經驗進程模組 (`domain.progression`)**
  - 建立 `com.example.htmlmud.domain.progression` 子 package。
  - 遷移 `XpProgressionService`、`XpService`、`SkillService`、`SkillCostService`、`SkillBridgeService` 及 `perform/` 招式邏輯。
  - 全域更新關聯引用，驗證相關測試綠燈。
- [ ] **Task 3: 收斂 M4 生靈與角色主體模組 (`domain.living`)**
  - 建立 `com.example.htmlmud.domain.living` 子 package。
  - 遷移 `LivingService`、`LivingStateService`、`PlayerService`、`MobService`、`CharacterSyncService`、`HealthStatusUtil`、`MessageUtil`。
  - 全域更新關聯引用，驗證相關測試綠燈。
- [ ] **Task 4: 收斂 M3 戰鬥判定與目標選擇模組 (`domain.combat`)**
  - 整合至既有 `com.example.htmlmud.domain.combat`。
  - 遷移 `CombatService`、`TargetSelector`、`BodyPartSelector`。
  - 整理模板適配器 `DrpgTemplateAdapter`、`MudTemplateAdapter`、`TemplateCatalog`。
  - 全域更新關聯引用，驗證相關測試綠燈。
- [ ] **Task 5: 全模組手冊對齊、全量 315 項測試回歸驗證與 Git Commit**
  - 同步更新各後端技術手冊中的路徑與架構圖。
  - 執行全量 `.\test.ps1`，確保 315 項測試 100% 綠燈，完成結構化 Git Commit。

---

## 🏆 已完成里程碑 (Completed Milestones)

- [x] **Milestone 2 — 核心雙軌實體大一統：Player 與 PartyMember 實體合一與單一真相源治理 (F1-01 / M1 / M4)**
  - Task 1: 行囊大一統 (Unified Party Inventory) — 消除個人 12 格限制與強制 stash 轉存，40 格小隊公共背包為唯一真相源。
  - Task 2: 裝備與核心屬性大一統 (Unified Equipment & Stats) — 徹底除役 5 秒 CompletableFuture 超時阻塞，達成 0ms 零延遲鏡像投影。
  - Task 3: 廢止 CharacterSyncService 偽 Outcome 拷貝 — 同步直接深度鏡像，修復線上運行時深層屬性被略過的缺陷。
  - Task 4: 測試驅動驗證 — 全專案 315 項測試 100% 綠燈通過。
  - Task 5: 模組技術手冊同步 — 更新後端 M1 與 M4 手冊。

---

## 🔮 後續備選里程碑 (Upcoming Milestones)

### 📌 Milestone 4 — 前端歷史樣式巨石除役 (FE-6)
- **目標**：完成 `static/css/style.css`（122 KB）歷史巨石安全移除，徹底切換至 `style-01` ~ `style-07` 七層樣式流。

---

## 🛡️ 三位一體之「完成定義 (Definition of Done, DoD)」

任何一個 Micro-Task 標記為 `[x]` 前，必須無條件通過以下檢驗：
1. **代碼實作**：100% 遵循「專案第一紅線（資料驅動、零硬編碼、深拷貝完整性）」。
2. **測試驗證**：新增機制必須具備對應測試，且 `.\test.ps1` 全量執行 100% 綠燈全數通過。
3. **文檔同步**：若涉及 API、DTO、配置結構變更，同步更新對應的 `docs/manual/` 模組手冊。
4. **繁體中文**：所有註解、日誌、Commit 訊息一律使用標準繁體中文。
