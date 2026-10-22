# ⚡ 後端 M9: 單機 DRPG 狀態管線與事件協定模組深度手冊 (State Pipeline & Events Manual)

> **模組編號**：M9  
> **系統類型**：後端核心系統模組  
> **核心定位**：**專為純單機第一人稱 Hack-and-Slash DRPG 設計的本機低延遲通訊與狀態管線**。  
> **設計原則**：**徹底除役 MUD 指令解析器與多實體 Actor 信箱，以「結構化 DRPG 動作協定 (Action Protocol)」、「單一全景狀態推送契約 (`DrpgStateDto`)」與「輕量虛擬執行緒時間線迴圈」為三大支柱**。

---

## 🏛️ 一、架構背景與設計哲學

本專案已全面確立為 **純單機單體 DRPG (Single-Player Monolith DRPG)**，因此通信與事件調度系統的核心目標是：
1. **零文字命令行負擔**：不再解析 `"look"`, `"step n"`, `"kill rat"` 等 MUD 字符串，完全由前端按鈕、鍵盤映射（W/A/S/D）與選單發送**結構化動作 JSON**。
2. **極致響應的單機狀態管線 (State Pipeline)**：任何遊戲邏輯變更（移動踩格、施法命中、穿換裝備、營地休息）完成後，後端透過單一全景契約 `DrpgStateDto` 統一推送，前端僅作單向視圖投影。
3. **輕量虛擬執行緒時間線 (Virtual Thread Timeline)**：利用 Java 21/25 Virtual Threads 驅動戰鬥 500ms Tick 迴圈 (`DrpgCombatLoop`)，代碼直觀、零鎖競爭、零回調地獄，天然免除多玩家併發複雜度。

---

## 🔄 二、本機 WebSocket 通訊流程

前後端透過極簡的本機全雙工 WebSocket（預設端點：`ws://localhost:8080/ws`）互動：

```text
[前端 視圖層 (websocket.js)]                                      [後端 業務引擎 (DrpgActionHandler)]
          │                                                                      │
          │ ────────── 1. 本機握手連線 (Local Handshake) ────────────────────────> │
          │ <───────── 2. 連線就緒 (Connection Ready) ─────────────────────────── │
          │                                                                      │
          │ ────────── 3. DRPG 結構化動作事件 (如 MOVE / CAST_SKILL) ─────────────> │
          │                                                                      │
          │ <───────── 4. 情境敘事文字日誌 (Narrative Text Event) ─────────────── │
          │ <───────── 5. 戰鬥打擊事件 (BattleEventsDto - 飄字/震屏/打斷反饋) ──── │
          │ <───────── 6. 單一全景狀態快照 (DrpgStateDto - 地牢/小隊/戰況) ────── │
          │                                                                      │
          │ ────────── 7. 本機心跳保活 Ping (每 30 秒) ──────────────────────────> │
```

---

## 🎮 三、DRPG 結構化動作協定規範 (Action Protocol)

前端向後端派發的請求一律為標準 JSON 物件，定義於 `com.example.htmlmud.interfaces.dto.DrpgClientActionDto`：

### 1. 地牢走格移動動作 (`MOVE`)
```json
{
  "action": "MOVE",
  "direction": "FORWARD" // 可為 "FORWARD" (前進), "BACKWARD" (後退), "TURN_LEFT" (左轉), "TURN_RIGHT" (右轉)
}
```

### 2. 戰鬥戰技施放動作 (`CAST_SKILL`)
```json
{
  "action": "CAST_SKILL",
  "actorId": "char-1",
  "skillId": "skill_strike_1",
  "targetId": "mob-001"
}
```

### 3. 陣眼終結技發動動作 (`FORMATION_FINISHER`)
```json
{
  "action": "FORMATION_FINISHER",
  "targetId": "all_enemies"
}
```

### 4. 營地選單設施交互 (`TOWN_FACILITY`)
```json
{
  "action": "TOWN_FACILITY",
  "facility": "INN", // 可為 "INN" (客棧調息), "BLACKSMITH" (鐵匠注靈), "SHOP" (集市補給), "DUNGEON_ENTRANCE" (啟程出征)
  "subAction": "REST"
}
```

### 5. 裝備穿戴與小隊管理 (`EQUIP` / `PARTY_FORMATION`)
```json
{
  "action": "EQUIP",
  "memberId": "char-1",
  "slot": "MAIN_HAND",
  "itemId": "item_black_alloy_blade"
}
```

---

## 📦 四、單一全景狀態契約：`DrpgStateDto`

後端透過 `GameStateBroadcastService` 統一組裝派發，嚴格對齊專案 **五大核心目標**：

```json
{
  "type": "DRPG_STATE",
  "viewMode": "DUNGEON", // "TOWN_HUB" (營地選單) 或 "DUNGEON" (地牢遠征)
  
  // 1. 地牢遠征視角 (當 viewMode == "DUNGEON" 時非空 - 世界觀介紹 & DRPG 走格)
  "dungeon": {
    "floorId": "black_vein_b1f",
    "floorName": "黑煞宗地底礦脈一層 (外神卵鞘偽地殼)",
    "x": 2, "y": 7,
    "direction": "NORTH", "directionArrow": "▲",
    "width": 10, "height": 10,
    "tiles": [ [ "#", ".", ... ] ],
    "visited": [ [ false, true, ... ] ],
    "openedChests": [ [ false, false, ... ] ],
    "torchLight": 85, // 火把光明度 (0~100)
    "torchStatus": "BRIGHT", // "BRIGHT" / "DIM" / "DARKNESS"
    "forwardInspection": "石壁摸起來如同堅韌的角質皮革，石縫正滲出暗紅黏稠組織..."
  },

  // 2. 五人小隊狀態 (始終存在 - 角色養成 & 裝備收集)
  "party": {
    "partyId": "party-main",
    "activeFormationId": "nine_palaces_eight_trigrams",
    "formationName": "九宮八卦陣",
    "resonancePoints": 3, // 陣法共鳴點數 (用於天外合擊)
    "agentChipLoad": 12, // 主角特工晶片加載進度 (12%...)
    "members": [
      {
        "id": "char-1",
        "name": "主角 (前千星特工)",
        "row": "FRONT",
        "hp": 150, "maxHp": 150,
        "mp": 90,  "maxMp": 90,
        "poise": 60, "maxPoise": 60,
        "level": 3,
        "realm": "煉氣三層",
        "potentialPoints": 5, // 待分配道基五維潛能點
        "skills": [ ... ],
        "equipment": { ... }
      }
    ],
    "inventory": {
      "capacity": 40, // 40格小隊公共行囊 (唯一真相源)
      "slots": [ ... ]
    }
  },

  // 3. 戰鬥擂台視角 (僅在戰鬥觸發時非空 - 戰鬥爽快 & 技能組合)
  "battle": {
    "inBattle": true,
    "crossComboStep": 2, // 動態連攜階數 (1:先導, 2:雙星合璧, 3:三才混元, 4:四象誅仙)
    "crossComboExpiresAt": 1774320000000,
    "enemies": [
      {
        "id": "mob-001",
        "name": "守墓道奴",
        "hp": 320, "maxHp": 800,
        "poise": 0, "maxPoise": 100,
        "isPoiseBroken": true, // 罡氣破防 (享受 1.5x 爆擊重創)
        "currentAction": "九幽聚煞引",
        "actionProgressPercent": 65,
        "isCasting": true,
        "isInterruptible": true, // 可反制打斷
        "buffs": [ { "id": "primer_soul_fracture", "type": "PRIMER" } ]
      }
    ]
  }
}
```

---

## ⚡ 五、輕量虛擬執行緒時間線調度器 (Virtual Thread Timeline)

在純單機單體架構下，後端不再為每個實體維護 Actor 信箱，而是收斂為單一清晰的時間線調度結構：

```mermaid
graph TD
    Client["前端 WebSocket (單一玩家連線)"] -->|動作事件 ActionDto| Controller["DrpgActionDispatcher"]
    Controller -->|同步修改狀態| Model["Party / Dungeon / Inventory (單一真相源)"]
    
    subgraph BattleTimeline ["戰鬥時間線 (僅在遭遇戰時啟動)"]
        Loop["DrpgCombatLoop (虛擬執行緒)"] -->|每 500ms Tick| CombatEngine["CombatRules / ComboResolver"]
        CombatEngine -->|解算削韌 / 連攜 / 反應| Model
    end

    Model -->|狀態變更通知| Broadcaster["GameStateBroadcastService"]
    Broadcaster -->|推送 DrpgStateDto| Client
```

- **極簡與高可靠**：沒有分佈式、沒有跨節點發信，所有狀態變更在單一 JVM 內完成，排查錯誤與編寫單元測試極其直觀。
- **隨開即停**：戰鬥觸發時啟動虛擬執行緒輪詢；戰鬥結束即刻優雅停止 (`running = false`)，釋放運算資源。

---

## 🔗 六、代表類別清單

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **動作分發器** | `com.example.htmlmud.interfaces.websocket.DrpgActionDispatcher` | 接收客戶端動作 JSON，分發至地牢、小隊或戰鬥服務 |
| **WebSocket 處理器** | `com.example.htmlmud.interfaces.websocket.DrpgWebSocketHandler` | 本機連線生命週期、心跳與文字日誌推送 |
| **狀態廣播器** | `com.example.htmlmud.application.service.GameStateBroadcastService` | 全景狀態組裝與 `DrpgStateDto` JSON 推送 |
| **戰鬥時間線迴圈** | `com.example.htmlmud.domain.dungeon.battle.DrpgCombatLoop` | 虛擬執行緒驅動的 500ms 半即時戰鬥結算 |
| **全景契約 DTO** | `com.example.htmlmud.interfaces.dto.DrpgStateDto` | 前後端全域資料交換唯一契約 |
| **客戶端動作 DTO** | `com.example.htmlmud.interfaces.dto.DrpgClientActionDto` | 前端按鈕與鍵盤觸發的動作協定契約 |
