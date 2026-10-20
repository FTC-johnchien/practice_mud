# 🎯 ACTIVE_TASKS (當前迭代微任務看板)

> **模式規範**：遵循 Spec-Driven Development 與 AI Agentic Workflow 最佳實踐。  
> **單一焦點原則**：每次對話僅推進 **1 個 Micro-Task**，完成後進行測試驗證與手冊同步，並勾選為 `[x]`。  
> **長遠演進參照**：宏觀藍圖與全局待辦已收錄於 👉 [`docs/plans/FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md)。

---

## 📌 當前焦點里程碑：路徑 X — 純單機單體化演進戰略 (Single-Player Monolith Transition)

- **長遠藍圖**：[`FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md) 第 24 章《純單機單體化演進戰略：多人機制、雙軌架構與膠水代碼全域收斂藍圖》
- **核心目標**：徹底消除舊有 MUD 多人連線、兩本假帳背包、多玩家廣播遍歷與雙軌膠水代碼，確立小隊 40 格行囊為全專案唯一真實載體，簡化架構並保持全專案 315 項測試 100% 綠燈。

### 📋 微任務清單 (Micro-Tasks: SP-01 ~ SP-09)

#### 階段 0：P0 級速勝清理 (Fast Wins & Immediate Decommission)
- [x] **Task 1 (SP-01): 刪除 `TellCommand` 多人私聊與 `findPlayerByName`**
  - 徹底刪除 `TellCommand.java`。
  - 從 `WorldManager.java` 移除多玩家搜尋方法 `findPlayerByName`。
  - 驗收通過：315 項測試 100% 綠燈。
- [x] **Task 2 (SP-02): 廢止 `StashCommand` 雙向轉存，除役個人背包假帳**
  - 廢除 `handleStash` 與 `handleWithdraw` 跨背包搬移業務邏輯。
  - `StashCommand` 簡化為全面友善導引大一統小隊公共行囊提示。
  - 標記 `Living.inventory` 僅限 Mob / 生靈專用，禁止主角使用。
  - 更新 `DualTierInventoryAndStashTest`，315 項測試 100% 綠燈。

#### 階段 1：P1 級核心實體與存檔單一真相源 (Single Source of Truth)
- [x] **Task 3 (SP-03): 存檔寫入與載入徹底除役 `personalInventory` 雙寫**
  - 升級存檔結構至 CURRENT_SCHEMA_VERSION = 3，SaveData 移除 personalInventory 寫出。
  - 在 SaveGameService 徹底除役 personalInventory 的搜集與還原邏輯，單一真相源為 party.inventory。
  - 在 SaveMigrationPipeline 新增 2 -> 3 遷移：舊存檔個人物品自動安全合流至小隊公共行囊，無物資遺失。
  - 新增專屬遷移測試，全專案 316 項測試 100% 綠燈通過。
- [x] **Task 4 (SP-04): `Room` 房間實體改為單一主角引用，除役多玩家廣播遍歷**
  - `Room.players` 列表重構為單一引用 `Player protagonist`（並提供基於單一主角的不可變相容視圖）。
  - 除役多人無差別廣播遍歷，重構 `RoomService`、`LivingService`、`CombatPresentation`、`AggressiveBehavior`、`WorldPulse` 為主角直接推送。
  - 驗收通過：全專案 316 項測試 100% 綠燈。
- [x] **Task 5 (SP-05): WebSocket 多開連線自動接管唯一主角，杜絕存檔覆寫競爭**
  - 引入單機單一主角實例模型（Singleton Protagonist），`SessionRegistry` 升級支援主角與活躍連線治理。
  - `MudWebSocketHandler` 實裝多開分頁/重連自動接管（Session Takeover）：無縫切換 `WebSocketOutput`，關閉舊分頁避免操作干擾，推送最新全景狀態。
  - 加固連線關閉隔離：舊會話關閉不干擾現有連線，活躍連線關閉觸發自動存檔防禦進度遺失。
  - 新增專屬驗收測試 `SessionTakeoverAndSingletonProtagonistTest`，全專案 320 項測試 100% 綠燈通過。
  - 🎉 **【階段 1：P1 級核心實體與存檔單一真相源】圓滿達成！**

#### 階段 2：P2 級機制大一統與膠水代碼徹底除役 (Deep Convergence)
- [x] **Task 6 (SP-06): 技能模型合一 (`UnifiedSkill`)，消除 MUD/DRPG 技能定義割裂**
  - 定義大一統規範技能介面 `UnifiedSkill`，統合修煉境界（Level 1~10、XP、MaxXP）、戰術數值（CD、消耗、傷害倍率、打斷標籤）與敘事文本（`moves` 動態招式播報）。
  - 升級 `PartyMemberSkill` 實作 `UnifiedSkill`，實裝內聚的熟練度積累（`addXp`）、境界突破（`levelUp`）與動態威力/冷卻等級縮放。
  - 在 `SkillEntry` 實作與大一統技能雙向無縫互轉（`toUnifiedSkill` / `fromUnifiedSkill`），主角與 4 位小隊隊員統一持有合一技能模型。
  - 新增專屬驗收測試 `UnifiedSkillModelTest`，全專案 324 項測試 100% 綠燈通過。
- [x] **Task 7 (SP-07): 徹底除役 `SkillBridgeService` 翻譯縮放膠水代碼**
  - 徹底物理刪除 `SkillBridgeService.java` 膠水代碼。
  - `CharacterSyncService` 直接以 `UnifiedSkill` / `PartyMemberSkill` 進行同步，不再需要中間層動態縮放與字串比對翻譯。
  - `DrpgRewardService` 戰鬥後熟練度原地增長給存活小隊成員，主角隊長技能同步回傳，消除反向回饋膠水代碼。
  - 重構 `CanonicalSkillModelTest`、`MechanismPurityAndBugfixTest` 與 `SkillBridgeIntegrationTest`，全專案 324 項測試 100% 綠燈通過。
- [x] **Task 8 (SP-08): 戰鬥引擎大一統，野外戰鬥全面對齊 5 人小隊協同作戰**
  - 徹底收斂野外 MUD 與地牢 DRPG 雙軌機制，消滅「主角一人單挑，小隊另外 4 位隊友冷眼看戲」的架構割裂。
  - `DrpgBattleService` 實裝 `startFieldBattle(Player, Mob)`，野外遭遇戰由 5 人小隊全員依照站位、陣法與戰術 AI (Gambit) 協同迎敵。
  - `BattleContext` 增設 `onBattleEnd` 生命週期回呼，戰鬥大捷時掉落物直入 40 格小隊公共行囊，隊員獲得修為經驗與技能熟練度原地增長，發布擊殺事件並同步移除/標記大世界房間 Mob。
  - `KillCommand` 與 `CombatService.startCombat` 全面委派至小隊戰鬥引擎，`TargetSelector` 自動過濾死亡怪防禦性防護。
  - 新增 `FieldCombatPartyUnificationTest`，全專案 327 項測試 100% 綠燈零回歸破壞。
- [ ] **Task 9 (SP-09): 小隊共享金庫與經濟體系大一統**

---

## 🏆 已完成里程碑 (Completed Milestones)

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
