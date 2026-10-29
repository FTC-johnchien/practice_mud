# 🛠️ 《深淵仙途》四層級工程文件對齊執行任務卡（供 `gemini 3.8` 單步執行）

> **文檔層級**：Level 4 — 專項文件治理執行任務卡（受指導員單步闖關簽核管制；執行模型僅依本檔步驟執行）
> **與 [`ACTIVE_TASKS.md`](./ACTIVE_TASKS.md) 之關係**：本任務卡僅於「四層級文件對齊專項」期間生效，全程不修改任何業務程式碼；待 Step 1 ~ Step 7 全數通過簽核後，本檔將與主控計劃一併封存至 `docs/0_archive/alignment_history/`，恢復 [`ACTIVE_TASKS.md`](./ACTIVE_TASKS.md) 單一業務看板。
> **問題溯源報告**：[`docs_hierarchy_review_report.md`](../0_references/docs_hierarchy_review_report.md)

---

## 🚨 執行模型（`gemini 3.8`）最高作業鐵律（必讀）

1. **🛑 單步停機鎖（Single-Step Gate Lock）**：
   - **你每次只准執行指導員在對話中點名派發的「單一 Step」（例如僅執行 `Step 1`）！**
   - 完成該 Step 內的所有項目與自檢指令後，請輸出「本步修改檔案清單與自檢結果」並**立即停下（Stop）等待指導員驗收**，**絕對禁止**自行跨步修改下一關檔案！
2. **🚫 五條安全紅線（Scope & Safety Lock）**：
   - **禁改業務代碼與禁讀指導員專屬檔**：僅限修改 Markdown 文件（`.md`）與 [`check-doc-links.ps1`](../../scripts/check-doc-links.ps1)；**嚴禁修改** `src/main/`、`src/test/` 下任何 `.java`、`.json`、`.html`、`.css`、`.js`，且**嚴禁讀取或修改**指導員專用之 `.agents/skills/`、`docs/3_task_lists/DOCS_ALIGNMENT_MASTER_PLAN.md` 與 `scripts/verify-doc-alignment-gates.ps1`。
   - **禁碰憲法保護鎖**：**絕對禁止修改、縮編或刪除** [`GEMINI.md`](../../GEMINI.md) 第 21~37 行「第 7 條：100% 資料驅動與零硬編碼鐵律（不可刪除／修改保護鎖）」，以及 [`documentation_alignment_guardrails.md`](../../.agents/rules/documentation_alignment_guardrails.md)。
   - **禁改封存與外部參考檔**：嚴禁修改 `docs/0_archive/` 與 `docs/0_references/` 下任何檔案。
   - **單檔行數守則**：修改後一般文件須 `< 350` 行；高內聚規格手冊（如 [`M3_1_BUFF_AND_STATUS_EFFECTS.md`](../2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md)、[`MODULES_MAP.md`](../2_specifications/MODULES_MAP.md)）豁免上限為 `<= 400` 行。
   - **全繁體中文與四級狀態標籤**：一律使用標準繁體中文；區分現況與規劃時統一標註 `【現行已驗證】`、`【已決議待實作】`、`【待深入討論】`、`【歷史／已除役】`；所有文檔內連結一律使用**相對路徑**（禁止寫死本機絕對磁碟路徑，確保跨電腦 Git 同步有效）。

---

## 📌 零、使用者已拍板之 6 大設計裁決基準（唯一權威判準）

| 編號 | 主題 | 拍板定案內容 |
| :---: | :--- | :--- |
| **D1** | **PC 桌機與手機版面** | 延後專題討論（低優先）。現行 [`index.html`](../../src/main/resources/static/index.html) 的「**左右雙主區（左主舞台切換＋左下 5 人小隊卡；右全高可摺疊日誌，支援 `⮃ 換邊`）**」標為 `【現行已驗證】`；L1 的「**PC 上三欄（左雷達、中視口、右小隊）＋底欄唯讀日誌**」與「**手機直向底部 5 分頁常駐 Tab Bar**」標為 `【待深入討論／已決議待實作】`。清除明顯筆誤（`FE2` 左右顛倒、`FE4` 左側日誌、`FE6` 四方出口按鈕 `.exit-btn`）。 |
| **D2** | **SAN 精神護盾規則** | 嚴格貫徹 L1 Option A：僅保留 `SAN = 0` 觸發單一輕度 Debuff `【精神萎靡】`（傷害 `-15%`，枚舉 `WEAKENED`）。移除 `M4` 的 `SAN <= 25 (-10%)` 階層、清理 `M3` / `FE5` 舊門檻描述，並刪除 `MODULES_MAP` 與 `Task M2-2` 的「低 SAN 幻覺／偽裝寶箱怪」。 |
| **D3** | **破防數值與適用對象** | 以 [`M3_1_BUFF_AND_STATUS_EFFECTS.md`](../2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md) 為準：**僅限 Boss 與特定菁英怪具備護體罡氣減傷（未破防僅受 `25%~33%` 傷害，實作 `1/3`；破防力竭持續 `6~8 秒`，實作 `7 秒 / 14 Ticks`，期間恢復 `100%` 受擊並享 `1.5x` 暴擊）**；一般普通小怪（`NORMAL`）無護體減傷。針對現行 [`BattleEnemy.java`](../../src/main/java/com/example/htmlmud/combat/BattleEnemy.java) 普通怪仍預設 `poise=30` 套用 `1/3` 減傷的落差，於 `M3_COMBAT_ENGINE_TASKS.md` 新增 `Task M3-3` 待辦追蹤。 |
| **D4** | **技能契約與孤兒任務** | **4-1**：`SKILL_SCHEMA.md` 的 `treeId` 對齊 `TREE_01 ~ TREE_17` 完整名，合體技與領域 `id`/數值對齊 [`SYNERGY_AND_COMBOS.md`](../2_specifications/skills/SYNERGY_AND_COMBOS.md)（註記 `party_skills.json` 主鍵 `combo_water_fire_taichi` 以 `aliases` 相容）。<br>**4-2**：`TREE_01 ~ TREE_17` 第 7 欄表頭由 `連攜角色` 改為 `戰術／連攜定位`，於 `SKILL_SCHEMA.md` 明定僅 `PRIMER`/`DETONATOR` 填 `comboRole`，其餘填 `tags`（`comboRole` 填 `NONE`）。<br>**4-3**：移除 `M5_TASKS` 舊重疊 `Task M5-1 (32+ 賽博天賦樹)`。<br>**4-4**：移除孤兒 `Task M3-2 (兵刃換彈)` 及違反憲法紅線之 `M7` §6.1 與 `Task M7-1 (動態熱重載)`。 |
| **D5** | **主角強殖基因覺醒** | 正式併入第 14 樹 [`TREE_14_CHITIN_BLADE.md`](../2_specifications/skills/4_cthulhu/TREE_14_CHITIN_BLADE.md) 與主角【萬象核心插槽 (`Core Socket`)】體系，不另設獨立第二條基因進度條。 |
| **D6** | **檢核腳本升級** | 擴充 [`check-doc-links.ps1`](../../scripts/check-doc-links.ps1)，納入根目錄 `GEMINI.md` 連結、反引號實體路徑存在性檢查，以及 `data_schemas/*.md` 內嵌 JSON 語法驗證。 |

---

## 🗂️ Step 0：環境定位與真實路徑對照字典（修改路徑必查，嚴禁自創檔名）

所有指令一律在 Git 倉庫根目錄 `practice_mud` 下執行（跨電腦請先確認當前目錄為 `practice_mud`）：
- **後端一級模組套件（`src/main/java/com/example/htmlmud/`）**：
  - M1 營地：`town`（`TownHubService`, `CognitiveProxyService`, `TownHubDto`, `FacilityDto`, `PerceptionTier`）
  - M2 地牢：`dungeon`（`DungeonManager`, `DungeonFloor`, `DungeonPosition`, `DungeonTile`, `GridCoord`, `GridDirection`）
  - M3 / M3.1 戰鬥與狀態：`combat`（`CombatRules`, `DrpgCombatLoop`, `DrpgBattleService`, `BattleEnemy`, `CombatFsmService`, `ComboResolver`, `BuffSettlementService`, `ActiveBuff`）
  - M4 小隊陣法：`party`（`PartyService`, `Party`, `PartyMember`, `PartyInventory`, `FormationEngine`, `GambitRule`, `PartyMemberSkill`）
  - M5 成長技能：`progression`（`XpProgressionService`, `DrpgSkillRules`, `SkillDefinition`）
  - M6 物品經濟：`item`（`UnifiedPartyEconomyService`, `ItemFactory`, `ItemInstance`, `ItemAffix`, `EquipmentSlot`）
  - M7 範本看門狗：`template` 與 `template.model`（`TemplateCatalog`, `TemplateRepository`, `TemplateReader`, `DataIntegrityValidator`）
  - M8 存檔持久化：`save`（`SaveGameService`, `SaveData`, `SaveSlotDto`, `SaveMigrationPipeline`；自動存檔真實檔名為 `saves/autosave.json` 與 `saves/autosave.json.bak`）
  - M9 狀態與通訊管線：`pipeline`（`CommandDispatcher`, `GameStateBroadcastService`, `MudWebSocketHandler`, `SessionRegistry`）
- **靜態資料真實路徑（`src/main/resources/data/`）**：
  - 營地設施：`data/zones/newbie_village/facilities.json`（舊 `facilities` 路徑【歷史／已除役】）
  - 全域物品目錄：`data/global/items/`（子目錄 `weapons/weapons.json`, `armors/`, `accessories/`, `consumables/`, `materials/`, `currencies/`, `quest/quest_items.json`；舊單檔路徑【歷史／已除役】）
  - 小隊技能與戰術：`data/party/party_skills.json`, `data/party/gambit_presets.json`
  - 地牢地圖：`data/dungeons/mozhu_mines_b1f.json`, `data/dungeons/taiyin_tomb_b1f.json`
- **前端真實 CSS 七層檔案（`src/main/resources/static/css/`）**：
  1. `style-01-tokens-and-shell.css`
  2. `style-02-combat-arena.css`
  3. `style-03-battle-actions.css`
  4. `style-04-main-menu.css`
  5. `style-05-town-and-hub.css`
  6. `style-06-shop-and-responsive.css`
  7. `style-07-battle-fx.css`
- **前端真實核心 JS 檔案（`src/main/resources/static/js/`）**：
  - 入口：`app.js`
  - 核心 (`js/core/`)：`state-store.js`、`event-bus.js`、`cmd-dispatcher.js`、`constants.js`、`ui-utils.js`、`buff-hud-helper.js`
  - 網路 (`js/network/`)：`websocket.js`
  - 特效 (`js/fx/`)：`battle-fx-director.js`、`floating-text.js`、`fx-geometry.js`、`fx-presets.js`
  - 面板 (`js/panels/`)：`town-panel.js`、`dungeon-panel.js`、`battle-panel.js`、`party-hud-panel.js`、`message-log-panel.js`（舊動作列面板檔名【歷史／已除役】）
  - 彈窗 (`js/modals/`)：`party-modal.js`、`party-equipment-tab.js`、`party-skills-tab.js`、`party-formation-tab.js`、`party-tactics-tab.js`、`bag-drawer.js`、`skill-drawer.js`、`shop-modal.js`、`save-modal.js`、`victory-modal.js`

---

## 🛡️ Step 1：升級看門狗腳本 [`check-doc-links.ps1`](../../scripts/check-doc-links.ps1)（建立 TDD 紅燈基準）

- **編碼與可攜性防呆**：`Write-Host` 提示文字請維持純英文 ASCII（`SCAN:` / `PASS:` / `FAIL:`）或確保 `.ps1` 以 **UTF-8 with BOM** 儲存，避免 Windows PowerShell 5.1 無 BOM 中文亂碼；路徑一律透過 `$MyInvocation.MyCommand.Path` 相對推導 `$WorkspaceRoot`。
- **擴充三項靜態檢查（任一項失敗則輸出明細並 `exit 1`，全數通過則 `exit 0`）**：
  1. **檢查 A — Markdown 連結有效性**：
     - 掃描範圍：`docs/**/*.md`（**排除 `0_archive` 與 `0_references`**）**加上根目錄 `GEMINI.md`**。
     - 先將單行內反引號片段（`` `...` ``）濾除後再擷取 Markdown 連結，略過 `http://`、`https://`、`mailto:`、`#` 及示例佔位 `path/file.md`、`path/to/file`。
  2. **檢查 B — 反引號實體檔案路徑存在性**：
     - 掃描範圍：`docs/**/*.md`（**排除 `0_archive`、`0_references` 以及專項治理任務檔 `DOCS_ALIGNMENT_*.md`**）。
     - 正規表達式擷取單行內形如 `` `((?:src/main/|src/test/|static/|js/|css/|data/|scripts/)[^`*\s]+\.(?:java|js|css|html|json|ps1|md))` `` 的路徑（略過含 `*` 萬用字元與執行期動態產生的 `saves/`）。
     - **豁免條件**：若該行包含 `【已決議待實作】`、`【候選提案】`、`【歷史／已除役】`、`除役`、`封存`、`已刪除` 或 `歸檔`，跳過檢查。
     - **路徑解析映射**：`static/...` → `src/main/resources/$rawPath`；`js/...` 或 `css/...` → `src/main/resources/static/$rawPath`；`data/...` → `src/main/resources/$rawPath`；其餘（`src/...`、`scripts/...`） → `$WorkspaceRoot/$rawPath`。
  3. **檢查 C — `data_schemas/*.md` 內嵌 JSON 語法合法性**：
     - 掃描 `docs/2_specifications/data_schemas/*.md`，以多行正則 `(?s)\x60\x60\x60json\r?\n(.*?)\r?\n\x60\x60\x60` 擷取每個 JSON 區塊執行 `$block | ConvertFrom-Json | Out-Null`，捕捉錯誤並回報檔名與區塊序號。
- **本步完成後自檢並立即停下**：執行 `powershell -ExecutionPolicy Bypass -File .\scripts\check-doc-links.ps1`，確認腳本成功抓出目前尚未修復的 `GEMINI.md` 斷鏈、`ITEM_SCHEMA.md` JSON 錯誤與虛構路徑（此時 Exit Code 必為 `1`），回報輸出摘要後停機待審。

---

## 🧭 Step 2：校準 Level 1 需求層與全域導覽（`docs/1_requirements/` & `docs/README.md`）

1. **[`WORLD_AND_LORE_BIBLE.md`](../1_requirements/WORLD_AND_LORE_BIBLE.md)**：
   - 於 §4.2「微量異形基因容器」（第 79~83 行）及 §6 第 5 點（第 109 行）補充：主角微量強殖基因覺醒在系統機制上**正式併入四大淵源之【強殖幾丁質專精樹】與主角【萬象核心插槽】體系**，不另設獨立進度條（D5）。
   - 於 §5 階段三（第 93~95 行）微調為「煉丹房／礦坑密室血肉祭壇反殺大師兄」，消除與地牢首領戰場景之落差。
2. **[`GAMEPLAY_CORE_LOOPS.md`](../1_requirements/GAMEPLAY_CORE_LOOPS.md)**：
   - 第 18~22 行表格「涉及系統與模組」欄：將具體 Java 類別／檔名（`CombatRules`、`DrpgCombatLoop`、`ComboResolver`、`party_skills.json`、`XpProgressionService`、`schemaVersion 3`）改為模組概念描述，對齊 L1 不寫死程式識別碼原則。
   - 第 19 行與 §3.2（第 79 行）：依 D3 明定「**護體罡氣減傷與破防力竭僅適用於深淵菁英與首領（Boss），普通雜兵無護體減傷以確保爽快清場**」。
   - 第 22 行：依 D5 明定「微量異形強殖基因覺醒（併入強殖幾丁質專精樹與萬象核心插槽）」。
   - §4 道基五維（第 86~90 行）：統一標註程式屬性縮寫——體質 (`CON`)、真元 (`ESS` / 屬性鍵 `INT`)、神識 (`SPI` / 屬性鍵 `WIS`)、根骨 (`STR`)、身法 (`DEX`)。
3. **[`UI_UX_REQUIREMENTS.md`](../1_requirements/UI_UX_REQUIREMENTS.md)**：
   - 於 §2「PC 橫向螢幕佈局」與 §3「手機直向自適應佈局」標題旁標註 `【待深入討論／演進目標】`，並加過渡說明：「現行實作版本先採左右雙主區（左主舞台切換＋左下小隊卡、右全高可收合日誌）過渡架構，三欄式與手機常駐底部 5 分頁導航列留待專題定案後推進」（D1）；移除第 65 行具體 CSS class 名 `.torch-darkness-vignette`。
4. **[`docs/README.md`](../README.md)**：
   - 於目錄樹補入 `skills/` 子目錄（含 `README.md`、`SYNERGY_AND_COMBOS.md` 與 `1_cultivator/` ~ `4_cthulhu/` 17 專精樹）及 `3_task_lists/CODE_CONVERGENCE_AND_REFACTORING_TASKS.md`；於 §Level 2 核心文件列表補上 `2_specifications/skills/` 相對連結。
- **本步完成後立即停下**，回報修改檔案清單待指導員驗收。

---

## 📜 Step 3：修復 Level 2 資料架構與 17 專精技能樹（`data_schemas/` & `skills/`）

1. **[`ITEM_SCHEMA.md`](../2_specifications/data_schemas/ITEM_SCHEMA.md)**：第 63 行 `"affixPool"` 結束大括號補上逗號 `},`，修復第 64 行 `"lenses": {` JSON 語法。
2. **[`SKILL_SCHEMA.md`](../2_specifications/data_schemas/SKILL_SCHEMA.md)**（D4-1 & D4-2）：
   - 全檔替換簡寫 `treeId`：`tree_overclock` → `tree_time_overclock`、`tree_chitin` → `tree_chitin_blade`、`tree_water` → `tree_water_frost`、`tree_fire` → `tree_fire_flame`、`tree_wood` → `tree_wood_life`。
   - 對齊 [`SYNERGY_AND_COMBOS.md`](../2_specifications/skills/SYNERGY_AND_COMBOS.md)：`"id": "combo_2p_taiji_water_fire"` → `"id": "combo_water_fire_taiji"`（附註現行 `party_skills.json` 主鍵 `combo_water_fire_taichi` 透過 `aliases: ["combo_water_fire_taiji"]` 相容），`"allDetonateBonusMultiplier"` 改為 `2.5`、補上 `"poiseDamage": 60`；`"id": "domain_4p_elemental_cascade"` → `"id": "domain_quad_celestial_blade"`，`"formationEnergyCost"` 由 `90` 改為 `100`。
   - 明定 `comboRole` 映射規則：`comboRole` 嚴格限定 `"NONE" | "PRIMER" | "DETONATOR"`；專精樹表「戰術／連攜定位」欄若為 `INTERRUPT`、`BARRIER`、`HEAL`、`CONTROL`、`STEALTH`、`DEFENSE`、`PURIFY`、`EXECUTE`、`BUFF`、`TACTICAL`、`ULTIMATE`、`POISE_BREAK` 等戰術職能，落盤時 `comboRole` 一律填 `"NONE"`，並將該職能寫入 `"tags"` 陣列。尚未全量落入 Java 之擴充契約標題旁加註 `【已決議待實作】`。
3. **批次更新 `docs/2_specifications/skills/` 下 `TREE_01` ~ `TREE_17` 共 17 檔**（D4-2）：
   - 將「二、四階主動戰技規格表」表頭 `| 連攜角色 |` 統一改為 `| 戰術／連攜定位 |`。
   - 修正 [`TREE_09_PLASMA_CORE.md`](../2_specifications/skills/2_engineer/TREE_09_PLASMA_CORE.md) 第 20 行錯字 `CD / 前討` → `CD / 前搖`。
- **本步完成後立即停下**，回報修改檔案清單待指導員驗收。

---

## ⚙️ Step 4：校準 Level 2 頂層架構與後端模組手冊（`ARCHITECTURE_SUMMARY`、`MODULES_MAP`、`backend/`）

1. **[`ARCHITECTURE_SUMMARY.md`](../2_specifications/ARCHITECTURE_SUMMARY.md)**：更新 §1 目錄結構呈現 `【現行已驗證】` 一級業務套件（`town` ~ `pipeline`）與過渡層；更新 §4.4 移除將「拆分 `domain.service`」列為未來的過時敘述；統一戰鬥 Tick 為 `500ms`。
2. **[`MODULES_MAP.md`](../2_specifications/MODULES_MAP.md)**：
   - 第 73 行營地設施路徑改為 `src/main/resources/data/zones/newbie_village/facilities.json`（舊路徑【歷史／已除役】；若提對話檔則標 `【已決議待實作】`）；第 82 行 `200ms` → `500ms`；第 151 行物品路徑改為 `src/main/resources/data/global/items/`（不加 `.json` 或指向真實 `weapons/weapons.json`，舊路徑【歷史／已除役】）；第 208、214 行狀態檔改為 `static/js/core/state-store.js` 並移除已不存在的動作列面板檔【歷史／已除役】。
   - §5 路線圖：依 D2 移除「低 SAN 幻覺與偽裝寶箱怪」；將已實作之 Gambit、元素反應、隨機詞綴移出未來路線圖或標 `【現行已驗證】`。
3. **[`M1_TOWN_HUB_AND_FACILITIES.md`](../2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md)**：第 5 行與第 18~22 行 Mermaid 圖改為對齊 §2 的 **6 大仙俠設施**（含宗門外門集市）；§3 NPC 對話樹資料檔標註 `【已決議待實作】`。
4. **[`M2_DRPG_DUNGEON.md`](../2_specifications/backend/M2_DRPG_DUNGEON.md)**：刪除第 28~30 行重複 `---`；第 61 行 `dangerRate` 由 `0.15` 改為整數 `15`；第 114 行重複 `## 五` 改為 `## 六`，更新套件為 `com.example.htmlmud.dungeon.*`（移除虛構 `DrpgActionDispatcher`）。
5. **[`M3_COMBAT_ENGINE.md`](../2_specifications/backend/M3_COMBAT_ENGINE.md)** & **[`M3_1_BUFF_AND_STATUS_EFFECTS.md`](../2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md)**：
   - **依 D3 統一破防規格**：載明**僅限帶護體罡氣之 Boss 與特定菁英怪，未破防僅受 `25% ~ 33%`（現行實作 `1/3`）傷害；破防力竭持續 `6 ~ 8 秒`（現行實作 `7 秒 / 14 Ticks`），期間恢復 `100%` 受擊並享 `1.5x` 暴擊且打斷蓄力；普通小怪（`NORMAL`）無護體減傷**（附註：`【待校準：現行 BattleEnemy.java 對一般小怪仍預設套用 1/3 減傷，由 Task M3-3 追蹤收斂】`）。
   - **依 D2 統一 SAN 描述**：刪除 `M3` §7.2「SAN < 30% 靈視畸變與滅團風險」，改為 `SAN = 0` 附加單一 `WEAKENED`（【精神萎靡】`-15%` 傷害）。
   - 統一兩檔戰鬥 Tick 為 `500ms`；補齊 `BuffCategory` 含 `PRIMER`；修正 `M3_1` 第 265 行路徑為 `src/main/resources/static/js/core/buff-hud-helper.js`（舊路徑【歷史／已除役】）；修正 `M3` 第 255 行重複 `## 八` → `## 九`；將未實作之 `M3_1` §4.2（隊員破防救援）與 §9.2（15s 控場遞減 DR、`BuffEffectHandler`、`valueFormula`）標註 `【已決議待實作】`。
6. **[`M4_PARTY_AND_FORMATION.md`](../2_specifications/backend/M4_PARTY_AND_FORMATION.md)**：依 D2 移除 §2.2 的 `SAN <= 25 【神識動搖】(-10%)`，統一僅保留 `SAN = 0 【精神萎靡 (WEAKENED)】(-15% 傷害)`；統一五維縮寫與 5+2 裝備槽名；更新套件 `com.example.htmlmud.party.*` 與 Gambit `【現行已驗證】`。
7. **[`M5_SKILL_AND_PROGRESSION.md`](../2_specifications/backend/M5_SKILL_AND_PROGRESSION.md)**：依 D5 明定主角強殖基因覺醒併入第 14 樹 `tree_chitin_blade` 與【萬象核心插槽】；修正第 102 行 `200ms` → `500ms`；更新套件 `com.example.htmlmud.progression.*`。
8. **[`M6_ITEM_AND_ECONOMY.md`](../2_specifications/backend/M6_ITEM_AND_ECONOMY.md)**：第 84 行重複 `## 三` 改為 `## 四`（後續順延）；更新套件與真實路徑 `src/main/resources/data/global/items/`；§5 標註隨機詞綴 `【現行已驗證】` 並刪除「依全服消耗量波動物價」。
9. **[`M7_DATA_DRIVEN_TEMPLATES.md`](../2_specifications/backend/M7_DATA_DRIVEN_TEMPLATES.md)**：依 D4-4 **刪除 §6.1 動態熱重載（`WatchService`）提案**；更新 §2 真實 `data/` 目錄結構與 `com.example.htmlmud.template.*`。
10. **[`M8_SAVE_AND_PERSISTENCE.md`](../2_specifications/backend/M8_SAVE_AND_PERSISTENCE.md)**：全檔自動存檔名統一修正為 `autosave.json`（及 `autosave.json.bak`），更新套件 `com.example.htmlmud.save.*`。
11. **[`M9_STATE_PIPELINE_AND_EVENTS.md`](../2_specifications/backend/M9_STATE_PIPELINE_AND_EVENTS.md)**：第 145 行 `"viewMode": "TOWN_HUB"` 改為 `"TOWN"`；第 242~247 行更新為真實 `com.example.htmlmud.pipeline.*` 類別。
- **本步完成後立即停下**，回報修改檔案清單待指導員驗收。

---

## 🎨 Step 5：校準 Level 2 前端模組手冊（`docs/2_specifications/frontend/` 共 8 檔）

1. **[`FRONTEND_LAYOUT_GUIDE.md`](../2_specifications/frontend/FRONTEND_LAYOUT_GUIDE.md)** & **[`UI_PAGE_STRUCTURE.md`](../2_specifications/frontend/UI_PAGE_STRUCTURE.md)**（D1）：
   - 刪除 `FRONTEND_LAYOUT_GUIDE.md` 第 9 行重複標題；第 116 行 `30 格儲物袋` 改為 `40 格公共行囊`；移除「輸入 `enter` / `leave` 文字指令」改為點擊按鈕。
   - 兩檔清楚區分：`【現行已驗證】` 為現行 `index.html` 左右雙主區（左舞台＋左下 5 人小隊卡、右全高可摺疊日誌，支援 `⮃ 換邊`）；`【待深入討論／已決議待實作】` 為 PC 上三欄＋底日誌與手機直向底部 5 分頁常駐 Tab Bar。
2. **[`FE1_CORE_STATE_AND_PIPELINE.md`](../2_specifications/frontend/FE1_CORE_STATE_AND_PIPELINE.md)**：第 30 行狀態檔路徑改為 `static/js/core/state-store.js`（舊路徑【歷史／已除役】）。
3. **[`FE2_DRPG_VIEWPORT_RENDERER.md`](../2_specifications/frontend/FE2_DRPG_VIEWPORT_RENDERER.md)**：修正 §1 左右面板顛倒描述（對齊 D1）；移除 §4/§5「打字輸入 `leave`」改為點擊撤離按鈕。
4. **[`FE3_BATTLE_ARENA_AND_FX.md`](../2_specifications/frontend/FE3_BATTLE_ARENA_AND_FX.md)**：第 110 行路徑改為 `static/js/core/buff-hud-helper.js`（舊路徑【歷史／已除役】）；修正章節編號跳號（`七→六`、`八→七`、重複 `七→八`）；已完工特效標 `【現行已驗證】`。
5. **[`FE4_TOWN_HUB_AND_ACTION_BAR.md`](../2_specifications/frontend/FE4_TOWN_HUB_AND_ACTION_BAR.md)**：修正 §4「左側敘事日誌」衝突描述（對齊 D1）；第 120 行刪除不存在的動作列面板檔【歷史／已除役】，改為 `static/js/panels/town-panel.js` 與 `static/index.html`。
6. **[`FE5_PARTY_EQUIPMENT_AND_MODALS.md`](../2_specifications/frontend/FE5_PARTY_EQUIPMENT_AND_MODALS.md)**：修正第 28、34 行 SAN 警示對齊 D2（`SAN = 0`【精神萎靡】`-15%` 傷害）；裝備屬性差值對比標 `【現行已驗證】`；將第 43 行寫死本機之 `file:///c:/Workspace/...` 連結改為相對路徑 `../../../src/main/java/com/example/htmlmud/item/EquipmentSlot.java`。
7. **[`FE6_CSS_TOKENS_AND_THEMES.md`](../2_specifications/frontend/FE6_CSS_TOKENS_AND_THEMES.md)**：移除第 55、91 行違規「四方出口網格按鈕 `.exit-btn`」；統一全檔 7 支真實 CSS 檔名（第 7 支為 `style-07-battle-fx.css`）。
- **本步完成後立即停下**，回報修改檔案清單待指導員驗收。

---

## 📋 Step 6：收斂 Level 3 任務清單與總覽看板（`docs/3_task_lists/` 共 22 檔，嚴禁動到主控檔）

1. **[`3_task_lists/README.md`](../3_task_lists/README.md)**：修正前端表格真實 JS/CSS 路徑（`core/state-store.js`、`fx/battle-fx-director.js`、`fx/floating-text.js`、`style-01` ~ `style-07` 真實名，移除已廢除之動作列面板檔【歷史／已除役】）；補充 `FRONTEND_LAYOUT_GUIDE.md`、`UI_PAGE_STRUCTURE.md` 與 `2_specifications/skills/`（19 檔）之承接清單映射。
2. **後端與 Schema 清單**：
   - [`M1_TOWN_HUB_TASKS.md`](../3_task_lists/backend/M1_TOWN_HUB_TASKS.md)：第 59 行 NPC 對話檔所在行加註 `【已決議待實作】`。
   - [`M2_DRPG_DUNGEON_TASKS.md`](../3_task_lists/backend/M2_DRPG_DUNGEON_TASKS.md)：依 D2 **刪除 `Task M2-2 (低 SAN 幻覺與偽裝寶箱怪)`**。
   - [`M3_COMBAT_ENGINE_TASKS.md`](../3_task_lists/backend/M3_COMBAT_ENGINE_TASKS.md)：將已完工之 `Task M3-1 (VS-01-CYBER-SKILLS)` 勾選 `- [x]` 移入已完成區；依 D4-4 **刪除 `Task M3-2 (COMBAT-ACTION-RELOAD 兵刃換彈)`**；依 D3 **新增 `- [ ] Task M3-3 (POISE-RANK-GATE)【已決議待實作】：護體罡氣減傷限定 Boss 與特定菁英怪`**（校準 `BattleEnemy.takeDamage` 與怪物 JSON，使一般 `NORMAL` 雜兵不承受未破防 `1/3` 傷害折減）。
   - [`M3_1_BUFF_AND_STATUS_TASKS.md`](../3_task_lists/backend/M3_1_BUFF_AND_STATUS_TASKS.md)：補列 `【已決議待實作】` 待辦（① 隊員破防硬直與救援秒回韌性；② 15 秒視窗控場遞減 DR 引擎；③ `BuffEffectHandler` 與 `valueFormula` 動態公式引擎）；註明 `Task M3_1-1` 與 `M4` 陣法聯動。
   - [`M5_SKILL_AND_PROGRESSION_TASKS.md`](../3_task_lists/backend/M5_SKILL_AND_PROGRESSION_TASKS.md) & [`SKILL_SCHEMA_TASKS.md`](../3_task_lists/data_schemas/SKILL_SCHEMA_TASKS.md)：依 D4-3 **刪除 `Task M5-1 (32+ 賽博天賦樹)`**；更新兩檔頁首職責為「四大淵源 × 17 專精技能樹與合體技體系」；將 `Task SKILL-SCHEMA-1` 勾選 `- [x]` 移入已完成區。
   - [`M6_ITEM_AND_ECONOMY_TASKS.md`](../3_task_lists/backend/M6_ITEM_AND_ECONOMY_TASKS.md)：於 `Task M6-2` 補入百煉堂「裝備數值強化 (`UPGRADE` / Refinement)」。
   - [`M7_DATA_DRIVEN_TEMPLATES_TASKS.md`](../3_task_lists/backend/M7_DATA_DRIVEN_TEMPLATES_TASKS.md)：依 D4-4 **刪除違憲待辦 `Task M7-1 (DATA-HOT-RELOAD)`**。
   - [`M8_SAVE_AND_PERSISTENCE_TASKS.md`](../3_task_lists/backend/M8_SAVE_AND_PERSISTENCE_TASKS.md)：將已完工之 `Task M8-2` 勾選 `- [x]` 移入已完成區；將舊自動存檔名統一改為 `autosave.json`。
3. **前端清單（`FE1` ~ `FE6`）**：依 Step 0 字典全面修正真實 CSS/JS 路徑（[`FE1`](../3_task_lists/frontend/FE1_CORE_STATE_TASKS.md) 改 `js/core/state-store.js`；[`FE2`](../3_task_lists/frontend/FE2_DRPG_VIEWPORT_TASKS.md) 改 `css/style-01-tokens-and-shell.css`；[`FE3`](../3_task_lists/frontend/FE3_BATTLE_ARENA_TASKS.md) 改 `js/fx/battle-fx-director.js` 與 `js/fx/floating-text.js`；[`FE4`](../3_task_lists/frontend/FE4_TOWN_HUB_TASKS.md) 移除舊動作列面板檔並新增低優先待辦 `【待深入討論】手機直向底部 5 分頁常駐導航列與 PC 三欄版面演進`；[`FE5`](../3_task_lists/frontend/FE5_PARTY_EQUIPMENT_TASKS.md) 改 `css/style-04-main-menu.css` 與 `css/style-06-shop-and-responsive.css`；[`FE6`](../3_task_lists/frontend/FE6_CSS_TOKENS_TASKS.md) 替換為真實 7 支 CSS 路徑）。
4. **[`FUTURE_IMPROVEMENTS.md`](../3_task_lists/FUTURE_IMPROVEMENTS.md)**：將懸空 `對應章節` 改為對應模組代碼；將重複已完工之 `CODE-01`、`DATA-03`、`PERSIST-01`、`FALLBACK-02` 更新為 `✅ 已完成`；新增 `UI-LAYOUT-DISCUSS`（P3 / `【待深入討論】`）追蹤 PC 版面與手機 5 分頁導航列。
5. **跨電腦相對路徑收斂**：將 `docs/3_task_lists/` 下各任務清單內舊有寫死本機磁碟之 `file:///c:/Workspace/my_practice/practice_mud/...` 連結全面轉為相對路徑（如 `../../../src/...` 或 `../../src/...`），確保跨電腦執行 `check-doc-links.ps1` 零斷鏈。
- **本步完成後立即停下**，回報修改檔案清單待指導員驗收。

---

## 🎯 Step 7：校準 Level 4 看板、根目錄 [`GEMINI.md`](../../GEMINI.md) 斷鏈、登載 [`CHANGELOG.md`](../../CHANGELOG.md) 與終極驗收

1. **[`SPRINT_GOAL.md`](./SPRINT_GOAL.md)**：第 20 行「五個問題」改為「四個問題」並修正第 34 行重複編號 `3.` → `4.`；第 32 行日誌正例加註 Option C 前提（`>70%` 覺醒揭示代碼特效，初期 `<30%` 純東方仙俠描寫）；第 44 行「微量強殖基因槽」改為「萬象核心插槽（含強殖幾丁質專精）」；第 45 行「16 項賽博修仙技能」改為「四大淵源 17 專精技能樹」。
2. **[`ACTIVE_TASKS.md`](./ACTIVE_TASKS.md)**：將不一致的測試數量（`388` / `376+` / `395`）收斂為「以 `.\test.ps1` 全量綠燈為準（基準見 `docs/3_task_lists/README.md`）」；於第 168 行已物理刪除之舊單一 CSS 檔所在行補註 `【歷史／已除役】`；並將第 62 行（以及 [`CYBERPUNK_CULTIVATION_SPEC.md`](../1_requirements/CYBERPUNK_CULTIVATION_SPEC.md) 第 55~75 行）殘留寫死本機之 `file:///c:/Workspace/...` 連結統一轉為相對路徑。
3. **[`GEMINI.md`](../../GEMINI.md)**（⚠️ **嚴禁觸碰第 21~37 行第 7 條紅線任何字元**）：
   - 僅修正第 5、50、58 行失效連結目標：`./docs/3_task_lists/CHANGELOG.md` 改為 `./CHANGELOG.md`；`./docs/3_task_lists/future_improvements_history.md` 改為 `./docs/0_archive/future_improvements_history.md`。
   - 修正第 160~162 行過時的 `2026-10-20 / 324 項測試` 與 `26 處` 描述，對齊 `docs/3_task_lists/README.md` 最新基準（`395 項測試 100% 綠燈`、`new TemplateCatalog()` 私建目錄已清零）。
4. **[`CHANGELOG.md`](../../CHANGELOG.md)**：於最上方新增 `## [2026-10-29] 四層級工程文件全面對齊與自動化防漂移看門狗升級` 繁體中文條目。
5. **終極自檢閘門（4 道指令全綠方可回報完成）**：
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\check-doc-links.ps1
   git status -s -- src/
   git diff -- GEMINI.md
   ```
   並確認 `docs/` 非封存文件無任何單檔超過 `400` 行，回報最終結果等待指導員總簽核。
