# ⚔️ FE3: 戰鬥擂台與打擊特效引擎模組實作進度清單 (Battle Arena Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/frontend/FE3_BATTLE_ARENA_AND_FX.md`](../../2_specifications/frontend/FE3_BATTLE_ARENA_AND_FX.md)  
> **核心職責**：管理戰鬥主舞台敵我陣列渲染、ATB 蓄力動態進度條、罡氣 Poise 條、首領多階段血條節點、彩色 FCT 戰鬥浮動文字與震盪光效。  
> **當前完成度**：95% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心檔案路徑**：
  - `src/main/resources/static/js/panels/battle-panel.js`（擂台血條、怪相與施法條）
  - `src/main/resources/static/js/fx/battle-fx-director.js`（震盪、光效與閃爍導演器；舊 `src/main/resources/static/js/battle-fx-director.js`【歷史／已除役】）
  - `src/main/resources/static/js/fx/floating-text.js`（FCT 彩色浮動文字爆發；舊 `src/main/resources/static/js/floating-text.js`【歷史／已除役】）
  - `src/main/resources/static/css/style-07-battle-fx.css`（打擊特效、浮動大字與狂暴光環）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **FE3-POISE-AND-CASTBAR (罡氣條與施法打斷)**
  - 目標下方渲染銀白色罡氣條；敵方蓄力顯示金色施法條；打斷發動時觸發 `⚡ 打斷!` 浮動字樣與青白電弧受擊震盪。
- [x] **FE3-FCT-REACTION-TEXT (元素反應彩色大字)**
  - 實裝蒸發 (`.ft-vaporize` 橙紅)、感電 (`.ft-electro` 亮青)、混沌 (`.ft-chaos` 暗紫)、引爆 (`.ft-detonation` 赤金) 彩色爆裂大字。
- [x] **FE3-BOSS-PHASES-UI (首領多階段標記與狂暴光環)**
  - 血條精準渲染多階段閾值點（`.hp-phase-marker`）；轉階段狂暴觸發全身赤紅過載光環（`.boss-enraged-aura`）。
- [x] **FE3-FORMATION-FINISHER-FX (天外合擊特效)**
  - 連攜接力彈出雙星合璧、三才混元、四象誅仙字樣；發動天外合擊時全螢幕金紫流光閃耀。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task FE3-1 (ANIMATED-SPRITE-BATTLE): 怪物動態呼吸與幀動畫支援**
  - **目標**：怪物頭像支援 CSS 呼吸縮放與受擊抖動粒子，取代靜態矩形頭像。
  - **驗收標準**：爆擊受擊時產生 120ms 強烈側震，視覺爽快感倍增。
