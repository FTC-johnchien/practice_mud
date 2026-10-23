# ⚡ 賽博修仙技能資料架構規格 (Skill Schema Specification)

> **文檔層級**：Level 2 — Module Specifications (data_schemas)  
> **系統類型**：資料驅動技能模板與戰鬥法變契約  
> **核心定位**：對齊 Level 1《賽博修仙技能體系需求規格》，定義四大流派、超頻階級、戰鬥削韌（Poise）、元素反應與 Primer/Detonator 戰技契約。

---

## 📌 一、核心設計原則

1. **四大賽博修仙流派 (Schools 1:1 對齊 L1 四大體系)**：
   - `CYBER_HACK`（網路安全與權限劫持類 / 虛空神經道）：神經超頻、算力逆流、破綻打斷、因果劫持。
   - `GENE_BIO`（生物駭客與義體過載類 / 生化血肉道）：異形強殖、外骨骼甲殼、煞種寄生、排斥控制。
   - `MECHA_CULT`（光學渲染與認知介面類 / 重力機械修真）：粒子震動刃、像素降噪、全息蜃樓、幀率鎖死。
   - `VOID_PSI`（底層架構與崩潰重置類 / 虛數靈能矩陣）：空指針解構、核心冷重啟、記憶體洩漏、格式化天劫。
2. **爽快戰鬥參數完備 (Combat Synergy)**：
   - 削韌破防值 (`poiseDamage`)、元素反應屬性 (`element`)、裂魂引爆標籤 (`comboRole`: `PRIMER` / `DETONATOR`)、打斷施法標籤 (`interruptsCasting`)。

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
    "comboRole": {
      "type": "string",
      "enum": ["NONE", "PRIMER", "DETONATOR"],
      "default": "NONE",
      "description": "連攜角色：NONE(無)、PRIMER(裂魂印記先手)、DETONATOR(引爆先手印記)。代碼層 isPrimer()/isDetonator() 為派生或相容別名"
    },
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
      "name": "神經超頻步",
      "school": "CYBER_HACK",
      "targetType": "SINGLE_ENEMY",
      "overclockTier": 1,
      "comboRole": "PRIMER",
      "gcdMs": 800,
      "castTimeMs": 0,
      "cooldownMs": 4000,
      "damageMultiplier": 1.4,
      "poiseDamage": 25,
      "element": "PHYSICAL",
      "interruptsCasting": true,
      "costs": { "sp": 20 },
      "cyberLore": "特工晶片向運動神經束注入瞬時超頻電流，身法如鬼魅位移，劍鋒精準直刺敵方詠唱破綻。"
    },
    {
      "id": "skill_bio_keratin_blade",
      "name": "強殖角質刃",
      "school": "GENE_BIO",
      "targetType": "SINGLE_ENEMY",
      "overclockTier": 1,
      "comboRole": "DETONATOR",
      "gcdMs": 1000,
      "castTimeMs": 0,
      "cooldownMs": 5000,
      "damageMultiplier": 2.2,
      "poiseDamage": 45,
      "element": "POISON",
      "interruptsCasting": false,
      "costs": { "sp": 35 },
      "cyberLore": "激活外神母體退化基因，手臂骨骼瞬間刺出漆黑幾丁質刃鋒，對破防弱點造成毀滅引爆。"
    }
  ]
}
```

---

## 📜 四、Level 1 十六項神通標準資料契約對照表 (The 16 Canonical Cyber Skills Matrix)

> 📌 **【技能表層仙俠先行呈現準則 (Option C)】**：  
> 遊戲中面向玩家呈現的所有 UI 介面、技能樹、戰鬥卡片與狀態欄中，展示名稱**一律使用【仙俠神通名稱】**（如【偷天換日·轉煞移因訣】）。底層資料結構則攜帶對應之 `cyberSchool`、`comboRole`、`poiseDamage` 等戰術數值。玩家在探索深層地牢或特工晶片加載度突破閾值（>70%）時，技能日誌方解鎖科技原貌解析。

依據 Level 1《[CYBERPUNK_CULTIVATION_SPEC.md](../../1_requirements/CYBERPUNK_CULTIVATION_SPEC.md)》，全專案 16 大核心神通技術規格與契約對應如下：

| # | 技能 ID (`id`) | 神通名稱 | 科技原貌 | 所屬流派 (`school`) | 連攜角色 (`comboRole`) | 元素屬性 (`element`) | 核心戰術機制與代價博弈 |
|---|---|---|---|---|---|---|---|
| 1 | `skill_mitm_transfer` | 【偷天換日·轉煞移因訣】 | MITM 流量劫持 | `CYBER_HACK` | `PRIMER` | `CHAOS` | 技能反噬或 SAN 扣減代價 100% 轉嫁敵方目標承受。 |
| 2 | `skill_docker_sandbox` | 【太上太極·胎藏隔識印】 | Sandbox 沙盒隔離 | `CYBER_HACK` | `NONE` | `FORCE` | 短時間免疫精神衝擊/幻覺/即死，期間無法調用高階靈氣。 |
| 3 | `skill_ddos_flooding` | 【萬念俱雜·三千紅塵濁浪潮】 | DDoS 巨量阻斷攻擊 | `CYBER_HACK` | `DETONATOR` | `CHAOS` | 心神過載走火入魔硬直，強制**打斷 (Interrupt)** 蓄力前搖。 |
| 4 | `skill_bruteforce_crack` | 【周天易數·萬相破陣梭】 | Brute-force 字典破解 | `CYBER_HACK` | `DETONATOR` | `PHYSICAL` | 無視護甲與罡氣韌性，造成真實貫穿傷害。 |
| 5 | `skill_hotpatch_firmware` | 【虛月逆行·皮相歸元法】 | Hot-patching 韌體熱抽換 | `GENE_BIO` | `NONE` | `FORCE` | 重置法變時間，平息神識過載，防範精神萎靡狀態。 |
| 6 | `skill_neural_overclock_step` | 【燃魂溯光·剎那千秋步】 | Neural Overclocking | `GENE_BIO` / `CYBER_HACK` | `PRIMER` | `PHYSICAL` | 移動迴避大幅提升，前搖歸零，每秒持續扣除生命氣血。 |
| 7 | `skill_shell_injection` | 【萬蟲蝕命·煞種寄生禁】 | SQL/Shell Injection 注入 | `GENE_BIO` | `PRIMER` | `POISON` | 敵方消耗法力時符文同步引爆，轉化為臟腑破爆傷害。 |
| 8 | `skill_immuno_override` | 【移花接木·太歲肉身契】 | Immunosuppressive Override | `GENE_BIO` | `NONE` | `FORCE` | 允許跨級裝備高污染法寶/器官，大幅降低排斥暴斃率。 |
| 9 | `skill_glitch_texture` | 【太虛匿影·無貌欺天障】 | Glitch 貼圖丟失 | `MECHA_CULT` | `PRIMER` | `CHAOS` | 絕對隱匿脫離仇恨列表，下次突襲必定 100% 暴擊。 |
| 10 | `skill_pixel_mosaic` | 【大智若愚·知見自盲術】 | Downsampling 畫素降噪 | `MECHA_CULT` | `NONE` | `FORCE` | 降低 80% 面對外神之精神衝擊 (SAN 扣減)，命中暫降 25%。 |
| 11 | `skill_deepfake_mirror` | 【虛妄重構·三世蜃樓鏡】 | Deepfake 偽造欺騙 | `MECHA_CULT` | `NONE` | `LIGHTNING` | 召喚 3 具 30% HP 虛影分身強制吸引仇恨，本體後撤。 |
| 12 | `skill_framerate_lock` | 【逆熵之眸·量子觀測鎖】 | Frame-rate Locking | `MECHA_CULT` | `NONE` | `FORCE` | 敵方全體 3 秒內攻擊與冷卻進度暴跌 70%。 |
| 13 | `skill_null_pointer` | 【萬法皆空·空指針崩解印】 | Null Pointer Dereference | `VOID_PSI` | `DETONATOR` | `CHAOS` | 斬殺技：目標生命 < 15% 且罡氣破防時，直接湮滅消散。 |
| 14 | `skill_hard_reboot` | 【九幽輪迴·硬體冷重啟】 | Hard Reboot 冷重啟 | `VOID_PSI` | `NONE` | `FIRE` | 致命傷觸發被動：免死、清負面 Debuff、30% 氣血復甦 (限一次)。 |
| 15 | `skill_memory_leak` | 【太初鴻濛·記憶體洩漏術】 | Memory Leak 資源耗盡 | `VOID_PSI` | `PRIMER` | `POISON` | 終極 DoT：每回合傷害倍增，直至敵方無法施放消耗法力招式。 |
| 16 | `skill_format_drive` | 【歸墟終末·格式化天劫】 | Format Drive 磁區格式化 | `VOID_PSI` | `DETONATOR` | `LIGHTNING` | 全場無差別毀滅打擊，洗清全場所有 Buff 與地形效果。 |
