# 🚨 專案第一紅線：100% 資料驅動與零硬編碼鐵律 (Zero-Hardcode & Data-Driven Rule)

> **Antigravity 系統級規則 (Workspace Rule)**:
> 本規則為專案最高優先級架構約束。每次 AI 接收到使用者指令、規劃架構或編寫代碼時，必須無條件遵守。

---

## ⚠️ 最高元契約：不可刪除／修改保護鎖 (Immutability & Consent Lock)
**本規則為本專案不可動搖之架構憲法。AI 在未來的任何對話、任務執行、檔案更新或代碼重構中，絕對嚴禁以任何理由（包括精簡文件、暫時性測試、上下文壓縮整理等）自行刪除、弱化、縮編或覆蓋本規則內容。**
**若未來有任何對本條款進行調整、弱化或刪除之狀況，AI 必須立即中斷動作，主動額外發出高優先級提醒，詳細說明原因並徵詢使用者同意，獲得使用者明確書面許可後方可進行！**

---

## 🚫 零硬編碼禁忌紅線 (Zero-Hardcode Red Lines)

1. **嚴禁業務特例比對**：
   - 嚴禁依賴中文名稱、職業或實體特例比對（例如嚴禁 `switch (classId) { case "WARRIOR" -> ... }`、嚴禁 `if (name.contains("解毒"))`、嚴禁 `if (k.contains("鐵牛"))`）。
   - 所有行為判定一律改用資料標籤（如 `tags: ["CLEANSE", "TAUNT"]`）或枚舉型別。

2. **嚴禁缺失配置寫死 Fallback**：
   - 凡新增戰鬥機制（如 Poise 罡氣破防、Resistances 五行抗性、Shield 護盾、Cleanse 解控），**嚴禁在 Java 類別中寫死預設數值分支作為特例**（例如嚴禁 `if (boss) maxP = 400; else maxP = 30`）。
   - 必須同時在 Model 擴充欄位，並在 JSON 資料庫（`classes.json`, `mobs.json`, `default_companions.json`, `party_skills.json`）落盤真實配置。

3. **嚴禁容器寫死初始數據**：
   - 嚴禁在容器建構子（如 `PartyInventory`）寫死塞入特定測試道具。

4. **嚴禁技能縮放丟失資料屬性**：
   - 任何技能縮放（`scaleSkill`）或實體深拷貝，必須完整保留資料模型的所有欄位（如 `poiseDamage`、`buffConfig`、`shield`、`tags`、`damageType`），絕不允許遺漏。

---

## 📋 提交前強制自我審查清單 (Self-Check Gate before Commit)
每次向使用者匯報完成或提供 git commit 前，AI 必須自主進行靜態檢查：
- [ ] 是否在 Java/JS 代碼中出現特例名稱/字串比對？
- [ ] 新增機制的數值是否已 100% 寫入對應的 JSON 設定檔？
- [ ] 既有與新增的 Model 欄位在縮放、轉換時是否完整傳遞無遺漏？
- [ ] 是否已執行 `ArchitectureGuardrailsTest` 與全套單元測試確認 100% 綠燈？
