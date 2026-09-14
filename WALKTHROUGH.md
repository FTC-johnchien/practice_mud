# Practice MUD 專案重構與架構升級總結報告 (Walkthrough)

本文件詳盡記錄本次針對 `com.example.htmlmud` 專案所進行之**資源資料重構**、**高併發領域架構修復**、**關鍵 Bug 排除**、**測試自動化驗證**以及**未來架構演進藍圖**。

---

## 1. 核心變更總覽 (Summary of Changes)

```mermaid
graph TD
    A[舊有雜亂資源檔<br/>散落各處/定義不一] -->|階層化規範與清理| B[標準化 data/ 資源庫]
    B --> C[4 大區域 Zones: newbie, mozhu, snow, silverleaf]
    B --> D[技能系統: 35 個標準 JSON & 5 大種族]
    
    E[領域模型與併發隱患] -->|Actor 封裝與線程安全| F[Living 戰鬥狀態安全封裝]
    E -->|Write-Behind 抽象提取| G[AbstractAsyncBatchPersistenceService]
    E -->|領域運算與排程修正| H[WorldPulse / ResourceType / EquipmentProp 修復]
    
    C --> I[TemplateRepository 啟動完整性校驗 validate()]
    D --> I
    I --> J[13/13 單元與整合測試綠燈 BUILD SUCCESS]
```

---

## 2. 資源資料層重構 (Data & Resource Refactoring)

### 2.1 區域與地圖拓撲修復
- **多區域動態載入**：啟用並支援 4 大核心區域：
  - `newbie_village` (新手村)：修正破損地圖拓撲，補齊 `inn`（村莊客棧）與 `the_dark_path` 連線。
  - `mozhu_mines` (墨竹礦坑)：8 個礦坑洞穴、高難度怪物與專屬掉落表、任務監聽器。
  - `snow` (雪亭鎮)：補齊鐵匠鋪武器 `heavy_hammer`（沉重鐵鎚），支援非前綴 ID 映射。
  - `silverleaf` (銀葉村)：修復舊格式相容性（`MobKind.HOSTILE` $\to$ `AGGRESSIVE`/`BOSS`、`ItemType.KEY` / `TRASH` $\to$ `KEY_ITEM`/`MISC`、`EquipmentSlot.TRINKET` 部位擴充）。
- **啟動完整性校驗 (`TemplateRepository.validate()`)**：
  - 開機自動掃描全地圖房間所有 Exit，杜絕懸空出口（Dangling Exits）。
  - 自動校驗怪物初始裝備槽位是否存在於物品庫。
  - 自動偵測並告警缺失之基礎武學定義。

### 2.2 技能與種族標準化
- **技能庫階層分類**：
  - 整理歸類至 `data/global/skills/common`、`unarmed`、`mobs`。
  - 補齊怪物天生攻擊（`mob_bite`, `mob_claw`, `mob_smash`, `mob_sting`, `mob_tail_swipe` 等 10 種天生技）。
- **種族設定標準化 (`races.json`)**：
  - 整合為 `human`、`wolf`、`rat`、`humanoid`、`undead`。
  - 清理重複與無效之草稿檔（`races_1.json`, `races_2.json`, `classes_2.json`）。

---

## 3. 領域模型與併發架構重構 (Architecture & Concurrency)

### 3.1 `Living` Actor 狀態安全封裝
- **問題**：`isInCombat`, `combatTargetId`, `nextAttackTime` 原為 `public` 可變欄位，多個虛擬線程在未經 Actor 郵箱時直接賦值，引發資料競爭（Data Race）。
- **重構**：
  - 將欄位降級為 `protected volatile`。
  - 提供線程安全的封裝行為方法：`enterCombat(targetId)`、`exitCombat()`、`setNextAttackTime(long)`。
  - 同步重構呼叫端 `CombatService`、`LivingService`、`KillCommand`。

### 3.2 抽象 Write-Behind 批次非同步存檔機制
- **問題**：`PlayerPersistenceService` 與 `RoomPersistenceService` 各自維護重複的虛擬線程死迴圈、Queue 輪詢與 Batch 計數邏輯（~150 行重複代碼）。
- **重構**：
  - 抽取泛型抽象基類 [`AbstractAsyncBatchPersistenceService<T>`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/infra/persistence/service/AbstractAsyncBatchPersistenceService.java)。
  - 統一封裝：
    - Virtual Thread 存檔背景工作者。
    - 雙觸發機制：緩衝區滿（50~100 筆）立即寫入，或超時（500ms）定期 Flush。
    - `@PreDestroy` 優雅停機（Graceful Shutdown）：清空佇列中所有待存檔項目，防止伺服器重啟時遺失進度。

### 3.3 嚴重領域邏輯 Bug 修復
1. **`ResourceType.COIN` 致命錯字**：
   - 原代碼：`s.setAge(s.getAge() - val)`，扣除金幣時竟扣到**年齡**！
   - 修復：更正為 `s.setCoin(s.getCoin() - val)`。
2. **`EquipmentProp.getDamageSource()` 參數倒置**：
   - 原代碼：`hitRate` 參數誤傳入 `this.attackSpeed`。
   - 修復：更正為精確傳遞 `this.hitRate`。
3. **`WorldPulse.java` 怪物重生除法優先級錯誤**：
   - 原代碼：`(currentTick % 10 % respawnTime == 0)`，因為模除由左至右結合，導致每 10 tick（1 秒）就判定重生！
   - 修復：修正為獨立週期計時或正確的括號運算。
4. **`SkillService.java` 怪物招架與閃避 ID 不一致**：
   - 修復：由 `"mob_dodge"` 更正為標準 ID `"mob_basic_dodge"` 與 `"mob_basic_parry"`。

---

## 4. 驗證與測試結果 (Verification & Test Suite)

- **測試環境隔離**：
  - 建立 [`src/test/resources/application.yml`](file:///c:/Workspace/my_practice/practice_mud/src/test/resources/application.yml)，測試時自動使用獨立記憶體資料庫 `jdbc:h2:mem:testdb`，徹底擺脫本機實體檔案 `muddb.mv.db` 損壞或鎖定問題。
- **測試覆蓋**：
  - `HtmlmudApplicationTests`: Spring Boot Context 啟動與資料庫連線驗證。
  - `MozhuMinesIntegrationTest`: 墨竹礦坑場景動態載入、怪物生成、Boss 戰鬥與任務掉落驗證。
  - `WorldDataIntegrityTest`: 4 大區域全部載入驗證、全圖無懸空出口拓撲驗證、自然攻擊與技能映射驗證、`COIN` 與 `EquipmentProp` Bug 修復迴歸測試。

```
[INFO] Results:
[INFO] 
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 5. 專案現狀總結與資深工程師發展建議 (Project Status & Roadmap)

### 5.1 現狀架構評估
| 維度 | 現狀評級 | 說明 |
| :--- | :---: | :--- |
| **並發模型** | **優良 (A-)** | 基於 Java 21+ Virtual Threads 與自研輕量 Actor 郵箱，兼具極高吞吐與低資源消耗。 |
| **資料持久化** | **健全 (B+)** | 具備 Write-Behind 批次異步落盤能力，大幅降低主線程資料庫 I/O 延遲。 |
| **世界拓撲與資源** | **完備 (A)** | 4 大區域、37 間房間、25 種怪物、89 件物品、35 門武學與 5 大種族全數標準化載入。 |
| **網路與協議** | **待深化 (B-)** | 目前依賴純文字/簡易 JSON 封包與 Spring WebMVC / WebSocket，缺少壓縮與二進位序列化機制。 |
| **腳本與動態邏輯** | **起步階段 (C+)** | 任務與怪物行為大多硬編碼在 Java 服務中，缺乏熱更新腳本引擎。 |

---

### 5.2 推薦未來發展路線圖 (Roadmap)

#### 階段一：戰鬥狀態機 (FSM) 與技能連段引擎 (Combat FSM & GCD Engine)
1. **全域冷卻時間 (GCD) 與技能施法佇列**：
   - 目前戰鬥主要依賴 `WorldPulse` 或 `nextAttackTime` 輪詢，建議改為**基於事件排程器 (HashedWheelTimer / ScheduledExecutor)** 的主動回呼架構。
   - 導入「吟唱 (Cast) $\to$ 導引 (Channel) $\to$ 後搖 (Recovery)」的嚴謹戰鬥狀態機。
2. **Combo 連招系統**：
   - 充分利用 `ResourceType.CHARGE` 與 `COMBO`，建構門派連招樹（例如：太極拳「起勢 $\to$ 攬雀尾 $\to$ 單鞭」觸發破防增傷）。

#### 階段二：動態腳本與任務引擎 (Scripting & Quest Engine)
1. **引入輕量級動態語言 (Lua 或 Groovy / JavaScript via GraalVM)**：
   - 將 NPC 對話、奇遇事件、副本機關從 Java 硬編碼抽離為腳本檔案。
   - 範例：進入某些特殊房間或觸碰機關時，直接執行 `scripts/traps/crystal_trap.lua`，實現服務不重啟即可線上熱更新地圖謎題。
2. **行為樹 (Behavior Tree) 賦予怪物 AI**：
   - 讓精英怪與 Boss 具備智慧判斷：血量低於 30% 自動施展「金鐘罩」或召喚衛兵，不再只是站樁普攻。

#### 階段三：高併發網路優化與二進位協議 (Protobuf / FlatBuffers)
1. **協定降載 (Network Protocol Optimization)**：
   - 當千人同服或同房間戰鬥廣播時，純文字或冗長 JSON 字串會造成極大的頻寬與 GC 壓力。
   - 導入 **Protobuf** 封包或差異更新廣播（Delta Broadcast）：只推播發生改變的血量、坐標或狀態，減少 70% 以上網路 Payload。
2. **Backpressure (背壓控制)**：
   - 為客戶端 WebSocket 連線建立流量監控，慢速網路用戶端不應拖垮 Actor 訊息佇列。

#### 階段四：分片與叢集架構 (Zone Sharding & Distributed Actors)
1. **房間 Actor 的透明分片 (Transparent Sharding)**：
   - 透過區域或哈希路由將 `RoomActor` 映射至叢集節點（可參考 Pekko Cluster 或自研 Redis-based 路由表），實現單一世界容納萬人並發遊玩之現代大型 MUD 架構。
