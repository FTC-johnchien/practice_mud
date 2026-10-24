# 📡 M9: 狀態管線與事件協定模組實作進度清單 (State Pipeline Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/backend/M9_STATE_PIPELINE_AND_EVENTS.md`](../../2_specifications/backend/M9_STATE_PIPELINE_AND_EVENTS.md)  
> **核心職責**：管理純單機 WebSocket 連線接管、結構化 Action 派發 (`CommandDispatcher`)、全景投影 DTO (`DrpgStateDto`) 與事件廣播。  
> **當前完成度**：90% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心網路與分發服務**：
  - [`MudWebSocketHandler.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/pipeline/MudWebSocketHandler.java)（純單機連線管理與重複登入優雅接管）
  - [`CommandDispatcher.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/pipeline/CommandDispatcher.java)（結構化動作命令中央派發器）
  - [`DrpgStateDto.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/dungeon/DrpgStateDto.java)（單一全景狀態契約鏡像）
- **關聯驗證測試**：
  - [`SessionTakeoverAndSingletonProtagonistTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/SessionTakeoverAndSingletonProtagonistTest.java)（連線接管 100% 綠燈）
  - [`NetworkSecurityAndPersistenceDrainTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/NetworkSecurityAndPersistenceDrainTest.java)（網路安全與生命週期 100% 綠燈）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **M9-PKG-ENCAPSULATION (Package-by-Feature 獨立封裝 / Phase 3 Step R3.9)**
  - 建立一級套件 `com.example.htmlmud.pipeline`，將狀態廣播服務 `GameStateBroadcastService`、指令分發核心 `CommandDispatcher`、分發埠 `CommandDispatcherPort`、網路處理器 `MudWebSocketHandler` 與會話註冊表 `SessionRegistry` 統一收斂至獨立套件，全專案 393 項測試 100% 綠燈通過。
- [x] **M9-SESSION-TAKEOVER (多開分頁優雅接管)**
  - 當玩家在多瀏覽器分頁開啟連線時，後端自動將唯一主角代理接管至新連線，踢除舊連線，杜絕存檔覆寫競爭。
- [x] **M9-DRPG-DTO-PIPELINE (全景單一狀態契約)**
  - 封裝 `DrpgStateDto`：集中推送第一人稱地牢視角、小隊狀態、40 格行囊、戰鬥擂台與日誌，消除零碎通道。
- [x] **M9-SINGLE-PROTAGONIST-REF (房間唯一主角引用)**
  - `Room` 實體改為單一主角引用，徹底除役多人廣播遍歷通道。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task M9-1 (ACTION-PROTOCOL-REFACTOR): 結構化 Action Protocol 前後端契約全域收斂**
  - **目標**：所有前後端互動 100% 收斂為 JSON 格式，全面實作與支援 [`M9_STATE_PIPELINE_AND_EVENTS.md`](../../2_specifications/backend/M9_STATE_PIPELINE_AND_EVENTS.md) 第三節定義之標準動作（`MOVE`、`TURN`、`INTERACT`、`EXTRACT`、`CAST_SKILL`、`FORMATION_FINISHER`、`TOWN_FACILITY`、`INTERACT_NPC`、`EQUIP`、`UNEQUIP`、`USE_ITEM`、`ALLOCATE_POTENTIAL`、`SAVE_GAME`、`LOAD_GAME`），徹底除役殘留的字串指令拼接。
  - **驗收標準**：WebSocket 僅接收與派發型別安全的 ActionCommand，單元測試覆蓋率 100%。
- [ ] **Task M9-2 (DRPG-WEBSOCKET-HANDLER-RENAME): 傳輸層類別除役舊 MUD 命名並重構為 DrpgWebSocketHandler**
  - **目標**：徹底消除外部 AI 審查反覆提出之「MUD 歷史遺留命名混淆」問題，將 `MudWebSocketHandler.java` 安全重構更名為 `DrpgWebSocketHandler.java`，同步更新 Spring WebSocket 路由配置、前端連線註解與測試類別。
  - **驗收標準**：
    1. 類別名稱正式由 `MudWebSocketHandler` 更名為 `DrpgWebSocketHandler`，完全清除 `com.example.htmlmud.pipeline` 中的 MUD 字樣。
    2. 全專案 395 項測試維持 100% 綠燈零回歸通過。
    3. L2《`M9_STATE_PIPELINE_AND_EVENTS.md`》與相應手冊代表類別名稱同步更新。
