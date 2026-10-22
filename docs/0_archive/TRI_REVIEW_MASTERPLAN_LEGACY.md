# 📝 專案歷史三方審查全面整合治理總表 (Tri-Review Masterplan Legacy)

> **歷史封存說明 (Archival Notice)**：  
> 本文件收錄 2026-10-15 期間整合 Codex、Copilot 與 Claude 三方審查建議之宏觀大一統歷史戰略表（原 §22 與 §23）。  
> 相關演化成果已分別落入當前 4 個層級手冊與各專題待辦中。本文件僅作歷史考證保留。

---

## 🚀 22. 全專案大一統與現代化演進戰略總表 (2026-10-15 全文件對齊版)

> **戰略定位與對齊宗旨**：  
> 本章節為全專案所有未實作項目、技術手冊未來演進（`docs/manual/` 15 本手冊）與概覽層規劃（`docs/overview/`）的**總集成樞紐**。  
> 徹底消除過往散落於不同文件的規劃脫節，將全專案未來演化嚴格收斂為**五大戰略演進戰線**，並與當前執行看板 [`ACTIVE_TASKS.md`](../../ACTIVE_TASKS.md) 建立動態閉環。

### 📊 全文件溯源與對齊矩陣 (Cross-Document Traceability Matrix)

| 戰線代號 | 核心規劃項目 | 優先級 | 主管後端手冊 (Java 25) | 主管前端手冊 (ES6/CSS) | 全域概覽對齊文件 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **F1-01** | `Player` 與 `PartyMember` 雙軌實體大一統 ✅ *(已完成)* | **P1** | [`後端_M1`](../manual/後端_M1_探索與世界模組_WORLD_AND_EXPLORATION.md) / [`後端_M4`](../manual/後端_M4_小隊生靈與陣法模組_PARTY_AND_FORMATION.md) | [`前端_FE5`](../manual/前端_FE5_小隊裝備與存檔彈窗_PARTY_EQUIPMENT_AND_MODALS.md) | [`MODULES_MAP.md`](../overview/MODULES_MAP.md) |
| **F1-02** | 後端領域層 Service 包結構按業務模組拆分 | **P1** | [`後端_M1`](../manual/後端_M1_探索與世界模組_WORLD_AND_EXPLORATION.md) ~ [`M6`](../manual/後端_M6_物品背包與經濟模組_ITEM_AND_ECONOMY.md) | *(無關)* | [`ARCHITECTURE.md`](../../ARCHITECTURE.md) |
| **F1-03** | 看門狗棘輪全面清零 (26 處 fallback 歸零) | **P1** | [`後端_M7`](../manual/後端_M7_資料驅動模板模組_DATA_DRIVEN_TEMPLATES.md) | *(無關)* | [`ARCHITECTURE_SUMMARY.md`](../overview/ARCHITECTURE_SUMMARY.md) |
| **F1-04** | 前端歷史樣式巨石 `style.css` (122 KB) 安全除役 | **P2** | *(無關)* | [`前端_FE6`](../manual/前端_FE6_七層樣式串流與視覺主題_CSS_TOKENS_AND_THEMES.md) | [`FRONTEND_LAYOUT_GUIDE.md`](../overview/FRONTEND_LAYOUT_GUIDE.md) |
| **F2-01** | 敵方行動預告蓄力條與即時反制打斷 (Combat React) ✅ *(已完成)* | **P1** | [`後端_M3`](../manual/後端_M3_核心戰鬥引擎模組_COMBAT_ENGINE.md) | [`前端_FE3`](../manual/前端_FE3_戰鬥擂台與特效引擎_BATTLE_ARENA_AND_FX.md) | [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md) |
| **F2-02** | 首領多階段血條與狂暴轉階段機制 (Boss Phases) | **P2** | [`後端_M3`](../manual/後端_M3_核心戰鬥引擎模組_COMBAT_ENGINE.md) | [`前端_FE3`](../manual/前端_FE3_戰鬥擂台與特效引擎_BATTLE_ARENA_AND_FX.md) | [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md) |
| **F2-03** | 十二時辰更迭與微氣候生態動態遇敵 | **P2** | [`後端_M1`](../manual/後端_M1_探索與世界模組_WORLD_AND_EXPLORATION.md) / [`後端_M2`](../manual/後端_M2_DRPG網格地牢模組_DRPG_DUNGEON.md) | [`前端_FE2`](../manual/前端_FE2_DRPG視口與雷達渲染器_DRPG_VIEWPORT_RENDERER.md) / [`前端_FE4`](../manual/前端_FE4_城鎮中樞與動作列面板_TOWN_HUB_AND_ACTION_BAR.md) | [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md) |
| **F2-04** | 道心異化深淵靈視 (Sanity Breakdown) | **P2** | [`後端_M4`](../manual/後端_M4_小隊生靈與陣法模組_PARTY_AND_FORMATION.md) | [`前端_FE3`](../manual/前端_FE3_戰鬥擂台與特效引擎_BATTLE_ARENA_AND_FX.md) | [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md) |
| **F2-05** | 複合靈氣連鎖反應與狀態引爆 (Elemental Reactions & Primer/Detonator) | **P2** | [`後端_M3-1`](../manual/後端_M3-1_狀態效果與Buff結算模組_BUFF_AND_STATUS_EFFECTS.md) | [`前端_FE3`](../manual/前端_FE3_戰鬥擂台與特效引擎_BATTLE_ARENA_AND_FX.md) | [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md) |
| **F3-01** | 古仙陣法動態連攜合擊技 (Cross Combos) | **P2** | [`後端_M4`](../manual/後端_M4_小隊生靈與陣法模組_PARTY_AND_FORMATION.md) | [`前端_FE5`](../manual/前端_FE5_小隊裝備與存檔彈窗_PARTY_EQUIPMENT_AND_MODALS.md) | [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md) |
| **F3-02** | 境界突破天道雷劫試煉 (Tribulation Lightning) | **P2** | [`後端_M5`](../manual/後端_M5_技能境界與修煉模組_SKILL_AND_PROGRESSION.md) | [`前端_FE5`](../manual/前端_FE5_小隊裝備與存檔彈窗_PARTY_EQUIPMENT_AND_MODALS.md) | [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md) |
| **F3-03** | 隨機古神詞綴體系 (Procedural Affixes) | **P2** | [`後端_M6`](../manual/後端_M6_物品背包與經濟模組_ITEM_AND_ECONOMY.md) / [`後端_M7`](../manual/後端_M7_資料驅動模板模組_DATA_DRIVEN_TEMPLATES.md) | [`前端_FE5`](../manual/前端_FE5_小隊裝備與存檔彈窗_PARTY_EQUIPMENT_AND_MODALS.md) | [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md) |
| **F4-01** | 動態沉浸微粒子氛圍層 (Ambient Canvas) | **P2** | *(無關)* | [`前端_FE2`](../manual/前端_FE2_DRPG視口與雷達渲染器_DRPG_VIEWPORT_RENDERER.md) / [`前端_FE3`](../manual/前端_FE3_戰鬥擂台與特效引擎_BATTLE_ARENA_AND_FX.md) | [`FRONTEND_LAYOUT_GUIDE.md`](../overview/FRONTEND_LAYOUT_GUIDE.md) |
| **F4-02** | 虛擬滾動情報日誌 (Virtualized Log) | **P2** | *(無關)* | [`前端_FE1`](../manual/前端_FE1_核心狀態與事件管線_CORE_STATE_AND_PIPELINE.md) | [`FRONTEND_LAYOUT_GUIDE.md`](../overview/FRONTEND_LAYOUT_GUIDE.md) |
| **F4-03** | 原生 Gamepad API 掌機手柄支援與震動回饋 | **P3** | *(無關)* | [`前端_FE1`](../manual/前端_FE1_核心狀態與事件管線_CORE_STATE_AND_PIPELINE.md) / [`前端_FE4`](../manual/前端_FE4_城鎮中樞與動作列面板_TOWN_HUB_AND_ACTION_BAR.md) | [`QUICKSTART.md`](../overview/QUICKSTART.md) |
| **F5-01** | 領域事件總線 (Domain EventBus EDA) | **P2** | [`後端_M9`](../manual/後端_M9_虛擬執行緒Actor通訊模組_ACTOR_AND_PROTOCOL.md) | *(無關)* | [`ARCHITECTURE.md`](../../ARCHITECTURE.md) |
| **F5-02** | WebSocket 雙軌傳輸協定 (Protobuf Pipeline) | **P3** | [`後端_M9`](../manual/後端_M9_虛擬執行緒Actor通訊模組_ACTOR_AND_PROTOCOL.md) | [`前端_FE1`](../manual/前端_FE1_核心狀態與事件管線_CORE_STATE_AND_PIPELINE.md) | [`ARCHITECTURE_SUMMARY.md`](../overview/ARCHITECTURE_SUMMARY.md) |
| **F5-03** | 外部模組擴充 SPI (Plugin SPI Architecture) | **P3** | [`後端_M7`](../manual/後端_M7_資料驅動模板模組_DATA_DRIVEN_TEMPLATES.md) | *(無關)* | [`MODULES_MAP.md`](../overview/MODULES_MAP.md) |
| **F5-04** | Docker 一鍵容器化與 Dev Container 支援 | **P3** | *(建構運維)* | *(建構運維)* | [`QUICKSTART.md`](../overview/QUICKSTART.md) |

---

### 22.1 戰線一：核心雙軌大一統與領域潔淨治理 (P1)

#### 1. F1-01：`Player` 與 `PartyMember` 雙軌實體大一統 (Unified Character Core) ✅ [已完成]
- **痛點根因**：
  - MUD 端 `Player`（繼承 `Living` VirtualActor）與 DRPG 端 `PartyMember`（POJO）雙軌並立，兩套背包、兩套裝備。
  - 僅在進入/離開地牢戰鬥時透過 `CharacterSyncService` 進行單向屬性拷貝，換裝與 Buff 無法互通。
- **大一統架構方案**：
  ```mermaid
  graph TD
      subgraph UnifiedModel ["大一統實體架構 (Single Source of Truth)"]
          PM["PartyMember (唯一真實角色實體)"]
          PM --> EQ["EquipmentSlots (7大槽位單一持有)"]
          PM --> INV["PartyInventory (小隊公共行囊單一持有)"]
          PM --> BF["ActiveBuffList (統一Buff池)"]
      end
      
      subgraph SpatialWorld ["世界空間交互 (World Spatial Layer)"]
          PL["Player (空間代理 Actor / Spatial Proxy)"]
          PL -.->|動態委派隊長真實槽位| PM
          PL --> Room["Room 空間座標 / 拓撲導航"]
      end
  ```
- **落地實施成果 (2026-10-21)**：
  1. **背包雙寫清零 (Task 1)**：重構 `GetCommand` 與 `DropCommand`，拾取與丟棄 100% 只操作 `party.getInventory()`，徹底移除對 `player.getInventory()` 的同步雙寫；`DrpgBattleService` 穿裝自動消除個人隨身背包過渡物品；`WorldFactory.createCorpse` 玩家死亡掉落物品優先從小隊行囊提取。
  2. **裝備單一真相源 (Task 2)**：`MessageFactory` 裝備展示改為優先從小隊隊長真實裝備（`party.getLeader().getEquipment()`）讀取；`Player` 覆寫 `getMainHandWeapon()` 與 `getOffHandEquip()` 動態委派隊長真實法寶槽位。
  3. **同步服務純淨化 (Task 3)**：`CharacterSyncService` 確立為唯讀單向投影鏡像服務，消除雙向淺拷貝競爭；存讀檔管線確認僅存小隊行囊與成員裝備 (schemaVersion 3)。
  4. **專屬整合測試驗收 (Task 4)**：新增專屬整合測試 `UnifiedCharacterCoreTest.java`（5/5 綠燈），涵蓋拾取丟棄只進出小隊行囊零雙寫、主副手武器動態委派隊長真實槽位、MessageFactory 反映真實裝備、戰鬥穿裝直扣小隊行囊、存讀檔大一統驗證。全專案 348 項測試 100% 綠燈通過。

#### 2. F1-02：後端領域層 Service 包結構按業務模組拆分
- **現狀問題**：`com.example.htmlmud.domain.service` 平鋪 10+ 個跨領域服務，缺乏領域邊界。
- **重構方案**：按業務模組拆分為子包：
  - `domain.world`：`RoomService`、`WorldManager`、`RoomMovementService`
  - `domain.combat`：`CombatService`、`DrpgCombatLoop`、`BuffSettlementService`
  - `domain.party`：`PartyService`、`FormationService`
  - `domain.progression`：`XpProgressionService`、`SkillBridgeService`
  - `domain.economy`：`ShopCommand`、`InventoryService`

#### 3. ✅ [已完成] F1-03：看門狗棘輪清零 (26 處 fallback 歸零)
- **落地進度**：已於 2026-10-21 完成。
  1. 徹底消除全專案 `src/main/java` 中歷史殘留的 26 處 `new TemplateCatalog()` 靜默 fallback。
  2. 核心服務 (`PartyCommand`, `CharacterSyncService`, `DrpgBattleService`, `DrpgEnemyTacticsService`, `DrpgRewardService`, `PartyService`, `DrpgTemplateAdapter`, `MudTemplateAdapter`) 全面落實 `TemplateReader` 顯式建構子注入。
  3. 實體與視圖模型 (`BattleEnemy`, `PartyInventory`, `PartyMember`, `ShopTemplate`, `MessageFactory`, `DrpgStateDto`) 全面改為 `party.getTemplateReader()` 或傳入參數，消除隱式目錄實例化。
  4. `ArchitectureGuardrailsTest.java` 棘輪閾值由 26 降為 0，實現架構零破窗與 338 項測試 100% 綠燈通過。

#### 4. ✅ [已完成] F1-04：前端歷史樣式巨石 `style.css` (122 KB) 安全除役
- **落地進度**：已於 2026-10-21 完成。
  1. 驗證主遊戲 `index.html` 100% 運行於 `style-01` ~ `style-07` 七層樣式流。
  2. 將存檔備份頁面 `singleplayer_v1.html` 的 CSS 引用升級為模組化七層樣式。
  3. 物理安全刪除 122 KB 之死代碼巨石 `static/css/style.css`，手冊與架構指標全數同步。

#### 5. ✅ [已完成] F1-05：地牢生存火把明滅與步進機關陷阱 (Dungeon Torch & Step Traps, P1)
- **落地進度**：已於 2026-10-21 完成。
  1. 實裝 `torchLight` (0~100) 與照度階級 (`BRIGHT` >50, `DIM` 1~50, `DARKNESS` 0)，步進衰減、絕對黑暗每步扣 5 點 SAN 與 25% 突襲檢定。
  2. 支援 `dungeon light` / `dungeon torch` 行囊火把道具與消耗 10 靈石燃靈補光（戰鬥中亦能點火照明）。
  3. `DungeonNavigator` 動態射線探測（3步 $\to$ 2步 $\to$ 1步）與牆壁視線遮擋。
  4. 多元機關檢定：暗弩陷阱（先鋒身法 DEX $\ge$ 12 免傷）、靈泉地窟（+50 HP, +30 MP, +15 SAN 滋養）、鎖鏈暗門（臂力 STR $\ge$ 12 / 工具破拆獲戰氣）。
  5. 前端 `dungeon-panel.js` 雷達照度條、暗影暈影特效 (`.torch-darkness-vignette`) 與靈泉圖示 (`♨`) 渲染。
  6. 新增專屬整合測試 `DungeonTorchAndTrapTest`（5 項測試），全專案 343 項測試 100% 綠燈通過。

---

### 22.2 戰線二：戰鬥深度突破與動態沉浸博弈 (P1 ~ P2)

#### 1. F2-01：敵方行動預告蓄力條與即時反制打斷 (Combat React, COMBAT-REACT-01) ✅ [已完成]
- **機制設計**：
  - 怪物強力招式配置 `castTimeMs` 與 `interruptible` 標籤。
  - 怪物進入 `CASTING` 施法前搖，前端 FE-3 即時展示青藍色/猩紅色蓄力進度條。
  - 玩家或同伴 Gambit 在吟唱窗口內打出帶有 `STUN`（眩暈）、`SILENCE`（沉默）、`PARRY`（精準偏架）或高 Poise 破防招式，觸發失衡打斷（`⚡ 打斷!`），造成怪物破防並進入虛弱狀態。
- **落地實施成果 (2026-10-21)**：
  1. **模型擴充 (Task 1)**：`UnifiedSkill.java` 增加 `getCastTimeMs()` 與 `isInterruptible()` 介面方法；`BattleEnemy.java` 引入施法狀態枚舉 `CastingState` (`IDLE`, `CASTING`, `INTERRUPTED`, `COMPLETED`) 與 `lastBrokenCastingAction` 破防中斷追蹤。
  2. **戰鬥迴圈與狀態機 (Task 2)**：`DrpgCombatLoop` 與 `CombatFsmService` 支援控制標籤反制打斷 (`resolveSkillInterrupt`) 與罡氣破防打斷 (`resolvePoiseBreakInterrupt`)；怪物攻擊結算完備呼叫 `finishCast()`。
  3. **反制招式與戰況廣播 (Task 3)**：`party_skills.json` 實裝反制專屬技能 `taibai_interrupt`（太白斷空）；打斷成功即時推送 `HitOutcome.INTERRUPTED` 與彩色戰報。
  4. **前端視覺與專屬驗證 (Task 4)**：`battle-panel.js` 實裝動態蓄力條與可斷/霸體標籤；`floating-text.js` 與 `style-07-battle-fx.css` 實裝 `⚡ 打斷！` 浮動文字與 `.react-interrupted` 劇烈震動光效；編寫專屬整合測試 `CombatReactInterruptTest.java`（5/5 綠燈），全專案 353 項測試 100% 綠燈通過。

#### 2. F2-02：首領多階段血條與狂暴轉階段機制 (Boss Phases & Enrage)
- 首領氣血跌至 70%、30% 時觸發轉階段（Phase Transition）：清除當前負面 Debuff、更換狂暴招式池、場景光影異化並召喚護法雜兵。

#### 3. F2-03：十二時辰晝夜更迭與微氣候生態動態遇敵
- 導入子丑寅卯循環，時辰影響妖邪能力與世界視野；雨、霧、雪等微氣候動態影響五行元素親和與地牢雷達半徑。

#### 4. F2-04：道心異化深淵靈視 (Sanity Breakdown & Eldritch Sight)
- 突破純負面懲罰思維：SAN 值低於 30% 開啟「不可名狀靈視」，介面產生克系低語視界畸變，但能窺探隱藏維度的遠古古碑與特殊機關。

#### 5. F2-05：複合靈氣連鎖反應與狀態引爆 (Elemental Reactions & Primer/Detonator)
- 借鑒現代 ARPG 元素反應與引爆機制：冰火蒸汽爆裂（$2.0\times$ 穿透傷害）、雷水感電傳導、陰陽太極混沌湮滅（直接扣除 50 點 Poise 破防值引發破防失衡）；刺客施加裂魂印記 (Primer)，後續隊友施展重錘或飛劍作為引爆技 (Detonator) 瞬間結算剩餘 DoT 並產生全場衝擊波。詳見專屬技術手冊 [`後端_M3-1`](../manual/後端_M3-1_狀態效果與Buff結算模組_BUFF_AND_STATUS_EFFECTS.md)。

---

### 22.3 戰線三：小隊養成、武學演化與動態經濟 (P2)

#### 1. F3-01：古仙陣法動態連攜合擊技 (Cross-Member Combos)
- 前衛主坦格擋觸發「反震波」，與後衛法修真元聯動施展「九天玄煞天外合擊」，強化小隊協同手感。

#### 2. F3-02：境界突破天道雷劫試煉 (Tribulation Lightning)
- 等級瓶頸突破時需引動天劫，全隊面臨數波天雷轟擊，考驗防禦減傷、護盾分配與團隊治療能力。

#### 3. F3-03：隨機古神詞綴體系 (Procedural Affixes)
- 怪物與裝備支援前綴與後綴動態抽取（如 `【腐爛的】+【太陰附骨】+【玄鐵重劍】`），100% 資料驅動配置。

#### 4. F3-04：法寶注靈重鑄與裝備數值對比浮窗 (Stat Diff Tooltip)
- 換裝介面滑鼠懸停即時展示數值增減對比（綠色提升 / 紅色下降），支援消耗靈石與天材地寶強化注靈。

#### 5. F3-05：隊友自動戰術 Gambit 規則鏈引擎 (Companion Gambit AI Chains, P1)
- **機制設計**：
  - 借鑒 Final Fantasy XII 戰術 Gambit 系統：為 4 名招募夥伴（鐵牛、凌霜、墨衍、燕青）提供優先順序由高至低的條件觸發鏈。
  - 條件範例：`[隊友氣血 < 35%] ➔ [施放回春術]`、`[自身處於破防狀態] ➔ [服用金剛丹]`、`[目標處於吟唱蓄力中] ➔ [施放太白斷空 (打斷技)]`、`[前排隊友承傷過高] ➔ [先鋒施放金鐘護體]`。
  - 玩家可在單人小隊彈窗介面中自訂或切換「激進攻堅」、「沉著續航」、「破防打斷」等戰術預設，在半即時戰鬥中無需手忙腳亂切換 5 名隊員，亦可隨時手動指令介入。

#### 6. F3-06：城鎮夥伴招募奇遇與道心羈絆故事 (Companion Recruitment & Bonding, P2)
- **機制設計**：
  - 各夥伴擁有獨特的個人身世與好感度/羈絆任務鏈：鐵牛的宗門復仇、凌霜的寒毒反噬、墨衍的丹道執念。
  - 隨著好感度提升與專屬地牢暗室解鎖，夥伴將領悟隱藏被動秘傳（如墨衍在 SAN 低於 50 時觸發「瘋魔妙手」醫療暴擊），深化單機 RPG 人物塑造與陪伴感。

---

### 22.4 戰線四：現代視覺呈現與跨端互動體驗 (P2)

#### 1. F4-01：動態沉浸微粒子氛圍層 (Ambient Canvas Layer)
- 60 FPS 輕量 WebGL / Canvas 微粒子覆蓋主舞台：煞氣游絲、磷火明滅、劍氣流光，支援減弱動畫降級。

#### 2. F4-02：虛擬滾動情報日誌 (Virtualized Log Scrolling)
- 採用 Virtualized List 技術，僅渲染螢幕可視區的 30~40 筆 DOM 節點，突破 200 行截斷限制，記憶體消耗恆定。

#### 3. F4-03：自適應停靠 HUD 與多套主題熱切換 (Dockable HUD & Theme Switching)
- 隊伍卡牌支援水平/垂直自適應排列；CSS Tokens 支援一鍵熱更切換【暗黑克系】與【太虛水墨】主題。

#### 4. F4-04：原生 Gamepad API 掌機手柄支援與震動回饋 (Steam Deck / Xbox)
- 完整適配 Gamepad API：雙搖桿探索與鎖定、肩鍵切換隊員、震動回饋受擊與破防。

---

### 22.5 戰線五：純單機工程加固與本機維運 (P2 ~ P3)
> 💡 **單機單體決策**：已正式決議專注於「純單機單體 (Single-Player Modular Monolith)」，徹底除役過度設計的多人連線、分散式協定與動態外掛熱更。

#### 1. F5-01：Docker 一鍵容器化與 Dev Container 支援 (P3)
- 提供 `docker-compose.yml` 實現零環境門檻 60 秒開箱即用，整合 CI/CD 自動化看門狗門禁。

#### 2. F5-02：本機單機可觀測性與健康指標 (Local Observability) (P3)
- 透過 `DomainMetricsPort` 監控本機虛擬執行緒戰鬥循環活躍數、Write-Behind 批次排空耗時、本機存檔 JSON 寫入延遲。

#### 3. F5-03：本地多槽位安全雙寫與 .bak 崩潰自動復原 (Atomic Local Saves & Crash Recovery) (P2)
- **機制設計**：
  - 實作「先寫臨時檔 `slot_*.json.tmp` $\to$ 完成校驗 $\to$ 原生 Atomic Rename 覆蓋主檔」之防斷電安全寫入機制。
  - 主動在 `saves/backups/` 維護前 3 個版本的自動快照滾動備份（`.bak1`, `.bak2`, `.bak3`）。
  - 若偵測到存檔因非正常關機造成 JSON 結構殘缺，開機時自動啟動「存檔修復精靈」，無縫讀取最近完整 `.bak` 備份，保證玩家珍貴進度永不丟失。

#### 4. 🚫 歷史過度設計除役項目 (Decommissioned)
- **F5-EX-01 [已除役] 領域事件總線 (EDA Pub-Sub)**：純單機維持單體內同步呼叫與 Actor 信箱，不引入跨服務非同步事件匯流排。
- **F5-EX-02 [已除役] Protobuf 二進位串流 (Binary Pipeline)**：本機 WebSocket 維持簡潔標準的 JSON 傳輸，不引入二進位編解碼複雜度。
- **F5-EX-03 [已除役] 外部模組擴充 SPI (Dynamic Plugin Hot-Reload)**：全規則由 Java 機制與靜態 JSON 配置驅動，不引入動態 JAR 載入或腳本熱更。

---



## 📝 23. 文件權威來源、交叉一致性與三方審查全面整合治理 (Documentation, Code Convergence & Tri-Review Masterplan) — Priority: P0 ~ P2（待決議）

> **來源與背景**：  
> 本章節為專案三大外部審查報告的**總集成規劃樞紐**，全面整合：  
> 1. [`Codex_Review.md`](../references/Codex_Review.md)（2026-10-15）：文件 SSoT 權責、四級狀態標記、可玩垂直切片、避免無界資料化。  
> 2. [`Copilot_Review.md`](../references/Copilot_Review.md)（2026-10-15）：相對死鏈定位、心跳週期分層、schemaVersion 歷史清理、單機 vs 遠期邊界。  
> 3. [`Claude_Review.md`](../references/Claude_Review.md)（2026-10-20）：實地代碼交叉校驗（除役服務殘留清繳、`ScopedValue` 與 `schemaVersion=3` 同步、行囊容量裁定）、存檔原子替換、測試分層加速（下沉純單元測試）、日誌虛擬滾動與不合理項目否決審計。  
> 
> 旨在徹底消滅高速迭代中產生的「文檔膨脹與代碼漂移」，確立不可動搖的權威規格，並為後續純單機 DRPG 玩法落地提供清晰防線。

### 23.1 DOC-LINK-01：修復文件相對連結並建立連結自動檢查機制 — ✅ 已修復 (2026-10-16)
- 核對並修復 `FUTURE_IMPROVEMENTS.md` 中歷史審查稿引用（清理 9 處不存在的 `Copilot_Code_Review.md` 引用）與 8 處錯誤的相對代碼路徑（`../src/` $\to$ `../../src/`）。
- 建立本機或 CI Markdown 相對連結檢查腳本，防止後續文件移動引發死鏈。
- **驗收**：全專案 Markdown 檔案無失效相對連結；歷史審查稿具備有效連結或清楚標示已除役。

### 23.2 DOC-SSOT-01：定義單一權威來源 (SSoT) 與四級狀態標籤規範
- **唯一權威文件職責劃分**：
  - `GEMINI.md`：專案最高憲法、第一紅線、不可動搖保護鎖（任何規範衝突以此為準）。
  - `ACTIVE_TASKS.md`：唯一當前迭代執行微任務看板（Checkbox 工作流）。
  - `ARCHITECTURE.md`：現行系統邊界、Clean Architecture、DDD 與無鎖並發唯一來源。
  - `docs/manual/`：經核對且已實作之模組契約、API、DTO 與配置說明（不維護獨立未來路線圖）。
  - `docs/overview/`：全域導覽指引，不定義第二套規格。
  - `docs/plans/FUTURE_IMPROVEMENTS.md`：候選改善、未決議提案與 P0~P3 演進總待辦唯一來源。
  - `CHANGELOG.md`：歷史交付歷程、重大重構與驗收證據唯一來源。
  - `docs/references/`：外部靈感與非規範性參考審查報告（未決議前不具規範力）。
- **四級內容狀態標記**：
  - 【現行已驗證】(Verified Live Spec)：具備代碼與測試 100% 綠燈支撐之現行規格。
  - 【已決議待實作】(Decided Backlog)：已獲決議並拆解於 `ACTIVE_TASKS.md` 之迭代任務。
  - 【候選提案】(Candidate Proposal)：收錄於本文件待排期或技術討論之提案，採將來式語態。
  - 【歷史／已除役】(Historical / Decommissioned)：已除役之 legacy 代碼或過渡設計。
- **驗收**：跨文件事實皆能對齊單一來源；手冊與概覽全面標註狀態標籤。

### 23.3 DOC-FACT-01：【P0 緊急】消除已除役服務殘留與代碼現況同步 (Decommissioned Residue & Fact Synchronization) — ✅ 已完成 (2026-10-20)
- **背景與痛點**：
  在 Path X (SP-01 ~ SP-07) 演進中，大量膠水代碼與雙軌實體已被重構或物理刪除，但多份核心架構文件與模組手冊未能及時同步，造成嚴重「文檔描述了已刪除代碼」的誤導風險。
- **具體整治行動清單**：
  1. **清除 `SkillBridgeService` 歷史殘留**：
     - 代碼於 SP-07 已徹底刪除 `SkillBridgeService.java`，改為大一統技能模型 [`UnifiedSkill`](../../src/main/java/com/example/htmlmud/domain/party/model/UnifiedSkill.java) 原地升級與縮放。
     - 必須全面修訂 [`ARCHITECTURE.md:119`](../../ARCHITECTURE.md#L119)、[`MODULES_MAP.md:125`](../overview/MODULES_MAP.md#L125)、[`後端_M5:27`](../manual/後端_M5_技能境界與修煉模組_SKILL_AND_PROGRESSION.md#L27) 與 [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md)，移除所有 `SkillBridgeService` 陳述，指引至 `UnifiedSkill` 與 `PartyMember.skills`。
  2. **同步 `MudContext` 為 Java 25 `ScopedValue`**：
     - 代碼 [`MudContext.java`](../../src/main/java/com/example/htmlmud/domain/context/MudContext.java) 實際已全面升級為 Java 25 `ScopedValue<Player>`。
     - 修訂 [`後端_M9:48`](../manual/後端_M9_虛擬執行緒Actor通訊模組_ACTOR_AND_PROTOCOL.md#L48)，糾正過時的「基於 `InheritableThreadLocal`」描述，補充說明 `ScopedValue.runWhere(...)` 之不可變與安全無洩漏生命週期。
  3. **同步 `SaveData` 為 `CURRENT_SCHEMA_VERSION = 3`**：
     - 代碼 [`SaveData.java:21`](../../src/main/java/com/example/htmlmud/domain/save/model/SaveData.java#L21) 於 SP-03 已升級至 v3（移除 `personalInventory` 雙寫）。
     - 修訂 [`後端_M8:41`](../manual/後端_M8_存檔與歷史快照模組_SAVE_AND_PERSISTENCE.md#L41) 範例 JSON 為 `"schemaVersion": 3`，並在手冊中明確記載 `SaveMigrationPipeline` 之 2 $\to$ 3 遷移路徑與個人物品自動合流入公共行囊機制。修訂 [`ARCHITECTURE_SUMMARY.md:94`](../overview/ARCHITECTURE_SUMMARY.md#L94) 之版本描述。
  4. **全域校準測試基準數字為 324 項全綠燈**：
     - 全專案目前實測基準為 **324 項測試 100% 綠燈**（78 個測試類別）。
     - 同步更新 [`GEMINI.md:144`](../../GEMINI.md#L144) 與 [`QUICKSTART.md:33`](../overview/QUICKSTART.md#L33) 記載之 304 項滯後數字。
- **驗收**：全專案無任何指向已刪除類別（如 `SkillBridgeService`）之失效文檔指針；所有類別技術規格與實際代碼 100% 吻合。

### 23.4 DOC-ENUM-01：【P1 重要】全域遊戲詞彙表分層與裝備枚舉對齊 (Canonical Vocabulary & Enum Alignment) — ✅ 已完成 (2026-10-20)
- **落地改動成果 (2026-10-20)**：
  1. **裝備槽位枚舉全域對齊 (5+2 結構)**：
     - 代碼唯一真相源為 [`EquipmentSlot.java`](../../src/main/java/com/example/htmlmud/domain/model/enums/EquipmentSlot.java)：`MAIN_HAND, OFF_HAND, HEAD, BODY, FEET, ACCESSORY_1, ACCESSORY_2`。
     - 修訂 [`後端_M6:27-46`](../manual/後端_M6_物品背包與經濟模組_ITEM_AND_ECONOMY.md#L27-L46)、[`前端_FE5:41-62`](../manual/前端_FE5_小隊裝備與存檔彈窗_PARTY_EQUIPMENT_AND_MODALS.md#L41-L62)、[`UI_PAGE_STRUCTURE.md:324`](../UI_PAGE_STRUCTURE.md#L324) 與 [`ARCHITECTURE_SUMMARY.md:56`](../overview/ARCHITECTURE_SUMMARY.md#L56)，徹底清除殘留之舊版 `EquipSlot`（`ACCESSORY, RING, WEAPON`）陳述，確立雙飾品（本命法寶/輔佐靈寶）標準。
  2. **戰鬥概念四層分離字典 (Combat Canonical Taxonomy)**：
     - 於 [`後端_M3:88-160`](../manual/後端_M3_核心戰鬥引擎模組_COMBAT_ENGINE.md#L88-L160) 正式確立四層架構專章，終結過去元素與傷害類型混淆：
       - **第 1 層：底層傷害類型 [`DamageType`](../../src/main/java/com/example/htmlmud/domain/model/enums/DamageType.java)**（20 種繼承樹：`PHYSICAL` 派生 `SLASH/PIERCE/BLUNT/METAL`；`MAGIC` 派生 `FIRE/ICE/WATER/LIGHTNING/WIND/EARTH/WOOD/POISON/HOLY/DARK`；特殊 `SONIC/TRUE`）。
       - **第 2 層：天道元素家族 (九大靈氣)**（五行天道：金木水火土；乾坤氣象：風雷；陰陽造化：陰煞純陽；對應抗性與「順成人逆成仙」> 100% 逆流回血契約）。
       - **第 3 層：狀態效果標籤 (Status / Skill Mechanism Tags)**（控場打斷矩陣：`STUN, SILENCE, SEAL, CONTROL, INTERRUPT`；戰術行為：`ATTACK, SPELL, HEAL, DEFENSE, AOE, TAUNT, CLEANSE, DISPEL, PARRY`）。
       - **第 4 層：底層 Buff 分類 [`BuffCategory`](../../src/main/java/com/example/htmlmud/domain/model/enums/BuffCategory.java)**（引擎底層結算生命週期：`SHIELD, HOT, DOT, STAT_MODIFIER, CONTROL`）。
  3. **時間心跳層級排程字典 (Timing & Heartbeat Hierarchy)**：
     - 在 [`後端_M3:11-45`](../manual/後端_M3_核心戰鬥引擎模組_COMBAT_ENGINE.md#L11-L45) 與 [`後端_M3-1:50`](../manual/後端_M3-1_狀態效果與Buff結算模組_BUFF_AND_STATUS_EFFECTS.md#L50) 校準戰鬥迴圈心跳為 **500ms**（修正錯誤的 200ms）。
     - 清楚劃分階層：`WorldPulse.pulse()`（底層定時器 100ms 輪詢）$\to$ `DrpgCombatLoop`（戰鬥虛擬執行緒迴圈 500ms）$\to$ `BuffSettlementService`（狀態結算 1 Tick = 500ms）$\to$ 世界生靈刷新（3,000ms）。
- **驗收**：手冊對裝備槽位、傷害類型、元素靈氣、狀態標籤與排程頻率之描述 100% 吻合代碼現況，零模糊歧義。

### 23.5 DOC-CAP-01：【P1 重要】小隊公共行囊容量全域標準裁定 (Party Inventory Capacity Canonicalization) — ✅ 已完成 (2026-10-20 拍板採方案 A)
- **現狀差距與裁定結果**：
  - 原代碼實作 `DEFAULT_CAPACITY = 30;`，部分手冊記錄為 40 格，造成跨文件與代碼常數漂移。
  - **決策結果**：**已拍板採方案 A（全域統一升級為 40 格）**。
    - 理由：5 人滿編小隊擁有 5×(5+2)=35 個裝備槽位，40 格行囊提供地牢冒險、刷寶掉落與備用裝備更寬裕流暢的冒險體驗。
    - 落地改動：[`PartyInventory.java`](../../src/main/java/com/example/htmlmud/domain/party/model/PartyInventory.java) 常數升級為 `DEFAULT_CAPACITY = 40;`；同步更新前端 `index.html`、`bag-drawer.js`、`後端_M6`、`前端_FE1`、`前端_FE5`、`QUICKSTART.md`、`README.md` 與全域手冊。
- **驗收**：代碼常數、UI 渲染上限、所有文檔與測試斷言格數 100% 同步一致為 40 格。

### 23.6 DOC-WS-01：【P1 重要】前端 WebSocket 通訊實況對齊與健壯性重構 (Frontend WebSocket Modularization & Reconnect) — ✅ 已完成 (2026-10-20)
- **現狀差距**：
  - 手冊 [`前端_FE1`](../manual/前端_FE1_核心狀態與事件管線_CORE_STATE_AND_PIPELINE.md) 宣稱存在獨立模組 `static/js/network/websocket.js`，且具備 15 秒文字 Ping 與指數退避重連。
  - 實際上連線由早期 `index.html` 內嵌腳本建立，斷線重連邏輯被註解，且無 Ping 機制。
- **落地改動成果 (2026-10-20)**：
  1. **獨立模組抽離**：新建 [`src/main/resources/static/js/network/websocket.js`](../../src/main/resources/static/js/network/websocket.js)，封裝連線建立、指令序列化發送、被動斷線監聽與向後相容掛載（`window.send = sendWsCommand; window.connect = connectWebSocket;`）。
  2. **指數退避斷線重連**：實裝健全的退避重試演算法（$1\text{s} \to 2\text{s} \to 4\text{s} \to 8\text{s} \to 15\text{s}$ 上限），斷線時友好提示倒數秒數；連線平滑恢復時主動發送 `saves quiet` 刷新存檔與世界最新狀態。
  3. **模組化入口整合**：在 [`app.js`](../../src/main/resources/static/js/app.js) 的 `setupApp()` 啟動連線，並於 [`cmd-dispatcher.js`](../../src/main/resources/static/js/core/cmd-dispatcher.js) 優先導入 `sendWsCommand`。
  4. **嚴格 CSP 規範達標**：徹底移除 [`index.html`](../../src/main/resources/static/index.html) 尾部 33 行內嵌 `<script>` 區塊，完全消除內嵌網路連線腳本。
  5. **單機心跳合理化**：恪守架構決策二與純單機單體定位，除役本地 15 秒文字 Ping，改為被動依賴原生協議與 `onclose` 事件，達成零 CPU 浪費之事件驅動架構。
  6. **手冊權威對齊**：已同步修訂技術手冊 [`前端_FE1`](../manual/前端_FE1_核心狀態與事件管線_CORE_STATE_AND_PIPELINE.md) 第 4 節與代表檔案清單。
- **驗收**：`index.html` 零內嵌連線腳本；後端 324 項測試 100% 綠燈；前端通訊管線模組化與重連邏輯完整具備。

### 23.7 DOC-ROADMAP-01：收斂 16 本模組手冊重複路線圖至中央總待辦
- **痛點**：16 本模組手冊各自在第 6~9 節維護數百行的「未來架構演進規劃」，與中央總待辦本文件大量重複，更新時極易顧此失彼。
- **方針**：
  - 將 16 本手冊中的未來規劃收斂精簡為指向本文件的超連結與一句話定位。
  - 模組手冊專注於：職責邊界、現行契約、核心類別、JSON 結構與驗收測試依據。
- **驗收**：所有待辦項目統整至本文件，模組手冊行數大幅精簡，不再作為第二個路線圖來源。

### 23.8 DOC-SLICE-01：玩法垂直切片驅動與純單機單體規劃
- 堅持「專注於一個人的純單機單體遊戲」架構決策。
- 圍繞核心五階段垂直切片閉環推進：
  $$\text{城鎮整備} \to \text{迷宮步進} \to \text{魂系博弈戰鬥} \to \text{戰利品成長} \to \text{安全撤離／存檔}$$
- 每項新玩法設計明確規定：玩家目標、操作選擇、失敗代價、視覺回饋與可觀察的 DoD（如「可打斷窗口為前搖前 60%」）。
- 無障礙、弱網復原與 `@media (prefers-reduced-motion)` 列為互動機能一級需求。
- **驗收**：所有新增玩法規劃具備完整垂直切片說明與可量測驗收。

### 23.9 DOC-SYNC-01：易變數值快照化與自動代碼產生／檢查
- 測試總數、檔案大小、效能百分比等易變數據附帶測量日期與環境依據，未實測者明確標記為目標。
- 評估後續透過自研測試工具或腳本自動校驗 DTO、枚舉與文檔表格之一致性。
- **驗收**：關鍵易變數值具備測試依據或自動生成機制。

### 23.10 DOC-REJECT-01：【架構紅線】三方審查不合理／過度設計項目明確否決宣告 (Decommissioned & Rejected Items Audit)
經由審慎評估，為確保團隊專注於純單機極致樂趣，以下來自歷史參考文件或外部建議的項目被正式**否決／確認除役**，不納入待辦事項：
1. ❌ **Groovy / Lua 外部動態腳本熱更 SPI**（見於早期草稿 `game-config-architecture.txt`）：
   - **否決原因**：專案已確立以 Java 25 現代特性與靜態 JSON 為核心。動態腳本引擎（如 GraalVM）會引入類別加載洩漏、執行緒上下文穿透與難以除錯的複雜度，嚴重違背「拒絕過度設計」之專案第一鐵律。
2. ❌ **多人在線、跨服同步、Redis Pub/Sub、Protobuf 二進位串流**：
   - **否決原因**：專案已全面定調為「純單機單體 DRPG」。本機 WebMVC REST API 與 WebSocket JSON 足夠輕量高效，無需承擔分散式系統的網路複雜度。
3. ❌ **無界資料化 / 萬能 JSON 直譯器 (Unbounded JSON Parser)**：
   - **否決原因**：恪守專案第一紅線「機制由引擎通用實作，資料只管數值與標籤」。避免試圖在 JSON 內部實作圖靈完備的自製虛擬機或通用狀態機解析器，避免系統淪為不可維護的黑盒。
4. ❌ **單機 Localhost 高頻 15 秒文字 Ping/Pong 保活**：
   - **否決原因**：單機環境下本機 WebSocket 連線無雲端網關超時問題，高頻 Ping 徒增虛擬執行緒無效喚醒。聚焦於斷線自動重連與會話接管即可。

---


