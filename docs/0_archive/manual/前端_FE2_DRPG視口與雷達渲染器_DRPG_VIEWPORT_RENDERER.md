# 🗺️ 前端 FE-2: DRPG 視口透視與雷達渲染器深度手冊 (DRPG Viewport & Radar Renderer Manual)

> **模組編號**：FE-2  
> **系統類型**：前端表現層模組  
> **核心定位**：管理第一人稱暗黑地牢 3 步透視視口投影渲染、雙雷達相機跟隨模式（Centered vs Full）、通用圖塊外觀樣式映射契約（populateTileAppearance），以及迷霧踩格即時揭示。

---

## 🏛️ 一、模組職責與視覺佈局

DRPG 視口與雷達渲染器託管於 `static/js/panels/dungeon-panel.js`，當 `store.viewMode === 'DUNGEON'` 時主導主螢幕的視覺呈現：

```
┌────────────────────────────────────────────────────────┐
│                   DRPG 探索主介面佈局                   │
├───────────────────────────┬────────────────────────────┤
│                           │                            │
│   第一人稱透視視口面板    │      雷達小地圖 (Minimap)   │
│   (#dungeon-viewport)     │      (#dungeon-radar)      │
│                           │                            │
│   • 3步暗黑走廊透視       │   • 10x10 二維網格         │
│   • 石門/寶箱/階梯地貌    │   • 視口居中跟隨 / 全圖模式│
│   • 前方氛圍描述文字      │   • 暗黑迷霧踩格揭示       │
│                           │   • 朝向箭頭標記 (▲ ► ▼ ◄) │
│                           │                            │
└───────────────────────────┴────────────────────────────┘
```

---

## 🔦 二、第一人稱透視視口渲染演算法

視口並非靜態圖片，而是根據後端推送之 `forwardInspection` 與前方地塊序列進行分層渲染：

1. **三層景深結構 (Depth Layers)**：
   - **近景 (Step 1)**：距離當前位置 1 格。若為玄岩石壁或閉合木門，地圖完全被阻斷並繪製近距離壓迫質感。
   - **中景 (Step 2)**：距離 2 格。可看見走廊轉角或地面機關陷阱。
   - **遠景 (Step 3)**：距離 3 格。黑暗深處微弱靈光或向下階梯輪廓。
2. **氛圍描述字幕動態融入**：
   - 在視口下方動態更新文字提示（如「正前方是一座緊閉的加固避煞木門，門縫隱隱散發暗紫異化煞氣」）。

---

## 🗺️ 三、雙雷達相機模式 (Dual Minimap Camera Modes)

玩家可隨時點擊雷達標題切換顯示模式：

### 1. 視口居中跟隨模式 (Centered Viewport Mode)
- **視覺體驗**：相機鏡頭始終鎖定以玩家小隊為中心，動態計算偏移量 `offset = (center - playerPos)`。
- **氛圍感**：重現經典 Wizardry / 巫術系列的幽閉探索感，僅展示小隊身邊局部視野。

### 2. 全圖宏觀模式 (Full Map Mode)
- **視覺體驗**：將整個 10x10 地牢網格等比縮放完整呈現。
- **戰術價值**：便於玩家全局審視已探明的走廊路徑、已開啟的棺槨寶箱位置與出口石階。

---

## 🎨 四、圖塊通用渲染契約 (`populateTileAppearance`)

嚴格遵循專案第一紅線，圖塊渲染完全中立、通用，禁止在代碼中寫死關卡特例：

```javascript
export function populateTileAppearance(cellElement, tileChar, isVisited, isOpenedChest) {
  // 1. 若該格未曾踏足 (未被 visited 矩陣收錄)，一律遮蔽為暗黑迷霧
  if (!isVisited) {
    cellElement.className = 'radar-cell cell-fog';
    cellElement.textContent = ' ';
    return;
  }
  
  // 2. 依圖元符號資料驅動映射
  ...
}
```

### 通用圖元樣式映射標準表
| 圖元符號 | CSS 類別 | 顯示圖標 | Tooltip 說明 | 行為特性 |
| :---: | :--- | :---: | :--- | :--- |
| `#` | `cell-wall` | `█` | 玄岩石壁 | 不可通行（阻擋視線與位移） |
| `+` / `D` | `cell-door` | `☩` | 封印石門 | 需互動開啟，阻擋視線 |
| `$` / `◆` | `cell-chest` | `◆` | 密封寶箱 / 棺槨 | 踩格自動觸發摸金探寶 |
| `◇` | `cell-chest-opened` | `◇` | 已開啟寶箱 | 已空置，可自由通行 |
| `^` / `T` | `cell-trap` | `▲` | 機關陷阱 | 踩格扣除道心 SAN 或氣血 |
| `>` | `cell-stairs` | `▼` | 向下階梯 | 通往更深地牢樓層 |
| `<` | `cell-stairs-up` | `▲` | 向上階梯 / 出口 | **安全撤離點**（輸入 `leave` 撤離） |
| `.` | `cell-path` | `·` | 通路 / 枕木道 | 常規可通行路徑 |

---

## ⌨️ 五、快捷操作與鍵盤綁定

前端提供鍵盤與點擊雙軌支援：
- **`W` / `↑`**：向前邁進一步（發送指令 `step w`）。
- **`S` / `↓`**：向後退一步（發送指令 `step s`）。
- **`A` / `←`**：原地向左轉身 90 度（發送指令 `turn a`）。
- **`D` / `→`**：原地向右轉身 90 度（發送指令 `turn d`）。
- **`L`**：在出口石階處一鍵撤離地牢（發送指令 `leave`）。

---

## 🔗 六、代表檔案清單

| 檔案路徑 | 模組定位 | 關鍵導出項目 |
| :--- | :--- | :--- |
| `static/js/panels/dungeon-panel.js` | DRPG 視口與雷達主控制器 | `renderDungeonPanel()`, `populateTileAppearance()` |
| `static/css/style-01-tokens-and-shell.css` | 視口幾何容器與雷達網格佈局 | `.dungeon-viewport`, `.radar-grid`, `.radar-cell` |

---

## 🚀 七、未來架構演進與現代化擴充規劃 (Future Roadmap & Scalability)

為使第一人稱地牢探索體驗從文字/網格邁向具備沉浸式氛圍感與高度操作流暢度的現代 DRPG，確立以下四大演進方向：

### 1. 🎯 現代視覺與機制深化 (Modern Visuals & Immersion)
- **輕量 WebGL / Raycasting 偽 3D 平滑過渡 (Smooth Step & Turn Transitions)**：
  在保持純原生無重型 3D 引擎的前提下，引入經典 Wolfenstein / 巫術風格的輕量 Raycasting 光線投射。玩家轉身或前進時不再是瞬間切圖，而是具備 150ms 的絲滑視角過渡、火把光影搖曳與煞氣迷霧流動，沉浸感倍增。
- **動態視野半徑與迷霧氛圍 (Fog of War Particles)**：
  根據小隊道心 SAN 值與火把照明，視口邊緣動態疊加不可名狀的暗紫呼吸粒子與暗角昏厥效應（Vignette）。

### 2. 🛡️ 穩定性與容錯防禦 (Robustness & Resilience)
- **圖元自我修復容錯機制 (Tile Self-Healing Fallback)**：
  當遇到策劃在地圖 JSON 中填入未定義的特殊圖元字元時，`populateTileAppearance` 自動平滑降級為通用通道（`cell-path`）或阻擋石壁，並於控制台輸出警告日誌，保證地牢渲染永遠不因單一圖元未知而全屏報錯崩潰。

### 3. ⚡ 效能與繪製最佳化 (Performance & Concurrency)
- **離屏 Canvas 靜態迷霧分塊快取 (OffscreenCanvas Layering)**：
  將雷達小地圖拆分為雙層架構：底層為已探索靜態迷霧（快取於離屏 Canvas），頂層僅重繪小隊箭頭座標與當前前方視野動態視錐，每步渲染耗時自 8ms 壓至 0.5ms 以內，極致流暢省電。

### 4. 🔌 可擴充性與模組化 (Extensibility & Modding)
- **雷達筆記與圖示標記系統 (Player Minimap Pinning System)**：
  支援玩家在雷達上點擊任意格子進行戰術標記（如：標記「未解石門 ☩」、「待戰妖皇 👹」、「隱蔽密道 ❓」）。標記資料隨 `SaveData` 一同持久化，還原經典走迷宮手繪地圖的骨灰樂趣。
