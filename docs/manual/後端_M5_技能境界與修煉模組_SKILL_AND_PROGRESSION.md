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

## ⚔️ 二、技能動態屬性縮放 (Skill Scaling Engine)

在遊戲中，一本「太陰玄冰訣」在練氣修士與金丹老怪手中釋放，威力截然不同。

專案透過 [`SkillBridgeService.scaleSkill()`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/service/SkillBridgeService.java) 實作高階動態縮放：

### 1. 完整保留原始元資料 (Metadata Preservation)
動態縮放時，嚴格保留原技能的全部核心戰鬥契約：
- **`poiseDamage`**：削韌值（不因縮放遺失）。
- **`damageType`**：五行傷害類型（如 `FIRE`, `WATER`, `CORRUPT`）。
- **`buffConfig`** / **`tags`**：狀態賦予與技能標籤（如 `CLEANSE`, `INTERRUPT`）。
- **`gcdMs`** / **`castTimeMs`**：吟唱與公共冷卻。

### 2. 縮放演算法公式
$$\text{ScaledDamage} = \text{BaseDamage} + (\text{Caster.SPI} \times \text{SpiRatio}) + (\text{Caster.STR} \times \text{StrRatio})$$
- 物理武技偏向根骨（STR）縮放。
- 道法仙術偏向真元與神識（SPI/PER）縮放。

---

## ⏳ 三、公共冷卻 (GCD) 與技能冷卻管制

1. **公共冷卻 (Global Cooldown, GCD)**：
   - 預設 1000ms ~ 1500ms。
   - 施放大部分攻擊技能後，觸發小隊隊員的短暫停頓，防範按鍵連發與巨集作弊。
2. **專屬冷卻 (Cooldown, CD)**：
   - 高階仙術（如「九天玄煞斬」）具備 8 ~ 15 秒獨立冷卻，需在戰鬥中看準破防時機釋放。

---

## 🔗 四、代表類別與資料設定檔

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **技能橋接** | `com.example.htmlmud.domain.service.SkillBridgeService` | 技能動態五維縮放與元契約深拷貝 |
| **經驗成長** | `com.example.htmlmud.domain.service.XpProgressionService` | 經驗分配、等級提升與屬性點獎勵 |
| **真元消耗** | `com.example.htmlmud.domain.service.SkillCostService` | 施法檢定真元消耗與冷卻時間判定 |
| **公式解算** | `com.example.htmlmud.domain.service.FormulaEvaluator` | 數值公式字串動態解析 |
| **資料設定** | `src/main/resources/data/classes/classes.json` | 境界階梯、各境界基礎屬性與成長率 |
| **資料設定** | `src/main/resources/data/skills.json` | 通用仙法與心法基礎設定 |

---

## 🚀 五、未來架構演進與現代化擴充規劃 (Future Roadmap & Scalability)

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
  摒棄過去每 200ms Tick 遍歷所有隊員技能進行 `cd -= 200` 的傳統輪詢做法，全面改為 `readyAtTimestamp = now() + cdMs`。判定時僅需一次長整數比較（`now >= readyAtTimestamp`），技能冷卻判定時間複雜度降為 $O(1)$，Tick 耗時降至微秒級。

### 4. 🔌 可擴充性與模組化 (Extensibility & Modding)
- **心法招式銘刻與自創神通 (Skill Synthesis & Rune Socketing)**：
  支援透過 JSON 擴充技能的插槽（Socket）與銘刻符印。玩家可將「烈焰符印」鑲嵌於「飛劍術」，自動產出「離火烈陽劍」，完全資料驅動，為 Mod 社群提供無窮創作空間。
