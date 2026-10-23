# 🚨 四層文件對齊與單一真相源防漂移守護鐵律 (Documentation Alignment & Anti-Drift Guardrails)

> **Antigravity 系統級工作區規則 (Workspace Rule)**:
> 本規則為專案最高優先級文檔治理與契約一致性約束。每次 AI 接收到使用者指令、審查文件、撰寫任務卡或同步規格時，必須無條件遵守。

---

## ⚠️ 最高元契約：不可刪除／修改保護鎖 (Immutability & Consent Lock)
**本規則為本專案不可動搖之架構憲法。AI 在未來的任何對話、任務執行、檔案更新或代碼重構中，絕對嚴禁以任何理由（包括精簡文件、暫時性測試、上下文壓縮整理等）自行刪除、弱化、縮編或覆蓋本規則內容。**
**若未來有任何對本條款進行調整、弱化或刪除之狀況，AI 必須立即中斷動作，主動額外發出高優先級提醒，詳細說明原因並徵詢使用者同意，獲得使用者明確書面許可後方可進行！**

---

## 🏛️ 1. 四層職責邊界與 DRY 單一真相源 (Single Source of Truth, SSoT)

嚴禁在不同層級重複維護同一項具體數值或欄位定義，違者視為架構破窗：

| 層級 | 唯一責任 (SSoT) | 嚴禁行為 (Forbidden) | 正確表述範例 |
| :--- | :--- | :--- | :--- |
| **L1: 需求與願景層**<br>(`docs/1_requirements/`) | **玩家體驗、世界觀背景與設計哲學** | 🚫 嚴禁出現具體程式 ID、JSON 欄位名與戰鬥平衡數值。 | 「特工晶片超頻突刺破防，造成強烈打擊反饋」 |
| **L2: 模組規格層**<br>(`docs/2_specifications/`) | **唯一架構契約、API、DTO、狀態機與 JSON Schema** | 🚫 嚴禁範例使用未在代碼或資料中支援的虛構欄位或過期枚舉。 | 定義 `SKILL_SCHEMA.md` 中的 `comboRole: NONE \| PRIMER \| DETONATOR` |
| **Data: 實體資料庫**<br>(`src/main/resources/data/`) | **全專案唯一之數值與內容庫 (100% 資料驅動)** | 🚫 嚴禁在 Java 程式碼分支中寫死數值特例。在迭代中新增實體、調整數值平衡或擴充格式時，**一律直接在 JSON 中落盤**。一旦落盤，JSON 即為該數值的生效真相源。 | `party_skills.json` 儲存 `poiseDamage: 25` |
| **L3: 演進清單層**<br>(`docs/3_task_lists/`) | **宏觀路線圖、架構治理清單與待辦儲備** | 🚫 嚴禁催促未啟動的里程碑，必須清楚標註前置依賴。 | 標註「Phase 2 依賴 Milestone VS-01 正式啟動」 |
| **L4: 微觀看板層**<br>(`docs/4_active_tasks/`) | **當前衝刺唯一微任務看板 (每次 1 Micro-Task)** | 🚫 **嚴禁多處雙寫導致漂移！**<br>• 新增/調平任務：明確宣告目標 JSON 與配置參數，交付標準為落盤至 JSON。<br>• 驗收/既有任務：僅做 Canonical ID 指針引用，嚴禁憑記憶自創衝突數值。 | ✅ `依據 SKILL_SCHEMA.md 驗證 party_skills.json#skill_neural_overclock_step 之破防判定` |

---

## ⚖️ 2. 三大層級遞進治理與使用者審批鐵律 (Mandatory Cascading Governance & User Approval Rules)

> [!IMPORTANT]
> **全專案必須無條件嚴格遵守的三大層級遞進與審批原則：**
> 1. **`1_requirements` (L1) 為最主要規範**：當 L1 內容發生任何異動時，必須檢視現有 L2 文件是否涵蓋 L1 異動後的內容。**若有需要進行新增或修改，AI 必須提供建議並由使用者決定是否新增/修改 L2 文件。**
> 2. **`2_specifications` (L2) 為 L1 內容的完整模組規劃**：當 L2 內容發生任何異動時，必須檢視現有 L3 文件是否涵蓋 L2 異動後的內容。**若有需要進行新增或修改，AI 必須提供建議並由使用者決定是否新增/修改 L3 文件。**
> 3. **`3_task_lists` (L3) 為 L2 規格的 1:1 完整實作項目清單**：記錄所有模組之落實現況、待辦與驗收標準。

---

## 🔄 2. 實體擴充、數值調平與 JSON 動態演進工作流 (Dynamic Content & Balancing Workflow)

專案遵循標準的 **100% 資料驅動架構**。在開發、討論與執行過程中進行內容演進時，標準作業流如下：

1. **新增實體 (技能/怪物/裝備/設施)**：
   - 在討論或 L4 任務卡中規劃新實體之機制定位與目標參數。
   - 直接在對應的 `src/main/resources/data/**/*.json` 中建立資料物件（包含唯一 Canonical ID 與初始平衡數值）。
   - 若新增機制需要補充新的屬性欄位，同步於 `docs/2_specifications/data_schemas/` 擴充 Schema 契約。
2. **數值平衡調整 (Buff / Nerf / Rebalance)**：
   - 調整攻擊力、冷卻時間、削韌值、掉落機率或怪物氣血時，**直接修改 JSON 檔案中的對應欄位**。
   - **嚴禁在 Java 代碼中新增 `if (id.equals(...))` 之特例分支覆蓋 JSON 數值**。
   - 一旦在 JSON 中完成調整並通過測試，JSON 即為該數值的唯一生效真相源，其他文字文件不再冗餘謄寫一份靜態數值，杜絕日後再次漂移。
3. **格式與標籤擴充 (Schema Expansion)**：
   - 當遊戲機制演進需要新的標籤（如新增 `tags: ["DISPEL", "TAUNT"]`）或結構欄位時，Schema 與 JSON 同步擴充，引擎僅提供通用解析機制。

## 🔍 3. 查證優先鐵律 (Evidence-First Rule)

1. **嚴禁憑記憶發明任何識別碼**：
   - 撰寫或更新任何任務卡、規格或測試前，AI 必須先使用 `grep_search` 或 `view_file` 查證現行程式碼庫（Java / JSON）中真實存在的檔案路徑、類別名稱、Canonical ID 與欄位名稱。
2. **任務卡必附來源檔案指針**：
   - L4 任務卡必須明確標註「目標檔案」，並引用真實 ID（例如使用 `skill_neural_overclock_step` 而非簡寫 `neuro_overclock_step`）。

---

## 🛡️ 4. 代碼化自動驗證防線 (Verification as Code)

1. **拒絕「肉眼宣稱全綠」**：
   - 文件對齊不可僅依賴 LLM 的文字描述宣稱完成，必須具備可執行的驗證手段。
2. **本機連結驗證看門狗**：
   - 每次重構或搬移文件目錄後，必須執行 `scripts/check-doc-links.ps1` 確保無任何本機 Markdown 相對死鏈。
3. **Schema 範例相容性**：
   - L2 Schema 所列之 JSON 範例，必須能被後端載入器（如 `DungeonFloorLoader`、`PartyMemberSkill`）成功反序列化，不得出現 Schema 規格與實體載入器脫節之「理想化草案」。

---

## 🚫 5. 嚴禁假完成與雙頭待辦 (No Fake Done & No Competing Backlogs)

1. **杜絕早產勾選**：
   - 任何 Micro-Task 標記為 `[x]` 前，必須具備代碼落地、測試全綠與手冊同步證據；禁止在程式碼尚未吻合前提前打勾。
2. **單一活躍看板原則**：
   - [`docs/4_active_tasks/ACTIVE_TASKS.md`](docs/4_active_tasks/ACTIVE_TASKS.md) 為專案當前衝刺的**唯一焦點**。
   - 歷史已完成之里程碑必須定期清理出「後續備選」區，嚴禁在多個文件同時標註互相衝突的「當前待辦」。

---

## 📐 6. 理性內聚性規範 (Cohesion over Micro-Fragmentation)

1. **禁止盲目切碎文件**：
   - 一般規格與治理文件原則上維持在 150~350 行以內。
   - 針對高度內聚之核心業務手冊（如 [`M3_1_BUFF_AND_STATUS_EFFECTS.md`](docs/2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md) 390 行、[`MODULES_MAP.md`](docs/2_specifications/MODULES_MAP.md) 291 行），賦予架構豁免（上限放寬至 400 行），嚴禁為了追求行數形式主義而切碎高度關聯之規格。

---

## 📋 提交前文件自檢清單 (Doc Self-Check Gate)
每次向使用者匯報文件對齊或修改規格前，AI 必須自主進行以下檢查：
- [ ] 任務卡中是否出現了未在代碼/資料中存在的虛構 ID 或重複寫死數值？
- [ ] L2 Schema 欄位名稱與代碼載入器/JSON 鍵名是否 1:1 吻合（包含大小寫與別名）？
- [ ] 是否檢查並確認了修改涉及的所有 Markdown 相對路徑點擊有效？
- [ ] 是否在 `ACTIVE_TASKS.md` 之外的舊待辦中殘留了重複的「當前焦點」？
- [ ] 是否嚴格遵循了「單一真相源 (SSoT)」原則，無多處數值雙寫？
