# ⚡ 後端 M5: 技能、境界與修煉模組深度手冊 (Skill & Progression Manual)

> **模組編號**：M5  
> **系統類型**：後端核心業務模組  
> **核心定位**：管理修仙境界突破、道基經驗成長、四大淵源十七專精技能樹、Tier 1~4 縱深（含元嬰大絕招）、道骨晶片插槽與多維小隊合體技解算演算法。

---

## 📈 一、境界突破與道基成長曲線 (Progression)

修仙之路遵循傳統道門境界階梯（`classes.json`）：
- **練氣期 (Qi Refinement)**：1 ~ 9 層。解鎖各樹 **Tier 1 (基礎循環武技)**。每次升級獲得 2 點自由道基屬性點（體質/真元/神識/根骨/身法）。
- **築基期 (Foundation Building)**：解鎖各樹 **Tier 2 (核心戰術招 / 打斷 / 破防削韌)** 與第二主動法術位，開放副修道骨插槽。
- **金丹期 (Golden Core)**：解鎖各樹 **Tier 3 (爆發殺招 / Detonator 引爆)** 與罡氣護體，解鎖五行/重力/特工領域。
- **元嬰期 (Nascent Soul)**：解鎖各樹 **Tier 4 (天道大絕招 / 全場領域 / 毀滅性終結)** 與全隊 5 人合體終結技。

### 升級公式與點數結算 (`XpProgressionService.java`)
- 升級所需經驗：$$\text{ExpRequired}(L) = \lfloor 100 \times L^{1.8} \rfloor$$
- 戰勝魔物時，隊長與存活隊員平均分配戰鬥經驗值。
- 當經驗槽滿載，觸發 `PlayerEvents.LevelUp` 事件，自動刷新氣血上限並通知前端彈出加點提示。

---

## ⚔️ 二、大一統技能模型與動態屬性縮放 (Unified Skill & Scaling Engine)

專案確立大一統技能模型 [`UnifiedSkill`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/party/UnifiedSkill.java) 與 [`PartyMemberSkill`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/party/PartyMemberSkill.java)：

### 1. 內聚合一的核心契約 (Unified Contract)
大一統模型同時封裝三大面向：
- **修煉境界維度**：道法等級（`level` 1~10）、熟練度累積（`xp` / `maxXp`）、境界倍率（`masteryMultiplier`）。
- **戰術機制維度**：傷害倍率（`damageMultiplier`）、冷卻時間（`cooldownMs`）、資源消耗（`costType` / `costValue`）、反制標籤（`INTERRUPT`, `POISE_BREAK`, `BARRIER`, `LIFE_LEECH`）、削韌（`poiseDamage`）。
- **連攜與合體維度**：連攜角色（`comboRole`: `NONE` / `PRIMER` / `DETONATOR`）、所屬淵源（`originSchool`）、專精樹（`treeId`）、階級（`tier`: 1~4）。

### 2. 動態屬性與境界縮放
- 戰鬥中呼叫 `skill.getEffectiveDamageMultiplier()` 時，自動結合隊員五維道基與修煉等級進行動態加成：
$$\text{ScaledDamage} = \text{BaseDamage} \times (1.0 + (\text{SkillLevel} - 1) \times 0.15) \times \text{MasteryMult}$$
- 物理武技偏向根骨（STR）縮放；道法仙術偏向真元與神識（SPI/PER）縮放；特工武技偏向身法（DEX）縮放。

---

## 🧬 三、四大淵源體系、十七專精樹與合體技架構 (Skill Trees & Synergies)

依據 Level 1《[CYBERPUNK_CULTIVATION_SPEC.md](../../1_requirements/CYBERPUNK_CULTIVATION_SPEC.md)》與 Level 2《[SKILL_SCHEMA.md](../data_schemas/SKILL_SCHEMA.md)》，全專案技能體系 1:1 規劃為四大淵源與 17 棵專精樹。所有技能在前端預設**一律展示純正東方仙俠神通名稱**，並經由 `CognitiveProxyService` 隨特工晶片加載率動態解鎖底層文明原貌：

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
- **層級一：法則相生相剋反應引擎 (Rule-based Reaction Engine)**：
  - 任何 2~4 樹相遇，自動比對元素與機制標籤（如水火蒸發 2.0x、火木擴散 DoT、引力聚怪、電磁穿甲、血肉木馬掠奪），確保任意小隊陣容皆能觸發連攜化學反應。
- **層級二：傳奇經典名牌合體技 (Signature Combos)**：
  - **2 樹雙星合璧 (16 大經典名招)**：如【太極水火·兩儀陰陽寂滅印】(水+火)、【星火燎原·青木赤霞焚天訣】(火+木)、【九霄紫電·天罡奔雷裂空斬】(金+火)、【乾坤碎嶽·太古黑洞八荒崩】(重力+神機)、【三千幻影·剎那因果無間刺】(時序+匿影)、【生化超導·破空磁煞幾丁刃】(幾丁質+電磁) 等。
  - **3 樹三才殺招 (8 大經典名招)**：如【萬毒天雨·碧落赤霞蒸仙煞】(水+火+木)、【天星地煞·地核隕星破界崩】(重力+土+熔爐)、【千星瞬影·萬劍超頻百步貫腦劫】(金+時序+電磁)、【九幽冥胎·母神血肉空指針噬魂煞】(幾丁質+血裔+虛無) 等。
- **層級三：4 樹天道領域展開 (Domain Expansions)**：
  - 四人合體技固定歸納為 **5 大天道領域**（純修仙【太乙混元·五行四象破天劍煞界】、純工程【造物神殿·行星地核定鼎陣列】、純高科【天衍神念·千星矩陣絕對零日網】、純外神【不可名狀·森之黑山羊深淵肉相界】、跨界混搭【四極降魔·誅仙滅魂混天伏魔陣】），提供 8 秒全場規則級壓制。
- **層級四：5 人全隊動態終結技（15 大體系形態全窮盡覆蓋）**：
  - 由 `ComboResolver` 動態分析 5 人出戰成分（不論 3+2/2+3/2+2+1 人數比例，純依隊伍包含之體系種類精準派發）：
    - **四大純粹極限型（單一體系 $\ge 4$ 人）**：純修仙【九天太乙誅仙劍陣】、純科技【千星神念天網劫】、純工程【太古神山崩天闕】、純外神【黑山羊母神血宴】。
    - **萬象混元型（4 大體系均衡各 1 人 ＋ 主角萬象）**：【大衍混元·諸天萬象開天闢地神劫】（全維無死角削韌破防 + 元素擴散 + 打斷 + 引爆 + 全隊回滿）。
    - **6 大雙曜交織型（隊伍僅包含 2 種體系）**：仙魔【太極血海·逆亂生死神魔印】、神機【天基軌道·萬鈞乾坤神機炮】、仙道賽博【九天星網·神念超頻萬劍矩陣】、天工仙道【天工造化·九嶽神山崩天闕】、生化黑客【生化天羅·黑煞奈米噬魂木馬】、血肉泰坦【深淵巨擘·太古血肉魔導重機】。
    - **4 大三才鼎立型（隊伍包含 3 種體系）**：仙魔科【偷天換日·生化天道逆亂真訣】、仙魔工【冥河重嶽·混沌太虛開山印】、仙科工【天衍算籌·九天神雷星際裂變陣】、魔科工【萬機歸淵·黑山羊奇異點格式化】。
- **長線低優先連攜寶庫**：由 `Task M5-7` 追蹤額外經典拍檔（如【地湧金蓮】、【水月太陰飛劍】、【萬骨機變爪】、【幽靈特工步】等），作為後續版本平滑擴充池。

### 3. 主角萬象陣眼與道骨副修插槽 (Socket System)
- **主角專屬【萬象核心插槽 (Core Socket)】**：主角在營地道場可自由更換核心晶片，在陣法判定中代表任意體系，杜絕玩家因隊友短缺而無法觸發心儀大絕招。
- **隊友【道骨副修插槽 (Sub-School Socket)】**：隊友突破大境界後解鎖道骨插槽，鑲嵌其他體系之道骨，在陣法判定中額外獲得該體系標籤，提供極致的 Build 自由度。
- **零成本洗點 (Zero-Cost Respec)**：在營地雜役演武道場洗點完全零成本，鼓勵玩家不斷嘗試新技能與合體技組合。

---

## ⏳ 四、公共冷卻 (GCD) 與技能冷卻管制

1. **公共冷卻 (Global Cooldown, GCD)**：
   - 預設 1000ms ~ 1500ms（時序超頻流派可將其壓縮至 0~500ms）。
   - 施放大部分攻擊技能後，觸發小隊隊員的短暫停頓，防範按鍵連發作弊。
2. **專屬冷卻 (Cooldown, CD)**：
   - Tier 1 循環招：3 ~ 5 秒；
   - Tier 2 戰術招：6 ~ 10 秒；
   - Tier 3 爆發招：12 ~ 18 秒；
   - Tier 4 元嬰大絕招：25 ~ 40 秒，需抓準敵怪破防與前搖時機釋放。

---

## 🔗 五、代表類別與資料設定檔

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **大一統技能** | `com.example.htmlmud.party.UnifiedSkill` | 修煉境界、四大淵源與戰術機制大一統介面 |
| **隊員技能實體** | `com.example.htmlmud.party.PartyMemberSkill` | 隊員專屬技能、熟練度成長與動態縮放 |
| **連攜合體解算器** | `com.example.htmlmud.combat.ComboResolver` | 解算 2人雙星、3~4人跨界與 5人全隊動態終結技 |
| **道骨晶片插槽** | `com.example.htmlmud.party.SocketService` | 管理主角萬象核心插槽與隊友副修道骨鑲嵌 |
| **經驗成長服務** | `com.example.htmlmud.progression.XpProgressionService` | 經驗分配、境界提升與屬性點獎勵 |
| **十七專精樹手冊** | `docs/2_specifications/skills/README.md` | 十七棵獨立專精規格目錄與導覽總索引 |
| **合體與終結技手冊** | `docs/2_specifications/skills/SYNERGY_AND_COMBOS.md` | 10 大反應引擎、16 雙星、8 三才、5 領域與 15 終結技 |
| **資料設定真值源** | `src/main/resources/data/party/party_skills.json` | 17 專精樹全武技、被動道骨與合體技資料庫 |
| **資料契約規範** | `docs/2_specifications/data_schemas/SKILL_SCHEMA.md` | 四大淵源、Tier 1~4 與合體技 Schema |

