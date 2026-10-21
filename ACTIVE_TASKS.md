# 🎯 ACTIVE_TASKS (當前迭代微任務看板)

> **模式規範**：遵循 Spec-Driven Development 與 AI Agentic Workflow 最佳實踐。  
> **單一焦點原則**：每次對話僅推進 **1 個 Micro-Task**，完成後進行測試驗證與手冊同步，並勾選為 `[x]`。  
> **長遠演進參照**：宏觀藍圖與全局待辦已收錄於 👉 [`docs/plans/FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md)。

---

## 📌 當前焦點里程碑：Milestone F1-03 — 看門狗棘輪全面清零，消除 26 處 new TemplateCatalog() 靜默 fallback

- **長遠藍圖**：[`FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md) 第 22 章《戰線一：核心雙軌大一統與領域潔淨治理 (F1-03)》
- **核心目標**：全面掃除 `src/main/java` 中歷史殘留的 26 處 `new TemplateCatalog()` 靜默 fallback，徹底落實 Spring 建構子依賴注入與不可變參數傳遞，將 `ArchitectureGuardrailsTest` 看門狗棘輪閾值降為 0，實現架構零破窗。

### 📋 微任務清單 (Micro-Tasks)

- [ ] **Task 1 (F1-03-SERVICE): Spring 託管服務與命令的 TemplateReader 依賴注入加固**
  - 清理 `PartyCommand`, `CharacterSyncService`, `DrpgBattleService`, `DrpgEnemyTacticsService`, `DrpgRewardService`, `PartyService`, `DrpgTemplateAdapter`, `MudTemplateAdapter` 的 fallback 實例化。
- [ ] **Task 2 (F1-03-DTO-ENTITY): 實體與模板類別靜態重載消除 new TemplateCatalog()**
  - 清理 `BattleEnemy`, `PartyInventory`, `PartyMember`, `ShopTemplate`, `MessageFactory` 中的 fallback 實例化。
- [ ] **Task 3 (F1-03-DRPG-STATE): DrpgStateDto 視圖構造全面對齊 TemplateReader 顯式傳遞**
  - 清理 `DrpgStateDto` 中 7 處 `new TemplateCatalog()`。
- [ ] **Task 4 (F1-03-GUARD-ZERO): 架構看門狗棘輪閥值歸零與全量回歸驗收**
  - 將 `ArchitectureGuardrailsTest.java` 棘輪閾值由 26 降至 0。
  - 全專案 338+ 測試 100% 綠燈通過。

---

## 🏆 已完成里程碑 (Completed Milestones)

- [x] **Milestone 3 — 後端領域層 Package 結構按業務模組收斂 (F1-02)**
  - Task 1 (F1-02-PROGRESSION): 修為與境界服務收斂至 `domain.progression.service`。
  - Task 2 (F1-02-COMBAT): 核心戰鬥引擎服務收斂至 `domain.combat.service`。
  - Task 3 (F1-02-ACTOR): 生靈主體與角色代理服務收斂至 `domain.actor.service`。
  - Task 4 (F1-02-ADAPTER & VERIFY): 範本目錄與適配器收斂至 `domain.repository`，338 測試全綠。

- [x] **Milestone Path Y — 單機語意純化與全域硬編碼大清剿 (SP-PURITY-01 ~ DATA-GUARD-03)**
  - Task 1: 房間廣播通道與多人遍歷舊代碼徹底除役，死代碼清理 (SP-PURITY-01)。
  - Task 2: 房間安全區標籤化與全域出生點 GameConfig 統一驅動 (DATA-CORE-01)。
  - Task 3: 商店 NPC 身份動態化、道核技能反查與夥伴列表配置化 (DATA-CORE-02)。
  - Task 4: 前端地塊動態 Tooltip 支援與看門狗棘輪加固至 3 處 (DATA-GUARD-03)，全專案 338 測試全綠。

- [x] **Milestone Path X — 純單機單體化演進戰略：多人機制、雙軌架構與膠水代碼全域收斂 (SP-01 ~ SP-09)**
  - Task 1 (SP-01): 刪除 `TellCommand` 多人私聊與 `findPlayerByName`。
  - Task 2 (SP-02): 廢止 `StashCommand` 雙向轉存，除役個人背包假帳。
  - Task 3 (SP-03): 存檔寫入與載入徹底除役 `personalInventory` 雙寫 (schemaVersion=3)。
  - Task 4 (SP-04): `Room` 房間實體改為單一主角引用，除役多玩家廣播遍歷。
  - Task 5 (SP-05): WebSocket 多開連線自動接管唯一主角，杜絕存檔覆寫競爭。
  - Task 6 (SP-06): 技能模型合一 (`UnifiedSkill`)，消除 MUD/DRPG 技能定義割裂。
  - Task 7 (SP-07): 徹底物理刪除 `SkillBridgeService.java` 膠水代碼。
  - Task 8 (SP-08): 戰鬥引擎大一統，野外戰鬥全面對齊 5 人小隊協同作戰 (`FieldCombatPartyUnificationTest`)。
  - Task 9 (SP-09): 小隊共享金庫與經濟體系大一統 (`UnifiedPartyEconomyTest`)，全專案 331 項測試全綠。

- [x] **Milestone 2 — 核心雙軌實體大一統：Player 與 PartyMember 實體合一與單一真相源治理 (F1-01 / M1 / M4)**
  - Task 1: 行囊大一統 (Unified Party Inventory) — 消除個人 12 格限制與強制 stash 轉存，40 格小隊公共背包為唯一真相源。
  - Task 2: 裝備與核心屬性大一統 (Unified Equipment & Stats) — 徹底除役 5 秒 CompletableFuture 超時阻塞，達成 0ms 零延遲鏡像投影。
  - Task 3: 廢止 CharacterSyncService 偽 Outcome 拷貝 — 同步直接深度鏡像，修復線上運行時深層屬性被略過的缺陷。
  - Task 4: 測試驅動驗證 — 全專案 315 項測試 100% 綠燈通過。
  - Task 5: 模組技術手冊同步 — 更新後端 M1 與 M4 手冊。

- [x] **Milestone 2.5 — 文檔代碼全域對齊、40 格行囊標準裁定與前端 WebSocket 模組化重構 (DOC-FACT-01 / DOC-CAP-01 / DOC-WS-01 / DOC-ENUM-01)**
  - Task 1: 40 格小隊行囊全域標準裁定與代碼文檔落地 (DOC-CAP-01) — 後端常數升級為 40，前端與 16 本手冊全域同步。
  - Task 2: 事實脫節全面清繳 (DOC-FACT-01) — 清理已刪除的 `SkillBridgeService`，對齊 Java 25 `ScopedValue`、`schemaVersion=3` 與 324 項測試基準。
  - Task 3: 前端 WebSocket 獨立成檔與健壯性重構 (DOC-WS-01) — 新建 `network/websocket.js`，實裝指數退避斷線重連（1s~15s），清除 `index.html` 內嵌連線腳本（嚴格 CSP），落實單機心跳合理化政策。
  - Task 4: 全域遊戲詞彙表分層與裝備枚舉對齊 (DOC-ENUM-01) — 校準 `EquipmentSlot` 5+2 雙飾品體系；在 `後端_M3` 確立戰鬥概念四層分離字典（DamageType 20 種繼承樹 / 九大靈氣 / 狀態標籤 / BuffCategory）與 500ms 心跳階層。

---

## 🔮 後續備選里程碑 (Upcoming Milestones)

### 📌 Milestone 3 — 後端領域層 Package 結構按模組收斂 (M1/M4/M5/M6)
- **已完成 M1**: 探索與世界模組 (`domain.world`)
- **待執行 M5/M4/M3**: 修為進程、生靈主體、戰鬥模組收斂。

### 📌 Milestone 4 — 前端歷史樣式巨石除役 (FE-6)
- **目標**：完成 `static/css/style.css`（122 KB）歷史巨石安全移除，徹底切換至 `style-01` ~ `style-07` 七層樣式流。

---

## 🛡️ 三位一體之「完成定義 (Definition of Done, DoD)」

任何一個 Micro-Task 標記為 `[x]` 前，必須無條件通過以下檢驗：
1. **代碼實作**：100% 遵循「專案第一紅線（資料驅動、零硬編碼、深拷貝完整性）」。
2. **測試驗證**：新增機制必須具備對應測試，且 `.\test.ps1` 全量執行 100% 綠燈全數通過。
3. **文檔同步**：若涉及 API、DTO、配置結構變更，同步更新對應的 `docs/manual/` 模組手冊。
4. **繁體中文**：所有註解、日誌、Commit 訊息一律使用標準繁體中文。
