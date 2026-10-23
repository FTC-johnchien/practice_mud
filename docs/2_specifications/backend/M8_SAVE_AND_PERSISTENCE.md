# 💾 後端 M8: 存檔與歷史快照模組深度手冊 (Save & Persistence Manual)

> **模組編號**：M8  
> **系統類型**：後端核心業務模組  
> **核心定位**：確立以 SaveData JSON 為唯一單一真相源（Single Source of Truth）、五大獨立存檔槽位與自動存檔、schemaVersion 版本遷移演算法，以及 .bak 壞檔自動回滾防禦機制。

---

## 🎯 一、單一真相源哲學 (Single Source of Truth)

在傳統線上 MUD 架構中，通常使用關係型資料庫（如 MySQL / PostgreSQL / H2）存放十幾張關聯表（`characters`, `items`, `skills`, `player_inventories` 等）。

然而，本專案在單機沉浸式 DRPG 的演進中確立了 **ADR-01 架構決策**：
- **痛點**：關係型資料庫在面對高頻欄位異動（如新增一種抗性、新增一個裝備槽位、小隊站位）時，需要維護繁瑣的 DDL Migration，極易因 Lazy Loading 或 N+1 查詢引發併發異常。
- **解法**：全面收斂至 **`SaveData` 檔案式 JSON 序列化** 作為唯一單一真相源（Single Source of Truth）。
- **效益**：
  1. 一鍵匯出 / 匯入存檔，完全不依賴外部資料庫服務。
  2. 存檔結構天然具備原子性（Atomic Save），避免半寫入壞檔。
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
└── slot_auto.json                      # 系統自動存檔 (AutoSave)
```

### `SaveData` JSON 核心結構
```json
{
  "schemaVersion": 3,
  "saveTimestamp": 1729000000000,
  "saveSlot": "slot_1",
  "saveName": "墨竹礦坑深處 ‧ 練氣九層",
  "player": {
    "id": "char-12345",
    "name": "李長生",
    "currentRoomId": "mozhu_mines:mine_entrance",
    "inDungeon": true,
    "stats": { ... },
    "learnedSkills": { ... }
  },
  "party": {
    "partyId": "party-李長生",
    "activeFormationId": "nine_palaces_eight_trigrams",
    "members": [ ... ],
    "sharedInventory": {
      "capacity": 40,
      "slots": [ ... ]
    }
  },
  "dungeonProgress": {
    "currentFloorId": "mozhu_mines_b1f",
    "coord": { "x": 1, "y": 8 },
    "facing": "NORTH",
    "visitedMatrix": [ ... ],
    "openedChestsMatrix": [ ... ]
  }
}
```

### 存檔安全鎖與覆滅結算協定 (Save Safeguards & Defeat Protocols)

依據 Level 1《GAMEPLAY_CORE_LOOPS.md §5.4》，存檔系統嚴格落實單機 DRPG 生存張力與防作弊保護：

1. **地牢手動存檔限制 (In-Dungeon Save Lock)**：
   - 當小隊身處地牢深淵時（`inDungeon = true` 且當前座標非撤離點 `E` 或石階），前端存檔按鈕自動鎖定置灰，後端 ActionHandler 阻斷手動儲存請求。
   - 玩家唯有在安全營地（`viewMode == TOWN`）或在地牢抵達撤離點石階時，方可進行手動存檔。
2. **小隊覆滅結算與強制自動存檔 (Defeat Wipe & AutoSave Protocol)**：
   - 當小隊在戰鬥或地牢中全員 HP 歸零覆滅時，觸發滅團結算：清空本次探索暫存未鑑定掉落物（已裝備物品與出發前行囊物資保留）。
   - 小隊狀態強制重置回安全營地（`viewMode = TOWN`），全員以 30% 氣血虛弱復甦。
   - 結算完成後，系統立即觸發原子性自動存檔至 `slot_auto.json`，杜絕玩家藉由強退遊戲逃避死亡懲罰。

---

## 🔄 三、版本治理與遷移機制 (Schema Versioning)

為確保遊戲在後續更新版本時，舊存檔依然能無痛相容載入，`SaveGameService` 實作了嚴格的版號遷移管線：

```
讀取 SaveData JSON
       │
       ▼
檢查 schemaVersion
       │
       ├─► 若為 v1: 執行 migrateV1toV2() 補齊九大抗性矩陣、罡氣韌性上限與裝備槽位
       │
       ├─► 若為 v2: 執行 migrateV2toV3()，將舊版個人隨身道具自動合流入小隊公共行囊
       │
       ├─► 若為 v3 (當前最新): 直接安全載入
       │
       ▼
寫回備份並完成反序列化
```

- **壞檔防禦（Fallback to .bak）**：
  若寫入存檔時遭遇異常中斷（如斷電或崩潰），系統在讀取失敗時會自動嘗試讀取 `.bak` 備份檔，確保玩家數十小時的修仙心血不會瞬間化為烏有。

---

## 🗄️ 四、歷史 H2 雙持久化實體收斂進度

在專案早期曾引入 JPA / H2（`CharacterEntity`, `RoomStateEntity`）。為消除維護雙套持久化架構的認知負擔：
1. **`CharacterEntity.inventory` 正式標記為 `@Deprecated`**（2026-10-15 完成）。
2. 業務層（`DropCommand`、`GetCommand`、`ShopCommand`、`StashCommand`）全面遷移至以 `PartyInventory` 與 `SaveGameService` 為核心。
3. H2 實體僅保留帳號驗證與歷史向下相容用途，後續版本將逐步平滑除役。

---

## 🔗 五、代表類別與存檔路徑

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **存檔服務** | `com.example.htmlmud.domain.save.service.SaveGameService` | 槽位讀寫、備份回滾與版本遷移 |
| **遷移管線** | `com.example.htmlmud.domain.save.migration.SaveMigrationPipeline` | v1 $\to$ v2 $\to$ v3 平滑遷移 |
| **存檔真相源** | `com.example.htmlmud.domain.save.model.SaveData` | 小隊、40格背包、進度聚合根 DTO |
| **房間持久化** | `com.example.htmlmud.domain.service.RoomPersistenceService` | 地面遺留物非同步寫入 |
| **存檔存放目錄** | `saves/slot_*.json` | 本地原子性 JSON 存檔目錄 |

---

## 🚀 六、未來架構演進與現代化擴充規劃 (Future Roadmap & Scalability)

為使以 `SaveData` 為核心的單機存檔系統具備商用級安全度與雲端互通性，確立以下四大演進方向：

### 1. 🎯 現代遊戲機制深化 (Modern Gameplay & Depth)
- **太陰萬劫·鐵人一命模式 (Permadeath Iron Man Mode)**：
  提供硬派暗黑克系模式。在此模式下，存檔槽位在戰敗全滅時自動鎖定為「殘魂古塚」，遊戲為該存檔生成一座特殊的深淵地宮，玩家開闢新道途時可在深淵底層討伐前世發狂的自己，奪回前世本命法寶！
- **雲端同步與跨端相容架構 (Cloud Sync & Portability)**：
  提供 WebDAV / REST 存檔匯出協議，支援玩家將 `SaveData` JSON 一鍵無縫上傳或同步至私人雲端儲存，輕鬆實現在 PC 與行動端之間的跨設備存檔遷移。

### 2. 🛡️ 穩定性與狀態防禦 (Robustness & Resilience)
- **WAL 預寫日誌與原子替換防禦 (Atomic Temp File Replacement)**：
  存檔寫入全面升級為三步原子操作：
  1. 完整資料寫入 `slot_X.json.tmp`。
  2. 使用 OS 級原子重命名（Atomic Move）替換現存檔，原檔案更名為 `slot_X.json.bak`。
  3. 寫入完畢校驗 SHA-256 摘要。
  即使在寫入的微秒級瞬間遭遇系統斷電或死機，也不會產生 0 位元組的半損壞檔案！
- **自動快照循環滾動 (Rolling Snapshots)**：自動存檔維護最近 3 個歷史檢查點（Checkpoints），給予玩家在遭遇致命錯誤時向前回滾的救贖機會。

### 3. ⚡ 效能與儲存壓縮 (Performance & Concurrency)
- **Zstandard 串流高壓儲存 (Zstd Stream Compression)**：
  大型存檔在小隊擁有多件隨機詞綴法寶與廣闊迷宮探索進度時體積上升。引入 Zstandard 壓縮，體積壓縮超過 90% 且解壓速度高達數百 MB/s，大幅減少儲存空間佔用。

### 4. 🔌 可擴充性與模組化 (Extensibility & Modding)
- **CI 存檔相容性回歸測試矩陣 (Automated Migration Test Matrix)**：
  在自動化測試套件中納入自 v1 至最新版的所有歷史存檔種子，在持續集成（CI）階段保證任何代碼重構均能無痛自動遷移舊存檔，承諾「存檔永不損壞、版本永遠相容」。
