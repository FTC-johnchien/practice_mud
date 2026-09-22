整體方向是對的：`FUTURE_IMPROVEMENTS.md` 已明確提出「Canonical Schema + Adapter」，但目前實作仍停在「資料來源集中、執行期再複製一份」的階段。真正該收斂的是「定義、實例、前端投影」三者的責任。

最優先建議處理物品與技能，因為它們現在仍有多套可變資料。

- 物品定義：`ItemTemplate`
- MUD 執行期實例：`GameItem`
- DRPG 背包／UI／戰鬥資料：`PartyItemSlot`

其中 `PartyItemSlot` 同時保存名稱、描述、類型、裝備欄、效果、傷害、防禦、SAN 等模板資料，又帶有 `slotId`、數量等實例資料，等於把 `ItemTemplate + GameItem + UI DTO` 混在同一個類別裡。[PartyItemSlot.java](/C:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/party/model/PartyItemSlot.java:18)

這使得轉換雖然「能用」，卻不是真正的單一真相來源：

- `PartyItemSlot.fromItemTemplate()` 內含藥水、護甲、武器的預設數值與效果推論。
- `PartyInventory.createFromTemplate()` 找不到模板時，會依 ID 字串產生另一套 fallback 物品規則。
- `PartyItemSlot.toGameItem()` 直接呼叫 infrastructure 的靜態 `TemplateRepository`。

我會改成下面的結構：

```text
ItemDefinition（唯一靜態 JSON）
    └── ItemInstance（唯一執行期資料）
          ├── 世界背包／地面掉落
          └── 小隊背包／裝備欄

ItemView / PartyItemView（只給前端，不存業務真相）
```

`ItemInstance` 只需有 `instanceId`、`definitionId`、`quantity`、耐久、詞綴、容器內容等「會變」的資料；名稱、效果、裝備屬性全由 `ItemDefinition` 依 `definitionId` 取得。如此 MUD 與 DRPG 都用同一個物品實例模型，小隊只是不同的 inventory owner 與顯示方式。

具體來說：

- 保留 `ItemTemplate`，但建議改名 `ItemDefinition`，強調 immutable definition。
- `GameItem` 縮減成純 `ItemInstance`；不要再複製 `name`、`description`、`type`、`subType`。
- 將 `PartyItemSlot` 改為 `PartyItemViewDto`，由 mapper 組合 `ItemInstance + ItemDefinition` 後輸出。
- 收斂所有建構邏輯為一個 `ItemFactory` / `ItemAssembler`；移除 `PartyItemSlot`、`PartyInventory` 裡的魔術數字和 ID 字串 fallback。

技能也有相同狀況。MUD 的 [SkillTemplate.java](/C:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/model/template/SkillTemplate.java:15) 與 DRPG 的 [PartyMemberSkill.java](/C:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/party/model/PartyMemberSkill.java:14) 分別有 ID、名稱、描述與效果；`data/party/party_skills.json` 也因此成為第二套技能定義。`SkillBridgeService` 雖然是好的過渡方案，但它是在同步兩份模型，而不是消除重複。

建議將它改成：

```text
SkillDefinition
  - identity / display / tags
  - requirements / costs / cooldown
  - effects（damage, heal, control, resource change…）
  - modeRules.mud
  - modeRules.drpg
```

小隊成員、職業、怪物只保留 `skillIds` 或 loadout；MUD／DRPG 分別透過 executor 或 projection 解讀同一份技能定義。兩種模式不必硬共用戰鬥演算法，但應共用技能「內容定義」。

資料檔方面，我會訂三條不可妥協的規則：

1. 全域定義只放在 `data/global/`；zone 只能引用 global ID，或明確宣告 `localItems`／`overrides`。
2. 所有 ID 使用完整 namespace，例如 `global:taiyin_pill`、`taiyin_tomb:tomb_key`，禁止 repository 以「去掉 namespace 再猜一次」的隱式查找。
3. 遊戲啟動前跑 schema 與 referential-integrity validation，重複 ID、失效掉落物、失效鑰匙、技能 ID 都直接失敗。

目前確實有可驗證的資料一致性問題：全域物品已使用 `village_elder_house_key`，但新手村房間仍引用 `village_elder_key`。[rooms.json](/C:/Workspace/my_practice/practice_mud/src/main/resources/data/zones/newbie_village/rooms.json:159) 此外 `snow/items.json` 的 `heavy_hammer` 在同一資料來源出現兩次。這類錯誤應由資料完整性測試攔住，而不是靠執行期 fallback 掩蓋。

程式架構上，下一個應收斂的是模板讀取。`TemplateRepository` 雖已是 Spring component，卻同時保留 static singleton、static map 與 static API。[TemplateRepository.java](/C:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/infra/persistence/repository/TemplateRepository.java:35) 多處也仍自行 `new TemplateCatalog()`。這會讓「同一套資料」在 DI、靜態存取與測試中有三種取得方式。建議：

- `TemplateReader` 作為唯一讀取 port。
- Spring 注入唯一實作，production 不允許 `new TemplateCatalog()`。
- 測試使用 fake/in-memory `TemplateReader`。
- `TemplateRepository` 改成 instance state，移除 static delegate 與 singleton。

最後，文件提到 `Living` 與 `PartyMember` 不要繼承，我完全同意；但目前的雙向全欄位 copy 仍有長期漂移風險。[CharacterSyncService.java](/C:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/service/CharacterSyncService.java:33) 建議改為定義 ownership：

- `Player`：世界位置、世界互動、角色長期成長的 owner。
- `PartyMember`：DRPG 戰鬥期間的 projection。
- 戰鬥結束回傳明確 outcome，例如 XP、HP/MP 差值、獲得物品、技能熟練度；不要雙向複製整個 stats object。

建議實施順序：

1. 先補資料完整性測試與 namespace 規範，修正已知 key／重複 ID。
2. 建立 `ItemDefinition + ItemInstance + ItemView`，逐步淘汰 `PartyItemSlot` 的業務欄位與 fallback。
3. 把技能改為單一 `SkillDefinition`，讓兩模式持有 skill ID 與模式規則。
4. 移除 TemplateRepository static 存取，讓所有 factory／adapter 只依賴注入的 `TemplateReader`。
5. 將 `CharacterSyncService` 演進為 outcome-based synchronization。

一句話總結：共用的應是「內容定義、識別碼、實例狀態與規則語意」；MUD、DRPG、前端各自該保留的只是行為執行器與呈現投影，而不是再複製一套資料模型。