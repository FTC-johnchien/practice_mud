# ⚡ 賽博修仙技能資料架構規格 (Skill Schema Specification)

> **文檔層級**：Level 2 — Module Specifications (data_schemas)  
> **系統類型**：資料驅動技能模板、被動道骨晶片與小隊合體技契約  
> **核心定位**：1:1 鏡像對齊 Level 1《賽博修仙與克蘇魯神話神通體系需求規格》，定義四大淵源體系、十七專精樹、Tier 1~4 四階武技（含元嬰大絕招）、被動道骨晶片插槽與多維合體技契約。

---

## 📌 一、核心設計原則與流派映射

1. **四大淵源體系 (`originSchool`) 1:1 對齊 L1**：
   - `CULTIVATOR`（東方修仙道統類）：五行基底（金/木/水/火/土）與四象變異（雷/風/陰/陽）。主打元素連鎖、真元續航與陣法光環。
   - `ENGINEER`（古代工程師類）：重力定錨、液壓裂解、蜂巢金剛、地核熔爐。主打破防削韌（Poise Break）、黑洞聚怪與重坦護盾。
   - `HIGH_TECH`（賽博高科技類）：時序超頻、協議逆亂、全息匿影、電磁動能。主打 100% 唱條打斷、子彈時間時停與真實破甲。
   - `CTHULHU`（克蘇魯外神類）：強殖幾丁質、黑山羊血裔、知見污染、虛無崩解。主打裂魂引爆 (`DETONATOR`)、嗜血吸血與斬殺。
2. **階梯縱深與大絕招 (Tier 1 ~ Tier 4)**：
   - 每棵專精樹具備 4 階主動武技：
     - `TIER_1` (煉氣·基礎循環 / 施加 Primer)
     - `TIER_2` (築基·核心戰術 / 打斷 / 破防削韌 / 增益)
     - `TIER_3` (金丹·極限殺招 / Detonator 引爆 / 巨額 AOE)
     - `TIER_4` (元嬰·天道大絕招 / 全場領域 / 毀滅性終結技)
   - 以及 1 個 `PASSIVE_TALENT`（道骨/晶片心法，裝備於角色插槽）。
3. **戰術標籤完備性與欄位映射規則 (Tactical Tags & Mapping Rules)**：
   - `comboRole`: 嚴格限定 `"NONE"` | `"PRIMER"` | `"DETONATOR"`。
   - **專精樹表格欄位映射規範**：各專精樹規格表「戰術／連攜定位」欄若為 `INTERRUPT`、`BARRIER`、`HEAL`、`CONTROL`、`STEALTH`、`DEFENSE`、`PURIFY`、`EXECUTE`、`BUFF`、`TACTICAL`、`ULTIMATE`、`POISE_BREAK` 等戰術職能，在落盤為技能資料實體時，`comboRole` 一律填 `"NONE"`，並將該戰術職能寫入 `"tags"`（或 `"synergyTags"`）陣列中，以保持連攜合體技觸發器與一般戰術職能分離。

---

## 📐 二、主動神通結構定義 (`ActiveSkillTemplate`)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ActiveSkillTemplate",
  "type": "object",
  "required": ["id", "name", "originSchool", "treeId", "tier", "targetType", "cooldownMs", "damageMultiplier", "costs"],
  "properties": {
    "id": { "type": "string", "description": "全域唯一代碼，如 skill_gold_tier4_wanjian" },
    "name": { "type": "string", "description": "仙俠先行古典名稱，如【萬劍歸宗·太白凌虛大殺劫】" },
    "originSchool": { 
      "type": "string", 
      "enum": ["CULTIVATOR", "ENGINEER", "HIGH_TECH", "CTHULHU"],
      "description": "四大淵源體系"
    },
    "treeId": { 
      "type": "string", 
      "description": "所屬專精樹，如 tree_gold_sword, tree_gravity, tree_time_overclock, tree_chitin_blade 等共 17 樹" 
    },
    "tier": { 
      "type": "integer", 
      "minimum": 1, 
      "maximum": 4, 
      "description": "1:煉氣循環, 2:築基戰術, 3:金丹殺招, 4:元嬰大絕招" 
    },
    "targetType": { 
      "type": "string", 
      "enum": ["SINGLE_ENEMY", "ALL_ENEMIES", "FRONT_ROW_ENEMIES", "COLUMN_ENEMIES", "SINGLE_ALLY", "ALL_ALLIES", "SELF"] 
    },
    "gcdMs": { "type": "integer", "default": 1000, "description": "公共冷卻時間" },
    "castTimeMs": { "type": "integer", "default": 0, "description": "吟唱前搖時間（可被打斷）" },
    "cooldownMs": { "type": "integer", "default": 4000, "description": "獨立冷卻時間" },
    "damageMultiplier": { "type": "number", "default": 1.0 },
    "poiseDamage": { "type": "integer", "default": 15, "description": "削韌破防值" },
    "element": { 
      "type": "string", 
      "enum": ["PHYSICAL", "METAL", "WOOD", "WATER", "FIRE", "EARTH", "LIGHTNING", "WIND", "POISON", "HOLY", "DARK", "CHAOS", "TRUE"] 
    },
    "comboRole": {
      "type": "string",
      "enum": ["NONE", "PRIMER", "DETONATOR"],
      "default": "NONE"
    },
    "synergyTags": {
      "type": "array",
      "items": { "type": "string" },
      "description": "戰術特性標籤：INTERRUPT, POISE_BREAK, GRAVITY_PULL, BARRIER, LIFE_LEECH, TIME_STOP, EXECUTE"
    },
    "costs": {
      "type": "object",
      "properties": {
        "mp": { "type": "integer", "default": 0 },
        "sp": { "type": "integer", "default": 0 },
        "hp": { "type": "integer", "default": 0 }
      }
    },
    "cyberLore": { 
      "type": "string", 
      "description": "底層文明原貌（特工晶片加載度 >70% 解鎖雙重疊影文本）" 
    }
  }
}
```

---

## 🧬 三、道骨/晶片心法結構定義 (`PassiveTalentTemplate`)【已決議待實作】

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "PassiveTalentTemplate",
  "type": "object",
  "required": ["id", "name", "originSchool", "treeId", "socketType", "modifiers"],
  "properties": {
    "id": { "type": "string", "description": "道骨/晶片代碼，如 talent_core_genesis_nexus" },
    "name": { "type": "string", "description": "仙俠主稱謂，如【劍心通明骨】 / 【神經突觸超頻晶片】" },
    "originSchool": { "type": "string", "enum": ["CULTIVATOR", "ENGINEER", "HIGH_TECH", "CTHULHU", "UNIVERSAL"] },
    "treeId": { "type": "string" },
    "socketType": { 
      "type": "string", 
      "enum": ["CORE_CHIP_PROTAGONIST", "SUB_SCHOOL_BONE", "TALENT_CHIP"] 
    },
    "modifiers": {
      "type": "object",
      "properties": {
        "critRateBonus": { "type": "number", "default": 0.0 },
        "poiseBreakBonus": { "type": "number", "default": 0.0 },
        "damageReductionPercent": { "type": "number", "default": 0.0 },
        "mpRegenOnCombo": { "type": "integer", "default": 0 },
        "darknessSanDrainReduction": { "type": "number", "default": 0.0 },
        "deathPrevention": { "type": "boolean", "default": false }
      }
    },
    "cyberLore": { "type": "string" }
  }
}
```

---

## 💥 四、小隊合體與動態陣法終結技契約 (`SynergySkillTemplate`)【已決議待實作】

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "SynergySkillTemplate",
  "type": "object",
  "required": ["id", "name", "tierLevel", "formationEnergyCost", "synergyType"],
  "properties": {
    "id": { "type": "string" },
    "aliases": { 
      "type": "array", 
      "items": { "type": "string" },
      "description": "相容現行實作之主鍵別名列表" 
    },
    "name": { "type": "string" },
    "tierLevel": { 
      "type": "string", 
      "enum": ["DUAL_STAR_2P", "TRI_RESONANCE_3P", "QUAD_DOMAIN_4P", "QUINTET_FINISHER_5P"] 
    },
    "synergyType": {
      "type": "string",
      "enum": [
        "REACTION_DUAL", "SIGNATURE_DUAL", "SIGNATURE_TRIO", "QUAD_DOMAIN_EXPANSION",
        "MONO_CULTIVATOR_5P", "MONO_ENGINEER_5P", "MONO_HIGH_TECH_5P", "MONO_CTHULHU_5P",
        "QUAD_BALANCED_NEXUS_5P",
        "DUAL_XIAN_MO_5P", "DUAL_CYBER_MECHA_5P", "DUAL_XIAN_CYBER_5P", "DUAL_XIAN_MECHA_5P", "DUAL_CTHULHU_CYBER_5P", "DUAL_CTHULHU_MECHA_5P",
        "TRIO_XIAN_MO_CYBER_5P", "TRIO_XIAN_MO_MECHA_5P", "TRIO_XIAN_CYBER_MECHA_5P", "TRIO_CTHULHU_CYBER_MECHA_5P"
      ]
    },
    "reactionType": {
      "type": "string",
      "enum": [
        "NONE", "STEAM", "SPREAD", "GROWTH", "MELT", "MUD_LOCK", 
        "GRAVITY_COLLAPSE", "SUPERCONDUCTOR", "BLOOD_TROJAN", "ABYSS_TERROR", 
        "MAGMA_BLOOD", "DOMAIN_BURST"
      ],
      "default": "NONE"
    },
    "requiredConditions": {
      "type": "object",
      "properties": {
        "minPartyAlive": { "type": "integer", "default": 2 },
        "requiredTrees": {
          "type": "array",
          "items": { "type": "string" },
          "description": "指定觸發之專精樹 ID（如 [tree_water_frost, tree_fire_flame]）"
        },
        "requiredSchools": { 
          "type": "array", 
          "items": { "type": "string" },
          "description": "所需體系成分（例如 [CULTIVATOR, CULTIVATOR] 或 [CULTIVATOR, ENGINEER, HIGH_TECH, CTHULHU]）"
        },
        "dominantSchoolCount": { "type": "integer", "description": "純化流派門檻（如 ≥4）" }
      }
    },
    "formationEnergyCost": { "type": "integer", "default": 100 },
    "effects": {
      "type": "object",
      "properties": {
        "instantPoiseBreak": { "type": "boolean", "default": false },
        "poiseDamage": { "type": "integer", "default": 0 },
        "timeStopDurationMs": { "type": "integer", "default": 0 },
        "partyHealPercent": { "type": "number", "default": 0.0 },
        "allDetonateBonusMultiplier": { "type": "number", "default": 1.0 },
        "fullPartyMpRefill": { "type": "boolean", "default": false },
        "domainDurationSec": { "type": "integer", "default": 0 }
      }
    },
    "cyberLore": { "type": "string" }
  }
}
```

---

## 📝 五、標準資料範例（涵蓋 2P/3P/4P 名牌合體與 Tier 4 元嬰大絕招）

```json
[
  {
    "id": "skill_gold_tier4_wanjian",
    "name": "【萬劍歸宗·太白凌虛大殺劫】",
    "originSchool": "CULTIVATOR",
    "treeId": "tree_gold_sword",
    "tier": 4,
    "targetType": "ALL_ENEMIES",
    "gcdMs": 1500,
    "castTimeMs": 1000,
    "cooldownMs": 30000,
    "damageMultiplier": 3.8,
    "poiseDamage": 60,
    "element": "METAL",
    "comboRole": "DETONATOR",
    "synergyTags": ["POISE_BREAK", "AOE_SWEEP"],
    "costs": { "mp": 60 },
    "cyberLore": "調用全域磁懸浮飛劍集束超音速飽和打擊，瞬間摧毀敵怪分子防護力場。"
  },
  {
    "id": "combo_water_fire_taiji",
    "aliases": ["combo_water_fire_taichi"],
    "name": "【太極水火·兩儀陰陽寂滅印】",
    "tierLevel": "DUAL_STAR_2P",
    "synergyType": "SIGNATURE_DUAL",
    "reactionType": "STEAM",
    "formationEnergyCost": 40,
    "requiredConditions": {
      "minPartyAlive": 2,
      "requiredTrees": ["tree_water_frost", "tree_fire_flame"]
    },
    "effects": {
      "allDetonateBonusMultiplier": 2.5,
      "poiseDamage": 60,
      "instantPoiseBreak": false,
      "partyHealPercent": 0.0
    },
    "cyberLore": "水火同源熱力學碰撞，先施加霜凍再瞬爆蒸發，引爆 3 段混元蒸氣衝擊波。（註：現行 party_skills.json 主鍵 combo_water_fire_taichi 透過 aliases: ['combo_water_fire_taiji'] 相容）"
  },
  {
    "id": "combo_3p_toxic_steam",
    "name": "【萬毒天雨·碧落赤霞蒸仙煞】",
    "tierLevel": "TRI_RESONANCE_3P",
    "synergyType": "SIGNATURE_TRIO",
    "reactionType": "SPREAD",
    "formationEnergyCost": 65,
    "requiredConditions": {
      "minPartyAlive": 3,
      "requiredTrees": ["tree_water_frost", "tree_fire_flame", "tree_wood_life"]
    },
    "effects": {
      "allDetonateBonusMultiplier": 2.5,
      "instantPoiseBreak": false
    },
    "cyberLore": "沸騰生化劇毒氣溶膠全場擴散，敵怪每 0.5 秒承受水火毒三重 DoT 傷害。"
  },
  {
    "id": "domain_quad_celestial_blade",
    "name": "【太乙混元·五行四象破天劍煞界】",
    "tierLevel": "QUAD_DOMAIN_4P",
    "synergyType": "QUAD_DOMAIN_EXPANSION",
    "formationEnergyCost": 100,
    "requiredConditions": {
      "minPartyAlive": 4,
      "requiredSchools": ["CULTIVATOR", "CULTIVATOR", "CULTIVATOR", "CULTIVATOR"]
    },
    "effects": {
      "domainDurationSec": 8,
      "fullPartyMpRefill": true
    },
    "cyberLore": "Element Overclock Cascade: 五行四象萬劍領域展開，所有基礎法術冷卻減半且真元消耗歸零。"
  },
  {
    "id": "combo_5p_protocol_genesis",
    "name": "【大衍混元·諸天萬象開天闢地神劫】",
    "tierLevel": "QUINTET_FINISHER_5P",
    "synergyType": "QUAD_BALANCED_NEXUS_5P",
    "formationEnergyCost": 100,
    "requiredConditions": {
      "minPartyAlive": 5,
      "requiredSchools": ["CULTIVATOR", "ENGINEER", "HIGH_TECH", "CTHULHU"]
    },
    "effects": {
      "instantPoiseBreak": true,
      "timeStopDurationMs": 2000,
      "partyHealPercent": 0.5,
      "allDetonateBonusMultiplier": 2.5
    },
    "cyberLore": "Protocol Genesis: 行星地核冷格式化指令，四源合一全面覆寫戰場因果。"
  }
]
```
