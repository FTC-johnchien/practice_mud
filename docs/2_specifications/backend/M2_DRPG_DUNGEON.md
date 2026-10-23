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

---

## 🔦 二、前方視野探測與火把光明度機制 (Inspect Forward & Light Level)

### 1. 前方 3 步射線探測
當玩家邁步或原地轉向時，後端 `DungeonNavigator.inspectForward(floor, pos)` 會依據小隊當前朝向，沿視線方向向前投射 3 步射線：

```
[小隊站在 (x, y), 朝向 NORTH]
Step 1: (x, y - 1)  ──>  若遇 WALL 或 DOOR，立即阻斷射線
Step 2: (x, y - 2)
Step 3: (x, y - 3)
```

- **阻擋機制**：若第 1 步即為「玄岩石壁（WALL）」或「閉合石門（DOOR）」，視線將無法穿透至第 2、3 步。
- **視野文字**：組裝結構化視野資訊，提供前端 3D 視口渲染與輔助敘事日誌。

### 2. 光明度與火把探險資源 (`lightLevel`)
在暗黑克蘇魯地牢中，光芒是生與死的分界：
- **光明度數值**：預設上限 100 點。每前進或轉向 1 步，光明度衰減 1 點。
- **火把消耗**：玩家使用小隊行囊中的【火把 (Torch)】可瞬間恢復 50 點光明度（最高 100 點）。
- **光照臨界懲罰**：
  - $\text{Light} > 50$【明亮】：視野探測完整 3 步，常規遇敵率。
  - $20 < \text{Light} \le 50$【昏暗】：視野縮減至 2 步，魔物突襲率（Ambush Rate）增加 20%。
  - $\text{Light} \le 20$【幽冥死寂】：視野僅餘 1 格，遇敵率提升 50%，且小隊每走 5 步承受 1 點精神侵蝕（SAN 扣減）。

> 📌 **【精神防護罩（SAN 護盾）定位架構裁決】**：  
> 地牢黑暗中扣除 SAN 數值純屬「地牢探索資源管理與火把消耗博弈」，SAN 歸零時僅附加【精神萎靡】(-15% 傷害) 狀態，絕無隨機發瘋、隊員自殘或隨機背叛。請勿將此機制與已取消的「魔化 (Aberration)」混淆。

---

## 🎲 三、動態遇敵率演算法 (Danger Rate)

地牢探索處處危機，每走一步均會觸發危險度檢定：
1. **基礎危險度**：來自樓層配置的 `dangerRate`（如墨竹礦坑 $0.15$）。
2. **步數累積加權**：若長時間未遇敵，累計危險權重逐步爬升。
3. **戰鬥觸發**：當隨機擲骰落入危險區間時，立即中斷常規探索，由 `DrpgBattleService` 接管並啟動半即時戰鬥。

---

## 🚪 四、地牢撤離、全軍覆沒與生存限制 (Dungeon Extraction & Wipe Settlement)

在地牢中隨時可能遭遇強大魔物滅隊。專案建立了嚴密的撤離保護與戰敗懲罰機制：

```mermaid
graph TD
    subgraph Extraction ["正常撤離 (Extraction)"]
        Step["踩在具有 STAIRS_UP (<) 的安全撤離點"] --> Action["ActionCommand: EXTRACT"]
        Action --> Query["讀取 floor.getExitRoomId() (如 hub_ore_mine_shelter)"]
        Query --> SaveSuccess["帶回全部煞晶與戰利品，AutoSave 觸發快照"]
    end

    subgraph DefeatWipe ["全軍覆沒 (Party Wipe)"]
        Wipe["戰鬥中 5 人小隊全員氣血歸零"] --> WipeAction["DrpgBattleService 判定滅隊結算"]
        WipeAction --> DropLoot["遺失本次遠征尚未帶出之隨身收穫"]
        DropLoot --> Revive["全隊於營地虛弱復甦 (30% HP，重置 inDungeon)"]
        Revive --> SaveWipe["AutoSave 觸發安全復甦快照"]
    end
```

### 1. 安全撤離條件 (Extraction)
- 玩家必須移動至具有 `STAIRS_UP`（`<`）特性的地塊（入口石階或遠古傳送陣）。
- 將玩家 `inDungeon` 重置為 `false`，安全切換回選單式營地中樞視圖，帶回所有戰利品，並觸發自動快照存檔。

### 2. 全軍覆沒戰敗結算 (Party Wipe & Defeat)
- **觸發條件**：小隊 5 名成員氣血（HP）全部歸零。
- **沉沒成本懲罰**：嚴格遵循 Level 1 規範，小隊遺失本次地牢遠征所獲得的全部隨身戰利品（外神煞晶、高階材料與未鑑定位元），全隊被神識傳送陣遣返營地虛弱復甦（生命氣血回復 30%）。
- **狀態收斂**：`inDungeon` 重置為 `false`，安全保存存檔，杜絕卡死在深淵死亡循環。

### 3. 地牢禁止任意存檔與脫離守則 (In-Dungeon Save Restriction)
- 嚴格對齊 Level 1 沉沒成本博弈：**地牢中禁止隨時隨地手動存檔或任意傳送脫離**。
- 唯有抵達安全撤離石階（`<`）執行 `EXTRACT`、戰勝首領結算、或切換樓層時，系統方進行安全自動存檔 (`slot_auto.json`)。

---

## ⚙️ 五、地塊機關檢定與互動機制 (Traps & Interactive Checks)

地牢地塊具備多樣化的環境檢定博弈：
1. **暗弩機關陷阱 (`TRAP`)**：踩格觸發，執行身法（AGI）檢定，檢定成功靈巧避開，失敗受到穿刺傷害並附加中毒 DoT。
2. **血肉暗門 / 封印石門 (`DOOR`)**：執行根骨（STR）檢定或消耗工坊破拆工具破拆，成功解鎖隱藏道藏。
3. **靈泉地窟 (`SPRING` / `EVENT`)**：互動飲用古修靈泉，瞬間回復全隊 100% 氣血 (HP)、真元 (MP) 與精神護盾 (SAN)。

---

## 🔗 五、代表類別與資料設定檔

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **地牢中樞** | `com.example.htmlmud.domain.dungeon.DungeonManager` | 地牢狀態持有與樓層切換 |
| **導航演算** | `com.example.htmlmud.domain.dungeon.DungeonNavigator` | 網格位移、朝向旋轉與前方 3 步探測 |
| **地圖載入** | `com.example.htmlmud.domain.dungeon.DungeonFloorLoader` | 讀取 JSON 解析二維字元矩陣與圖例 |
| **樓層模型** | `com.example.htmlmud.domain.dungeon.DungeonFloor` | 尺寸、地塊、初始點、危險度、exitRoomId |
| **動作分發** | `com.example.htmlmud.domain.dungeon.DrpgActionDispatcher` | 處理 MOVE / TURN / INTERACT / EXTRACT 結構化動作 |
| **資料設定** | `src/main/resources/data/dungeons/mozhu_mines_b1f.json` | 墨竹礦坑一層 10x10 地牢地圖與圖塊配置 |
| **資料設定** | `src/main/resources/data/dungeons/taiyin_tomb_b1f.json` | 太陰古塚一層 10x10 地牢地圖與圖例宣告 |

---

## 🚀 六、未來架構演進與現代化擴充規劃 (Future Roadmap & Scalability)

為使 DRPG 網格地牢模組向經典《世界樹迷宮》與現代克系地牢體驗看齊，本模組確立以下四大演進方向：

### 1. 🎯 現代遊戲機制深化 (Modern Gameplay & Depth)
- **立體機關地貌機制 (Advanced Dungeon Mechanics)**：引入「單向冰面滑行（踩格強制沿滑行方向滑動直至撞牆）」、「旋轉機關石盤（改變當前小隊朝向）」、「古修傳送玄陣」與「陷落深坑落穴（掉入下層未知區域）」，打破單純平面的走格子侷限。
- **動態環境陷阱與機關博弈**：地刺、落石、毒氣噴湧與暗格開關，玩家可使用神識感知提前識破陷阱，或透過特工神經晶片遠程拆解。

### 2. 🛡️ 穩定性與狀態防禦 (Robustness & Resilience)
- **地塊事件策略化工廠 (TileEventStrategyFactory)**：目前地塊觸發零散分佈於導航器中。未來全面抽象為 `TileEventHandler` 介面（`DoorEventHandler`, `ChestEventHandler`, `TrapEventHandler`, `StairsEventHandler`），事件新增完全閉合，杜絕條件分支膨脹。
- **邊界死鎖與穿牆嚴密防護斷言**：在 `DungeonNavigator` 增加越界雙重斷言，任何未配置的未知字元自動當作堅硬玄岩石壁（WALL）處理，嚴防玩家座標穿出地圖邊界造成空指針崩潰。

### 3. ⚡ 效能與記憶體最佳化 (Performance & Concurrency)
- **迷霧與寶箱矩陣 BitSet 位元壓縮**：將 `boolean[y][x]` 二維物件陣列升級為 `java.util.BitSet`，10x10 網格僅需 100 bits（不到 13 位元組），序列化傳輸體積與記憶體佔用驟降 87.5%，存檔讀寫極速化。
- **射線投射探測快取 (Raycast Spatial Cache)**：靜態石壁地貌之 3 步探測文字引入空間雜湊快取，相同座標與朝向直接自快取回傳，避免頻繁的字串拼接與物件建構。

### 4. 🔌 可擴充性與模組化 (Extensibility & Modding)
- **程序化隨機地牢生成器 (Procedural Dungeon Generator / PCG)**：為「無盡太陰幽冥深淵」模式引入 BSP（二元空間分割）或隨機漫步演算法，遊戲可依據世界種子動態生成符合 JSON 結構標準的無限層地下城，實現高重玩度的 Roguelike 修仙地牢。
