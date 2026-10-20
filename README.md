# ☯️ 道祖是克蘇魯 (Dao Ancestor is Cthulhu)
### 太陰萬劫 ‧ 暗黑修仙 DRPG & 文字 MUD 雙軌遊戲

[![Java](https://img.shields.io/badge/Java-21%20%2F%2025-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-green.svg)](https://spring.io/projects/spring-boot)
[![Tests](https://img.shields.io/badge/Tests-335%20Passed-brightgreen.svg)]()
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%26%20100%25%20Data--Driven-blue.svg)]()

> **遊戲簡介**：  
> 《道祖是克蘇魯》是一款融合了**東方古典修仙**與**克蘇魯神話不可名狀恐怖**的雙軌模式遊戲。  
> 玩家在**「城鎮 MUD 模式」**中體驗古典文字探索、NPC 招募、法寶交易與打坐調息；在**「地牢 DRPG 模式」**中置身第一人稱 3D 靈境走廊，透過走格子探索暗黑迷宮、避開機關陷阱，並在半即時小隊戰鬥中藉由圓桌判定、罡氣削韌破防與五行抗性克敵制勝！

---

## 🚀 極速開始 (Quickstart in 1 Minute)

### 1. 環境需求
- **JDK**：Java 21 或 Java 25
- **Maven**：3.9+
- **瀏覽器**：Chrome / Edge / Firefox / Safari (支援現代 ES6 Module 與 WebSocket)

### 2. 啟動伺服器
```powershell
# 執行 Spring Boot 服務
.\mvnw spring-boot:run

# 或執行專案啟動腳本
.\run.ps1
```
啟動後造訪：👉 **`http://localhost:8080`**

### 3. 執行全量測試驗證
```powershell
# 執行全套單元、整合與架構看門狗測試 (目前基準: 331 項 100% 綠燈)
.\test.ps1
```

---

## 📚 專案完整文檔導覽體系 (Documentation Hub)

專案提供**分層式技術文檔庫**，方便您快速概覽或深入研究機制細節：

```
practice_mud/
├── GEMINI.md                    # 📜【第一層：憲法中樞】專案最高元契約、第一紅線、技術邊界與按需載入矩陣
├── ACTIVE_TASKS.md              # 🎯【第一層：作戰看板】當前迭代單一真相源，以 [ ] 微任務推進 (AI Agentic Workflow)
├── README.md                    # 🚀【專案首頁】四層體系全景導覽地圖與極速開始指南
├── ARCHITECTURE.md              # 🏗️【系統宏觀設計】潔淨架構、DDD 模型、Actor 併發與 SOP
├── CHANGELOG.md                 # 📜【歷史紀錄中樞】歷史落地、修復足跡與架構決策演進
│
└── docs/
    ├── overview/                # 🚀【第二層：簡要概覽層 (3~5 分鐘掌握全局)】
    │   ├── MODULES_MAP.md       # 🧩 16 大模組全景地圖與端到端協同工作流
    │   ├── FRONTEND_LAYOUT_GUIDE.md # 🖥️ 前端五大版塊骨架、三大主舞台互斥與場景狀態機
    │   ├── ARCHITECTURE_SUMMARY.md  # 🏛️ 高階架構速覽與專案第一紅線
    │   ├── GAMEPLAY_OVERVIEW.md     # 🌌 暗黑修仙世界觀、道基五維與雙軌模式
    │   └── QUICKSTART.md        # ⚡ 開發者快速上手指南
    │
    ├── manual/                  # 🔬【第三層：深入模組手冊 (後端 M1~M9 + M3-1、前端 FE1~FE6)】
    │   ├── 後端_M1_探索與世界模組_WORLD_AND_EXPLORATION.md
    │   ├── 後端_M2_DRPG網格地牢模組_DRPG_DUNGEON.md
    │   ├── 後端_M3_核心戰鬥引擎模組_COMBAT_ENGINE.md
    │   ├── 後端_M3-1_狀態效果與Buff結算模組_BUFF_AND_STATUS_EFFECTS.md
    │   ├── 後端_M4_小隊生靈與陣法模組_PARTY_AND_FORMATION.md
    │   ├── 後端_M5_技能境界與修煉模組_SKILL_AND_PROGRESSION.md
    │   ├── 後端_M6_物品背包與經濟模組_ITEM_AND_ECONOMY.md
    │   ├── 後端_M7_資料驅動模板模組_DATA_DRIVEN_TEMPLATES.md
    │   ├── 後端_M8_存檔與歷史快照模組_SAVE_AND_PERSISTENCE.md
    │   ├── 後端_M9_虛擬執行緒Actor通訊模組_ACTOR_AND_PROTOCOL.md
    │   ├── 前端_FE1_核心狀態與事件管線_CORE_STATE_AND_PIPELINE.md
    │   ├── 前端_FE2_DRPG視口與雷達渲染器_DRPG_VIEWPORT_RENDERER.md
    │   ├── 前端_FE3_戰鬥擂台與特效引擎_BATTLE_ARENA_AND_FX.md
    │   ├── 前端_FE4_城鎮中樞與動作列面板_TOWN_HUB_AND_ACTION_BAR.md
    │   ├── 前端_FE5_小隊裝備與存檔彈窗_PARTY_EQUIPMENT_AND_MODALS.md
    │   └── 前端_FE6_七層樣式串流與視覺主題_CSS_TOKENS_AND_THEMES.md
    │
    ├── plans/                   # 📈【第四層：長遠演化武器庫】
    │   └── FUTURE_IMPROVEMENTS.md # P0~P3 全專案架構改善總表與技術儲備庫
    │
    └── references/              # 📚【原始靈感與參考素材庫】世界觀文字、概念圖與外部靈感素材
```

### 🏛️ 一、憲法與任務中樞 (Constitution & Active Tasks)
- 📜 **[專案全局指令與架構憲法 (GEMINI.md)](GEMINI.md)**：包含最高元契約「不可刪除/修改保護鎖」、100% 資料驅動鐵律、技術邊界約束與 16 大模組按需載入矩陣。
- 🎯 **[當前迭代微任務看板 (ACTIVE_TASKS.md)](ACTIVE_TASKS.md)**：遵循 AI Agentic Workflow，以 `- [ ]` 微任務單一聚焦當前里程碑，實施三位一體完成定義 (DoD)。

### 🚀 二、簡要概覽層 (Overview — 3~5 分鐘快速掌握)
- 🧩 **[遊戲功能模組全景地圖 (MODULES_MAP.md)](docs/overview/MODULES_MAP.md)**：後端 9 大核心模組、前端 6 大表現模組與端到端協同工作流。
- 🖥️ **[前端整體佈局與視圖切換全景手冊 (FRONTEND_LAYOUT_GUIDE.md)](docs/overview/FRONTEND_LAYOUT_GUIDE.md)**：五大版塊空間骨架、三大主舞台互斥呈現、五大場景模組配置與狀態轉移矩陣。
- ⚡ **[快速上手與操作指南 (QUICKSTART.md)](docs/overview/QUICKSTART.md)**：環境配置、開局冒險流程、鍵盤快捷鍵與常見問答。
- 🌌 **[玩法世界觀與雙軌模式 (GAMEPLAY_OVERVIEW.md)](docs/overview/GAMEPLAY_OVERVIEW.md)**：暗黑克系修仙、道基五維、道心 SAN 值異變與小隊編制。
- 🏛️ **[高階架構與核心準則 (ARCHITECTURE_SUMMARY.md)](docs/overview/ARCHITECTURE_SUMMARY.md)**：潔淨架構依賴反轉、零硬編碼鐵律與架構看門狗。

### 🔬 三、深入層 (Deep Dive Manuals — 15 大前後端模組技術手冊)

#### ⚙️ 後端 9 大核心業務模組手冊 (M1 ~ M9)
- 🌲 **[後端 M1. 探索與世界模組手冊](docs/manual/後端_M1_探索與世界模組_WORLD_AND_EXPLORATION.md)**：房間拓撲網絡、NPC 活動、地面遺落物非同步持久化與 WorldPulse 心跳。
- 🧭 **[後端 M2. DRPG 網格地牢模組手冊](docs/manual/後端_M2_DRPG網格地牢模組_DRPG_DUNGEON.md)**：離散網格座標模型、前方 3 步視野透視、暗黑迷霧與 exitRoomId 撤離保護。
- ⚔️ **[後端 M3. 核心戰鬥引擎手冊](docs/manual/後端_M3_核心戰鬥引擎模組_COMBAT_ENGINE.md)**：半即時 ATB 輪詢、圓桌判定公式、罡氣削韌破防、九大五行抗性與逆流回血。
- 👥 **[後端 M4. 小隊、生靈與陣法模組手冊](docs/manual/後端_M4_小隊生靈與陣法模組_PARTY_AND_FORMATION.md)**：5 人小隊編制、道基五維、道心 SAN 值異變、夥伴招募與陣法自動排位。
- ⚡ **[後端 M5. 技能、境界與修煉模組手冊](docs/manual/後端_M5_技能境界與修煉模組_SKILL_AND_PROGRESSION.md)**：境界突破曲線、自由屬性點分配、技能動態屬性縮放 (Skill Scaling)。
- 📦 **[後端 M6. 物品、背包與經濟模組手冊](docs/manual/後端_M6_物品背包與經濟模組_ITEM_AND_ECONOMY.md)**：40 格小隊儲物袋、裝備槽位枚舉契約、貨棧買賣與靈藥消耗。
- 🗄️ **[後端 M7. 100% 資料驅動系統手冊](docs/manual/後端_M7_資料驅動模板模組_DATA_DRIVEN_TEMPLATES.md)**：JSON 檔案目錄佈局、TemplateRepository 快取、開機完整性自我校驗。
- 💾 **[後端 M8. 存檔與持久化架構手冊](docs/manual/後端_M8_存檔與歷史快照模組_SAVE_AND_PERSISTENCE.md)**：單機 SaveData JSON 單一真相源、五大槽位與自動存檔、壞檔備份防護。
- 🧵 **[後端 M9. 虛擬執行緒 Actor 通訊手冊](docs/manual/後端_M9_虛擬執行緒Actor通訊模組_ACTOR_AND_PROTOCOL.md)**：Java 21/25 Virtual Threads 實踐、VirtualActor 信箱隔離、MudContext 治理與 WebSocket 全狀態契約。

#### 🎨 前端 6 大表現模組手冊 (FE-1 ~ FE-6)
- ⚡ **[前端 FE-1. 核心狀態與事件管線手冊](docs/manual/前端_FE1_核心狀態與事件管線_CORE_STATE_AND_PIPELINE.md)**：中央響應式狀態樹 (store.js)、純原生 `[data-action]` 全域事件委派、WebSocket 心跳與退避重連。
- 🗺️ **[前端 FE-2. DRPG 視口與雷達渲染器手冊](docs/manual/前端_FE2_DRPG視口與雷達渲染器_DRPG_VIEWPORT_RENDERER.md)**：第一人稱透視視口、雙雷達相機模式 (跟隨 vs 全圖)、7 大通用圖塊圖元外觀渲染契約。
- ⚔️ **[前端 FE-3. 戰鬥擂台與特效引擎手冊](docs/manual/前端_FE3_戰鬥擂台與特效引擎_BATTLE_ARENA_AND_FX.md)**：即時血條/韌性條/ATB 行動進度條、破防爆裂粒子動效、打擊飄字 (FCT) 與 Buff HUD 氣泡。
- 🏛️ **[前端 FE-4. 城鎮中樞與動作列面板手冊](docs/manual/前端_FE4_城鎮中樞與動作列面板_TOWN_HUB_AND_ACTION_BAR.md)**：城鎮 NPC 卡牌與出口網格、探索與戰鬥雙態動態變形快捷動作列、行動端響應式排版。
- 👥 **[前端 FE-5. 小隊裝備與存檔彈窗手冊](docs/manual/前端_FE5_小隊裝備與存檔彈窗_PARTY_EQUIPMENT_AND_MODALS.md)**：5 人小隊數值與陣法切換、100% 資料驅動換裝分頁、五大存檔槽位與戰鬥勝利戰利品結算。
- 🎨 **[前端 FE-6. 七層樣式串流與視覺主題手冊](docs/manual/前端_FE6_七層樣式串流與視覺主題_CSS_TOKENS_AND_THEMES.md)**：暗黑克系修仙調色盤 (CSS Tokens)、七層 Cascade 依賴串流順序、無障礙字級排版規範。

### 📈 四、工程演進與歷史 (Evolution & Roadmap)
- 📜 **[專案變更日誌 (CHANGELOG.md)](CHANGELOG.md)**：記錄所有版本重構、功能上線與缺陷修復歷史。
- 🏗️ **[系統架構說明書 (ARCHITECTURE.md)](ARCHITECTURE.md)**：完整的系統目錄樹與設計架構細節。
- 🎯 **[未來演進與治理藍圖 (FUTURE_IMPROVEMENTS.md)](docs/plans/FUTURE_IMPROVEMENTS.md)**：全專案待辦優先級矩陣與治理清單。

---

## 🚨 專案開發第一紅線提醒 (Golden Rules)
1. **100% 資料驅動與零硬編碼**：嚴禁在 Java 或 JS 代碼中透過中文名稱推測業務邏輯（如 `name.includes("甲")`），所有數值與特例均須由 JSON 宣告。
2. **實體深拷貝不可遺漏屬性**：在 `LivingStats.deepCopy()` 等快照處，嚴禁遺漏任何屬性（包含抗性、技能、戰鬥資源）。
3. **繁體中文 Commit 規範**：所有 Git commit 訊息必須使用繁體中文，並遵循 Conventional Commits 格式。
