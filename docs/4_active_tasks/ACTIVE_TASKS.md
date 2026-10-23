# 🎯 ACTIVE_TASKS (當前迭代微任務看板)

> **模式規範**：遵循 Spec-Driven Development 與 AI Agentic Workflow 最佳實踐。  
> **單一焦點原則**：每次對話僅推進 **1 個 Micro-Task**，完成後進行測試驗證與手冊同步，並勾選為 `[x]`。  
> **長遠演進參照**：宏觀藍圖與全局待辦已收錄於 👉 [`docs/3_task_lists/FUTURE_IMPROVEMENTS.md`](../3_task_lists/FUTURE_IMPROVEMENTS.md)。

---

## 📌 當前狀態：進入 Phase 2 第一章序幕垂直切片驗證 (Vertical Slice Phase 2 Active)

> **當前完成事項**：
> 1. ✅ **需求層 (L1)**：確立五大核心目標、純單機 85/15 刷寶黃金權重、SAN 護盾生存化（Option A）與靈識三階透鏡轉譯（Option C）。
> 2. ✅ **規格層 (L2)**：後端 10 大模組 (M1~M9, M3.1)、前端 6 大表現模組 (FE1~FE6)、3 大資料 Schema 契約全數完備並 100% 涵蓋 L1。
> 3. ✅ **任務庫 (L3)**：落實 1:1 鏡像對齊結構，確立專屬執行計畫 [`CODE_CONVERGENCE_AND_REFACTORING_TASKS.md`](../3_task_lists/CODE_CONVERGENCE_AND_REFACTORING_TASKS.md)。
> 4. ✅ **治理憲法**：於全域文件定錨「三大層級遞進治理與使用者審批鐵律」，全量 388 項測試 100% 綠燈守護。
> 5. ✅ **收斂里程碑 (CONV-01)**：三階靈識透鏡轉譯代理 (`CognitiveProxyService`)、營地機能中樞 (`TownHubService` / `TownHubDto`)、營地設施與調息派發 (`TownFacilityCommand`)、16 大標準賽博神通資料矩陣全數落地！
>
> 🎯 **當前唯一推進焦點**：**Milestone VS-01 — 第一章序幕垂直切片：黑煞宗地底礦脈・採礦奴隸與特工覺醒 (Vertical Slice, P1)**。

---

## 📌 當前焦點里程碑：Milestone VS-01 — 第一章序幕垂直切片：黑煞宗地底礦脈・採礦奴隸與特工覺醒 (Vertical Slice, P1)

- **核心目標對齊**：依據三大戰略與【五大核心目標】（世界觀介紹、戰鬥爽快、技能組合、裝備收集、角色養成），徹底取消道心崩潰魔化設計，打造首個完整閉環的純單機 Hack-and-slash DRPG 序章！
- **背景設定融合**：主角在黑煞宗地底奴役通鋪甦醒，特工晶片加載 12%，在萬年母神卵鞘地殼深處面臨塌陷與退化妖獸，收集黑煞煞石，反殺密室修士並繳獲千星科技加密追蹤晶片。
- **資料契約綁定**：全任務 100% 依循 `DUNGEON_SCHEMA.md`、`ITEM_SCHEMA.md` 與 `SKILL_SCHEMA.md`，全量測試維持 376+ 測試 100% 綠燈守護。

### 📋 微任務清單 (Micro-Tasks)

- [ ] **Task 1 (VS-01-LORE-GEOLOGY): 黑煞宗地底礦脈地質與外神卵鞘偽地殼環境重塑 (世界觀介紹)**
  - **目標檔案**：`src/main/resources/data/dungeons/mozhu_mines_b1f.json`、[`DUNGEON_SCHEMA.md`](../2_specifications/data_schemas/DUNGEON_SCHEMA.md)
  - **契約規範**：依循 `DUNGEON_SCHEMA.md` 規範 10x10 地牢網格，確認黑煞煞石岩壁 (`#`)、腐朽枕木坑道 (`.`)、避煞木門 (`D`)、雜役木箱 (`T`)、墨家殘碑 (`E`)、外神卵塊菌絲泥沼 (`X`)、虛月血肉祭壇首領房 (`B`) 地貌 Legend 與遇敵配置 (`encounters.dangerRate=15`)。
  - **驗收標準**：地圖資料 100% 通過 `DataIntegrityValidator` 檢驗；進入地城時第一人稱描述精準呈現克蘇魯外神卵鞘地質異變。

- [ ] **Task 2 (VS-01-AGENT-HUD): 主角特工晶片 HUD 視界 (12%...) 與道基五維初始潛能點加點 (世界觀 + 角色養成)**
  - **目標檔案**：[`Player.java`](../../src/main/java/com/example/htmlmud/domain/actor/impl/Player.java)、[`PartyMember.java`](../../src/main/java/com/example/htmlmud/domain/party/model/PartyMember.java)、`party-overview-tab.js` (預計於 `src/main/resources/static/js/modals/` 模組化新建或擴充 `party-modal.js`)
  - **契約規範**：對齊 Level 2 [`M4_PARTY_AND_FORMATION.md`](../2_specifications/backend/M4_PARTY_AND_FORMATION.md)（道基五維體系）與 [`M5_SKILL_AND_PROGRESSION.md`](../2_specifications/backend/M5_SKILL_AND_PROGRESSION.md)（潛能點與成長曲線）；隊長初始加載「特工覺醒度 12%」，分配 5 點自由道基潛能點（體質、真元、神識、根骨、身法）。
  - **驗收標準**：後端提供潛能點分配 Action 與 DTO 投影，加點即時轉化為 HP/MP/攻防速五維屬性；前端隊伍面板提供直觀加點 UI；專屬單元測試綠燈。

- [ ] **Task 3 (VS-01-CYBER-SKILLS): 首批賽博特工戰技資料驅動配置【神經超頻步】與【強殖角質刃】(戰鬥爽快 + 技能組合)**
  - **目標檔案**：`src/main/resources/data/party/party_skills.json`、[`SKILL_SCHEMA.md`](../2_specifications/data_schemas/SKILL_SCHEMA.md)
  - **契約規範**：依循 `SKILL_SCHEMA.md` 規範，對齊 `party_skills.json` 序章核心戰技：
    - `skill_neural_overclock_step`（神經超頻步）：流派 `CYBER_HACK`、超頻 `tier=1`、`comboRole=PRIMER`、破防削韌 25、提供急速與破綻打斷。
    - `skill_bio_keratin_blade`（強殖角質刃）：流派 `GENE_BIO`、超頻 `tier=1`、`comboRole=DETONATOR`、破防削韌 45、引爆 Primer 造成 1.5x 爆裂。
  - **驗收標準**：`DataIntegrityValidator` 啟動檢驗零警告；在 `DrpgCombatLoop` 戰鬥中驗證發動 Primer 裂魂標記與 Detonator 引爆震盪衝擊波。

- [ ] **Task 4 (VS-01-LOOT-CHIP): 首領掉落【千星科技加密追蹤晶片】與隨機詞綴裝備掉落驗收 (裝備收集 + 376+ 測試守護)**
  - **目標檔案**：`src/main/resources/data/global/items/quest/quest_items.json`、[`ITEM_SCHEMA.md`](../2_specifications/data_schemas/ITEM_SCHEMA.md)、[`DrpgRewardService.java`](../../src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgRewardService.java)
  - **契約規範**：依循 `ITEM_SCHEMA.md` 規範：
    - 黑煞宗礦脈首領（密室畸變修士·宋天衡）掉落專屬任務道核 `item_chip_qianxing_tracker`（千星科技加密追蹤晶片）。
    - 戰鬥結算掉落武器/防具自動隨機掛載前綴與後綴詞條（例如：`超頻的黑煞短匕` 附帶削韌強度）。
  - **驗收標準**：`victory-modal.js` 結算視窗金色 3D 翻牌正確呈現晶片與隨機詞綴裝備；`.\test.ps1` 既有 376 項測試 100% 綠燈守護。


---

## 🏆 已完成里程碑 (Completed Milestones)

- [x] **Milestone CONV-02 — 地牢生存機制與存檔安全鎖收斂 (Dungeon Survival & Save Restrictions Convergence, P1)**
  - Task 1 (CONV-02-DEFEAT-WIPE): 在 `DrpgRewardService` 實裝滅團結算，全員 HP 歸零立即終止戰鬥、重置 `inDungeon = false` 遣返營地客棧 (`exitRoomId` / `newbie_village:inn`)、30% HP 虛弱復甦、扣除 20 點 SAN，透過 `ObjectProvider<SaveGameService>` 解耦觸發 `slot_0` (AutoSave)，`DungeonDefeatSettlementTest` 100% 綠燈。
  - Task 2 (CONV-02-SAVE-RESTRICTIONS): 在 `SaveGameService` 與 `SaveCommand` 實裝地牢手動存檔安全鎖，走廊深處殺機四伏嚴格阻斷，唯有抵達生路石階 (`<`, `>`) 或古仙殘碑 (`EVENT !`) 方可入定存檔。
  - Task 3 (CONV-02-DAOIST-ATTRIBUTES): 前端 `party-equipment-tab.js` 五維屬性標籤對齊道基五維（根骨 STR、體質 CON、身法 DEX、真元 INT、神識 WIS）；`XpProgressionService` 擴充加點別名相容。
  - Task 4 (CONV-02-TEST-VERIFY): 撰寫 `DungeonDefeatSettlementTest.java`（2/2 綠燈），全專案 390 項測試 100% 綠燈守護。

- [x] **Milestone CONV-01 — 營地機能中樞與靈識透鏡核心服務收斂 (Town Hub & Cognitive Proxy Convergence, P1)**
  - Task 1 (CONV-01-COGNITIVE-PROXY): 實作三階靈識認知轉譯代理服務 `CognitiveProxyService`（`<30%` 純仙俠表象、`30%~70%` 異象微顯 Glitch、`>70%` 科技覺醒），專屬測試 `CognitiveProxyServiceTest` 100% 綠燈。
  - Task 2 (CONV-01-TOWNHUB-SERVICE): 實作營地機能中樞服務 `TownHubService` 與 DTO 投影 `TownHubDto`，加載 6 大設施契約，實裝全員一鍵調息休整（回滿 HP/MP/SAN，清除精神萎靡），`TownHubServiceTest` 100% 綠燈。
  - Task 3 (CONV-01-TOWN-DISPATCH): 實作 `TownFacilityCommand` 派發器，收斂前端 `TOWN_FACILITY` 與 `CAMP_REST` 動作，徹底擺脫舊 MUD 房間 exits 拓撲；`RestCommand` 支援安全區委派，`TownFacilityCommandTest` 100% 綠燈。
  - Task 4 (CONV-01-16-SKILLS-DATA): 完整落地 Level 1 十六項神通標準資料契約 (`party_skills.json`)，配置古典仙俠展示名稱（Option C）與底層科技疊影 (`cyberLore`)，`CyberSkillAndItemAffixTest` 與 `WorldDataIntegrityTest` 100% 綠燈通過。

- [x] **Milestone DEC-ALIGN — 四層級文檔對齊與代碼歷史毒瘤清剿 (Four-Tier Alignment & Legacy Purge, P1)**
  - Task 1 (DEC-01-COMBAT / ALIGN-COMBAT-SAN): 重構 `DrpgCombatLoop.java`，徹底拔除隊員 40% 心魔背刺攻擊隊友與畸變體變身敵方首領邏輯，落地純單機精神萎靡 Debuff (-15% 傷害輸出)，清理 `selectRandomTeammateExcept` 死代碼。
  - Task 2 (DEC-02-HTML / ALIGN-FE-CMD-PURGE): 徹底下架 `index.html` 底部文字指令輸入框 (`#cmd-input-container`) 與操作列按鈕 (`toggle-cmd-input`)，`app.js` 導向開發者終端，介面 100% 按鈕與 WASD 走格化。
  - Task 3 (ALIGN-SKILL-SCHEMA): 在 `PartyMemberSkill` 擴充四大賽博修仙流派 (`CyberSchool`)、超頻階級 (`overclockTier`)、連攜角色 (`ComboRole`) 與雙重疊影文本，並在 `party_skills.json` 注入首批賽博特工戰技【神經超頻步】與【強殖角質刃】。
  - Task 4 (ALIGN-ITEM-AFFIX): 建立隨機詞綴實體 `ItemAffix` (PREFIX/SUFFIX)，在 `PartyItemSlot` 支援詞綴池動態掛載、合成名稱格式化與動態數值累加計算。
  - Task 5 (ALIGN-TEST-VERIFY): 撰寫專屬測試 `CyberSkillAndItemAffixTest.java`，執行 `.\test.ps1`，全專案 375 項單元與整合測試 100% 綠燈通過。
  - Task 6 (ALIGN-TOWN-DATA): 在 `data/zones/newbie_village/` 建立營地選單機能設施 `facilities.json`（工坊、晶片實驗室、道場、醫療站、傳送門），對齊 Level 2 M1/M7 規格。
  - Task 7 (ALIGN-DEADCODE-PURGE): 在 `Room.java` 實裝純單機 `broadcast(String message)` 便捷方法，徹底拔除 `Mob.java` 對多人 `broadcastToOthers` 的呼叫。
  - Task 8 (ALIGN-DUNGEON-DATA): 地牢資料 `mozhu_mines_b1f.json` 與 `taiyin_tomb_b1f.json` 補齊 `exitRoomId` 出口契約，並將地貌 Legend 對齊《世界觀聖經》與 `DUNGEON_SCHEMA.md`（外神卵鞘、工程師基岩與外星生化腐殖），`DungeonFloorLoader` 支援 `loreDescription` 別名。
  - Task 9 (ALIGN-FE-EQUIP-AFFIX): 前端 `party-equipment-tab.js` 裝備挑選與 Diff 面板全面支援削韌破防強度 (`poiseBreakPower`) 與隨機道韻詞綴清單展示，精準對齊 5+2 槽位體系。
  - Task 10 (ALIGN-TEST-AND-VALIDATOR): `PartyInventoryAndMadnessTest.java` 增加隊員理智歸零精神萎靡 Debuff (-15% 輸出) 且絕不背刺隊友之單元測試；`DataIntegrityValidator.java` 擴充超頻階級 (0~3) 與裝備槽位合法性啟動期 Fail-Fast 檢驗。全專案 376 測試 100% 綠燈通過。
  - Task 11 (PURGE-SEAL-AND-HARDCODE): 物理刪除 `SealCommand.java` 封印術指令；`PartyMember.MadnessState` 精簡為純粹的 SANE 與 CHAOS (精神萎靡)；`DrpgRewardService` 拔除戰後隊員除名代碼；`DungeonManager` 刪除 65 行手刻地圖 `buildTombB1F()` 全面資料驅動。376 測試全綠。

- [x] **Milestone F3-01 — 古仙陣法動態連攜合擊技與共鳴印記 (Cross Combos & Formation Resonance, P1)**
  - Task 1 (F3-01-COMBO-MODEL): 在 `CombatRules.java` 中建立純函數式跨隊員動態連攜判定 `evaluateCrossComboStep` 與 `CrossComboResolution` 模型，支援 3.5s 連攜時間窗口、同目標接力檢定、雙星合璧 (+25% 傷, +15 削韌)、三才混元 (+50% 傷, +25 削韌, +5 SAN)、四象誅仙 (+100% 傷, +40 削韌, +10 SAN, 終結技就緒)。
  - Task 2 (F3-01-RESONANCE-RESOLVER): 在 `BattleContext.java` 擴充連攜步數、窗口過期時間與共鳴點數 (`resonancePoints`) 追蹤；在 `ComboResolver.java` 實裝 `processCrossCombo` 動態推進連攜與 `executeFormationFinisher` 天外合擊終結絕技發動引擎（消耗 3+ 點共鳴引爆全場巨創貫穿與團隊大回血回道心）；在 `DrpgCombatLoop.java` 全面注入管道。
  - Task 3 (F3-01-DATA-TEMPLATES): 在 `party_skills.json` 建立資料驅動天外合擊終結技能 `combo_formation_finisher`（4.0x 傷害倍率、60 點破防削韌、+50 HP, +15 SAN）。
  - Task 4 (F3-01-FE-PRESENTATION & VERIFY): 前端 `style-07-battle-fx.css` 與 `floating-text.js` 實裝階級連攜提示與天外合擊金紫流光字樣；編寫專屬整合測試 `CrossComboAndFormationResonanceTest.java`（5/5 綠燈），全專案 373 項測試 100% 綠燈通過。

- [x] **Milestone F2-05 — 複合靈氣連鎖反應與狀態引爆 (Elemental Reactions & Primer/Detonator, P1)**
  - Task 1 (F2-05-REACTION-RULE): 在 `CombatRules.java` 中建立九大天道靈氣純函數式元素反應判定與結算模型（`VAPORIZE` 2.0x 傷害 +20 削韌、`ELECTRO_CHARGED` 1.5x 傷害 +10 削韌 800ms 微眩暈與行傳導、`CHAOS_ANNIHILATION` 1.5x 傷害 +50 削韌強制失衡、`COMBUSTION` 1.8x 傷害 +15 削韌），支援精準 Buff 消耗。
  - Task 2 (F2-05-PRIMER-DETONATE): 建立裂魂印記 (Primer) 與引爆戰技 (Detonator) 機制；擴充 `BuffCategory.PRIMER` 與 `UnifiedSkill.isDetonator()` / `isPrimer()`，命中帶有 Primer 目標時瞬間結算剩餘 DoT 總傷害並生成 50% 全場震盪衝擊波。
  - Task 3 (F2-05-COMBAT-INTEGRATE): 在 `DrpgCombatLoop` 單體與 AOE 技能結算中全面整合元素反應與裂魂印爆，包含同排電弧跳躍打擊、全場衝擊波波及、對應 Buff 移除與彩色戰報廣播；在 `party_skills.json` 擴充三昧真火、玄冰寒煞、幽冥劇毒技能，並為穿心瞬影 (Primer) 與裂地崩山 (Detonator) 賦予引爆機制。
  - Task 4 (F2-05-FE-VFX & VERIFY): 前端 `style-07-battle-fx.css` 與 `floating-text.js` 實裝 5 大反應專屬彩色浮動大字 (`.ft-vaporize`, `.ft-electro`, `.ft-chaos`, `.ft-combustion`, `.ft-detonation`)；撰寫專屬測試集 `ElementalReactionAndDetonateTest.java`（7/7 綠燈），全專案 368 項測試 100% 綠燈通過。

- [x] **Milestone F2-02 — 罡氣破防重創與首領多階段狂暴轉階段 (Poise Break & Boss Phases, P1)**
  - Task 1 (F2-02-POISE-STAGGER-CRIT): 在 `CombatRules.java` 實裝 `applyPoiseBreakCrit`，當目標處於 `isPoiseBroken()` 時 100% 暴擊 (`HitOutcome.CRIT`) 享受 1.5 倍致命傷害，在 `DrpgCombatLoop` 與 `ComboResolver` 中單體、群攻、合擊與狂暴打擊全面支援並輸出彩色高亮 `💥【破防重創!】` 戰報。
  - Task 2 (F2-02-BOSS-PHASES-MODEL): 擴充 `BattleEnemy.java` 與 `MobTemplate.java`，支援 `currentPhase`、`maxPhases`、`phaseThresholds`（如 `[70, 30]`）、`isEnraged` 與狂暴攻速加成 (+30%)；DTO 鏡像全面支援；在 `mozhu_mines/mobs.json` 配置畸變大師兄·宋天衡（3 階段，`[70, 30]` 閾值）與 `taiyin_tomb/mobs.json` 守墓道奴（2 階段）。
  - Task 3 (F2-02-PHASE-TRANSITION-LOOP): `DrpgCombatLoop` 與 `ComboResolver` 受擊結算落實轉階段檢查與執行；跌破閾值觸發古代能量過載超載協議（震散全體負面 Debuff、解除眩暈、重聚罡氣且上限攀升 25%、震退打斷全隊員施法前搖並發送衝擊波事件）。
  - Task 4 (F2-02-FE-UI & VERIFY): 前端 `battle-panel.js` 實裝階段徽章 (`.enemy-phase-badge`) 與血條閾值節點標記 (`.hp-phase-marker`)，狂暴首領渲染赤紅過載光環 (`.boss-enraged-aura`)；`floating-text.js` 彈出 `💥 破防重創!` 浮動文字與轉階段衝擊波橫幅；編寫專屬整合測試 `BossPhaseAndPoiseStaggerTest.java`（4/4 綠燈），全專案 361 項測試 100% 綠燈全數通過。

- [x] **Milestone F3-05 — 隊友自動戰術 Gambit 規則鏈引擎 (Companion Gambit AI Chains, P1)**
  - Task 1 (F3-05-GAMBIT-MODEL): 定義 `GambitRule`、`GambitActionType`、擴充 `TacticsCondition`（`ENEMY_IS_CASTING`, `ENEMY_CASTING_INTERRUPTIBLE`, `ENEMY_POISE_BROKEN`, `ENEMY_HP_LESS_THAN`, `ALLY_SAN_LESS_THAN`）與 `TacticsTarget`（`CASTING_ENEMY`, `POISE_BROKEN_ENEMY`, `LOWEST_HP_ENEMY`）；`PartyMember` 增加 `gambitEnabled` (預設 `true`) 開關，`initDefaultTactics` 自動將反制打斷技能（`taibai_interrupt` 或帶 `INTERRUPT` 標籤）配置為 Priority 1 最高優先級。
  - Task 2 (F3-05-EVALUATOR): 擴充 `DrpgEnemyTacticsService.evaluateTacticsCondition` 實現新增戰術條件精準求值；實裝 `resolveEnemyTarget` 智慧解析打斷目標、破防弱點怪與最低氣血怪；`DrpgCombatLoop` 實裝 `gambitEnabled` 防護與戰術單體目標動態綁定。
  - Task 3 (F3-05-PRESETS-DATA): 建立資料驅動戰術範本庫 `data/party/gambit_presets.json`（【斷法攻堅】、【急救續航】、【守禦護坦】、【破防爆發】）；`TemplateRepository` 與 `TemplateCatalog` 自動快取載入；`PartyMember.applyGambitPreset` 支援依同伴技能標籤智慧適配策略鏈；`PartyCommand` 擴充 `party tactics <idx> gambit on|off` 與 `party tactics <idx> preset <id>|list`。
  - Task 4 (F3-05-FE-UI & VERIFY): 前端 `party-tactics-tab.js` 實裝 Gambit 自動戰術總開關狀態徽章、快速切換按鈕、戰術範本庫下拉選單與新增條件/目標選項；編寫專屬整合測試 `CompanionGambitEngineTest.java`（4/4 綠燈），全專案 357 項測試 100% 綠燈全數通過。

- [x] **Milestone F2-01 — 敵方行動預告蓄力條與即時反制打斷 (Combat React, COMBAT-REACT-01, P1)**
  - Task 1 (F2-01-CAST-MODEL): `UnifiedSkill` 擴充 `getCastTimeMs()` / `isInterruptible()` 介面方法；`BattleEnemy` 實裝 `CastingState` 狀態枚舉 (`IDLE`/`CASTING`/`INTERRUPTED`/`COMPLETED`) 與當前施法招式結構。
  - Task 2 (F2-01-CAST-LOOP): `DrpgCombatLoop` 與 `CombatFsmService` 實裝施法前搖窗口、`resolveSkillInterrupt` 控制標籤打斷與 `resolvePoiseBreakInterrupt` 罡氣破防打斷；怪物攻擊結算落實 `finishCast()` 狀態流轉。
  - Task 3 (F2-01-REACT-ACTION): `party_skills.json` 新增反制專屬技能 `taibai_interrupt`（太白斷空）；打斷成功發送 `HitOutcome.INTERRUPTED` 與戰況高亮廣播；DTO 完備支援蓄力進度與反制標籤。
  - Task 4 (F2-01-FE-CASTBAR & VERIFY): 前端 `battle-panel.js` 動態蓄力條、`floating-text.js` 閃耀 `⚡ 打斷!` 浮動文字、`battle-fx-director.js` 與 `style-07-battle-fx.css` 實裝 `.react-interrupted` 震盪受擊光效；編寫專屬整合測試 `CombatReactInterruptTest`（5/5 綠燈），全專案 353 項測試 100% 綠燈通過。

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
  - Task 1: 房間廣播通道與多人遍歷舊代碼清理完畢 (SP-PURITY-01)。
  - Task 2: 房間安全區標籤化與全域出生點 GameConfig 統一驅動 (DATA-CORE-01)。
  - Task 3: 商店 NPC 身份動態化、道核技能反查與夥伴列表配置化 (DATA-CORE-02)。
  - Task 4: 前端地塊動態 Tooltip 支援與看門狗棘輪加固至 3 處 (DATA-GUARD-03)，全專案 338 測試全綠。

- [x] **Milestone Path X — 純單機單體化演進戰略：多人機制、雙軌架構與膠水代碼全域收斂 (SP-01 ~ SP-09)**
  - Task 1 (SP-01): 刪除 `TellCommand` 多人私聊與 `findPlayerByName`。
  - Task 2 (SP-02): 廢止 `StashCommand` 雙向轉存，除役個人背包假帳。
  - Task 3 (SP-03): 存檔寫入與載入全面統一為小隊行囊 (schemaVersion=3)。
  - Task 4 (SP-04): `Room` 房間實體改為單一主角引用，除役多玩家廣播遍歷。
  - Task 5 (SP-05): WebSocket 多開連線自動接管唯一主角，杜絕存檔覆寫競爭。
  - Task 6 (SP-06): 技能模型合一 (`UnifiedSkill`)，消除 MUD/DRPG 技能定義割裂。
  - Task 7 (SP-07): 徹底物理刪除 `SkillBridgeService.java` 膠水代碼。
  - Task 8 (SP-08): 戰鬥引擎大一統，野外戰鬥全面對齊 5 人小隊協同作戰 (`FieldCombatPartyUnificationTest`)。
  - Task 9 (SP-09): 小隊共享金庫與經濟體系大一統 (`UnifiedPartyEconomyTest`)，全專案 331 項測試全綠。

- [x] **Milestone 2 — 核心雙軌實體大一統：Player 與 PartyMember 實體合一與單一真相源治理 (F1-01 / M1 / M4)**
  - Task 1: 行囊大一統 (Unified Party Inventory) — 消除個人 12 格限制與強制 stash 轉存，40 格小隊公共背包為唯一真相源。
  - Task 2: 裝備與核心屬性大一統 (Unified Equipment & Stats) — 消除 5 秒 CompletableFuture 超時阻塞，達成 0ms 零延遲鏡像投影。
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

### 📌 Milestone VS-02 — 第二章：太陰古塚・生化道奴與古修士遺藏 (P2)
- **核心內容**：推進至地牢 `taiyin_tomb_b1f.json`，挑戰雙階段守墓道奴，解鎖五大陣法動態切換與高級詞綴鍛造。

### 📌 Milestone VS-03 — 第三章：千星殖民巨艦墜毀核心・神經網絡逆向工程 (P2)
- **核心內容**：探索飛船殘骸高危區域，解鎖特工晶片 50%+ 深度超頻與虛數靈能流派神通。

---

## 🛡️ 三位一體之「完成定義 (Definition of Done, DoD)」

任何一個 Micro-Task 標記為 `[x]` 前，必須無條件通過以下檢驗：
1. **代碼實作**：100% 遵循「專案第一紅線（資料驅動、零硬編碼、深拷貝完整性）」。
2. **測試驗證**：新增機制必須具備對應測試，且 `.\test.ps1` 全量執行 100% 綠燈全數通過。
3. **文檔同步**：若涉及 API、DTO、配置結構變更，同步更新對應的 `docs/2_specifications/` 模組手冊。
4. **繁體中文**：所有註解、日誌、Commit 訊息一律使用標準繁體中文。
5. **防漂移守護**：嚴格遵循 [`.agents/rules/documentation_alignment_guardrails.md`](../../.agents/rules/documentation_alignment_guardrails.md)，落實「單一真相源 (SSoT)」與「查證優先 (Evidence-First)」，任務卡嚴禁自創/重複抄寫數值，禁止提前假完成。
