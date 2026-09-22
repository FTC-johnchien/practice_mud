# 專案架構與目錄規格說明書 (Architecture & Project Structure)

本文件定義本專案的**目錄劃分**、**資料存放規則**與**新增資源之標準作業流程 (SOP)**，避免檔案散落混亂。

---

## 1. 頂層目錄總覽 (Root Directory Layout)

```
practice_mud/
├── .mvn/                       # Maven Wrapper 配置
├── db/                         # 【本機資料庫】H2 Database 檔案 (./db/muddb.mv.db，Git 忽略)
├── docs/                       # 【開發文檔與設計規格】
│   ├── templates/              # 靜態資源 JSON 規範範本 (如技能、配置草稿)
│   └── references/             # 【世界觀原著參考】小說文本與文案資料
├── saves/                      # 【單機遊戲存檔】本機 JSON 存檔 (slot_1.json, autosave.json 等)
├── src/
│   ├── main/
│   │   ├── java/               # 領域驅動設計 (DDD) 核心 Java 原始碼
│   │   └── resources/          # 遊戲靜態設定、資料庫與前端資源
│   └── test/                   # 單元與整合測試套件 (使用獨立記憶體資料庫)
├── pom.xml                     # Maven 專案配置 (Java 25, Spring Boot 4.1.1)
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

### 資源 ID 與 namespace 規範

- `data/global/items/` 的物品定義 ID 不加前綴，例如 `taiyin_pill`。
- `data/zones/<zone>/items.json` 的物品定義 ID 同樣不加前綴；載入後才正規化為 `<zone>:<id>`。
- **區域資料中的物品參照必須明確標示 scope**：全域物品使用 `global:<id>`，區域物品使用 `<zone>:<id>`。適用於怪物掉落／裝備、商店商品與房間門鎖鑰匙。
- 載入器會將 `global:<id>` 正規化為全域模板 ID；不得依賴「移除 namespace 後猜測全域物品」的 fallback。
- `DataNamespaceIntegrityTest` 是此契約的防線；新增或修改區域資料時，必須先讓該測試通過。

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

---

## 5. MUD 與 DRPG 共用與整合架構 (MUD & DRPG Unified Integration Architecture)

為避免 MUD (Tick/敘事) 與 DRPG (Turn/隊伍戰術) 資料模型分裂，專案採用四項核心整合模式：

### A. 物品與屬性單一真相源 (Single Source of Truth)
- **物品資料庫唯一化**：所有物品集中於 `data/global/items/` 與 `data/zones/*/items.json`，嚴禁因 MUD/DRPG 模式差異而重複定義同名物品。
- **執行期實體轉換**：
  - `PartyItemSlot.fromItemTemplate(ItemTemplate)`：直接將靜態模板轉化為 DRPG 隊伍行囊插槽。
  - `PartyItemSlot.toGameItem()`：將 DRPG 物品無損轉化回 MUD `GameItem`。
  - `MudTemplateAdapter` / `DrpgTemplateAdapter`：提供領域專屬的實例化工廠方法。
- **角色數值同步**：
  - 由 `CharacterSyncService` 維護單一真相源：進入副本時從 `Player` 同步至 `PartyMember` 隊長；離開副本或戰鬥勝利時將屬性回寫 `Player`。

### B. 穩定角色識別與會話邊界 (Character Identity & Session Boundary)
- **值物件封裝**：以強型別不可變值物件 `CharacterId` 作為跨系統的唯一角色識別，徹底淘汰單機寫死之 `"p-single"`。
- **雙重別名容錯索引 (Dual-Index Alias Resolution)**：
  - `PartyService` 與 `DungeonManager` 支援以 `CharacterId`、`playerId` 或隊長名稱雙向查詢，徹底杜絕「存檔用 ID、指令用 Name」造成的隊伍狀態分裂。

### C. 技能語意橋接與動態縮放 (SkillBridge Runtime Semantic Bridge)
- **拒絕脆弱的 JSON 硬合並**：
  - MUD 技能（`SkillTemplate`、連續敘事招式名、熟練度 `SkillEntry`）與 DRPG 戰術技能（`PartyMemberSkill`、冷卻、傷害倍率、怒氣/連擊資源）保持分離。
- **執行期語意映射與動態倍率縮放**：
  - 由 `SkillBridgeService` 依據玩家已習得的 MUD 武學與等級，動態解鎖對應的 DRPG 小隊戰術招式（如劍法解鎖破空劍氣與太陰萬劍訣、守禦解鎖金剛怒目、刺術解鎖穿心瞬影）。
  - MUD 武學等級與角色基礎屬性（STR/DEX/INT）即時提升傷害倍率、縮減冷卻時間（-100ms/級）、提升治療效果。
  - 戰後結算調用 `awardCombatSkillXp`，將 DRPG 戰鬥成果回饋為 MUD 熟練度並促成突破。

### D. 戰鬥服務模組化解構 (Battle Service Decomposition)
- **門面外觀模式**：`DrpgBattleService` 轉為輕量協調者（Facade Coordinator），100% 維持既有 API 簽名與建構子，指令層與測試無痛相容。
- **職責單一化拆分**：
  - `DrpgCombatLoop`：管理虛擬執行緒戰鬥心跳迴圈（500ms Tick）、走火入魔瘋狂狀態機（SAN 歸零 CHAOS、異變計數、心魔背刺、畸變降世）。
  - `DrpgEnemyTacticsService`：管理仇恨計算（前排 1.3 倍權重）、嘲諷鎖定、怪物天生攻擊抽取與隊友 Gambit 戰術規則鏈求值。
  - `DrpgRewardService`：管理勝利/潰敗結算、深淵道核剖取、首領擊殺事件發布、掉落收納與戰後封印解除。

---

## 6. 後續改善與架構演化索引 (Future Improvements Index)

專案後續之長期演化藍圖、安全加固、領域解耦、Actor 併發安全以及測試品質加固清單，請統一參照：
👉 **[`docs/plans/FUTURE_IMPROVEMENTS.md`](./docs/plans/FUTURE_IMPROVEMENTS.md)**

該文件包含：
1. **安全加固 (P0/P1)**：H2 Console 遠端存取防禦、前端 DOM XSS 轉義、存檔槽位使用者空間隔離、WebSocket CORS 白名單、AuthService 密碼加密 Bean 注入。
2. **清潔架構與解耦 (P1)**：領域層依賴反轉（避免 Domain 依賴 Web/Auth）、Direction/ResourceType 命名衝突拆分、指令冗餘消除與解耦。
3. **併發與 Actor 健全度 (P1)**：VirtualActor 自死鎖防禦、RoomMessageBuffer 執行緒洩漏修復、Actor 例外中毒恢復、LivingStats 封裝防禦。
4. **資料驅動與死代碼清理 (P2)**：Canonical Template 深化、`rooms.json` 鑰匙 ID 校驗、歷史死代碼清理、`.gitignore` 存檔路徑補齊。
5. **前端工程模組化 (P2)**：`drpg-view.js` 與 `style.css` ES 模組化拆分、DOM 安全存取防禦。
6. **測試工程 (P2/P3)**：補齊未測試指令與 CommandDispatcher 測試、以 Awaitility 取代 Thread.sleep、靜態表註冊清理。
