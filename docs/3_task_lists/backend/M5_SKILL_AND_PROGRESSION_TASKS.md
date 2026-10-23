# ⚡ M5: 技能體系與修為突破模組實作進度清單 (Skill & Progression Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/backend/M5_SKILL_AND_PROGRESSION.md`](../../2_specifications/backend/M5_SKILL_AND_PROGRESSION.md)  
> **核心職責**：管理四大賽博修仙流派技能、超頻階級 (0~3)、連攜角色 (ComboRole)、境界階梯突破與潛能點成長。  
> **當前完成度**：90% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心資料配置**：
  - `src/main/resources/data/party/party_skills.json`（小隊可用賽博戰技庫）
  - `src/main/resources/data/global/classes.json`（境界階梯、基礎屬性與成長率）
- **核心實體與服務**：
  - [`SkillDefinition.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/model/definition/SkillDefinition.java)（大一統技能核心定義）
  - [`PartyMemberSkill.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/party/model/PartyMemberSkill.java)（賽博流派、超頻階級、ComboRole）
  - [`XpProgressionService.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/progression/service/XpProgressionService.java)（修為經驗與等級計算）
- **關聯驗證測試**：
  - [`CyberSkillAndItemAffixTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/CyberSkillAndItemAffixTest.java)（賽博技能流派與超頻 100% 綠燈）
  - [`UnifiedSkillModelTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/UnifiedSkillModelTest.java)（大一統技能模型 100% 綠燈）
  - [`XpProgressionServiceTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/XpProgressionServiceTest.java)（9/9 綠燈）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **M5-UNIFIED-SKILL (技能核心大一統)**
  - 徹底消除 MUD 與 DRPG 雙軌技能定義，物理刪除 `SkillBridgeService`，全系統統一運行於 `UnifiedSkill`。
- [x] **M5-CYBER-SCHOOLS (四大賽博修仙流派枚舉)**
  - 實作 `CyberSchool`：黑客劫持 (`CYBER_HACK`)、強殖生化 (`GENE_BIO`)、機械修真 (`MECHA_CULT`)、虛數靈能 (`VOID_PSI`)。
- [x] **M5-OVERCLOCK-TIERS (超頻階級與合法性校驗)**
  - 支援超頻階級 `overclockTier 0~3`；`DataIntegrityValidator` Fail-Fast 啟動期檢驗防破窗。
- [x] **M5-PROLOGUE-SKILLS (首批序章戰技注入)**
  - 在 `party_skills.json` 實裝【神經超頻步】(`skill_neural_overclock_step`, Primer, 削韌 25) 與【強殖角質刃】(`skill_bio_keratin_blade`, Detonator, 削韌 45)。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task M5-1 (CYBER-TALENT-TREE): 32+ 賽博天賦樹分支加點**
  - **目標**：各流派提供 8 個天賦節點（被動數值增強、冷卻縮減、元素轉化），由特工晶片算力解鎖。
  - **驗收標準**：加點存讀檔正常，戰鬥結算享受天賦增益。
- [ ] **Task M5-2 (BREAKTHROUGH-TRIALS): 境界突破雷劫試煉副本**
  - **目標**：築基破金丹時，需消耗突破丹藥並觸發九重天劫考驗，成功後五維上限大幅躍遷。
  - **驗收標準**：突破失敗有保底保護，突破成功廣播天地異象。
- [ ] **Task M5-3 (16-CANONICAL-SKILLS-INGESTION): 落地 16 大標準賽博修仙技能庫（仙俠表層定名）**
  - **目標**：依據 L1《CYBERPUNK_CULTIVATION_SPEC.md》與 L2《SKILL_SCHEMA.md》第四節 16 大技能矩陣，在 `party_skills.json` 完整補齊 16 項技能資料配置。所有技能對外展示名稱一律採用東方仙俠神通名稱（如【偷天換日·轉煞移因訣】、【燃魂溯光·剎那千秋步】），後端對齊四大 `cyberSchool` 與 `comboRole` 連攜機制。
  - **驗收標準**：`WorldDataIntegrityTest` 綠燈，技能在前端呈現純正仙俠神通名，戰鬥中 Primer/Detonator 連攜機制運作正常。
