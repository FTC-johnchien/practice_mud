# 🗺️ 前端 FE-2: DRPG 視口透視與雷達渲染器深度手冊 (DRPG Viewport & Radar Renderer Manual)

> **模組編號**：FE-2  
> **系統類型**：前端表現層模組  
> **核心定位**：管理 2D 迷霧網格雷達地圖、正前方三步文字探查卡、九宮格步進羅盤、雙雷達相機跟隨模式（Centered vs Full），以及圖塊通用渲染契約（`populateTileAppearance`）。

---

## 🏛️ 一、模組職責與視覺佈局

DRPG 探索面板託管於 `src/main/resources/static/js/panels/dungeon-panel.js`，當 `store.get('mode') === 'DUNGEON'` 時主導中央主舞台的視覺呈現：

```
┌────────────────────────────────────────────────────────┐
│               【現行已驗證】DRPG 探索主介面佈局         │
│                 (#dungeon-radar-panel)                 │
├────────────────────────────────────────────────────────┤
│ • 頂部控制列 (#radar-dock-toggle-btn 換邊 / 視口全圖)  │
│ • 樓層資訊與雙條 (#drpg-floor-info: 靈壓條 + 光明度條) │
│ • 🔥 點火按鈕 (data-action="dungeon-torch")           │
│ • 2D 迷霧網格雷達 (#drpg-grid 搭配 .grid-cell)         │
│ • 正前方三步探查卡 (#drpg-forward-inspect)             │
│ • 九宮格步進方向羅盤 (.compass-grid)                   │
└────────────────────────────────────────────────────────┘
```

> 📌 **【視口演進藍圖聲明】**：  
> 現行以 2D 迷霧網格雷達與前方文字探查卡為核心驗證基準；第一人稱 3 步圖像透視畫布（`#viewport-canvas`）列為 **`【待深入討論／長線演進藍圖】`**。

---

## 🔦 二、前方視野探查與氛圍融入

視口根據後端推送之 `forwardInspection` 與前方地塊序列進行文字敘事投影：
- **正前方三步文字探查卡 (`#drpg-forward-inspect`)**：接收後端 `DungeonNavigator.inspectForward()` 射線結果，即時更新前方障礙、機關或石門氛圍（如「前方三步為堅硬玄岩，石縫滲出暗紅組織...」）。
- **第一人稱 3 步透視畫布 (`#viewport-canvas`)【待深入討論／長線演進藍圖】**：規劃分為近景 (Step 1)、中景 (Step 2)、遠景 (Step 3) 三層景深圖像透視，待後續版本深入設計。

---

## 🗺️ 三、雙雷達相機模式 (Dual Minimap Camera Modes)

玩家可隨時點擊頂部 `#radar-mode-toggle-btn` 按鈕切換顯示模式：

### 1. 視口居中跟隨模式 (Centered Viewport Mode)
- **視覺體驗**：相機鏡頭始終以玩家小隊座標為中心，動態計算偏移。
- **氛圍感**：重現經典 Wizardry 幽閉探索感，僅展示小隊身邊局部視野。

### 2. 全圖宏觀模式 (Full Map Mode)
- **視覺體驗**：將整個 10x10 地牢網格完整呈現。
- **戰術價值**：便於玩家全局審視已探明的走廊路徑、已開啟的寶箱位置與出口石階。

---

## 🎨 四、圖塊通用渲染契約 (`populateTileAppearance`)

嚴格遵循專案第一紅線，圖塊渲染完全中立、通用，禁止在代碼中寫死關卡特例：

```javascript
export function populateTileAppearance(cell, symbol, gx, gy, customName = null) {
  // 1. 依圖元符號資料驅動映射外觀、圖標與 Tooltip
  // 2. 支援自定義名稱（若 DTO 提供 tileNames 矩陣則優先使用，未提供則使用通用預設名稱）
}
```

### 通用圖元樣式映射標準表
| 圖元符號 | CSS 類別 | 顯示圖標 | Tooltip 說明 | 行為特性 |
| :---: | :--- | :---: | :--- | :--- |
| `#` | `cell-wall` / `tile-wall` | `█` | 玄岩石壁 | 不可通行（阻擋視線與位移） |
| `.` | `cell-path` / `tile-path` | `·` | 通路 / 碎石道 | 常規可通行路徑 |
| `+` / `D` | `cell-door` / `tile-door` | `+` (或 `☩`) | 石門 / 加固避煞門 | 檢定根骨或破拆工具開啟 |
| `$` / `◆` / `T` | `cell-chest` / `tile-chest` | `⌸` (或 `◆`) | 密封寶箱 / 古修遺骸 | 踩格自動觸發摸金探寶 |
| `◇` | `cell-chest-opened` / `tile-chest-opened` | `◇` | 已開啟寶箱 | 已空置，可自由通行 |
| `^` / `~` / `X` | `cell-trap` / `tile-trap` | `^` (或 `▲`) | 機關陷阱 / 菌絲泥沼 | 踩格檢定身法，失敗扣氣血與 SAN |
| `B` | `cell-boss` / `tile-boss` | `☠` (或 `👹`) | 首領核心 | 強敵巢穴，踩格觸發 Boss 決戰 |
| `E` | `cell-event` / `tile-event` | `!` (或 `📜`) | 遺跡古碑 / 特殊事件 | 互動觸發特殊劇情或手動存檔點 |
| `S` | `cell-safe` / `tile-safe` | `⛺` (或 `S`) | 安全營地出入口 | 安全休整點 |
| `P` | `cell-spring` / `tile-spring` | `♨` | 太玄清心靈泉 | 滋養靈泉，撫平道心、氣血與真元 |
| `>` | `cell-stairs` / `tile-stairs` | `▼` | 向下階梯 | 通往更深地牢樓層，允許存檔 |
| `<` | `cell-stairs-up` / `tile-stairs-up` | `▲` | 向上階梯 / 出口 | **安全撤離點**（允許手動存檔，或發送 `dungeon extract` 安全撤離） |

---

## ⌨️ 五、快捷操作與鍵盤綁定

前端提供鍵盤與點擊雙軌支援：
- **`W` / `↑`**（或點擊 `data-action="step" data-step="w"`）：發送 `'step w'`（前進）。
- **`S` / `↓`**（或點擊 `data-action="step" data-step="s"`）：發送 `'step s'`（後退）。
- **`A` / `←`**（或點擊 `data-action="step" data-step="a"`）：發送 `'step a'`（左轉）。
- **`D` / `→`**（或點擊 `data-action="step" data-step="d"`）：發送 `'step d'`（右轉）。
- **`I` / `L`**（或點擊 `data-action="trigger-inspect"`）：發送 `'dungeon look'`（凝神探查正前方）。
- **`T`**（或點擊 `data-action="dungeon-torch"`）：發送 `'dungeon torch'`（消耗火把或 10 靈石點火）。
- **`M`**：發送 `'map'` 並觸發 `#drpg-grid` 脈衝動效。
- **安全撤離**：於向上石階（`<` `STAIRS_UP`）處透過指令 `dungeon extract` 安全撤離返回營地；結構化 `MOVE` / `EXTRACT` 動作協定標註為 `【待深入討論／長線演進藍圖】`。

---

## 🔗 六、代表檔案清單

| 檔案路徑 | 模組定位 | 關鍵導出項目 |
| :--- | :--- | :--- |
| `src/main/resources/static/js/panels/dungeon-panel.js` | DRPG 雷達與探查控制器 | `renderMinimap()`, `populateTileAppearance()`, `toggleRadarPosition()`, `applyRadarPosition()`, `toggleRadarViewMode()`, `updateRadarModeBtn()` |
| `src/main/resources/static/css/style-01-tokens-and-shell.css` | 雷達面板與網格佈局樣式 | `.dungeon-radar-panel`, `.radar-grid`, `.grid-cell`, `.forward-inspect-card`, `.compass-grid` |

---

## 🚀 七、未來架構演進規劃 (Roadmap)【待深入討論／長線演進藍圖】

1. **第一人稱 3 步圖像透視畫布 (`#viewport-canvas`)**：引入 WebGL 或輕量 Raycasting 光影透視。
2. **動態視野半徑與迷霧氛圍粒子**：根據道心 SAN 值與火把照明疊加暗角與粒子特效。
3. **離屏 Canvas 靜態迷霧分塊快取**：最佳化超大型地牢網格繪製幀率。
4. **雷達筆記與圖示標記系統**：支援玩家在地圖上手動標記石門與寶箱。
