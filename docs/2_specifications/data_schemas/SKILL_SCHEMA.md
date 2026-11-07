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
   - **專精樹表格欄位映射規範**：各專精樹規格表「戰術／連攜定位」欄若為 `INTERRUPT`、`BARRIER`、`HEAL`、`CONTROL`、`STEALTH`、`DEFENSE`、`PURIFY`、`EXECUTE`、`BUFF`、`TACTICAL`、`ULTIMATE`、`POISE_BREAK` 等戰術職能，在落盤為技能資料實體時，`comboRole` 一律填 `"NONE"`，並將該戰術職能寫入 `"tags"` 陣列中，以保持連攜合體技觸發器與一般戰術職能分離。

---

## 📐 二、主動神通與小隊合擊結構定義 (PartyMemberSkill - 現行 `party_skills.json` 載入契約)

> 💡 **契約對齊說明**：對齊 `PartyMemberSkill.java` 真實欄位。現行主動武技與 2P~4P 組合合體技均統一以本契約結構持久化於 `src/main/resources/data/party/party_skills.json`。

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "PartyMemberSkill",
  "type": "object",
  "required": ["id", "name", "costType", "costValue", "cooldownMs"],
  "properties": {
    "id": { "type": "string", "description": "全域唯一代碼，如 skill_null_pointer 或 combo_water_fire_taichi" },
    "name": { "type": "string", "description": "技能仙俠古典名稱，如【萬法皆空·空指針崩解印】" },
    "aliases": { "type": "array", "items": { "type": "string" }, "description": "技能別名列表" },
    "icon": { "type": "string", "description": "UI 呈現圖示" },
    "description": { "type": "string", "description": "技能敘事與戰鬥效果說明" },
    "originSchool": {
      "type": "string",
      "enum": ["CULTIVATOR", "ENGINEER", "HIGH_TECH", "CTHULHU", "UNIVERSAL", "NONE"],
      "description": "所屬四大淵源體系"
    },
    "treeId": { "type": "string", "description": "所屬專精樹代碼（17 專精樹之一）" },
    "tier": { "type": "integer", "minimum": 1, "maximum": 4, "default": 1, "description": "1:煉氣循環, 2:築基戰術, 3:金丹殺招, 4:元嬰大絕招" },
    "overclockTier": { "type": "integer", "minimum": 0, "maximum": 3, "default": 0, "description": "超頻增幅階級" },
    "comboRole": { "type": "string", "enum": ["NONE", "PRIMER", "DETONATOR"], "default": "NONE" },
    "costType": {
      "type": "string",
      "enum": ["MP", "SP", "HP", "RAGE", "COMBO", "STAMINA", "FORCE", "ENERGY"],
      "description": "主要戰鬥資源消耗類型"
    },
    "costValue": { "type": "integer", "default": 0, "description": "消耗點數" },
    "spCost": { "type": "integer", "default": 0 },
    "mpCost": { "type": "integer", "default": 0 },
    "hpCost": { "type": "integer", "default": 0 },
    "category": {
      "type": "string",
      "enum": ["WEAPON", "CLASS", "SPELL", "COMBO", "FORMATION", "PASSIVE"],
      "description": "技能槽位與功能分類"
    },
    "gcdMs": { "type": "integer", "default": 1200, "description": "公共冷卻時間（毫秒）" },
    "triggersGcd": { "type": "boolean", "default": true },
    "castTimeMs": { "type": "integer", "default": 0, "description": "吟唱前搖時間（0 為瞬發）" },
    "interruptible": { "type": "boolean", "default": true },
    "cooldownMs": { "type": "integer", "default": 4000, "description": "獨立調息冷卻時間（毫秒）" },
    "damageMultiplier": { "type": "number", "default": 1.0, "description": "傷害係數倍率" },
    "damageType": { "type": "string", "default": "PHYSICAL", "description": "傷害屬性（對齊 DamageType: PHYSICAL, METAL, WOOD, WATER, FIRE, EARTH, FORCE, CHAOS, TRUE）" },
    "poiseDamage": { "type": "integer", "default": 0, "description": "削韌破防強度" },
    "aoe": { "type": "boolean", "default": false, "description": "是否為群體範圍效果" },
    "heal": { "type": "boolean", "default": false, "description": "是否為治療技能" },
    "healAmount": { "type": "integer", "default": 0 },
    "sanRestore": { "type": "integer", "default": 0, "description": "道心靈智回復點數" },
    "taunt": { "type": "boolean", "default": false },
    "stun": { "type": "boolean", "default": false },
    "stunDurationSeconds": { "type": "integer", "default": 0 },
    "shield": { "type": "boolean", "default": false },
    "buff": { "type": "boolean", "default": false },
    "defense": { "type": "boolean", "default": false },
    "tags": { "type": "array", "items": { "type": "string" }, "description": "戰術特性標籤清單" },
    "allowedWeapons": { "type": "array", "items": { "type": "string" }, "description": "允許適配之武器類型" },
    "synergy": { "type": "boolean", "default": false, "description": "是否為小隊組合合體技" },
    "requiredClasses": { "type": "array", "items": { "type": "string" }, "description": "合體技所需隊員職業" },
    "minPartyAlive": { "type": "integer", "default": 1 },
    "requiredFormation": { "type": "string", "description": "觸發所需陣法" },
    "formationEnergyCost": { "type": "integer", "default": 0, "description": "陣法能量消耗" },
    "synergyType": { "type": "string", "description": "合擊連攜類型識別" },
    "reactionType": { "type": "string", "description": "元素相變反應識別" },
    "cyberLore": { "type": "string", "description": "底層科技原貌描述文本" }
  }
}
```

---

## 🧬 三、道骨/晶片心法結構定義 (`PassiveTalentTemplate`)【待深入討論／長線演進藍圖】

> ⚠️ **架構說明**：被動心法目前在現行執行期透過 `PartyMemberSkill`（`category: "PASSIVE"`）與 `PartyMember.passiveSlots`（`DODGE`, `PARRY`, `FORCE`）掛載。獨立插槽式 `PassiveTalentTemplate` 列為長線擴充藍圖。

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

## 💥 四、小隊合體與動態陣法終結技契約 (`SynergySkillTemplate`)【待深入討論／長線演進藍圖】

> ⚠️ **架構實況與邊界說明**：
> 1. 現行 **2P~4P 名牌合體技** 直接存儲於 `src/main/resources/data/party/party_skills.json`（以 §二 `PartyMemberSkill` 之 `synergy: true` 欄位承載）。
> 2. 現行 **15 大 5P 全隊動態終結技** 則由 [`FormationFinisherClassifier.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/combat/FormationFinisherClassifier.java) 於戰鬥中根據小隊 5 人淵源成分即時判定觸發。
> 3. 以下含巢狀 `requiredConditions` 與 `effects` 之獨立結構化 `SynergySkillTemplate` 屬於長線資料重構藍圖。

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "SynergySkillTemplate",
  "type": "object",
  "required": ["id", "name", "tierLevel", "formationEnergyCost", "synergyType"],
  "properties": {
    "id": { "type": "string" },
    "aliases": { "type": "array", "items": { "type": "string" } },
    "name": { "type": "string" },
    "tierLevel": {
      "type": "string",
      "enum": ["DUAL_STAR_2P", "TRI_RESONANCE_3P", "QUAD_DOMAIN_4P", "QUINTET_FINISHER_5P"]
    },
    "synergyType": { "type": "string" },
    "reactionType": { "type": "string", "default": "NONE" },
    "formationEnergyCost": { "type": "integer", "default": 100 },
    "requiredConditions": {
      "type": "object",
      "properties": {
        "minPartyAlive": { "type": "integer", "default": 2 },
        "requiredTrees": { "type": "array", "items": { "type": "string" } },
        "requiredSchools": { "type": "array", "items": { "type": "string" } }
      }
    },
    "effects": {
      "type": "object",
      "properties": {
        "instantPoiseBreak": { "type": "boolean", "default": false },
        "poiseDamage": { "type": "integer", "default": 0 },
        "timeStopDurationMs": { "type": "integer", "default": 0 },
        "partyHealPercent": { "type": "number", "default": 0.0 }
      }
    },
    "cyberLore": { "type": "string" }
  }
}
```

---

## 📝 五、標準資料範例（100% 對齊真實 `party_skills.json` 載入契約）

### 範例一：單人主動殺招 (`skill_null_pointer`)

```json
{
  "id": "skill_null_pointer",
  "name": "【萬法皆空·空指針崩解印】",
  "icon": "🕳️💥",
  "description": "指尖引動空無死印：當目標生命值低於 15% 且罡氣破防時，直接引爆湮滅消散！",
  "originSchool": "CTHULHU",
  "treeId": "tree_null_void",
  "tier": 3,
  "overclockTier": 3,
  "comboRole": "DETONATOR",
  "costType": "MP",
  "costValue": 50,
  "gcdMs": 1500,
  "castTimeMs": 0,
  "cooldownMs": 12000,
  "damageMultiplier": 3.5,
  "damageType": "CHAOS",
  "poiseDamage": 70,
  "aoe": false,
  "heal": false,
  "allowedWeapons": [],
  "tags": [
    "CTHULHU",
    "DETONATOR",
    "EXECUTE"
  ],
  "cyberLore": "在物理層面對指標指向之記憶體區塊實施硬體斷電，令目標物質架構直接失序坍塌。"
}
```

### 範例二：名牌三人合體技 (`combo_water_fire_taichi`，相容 `combo_water_fire_taiji` 別名)

```json
{
  "id": "combo_water_fire_taichi",
  "name": "【太極水火·兩儀陰陽寂滅印】",
  "aliases": [
    "combo_water_fire_taiji"
  ],
  "icon": "🔥🌊",
  "description": "法師引玄冰真火，醫者融素問靈露，戰士橫刀護法，全隊受擊減免 50% 且全體回復 50 氣血！",
  "costType": "MP",
  "costValue": 50,
  "cooldownMs": 20000,
  "damageMultiplier": 2.0,
  "aoe": true,
  "heal": true,
  "taunt": false,
  "stun": false,
  "healAmount": 50,
  "sanRestore": 15,
  "allowedWeapons": [],
  "synergy": true,
  "requiredClasses": [
    "MAGE",
    "CLERIC",
    "WARRIOR"
  ],
  "minPartyAlive": 3,
  "requiredFormation": null,
  "formationEnergyCost": 50,
  "synergyType": "SIGNATURE_COMBO",
  "reactionType": "VAPORIZE",
  "cyberLore": "利用水冷與熱超載相位差引發電漿相變爆縮，形成兩儀力場護佑全隊。"
}
```

### 範例三：雙人跨源連攜殺招 (`combo_chitin_kinetic_superconductor`)

```json
{
  "id": "combo_chitin_kinetic_superconductor",
  "name": "【生化超導·破空磁煞幾丁刃】",
  "icon": "🧬⚡🗡️",
  "description": "外神漆黑幾丁骨刃與天基電磁動能強烈共鳴超導，一刀兩斷貫穿敵方全體前列，引爆裂魂標記造成 100% 暴擊穿甲！",
  "costType": "SP",
  "costValue": 45,
  "cooldownMs": 15000,
  "damageMultiplier": 3.8,
  "damageType": "PHYSICAL",
  "poiseDamage": 70,
  "aoe": true,
  "heal": false,
  "allowedWeapons": [],
  "tags": [
    "CTHULHU",
    "HIGH_TECH",
    "SYNERGY",
    "DETONATOR",
    "AOE"
  ],
  "synergy": true,
  "requiredClasses": [],
  "minPartyAlive": 2,
  "requiredFormation": null,
  "formationEnergyCost": 35,
  "synergyType": "SIGNATURE_COMBO",
  "reactionType": "SUPERCONDUCTOR",
  "cyberLore": "外神活性幾丁質骨刃充當常溫超導介質，引導微型電磁加速線圈超高頻放電，瞬間熔斷敵方護盾。"
}
```
