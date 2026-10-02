# 專案後續架構改善與演化藍圖 (Consolidated Future Improvements & Evolution Roadmap)

> **文件定位與目的**：
> 本文件作為專案所有後續架構演進、機制補完、安全加固與重構任務的**單一權威來源 (Single Source of Truth, SSoT)**。
> 完整收斂並整合了歷史架構規劃與各模型之深度代碼審查報告：
> 1. **戰鬥與成長機制** (源自 `2026-09-18_combat_logic_damage_parry_dodge_and_growth.md`)
> 2. **全實體資料驅動** (源自 `2026-09-18_data_driven_architecture_and_entity_relations.md`)
> 3. **共用標準模型** (源自 `2026-09-22_shared_canonical_model_execution_plan.md`)
> 4. **歷史深度架構審查報告** (整合並收斂 `Claude_Code_Review.md`、`Gemini_Code_Review.md`、`Copilot_Code_Review.md` 與 `Codex_Code_Review.md`)
>
> 經與當前程式庫（Java 21/25 Spring Boot / ES6 前端）精準比對，剔除已完成項目、修正過往宣稱完成之殘留缺陷，並按優先級（P0 ~ P3）與業務領域結構化整理，供後續開發與重構直接依序落地。

---

## 📌 歷史演進指針 (Historical Records)

> 💡 **已完成里程碑與歷史變更**：  
> 本專案所有已落地之里程碑（Phase 0 ~ Phase 11.5 主選單重構）、Bug 修復與架構升級歷程，已全數移轉至專門文件維護：  
> 👉 **請參閱 [`CHANGELOG.md`](../../CHANGELOG.md)**。  
> 本文件僅專注於**「未決定議題、待執行架構任務與後續演化路線圖」**。

---

## 🧭 後續推薦實施路徑 (Next Evolution Paths)

- [x] **路徑 0: 緊急程式與安全漏洞修復 (Hotfixes & Hardening)** (§0 全項)
- [ ] **路徑 A: 共用 Canonical Model 徹底解耦 (Phase 2 ~ 5)** (§1 全項)
- [ ] **路徑 B: 全域數值配置與硬編碼消除 (GameConfig)** (§2 全項)
- [ ] **路徑 C: 戰鬥圓桌判定、盾牌格擋與精力/成長機制補完** (§3 全項)
- [ ] **路徑 D: MUD 端 13 項空殼機制補完** (§4 全項)
- [ ] **路徑 E: UI/UX 全域佈局重構、三種資訊密度與字級規範 (>= 13px)** (§5 全項)
- [ ] **路徑 F: 領域邊界深化與反向依賴反轉 (Phase 10)** (§6 全項)
- [ ] **路徑 G: 前端安全 XSS 轉義、CSP 事件委派與巨石 CSS 模組化** (§7 全項)
- [ ] **路徑 H: 基礎設施持久化加固與優雅關閉** (§8 全項)
- [ ] **路徑 I: 網路安全配額與啟動期資料校驗** (§9 全項)
- [x] **路徑 J: 文件真理源與目錄結構治理** (§10 全項)

---

## 🚨 0. 緊急核心修復與安全性加固 (Critical Hotfixes & Hardening) — Priority: P0

> **來源**：`Codex_Code_Review.md`、`Copilot_Code_Review.md` 深度抽查實地驗證。
> 針對當前系統已存在之資料遺失風險、戰鬥數學溢出與安全漏洞進行第一優先修復。

### 0.1 ✅ [已完成] 房間狀態與地面掉落物持久化閉環 (P0)
> **落地進度**：已於 2026-10-02 完成。
> 1. `RoomService` 注入 `RoomPersistenceService`，於物品丟棄、拾取或快照時非同步排入寫入佇列。
> 2. `RoomPersistenceService.flushBatch()` 實作新舊實體 upsert，完整保存 `droppedItems`。
> 3. `RoomService.spawnInitial()` 於房間初次載入時自動讀取歷史持久化狀態還原。
> 4. `GameItem` 輔助方法補齊 `@JsonIgnore` 與空指針防禦，修復 Hibernate/Jackson 序列化失敗漏洞。
> 5. 新增專屬整合測試 `RoomDroppedItemPersistenceTest` 100% 綠燈通過。

### 0.2 ✅ [已完成] 戰鬥圓桌防禦切片累積總機率收斂 (P0)
> **落地進度**：已於 2026-10-02 完成。
> 1. `DefenseResolver` 導入總防禦率上限（預設 $75\%$），超過時依比例縮放防禦切片（Miss / Dodge / Parry / Block）。
> 2. 致命一擊 (Crit) 僅分配剩餘機率，並保留至少 $5\%$ 作為普通命中 (Normal Hit) 窗口，消除複合高防禦下的機率湮滅 Bug。
> 3. `GameConfig.Combat` 新增 `maxTotalDefenseChance` 與 `minNormalHitChance` 設定與單元測試。
> 4. `OneRollCombatTableTest` 新增複合防禦邊界與 3,000 次蒙地卡羅隨機模擬測試 100% 綠燈通過。

### 0.3 ✅ [已完成] Write-Behind 持久化佇列有界化與可靠關閉 (P0)
> **落地進度**：已於 2026-10-02 完成。
> 1. `AbstractAsyncBatchPersistenceService` 佇列設為有界容量 (10,000)，滿載時回報背壓日誌。
> 2. 引入 `AtomicBoolean accepting` 狀態，關閉時拒收新資料防止漏寫。
> 3. 優雅關閉時等待 worker 虛擬執行緒 `join(3s)`，並同步排空剩餘資料，消除併發 flush 競態。
> 4. 提供 `flushImmediately()` 支援測試與關鍵節點即時排空。

### 0.4 ✅ [已完成] 前端資料輸出轉義與 CSP 事件委派整頓 (P0)
> **落地進度**：已於 2026-10-02 完成。
> 1. `party-modal.js` Gambit 戰術方針動態插值（`conditionLabel`、`targetLabel`、`skillName`、`valDisplay`）全面導入 `escapeHtml()` 轉義防禦。
> 2. `index.html` 移除全數 53 個 inline `onclick` 與 `on*=` 屬性，全面改為 `data-action` 屬性。
> 3. `app.js` 實作全局集中式事件委派 (`initEventDelegation()`)，支援背景遮罩關閉、選單跳轉、快捷指令發送，徹底滿足嚴格 Content Security Policy (CSP) 規範。

---

## 🏛️ 1. 共用 Canonical Model 實施計畫 (Canonical Model Decoupling) — Priority: P1

> **來源**：`2026-09-22_shared_canonical_model_execution_plan.md`
> **核心目標**：讓 MUD 世界模式與 DRPG 小隊模式共用「內容定義、ID 與可持久化狀態」，保留各自的戰鬥迴圈與 DTO，杜絕雙向任意複製與靜態查表。

### 1.1 ✅ [已完成] Phase 2: Canonical Item Model (`ItemDefinition` + `ItemInstance` + `ItemView`) (P1)
> **落地進度**：已於 2026-10-02 完成。
> 1. 建立不可變 `ItemDefinition` record，作為所有靜態定義之唯一來源，統一解析圖示、裝備槽位、傷害加成與消耗品效果。
> 2. 建立純運行期實體 `ItemInstance`，持有 instanceId、definitionId、數量、耐久度、強化等級與動態詞綴。
> 3. 建立不可變展示視圖 `ItemView`，由 `ItemDefinition` 與 `ItemInstance` 動態合成，杜絕可變性洩漏。
> 4. 建立 Spring 託管之 `ItemFactory`，統一管理物品實體、道核遺物與槽位創建。
> 5. 重構 `PartyItemSlot` 全面委派至 `ItemDefinition`，並於 `PartyInventory` 徹底移除 substring 猜測硬編碼。
> 6. 新增專屬單元測試 `CanonicalItemModelTest` (7 測試項) 100% 綠燈通過。

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
  - `TemplateRepository` 仍持有靜態 Map 快取 (`static final Map`) 與單例實例 (`INSTANCE`)。
  - 專案中仍有呼叫點依賴靜態查詢或建構子 fallback (`new TemplateCatalog()`)，導致 Spring 管理生命週期不純淨且跨測試共享狀態。
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

## 🚨 2. 全域數值配置與硬編碼消除 (Hardcoded Values & Data-Driven) — Priority: P1

### 2.1 ✅ [已完成] 全域數值集中管理 — 建立 `GameConfig` (P0)
> **落地進度**：已於 2026-10-02 完成。新增 `GameConfig.java` 支援 Spring Boot 綁定與靜態單例存取，於 `application.yml` 集中管理常數，已替換 `Player`、`PlayerService`、`CombatService`、`LivingService`、`Living` 各處硬編碼，並補齊單元測試 `GameConfigTest` 驗證通過。詳細見 `CHANGELOG.md`。

### 2.2 🟧 經驗值公式 3 套矛盾實作收斂 (P1)
- **現存狀況**：
  - `CombatService.calculateNextLevelXp()`：固定回傳 `10`（placeholder）。
  - `XpProgressionService.calculateNextLevelExp()`：`floor(60 * L^1.6 + 120 * L)`（小隊成員）。
  - `XpService.getRequiredXp()`：`50 * lv^2 * difficulty`（MUD 技能）。
- **改善方案**：統一由 `XpProgressionService` 集中管理所有角色等級曲線與技能熟練度曲線。

### 2.3 🟧 職業門派資料驅動 (`classes.json`) 串接 (P1)
- `data/global/classes.json` 已定義職業屬性與成長模板，但後端部分邏輯仍依賴 `ClassType.java` 靜態列舉。
- **改善方案**：新增 `ClassTemplate` 完整解析器，將升級成長係數完全由 JSON 控制。

### 2.4 🟨 夥伴初始套路 (`learnedStances`) 與貨棧商品 (`INN_GOODS`) 資料驅動 (P2)
- 擴充 `default_companions.json` 增加 `"learnedStances": [...]` 欄位，移除 `PartyService` 代碼保底給予。
- 將 `ShopCommand.INN_GOODS` 寫死的商品列表移至各區域 `shops.json`。
- 開局行囊預設道具由代碼給予改由開局設定檔驅動。
- `BodyPartSelector.DEFAULT_PARTS`（頭部、胸口、四肢等）改由 `RaceTemplate` 提供 `hitParts` 列表。

---

## ⚔️ 3. 戰鬥圓桌判定、格擋與精力成長機制 (Combat Resolution & Progression) — Priority: P1 / P2

### 3.1 🔄 一次擲骰圓桌判定 (One-Roll Combat Table) 收斂與修復 (P1)
> **狀態說明**：核心架構已導入 `DefenseResolver.java`，但需執行 **§0.2** 之總機率防禦上限與歸一化修正，以消除極端屬性下暴擊與命中被吃掉的漏洞。

- **判定扇區與累積邊界**：
  $$\text{[Miss]} \to \text{[Dodge]} \to \text{[Parry]} \to \text{[Block]} \to \text{[Crit]} \to \text{[Normal Hit]}$$
  - 各防禦切片加總需設定總上限（如 $75\%$），避免防守者達成絕對免傷。
  - 未落入前述各事件之剩餘骰值必定為 `Normal Hit`。

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

### 7.1 🔄 前端全域 DOM XSS 防禦與事件委派 (P0)
- **現狀校正**：先前雖引入 `escapeHtml()`，但未全面覆蓋模態框（如 `party-modal.js`）。
- **待執行清單**：
  1. 盤查並轉義 `party-modal.js`、`shop-modal.js`、`skill-drawer.js`、`battle-panel.js` 中的動態字串插值。
  2. 移除 `index.html` 中的 53 個 inline `onclick`，全面改為模組監聽與委派。
  3. 驗證 ANSI 轉 HTML 顏色解析器，確保只接受合法顏色代碼，文字內容經 HTML escape 處理。

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

## 🛡️ 8. 基礎設施持久化加固 (Persistence Hardening) — Priority: P0 / P1

### 8.1 Write-Behind 有界背壓與可靠關閉 (P0)
- 參見 **§0.3**。落實有界佇列、滿載策略、優雅關機等待機制與錯誤重試。

### 8.2 存檔資料版本相容與序列化隔離 (P1)
- 存檔 Entity (`PlayerSaveEntity`、`PartySaveEntity`) 導入資料格式版本號 (`schemaVersion`)。
- 建立升級遷移適配器 (Data Migration Adapter)，確保 JSON 結構演進時舊有存檔可平滑升級，避免資料庫反序列化報錯。

---

## 🌐 9. 網路安全與資料校驗 (Network & Data Integrity) — Priority: P1 / P2

### 9.1 WebSocket 身分驗證、資源配額與本機綁定 (P1)
- **現存缺陷**：
  - `MudWebSocketHandler.afterConnectionEstablished()` 對所有連線直接分配 Actor，無握手驗證；僅有 Origin 白名單限制。
  - 缺乏單一 IP 連線配額、訊息長度限制與指令速率限制 (Rate Limiting)。
- **改善方案**：
  - **本機單人模式防護**：若維持本機開發/遊玩，於設定中強制綁定本機環回介面 (`127.0.0.1`)，明確記載部署限制。
  - **對外/多人部署防護**：
    1. 在握手攔截器 (`HandshakeInterceptor`) 實作 Token / Principal 驗證。
    2. 加入 `WebSocketSessionManager` 限制每連線最大訊息大小（例如 64KB）與頻率限制（例如 20 req/sec）。
    3. 實作存檔存取權限驗證，防止越權操作其他角色資料。

### 9.2 集中式遊戲 JSON 啟動期拓撲驗證 (P1)
- **現存缺陷**：
  - 大量 JSON 模板分散在不同 loader 載入，資料欄位缺失或跨檔引用無效（如引用不存在的技能 ID、物品 ID 或出口房間 ID）需等到執行期才能發現。
- **改善方案**：
  - 建立 `DataIntegrityValidator` 於 Spring Boot 啟動時執行：
    - 驗證所有物品 ID、技能 ID、怪物 ID 的唯一性。
    - 驗證房間出口對應的目標房間是否存在。
    - 驗證怪物的掉落清單物品 ID 是否存在。
    - 產出詳細的啟動資料檢核報告，若有致命缺失直接阻止啟動。

---

## 📚 10. ✅ [已完成] 文件真理源與目錄結構治理 (Documentation & Governance) — Priority: P1

> **落地進度**：已於 2026-10-02 完成。
> 1. 全專案嚴格以 `Party.MAX_PARTY_SIZE = 5` 為單一真相源。
> 2. `ARCHITECTURE.md` 歷史殘留「6 人小隊 HUD」已全數更正為 5 人小隊 HUD。
> 3. `ARCHITECTURE.md` 目錄結構校正：`domain/actor/core/VirtualActor.java`、`RoomMessageBuffer.java`、`MessageFragment.java`、`MessageOutput.java`。
> 4. 消除死鏈接，統一文檔引用。

### 10.1 全專案小隊容量規範統一
- 全專案嚴格以 `Party.MAX_PARTY_SIZE = 5` 為單一真相源。
- 全面清查並修正 `ARCHITECTURE.md`、`GEMINI.md` 等歷史文檔中殘留的「6 人小隊 HUD」等舊字樣，統一為 **5 人小隊**。

### 10.2 架構圖與實體原始碼路徑校準
- 校正 `ARCHITECTURE.md` 的目錄樹：
  - `VirtualActor` 修正為 `domain/actor/core/VirtualActor.java`。
  - 前端靜態資源修正為 `src/main/resources/static/js/` 及其子目錄（`panels/`, `modals/`, `network/` 等）。
  - 移除已過時之檔案路徑描述，聚焦模組職責邊界。

### 10.3 測試環境標準化與驗證基線
- 專案基於 Java 25 與 Spring Boot 4.x。
- 明確規定本機與 CI 必須以 JDK 25 執行 `test.ps1` 或 `mvn clean test`，避免因環境 Java 8 / 17 導致 `UnsupportedClassVersionError`。

---

## 📋 改善項目實施優先序總表 (Execution Priority Matrix)

| 優先序 | 類別代碼 | 改善任務簡述 | 核心影響領域 | 對應章節 |
| :---: | :---: | :--- | :--- | :--- |
| **P0** | ROOM-01 | ✅ 房間狀態與地面掉落物持久化閉環 (`RoomService.record`) (已完成) | 世界狀態防遺失 | §0.1 |
| **P0** | COMB-00 | ✅ 圓桌戰鬥防禦切片累積總機率收斂 (消除 Crit/Hit 湮滅漏洞) (已完成) | 核心戰鬥數學 | §0.2, §3.1 |
| **P0** | ASYNC-01 | ✅ Write-Behind 佇列有界背壓與優雅關閉無競態 (已完成) | 系統併發安全 | §0.3, §8.1 |
| **P0** | SEC-01 | ✅ 前端未轉義動態插值修補與 53 個 inline 事件委派改造 (已完成) | 前端安全性與 CSP | §0.4, §7.1 |
| **P0** | CFG-01 | ✅ 建立 `GameConfig` 集中管理全域硬編碼常數 (已完成) | 基礎設定解耦 | §2.1 |
| **P1** | CANON-01 | ✅ Phase 2: `ItemDefinition` + `ItemInstance` + `ItemView` (已完成) | 物品單一真相源 | §1.1 |
| **P1** | CANON-02 | Phase 3: Single `SkillDefinition` (MUD/DRPG Facets) | 技能定義合併 | §1.2 |
| **P1** | CANON-03 | Phase 4: 徹底移除 `TemplateRepository` 靜態呼叫 (DI 遷移) | 測試與架構隔離 | §1.3 |
| **P1** | CANON-04 | Phase 5: 基於 Snapshot / Outcome 的角色戰鬥同步 | 併發與資料一致性 | §1.4 |
| **P1** | MUD-01 | 補完 `Mob.sayToRoom()` / `attack()` 與 AI 行為 | 怪物 AI 運作 | §4.1 |
| **P1** | MUD-02 | 補完 MUD 端角色升級閉環與經驗值統一 | 角色數值成長 | §2.2, §4.2 |
| **P1** | MUD-03 | 串接 MUD 端 Buff/Debuff 心跳處理引擎 | MUD 戰鬥完整性 | §4.3 |
| **P1** | UI-01 | 全域佈局重構與三種資訊密度 (字級 >= 13px) | 介面破版徹底根治 | §5.1, §5.2 |
| **P1** | ARCH-01 | Domain 層反向依賴反轉 (Phase 10) | 六角形純淨架構 | §6.1 |
| **P1** | DATA-01 | `classes.json` 職業門派 JSON 串接 | 職業成長資料驅動 | §2.3 |
| **P1** | VAL-01 | 集中式遊戲 JSON 啟動期拓撲校驗 (`DataIntegrityValidator`) | 資料啟動防禦 | §9.2 |
| **P1** | DOC-01 | ✅ 文件全域統一 (5人小隊/真實目錄校準/狀態核銷) (已完成) | 文件單一真理源 | §10.1, §10.2 |
| **P1** | NET-01 | WebSocket 本機綁定限制與公開部署身分驗證/配額防禦 | 網路安全邊界 | §9.1 |
| **P2** | COMB-02 | 盾牌獨立格擋 (Block)、破招反擊 (Riposte)、魂系精力破防 | 魂系與深度博弈 | §3.2, §3.3 |
| **P2** | COMB-03 | 主角自由潛能點 (Potential Points) 雙軌升級 | 角色 Build 自由度 | §3.4 |
| **P2** | MUD-04 | RoomFlag / LivingPosture / DamageType 抗性生效 | 世界規則生效 | §4.4 |
| **P2** | UI-02 | HP / MP / SP / SAN 雙軌資源顯示契約 | 技能資源清晰度 | §5.3 |
| **P2** | UI-03 | 探索舞台雙欄化與控制回歸羅盤 | 操作流暢度 | §5.4 |
| **P2** | FE-01 | 巨石 CSS 模組化拆分 (4180+ 行拆分) | 前端可維護性 | §7.2 |
| **P2** | DATA-02 | 夥伴套路、貨棧商品、初始道具完全移入 JSON | 100% Data-Driven | §2.4 |
| **P2** | QUAL-01 | 歷史註解殘留清理 / 死代碼移除 / 模板未找到異常 | 程式庫整潔度 | §4.4, §7.3 |
| **P2** | DB-01 | 存檔 Entity 版本相容 (`schemaVersion`) 與遷移適配器 | 持久化平滑升級 | §8.2 |
