# 專案後續架構改善與演化藍圖 (Consolidated Future Improvements & Evolution Roadmap)

> **文件定位與目的**：
> 本文件整合了歷史上所有架構與代碼審查（**Claude Opus Code Review**、**Codex Code Review**、**Copilot Code Review** 及架構小組審查）之核心精華。經過實際代碼現況比對，嚴格剔除已落地項目（Phase 0~8、H2 Console 隔離、WebSocket CORS 白名單、AuthService 驗證與 Bean 注入、指令間解耦等），並補齊跨 AI 審查所挖掘出的關鍵隱患（戰鬥 Miss Sentinel 擊中漏洞、行囊 maxStack 溢出、戰利品袋併發競態、存檔原子寫入與構建配置不一致等）。
> **AI / 開發者守則**：啟動新階段重構、安全加固、併發調整或功能擴展前，必須主動檢索本文件對齊架構決策與缺陷清單。

---

## 📌 進度現況對齊 (Completed Milestones Status)

在推進後續改進前，確認以下項目**已於前期完成並全數通過自動化回歸測試**：

| 已完成里程碑 | 涵蓋範疇與關鍵落實元件 | 狀態 |
| :--- | :--- | :--- |
| **Phase 0: 核心邏輯修復** | 1. `DropCommand` 移除無效 re-add 邏輯<br>2. `EquipCommand` 移除內嵌 unequip 呼叫<br>3. `Player.lookAtMe` 修正為 `.equals()`<br>4. `LivingService.unequip` 恢復裝備解除邏輯<br>5. `RoomService.broadcastJson` 實作完成 | ✅ 已完成 |
| **Phase 1: 物品與屬性單一真相源** | 1. 廢棄 `data/global/items.json`，改以 `data/global/items/**/*.json` 分類目錄唯一收斂<br>2. `PartyItemSlot` 與 `GameItem` / `ItemTemplate` 實現雙向無損轉換<br>3. `CharacterSyncService` 維護世界主體 `Player` 與小隊隊長 `PartyMember` 核心狀態同步 | ✅ 已完成 |
| **Phase 2: 穩定角色識別與生命週期** | 1. 導入 `CharacterId` 強型別值物件，徹底廢除單機寫死 `"p-single"`<br>2. `PartyService` 與 `DungeonManager` 實作 Dual-Index Alias 雙向容錯索引（ID/Name）<br>3. `TemplateRepository` 轉型為 Spring Bean 託管元件 | ✅ 已完成 |
| **Phase 3: 技能語意橋接與戰鬥解耦** | 1. `SkillBridgeService` 建立 MUD 熟練度與 DRPG 戰術招式之執行期語意映射與動態等級縮放<br>2. `DrpgBattleService` 解耦為輕量 Facade，拆分 `DrpgCombatLoop`、`DrpgEnemyTacticsService`、`DrpgRewardService` | ✅ 已完成 |
| **Phase 4: 併發安全與 Actor 隱形地雷加固** | 1. `VirtualActor` 引入 `isActorThread()` 判定與內外分流，消除自身 `join()` 永久死鎖<br>2. `VirtualActor.runLoop()` 實作單訊息例外隔離防護，免疫 RuntimeException 中毒停止<br>3. `RoomMessageBuffer` 廢除 per-room 執行緒池，改以共用 Daemon 排程器統一處理碎片 Flush<br>4. `LivingStats` 強化非負邊界防禦與 `clampToMax()` 數值夾緊，消除序列化順序覆蓋問題 | ✅ 已完成 |
| **Phase 5: 前端組件模組化重構** | 1. 拆解 3,120 行巨石 `drpg-view.js` 為原生 ES6 Modules (`core/`, `panels/`, `modals/`)<br>2. 對齊畫面佈局 6 大區塊（主舞台、方位、戰鬥、隊伍HUD、功能抽屜、訊息日誌）<br>3. 支援全域快捷鍵與彈窗 In-place 更新防閃爍 | ✅ 已完成 |
| **Phase 6: 三層技能架構與小隊合擊系統** | 1. 規範單一真相源：武器技能與職業特性技能統整為標準 `SkillTemplate`（`data/global/skills/**`）<br>2. 職業技能不限武器（`allowedWeapons: []`），如戰士嘲諷/鐵壁、牧師治療/驅散、盜賊潛伏/煙霧<br>3. `party_skills.json` 轉型為多角色小隊合擊技能（Party Combo/Synergy），支援職業組合、人數、陣法與複合能量要求<br>4. 引擎動態適配器、夥伴職業技能自動掛載與全域測試通過 | ✅ 已完成 |
| **Phase 7: 全域統一技能與戰鬥動能系統** | 1. 數值資源收斂為 HP/MP/SP 三槽模型，戰鬥中動態獲取戰氣勢能（普通命中+15、受擊+10）<br>2. 多人合擊解算器（`ComboResolver`）解算組合條件、排除異常失控成員並原子化扣除複合資源<br>3. 前端小隊 HUD 三色條規範化，`skill-drawer.js` 實現 WoW 常用列 + DQ/FF 5 大分類 Tab 抽屜<br>*(註：DRPG 戰鬥迴圈中對舊版 Rage/Combo 欄位的相容性過渡代碼待後續全面退役)* | ✅ 已完成 |
| **Phase 8: 清潔架構與指令解耦 (c 第一階段)** | 1. 消除 Enum 碰撞：DRPG 網格方向轉為 `GridDirection`，小隊戰鬥資源轉為 `CombatResourceType`<br>2. 消除指令重複實作：招募/離隊邏輯統一收斂至 `PartyService`，清理行囊重複封印分支<br>3. 指令間完全解耦 (Zero Inter-Command Coupling)：新增 `RoomMovementService` 解耦移動/觀察，擴充 `SaveGameService` 解耦存讀檔<br>4. 新增 `CleanArchitectureDecouplingTest`，156+ 項測試 100% 通過 (Commit: `e3f9896`) | ✅ 已完成 |
| **Phase 8.5: 關鍵缺陷修復與機制純化衝刺** | 1. `CombatService` 戰鬥 Miss Sentinel `-1` 傷害加乘穿透修復（未命中立即短路免傷）<br>2. `PartyInventory` 行囊 `maxStack` 分槽堆疊防禦，且**徹底拔除建構子寫死道具**，容器回歸純粹機制<br>3. `PartyService` **徹底拔除職業技能 `switch (cId)` 與中文別名 hardcode**，改由 JSON 模板完全資料驅動<br>4. `LivingService` 房間戰利品袋 (`Loot Pouch`) 增加 `synchronized (room)` 消除併發競態<br>5. `SaveGameService` 實作 `.tmp` 暫存檔原子化替換 (`ATOMIC_MOVE`) 與 `0..5` 槽位邊界防禦<br>6. `pom.xml` 統一 Lombok 依賴與註解處理器版本為 `1.18.48`<br>7. 新增 `MechanismPurityAndBugfixTest`，全專案 160 項測試 100% 全綠通過 | ✅ 已完成 |
| **Phase 9: 被動心法裝配與戰鬥檢定 (a 階段)** | 1. 資料模板配置：主角與同伴初始被動心法（`basic_dodge`, `basic_parry`, `basic_breathing`, `cloud_step`, `iron_cloth`, `violet_mist_force` 等）<br>2. 戰鬥被動心法動態檢定：`DefenseResolver` 實裝身法閃避（受擊免傷 + SP）、招架格擋（大幅減傷 + 金鐵交鳴日誌）、護體罡氣（傷害吸收與轉化）<br>3. 前端 WoW 心法典籍：`party-modal.js` 實裝第 3 子頁籤 **【🧘 被動心法】** 與啟用裝配切換<br>4. 新增 `PassiveSkillsAndCombatCheckTest`，164 項測試 100% 全綠通過 (Commit: `dfd4646`) | ✅ 已完成 |
| **Phase 9.1: 戰術護盾、目標指定與技能去重修復** | 1. 戰術方針目標庫擴充：`TacticsTarget` 新增 `FRONT_ROW_ALLY`（前衛肉盾 Tank）、`LEADER`（小隊隊長）、`MEMBER_1..5`（指定隊員）<br>2. 護盾與防禦招式機制化：【金光辟邪護體】（25% 最大生命護盾）與【不動明王】（30% 最大生命金身護體）不再誤擊怪物，`takeDamage` 實裝護盾吸收<br>3. 同伴技能去重：修正 `PartyService` 載入邏輯，依 ID 與名稱同時去重，消除重複技能<br>4. 前端 ANSI 訊息日誌修復：增強 `AnsiUp` 建構子偵測與內建正則解析回退，杜絕 `[1;36m` 亂碼<br>5. 新增 `CompanionShieldAndTacticsTargetTest`，全專案 165 項測試 100% 全綠通過 (Commit: `cbfdc88`) | ✅ 已完成 |
| **Phase 9.5: WoW 風格 Buff / Debuff 體系與戰術防呆** | 1. 基準心跳計數時間模型 (1 Tick = 500ms)，數值可預測、可存檔、純粹可測<br>2. 提取 `Buffable` 介面，`PartyMember` 與 `BattleEnemy` 統一具備狀態容器與吸收機制<br>3. 獨立 `BuffSettlementService`：相同技能刷新時間（護盾取 max）、不同技能共存（Shortest Duration First 依序抵扣）、HoT 週期治療與 DoT 週期傷害跳算<br>4. 戰術方針 AI 防呆：目標已擁有同 ID 未過期 Buff 時自動略過，根除重複施放<br>5. 招式模板全面資料驅動 (`buff` 節點)，新增 `BuffDebuffSystemTest`，全專案 169 項測試 100% 全綠通過 | ✅ 已完成 |
| **Phase 11: 戰鬥狀態機 (FSM)、全域冷卻 (GCD) 與仇恨機制** | 1. WoW 風格 GCD (預設 1200ms) 與 Off-GCD 瞬發支援<br>2. 吟唱狀態機 (Casting FSM)：施法時間、施法條追蹤、重創 (>15% 最大HP) 或死亡打斷<br>3. 怪物獨立仇恨清單 (`threatTable`)、前排 1.3x 仇恨權重、治療全體仇恨均攤、嘲諷鎖定 (`setTaunt`)<br>4. 前端小隊 HUD 與戰鬥卡片動態施法條、GCD 膠囊與怪物目標指示徽章 (`🎯 盯上`)<br>5. 新增 `CombatFsmGcdAndThreatTest`，全專案 179 項測試 100% 全綠通過 | ✅ 已完成 |

---

## 🧭 當前進行中與下一次喚醒執行步驟 (Next Steps Checklist)

依據系統演進規劃，Phase 11 已全數落地，下一步可選擇推進：

- [x] **步驟 1: 第一個 `c` (Phase 8 - 清潔架構與指令解耦)**：已 100% 完成並 commit (`e3f9896`)。
- [x] **步驟 1.5: 關鍵缺陷修復與機制純化衝刺 (Phase 8.5 Bugfix Sprint)**：已 100% 完成 (160 項測試全數通過！)。
- [x] **步驟 2: `a` (Phase 9 - 被動心法 Enable 裝配與戰鬥檢定系統)**：已 100% 完成 (164 項測試全數通過，commit `dfd4646`)。
- [x] **步驟 2.1: 戰術護盾、目標指定、同伴技能去重與 ANSI 顯示修復 (Phase 9.1)**：已 100% 完成 (165 項測試全數通過，commit `cbfdc88`)。
- [x] **步驟 2.5: (Phase 9.5 - WoW 風格 Buff / Debuff 體系與戰術防呆)**：已 100% 完成 (169 項測試全數通過！)。
- [x] **步驟 3: (Phase 11 - 戰鬥狀態機 FSM、全域冷卻 GCD 與獨立仇恨體系)**：已 100% 完成 (179 項測試全數通過！)。
  - [x] 任務 1: WoW 風格 GCD (1200ms) 與 Off-GCD 標籤解析。
  - [x] 任務 2: 吟唱狀態機 Casting FSM、心跳計時與受重創 (>15% HP) 打斷機制。
  - [x] 任務 3: 怪物獨立仇恨清單 (`threatTable`)、前排 1.3x 權重、治療全體均攤與嘲諷鎖定。
  - [x] 任務 4: 戰鬥視圖 DTO 擴充與前端 UI（即時施法條、GCD 膠囊、怪物盯上徽章）。
  - [x] 任務 5: 全量純機制測試 `CombatFsmGcdAndThreatTest` (5 項全新測試)。
- [ ] **下一步備選路徑 A: (Phase 10 - 第二個 `c` 領域邊界深化與反向依賴反轉)**：
  - 重構 Domain 層 Output Ports，解除 `LivingService`、`PlayerService`、`GuestBehavior` 等對外層 Application/Infra 的反向 import。
- [ ] **下一步備選路徑 B: (Phase 12 - 體力值 Stamina、架勢槽 Poise 與破防力竭狀態)**：
  - 增加近戰/招架體力損耗、衝刺/躲避消耗、破招硬直 (Stagger/Poise break) 與力竭易傷狀態。
- [ ] **下一步備選路徑 C: (Phase 13 - 前端安全 XSS 轉義與 CSS 模組化拆分)**：
  - 全面導入 `escapeHtml()` 與將 4,180+ 行 `style.css` 拆解為 `base.css`、`town.css`、`battle.css`、`modal.css`。

---

## 🚨 1. 安全加固與資料健全度 (Security & Data Integrity) - Priority: P0 / P1

### 1.1 H2 Console 本機與生產環境防護 (P0) 【✅ 已落實配置防護】
- **現狀成果**：`application.yml` 與各 profile 已配置限制本機存取，生產環境預設停用 H2 Console。
- **後續注意**：於 CI/CD 與容器化部署檢核清單中維持此一驗證點。

### 1.2 前端 DOM XSS 防禦與字串轉義 (P0)
- **現狀問題**：跨 AI 審查（Codex §3.1、Copilot P1-3、Claude Opus F-6）一致指出：`drpg-view.js`、`mud-core.js` 與 `js/panels/` 模組中存在多處將使用者輸入（主角名稱、自訂稱號）、後端返回之 NPC 對話、物品說明直接使用樣板字串拼接注入 `innerHTML`。
- **改善方案**：
  1. 在前端全域模組（如 `js/core/utils.js`）建立標準 HTML 轉義工具：
     ```javascript
     export function escapeHtml(str) {
         if (!str) return '';
         return String(str)
             .replace(/&/g, '&amp;')
             .replace(/</g, '&lt;')
             .replace(/>/g, '&gt;')
             .replace(/"/g, '&quot;')
             .replace(/'/g, '&#039;');
     }
     ```
  2. 全面審查 `town-panel.js`、`party-modal.js`、`battle-panel.js`、`inventory-modal.js`，將未轉義字串拼接改為 `escapeHtml()` 或優先使用 DOM API 之 `element.textContent`。

### 1.3 存檔原子化寫入與槽位防禦 (P1)
- **現狀問題**（Codex §3.7、Copilot P1-2、Claude Opus F-4）：
  1. **非原子寫入風險**：`SaveGameService.java:171` 直接以 `objectMapper.writeValue(file, saveData)` 覆寫目標檔案。若寫入過程斷電、系統崩潰或行程中斷，將產生不完整的 JSON 檔案，導致玩家存檔永久損壞。
  2. **槽位邊界未封裝**：`SaveCommand` 雖有驗證，但 `SaveGameService.saveGame()` 未對 `slotId` 進行防禦性檢查，若外部傳入負數或任意數值將建立異常檔案。
  3. **多租戶隔離缺失**：目前存檔統一寫入 `saves/slot_*.json`，未來多人情境下未按 `playerId` / `accountId` 進行資料夾隔離。
- **改善方案**：
  1. 導入原子寫入：先寫入同目錄暫存檔 `slot_X.json.tmp`，完成後透過 `Files.move(..., StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)` 完成原子替換。
  2. 在 `SaveGameService` 核心方法加入 `slotId` 邊界校驗（`0 <= slotId <= TOTAL_MANUAL_SLOTS`），防範 Path Traversal。
  3. 存檔路徑設計改為 `saves/{playerId}/slot_{slotId}.json`，強化用戶空間隔離。

### 1.4 WebSocket CORS 來源限制 (P1) 【✅ 已完成白名單限制】
- **現狀成果**：`WebSocketConfig.java` 已改為自配置檔讀取允許之 Origin 清單，消除 CSWSH 跨站劫持風險。

### 1.5 身份驗證防禦與密碼加密 Bean 注入 (P1) 【✅ 已完成校驗與 Bean 注入】
- **現狀成果**：`AuthService` 已導入 `@Bean PasswordEncoder` 依賴注入，並在註冊流程落實使用者名稱與密碼長度格式校驗。

### 1.6 房間戰利品袋 (Loot Pouch) 併發競爭修復 (P1)
- **現狀問題**（Copilot P1-5、Claude Opus F-5）：
  `LivingService.java:166-179` 中，怪物死亡產生戰利品時，先透過 `room.getItems().stream()` 查詢是否存在現有的 `【散落的儲物袋】`，若存在則直接呼叫 `targetPouch.getContents().addAll(drops)`。在多怪同時被 AOE 擊殺（各在獨立的虛擬執行緒執行）情境下：
  - 兩個執行緒可能同時判定「不存在儲物袋」而各自建立新的儲物袋丟入房間。
  - 兩個執行緒同時取得同一儲物袋並同時對非執行緒安全的 `ArrayList` 執行 `addAll()`，造成併發修改異常 (`ConcurrentModificationException`) 或物品遺失。
- **改善方案**：
  - 將戰利品收納操作移交至 `Room` / Actor 內部同步執行，或針對 `targetPouch` 進行鎖定/使用執行緒安全集合。

---

## ⚔️ 2. 核心戰鬥與物品規則防禦 (Combat & Inventory Integrity) - Priority: P1

### 2.1 MUD Miss Sentinel `-1` 傷害穿透修復 (P1)
- **現狀問題**（Copilot P1-4、Claude Opus F-1）：
  在 `CombatService.java:273-282`：
  ```java
  int rawDmg = calculateDamage(self, target);
  double skillDmg = skill.template().getMechanics().damage() + ...;
  rawDmg += skillDmg; // 💥 若 calculateDamage 返回 -1 代表 Miss，此處直接加乘技能傷害！
  int dmgAmount = (int) (rawDmg * action.damageMod());
  ```
  `calculateDamage` 判定未命中時以 `-1` 表示 Miss。但後續代碼未檢查 `-1`，直接累加正數的 `skillDmg`，若招式倍率 `action.damageMod()` 大於 0，將使本應未命中的攻擊轉為正數傷害直接扣減目標 HP！
- **改善方案**：
  在 `calculateDamage()` 返回後立即判定：
  ```java
  int rawDmg = calculateDamage(self, target);
  if (rawDmg < 0) {
      // 確定為未命中，格式化 Miss 戰鬥日誌並直接 return，不得累加傷害
      broadcastMissMessage(self, target, action);
      return;
  }
  ```

### 2.2 物品堆疊上限 `maxStack` 溢出防禦 (P1)
- **現狀問題**（Copilot P2-2、Claude Opus F-3）：
  `PartyInventory.java:58-63` 與 `LivingService.addItem()` 在將物品放入行囊時：
  ```java
  if (isSameItemId(templateId, slot.getItemId()) && slot.isStackable()) {
      slot.setCount(slot.getCount() + count); // 💥 未檢查 slot.getMaxStack()
      return true;
  }
  ```
  完全忽略了物品模板定義之 `maxStack`（例如藥品上限 99、特殊符籙上限 10），可被無限累加至數千甚至數萬，嚴重破壞背包空間平衡。
- **改善方案**：
  累加時計算可容納差額 `available = slot.getMaxStack() - slot.getCount()`：
  1. 若 `count <= available`，直接累加。
  2. 若 `count > available`，填滿當前槽位至 `maxStack`，剩餘數量在行囊未滿時尋找下一個同類未滿槽位或開闢新槽位；若行囊已滿則拒絕放入或掉落地面。

### 2.3 `PartyInventory` 模糊前綴匹配 (Substring Fallback) 收斂 (P1)
- **現狀問題**（Codex §3.3、Copilot P2-3、Claude Opus F-2）：
  `PartyInventory.isSameItemId()` 會自動將 `:` 冒號之後的子字串提出來做 `equalsIgnoreCase` 比較。這導致 `DataNamespaceIntegrityTest` 所建立的命名空間防護網在運行期被完全繞過（例如 `mud:sword` 與 `drpg:sword` 被混為一談），且隱蔽了錯誤的資料配置 ID。
- **改善方案**：
  移除或大幅限縮模糊子字串比對，全面改用精確的 Canonical Item ID。若有向後相容需求，僅允許在特定遷移適配器中進行明確映射，不可在底層容器中靜默模糊匹配。

### 2.4 `CharacterSyncService` 雙向同步欄位邊界與責任清單 (P2)
- **現狀問題**（Codex §3.5、Copilot P2-1、Claude Opus F-7）：
  設計文件標註 `CharacterSyncService` 提供「Player 與 PartyMember 之完整雙向同步」，但實際代碼僅同步 HP、MP 等少數欄位，裝備、進階屬性、狀態標籤並未真正全量聯動，容易引發其他開發者對資料同步完整性的誤判。
- **改善方案**：
  1. 在 `CharacterSyncService` 類別頭部明確標註其負責之同步邊界（如：僅同步即時戰鬥核心計量，裝備與背包以 `PartyInventory` 為唯一真相源）。
  2. 補齊主角與隊長之間遺漏的即時狀態聯動（如：死亡狀態、等級突破同步）。

---

## 🏛️ 3. 清潔架構與領域邊界 (Clean Architecture & Decoupling) - Priority: P1 / P2

### 3.1 領域層反向依賴反轉 (Domain Dependency Inversion) - Priority: P1 (Phase 10 重點)
- **現狀問題**：
  `LivingService`、`PlayerService`、`GuestBehavior`、`RoomService` 等 Domain 層類別，仍直接 `import` 了 Application / Infrastructure 層的元件（如 `WebSocketSession`、`MudWebSocketHandler`、`AuthService`）。
- **改善方案**：
  依循 DDD 原則，Domain 層不可依賴外層。提取 Output Ports（輸出介面）：例如 `SessionMessageSender`、`SessionContext`、`GameEventPublisher`，由 Infrastructure 層實作這些介面並注入 Domain 服務。

### 3.2 命名衝突 Enum 拆分與語意收斂 - Priority: P2
- **現狀進度**：
  - `GridDirection`（DRPG 4 向）與 `CombatResourceType`（小隊戰鬥資源）已於 Phase 8 完成引入並消除衝突。
- **後續收尾**：
  - 將殘留之 MUD 10 方向 `net.mud.world.Direction` 正式更名為 `WorldDirection`。
  - 在共用層提供 `DirectionAdapter` 處理正交方向轉換 (`WorldDirection.NORTH ↔ GridDirection.NORTH`)。

### 3.3 指令冗餘消除與責任收斂 - Priority: P2
- **現狀進度**：
  - 招募/離隊邏輯已於 Phase 8 收斂至 `PartyService.dismissMember`。
  - 存讀檔邏輯已於 Phase 8 收斂至 `SaveGameService`。
- **後續收尾**：
  - 清理 `use` 指令在 `UseCommand` 與 `InventoryCommand` 的雙重入口，統一由 `ItemUsageService` 處理。

---

## ⚡ 4. 併發安全與 Actor 執行緒健全度 (Concurrency & Actor Integrity) - Priority: P1 【✅ 已完成】

> **完成狀態**：已全數實作防禦機制，並建立 `ConcurrencyAndActorSafetyTest` 驗證套件，全數通過自動化回歸測試。

- **4.1 VirtualActor 自死鎖防禦**：`isActorThread()` 判定與內外分流已落地。
- **4.2 RoomMessageBuffer 執行緒池洩漏修復**：改由全域 Daemon 排程器統一派發 Flush。
- **4.3 VirtualActor 例外中毒防護**：事件處理迴圈加上 `try-catch(Throwable t)` 保持 Mailbox 運轉。
- **4.4 LivingStats 欄位封裝與執行緒邊界**：數值欄位私有化並加入 `clampToMax()` 邊界防禦。

---

## 📦 5. 資料驅動架構演進與代碼健康 (Data-Driven & Code Health) - Priority: P2

### 5.1 Canonical Data Model 與 TemplateCatalog 容錯行為修正 (P2)
- **現狀問題**（Codex §3.2、Copilot P2-5、Claude Opus F-8）：
  `TemplateCatalog` 與 `TemplateRepository` 在查無模板時，部分路徑會 fallback 到無參空建構子或回傳預設空白物件。這會讓 JSON 拼寫錯誤（Typo）在運行期被靜默忽略，造成難以追蹤的怪異行為。
- **改善方案**：
  在模板查無項目時明確拋出 `TemplateNotFoundException` 或回傳 `Optional.empty()`，並在啟動期（如 `TemplateIntegrityTest`）實行嚴格的啟動驗證。

### 5.2 `PartyInventory` 無參建構子副作用防護 (P2)
- **現狀問題**（Claude Opus 專有發現）：
  `PartyInventory.java:24-40` 之預設建構子中直接呼叫 `addItem("taiyin_pill", 3); ...`。當 Jackson 進行反序列化（例如讀取存檔）時，若先使用預設建構子初始化物件，會預先塞入 4 種測試道具，隨後反序列化的真實存檔資料又被追加進去，造成玩家存檔中憑空多出初始物品。
- **改善方案**：
  預設建構子必須保持乾淨（不帶業務副作用）。初始贈送物品之邏輯應明確移至 `NewGameCommand` 或角色創建工廠（`CharacterCreationService`）。

### 5.3 歷史死代碼與命名規範清理 (P2)
- **清理標的**（Claude Opus 專有發現）：
  1. `CombatService` 中完全無外部與內部調用之 private 死方法：`startRound()`、`afterAttack()`、`processSkillExperience()`。
  2. `LivingStateService.calculateNextLevelXp()`：目前為硬編碼固定返回 `10` 之 placeholder，需對齊經驗值公式。
  3. `CombatService.java:317`：方法命名拼寫錯誤 `CombineString`（大寫 C），應修正為標準駝峰 `combineString`。
  4. 註解掉的 `PlayerLoginListener` 與 `singleplayer_v2.html` 殘留之 `alert('CORPSE_DETAIL')` 測試代碼。

### 5.4 構建與環境配置補強 (P2)
- **pom.xml Lombok 版本不一致**（Claude Opus 專有發現）：
  `pom.xml` 中宣告之 Lombok dependency 版本為 `1.18.48`，但 `maven-compiler-plugin` 之 `annotationProcessorPaths` 中的 Lombok 版本卻為 `1.18.42`。此版本落差在 Java 25 環境下可能導致編譯器與註解處理器行為不一致或警告，應統一版本。
- **Git 忽略**：`.gitignore` 目前僅忽略 `autosave.json`，需補齊 `/saves/*.json`。

---

## 🎨 6. 前端現代化與組件模組化 (Frontend Architecture) - Priority: P2

### 6.1 巨石 JS / CSS 檔案模組化拆分 【JS 部分 ✅ 已完成】
- **現狀成果**：巨石 `drpg-view.js` 已成功拆分為 10 個核心 ES6 Modules。
- **待跟進項目**：超過 4,180 行之 `style.css` 待後續依模組拆分為 `base.css`、`town.css`、`battle.css`、`modal.css`。

### 6.2 前端異常防禦與無障礙優化 (P2)
- **DOM ID 存取保護**：`mud-core.js` 中 `updateStats()` 加入 optional chaining 或 null 檢查防範 `TypeError`。
- **無障礙 (ARIA)**：所有彈窗補全 `role="dialog"`、`aria-modal="true"` 與 ESC 鍵關閉支援。
- **外部 CDN 資源安全**：靜態引用的 CDN 資源（如 `ansi_up`）補上 Subresource Integrity (`integrity`) 驗證碼。

---

## 🧪 7. 測試覆蓋率與文檔一致性 (Test Coverage & Doc Sync) - Priority: P2 / P3

### 7.1 關鍵元件測試補完 (P2)
- 新增 `CombatMissAndScalingTest`：驗證 Miss 情況下傷害必定為 0，且不觸發目標受傷事件。
- 新增 `ItemStackLimitTest`：驗證 `PartyInventory.addItem` 在超過 `maxStack` 時正確分槽與滿載拒絕。
- 新增 `LootPouchConcurrencyTest`：模擬多線程怪物同時死亡，驗證房間儲物袋不遺失物品且無例外。
- 新增 `AtomicSaveIntegrityTest`：驗證存檔以臨時檔原子替換，並拒絕非法槽位 index。

### 7.2 文檔數值與代碼真實性同步 (P3)
- **隊伍人數上限對齊**（Copilot P3-2）：
  `Party.java:20-21` 定義之隊伍上限為 **5 人**（隊長 + 4 名夥伴），部分歷史文檔（如舊版 README）曾誤寫為「6 人隊伍」。後續所有新撰寫與修訂之文檔一律嚴格對齊代碼上限 5 人。
- **動態測試數量表記**：
  文檔中不再寫死固定測試數字，統一改以「全專案自動化測試全綠通過」或以 Maven 測試輸出為準，避免代碼擴充後文檔數字迅速過期。

---

## 📋 改善項目實施優先序總表 (Execution Priority Matrix)

| 優先序 | 項目類別 | 改善任務簡述 | 狀態 / 影響 |
| :---: | :--- | :--- | :--- |
| **P0** | 安全防禦 | 1. 建立全域 `escapeHtml()` 杜絕前端 XSS<br>2. H2 Console 外部存取防禦（✅ 已完成） | 消除重大安全漏洞與注入風險 |
| **P1** | 核心戰鬥 | 1. **修復 Miss Sentinel `-1` 傷害加乘穿透 Bug**（✅ 已完成）<br>2. **修復物品行囊忽略 `maxStack` 無限堆疊 Bug**（✅ 已完成）<br>3. **Phase 9: 被動心法裝配與 DefenseResolver 檢定系統**（✅ 已完成） | 戰鬥數值、閃避招架與行囊規則健全 |
| **P1** | 併發與資料 | 1. **修復房間戰利品袋 (`Loot Pouch`) 多怪併發掉落競態**（✅ 已完成）<br>2. **實作存檔 `.tmp` 原子化寫入與槽位範圍校驗**（✅ 已完成）<br>3. **夥伴別名與職業特徵技能純資料驅動化**（✅ 已完成）<br>4. VirtualActor 自死鎖防禦（✅ 已完成） | 杜絕資料損壞、丟寶與硬編碼邏輯 |
| **P1** | 構建一致 | 1. **對齊 `pom.xml` 中 Lombok 依賴與註解處理器版本**（✅ 已完成） | 確保 Java 25 編譯環境穩定性 |
| **P2** | 代碼與架構 | 1. 收斂 `PartyInventory` 模糊前綴比對<br>2. 清理 `PartyInventory` 無參建構子副作用（✅ 已完成）<br>3. 清理 `CombatService` dead code 與命名大小寫<br>4. 領域層依賴反轉 (Phase 10 Output Ports) | 提升架構整潔度與可維護性 |
| **P2** | 前端工程 | 1. `style.css` 巨石 CSS 元件模組化拆分<br>2. 前端 DOM 防禦與 CDN 資源 SRI 驗證<br>3. **WoW Spellbook Tab 3 被動心法面板實裝**（✅ 已完成） | 降低樣式維護難度，提升用戶體驗 |
| **P2** | 測試工程 | 1. 補齊 Miss、Stack、Loot 併發與原子存檔測試（✅ 已完成）<br>2. 補齊被動技能與戰鬥檢定純機制測試（✅ 已完成）<br>3. 修正測試間靜態上下文隔離 | 健全自動化回歸防護網 (164/164 全綠) |

