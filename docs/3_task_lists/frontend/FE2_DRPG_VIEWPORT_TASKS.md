# 🗺️ FE2: DRPG 視口與雷達渲染器模組實作進度清單 (Viewport Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/frontend/FE2_DRPG_VIEWPORT_RENDERER.md`](../../2_specifications/frontend/FE2_DRPG_VIEWPORT_RENDERER.md)  
> **核心職責**：管理第一人稱偽 3D/2.5D 深度透視視口、暗黑迷霧雷達、前方景致文字卡、八方步進羅盤與火把光影濾鏡。  
> **當前完成度**：90% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心檔案路徑**：
  - `src/main/resources/static/js/panels/dungeon-panel.js`（透視視口與雷達地圖渲染器）
  - `src/main/resources/static/css/style-03-dungeon-view.css`（地牢視口與迷霧特效樣式）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **FE2-DEPTH-RAYCAST (前方景深文字與透視)**
  - 實作前進 3 步景致深度渲染，支援射線阻擋遮擋（WALL 遮蔽後方視線）。
- [x] **FE2-FOG-RADAR (10x10 暗黑迷霧雷達)**
  - 玩家即時踩格揭開迷霧，動態標記已探索區塊 (`visited`)、當前座標與面朝方向箭頭。
- [x] **FE2-TORCH-VIGNETTE (火把光明度暈影特效)**
  - 光照度降低時動態疊加周邊暗黑光暈（Vignette）與漆黑煞氣粒子效果。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task FE2-1 (CANVAS-PERSPECTIVE-UPGRADE): 畫布深度幾何走廊渲染**
  - **目標**：以 HTML5 Canvas 渲染第一人稱復古線框/色塊幾何走廊，取代純 CSS 邊框堆疊，帶來真實 DRPG 穿行體感。
  - **驗收標準**：轉身與步進具備順滑過渡動畫（150ms 內），無卡頓掉幀。
