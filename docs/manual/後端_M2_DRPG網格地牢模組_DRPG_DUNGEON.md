# 🧭 後端 M2: DRPG 網格地牢模組深度手冊 (DRPG Dungeon Manual)

> **模組編號**：M2  
> **系統類型**：後端核心業務模組  
> **核心定位**：管理第一人稱離散網格座標系、前方 3 步光照視野射線、暗黑迷霧踩格狀態機、地塊機關互動（石門/寶箱/陷阱/出口），以及步步驚心遇敵檢定。

---

## 🧭 一、離散網格地牢座標系與模型

地下城採用離散整數網格系統，以地圖左上角為原點 `(0, 0)`：

```
(0, 0) ────────────────── X+ (Width)
  │      北 (NORTH, dy = -1)
  │  西 ◄──────┼──────► 東 (EAST, dx = +1)
  │      南 (SOUTH, dy = +1)
  ▼
 Y+ (Height)
```

### 1. 核心實體模型
- **`DungeonFloor`**：表示單一樓層（如墨竹礦坑一層），持有 `width`、`height`、`tiles[y][x]` 二維陣列、`startCoord`、`dangerRate`，以及 2026-10-15 新增的 **`exitRoomId`（撤離後重返地表房間 ID）**。
- **`DungeonPosition`**：玩家小隊當前動態位置，持有 `floorId`、`x`、`y`、`facing`（朝向：`NORTH`, `EAST`, `SOUTH`, `WEST`），以及兩個關鍵動態狀態矩陣：
  - `visited[y][x]`：布林陣列，記錄小隊已走過的暗黑迷霧。
  - `openedChests[y][x]`：布林陣列，記錄該樓層已開啟的棺槨寶箱。

---

## 🔦 二、前方視野探測演算法 (Inspect Forward)

當玩家邁步或原地轉身時，後端 `DungeonNavigator.inspectForward(floor, pos)` 會依據小隊當前朝向，沿視線方向向前投射 3 步射線：

```
[小隊站在 (x, y), 朝向 NORTH]
Step 1: (x, y - 1)  ──>  若遇 WALL 或 DOOR，立即阻斷射線
Step 2: (x, y - 2)
Step 3: (x, y - 3)
```

- **阻擋機制**：若第 1 步即為「玄岩石壁（WALL）」或「閉合石門（DOOR）」，視線將無法穿透至第 2、3 步。
- **視野文本描述**：將前方地塊特徵組裝成生動的修仙文本（如：「正前方是一座緊閉的加固避煞木門，門縫隱隱散發暗紫異化煞氣」）。

---

## 🎲 三、動態遇敵率演算法 (Danger Rate)

地牢探索處處危機，每走一步均會觸發危險度檢定：
1. **基礎危險度**：來自樓層配置的 `dangerRate`（如墨竹礦坑 $0.15$）。
2. **步數累積加權**：若長時間未遇敵，累計危險權重逐步爬升。
3. **戰鬥觸發**：當隨機擲骰落入危險區間時，立即中斷常規探索，由 `DrpgBattleService` 接管並啟動半即時戰鬥。

---

## 🚪 四、地牢撤離與安全結算機制 (Dungeon Exit Pipeline)

在地牢中隨時可能遭遇強大魔物全滅。專案建立了嚴密的撤離保護機制：

```mermaid
graph TD
    Step["玩家踩在具有 STAIRS_UP (<) 的安全石階"] --> Input["輸入指令 leave 或點擊撤離按鈕"]
    Input --> Check{"檢驗當前地塊是否具備 EXIT 特性"}
    Check -- 是 --> Query["讀取 floor.getExitRoomId() (如 mozhu_mines:mine_entrance)"]
    Query --> Reset["inDungeon 標記重置為 false，切回城鎮 MUD 模式"]
    Reset --> Save["AutoSaveService 自動觸發戰利品安全快照存檔"]
    Reset --> Broadcast["GameStateBroadcastService 廣播城鎮大堂狀態"]
    Check -- 否 --> Reject["提示: 此處沒有通往地表的出口石階，無法撤離"]
```

1. **撤離條件**：玩家必須移動至具有 `STAIRS_UP`（`<`）特性的地塊（通常為入口石階）。
2. **動態導向**：後端讀取當前樓層配置之 `exitRoomId`，徹底告別寫死房間字串。
3. **狀態復原**：將玩家 `inDungeon` 重置為 `false`，安全切換回城鎮 MUD 視圖，並觸發自動快照存檔。

---

## 🔗 五、代表類別與資料設定檔

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **地牢中樞** | `com.example.htmlmud.domain.dungeon.DungeonManager` | 地牢狀態持有與樓層切換 |
| **導航演算** | `com.example.htmlmud.domain.dungeon.DungeonNavigator` | 網格位移、朝向旋轉與前方 3 步探測 |
| **地圖載入** | `com.example.htmlmud.domain.dungeon.DungeonFloorLoader` | 讀取 JSON 解析二維字元矩陣與圖例 |
| **樓層模型** | `com.example.htmlmud.domain.dungeon.DungeonFloor` | 尺寸、地塊、初始點、危險度、exitRoomId |
| **指令轉發** | `com.example.htmlmud.application.command.impl.DungeonCommand` | 處理 step / turn / inspect / leave 指令 |
| **資料設定** | `src/main/resources/data/dungeons/mozhu_mines_b1f.json` | 墨竹礦坑一層 10x10 地牢地圖與圖塊配置 |
| **資料設定** | `src/main/resources/data/dungeons/taiyin_tomb_b1f.json` | 太陰古塚一層 10x10 地牢地圖與圖例宣告 |

---

## 🚀 六、未來架構演進與現代化擴充規劃 (Future Roadmap & Scalability)

為使 DRPG 網格地牢模組向經典《世界樹迷宮》與現代克系地牢體驗看齊，本模組確立以下四大演進方向：

### 1. 🎯 現代遊戲機制深化 (Modern Gameplay & Depth)
- **立體機關地貌機制 (Advanced Dungeon Mechanics)**：引入「單向冰面滑行（踩格強制沿滑行方向滑動直至撞牆）」、「旋轉機關石盤（改變當前小隊朝向）」、「古修傳送玄陣」與「陷落深坑落穴（掉入下層未知區域）」，打破單純平面的走格子侷限。
- **照明度與火把資源機制 (Torch & Sanity System)**：引入照明半徑。火把燃盡時視野縮短為 1 格、遭遇魔物突襲率（Ambush Rate）提升 50%，且在深沉死寂的黑暗中行走每 10 步扣減小隊 2 點 SAN 值，營造極致心理壓迫感。

### 2. 🛡️ 穩定性與狀態防禦 (Robustness & Resilience)
- **地塊事件策略化工廠 (TileEventStrategyFactory)**：目前地塊觸發零散分佈於導航器中。未來全面抽象為 `TileEventHandler` 介面（`DoorEventHandler`, `ChestEventHandler`, `TrapEventHandler`, `StairsEventHandler`），事件新增完全閉合，杜絕條件分支膨脹。
- **邊界死鎖與穿牆嚴密防護斷言**：在 `DungeonNavigator` 增加越界雙重斷言，任何未配置的未知字元自動當作堅硬玄岩石壁（WALL）處理，嚴防玩家座標穿出地圖邊界造成空指針崩潰。

### 3. ⚡ 效能與記憶體最佳化 (Performance & Concurrency)
- **迷霧與寶箱矩陣 BitSet 位元壓縮**：將 `boolean[y][x]` 二維物件陣列升級為 `java.util.BitSet`，10x10 網格僅需 100 bits（不到 13 位元組），序列化傳輸體積與記憶體佔用驟降 87.5%，存檔讀寫極速化。
- **射線投射探測快取 (Raycast Spatial Cache)**：靜態石壁地貌之 3 步探測文字引入空間雜湊快取，相同座標與朝向直接自快取回傳，避免頻繁的字串拼接與物件建構。

### 4. 🔌 可擴充性與模組化 (Extensibility & Modding)
- **程序化隨機地牢生成器 (Procedural Dungeon Generator / PCG)**：為「無盡太陰幽冥深淵」模式引入 BSP（二元空間分割）或隨機漫步演算法，遊戲可依據世界種子動態生成符合 JSON 結構標準的無限層地下城，實現高重玩度的 Roguelike 修仙地牢。
