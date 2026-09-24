# Claude Opus 專案 Review：2026-09-24 現況與架構評價

> **審查範圍**：`src/main/java`、`src/test`、`src/main/resources`（含 JSON 資料與前端模組）、`docs/plans`、`docs/references`、`ARCHITECTURE.md`、`pom.xml`、既有 Codex / Copilot Code Review。
> **審查原則**：以目前程式碼為事實基準；文件中的設計願景與歷史快照不視為已實作證據。對每項發現標示「已證實缺陷」、「有證據的風險」或「架構改善建議」，並給出可操作的修正方案與相容成本評估。

---

## 一、整體評價與結論摘要

### 1.1 專案優勢 — 做得好的部分

這是一個野心極大且執行力相當出色的個人專案。從程式碼結構來看，以下幾點值得肯定：

1. **雙模架構設計精巧**：MUD 文字拓撲圖（10 方向 Room Graph）與 DRPG 2D 網格迷宮（4 方向 Step-based）雙模並存，由 `CharacterSyncService` 與 `SkillBridgeService` 銜接，概念上清晰。
2. **資料驅動做得紮實**：50+ 門技能、多個城鎮區域、地城迷宮、職業/種族/陣法均以 JSON 定義，由 `TemplateRepository` 統一載入，避免了大量硬編碼。`DataNamespaceIntegrityTest` 提供了資料完整性的自動化防線。
3. **Actor 模型與虛擬執行緒**：以 Java 25 Virtual Threads 實作的 `VirtualActor` 是正確的併發選型，已具備 `isActorThread()` 自死鎖防禦與例外隔離，展現了對併發安全的重視。
4. **前端已完成模組化拆分**：3,120 行巨石 `drpg-view.js` 已拆分為 ES6 Modules（`core/`、`panels/`、`modals/`），降低了前端維護成本。
5. **戰鬥系統解耦到位**：`DrpgBattleService` → `DrpgCombatLoop` + `DrpgEnemyTacticsService` + `DrpgRewardService` + `ComboResolver` 的 Facade 拆分，責任清晰。
6. **累積了 156+ 項自動化測試**：涵蓋資料完整性、戰鬥流程、小隊裝備、併發安全、技能橋接等核心路徑，展現了測試工程的持續投入。

### 1.2 核心風險 — 需要優先處理的問題

在肯定上述成果的同時，以下問題是目前程式碼中**可證實的缺陷或高機率風險**，應在功能擴展之前優先處理：

| # | 問題 | 嚴重度 | 類別 |
| :---: | :--- | :---: | :--- |
| **F-1** | MUD 戰鬥 miss sentinel `-1` 在技能倍率後可變為正傷害 | **P1** | 已證實缺陷 |
| **F-2** | `PartyInventory` 的 substring fallback 靜默猜測未知物品 | **P1** | 已證實缺陷 |
| **F-3** | 堆疊物品忽略 `maxStack`，可無限疊加超過 99 | **P1** | 已證實缺陷 |
| **F-4** | 存檔服務缺少 owner 隔離、slot 範圍校驗與原子寫入 | **P1** | 有證據的風險 |
| **F-5** | 怪物死亡 loot pouch 合併在 Room Actor 外操作，非原子 | **P1** | 有證據的風險 |
| **F-6** | 前端多處 `innerHTML` 插入動態值，未做 XSS 防禦 | **P1** | 有證據的風險 |
| **F-7** | `CharacterSyncService` 只同步部分欄位，文件宣稱「無損同步」 | **P2** | 設計債務 |
| **F-8** | `TemplateCatalog` 仍有 no-arg fallback 到 static singleton | **P2** | 設計債務 |
| **F-9** | SP/Rage/Combo 三資源並存，白皮書宣稱已收斂為三槽 | **P2** | 文件漂移 |
| **F-10** | Dodge/Parry/Block 只有資料掛載，無實際防禦判定 | **P2** | 功能缺口 |

---

## 二、已確認完成、不應重列的項目

與 Codex / Copilot Review 對齊後，以下項目確認已在程式碼中落實，不應再列為待修：

| 項目 | 程式碼證據 |
| :--- | :--- |
| H2 Console 遠端存取限制 | `application-dev.yml` 設 `web-allow-others: false`；prod profile 設 `enabled: false` |
| WebSocket CORS 白名單 | `WebSocketConfig` 讀取 `app.websocket.allowed-origins`，dev 限 localhost |
| `AuthService` 校驗與 Bean 注入 | 註冊流程調用 `validateUsername()`/`validatePassword()`，`PasswordEncoder` 由建構子注入 |
| `VirtualActor` 自死鎖與例外隔離 | `isActorThread()` 判定 + `catch(Throwable)` 隔離已存在 |
| `RoomMessageBuffer` 共用排程 | 已使用全域 daemon scheduler，非 per-room |
| 新手村鑰匙 ID | `rooms.json` 已統一使用 `global:village_elder_house_key` |
| 前端巨石 JS 拆分 | ES6 Modules 架構已完成 |
| 物品 namespace 完整性測試 | `DataNamespaceIntegrityTest` 提供防線 |

---

## 三、逐項發現與建議

### F-1：MUD 戰鬥 miss sentinel `-1` 在技能加成後變為正傷害（P1 · 已證實缺陷）

**證據**：[CombatService.java#L132-L174](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/service/CombatService.java)

`calculateDamage()` 以 `-1` 表示 miss（L143），但呼叫端 `performAttack()` 繼續加上 `skillDmg`（L276-L279）並乘以 `damageMod`（L282），miss 的 `-1` 可被正向技能傷害抵消為正值，最終送入 `target.onDamage()`。

```java
// L273: rawDmg = calculateDamage() → 可能回傳 -1
// L276-279: rawDmg += skillDmg → -1 + 15 = 14
// L282: dmgAmount = (int)(14 * 1.2) = 16
// L323: target.onDamage(16, ...) → miss 卻造成 16 傷害
```

**影響**：命中率機制失效，戰鬥數值失真。

**建議修正**：
```java
int rawDmg = calculateDamage(self, target);
if (rawDmg < 0) {
    // miss: 直接發送閃避訊息並返回，不進入傷害計算
    for (Player receiver : audiences) {
        MessageUtil.send(action.msg().miss(), self, target, receiver);
    }
    return;
}
```

或者更好的做法：將 `calculateDamage` 改為回傳 `CombatResult` 記錄型別，以 `MISS`/`HIT`/`CRITICAL` 等枚舉取代魔術數字。

**相容成本**：低。只影響 `performAttack()` 內部流程，不影響序列化或存檔。

---

### F-2：未知物品 ID 的 substring fallback 掩蓋資料錯誤（P1 · 已證實缺陷）

**證據**：[PartyInventory.java#L127-L138](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/party/model/PartyInventory.java)

`createFromTemplate()` 在找不到模板時，依 `pill`、`talisman`、`sword`、`robe` 子字串產生硬編碼的備援物品，其他 ID 則建立通用「古仙法物」。

**影響**：
- 掉落表、商店、存檔中的物品 ID 錯誤（如打字錯、namespace 缺失）會被靜默吞沒。
- `DataNamespaceIntegrityTest` 保護了 JSON 定義的 namespace 正確性，但 **runtime fallback 繞過了這道防線**。
- 玩家可能拿到與實際定義不符的假物品，卻無任何日誌可追蹤。

**建議修正**：
1. 將 fallback 分支改為回傳帶有原始 ID 的 `UnknownItem` placeholder，並記錄 `WARN` 日誌：
   ```java
   log.warn("Item template not found for ID '{}', creating unknown-item placeholder", templateId);
   return PartyItemSlot.builder()
       .slotId(slotId).itemId(templateId)
       .name("❓ 未知法物 (" + templateId + ")")
       .icon("❓").itemType(ItemType.MISC).count(count)
       .description("模板缺失的物品，請回報開發者。")
       .quality("COMMON").build();
   ```
2. 在 `DataNamespaceIntegrityTest` 中新增一條：啟動完成後掃描所有區域的 mob drops、shop inventory、初始背包，確認 `findItem()` 均回傳 `present`。

**相容成本**：中低。舊存檔中若已有 fallback 物品，需要 migration 或容錯讀取。

---

### F-3：堆疊物品忽略 maxStack，可無限疊加（P1 · 已證實缺陷）

**證據**：[PartyInventory.java#L54-L63](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/party/model/PartyInventory.java)

```java
// L60: 直接加總，未讀取 maxStack
slot.setCount(slot.getCount() + count);
```

**影響**：批量購買、loot merge、背包整合可產生單槽 999+ 的物品堆疊，破壞數值平衡。

**建議修正**：
```java
int maxStack = slot.getMaxStack() > 0 ? slot.getMaxStack() : 99;
int canAdd = maxStack - slot.getCount();
if (canAdd >= count) {
    slot.setCount(slot.getCount() + count);
} else {
    slot.setCount(maxStack);
    int remaining = count - canAdd;
    // 遞迴或迴圈建立新槽位處理溢出
    return addItem(templateId, remaining);
}
```

並補 `98+2`、`99+1`、批量購買 100 的邊界測試。

---

### F-4：存檔服務缺少 owner 隔離、slot 範圍校驗與原子寫入（P1 · 有證據的風險）

**證據**：[SaveGameService.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/save/service/SaveGameService.java)

1. **缺少 owner 隔離**：`listSaveSlots()`（L59）、`readSlotSummary()`（L73）、`deleteSave()`（L273）均不接收 owner 參數，任何連線皆可讀寫全域存檔。
2. **缺少 slot 範圍校驗**：`getSaveFile(int slotId)` 接受任意整數，`slotId = -1` 或 `slotId = 999` 會產生 `slot_-1.json` 或 `slot_999.json`。
3. **非原子寫入**：`objectMapper.writeValue(file, saveData)`（L171）直接寫入正式檔案，程序中斷（OOM、斷電、殺程序）可留下截斷 JSON。
4. **`loadGame` 不核對 owner**：L187 讀取的 `SaveData.playerId` 未與呼叫者比對。

**影響**：
- 目前單機模式下影響較低，但 WebSocket handler 已為每個 session 建立獨立 Player Actor（`p-{sessionId}`），多個瀏覽器分頁同時連線即可重現。
- 中斷寫入可導致存檔永久損壞。

**建議修正**（分兩階段）：

**階段 A — 防禦性加固（低成本）**：
```java
private File getSaveFile(int slotId) {
    if (slotId < 0 || slotId > TOTAL_MANUAL_SLOTS) {
        throw new IllegalArgumentException("Invalid slot ID: " + slotId);
    }
    // ...
}
```

原子寫入：
```java
File tempFile = new File(savesDir, file.getName() + ".tmp");
objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile, saveData);
Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
```

**階段 B — Owner 隔離（需遷移舊存檔）**：
- 路徑改為 `saves/{ownerId}/slot_N.json`。
- 所有 API 增加 `ownerId` 參數，service 層核對 `SaveData.playerId == ownerId`。
- 提供一次性 migration script 將 `saves/` 下的現有存檔移入預設 owner 目錄。

---

### F-5：Loot pouch 合併在 Room Actor 外操作，存在並行競態（P1 · 有證據的風險）

**證據**：[LivingService.java#L161-L181](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/service/LivingService.java)

怪物死亡時在 Room Actor 外部直接讀取 `room.getItems()`、遍歷搜尋 pouch、修改 `targetPouch.getContents()`。多個怪物在同一 Tick 內死亡（MUD CombatRound 由虛擬執行緒併行處理）可導致：
- 兩條執行緒同時判定「地上無 pouch」→ 各自 `dropItem()` → 地面出現兩個儲物袋。
- 兩條執行緒同時對同一 `targetPouch.getContents()` 做 `addAll()` → 交錯修改 ArrayList。

**建議修正**：
將「尋找/建立/合併 pouch」封裝為 Room Actor 的單一訊息操作：
```java
room.tell(new MergeLootPouchMessage(mob, drops));
```
Room Actor 在其單執行緒郵箱內序列處理合併邏輯，徹底消除競態。

---

### F-6：前端多處 innerHTML 插入動態值（P1 · 有證據的風險）

**證據**：
- [save-modal.js](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/modals/save-modal.js)：存檔主角名、樓層、陣法名拼入模板字串。
- [town-panel.js](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/panels/town-panel.js)：NPC 名稱、出口名、物品名拼入 `innerHTML`。
- [party-modal.js](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/modals/party-modal.js)：角色名/職業/陣法資料拼入 HTML。
- [mud-core.js](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/mud-core.js)：ANSI 轉換結果交給 HTML。

**影響**：若主角名稱、存檔標題、NPC 對話等包含 HTML 特殊字元（如 `<img onerror=alert(1)>`），可觸發 DOM XSS。目前主角名稱由前端 `handleNew` 傳入，無 server-side 限制。

**建議修正**：
1. **建立全域轉義工具**（已在 FUTURE_IMPROVEMENTS.md 定義，應立即落地）：
   ```javascript
   export function escapeHtml(str) {
       if (!str) return '';
       return String(str)
           .replace(/&/g, '&amp;').replace(/</g, '&lt;')
           .replace(/>/g, '&gt;').replace(/"/g, '&quot;')
           .replace(/'/g, '&#039;');
   }
   ```
2. **純文字優先用 `textContent`**：NPC 名、物品名等不含格式的動態值改用 `el.textContent = name`。
3. **Server-side 限制**：在 `SaveGameService.handleNew()` 中限制主角名稱長度（≤16 字元）、禁止 `<`, `>`, `"`, `'`, `&` 等特殊字元。

---

### F-7：CharacterSyncService 只同步部分欄位（P2 · 設計債務）

**證據**：[CharacterSyncService.java#L68-L82](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/service/CharacterSyncService.java)

`copyStats()` 只複製等級、XP、HP/MP、五維屬性與自由點數。以下欄位**不在同步契約中**：
- 裝備（Equipment）
- 金幣（Gold）
- 理智值（SAN）
- 戰氣（SP）
- 冷卻時間
- 陣型位置
- 技能配置（由 `SkillBridgeService.syncSkills` 部分處理，但不含被動裝配）

然而 `ARCHITECTURE.md` 與多份 Plan 均以「單一真相源・雙向同步」描述此服務。

**建議**：
1. 建立「欄位 Ownership 表」：明確列出每個欄位在 Player 與 PartyMember 間的 owner 與同步方向。
2. 戰鬥結算改用 immutable `BattleOutcome` VO，只攜帶 HP/MP 變化量、XP 獲得、掉落物品，而非整份 stats 複製。
3. 文件中更新為「部分數值同步」或註明同步範圍。

---

### F-8：TemplateCatalog 的 no-arg 建構子 fallback 到 static singleton（P2 · 設計債務）

**證據**：[TemplateCatalog.java#L34-L36](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/service/TemplateCatalog.java)、[PartyInventory.java#L24-L26](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/party/model/PartyInventory.java)

```java
// TemplateCatalog: no-arg constructor falls back to static singleton
public TemplateCatalog() {
    this(TemplateRepository.getInstance());
}

// PartyInventory: production POJO 自行 new TemplateCatalog()
public PartyInventory() {
    this(DEFAULT_CAPACITY, new TemplateCatalog());
}
```

**影響**：
- Jackson 反序列化 `PartyInventory`（讀檔時）會走 no-arg constructor → `new TemplateCatalog()` → `TemplateRepository.getInstance()`，繞過 Spring DI。
- 測試中難以替換為 in-memory reader，必須依賴全域 static 狀態。
- `PartyInventory` 建構時即會呼叫 `addItem()` 加入預設物品，反序列化時造成重複。

**建議**：依 `2026-09-22_shared_canonical_model_execution_plan.md` 漸進遷移。近期可先用 `@JsonCreator` 配合 `@JacksonInject` 注入 `TemplateReader`，避免 no-arg constructor 被 Jackson 觸發時 fallback。

---

### F-9：SP / Rage / Combo 三資源並存，白皮書宣稱已收斂（P2 · 文件漂移）

**證據**：[DrpgCombatLoop.java#L181-L190](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java)

```java
member.gainSp(15);
// 相容舊資源計數
if (member.getResourceType() == CombatResourceType.COMBO) {
    member.gainCombo(1);
}
if (member.getResourceType() == CombatResourceType.RAGE) {
    member.gainRage(15);
}
```

白皮書宣稱「徹底淘汰各職業自立門戶的零散計數」，但程式碼仍維護三套資源。

**建議**：在 `FUTURE_IMPROVEMENTS.md` 中將 SP/Rage/Combo migration 標記為 `migration-in-progress`，註明現行相容層與目標模型。待被動技能系統完成後再統一清除舊資源分支。

---

### F-10：Dodge / Parry / Block 只有資料掛載，無實際防禦判定（P2 · 功能缺口）

**證據**：
- `BattleEnemy` 與 `PartyService` 會掛載防禦技能。
- `DrpgCombatLoop` 中直接計算傷害並扣血（L237-L238），無任何閃避/招架/格擋判定。
- `CombatService.performAttack()` 中的 dodge/parry 仍為 TODO 註解（L253-L267）。

**建議**：這與 `FUTURE_IMPROVEMENTS.md` 中 Phase 9 的路線一致。建議實作時：
1. 先定義 `DefenseResolver` 服務，接收攻擊者/防禦者/已啟用被動技能，回傳 `DefenseResult`（MISS/DODGE/PARRY/BLOCK/HIT）。
2. 在 MUD `CombatService` 與 DRPG `DrpgCombatLoop` 中統一使用此 resolver。
3. 以可控隨機源（`Supplier<Double>`）注入測試，補 deterministic tests。

---

## 四、架構層面觀察與建議

### 4.1 Lombok 版本不一致

**證據**：[pom.xml](file:///c:/Workspace/my_practice/practice_mud/pom.xml)

```xml
<!-- dependency 宣告 -->
<version>1.18.48</version>

<!-- annotationProcessorPaths -->
<version>1.18.42</version>
```

Lombok 的 dependency 版本 (1.18.48) 與 annotation processor 版本 (1.18.42) 不一致，可能導致編譯期與執行期行為差異。應統一為同一版本。

### 4.2 Domain 層對 Infrastructure 的反向依賴仍然存在

`TemplateCatalog`（domain/service）直接 import `TemplateRepository`（infra/persistence）。`PlayerService` 引用 `AuthService`、WebSocket handler。`Player.java` import `WebSocketSession`（protocol 層）。

這不是需要一次性解決的問題，但建議在每次接觸到這些檔案時，優先提取 Output Port 介面到 domain 層，由 infra 層實作。

### 4.3 CombatService 中的註解掉的日誌與多處空分隔線

[CombatService.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/service/CombatService.java) 中有大量被註解掉的 `log.info()` 呼叫（L46、L51、L58、L71-L75、L141-L143、L162、L173、L192、L278、L280）以及 20+ 行的空分隔線（L115-L123、L179-L188、L337-L359），降低了可讀性。

**建議**：清理所有 `// log.info(...)` 與空分隔塊。若需要偵錯日誌，使用 `log.trace()` 或 `log.debug()`，由 logback 配置控制開關。

### 4.4 `startRound`、`afterAttack`、`processSkillExperience` 疑似死代碼

`CombatService` 中的 `startRound()`（L383）、`processSkillExperience()`（L411）、`afterAttack()`（L444）三個 private 方法在類別內部**無任何呼叫者**。搜尋全專案也無反射或外部呼叫。

**建議**：確認後移除，或標記 `@Deprecated` 並附註保留原因。

### 4.5 建構子中調用虛擬方法與預設資料

`PartyInventory` 的建構子直接呼叫 `addItem()` 加入預設物品（L36-L39）。當 Jackson 反序列化時走 no-arg constructor，會先添加預設物品，再被 `setSlots()` 覆蓋。這意味著：
- 反序列化路徑會建立然後丟棄 4 個預設物品。
- 如果 `setSlots()` 被 Jackson 在 `addItem()` 之前呼叫（屬性順序不確定），則預設物品會被保留並與存檔物品疊加。

**建議**：將預設物品的建立移到工廠方法 `PartyInventory.createDefault()` 中，no-arg constructor 保持乾淨。

---

## 五、與前兩次 Review 的對齊與差異

### 5.1 與 Codex Review 的共識

- **一致同意**：先做可驗收的小步改善，不宜一次導入全新 Canonical Item/Skill Model。
- **一致同意**：TemplateReader 遷移應漸進式，逐批移除 static 呼叫點。
- **補充**：Codex Review 未深入指出 miss sentinel 的具體傷害計算路徑，本次提供了完整的數值推導。

### 5.2 與 Copilot Review 的共識

- **一致同意**：WebSocket 身份邊界與存檔隔離是目前最重要的待修項。
- **一致同意**：loot pouch 合併需封裝為 Room Actor 訊息。
- **補充**：Copilot Review 指出 `pom.xml` 要求 JDK 25 但環境為 JDK 8 導致測試無法執行。本次不重複此環境問題，但提醒**任何宣稱「測試全通過」的文件應附上 CI/CD 實際報告**。

### 5.3 本次獨有的發現

| 獨有發現 | 說明 |
| :--- | :--- |
| Lombok 版本不一致 | dependency 1.18.48 vs annotationProcessor 1.18.42 |
| `CombatService` 三個無呼叫者的 private 方法 | `startRound`、`afterAttack`、`processSkillExperience` |
| `PartyInventory` 建構子在反序列化路徑的副作用 | 預設物品建立可能與存檔物品疊加 |
| `CombinceString` 方法名不符合 Java 命名慣例 | 大寫開頭的 private 方法，且名稱有 typo（`CombineString`） |
| `calculateNextLevelXp` 固定回傳 10 | L370-L372，疑為開發期 placeholder |

---

## 六、建議實作順序

### Phase 0：修正可觀察的玩法缺陷（1-2 天）

1. **修正 miss sentinel**（F-1）：在 `performAttack()` 中 miss 時直接 return。
2. **修正堆疊上限**（F-3）：`addItem()` 尊重 `maxStack`，溢出建新槽。
3. **移除 substring fallback**（F-2）：改為 WARN 日誌 + unknown-item placeholder。
4. **清理 CombatService 死代碼與註解**：移除無呼叫者方法與註解掉的日誌。
5. **統一 Lombok 版本**：`pom.xml` 中 annotation processor 版本對齊 1.18.48。

### Phase 1：加固邊界安全（2-3 天）

1. **存檔 slot 範圍校驗與原子寫入**（F-4 階段 A）。
2. **前端 XSS 防禦**（F-6）：建立 `escapeHtml()` 工具並逐模組套用。
3. **Server-side 輸入限制**：限制主角名稱長度與字元白名單。
4. **Loot pouch 原子化**（F-5）：封裝為 Room Actor 訊息。

### Phase 2：推進 FUTURE_IMPROVEMENTS 路線（接續既定路線）

繼續 `FUTURE_IMPROVEMENTS.md` 中定義的 **Direction A (Phase 9)**：被動技能 Enable 與戰鬥檢定系統。本次 Review 中的 F-10（Dodge/Parry/Block 缺口）將在此階段自然解決。

### Phase 3：漸進式架構改善（持續）

1. 建立 `BattleOutcome` VO 改善 `CharacterSyncService`（F-7）。
2. 逐批遷移 TemplateReader DI（F-8）。
3. 同步文件：標註 SP/Rage/Combo 相容層狀態（F-9）。
4. 更新 `FUTURE_IMPROVEMENTS.md` 中已完成/過時的項目標記。

---

## 七、Review 原則聲明

1. **缺陷、風險、偏好分開標示**：不將理論可能發生的情境直接寫成現有漏洞。
2. **每項建議附帶相容成本評估**：避免「應立即重構」的空泛指示。
3. **尊重既有架構決策**：不為追求「教科書 Clean Architecture」而提出全面重寫。
4. **可驗收性**：每項改善設定明確的完成條件（測試、日誌、程式碼證據），不以「完全解耦」等無法量化的目標結案。

---

> **本文件由 Claude Opus 於 2026-09-24 依據原始碼與既有文件產出，供開發者作為架構決策參考。文件中的建議非直接執行指令，實施前仍應結合最新程式碼狀態與產品優先序進行判斷。**
