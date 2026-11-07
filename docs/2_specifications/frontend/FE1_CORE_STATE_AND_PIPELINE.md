# ⚡ 前端 FE-1: 核心狀態與事件管線深度手冊 (Core State & Pipeline Manual)

> **模組編號**：FE-1  
> **系統類型**：前端表現層模組  
> **核心定位**：全域單一資料流狀態管理中心、全域純原生 `[data-action]` 事件委派機制、WebSocket 即時網路通訊與斷線重連管線，以及 ANSI 彩色文字串流解碼。

---

## 🏛️ 一、模組架構與工作管線全覽

前端完全摒棄了 React / Vue / Angular 等重型框架與 Node.js 構建依賴，基於現代瀏覽器純原生 **ES6 Modules** 構建輕量、極速、強健的響應式架構：

```mermaid
graph TD
    WS["WebSocket 網路連線 (websocket.js)"] -->|解析 JSON (DRPG_STATE)| App["應用主控制器 (app.js: updateDrpgView)"]
    App -->|更新狀態快照| Store["中央狀態樹 (state-store.js)"]
    WS -->|情境敘事日誌流| Terminal["歷程情報日誌 (#log / message-log-panel.js)"]
    
    User["玩家點擊任意 DOM 節點"] -->|事件冒泡| Delegation["全域事件委派 (app.js 監聽 document)"]
    Delegation -->|解析 data-action| ActionHandler["執行對應 Action (如 openSaveModal, sendStep)"]
    ActionHandler -->|發送文字指令封包 {type: INPUT, text: ...} (sendCmd)| WS
    ActionHandler -->|更新本地 UI 標記| Store
    
    Store -->|廣播狀態變更事件 (state:changed)| Panels["各面板重繪 (renderTownNav, renderMinimap, renderBattleArena, renderPartyHud)"]
```

> 📌 **【傳入通訊協定規範】**：  
> 現行前後端互動統一透過 `sendCmd(text)` 發送 `{"type": "INPUT", "text": "..."}` 文字指令封包；結構化 `ActionCommand` 協定標註為 `【待深入討論／長線演進藍圖】`。

---

## 🌳 二、單一響應式狀態中心 (`state-store.js`)

前端所有視圖資料統一託管於 `src/main/resources/static/js/core/state-store.js`：

### 1. 核心狀態樹結構
- **`mode`**：當前主視圖模式，為 `'TOWN'`（營地中樞）或 `'DUNGEON'`（第一人稱地牢 DRPG）。
- **`lastTown`**：營地資訊快照（`zoneId`, `zoneName`, `hub: TownHubDto`, `npcs`, `items`）。
- **`lastDungeon`**：地牢樓層 ID、座標 `(x, y)`、朝向、`visited` 迷霧矩陣、`openedChests` 寶箱矩陣、`forwardInspection` 前方視野文字、`dangerLevel`、`torchLight`。
- **`lastParty`**：小隊 5 位成員之即時氣血（HP）、真元（MP）、精神防護罩（SAN）、罡氣韌性（Poise）、裝備、技能與 40 格小隊共用儲物袋 (`inventory`) 及靈石金庫 (`coin`)。
- **`lastBattle`**：戰鬥進行標記 (`inBattle`)、敵方怪物清單、敵方氣血/韌性/行動進度條、目標鎖定。
- **`radarPosition` / `radarMode`**：雷達位置（`'left'` | `'right'`）與模式（`'centered'` | `'full'`）。
- **`saveSlots`**：五大存檔槽位元資料快照。
- **彈窗與抽屜可見性**：`isTitleScreenOpen`, `isPartyModalOpen`, `isBagDrawerOpen`, `isSaveModalOpen`, `isShopModalOpen`, `isDevConsoleOpen`, `isSkillDrawerOpen`。

### 2. 狀態更新與觀察者模式
```javascript
class StateStore {
  getState() { return this.state; }
  get(key) { return this.state[key]; }

  setState(partial, silent = false) {
    const changedKeys = [];
    for (const [key, value] of Object.entries(partial)) {
      if (this.state[key] !== value) {
        this.state[key] = value;
        changedKeys.push(key);
      }
    }
    if (!silent && changedKeys.length > 0) {
      eventBus.emit('state:changed', { state: this.state, changedKeys });
      for (const key of changedKeys) {
        eventBus.emit(`state:${key}`, this.state[key]);
      }
    }
  }

  subscribe(key, handler) {
    return eventBus.on(`state:${key}`, handler);
  }
}
export const store = new StateStore();
```
- **單向資料流**：任何面板嚴禁直接修改其他面板的 DOM，統一透過更新 `store` 觸發自定義事件通知面板自我重繪。

---

## 🎯 三、純原生全域事件委派機制 (`app.js`)

為確保極致渲染效能並嚴格遵循 CSP（內容安全策略），前端**全面杜絕 HTML 內聯 `onclick` 屬性（現役 JS 達成 0 處內聯 `onclick`）**：

### 1. 事件委派設計模式
在頁面加載時，向最外層 `document` 註冊單一 `click` 監聽器，解析 `data-action` 屬性：
```javascript
document.addEventListener('click', (event) => {
  const trigger = event.target.closest('[data-action]');
  if (!trigger) return;
  const action = trigger.getAttribute('data-action');
  handleAction(action, trigger);
});
```

### 2. 標準動作清單 (Action Handlers)
| `data-action` 名稱 | 攜帶參數範例 | 行為說明 |
| :--- | :--- | :--- |
| `open-save-modal` | `data-mode="save"` / `data-mode="load"` | 開啟存檔/讀檔多槽位視窗 |
| `close-save-modal` | 無 | 關閉存檔彈窗 |
| `toggle-party` / `close-party` | 無 | 開啟/關閉 5 人小隊仙道總覽彈窗 |
| `main-menu-tab` | `data-menu="ITEMS\|EQUIP\|SKILLS\|CHARACTERS\|PARTY\|FORMATION\|TACTICS\|SYSTEM"` | 切換仙道總覽主選單分頁 |
| `open-menu` | `data-menu="..."` | 快捷導向特定選單分頁 |
| `toggle-bag` / `close-bag` | 無 | 開啟/關閉 40 格小隊共用儲物袋抽屜 |
| `step` | `data-step="w"` / `"a"` / `"s"` / `"d"` | 派發地牢移動或轉向指令 (`step <dir>`) |
| `dungeon-torch` | 無 | 消耗火把或靈石點燃火把 (`dungeon torch`) |
| `trigger-inspect` | 無 | 凝神探查前方視野 (`dungeon look`) |
| `trigger-rest` | 無 | 地牢原地調息 (`rest`) |
| `open-facility` | `data-facility-id="fac_forge"` | 開啟營地六大機能設施 |
| `camp-rest` | 無 | 營地客棧全隊打坐調息 (`camp_rest`) |
| `enter-dungeon` | `data-dungeon-id="mozhu_mines_b1f"` | 啟程進入指定地牢 |
| `toggle-radar-dock` | 無 | 切換雷達停靠於視口左側或右側 |
| `toggle-radar-mode` | 無 | 切換雷達中心跟隨 (`centered`) 與全圖 (`full`) |
| `filter-log` | `data-cat="all"` / `"combat"` / `"story"` / `"system"` | 過濾歷程日誌分類 |
| `toggle-log-collapse` / `clear-log` | 無 | 折疊/清空歷程日誌 |
| `send-cmd` | `data-cmd="who"` | 發送指定文字指令 |

> 📌 **【容器級委派】**：各彈窗與面板（`party-modal.js`、`party-equipment-tab.js`、`party-skills-tab.js`、`party-formation-tab.js`、`battle-panel.js`、`save-modal.js`、`shop-modal.js`、`skill-drawer.js`）亦全面採用模組級 `data-action` 委派。

---

## 🌐 四、WebSocket 連線與強健重連管線 (`websocket.js`)

前端透過 `src/main/resources/static/js/network/websocket.js` 與後端保持全雙工長連線通訊：

1. **指數退避斷線重連 (Exponential Backoff)**：
   - 當網路異常或伺服端重啟中斷時，自動以 $1\text{s} \to 2\text{s} \to 4\text{s} \to 8\text{s} \to 15\text{s}$（上限 15 秒）間隔嘗試重連。
   - 重連成功後自動發送 `saves quiet` 刷新最新狀態。
2. **單機本機心跳政策 (Localhost Keep-Alive Policy)**：
   - 遵循架構決策二與純單機定位：在純本機開發與執行環境（`localhost:8080`），除役過度設計的高頻 15 秒文字 Ping，連線保活依賴底層 Ping-Pong 與瀏覽器 `onclose` 事件。
3. **ANSI 彩色字串解碼器與靜默過濾**：
   - 步進移動（`step *`）與背景狀態拉取（`*quiet*`）自動走靜默通道，不洗版終端文字日誌。
   - 將伺服端傳送的 ANSI 終端跳脫序列即時轉換為具備對應 CSS 樣式的 `<span>` 元素。

---

## 🔗 五、代表檔案清單

| 檔案路徑 | 模組定位 | 關鍵導出項目 |
| :--- | :--- | :--- |
| `src/main/resources/static/js/app.js` | 核心應用控制器與事件委派 | `updateDrpgView()`, `updateGlobalHeader()`, `updateContextActionBar()`, `initKeyboardControls()`, `initEventDelegation()`, `triggerRestAction()`, `triggerInspectAction()`, `triggerFormationUltimate()` |
| `src/main/resources/static/js/core/state-store.js` | 全域響應式狀態中心 | `store` (`getState()`, `get()`, `setState()`, `subscribe()`) |
| `src/main/resources/static/js/core/event-bus.js` | 純原生全域事件總線 | `eventBus` (`on()`, `off()`, `emit()`) |
| `src/main/resources/static/js/core/cmd-dispatcher.js` | 指令封裝與派發器 | `sendCmd()`, `sendStep()`, `sendTownMove()`, `handleDpad()` |
| `src/main/resources/static/js/core/constants.js` | 全域常數與按鍵定義 | `UI_CONSTANTS`, `MIN_FONT_SIZE_PX`, `MAX_LOG_HISTORY_LINES`, `MENU`, `KEYS`, `RESOURCE_THEMES` |
| `src/main/resources/static/js/core/ui-utils.js` | 通用 UI 渲染輔助工具 | `UiUtils`, `escapeHtml()`, `calculatePercentage()`, `createResourceBar()`, `getQualityClass()`, `formatNumber()` |
| `src/main/resources/static/js/network/websocket.js` | WebSocket 通訊管線 | `connectWebSocket()`, `sendWsCommand()`, `disconnectWebSocket()`, `getWebSocket()`, `isWebSocketConnected()` |
| `src/main/resources/static/js/panels/message-log-panel.js` | 歷程情報日誌面板 | `appendLog()`, `clearLog()`, `filterLog()`, `toggleLogCollapse()`, `initMessageLogControls()`, `updateTicker()` |

---

## 🚀 六、未來架構演進規劃 (Roadmap)【待深入討論／長線演進藍圖】

1. **時間旅行狀態重放器 (Time-Travel Debugger)**：中央狀態樹支援 Action 歷史快照隊列，支援倒帶與前進。
2. **全量 JSDoc / TypeScript 型別宣告**：為 `DrpgStateDto`、`PartyState`、`BattleState` 提供端到端靜態型別提示。
3. **Service Worker 離線資產快取**：預先快取所有 HTML、CSS 與 JS 靜態資源，弱網環境下介面永不白屏。
4. **Web Worker 背景資料解碼**：將大體積 JSON 反序列化與 ANSI 彩色字元解析移入專屬 Web Worker。
