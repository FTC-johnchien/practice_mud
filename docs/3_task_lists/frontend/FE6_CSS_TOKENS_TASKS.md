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
  - 於 `:root` 確立暗紫黑底、科技青藍、靈煞赤紅與天道金等標準 CSS 變數，並定義全域最小字級基準 Token `--font-xs: 13px`（部分歷史緊湊角標與輔助標籤之 `9px~12px` 細節列為非阻擋漸進收斂項）。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task FE6-1 (CRT-SCANLINE-OVERLAY)【待深入討論／長線演進藍圖】：復古賽博 CRT 掃描線光影選用開關**
  - **目標**：在設定面板提供選用開關（預設關閉，狀態持久化於 `localStorage`），開啟時於視口疊加輕微 CRT 掃描線濾鏡。
  - **驗收標準**：開關即時切換生效、關閉時零額外繪製開銷，且於 `@media (prefers-reduced-motion: reduce)` 下預設停用。
- [ ] **Task FE6-2 (LEGACY-COMPACT-FONT-CONVERGENCE)【待深入討論／長線演進藍圖】：歷史緊湊角標與輔助標籤（9px~12px）字級漸進收斂與例外規範**
  - **目標**：承接 `FUTURE_IMPROVEMENTS.md` 之 `UI-01`，盤點 `style-01-tokens-and-shell.css` 與 `style-02-combat-arena.css` 等樣式表中殘留之 `9px~12px` 宣告，將正文字級統一收斂至 `--font-xs: 13px` 以上，或針對純裝飾性微型角標／快速鍵提示明訂例外範圍。
  - **驗收標準**：非裝飾性正文與面板數值 100% 採用 `--font-xs`（`13px`）以上 Token，視覺排版不溢位。
