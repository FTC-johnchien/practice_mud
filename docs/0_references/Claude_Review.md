# 文件對齊審查報告（Claude）

> **審查日期：** 2026-10-23
> **審查範圍：** `docs/1_requirements/`（L1，4 份）、`docs/2_specifications/`（L2，22 份）、`docs/3_task_lists/`（L3，20 份）
> **審查基準：** L1 為最高規範；L2 必須完整承接 L1；L3 必須逐模組完整列出 L2 的實作項目並記錄進度。
> **參考資料：** [Copilot_Review.md](Copilot_Review.md)、[Codex_Review.md](Codex_Review.md) 只作為線索；每一項結論都已回到原文重新核對（見〈§ 七、對前兩份報告的查證〉）。
> **審查方式：** 靜態閱讀與關鍵字交叉比對；執行 `scripts/check-doc-links.ps1`（結果 PASS，251 條內部連結全部有效）。**未檢查程式碼、未執行測試**，文中的「已完成／百分比」只代表文件自述。

---

## 一、結論

**三層文件尚未對齊。模組骨架是對的，內容並不一致。**

- **骨架層面（通過）：** L2 的 10 份後端手冊、6 份前端手冊、3 份 Schema 都有對應的 L3 清單，檔案與連結完整。
- **內容層面（不通過）：**
  1. **L1 → L2 有整塊需求沒有落地。** 最明顯的是 CYBERPUNK_CULTIVATION_SPEC 的 16 項神通：L2、L3 完全沒有任何一項（見 F1）。
  2. **L1 和 L2 對同一件事的描述互相衝突。** 營地設施清單、PC 版面、存檔槽位、戰敗規則都不一致（見 F2、F3、F6）。
  3. **L2 內部自相矛盾。** 同一個概念在不同手冊有不同名稱或數值，例如戰鬥 Tick 有 200ms 和 500ms 兩種、行囊有 30 格和 40 格兩種、設施互動的動作協定有四種寫法（見 F5、F8）。
  4. **L2 → L3 的對應偏粗。** L3 多以「3–5 項概括已完成 + 1–2 項待辦」記錄，無法逐項追蹤 L2 的交付內容；同時有一批 L3「已完成」項目在 L2 找不到依據，L2 的路線圖（Roadmap）也沒有被 L3 追蹤（見 F7、F9）。
  5. **MUD／魔化的舊語意仍大量殘留在 L2、L3**，其中 `FRONTEND_LAYOUT_GUIDE.md` 的狀態機仍以「輸入 enter／leave 指令」為主軸（見 F4）。

### 對齊度總表

| 對齊面向 | 判定 | 摘要 |
|---|---|---|
| 檔案／模組骨架（L2 ↔ L3） | ✅ 一致 | 19 個模組皆有 L3 清單，連結 251 條全部有效 |
| L1 → L2 完整承接 | ❌ 不通過 | 16 神通 0/16、營地設施、手機版面、戰敗規則、晶片加載機制皆缺漏或衝突 |
| L1 ↔ L2 語意一致 | ❌ 不通過 | 設施、版面、存檔、零 MUD 原則都有衝突 |
| L2 內部一致 | ❌ 不通過 | Tick、格數、動作名稱、屬性縮寫、路徑與類別名稱皆不一致 |
| L2 → L3 逐項承接 | ⚠️ 部分 | 有清單但顆粒度粗；反向（L3 已完成但 L2 無依據）也有缺口 |
| L3 進度可稽核性 | ⚠️ 不足 | 完成度百分比與「376+ 測試」沒有計算依據或來源 |

---

## 二、發現（依嚴重度排序）

嚴重度：**P0** = L1 內容在 L2/L3 缺漏或與 L1 直接衝突，會讓實作者做錯東西；**P1** = 規格互相矛盾或承接缺口；**P2** = 追蹤與可稽核性問題；**P3** = 文件衛生。

### P0

#### F1. 16 項賽博神通在 L2、L3 完全缺席，且「四大體系」存在三套互相矛盾的分類

- **L1 內容：** [CYBERPUNK_CULTIVATION_SPEC.md](../1_requirements/CYBERPUNK_CULTIVATION_SPEC.md) 定義四大技能體系（網路安全與權限劫持／生物駭客與義體過載／光學渲染與認知介面／底層架構與崩潰重置），共 16 項神通（#1 偷天換日 … #16 歸墟終末），每項都有科技原貌、克蘇魯表象、實戰機制。
- **核對結果：** 以 16 個神通名稱逐一在 `2_specifications/`、`3_task_lists/` 全文檢索，**命中數為 0**；只有 L1 自己出現。L2/L3 實際承接的只有 2 項序章戰技（【神經超頻步】、【強殖角質刃】），而這兩項**不在 L1 的 16 項之內**。
- **三套分類並存：**

  | 來源 | 四個流派／體系 |
  |---|---|
  | L1 CYBERPUNK_CULTIVATION_SPEC | 網路安全與權限劫持／生物駭客與義體過載／光學渲染與認知介面／底層架構與崩潰重置 |
  | L2 [M5 §三](../2_specifications/backend/M5_SKILL_AND_PROGRESSION.md#L47-L52) | 虛空神經道／生化血肉道／重力巨力道／太虛矩陣道 |
  | L2 [SKILL_SCHEMA](../2_specifications/data_schemas/SKILL_SCHEMA.md#L11-L15) 與 [M5 任務 L31](../3_task_lists/backend/M5_SKILL_AND_PROGRESSION_TASKS.md#L31) | `CYBER_HACK`／`GENE_BIO`／`MECHA_CULT`／`VOID_PSI` |

  M5 手冊 §三自稱「依據 L1 CYBERPUNK_CULTIVATION_SPEC」，SKILL_SCHEMA 也自稱「對齊 Level 1《賽博修仙技能體系需求規格》」，但兩者的分類都和 L1 對不起來，彼此也不一致。
- **影響：** L1 最核心的技能設計（16 神通的機制與代價，例如「SAN 代價 100% 轉嫁給目標」「斬殺線 15%」「每場限一次復活」）沒有任何下游文件承接；L3 的 M5 完成度標 90% 沒有對照基準。
- **建議：** 先由 L1 決定四個體系與流派的唯一命名；L2 在 M5 與 SKILL_SCHEMA 補齊 16 項神通的資料契約（至少要有 ID、流派、機制標籤）；L3 在 M5 與 SKILL_SCHEMA 清單中**逐項**列出 16 個任務與狀態。

#### F2. L1 的五個營地設施，與 M1／M9／M4／M6 的設施清單互相不同

| 來源 | 設施清單 |
|---|---|
| L1 [GAMEPLAY_CORE_LOOPS §二.1](../1_requirements/GAMEPLAY_CORE_LOOPS.md#L45-L51) | 通鋪柴房/客棧（調息）、集市/雜貨鋪（買火把、辟邪符、金創藥、解毒散）、百煉堂/鐵匠鋪（強化、詞綴重鑄、注靈拆解）、雜役窩點/酒館（招募夥伴、配置 5 人名冊與站位）、礦道入口/傳送門 |
| L2 [M1 §二](../2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md#L37-L65) | 裝備工坊、賽博晶片實驗室、道場與傳功室、生化醫療站、地牢傳送門 |
| L2 [M9 §三.4](../2_specifications/backend/M9_STATE_PIPELINE_AND_EVENTS.md#L70-L77) | `INN`、`BLACKSMITH`、`SHOP`、`DUNGEON_ENTRANCE` |
| L3 [M1 任務 L26](../3_task_lists/backend/M1_TOWN_HUB_TASKS.md#L24-L30) | 天工百煉坊、賽博晶片工坊、太華傳功道場、回春草堂、地牢傳送門 |
| L3 [M4 任務 M4-2](../3_task_lists/backend/M4_PARTY_AND_FORMATION_TASKS.md#L42-L44) | 「營地客棧」招募夥伴（L1 寫的是「雜役窩點/酒館」） |

- **缺口：** M1 手冊**沒有「客棧調息」「雜貨鋪補給」「酒館招募與編隊」這三個 L1 設施**。休整被放進 M1 的「核心職責 2」而不是設施；補給只出現在醫療站的「行囊補給配置」；招募與站位配置（L1 明列的功能）在 M1 完全沒有。
- **反向缺口：** M1 手冊有 L1 沒有的設施：賽博晶片實驗室、道場。L1 的「注靈拆解」在 M1 變成「分解/強化/詞綴重鑄」，沒有「注靈」。
- **M6 還綁在房間概念上：** 商店資料由 `findShopByRoomId(roomId)` 查詢（[M6 L70](../2_specifications/backend/M6_ITEM_AND_ECONOMY.md#L69-L71)、[M7 L80](../2_specifications/backend/M7_DATA_DRIVEN_TEMPLATES.md#L80)），M9 的動作則用 `facility: "SHOP"`；兩者之間沒有銜接契約。
- **建議：** 以 L1 為準，重新定義設施清單與唯一 ID（例如 `INN`、`SHOP`、`FORGE`、`TAVERN`、`PORTAL`）；晶片實驗室、道場若要保留，必須先回 L1 補需求。M1／M9／FE4／M6／M4 再統一引用同一組 ID。

#### F3. PC／手機版面：L1 與 L2 衝突，且 L2 自己有三種桌機版面，手機版面完全缺席

- **L1：** [UI_UX_REQUIREMENTS](../1_requirements/UI_UX_REQUIREMENTS.md#L27-L55) 要求 PC 三欄（左：迷霧地圖／座標／方向盤／火把條；中：主視口；右：5 人小隊面板）＋**底欄唯讀敘事日誌**；手機直向則是頂部樓層名、上中下堆疊、底部 Tab Bar【遠征／小隊／行囊／技能／存檔】。
- **L2 三種桌機版面並存：**
  - [FRONTEND_LAYOUT_GUIDE §一](../2_specifications/frontend/FRONTEND_LAYOUT_GUIDE.md#L11-L48)：頂欄＋左舞台＋**右側日誌**＋底部小隊 HUD＋底部操作列。
  - [FE4 §四](../2_specifications/frontend/FE4_TOWN_HUB_AND_ACTION_BAR.md#L103-L104)：**左側敘事日誌**＋中央視口＋右側小隊血條。
  - L1：底欄日誌。
- **手機版面：** 全部 L2/L3 找不到 Tab Bar、底欄 120px 日誌、單手操作等描述；唯一相關內容是 FE4 的「<1024px 收斂為垂直單欄、按鈕 44×44px」與 FE6 的 `@media (max-width: 1024px)`。
- **影響：** FE2／FE3／FE4／FE5／FE6 的實作者無法判斷以哪份佈局為準；L3 沒有任何手機版任務。
- **建議：** 由 L1 版面為準，統一 FRONTEND_LAYOUT_GUIDE 與 FE4；補一份手機版面規格（Tab Bar 五分頁與場景對應），並在 FE4／FE6／FE5 任務清單加上對應項目。

#### F4. MUD／文字指令／魔化的舊語意仍大量殘留在 L2、L3（與 L1「零 MUD 指令」直接衝突）

L1 依據：[GAMEPLAY_CORE_LOOPS L5、L97–L98](../1_requirements/GAMEPLAY_CORE_LOOPS.md#L97-L98)、[UI_UX_REQUIREMENTS L5、L14–L15](../1_requirements/UI_UX_REQUIREMENTS.md#L14-L15)。

**L2 殘留：**

| 檔案 | 位置 | 殘留內容 |
|---|---|---|
| FRONTEND_LAYOUT_GUIDE | [L84–L85](../2_specifications/frontend/FRONTEND_LAYOUT_GUIDE.md#L84-L85) | 狀態機寫「**輸入 enter 指令**進入秘境」「輸入 **leave** 撤離地牢」 |
| 同上 | [L88](../2_specifications/frontend/FRONTEND_LAYOUT_GUIDE.md#L88)、[L92](../2_specifications/frontend/FRONTEND_LAYOUT_GUIDE.md#L92) | 「城鎮野外遭遇主動怪拉入戰鬥」；戰敗「重聚於客棧」 |
| 同上 | [L29–L31](../2_specifications/frontend/FRONTEND_LAYOUT_GUIDE.md#L29-L31)、[L112](../2_specifications/frontend/FRONTEND_LAYOUT_GUIDE.md#L112) | 城鎮仍是「房間描述／道路方位羅盤（WASD 出路網格）／地面散落靈物」 |
| 同上 | [L46](../2_specifications/frontend/FRONTEND_LAYOUT_GUIDE.md#L46) | 底部操作列仍有 `[⌨️ 指令框]`；L23 還有 ANSI 彩色串流 |
| UI_PAGE_STRUCTURE | [L96](../2_specifications/frontend/UI_PAGE_STRUCTURE.md#L96)、[L195](../2_specifications/frontend/UI_PAGE_STRUCTURE.md#L195) | `CONSOLE` 快捷鍵與「`~` 終端控制台」；[L171](../2_specifications/frontend/UI_PAGE_STRUCTURE.md#L171) 日誌仍是「ANSI 彩色轉義」 |
| FE1 | [L5、L100–L102](../2_specifications/frontend/FE1_CORE_STATE_AND_PIPELINE.md#L100-L102) | 以「ANSI 解碼、步進 `step *`、`*quiet*` 通道、復古 MUD 文字氛圍」為核心職責 |
| ARCHITECTURE_SUMMARY | [L21、L27、L43](../2_specifications/ARCHITECTURE_SUMMARY.md#L21) | `MudWebSocketHandler`、`CommandDispatcher`、`MozhuMinesQuestListener`、「玩家輸入指令的調度」；L99 還寫「Actor 信箱模型」，與 M9 §設計原則「除役多實體 Actor 信箱」直接相反 |
| MODULES_MAP | [L137](../2_specifications/MODULES_MAP.md#L137) | M6 代表類別仍列 `ShopCommand`、`DropCommand`、`GetCommand` |
| M4 | [L75–L98、L102–L108](../2_specifications/backend/M4_PARTY_AND_FORMATION.md#L75-L108) | 仍有「Player (MUD 空間代理)」鏡像、`get/use/drop`、`EquipCommand`；與 L1「單一真相源」「零 MUD」及 M4 自己的「小隊為唯一真相源」並存 |
| M6 | [L21、L70–L74、L91–L92](../2_specifications/backend/M6_ITEM_AND_ECONOMY.md#L69-L74) | `get/drop/buy 1 5/sell herb 1` 文字指令、`roomId.contains("inn")` |
| M7 | [L89](../2_specifications/backend/M7_DATA_DRIVEN_TEMPLATES.md#L89) | 校驗項目仍是「房間出口 exits 連通性」 |
| M3-1 | [L35](../2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md#L35) | Actor 標為「Player / Living (MUD 生靈)」 |
| M8 | [L103](../2_specifications/backend/M8_SAVE_AND_PERSISTENCE.md#L103)、[L115](../2_specifications/backend/M8_SAVE_AND_PERSISTENCE.md#L115) | `StashCommand`、`RoomPersistenceService`（地面遺留物） |

**L3 殘留：**

- [M1 任務 L15、L27–L28](../3_task_lists/backend/M1_TOWN_HUB_TASKS.md#L15)：`Room.broadcast(String)` 被列為「單機通道」且列在**已完成**；L3 的程式碼真相源仍以 `Room.java` 為核心。
- [M9 任務 L28–L29](../3_task_lists/backend/M9_STATE_PIPELINE_TASKS.md#L28-L29)：「房間唯一主角引用」「多人廣播遍歷通道」。
- [FE1 任務 L26–L27](../3_task_lists/frontend/FE1_CORE_STATE_TASKS.md#L26-L27)：保留 `~` 開發者終端；L33 還有 `mud-core.js`／`mud-ui.js` 歷史腳本。
- [FE2 任務 L5](../3_task_lists/frontend/FE2_DRPG_VIEWPORT_TASKS.md#L5)：職責仍寫「八方步進羅盤」（L1 只有前進／後退／左轉／右轉；FE4 手冊甚至還有 `Q/E` 平移，見 F8）。
- [README L19](../3_task_lists/README.md)、[M4 任務 L19](../3_task_lists/backend/M4_PARTY_AND_FORMATION_TASKS.md#L19)：測試名 `PartyInventoryAndMadnessTest`。
- [M3 任務 L39–L40](../3_task_lists/backend/M3_COMBAT_ENGINE_TASKS.md#L39-L40)：`M3-PURGE-BETRAYAL` 除役項目與 [M3-1 任務 L28](../3_task_lists/backend/M3_1_BUFF_AND_STATUS_TASKS.md#L28) 的 `MADNESS-DEBUFF`，沒有標示為歷史。

> 判斷：部分殘留可能只是「描述舊系統如何被取代」，但上面大多數是**現行規則描述**（狀態機、快捷鍵、類別清單），不是歷史註記。建議逐項決定「刪除」或「標註為已除役（歷史）」，並同步更新 L3 的程式碼單一真相源。

---

### P1

#### F5. 結構化動作協定在 L2／L3 有多套互相矛盾的寫法

| 位置 | 動作／格式 |
|---|---|
| M9 手冊 [§三](../2_specifications/backend/M9_STATE_PIPELINE_AND_EVENTS.md#L40-L87) | `MOVE`（`direction` 為 `FORWARD/BACKWARD/TURN_LEFT/TURN_RIGHT`）、`CAST_SKILL`、`FORMATION_FINISHER`、`TOWN_FACILITY`（`facility` 為 `INN/BLACKSMITH/SHOP/DUNGEON_ENTRANCE`）、`EQUIP`；**欄位平鋪** |
| M1 手冊 [§一 圖](../2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md#L15) | `ENTER_FACILITY`／`INTERACT_NPC`；對話選項 action 為 `OPEN_FACILITY_SHOP`、`CLOSE_DIALOGUE` |
| M2 手冊 [§五](../2_specifications/backend/M2_DRPG_DUNGEON.md#L95) | `MOVE / TURN / INTERACT / EXTRACT`（M9 無 `TURN`、`INTERACT`、`EXTRACT`） |
| M1 任務 [M1-2](../3_task_lists/backend/M1_TOWN_HUB_TASKS.md#L39-L41) | `INTERACT_FACILITY`、`REST_PARTY`，註冊在 `CommandDispatcher` |
| M9 任務 [M9-1](../3_task_lists/backend/M9_STATE_PIPELINE_TASKS.md#L35-L37) | 格式為 `{action, payload:{direction:"NORTH"}}`（巢狀 `payload`、方向為羅盤方位） |
| FE4 手冊 [§二](../2_specifications/frontend/FE4_TOWN_HUB_AND_ACTION_BAR.md#L60-L70) | 前端 `data-action`：`open-facility`、`enter-dungeon`、`interact-npc` |

- 同名類別 `DrpgActionDispatcher` 在 [M2](../2_specifications/backend/M2_DRPG_DUNGEON.md#L95) 位於 `domain.dungeon`，在 [M9](../2_specifications/backend/M9_STATE_PIPELINE_AND_EVENTS.md#L195) 位於 `interfaces.websocket`；M9 任務與 ARCHITECTURE_SUMMARY 則用 `CommandDispatcher`、`MudWebSocketHandler`，M9 手冊用 `DrpgWebSocketHandler`。
- M9 手冊**完全沒有**小隊加點（M4-1／FE5-1 需要）、存讀檔、使用道具、買賣、撤離、設施內子動作（強化、重鑄、洗點）等動作定義；而 M9-1 的目標卻是「所有互動 100% 收斂為 JSON」。
- **結果：** M9-1 待辦的「全域收斂」沒有可驗收的完整動作清單；M9 手冊既是「已定案契約」又被 M9 任務當成「尚未完成的重構」，兩者對現況的描述矛盾。
- **建議：** 在 M9 手冊建立「動作總表」（動作名稱、payload 結構、負責模組）作為唯一出處；M1／M2／M4／M6／M8 的動作一律引用；M9-1 拆成每個動作一個子任務。

#### F6. L1 明文規則在 L2/L3 沒有對應（或與 L2 衝突）

| L1 規則 | 位置 | L2 | L3 | 判定 |
|---|---|---|---|---|
| 全軍覆沒：**遺失隨身收穫並在營地虛弱復甦** | [GAMEPLAY L62](../1_requirements/GAMEPLAY_CORE_LOOPS.md#L62) | 全 L2 搜尋「全軍覆沒／團滅／遺失／虛弱復甦」無命中；僅 FRONTEND_LAYOUT_GUIDE 的狀態圖有「戰敗 → 客棧」一行 | 無 | ❌ 缺漏 |
| 地牢中**無法任意存檔或傳送退出** | [GAMEPLAY L61](../1_requirements/GAMEPLAY_CORE_LOOPS.md#L61) | M8 存檔結構含 `inDungeon: true` 與 `dungeonProgress`，M8 任務自動存檔含「切換地牢樓層」，未規範手動存檔在地牢中的限制 | 無 | ⚠️ 未定義，且 M8 設計暗示可在地牢內存檔 |
| **單存檔單主角**（schemaVersion 3） | [GAMEPLAY L100](../1_requirements/GAMEPLAY_CORE_LOOPS.md#L100) | M8：5 手動槽＋1 自動槽；FRONTEND_LAYOUT_GUIDE：「五大存檔槽」 | M8-1：「Slot 1~3」 | ❌ L1 單存檔／L2 五槽／L3 三槽；L1 另有「本地存檔管理」（UI_UX L20），需先釐清「單存檔」的語意 |
| 主角特工晶片**神經元加載度 12%→100% 解鎖戰術算法** | [GAMEPLAY L19](../1_requirements/GAMEPLAY_CORE_LOOPS.md#L19)、[WORLD L76](../1_requirements/WORLD_AND_LORE_BIBLE.md#L76) | 只有 M9 DTO 的 `agentChipLoad: 12`；沒有「加載狀態機」、增加條件、解鎖哪些算法 | 只有 M4-1、FE5-1 顯示 12% 進度條 | ⚠️ 只有顯示，沒有機制 |
| **微量異形強殖基因覺醒**（角質骨膜、骨刺） | [GAMEPLAY L19](../1_requirements/GAMEPLAY_CORE_LOOPS.md#L19) | 僅 M1 醫療站的「基因強化」一句與 2 個序章戰技名稱 | 無獨立項目 | ⚠️ 無機制規格 |
| 戰利品類型：**煞晶、古代工程師零件、巨企晶片**、稀有度、隨機詞綴 | [GAMEPLAY L18](../1_requirements/GAMEPLAY_CORE_LOOPS.md#L18) | ITEM_SCHEMA 有七階稀有度；M6 的詞綴系統只在「未來路線圖」；沒有三類戰利品的分類 | M6 任務有詞綴、任務晶片 | ⚠️ 見 F7 |
| 序章四階段（醒來／雜役／煉丹房反殺／追蹤器懸念） | [WORLD §五](../1_requirements/WORLD_AND_LORE_BIBLE.md#L85-L97) | 無任何 L2 模組承接劇情、事件腳本或敘事日誌；L2 的任務系統僅 `quests.json` 一行 | 僅 L4 SPRINT_GOAL 有「第一章」標題 | ❌ L2/L3 缺口；現況只存在 L4 |
| 敘事日誌文案來源（石壁血肉聲、外神囈語、晶片代碼日誌） | [UI_UX §一.4](../1_requirements/UI_UX_REQUIREMENTS.md#L21-L23) | 只有 M9 的「Narrative Text Event」與 FE1 的日誌面板；沒有事件類型、文案資料契約 | 無 | ⚠️ |
| 遠征的「機關與暗室檢定」：暗弩（DEX）、血肉暗門（STR/工具）、靈泉全滿 | [GAMEPLAY L59](../1_requirements/GAMEPLAY_CORE_LOOPS.md#L59) | M2 手冊與 DUNGEON_SCHEMA **都沒有**檢定欄位（Schema 只有 `TRAP`、`eventId`，無檢定屬性或靈泉類型） | M2 任務有「已完成」記錄 | ❌ 反向缺口：L3 已完成、L2 無規格 |

#### F7. 反向漂移：L3 已完成（或列為待辦）的項目，在 L2 找不到依據；L2 有而 L1 沒有的內容

**(a) L3「已完成」但 L2 手冊沒有規格或僅列為「未來路線圖」：**

| L3 項目 | L2 現況 |
|---|---|
| [M4-GAMBIT-ENGINE](../3_task_lists/backend/M4_PARTY_AND_FORMATION_TASKS.md#L32-L33)（含四個預設範本） | M4 手冊只在 [§六 路線圖](../2_specifications/backend/M4_PARTY_AND_FORMATION.md#L136-L137) 描述；M3-1 只有「Gambit 反制」一句 |
| [M6-AFFIX-SYSTEM](../3_task_lists/backend/M6_ITEM_AND_ECONOMY_TASKS.md#L30-L31) | M6 手冊在 [§五 路線圖](../2_specifications/backend/M6_ITEM_AND_ECONOMY.md#L104) 才提詞綴；只有 ITEM_SCHEMA 有結構 |
| M6-POISE-BREAK-POWER、M6-QUEST-CHIP-DATA | M6 手冊無 `poiseBreakPower`、任務晶片 |
| [M7-DATA-PARTITION](../3_task_lists/backend/M7_DATA_DRIVEN_TEMPLATES_TASKS.md#L28-L29)（`data/global/`、`data/zones/`） | M7 手冊 [§二](../2_specifications/backend/M7_DATA_DRIVEN_TEMPLATES.md#L25-L51) 仍是 `items.json`、`classes/classes.json` 等舊結構 |
| [M2-STEP-TRAPS](../3_task_lists/backend/M2_DRPG_DUNGEON_TASKS.md#L31-L32)（DEX/STR 檢定、靈泉） | M2 手冊與 DUNGEON_SCHEMA 無此規格（見 F6） |
| FE2-TORCH-VIGNETTE | FE2 手冊無暈影規格（L1 UI_UX L64–L65 有） |
| M5-OVERCLOCK-TIERS、ComboRole | 只在 SKILL_SCHEMA，M5 手冊本文沒有 |

**(b) L3 待辦中 L2 沒有規格的項目：** M1-2 的 `INTERACT_FACILITY/REST_PARTY`（見 F5）、M2-2「低 SAN 幻覺與靈視暗門」（只在 MODULES_MAP 路線圖，L1 沒有）、M3-2「兵刃熱量換彈」（L2、L1 皆無）、M4-1「5 點初始潛能點」（M5 手冊是「每級 2 點」，沒有初始 5 點）、M4-2「墨家機關師、神農醫修」招募（L1 的四位夥伴是鐵牛／凌霜／燕青／墨衍，已在 M4 手冊編制中，招募流程本身無規格）、M5-1「32+ 賽博天賦樹」（M5 手冊路線圖是「周天二十八宿」，是另一套）、M5-2「雷劫試煉」。

**(c) L2 引入而 L1 沒有的機制（需 L1 裁決保留或移除）：**

- **SAN 護盾與【精神萎靡】Debuff**：L1 只在 CYBERPUNK_CULTIVATION_SPEC 中把 SAN 當作技能代價，並要求「取消道心崩潰魔化」；L2 卻建立完整的 SAN 系統（M4 §二.2、M2 光明度扣 SAN、M6 辟邪茶回 SAN、FE5 警示、M3-1 Debuff 0.85）。這是否等於「魔化的溫和版」需 L1 說明。同時 L2 內部門檻也不一致：M4 為 ≤25 弱化 −10%、=0 −15%；FE5 任務以 <50% 警示；M2-2 以 <30 觸發靈視。
- **L2 路線圖中與 L1 精神衝突的項目：** M5「禁忌邪術（燃燒壽元）」、M6「依全服消耗量波動物價」、M8「鐵人模式／討伐前世發狂的自己」、MODULES_MAP §五.2「SAN 值幻覺／偽裝寶箱怪」。這些在 L3 沒有追蹤（只有 `FUTURE_IMPROVEMENTS.md`，本次未逐項比對）。

#### F8. L2 內部的數值與命名不一致

| 項目 | 不一致內容 |
|---|---|
| 戰鬥 Tick | 500ms：L1、M3 [L5、L42](../2_specifications/backend/M3_COMBAT_ENGINE.md#L42)、M3-1、M9、MODULES_MAP L171。**200ms**：MODULES_MAP [L82](../2_specifications/MODULES_MAP.md#L82)、ARCHITECTURE_SUMMARY [L99](../2_specifications/ARCHITECTURE_SUMMARY.md#L99)、M3 [L273](../2_specifications/backend/M3_COMBAT_ENGINE.md#L273)、M3-1 [L320](../2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md#L320)、M5 [L102](../2_specifications/backend/M5_SKILL_AND_PROGRESSION.md#L102) |
| 行囊格數 | 40 格（L1 及多數 L2）；[FRONTEND_LAYOUT_GUIDE L116](../2_specifications/frontend/FRONTEND_LAYOUT_GUIDE.md#L116) 寫「**30 格**儲物袋」 |
| 裝備槽位 | M6：5+2 共 7 槽（`MAIN_HAND/OFF_HAND/HEAD/BODY/FEET/ACCESSORY_1/2`）；[M4 L106](../2_specifications/backend/M4_PARTY_AND_FORMATION.md#L106)：7 槽但內容是「武器、衣服、護腕、腰帶、鞋履、護符、法寶」 |
| 遇敵率 `dangerRate` | M2 手冊 [L61](../2_specifications/backend/M2_DRPG_DUNGEON.md#L61) 為小數 `0.15`；DUNGEON_SCHEMA [L48](../2_specifications/data_schemas/DUNGEON_SCHEMA.md#L48) 為整數百分比 `15` |
| 黑暗扣 SAN | M2 手冊 [L54](../2_specifications/backend/M2_DRPG_DUNGEON.md#L54)：幽冥時「每走 5 步 1 點」；M2 任務 [L30](../3_task_lists/backend/M2_DRPG_DUNGEON_TASKS.md#L30)：「每步扣除」；L1 無此規則 |
| 光明度消耗時機 | M2 手冊 [L49](../2_specifications/backend/M2_DRPG_DUNGEON.md#L49)：「前進**或轉向**」各 −1；L1「每走一步消耗 1 點」 |
| 移動動作 | L1／M9：前進／後退／左轉／右轉；FE4 手冊 [L87](../2_specifications/frontend/FE4_TOWN_HUB_AND_ACTION_BAR.md#L87) 另有 `Q/E` 左右平移；FE2 任務／FRONTEND_LAYOUT_GUIDE「八方羅盤」 |
| 快捷鍵 | L1：W/A/S/D、數字鍵 1~5、ESC；FE4 手冊：技能 1~4；FE4 任務：1~8 動作列；UI_PAGE_STRUCTURE：`1~5` 技能 |
| 道基五維縮寫 | L1：CON／**ESS**（真元）／**SPI**（神識）／STR／DEX；M4：CON／**SPI/INT**（真元）／**PER**（神識）／STR／**AGI**；M5 用 `SPI/PER`。**同一個 SPI 在 L1 是神識、在 M4 是真元** |
| 存檔槽位 | 見 F6 |
| 樓層／設施 ID | M9 範例 `black_vein_b1f`；M2/M7/DUNGEON_SCHEMA `mozhu_mines_b1f`；DUNGEON_SCHEMA `exitRoomId` 為 `mozhu_mines:mine_entrance`（房間語意）；M2 手冊 `hub_ore_mine_shelter`；M1 手冊 `hubId`。撤離後如何對應 `TownHubDto.hubId` 沒有契約 |
| 營地命名 | L1/M1 手冊「黑煞宗」；M1 任務「新手村／墨竹前進營地」；M7「桃源新手村」；M2「墨竹」 |
| 境界稱呼 | L1「煉氣→築基→**結丹**」；M5「練氣→築基→**金丹**」 |

#### F9. L3 清單顆粒度不足：模組完成度無法對照 L2 的每項交付

對 L2 各模組逐一比對 L3（✅ 有明確任務、⚠️ 只有概括、❌ 沒有）：

| 模組 | L2 主要交付內容 | L3 覆蓋 |
|---|---|---|
| **M1** | 5 設施各自的功能（分解／強化／重鑄／晶片調試／插槽／技能洗點／Gambit／基因強化／補給／區域選擇／出征校驗）、NPC 對話樹、任務分發、好感度、解鎖條件、`TownHubDto` | ❌ 完成項目只有「設施資料化／一鍵休整／Room.broadcast」；M1-1、M1-2 兩項待辦；對話系統與每個設施行為**無任何項目** |
| **M2** | 3 步視野、光明度三級懲罰、危險度累積、撤離流程、寶箱、機關、`openedChests` | ⚠️ 撤離流程、危險度累積加權、寶箱無項目 |
| **M3 / M3-1** | 圓桌判定、Poise、元素反應、打斷 FSM、連攜、Boss 階段；Buff 刷新／堆疊策略、最短護盾、控場門控、HUD 聚合 | ⚠️ M3 項目頗完整；M3-1 缺「刷新／堆疊策略」「護盾抵扣」「控場門控」「圖示聚合」 |
| **M4** | 5 人編制與站位、五維公式、SAN、陣法、行囊大一統、裝備投影、招募好感度 | ⚠️ 好感度、站位傷害規則、公式、裝備投影無項目；M4-2 招募只有一行 |
| **M5** | 境界與升級公式、技能縮放、四流派、晶片插槽（2 個）、4 個主動槽、GCD/CD、零成本洗點 | ⚠️ 升級公式、縮放公式、晶片插槽、GCD/CD 無項目 |
| **M6** | 40 格行囊、5+2 槽、買賣、共享金庫、藥品效果、戰利品結算、寶箱靈石 | ⚠️ 買賣與藥品效果、行囊堆疊上限無項目；M6-UNIFIED-ECONOMY 敘述為「從小隊行囊扣款」（應為小隊金庫） |
| **M7** | 資料目錄、載入快取、`TemplateReader`、4 類校驗 | ⚠️ 路徑與類別名與 L2 不同（見 F10） |
| **M8** | 槽位、`.bak`、遷移管線 v1→v2→v3、自動存檔觸發 | ⚠️ 遷移管線與 `.bak` 回滾沒有已完成項目（只在 M8-1 驗收標準提到） |
| **M9** | WebSocket 流程、5 類動作、`DrpgStateDto`、500ms 迴圈、心跳 | ⚠️ 心跳、迴圈、日誌推送無項目 |
| **FE1–FE6、3 個 Schema** | 見各手冊 | 本次只逐項比對 FE1／FE2／FE4／FE5 與 DUNGEON／SKILL 任務；FE3、FE6、ITEM 任務僅檢視摘要，未逐項核對 |

另外，**`FRONTEND_LAYOUT_GUIDE.md`、`UI_PAGE_STRUCTURE.md`、`ARCHITECTURE_SUMMARY.md`** 雖屬導覽／架構類，但含有可實作的規範（狀態機、快捷鍵、`constants.js`／`ui-utils.js`、7 個視圖模組、深拷貝與棘輪守則），L3 沒有對應清單。這與 Copilot 認為「不構成獨立交付模組」的看法不同：這些規範若沒有任務檔，就沒有人負責落實。

---

### P2

#### F10. L3 儀表板與 L2↔L3 路徑／名稱漂移

- **儀表板連結：** [3_task_lists/README.md](../3_task_lists/README.md#L15-L37) 的「Level 2 規格手冊」欄，M1、M3.1、M4–M9、FE1–FE6 共 **14 個**顯示文字使用舊檔名（如 `M1_TOWN_HUB.md`、`FE2_VIEWPORT.md`），**連結目標本身是對的**（檢查腳本 251 條全部 PASS）。這是標籤過時，不是連結失效。
- **完成度與測試基準：** 各模組百分比與「376+ 項測試 100% 綠燈」（[L5](../3_task_lists/README.md#L5)）沒有分母定義與驗證來源。
- **L2 與 L3 對同一個程式實體的路徑或名稱不同：**

  | 項目 | L2 | L3 |
  |---|---|---|
  | M7 載入與快取 | `TemplateRepository`、`infrastructure.template` | `TemplateCatalog`、`domain.repository` |
  | `TemplateReader` 套件 | `domain.ports` | `domain.repository` |
  | 設施資料 | `data/hub/facilities.json`、`data/hub/dialogues.json` | `data/zones/newbie_village/facilities.json`（M1 任務 L13） |
  | 職業資料 | `data/classes/classes.json`、`data/skills.json` | `data/global/classes.json`、`data/party/party_skills.json` |
  | 地牢服務 | `domain.dungeon.DungeonManager` | `domain.dungeon.service.DungeonManager` |
  | 成長服務 | `domain.service.XpProgressionService` | `domain.progression.service.XpProgressionService` |
  | CSS 檔名 | MODULES_MAP：`style-03-battle-actions.css`；FE6 手冊：`style-06-shop-and-responsive.css` | FE2 任務：`style-03-dungeon-view.css`；FE5 任務：`style-06-modals-and-forms.css` |
  | M6 經濟敘述 | 共享金庫 `Party.coin` | M6-UNIFIED-ECONOMY：「從小隊行囊扣款」 |

  M1 手冊指定的 `TownHubService`／`TownHubDto`／`DialogueService` 在 M1 任務標為「規劃中／未建立」，而 ARCHITECTURE 與 M9 任務引用的是現有的舊類別，**無法判斷 L2 描述的是「現況」還是「目標」**。建議在每份 L2 手冊標示「現況／目標」，路徑以實際程式碼為準。

#### F11. L1 本身的衛生問題（會影響下游追溯）

- [docs/README.md L80](../README.md#L80) 規定 L1「嚴禁出現具體程式 ID、JSON 欄位名」，但 [GAMEPLAY_CORE_LOOPS L16–L19](../1_requirements/GAMEPLAY_CORE_LOOPS.md#L16-L19) 含 `CombatRules`、`DrpgCombatLoop`、`ComboResolver`、`party_skills.json`、`XpProgressionService`、`schemaVersion 3`；[UI_UX L62–L65](../1_requirements/UI_UX_REQUIREMENTS.md#L62-L65) 含色碼與 `.torch-darkness-vignette`。
- L1 沒有穩定的需求 ID；只有 GAMEPLAY_CORE_LOOPS 有「涉及系統與模組」欄。CYBERPUNK、WORLD、UI_UX 沒有任何向 L2 的指向，是前述漏接（F1、F6）反覆發生的結構原因。
- SAN 與精神萎靡在 L1 沒有定義（F7c）。

---

### P3

- 章節編號重複：M3 手冊兩個「八」（[L237](../2_specifications/backend/M3_COMBAT_ENGINE.md#L237)、[L255](../2_specifications/backend/M3_COMBAT_ENGINE.md#L255)）；M5 手冊兩個「五」（L71、L85）；M2 手冊有連續兩條分隔線（L28–L30）。
- 模組計數寫法不統一：MODULES_MAP 稱「後端 9 大（含 M3-1）」卻列出 10 個條目；docs/README 稱「9+1」「10 大」；MODULES_MAP 前端「6 大」，其 Mermaid 只畫出 FE-1 至 FE-5。
- MODULES_MAP Mermaid 把 M1 標為「探索與世界模組 (World & Exploration)」、M9 標為「虛擬執行緒 Actor 通訊模組」，與各自手冊標題不同。
- M2 手冊 `exitRoomId` 描述為「撤離後重返地表房間 ID」，語意仍是房間，與 M1「選單式營地」不一致（與 F8 `hubId` 同源）。

---

## 三、需求 → 規格 → 任務 追溯矩陣（主要需求條目）

符號：✅ 已承接且一致　⚠️ 部分承接或有衝突　❌ 缺漏。

| L1 需求（來源） | L2 | L3 | 判定／問題編號 |
|---|---|---|---|
| 世界觀呈現：石壁質地、NPC、道具 Lore（GAMEPLAY 目標 1） | DUNGEON_SCHEMA 描述欄位、M1 對話 | M2-1、DUNGEON-SCHEMA-1 | ⚠️ 無專屬敘事模組（F6） |
| 世界設定（卵鞘、工程師、巨企、對照表） | 無 | 無 | ❌ 屬純敘事需求，需決定是否由 M7 資料／敘事資料承接（F6） |
| 序章四階段（WORLD §五） | 無 | 僅 L4 | ❌ F6 |
| 戰鬥：500ms ATB、圓桌、Poise、1.5x、打斷、FCT | M3、M3-1、FE3 | M3 全套 | ✅（Tick 在他處不一致：F8） |
| 連攜（雙星合璧…天外合擊）、元素反應、Primer/Detonator | M3 | M3-CROSS-COMBO 等 | ✅ |
| Gambit 自動戰術鏈 | M4（僅路線圖）、M3-1 一句 | M4-GAMBIT-ENGINE、FE5-GAMBIT | ⚠️ F7a |
| 16 項神通、四大體系 | ❌ | ❌ | ❌ F1 |
| 戰利品類型、稀有度、詞綴、鐵匠注靈 | ITEM_SCHEMA（稀有度）、M6（路線圖）、M1（工坊） | M6-AFFIX、M6-2 | ⚠️ F2、F6、F7a |
| 道基五維、境界（煉氣→築基→結丹） | M4、M5 | M5 部分 | ⚠️ 縮寫與稱呼不一致（F8） |
| 特工晶片加載 12%→100% | M9 DTO 欄位 | M4-1、FE5-1 | ⚠️ 無機制（F6） |
| 異形基因覺醒 | M1 一句 | 無 | ⚠️ F6 |
| 5 人小隊、前中後衛、4 位夥伴 | M4 | M4 多項 | ✅（招募：F7b） |
| 營地五設施選單 | M1、FE4（設施與 L1 不同） | M1、FE4 | ❌ F2 |
| 地牢：3 步視野、火把、迷霧、撤離 | M2 | M2 | ✅（細節不一致：F8） |
| 地牢機關檢定（DEX/STR/靈泉） | ❌ | M2-STEP-TRAPS | ❌ F6、F7a |
| 全軍覆沒懲罰 | ❌ | ❌ | ❌ F6 |
| 地牢中不可任意存檔 | ❌（M8 暗示相反） | ❌ | ❌ F6 |
| 零 MUD 指令 | M9 宣告 | 多處殘留 | ⚠️ F4 |
| 40 格行囊唯一真相源 | M4、M6 | M4-UNIFIED-INVENTORY | ✅（M4 鏡像殘留、FRONTEND 30 格：F4、F8） |
| 單存檔單主角 | M8（5+1 槽） | M8-1（3 槽） | ❌ F6 |
| PC 三欄＋底欄日誌 | FRONTEND_LAYOUT_GUIDE（右側日誌）、FE4（左側日誌） | 無 | ❌ F3 |
| 手機直向＋Tab Bar | 無 | 無 | ❌ F3 |
| 視覺：暗色基調、Canvas 走廊、暈影 | FE6（tokens）、FE2（無暈影規格） | FE2-TORCH-VIGNETTE、FE2-1 | ⚠️ F7a |
| 唯讀敘事日誌 | FE1（ANSI 日誌）、M9 | — | ⚠️ 仍有指令框／終端（F4） |
| 戰鬥回饋：破防震動、打斷電弧、連攜大字 | FE3 | FE3 任務 | 未逐項核對 FE3 |

---

## 四、反向檢查：L3 對 L2 的主要缺口

- **未被任何 L3 追蹤的 L2 章節：** 各手冊的「未來路線圖」（M2／M4／M5／M6／M7／M8／FRONTEND_LAYOUT_GUIDE）。L3 的「1:1 鏡像」規則（[README L4](../3_task_lists/README.md#L4)）沒有說明路線圖是否在範圍內。建議明確標示：路線圖為「非規範性」，並移出 L2 或集中於 `FUTURE_IMPROVEMENTS.md`。
- **只有 L3、沒有 L2 的實作：** 見 F7a/b。

---

## 五、已確認一致的部分

- 19 個模組（10 後端、6 前端、3 Schema）與 L3 清單的檔名、連結目標完全對應；`check-doc-links.ps1` PASS。
- 五大核心目標、500ms ATB（以 M3／M9 為準）、Poise 破防 1.5x、一元一次圓桌、3.5 秒連攜窗口、40 格唯一行囊、`schemaVersion 3`、7 階稀有度、5+2 裝備槽（M6）在 L1/L2/L3 主要出處一致。
- L3 各模組皆有「程式碼單一真相源 → 已完成 → 待辦（含驗收標準）」的固定結構，格式本身良好，適合直接擴充顆粒度。

---

## 六、建議修正順序

1. **L1 裁決（先做，否則下游無法收斂）：**
   - 設施清單與唯一 ID（F2）；
   - 「單存檔」的語意與槽位數、地牢內是否可存檔（F6）；
   - SAN／精神萎靡是否保留與其門檻（F7c）；
   - 技能體系與流派的唯一命名（F1）；
   - 補上戰敗懲罰的具體規則、晶片加載與基因覺醒的最小機制描述。
2. **清除 MUD 殘留（F4）：** 先改 FRONTEND_LAYOUT_GUIDE 的狀態機、版面與指令框；再處理 FE1／UI_PAGE_STRUCTURE 的 ANSI／終端；最後更新 ARCHITECTURE_SUMMARY、M4、M6、M7、M8 的類別清單；L3 同步更新「程式碼單一真相源」並標註歷史項目。
3. **統一 L2 的共用契約（F5、F8）：** 建立 M9「動作總表」；統一 Tick、格數、槽位、`dangerRate`、五維縮寫、ID 命名。
4. **補齊 L2 缺口（F1、F6、F7）：** 16 神通資料契約、地牢機關檢定欄位、戰敗規則、晶片加載機制、序章／敘事資料契約、手機版面。
5. **重建 L3（F9）：** 以修正後的 L2 逐模組拆出具唯一 ID、狀態、驗收條件的任務；為 FRONTEND_LAYOUT_GUIDE／UI_PAGE_STRUCTURE／ARCHITECTURE_SUMMARY 補清單；為 L2 路線圖定義處理方式。
6. **維護性修正（F10、F11）：** 更新儀表板標籤；定義完成度計算方式與測試基準來源；為 L1 加入穩定需求 ID 與向 L2 的指向欄位；移除 L1 中的程式識別碼。
7. **流程防呆：** 在 [check-doc-links.ps1](../../scripts/check-doc-links.ps1) 之外，新增「L1 關鍵詞出現於 L2、L3」的比對檢查（例如 16 神通名稱、設施 ID），讓 F1 這類缺漏能被自動偵測。

---

## 七、對前兩份報告的查證

### Copilot_Review.md

| 發現 | 查證結果 |
|---|---|
| #1 去除 MUD 與規格衝突 | ✅ 屬實，但範圍更大（F4）；實際最嚴重的是 FRONTEND_LAYOUT_GUIDE，未被提到 |
| #2 儀表板連結使用不存在的檔名 | ❌ **不正確**。連結**目標**都存在，`check-doc-links.ps1` PASS（251/251）；過時的只是顯示文字（F10）。Codex 的描述才正確 |
| #3 缺需求→模組追溯 | ✅ 屬實，且我進一步確認造成實際漏接（F1） |
| #4 M1 任務未覆蓋規格 | ✅ 屬實，F9 |
| #5 七種視圖與 FE1–FE6 無對照 | ❌ **不正確**。[UI_PAGE_STRUCTURE §3 表格](../2_specifications/frontend/UI_PAGE_STRUCTURE.md#L164-L172) 的「對應規格手冊」欄已把 7 個視圖映射到 FE1–FE6，FRONTEND_LAYOUT_GUIDE §二 也有 FE 落點表 |
| #6 除役語意混入任務與測試索引 | ✅ 屬實，F4 |
| 「主要模組皆有任務檔、無缺少」 | ✅ 屬實；但「導覽類文件不構成獨立交付」的判斷我不同意（F9 末段） |

### Codex_Review.md

| 發現 | 查證結果 |
|---|---|
| P1 看板標籤／測試名／百分比 | ✅ 屬實（F10） |
| P1 M9 協定與任務矛盾 | ⚠️ 大致屬實，但 `INTERACT_FACILITY`／`REST_PARTY` 是出現在 **M1 任務 M1-2**，不是 M9 任務；且實際衝突不止兩份文件，而是至少六處（F5） |
| P1 MUD 殘留（M1/M9/FE2/README） | ✅ 四處皆查證屬實；並補充 FRONTEND_LAYOUT_GUIDE、FE1、ARCHITECTURE、M4、M6、M7 等更多處（F4） |
| P1 需求缺逐項追溯 | ✅ 屬實 |
| P1 L3 顆粒度不足 | ✅ 屬實（F9） |
| P2 多槽位語意 | ✅ 屬實，另補充 FRONTEND「五大槽」與 M8-1「Slot 1~3」三方不一致（F6） |
| P2 進度缺證據 | ✅ 合理，F10 |

### 兩份報告都沒有發現的重大問題

16 神通完全缺席與三套流派分類（F1）；L1 設施清單與 M1／M9 不同（F2）；L1 與 L2 版面衝突與手機版缺席（F3）；戰敗懲罰缺漏、地牢內存檔限制缺漏（F6）；L3 已完成但 L2 無依據的反向漂移（F7）；Tick 200/500、30/40 格、五維縮寫、`dangerRate` 等 L2 內部不一致（F8）；M2 機關檢定在 L2/Schema 無欄位（F6）。

---

## 八、審查限制

- 本次完整閱讀：L1 全部 4 份；L2 的 MODULES_MAP、ARCHITECTURE_SUMMARY、M1、M2、M4、M5、M6、M7、M8、M9、FE4、FRONTEND_LAYOUT_GUIDE、UI_PAGE_STRUCTURE、SKILL_SCHEMA，DUNGEON_SCHEMA（前 120 行）；L3 的 README、M1–M9、M3.1、FE1、FE2、FE4、FE5、DUNGEON／SKILL 任務。
- **僅以關鍵字比對或局部閱讀：** M3、M3-1 手冊本文（只讀開頭與關鍵字命中處）、FE2、FE3、FE5、FE6 手冊、ITEM_SCHEMA、FE3／FE6／ITEM 任務、FUTURE_IMPROVEMENTS。這些檔案中可能還有本報告未列出的差異，尤其是 FE3 與 FE6 對 L1 視覺／戰鬥回饋需求的承接。
- 未讀取程式碼與資料檔，因此「L2 描述的是現況或目標」「L3 已完成是否屬實」皆無法判定，只能指出文件之間的矛盾。
- 關鍵字比對以繁體中文原文進行；若同一概念在文件中使用不同用詞，可能有漏判，但 F1 的 16 項神通以名稱為鍵、並以 M5／SKILL_SCHEMA 的流派分類人工交叉確認，結論可靠。
