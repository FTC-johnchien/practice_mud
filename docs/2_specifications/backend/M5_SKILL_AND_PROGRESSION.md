# ⚡ 後端 M5: 技能、境界與修煉模組深度手冊 (Skill & Progression Manual)

> **模組編號**：M5  
> **系統類型**：後端核心業務模組  
> **核心定位**：管理修仙境界突破、道基經驗成長、四大淵源十七專精技能樹、Tier 1~4 縱深（含元嬰大絕招）、道骨晶片插槽與多維小隊合體技解算演算法。

---

## 📈 一、境界突破與道基成長曲線 (Progression)

修仙之路遵循傳統道門境界階梯（`classes.json`）：
- **練氣期 (Qi Refinement)**：1 ~ 9 層。解鎖各樹 **Tier 1 (基礎循環武技)**。每次升級全隊套用職業成長，**主角（`isLeader`）額外獲得每級 2 點自由屬性點**（體質/真元/神識/根骨/身法）。
- **築基期 (Foundation Building)**：解鎖各樹 **Tier 2 (核心戰術招 / 打斷 / 破防削韌)** 與第二主動法術位，開放副修道骨插槽藍圖。
- **金丹期 (Golden Core)**：解鎖各樹 **Tier 3 (爆發殺招 / Detonator 引爆)** 與罡氣護體，解鎖五行/重力/特工領域。
- **元嬰期 (Nascent Soul)**：解鎖各樹 **Tier 4 (天道大絕招 / 全場領域 / 毀滅性終結)** 與全隊 5 人合體終結技。

### 升級公式與點數結算 (`XpProgressionService.java`)
- 升級所需經驗曲線（對齊 `calculateNextLevelExp`）：
  $$\text{NextLevelExp}(L) = \max(180, \lfloor 60 \times L^{1.6} + 120 \times L \rfloor)$$
  *(Level 1 升級基準為 180 點經驗)*
- 戰勝魔物時，隊長與存活隊員平均分配戰鬥經驗值。
- 當經驗槽滿載升級時，全隊套用職業成長（`ClassTemplate.growth`），**主角（`isLeader`）額外獲得每級 2 點自由分配屬性點**，並發布 `PlayerEvents.LevelUp` 事件刷新氣血上限。

---

## ⚔️ 二、大一統技能模型與動態屬性縮放 (Unified Skill & Scaling Engine)

專案確立大一統技能模型 [`UnifiedSkill`](../../../src/main/java/com/example/htmlmud/party/UnifiedSkill.java) 與 [`PartyMemberSkill`](../../../src/main/java/com/example/htmlmud/party/PartyMemberSkill.java)：

### 1. 內聚合一的核心契約 (Unified Contract)
大一統模型同時封裝三大面向：
- **修煉境界維度**：道法等級（`level` 1~10，上限 10 級）、熟練度累積（`xp` / `maxXp`）、境界倍率（`masteryMultiplier`）。
- **戰術機制維度**：傷害倍率（`damageMultiplier`）、冷卻時間（`cooldownMs`）、資源消耗（`costType` / `costValue`）、反制標籤（`INTERRUPT`, `POISE_BREAK`, `BARRIER`, `LIFE_LEECH`）、削韌（`poiseDamage`）。
- **連攜與合體維度**：連攜角色（`comboRole`: `NONE` / `PRIMER` / `DETONATOR`）、所屬淵源（`originSchool`）、專精樹（`treeId`）、階級（`tier`: 1~4）。

### 2. 動態屬性與境界縮放（對齊 `PartyMemberSkill` 真實公式）
- 技能升級所需熟練度曲線（對齊 `calculateRequiredXpForLevel`）：
  $$\text{RequiredXp}(L) = \lfloor 100 \times L^{1.4} \rfloor$$
  *(Lv.1 為 100 點熟練度)*
- **數值等級縮放**：
  - `getEffectiveDamageMultiplier()`：每升 1 級 **`+5%`** 傷害：
    $$\text{EffectiveMult} = \text{damageMultiplier} \times (1.0 + (\text{level} - 1) \times 0.05)$$
  - `getEffectiveHealAmount()`：每升 1 級 **`+8%`** 治療量：
    $$\text{EffectiveHeal} = \text{round}(\text{healAmount} \times (1.0 + (\text{level} - 1) \times 0.08))$$
  - `getEffectiveSanRestore()`：每升 1 級 **`+5%`** 理智回復量：
    $$\text{EffectiveSan} = \text{round}(\text{sanRestore} \times (1.0 + (\text{level} - 1) \times 0.05))$$
  - `getEffectiveCooldownMs()`：每升 1 級 **`-100ms`** 冷卻時間（下限保底 1000ms）：
    $$\text{EffectiveCd} = \max(1000, \text{cooldownMs} - (\text{level} - 1) \times 100)$$

---

## 🧬 三、四大淵源體系、十七專精樹與合體技架構 (Skill Trees & Synergies)

依據 Level 1《[CYBERPUNK_CULTIVATION_SPEC.md](../../1_requirements/CYBERPUNK_CULTIVATION_SPEC.md)》與 Level 2《[SKILL_SCHEMA.md](../data_schemas/SKILL_SCHEMA.md)》，全專案技能體系規劃為四大淵源與 17 棵專精樹。所有技能在前端預設**一律展示純正東方仙俠神通名稱**，並經由 `CognitiveProxyService` 隨特工晶片加載率動態解鎖底層文明原貌：

### 1. 四大淵源與十七專精樹矩陣
> 📖 **十七專精樹詳細規格目錄**：👉 [`docs/2_specifications/skills/README.md`](../skills/README.md)（內含 17 份獨立專精樹手冊與四階武技、道骨插槽、數值公式詳情）

| 淵源體系 (`originSchool`) | 專精技能樹 (`treeId`) | 核心戰術機制 | Tier 4 元嬰大絕招範例 |
| :--- | :--- | :--- | :--- |
| **東方修仙道統 (`CULTIVATOR`)** | 1. 庚金劍煞樹 (`tree_gold_sword`) | 飛劍破甲、金煞印記、雷法變異 | 【萬劍歸宗·太白凌虛大殺劫】 |
| | 2. 青帝靈木樹 (`tree_wood_life`) | 草木回血、神經真菌流毒、風法變異 | 【萬象回春·太陰神樹降世生】 |
| | 3. 玄冥真水樹 (`tree_water_frost`) | 極寒減速、冰盾吸收、陰煞變異 | 【翻江倒海·九幽天河覆地陣】 |
| | 4. 九天離火樹 (`tree_fire_flame`) | 赤陽灼燒、範圍爆破、熔岩變異 | 【九天焚世·金烏大日焚天劫】 |
| | 5. 后土鎮嶽樹 (`tree_earth_shield`) | 金剛霸體、反震擊退、純陽變異 | 【八荒六合·九州神山定鼎引】 |
| **古代工程師類 (`ENGINEER`)** | 6. 天引重力樹 (`tree_gravity`) | 引力定錨、黑洞聚怪、行動條壓制 | 【太古黑洞·奇異點湮滅核】 |
| | 7. 神機裂解樹 (`tree_fracture`) | 液壓破碎、高頻震盪、極限削韌破防 | 【地核共振·萬刃崩山破】 |
| | 8. 太古金剛樹 (`tree_forcefield`) | 蜂巢力場、單向偏折、重坦護盾 | 【絕對防衛·造物主力場堡壘】 |
| | 9. 地核熔爐樹 (`tree_plasma_core`) | 地熱等離子、持續熔岩地形 | 【地心超新星·行星熔爐過載爆發】 |
| **賽博高科技類 (`HIGH_TECH`)** | 10. 時序超頻樹 (`tree_time_overclock`) | 神經加速、子彈時間、指令無前搖 | 【子彈時間·天衍特工時停獵殺】 |
| | 11. 協議逆亂樹 (`tree_cyber_warfare`) | 100% 唱條打斷、中間人劫持 Buff | 【天道宕機·全頻段電磁因果癱瘓】 |
| | 12. 全息匿影樹 (`tree_optics_glitch`) | 頂點錯誤穿透、全息分身、暴擊伏擊 | 【虛實莫辨·九幽全息幻殺陣】 |
| | 13. 電磁動能樹 (`tree_kinetic_rails`) | 高斯磁軌、反物質狙殺、真實傷害 | 【天基軌道·上帝之杖天劫滅】 |
| **克蘇魯外神類 (`CTHULHU`)** | 14. 強殖幾丁質樹 (`tree_chitin_blade`) | 骨刃增生、裂魂引爆 (`DETONATOR`) | 【完全降臨·強殖阿修羅天魔相】 |
| | 15. 黑山羊血裔樹 (`tree_blood_siphon`) | 嗜血撕裂、氣血吞噬、全隊回哺 | 【萬子千孫·母神卵鞘血海臨】 |
| | 16. 知見污染樹 (`tree_cosmic_madness`)| 不可名狀低語、恐懼混亂、思維 DoT | 【不可名狀·盲目痴愚知見障】 |
| | 17. 虛無崩解樹 (`tree_null_void`) | 空指針斬殺、記憶體洩漏成長 DoT | 【歸墟終末·格式化天劫】 |

### 2. 小隊合體技與動態陣容終結技解算 (Combo & Finisher Resolver)
> 📖 **合體技與終結技詳細規格手冊**：👉 [`docs/2_specifications/skills/SYNERGY_AND_COMBOS.md`](../skills/SYNERGY_AND_COMBOS.md)（涵蓋 10 大反應引擎、16 雙星、8 三才、5 領域、15 終結技全窮盡矩陣）

後端 `ComboResolver` 採分層解算架構：
- **層級一：法則相生相剋反應引擎**：水火氣化爆裂 (2.0x)、雷水感電、火毒三昧燎原、太極混沌湮滅。
- **層級二：傳奇名牌合體技**：16 款 2 樹雙星合璧、8 款 3 樹三才殺招。
- **層級三：4 樹天道領域展開**：5 大天道領域，提供 8 秒全場規則級壓制。
- **層級四：5 人全隊動態終結技**：由 `ComboResolver` 與 `FormationFinisherClassifier` 動態分析 5 人出戰成分派發（含四大純粹極限型、萬象混元型、6 大雙曜交織型、4 大三才鼎立型共 15 大終結技）。

### 3. 主角萬象陣眼與道骨副修插槽
- **主角專屬萬象陣眼【現行已驗證】**：主角 `"originSchool": "UNIVERSAL"` 萬象陣眼判定已實裝生效，在陣容終結技分析中可自適應補足任意體系標籤。
- **道骨副修插槽與被動天賦【待深入討論／長線演進藍圖】**：道骨晶片插槽鑲嵌 UI 與 `PassiveTalentTemplate` 標註為後續擴充藍圖。
- **零成本洗點 (Zero-Cost Respec)**：在營地道場洗點零成本，鼓勵自由構建 Build。

---

## ⏳ 四、公共冷卻 (GCD) 與技能冷卻管制

1. **公共冷卻 (Global Cooldown, GCD)**：
   - 預設 1200ms（部分防禦反制招式標記 `OFF_GCD` 或 `INSTANT_DEFENSE` 豁免 GCD）。
2. **專屬冷卻 (Cooldown, CD)**：
   - 各技能冷卻時間隨等級縮放，每級縮減 100ms（下限 1000ms）。

---

## 🔗 五、代表類別與資料設定檔

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **大一統技能** | `com.example.htmlmud.party.UnifiedSkill` | 修煉境界、四大淵源與戰術機制大一統介面 |
| **隊員技能實體** | `com.example.htmlmud.party.PartyMemberSkill` | 隊員專屬技能、熟練度成長與動態縮放 |
| **技能規則檢定** | `com.example.htmlmud.progression.DrpgSkillRules` | 技能施展條件、真元消耗與冷卻檢定 |
| **連攜合體解算器** | `com.example.htmlmud.combat.ComboResolver` | 解算 2人雙星、3~4人跨界與 5人全隊動態終結技 |
| **終結技分類器** | `com.example.htmlmud.combat.FormationFinisherClassifier` | 動態分析隊伍陣容組成派發對應終結技 |
| **經驗成長服務** | `com.example.htmlmud.progression.XpProgressionService` | 經驗分配、境界提升與屬性點獎勵 |
| **十七專精樹手冊** | `docs/2_specifications/skills/README.md` | 十七棵獨立專精規格目錄與導覽總索引 |
| **合體與終結技手冊** | `docs/2_specifications/skills/SYNERGY_AND_COMBOS.md` | 10 大反應引擎、16 雙星、8 三才、5 領域與 15 終結技 |
| **資料設定真值源** | `src/main/resources/data/party/party_skills.json` | 17 專精樹全武技、被動道骨與合體技資料庫 |
| **資料契約規範** | `docs/2_specifications/data_schemas/SKILL_SCHEMA.md` | 四大淵源、Tier 1~4 與合體技 Schema |
