# 👥 FE5: 小隊裝備、戰術與彈窗模組實作進度清單 (Modals Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/frontend/FE5_PARTY_EQUIPMENT_AND_MODALS.md`](../../2_specifications/frontend/FE5_PARTY_EQUIPMENT_AND_MODALS.md)  
> **核心職責**：管理隊伍總覽、5+2 裝備挑選與 Diff 面板、Gambit 戰術配置、存檔切換彈窗與金色 3D 結算翻牌。  
> **當前完成度**：90% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心檔案路徑**：
  - `src/main/resources/static/js/modals/party-modal.js`（小隊彈窗容器）
  - `src/main/resources/static/js/modals/party-equipment-tab.js`（5+2 裝備槽位與 Diff 挑選）
  - `src/main/resources/static/js/modals/party-skills-tab.js`（技能與道骨經法分頁）
  - `src/main/resources/static/js/modals/party-formation-tab.js`（戰陣典籍與 5×3 陣盤配置分頁）
  - `src/main/resources/static/js/modals/party-tactics-tab.js`（Gambit 隊友戰術方針設定）
  - `src/main/resources/static/js/modals/bag-drawer.js`（40 格公共行囊抽屜面板）
  - `src/main/resources/static/js/modals/skill-drawer.js`（隊員即時法術快捷抽屜）
  - `src/main/resources/static/js/modals/shop-modal.js`（貨棧商人買賣彈窗）
  - `src/main/resources/static/js/modals/save-modal.js`（存檔與槽位管理）
  - `src/main/resources/static/js/modals/victory-modal.js`（戰鬥結算翻牌視窗）
  - `src/main/resources/static/js/panels/party-hud-panel.js`（常駐小隊戰備抬頭面板）
  - `src/main/resources/static/css/style-04-main-menu.css`、`src/main/resources/static/css/style-06-shop-and-responsive.css`（彈窗與卡片樣式；舊 `src/main/resources/static/css/style-06-modals-and-forms.css`【歷史／已除役】）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **FE5-EQUIP-DIFF-AND-AFFIXES (裝備 Diff 與隨機詞綴展示)**
  - 裝備挑選面板支援 5+2 槽位體系，完整呈現削韌強度 (`poiseBreakPower`) 與前綴/後綴隨機詞綴色彩。
- [x] **FE5-GAMBIT-TACTICS-UI (Gambit 戰術方針配置介面)**
  - 實裝戰術總開關徽章、範本庫下拉選單（斷法/急救/護坦/爆發）與條件目標自訂。
- [x] **FE5-SAN-LOW-WARN (神識護盾警示邊框)**
  - 隊員神識護盾 (SAN) 低於 50% 時，卡片浮現暗紫符紋警示，提示及時服藥休整。
- [x] **FE5-KEY-ITEM-ACCESSORY-COMPAT (任務晶片飾品槽位穿戴相容)**
  - `party-equipment-tab.js` 支援 `KEY_ITEM` / `QUEST` 任務信物晶片掛載於飾品槽位，且過濾器與裝備挑選視圖正常支援。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task FE5-1 (VS-01-AGENT-HUD / ACTIVE-TASK-2): 特工加載度同步與道基加點端到端驗收**
  - **目標**：`party-equipment-tab.js` 已實作道基五維 `[+1]` 潛能點分配按鈕（發送 `party stat add`）且營地 `#town-perception-badge` 已顯示晶片加載度；本任務剩餘聚焦於在小隊總覽／隊員面板同步顯示特工晶片加載度進度條，並配合後端隊長初始 5 點自由潛能點進行端到端驗收。
  - **驗收標準**：隊員面板加點與加載度進度條數值與後端狀態同步，測試全綠。
- [ ] **Task FE5-2 (VS-01-LOOT-CHIP / ACTIVE-TASK-4): 結算金色 3D 翻牌呈現加密晶片**
  - **目標**：擊敗首領後，`victory-modal.js` 結算翻牌視窗正確識別並以傳說品質（`.quality-legendary`）金色翻牌光暈呈現 `item_chip_qianxing_tracker`（千星科技加密追蹤晶片）及其物品敘事文本。
  - **驗收標準**：金色翻牌動效與傳說品質樣式正常渲染（不採用高頻閃爍動畫，並相容 `prefers-reduced-motion` 減弱動效設定）。
