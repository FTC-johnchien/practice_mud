# 🚀 快速上手與操作指南 (Quickstart Guide)

> **閱讀時間**：約 3 分鐘  
> 本指南提供開發者與玩家最快速啟動、執行測試與掌握核心操作的方法。

---

## 🛠️ 一、環境需求與啟動方式

### 1. 環境需求
- **JDK**：Java 21 或 Java 25（支援 Virtual Threads 虛擬執行緒）
- **建構工具**：Apache Maven 3.9+
- **瀏覽器**：支援現代 ES6 Module 與 WebSocket 的主流瀏覽器（Chrome, Edge, Firefox, Safari）

### 2. 啟動本機伺服器
於專案根目錄執行：

```powershell
# 使用 Maven Wrapper 或本機 Maven 啟動 Spring Boot
.\mvnw spring-boot:run

# 或使用專案提供的輔助腳本
.\run.ps1
```

服務啟動後，開啟瀏覽器造訪：
👉 **`http://localhost:8080`**

### 3. 執行全量測試套件
專案具備完善的單元、整合與架構防護測試：

```powershell
# 執行全套測試（目前基準：324 項測試 100% 綠燈全數通過）
.\test.ps1

# 或透過 Maven 執行單一測試類別
.\mvnw test -Dtest=ArchitectureGuardrailsTest
```

---

## 🎮 二、開局冒險流程 (Play Flow)

1. **封面首頁 (Title Screen)**：
   - **開闢新途**：點擊「✨ 開闢新途」，設定道號（例如「李長生」），挑選初始戰陣（預設載入九宮八卦陣與滿編 5 人小隊）。
   - **繼續冒險 / 選擇存檔**：若已有進度，可直接點擊「⚔️ 繼續冒險」進入最新存檔，或在五大槽位與自動存檔間切換。
2. **城鎮中樞 (Town Mode)**：
   - 起始地位於「桃源新手村·客棧」。
   - 可在右側城鎮面板與 NPC（如客棧掌櫃福伯）交談、進入貨棧買賣補給（辟邪靈茶、金創藥），或前往村落廣場招募夥伴。
3. **前往地牢 (Dungeon Entry)**：
   - 經由「新手村南門 ➔ 墨竹山外圍山道 ➔ 廢棄礦坑入口」。
   - 輸入指令 `dungeon enter mozhu_mines_b1f` 或點擊介面「踏入礦坑」，即可切換為第一人稱 3D 靈境視口！
4. **地牢探險 (DRPG Exploration)**：
   - 探索黑暗礦坑，利用鍵盤方向鍵移動、避開陷阱、搜尋寶箱、擊倒畸變妖魔。
   - 走到向上階梯（`<`）圖塊時，輸入 `leave` 即可安全將戰利品帶回地表！

---

## ⌨️ 三、常用鍵盤與終端指令索引

系統支援**滑鼠視覺化點擊**與**經典鍵盤文字指令**雙向並行：

### 1. DRPG 地牢移動與探索
| 指令 | 鍵盤快捷鍵 | 功能說明 |
| :--- | :--- | :--- |
| `step w` | <kbd>W</kbd> 或 <kbd>↑</kbd> | 向前邁步（消耗時間，檢定遇敵危險率） |
| `step s` | <kbd>S</kbd> 或 <kbd>↓</kbd> | 向後退步 |
| `step a` | <kbd>A</kbd> 或 <kbd>←</kbd> | 向左轉身 90 度 |
| `step d` | <kbd>D</kbd> 或 <kbd>Directions →</kbd> | 向右轉身 90 度 |
| `leave` / `exit` | — | 於向上石階（`<`）處撤出地牢，重返地表房間 |
| `map` | <kbd>M</kbd> | 查看當前樓層迷宮小地圖與已探明迷霧 |

### 2. 半即時小隊戰鬥
- **滑鼠點擊**：戰鬥發生時，下方動作列會自動切換為小隊技能卡牌，直接點擊技能圖標即可施法。
- **終端指令**：
  - `cast <技能序號/名稱>`：指揮當前行動成員施放法術或戰技。
  - `flee`：嘗試遁地逃跑（消耗真元與道心 SAN 值）。

### 3. 城鎮與角色指令
| 指令 | 說明 |
| :--- | :--- |
| `look` | 審視四周環境、房間內生靈與地面物品 |
| `party` | 查看 5 人小隊隊員血量、真元、道心 SAN 值與陣法站位 |
| `inventory` | 檢視個人背包與小隊 40 格共用儲物袋 |
| `shop` | 瀏覽當前房間貨棧商品清單 |
| `buy <編號/ID> [數量]` | 購買靈藥乾糧 |
| `rest` | 於客棧或安全屋打坐調息，回滿氣血與真元 |

---

## ⚠️ 四、常見問題與排查

- **Q1: 執行 `.\test.ps1` 失敗怎麼辦？**  
  請確認目前本機 JDK 版本為 Java 21 以上。專案使用了 Java 21+ 虛擬執行緒（Virtual Threads）與 Pattern Matching 語法。
- **Q2: 進入地牢後畫面一片漆黑？**  
  此為克蘇魯暗黑修仙特色！地牢具備動態暗黑迷霧（Fog of War），未走過的地塊預設不可見。向前邁步（<kbd>W</kbd>）即可探明四周岩壁。
- **Q3: 存檔存放在哪裡？**  
  單機存檔完全為本機 JSON 檔案，存放在專案根目錄的 `saves/slot_*.json`。可隨時手動備份或遷移。

---

## 🚀 五、開發運維與玩家體驗現代化擴充規劃 (Onboarding & Player Experience Roadmap)

為讓新加入的開發者能在 60 秒內完成環境佈署，並讓新進玩家無痛融入克蘇魯暗黑修仙世界，規劃以下現代化體驗升級：

### 1. 現代化開發與容器化 (Modern DevOps & Containerization)
- **Docker 一鍵容器化佈署 (Zero-Config Docker Compose)**：
  - 提供輕量 Multi-stage Build Dockerfile 與 `docker-compose.yml`，封裝 OpenJDK 25 與 Maven，開發者本機即便未安裝 Java 亦能透過 `docker compose up` 瞬間拉起全套遊戲伺服器。
- **VS Code Dev Containers 開箱即用**：
  - 配置 `.devcontainer/devcontainer.json`，自動掛載環境變數、Java 擴充套件與自動化格式化工具，新成員 Clone 即可進入結對編程。

### 2. 玩家體驗與沉浸引導 (Player Experience & Interactive Onboarding)
- **非侵入式互動新手教學導覽 (Interactive Guided Tour)**：
  - 玩家初次進入遊戲時，以修仙畫卷風格提供輕量高亮引導（「點擊此處與掌櫃交談」、「按下 P 鍵檢視陣法」），杜絕硬塞長篇文字手冊導致的認知超載。
- **遊戲內天地異象與求知典籍 (In-Game Mechanics Codex)**：
  - 介面整合「仙道異聞錄」，即時索引已遭遇之妖魔弱點、五行生剋關係與道心異化說明，隨查隨用。

### 3. 跨端掌機與外設適配 (Gamepad & Handheld Console Support)
- **原生 Gamepad API 手柄適配 (Steam Deck / Xbox Controller)**：
  - 完整支援 Gamepad API：左搖桿控制 DRPG 轉向與步進，右搖桿切換集火目標，肩鍵（LB/RB）切換 5 人隊員，手柄震動（Vibration API）反饋被暴擊與罡氣破防之震撼手感。
- **行動端觸控手勢增強 (Touch Gestures)**：
  - 支援滑動轉向（Swipe Turn）與點擊螢幕邊緣前進，完美適配 iPad 與手機平板觸控探索。

### 4. 自動化工程與品質防線 (CI/CD Quality Gate)
- **GitHub Actions 自動化測試矩陣**：
  - 每次 PR 自動在 Linux 與 Windows 雙環境運行 `.\test.ps1`，確保 304 項測試與架構看門狗 100% 綠燈，杜絕回歸破壞。
