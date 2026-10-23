# 🎨 FE6: 七層樣式串流與視覺主題模組實作進度清單 (CSS Tokens Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/frontend/FE6_CSS_TOKENS_AND_THEMES.md`](../../2_specifications/frontend/FE6_CSS_TOKENS_AND_THEMES.md)  
> **核心職責**：管理七層樣式流 (`style-01` ~ `style-07.css`)、全域 CSS Design Tokens（古風東方賽博紫黑金配色）、稀有度文字色彩與歷史樣式巨石除役。  
> **當前完成度**：95% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心檔案路徑**：
  - `src/main/resources/static/css/style-01-tokens.css`（全域 CSS 變數、顏色、字級、間距）
  - `src/main/resources/static/css/style-02-layout.css`（響應式五大板塊骨架）
  - `src/main/resources/static/css/style-03-dungeon-view.css`（DRPG 視口與迷霧）
  - `src/main/resources/static/css/style-04-battle-arena.css`（擂台排位佈局）
  - `src/main/resources/static/css/style-05-town-and-hub.css`（營地中樞卡片）
  - `src/main/resources/static/css/style-06-modals-and-forms.css`（彈窗與對話框）
  - `src/main/resources/static/css/style-07-battle-fx.css`（打擊動效與浮動大字）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **FE6-STYLE-MONOLITH-PURGE (歷史樣式巨石安全除役)**
  - 物理安全刪除 122 KB 之舊樣式巨石 `style.css`，全系統 100% 運行於 7 層模組化串流。
- [x] **FE6-SEVEN-TIER-STREAM (七層結構建立)**
  - 確立清晰層次：Tokens $\to$ Layout $\to$ Viewport $\to$ Battle $\to$ Hub $\to$ Modals $\to$ FX，無循環依賴。
- [x] **FE6-THEME-TOKENS (賽博克蘇魯修仙主題 Token)**
  - 確立暗紫黑底、科技青藍、靈煞赤紅與天道金等標準 CSS 變數，字級下限不低於 13px。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task FE6-1 (CRT-SCANLINE-OVERLAY): 復古賽博 CRT 掃描線光影選用開關**
  - **目標**：在設定面板提供開關，可於視口疊加輕微 CRT 掃描線與微幅曲面色差光暈，增強沉浸感。
  - **驗收標準**：開關即時切換生效，關閉時零額外 GPU 繪製開銷。
