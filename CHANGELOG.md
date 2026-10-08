# 專案版本演進與變更紀錄 (Changelog & Historical Migration Log)

> **文件定位**：  
> 本文件作為專案所有歷史重構、功能上線、架構加固與缺陷修復的**唯一歷史紀錄中樞**。  
> 遵循「每次項目新增、變更或重構落地時必登載」的文檔治理鐵律，記錄專案自開拓以來的演進足跡。

---

## 📌 最新里程碑 (Latest Releases)

### [2026-10-08] 修復選單文字溢出、實裝防禦判定動態差異化、陣法奧義全場演出與金剛怒目嘲諷光環
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
