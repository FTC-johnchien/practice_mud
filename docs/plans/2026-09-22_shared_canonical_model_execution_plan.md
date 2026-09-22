# 共用 Canonical Model 實施計畫（項目 2–5）

## 給執行 AI 的任務定位

本計畫的目標是讓 MUD 世界模式與 DRPG 小隊模式共用「內容定義、ID 與可持久化狀態」，但**不要求兩種模式共用行為類別或戰鬥迴圈**。兩種模式應保留各自的 command、actor、battle loop 與前端 DTO。

請依本文件的 phase 依序實作；每完成一個 phase 必須先跑該 phase 的測試，再進入下一個 phase。不要把所有 phase 合併為單一大型重構。

### 全程不可違反的規則

1. 靜態資料只有一份 canonical definition；同一物品或技能不得在 MUD、DRPG、UI 各自保存名稱、描述、基礎效果或基礎數值。
2. 執行期可變資料以 instance / state 表達，禁止回寫 template JSON 或 template object。
3. 前端傳輸模型是 projection / DTO，不能成為遊戲規則的來源。
4. `Living` 與 `PartyMember` 不得互相繼承；同步必須是明確的資料流，而非任意雙向複製。
5. Domain / application code 只能依賴 `TemplateReader` port，不能呼叫 `TemplateRepository` static API，也不能自行 `new TemplateCatalog()`。
6. 既有存檔與前端 payload 在過渡期需要能讀取；以 `@JsonAlias`、versioned migration 或專用 mapper 相容，不可靜默遺失背包、裝備或技能。

### 名詞與目標模型

```text
ItemDefinition (immutable JSON definition)
  └── ItemInstance (mutable, persisted runtime state)
        └── ItemView / PartyItemViewDto (read-only client projection)

SkillDefinition (immutable JSON definition; has mud/drpg facets)
  └── CharacterSkillState / PartySkillLoadout (skill ID + progress/state only)
        └── ResolvedSkillView (read-only client/battle projection)

Player --creates--> BattleParticipantSnapshot --produces--> BattleOutcome --applies--> Player
```

---

## Phase 2 — ItemDefinition + ItemInstance + ItemView

### 現況與問題

- `ItemTemplate` 是靜態定義，但 `GameItem` 重複保存 `name`、`description`、`type`、`subType`。
- `PartyItemSlot` 同時充當背包實體、裝備規則、效果計算與 UI DTO。
- `PartyItemSlot.fromItemTemplate()`、`PartyInventory.createFromTemplate()` 有硬編碼效果與 ID 字串 fallback；找不到資料時會產生另一套規則。

### Phase 2.0：先建立測試護欄（只加測試，不改模型）

1. 為物品建立 characterization tests：由每一個現有 `ItemTemplate` 建立 MUD 物品與 DRPG 背包項目，驗證 template ID、數量、裝備欄、耐久與效果資料沒有遺失。
2. 新增負面案例：未知 `definitionId` 必須回傳可識別的失敗（`Optional.empty` / domain exception），不得產生「古仙法物」或依 `pill`、`sword` 等名稱猜測的物品。
3. 為現有存檔 JSON 準備 fixture，至少涵蓋：可堆疊消耗品、武器、護甲、有耐久物品、容器物品、帶 dynamicProps 的物品。

**驗收：** 測試能先描述目前可保留的資料行為；尚未刪除任何 production fallback。

### Phase 2.1：引入 canonical item model

1. 建立 `ItemDefinition`，以目前 `ItemTemplate` 欄位作為唯一靜態來源：`id`、顯示資料、type/subType、stack 規則、equipment、consumable、bonusStats、extraProps。
2. 將 JSON loader、`TemplateReader`、資料完整性測試移到讀取 `ItemDefinition`。如果一次改名造成影響過大，可讓 `ItemTemplate` 暫時作為 deprecated compatibility adapter；不可維護兩份獨立欄位模型。
3. 建立 `ItemInstance`，僅保存：`instanceId`、`definitionId`、`quantity`、`currentDurability`、`dynamicProps`、`contents`。`contents` 也必須是 `ItemInstance`。
4. 建立唯一的 `ItemFactory`（或 `ItemInstanceFactory`）：輸入 `ItemDefinition + quantity + instanceId policy`，輸出 `ItemInstance`。所有產生掉落、商店購買、初始背包、地圖物品都必須改由它建立。
5. 將目前 `GameItem` 過渡為 `ItemInstance`，或以 `GameItem` 作為相容名稱但移除所有從 definition 可推得的副本欄位。不可在不同呼叫點自行複製 template 欄位。

**禁止：** 不可將 template 整個嵌入並持久化到 instance；不可在 item entity 中查 repository。

**驗收：** 同一 `definitionId` 的兩把劍可有不同耐久／詞綴，但名稱、裝備屬性與基礎傷害都由同一 `ItemDefinition` 解讀。

### Phase 2.2：把 PartyItemSlot 降級成 view

1. 建立 `ItemViewMapper`，輸入 `ItemInstance + ItemDefinition`，輸出 `ItemView` 或 `PartyItemViewDto`。
2. View 可以含 icon、名稱、描述、效果摘要、裝備欄與計算後展示數字；它們都不可被戰鬥或 inventory service 當作真實資料寫回。
3. `PartyInventory` 改為持有 `ItemInstance`（或僅持有 instance ID 並由 inventory aggregate 查詢）；`PartyMember.equipment` 也改為 `EquipmentSlot -> ItemInstance/instanceId`。
4. 裝備加成、消耗品效果等規則移到 `ItemUsageService` / `EquipmentService`，由 `ItemDefinition` 解讀；刪除 `PartyItemSlot` 內的預設傷害、預設 icon、`isStackable()` 猜測邏輯。
5. 最後才刪除 `PartyInventory.createFromTemplate()` 的名稱 substring fallback 與 `PartyItemSlot.toGameItem()` 的 static repository 呼叫。若找不到定義，讓操作失敗並記錄 definition ID。

**驗收：**

- `PartyItemSlot` 不再是 entity / persistence source；僅剩 DTO，或已被 `PartyItemViewDto` 取代。
- MUD 與 DRPG 對同一 instance 的數量、耐久、詞綴一致。
- 物品查找失敗不會產生假資料。
- 既有 save fixture 可 migration 後讀回，且 migration 有測試。

---

## Phase 3 — Single SkillDefinition

### 設計決策

`SkillTemplate` 與 `PartyMemberSkill` 的確有模式差異，不能硬合併成巨大 boolean class。正確做法是一個 `SkillDefinition` root 加上可選 facet：

```text
SkillDefinition
  identity: id, name, description, icon, tags
  learning / weapon requirements / scaling
  mud: MudSkillRules?       # moves, counter, combo, MUD cost / mechanics
  drpg: DrpgSkillRules?     # resource cost, cooldown, target, effects
  bridges: SkillBridgeRule[] # 從 MUD skill / level 解鎖哪些 skill ID
```

`drpg` 不存在代表該技能不可作為 DRPG 主動技能；`mud` 不存在代表它是純 DRPG 技能。兩者都存在時仍只有同一組 identity / display metadata。

### Phase 3.0：盤點與測試

1. 列出 `data/global/skills/**` 和 `data/party/party_skills.json` 的所有 ID，確認重名、同義技能及僅限某模式的技能。
2. 為目前 `SkillBridgeService` 的每個 mapping 建立 table-driven test：輸入 MUD skill ID 與等級，驗證解鎖的 DRPG skill IDs。
3. 為 `PartyMember` 建立測試：它持有技能 ID / 等級時，能透過 resolver 得到可施放的 DRPG projection。

### Phase 3.1：資料合併但保持相容

1. 在 `data/global/skills/` 的相應 JSON 加入 `drpg` facet 與 `bridges`，把 `party_skills.json` 的 cost、cooldown、AOE、heal、taunt、stun、武器限制等搬入；不要複製 ID/name/description。
2. 新增 `SkillDefinition`、`DrpgSkillRules`、`SkillEffect`、`SkillBridgeRule` 等小型類型。效果使用可擴充 typed effect，而非再加無限 boolean 欄位。
3. 讓 loader 從 global skills 載入 definition；`findPartySkill` 在過渡期可由 `SkillDefinition.drpg` 產生 `ResolvedPartySkill` compatibility view。
4. 把 `SkillBridgeService.mapMudSkillToDrpgSkills()` 中的硬編碼 if/else 改為讀取 `bridges` 資料。保留舊方法作為 deprecated delegate，直到所有呼叫端已遷移。

### Phase 3.2：角色與戰鬥只保存 ID/state

1. `PartyMember.skills` 改為 `Map<skillId, PartySkillState>` 或 `List<skillId>`；可變 cooldown 保留在 battle state / member state，不能寫入 definition。
2. `DrpgCombatLoop` 以 `SkillResolver` 把 `skillId + PartySkillState + SkillDefinition` 解析成當次施放用的 resolved skill。
3. 前端 DTO 由 resolver 輸出 icon、說明、成本、冷卻，不再序列化一份 `PartyMemberSkill` 實體。
4. 當所有 consumer 都已使用 `SkillDefinition` 後，刪除 `data/party/party_skills.json`、`PartyMemberSkill` template repository map 與其 loader。

**驗收：** 同一 skill ID 的名稱／說明／限制僅存在 global definition；MUD 與 DRPG 都能使用既有技能；bridge mapping 完全由 JSON 驅動並有測試。

---

## Phase 4 — 移除 TemplateRepository static access

### Phase 4.0：先建立依賴圖與替代品

1. 用 `rg` 列出所有 `TemplateRepository.`、`TemplateRepository.getInstance()`、`new TemplateCatalog()`、無參數 constructor fallback。
2. 將呼叫點分為 production bean、domain entity、DTO、純單元測試四類；先從 production bean 開始，不能以 static bridge 當最終解法。
3. 建立 test-only `InMemoryTemplateReader`，可用 builder 註冊 item/skill/mob/room；純單元測試只能使用它，不應啟動 Spring context。

### Phase 4.1：注入唯一的 TemplateReader

1. `TemplateRepository` 改為非 static 的 Spring-managed catalog state；maps 為 instance fields，讀寫 API 為 instance methods。
2. `TemplateCatalog` 若仍有價值，可作為 `TemplateReader` 的唯一 Spring adapter；否則將 repository 直接實作 port。只能保留一個 production implementation。
3. 所有 factory、service、adapter、battle service、DTO assembler 以 constructor injection 取得 `TemplateReader`。
4. 移除 `MudTemplateAdapter`、`DrpgTemplateAdapter`、`SkillBridgeService` 等無參數 constructor 與 `new TemplateCatalog()` fallback。
5. Entity / record 不應保存可注入 service。像 `PartyMember` 的 template lookup 必須搬到 application service / resolver，而非 lazy `getTemplateReader()`。

### Phase 4.2：切斷 static compatibility

1. 先移除所有 production call site 的 static API，並以 architecture test（或 CI `rg` check）禁止新增。
2. 再移除 `TemplateRepository.INSTANCE`、全部 static map、static `find/register/get` delegates。
3. 測試改為注入 `InMemoryTemplateReader` 或 Spring 的 test bean；每個 test 在 setup/teardown 建立自己的資料，避免 static contamination。

**驗收：** production `src/main/java` 中找不到 `TemplateRepository.` static call 或 `new TemplateCatalog()`；兩次完整 Maven test 的執行順序不同也不會互相污染。

---

## Phase 5 — Outcome-based Character Synchronization

### Ownership 契約

| 資料 | Owner | 說明 |
| --- | --- | --- |
| 世界位置、房間、對話、actor lifecycle | `Player` | 只能由 Player actor / world command 改變 |
| 長期等級、XP、基礎屬性、已學技能、持久背包 | `Player` | 戰鬥結束後以 outcome 套用 |
| 陣型、當場目標、仇恨、怒氣、連擊、SAN、cooldown | DRPG battle state / `PartyMember` projection | 僅在 DRPG 戰鬥期存在或持久化為模式專屬狀態 |
| HP/MP、取得物品、技能進度 | `BattleOutcome` | 必須明確定義怎樣折算／套用 |

### Phase 5.0：鎖住舊行為

1. 為目前 `CharacterSyncService` 寫測試，列出它目前同步的每個欄位。
2. 明確標記每個欄位在新 ownership 表的 owner 與同步方向；若沒有 owner，不得直接開始實作。
3. 補 battle integration tests：勝利、失敗、逃跑、死亡、獲得 XP、掉落物、消耗品、HP/MP 變化。

### Phase 5.1：建立 snapshot 與 outcome

1. 建立 immutable `BattleParticipantSnapshot`：從 `Player` 建立 DRPG 隊長初始戰鬥資料；它不是 Player 的共享 mutable `LivingStats` copy。
2. 建立 immutable `BattleOutcome`，至少含 `characterId`、戰鬥結果、HP/MP delta 或最終值、XP、物品增減、技能進度、模式專屬結果。每個欄位要有明確語意，不可塞入整個 `PartyMember`。
3. 建立 `BattleOutcomeApplier` application service：驗證 character ID、合法範圍與 inventory capacity，然後在 Player actor thread 中套用 outcome。
4. `DrpgRewardService` / `DrpgBattleService` 只產生 outcome；不可直接持有或修改 `Player`。

### Phase 5.2：逐步替換 CharacterSyncService

1. 開戰：以 `Player -> BattleParticipantSnapshot` 取代 `syncFromPlayerToParty`。
2. 戰鬥中：只改 DRPG battle state，不同步回 Player。
3. 結算：以 `BattleOutcomeApplier` 取代 `syncFromPartyToPlayer`。
4. 每完成一條 outcome 路徑（HP/MP、XP、物品、技能）就新增 integration test；完成所有路徑後刪除 `copyStats` 與雙向 sync API。

**併發要求：** `BattleOutcomeApplier` 不可在 actor thread 內 `join()` 自己；要以既有 actor mailbox 的非阻塞排程方式執行。每個 outcome 必須具備 battle ID / idempotency key，避免重送造成 XP 或戰利品重複發放。

**驗收：**

- battle service 不再直接 mutate `Player`。
- Player 與 PartyMember 不共享 `LivingStats` reference。
- outcome 重送不會重複發獎。
- 戰鬥中斷、逃跑、死亡等結果都有定義且可測試。

---

## 建議 PR 切分與總驗收

1. PR-A：Phase 2.0 + 2.1（只建立 item definition/instance/factory 與相容 mapper）。
2. PR-B：Phase 2.2（party inventory/equipment 改用 instance，移除 fallback）。
3. PR-C：Phase 3.0 + 3.1（SkillDefinition / JSON facets / bridge data 化）。
4. PR-D：Phase 3.2（移除 party skill duplicate source）。
5. PR-E：Phase 4（DI migration；不與資料模型改造混在同一 PR）。
6. PR-F：Phase 5.0 + 5.1（snapshot/outcome，先與舊 sync 並存）。
7. PR-G：Phase 5.2（切換呼叫點與刪除舊 sync）。

每個 PR 都要執行 `./test.ps1`（Windows 使用 `./test.ps1`），並至少新增相應 unit/integration test。若資料格式或 save format 有變更，必須新增 migration fixture 與 backward-compatibility test。
