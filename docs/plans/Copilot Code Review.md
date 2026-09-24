# Copilot 專案 Review：2026-09-24 現況

> 本次 review 重新核對 `src/main`、`src/test`、`docs/plans`、`docs/references`、`ARCHITECTURE.md`、`GEMINI.md` 與 `WALKTHROUGH.md`。判斷以目前程式碼為準；文件中的願景、歷史快照與測試數字不視為已實作證據。

## 結論摘要

最新提交已完成技能／戰鬥模組拆分、SP 與 Combo 初步整合、前端 ES6 模組化，以及多項 Actor、origin 與資料 namespace 防護。這些改善有效降低了舊 review 的部分風險，但沒有解決核心身份邊界：WebSocket 仍以匿名 session 直接建立遊戲 Actor，存檔仍是所有連線共用的固定槽位。

目前最重要的工作順序是：

1. 固定 session、`CharacterId`、owner 與顯示名稱的責任，並封住存檔隔離。
2. 修補動態 HTML 輸出與存檔原子寫入。
3. 修正會直接改變玩法結果的 miss、loot merge、堆疊上限與未知物品 fallback。
4. 定義 Player／PartyMember／Battle state 的欄位 ownership，再處理完整同步。
5. 最後收斂 Dodge／Parry 與 SP、Rage、Combo 的重疊模型，並同步文件。

## Findings

### P1-1：WebSocket 身份仍繞過 Auth，且初始化會讀取全域第一份存檔名稱

**證據：** [MudWebSocketHandler.java](../../src/main/java/com/example/htmlmud/infra/server/MudWebSocketHandler.java#L27-L58) 在連線建立時直接呼叫 `Player.createSinglePlayer`，沒有 authenticated session 或 character identity；建立前還會呼叫 `saveGameService.listSaveSlots()`，取第一個有名稱的全域存檔。`promoteToPlayer` 存在，但不是連線入口的必要流程。

**影響：**多個 WebSocket session 會各自建立 Actor，卻可能共用同一主角顯示名稱；後續 Party、Dungeon、Battle 仍有以名稱作 key 的路徑。這使 Auth 不是安全邊界，也使改名／讀檔可能造成狀態 key 分裂。

**建議：**若產品仍是單機匿名，明確限制單一有效 session 並移除誤導性的多人／Auth 假設；若要支援帳號，先把已驗證的 account／`CharacterId` 放入 handshake/session，再由該 identity 建立 Player。所有 Party、Dungeon、Battle、Save API 都應使用不可變 identity，不應使用顯示名稱。

### P1-2：存檔槽位全域共享，沒有 owner 驗證、slot 邊界或原子寫入

**證據：** [SaveGameService.java](../../src/main/java/com/example/htmlmud/domain/save/service/SaveGameService.java#L42-L75) 固定使用 `saves/autosave.json` 與 `saves/slot_N.json`；`listSaveSlots`、`readSlotSummary` 與 `deleteSave` 不接收 owner，`loadGame` 也不核對 `SaveData.playerId`。`saveGame` 在 [SaveGameService.java](../../src/main/java/com/example/htmlmud/domain/save/service/SaveGameService.java#L159-L176) 直接寫正式 JSON 檔，沒有 temporary file、atomic move 或 per-slot lock。

**影響：**多連線時可讀取、覆寫或刪除他人槽位；並行存檔或程序中斷可能留下截斷 JSON。`slotId` 也由 public service API 直接接受，不能只依賴 command 的輸入提示。

**建議：**所有 save/load/list/summary/delete API 接收 owner identity；集中驗證 `0..5`、核對存檔內 owner，採 `saves/{ownerId}/slot_N.json` 並保留舊檔 migration；以 `Path.normalize`／root containment 防止路徑越界；先寫暫存檔再 atomic move，並以 owner+slot lock 保護並行操作。補兩個 owner 的跨存檔測試與損壞／中斷寫入測試。

### P1-3：多個前端模組仍將伺服器或玩家資料直接插入 `innerHTML`

**證據：** [save-modal.js](../../src/main/resources/static/js/modals/save-modal.js#L100-L108) 將主角、樓層、陣法與時間放入模板；[town-panel.js](../../src/main/resources/static/js/panels/town-panel.js#L105-L212) 將 NPC、出口與物品名稱放入 `innerHTML`；[party-modal.js](../../src/main/resources/static/js/modals/party-modal.js#L467-L500) 將角色／職業／陣法資料放入 HTML。`mud-core.js` 也把 ANSI 轉換結果交給 HTML。

**影響：**主角名稱、存檔標題、資料檔的 NPC／物品文字或伺服器訊息若含 HTML，可形成 DOM XSS。固定 markup 的 `innerHTML` 不等於動態值已安全。

**建議：**純文字改用 `textContent` 或建立 DOM node；保留 markup 時只組固定結構，動態值逐一作文字節點；class、style、URL 使用白名單；用惡意名稱、引號、事件屬性與 ANSI payload 補瀏覽器／單元回歸測試。另在 server 端限制主角名稱長度、控制字元與空白。

### P1-4：MUD miss 的 `-1` sentinel 會在技能加成後變成正傷害

**證據：** [CombatService.java](../../src/main/java/com/example/htmlmud/domain/service/CombatService.java#L130-L145) 的 `calculateDamage` 以 `-1` 表示 miss；[CombatService.java](../../src/main/java/com/example/htmlmud/domain/service/CombatService.java#L271-L306) 接著仍加上技能傷害並乘以招式倍率，最後把正值送進 `target.onDamage`。

**影響：**未命中仍可能造成傷害，命中率、戰鬥訊息與技能數值都會失真。

**建議：**以 `CombatResolution`／明確 `MISS` 結果取代 sentinel；miss 應在技能倍率前直接結束。補可控制隨機來源的 deterministic miss test。

### P1-5：怪物死亡時的 loot pouch 合併跨出 Room actor，非原子

**證據：** [LivingService.java](../../src/main/java/com/example/htmlmud/domain/service/LivingService.java#L161-L181) 在 Room actor 外先讀取 `room.getItems()`、尋找 pouch，再直接修改既有 pouch contents 或呼叫 `room.dropItem`。多個戰鬥 loop／CombatRound 可同時進入此段。

**影響：**同時死亡可能各自建立袋子，或在同一 pouch 上交錯修改，造成地面實體與內容不一致。現有 [ItemPickupAndEntitySyncTest.java](../../src/test/java/com/example/htmlmud/ItemPickupAndEntitySyncTest.java#L164-L232) 主要手動重演合併，未驗證並行流程。

**建議：**把「尋找／建立／合併 pouch」封裝成 Room actor 的單一訊息操作；補並行擊殺、重複死亡通知與搜刮競態測試。

## P2：功能完整性與架構風險

### P2-1：角色同步不是無損同步

[CharacterSyncService.java](../../src/main/java/com/example/htmlmud/domain/service/CharacterSyncService.java#L33-L75) 只複製等級、XP、HP/MP、五維與自由點數，雖另外呼叫技能 bridge，仍沒有在此契約中處理裝備、金幣、SAN、SP、cooldown、陣型或完整技能配置。`GEMINI.md` 與資料驅動計畫則以「單一真相源／雙向同步」描述它。

這會讓進出 DRPG、戰鬥結算與存檔的 ownership 不清。先建立欄位 ownership 表與 immutable `BattleOutcome`，再逐條測試 HP/MP、XP、技能進度、裝備與掉落的套用及 idempotency，不要再擴大整份 mutable stats copy。

### P2-2：行囊堆疊忽略 `maxStack`，可超過 99

[PartyInventory.java](../../src/main/java/com/example/htmlmud/domain/party/model/PartyInventory.java#L47-L68) 對既有堆疊物品直接加總，未讀取 `maxStack`、拆槽或處理剩餘數量。批量購買與 loot merge 因而可能產生非法堆疊。

應依模板的 max stack 分批填入既有槽位，剩餘數量建立新槽位；補 `98+2`、`99+1`、批量購買與掉落合併測試。

### P2-3：未知物品 ID 仍被 substring fallback 靜默猜測

[PartyInventory.java](../../src/main/java/com/example/htmlmud/domain/party/model/PartyInventory.java#L108-L140) 找不到模板時，依 `pill`、`talisman`、`sword`、`robe` 建立假物品，其他 ID 則建立「古仙法物」。這會掩蓋掉落、商店、存檔或 namespace 參照錯誤。

應改為帶 ID 的 `UnknownItem`／可觀測 domain failure；舊存檔 migration 可另行處理，但 production 不應默認猜測。`DataNamespaceIntegrityTest` 已提供資料完整性方向，不應再用 fallback 掩蓋失敗。

### P2-4：Dodge／Parry／Block 目前是資料綁定，不是實際防禦判定

[BattleEnemy.java](../../src/main/java/com/example/htmlmud/domain/dungeon/battle/BattleEnemy.java#L88-L132) 與 [PartyService.java](../../src/main/java/com/example/htmlmud/domain/party/service/PartyService.java#L245-L250) 會掛載防禦技能，但 [DrpgCombatLoop.java](../../src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java#L210-L245) 仍直接計算並扣血；[CombatService.java](../../src/main/java/com/example/htmlmud/domain/service/CombatService.java#L250-L264) 的 dodge/parry 仍是 TODO。

文件已描述精力消耗、招架與破防，現行戰鬥卻沒有同等契約。先用單一 resolver 補 deterministic miss/dodge/parry tests，再接入 stamina／poise；在完成前不要把防禦技能宣稱為已實裝。

### P2-5：TemplateReader DI 仍有 static／no-arg fallback

[PartyInventory.java](../../src/main/java/com/example/htmlmud/domain/party/model/PartyInventory.java#L21-L33) 與 `TemplateCatalog` 的無參數建立會讓 production 物件自行選擇資料來源；Template repository 仍保留 static 相容入口。這是初始化順序、測試污染與資料追蹤風險，不是本次最急迫的安全漏洞。

依 [2026-09-22_shared_canonical_model_execution_plan.md](./2026-09-22_shared_canonical_model_execution_plan.md) 先注入 `TemplateReader`，建立 in-memory reader，再逐批移除 static caller；不要一次刪除相容 API。

## P3：文件與測試治理

### P3-1：SP、Rage、Combo 的現行程式與白皮書不一致

最新提交的白皮書宣稱統一 HP/MP/SP，但 [DrpgCombatLoop.java](../../src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java#L181-L195) 仍累積 SP，並依 `ResourceType` 同時累積 Rage 或 Combo；`PartyMember`、`CombatResourceType` 與前端也仍保留舊資源。這是 migration 未完成，不應在文件中寫成已完成的三槽模型。

請明確標註現行相容層與目標模型，定義存檔版本、讀寫轉換與單一成本來源，再移除舊資源。

### P3-2：`docs/plans` 與 `docs/references` 有過時數字和 6 人描述

目前程式的 [Party.java](../../src/main/java/com/example/htmlmud/domain/party/model/Party.java#L20-L21) 上限是 5，但 `2026-09-18_data_driven_architecture_and_entity_relations.md`、`FUTURE_IMPROVEMENTS.md` 與部分前端註解仍寫 6 人；`docs/references/README.md`、`GEMINI.md`、`WALKTHROUGH.md` 也固定寫不同的測試／技能數字。這些數字會誤導後續實作與 review。

應改成「以 CI 報告與資料掃描為準」，或明確標註歷史快照；移除 6 人規格殘留。文件中的設計草案也應標記 `current`、`target` 或 `historical`。

### P3-3：本次測試無法執行，不能宣稱基準通過

已執行 `mvnw.cmd test`，Maven 在 compile 階段因目前環境是 Temurin Java 8，而 `pom.xml` 要求 `release 25`，以 `invalid target release: 25` 結束。這是環境阻塞，不是測試通過；安裝並選用 JDK 25 後應重新執行完整 suite，再保存實際 Surefire 統計。

## 已確認不應重列為現行 P1

- H2 console 遠端存取已由 `application-dev.yml`／production profile 限制。
- WebSocket origin 已改為設定白名單，不再是 `*`。
- `AuthService.register()` 已使用驗證與注入的 `PasswordEncoder`。
- `VirtualActor` 的 self-deadlock、例外隔離與 Room shared scheduler 已有修復及測試。
- 物品 namespace 完整性已有 `DataNamespaceIntegrityTest` 防線。
- Party 執行時上限已是 5；目前剩餘的是文件與註解漂移。
- 前端巨石已拆成 ES6 模組；後續 review 應針對各模組的輸出 context，不應再以「尚未拆分」列缺陷。

## 建議實作順序

### Phase 0：建立身份與可重現基準

- 決定匿名單機或 authenticated account 模式，並記錄 session、`CharacterId`、player ID、display name 的生命週期。
- 使用 JDK 25 執行完整 `mvnw test`，以 Surefire 實際結果作基準。
- 建立 WebSocket smoke test，驗證連線 identity、重連、讀檔與 disconnect。

### Phase 1：封住存檔與輸出邊界

- 實作 owner-scoped save API、slot validation、migration、atomic write 與跨 owner 測試。
- 將 save、town、party、bag 與 ANSI rendering 的動態值改為安全 DOM 建構。
- 對名稱、標題、描述加入 server-side input constraints 與回歸 payload。

### Phase 2：修正可觀察的玩法與資料錯誤

- 修正 miss sentinel、loot pouch actor 原子性與 99/maxStack 堆疊。
- 移除未知物品的 substring fallback，讓錯誤 ID 帶出明確診斷。
- 將整合測試接到真實 command/service path，不以手動修改資料結構代替行為測試。

### Phase 3：收斂戰鬥與同步契約

- 建立 Player／PartyMember／Battle state ownership 表與 `BattleOutcome`。
- 以 deterministic tests 完成 Dodge／Parry／Block／Stamina，再整理 SP、Rage、Combo migration。
- 確認勝利、失敗、逃跑、死亡、掉落、技能 XP 與重複結算的 idempotency。

### Phase 4：漸進式解耦與文件同步

- 逐批把 production template lookup 改為注入 `TemplateReader`，最後才移除 static facade。
- 更新 `docs/plans`、`docs/references`、`GEMINI.md` 與 `WALKTHROUGH.md` 的 current/target 狀態；不再硬編測試數字。

## 最終判斷

專案的資料驅動與模組化方向已比 2026-09-23 明顯前進，舊 review 中的 H2、origin、Actor scheduler、namespace 與前端巨石問題不應重做。現在真正需要先處理的是 identity／save ownership 與輸出安全；同時修掉 miss、loot merge、stack cap 和 unknown-item fallback 這些可直接影響遊戲結果的缺陷。大型 Canonical Model 或 Clean Architecture 重構應延後，直到上述契約有測試保護且文件已區分現行與目標模型。
