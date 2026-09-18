# 技能資料整理與武器分類治理實施計畫與架構紀錄 (Skills Taxonomy & Weapon Binding Governance)

> **歸檔時間**：2026-09-18  
> **目標**：完成 resources/data/global/skills/ 的二級目錄治理，依照武器種類與修仙武學體系分門別類收納，擴充門派特色武學，加固武器與招式綁定機制，並建立自動化驗證。

---

## 1. 治理背景與架構動機

1. **原技能資料庫零散**：
   - 舊有技能檔散落於 common/、archery/、dodge/、force/、magic/、meditate/、parry/、throwing/、unarmed/ 等非正規化目錄，缺乏統一的武器大類與武學階級體系。
2. **武器綁定與門派特色武學不足**：
   - 刀法、槍法、杖法、匕首刺擊等缺乏鮮明門派代表性高階技能。
   - 夥伴（鐵牛、燕青、墨道人、芷若）初始武學套路（learnedStances）需要更貼合其門派與武器特性。
3. **安全防禦與容錯擴充**：
   - PartyMember.resolveSkillCategory 對被動/反應招式（REACTIVE）、仙術道法（MAGIC）需具備穩健的降級與容錯能力。
   - PICKAXE、SCYTHE 等特殊副類武器需要對齊基礎武器大類（AXE、POLEARM）。

---

## 2. 治理成果與架構體系

### 2.1 技能庫二級目錄體系 (resources/data/global/skills/)
`	ext
skills/
├── weapons/                  # 武器專屬武學體系
│   ├── blade/                # 刀法（基礎刀法、霸刀歸一斬、狂風絕息刀等）
│   ├── sword/                # 劍法（基本劍法、太極劍法、太陰幽冥劍法、天劍飛仙術）
│   ├── spear/                # 槍矛（基本長槍、破陣遊龍槍）
│   ├── blunt/                # 鈍器/杖法（基本棍棒、少林瘋魔杖法）
│   ├── dagger/               # 短兵/刺客（基本匕首、暗影無影幽冥刺）
│   ├── axe/                  # 斧鉞（基本開山斧、開山裂地斧）
│   ├── bow/                  # 弓弩（基本箭術、精準射擊、二連射、流星連珠）
│   ├── throwing/             # 暗器（基本暗器、漫天花雨）
│   └── whip/                 # 軟兵/鞭法（基本軟鞭）
├── martial_arts/             # 體術、身法與搏擊
│   ├── unarmed/              # 拳腳空手（基本拳腳、太極拳、獅子吼）
│   ├── dodge/                # 輕功身法（流雲步）
│   └── parry/                # 招架防禦（鐵布衫）
├── cultivation/              # 內功修為與定境
│   ├── force/                # 內功心法（紫霞神功、九轉混元功、素問靈素訣）
│   └── meditate/             # 禪定打坐（枯木禪）
├── spells/                   # 五行道術與法術
│   ├── elemental/            # 五行法術（烈火術、九天應元雷訣、玄冰聚煞引）
│   └── utility/              # 輔助與醫道（回春術、九轉回春、辟邪清心符）
├── corrupted/                # 克蘇魯與禁忌墮落武學
└── mobs/                     # 怪物專屬身法與攻擊（野鼠、不死族、太古赤龍等）
`

### 2.2 新增 8 個門派高階武學與道術
1. **霸刀門・霸刀歸一斬 (adao_blade.json)**：
   - 消耗：COMBO 3 | 冷卻：6000ms | 威力：260% 物理劈砍傷害。
   - 適用兵刃：BLADE, SABER, SCIMITAR。
2. **狂風門・狂風絕息刀 (storm_blade.json)**：
   - 消耗：COMBO 2 | 冷卻：3500ms | 威力：190% 物理劈砍傷害。
   - 適用兵刃：BLADE, SABER, SCIMITAR。
3. **天策府・破陣遊龍槍 (dragon_spear.json)**：
   - 消耗：RAGE 40 | 冷卻：7000ms | 威力：220% 物理突刺破甲傷害。
   - 適用兵刃：SPEAR, POLEARM, HALBERD, JAVELIN。
4. **少林寺・瘋魔杖法 (mad_demon_staff.json)**：
   - 消耗：RAGE 50 | 冷卻：8000ms | 威力：210% 鈍擊範圍傷害並震懾敵人。
   - 適用兵刃：STAFF, BLUNT, HAMMER。
5. **暗影閣・無影幽冥刺 (shadow_strike.json)**：
   - 消耗：COMBO 3 | 冷卻：4000ms | 威力：280% 致命刺殺傷害。
   - 適用兵刃：DAGGER, KNIFE, DIRK, STILETTO。
6. **巨力門・開山裂地斧 (mountain_split_axe.json)**：
   - 消耗：RAGE 45 | 冷卻：6500ms | 威力：240% 斬擊與破甲。
   - 適用兵刃：AXE, POLEAXE, HALBERD。
7. **神霄派・九天應元雷訣 (	hunder_strike.json)**：
   - 消耗：MP 45 | 冷卻：7500ms | 威力：230% 雷系道術傷害。
   - 傷害類型：LIGHTNING。
8. **太陰門・玄冰聚煞引 (ice_spear.json)**：
   - 消耗：MP 35 | 冷卻：5000ms | 威力：170% 冰煞減速傷害。
   - 傷害類型：ICE。

### 2.3 程式碼架構加固
- TemplateRepository.java：新增 getAllSkills()，支援全量技能快照查詢。
- PartyMember.java：
  - 增強 getMainHandWeaponType()：採礦鎬 PICKAXE 自動對齊 AXE，巨鐮 SCYTHE 自動對齊 POLEARM。
  - 增強 esolveSkillCategory()：凡是 SkillType.REACTIVE 技能或 MAGIC 仙術，若 llowedWeapons 為空或含 null，安全歸類為對應類別，防範 NPE。
- default_companions.json：
  - 鐵牛：配置狂風刀法與霸刀斬。
  - 燕青：配置無影幽冥刺。
  - 墨道人：配置九天應元雷訣。
  - 芷若：配置玄冰聚煞引。

---

## 3. 測試與驗證

- 新增專屬整合測試：src/test/java/com/example/htmlmud/integration/SkillsTaxonomyAndWeaponBindingTest.java
  1. 	estAllSkillsLoadedRecursively：驗證全域技能 67 個透過子目錄遞迴全數載入。
  2. 	estWeaponSkillBindingMapping：驗證 9 大武器種類與對應招式綁定關係。
  3. 	estCompanionLearnedStances：驗證夥伴初始配置門派特色武學正確。
  4. 	estFactionHighTierSkillsLoaded：驗證 8 大門派高階技能參數與屬性完好。
- 全專案回歸測試：
  - powershell -ExecutionPolicy Bypass -File .\test.ps1
  - 結果：Tests run: 94, Failures: 0, Errors: 0, Skipped: 0 -> **BUILD SUCCESS**
