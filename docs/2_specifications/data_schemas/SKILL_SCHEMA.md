# ⚡ 賽博修仙技能資料架構規格 (Skill Schema Specification)

> **文檔層級**：Level 2 — Module Specifications (data_schemas)  
> **系統類型**：資料驅動技能模板與戰鬥法變契約  
> **核心定位**：對齊 Level 1《賽博修仙技能體系需求規格》，定義四大流派、超頻階級、戰鬥削韌（Poise）、元素反應與 Primer/Detonator 戰技契約。

---

## 📌 一、核心設計原則

1. **四大賽博修仙流派 (Schools)**：
   - `CYBER_HACK`（黑客劫持）：神經超頻、算力逆流、破綻打斷。
   - `GENE_BIO`（強殖生化）：外神同化、甲殼強殖、異形毒素 DoT。
   - `MECHA_CULT`（機械修真）：經脈義體化、飛劍高頻電弧、過載重創。
   - `VOID_PSI`（虛數靈能）：量子道心、空間折疊、虛空湮滅。
2. **爽快戰鬥參數完備 (Combat Synergy)**：
   - 削韌破防值 (`poiseDamage`)、元素反應屬性 (`element`)、裂魂引爆標籤 (`isPrimer`, `isDetonator`)、打斷施法標籤 (`interruptsCasting`)。

---

## 📐 二、技能結構定義 (Schema)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "CyberSkillTemplate",
  "type": "object",
  "required": ["id", "name", "school", "targetType", "cooldownMs", "damageMultiplier", "costs"],
  "properties": {
    "id": { "type": "string" },
    "name": { "type": "string" },
    "school": { "type": "string", "enum": ["CYBER_HACK", "GENE_BIO", "MECHA_CULT", "VOID_PSI"] },
    "targetType": { "type": "string", "enum": ["SINGLE_ENEMY", "ALL_ENEMIES", "FRONT_ROW_ENEMIES", "SINGLE_ALLY", "ALL_ALLIES", "SELF"] },
    "overclockTier": { "type": "integer", "default": 0, "minimum": 0, "maximum": 3 },
    "gcdMs": { "type": "integer", "default": 1000 },
    "castTimeMs": { "type": "integer", "default": 0 },
    "cooldownMs": { "type": "integer", "default": 3000 },
    "damageMultiplier": { "type": "number", "default": 1.0 },
    "poiseDamage": { "type": "integer", "default": 15 },
    "element": { "type": "string", "enum": ["PHYSICAL", "CHAOS", "FIRE", "ICE", "POISON", "LIGHTNING", "FORCE"] },
    "isPrimer": { "type": "boolean", "default": false },
    "isDetonator": { "type": "boolean", "default": false },
    "interruptsCasting": { "type": "boolean", "default": false },
    "costs": {
      "type": "object",
      "properties": {
        "mp": { "type": "integer", "default": 0 },
        "sp": { "type": "integer", "default": 0 },
        "hp": { "type": "integer", "default": 0 }
      }
    },
    "cyberLore": { "type": "string", "description": "科技底層與道法表象之雙重疊影描述" }
  }
}
```

---

## 📝 三、範例：首批序章戰技【神經超頻步】與【強殖角質刃】

```json
{
  "skills": [
    {
      "id": "skill_neural_overclock_step",
      "name": "神經超頻步 (Neuro-Overclock Dash)",
      "school": "CYBER_HACK",
      "targetType": "SINGLE_ENEMY",
      "overclockTier": 1,
      "gcdMs": 800,
      "castTimeMs": 0,
      "cooldownMs": 4000,
      "damageMultiplier": 1.4,
      "poiseDamage": 25,
      "element": "PHYSICAL",
      "isPrimer": true,
      "interruptsCasting": true,
      "costs": { "sp": 20 },
      "cyberLore": "特工晶片向運動神經束注入瞬時超頻電流，身法如鬼魅位移，劍鋒精準直刺敵方詠唱破綻。"
    },
    {
      "id": "skill_bio_keratin_blade",
      "name": "強殖角質刃 (Keratin Carapace Cleave)",
      "school": "GENE_BIO",
      "targetType": "SINGLE_ENEMY",
      "overclockTier": 1,
      "gcdMs": 1000,
      "castTimeMs": 0,
      "cooldownMs": 5000,
      "damageMultiplier": 2.2,
      "poiseDamage": 45,
      "element": "POISON",
      "isDetonator": true,
      "interruptsCasting": false,
      "costs": { "sp": 35 },
      "cyberLore": "激活外神母體退化基因，手臂骨骼瞬間刺出漆黑幾丁質刃鋒，對破防弱點造成毀滅引爆。"
    }
  ]
}
```
