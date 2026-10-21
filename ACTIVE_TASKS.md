# 🎯 ACTIVE_TASKS (當前迭代微任務看板)

> **模式規範**：遵循 Spec-Driven Development 與 AI Agentic Workflow 最佳實踐。  
> **單一焦點原則**：每次對話僅推進 **1 個 Micro-Task**，完成後進行測試驗證與手冊同步，並勾選為 `[x]`。  
> **長遠演進參照**：宏觀藍圖與全局待辦已收錄於 👉 [`docs/plans/FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md)。

---

## 📌 當前焦點里程碑：Milestone F2-01 — 敵方行動預告蓄力條與即時反制打斷 (Combat React, COMBAT-REACT-01, P1)

- **長遠藍圖**：[`FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md) 第 22.2 章《戰線二：戰鬥深度突破與動態沉浸博弈 (F2-01)》
- **核心目標**：在 DRPG 半即時戰鬥中引入敵方強力招式前搖吟唱 (Casting Window)，支援前端視覺化蓄力條預告；玩家與隊友可於窗口期內打出帶有 `STUN`、`SILENCE`、`PARRY` 或高 Poise 破防打斷技，觸發失衡中斷與虛弱，大幅提升戰鬥博弈深度。

### 📋 微任務清單 (Micro-Tasks)

- [ ] **Task 1 (F2-01-CAST-MODEL): 技能與敵方行動前搖/可打斷模型擴充**
  - 在 `SkillDefinition.java` / `UnifiedSkill.java` 擴充 `castTimeMs`（吟唱蓄力時間）、`interruptible`（是否可被打斷標籤）。
  - 在 `MobTemplate.java` 與 `BattleEnemy.java` 擴充敵方蓄力招式設定與當前施法狀態結構 (`CastingState` / `currentCastingSkill`)。
- [ ] **Task 2 (F2-01-CAST-LOOP): 戰鬥迴圈吟唱窗口推進與打斷判定**
  - 在 `DrpgCombatLoop` 與 `CombatService` 實裝敵人施法前搖計時窗口；在吟唱期間敵方持續推進蓄力進度。
  - 結算受擊時若判定帶有控制或打斷標籤（`STUN`, `SILENCE`, `PARRY`, 或罡氣破防），立刻中斷吟唱並進入短暫破防失衡狀態。
- [ ] **Task 3 (F2-01-REACT-ACTION): 玩家與小隊反制技能與戰況廣播**
  - 擴充玩家/隊友反制招式（如 `太白斷空`、`震天撼地`、格擋偏架），成功打斷時發布 `COMBAT_INTERRUPT` 戰鬥事件。
  - DTO 擴充 `DrpgCombatantDto` 支援 `castingSkillId`、`castingProgress`、`interruptible` 即時下發。
- [ ] **Task 4 (F2-01-FE-CASTBAR & VERIFY): 前端蓄力條特效渲染與專屬測試驗證**
  - 前端 `drpg-renderer.js` / `battle-panel.js` 於敵方血條上方實裝青藍色/猩紅色動態蓄力條 (`.casting-bar`) 與打斷特效 (`⚡ 打斷!`)。
  - 編寫專屬整合測試 `CombatReactInterruptTest.java`，並跑通全量 348+ 測試回歸。

---

## 🏆 已完成里程碑 (Completed Milestones)

- [x] **Milestone F1-01 — Player 徹底降級為純空間代理與實體雙寫徹底清零 (Unified Character Core, P1)**
  - Task 1 (F1-01-PROXY-DELEGATE): 重構 `GetCommand` 與 `DropCommand`，拾取/丟棄 100% 只操作 `party.getInventory()`，移除對 `player.getInventory()` 的雙寫；`WorldFactory.createCorpse` 玩家死亡掉落物品優先從小隊行囊提取。
  - Task 2 (F1-01-EQUIP-UNIFY): `MessageFactory` 裝備展示優先讀取小隊隊長真實裝備；`Player` 覆寫 `getMainHandWeapon()` 與 `getOffHandEquip()` 動態委派隊長真實槽位。
  - Task 3 (F1-01-SYNC-SLIM): `CharacterSyncService` 確立為唯讀單向投影鏡像服務，消除雙向淺拷貝競爭；存讀檔管線確認僅存小隊行囊與成員裝備 (schemaVersion 3)。
  - Task 4 (F1-01-VERIFY): 新增專屬整合測試 `UnifiedCharacterCoreTest.java`（5/5 綠燈），全專案 348 項測試 100% 綠燈通過。

- [x] **Milestone F1-05 — 地牢生存火把明滅與步進機關陷阱 (Dungeon Torch & Step Traps, P1)**
  - Task 1 (F1-05-TORCH-CORE): 實裝火把光明度核心模型 (`torchLight` 0~100)、照度等級 (`BRIGHT`/`DIM`/`DARKNESS`)、步進消耗、黑暗扣減 5 點 SAN 與突襲、`dungeon light / torch` 補光機制。
  - Task 2 (F1-05-VISION-RAYCAST): 光明度動態視距與射線阻擋遮擋（BRIGHT 3步 -> DIM 2步 -> DARKNESS 1步），DTO 擴充 `torchLight`、`torchStatus`、`torchIcon`。
  - Task 3 (F1-05-TRAP-SPRING): 暗弩陷阱先鋒身法檢定、靈泉地窟全員滋養（+50 HP, +30 MP, +15 SAN）、鎖鏈暗門先鋒臂力/工具破拆獲得戰氣，JSON 拓撲完整資料驅動。
  - Task 4 (F1-05-FE-RADAR & VERIFY): 前端 `dungeon-panel.js` 雷達火把狀態條、點火按鈕、黑暗暗影暈影特效與靈泉地窟 `♨` 渲染，新增專屬測試 `DungeonTorchAndTrapTest`，全專案 343 項測試 100% 綠燈通過。

- [x] **Milestone F1-04 — 前端歷史樣式巨石 `style.css` (122 KB) 安全除役**
  - 物理安全刪除 122 KB 之 `static/css/style.css`，全系統 100% 運行於 `style-01` ~ `style-07` 七層樣式流。
  - 存檔歷史頁面 `singleplayer_v1.html` 同步對齊為引用七層樣式流。
  - 同步更新前端技術手冊 `前端_FE6` 與 `UI_PAGE_STRUCTURE.md`，全專案 338 項測試全綠通過。

- [x] **Milestone F1-03 — 看門狗棘輪全面清零，消除 26 處 new TemplateCatalog() 靜默 fallback**
  - 核心服務強制落實 TemplateReader 建構子依賴注入 (PartyService, DrpgBattleService 等)。
  - 實體與視圖模型消除內部私建目錄反模式 (BattleEnemy, PartyInventory, DrpgStateDto)。
  - 架構看門狗 ArchitectureGuardrailsTest 違規閾值由 26 降為 0，實現架構零破窗。
  - 全專案 338 項測試 100% 綠燈通過。

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
