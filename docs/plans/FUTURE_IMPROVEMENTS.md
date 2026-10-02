# 專案後續架構改善與演化藍圖 (Consolidated Future Improvements & Evolution Roadmap)

> **文件定位與目的**：
> 本文件作為專案所有後續架構演進、機制補完與重構任務的**單一權威來源 (Single Source of Truth)**。
> 完整收斂並整合了歷史上所有架構規劃與各模型之深度代碼審查：
> 1. **戰鬥與成長機制** (源自 `2026-09-18_combat_logic_damage_parry_dodge_and_growth.md`)
> 2. **全實體資料驅動** (源自 `2026-09-18_data_driven_architecture_and_entity_relations.md`)
> 3. **共用標準模型** (源自 `2026-09-22_shared_canonical_model_execution_plan.md`)
> 4. **深度架構審查報告** (源自 `Claude_Code_Review.md`、`Copilot_Code_Review.md`、`Gemini_Code_Review.md`、`Codex_Code_Review`)
>
> 經與當前程式庫（Java 21+ Spring Boot / ES6 前端）精準比對，剔除已完成項目，並按優先級（P0 ~ P3）與業務領域結構化整理，供後續開發與重構直接依序落地。

---

## 📌 歷史演進指針 (Historical Records)

> 💡 **已完成里程碑與歷史變更**：  
> 本專案所有已落地之里程碑（Phase 0 ~ Phase 11.5 主選單重構）、Bug 修復與架構升級歷程，已全數移轉至專門文件維護：  
> 👉 **請參閱 [`CHANGELOG.md`](../../CHANGELOG.md)**。  
> 本文件僅專注於**「未決定議題、待執行架構任務與後續演化路線圖」**。

---

## 🧭 後續推薦實施路徑 (Next Evolution Paths)

- [ ] **路徑 A: 共用 Canonical Model 徹底解耦 (Phase 2 ~ 5)** (§1 全項)
- [ ] **路徑 B: 全域數值配置與硬編碼消除 (GameConfig)** (§2 全項)
- [ ] **路徑 C: 戰鬥圓桌判定、盾牌格擋與精力/成長機制補完** (§3 全項)
- [ ] **路徑 D: MUD 端 13 項空殼機制補完** (§4 全項)
- [ ] **路徑 E: UI/UX 全域佈局重構、三種資訊密度與字級規範 (>= 13px)** (§5 全項)
- [ ] **路徑 F: 領域邊界深化與反向依賴反轉 (Phase 10)** (§6.1)
- [ ] **路徑 G: 前端安全 XSS 轉義與巨石 CSS 模組化** (§7 全項)

---

## 🏛️ 1. 共用 Canonical Model 實施計畫 (Canonical Model Decoupling) — Priority: P1

> **來源**：`2026-09-22_shared_canonical_model_execution_plan.md`
> **核心目標**：讓 MUD 世界模式與 DRPG 小隊模式共用「內容定義、ID 與可持久化狀態」，保留各自的戰鬥迴圈與 DTO，杜絕雙向任意複製與靜態查表。

### 1.1 🟧 Phase 2: Canonical Item Model (`ItemDefinition` + `ItemInstance` + `ItemView`) (P1)
- **現存缺陷**：
  - `GameItem` 重複保存 `name`、`description`、`type`、`subType` 等 template 靜態定義。
  - `PartyItemSlot` 同時充當背包實體、裝備規則、效果計算與 UI DTO，職責嚴重混雜。
  - `PartyItemSlot.fromItemTemplate()` 與 `PartyInventory.createFromTemplate()` 存在名稱 substring 猜測 fallback 與硬編碼數值。
- **改善方案**：
  1. **建立 `ItemDefinition`**：作為唯一不可變靜態來源（保留在 template/JSON）。
  2. **建立 `ItemInstance`**：可變執行期實體，僅持有 `instanceId`、`definitionId`、`quantity`、`currentDurability`、`dynamicProps`、`contents`。
  3. **將 `PartyItemSlot` 降級為 `PartyItemViewDto` / `ItemView`**：純前端只讀視圖，禁止被戰鬥或背包服務當作持久化寫回。
  4. **統一 `ItemFactory`**：所有掉落、商店、初始背包均透過 `ItemFactory` 產出 `ItemInstance`，徹底移除靜態 fallback。

### 1.2 🟧 Phase 3: 統一技能定義 Single `SkillDefinition` (MUD / DRPG Facets) (P1)
- **現存缺陷**：
  - `data/global/skills/**` 與 `data/party/party_skills.json` 存在雙重維護與重複定義。
  - `SkillBridgeService` 以大量硬編碼 `if-else / switch` 映射 MUD 與 DRPG 技能。
- **改善方案**：
  1. **Single `SkillDefinition` with Facets**：
     - `identity`: id, name, description, icon, tags, weapon requirements.
     - `mud`: `MudSkillRules` (招式 moves, counter, combo, SP cost).
     - `drpg`: `DrpgSkillRules` (冷卻, 射程, 仇恨, 治療, 護盾, 異常狀態).
     - `bridges`: `SkillBridgeRule[]`（定義 MUD 技能達到特定等級解鎖的 DRPG 技能 ID 清單）。
  2. **資料驅動橋接**：將 `SkillBridgeService` 的映射邏輯改為完全由 JSON `bridges` 驅動，廢棄硬編碼 switch。
  3. **小隊與角色僅持狀態**：`PartyMember` 僅保存 `Map<String, SkillState>`（ID 與冷卻/進度），施放時由 `SkillResolver` 動態合成視圖。

### 1.3 🟧 Phase 4: 徹底移除 `TemplateRepository` 靜態呼叫 (DI Migration) (P1)
- **現存缺陷**：
  - 專案中仍有呼叫點依賴 `TemplateRepository.INSTANCE` 或無參建構子 fallback (`new TemplateCatalog()`)，阻礙單元測試隔離。
- **改善方案**：
  1. 將 `TemplateRepository` 改為純 Spring 管理的非靜態 Bean，全面落實 `TemplateReader` port。
  2. 所有 Service、Factory、Adapter 均透過 Constructor Injection 取得 `TemplateReader`。
  3. 刪除所有 static map 與 static delegate 方法，單元測試注入 `InMemoryTemplateReader`。

### 1.4 🟧 Phase 5: 基於 Outcome 的角色同步機制 (Outcome-based Character Synchronization) (P1)
- **現存缺陷**：
  - `CharacterSyncService` 在開戰/戰後直接互相拷貝 `Player` 與 `PartyMember` 欄位，甚至共享 `LivingStats` 物件引用，存在併發競爭風險。
- **改善方案**：
  1. **開戰前產生 Snapshot**：建立不可變 `BattleParticipantSnapshot` 提供戰鬥初始數據，不持有 Player 引用。
  2. **戰鬥結算產生 Outcome**：戰鬥結束輸出不可變 `BattleOutcome` (包含 HP/MP delta, 經驗獲得, 道具異動, 技能進度, idempotency key)。
  3. **由 `BattleOutcomeApplier` 套用**：在 Player actor thread 中單向應用 outcome，徹底消除雙向同步與重複發獎問題。

---

## 🚨 2. 全域數值配置與硬編碼消除 (Hardcoded Values & Data-Driven) — Priority: P0 / P1

> **來源**：`Claude_Code_Review.md` §一、`2026-09-18_data_driven_architecture_and_entity_relations.md` §5

### 2.1 ✅ [已完成] 全域數值集中管理 — 建立 `GameConfig` (P0)
> **落地進度**：已於 2026-10-02 完成。新增 `GameConfig.java` 支援 Spring Boot 綁定與靜態單例存取，於 `application.yml` 集中管理常數，已替換 `Player`、`PlayerService`、`CombatService`、`LivingService`、`Living` 各處硬編碼，並補齊單元測試 `GameConfigTest` 驗證通過。詳細見 `CHANGELOG.md`。

| 原硬編碼位置 | 寫死數值 | 收納設定欄位 | 狀態 |
|:---|:---|:---|:---:|
| `Player.createSinglePlayer()` | HP=200, MP=100, Coin=100 | `defaults.player.initialHp` / `initialMp` / `initialCoin` | ✅ 已替換 |
| `Player.createSinglePlayer()` | 起始房間 `"newbie_village:inn"` | `defaults.player.spawnRoomId` | ✅ 已替換 |
| `PlayerService.handleRelive()` | 復活地點 `"newbie_village:cemetery"` | `defaults.player.respawnRoomId` | ✅ 已替換 |
| `Player.triggerGcd()` | GCD 預設 `1500` ms | `combat.defaultGcdMs` | ✅ 已替換 |
| `CombatService.calculateDamage()` | 基礎命中率 `0.8`, DEX 修正 `0.01` | `combat.baseHitChance` / `combat.dexHitModifier` | ✅ 已替換 |
| `LivingService.processRegen()` | 心跳頻率 `% 150`, HP 回復 `5%` | `regen.tickModulo` / `regen.hpPercent` / `mpPercent` | ✅ 已替換 |
| `Living.getAttackSpeed()` | 赤手空拳攻速 `2000` ms | `combat.unarmedAttackSpeedMs` | ✅ 已替換 |
| `CombatService.performAttackRound()` | 多次攻擊間隔 `450~551` ms | `combat.multiAttackMinIntervalMs` / `Max` | ✅ 已替換 |

### 2.2 🟧 經驗值公式 3 套矛盾實作收斂 (P1)
- **現存狀況**：
  - `CombatService.calculateNextLevelXp()`：固定回傳 `10`（placeholder）。
  - `XpProgressionService.calculateNextLevelExp()`：`floor(60 * L^1.6 + 120 * L)`（小隊成員）。
  - `XpService.getRequiredXp()`：`50 * lv^2 * difficulty`（MUD 技能）。
- **改善方案**：統一由 `XpProgressionService` 集中管理所有角色等級曲線與技能熟練度曲線。

### 2.3 🟧 職業門派資料驅動 (`classes.json`) 串接 (P1)
- `data/global/classes.json` 已定義職業屬性與成長模板，但尚未載入記憶體，後端仍由 `ClassType.java` 靜態列舉。
- **改善方案**：新增 `ClassTemplate` 與載入解析器，將升級成長係數完全由 JSON 控制。

### 2.4 🟨 夥伴初始套路 (`learnedStances`) 與貨棧商品 (`INN_GOODS`) 資料驅動 (P2)
- 擴充 `default_companions.json` 增加 `"learnedStances": [...]` 欄位，移除 `PartyService` 代碼保底給予。
- 將 `ShopCommand.INN_GOODS` 寫死的商品列表移至各區域 `shops.json`。
- 開局行囊預設道具由代碼給予改由開局設定檔驅動。
- `BodyPartSelector.DEFAULT_PARTS`（頭部、胸口、四肢等）改由 `RaceTemplate` 提供 `hitParts` 列表。

---

## ⚔️ 3. 戰鬥圓桌判定、格擋與精力成長機制 (Combat Resolution & Progression) — Priority: P1 / P2

> **來源**：`2026-09-18_combat_logic_damage_parry_dodge_and_growth.md`

### 3.1 ✅ [已完成] 一次擲骰圓桌判定 (One-Roll Combat Table) 落地 (P1)
> **落地進度**：已於 2026-10-02 完成。重構 `DefenseResolver.java` 導入一元一次擲骰累積區間圓桌算法，依 `[Miss] -> [Dodge] -> [Parry] -> [Block] -> [Crit] -> [Normal Hit]` 依序劃分扇區，消除機率覆蓋偏差。補齊 `PartyMember` 盾牌裝備封裝，並新增 `OneRollCombatTableTest` 與迴歸測試 `PassiveSkillsAndCombatCheckTest` 100% 綠燈通過。詳細見 `CHANGELOG.md`。

- **判定扇區與累積邊界**：
  $$\text{[Miss]} \to \text{[Dodge]} \to \text{[Parry]} \to \text{[Block]} \to \text{[Crit]} \to \text{[Normal Hit]}$$
  - **Miss 閾值**：$\text{clamp}(0.02, 0.25, 0.05 + \max(0, \text{Def.DEX} - \text{Atk.DEX}) \times 0.5\%)$。
  - **Dodge 閾值**：$\text{Skill.dodgeRate} + (\text{Def.DEX} \times 0.8\%) - (\text{Atk.DEX} \times 0.3\%)$。
  - **Parry 閾值**：$\text{Skill.parryRate} + (\text{Def.STR} \times 0.4\%) + (\text{Def.DEX} \times 0.4\%)$。
  - **Block 閾值**：$0.20 + (\text{Def.CON} \times 0.3\%)$（需副手配備盾牌）。
  - **Crit 閾值**：$0.05 + (\text{Atk.DEX} \times 0.2\%)$。
  - **Normal Hit**：其餘未落入前述事件之累積剩餘區間。

### 3.2 🟧 獨立盾牌格擋機制 (Shield Block) 與破招反擊 (Riposte) (P2)
- **盾牌格擋 (Block)**：副手配備盾牌時參與圓桌判定，成功時直接扣減盾牌「格擋值 (Block Value)」：
  $$\text{FinalDamage} = \max(1, \text{RawDamage} - \text{ShieldBlockValue})$$
- **破招反擊 (Riposte)**：俠客 (SWORDSMAN) 或裝備特定心法時，招架 (Parry) 成功機率觸發一次無消耗反擊突刺。

### 3.3 🟧 魂系精力 (Stamina) 消耗與架勢破防 (Poise Break) (P2)
- 防禦動作消耗精力：身法閃避消耗 2 點、兵刃招架消耗 3 點 Stamina。
- **精力枯竭狀態 ($\text{Stamina} \le 0$)**：
  1. 閃避與招架機率強制歸零。
  2. 陷入架勢崩潰 (Stagger / Vulnerable)，遭受攻擊傷害額外提升 **20%**，戰鬥日誌呈現破防提示。

### 3.4 🟨 主角自由潛能點 (Potential Points) 雙軌升級 (P2)
- **夥伴自動成長**：依 `classes.json` 中的 growth 模板自動分配 HP/MP 與四維屬性。
- **主角專屬特權**：升級時除基礎職業成長外，額外入帳 **2~3 點自由道基潛能點**，玩家可於角色選單自由加點打造專屬 Build。

---

## 🧟 4. MUD 端空殼機制補完 (Skeleton Mechanisms) — Priority: P1 / P2

> **來源**：`Claude_Code_Review.md` §二
> **注意**：DRPG 端已完備 FSM/Buff/Threat，以下為 MUD 文字冒險端待補齊的核心機制：

### 4.1 🟥 Mob 基礎行為與反應補完 (P1)
- **`Mob.sayToRoom()` 與 `Mob.attack()`**：補完空殼方法，使怪物具備在房間說話與主動開戰能力。
- **`MobBehavior.handle()` 各 Case 補完**：為 `AggressiveBehavior`、`PassiveBehavior`、`MerchantBehavior` 補齊 `OnPlayerEnter`、`AgroScan`、`RandomMove`、`Respawn` 等具體實現。

### 4.2 🟥 MUD 端角色升級 (Player Level Up) 路徑閉環 (P1)
- `Player.GainExp` 補回等級檢定與升級觸發。
- 實作 `Player.levelUp()` 與 `LivingStateService.processLevelUp()`，完成 MUD 端角色升級廣播與屬性刷新。

### 4.3 🟧 MUD 端 Buff / Debuff 引擎串接 (P1)
- 補完 `ActorMessage.BuffEffect` 處理。
- 恢復 `Living` 的 Buff 集合管理與 `LivingService.tick()` 中的週期結算 (`processBuffs()`)。

### 4.4 🟧 MUD 規則與戰鬥細節補齊 (P2)
- **`LivingPosture` 姿勢系統生效**：戰鬥時切換為 FIGHTING、瀕死/死亡切換為 DEAD、非 STANDING/SITTING 限制移動。
- **`RoomFlag` 15 種旗標生效**：實作 SAFE_ZONE 禁止攻擊、NO_MAGIC 禁止施法、HIGH_REGEN 加速回復等環境檢定。
- **`DamageType` 抗性計算生效**：將 13 種傷害類型與 `LivingStats.resistances` 納入 `CombatService.calculateDamage()`。
- **`MobRank` 倍率生效**：精英怪 (1.5x HP/1.2x Dmg)、首領怪 (3.0x HP/1.5x Dmg) 於生成時正確套用倍率。
- **法力自然回復與道具使用**：補齊 `LivingService.processRegen()` 的 MP 回復，實作 `LivingService.use()`。
- **`Player.forceLogout()` 補全**：解開被註解的存檔、房間移出與全域清理邏輯。
- **死代碼清理**：清理 `CombatService` 中未被呼叫的 `startRound()`、`processSkillExperience()`、`calculateExp()` 等私有死方法。

---

## 🖥️ 5. UI/UX 全域佈局重構與三種資訊密度 (UI Layout & Density) — Priority: P1 / P2

> **來源**：`Copilot_Code_Review.md`、`Gemini_Code_Review.md`

### 5.1 🟧 三種資訊密度分區與字級規範 (>= 13px) (P1)
- **核心原則**：徹底杜絕使用 < 12px 的過小字級硬塞排版，破版由佈局分區與捲動機制解決：
  1. **探索密度**：字級 >= 13px，地圖格子固定尺寸，環境與日誌獨立捲動。
  2. **戰鬥密度**：嚴格採「敵方目標區 → 回合/集火狀態 → 我方小隊摘要」三層分區，卡片不塞全量資料。
  3. **管理密度**：主選單固定 1600x900 規格，獨立視口捲動，每頁 20 項分頁。

### 5.2 🟧 共用頁面 Shell 重構 (Global Layout) (P1)
- 重構 `.game-container` 佈局：
  - **1. Global Header**：模式、當前區域、陣法靈威、全域選單入口。
  - **2. Context Viewport**：探索或戰鬥主舞台 (`min-height: 0`，內部各自捲動)。
  - **3. Party Summary Rail**：固定高度的小隊摘要列（顯示 #1~#5、姓名、等級、4 資源條、關鍵狀態）。
  - **4. Context Action Bar (Footer)**：縮減為 48~64px，僅保留當前模式的高頻立即動作（文字指令、開啟選單、戰鬥撤退/集火），移除重複的存檔/陣法按鈕。

### 5.3 🟧 HP / MP / SP / SAN 雙軌資源顯示契約 (P2)
- 修正 `DrpgStateDto` 將 MP 或 SP 粗暴二選一的遮蔽問題。
- 小隊 HUD 與戰鬥卡片同時顯示法力 (MP) 與戰氣/真氣 (SP)，支援法系與近戰雙修武學資源呈現。

### 5.4 🟨 探索舞台雙欄化與控制回歸雷達 (P2)
- 左欄佔比約 60%（場景資訊 + 3x3 方向羅盤 + NPC 網格卡片 + 地面物品）。
- 右欄佔比約 40%（沉浸式 MUD 故事日誌與戰報，純粹自動捲動）。
- 方向鍵、探查等控制收納於羅盤周邊，杜絕 footer 重複佔位。

---

## 📐 6. 清潔架構與領域邊界 (Clean Architecture) — Priority: P1 / P2

### 6.1 領域層反向依賴反轉 — Phase 10 (P1)
- `LivingService`、`PlayerService`、`GuestBehavior` 等 Domain 層類別，目前仍有 import Application/Infrastructure 層元件。
- 提取 Output Ports（如 `SessionMessageSender`、`GameEventPublisher`），由 Infrastructure 實作，確保領域模型純淨。

### 6.2 指令入口與併發調度收斂 (P2)
- **指令去重**：統一 `UseCommand` 與 `InventoryCommand` 的使用入口，全部收斂至 `ItemUsageService`。
- **Room 併發安全**：修復 `Room.removePlayer()` 先在呼叫線程操作集合、又送 `RoomMessage` 重複操作之隱患，統一走 Actor 訊息通道。
- **類型修正**：修復 `MerchantBehavior.shopId` (int) 與 `MobTemplate.shopId` (String) 型別不符問題。

---

## 🔒 7. 安全加固與前端工程品質 (Security & Quality) — Priority: P0 / P2

### 7.1 🟥 前端全域 DOM XSS 防禦 (P0) — ✅ 已於 2026-10-02 完成
- 建立全域 `constants.js` 與 `ui-utils.js`（含嚴格轉義之 `escapeHtml()`）。
- 全面盤查並替換 `town-panel.js`、`party-hud-panel.js`、`battle-panel.js`、`bag-drawer.js`、`shop-modal.js`、`skill-drawer.js`、`save-modal.js`、`party-modal.js`、`message-log-panel.js` 中的高危 `innerHTML` 拼接。

### 7.2 🟧 巨石 CSS 模組化拆分 (P2)
- 將 4,180+ 行的單一 `style.css` 拆分為模組化樣式檔：
  - `base.css`（全域變數、排版、字型、重置）
  - `town.css`（城鎮舞台、NPC、羅盤）
  - `battle.css`（戰鬥舞台、敵方陣列、技能台）
  - `menu.css`（1600x900 主選單、裝備、Gambit）
  - `hud.css`（小隊狀態軌道、Toast、狀態條）

### 7.3 🟨 邊界防禦與歷史死代碼清理 (P2)
- `TemplateCatalog` 查無模板時統一拋出 `TemplateNotFoundException`，禁止回傳空物件偽裝。
- 清理 `Mob.java:L138-224` 約 90 行註解殘留，整理 `LivingStats` 7 項過期註解欄位。
- 前端 CDN 外部依賴補齊 SRI (Subresource Integrity) 雜湊校驗。

---

## 📋 改善項目實施優先序總表 (Execution Priority Matrix)

| 優先序 | 類別代碼 | 改善任務簡述 | 核心影響領域 | 對應章節 |
| :---: | :---: | :--- | :--- | :--- |
| **P0** | SEC-01 | ✅ 全域 DOM `escapeHtml()` 防禦 XSS 注入 (已完成) | 前端安全防護 | §7.1 |
| **P0** | CFG-01 | ✅ 建立 `GameConfig` 集中管理全域硬編碼常數 (已完成) | 基礎設定解耦 | §2.1 |
| **P1** | CANON-01 | Phase 2: `ItemDefinition` + `ItemInstance` + `ItemView` | 物品單一真相源 | §1.1 |
| **P1** | CANON-02 | Phase 3: Single `SkillDefinition` (MUD/DRPG Facets) | 技能定義合併 | §1.2 |
| **P1** | CANON-03 | Phase 4: 徹底移除 `TemplateRepository` 靜態呼叫 (DI) | 測試與架構隔離 | §1.3 |
| **P1** | CANON-04 | Phase 5: 基於 Snapshot / Outcome 的角色戰鬥同步 | 併發與資料一致性 | §1.4 |
| **P1** | COMB-01 | 一次擲骰圓桌判定 (One-Roll Combat Table) 落地 | 戰鬥核心結算 | §3.1 |
| **P1** | MUD-01 | 補完 `Mob.sayToRoom()` / `attack()` 與 AI 行為 | 怪物 AI 運作 | §4.1 |
| **P1** | MUD-02 | 補完 MUD 端角色升級閉環與經驗值統一 | 角色數值成長 | §2.2, §4.2 |
| **P1** | MUD-03 | 串接 MUD 端 Buff/Debuff 心跳處理引擎 | MUD 戰鬥完整性 | §4.3 |
| **P1** | UI-01 | 全域佈局重構與三種資訊密度 (字級 >= 13px) | 介面破版徹底根治 | §5.1, §5.2 |
| **P1** | ARCH-01 | Domain 層反向依賴反轉 (Phase 10) | 六角形純淨架構 | §6.1 |
| **P1** | DATA-01 | `classes.json` 職業門派 JSON 串接 | 職業成長資料驅動 | §2.3 |
| **P2** | COMB-02 | 盾牌獨立格擋 (Block)、破招反擊 (Riposte)、魂系精力破防 | 魂系與深度博弈 | §3.2, §3.3 |
| **P2** | COMB-03 | 主角自由潛能點 (Potential Points) 雙軌升級 | 角色 Build 自由度 | §3.4 |
| **P2** | MUD-04 | RoomFlag / LivingPosture / DamageType 抗性生效 | 世界規則生效 | §4.4 |
| **P2** | UI-02 | HP / MP / SP / SAN 雙軌資源顯示契約 | 技能資源清晰度 | §5.3 |
| **P2** | UI-03 | 探索舞台雙欄化與控制回歸羅盤 | 操作流暢度 | §5.4 |
| **P2** | FE-01 | 巨石 CSS 模組化拆分 (4180+ 行拆分) | 前端可維護性 | §7.2 |
| **P2** | DATA-02 | 夥伴套路、貨棧商品、初始道具完全移入 JSON | 100% Data-Driven | §2.4 |
| **P2** | QUAL-01 | 歷史註解殘留清理 / 死代碼移除 / 模板未找到異常 | 程式庫整潔度 | §4.4, §7.3 |
