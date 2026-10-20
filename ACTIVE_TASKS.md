# 🎯 ACTIVE_TASKS (當前迭代微任務看板)

> **模式規範**：遵循 Spec-Driven Development 與 AI Agentic Workflow 最佳實踐。  
> **單一焦點原則**：每次對話僅推進 **1 個 Micro-Task**，完成後進行測試驗證與手冊同步，並勾選為 `[x]`。  
> **長遠演進參照**：宏觀藍圖與全局待辦已收錄於 👉 [`docs/plans/FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md)。

---

## 📌 當前焦點里程碑：路徑 Y — 單機語意純化與全域硬編碼大清剿 (SP-PURITY & DATA-CORE)

- **長遠藍圖**：[`FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md) 第 25 章《單機語意純化與全域硬編碼大清剿》
- **核心目標**：徹底消除房間內「其他玩家廣播」等非單機多人殘留語意，清除客棧 `contains("inn")`、商店掌櫃福伯台詞、道核固定數值與全域出生地硬編碼，守衛專案第一紅線（100% 資料驅動），維持全專案 331+ 測試全綠。

### 📋 微任務清單 (Micro-Tasks)

- [x] **Task 1 (SP-PURITY-01): 房間廣播通道與多人遍歷舊代碼徹底除役**
  - 物理刪除 `RoomService.java` 中標註 `@Deprecated` 之 `broadcastJson(List<Player>, ...)` 與 `broadcastToOthers(List<Player>, ...)`。
  - 將各指令與實體中語意混淆之 `room.broadcastToOthers(...)` 收斂為純粹單機環境事件或主角通知，去除無意義的排他性廣播。
  - 清理 `WorldManager.java` 底部註解殘留之 20 行死代碼。
  - 校準 `README.md` 測試數量指標（315 -> 331+）。
  - 驗收測試全數通過，全專案 331 項測試維持 100% 綠燈。
- [x] **Task 2 (DATA-CORE-01): 房間安全區標籤化與全域出生點 GameConfig 統一驅動**
  - 消除 `RestCommand` 與 `GameStateBroadcastService` 中寫死字串 `id.contains("inn")`，改以 `RoomFlag.SAFE_ZONE` 判定。
  - 消除 `SaveGameService` 與 `DungeonCommand` 中寫死字串 `"newbie_village:inn"`，全面改由注入之 `gameConfig.getDefaults().getPlayer().getSpawnRoomId()` 驅動。
  - 在 `DataIntegrityValidator` 擴充地牢 `exitRoomId` 拓撲校驗，消除首領地塊 hardcoded `"mozhu_mines:boss_song_tianheng"`。
  - 新增 `DataCoreIntegrityTest` 專屬整合測試，全專案 335 項測試維持 100% 綠燈。
- [ ] **Task 3 (DATA-CORE-02): 商店 NPC 身份動態化、道核技能反查與夥伴列表配置化**
  - 重構 `ShopCommand`：依據 `shop.npcId()` 查詢真實掌櫃名稱與台詞，徹底消除所有商店寫死「掌櫃福伯擦著汗道」、「客官要進墨竹山吧？」。
  - 重構 `Party.useItemOnMember` 中 `LEARN_SKILL`：自 `TemplateReader` 動態反查技能範本數值，消滅 `cooldownMs(6000)` 與 `damageMultiplier(1.8)` 寫死數值。
  - 重構 `PartyService` 夥伴招募失敗提示：自 `templateReader.getAllCompanions()` 動態產出可用夥伴清單。
- [ ] **Task 4 (DATA-GUARD-03): 前端地塊動態 Tooltip 與看門狗棘輪加固**
  - `dungeon-panel.js` 懸停 title 優先讀取 DTO 地塊動態名稱，消除「玄岩石壁」寫死符號推論。
  - 清理 `Player.java` 與 `Living.java` 中的 `GameConfig.getInstance()`，收緊 `ArchitectureGuardrailsTest` 棘輪閾值。

---

## 🏆 已完成里程碑 (Completed Milestones)

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
