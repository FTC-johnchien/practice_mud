# 外部靈感與原始參考資料庫 (Raw References & Inspirations)

> **目錄定位與歷史演進說明**：  
> 本目錄原為專案中期（Phase 8 ~ Phase 9）之 12 篇主題式設計稿（`01_` ~ `12_`）。  
> 隨專案全面推進至四層文檔體系（Phase 11.5），該 12 篇設計稿之實作規格、代碼錨點與未來演進，**已 100% 全面收斂、深化並移轉至 `docs/manual/` 15 本模組深度手冊與 `docs/overview/` 概覽指引**，原 12 篇過渡文件已正式除役。

---

## 🧭 正式模組手冊導航 (Official Manuals)

請移步查閱已全面包含並擴充原 12 篇內容的權威手冊體系：

| 原始主題 | 權威手冊歸屬 (docs/manual/) | 全域概覽對齊 (docs/overview/) |
| :--- | :--- | :--- |
| **01. 玩家角色與修為體系** | [`後端_M1_探索與世界模組`](../manual/後端_M1_探索與世界模組_WORLD_AND_EXPLORATION.md) / [`後端_M4_小隊生靈`](../manual/後端_M4_小隊生靈與陣法模組_PARTY_AND_FORMATION.md) / [`後端_M5_技能境界`](../manual/後端_M5_技能境界與修煉模組_SKILL_AND_PROGRESSION.md) | [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md) |
| **02. 可組隊夥伴 NPC** | [`後端_M4_小隊生靈與陣法模組`](../manual/後端_M4_小隊生靈與陣法模組_PARTY_AND_FORMATION.md) / [`前端_FE5_小隊裝備`](../manual/前端_FE5_小隊裝備與存檔彈窗_PARTY_EQUIPMENT_AND_MODALS.md) | [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md) |
| **03. 城鎮居民與功能型 NPC** | [`後端_M1_探索與世界模組`](../manual/後端_M1_探索與世界模組_WORLD_AND_EXPLORATION.md) / [`後端_M6_物品與經濟`](../manual/後端_M6_物品背包與經濟模組_ITEM_AND_ECONOMY.md) / [`前端_FE4_城鎮中樞`](../manual/前端_FE4_城鎮中樞與動作列面板_TOWN_HUB_AND_ACTION_BAR.md) | [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md) |
| **04. 物品、5+2 裝備與行囊** | [`後端_M6_物品背包與經濟模組`](../manual/後端_M6_物品背包與經濟模組_ITEM_AND_ECONOMY.md) / [`前端_FE5_小隊裝備`](../manual/前端_FE5_小隊裝備與存檔彈窗_PARTY_EQUIPMENT_AND_MODALS.md) | [`MODULES_MAP.md`](../overview/MODULES_MAP.md) |
| **05. 怪物、首領與深淵畸變** | [`後端_M1`](../manual/後端_M1_探索與世界模組_WORLD_AND_EXPLORATION.md) / [`後端_M2_地牢`](../manual/後端_M2_DRPG網格地牢模組_DRPG_DUNGEON.md) / [`後端_M3_戰鬥引擎`](../manual/後端_M3_核心戰鬥引擎模組_COMBAT_ENGINE.md) / [`後端_M7_模板`](../manual/後端_M7_資料驅動模板模組_DATA_DRIVEN_TEMPLATES.md) | [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md) |
| **06. 任務系統與冒險探索** | [`後端_M1_探索與世界模組`](../manual/後端_M1_探索與世界模組_WORLD_AND_EXPLORATION.md) / [`後端_M7_模板`](../manual/後端_M7_資料驅動模板模組_DATA_DRIVEN_TEMPLATES.md) / [`前端_FE4_城鎮中樞`](../manual/前端_FE4_城鎮中樞與動作列面板_TOWN_HUB_AND_ACTION_BAR.md) | [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md) |
| **07. 種族與職業體系** | [`後端_M4_小隊生靈`](../manual/後端_M4_小隊生靈與陣法模組_PARTY_AND_FORMATION.md) / [`後端_M5_技能境界`](../manual/後端_M5_技能境界與修煉模組_SKILL_AND_PROGRESSION.md) / [`後端_M7_模板`](../manual/後端_M7_資料驅動模板模組_DATA_DRIVEN_TEMPLATES.md) | [`MODULES_MAP.md`](../overview/MODULES_MAP.md) |
| **08. 小隊陣法系統** | [`後端_M4_小隊生靈與陣法模組`](../manual/後端_M4_小隊生靈與陣法模組_PARTY_AND_FORMATION.md) / [`前端_FE5_小隊裝備`](../manual/前端_FE5_小隊裝備與存檔彈窗_PARTY_EQUIPMENT_AND_MODALS.md) | [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md) |
| **09. 技能體系與法術** | [`後端_M3_戰鬥引擎`](../manual/後端_M3_核心戰鬥引擎模組_COMBAT_ENGINE.md) / [`後端_M5_技能境界與修煉模組`](../manual/後端_M5_技能境界與修煉模組_SKILL_AND_PROGRESSION.md) / [`前端_FE4`](../manual/前端_FE4_城鎮中樞與動作列面板_TOWN_HUB_AND_ACTION_BAR.md) | [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md) |
| **10. 戰鬥機制與動能引擎** | [`後端_M3_核心戰鬥引擎模組`](../manual/後端_M3_核心戰鬥引擎模組_COMBAT_ENGINE.md) / [`前端_FE3_戰鬥擂台`](../manual/前端_FE3_戰鬥擂台與特效引擎_BATTLE_ARENA_AND_FX.md) | [`GAMEPLAY_OVERVIEW.md`](../overview/GAMEPLAY_OVERVIEW.md) |
| **11. 世界架構與城鎮地城** | [`後端_M1_探索與世界`](../manual/後端_M1_探索與世界模組_WORLD_AND_EXPLORATION.md) / [`後端_M2_DRPG網格地牢模組`](../manual/後端_M2_DRPG網格地牢模組_DRPG_DUNGEON.md) / [`前端_FE2_DRPG視口`](../manual/前端_FE2_DRPG視口與雷達渲染器_DRPG_VIEWPORT_RENDERER.md) | [`FRONTEND_LAYOUT_GUIDE.md`](../overview/FRONTEND_LAYOUT_GUIDE.md) |
| **12. 畫面 Layout 與前端 UI/UX** | [`前端_FE1`](../manual/前端_FE1_核心狀態與事件管線_CORE_STATE_AND_PIPELINE.md) ~ [`FE6`](../manual/前端_FE6_七層樣式串流與視覺主題_CSS_TOKENS_AND_THEMES.md) 全六冊 | [`FRONTEND_LAYOUT_GUIDE.md`](../overview/FRONTEND_LAYOUT_GUIDE.md) |

---

## 🎨 本目錄保留之原始靈感與參考素材 (Asset Stash)

本目錄目前作為遊戲美術概念圖、文字設定靈感與原始參考資料庫保留：
* `戰鬥畫面.jpg`：經典第一人稱回合戰鬥佈局視覺參考
* `個人狀態.png`：角色道基屬性面板介面概念參考
* `畫面佈局.txt`：早期畫面切分思考草稿
* `賽博龐克技能對應到修仙技能的概念.txt`：技能五行仙俠化設計靈感對照
* `道祖是克苏鲁_全本.txt`：克蘇魯仙俠世界觀、SAN 異化與道核克系修真原始背景文本

---

## 🔍 外部代碼與文件架構審查報告 (External Review Reports)

> ⚠️ **非規範性文件提醒 (Non-Normative Reference Notice)**：  
> 本目錄下的審查文件屬於外部審查員／AI 審查產物，定位為**「參考與改善建議」**。  
> 審查報告中的項目**不取代**專案憲法（`GEMINI.md`）、架構邊界（`ARCHITECTURE.md`）或已決議規格（`docs/manual/`）。  
> 僅有經過審查、決議並納入 [`docs/plans/FUTURE_IMPROVEMENTS.md`](../plans/FUTURE_IMPROVEMENTS.md) 及 [`ACTIVE_TASKS.md`](../../ACTIVE_TASKS.md) 的項目，才正式成為專案執行規劃。

* [`Codex_Review.md`](./Codex_Review.md)（2026-10-15）：
  - **主題**：專案 Markdown 文件體系與遊戲規劃審查。
  - **核心建議**：建立單一權威來源 (SSoT)、定義四級內容狀態標籤（現行已驗證／已決議待實作／候選提案／歷史已除役）、修復相對連結、收斂重複路線圖、以可玩的垂直切片推進核心玩法、對齊傷害/元素/抗性與鍵位詞彙表。
* [`Copilot_Review.md`](./Copilot_Review.md)（2026-10-15）：
  - **主題**：專案文件體系與遊戲規劃審查建議。
  - **核心建議**：具體定位失效連結與歷史引用、跨文件不一致性對照表（心跳排程、SaveData schemaVersion、CSS 層數與模組定義）、區分近期單機模組化與遠期在線願景、微任務驗收語意精密化。

