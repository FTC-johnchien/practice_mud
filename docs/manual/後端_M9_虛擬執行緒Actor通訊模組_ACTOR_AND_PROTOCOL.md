# 🧵 後端 M9: 虛擬執行緒 Actor 通訊模組深度手冊 (Actor & Protocol Manual)

> **模組編號**：M9  
> **系統類型**：後端核心業務模組  
> **核心定位**：管理 Java 21/25 Virtual Threads 極致輕量併發、VirtualActor 訊息信箱單一擁有權無鎖模型、MudContext 執行緒安全上下文、WebSocket 全雙工連線生命週期，以及 DrpgStateDto 單一全景狀態契約。

---

## ⚡ 一、為什麼選擇 Java 21+ 虛擬執行緒？

傳統 Java 多執行緒（Platform Threads）與作業系統核心執行緒 1:1 綁定，每個執行緒需消耗 1MB 記憶體，面對大量並行連線或高頻定時器時成本極高。

本專案採用 **Java 21/25 Virtual Threads（Project Loom）**：
- **極致輕量**：單個虛擬執行緒僅佔用幾百位元組記憶體，可在單機 JVM 內輕鬆創建數萬個獨立執行緒。
- **簡化併發編程**：完全告別繁瑣的 Reactive / CompletableFuture 回調地獄，以直觀的同步代碼風格撰寫高效的非同步業務！

---

## 🎭 二、VirtualActor 模型與訊息信箱

專案在 `com.example.htmlmud.domain.actor.core.VirtualActor` 中實作了輕量級 Actor 模型：

```mermaid
graph LR
    Command["玩家輸入 / 世界定時器"] -->|send(Message)| Mailbox["LinkedBlockingQueue 信箱"]
    subgraph VirtualActor ["VirtualActor (執行於獨立虛擬執行緒)"]
        Mailbox --> Worker["while(running) { take() & process() }"]
        Worker --> State["Actor 私有狀態 (如 Player.stats)"]
    end
```

### 1. 單一擁有權原則 (Single Ownership)
- 冒險主角（`Player`）與活躍房間（`RoomActor`）皆繼承或持有 `VirtualActor`。
- **無鎖化設計**：外部執行緒不得直接修改 `Player` 內部的狀態變數，而是透過發送訊息進入其專屬信箱，由該 Actor 的專屬虛擬執行緒循序處理，天然免除鎖競爭（Race Condition）與死鎖風險。

### 2. 生靈生命週期 (Lifecycle)
- **`start()`**：啟動虛擬執行緒，進入訊息輪詢迴圈。
- **`stop()`**：安全排空未處理訊息，釋放資源。

---

## 🧭 三、MudContext 上下文治理

在處理跨服務呼叫時，系統需要知道「當前發起請求的玩家是誰」。

專案提供 `MudContext`（基於 `InheritableThreadLocal`）：
```java
public class MudContext {
  private static final ThreadLocal<Player> CURRENT_PLAYER = new ThreadLocal<>();
  
  public static Player currentPlayer() {
    return CURRENT_PLAYER.get();
  }
}
```
- **生命週期規範**：
  在 WebSocket 收到文字指令時綁定 `MudContext.setCurrentPlayer(player)`，指令處理完畢於 `finally` 區塊強制 `clear()`，徹底防範 ThreadLocal 記憶體洩漏與跨請求資料污染。

---

## 🌐 四、WebSocket 連線生命週期與全雙工通訊

前後端透過單一全雙工 WebSocket 連線（預設端點：`ws://localhost:8080/ws`）進行即時互動：

```
[前端 (websocket.js)]                                      [後端 (MudWebSocketHandler)]
         │                                                            │
         │ ────────── 1. HTTP 升級連線 (Handshake) ───────────────────> │
         │ <───────── 2. 連線就緒 (Connection Established) ─────────── │
         │                                                            │
         │ ────────── 3. 玩家指令 (如 "step w", "look") ──────────────> │
         │                                                            │
         │ <───────── 4. ANSI 終端彩色訊息 (如 "你走進了墨竹山...") ───── │
         │ <───────── 5. DRPG 全景狀態 JSON (DrpgStateDto) ─────────── │
         │                                                            │
         │ ────────── 6. 心跳 Ping / Pong 保活 (每 15 秒) ────────────> │
```

- **自動斷線重連**：前端 `websocket.js` 實作指數退避重連機制（Exponential Backoff），當網路短暫中斷時自動嘗試重連，並平滑恢復遊戲狀態。

---

## 📦 五、單一全景狀態契約：`DrpgStateDto`

遊戲避免了傳統 API 設計中「發送 10 個小請求更新不同面板」的碎裂做法，而是採用統一的 **單一全狀態推送模型 (Single State Payload)**。

後端透過 `GameStateBroadcastService.broadcastState(player)` 統一派發 `DrpgStateDto`：

### 核心 JSON 結構切片
```json
{
  "type": "DRPG_STATE",
  "viewMode": "DUNGEON", // 可為 "TOWN" 或 "DUNGEON"
  
  // 地牢視角 (當 viewMode == "DUNGEON" 時非空)
  "dungeon": {
    "floorId": "mozhu_mines_b1f",
    "floorName": "墨竹山廢棄靈石礦坑一層 (B1F)",
    "x": 1,
    "y": 8,
    "direction": "NORTH",
    "directionArrow": "▲",
    "width": 10,
    "height": 10,
    "tiles": [ [ "#", ".", ... ] ],
    "visited": [ [ false, true, ... ] ],
    "openedChests": [ [ false, false, ... ] ],
    "forwardInspection": "正前方是一座緊閉的加固避煞木門...",
    "dangerLevel": 1,
    "dangerStatus": "靜謐微寒"
  },

  // 小隊狀態視角 (始終存在)
  "party": {
    "partyId": "party-李長生",
    "activeFormationId": "nine_palaces_eight_trigrams",
    "formationName": "九宮八卦陣",
    "members": [
      {
        "id": "char-1",
        "name": "李長生",
        "row": "FRONT",
        "hp": 120, "maxHp": 120,
        "mp": 80,  "maxMp": 80,
        "san": 95, "maxSan": 100,
        "poise": 50, "maxPoise": 50,
        "skills": [ ... ]
      }
    ],
    "inventory": {
      "capacity": 30,
      "slots": [ ... ]
    }
  },

  // 戰鬥視角 (僅戰鬥中非空)
  "battle": {
    "enemies": [
      {
        "id": "mob-001",
        "name": "嗜煞食腐鼠",
        "hp": 45, "maxHp": 45,
        "poise": 20, "maxPoise": 20,
        "actionIntervalMs": 1800,
        "currentProgressMs": 600,
        "resistances": { "FIRE": -0.2, "CORRUPT": 0.5 }
      }
    ],
    "inBattle": true
  }
}
```

---

## 🔗 六、代表類別與通訊路徑

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **虛擬執行緒 Actor** | `com.example.htmlmud.domain.actor.core.VirtualActor` | 訊息信箱輪詢與虛擬執行緒生命週期 |
| **玩家 Actor** | `com.example.htmlmud.domain.actor.Player` | 玩家實體 Actor、信箱隔離 |
| **通訊處理器** | `com.example.htmlmud.interfaces.websocket.MudWebSocketHandler` | WebSocket 握手、中斷與文字指令接收 |
| **狀態廣播器** | `com.example.htmlmud.application.service.GameStateBroadcastService` | 全景狀態組裝與 JSON 推送 |
| **上下文管理** | `com.example.htmlmud.infrastructure.context.MudContext` | 執行緒安全當前玩家綁定與排空 |
| **協議 DTO** | `com.example.htmlmud.interfaces.dto.DrpgStateDto` | 前後端全景資料交換單一契約 |

---

## 🎯 七、純單機單體通訊演進規劃 (Single-Player Monolith Roadmap)

專案已正式決議專注於**「純單機單體 (Single-Player Modular Monolith)」**，通訊層聚焦於本機極致響應與乾淨的 JSON 契約：

### 1. 🎯 單機狀態同步深化 (Single-Player State Efficiency)
- **Delta 增量差異同步 (JSON Patch / RFC 6902)**：
  目前每次狀態變更均推送完整的 `DrpgStateDto`。未來可引入差異比對演算，僅在戰鬥結算或位移時推送欄位 Patch（如 `[{"op": "replace", "path": "/party/members/0/hp", "value": 115}]`），減少 DOM 重新渲染成本與本機序列化開銷。
- **本機 WebSocket JSON 乾淨契約**：
  維持純粹、標準的 JSON 文字通訊，便於瀏覽器 DevTools 排查與單元測試斷言，不引入複雜二進位協定。

### 2. 🛡️ 穩定性與狀態防禦 (Robustness & Resilience)
- **Actor 信箱背壓機制 (Mailbox Backpressure & Throttling)**：
  當信箱積壓過多無效高頻廣播時，自動合流丟棄中間渲染影格，始終保障玩家的「逃跑」、「喝藥」等關鍵輸入即時被 Actor 優先響應。
- **本機狀態生命週期清理**：
  戰鬥結束或玩家退出時，嚴格清理 Actor 上下文與信箱隊列，杜絕虛擬執行緒或記憶體洩漏。

### 3. 🚫 過度設計正式除役宣告 (Decommissioned Over-Engineering Scope)
- ❌ **二進位 FlatBuffers / Protobuf 串流**：已正式除役，維持標準 WebMVC REST API 與 WebSocket JSON。
- ❌ **分散式 Actor 房間分片 (Redis Pub/Sub / gRPC)**：已正式除役，所有 Actor 嚴格運行於本機單一 JVM 處理程序，杜絕分散式與跨服網路複雜度。
