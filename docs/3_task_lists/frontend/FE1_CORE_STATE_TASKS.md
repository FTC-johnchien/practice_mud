# ⚡ FE1: 前端核心狀態與管線模組實作進度清單 (Core State Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/frontend/FE1_CORE_STATE_AND_PIPELINE.md`](../../2_specifications/frontend/FE1_CORE_STATE_AND_PIPELINE.md)  
> **核心職責**：管理前端全域響應式狀態樹 (`store.js`)、WebSocket 指數退避連線 (`websocket.js`)、全域 `data-action` 事件委派 (`app.js`) 與開發者終端。  
> **當前完成度**：90% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心檔案路徑**：
  - `src/main/resources/static/js/app.js`（核心入口與全域事件委派）
  - `src/main/resources/static/js/state/store.js`（中央響應式狀態樹）
  - `src/main/resources/static/js/network/websocket.js`（獨立 WebSocket 通訊模組）
  - `src/main/resources/static/index.html`（純 HTML5 單頁結構，嚴格 CSP 零內嵌連線代碼）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **FE1-MODULAR-WS (WebSocket 獨立成檔與斷線重連)**
  - 獨立成 `network/websocket.js`，實裝指數退避斷線重連（1s~15s），清除 `index.html` 內嵌連線腳本。
- [x] **FE1-GLOBAL-DELEGATION (全域 data-action 委派)**
  - 在 `app.js` 實裝集中式事件委派，消除元素內聯 `onclick`，符合嚴格內容安全策略 (CSP)。
- [x] **FE1-DEV-CONSOLE (文字輸入框下架與終端轉向)**
  - 下架底部文字指令輸入框 (`#cmd-input-container`) 與操作列按鈕 (`toggle-cmd-input`)，轉向按 `~` 呼出的開發者終端 (`#dev-console`)。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task FE1-1 (FE-LEGACY-CLEANUP): 前端歷史腳本 (`mud-core.js`/`mud-ui.js`) 依賴鏈調研與安全除役 (P3)**
  - **目標**：透過無頭測試與瀏覽器 DevTools 逐行掃描歷史腳本在開發者終端與視窗縮放中的呼叫點，進行平滑替換除役。
  - **驗收標準**：控制台無任何全域變數污染或報錯。
