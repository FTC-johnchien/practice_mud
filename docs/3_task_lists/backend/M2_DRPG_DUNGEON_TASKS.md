# 🗺️ M2: DRPG 網格地牢探索模組實作進度清單 (DRPG Dungeon Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/backend/M2_DRPG_DUNGEON.md`](../../2_specifications/backend/M2_DRPG_DUNGEON.md)  
> **核心職責**：管理純單機第一人稱 2D 矩陣地牢（10x10 等規格）、走格步進、暗雷遇敵率、火把光明度消耗與機關陷阱檢定。  
> **當前完成度**：90% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心資料配置**：
  - `src/main/resources/data/dungeons/mozhu_mines_b1f.json`（墨竹山廢棄礦坑一層）
  - `src/main/resources/data/dungeons/taiyin_tomb_b1f.json`（太陰古塚一層）
- **核心實體與服務**：
  - [`DungeonFloorLoader.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/service/DungeonFloorLoader.java)（地牢樓層資料載入器，支援別名解析）
  - [`DungeonManager.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/service/DungeonManager.java)（地牢狀態機、位移轉向與踩格判定）
  - [`DungeonPosition.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/model/DungeonPosition.java)（玩家座標 x, y 與朝向 facing）
- **關聯驗證測試**：
  - [`DungeonTorchAndTrapTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/DungeonTorchAndTrapTest.java)（火把與陷阱 100% 綠燈）
  - [`WorldDataIntegrityTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/WorldDataIntegrityTest.java)（地牢資料拓撲檢驗 100% 綠燈）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **M2-CORE-MAP (10x10 地牢矩陣載入器)**
  - 實作 `DungeonFloorLoader`，由 JSON 資料驅動解析 layout 矩陣、Legend 圖例、遇敵配置與出口 `exitRoomId`。
- [x] **M2-TORCH-SYSTEM (火把照明度核心計量)**
  - 實裝光明度計量 (0~100) 與步進衰減；支援明亮 (BRIGHT, 視野 3 格)、昏暗 (DIM, 視野 2 格) 與漆黑 (DARKNESS, 視野 1 格且每步扣除 SAN 值)。
- [x] **M2-STEP-TRAPS (步進機關陷阱檢定)**
  - 實裝暗弩陷阱（身法檢定）、靈泉滋養（全員 HP/MP/SAN 大回補）、鎖鏈暗門（臂力/工具破拆）。
- [x] **M2-DATA-DRIVEN-PURGE (硬編碼地圖徹底清除)**
  - 物理刪除 `DungeonManager` 中 65 行手刻地圖代碼（`buildTombB1F()`），全專案 100% 依循 JSON 範本驅動。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task M2-1 (VS-01-LORE-GEOLOGY / ACTIVE-TASK-1): 黑煞宗地底礦脈地貌與第一人稱文字重塑**
  - **目標**：在 `mozhu_mines_b1f.json` 中充實黑煞岩壁 (`#`)、枕木坑道 (`.`)、避煞門 (`D`)、木箱 (`T`)、殘碑 (`E`)、外神卵塊泥沼 (`X`)、祭壇 (`B`) 之世界觀敘事。
  - **驗收標準**：地圖資料 100% 通過 `DataIntegrityValidator`；探索時文字卡與日誌精準呈現克蘇魯外神卵鞘地殼異變。
- [ ] **Task M2-2 (SAN-VISION-SECRETS): 低 SAN 幻覺與古修士靈視暗門**
  - **目標**：當小隊 SAN 值過低時，視界邊緣渲染扭曲粒子，並在地牢特定座標浮現「常態隱形之古修士道藏暗門」。
  - **驗收標準**：SAN < 30 時可與隱藏格互動獲得古代特工晶片或高階丹方。
- [ ] **Task M2-3 (DEFEAT-WIPE-SETTLEMENT): 全隊覆滅回營懲罰與虛弱復甦機制 (Defeat Settlement)**
  - **目標**：當小隊全員 HP 歸零或於地牢判定全滅時，觸發結算：扣除隨身戰利品、將狀態安全重置回營地、全員氣血虛弱復甦（30% HP）、安全保存寫入存檔。
  - **驗收標準**：全滅時不破壞存檔，營地復甦並提示扣除戰利品之情境日誌。
- [ ] **Task M2-4 (IN-DUNGEON-RESTRICTIONS): 地牢中禁止手動任意存檔與任意脫離**
  - **目標**：確保地牢探索中無法任意保存/讀取跳出，僅在抵達石階/單向脫離法陣時方可派發 `EXTRACT` 動作回營存檔。
  - **驗收標準**：在地牢中嘗試任意存檔或脫離被 ActionHandler 阻斷並提示需尋找撤離點。
