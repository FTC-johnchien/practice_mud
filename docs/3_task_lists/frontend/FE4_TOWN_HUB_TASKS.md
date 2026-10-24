# 🏛️ FE4: 營地中樞設施與動作列面板模組實作進度清單 (Town Hub & Action Bar Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/frontend/FE4_TOWN_HUB_AND_ACTION_BAR.md`](../../2_specifications/frontend/FE4_TOWN_HUB_AND_ACTION_BAR.md)  
> **核心職責**：管理城鎮設施卡片式選單展示、快捷休整按鈕與全域 8 鍵自適應動作列面板 (`action-bar-panel.js`)。  
> **當前完成度**：85% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心檔案路徑**：
  - `src/main/resources/static/js/panels/town-panel.js`（營地中樞設施面板）
  - `src/main/resources/static/js/panels/action-bar-panel.js`（底部 8 格動態動作列）
  - `src/main/resources/static/css/style-05-town-and-hub.css`（營地設施卡片與功能面板樣式）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **FE4-ACTION-BAR-ADAPT (動作列情境自適應)**
  - 根據當前視圖模式（探索視圖、戰鬥視圖、營地視圖）動態切換 1~8 數字熱鍵動作按鈕。
- [x] **FE4-REST-BUTTON (營地一鍵調息)**
  - 營地中樞提供一鍵全員調息休整，直觀觸發回補氣血與道心。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task FE4-1 (TOWN-PANEL-CARDS-REFACTOR): 營地選單網格卡片重構 (移除 4 向羅盤，呈現六大設施)**
  - **目標**：徹底自 `town-panel.js` 移除渲染舊 MUD 4 向出口（`exits.forEach` / `renderTownNav`），全面以卡片呈現六大機能設施（百煉堂、靈台藏經閣、雜役演武道場、宗門外門集市、回春客棧、廢棄礦脈坑道）。
  - **驗收標準**：營地介面不再出現四向移動羅盤，全面轉為純單機點選式現代 RPG 卡片選單，六大設施卡牌（含外門集市）完整渲染。
- [ ] **Task FE4-2 (XIANXIA-THEMED-SURFACE-RENDER): 表層仙俠風格純化與動態真相浮水印解密 (Glitch FX)**
  - **目標**：落實 Option C 渲染原則，營地設施卡牌、NPC 對話框與按鈕文字預設 100% 呈現古典仙俠稱謂；當後端 `TownHubDto` 標記 `glitch: true` 時，前端觸發 CSS 故障光斑特效；當晶片加載度突破閾值時，卡片動態解鎖賽博底法解析浮水印。
  - **驗收標準**：前端卡牌預設無破梗詞彙，臨界與突破期平滑呈現故障過渡與真相解密。
- [ ] **Task FE4-3 (MARKET-TRADING-PANEL-AND-SUPPLIES): 宗門外門集市交易與物資補給面板**
  - **目標**：依據 L1《[`GAMEPLAY_CORE_LOOPS.md`](../../1_requirements/GAMEPLAY_CORE_LOOPS.md)》與 L2《[`FE4_TOWN_HUB_AND_ACTION_BAR.md`](../../2_specifications/frontend/FE4_TOWN_HUB_AND_ACTION_BAR.md)》，實作宗門外門集市（Outer Sect Market）交易視圖/面板，支援玩家購買地牢生存物資（火把、金創藥、辟邪符籙），並可向遊商·瘸狼銷贓地牢戰利品（黑煞石、怪獸素材）換取靈石。
  - **驗收標準**：點擊外門集市卡牌或 NPC 名片開啟交易面板，支援買入補給與批量銷贓，小隊 40 格公共行囊數量即時同步刷新。
