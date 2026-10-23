# ⚡ 後端 M5: 技能、境界與修煉模組深度手冊 (Skill & Progression Manual)

> **模組編號**：M5  
> **系統類型**：後端核心業務模組  
> **核心定位**：管理修仙境界突破、道基經驗成長、技能學習與動態屬性縮放演算法（Skill Scaling）。

---

## 📈 一、境界突破與道基成長曲線 (Progression)

修仙之路遵循傳統道門境界階梯（`classes.json`）：
- **練氣期 (Qi Refinement)**：1 ~ 9 層。每次升級獲得 2 點自由道基屬性點（可自由分配至體質/真元/神識/根骨/身法）。
- **築基期 (Foundation Building)**：解鎖第二主動法術位，解鎖真元護體。
- **金丹期 (Golden Core)**：解鎖丹田法力海，解鎖罡氣護體與五行領域。

### 升級公式與點數結算 (`XpProgressionService.java`)
- 升級所需經驗：$$\text{ExpRequired}(L) = \lfloor 100 \times L^{1.8} \rfloor$$
- 戰勝魔物時，隊長與存活隊員平均分配戰鬥經驗值。
- 當經驗槽滿載，觸發 `PlayerEvents.LevelUp` 事件，自動刷新氣血上限並通知前端彈出加點提示。

---

## ⚔️ 二、大一統技能模型與境界動態縮放 (Unified Skill & Scaling Engine)

在遊戲中，一本「太陰玄冰訣」在練氣修士與金丹老怪手中釋放，威力截然不同。

專案正式確立**大一統技能模型** [`UnifiedSkill`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/party/model/UnifiedSkill.java) 與 [`PartyMemberSkill`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/party/model/PartyMemberSkill.java)，隊員直接持有技能，由系統進行無膠水同步：

### 1. 內聚合一的核心契約 (Unified Contract)
大一統模型同時封裝三大面向：
- **修煉境界維度**：道法等級（`level` 1~10）、熟練度累積（`xp` / `maxXp`）、境界倍率（`masteryMultiplier`）。
- **戰術機制維度**：傷害倍率（`damageMultiplier`）、冷卻時間（`cooldownMs`）、資源消耗（`costType` / `costValue`）、反制標籤（`STUN`, `SILENCE`, `TAUNT`, `CLEANSE`）、削韌（`poiseDamage`）。
- **動態播報敘事**：招式文本範本（`moves`）。

### 2. 動態屬性與境界縮放
- 戰鬥中呼叫 `skill.getEffectiveDamageMultiplier()` 時，自動結合隊員五維道基與修煉等級進行動態加成：
$$\text{ScaledDamage} = \text{BaseDamage} \times (1.0 + (\text{SkillLevel} - 1) \times 0.15) \times \text{MasteryMult}$$
- 物理武技偏向根骨（STR）縮放；道法仙術偏向真元與神識（SPI/PER）縮放。
- 戰鬥大捷後，熟練度直接原地記入 `skill.addXp(amount)`，零跨層轉錄。

---

## 🧬 三、賽博特工四大流派與 16 大標準神通矩陣 (Cyber Cultivation Trees)

依據 Level 1《[CYBERPUNK_CULTIVATION_SPEC.md](../../1_requirements/CYBERPUNK_CULTIVATION_SPEC.md)》與 Level 2《[SKILL_SCHEMA.md](../data_schemas/SKILL_SCHEMA.md)》，全專案技能體系 1:1 映射為四大賽博修仙流派與 16 項神通契約。所有技能在前端預設**一律展示純正東方仙俠神通名稱**，並經由 `CognitiveProxyService` 隨特工晶片加載率動態解鎖賽博真相透鏡：

### 16 大標準神通清單 (對齊 L1 仙俠主要定名)

| 技能代碼 (`id`) | 仙俠主稱謂 (`name`) | 流派 (`school`) | 階級 | 連攜角色 (`comboRole`) | 破防削韌 | 核心戰術機制 |
| :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| `skill_neural_overclock_step` | **【燃魂溯光·剎那千秋步】** | `CYBER_HACK` | 1 | `PRIMER` | 25 | 突觸超頻，施加裂魂印記，自身極速 |
| `skill_causal_hijack` | **【偷天換日·轉煞移因訣】** | `CYBER_HACK` | 2 | `NONE` (反制) | 30 | 劫持施法，命中 100% 打斷蓄力 |
| `skill_packet_storm` | **【萬念俱雜·搜魂奪舍刺】** | `CYBER_HACK` | 2 | `PRIMER` | 20 | 高頻突刺，疊加感電傳導 DoT |
| `skill_zero_day_strike` | **【九冥無間·破空斬魄芒】** | `CYBER_HACK` | 3 | `DETONATOR` | 45 | 致命破綻終結打擊，引爆 1.8x 爆裂 |
| `skill_bio_keratin_blade` | **【化煞成罡·玄甲骨刃斬】** | `GENE_BIO` | 1 | `DETONATOR` | 45 | 強殖幾丁質重劈，引爆 1.5x 衝擊波 |
| `skill_blood_siphon` | **【血影遁真·九幽嗜元魔經】** | `GENE_BIO` | 2 | `PRIMER` | 20 | 造成撕裂 DoT 並吸取 30% 氣血 |
| `skill_flesh_aberration` | **【魔神降世·八臂修羅法身】** | `GENE_BIO` | 3 | `NONE` (霸體) | 0 | 進入 10s 鋼骨霸體，免疫控制，攻速 +40% |
| `skill_hive_swarm` | **【萬毒噬心·腐仙百足針】** | `GENE_BIO` | 2 | `PRIMER` | 15 | 施加幽冥劇毒 DoT，降低目標防禦 20% |
| `skill_gravity_anchor` | **【不動明王·九嶽鎮嶽印】** | `MECHA_CULT` | 1 | `NONE` (護坦) | 35 | 嘲諷全體敵怪，自身罡氣上限 +50% |
| `skill_matter_crush` | **【裂地崩山·混元覆海錘】** | `MECHA_CULT` | 2 | `DETONATOR` | 55 | 巨力破防打擊，目標破防狀態下傷害翻倍 |
| `skill_optical_camouflage` | **【鏡花水月·無相匿息咒】** | `MECHA_CULT` | 2 | `PRIMER` | 10 | 進入隱匿狀態，下一次打擊暴擊率 100% |
| `skill_quantum_mirror` | **【兩儀逆轉·太虛乾坤罡】** | `MECHA_CULT` | 3 | `NONE` (反震) | 25 | 生成反傷力場，反彈 50% 物理傷害 |
| `skill_null_pointer_collapse` | **【萬法皆空·大乘湮滅指】** | `VOID_PSI` | 3 | `DETONATOR` | 60 | 引爆目標全體 DoT，造成 2.5x 混沌湮滅 |
| `skill_overcharge_arc` | **【九霄紫電·雷霆震煞術】** | `VOID_PSI` | 1 | `PRIMER` | 30 | 發射高壓電弧，同排敵人間連鎖跳躍 |
| `skill_memory_wipe` | **【忘川秋水·醉夢前塵曲】** | `VOID_PSI` | 2 | `NONE` (控場) | 20 | 清空目標蓄力條並陷入 2.5s 眩暈 |
| `skill_system_reboot` | **【金丹涅槃·枯木逢春訣】** | `VOID_PSI` | 3 | `NONE` (急救) | 0 | 全隊恢復 50% HP 並重置隊友技能冷卻 |

### 靈識認知轉譯代理 (Cognitive Lore Proxy for Skills)
- **初始狀態 (< 30%)**：技能面板、戰鬥日誌與戰術 HUD 100% 呈現仙俠神通名，無任何科技或賽博字眼。
- **微光異象 (30% ~ 70%)**：技能懸停資訊卡偶現藍色 Glitch 微光，標註「神經超頻」、「引子/引爆」連攜標籤。
- **科技覺醒 (> 70%)**：解鎖副標題，揭露【燃魂溯光·剎那千秋步】實為千星科技「神經突觸超頻協議」之底層真相。

### 特工晶片插槽與超頻技能裝配 (Chip Socketing)
- **主動技能槽位**：每位隊員預設 4 個戰術法訣槽（`activeSkills[4]`）。
- **特工晶片插槽**：主角前世特工晶片提供 2 個額外「超頻插槽（Overclock Sockets）」，隨晶片加載進度（`agentChipLoad` 12% ➔ 100%）逐步解鎖。
- **技能重置 (Zero-Cost Respec)**：在營地道場設施中洗點與更換技能完全零成本，鼓勵玩家自由測試技能 Synergy 構築。

---

## ⏳ 四、公共冷卻 (GCD) 與技能冷卻管制

1. **公共冷卻 (Global Cooldown, GCD)**：
   - 預設 1000ms ~ 1500ms。
   - 施放大部分攻擊技能後，觸發小隊隊員的短暫停頓，防範按鍵連發與巨集作弊。
2. **專屬冷卻 (Cooldown, CD)**：
   - 高階仙術（如「萬法皆空·大乘湮滅指」）具備 8 ~ 15 秒獨立冷卻，需在戰鬥中看準破防時機釋放。

---

## 🔗 五、代表類別與資料設定檔

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **大一統技能** | `com.example.htmlmud.domain.party.model.UnifiedSkill` | 修煉境界與戰術機制大一統介面 |
| **隊員技能實體** | `com.example.htmlmud.domain.party.model.PartyMemberSkill` | 隊員專屬技能、熟練度成長與動態縮放 |
| **經驗成長** | `com.example.htmlmud.domain.service.XpProgressionService` | 經驗分配、等級提升與屬性點獎勵 |
| **真元消耗** | `com.example.htmlmud.domain.service.SkillCostService` | 施法檢定真元消耗與冷卻時間判定 |
| **公式解算** | `com.example.htmlmud.domain.service.FormulaEvaluator` | 數值公式字串動態解析 |
| **資料設定** | `src/main/resources/data/classes/classes.json` | 境界階梯、各境界基礎屬性與成長率 |
| **資料真值源** | `src/main/resources/data/party/party_skills.json` | 16 大標準賽博戰技資料庫（仙俠主稱謂） |

---

## 🚀 六、未來架構演進與現代化擴充規劃 (Future Roadmap & Scalability)

為使技能與境界修煉體系具備現代 RPG 的 Build 構建自由度與修仙特色，確立以下四大演進方向：

### 1. 🎯 現代遊戲機制深化 (Modern Gameplay & Depth)
- **周天星盤天賦樹 (Constellation Talent System)**：
  玩家在練氣 ➔ 築基 ➔ 金丹每次大境界突破時，解鎖「周天二十八宿」天賦分支。玩家可自由點亮「純陽烈火道」、「玄陰極寒道」或「肉身成聖道」，為招式賦予質變特性（例如讓普攻附帶 20% 五行穿透，或使破防持續時間翻倍）。
- **不可名狀外道邪術 (Forbidden Occult Arts)**：
  引入克蘇魯修仙特色之禁忌邪功。此類功法威力毀天滅地，但需以「永久燃燒壽元」或「當前戰鬥道心上限 -10」為施法代價，提供高風險、極限輸出的狂戰士修仙流派。

### 2. 🛡️ 穩定性與狀態防禦 (Robustness & Resilience)
- **公式解析沙箱化與 AST 預編譯 (Compiled Formula Engine)**：
  目前 `FormulaEvaluator` 採用動態正則字串運算。未來升級為抽象語法樹（AST）預編譯機制（或封裝為輕量 Bytecode），在開機時完成靜態校驗，運行時直接呼叫原生委派，杜絕運行時公式異常與型別轉換崩潰。
- **冷卻時間溢出保護**：在身法提供的高額冷卻縮減（Cooldown Reduction, CDR）下，嚴格設置 75% 縮減軟上限（Soft Cap），杜絕 GCD 縮減至 0 導致的無窮連發循環。

### 3. ⚡ 效能與記憶體最佳化 (Performance & Concurrency)
- **時間戳無輪詢冷卻演算法 (Timestamp-based Cooldown)**：
  摒棄過去每 500ms Tick 遍歷所有隊員技能進行 `cd -= 500` 的傳統輪詢做法，全面改為 `readyAtTimestamp = now() + cdMs`。判定時僅需一次長整數比較（`now >= readyAtTimestamp`），技能冷卻判定時間複雜度降為 $O(1)$，Tick 耗時降至微秒級。

### 4. 🔌 可擴充性與模組化 (Extensibility & Modding)
- **心法招式銘刻與自創神通 (Skill Synthesis & Rune Socketing)**：
  支援透過 JSON 擴充技能的插槽（Socket）與銘刻符印。玩家可將「烈焰符印」鑲嵌於「飛劍術」，自動產出「離火烈陽劍」，完全資料驅動，為 Mod 社群提供無窮創作空間。
