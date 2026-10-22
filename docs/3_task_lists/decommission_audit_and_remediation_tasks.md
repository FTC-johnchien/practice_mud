# 🔍 未竟除役項目實地查驗報告與代碼治理執行清單
# (Decommission Audit & Remediation Tasks)

> **文檔層級**：Level 3 — Task Lists (專案治理與技術債清剿清單)  
> **制定日期**：2026-10-22  
> **核心目標**：盤查全專案 36 份 Markdown 文件中所有宣稱「徹底除役」之條目，對比 Java 代碼與前端 HTML/JS 實況，揪出**「文檔宣稱已除役，但代碼依然在跑」**的虛假完成項目，建立精確、可落地的技術收斂任務清單。

---

## 📌 一、查驗綜述與現狀判定 (Audit Summary)

全專案經全文盤查，共檢出 **22 處「徹底除役」條目**：
- ✅ **16 處已確實完成**：包含歷史樣式巨石 `style.css` 移除、`SkillBridgeService` 刪除、`stash` 個人背包雙層轉存廢除、存檔雙寫 `personalInventory` 拔除、5 秒超時阻塞消除、多人連線/Protobuf/Redis 過度設計等。（**已依指令於各手冊中移除過渡修辭，精簡為現況陳述**）
- ⚠️ **6 處未真正完成（虛假除役）**：文檔宣稱徹底除役，但實地代碼與前端中仍活躍運行，集中於以下三大核心架構領域。

---

## 🚨 二、三大未完成除役領域深度剖析 (Reality Check)

### 1. 領域一：道心崩潰魔化與隊友背刺機制 (Madness & Chaos Betrayal)
- **文檔宣告**：`GAMEPLAY_CORE_LOOPS.md:5, 97`、`tech_debt_and_hardening.md:61`、`FUTURE_IMPROVEMENTS.md:28` 宣誓「徹底除役道心魔化與隊員背刺（40% 機率）機制，相關代碼在純單機單體化演進中已徹底拔除」。
- **代碼實況 (未完成 🔴)**：
  - [`DrpgCombatLoop.java:230-320`](../../src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java#L230-L320)：隊員 SAN 歸零仍會進入 `CHAOS` 走火入魔狀態，每回合仍有 **40% 機率執行【心魔背刺】攻擊我方隊友**！
  - [`PartyMember.java`](../../src/main/java/com/example/htmlmud/domain/party/model/PartyMember.java)：仍持有 `MadnessState` (`CHAOS`, `SEALED`, `ABERRANT`) 與 `aberrationCounter`。
  - [`SealCommand.java`](../../src/main/java/com/example/htmlmud/application/command/impl/SealCommand.java)：依然存在「太上鎮魔封印術」用以封印異變隊友。
  - [`PartyInventoryAndMadnessTest.java`](../../src/test/java/com/example/htmlmud/PartyInventoryAndMadnessTest.java)：仍有 3 個單元測試在保護走火入魔與背刺邏輯。
- **架構違規**：違背「爽快砍殺刷寶 (Hack-and-slash)」核心目標，玩家隊友隨機背刺造成挫敗感。

### 2. 領域二：前端 MUD 文字指令輸入框與操作列按鈕 (Command Input & Footer Bar)
- **文檔宣告**：`UI_UX_REQUIREMENTS.md:5` 宣告「關鍵界定：徹底除役 MUD 指令輸入框與文字房間移動」。
- **前端實況 (未完成 🔴)**：
  - [`index.html:651-661`](../../src/main/resources/static/index.html#L651-L661)：底部操作列仍有 `<button data-action="toggle-cmd-input">⌨️ 指令</button>` 按鈕，點擊仍會展開文字指令輸入框 (`#cmd-input-container`)。
  - [`app.js`](../../src/main/resources/static/js/app.js) & [`cmd-dispatcher.js`](../../src/main/resources/static/js/core/cmd-dispatcher.js)：仍監聽並向後端派發原始字串指令。
- **架構違規**：殘留雙軌操作介面，混淆純單機 DRPG 的全按鈕/全鍵盤走格操作體驗。

### 3. 領域三：後端 MUD 走房間與多人廣播通道殘留 (Room & Broadcast Remnants)
- **文檔宣告**：`GAMEPLAY_CORE_LOOPS.md:46` 宣告「徹底除役任何文字 MUD 走房間模式，採用現代 DRPG 的標準選單式設施中樞」。
- **代碼實況 (未完成 🔴)**：
  - [`Mob.java`](../../src/main/java/com/example/htmlmud/domain/actor/impl/Mob.java)、[`Room.java`](../../src/main/java/com/example/htmlmud/domain/actor/impl/Room.java)、[`RoomService.java`](../../src/main/java/com/example/htmlmud/domain/world/RoomService.java)：依然殘留 `broadcastToOthers` 等多人 MUD 專屬死代碼。
  - `src/main/resources/data/zones/` 下仍以舊 MUD `rooms.json`（房間拓撲 exits）存放，尚未轉換為純單機營地的 `facilities.json`。

---

## 📋 三、詳細可執行治理微任務清單 (Actionable Remediation Tasks)

### 🔹 Milestone DEC-01: 道心魔化與隊友背刺機制代碼徹底清理 (Priority: P1)

- [x] **Task 1.1 (DEC-01-COMBAT): 重構 `DrpgCombatLoop.java` 戰鬥循環**
  - **執行步驟**：
    1. 移除 lines 230-320 中隊員 SAN 歸零後的 `CHAOS` 走火入魔檢定與 40% 心魔背刺攻擊隊友邏輯。
    2. 將 SAN 重新定義為「精神護盾」：SAN 耗盡時僅給予隊員【精神萎靡】Debuff（降低 15% 輸出），絕不發生隊友背刺與失控叛變。
  - **交付驗收**：戰鬥中心跳循環無任何背刺隊友代碼執行。✅ 已完成落地 (373 測試全綠)

- [x] **Task 1.2 (DEC-01-MODEL): 簡化 `PartyMember.java` 實體與除役封印術**
  - **執行步驟**：
    1. 精簡 `PartyMember.MadnessState`（純化為 SANE / CHAOS 精神萎靡），拔除 SEALED、ABERRATION、DEAD_MEAT。
    2. 物理刪除 `SealCommand.java`（太上鎮魔封印術），並自 `BattleCommand` 與 `DrpgBattleService` 移除封印術邏輯。
    3. `DrpgRewardService` 戰後清理徹底拔除隊員除名代碼，純單機隊員資產永久保全。
  - **交付驗收**：`PartyMember` 領域模型純淨化，封印術代碼清零。✅ 已完成落地 (376 測試全綠)

- [x] **Task 1.3 (DEC-01-TESTS): 重構單元測試集並確保 376+ 測試全綠**
  - **執行步驟**：
    1. `PartyInventoryAndMadnessTest.java` 移除封印術測試，改為驗證精神萎靡 Debuff 與清心符解除萎靡恢復澄澈。
    2. `DrpgBattleDecompositionTest.java` 驗證戰後小隊編制無損保全。
  - **交付驗收**：`.\test.ps1` 執行無損通過，376 測試 100% 綠燈。✅ 已完成落地 (376 測試全綠)

---

### 🔹 Milestone DEC-02: 前端 MUD 文字指令輸入框與操作列按鈕徹底下架 (Priority: P2)

- [x] **Task 2.1 (DEC-02-HTML): 下架 `index.html` 底部文字指令框**
  - **執行步驟**：
    1. 自 `index.html` 操作列移除 `<button data-action="toggle-cmd-input">⌨️ 指令</button>` 按鈕。
    2. 移除 `#cmd-input-container` 展開式容器。
    3. 保留按 `~` 鍵呼出的 `#dev-console` 作為開發者專用偵錯控制台，玩家日常遊玩介面 100% 走格與按鈕化。
  - **交付驗收**：常規遊戲視圖完全消除文字指令輸入框。✅ 已完成落地

- [ ] **Task 2.2 (DEC-02-JS): 精簡 `app.js` 與 `cmd-dispatcher.js`**
  - **執行步驟**：
    1. 移除 `toggle-cmd-input` 事件委派監聽。
    2. 操作管線全數收斂至以 `ActionCommand` 為核心的結構化派發。
  - **交付驗收**：前端事件與動作管線零文字輸入框依賴。

---

### 🔹 Milestone DEC-03: 後端 MUD 房間與多人廣播死代碼徹底純化 (Priority: P2)

- [x] **Task 3.1 (DEC-03-BROADCAST): 清理後端 `broadcastToOthers` 冗餘代碼**
  - **執行步驟**：
    1. 在 `Room.java` 實裝純單機 `broadcast(String message)` 便捷方法。
    2. 在 `Mob.java` 徹底拔除 `broadcastToOthers` 呼叫，直接向單機主角廣播。
  - **交付驗收**：單機單體語意徹底純化。✅ 已完成落地 (375 測試全綠)

- [x] **Task 3.2 (DEC-03-DATA): 營地資料檔正式轉換為 `facilities.json`**
  - **執行步驟**：
    1. 在 `src/main/resources/data/zones/newbie_village/` 正式建立選單式機能設施 `facilities.json`。
    2. 配合 `M1_TOWN_HUB_AND_FACILITIES.md` 與 `M7_DATA_DRIVEN_TEMPLATES.md` 規範落地。
  - **交付驗收**：資料目錄與 L2 規格手冊達成 100% 同步。✅ 已完成落地

---

## 🛡️ 四、驗收標準與品質守護門 (DoD)

1. **代碼與文檔一致性**：文檔宣稱「徹底除役」之功能，代碼中必無任何殘留執行邏輯。
2. **測試守護門**：每次推進上述微任務，`.\test.ps1` 必須維持 373+ 測試 100% 綠燈，零迴歸破壞。
3. **文件微型化**：本治理清單與所有關聯文件嚴格壓制在 200 行以內。
