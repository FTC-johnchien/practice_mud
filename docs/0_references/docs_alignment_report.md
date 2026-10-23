# 四層文件（L1~L4）對齊審查報告

> 日期：2026-10-23｜範圍：`docs/1~4`（36 份 md）＋ 對照 `src/main/resources/data` 與 Java 模型。
> 本次**未跑測試**，僅靜態比對文件與資料檔；僅讀取，**沒有修改任何檔案**。

## 結論

**四層方向一致，但資料契約層（L2 Schema）與真實資料/L4 任務卡明顯脫節。** 最嚴重的是：L2 的 `DUNGEON_SCHEMA` 幾乎是「理想規格」，與實際載入的 JSON 欄位名完全不同；L4 的 VS-01 任務卡又與 L2 Schema、實際資料三方互不一致。

## 一、實測發現（按嚴重度）

### 🔴 A. 地牢：Schema ≠ 真實資料 ≠ L4

| 項目 | `DUNGEON_SCHEMA.md` | 實際 `mozhu_mines_b1f.json` | L4 Task 1 |
|---|---|---|---|
| 主鍵 | `id`/`zoneId`/`floor` | `floorId`（無 zoneId/floor） | — |
| 起點 | `startPosition{x,y,facing}` | `startCoord{x,y}` + `startFacing` | — |
| 遇敵 | `encounterRate` 0~1、`encounterPool[]` | `encounters.dangerRate: 15`、`encounters.mobs[]` | `encounterRate=0.15` |
| 圖例描述 | `loreDescription` | `description` | — |
| 圖例 type | `CHEST` | `TREASURE`、`TRAP` | — |
| `E` 符號 | 未定義 | `EVENT`＝**墨家十戒非攻殘碑** | **外神卵塊** |
| `B` 符號 | `@`=BOSS | `B`=BOSS 祭壇 | **偽地殼生化腐殖** |
| `events` | 未定義 | 無，改用圖例 `eventId` | 要求注入 `events` |
| `exitRoomId` | `mozhu_mines:slave_camp` | `mozhu_mines:mine_entrance` | — |

- 實際資料中，外神卵塊是 `X`（TRAP），`B` 是 Boss 祭壇——L4 的符號語意與現況**相反**。
- Boss 名稱／地圖名稱也不同：Schema 範例「黑煞宗地底礦脈」vs 資料「墨竹山廢棄靈石礦坑」（Sprint 目標要求改寫為黑煞宗，尚未做）。
- `DataIntegrityValidator` 讀的是 `startCoord`、`eventId`、`exitRoomId`，不是 Schema 欄位 → **Schema 不可用於驗證現有資料**。

### 🔴 B. 技能：ID 與欄位三方不同

| | L2 `SKILL_SCHEMA` | L4 Task 3 | 實際 `party_skills.json` |
|---|---|---|---|
| 神經超頻步 ID | `skill_neural_overclock_step` | `neuro_overclock_step` | `skill_neural_overclock_step` |
| 強殖角質刃 ID | `skill_bio_keratin_blade` | `bio_carapace_blade` | `skill_bio_keratin_blade` |
| 連攜欄位 | `isPrimer`/`isDetonator`（boolean） | `comboRole` | `comboRole: PRIMER/DETONATOR` |
| 削韌 | 25 / 45 | 15 / 25 | 25 / 45 |
| 成本欄位 | `costs{mp,sp,hp}` | — | `costType` + `costValue` |
| 傷害屬性 | `element` | — | `damageType` |

→ **實際資料與 L2 範例的數值一致，L4 才是錯的**（ID、削韌值都與資料不符）。L2 的欄位形狀（`isPrimer`、`costs`、`element`）則與資料不同。
→ 更嚴重：**這兩個技能其實已經存在於資料中**，且 L4「已完成」區的 ALIGN-SKILL-SCHEMA 也宣稱已注入，但 Task 3 仍為未勾選待辦且用不同 ID → 看板重工／誤導。

### 🔴 C. 物品：L2 與 L4 ID 不同，目標資料不存在

- L2 範例晶片：`item_chip_qianxing_tracker`；L4 Task 4：`thousand_stars_tracking_chip`。
- `data/` 內兩者都**找不到**（僅 `facilities.json` 提到「千星」）→ Task 4 確實尚未實作，但 ID 必須先統一。
- `ItemAffix` record 為 `{id,type,name,statKey,value}`，對應 L2 的**實例**結構；L2 Schema 卻只定義**模板**（`affixPool`＋`minValue/maxValue`），無轉換說明，且 `affixes` 範例不在 schema 內。
- `itemType: CONSUMABLE` 但 `slot` 為 required → 道具（任務道核）無法合規。

### 🟠 D. SAN／魔化語意（L1 vs L2）

- L1 `GAMEPLAY_CORE_LOOPS`：取消魔化；L1 `CYBERPUNK_CULTIVATION_SPEC` 仍有 SAN 代價（#1、#10、設計哲學 §3）。
- L2 仍保留 SAN 護盾並**命名不一**：
  - M4：SAN=0 →「精神震懾」（受創 +20%、韌性回復 -30%）；閾值 25
  - MODULES_MAP / L4 / 程式：「精神萎靡」（輸出 -15%）；M3：閾值 30%
  - FE2/FE6、M2、M3 仍有「SAN 扣減」「道心 SAN」「SAN 崩潰」措辭
- 屬性縮寫：L1 `SPI`/`STR`，M4 `PER`。
- 建議：**明確裁定「SAN 保留為資源，歸零 = 單一 debuff」並統一名稱與數值**，而非整個刪除（程式與 L2 已依此實作，刪 SAN 成本高）。

### 🟠 E. L1 技能分類 vs Schema 流派

L1 四大體系：網路安全／生物駭客／光學認知／底層崩潰；Schema 四流派：`CYBER_HACK`／`GENE_BIO`／`MECHA_CULT`／`VOID_PSI`。名稱與數量對不上（`MECHA_CULT` 在 L1 找不到對應）。
另外 L1「燃魂溯光·剎那千秋步」(神經元超頻) 是「HP 持續損耗的加速 buff」，L2/資料的「神經超頻步」卻是「PRIMER 單體突刺」→ 同名不同機制。

### 🟠 F. L3/L4 狀態矛盾

- `ACTIVE_TASKS.md` §已完成：Milestone 3 (F1-02)、F1-04 已 `[x]`；§「後續備選」仍列「Milestone 3 待執行 M5/M4/M3」「Milestone 4 style.css 除役」→ **重複且過期**。
- 標題「文件對齊**全面完工**」但 L3 `codebase_audit_and_alignment_tasks.md` 全部 `[ ]`，且其「基線」寫成 100% 落實。
- `codebase_audit_and_alignment_tasks` Task 2.1/2.2 自稱「落實 VS-01 規劃」，但 VS-01 在 L4 是「待命」→ 兩個「下一步」來源。
- 測試數字散落：373 / 375 / 376 / 「376+」/ 324；`codebase_audit` 稱「88 個 Java 源碼檔」，實測 `src/main` 為 **288**、`src/test` 為 89（疑似混淆）。
- `ACTIVE_TASKS` DoD 仍寫 `docs/manual/`（目錄已不存在）。
- L3 `four_tier_alignment_and_execution_plan` 的標準「每份 < 300 行」：`M3_1` 實測 **390 行**、`MODULES_MAP` 291、`M3` 278。

### 🟡 G. 連結 / 索引

- ✅ 確認失效：`tech_debt_and_hardening.md`（L19、L101）、`content_and_combat_roadmap.md`（L18）指向不存在的 `../0_archive/references/Copilot_Review.md`；實際位置是 `docs/0_references/Copilot_Review.md`。
- `WORLD_AND_LORE_BIBLE.md`、`CYBERPUNK_CULTIVATION_SPEC.md` 寫 `docs/references/…`，實際為 `docs/0_references/`。
- `docs/README.md` 目錄樹與導覽**完全沒有列出 `0_references/`**。
- `docs/0_archive/` 內舊檔仍引用 `docs/references/`、`docs/manual/`（歷史檔，建議在封存首頁統一註記，不必逐一修）。

## 二、對兩份 Review 的評估

| 項目 | Copilot | Codex | 我的判斷 |
|---|---|---|---|
| 技能 ID／連攜欄位／削韌不一致 | ✅ 提出 | 只說「ID 找不到」 | **Copilot 準確**；補充：真相在資料檔，L4 才是錯的 |
| 地牢 events/E/B 契約缺口 | ✅ | ✅ | 兩者都低估，實為**整份 Schema 欄位名與資料脫節** |
| 裝備模板 vs 實例 | ✅ | — | 採納 |
| SAN 殘留 | ✅（建議移除玩家 SAN） | — | **部分不採納**：L2/程式已用 SAN，建議「保留＋統一」 |
| L3/L4 多個待辦來源 | ✅ | ✅ | 採納 |
| 失效連結 | ✅ 正確 | ❌ 稱「根 README 指向不存在的 `docs/README.md`」 | **Codex 此項過期**：`docs/README.md` 存在且連結有效 |
| `0_references/` 未列入索引 | — | 部分 | 採納（兩者都沒明說） |
| M3-1 約 316 行 | 數字過期 | — | 現為 **390 行** |
| 前端 `index.html` 雙載入舊腳本、Java 版本矩陣 | — | ✅ | 本次未驗證，保留為待查 |
| 只重 Phase/流程、缺少具體差異表 | ⚠️ | ⚠️ | 兩者皆為「執行方案」，沒有逐欄位對照；本報告補上 |
| 「新增 link checker／事實來源核對表」 | ✅ | ✅ | 採納，但建議只做**一個輕量 PowerShell/測試**，避免過度工程 |

## 三、我的建議

### 決策先行（需要你拍板，見下方問題）
1. **權威來源 = 實際資料檔 + Java 模型**，Schema 向資料靠攏（而非反過來改資料）。理由：已有 376 測試與存檔相容負擔。
2. SAN 定位。
3. 是否沿用 `exitRoomId` 命名。

### 執行順序
1. **L2 Schema 向資料對齊**（最優先）
   - `DUNGEON_SCHEMA`：改為 `floorId/startCoord/startFacing/encounters{dangerRate,mobs}/legend.description/eventId`，補 `TREASURE/TRAP/EVENT` 並保留 `E/B/X` 實例；明確 `exitRoomId` 語意。
   - `SKILL_SCHEMA`：改 `comboRole`、`costType/costValue`、`damageType`，修正範例使其即為資料檔內容。
   - `ITEM_SCHEMA`：拆「模板」與「實例（ItemAffix）」、補任務道具範例（無 slot）。
2. **L4 修正任務卡**：ID／削韌／符號語意改以資料為準；Task 3 改為「驗證既有技能＋戰鬥整合測試」（技能已存在）；`events` 欄位若無實作則移出驗收前提；統一 `dangerRate` 表達。
3. **L1**：SAN 措辭統一；在 `CYBERPUNK_CULTIVATION_SPEC` 加「設計分類→程式流派」對照表，並標註 16 技能中僅 2 項已實作；同名技能機制衝突擇一。
4. **L3/L4 整理**：移除 ACTIVE_TASKS「後續備選」過期項；L3 `codebase_audit…` Phase 2 標註「依賴 VS-01 啟動」；測試數字改為「以 `.\test.ps1` 最近一次結果為準」（不寫死）。
5. **索引/連結**：改 3 處失效連結、2 處 `docs/references/`、`docs/README.md` 加入 `0_references/`；`M3_1` 拆分或把 300 行改為「建議」。
6. **防漂移**：加一個極簡連結檢查腳本（可選）。

> [!NOTE]
> 以上皆為建議；尚未修改任何檔案。待你確認決策後可直接執行。
