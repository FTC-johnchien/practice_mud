# 🧭 後端 M2: DRPG 網格地牢模組深度手冊 (DRPG Dungeon Manual)

> **模組編號**：M2  
> **系統類型**：後端核心業務模組  
> **核心定位**：管理第一人稱離散網格座標系、前方光照視野射線、暗黑迷霧踩格狀態機、地塊機關互動（石門/寶箱/陷阱/出口），以及步步驚心動態危險槽遇敵檢定。

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
- **`DungeonFloor`**：表示單一樓層（如墨竹礦坑一層），持有 `width`、`height`、`tiles[y][x]` 二維陣列、`startCoord`、`dangerRate`，以及 **`exitRoomId`（撤離後重返營地房間 ID，預設 `newbie_village:inn`）**。
- **`DungeonPosition`**：玩家小隊當前動態位置，持有 `floorId`、`x`、`y`、`facing`（朝向：`NORTH`, `EAST`, `SOUTH`, `WEST`），以及兩個關鍵動態狀態矩陣：
  - `visited[y][x]`：布林陣列，記錄小隊已走過的暗黑迷霧。
  - `openedChests`：內部以 `Set<GridCoord>` 記錄已開啟寶箱座標（於 `DrpgStateDto.DungeonViewDto` 投影為 `boolean[][] openedChests`，於 `SaveData` 存為 `"x,y"` 字串集合）。

---

## 🔦 二、前方視野探測與火把光明度機制 (Inspect Forward & Light Level)

### 1. 前方 3 步射線探測
當玩家邁步或原地轉向時，後端 `DungeonNavigator.inspectForward(floor, pos)` 依據小隊當前朝向，沿視線方向向前投射 3 步射線：

```
[小隊站在 (x, y), 朝向 NORTH]
Step 1: (x, y - 1)  ──>  若 !tile.isPassable() (如 WALL)，立即阻斷射線
Step 2: (x, y - 2)
Step 3: (x, y - 3)
```

- **阻擋機制**：以 `!tile.isPassable()` 判定阻斷。堅硬玄岩石壁（`WALL`）為不可通行故阻斷射線；而石門（`DOOR`）為可通行地塊故視線可穿透。
- **視野文字**：組裝結構化視野資訊，提供前端視口渲染與輔助敘事日誌。

### 2. 光明度與火把探險資源 (`torchLight`)
在暗黑克蘇魯地牢中，光芒是生與死的分界：
- **光明度衰減**：預設上限 100 點。僅前進或後退（`step w` / `step s`）每步消耗 1 點光明度（左右原地轉向消耗 0 點）。
- **點燃火把**：玩家使用行囊中的【火把 (Torch)】（`dungeon light` / `torch`）呼叫 `pos.resetTorch()` **直接回滿至 100 點**；若無火把則可消耗 10 靈石購買臨時靈火恢復 `+60` 點（上限 100）。
- **照度臨界區間**：
  - $\text{torchLight} > 50$【`BRIGHT` 明亮】：視野探測完整 3 步，危險槽累積倍率 `1.0x`。
  - $20 \le \text{torchLight} \le 50$【`DIM` 昏暗】：視野縮減至 2 步，危險槽累積倍率 `1.5x`。
  - $\text{torchLight} < 20$【`DARKNESS` 幽冥死寂】：視野僅餘 1 格，危險槽累積倍率 `2.0x`、**每走 1 步全隊扣減 5 點 SAN**，且每步有額外 25% 機率直接觸發暗影突襲戰鬥。

> 📌 **【精神防護罩（SAN 護盾）定位架構裁決】**：  
> 地牢黑暗中扣除 SAN 數值純屬「地牢探索資源管理與火把消耗博弈」，SAN 歸零時全員套用【精神萎靡】(-15% 傷害) 狀態，保留正常戰鬥與合擊資格。

---

## 🎲 三、動態遇敵槽演算法 (Danger Level)

地牢探索採用步步累積的危險槽機制（對齊 `DungeonCommand.handlePostMovement`）：
1. **危險槽累積**：玩家每走一步，隨機生成 $8 \sim 16$ 點基礎危險值，依當前照度狀態乘以倍率（`1.0x / 1.5x / 2.0x`），累加至 `pos.dangerLevel`。
2. **戰鬥觸發與重置**：當 `dangerLevel >= 100` 時，自當前樓層 `floor.mobPool` 隨機抽取 1~2 隻魔物組隊觸發戰鬥，並重置 `dangerLevel = 0`。
3. **樓層參數**：`floor.dangerRate` 作為樓層基準危險度元數據。

---

## 🚪 四、地牢撤離、全軍覆沒與生存限制 (Extraction & Defeat Settlement)

專案建立了嚴密的單機資產保全與戰敗結算機制（對齊 `DrpgRewardService.resolveDefeat` 與 `SaveGameService`）：

```mermaid
graph TD
    subgraph Extraction ["正常撤離 (Extraction)"]
        Step["踩在具有 STAIRS_UP (<) 撤離點"] --> Action["指令: dungeon extract"]
        Action --> Query["讀取 floor.getExitRoomId() (預設 newbie_village:inn)"]
        Query --> SaveSuccess["帶回所有裝備與戰利品，切換回營地並觸發 autosave.json"]
    end

    subgraph DefeatWipe ["全軍覆沒 (Party Wipe)"]
        Wipe["戰鬥中 5 人小隊全員氣血歸零"] --> WipeAction["DrpgRewardService 判定滅隊結算"]
        WipeAction --> SafeAssets["保留行囊內所有戰利品與資產 (單機友善)"]
        SafeAssets --> Penalties["全隊扣減 -20 SAN，氣血回復 30% MaxHP"]
        Penalties --> Revive["傳送回 floor.exitRoomId (營地客棧)"]
        Revive --> SaveWipe["立即寫入 autosave.json (slot 0)"]
    end
```

### 1. 安全撤離條件 (Extraction)
- 玩家移動至具有 `STAIRS_UP`（`<`）特性的地塊（入口石階或傳送陣）。
- 將玩家 `inDungeon` 重置為 `false`，安全切換回營地中樞視圖，帶回所有戰利品，並自動觸發存檔。

### 2. 全軍覆沒戰敗結算 (Party Wipe & Defeat)
- **觸發條件**：小隊 5 名成員氣血（HP）全部歸零。
- **單機資產保全**：全員扣除 **`-20 SAN`**、氣血回復至 `30% MaxHP`（保底 20 HP）、**保留行囊內所有戰利品與資產**（不掉落清空物品），傳送回 `floor.exitRoomId` 並立即寫入 `autosave.json`（`slot 0`）。

### 3. 地牢手動存檔限制 (In-Dungeon Save Restriction)
- 為維持探索博弈張力，地牢中手動存檔（`save <slot>`）僅允許於 **`STAIRS_UP` (`<`)、`STAIRS_DOWN` (`>`) 或 `EVENT` (`!`)** 三類地塊執行。

---

## ⚙️ 五、地塊機關檢定與互動機制 (Traps & Interactive Checks)

地塊具備多樣化的環境檢定博弈（對齊 `DungeonCommand.handleTileEvent`）：
1. **暗弩機關陷阱 (`TRAP`)**：踩格檢定先鋒隊員 `EffectiveDex >= 12` 免傷，檢定失敗先鋒受 `-25 HP`、餘人受 `-10 HP` 且全隊 `-5 SAN`；普通泥沼陷阱全員受 `-15 HP`、`-5 SAN`。
2. **鎖鏈暗門 (`DOOR`)**：檢定先鋒隊員 `EffectiveStr >= 12` 或小隊行囊持有破拆工具（`pickaxe` / `key`），成功解鎖並獎勵先鋒 `+20 SP`。
3. **靈泉地窟 (`SPRING`)**：每層樓單次觸發（`!tile.isTriggered()`），全體存活隊員固定回復 **`+50 HP`、`+30 MP`、`+15 SAN`**。

---

## 🔗 六、代表類別與資料設定檔

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **地牢指令** | `com.example.htmlmud.application.command.impl.DungeonCommand` | 處理走格、轉向、探測、點火與機關互動 |
| **導航演算** | `com.example.htmlmud.dungeon.DungeonNavigator` | 網格位移、朝向旋轉與前方 3 步探測 |
| **地圖載入** | `com.example.htmlmud.dungeon.DungeonFloorLoader` | 讀取 JSON 解析二維字元矩陣與圖例 |
| **樓層模型** | `com.example.htmlmud.dungeon.DungeonFloor` | 尺寸、地塊、初始點、危險度、exitRoomId |
| **位置模型** | `com.example.htmlmud.dungeon.DungeonPosition` | 玩家小隊動態網格座標、朝向、迷霧與已開寶箱集合 |
| **資料設定** | `src/main/resources/data/dungeons/mozhu_mines_b1f.json` | 墨竹礦坑一層 10x10 地牢地圖與圖塊配置 |
| **資料設定** | `src/main/resources/data/dungeons/taiyin_tomb_b1f.json` | 太陰古塚一層 10x10 地牢地圖與圖例宣告 |

---

## 🚀 七、未來架構演進規劃 (Roadmap)【待深入討論／長線演進藍圖】

1. **立體機關地貌機制**：引入單向冰面滑行、旋轉機關石盤與傳送玄陣，深化走格子博弈。
2. **地塊事件策略化工廠**：抽象為 `TileEventHandler` 策略介面，杜絕條件分支膨脹。
3. **迷霧與寶箱 BitSet 壓縮**：將二維布林陣列升級為 `BitSet`，進一步最佳化通訊體積與存檔讀寫效率。
4. **程序化地牢生成器 (PCG)**：引入 BSP 二元空間分割演算法支援無盡地下城。
