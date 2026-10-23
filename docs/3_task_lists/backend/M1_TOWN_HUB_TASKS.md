# 🏛️ M1: 營地機能中樞模組實作進度清單 (Town Hub & Facilities Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md`](../../2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md)  
> **核心職責**：管理純單機安全據點（新手村 / 墨竹前進營地）選單式機能設施（工坊、修煉場、道場、醫療站、傳送門）與休整機制。  
> **當前完成度**：80% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心資料配置**：
  - `src/main/resources/data/zones/newbie_village/facilities.json`（營地選單機能設施與解鎖契約）
- **核心實體與服務**：
  - [`Room.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/actor/impl/Room.java)（過渡期單機空間實體，未來將隨 TownHubService 完全脫離舊 MUD 拓撲）
  - `TownHubService.java`（規劃中：負責加載 facilities.json 與設施業務解析）
- **關聯驗證測試**：
  - [`WorldDataIntegrityTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/WorldDataIntegrityTest.java)（8/8 綠燈）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **M1-FACILITY-DATA (選單機能設施資料化)**
  - 在 `src/main/resources/data/zones/newbie_village/` 落地 `facilities.json`。
  - 定義百煉堂／裝備工坊、賽博晶片實驗室、修煉道場、生化醫療站、地牢傳送門五大機能設施。
- [x] **M1-BROADCAST-PURGE (單機語意純化)**
  - 在 `Room.java` 實裝純單機 `broadcast(String message)` 便捷通道，徹底清剿多人 `broadcastToOthers` 代碼。
- [x] **M1-CAMP-REST (營地一鍵調息休整)**
  - 提供安全區內小隊氣血 (HP)、真元 (MP) 與精神護盾 (SAN) 完全回復能力，解除【精神萎靡】狀態。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task M1-1 (VS-01 前置 / TOWNHUB-SERVICE): 營地服務類別與 DTO 正式建立**
  - **目標**：新建 `TownHubService.java` 與 `TownHubDto.java`，加載 `facilities.json` 並管理設施互動，徹底使營地業務脫離舊 MUD `RoomService.exits` 拓撲。
  - **驗收標準**：`TownHubServiceTest` 通過，提供結構化 DTO 供前端卡片渲染。
- [ ] **Task M1-2 (TOWNHUB-ACTIONS): 設施互動事件派發器收斂 (TOWN_FACILITY / INTERACT_NPC)**
  - **目標**：在 `CommandDispatcher` 註冊 `TOWN_FACILITY`（支援 `facilityId`、`subAction`、`payload`）與 `INTERACT_NPC` 結構化動作，直接調用設施業務（如工坊打造、道場傳功、休整）。
  - **驗收標準**：前端點選設施卡片時，後端正確派發並回傳營地最新全景狀態。
- [ ] **Task M1-3 (COGNITIVE-PROXY-SERVICE-AND-FACILITY-LENSES): 靈識認知轉譯代理服務與營地設施多階透鏡實作**
  - **目標**：實作 `CognitiveProxyService.java`，加載 `facilities.json` 中之三階認知透鏡（`lenses`），依據玩家特工晶片加載度（`<30%` 愚昧求生·純仙俠、`30%~70%` 知見初裂·異象微顯、`>70%` 真理洞察·科技覺醒）動態轉譯 `TownHubDto.facilities` 顯示名稱、說明與 `glitch` 標籤。
  - **驗收標準**：`CognitiveProxyServiceTest` 單元測試覆蓋三階認知切換，驗證初期營地卡片 100% 呈現古典仙俠稱謂與敘事，加載度突破後正確解鎖底層賽博真實解析。
