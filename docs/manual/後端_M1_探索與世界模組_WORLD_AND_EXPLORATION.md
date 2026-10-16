# 🌲 後端 M1: 探索與世界模組深度手冊 (World & Exploration Manual)

> **模組編號**：M1  
> **系統類型**：後端核心業務模組  
> **核心定位**：管理城鎮與野外地圖拓撲網絡、房間生命週期、生靈巡邏活動、地面遺落物，以及世界心跳輪詢機制。

---

## 🏛️ 一、模組職責與架構全覽

探索與世界模組（M1）負責構建修仙世界的宏觀地理網絡，為玩家提供「城鎮 MUD 模式」下的沉浸式探索體驗：

```mermaid
graph TD
    Pulse["WorldPulse 世界心跳 (3秒/Tick)"] -->|Tick| RoomMgr["WorldManager 房間中樞"]
    RoomMgr --> Room1["RoomActor: 桃源村客棧"]
    RoomMgr --> Room2["RoomActor: 礦坑入口"]
    
    Player["Player Actor (玩家)"] -->|move north| MoveService["RoomMovementService"]
    MoveService -->|leave(player)| Room1
    MoveService -->|enter(player)| Room2
    MoveService -->|廣播最新城鎮狀態| Broadcast["GameStateBroadcastService"]
    
    Room2 -->|地面遺落物非同步寫入| RoomPersist["RoomPersistenceService"]
```

### 核心職責劃分
1. **房間拓撲維護**：管理世界所有房間（Room）的空間關係、進出出口（Exits）與方位朝向。
2. **生靈活動調度**：追蹤房間內常駐的 NPC（店小二、長老、鐵匠）、可招募夥伴與遊蕩野怪。
3. **小隊協同移動**：處理玩家率領 5 人小隊在城鎮與野外地圖間的平滑位移。
4. **地面物品持久化**：管理丟棄或掉落在房間地面的法寶丹藥，確保斷電或重啟不遺失。
5. **世界心跳推進**：定期觸發野怪刷新與環境動態事件。

---

## 🗺️ 二、房間拓撲網絡與移動機制 (Room Movement)

### 1. 房間拓撲定義 (`rooms.json`)
每個房間具備全域唯一識別碼（如 `newbie_village:inn`），並透過 `exits` 宣告連通關係：

```json
{
  "id": "inn",
  "name": "客棧大堂",
  "zoneId": "newbie_village",
  "description": "古樸溫暖的客棧正廳，空氣中飄散著靈米粥與淡雅茶香...",
  "exits": {
    "south": { "targetRoomId": "village_square", "name": "村落廣場" },
    "north": { "targetRoomId": "newbie_village:kitchen", "name": "客棧後廚" }
  }
}
```

### 2. 移動狀態機流水線 (`RoomMovementService.java`)
當玩家輸入 `north` / `south` / `east` / `west` 等方向移動指令時：
1. **方向與出口合法性檢驗**：向當前房間範本查詢該方向是否存在有效出口。若無出路即時回傳「此處無路可走」。
2. **目標房間載入**：向 `WorldManager.getRoomActor(targetRoomId)` 請求目標房間 Actor 實例。
3. **離場與進場事件派發**：
   - 呼叫 `currentRoom.leave(player, dir)`，解除當前房間之駐留監聽與足跡錨點。
   - 呼叫 `targetRoom.enter(player, dir.opposite())`，觸發新房間生靈（NPC、巡邏怪）之警覺與環境敘事日誌（如「你率領小隊推門進入客棧正廳...」）。
4. **五人小隊協同空間位移**：5 人戰術小隊作為不可分割的整體冒險單元，位移時同步更新所有小隊成員之房間空間座標與環境光照。
5. **全景狀態推送**：呼叫 `GameStateBroadcastService.broadcastState(player)`，即時推送最新城鎮數據（`TownViewDto`）給前端進行介面重繪。

---

## 💓 三、世界心跳與野怪刷新 (WorldPulse)

`WorldPulse` 作為伺服端核心定時器（預設每 3 秒觸發一次 Tick）：
- **野怪活動與動態刷新**：檢查各地圖房間內的野怪存活數量。若低於範本設定之 `spawnCount` 且刷新冷卻就緒，自動從 `mobs.json` 重新孕育生靈。
- **生靈 AI 驅動**：觸發具備 `AggressiveBehavior`（主動攻擊型）之魔物進行視野探測，若房內存在修士且無仇恨保護，自動拉入戰鬥。

---

## 📦 四、地面遺落物與持久化 (Room Persistence)

玩家丟棄（`drop`）或怪物死亡爆出的靈物會暫留於房間地面：
- **`Room.getItems()`**：記憶體中即時維護當前房間的實體道具清單。
- **異步批次寫入 (`RoomPersistenceService`)**：當地面物品變動時，非同步排入寫入佇列，保證伺服器重啟時能完整還原各地面寶物，杜絕刷物或吞物漏洞。

---

## 🔗 五、代表類別與資料設定檔

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **主要服務** | `com.example.htmlmud.domain.service.RoomService` | 房間查詢與生命週期 |
| **移動服務** | `com.example.htmlmud.domain.service.RoomMovementService` | 出口檢驗、小隊位移與進出場廣播 |
| **世界中樞** | `com.example.htmlmud.domain.world.WorldManager` | 房間 Actor 實例池管理 |
| **世界心跳** | `com.example.htmlmud.domain.world.WorldPulse` | 定時怪物刷新與生靈心跳 |
| **資料設定** | `src/main/resources/data/zones/*/rooms.json` | 房間拓撲、出口與描述 |
| **資料設定** | `src/main/resources/data/zones/*/mobs.json` | 區域野怪與 NPC 實例配置 |

---

## 🚀 六、未來架構演進與現代化擴充規劃 (Future Roadmap & Scalability)

為使探索與世界模組邁向現代頂尖 RPG 規格，本模組確立以下四大演進方向：

### 1. 🎯 現代遊戲機制深化 (Modern Gameplay & Depth)
- **十二時辰動態天候與五行天象**：引入十二時辰流轉（子丑寅卯...）與動態天氣（暴雨、雷暴、毒瘴、大霧）。子夜陰煞加重，魔物攻擊力提升但掉落珍稀煞石；雷雨天全場雷法傷害提高 30%，大幅提升世界的動態生命力。
- **NPC 日夜作息行程表 (Living NPC Schedule)**：NPC 不再死板站樁，根據時辰在客棧、店鋪與洞府之間移動，特定時辰觸發深夜神秘黑市商人，營造生動市井生態。

### 2. 🛡️ 穩定性與狀態防禦 (Robustness & Resilience)
- **閒置房間 Actor 休眠回收機制 (Idle Eviction & Wakeup)**：目前所有房間載入後常駐記憶體。未來引入基於 LRU 的房間 Actor 生命週期池，當房間超過 10 分鐘無玩家且無動態戰鬥時自動鈍化休眠，玩家臨近時瞬間喚醒，記憶體開銷降低 70% 以上。
- **房間廣播隔離保護 (Broadcast Boundary Protection)**：限制房間內廣播文字的吞吐佇列上限，當某房間發生大量刷屏或密集事件時自動合流抑制，防範單一房間拖垮 WebSocket 頻寬。

### 3. ⚡ 效能與併發調優 (Performance & Concurrency)
- **拓撲路徑快取與 A* 導航引擎**：城鎮巡邏 NPC 與跟隨夥伴移動引入拓撲圖 A* 快取演算法，避免高頻 WorldPulse 輪詢時重複計算圖走訪。
- **地面物品寫入 WAL 緩衝**：將 `RoomPersistenceService` 升級為環形非同步緩衝佇列（Disruptor / RingBuffer），高頻掉落與拾取時僅操作記憶體指標，定期批次排空，磁碟 I/O 零阻塞。

### 4. 🔌 可擴充性與模組化 (Extensibility & Modding)
- **世界動態事件匯流排 (World Event Trigger System)**：透過 JSON 配置區域動態突發事件（如「墨竹山獸潮暴動」、「萬年靈藥出世」），系統可動態為整片區域的房間附加環境 Debuff 或刷新特殊首領，無需更動任何 Java 代碼。
