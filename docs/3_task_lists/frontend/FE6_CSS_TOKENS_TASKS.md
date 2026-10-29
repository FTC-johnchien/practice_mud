# 🎨 FE6: 七層樣式串流與視覺主題模組實作進度清單 (CSS Tokens Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/frontend/FE6_CSS_TOKENS_AND_THEMES.md`](../../2_specifications/frontend/FE6_CSS_TOKENS_AND_THEMES.md)  
> **核心職責**：管理七層樣式流 (`style-01` ~ `style-07.css`)、全域 CSS Design Tokens（古風東方賽博紫黑金配色）、稀有度文字色彩與歷史樣式巨石除役。  
> **當前完成度**：95% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心檔案路徑**：
  - `src/main/resources/static/css/style-01-tokens-and-shell.css`（全域 Tokens 與基礎骨架 Shell；舊 `style-01-tokens.css`、`style-02-layout.css`【歷史／已除役】）
  - `src/main/resources/static/css/style-02-combat-arena.css`（戰鬥擂台視口與排位佈局；舊 `style-04-battle-arena.css`【歷史／已除役】）
  - `src/main/resources/static/css/style-03-battle-actions.css`（動作技能列與技能卡；舊 `style-03-dungeon-view.css`【歷史／已除役】）
  - `src/main/resources/static/css/style-04-main-menu.css`（主選單與全域彈窗容器）
  - `src/main/resources/static/css/style-05-town-and-hub.css`（營地中樞卡片）
  - `src/main/resources/static/css/style-06-shop-and-responsive.css`（集市商店、行囊與響應式規則；舊 `style-06-modals-and-forms.css`【歷史／已除役】）
  - `src/main/resources/static/css/style-07-battle-fx.css`（打擊動效與浮動大字）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **FE6-STYLE-MONOLITH-PURGE (歷史樣式巨石安全除役)**
  - 物理安全刪除 122 KB 之舊樣式巨石 `style.css`，全系統 100% 運行於 7 層模組化串流。
- [x] **FE6-SEVEN-TIER-STREAM (七層結構建立)**
  - 確立清晰層次：Tokens/Shell $\to$ Arena $\to$ Actions $\to$ Main Menu $\to$ Hub $\to$ Shop/Responsive $\to$ FX，無循環依賴。
- [x] **FE6-THEME-TOKENS (賽博克蘇魯修仙主題 Token)**
  - 確立暗紫黑底、科技青藍、靈煞赤紅與天道金等標準 CSS 變數，字級下限不低於 13px。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task FE6-1 (CRT-SCANLINE-OVERLAY): 復古賽博 CRT 掃描線光影選用開關**
  - **目標**：在設定面板提供開關，可於視口疊加輕微 CRT 掃描線與微幅曲面色差光暈，增強沉浸感。
  - **驗收標準**：開關即時切換生效，關閉時零額外 GPU 繪製開銷。
