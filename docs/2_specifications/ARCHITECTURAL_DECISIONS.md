# 🏛️ 專案既定架構裁決與外部 AI 審閱豁免基準 (Architectural Decisions & Review Guardrails)

> **文檔層級**：Level 2 — Module Specifications (跨模組架構裁決與治理基準)  
> **制定目的**：記錄專案擁有者已拍板定案之核心設計裁決、治理基準與外部 AI（如 Codex / Copilot / Claude 等）審查豁免清單。供後續各 AI 代理閱讀以理解既定方向，**避免針對已決議事項重複提出無效衝突或倒退建議**。  
> **問題溯源與外部審查報告**：[`docs/0_references/`](../0_references/)

---

## 📋 外部 AI 文件審閱前置基準（必讀豁免清單）

在審閱本專案 [`docs/1_requirements/`](../1_requirements/)、[`docs/2_specifications/`](./)、[`docs/3_task_lists/`](../3_task_lists/) 與 [`docs/4_active_tasks/`](../4_active_tasks/) 之前，請先閱讀以下 **7 項專案擁有者已拍板定案之設計與治理基準**。請勿將以下項目列為「衝突」、「缺失」或「待改進建議」：

### 1. 【SAN 精神護盾 vs 取消魔化機制】並非衝突（已定案：Option A）
- **常見誤判**：誤以為 [`GAMEPLAY_CORE_LOOPS.md`](../1_requirements/GAMEPLAY_CORE_LOOPS.md) 宣告「除役道心崩潰魔化（Madness / Aberration）」，卻在 [`3_task_lists/README.md`](../3_task_lists/README.md) (R9)、[`MODULES_MAP.md`](./MODULES_MAP.md)、[`M3_1_BUFF_AND_STATUS_EFFECTS.md`](./backend/M3_1_BUFF_AND_STATUS_EFFECTS.md)、[`M4_PARTY_AND_FORMATION.md`](./backend/M4_PARTY_AND_FORMATION.md) 保留「SAN 精神護盾與【精神萎靡】」是前後矛盾。
- **權威事實**：專案徹底除役的是**「低 SAN 時隊員隨機發瘋自殘、叛變攻擊隊友、或產生偽裝寶箱怪幻覺」**等中斷戰鬥心流的 RNG 失控機制；但**明確保留數值化的「精神防護罩（Psychic SAN Shield）」**作為地牢摸黑探索的資源警示。當 `SAN = 0` 時，僅賦予單一輕度數值 Debuff **【精神萎靡（WEAKENED，全傷害 -15%）】**，回營調息或服丹即解除（見 [`docs/1_requirements/GAMEPLAY_CORE_LOOPS.md`](../1_requirements/GAMEPLAY_CORE_LOOPS.md) §5.1）。兩者完全相容，請勿再報為 P0 衝突。

### 2. 【L1 ➔ L2 ➔ L3 遞進審批鐵律】為刻意設計的防幻覺護欄，請勿建議放寬
- **常見誤判**：認為 [`docs/README.md`](../README.md) 與 [`docs/3_task_lists/README.md`](../3_task_lists/README.md) 規定「L1 異動需詢問使用者是否改 L2、L2 異動需詢問是否改 L3」摩擦過高，建議改由 AI 自行依風險分級修改下游。
- **權威事實**：這是專案擁有者在 [`.agents/rules/documentation_alignment_guardrails.md`](../../.agents/rules/documentation_alignment_guardrails.md) 親自設立的核心防線，目的是**防止 AI 在單次任務中擅自擴張需求或連鎖竄改多層規格**。此外，先前出現的專項任務卡僅為一次性全量對齊專項任務（完工後封存至 [`docs/0_archive/alignment_history/`](../0_archive/alignment_history/)），並非日常開發的永久流程負擔。

### 3. 【拒絕過度文件治理】不加 73 檔頂部日期/版本 Header、不編配全域 `REQ-XXX` 流水號
- **常見誤判**：建議在全部 70+ 份 Markdown 頂部維護「最後核對日期／版本號／被引用 ID」，或要求將所有需求改為 `REQ-COMBAT-001` 格式並由 CI 檢查。
- **權威事實**：本專案為單人主導之純單機 DRPG，在 70+ 份文件頂部維護「最後核對日期」與「版本號」會造成每次微調都產生大量無意義的同步噪音。現行採用：
  1. [`docs/3_task_lists/README.md`](../3_task_lists/README.md) 的 **R1 ~ R11 端到端追溯矩陣**；
  2. 內嵌的四級狀態標籤（`【現行已驗證】`、`【已決議待實作】`、`【待深入討論】`、`【歷史／已除役】`）；
  3. [`scripts/check-doc-links.ps1`](../../scripts/check-doc-links.ps1)（自動檢查 Markdown 連結、反引號實體路徑存在性、`data_schemas/*.md` 內嵌 JSON 語法）；
  4. 本檔（[`ARCHITECTURAL_DECISIONS.md`](./ARCHITECTURAL_DECISIONS.md)）作為跨模組決策之聚合指針。  
  此組合已兼顧可追溯性與低維護成本，請勿再建議額外的文件元資料（Metadata Header）或流水號資料庫。

### 4. 【數值單一真相源（SSoT）】為 `src/main/resources/data/**/*.json`，不另建獨立平衡試算表
- **常見誤判**：建議把 `TREE_01 ~ TREE_17.md` 或戰鬥規格中的數值抽離，另外建立一套獨立的平衡資料表／試算表，或要求先寫出全章節探索時間、休整點間距與掉落預算模型。
- **權威事實**：專案嚴格遵循「100% 資料驅動（Data-Driven）」，執行期所有數值的唯一真相源（SSoT）就是 `src/main/resources/data/**/*.json`（並由啟動期的 `DataIntegrityValidator` 進行跨檔引用與語意校驗），而 [`docs/2_specifications/skills/`](./skills/README.md) 是供人類與 AI 理解設計意圖的規格手冊。若再新建獨立的平衡試算表，反而會造成「JSON、Markdown、試算表」三處同步的維護災難。

### 5. 【版面演進與無障礙（Accessibility）範疇】現階段聚焦 VS-01 核心玩法，不引入無障礙、音效字幕與戰鬥調速規格
- **常見誤判**：建議在前端規格（`FE1 ~ FE6`）與驗收中加入完整的無障礙／可及性規範（螢幕閱讀器焦點順序、文字縮放、色盲輔助、閃光減弱、音效與字幕控制、戰鬥暫停／減速模式、完整 UI 狀態矩陣）。
- **權威事實**：
  1. 本專案目前並無音效與語音字幕系統；
  2. 連 PC 三欄式與手機直向 5 分頁導航列都已由專案擁有者拍板列為「低優先延後專題討論（`UI-LAYOUT-DISCUSS`, P3）」，現行以 `index.html` 左右雙主區（支援換邊）為已驗證基準；
  3. 在現階段往 `FE1 ~ FE6` 塞入無障礙與音效字幕規範屬於範疇蔓延（Scope Creep），會製造大量「無程式碼對應的空頭規格」。請勿再建議將其寫入現行模組規格。

### 6. 【地名與宗門關係】「黑煞宗地底礦脈」與「墨竹山廢棄靈石礦坑」並非名稱不一致
- **常見誤判**：認為世界觀寫「黑煞宗地底礦脈一層」，而 [`DUNGEON_SCHEMA.md`](./data_schemas/DUNGEON_SCHEMA.md) / `data/dungeons/mozhu_mines_b1f.json` 寫「墨竹山廢棄靈石礦坑一層 (B1F)」是地名未統一。
- **權威事實**：依 [`WORLD_AND_LORE_BIBLE.md`](../1_requirements/WORLD_AND_LORE_BIBLE.md) 設定，「墨竹山廢棄靈石礦坑（`mozhu_mines_b1f`）」即為「黑煞宗」轄下的外門採礦據點——前者為**宗門勢力歸屬**，後者為**具體地理座標名稱**，兩者為同一地點的宗門稱謂與地理稱謂，並非筆誤。

### 7. 【外部參考目錄路徑】審查報告統一存放於 `docs/0_references/`
- **常見誤判**：誤以為審查報告位於 `docs/references/` 而與 [`docs/README.md`](../README.md) 的 `docs/0_references/` 描述不符。
- **權威事實**：本專案所有外部 AI 審查與歷史評估報告均固定存放於 [`docs/0_references/`](../0_references/)，且 [`scripts/check-doc-links.ps1`](../../scripts/check-doc-links.ps1) 已設定排除 `0_archive/` 與 `0_references/`，路徑與治理規則 100% 一致。

---

## 🔍 附錄：原外部 AI 報告各點採納 vs 駁回對照速查表（供留存備查）

| 來源 AI | 原報告提出之問題 | 我們的處置結果 | 處置依據說明 |
| :---: | :--- | :---: | :--- |
| **Copilot P0** | `slot` vs `equipSlot`、`EQUIP_ITEM` vs `EQUIP`、`FE2` 殘留 `step w` / `turn a` | ✅ **已採納並修正完成** | 確實存在跨層欄位與動作名稱落差，已在 [`ITEM_SCHEMA.md`](./data_schemas/ITEM_SCHEMA.md)、[`FE5`](./frontend/FE5_PARTY_EQUIPMENT_AND_MODALS.md)、[`FE2`](./frontend/FE2_DRPG_VIEWPORT_RENDERER.md) 完成統一。 |
| **Copilot P1** | 認知透鏡二階門檻寫 `35`（與 L1 的 `30%` 不一致）、`M7` 寫 `Draft 2020-12`（與 Schema 的 `draft-07` 不一致） | ✅ **已採納並修正完成** | 對照 `PerceptionTier.java` 確認真實門檻為 `0 / 30 / 70`，已修正 [`ITEM_SCHEMA.md`](./data_schemas/ITEM_SCHEMA.md)、[`M6`](./backend/M6_ITEM_AND_ECONOMY.md) 與 [`M7`](./backend/M7_DATA_DRIVEN_TEMPLATES.md)。 |
| **Copilot P1/P2 & Codex P1** | `Task M2-1` 與 `Task DUNGEON-SCHEMA-1` 重複列管、L2 末章 Future Roadmap 缺標籤、17 專精樹需標明分階交付、`3_task_lists/README.md` 1:1 表述與測試基準日期 | ✅ **已採納並修正完成** | 已在 `M1~M9`/`FE1~FE6` 末章加註 `【待深入討論／長線演進藍圖】`、標明 VS-01 範圍並去重。 |
| **Codex P0** | 稱 `GAMEPLAY_CORE_LOOPS.md` 除役 SAN 與 `R9`/`MODULES_MAP` 保留 SAN 護盾互相衝突 | ❌ **駁回（事實誤判）** | 除役的是「魔化發狂叛變」，保留的是「SAN=0 賦予 -15% 傷害之【精神萎靡】」（裁決 D2）。 |
| **Codex P0/P1** | 建議新建「設計裁決紀錄專頁」、73 檔頂部加日期/版本狀態欄、全面導入 `REQ-XXX` 穩定 ID | ❌ **駁回（過度工程）** | 增加大量無謂維護成本與 Git diff 噪音；現有 R1~R11 矩陣＋四級標籤已足夠。 |
| **Codex P1** | 建議放寬 L1→L2→L3 使用者審批規則，改由 AI 按風險分級自行決定 | ❌ **駁回（違反護欄）** | 直接違反 `.agents/rules/documentation_alignment_guardrails.md` 防 AI 連鎖竄改的核心鐵律。 |
| **Codex §6** | 建議在 FE 規格加入無障礙（焦點順序、色盲、閃光減弱）、音效字幕控制與戰鬥調速器 | ❌ **駁回（範疇蔓延）** | 遊戲無音效字幕，且 UI 版面已列 P3 延後討論，寫入現行規格會變成空頭支票。 |
| **Codex §2/§3** | 建議將平衡數值從規格移出另建獨立試算表，並補全章節探索時間與掉落預算模型 | ❌ **駁回（重複造輪子）** | 執行期數值 SSoT 為 `data/**/*.json`，另建平衡表會造成三處同步負擔。 |
| **Codex 末段** | 稱審查報告位於 `docs/references/` 與 `docs/README.md` 的 `0_references/` 不符 | ❌ **駁回（環境誤判）** | 實際路徑即為 `docs/0_references/`，並無不一致。 |
| **Copilot P1** | 質疑「黑煞宗地底礦脈」與「墨竹山廢棄靈石礦坑」地名不一致 | ❌ **駁回（設定誤解）** | 前者為所屬宗門勢力、後者為地理礦坑名稱，並非矛盾。 |

