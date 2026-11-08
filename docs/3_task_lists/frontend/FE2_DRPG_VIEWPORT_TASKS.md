# 🗺️ FE2: DRPG 視口與雷達渲染器模組實作進度清單 (Viewport Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/frontend/FE2_DRPG_VIEWPORT_RENDERER.md`](../../2_specifications/frontend/FE2_DRPG_VIEWPORT_RENDERER.md)  
> **核心職責**：管理地牢探索面板（`#dungeon-radar-panel`）、2D 暗黑迷霧雷達（`#drpg-grid`）、正前方三步景致文字探查卡（`#drpg-forward-inspect`）、九宮格步進羅盤（`.compass-grid`）與火把光明度計量條（第一人稱 Canvas 走廊畫布列為長線演進藍圖）。  
> **當前完成度**：90% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心檔案路徑**：
  - `src/main/resources/static/js/panels/dungeon-panel.js`（2D 迷霧雷達、視口/全圖模式切換與前方文字探查卡渲染器）
  - `src/main/resources/static/css/style-01-tokens-and-shell.css`（地牢雷達面板與網格樣式，舊樣式檔名 `style-03-dungeon-view.css`【歷史／已除役】）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **FE2-FORWARD-INSPECT-CARD (正前方三步探查文字卡呈現)**
  - 接收後端依視野與牆體遮擋計算之正前方探查結果，於 `#drpg-forward-inspect` 即時渲染前方三步地貌景致與事件提示文字。
- [x] **FE2-FOG-RADAR (10x10 暗黑迷霧雷達與視口/全圖切換)**
  - 玩家即時踩格揭開迷霧，於 `#drpg-grid` 動態標記已探索區塊 (`visited`)、當前座標、面朝方向箭頭與地塊類型圖示，並支援左右換邊與視口／全圖模式切換。
- [x] **FE2-TORCH-AND-DANGER-BARS (火把光明度與警戒靈壓計量條)**
  - 於 `#drpg-floor-info` 動態渲染 `dangerLevel` 警戒靈壓條、`torchLight` 火把光明度進度條與 `🔥 點火` 互動按鈕（`data-action="dungeon-torch"`）。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task FE2-1 (CANVAS-PERSPECTIVE-UPGRADE)【待深入討論／長線演進藍圖】：第一人稱畫布幾何走廊透視渲染 (`#viewport-canvas`)**
  - **目標**：依據 L2 規格長線藍圖，在現有 2D 雷達與文字探查卡基礎上，評估新增 HTML5 Canvas（`#viewport-canvas`）渲染第一人稱三步景深幾何走廊與火把暗角暈影濾鏡。
  - **驗收標準**：正確依據前方三步地塊類型繪製走廊牆面與門扉，且相容 `@media (prefers-reduced-motion: reduce)` 減弱動效模式。
