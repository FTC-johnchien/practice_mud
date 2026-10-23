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
  - `src/main/resources/static/js/modals/party-tactics-tab.js`（Gambit 隊友戰術方針設定）
  - `src/main/resources/static/js/modals/save-modal.js`（存檔與槽位管理）
  - `src/main/resources/static/js/modals/victory-modal.js`（戰鬥結算翻牌視窗）
  - `src/main/resources/static/css/style-06-modals-and-forms.css`（彈窗與卡片樣式）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **FE5-EQUIP-DIFF-AND-AFFIXES (裝備 Diff 與隨機詞綴展示)**
  - 裝備挑選面板支援 5+2 槽位體系，完整呈現削韌強度 (`poiseBreakPower`) 與前綴/後綴隨機詞綴色彩。
- [x] **FE5-GAMBIT-TACTICS-UI (Gambit 戰術方針配置介面)**
  - 實裝戰術總開關徽章、範本庫下拉選單（斷法/急救/護坦/爆發）與條件目標自訂。
- [x] **FE5-SAN-LOW-WARN (神識護盾警示邊框)**
  - 隊員神識護盾 (SAN) 低於 50% 時，卡片浮現暗紫符紋警示，提示及時服藥休整。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task FE5-1 (VS-01-AGENT-HUD / ACTIVE-TASK-2): 特工覺醒度 (12%...) 與道基五維加點 UI**
  - **目標**：在隊伍總覽面板新增「特工覺醒度 12%」進度條，並在道基五維屬性（體質/真元/神識/根骨/身法）旁提供 `[+]` 加點按鈕。
  - **驗收標準**：玩家點擊加點即時發送 Action，加點即時轉化為 HP/MP/攻防速五維數值。
- [ ] **Task FE5-2 (VS-01-LOOT-CHIP / ACTIVE-TASK-4): 結算金色 3D 翻牌呈現加密晶片**
  - **目標**：擊敗首領後，`victory-modal.js` 金色 3D 翻牌專屬動畫呈現 `item_chip_qianxing_tracker`。
  - **驗收標準**：翻牌動畫流暢，展示晶片 12Hz 閃爍特徵。
