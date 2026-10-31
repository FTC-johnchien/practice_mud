# ⚡ M5: 技能體系與修為突破模組實作進度清單 (Skill & Progression Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/backend/M5_SKILL_AND_PROGRESSION.md`](../../2_specifications/backend/M5_SKILL_AND_PROGRESSION.md)  
> **核心職責**：管理四大淵源 × 17 專精技能樹與合體技體系、超頻階級 (0~3)、連攜角色 (ComboRole)、境界階梯突破與潛能點成長。  
> **分階實裝說明**：17 專精樹為四大淵源完整架構藍圖；當前 VS-01 垂直切片優先實裝並驗收代表性核心專精樹與基礎合擊，其餘隨後續章節分批擴充。  
> **當前完成度**：85% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心資料配置**：
  - `src/main/resources/data/party/party_skills.json`（小隊可用賽博戰技庫）
  - `src/main/resources/data/global/classes.json`（境界階梯、基礎屬性與成長率）
- **核心實體與服務**：
  - [`SkillDefinition.java`](../../../src/main/java/com/example/htmlmud/progression/SkillDefinition.java)（大一統技能核心定義）
  - [`PartyMemberSkill.java`](../../../src/main/java/com/example/htmlmud/party/PartyMemberSkill.java)（賽博流派、超頻階級、ComboRole）
  - [`XpProgressionService.java`](../../../src/main/java/com/example/htmlmud/progression/XpProgressionService.java)（修為經驗與等級計算）
- **關聯驗證測試**：
  - [`CyberSkillAndItemAffixTest.java`](../../../src/test/java/com/example/htmlmud/CyberSkillAndItemAffixTest.java)（賽博技能流派與超頻 100% 綠燈）
  - [`UnifiedSkillModelTest.java`](../../../src/test/java/com/example/htmlmud/UnifiedSkillModelTest.java)（大一統技能模型 100% 綠燈）
  - [`XpProgressionServiceTest.java`](../../../src/test/java/com/example/htmlmud/XpProgressionServiceTest.java)（9/9 綠燈）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **M5-PKG-ENCAPSULATION (Package-by-Feature 獨立封裝 / Phase 3 Step R3.6)**
  - 建立一級套件 `com.example.htmlmud.progression`，將 `SkillDefinition`、`XpProgressionService`、`XpService`、`DrpgSkillRules`、`MudSkillRules`、`SkillBridgeRule`、`SkillType`、`SkillCategory`、`SkillEntry` 全面自雜湊目錄移出歸整，全專案 395 項測試 100% 綠燈通過。
- [x] **M5-UNIFIED-SKILL (技能核心大一統)**
  - 徹底消除 MUD 與 DRPG 雙軌技能定義，物理刪除 `SkillBridgeService`，全系統統一運行於 `UnifiedSkill`。
- [x] **M5-CYBER-SCHOOLS (四大賽博修仙流派枚舉)**
  - 實作 `CyberSchool`：黑客劫持 (`CYBER_HACK`)、強殖生化 (`GENE_BIO`)、機械修真 (`MECHA_CULT`)、虛數靈能 (`VOID_PSI`)。
- [x] **M5-OVERCLOCK-TIERS (超頻階級與合法性校驗)**
  - 支援超頻階級 `overclockTier 0~3`；`DataIntegrityValidator` Fail-Fast 啟動期檢驗防破窗。
- [x] **M5-PROLOGUE-SKILLS (首批序章戰技注入)**
  - 在 `party_skills.json` 實裝【燃魂溯光·剎那千秋步】(`skill_time_tier1_step` / `skill_neural_overclock_step`, Primer, 削韌 25) 與【血肉強殖·玄冥幾丁刃】(`skill_chitin_tier1_blade` / `skill_bio_keratin_blade`, Detonator, 削韌 45)。
- [x] **Task M5-3 (16-CANONICAL-SKILLS-INGESTION): 落地 16 大標準賽博修仙技能庫（仙俠表層定名）**
  - 依據 L1《CYBERPUNK_CULTIVATION_SPEC.md》與 L2《SKILL_SCHEMA.md》第四節 16 大技能矩陣，在 `party_skills.json` 完整補齊 16 項技能資料配置。所有技能對外展示名稱一律採用東方仙俠神通名稱（如【偷天換日·轉煞移因訣】、【燃魂溯光·剎那千秋步】），後端對齊四大 `cyberSchool` 與 `comboRole` 連攜機制。
  - `CyberSkillAndItemAffixTest` 與 `WorldDataIntegrityTest` 綠燈通過，前端呈現純正仙俠神通名，戰鬥中 Primer/Detonator 連攜機制運作正常。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task M5-2 (BREAKTHROUGH-TRIALS): 境界突破雷劫試煉副本**
  - **目標**：築基破金丹時，需消耗突破丹藥並觸發九重天劫考驗，成功後五維上限大幅躍遷。
  - **驗收標準**：突破失敗有保底保護，突破成功廣播天地異象。
- [ ] **Task M5-4 (17-TREES-SKILL-INGESTION-AND-TIER4-ULTIMATES): 17 專精樹 Tier 1~4 戰技與大絕招資料庫擴充**
  - **目標**：依據 L1《CYBERPUNK_CULTIVATION_SPEC.md》與 L2《M5_SKILL_AND_PROGRESSION.md》，在 `party_skills.json` 完整擴充 17 專精樹的 Tier 1~4 主動戰技（涵蓋【萬劍歸宗·太白凌虛大殺劫】、【子彈時間·天衍特工時停獵殺】等元嬰大絕招），嚴格落實「仙俠先行 × 底層真相揭示（cyberLore）」。
  - **驗收標準**：`WorldDataIntegrityTest` 綠燈通過，所有新技能在戰鬥中施放前搖、傷害倍率、削韌值、打斷標籤與冷卻時間均符合規格。
- [ ] **Task M5-5 (DYNAMIC-FORMATION-FINISHER-RESOLVER): 小隊分層合體技、四象領域與 5 人動態終結技解算器**
  - **目標**：在 `ComboResolver.java` 實裝分層解算架構：
    1. **法則反應引擎**：自動解算任意雙樹相剋裂變（水火蒸發 2.0x、火木擴散 DoT、引力聚怪、電磁穿甲、血肉木馬掠奪）。
    2. **傳奇名牌合體技**：解算 16 大雙星合璧（如太極水火、乾坤碎嶽、星隕高斯重力炮）與 8 大三才殺招（如萬毒天雨、天星地煞、千星瞬影）。
    3. **四象天道領域展開**：解算 5 大 4 樹極限領域（五行太乙萬劍、造物神殿、天衍神念時停、黑山羊肉相、四極降魔）。
    4. **5 人動態終結技**：動態掃描 5 人出戰成分之 15 大全窮盡形態（4 純系、6 雙曜、4 三才、1 萬象），純依隊伍流派體系決定，不限 3+2/2+3 比例，並聯動主角【萬象核心插槽】自由調和陣眼。
  - **驗收標準**：撰寫 `DynamicComboResolverTest` 驗證 2P 雙星反應、3P 三才裂變、4P 四象領域與 15 大 5P 終結技均能精準觸發，數值增益與特效標記無誤。
- [ ] **Task M5-6 (PASSIVE-SOCKET-SERVICE): 道骨晶片插槽與營地零成本洗點服務**
  - **目標**：實裝主角萬象核心插槽與隊友副修道骨插槽資料模型 (`SocketService.java`)，在營地【雜役演武道場】與【靈台藏經閣】提供無損自由插拔洗點。
  - **驗收標準**：更換道骨插槽後立即同步至 `PartyMember` 戰鬥屬性，存讀檔狀態正常持久化。
- [ ] **Task M5-7 (EXTENDED-CLASSIC-COMBOS-POOL): 長線低優先連攜擴充寶庫（Backlog / 低優先度）**
  - **目標**：依據 L1《CYBERPUNK_CULTIVATION_SPEC.md》第 3.5 節的長線連攜擴充寶庫，在基礎核心完成後，陸續注入更多玩家喜愛的經典拍檔（如【地湧金蓮·枯木逢春陣】、【水月洞天·太陰飛劍潮】、【萬骨機變·白骨液壓碎顱爪】、【幽靈特工·零日賽博天羅步】、【焚天滅度·虛無黑洞超新星】、【萬劍磁壁·超導御劍結界】等）。
  - **驗收標準**：本任務為長線平滑擴充池，不阻斷主流程發布；每次擴充新組合時同步補齊單元測試與資料完整性校驗。
