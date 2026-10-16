# 🎯 ACTIVE_TASKS (當前迭代微任務看板)

> **模式規範**：遵循 Spec-Driven Development 與 AI Agentic Workflow 最佳實踐。  
> **單一焦點原則**：每次對話僅推進 **1 個 Micro-Task**，完成後進行測試驗證與手冊同步，並勾選為 `[x]`。  
> **長遠演進參照**：宏觀藍圖與全局待辦已收錄於 👉 [`docs/plans/FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md)。

---

## 📌 當前焦點里程碑：Milestone 1 — 戰鬥深度演進：敵方行動預告蓄力條與打斷反制機制 (M3 & FE-3)

- **對應模組手冊**：
  - 後端：[`後端_M3_核心戰鬥引擎模組_COMBAT_ENGINE.md`](./docs/manual/後端_M3_核心戰鬥引擎模組_COMBAT_ENGINE.md)
  - 前端：[`前端_FE3_戰鬥擂台與特效引擎_BATTLE_ARENA_AND_FX.md`](./docs/manual/前端_FE3_戰鬥擂台與特效引擎_BATTLE_ARENA_AND_FX.md)
- **核心目標**：將目前的純意圖圖示升級為「動態吟唱蓄力條 + 玩家即時打斷/反制窗口 (Stun/Silence/Parry) + 仇恨轉火告警」，徹底落實戰鬥視覺化。

### 📋 微任務清單 (Micro-Tasks)

- [x] **Task 1: 資料模型與設定擴充 (Data Model & Schema)**
  - 檢視 `mobs.json` 與技能 JSON，補齊蓄力招式之 `castTimeMs` 與 `interruptible` 標籤。
  - `CombatMobDto` / `BattleEnemyDto` 擴充 `currentAction`、`actionProgressPercent`、`interruptible` 欄位。
- [x] **Task 2: 戰鬥迴圈蓄力與打斷狀態解算 (Combat Engine FSM)**
  - 在 `DrpgCombatLoop` 與 `CombatFsmService` 實裝怪物施法階段（`CASTING`）。
  - 當怪物受到帶有 `STUN`、`SILENCE` 標籤的招式時觸發打斷判定，重置蓄力並進入失衡狀態。
- [x] **Task 3: 機制單元與整合測試編寫 (Test-Driven Verification)**
  - 編寫 `EnemyCastAndInterruptTest.java`，驗證蓄力進度推移、打斷成功率與免疫機制。
  - 執行 `.\test.ps1` 確認全專案 315 測試全數綠燈。
- [ ] **Task 4: 前端戰鬥擂台 FE-3 視覺化呈現 (Frontend Visuals)**
  - `battle-panel.js` 與 `style-03-battle.css` 渲染敵方卡片即時蓄力條（普通招式青藍色 / 致命招式猩紅色，平滑進度百分比）。
  - 打斷反制視覺回饋：觸發 `⚡ 打斷!` 震顫粒子、失衡紅框閃爍與 FCT 浮動提示文字；微動態相容 `@media (prefers-reduced-motion)`。
  - 邊界防禦：怪物死亡、逃跑或戰鬥超時熔斷時即時重置蓄力狀態與 UI，杜絕幽靈施法與殘留。
- [ ] **Task 5: 全面整合與架構防線驗證 (Verification Gate)**
  - 邊界案例覆蓋：確認打斷判定為確定性標籤比對（`STUN` / `SILENCE` 於可打斷窗口內 100% 觸發打斷）、首領霸體（`UNSTOPPABLE`）免疫防禦。
  - 執行全量 `.\test.ps1`，確保 315 測試 100% 綠燈全數通過，架構看門狗維持在 26 處以內。
- [ ] **Task 6: 模組手冊同步與 Git Commit 提交流程**
  - 同步更新 `後端_M3` 與 `前端_FE3` 手冊之 API / DTO 欄位說明與蓄力打斷時序圖。
  - 由使用者審查後執行繁體中文 Git Commit。

---

## 🔮 後續備選里程碑 (Upcoming Milestones)

### 📌 Milestone 2 — 後端領域層 Package 結構按模組收斂 (M1/M4/M5/M6)
- **目標**：解決 `com.example.htmlmud.domain.service` 平鋪 10+ 個跨領域服務之問題，依模組拆分為 `domain.world`、`domain.party`、`domain.progression`、`domain.economy`。
- **微任務預排**：
  - [ ] 1. 建立領域子包並搬移對應 Service。
  - [ ] 2. 修正 package import 與 Spring Bean 宣告。
  - [ ] 3. 執行 `ArchitectureGuardrailsTest` 確認 Clean Architecture 依賴反轉無違規。
  - [ ] 4. 執行 `.\test.ps1` 確保 315 測試通過。

### 📌 Milestone 3 — 前端歷史樣式巨石除役 (FE-6)
- **目標**：完成 `static/css/style.css`（122 KB）歷史巨石安全移除，徹底切換至 `style-01` ~ `style-07` 七層樣式流。
- **微任務預排**：
  - [ ] 1. 確認 `index.html` 是否完全不依賴舊 `style.css`。
  - [ ] 2. 備份或安全刪除 `static/css/style.css`。
  - [ ] 3. 啟動本機伺服器並使用瀏覽器次代理（Browser Subagent）全景檢驗各視圖外觀。
  - [ ] 4. 更新 `前端_FE6` 樣式手冊。

---

## 🛡️ 三位一體之「完成定義 (Definition of Done, DoD)」

任何一個 Micro-Task 標記為 `[x]` 前，必須無條件通過以下檢驗：
1. **代碼實作**：100% 遵循「專案第一紅線（資料驅動、零硬編碼、深拷貝完整性）」。
2. **測試驗證**：新增機制必須具備對應測試，且 `.\test.ps1` 全量執行 100% 綠燈全數通過。
3. **文檔同步**：若涉及 API、DTO、配置結構變更，同步更新對應的 `docs/manual/` 模組手冊。
4. **繁體中文**：所有註解、日誌、Commit 訊息一律使用標準繁體中文。
