# 專案版本演進與變更紀錄 (Changelog & Historical Migration Log)

> **文件定位**：  
> 本文件作為專案所有歷史重構、功能上線、架構加固與缺陷修復的**唯一歷史紀錄中樞**。  
> 遵循「每次項目新增、變更或重構落地時必登載」的文檔治理鐵律，記錄專案自開拓以來的演進足跡。

---

## 📌 最新里程碑 (Latest Releases)

### [2026-10-21] Milestone F3-05：隊友自動戰術 Gambit 規則鏈引擎 (Companion Gambit AI Chains, P1)
- **Gambit 領域模型與資料驅動條件/動作枚舉 (Task 1)**：
  - 新增領域枚舉 `GambitActionType` (`SKILL`, `ITEM`, `DEFENSE`, `ATTACK`)，擴充 `TacticsRule` 支援 `actionType` 與 `itemId`。
  - 擴充 `TacticsCondition` 新增 `ENEMY_IS_CASTING`、`ENEMY_CASTING_INTERRUPTIBLE`、`ENEMY_POISE_BROKEN`、`ENEMY_HP_LESS_THAN`、`ALLY_SAN_LESS_THAN`。
  - 擴充 `TacticsTarget` 新增 `CASTING_ENEMY`、`POISE_BROKEN_ENEMY`、`LOWEST_HP_ENEMY`。
  - 建立領域模型 `GambitRule` 並提供便利工廠方法（`ofInterrupt`、`ofHeal`、`ofEmergencySan`、`ofPoiseBrokenBurst`）；`PartyMember` 增加 `gambitEnabled` (預設 `true`) 開關，`initDefaultTactics` 自動將反制打斷技能配置為 Priority 1 最高優先級（目標為 `CASTING_ENEMY`）。
- **戰術條件評估器與動態目標解析 (Task 2)**：
  - `DrpgEnemyTacticsService.evaluateTacticsCondition` 補齊新增戰術條件求值邏輯；新增 `resolveEnemyTarget` 智慧解析蓄力吟唱中敵怪、破防失衡弱點怪與最低氣血怪；擴充 `resolveAllyTarget` 支援道心受損隊友精準鎖定。
  - `DrpgCombatLoop` 在 `tryTriggerCompanionTactics` 中加入 `!member.isGambitEnabled()` 守護，並在單體傷害招式中動態綁定戰術目標。
- **資料驅動戰術範本庫與指令擴充 (Task 3)**：
  - 新建預設戰術範本庫 `data/party/gambit_presets.json`（收錄【斷法攻堅】、【急救續航】、【守禦護坦】、【破防爆發】4 大經典方針），建立 `GambitPreset` 模型。
  - `TemplateRepository` 與 `TemplateCatalog` 自動快取載入，落實唯一真相源 (SSoT)。
  - `PartyMember.applyGambitPreset` 支援依同伴技能標籤智慧適配策略鏈；`PartyCommand` 擴充 `party tactics <idx> gambit on|off` 與 `party tactics <idx> preset <id>|list`。
- **前端配置介面與專屬測試驗證 (Task 4)**：
  - 前端 `party-tactics-tab.js` 實裝 Gambit 自動戰術總開關狀態徽章、快速切換按鈕、戰術範本庫下拉選單與新增條件/目標選項。
  - 編寫專屬整合測試 `CompanionGambitEngineTest.java`（4/4 綠燈），全專案 357 項測試 100% 綠燈通過。

### [2026-10-21] Milestone F2-01：敵方行動預告蓄力條與即時反制打斷 (Combat React, COMBAT-REACT-01, P1)
- **技能與敵方行動前搖/可打斷模型擴充 (Task 1)**：
  - `UnifiedSkill.java` 介面擴充 `getCastTimeMs()` 與 `isInterruptible()` 預設契約，大一統修煉與戰鬥技能的施法屬性。
  - `BattleEnemy.java` 引入施法狀態枚舉 `CastingState` (`IDLE`, `CASTING`, `INTERRUPTED`, `COMPLETED`)、`currentCastingSkillId` 與 `lastBrokenCastingAction`，提供嚴謹的狀態流轉與破防被打斷動作追蹤。
- **戰鬥迴圈吟唱窗口推進與打斷判定 (Task 2)**：
  - `DrpgCombatLoop.java` 在敵方行動前搖時推進蓄力進度，並於普通攻擊與技能結算中全面導入 `resolveSkillInterrupt` 控制標籤打斷與 `resolvePoiseBreakInterrupt` 罡氣破防打斷機制。
  - 怪物蓄力攻擊結算落實 `finishCast()` 轉為 `COMPLETED`，確保未被打斷的蓄力招式能精準釋放。
- **玩家與小隊反制技能與戰況廣播 (Task 3)**：
  - `party_skills.json` 新增專屬反制打斷技能 `taibai_interrupt`（太白斷空，造成 160% 穿刺傷害並帶有 `INTERRUPT` 與 `SILENCE` 標籤）；`SkillDefinition.java` 擴充 `taibai_sword` 橋接映射。
  - 結算受擊時發布帶有 `HitOutcome.INTERRUPTED` 的戰鬥事件與 ANSI 高亮彩色戰報，即時告知怪物罡氣潰散與失衡力竭。
- **前端蓄力條特效渲染與專屬測試驗證 (Task 4)**：
  - 前端 `battle-panel.js` 動態渲染青藍/猩紅蓄力條、可斷/霸體標籤與剩餘倒數秒數；`floating-text.js` 彈出高亮金黃色 `⚡ 打斷！` 浮動文字。
  - `battle-fx-director.js` 與 `style-07-battle-fx.css` 實裝 `.react-interrupted` 劇烈震動與雷電受擊光效。
  - 新增專屬整合測試 `CombatReactInterruptTest.java`（5/5 綠燈），全專案 353 項測試 100% 綠燈通過。

### [2026-10-21] Milestone F1-01：Player 徹底降級為純空間代理與實體雙寫徹底清零 (Unified Character Core, P1)
- **Player 背包與物品操作委派化與雙寫清零 (Task 1)**：
  - 重構 `GetCommand.java` 與 `DropCommand.java`，拾取與丟棄物品 100% 只操作 `party.getInventory()`，徹底移除歷史殘留對 `player.getInventory()` 的同步雙寫，徹底根除雙軌假帳。
  - `PartyItemSlot.java` 新增領域狀態輔助方法 `isEmpty()`，提升槽位有效性判讀純度。
  - `DrpgBattleService.java` 重構穿戴裝備邏輯，保留使用個人隨身物品穿戴時自動流轉至小隊裝備、消除個人物品之過渡機制。
  - `WorldFactory.createCorpse` 玩家死亡掉落物品優先從小隊公共行囊提取，若無隊員則安全 fallback。
  - 更新 `DualTierInventoryAndStashTest.java` 契合 F1-01 規範，驗證拾取散落物品直入小隊行囊。
- **裝備槽位單一真相源落地與動態委派 (Task 2)**：
  - `MessageFactory.java` 裝備展示清單優先從小隊隊長真實裝備（`party.getLeader().getEquipment()`）讀取，不再讀取 `player.getStats().getEquipment()` 投影。
  - `Player.java` 覆寫 `getMainHandWeapon()` 與 `getOffHandEquip()`，優先動態委派小隊隊長真實法寶槽位，確保戰鬥反擊、破防與屬性判定與真實佩戴裝備完全一致。
- **CharacterSyncService 邊界純淨化與單向投影 (Task 3)**：
  - 重構 `CharacterSyncService.java` Javadoc 確立空間代理單一真相源邊界，確認其僅作為唯讀單向投影鏡像服務，消除雙向淺拷貝競爭。
  - 驗證 JSON 存讀檔管線純淨維持 `Party.inventory` 與 `PartyMember.equipment` (schemaVersion 3)，不持久化空間代理私有集合。
- **專屬大一統整合測試與全量 348 測試回歸 (Task 4)**：
  - 編寫專屬整合測試 `UnifiedCharacterCoreTest.java`（5/5 綠燈），涵蓋：
    1. `testPickAndDropOperatesOnlyOnPartyInventory`: 拾取與丟棄僅進出小隊公共行囊，`player.getInventory()` 零雙寫。
    2. `testPlayerWeaponsDelegateToPartyLeader`: 主手武器與副手法寶動態委派小隊隊長真實槽位。
    3. `testMessageFactoryEquipmentReflectsPartyLeader`: 裝備展示訊息 100% 反映小隊隊長真實佩戴法寶。
    4. `testDrpgEquipConsumesPartyInventoryDirectly`: 戰鬥穿裝直接消耗小隊公共行囊，不產生個人假帳。
    5. `testSaveAndLoadMaintainsSingleSourceOfTruth`: 存檔與讀檔管線維持 `Party.inventory` 為唯一真相源。
  - 全專案 348 項測試 100% 綠燈全數通過 (`Tests run: 348, Failures: 0, Errors: 0, Skipped: 0`)。

### [2026-10-21] Milestone F1-05：地牢生存火把明滅與步進機關陷阱 (Dungeon Torch & Step Traps, P1)
- **地牢火把光明度核心模型與步進消耗機制 (Task 1)**：
  - `DungeonPosition.java` 實裝 `torchLight`（0~100 照度值）與照度階級枚舉 `TorchStatus`（`BRIGHT` >50, `DIM` 1~50, `DARKNESS` 0）。
  - `DungeonCommand.java` 步進自動扣減 1 點光明度；`DIM` 狀態警戒靈壓加速 1.5 倍；`DARKNESS` 狀態警戒加速 2.0 倍、每步扣減全員 5 點 SAN 值並觸發 25% 暗影妖邪突襲。
  - 實裝 `dungeon light` / `dungeon torch`：優先消耗行囊火把道具補滿照度，亦可消耗 10 靈石燃靈補光，且支援在戰鬥中緊急點火照明。
- **光明度動態視距與射線阻擋遮擋 (Task 2)**：
  - `DungeonNavigator.java` 動態多步射線探測：`BRIGHT` 探測 3 步、`DIM` 探測 2 步、`DARKNESS` 僅探測 1 步，且遇牆壁自動阻斷前方視野遮擋。
  - `DrpgStateDto.java` 與 `DungeonViewDto` 擴充 `torchLight`、`torchStatus`、`torchIcon` 動態同步至前端。
- **地牢多元機關陷阱與先鋒屬性檢定 (Task 3)**：
  - 地牢資料拓撲加入靈泉地窟（`P`, 太玄清心靈泉）與淬毒暗弩陷阱（`^`）。
  - 暗弩機關觸發小隊先鋒（如鐵牛）身法 (DEX >= 12) 敏捷規避檢定，成功全員無傷，失敗先鋒受重創全員扣 SAN。
  - 靈泉地窟未枯竭時自動滋養全員（+50 HP, +30 MP, +15 SAN）並標記枯竭狀態；鎖鏈暗門支援臂力 (STR >= 12) 破拆與工具破門獲得戰氣。
- **前端雷達照度視覺化與全量驗證 (Task 4)**：
  - `dungeon-panel.js` 實裝火把狀態 badge、照度漸層進度條、即時百分比與【🔥 點火】按鈕；在絕對黑暗狀態為地牢面板即時疊加暗紫色邊緣暗影暈影特效 (`.torch-darkness-vignette`)。
  - 地塊渲染擴充靈泉地窟（`♨`，天藍色微光樣式）。
  - 編寫專屬整合測試 `DungeonTorchAndTrapTest`（5 項測試 100% 綠燈），全專案 343 項測試全數綠燈通過。

### [2026-10-21] Milestone F1-04：前端歷史樣式巨石 `style.css` (122 KB) 安全除役
- **歷史樣式巨石物理移除**：
  - 物理刪除根目錄 122 KB 巨石樣式 `static/css/style.css`，全系統 100% 運行於 `style-01` ~ `style-07` 七層樣式串流。
  - 存檔歷史頁面 `singleplayer_v1.html` 同步對齊為引用七層樣式流。
  - 同步更新前端技術手冊 `前端_FE6` 與 `UI_PAGE_STRUCTURE.md`，全專案 338 項測試全綠通過。

### [2026-10-21] Milestone F1-03：看門狗棘輪全面清零，消除 26 處 `new TemplateCatalog()` 靜默 fallback
- **架構看門狗棘輪全面歸零 (`ArchitectureGuardrailsTest`)**：
  - `ArchitectureGuardrailsTest.java` 中的 `new TemplateCatalog()` 違規檢測閾值正式由 26 降為 0 (`.isLessThanOrEqualTo(0)`)，達成專案代碼庫中零靜默 Fallback 歷史性突破。
- **Spring 核心託管服務與命令注入加固 (Task 1)**：
  - 重構 `PartyCommand`, `CharacterSyncService`, `DrpgBattleService`, `DrpgEnemyTacticsService`, `DrpgRewardService`, `PartyService`, `DrpgTemplateAdapter`, `MudTemplateAdapter`，徹底移除無參或 fallback 建構邏輯，強制落實 `TemplateReader` 顯式依賴注入。
- **實體、模板與視圖模型解耦 (Task 2 & Task 3)**：
  - `BattleEnemy.java`：移除無 `TemplateReader` 的 4 參數過載，消除內部私建目錄。
  - `PartyInventory.java`：消除 `createFromTemplate` 與 `getTemplateReader` 中的目錄實例化，改以 `party.getTemplateReader()` 與安全 fallback 處理。
  - `PartyMember.java`：`getTemplateReader()` 回歸純屬性讀取，`getClassTemplate()` 與被動心法全面導入空防禦。
  - `ShopTemplate.java`：移除 3 處無參 `getEffective*` 方法，全面依賴 `templateReader` 顯式傳入。
  - `MessageFactory.java`：`mobDetail` 徹底移除 fallback 目錄實例化，引入安全流式投影防禦。
  - `DrpgStateDto.java`：7 處 `new TemplateCatalog()` 全數替換為 `party.getTemplateReader()` 與顯式傳遞，並加固套路/心法流式映射防禦。
- **測試體系與全量回歸驗證 (Task 4)**：
  - 更新 10+ 個單元/整合測試顯式傳入 `templateRepository`/`templateReader`。
  - 全專案 338 項測試 100% 綠燈全數通過。

### [2026-10-21] Milestone F1-02：後端領域層 Service 包結構按業務模組收斂
- **後端領域層模組化重構**：
  - 修為與境界服務收斂至 `domain.progression.service` (`XpProgressionService`, `XpService`)。
  - 核心戰鬥引擎服務收斂至 `domain.combat.service` (`CombatService`, `SkillService`, `SkillCostService`, `BodyPartSelector`, `TargetSelector`, `perform/*`)。
  - 生靈主體與角色服務收斂至 `domain.actor.service` (`PlayerService`, `MobService`, `LivingService`, `LivingStateService`, `CharacterSyncService`)。
  - 範本目錄與適配器收斂至 `domain.repository` (`TemplateCatalog`, `DrpgTemplateAdapter`, `MudTemplateAdapter`)。
  - 同步更新架構圖譜 `ARCHITECTURE.md`，338 項測試全綠通過。

- **建立文件審查報告 (`Copilot_Review.md`)**：
  - 審查 `docs/` 現有 Markdown 文件及指定的 `ACTIVE_TASKS.md`、`ARCHITECTURE.md`、`UI_PAGE_STRUCTURE.md`。
  - 彙整權威來源、規格狀態、交叉一致性、可驗收遊戲規劃、可擴充性及文件自動檢查建議；本報告屬候選建議，不代表設計已核准。
- **建立候選治理路徑 W**：
  - 在 `FUTURE_IMPROVEMENTS.md` 新增文件連結、權責／共同詞彙、roadmap 收斂及文件同步治理待辦，等待後續決議與驗收。

### [2026-10-15] 全專案深入審查、紅線清理與地牢出入口配置化 (P0/P1 治理落地)
- **裝備槽位中文推測硬編碼徹底清零 (`EQUIP-SLOT-CLEAN-01` - P0)**：
  - 徹底移除 `party-equipment-tab.js` 中 `name.includes('冠')`、`'甲'`、`'靴'`、`'戒'` 等中文名稱推測邏輯，嚴格依賴後端 `ItemTemplate` 的 `equipSlot` 枚舉，落實專案第一紅線。
- **地牢出入口與地貌通用化 (`DATA-03-REVISIT` - P1)**：
  - 在 `DungeonFloor`、`DungeonFloorLoader` 與地牢 JSON（`mozhu_mines_b1f.json`、`taiyin_tomb_b1f.json`）新增 `exitRoomId` 欄位配置。
  - 重構 `DungeonCommand.java` 撤離邏輯，以 `floor.getExitRoomId()` 動態導向取代寫死 `if-else` 分支。
  - 重構 `dungeon-panel.js` 的 `populateTileAppearance`，去除太陰古塚專屬名稱綁定，支援通用圖塊 Tooltip 並補齊向上階梯（`<`）圖標提示。
- **商店客棧名稱比對與偽商店 Fallback 移除 (`SHOP-FALLBACK-01` - P1)**：
  - 移除 `ShopCommand.java` 中 `if (roomId.contains("inn"))` 的硬編碼 fallback，全面由 `TemplateReader.findShopByRoomId(roomId)` 提供資料驅動查詢。
- **死代碼清理與日誌標準化 (`DEADCODE-01` - P2)**：
  - 刪除 100% 註解的冗餘類別 `PlayerLoginListener.java`。
  - 將 `QuestEventListener.java` 的 `System.out.println` 替換為標準 `@Slf4j` `log.debug`。
  - 移除 `index.html` 標題選單中 4 處過渡期 inline `onclick` 屬性，回歸純 `data-action` 事件委派。
- **核心架構文件同步修復 (`DOC-CONSISTENCY-01` - P0)**：
  - 更新 `FUTURE_IMPROVEMENTS.md`：記錄 304 項綠燈基準，補齊路徑 U 與 §21 綜合治理藍圖。
  - 更新 `UI_PAGE_STRUCTURE.md`：重寫第 4 節盤點，消除與已實裝模組矛盾的歷史紅燈。
  - 更新 `ARCHITECTURE.md`：目錄樹補齊 `domain.combat`、`domain.port`、`domain.event` 等子包與前端最新模組。
- **看門狗棘輪門檻收斂 (`FALLBACK-02` - P2)**：
  - 重構 `PartyInventory`、`SkillBridgeService`、`DrpgEnemyTacticsService`、`XpProgressionService`、`DrpgTemplateAdapter`、`MudTemplateAdapter` 建構子，消除多處冗餘的空 Catalog 實例化。
  - 將 `ArchitectureGuardrailsTest` 中 `new TemplateCatalog()` 的棘輪上限自 33 處收斂至 26 處（減幅達 21.2%）。
- **通用任務模型與劇情資料驅動落地 (`QUEST-DATA-01` - P3)**：
  - 建立通用領域模型 `QuestTemplate` 與設定檔 `quests.json`。
  - 擴充 `TemplateReader.findQuestByTriggerMob()` 與 `TemplateRepository` 自動加載機制。
  - 重構 `MozhuMinesQuestListener.java`，徹底消除硬編碼的 25 行台詞與首領特例比對，劇情台詞全面回歸資料驅動解算。
- **H2 實體雙持久化收斂 (`PERSIST-01` - P3)**：
  - 將 `CharacterEntity.inventory` 正式標記為 `@Deprecated`，確立單機存檔單一真相源為 `SaveData` JSON 與 `SaveGameService`。
- **測試套件 100% 綠燈全數通過**：
  - 全專案 304 項單元與整合測試全數通過（0 失敗、0 錯誤），架構看門狗測試與業務邏輯無任何回歸破壞。

### [2026-10-14] 專案紅線治理、看門狗護欄與實體深拷貝加固 (GUARD-02 & DEEPCOPY-01)
- **實體深拷貝加固 (LivingStats.deepCopy)**：
  - 補齊 `LivingStats.deepCopy()` 遺漏之 `resistances`、`equipment`、`learnedSkills`、`enabledSkills`、`combatResources` 防禦性淺拷貝，消除快照丟失屬性隱患，落實專案第一紅線。
- **100% 資料驅動不可篡改第一紅線確立**：
  - 升級 `GEMINI.md` 第 7 條為專案第一紅線，明訂最高元契約，未經使用者書面許可嚴禁以任何理由刪除或修改。
  - 於 `.agents/rules/data_driven_architecture.md` 建立系統級規則，確保 Antigravity 核心引擎每次自動加載。
  - 明確規定 AI 提供的 Git commit 訊息必須一律使用繁體中文，嚴禁純英文。
- **架構看門狗測試擴充 (GUARD-02)**：
- **JPA 實體規範化與無欄位註解清理**：
  - 全面規範 JPA 實體（`CharacterEntity`、`ItemTemplateEntity`、`RoomStateEntity`、`RoomStateId`、`UserEntity`）與核心領域模型（`GameItem`、`LivingStats`），改用 JPA 領域最佳實踐顯式建構子搭配 `@Getter`/`@Setter`（避免 `@Data` 產生之 `equals`/`hashCode` 懶加載與實體狀態穿透副作用）。
  - 清理所有無欄位標註 `@RequiredArgsConstructor` 之無狀態類別（`FormulaEvaluator`、`SkillCostService`、`AggressiveBehavior`、`InGameBehavior`、`PassiveBehavior`），全面改用原生顯式建構子消除雜訊。
  - 保留核心 Spring 業務服務（`WorldPulse`、`CombatService`、`ShopCommand`、`WorldManager`、`MudWebSocketHandler`、`DungeonFloorLoader`）之簡潔 `@RequiredArgsConstructor` 建構子注入，確立乾淨的依賴宣告。
- **單元與整合測試 100% 綠燈**：
  - 全專案 304 項單元與整合測試全數通過（0 失敗、0 錯誤），Spring Boot 本機服務正常啟動完備。

### [2026-10-14] 罡氣破防、五行抗性與戰鬥機制全面資料驅動落地 (DATA-DRIVEN-COMBAT-01)
- **修復技能縮放抹消漏洞 (Skill Scaling Defect Fix)**：
  - 修復 `SkillBridgeService.scaleSkill()` 在動態縮放技能時漏拷貝屬性的重大 Bug；完整保留 `poiseDamage`、`buffConfig`、`shield`、`defense`、`buff`、`tags`、`aliases`、`gcdMs`、`castTimeMs`、`triggersGcd`、`threatBonus`、`damageType`、`fxKey`。
- **全域九大天道靈氣傷害類型矩陣擴充 (`DamageType`)**：
  - 依據道門修仙元素體系擴充 `DamageType` 枚舉：補齊 `METAL`（金/金石）、`WOOD`（木/草木）、`WATER`（水）、`EARTH`（土/厚土）、`WIND`（風/神風）、`CORRUPT`（陰煞/混沌）、`LIGHT`（陽/光）。
  - 在 `CombatRules.resolveResistance()` 中支持以 `type.is(DamageType.PHYSICAL)` 動態識別物理子系，嚴守「物理保底 10% 穿透防死鎖、法術與道法五行抗性 $>100\%$ 逆流回血」之核心法則。
- **戰鬥模型地基擴充 (Poise / Break Gauge & Resistances)**：
  - 在 `MobTemplate`、`CompanionTemplate`、`BattleEnemy`、`PartyMember`、`PartyMemberSkill`、`Mechanics` 全面擴充 `poise`、`maxPoise`、`resistances`、`poiseDamage` 欄位與方法。
  - `Buffable` 介面提供 `heal(int)` 預設實作，`BattleEnemy` 具備完整的生命值回復與抗性查詢能力。
- **消除寫死中文判斷與打通戰鬥迴圈**：
  - 移除 `DrpgCombatLoop` 中寫死字串比對（如 `contains("解毒")` 等），改為嚴格資料驅動 `skill.getTags().contains("CLEANSE")`。
  - 在單體與 AOE 傷害技能結算中，正式串接五行抗性檢定 `CombatRules.resolveResistance()`，支援五行吸收逆流回血 (`healAmt > 0`)、常規抗性折減與 `skill.getPoiseDamage()` 削韌結算。
- **前端 HUD 枚舉比對雙向相容**：
  - `buff-hud-helper.js` 擴充支援後端枚舉 `STAT_MODIFIER` 與 `CONTROL`（原僅相容 `STAT_MOD` 與 `CC`）。
- **全面擴充 JSON 資料驅動配置**：
  - `classes.json`：為全體 6 大職業補齊基礎破防值（戰士 180、武僧 150、俠客 100、盜賊 80、牧師 40、法師 35）。
  - `taiyin_tomb/mobs.json`：太陰地宮 4 隻怪物（腐化屍偶 25、邪祟骨蝠 15、陰靈血蟒 45、異化守墓道奴 110）配置專屬破防值與五行弱點/抗性表。
  - `default_companions.json`：為 6 位初始同伴配置專屬 `poise`、`maxPoise` 與 `resistances`。
  - `party_skills.json`：全面補齊 `damageType`、`poiseDamage` 與 `tags`（如 `heal_all_purify` 包含 `"CLEANSE"`）。
- **單元與回歸測試 100% 綠燈**：
  - 於 `MechanismPurityAndBugfixTest` 新增資料驅動載入與技能縮放保留驗證測試。
  - 全專案 301 項單元與整合測試 100% 綠燈通過。

### [2026-10-13] 非同步持久化生命週期原子化與背壓契約 (ASYNC-03)
- **讀寫鎖生命週期原子協定 (`ReentrantReadWriteLock`)**：
  - 在 `AbstractAsyncBatchPersistenceService` 導入讀寫鎖：提交請求以 ReadLock 併發防護；停機時以 WriteLock 原子化切斷接收管道。
  - 徹底杜絕「先通過接收檢查、關機排空結束、再入列隊列無人處理」的致命併發競態。
- **明確結果契約與有界背壓 (`SubmitResult`)**：
  - 新增 `submitAsync(T record)` 返回 `SubmitResult`（`ACCEPTED`、`REJECTED_SHUTTING_DOWN`、`REJECTED_QUEUE_FULL`、`REJECTED_NULL`），佇列滿載與停機時提供明確反饋，杜絕靜默吞掉。
  - 擴充可觀測度量指標（`submittedCount`, `acceptedCount`, `flushedCount`, `rejectedQueueFullCount`, `rejectedShuttingDownCount`）。
- **高併發交錯競態證明與專屬整合測試**：
  - 新建 `AsyncPersistenceLifecycleTest`，以 `CountDownLatch` 在關機瞬間併發 500 筆寫入，證明「每筆提交不是 100% 落盤就是明確回傳 REJECTED，佇列停機後 100% 乾淨清零無任何滯留」。
  - 全專案 299 項測試 100% 綠燈通過。

### [2026-10-13] 戰況與狀態動態視覺全面升級 (COMBAT-VFX-STATUS-02)
- **護盾血條疊層與外溢晶格 (Shield Overlay & Overflow Lattice)**：
  - 敵我血條疊加半透明天藍玄罡護盾條，當護盾值超出最大生命時觸發 `.shield-overflow-lattice` 金色流光晶格條。
  - 實裝受創護盾抵扣高光震顫（`.react-shield-hit`）與破盾晶格碎裂爆發微動畫（`.react-shield-shatter`）。
- **五大異常狀態呼吸脈衝邊框 (Status Pulse Border)**：
  - 依卡片單位活動狀態優先級動態渲染動態呼吸邊框：中毒翡翠脈衝（`.status-pulse-poison`）、烈火流血暗紅炎芒（`.status-pulse-burn`）、寒冰霜晶冷光（`.status-pulse-frozen`）、眩暈定身微震（`.status-pulse-stun`）、暗煞走火入魔紫黑煞氣（`.status-pulse-chaos`）。
- **Buff 瀕危高頻呼吸閃爍 (<3s Warning Pulse)**：
  - 聚合 HUD 晶片於狀態剩餘秒數 $<3$ 秒時自動觸發 `.buff-expiring-warning` 高頻赤紅呼吸閃爍，提示即將補盾或解控。
- **DoT 跳血打擊回饋與屬性浮字**：
  - 收到 DoT 結算事件時觸發目標卡片 `.react-dot-tick` 微震，浮字對齊屬性圖示（`🧪 -N`、`🔥 -N`、`🛡️ 破盾 -N`、`🛡️ +N`）。
- **無障礙支援與全專案綠燈**：
  - 完整相容 `@media (prefers-reduced-motion)` 關閉呼吸動態；全專案 295 項測試 100% 綠燈通過。

### [2026-10-13] 太陰地宮 Buff/Debuff 狀態系統全面現代化重構 (BUFF-01)
- **Phase 1: 核心契約、生命週期、Poise 架勢與五行吸收**：
  - 實裝常駐光環契約：`ActiveBuff.PERMANENT_DURATION = -1`，常駐陣法 buff 不會因 tick 遞減而消退。
  - 落地階梯式護盾模型（WoW-style Priority）：多層護盾依剩餘時效由短至長優先抵扣，同名護盾取較高值刷新。
  - 引入【罡氣破防條 (Poise / Break Gauge)】：未破防時傷害強制降為 1/3 且免疫硬控；破防失衡時打斷施法吟唱並清除 1500ms 意圖預告，進入力竭全額 100% 輸出，力竭後自動復甦。
  - 五行法術抗性逆流吸收：法術抗性 $>100\%$ 時（如火抗 130%），超額部分轉化為氣血回復（30 點），支援向隊友丟 AOE 補血套路；物理抗性保底 10% 穿透防死鎖。
- **Phase 2: 控場門控與全動作入口中斷**：
  - 收斂 `Buffable.isControlled()` 與 `isSilenced()` 統一查詢介面。
  - 控場阻斷覆蓋：眩暈打斷普通攻擊輪次、技能吟唱中斷，清除敵方意圖預告；封咒禁止魔法道法念咒。
- **Phase 3: DTO 補全、屬性圖示聚合 HUD 與雙舞台視覺**：
  - 前後端 DTO 串接 `ActiveBuffDto`，攜帶 `element` 靈氣屬性。
  - 戰鬥卡片「屬性圖示聚合 HUD」：以 `[🧪 54/s x3]`、`[🛡️ 120]` 極簡呈現，同屬性 DoT 自動加總，滑鼠懸停顯示明細來源。
  - 1600×900 角色道基詳細面板追加「☯️ 道身加持與狀態」卡片，安全轉義與響應式排版。
  - 技能 JSON 資料校準：`thunder_strike` (LIGHTNING ⚡)、`storm_blade` (SLASH ⚔️)、`spell_corpse_poison` (POISON 🧪)、`taiyin_sword` (DARK 🌑)。
- **Phase 4: Gambit 戰術方針 AI 反制與全自動化整合測試**：
  - 擴充 `TacticsCondition`（`ALLY_HAS_DEBUFF`、`ALLY_IS_CONTROLLED`、`SELF_LACKS_SHIELD`）。
  - AI 防呆機制：目標已具備狀態且剩餘時效 $>3$ 秒時跳過不重複施放。
  - 支援治療與特定技能解毒淨化（Cleanse），自動驅散隊友身上的 DoT 與控場。
  - 新增 6 大場景全自動整合測試 `BuffDebuffComprehensiveTest`，全專案 295 項測試 100% 綠燈通過。

### [2026-10-13] Copilot 全專案程式碼與架構文件審查
- 新增 [`docs/plans/Copilot_Code_Review.md`](docs/plans/Copilot_Code_Review.md)，整理全專案靜態程式碼與五份指定文件的審查發現、Buff 規劃建議及短中長期演進方向。
- 建立審查後續追蹤項目：Write-Behind 提交／停機競態、WebSocket 身分邊界、fallback 架構護欄、前端儲存故障降級及 Buff 契約。
- **測試狀態**：本次 `.\mvnw.cmd test` 因 `JAVA_HOME` 設定錯誤，於 Maven 啟動前中止，未產生本次測試結果；最近一次成功測試數字仍以 2026-10-11 歷史紀錄為準。

### [2026-10-11] 階段 3 戰鬥引擎收斂與雙模呈現投影 (UNIFY-03: Single Combat Engine & Dual Presentation)
- **純函式戰鬥規則與統一數學公式 (CombatRules SSoT)**：
  - 新建 `CombatRules`：抽離純函式戰鬥演算法，包含基礎傷害計算（`calculateRawDamage`，支援武器浮動與 `MobRank` 傷害倍率）、抗性折減（`applyResistance`）以及一元一次擲骰圓桌判定（`executeCombatTable`: Miss -> Dodge -> Parry -> Block -> Crit -> Hit）。
  - `CombatService`、`DefenseResolver`、`DrpgEnemyTacticsService` 100% 收斂委託 `CombatRules`，消除兩套戰鬥數學公式的割裂。
- **大一統戰鬥參與者抽象 (Universal Combatant Interface)**：
  - 定義 `Combatant` 介面（繼承 `Buffable`），宣告攻防範圍、抗性、屬性與生命週期契約。
  - `Living` (包括 `Player`, `Mob`)、`PartyMember`、`BattleEnemy` 統一實現 `Combatant`，使戰鬥規則可多態消費任意對決實體。
- **雙模呈現投影架構 (Dual Presentation Projection)**：
  - 新建 `CombatPresentation`：
    - **MUD 模式投影 (`projectMudCombatLog`)**：將戰鬥結果轉化為純淨無時間戳的武俠招式敘事，支援 `$W`/`$w`/`$l`/`$d` 佔位符色彩渲染並廣播至當前房間終端。
    - **DRPG/Web 模式投影 (`projectGuiCombatEvent`)**：生成標準結構化 `BattleEvent` / `BattleEventsDto`，支援 Web 介面卡片動態演出、血條扣減與浮動數字特效。
- **城鎮遭遇戰與戰鬥流程統一 (ADR-01 決策落地)**：
  - `CombatService` 普攻計算與城鎮 `kill` 指令全面接入 `CombatRules` 與 `CombatPresentation`，支援雙模投影並維護狀態機閉環。
- **整合驗證測試**：
  - 新增 `UnifiedCombatEngineAndPresentationTest`（涵蓋純函式數學與抗性、Combatant 多態契約、雙模投影生成、城鎮遭遇戰整合廣播 4 項測試）。
  - 全專案 283 項測試 100% 綠燈通過。

### [2026-10-11] 階段 3 角色與裝備大一統 (UNIFY-02: 7-Slot Canonical Equipment SSoT & Real-Time Projection)
- **7 槽位裝備單一狀態持有 (Single Source of Truth)**：
  - `PartyMember`（小隊首位主角隊長，`memberIdx == 0`）正式確立為 7 大槽位（`MAIN_HAND`, `OFF_HAND`, `HEAD`, `BODY`, `FEET`, `ACCESSORY_1`, `ACCESSORY_2`）之唯一權威持有者。
  - 徹底終結過去 MUD `Player.stats.equipment`（4 槽位）與 DRPG `PartyMember.equipment`（7 槽位）各自穿脫、數值分裂之歷史痛點。
- **Actor Mailbox 執行緒安全單向投影 (Mailbox Projection Protocol)**：
  - `ActorMessage` 擴充 `ProjectEquipment(Map<EquipmentSlot, PartyItemSlot> equipment, CompletableFuture<Boolean> future)` 訊息協定。
  - `Player.handlePlayerMessage` 於 Player 虛擬執行緒中安全重構 `GameItem` 投影映射，並呼叫 `livingService.recalculateStats(this)` 重算攻擊力與防禦力。
  - `CharacterSyncService.syncEquipmentFromPartyToPlayer`:
    - 具備 Actor 執行緒邊界感知：同執行緒或離線（測試環境）同步套用；跨執行緒投遞至 Mailbox 確保不破壞 Single-Writer 原則。
    - 依據 `TemplateReader` 解析物品定義，對齊屬性加成 (`bonusMinDamage`, `bonusMaxDamage`, `bonusDefense`)。
- **雙層背包穿脫互通與即時數值回落**：
  - `DrpgBattleService.equipItem`:
    - 穿戴裝備時，優先從小隊公共倉庫尋找，若無則自動在主角個人隨身行囊檢索裝備，裝備成功後扣除並將舊裝備卸至倉庫/行囊。
    - 穿裝與卸裝後，立即觸發 `CharacterSyncService.syncEquipmentFromPartyToPlayer` 即時投影更新主角攻防屬性。
  - `LivingService.recalculateStats`: 公開為 API 並擴充支援 `ItemDefinition`，卸下裝備時數值立即精確回落。
- **存檔與開局初始同步**：
  - `SaveGameService.loadGame` 與 `createNewGame` 中，讀檔與開局後自動執行 `syncEquipmentFromPartyToPlayer`，保證存檔讀取後 MUD 視角立即可見 7 槽位投影數值。
- **MUD 指令介面擴充**：
  - `EquipCommand`: 支援 `eq` / `equipment` 別名；無參數時以武俠格式格式化輸出 7 槽位穿著狀態、攻擊區間與總防禦力。
- **整合驗證測試**：
  - 新增 `CanonicalEquipmentProjectionTest`（5 項整合測試：7 槽位投影、個人行囊穿裝、卸裝數值回落、存檔讀取投影、指令視圖渲染）。
  - 全專案 279 項測試 100% 綠燈通過。

### [2026-10-11] 階段 3 雙層背包收斂與存檔 v2 平滑遷移 (UNIFY-01: Dual-Tier Inventory & SaveData v2)
- **消除雙寫假帳 (No Double Accounting)**：
  - `GetCommand`: 地面散落物品拾取至玩家個人隨身行囊 `player.getInventory()`（12 格上限，滿載時自動溢流至小隊倉庫）；容器戰利品（如【散落的儲物袋】、寶箱）搜刮後全數納入 `party.getInventory()`（小隊倉庫 30 格）。徹底消除過去拾取時同時寫入兩處之假帳漏洞。
  - `DropCommand`: 支援預設 `drop <物品>` 自隨身行囊丟棄至地面；新增 `drop stash <物品>` / `drop storage <物品>` 自小隊倉庫丟棄至地面；若行囊無該物但倉庫存在時，給予友善提示。
  - `StashCommand`: 新建指令處理個人隨身行囊與小隊倉庫之雙向流轉。支援 `stash <物品>` / `store <物品>` 存入倉庫；支援 `withdraw <物品>` / `take <物品>` 取出回行囊。
  - `InventoryCommand`: 介面升級，同步分區列出【個人隨身行囊】與【小隊儲物倉庫】，並標註快捷流轉語法提示。
- **存檔架構升級 (SaveData v2 Migration)**：
  - `SaveData.CURRENT_SCHEMA_VERSION` 升級為 `2`，新增 `personalInventory: List<PartyItemSlot>`。
  - `SaveMigrationPipeline`: 實作 `1 -> 2` 自動補齊空行囊陣列，確保舊版存檔完全平滑讀取相容。
  - `SaveGameService`: 存檔時自動序列化線上玩家行囊至 `personalInventory`；讀檔時完美還原至 `player.getInventory()`。
- **整合驗證測試**：
  - 新增 `DualTierInventoryAndStashTest`（6 項雙層背包流轉與存檔還原測試）與 `SaveGameServiceTest.testSaveMigrationFromV1ToV2`。
  - 全專案 274 項測試 100% 綠燈通過。

### [2026-10-11] 階段 3 架構設計決策定案 (ADR-01: Architecture Decision Records)
- **1. 戰鬥機制統一 (UNIFY-03)**：定案採用「**共用同一套機制**」。城鎮 `kill` 與地牢探索遇敵統一接入現代小隊戰鬥引擎，MUD 視窗負責長篇文字敘事投影，GUI 負責卡片動態演出。
- **2. 背包所有權模型 (UNIFY-01)**：定案採用「**個人行囊 + 小隊倉庫**」雙層模型。個人行囊管理隨身裝備與快捷丹藥，小隊倉庫管理團隊戰利品與備用素材，消除 `get/drop/shop` 雙寫假帳。
- **3. 持久化真相源 (SAVE-01)**：定案以「**單機版 SaveData JSON**」作為唯一單一真相源。升級 `schemaVersion: 2` 進行原子化存檔槽平滑遷移，逐步解耦 H2 DB。
- **4. 客戶端呈現定位**：定案聚焦於現代「**Web 終端視窗 + GUI 混合介面**」，無需為純外部 telnet 終端過度設計。

### [2026-10-11] 階段 2 Actor 邊界同步正確性與雙軌領域事件主幹打通 (SYNC-01 / EVENT-01)
- **Actor Mailbox 循序派發與執行緒邊界收斂 (SYNC-01)**：
  - `ActorMessage` 擴充 `ApplyBattleOutcome(BattleOutcome outcome, CompletableFuture<Boolean> future)` 訊息協定。
  - `Player.handlePlayerMessage` 增加此訊息的非同步安全派發與結算處理，確保對 `LivingStats` 的修改完全限制在 Player Virtual Thread 串行處理。
  - `CharacterSyncService.applyOutcome`: 嚴格遵守 Actor 併發模型邊界。若調用方已在 Player 虛擬執行緒內或 Actor 未啟動（單元測試/離線），直接同步套用避免自我等待死鎖；若跨執行緒邊界且 Player 運行中，將不可變 Outcome 投遞至 Mailbox 並等待完成，杜絕併發競態與跨線程穿透修改。
  - `CharacterSyncService.syncFromPartyToPlayer`: 將戰鬥後的隊長屬性增長包裝為不可變 `BattleOutcome` 並透過 `applyOutcome` 投遞。
- **雙軌領域事件主幹打通 (EVENT-01)**：
  - `PlayerEvents` 擴充 `record ItemAcquired(String playerId, String itemId, int count, Instant occurredOn)` 封閉介面成員。
  - `AuditLogEventListener` 擴充模式匹配，自動審計 `PlayerEvents.ItemAcquired`。
  - `XpProgressionService`: 在主角及小隊隊長突破境界升級時，統一透過 `ApplicationEventPublisher` 發布 `PlayerEvents.LevelUp`。
  - `LivingService.onDeath`: MUD 端怪物死亡時，統一發布 `MobEvents.MobDead`。
  - `DrpgRewardService.resolveVictory`: DRPG 端戰鬥大捷時，敵方戰利品掉落與血肉道核獲取時，統一發布 `PlayerEvents.ItemAcquired`。
  - `MozhuMinesQuestListener`: 首領宋天衡擊殺判定由模糊 `contains("boss_song_tianheng")` 改為精確命名空間匹配，修正提示文本。
- **整合驗證測試與架構棘輪穩固**：
  - 新增 `ActorOutcomeSyncAndEventBackboneTest`：完整涵蓋 Actor Mailbox 執行緒安全投遞、非運行同步退回、MUD 怪物死亡事件廣播、突破升級事件廣播與 DRPG 戰利品獲取事件廣播。
  - 收斂 `DrpgCombatLoop` 與 `DrpgRewardService` 建構子依賴，消除 `new TemplateCatalog()` 重複調用，嚴格鎖定棘輪上限為 33。
  - 全套測試套件（267 項測試）100% 綠燈通過。

### [2026-10-11] 階段 1 移除靜默 Fallback 與內容資料化 (FALLBACK-01 / DATA-03~06 / CONF-01)
- **全面移除硬編碼掉落物與怪物清單 (DATA-04 / DATA-05 / DATA-06)**：
  - 徹底移除 `DungeonCommand`、`DrpgBattleService`、`DrpgRewardService` 中的 `TOMB_MOBS` 與 `TOMB_ITEMS` 硬編碼常數。
  - 迷宮隨機遭遇戰 (`triggerRandomEncounter`) 嚴格基於地牢樓層 `mobPool` 生成，空池安全提示；地塊事件 (`handleTileEvent`) 寶箱與首領戰 100% 依賴地塊 `drops` 與 `eventId`，無靜默假資料 fallback。
  - 重構戰鬥擊殺掉落機制：`BattleEnemy` 支援 `dropItemIds` 多掉落物動態結算，`DrpgBattleService.startBossBattle` 與 `BattleEnemy.fromTemplate` 依照 `MobTemplate.loot()` 掉落表機率擲骰掉落；首領宋天衡擊殺必定掉落 `black_obsidian_scythe` 與 `elder_token`，徹底刪除 `contains("boss_song_tianheng")` 人工寫死物品注入。
  - 首領宋天衡於 `mobs.json` 補齊 `"expReward": 500`，戰鬥結算動態讀取 (DATA-03)。
- **清理職業與陣法硬編碼 (DATA-06 / SWORDSMAN)**：
  - `DefenseResolver` 招架反擊 (`canRiposte`) 改為 100% 依據職業定義 traits (`p.getClassTemplate().map(t -> t.hasTrait("canRiposte")).orElse(false)`)，移除硬編碼 `"SWORDSMAN".equalsIgnoreCase(...)`。
  - `FormationTemplate` 移除 SWORDSMAN 相容 TANK 的例外旁路判斷，嚴格遵守 `RoleCategory` 職能分類體系。
  - `Player.getClassId()` 預設職業改由 `GameConfig.getDefaults().getPlayer().getDefaultClassId()` 驅動，取代代碼寫死。
- **全域遊戲配置驅動 (CONF-01)**：
  - `GameConfig.PlayerDefaults` 與 `application.yml` 新增 `default-class-id` 與 `starting-supplies`（太陰清靈丹、淨穢符、精鋼佩劍、玄鐵槍）。
  - `PartyService.getStartingSupplies()` 全面改為讀取 `GameConfig` 設定，支援外置 YAML 定制。
- **治理靜默 Fallback 反模式 (FALLBACK-01)**：
  - 移除 `PartyCommand` 的 5 參數冗餘建構子，注入 `XpProgressionService` 與 `TemplateReader`。
  - 重構 `DrpgRewardService`：以建構子注入替換多個 `@Autowired(required = false)` 與延遲 `new`，道核生成標準化委由 `ItemFactory.createDaoCore(...)`。
  - 縮緊 `ArchitectureGuardrailsTest` 的 `new TemplateCatalog()` 棘輪上限至 33 處。
  - 全套測試套件（262 項測試）100% 綠燈通過。

### [2026-10-11] 階段 0 重構護欄就位：校驗器擴充、防刷除錯指令封裝與架構防回歸檢驗 (GUARD-01 / DEBUG-01)
- **資料拓撲校驗器擴充 (`DataIntegrityValidator` - GUARD-01)**：
  - 深度盤查地牢樓層所有地塊（`DungeonTile`），校驗地塊掉落物（`drops`）及首領/事件怪物 ID（`eventId`）於物品庫與怪物庫之存在性，杜絕移除 fallback 時發生靜默 NPE。
  - 擴充全域技能與小隊技能（`SkillTemplate` / `PartyMemberSkill`）橋接規則雙向校驗，確保所有 `bridgeRules` 及預設橋接目標 DRPG 技能（如 `sword_pierce`、`star_warp` 等）均存在於技能庫。
  - 新增開局物資存在性校驗（`PartyService.getStartingSupplies()`）。
  - 更新 `ValidationReport` 指標維度（物品 117、怪物 69、房間 40、商店 2、夥伴 6、陣法 10、地牢 2、技能 100、開局物資 4），全部校驗 100% 通過。
- **除錯指令正式環境封裝 (`BattleCommand` / `DungeonCommand` / `GameConfig` - DEBUG-01)**：
  - `GameConfig` 與 `application.yml` 新增 `game.debug.enabled` 設定（預設 `false`）。
  - 封鎖正式環境隨意召喚玄鐵試道傀儡（`battle test/dummy/木樁`、`dungeon dummy/test/testmob`）與瞬間全滿靈威（`battle charge/充能`），防範玩家刷 XP 與掉落資源；僅於除錯環境或管理員旗標下開放。
- **架構防回歸棘輪與邊界掃描測試 (`ArchitectureGuardrailsTest` / `DebugCommandGuardTest`)**：
  - 新增 `ArchitectureGuardrailsTest`：為 `new TemplateCatalog()` 與 `GameConfig.getInstance()` 建立上限基準（Ratchet 棘輪模式），嚴禁新代碼新增靜默 fallback；並自動驗證領域層不依賴 Web/Servlet 控制器。
  - 新增 `DebugCommandGuardTest`：針對正式環境封鎖與除錯模式放行進行 100% 單元測試覆蓋。
  - 全套測試套件（262 項測試）100% 綠燈通過。
- **修復點選選單文字溢出左側舞台問題 (圖 1 Bug 修復)**：
  - 原因：在城鎮點擊「選單 (C)」時，前端同時彈出圖形化選單並發送了向後端請求純文字 `party` 的指令；後端回傳 50 行長 ASCII 文本，且因右側日誌欄位 flex 項目缺少 `overflow: hidden` 與 `min-width: 0`，長字元跨越溢出高達 500px，覆蓋了左側客棧舞台與 NPC 按鈕。
  - 修正：[`style-01-tokens-and-shell.css`](./src/main/resources/static/css/style-01-tokens-and-shell.css) 為 `.log-and-battle-area` 加上 `min-width: 0; width: 25%; overflow: hidden;`，`#log` 加上 `overflow-x: hidden; overflow-wrap: anywhere; word-break: break-word;`；並在 [`party-modal.js`](./src/main/resources/static/js/modals/party-modal.js) 移除多餘的 `send('party')`，點選選單直接彈出沉浸式圖形化模態視窗，徹底消除純文字覆蓋舞台。
- **戰鬥防禦判定動態差異化（被擊中 / 閃避 / 招架 / 格擋）(圖 2 視覺增強)**：
  - **格擋 (BLOCKED)**：實裝 `.fx-shield-block`，目標面前升起玄鐵金光護盾光環，吸收衝擊波紋；卡片觸發縮放沉穩脈衝。
  - **招架 (PARRIED)**：實裝 `.fx-parry-sparks`，雙刃拼刀金鐵交鳴四濺火星；卡片觸發左右傾斜回架動態。
  - **閃避 (DODGED)**：實裝 `.fx-dodge-mist`，觸發太極殘影步法與流雲青煙；卡片觸發向側方快速位移滑動。
  - **暴擊 (CRIT)**：實裝 `.fx-crit-burst`，目標遭受猩紅撕裂血煞爆裂衝擊；卡片觸發劇烈震顫。
  - 整合至 [`style-07-battle-fx.css`](./src/main/resources/static/css/style-07-battle-fx.css)、[`fx-presets.js`](./src/main/resources/static/js/fx/fx-presets.js) 與 [`battle-fx-director.js`](./src/main/resources/static/js/fx/battle-fx-director.js)。
- **陣法奧義（U）狀態視覺化與全場大招演出 (圖 2 體驗修復)**：
  - 原因：陣法奧義需滿 100 靈威方可施放，截圖中靈威為 50/100，原先按鈕固定顯示可按且無進度提示，點擊後僅在日誌回傳一行文字，在淡化日誌模式下易被誤解為「沒有效果 / 未實作」；且原後端釋放時未發送結構化 `BattleEvent`。
  - 修正：
    1. 後端 [`DrpgBattleService.java`](./src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgBattleService.java) 於施展陣法奧義時發布全場 `BattleEvent`（辟邪金光橫掃 / 暗蝕星蝕覆蓋），並廣播 `BattleEventsDto`。
    2. 前端 [`battle-panel.js`](./src/main/resources/static/js/panels/battle-panel.js) 連動底欄 `#footer-ult-btn`：靈威未滿 100 時顯示置灰與蓄力進度 `⚡ 陣法奧義 [X/100] (U)`；滿 100 靈威時切換為金色流光呼吸脈衝 `⚡ 陣法奧義【就緒!】(U)`。
    3. 前端 [`app.js`](./src/main/resources/static/js/app.js) 在按快捷鍵 `U` 或點擊按鈕時增加攔截：若靈威不足，按鈕抖動並在 Ticker 提示「⚠️ 陣法靈威不足 (X/100)，戰鬥中交鋒蓄滿 100 方可施展奧義！」，反饋清晰明瞭。
- **金剛怒目轉型全體嘲諷震懾光環**：
  - 原因：金剛怒目本質為群體吸引仇恨技能，但此前後端發送了 `damage: 0` 的打擊事件，前端對全體怪物播放了普通武器切口與受傷晃動，看似全體傷害攻擊。
  - 修正：後端 [`DrpgCombatLoop.java`](./src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java) 將事件 `damageType` 設為 `"TAUNT"`、`fxKey` 設為 `"taunt_roar"`；前端特效導演 [`battle-fx-director.js`](./src/main/resources/static/js/fx/battle-fx-director.js) 與 [`floating-text.js`](./src/main/resources/static/js/fx/floating-text.js) 改為在施法者周圍爆發金剛獅子吼金色震懾光環，怪物頭頂彈出「💢 仇恨鎖定」警示並觸發挑釁紅光邊框微晃，不播放傷害刀光。
- **後端狀態機與 Telegraph 階段 (`BattleEnemy`, `DrpgCombatLoop`)**：
  - 擴充 [`BattleEnemy.java`](./src/main/java/com/example/htmlmud/domain/dungeon/battle/BattleEnemy.java) 新增 `currentIntentIcon`、`currentIntentName`、`currentIntentType`、`isCasting`、`castDurationMs`、`castRemainingMs`、`isInterruptible` 等意圖管理欄位，支援 `startIntent(...)`、`clearIntent()` 與 `getCastRemainingMs()`。
  - 重構 [`DrpgCombatLoop.java`](./src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java)，引入攻擊前 1500ms **Telegraph 預告視窗**：敵人在攻擊前預先決定攻擊招式與目標隊員，並啟動蓄力倒數；遭眩暈時立即中斷吟唱並廣播打斷事件；普通命中時清除意圖並無縫進入下個行動循環。
- **視圖數據傳遞向後相容 (`BattleEnemyViewDto`)**：
  - 於 [`BattleEnemyViewDto.java`](./src/main/java/com/example/htmlmud/domain/dungeon/dto/BattleEnemyViewDto.java) 擴充意圖欄位，並保留現有所有過載建構子，委派全參建構子，確保 100% 舊代碼與測試相容。
  - 於 [`DrpgBattleService.java`](./src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgBattleService.java) 的 `createBattleView` 精確映射敵方意圖與即時剩餘蓄力毫秒。
- **前端敵方卡片視覺呈現 (`battle-panel.js`, `style-07-battle-fx.css`)**：
  - 各規格怪物卡片（1x1、2x2、3x3、1x3、1x5）新增專屬意圖容器 `.enemy-intent-container`，即時渲染意圖圖示、招式名稱、鎖定目標標籤（`➔ 隊員名`）與可斷招標籤（`可斷`）。
  - 新增動態蓄力進度條 `.enemy-cast-track` 與 `.enemy-cast-fill`，區分普通物理 (`intent-bar-physical`)、重擊危險 (`intent-bar-heavy`)、法術咒道 (`intent-bar-spell`)、深淵暗蝕 (`intent-bar-aberration`) 等炫彩流光與呼吸動態。
  - 重擊與深淵蓄力時自動觸發怪物卡片危險紅色脈衝光暈（`.intent-danger-telegraph`）。
  - 前端 In-place 更新循環保持零閃爍，動態平滑更新意圖與蓄力寬度。
- **單元測試驗證**：新增專屬單元測試 [`EnemyIntentTest.java`](./src/test/java/com/example/htmlmud/EnemyIntentTest.java)，全專案 252 個單元測試 100% 綠燈通過。

### [2026-10-08] 實裝戰鬥大捷翻牌視窗、經驗長條充能與境界突破特效 (BATTLE-RESULT-01, P2)
- **結構化大捷事件載荷 (`BattleVictoryDto`)**：後端 [`DrpgRewardService.java`](./src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgRewardService.java) 於大捷時發送結構化 `BattleVictoryDto`，攜帶戰鬥 ID、全員修為、靈石獲得、隊員突破明細 (`MemberLevelUpDto`) 與掉落物清單 (`BattleLootDto`)，純文字 Log 收斂為單行摘要，杜絕洗版。
- **戰後結算模態視窗 (`#battle-victory-modal`)**：於 [`index.html`](./src/main/resources/static/index.html) 與 [`style-07-battle-fx.css`](./src/main/resources/static/css/style-07-battle-fx.css) 打造暗色仙俠金光橫幅視覺視窗，支援自適應光暈呼吸動畫與 Space / Enter / Esc 便捷關閉。
- **全員修為長條充能與境界突破光圈**：[`victory-modal.js`](./src/main/resources/static/js/modals/victory-modal.js) 動態渲染全隊成員修為增長，進度條帶平滑 CSS 動畫；升級成員浮現「✨ 境界突破！」金光閃爍標籤與數值成長明細。
- **戰利品 3D 翻牌動態 (3D Card Flip)**：戰利品初始為太極道紋「☯️ 秘寶封印」背面，支援 200ms 階梯式自動翻牌與點擊翻面，依品質（凡品/下品/中品/上品/極品）展現對應光暈流光（Common 到 Legendary）。
- **無障礙降級與系統穩固**：支援 `prefers-reduced-motion` 動畫降級；完善後端戰鬥生命週期與虛擬執行緒隔離，全專案 250 項單元測試 100% 綠燈通過。

### [2026-10-08] 落地純文字日誌淡化與主舞台空間解放 (LOG-DIM-01, P1)
- **主舞台空間釋放 (75% vs 25%)**：重構 [`style-01-tokens-and-shell.css`](./src/main/resources/static/css/style-01-tokens-and-shell.css)，將 `.stage-left-container`（城鎮/地牢/戰場主舞台）擴增至 75% 佔比，`.log-and-battle-area`（文字日誌）收斂至 25% 輔助資訊欄（限制 max-width: 380px），徹底扭轉純文字 MUD 視覺權重。
- **一鍵摺疊日誌模式 (`.log-collapsed`)**：支援一鍵摺疊日誌面板，主舞台即刻擴展至 **100% 滿版沉浸畫面**，並記住使用者偏好至 `localStorage`。
- **底部單行水墨跑馬燈 (`#hud-event-ticker`)**：於主舞台小隊 HUD 上方新增單行事件跑馬燈，日誌即使摺疊，也能即時呈現最新一筆戰報或劇情動態，點擊跑馬燈可秒速展開日誌。
- **日誌工具列與分類過濾 Tab**：日誌頂部新增【全部】、【⚔️ 戰鬥】、【💬 劇情】、【⚙️ 系統】篩選按鈕與清空日誌按鈕，避免戰鬥跳字刷屏淹沒劇情。
- **前端代碼安全**：[`message-log-panel.js`](./src/main/resources/static/js/panels/message-log-panel.js) 與 [`app.js`](./src/main/resources/static/js/app.js) 實裝自動類別偵測與 CSP 事件委派，Node.js 語法校驗零錯誤，全單元測試 100% 綠燈通過。

### [2026-10-02] 落地戰鬥「一次擲骰圓桌判定 (One-Roll Combat Table)」(Phase 3.1, P1)
- **一次擲骰連續累積圓桌**：重構 [`DefenseResolver.java`](./src/main/java/com/example/htmlmud/domain/dungeon/battle/DefenseResolver.java)，消除傳統 `if-else` 鏈之優先序偏倚，採用單一隨機浮點數 $R \in [0.0, 1.0)$，依標準圓桌扇區結算：
  $$\text{[Miss]} \to \text{[Dodge]} \to \text{[Parry]} \to \text{[Block]} \to \text{[Crit]} \to \text{[Normal Hit]}$$
- **判定扇區與公式**：
  - **未命中 (Miss)**：$\text{clamp}(0.02, 0.25, 0.05 + \max(0, \text{Def.DEX} - \text{Atk.DEX}) \times 0.005)$。
  - **身法閃避 (Dodge)**：需配置身法技能，$\text{baseDodge} + (\text{Def.DEX} \times 0.008) - (\text{Atk.DEX} \times 0.003)$，成功時傷害歸零並獎勵 15 點 SP。
  - **兵刃招架 (Parry)**：需配置招架技能，$\text{baseParry} + (\text{Def.STR} \times 0.004) + (\text{Def.DEX} \times 0.004)$，成功時傷害減半，獎勵 5 點 SP 與怒氣。
  - **盾牌格擋 (Block)**：需副手裝備盾牌，$0.20 + (\text{Def.CON} \times 0.003)$，成功時扣減盾牌格擋值 ($\text{Defense} \times 2 + \text{CON} / 2$)，保底 1 點傷害。
  - **致命重創 (Crit)**：$0.05 + (\text{Atk.DEX} \times 0.002)$，造成 1.5 倍爆擊重創。
  - **普通命中 (Hit)**：骰值落於其餘常態扇區，扣除防禦正常承受傷害。
- **角色領域擴充**：於 [`PartyMember.java`](./src/main/java/com/example/htmlmud/domain/party/model/PartyMember.java) 補齊 `getEquippedShield()`、`equipShield(PartyItemSlot)`、`unequipShield()` 委派封裝。
- **測試保護網**：新增 [`OneRollCombatTableTest.java`](./src/test/java/com/example/htmlmud/OneRollCombatTableTest.java) 針對 6 大圓桌扇區與預定擲骰進行 100% 綠燈單元測試驗證，並通過 [`PassiveSkillsAndCombatCheckTest.java`](./src/test/java/com/example/htmlmud/PassiveSkillsAndCombatCheckTest.java) 迴歸檢定。

### [2026-10-02] 實裝全域 escapeHtml() 與前端共用工具底座 (Phase UI-1, P0)
- **全域常數中樞**：新增 [`src/main/resources/static/js/core/constants.js`](./src/main/resources/static/js/core/constants.js)，統一定義字級底線 (`MIN_FONT_SIZE_PX: 13`)、日誌緩衝上限 (`MAX_LOG_HISTORY_LINES: 200`)、主選單規格 (1600x900 / 20筆每頁)、快捷鍵對照 `KEYS` 與品質樣式主題。
- **UI 共用函式庫**：新增 [`src/main/resources/static/js/core/ui-utils.js`](./src/main/resources/static/js/core/ui-utils.js)，提供高強度 HTML 特殊字元轉義 (`escapeHtml(str)`)、數值百分比計算 (`calculatePercentage`)、標準狀態能量條建構 (`createResourceBar`) 等工具，並雙向掛載至 `window` 維持向後相容。
- **全域 DOM XSS 漏洞根除**：
  - **城鎮探索視圖** ([`town-panel.js`](./src/main/resources/static/js/panels/town-panel.js))：對特殊出口房間名、NPC 姓名/稱號/特徵標籤、地面掉落物名稱全數進行 `escapeHtml` 消毒。
  - **小隊 HUD 狀態列** ([`party-hud-panel.js`](./src/main/resources/static/js/panels/party-hud-panel.js))：對隊員姓名、道心異變狀態、當前施法名稱、Buff 名稱與類別進行安全轉義。
  - **戰場敵我陣列** ([`battle-panel.js`](./src/main/resources/static/js/panels/battle-panel.js))：對敵方名稱、仇恨鎖定目標名稱、Buff 徽章、戰備指揮台技能資訊進行嚴格轉義防護。
  - **行囊抽屜與貨棧交易** ([`bag-drawer.js`](./src/main/resources/static/js/modals/bag-drawer.js), [`shop-modal.js`](./src/main/resources/static/js/modals/shop-modal.js))：對物品名稱、效果敘述、物品 ID、隊員目標名稱及交易按鈕參數全面消毒。
  - **技能盤與存讀檔** ([`skill-drawer.js`](./src/main/resources/static/js/modals/skill-drawer.js), [`save-modal.js`](./src/main/resources/static/js/modals/save-modal.js), [`party-modal.js`](./src/main/resources/static/js/modals/party-modal.js))：對技能名稱、描述、槽位主角名稱、地宮樓層與陣法名稱全面防護。
  - **滾動日誌安全** ([`message-log-panel.js`](./src/main/resources/static/js/panels/message-log-panel.js), [`mud-core.js`](./src/main/resources/static/js/mud-core.js))：於 ANSI 色碼轉換前進行前置 HTML 標籤轉義，並統一接入 `MAX_LOG_HISTORY_LINES` 防護。

### [2026-10-02] 建立 GameConfig 集中管理全域硬編碼常數 (P0)
- **數值中樞**：新增 [`GameConfig.java`](./src/main/java/com/example/htmlmud/config/GameConfig.java)，支援 Spring Boot `@ConfigurationProperties(prefix = "game")` 綁定與靜態回退單例模式 (`GameConfig.getInstance()`)。
- **配置落地**：於 [`src/main/resources/application.yml`](./src/main/resources/application.yml) 統一定義 `game.defaults`、`game.combat`、`game.regen` 配置塊。
- **硬編碼清理**：
  - 玩家初始數值：[`Player.createSinglePlayer()`](./src/main/java/com/example/htmlmud/domain/actor/impl/Player.java) 替換 HP(200)、MP(100)、Coin(100)、起點房間 (`newbie_village:inn`) 為動態設定。
  - 公共冷卻：[`Player.triggerGcd()`](./src/main/java/com/example/htmlmud/domain/actor/impl/Player.java) 替換寫死 1500ms 為 `combat.defaultGcdMs`。
  - 復活重生點：[`PlayerService.handleRelive()`](./src/main/java/com/example/htmlmud/domain/service/PlayerService.java) 注入 `GameConfig` 取得 `defaults.player.respawnRoomId`。
  - 命中率與攻擊間隔：[`CombatService.calculateDamage()`](./src/main/java/com/example/htmlmud/domain/service/CombatService.java) 替換 0.80 基礎命中與 0.01 靈巧係數；多次攻擊間隔替換 450~551ms 區間。
  - 回復心跳與 MP 回復：[`LivingService.java`](./src/main/java/com/example/htmlmud/domain/service/LivingService.java) 替換每 150 tick (15秒) 回復頻率與 5% HP 比率，並補齊長期被註解之 1% MP 自然回復。
  - 赤手攻速：[`Living.getAttackSpeed()`](./src/main/java/com/example/htmlmud/domain/actor/impl/Living.java) 替換寫死 2000ms 為 `combat.unarmedAttackSpeedMs`。
  - 經驗公式收斂：[`CombatService.calculateNextLevelXp()`](./src/main/java/com/example/htmlmud/domain/service/CombatService.java) 消除寫死回傳 10，委派至 [`XpProgressionService`](./src/main/java/com/example/htmlmud/domain/service/XpProgressionService.java)。
- **單元測試**：新增 [`GameConfigTest.java`](./src/test/java/com/example/htmlmud/GameConfigTest.java) 100% 綠燈驗證。

### [2026-10-02] 文檔治理四體系落地與架構收斂
- **規範治理**：在 [`GEMINI.md`](./GEMINI.md) 確立三大開發鐵律（未決定與待辦第一時間入庫 `FUTURE_IMPROVEMENTS.md`、每次變更必記 `CHANGELOG.md`、每次變更必 Review `ARCHITECTURE.md`）。
- **架構整併**：將 `docs/PROJECT_STRUCTURE.md` 與 `plans/README.md` 之檔案結構與 AI 鐵律整併入 [`ARCHITECTURE.md`](./ARCHITECTURE.md)，消除雙頭馬車。
- **前端視圖規範化**：產出 [`docs/UI_PAGE_STRUCTURE.md`](./docs/UI_PAGE_STRUCTURE.md)，確立 7 大模組化視圖、Data-Driven 鐵律與共用工具庫規格。
- **純粹化待辦清單**：清理 `FUTURE_IMPROVEMENTS.md` 歷史完成項，專注未來 P0~P3 路線圖。
- **歷史歸檔**：將 `WALKTHROUGH.md` 與 `main_menu_refactor_plan.md` 完整收斂至本文件後安全歸檔。

---

## 🏛️ 里程碑演進歷程 (Milestones & Evolution)

### [Phase 11.5] 主選單系統 (Main Menu) 重構全面落地 (2026-09-27)
- **視覺規範**：導入固定比例 `width: min(1600px, 96vw); height: min(900px, 94vh);` 滿版適配，鎖定標準字級 >= 13px~14px，徹底根絕小於 12px 擠壓破版。
- **左側 8 大功能主導覽**：
  1. `ITEMS`（道具：20 筆/頁分頁、可使用回復與 Buff 類支援目標選擇）
  2. `EQUIP`（裝備：5 人直排 260px 並列點將台，點擊槽位觸發裝備候選與數值/功法 Diff 比較子視圖）
  3. `SKILLS`（技能&法術：探索可用/主動/全部，顯示冷卻與資源消耗）
  4. `CHARACTERS`（角色：5 位成員詳細數值、四維屬性、道基加點）
  5. `PARTY`（隊伍：隊伍出戰名冊 #1~#5 排序調整，與 5x3 戰陣幾何座標解耦）
  6. `FORMATION`（陣法：5x3 戰陣盤孔位加成與典籍庫）
  7. `TACTICS`（戰術方針：FFXII 風格 Gambit 優先序規則鏈配置）
  8. `SYSTEM`（系統：多槽位存檔、讀檔、自動存檔）
- **關聯計畫歸檔**：`main_menu_refactor_plan.md` 全數階段完成並歸檔。

---

### [Phase 11] 戰鬥狀態機 (FSM)、全域冷卻 (GCD) 與仇恨機制 (2026-09-24)
- **戰鬥狀態機 (CombatFsmService)**：定義戰鬥生命週期（IDLE ➔ CASTING ➔ CHANNELING ➔ RECOVERY ➔ COOLDOWN）。
- **全域冷卻 (GCD)**：落實技能公共冷卻機制與施法唱條中斷判定。
- **仇恨管理 (ThreatTable)**：支援傷害、治療、嘲諷（Taunt）累積仇恨，敵方 AI 依仇恨榜動態切換攻擊目標。

---

### [Phase 9.5] WoW 風格 Buff / Debuff 體系 (2026-09-24)
- **Buff 結算引擎 (BuffSettlementService)**：支援持續傷害 (DoT)、持續治療 (HoT)、屬性增減益、控制效果（眩暈、定身、沉默）。
- **驅散與疊加機制**：支援法術驅散、道門淨化，以及層數堆疊與獨立計時。

---

### [Phase 9 & 9.1] 被動心法裝配、戰鬥檢定與戰術護盾 (2026-09-23)
- **Generic Defense & Passive Resolver (`DefenseResolver`)**：
  - 身法閃避 (DODGE)：依身法技能與 DEX 檢定，免除 100% 傷害並恢復 SP。
  - 兵刃招架 (PARRY)：依招架技能、STR 與 CON 檢定，減免 50%~70% 傷害。
  - 心法內功 (FORCE)：依心法提供常駐護甲與真元護體。
- **戰術護盾與目標指定**：支援護盾吸收傷害、小隊集火目標鎖定，消除重複技能條目。

---

### [Phase 8 & 8.5] 清潔架構、指令解耦與關鍵併發修復 (2026-09-20)
- **方位解耦**：引入 `GridDirection` 與 `RoomMovementService`，徹底分離 MUD 文字方向與 2D 矩陣網格方向。
- **關鍵缺陷修復**：
  - 修復 Miss 傷害未命中時的 Sentinel 判定。
  - 背包物品 `maxStack` 超限保護。
  - 散落儲物袋 (Loot Pouch) 併發存取安全加固。
  - 存檔系統改採原子寫入 (Atomic Write) 防止斷電或崩潰損壞。

---

### [Phase 6 & 7] 三層技能架構、全域戰氣動能與小隊合擊 (2026-09-19)
- **三層技能體系**：平砍套路 (Stance)、主動特攻 (Skill)、道門大招 (Ultimate)。
- **動能系統 (ComboResolver)**：引入 HP / MP / SP 三態資源模型，近戰平砍累積戰氣 (SP)、法系消耗真元 (MP)、刺客積累連擊點 (Combo)。
- **武器與套路綁定**：持劍施展劍法、空手施展拳腳，更換兵刃自動校驗套路相容性。

---

### [Phase 4 & 5] 併發安全加固與前端 ES6 模組化 (2026-09-18)
- **Actor 隱形地雷加固**：全面改採 `VirtualActor` 郵箱排程，建立 `RoomMessageBuffer` 解決跨房間訊息廣播死鎖。
- **前端 ES6 模組化重構**：
  - 建立 `core/state-store.js`、`core/event-bus.js`、`core/cmd-dispatcher.js` 單向資料流核心。
  - 拆分城鎮面板 (`town-panel.js`)、地牢雷達 (`dungeon-panel.js`)、隊伍狀態 (`party-hud-panel.js`)、戰鬥面板 (`battle-panel.js`)。

---

### [Phase 1 ~ 3] 物品單一真相源、穩定識別與技能解耦 (2026-09-16 ~ 2026-09-17)
- **Phase 1**：全域物品原型定義 (`global/items/`)，建立初始 `CharacterSyncService` 同步主角狀態。
- **Phase 2**：引入強型別 `CharacterId` 與雙索引別名系統 (Dual-Index Alias)，解決隊員生命週期識別混亂問題。
- **Phase 3**：建立 `SkillBridgeService` 進行 MUD 技能與 DRPG 技能語意橋接，解除戰鬥迴圈強偶合。

---

### [Phase 0] 基礎資源層與核心並發重構 (原 WALKTHROUGH.md 成果)
- **世界拓撲健全化**：
  - 啟用 4 大核心區域：`newbie_village`（新手村）、`mozhu_mines`（墨竹礦坑）、`snow`（雪亭鎮）、`silverleaf`（銀葉村）。
  - 開機啟動完整性校驗 (`TemplateRepository.validate()`)，消除全圖懸空出口（Dangling Exits）。
- **技能與種族標準化**：
  - 歸納整理 35 門標準武學 JSON（含 10 種怪物天生攻擊）。
  - 標準化 5 大種族（human, wolf, rat, humanoid, undead）。
- **領域模型與線程安全加固**：
  - `Living` Actor 狀態安全封裝：將 `isInCombat`、`combatTargetId`、`nextAttackTime` 改為 `protected volatile`，封裝現程安全行為方法。
  - 抽象 Write-Behind 批次非同步存檔：提取 [`AbstractAsyncBatchPersistenceService<T>`](./src/main/java/com/example/htmlmud/infra/persistence/service/AbstractAsyncBatchPersistenceService.java)，支援 Virtual Threads 背景存檔、雙觸發機制與 `@PreDestroy` 優雅停機。
- **嚴重領域 Bug 修復**：
  - `ResourceType.COIN`：修正扣除金幣時誤扣年齡 (`setAge`) 之致命錯字。
  - `EquipmentProp.getDamageSource()`：更正 `hitRate` 誤傳 `attackSpeed` 參數倒置問題。
  - `WorldPulse.java`：修正怪物重生模除結合性錯誤。
  - `SkillService.java`：修正怪物閃避與招架標準 ID (`mob_basic_dodge`, `mob_basic_parry`)。
- **測試隔離健全**：
  - 測試環境切換至獨立記憶體資料庫 (`jdbc:h2:mem:testdb`)，與本機實體資料庫徹底隔離。
