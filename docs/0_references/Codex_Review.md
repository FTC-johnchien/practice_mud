# 程式碼與文件對齊審查暨可執行任務清單

> **審查日期：** 2026-10-31  
> **範圍：** `docs/1_requirements/`、`docs/2_specifications/`、`docs/3_task_lists/`、`docs/4_active_tasks/`；參考 `Copilot_Review.md` 與 `CODE_DOCS_ALIGNMENT_TASKS.md` 作為核對線索。  
> **性質：** 本報告是建議任務，不自行改變 L1 需求或 L2 規則。參考 review 的主張只有在能對照現行程式／資料時才列為已確認；待裁決項目不視為已證實缺陷。

## 審查摘要

主要風險是新規格與舊實作並存：前端移動仍以文字命令傳輸；雷達圖元與實際地圖圖例不一致；主角晶片加載度未納入目前 SaveData 模型；除役方向的精神魔化狀態仍出現在 HUD。任務看板亦混合即時狀態、完成百分比及不同基準日期。建議先修正玩家可見錯誤與存檔完整性，再處理跨層契約及文件治理。

## 已確認差異

| 優先級 | 差異與證據 |
|---|---|
| P1 | **地牢圖元不一致。** `data/dungeons/mozhu_mines_b1f.json` 使用 `B`、`S`、`T`、`X`；其 legend 定義 `T=TREASURE`、`X=TRAP`。`dungeon-panel.js` 的 `populateTileAppearance()` 將 `S` 和 `P` 一律畫成靈泉；FE2 表格則把 `T` 列為陷阱。 |
| P1 | **晶片進度未進 SaveData。** `Player` 有預設值為 12 的 `chipRate`，營地服務讀取它；`SaveData` 無此欄位，`SaveGameService` builder 亦未寫入。須確認是否有另一路權威狀態；若無，存檔往返不能保證保留進度。 |
| P1 | **UI 移動協定仍是文字命令。** `cmd-dispatcher.js` 的 `sendStep()` 送出 `step <dir>`，`sendTownMove()` 送方向文字；`websocket.js` 封裝為 `{type:"INPUT", text:cmd}`。與 M9/FE2 描述的結構化 Action 契約不同。 |
| P1 | **舊魔化狀態仍呈現在 HUD。** `party-hud-panel.js` 依 `madnessState` 顯示 `CHAOS/SEALED/DEAD_MEAT/ABERRATION` 並提供 `seal` 操作；L1 `GAMEPLAY_CORE_LOOPS.md` 明確除役精神崩潰／魔化方向。仍需保留的 SAN 防護應另外核定，不能一概刪除。 |
| P2 | **活躍 UI 使用 inline onclick。** FE1 要求 `[data-action]` 事件委派；戰鬥、地牢、技能、商店、隊伍戰術及裝備等有效 JS 模組仍產生 inline handler。局部遷移任務不代表全域符合。 |
| P2 | **追蹤矩陣描述不精確。** `docs/3_task_lists/README.md` 宣稱模組「1:1 鏡像」，同時又說技能樹採聚合映射；任務、規格實際存在一對多／多對一。測試基準是 2026-10-29 快照，需避免被當成即時狀態。 |

## 待裁決／待驗證線索

- **追蹤晶片任務獎勵：** `data/quests.json` 首領任務的 `rewardItemIds` 為空，listener 觸發劇情。先確認晶片是必須取得的任務物品、探索拾取物或純敘事線索，再決定要接獎勵或修文案。
- **隨機詞綴掉落：** `DrpgRewardService` 的一般掉落分支呼叫 `inventory.addItem(dropItemId, 1)`，但服務也有 `ItemFactory` 特殊建構路徑。需追完整模板、實例與詞綴流，才能確認是否違規。
- **技能樹資料契約：** 完整比對 `party_skills.json`、schema、TREE_01–17、loader 和存檔引用；疑似 alias 不可直接刪除，先定 canonical ID 及舊 ID 遷移規則。
- **屬性名稱：** L1 定義 `CON=體質`、`STR=根骨`；`StatCommand` 顯示 `STR=力量/臂力`、`CON=根骨/體質`。先核定標準名，再追查加點、公式、裝備、DTO 是否只文字不一致或有實際數值錯置。
- **夥伴初始定位／技能：** 對照 M4、`default_companions.json`、`party_skills.json` 和技能執行規則，指定權威來源後再判定差異。

## 可執行任務

### [ ] CD-01（P1）統一地圖圖元契約與雷達呈現

1. 盤點所有 `data/dungeons/*.json` 的實際符號及 `legend.type/name/passable`，建立對照表，涵蓋 `B/S/T/X/E/D/P` 和樓梯符號。
2. 核定符號語意是否全地圖共用；若地圖可自訂，renderer 以 DTO legend 為主，不對特定地圖 ID 寫分支。
3. 修正雷達，讓已用圖元有正確圖示、CSS 類別、名稱；未知圖元安全降級並輸出診斷訊息。
4. 修正 FE2、Schema／範例與地圖 JSON 的矛盾描述。
5. 驗收：墨竹礦坑每個圖元呈現正確；所有地圖圖元都有定義或明確 fallback。

### [ ] CD-02（P1）持久化主角晶片加載進度

1. 追查 `chipRate` 的初始化、變更、DTO、同步與載入來源，重現保存前後實際值。
2. 若 `Player.chipRate` 是持久化狀態，加入 `SaveData`、存檔 builder、載入還原及舊檔預設／範圍處理；若應由其他狀態推導，移除重複真值並確保載入不回退預設 12。
3. 按 schema version 增加遷移和舊存檔 fixture，驗收舊檔讀取、新檔往返及變更後重載。
4. 更新 M8 與晶片進度規格，指定唯一權威欄位。

### [ ] CD-03（P0 裁決、P1 實作）遷移至結構化 Action 協定

1. 依 L1「除役 MUD」與 M9 核定 Action 類型、欄位、錯誤回應及相容期，決定文字命令是否僅限開發用途。
2. 列出 `sendCmd/window.send` 全部操作與後端 handler 映射，涵蓋移動、互動、戰鬥、設施、物品、存檔。
3. 在協定邊界驗證 payload 並派至既有應用服務；未知／無效 Action 回傳可辨識錯誤。
4. 分批遷移 UI，必要舊命令集中於 adapter 並註明移除條件。
5. 更新 M9、FE1/FE2 和協定驗收案例；主要玩家流程不需輸入文字命令。

### [ ] CD-04（P1）清理已除役魔化 UI 並保留核准的 SAN 功能

1. 依 L1、ADR、M4/M3.1、Sprint Goal 列出精神相關欄位／效果，標記保留、除役、待裁決（SAN、護盾、黑暗、陷阱、madness state、seal、隨機魔化）。
2. 追查 PartyMember、戰鬥／地牢結算、DTO、存檔及 HUD 的讀寫流。
3. 裁決後移除舊狀態 badge、異變進度與 `seal` 操作；若 SAN 防護保留，提供與 HP 區分清楚的合法 UI。
4. 清理不用的資料、測試及舊存檔值，確保舊檔安全載入。
5. 同步 L1/L2/L3，避免舊測試名稱被誤作功能仍有效的證據。

### [ ] CD-05（P1）結案首領線索／任務獎勵閉環

1. 交叉核對序幕需求、`quests.json`、Boss 掉落、寶箱及 quest listener，列出玩家實際看見／取得的線索。
2. 若核定為任務物品，定 canonical ID、保證來源、重複擊殺、滿包、任務狀態與存檔行為，接通獎勵並驗收不重複／不遺失。
3. 若核定為敘事線索，修正「掉落／取得」說法並指明可重現的呈現節點。
4. 驗證獎勵引用與 item schema，更新追蹤矩陣及任務卡。

### [ ] CD-06（P1）驗證一般戰利品詞綴生成路徑

1. 追 `DrpgRewardService` 一般掉落到模板、`ItemFactory`、PartyInventory 與裝備實例，記錄實際流程。
2. 用可重現掉落案例檢查品質、詞綴、實例 ID、堆疊及數值修正。
3. 若未符合 M6，修正集中建構路徑；保留消耗品／材料堆疊和裝備唯一實例規則。
4. 驗收存檔還原、滿包及非裝備掉落；若已符合，將線索結案而不新增重複修復任務。
5. 更新 M6、ITEM_SCHEMA 及掉落任務。

### [ ] CD-07（P1）稽核技能 schema、樹歸屬與 canonical ID

1. 對所有技能 JSON、`party_skills.json`、schema、TREE_01–17、loader 及存檔引用產生欄位／枚舉／引用／alias 差異清單。
2. 區分版本相容欄位與真正錯誤；不以名稱相似作為刪除 ID 的依據。
3. 每組 alias 明訂 canonical ID、舊 ID 讀取方式、輸出規則和存檔遷移。
4. 修正核實不符的 school/tree/tier/passive metadata，並驗收所有資料可載入。
5. 更新 schema 任務及引用完整性檢查。

### [ ] CD-08（P1）核定並統一五維屬性語意

1. 彙整 L1 名稱、程式 key、JSON `scaleStat/reqStats`、DTO、公式與所有 UI 標籤。
2. 核定 `STR=根骨`、`CON=體質`、`DEX=身法`、`ESS=真元`、`SPI/WIS=神識` 等正式對應及舊別名。
3. 查明加點與衍生公式是否用錯數值；只在確認後修改，不能只改 prompt。
4. 更新角色面板、指令、技能需求、裝備和文件；增加映射與數值回歸驗收。
5. 保護舊存檔數值不因欄位調整互換。

### [ ] CD-09（P2）移除活躍 UI inline onclick

1. 搜尋活躍 `static/**/*.js` 和 HTML 中的 `onclick=`，排除 legacy，列出檔案和動作。
2. 建立 `[data-action]` 委派入口與明確 handler，避免 HTML 屬性內拼接命令字串。
3. 逐步遷移 battle、dungeon、party、equipment、skills、shop 等模組，保留鍵盤與無障礙行為。
4. 全域掃描確認活躍頁面不再有 inline handler；例外需在 FE1 註明。
5. 驗收動態重繪後事件可用、單次點擊只觸發一次、輸入值不能破壞 payload。

### [ ] CD-10（P2）合併重複任務並修正追蹤矩陣

1. 盤點 `ACTIVE_TASKS.md`、`SPRINT_GOAL.md`、總覽及各模組任務的重複／依賴項，指定唯一主卡與穩定 ID。
2. 其他清單改為連結主卡，區分當前衝刺、已排程、未來提案、完成與除役。
3. 將「1:1 鏡像」修正為實際的一對多／多對一關係模型。
4. 完成度、測試數和綠燈宣稱標明命令與日期；無法維護的即時數字改為明確狀態或移除。
5. 更新文件索引、相對連結及狀態，封存已結束任務，不把歷史項目當現行待辦。

## 執行順序與驗收紀錄

先做 CD-01、CD-02；CD-03、CD-04、CD-08 先完成規則裁決再遷移；CD-05 至 CD-07 先追完資料流再判斷修復；CD-09 可按模組分批；CD-10 收斂任務狀態。每張主任務卡記錄變更檔案、文件段落、驗收命令／手動步驟、結果和日期。協定、存檔及 ID 變更須驗收舊資料相容性。未執行檢查不得標為通過。

**本次限制：**本次完成靜態文件／程式核對；未修改程式、未執行測試。
