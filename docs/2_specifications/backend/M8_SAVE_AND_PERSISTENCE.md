# 💾 後端 M8: 存檔與歷史快照模組深度手冊 (Save & Persistence Manual)

> **模組編號**：M8  
> **系統類型**：後端核心業務模組  
> **核心定位**：確立以 SaveData JSON 為唯一單一真相源（Single Source of Truth）、五大獨立存檔槽位與自動存檔、schemaVersion 四階版本遷移演算法，以及 .bak 壞檔手動還原防禦機制。

---

## 🎯 一、單一真相源哲學 (Single Source of Truth)

本專案在單機沉浸式 DRPG 的演進中確立了 **ADR-01 架構決策**：
- **痛點**：關係型資料庫在面對高頻欄位異動（如新增一種抗性、裝備隨機詞綴、小隊站位）時需維護繁瑣 DDL Migration。
- **解法**：全面收斂至 **`SaveData` 檔案式 JSON 序列化** 作為唯一單一真相源（Single Source of Truth）。
- **效益**：
  1. 一鍵匯出 / 匯入存檔，完全不依賴外部資料庫服務。
  2. 存檔結構具備原子性（Atomic Save via `.tmp`），避免半寫入壞檔。
  3. 與現代單機 RPG 存檔管理機制 100% 契合。

---

## 📂 二、存檔結構與槽位管理

存檔目錄位於專案根目錄的 `saves/`：

```
saves/
├── slot_1.json                         # 槽位 1
├── slot_1.json.bak                     # 槽位 1 自動備份 (壞檔防護)
├── slot_2.json                         # 槽位 2
├── slot_3.json                         # 槽位 3
├── slot_4.json                         # 槽位 4
├── slot_5.json                         # 槽位 5
├── autosave.json                       # 系統自動存檔 (AutoSave / slot 0)
└── autosave.json.bak                   # 自動存檔備份
```

### `SaveData` JSON 核心結構
```json
{
  "schemaVersion": 3,
  "slotId": 1,
  "title": "墨竹礦坑深處 ‧ 練氣九層",
  "playerId": "char-12345",
  "protagonistName": "長生",
  "inDungeon": true,
  "currentRoomId": "newbie_village:inn",
  "chipRate": 12,
  "floorId": "mozhu_mines_b1f",
  "floorX": 1,
  "floorY": 8,
  "floorFacing": "NORTH",
  "visitedTiles": [ [ true, true, false ] ],
  "openedChests": [ "3,5" ],
  "party": {
    "id": "party-hero",
    "partyName": "墨竹勘探隊",
    "equippedFormation": { "id": "formation_four_symbols", "name": "四象辟邪陣" },
    "inventory": { "capacity": 40, "slots": [] },
    "members": []
  },
  "savedAt": "2026-11-02 10:00:00",
  "playtimeSeconds": 120
}
```

### 存檔安全鎖與覆滅結算協定 (Save Safeguards & Defeat Protocols)
1. **地牢手動存檔限制 (In-Dungeon Save Lock)**：
   - 地牢中手動存檔（`save <slot>`）僅允許於 **`STAIRS_UP` (`<`)、`STAIRS_DOWN` (`>`) 或 `EVENT` (`!`)** 三類地塊執行。其餘常規地塊由系統提示無法存檔。
   - 營地中樞（`mode == TOWN`）則隨時允許自由手動存檔。
2. **小隊覆滅結算與自動存檔 (Defeat Wipe & AutoSave)**：
   - 當小隊全員氣血歸零戰敗時，觸發結算：全員扣減 **`-20 SAN`**、氣血回復至 30% MaxHP，**保留行囊內所有戰利品與資產**（單機友善，不清空戰利品）。
   - 小隊安全傳送回 `floor.exitRoomId`（預設營地客棧 `newbie_village:inn`）。
   - 結算完成後，系統立即觸發自動存檔寫入 `autosave.json`（`slot 0`）。

---

## 🔄 三、版本治理與遷移機制 (Schema Versioning)

為確保舊存檔相容，`SaveMigrationPipeline` 實作了四階版本遷移鏈：

```
讀取 SaveData JSON
       │
       ▼
檢查 schemaVersion
       │
       ├─► 若為 v0 (無版號): 遷移至 v1 (補齊 schemaVersion=1、openedChests、playtimeSeconds)
       │
       ├─► 若為 v1: 遷移至 v2 (補齊 personalInventory 空陣列)
       │
       ├─► 若為 v2: 遷移至 v3 (將 personalInventory 道具無損合流至 party.inventory.slots 並移除 personalInventory)
       │
       ├─► 若為 v3 (當前最新): 直接安全載入
       │
       ▼
寫回備份並完成反序列化
```

- **原子寫入與備份**：`SaveGameService` 在覆寫存檔前自動備份為 `.bak`，並透過 `.tmp` 暫存檔進行原子替換寫入。
- **壞檔修復機制**：若讀取存檔時檔案損壞（如 JSON 截斷），反序列化失敗會拋出 `SaveCorruptedException`（標記 `corrupted = true`），前端提示玩家執行指令 **`save restore <slot>`** 從 `.bak` 備份無損還原。

---

## 🗄️ 四、歷史 H2 雙持久化實體收斂進度

1. **`CharacterEntity.inventory` 正式標記為 `@Deprecated`**。
2. 業務層（`DropCommand`、`GetCommand`、`ShopCommand`）全面遷移至以 `PartyInventory` 與 `SaveGameService` 為核心。
3. H2 實體僅保留歷史向下相容用途。

---

## 🔗 五、代表類別與存檔路徑

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **存檔服務** | `com.example.htmlmud.save.SaveGameService` | 槽位讀寫、備份、原子替換與壞檔還原 |
| **遷移管線** | `com.example.htmlmud.save.SaveMigrationPipeline` | v0 $\to$ v1 $\to$ v2 $\to$ v3 平滑遷移管線 |
| **存檔真相源** | `com.example.htmlmud.save.SaveData` | 小隊、40格背包、進度聚合根 DTO |
| **存檔槽位模型** | `com.example.htmlmud.save.SaveSlotDto` | 存檔槽位元資料與預覽傳輸契約 |
| **存檔存放目錄** | `saves/slot_*.json, saves/autosave.json` | 本地原子性 JSON 存檔目錄 |

---

## 🚀 六、未來架構演進規劃 (Roadmap)【待深入討論／長線演進藍圖】

1. **太陰萬劫·鐵人一命模式**：硬派克系模式，戰敗存檔鎖定為殘魂深淵地宮。
2. **雲端同步與跨端相容架構**：提供 WebDAV / REST 存檔匯出協議。
3. **Zstandard 串流高壓儲存**：大型存檔引入 Zstandard 壓縮。
