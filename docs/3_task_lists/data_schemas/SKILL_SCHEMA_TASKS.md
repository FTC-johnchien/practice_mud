# ⚡ SKILL_SCHEMA: 賽博修仙技能資料架構實作進度清單 (Skill Schema Tasks)

> **文檔層級**：Level 3 — Task Lists (資料契約進度清單)  
> **對應規格**：👉 [`docs/2_specifications/data_schemas/SKILL_SCHEMA.md`](../../2_specifications/data_schemas/SKILL_SCHEMA.md)  
> **核心職責**：驗證四大賽博修仙流派、超頻階級 (0~3)、連攜角色 (`comboRole`) 與破防削韌值 (`poiseDamage`) 結構契約。  
> **當前完成度**：95% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心資料檔案**：
  - `src/main/resources/data/party/party_skills.json`
- **核心實體與載入模型**：
  - [`PartyMemberSkill.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/party/model/PartyMemberSkill.java)
  - [`SkillDefinition.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/model/definition/SkillDefinition.java)
- **關聯驗證測試**：
  - [`CyberSkillAndItemAffixTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/CyberSkillAndItemAffixTest.java)（100% 綠燈）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **SCHEMA-SKILL-COMBO-ROLE (連攜角色互斥枚舉)**
  - Schema 正式定義 `comboRole: NONE | PRIMER | DETONATOR`，消除 Schema 雙布林與代碼枚舉脫節。
- [x] **SCHEMA-SKILL-CANONICAL-IDS (序章核心技能規範)**
  - 範例與規範精準鎖定 `skill_neural_overclock_step`（削韌 25）與 `skill_bio_keratin_blade`（削韌 45）。
- [x] **SCHEMA-OVERCLOCK-TIERS (超頻階級 0~3 驗證)**
  - 整合 `DataIntegrityValidator` Fail-Fast 檢驗，階級超界立即阻斷。

- [x] **Task SKILL-SCHEMA-2 (16-CANONICAL-SKILLS-VALIDATION): 16 大標準賽博技能 Schema 契約齊備性驗證（仙俠神通名契約）**
  - 依據 `SKILL_SCHEMA.md` 第四節 16 大技能矩陣，`party_skills.json` 完整包含四大流派之 16 項技能定義，`name` 欄位為古典仙俠神通名（如【移花接木·協議解析】、【萬劍歸宗·分散式洪流】），且 `cyberSchool`、`comboRole`、`poiseDamage`、`costMp`、`cooldownMs` 契約無遺漏。
  - `CyberSkillAndItemAffixTest.testCanonical16CyberSkillsLoadedFromData` 測試 16 個技能 Schema 結構與連攜角色全數 100% 綠燈。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task SKILL-SCHEMA-1 (VS-01 戰鬥連攜實測): 戰鬥中 Primer 標記與 Detonator 引爆閉環驗收**
  - **目標**：驗證技能發動後 DTO 投影正確攜帶 `comboRole`，戰鬥中正確發動 50% 震盪衝擊波。
