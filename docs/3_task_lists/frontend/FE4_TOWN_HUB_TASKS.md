# 🏛️ FE4: 營地中樞設施與動作列面板模組實作進度清單 (Town Hub & Action Bar Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/frontend/FE4_TOWN_HUB_AND_ACTION_BAR.md`](../../2_specifications/frontend/FE4_TOWN_HUB_AND_ACTION_BAR.md)  
> **核心職責**：管理城鎮設施卡片式選單展示、快捷休整按鈕與全域 8 鍵自適應動作列面板。  
> **當前完成度**：95% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心檔案路徑**：
  - `src/main/resources/static/js/panels/town-panel.js`（營地中樞設施面板）
  - `src/main/resources/static/index.html`（全域動作列與熱鍵容器；舊 `src/main/resources/static/js/panels/action-bar-panel.js`【歷史／已除役】）
  - `src/main/resources/static/css/style-05-town-and-hub.css`（營地設施卡片與功能面板樣式）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **FE4-ACTION-BAR-ADAPT (動作列情境自適應)**
  - 根據當前視圖模式（探索視圖、戰鬥視圖、營地視圖）動態切換快捷動作按鈕。
- [x] **FE4-REST-BUTTON (營地一鍵調息)**
  - 營地中樞提供一鍵全員調息休整，直觀觸發回補氣血與道心。
- [x] **Task FE4-1 (TOWN-PANEL-CARDS-REFACTOR): 營地選單網格卡片重構 (移除 4 向羅盤，呈現六大設施)**
  - 徹底自 `town-panel.js` 移除舊 MUD 4 向出口方塊，改以現代 DRPG 六大機能設施卡牌網格（百煉堂、靈台藏經閣、雜役演武道場、宗門外門集市、回春客棧、廢棄礦脈坑道）渲染主舞台。
- [x] **Task FE4-2 (XIANXIA-THEMED-SURFACE-RENDER): 表層仙俠風格純化與動態真相浮水印解密 (Glitch FX)**
  - 落實 Option C 渲染原則，設施文字預設 100% 呈現古典仙俠稱謂；當後端 DTO 標記 `glitch: true` 時，前端觸發 `.facility-glitch` 樣式與徽章；`#town-perception-badge` 即時呈現晶片加載率。
- [x] **Task FE4-3 (MARKET-TRADING-PANEL-AND-SUPPLIES): 宗門外門集市交易與物資補給面板**
  - 接通外門集市貨棧交易視圖，支援地牢補給品購買與批量銷贓，公共行囊數量即時同步刷新。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task FE4-4 (UI-LAYOUT-MOBILE-TABS-AND-PC-3COL)【待深入討論／長線演進藍圖】：手機直向底部 5 分頁常駐導航列與 PC 三欄版面演進**
  - **目標**：依據 L1 UI/UX 規範與待辦議程，評估在維持當前單一視圖容器的基礎上，引進手機直向底部 5 分頁常駐導航列，以及 PC 寬螢幕三欄沉浸式戰鬥/探索版面。
  - **驗收標準**：提出響應式版面重構提案，不破壞現有單檔 JS 模組化設計。
- [ ] **Task FE4-5 (NPC-DIALOGUE-MODAL)【已決議待實作】：NPC 沉浸式分支對話彈窗 (.npc-dialogue-modal)**
  - **目標**：依據 L2 規格 §二，實作獨立 NPC 劇情分支對話彈窗，支援立繪展示、對話樹分支選擇與情報解鎖。
  - **驗收標準**：點擊 NPC 卡片交談按鈕可正確喚出分支對話彈窗，派發對應命令並推進對話歷程。
