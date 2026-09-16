# 專案架構與目錄規格說明書 (Architecture & Project Structure)

本文件定義本專案的**目錄劃分**、**資料存放規則**與**新增資源之標準作業流程 (SOP)**，避免檔案散落混亂。

---

## 1. 頂層目錄總覽 (Root Directory Layout)

```
practice_mud/
├── .mvn/                       # Maven Wrapper 配置
├── db/                         # 【本機資料庫】H2 Database 檔案 (./db/muddb.mv.db，Git 忽略)
├── docs/                       # 【開發文檔與設計規格】
│   └── templates/              # 靜態資源 JSON 規範範本 (如技能、配置草稿)
├── reference/                  # 【世界觀原著參考】小說文本與文案資料
├── saves/                      # 【單機遊戲存檔】本機 JSON 存檔 (slot_1.json, autosave.json 等)
├── src/
│   ├── main/
│   │   ├── java/               # 領域驅動設計 (DDD) 核心 Java 原始碼
│   │   └── resources/          # 遊戲靜態設定、資料庫與前端資源
│   └── test/                   # 單元與整合測試套件 (使用獨立記憶體資料庫)
├── pom.xml                     # Maven 專案配置 (Java 25, Spring Boot 3.5.10)
├── run.bat / run.ps1           # 遊戲本機啟動指令碼 (Port 8080)
└── test.bat / test.ps1         # 全自動化測試指令碼
```

---

## 2. 資源配置層劃分 (`src/main/resources/`)

```
src/main/resources/
├── application.yml             # Spring Boot 核心配置 (虛擬執行緒、H2 連線池、連接埠)
├── logback-spring.xml          # 日誌格式配置
├── data/                       # 【遊戲靜態數值與地圖庫】
│   ├── dungeons/               #  【地牢探索區】(DRPG 2D 矩陣網格迷宮)
│   ├── global/                 # 🌐 【全域規則庫】
│   │   ├── classes.json        # 門派 / 職業設定
│   │   ├── races.json          # 種族與體質係數
│   │   ├── weapon_config.json  # 武器分類設定
│   │   └── skills/             # 50 門武學與法術 JSON (依派系子資料夾分類)
│   └── zones/                  # 🏛️ 【城鎮樞紐區】(MUD 圖狀拓撲房間)
│       ├── newbie_village/     # 新手村
│       ├── mozhu_mines/        # 墨竹礦坑
│       ├── snow/               # 雪亭鎮
│       ├── silverleaf/         # 銀葉村
│       └── taiyin_tomb/        # 太陰古塚地表入口與外圍
└── static/                     # 【Web 前端客戶端】
    ├── index.html              #  主要遊戲客戶端 (DRPG 探索器 + 陣法奧義 + 存讀檔)
    ├── css/style.css           # 遊戲視覺樣式表
    ├── js/drpg-view.js         # DRPG 網格即時雷達與探索控制器
    └── legacy/                 # 歷史測試頁面歸檔 (封存備用)
```

---

## 3. 地圖資料雙層體系規範 (Hub & Dungeon Model)

為了最大化發揮專案實力並確保遊玩節奏順暢，地圖採用「**雙層設計模式**」：

### A. 城鎮樞紐區 (`data/zones/`)
- **適用場景**：新手村、市集、門派、客棧、店鋪。
- **特點**：以房間節點 (Room) 與方向出口 (Exits) 連接。
- **職責**：
  - 提供 NPC 對話、任務承接。
  - 鐵匠鋪裝備鍛造與買賣、客棧打坐回血。
  - 整理背包、編制隊伍陣法（Formation）。
- **結構**：每個 Zone 目錄包含 `manifest.json`、`rooms.json`、`mobs.json`、`items.json`。

### B. 地牢探索區 (`data/dungeons/`)
- **適用場景**：深淵、古塚內部、礦坑深處、迷宮副本。
- **特點**：以 2D ASCII 陣列佈局，具備朝向、步進、視野迷霧與暗雷隨機遇敵。
- **職責**：
  - 步進探索、避開陷阱、開棺摸金、探索地牢謎題。
  - 遭遇不可名狀怪物戰鬥、消耗 San 值與精力。
- **結構**：單檔 `.json`，包含 `layout`、`legend`、`encounters`。

### C. 兩者連接規範
- 在城鎮房間（例如 `taiyin_tomb:entrance`）中提供通往地牢的出口。
- 玩家進入地牢後切換為 DRPG 雷達步進模式；抵達地牢出口（階梯 `<`）後返回城鎮房間。

---

## 4. 新增遊戲內容之 SOP

### 新增一門技能 (Skill)
1. 確定技能派系（例如 `common`, `force`, `unarmed`, `mobs`）。
2. 在 `src/main/resources/data/global/skills/<派系>/` 下新增 `<skill_id>.json`。
3. 確保 `type` 屬於 `SkillType`（`ACTIVE`, `PASSIVE`, `REACTIVE`, `CHANNEL`）。
4. 確保 `allowedWeapons` 屬於 `WeaponType` 中的枚舉項。

### 新增一個地牢迷宮 (Dungeon Floor)
1. 在 `src/main/resources/data/dungeons/` 新增 `<floor_id>.json`。
2. 規劃 2D `layout` 矩陣與 `legend` 圖例。
3. 在 `encounters.mobs` 中引用已存在的怪物 ID（格式為 `<zone_id>:<mob_id>`）。
