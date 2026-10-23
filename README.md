# ☯️ 深淵仙途 (The Dao Protocol: Abyssal Overclock)
### 純單機賽博修仙 Hack-and-slash DRPG (Action-Tactical DRPG)

[![Java](https://img.shields.io/badge/Java-21%20%2F%2025-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-green.svg)](https://spring.io/projects/spring-boot)
[![Tests](https://img.shields.io/badge/Tests-376%20Passed-brightgreen.svg)]()
[![Architecture](https://img.shields.io/badge/Architecture-Pure%20Singleplayer%20DRPG%20%26%20100%25%20Data--Driven-blue.svg)]()

> **遊戲簡介**：  
> 《深淵仙途》是一款融合了**克蘇魯神話不可名狀恐怖**、**賽博巨企掠奪科技**與**東方古典修仙**的**純單機 Action-Tactical DRPG**。  
> 玩家將扮演在黑煞宗地底礦脈通鋪甦醒的採礦奴隸——其體內加載著前世賽博特工晶片與異形強殖基因。在**「選單式營地中樞 (Town Hub)」**進行裝備鍛造分解、晶片超頻鑲嵌、道場洗點與小隊整備；在**「第一人稱 3D 網格地牢 (Grid Dungeon)」**中置身深邃幽冥走廊，消耗火把探索迷宮與避開機關陷阱；並在半即時戰鬥中藉由圓桌判定、罡氣削韌破防 (Poise Break)、反制打斷 (Interrupt)、元素連鎖與古仙陣法連攜合擊，體驗拳拳到肉、滿屏跳字的極致砍殺刷寶快感！

---

## 🌟 五大核心目標 (The Five Pillars)

全專案的所有系統架構與業務代碼實作，無條件服務於五大核心目標：
1. 🌌 **世界觀的介紹 (World & Lore)**：森羅星萬年外神偽地殼卵鞘、工程師採掘遺產、賽博巨企掠奪、特工晶片 × 異形強殖基因。
2. ⚡ **戰鬥爽快 (Impactful Combat)**：半即時快節奏砍殺、罡氣削韌、破防暴擊、反制打斷、滿屏 FCT 浮動大字與強烈打擊回饋。
3. 🧬 **技能組合 (Skill Synergy)**：四大賽博修仙流派（神經超頻、生化血肉、重力巨力、太虛矩陣）、元素反應引爆、Gambit 自動戰術鏈。
4. 💎 **裝備收集 (Loot Progression)**：妖獸煞石、千星科技加密晶片、豐富隨機詞綴、多階稀有度與裝備注靈分解重鑄。
5. 📈 **角色養成 (Character Progression)**：道基五維自由加點、境界突破、特工核心超頻插槽、5 人戰術小隊自由構築。

---

## 🚀 極速開始 (Quickstart in 1 Minute)

### 1. 環境需求
- **JDK**：Java 21 或 Java 25（支援現代 Virtual Threads 與高吞吐並發）
- **Maven**：3.9+（專案內建 `mvnw.cmd`）
- **瀏覽器**：Chrome / Edge / Firefox / Safari (支援現代 ES6 Modules 與 WebSocket)

### 2. 啟動本機伺服器
```powershell
# 執行 Spring Boot 服務
.\mvnw.cmd spring-boot:run

# 或執行專案一鍵啟動腳本
.\run.ps1
```
啟動後造訪：👉 **`http://localhost:8080`**

### 3. 執行全量測試驗證
```powershell
# 執行全套單元、整合與架構看門狗測試 (目前基準: 373 項 100% 綠燈通過)
.\test.ps1
```

---

## 📚 專案工程文檔導覽體系 (Documentation Map)

本專案嚴格遵循現代軟體工程與高沉浸 RPG 開發規範，所有系統規格與任務實施統一託管於 [`docs/`](./docs/) 目錄下，劃分為 **4 大遞進層級**：

```text
docs/
├── 1_requirements/              # Level 1: 需求與願景層 (Why & What)
│   ├── WORLD_AND_LORE_BIBLE.md   # 世界觀聖經：森羅星卵鞘、工程師、賽博巨企、擬態修仙
│   ├── CYBERPUNK_CULTIVATION_SPEC.md # 賽博特工能力與克蘇魯修真技能映射需求規格
│   ├── UI_UX_REQUIREMENTS.md     # 畫面佈局：PC優先/手機自適應、敘事文字視窗、走格與戰鬥
│   └── GAMEPLAY_CORE_LOOPS.md    # 核心循環：營地中樞選單/地牢走格遠征閉環、道基五維體系
│
├── 2_specifications/            # Level 2: 模組架構與技術規格層 (How & Architecture)
│   ├── ARCHITECTURE_SUMMARY.md   # 頂層技術架構總覽 (Java 25 虛擬執行緒、Spring Boot)
│   ├── MODULES_MAP.md            # 全功能模組拼圖與拓撲邊界 (後端9+1、前端6)
│   ├── backend/                  # 後端 9+1 大業務模組技術規格手冊 (M1 ~ M9)
│   ├── frontend/                 # 前端 6 大表現模組技術規格手冊 (FE1 ~ FE6)
│   └── data_schemas/             # 資料驅動拓撲與 JSON Schema 模板
│
├── 3_task_lists/                # Level 3: 全局任務清單與演進路線層 (Roadmap & Backlog)
│   ├── FUTURE_IMPROVEMENTS.md    # 宏觀演進戰略總表 (含五大核心目標中長程內容拓展藍圖)
│   ├── future_improvements_history.md # 歷史已完成驗收之架構改善庫 (Phase 0~25)
│   └── CHANGELOG.md              # 歷史版本交付變更記錄 (單一權威來源)
│
├── 4_active_tasks/              # Level 4: 當前焦點與微任務看板層 (Current Iteration & Sprint)
│   ├── ACTIVE_TASKS.md           # 當前單一焦點微任務看板 (單一權威來源)
│   └── SPRINT_GOAL.md            # 當前衝刺目標對齊聲明 (防偏離校準錨點)
│
└── 0_archive/                   # 封存歷史參考資料 (Historical References & Legacy Architecture)
```

👉 **深入查閱各模組規格與架構詳情，請參閱：[`docs/README.md`](./docs/README.md)**

---

## 🛡️ 專案開發三大第一紅線 (Core Guardrails)

1. **100% 資料驅動 (Data-Driven Purity)**：
   遊戲所有數值、技能、道具、地圖、陣型、掉落表完全脫離 Java 代碼，100% 定義於 `src/main/resources/data/**/*.json`。代碼中嚴禁任何硬編碼判斷。
2. **純單機單體架構 (Single-Player Monolith)**：
   小隊行囊 (`party.getInventory()`) 為全遊戲唯一背包；主角即隊長；前端以單一全景狀態契約 (`DrpgStateDto`) 與結構化動作協定 (`ActionCommand`) 與後端通訊。
3. **機制運行而非特例驗證 (Mechanism Testing)**：
   測試驗證專注於遊戲機制的邊界防禦與演算法，全專案 373+ 項單元與整合測試保持 100% 全天候綠燈通過。
