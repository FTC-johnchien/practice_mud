# ⚡ 前端 FE-1: 核心狀態與事件管線深度手冊 (Core State & Pipeline Manual)

> **模組編號**：FE-1  
> **系統類型**：前端表現層模組  
> **核心定位**：全域單一資料流狀態管理中心、全域純原生 `[data-action]` 事件委派機制、WebSocket 即時網路通訊與斷線重連管線，以及 ANSI 彩色文字串流解碼。

---

## 🏛️ 一、模組架構與工作管線全覽

前端完全摒棄了 React / Vue / Angular 等重型框架與 Node.js 構建依賴，基於現代瀏覽器純原生 **ES6 Modules** 構建輕量、極速、強健的響應式架構：

```mermaid
graph TD
    WS["WebSocket 網路連線 (websocket.js)"] -->|解析 JSON (DrpgStateDto)| Store["中央狀態樹 (store.js)"]
    WS -->|解析 ANSI 文字流| Terminal["終端輸出面板 (terminal-output)"]
    
    User["玩家點擊任意 DOM 節點"] -->|事件冒泡| Delegation["全域事件委派 (app.js 監聽 document)"]
    Delegation -->|解析 data-action| ActionHandler["執行對應 Action (如 openSaveModal, castSkill)"]
    ActionHandler -->|發送文字指令| WS
    ActionHandler -->|更新本地 UI 標記| Store
    
    Store -->|廣播狀態變更事件| Panels["各面板重繪 (Dungeon / Battle / Town / Modals)"]
```

---

## 🌳 二、單一響應式狀態中心 (`store.js`)

前端所有視圖資料統一託管於 `static/js/state/store.js`：

### 1. 核心狀態樹結構
- **`viewMode`**：當前主視圖模式，可為 `'TOWN'`（城鎮文字 MUD）或 `'DUNGEON'`（第一人稱地牢 DRPG）。
- **`dungeon`**：地牢樓層 ID、座標 `(x, y)`、朝向、`visited` 迷霧矩陣、`openedChests` 寶箱矩陣、`forwardInspection` 前方視野文字。
- **`party`**：小隊 5 位成員之即時氣血（HP）、真元（MP）、道心（SAN）、罡氣（Poise）、裝備與 30 格小隊共用儲物袋。
- **`battle`**：戰鬥進行標記、敵方怪物清單、敵方氣血/韌性/行動進度條、當前目標鎖定。
- **`radarMode`**：雷達地圖顯示模式，支援 `'centered'`（中心跟隨）與 `'full'`（全圖宏觀）。

### 2. 狀態更新與觀察者模式
```javascript
export const store = {
  state: { ... },
  listeners: [],
  setState(partialState) {
    Object.assign(this.state, partialState);
    this.notify();
  },
  subscribe(fn) {
    this.listeners.push(fn);
  },
  notify() {
    this.listeners.forEach(fn => fn(this.state));
  }
};
```
- **單向資料流**：任何面板嚴禁直接修改其他面板的 DOM，統一透過更新 `store` 觸發自定義事件通知面板自我重繪。

---

## 🎯 三、純原生全域事件委派機制 (`app.js`)

為確保極致渲染效能並嚴格遵循 CSP（內容安全策略），前端**全面杜絕 HTML 內聯 `onclick` 屬性**：

### 1. 事件委派設計模式
在頁面加載時，僅向最外層 `document` 註冊單一 `click` 監聽器：
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
| `open-save-modal` | `data-mode="save"` / `data-mode="load"` | 開啟五大槽位存檔/讀檔視窗 |
| `close-save-modal` | 無 | 關閉存檔彈窗 |
| `toggle-party-modal`| 無 | 開啟/關閉 5 人小隊管理彈窗 |
| `switch-equipment-tab`| `data-member-id="char-1"` | 切換小隊成員裝備換裝分頁 |
| `cast-skill` | `data-skill-id="slash_01"` | 向伺服器派發施放技能指令 |
| `select-target` | `data-target-id="mob-001"` | 鎖定集火目標怪物 |
| `toggle-radar-mode` | 無 | 切換視口中心跟隨與全圖雷達 |
| `dungeon-move` | `data-dir="forward"` / `data-dir="turn-left"` | 派發地牢移動或旋轉指令 |

---

## 🌐 四、WebSocket 連線與強健重連管線 (`websocket.js`)

前端透過單一連線管道與後端 `MudWebSocketHandler` 保持通訊：

1. **心跳保活機制 (Keep-Alive)**：
   - 每 15 秒主動發送一次文字 Ping，防止代理伺服器或瀏覽器閒置切斷連線。
2. **指數退避斷線重連 (Exponential Backoff)**：
   - 當網路中斷時，自動以 $1\text{s} \to 2\text{s} \to 4\text{s} \to 8\text{s}$（上限 15 秒）間隔嘗試重連。
   - 重連成功後自動請求刷新最新狀態，玩家無需手動重整瀏覽器。
3. **ANSI 彩色字串解碼器**：
   - 將伺服端傳送的 ANSI 終端跳脫序列（如 `\u001b[32m` 綠色、`\u001b[31m` 紅色）即時轉換為具備對應 CSS 樣式的 `<span>` 元素，完美呈現復古 MUD 文字氛圍。

---

## 🔗 五、代表檔案清單

| 檔案路徑 | 模組定位 | 關鍵導出項目 |
| :--- | :--- | :--- |
| **代表檔案** | `static/js/network/websocket.js` | WebSocket 通訊管線 | `connectWebSocket()`, `sendWsCommand()`, `onMessage` |

---

## 🚀 六、未來架構演進與現代化擴充規劃 (Future Roadmap & Scalability)

為使前端核心管線具備商用級的響應極速性、狀態可溯性與跨平台移植能力，確立以下四大演進方向：

### 1. 🎯 現代前端架構深化 (Modern Frontend Architecture)
- **時間旅行狀態重放器 (Time-Travel Debugger)**：
  在開發與調試模式下，中央狀態樹（`store.js`）支援 Action 歷史快照隊列。前端可隨時「倒帶（Rewind）」或「前進（Fast-forward）」任何一個戰鬥回合或地牢位移，大幅提升 UI 動效調整與競態邊界的排查效率。
- **全量 JSDoc / TypeScript 型別宣告**：
  為 `DrpgStateDto`、`PartyState`、`BattleState` 提供端到端的靜態型別提示與自動補全，杜絕因手誤造成的 `undefined` 存取錯誤。

### 2. 🛡️ 穩定性與離線防禦 (Robustness & Resilience)
- **Service Worker 離線資產快取 (PWA Capability)**：
  引入輕量 Service Worker，預先快取所有 HTML、CSS、JS 與音效資源。在網路偶發中斷或地鐵弱網環境下，介面永不白屏；連線中斷時頂部 HUD 顯示「靈識微弱」，自動排隊指令待重連時發送。
- **嚴格 CSP（內容安全策略）零違規防禦**：全面杜絕任何動態 `eval()`，所有事件綁定 100% 透過 `data-action` 委派，天然免疫 XSS 腳本注入風險。

### 3. ⚡ 效能與多執行緒解碼 (Performance & Concurrency)
- **Web Worker 背景資料解碼 (Off-Main-Thread Parsing)**：
  將大體積 JSON 反序列化、Delta 比對以及 ANSI 彩色字元解析移入專屬 Web Worker。主執行緒僅負責純粹的 DOM 節點更新與 60/120 FPS 渲染，徹底杜絕巨量戰鬥日誌輸出時的介面微幅卡頓。

### 4. 🔌 可擴充性與中介軟體 (Extensibility & Middleware)
- **外掛中介軟體管線 (Middleware Pipeline)**：
  為 `store.dispatch` 注入類似 Redux 的中介軟體擴充機制。開發者可自由掛載 `loggerMiddleware`（自動記錄動作至控制台）、`analyticsMiddleware`（統計玩家探索步數與戰鬥時長）或自訂 UI 巨集擴充。
