# 🏛️ M1: 營地機能中樞模組實作進度清單 (Town Hub & Facilities Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md`](../../2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md)  
> **核心職責**：管理純單機安全據點（新手村 / 墨竹前進營地）選單式機能設施（工坊、修煉場、道場、醫療站、傳送門）與休整機制。  
> **當前完成度**：85% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心資料配置**：
  - `src/main/resources/data/zones/newbie_village/facilities.json`（營地選單機能設施與三階靈識透鏡契約）
- **核心實體與服務**：
  - [`CognitiveProxyService.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/town/CognitiveProxyService.java)（靈識認知轉譯代理服務）
  - [`TownHubService.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/town/TownHubService.java)（營地機能中樞服務）
  - [`TownHubDto.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/town/TownHubDto.java)（營地狀態傳輸投影）
  - [`FacilityDto.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/town/FacilityDto.java)（設施狀態資料傳輸模型）
  - [`PerceptionTier.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/town/PerceptionTier.java)（三階靈識認知位階枚舉）
  - [`TownFacilityCommand.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/application/command/impl/TownFacilityCommand.java)（營地機能設施與調息指令派發器）
  - [`Room.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/actor/impl/Room.java)（過渡期單機空間實體，營地業務已由 TownHubService 完全接管）
- **關聯驗證測試**：
  - [`CognitiveProxyServiceTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/CognitiveProxyServiceTest.java)（3/3 綠燈）
  - [`TownHubServiceTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/TownHubServiceTest.java)（3/3 綠燈）
  - [`TownFacilityCommandTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/TownFacilityCommandTest.java)（5/5 綠燈）
  - [`WorldDataIntegrityTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/WorldDataIntegrityTest.java)（8/8 綠燈）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **M1-PACKAGE-BY-FEATURE (模組 M1 套件獨立封裝)**
  - 依循依賴拓撲完成 M1 套件封裝，建立 `com.example.htmlmud.town` 獨立套件，將 `TownHubService`, `CognitiveProxyService`, `TownHubDto`, `FacilityDto`, `PerceptionTier` 歸整至新套件。

- [x] **M1-FACILITY-DATA (選單機能設施資料化)**
  - 在 `src/main/resources/data/zones/newbie_village/` 落地 `facilities.json`。
  - 定義百煉堂、靈台藏經閣、雜役演武道場、宗門外門集市、回春客棧、廢棄礦脈坑道六大機能設施與三階認知透鏡（`lenses`）。
- [x] **M1-BROADCAST-PURGE (單機語意純化)**
  - 在 `Room.java` 實裝純單機 `broadcast(String message)` 便捷通道，徹底清剿多人 `broadcastToOthers` 代碼。
- [x] **M1-CAMP-REST (營地一鍵調息休整)**
  - 提供安全區內小隊氣血 (HP)、真元 (MP) 與精神護盾 (SAN) 完全回復能力，解除【精神萎靡】狀態。
- [x] **Task M1-1 (TOWNHUB-SERVICE): 營地服務類別與 DTO 正式建立**
  - 新建 `TownHubService.java` 與 `TownHubDto.java`，加載 `facilities.json` 並管理設施互動，徹底使營地業務脫離舊 MUD `RoomService.exits` 拓撲。
  - `TownHubServiceTest` 通過，提供結構化 DTO 供前端卡片渲染。
- [x] **Task M1-2 (TOWNHUB-ACTIONS): 設施互動事件派發器收斂 (TOWN_FACILITY / CAMP_REST)**
  - 在 `CommandDispatcher` 註冊 `TownFacilityCommand`（支援 `TOWN_FACILITY`、`CAMP_REST`、`hub`、`facility <id>` 等動作），直接調用設施業務（如工坊打造、道場傳功、休整）。
  - 前端點選設施卡片時，後端正確派發並回傳營地最新全景狀態；`TownFacilityCommandTest` 100% 綠燈。
- [x] **Task M1-3 (COGNITIVE-PROXY-SERVICE-AND-FACILITY-LENSES): 靈識認知轉譯代理服務與營地設施多階透鏡實作**
  - 實作 `CognitiveProxyService.java`，加載 `facilities.json` 中之三階認知透鏡（`lenses`），依據玩家特工晶片加載度（`<30%` 愚昧求生·純仙俠、`30%~70%` 知見初裂·異象微顯、`>70%` 真理洞察·科技覺醒）動態轉譯 `TownHubDto.facilities` 顯示名稱、說明與 `glitch` 標籤。
  - `CognitiveProxyServiceTest` 單元測試覆蓋三階認知切換，初期營地卡片 100% 呈現古典仙俠稱謂與敘事，加載度突破後正確解鎖底層賽博真實解析。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task M1-4 (NPC-DIALOGUE-TREE-AND-QUESTS): NPC 劇情對話樹、狀態模型與委託交互管線**
  - **目標**：依據 L2 規格《[`M1_TOWN_HUB_AND_FACILITIES.md`](../../2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md)》第三節，實裝 `dialogues.json` 對話樹結構（採礦長老·鐵手、外門集市遊商·瘸狼等 NPC），建立 `DialogueStateDto`、對話選項跳轉管線與任務分發/交付契約。
  - **驗收標準**：
    1. 在 `src/main/resources/data/hub/dialogues.json` 或 `zones/newbie_village/dialogues.json` 落地 NPC 結構化對話樹資料。
    2. 後端提供 NPC 對話查詢與選項互動介面（支援 `INTERACT_NPC` 指令），組裝並回傳 `DialogueStateDto` 納入 `DrpgStateDto`。
    3. 對話文本經由 `CognitiveProxyService` 進行表層仙俠化與漸進真相解密。
    4. 撰寫 `DialogueServiceTest` 單元測試驗證對話分支跳轉與任務交付邏輯。

