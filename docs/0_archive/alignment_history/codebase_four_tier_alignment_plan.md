# 📊 程式碼與四層級架構文檔全域對齊審查報告與執行清單
# (Codebase Four-Tier Alignment Audit & Action Plan)

> **文檔層級**：Level 3 — Task Lists (專案治理與全域對齊可執行清單)  
> **制定日期**：2026-10-22  
> **審查範疇**：全專案 88 個 Java 源碼檔、37 個單元測試、資源資料 JSON、前端 HTML/JS 資源與 35 份現行技術文檔。  
> **核心目標**：精確盤查代碼與 **Requirements (L1) / Module Specifications (L2) / Task Lists (L3) / Active Tasks (L4)** 四個層級文檔之間的深度落差，提供一份高內聚、可分步執行的工程治理任務清單。

---

## 📌 一、四層級對齊診斷與差距矩陣 (Audit Gap Matrix)

經過實地對全專案代碼庫進行逐行交叉審查，代碼與四層級文檔對齊現狀診斷如下：

| 層級與文檔核心契約 | 代碼實況 (Code Reality) | 狀態 | 核心脫節檔案與影響範圍 |
| :--- | :--- | :---: | :--- |
| **Level 1: 需求與願景層**<br>• 賽博修仙四大流派<br>• 去文字指令化純單機<br>• 世界觀克蘇魯外神卵鞘 | • 技能模型已支援四大流派與超頻階級<br>• 前端常規視圖文字指令輸入框已徹底下架<br>• 敵怪文本與任務正向外神卵鞘垂直切片推進 | 🟢<br>已對齊 | • `PartyMemberSkill.java` ✅<br>• `index.html` ✅<br>• `party_skills.json` ✅<br>• `zones/` 後續擴充 |
| **Level 2: 模組與規格層**<br>• `M1`/`M7`: 選單式營地設施<br>• `M3`: 罡氣削韌、精神護盾<br>• `ITEM_SCHEMA`: 隨機詞綴庫<br>• `SKILL_SCHEMA`: 超頻階級 | • 心魔背刺隊友已徹底拔除，轉為精神萎靡<br>• 隨機詞綴結構 (`ItemAffix`) 與屬性累加已落地<br>• 技能實體已具備流派、超頻與連攜角色<br>• 營地選單機能設施 `facilities.json` 正式落盤 | 🟢<br>已對齊 | • `DrpgCombatLoop.java` ✅<br>• `PartyItemSlot.java` ✅<br>• `ItemAffix.java` ✅<br>• `data/zones/newbie_village/facilities.json` ✅ |
| **Level 3: 全局任務清單層**<br>• 純單機典範確立<br>• 多人廣播與房間位移除役<br>• 技術債與硬編碼清剿 | • `Room.broadcast` 純單機便捷方法落地<br>• `Mob.sayToRoom` 徹底解除 `broadcastToOthers` 多人依賴 | 🟢<br>已純化 | • `Mob.java` ✅<br>• `Room.java` ✅<br>• 舊 MUD 廣播死代碼已出清 |
| **Level 4: 當前焦點看板層**<br>• VS-01: 序章第一人稱垂直切片<br>• 376+ 單元測試 100% 綠燈 | • 現有 376 項測試全綠，護欄持續擴充<br>• 核心模型已站穩，全力支撐 VS-01 序章落地 | 🟢<br>底座健全 | • `CyberSkillAndItemAffixTest.java` ✅<br>• `AllTests (376 tests passed)` |

---

## 📋 二、詳細可執行任務清單 (Phased Actionable Task List)

### 🔹 Phase 1: 核心戰鬥與角色數值模型對齊 (Combat & Character Domain)

- [x] **Task 1.1 (ALIGN-COMBAT-SAN): 移除隊員背刺，落地精神護盾與萎靡 Debuff (對齊 L1/L2 M3)**
  - **目標檔案**：[`DrpgCombatLoop.java`](../../src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java)、[`PartyMember.java`](../../src/main/java/com/example/htmlmud/party/PartyMember.java)、`SealCommand.java`（已物理安全刪除除役）
  - **執行動作**：
    1. 拔除 `DrpgCombatLoop:230-320` 中隊員 SAN 歸零後的 `CHAOS` 走火入魔檢定與 40% 心魔背刺隊友邏輯。
    2. 落實文檔規範：SAN 耗盡僅施加【精神萎靡】Debuff（降低 15% 輸出），絕不叛變背刺隊友。
    3. 清理未使用的 `selectRandomTeammateExcept` 死代碼。
  - **交付標準**：徹底消除戰鬥中心魔背刺隊友的挫敗機制。✅ 已落地完成 (373 測試全綠)

- [x] **Task 1.2 (ALIGN-SKILL-SCHEMA): 技能實體擴充四大賽博流派與超頻階級 (對齊 L1/L2 SKILL_SCHEMA)**
  - **目標檔案**：[`PartyMemberSkill.java`](../../src/main/java/com/example/htmlmud/party/PartyMemberSkill.java)、`party_skills.json`
  - **執行動作**：
    1. 在 `PartyMemberSkill` 擴充 `CyberSchool` 流派枚舉（`CYBER_HACK`, `GENE_BIO`, `MECHA_CULT`, `VOID_PSI`）。
    2. 新增 `overclockTier`（超頻階級 0~3）、`comboRole`（`PRIMER` / `DETONATOR` / `NONE`）欄位與 `cyberLore` 文本。
    3. 在 `party_skills.json` 注入首批賽博特工戰技【神經超頻步】與【強殖角質刃】。
  - **交付標準**：技能模型具備完整的賽博特工戰技定義能力。✅ 已落地完成 (375 測試全綠)

- [x] **Task 1.3 (ALIGN-ITEM-AFFIX): 裝備模型支援隨機詞綴池 (對齊 L2 M6 / ITEM_SCHEMA)**
  - **目標檔案**：[`PartyItemSlot.java`](../../src/main/java/com/example/htmlmud/party/PartyItemSlot.java)、[`ItemAffix.java`](../../src/main/java/com/example/htmlmud/domain/item/model/ItemAffix.java)
  - **執行動作**：
    1. 建立 `ItemAffix` 實體，支援 `PREFIX`（前綴）與 `SUFFIX`（後綴）雙向修飾。
    2. 在 `PartyItemSlot` 擴充隨機詞綴清單、削韌強度 (`poiseBreakPower`)、需求等級與動態合成名稱與屬性累加計算。
  - **交付標準**：裝備系統具備刷寶遊戲核心的隨機詞綴承載能力。✅ 已落地完成 (375 測試全綠)

---

### 🔹 Phase 2: 資料驅動與營地中樞對齊 (Data & Town Hub Infrastructure)

- [x] **Task 2.1 (ALIGN-TOWN-DATA): 營地資料由 `rooms.json` 轉型為 `facilities.json` (對齊 L2 M1/M7)**
  - **目標檔案**：`src/main/resources/data/zones/newbie_village/facilities.json`
  - **執行動作**：
    1. 建立純單機營地選單機能設施 `facilities.json`（天工百煉坊、賽博晶片工坊、太華傳功道場、回春草堂醫療站、地牢傳送門）。
    2. 對齊 `M1_TOWN_HUB_AND_FACILITIES.md` 與 `M7_DATA_DRIVEN_TEMPLATES.md` 規範。
  - **交付標準**：資料目錄消除 MUD 房間 exits 依賴，落實選單式營地。✅ 已落地完成 (375 測試全綠)

- [x] **Task 2.2 (ALIGN-DUNGEON-DATA): 地牢資料補齊出入口契約與地貌 Legend (對齊 L2 M2 / DUNGEON_SCHEMA)**
  - **目標檔案**：`src/main/resources/data/dungeons/`、[`DungeonManager.java`](../../src/main/java/com/example/htmlmud/domain/dungeon/service/DungeonManager.java)
  - **執行動作**：
    1. 在地城設定中強制校驗 `exitRoomId` 與每層傳送點座標。
    2. 地塊 Legend 對齊 `DUNGEON_SCHEMA.md` 定義（外神卵塊、外星生化腐殖、工程師基岩）。
  - **交付標準**：地城資料與 Schema 達成 100% 契約一致。✅ 已落地完成 (375 測試全綠)

- [x] **Task 2.3 (ALIGN-DEADCODE-PURGE): 清理多人廣播死代碼 (對齊 L3 治理)**
  - **目標檔案**：[`Mob.java`](../../src/main/java/com/example/htmlmud/domain/actor/impl/Mob.java)、[`Room.java`](../../src/main/java/com/example/htmlmud/domain/actor/impl/Room.java)
  - **執行動作**：
    1. 在 `Room.java` 增加單參數 `broadcast(String message)` 純單機便捷方法。
    2. 在 `Mob.java` 徹底拔除 `broadcastToOthers` 呼叫，直接向單機房間主角廣播。
  - **交付標準**：後端核心領域模型語意純淨單機化。✅ 已落地完成 (375 測試全綠)

---

### 🔹 Phase 3: 前端介面與純單機表現對齊 (Frontend & Presentation Layer)

- [x] **Task 3.1 (ALIGN-FE-CMD-PURGE): 徹底下架常規視圖文字指令輸入框 (對齊 L1 UI/UX / L2 FE4)**
  - **目標檔案**：[`index.html`](../../src/main/resources/static/index.html)、[`app.js`](../../src/main/resources/static/js/app.js)
  - **執行動作**：
    1. 從操作列移除 `<button data-action="toggle-cmd-input">⌨️ 指令</button>` 按鈕。
    2. 移除常規可見的 `#cmd-input-container`，保留開發者專用按 `~` 鍵呼出的 `#dev-console`。
    3. 所有玩家操作 100% 收斂至快捷鍵（WASD 走格）與點擊按鈕。
  - **交付標準**：一般玩家遊玩介面無任何文字指令輸入殘留。✅ 已落地完成

- [x] **Task 3.2 (ALIGN-FE-EQUIP-AFFIX): 前端裝備面板支援 5+2 槽位與詞綴展示 (對齊 L2 FE5)**
  - **目標檔案**：[`party-equipment-tab.js`](../../src/main/resources/static/js/modals/party-equipment-tab.js)、`style.css`
  - **執行動作**：
    1. 確保 5 主槽位 + 2 飾品槽位在 UI 上精確對齊。
    2. Diff 面板與候選列表支援展示隨機詞綴清單、削韌強度 (`poiseBreakPower`) 與顏色區分。
  - **交付標準**：裝備面板清晰反應用戶端刷寶收穫與道韻詞綴。✅ 已落地完成

---

### 🔹 Phase 4: 測試保護與整合驗收 (Regression Guard & Verification)

- [x] **Task 4.1 (ALIGN-TEST-REFACTOR): 重構走火入魔背刺單元測試 (對齊 L4 測試守護)**
  - **目標檔案**：[`PartyInventoryAndMadnessTest.java`](../../src/test/java/com/example/htmlmud/PartyInventoryAndMadnessTest.java)
  - **執行動作**：
    1. 驗證隊員理智歸零進入精神萎靡 Debuff (-15% 傷害輸出)。
    2. 驗證絕不背刺隊友，攻擊目標始終鎖定敵方。
  - **交付標準**：全專案 375+ 項測試全數綠燈通過。✅ 已落地完成 (376 測試全綠)

- [x] **Task 4.2 (ALIGN-VALIDATOR-EXPAND): 資料校驗器擴充流派與詞綴規則 (對齊 L2 M7)**
  - **目標檔案**：[`DataIntegrityValidator.java`](../../src/main/java/com/example/htmlmud/domain/validation/DataIntegrityValidator.java)
  - **執行動作**：在遊戲啟動階段增加對技能超頻階級 (0~3) 合法性與裝備部位 (slot) 存在性的 Fail-Fast 檢驗。
  - **交付標準**：任何非法技能或裝備資料在啟動時明確報警，防範執行期隱性崩潰。✅ 已落地完成 (376 測試全綠)

---

## 🛡️ 三、完成定義 (Definition of Done, DoD)

1. **四層穿透無矛盾**：L1 的賽博修仙與純單機體驗，在 L2 均有清晰結構、在 L3 有追蹤清單、在 L4 有當前微任務對齊。
2. **零代碼語意割裂**：代碼中不存在文檔已廢除之機制（如背刺隊友、MUD 指令框、房間 exits）。
3. **綠燈守護門**：執行過程中始終守護 `.\test.ps1` 373+ 項測試 100% 綠燈通過。
4. **極致輕量**：本執行清單精確控制在 200 行內，避免長文本閱讀疲勞。
