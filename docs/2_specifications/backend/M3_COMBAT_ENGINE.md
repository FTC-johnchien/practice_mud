# ⚔️ 後端 M3: 核心戰鬥引擎模組深度手冊 (Combat Engine Manual)

> **模組編號**：M3  
> **系統類型**：後端核心業務模組  
> **核心定位**：管理半即時戰鬥時間線（500ms Tick 虛擬執行緒迴圈）、一元一次擲骰圓桌判定公式、罡氣削韌與破防（Poise Break）、戰鬥概念四層分離字典（DamageType / 九大靈氣 / 狀態標籤 / BuffCategory）、五行逆流回血（順成人逆成仙）、智慧仇恨索敵與打斷反制狀態機。

---

## ⏱️ 一、半即時戰鬥迴圈 (Semi-Realtime ATB Loop)

遊戲跳脫傳統純回合制的僵化等待，採用類似經典 ATB（Active Time Battle）的**半即時時間線機制**：

```mermaid
sequenceDiagram
    autonumber
    participant Loop as DrpgCombatLoop (500ms Tick 迴圈)
    participant Hero as 玩家小隊成員
    participant Enemy as 怪物敵方
    participant Rules as CombatRules (純領域規則)
    participant UI as 前端 BattlePanel

    Loop->>Hero: 推進 GCD 與技能冷卻時間
    Loop->>Enemy: 推進攻擊間隔冷卻 (Action Interval)
    
    alt 怪物冷卻就緒 (Ready to Attack)
        Loop->>Enemy: 智慧索敵 (DrpgEnemyTacticsService)
        Note over Enemy: 依前衛站位加成、嘲諷係數與陣法仇恨選擇目標
        Loop->>Rules: 執行圓桌判定 executeCombatTable()
        Rules-->>Loop: 回傳判定結果 (命中 / 閃避 / 招架 / 暴擊)
        Loop->>Rules: resolveResistance() 折減傷害與五行逆流回血
        Loop->>Hero: 扣減氣血 HP 與精力 / 罡氣 Poise
        Loop->>UI: 廣播戰鬥動畫與飄字特效 (DrpgStateDto)
    end

    alt 玩家點擊施放技能
        Hero->>Loop: 派發技能指令
        Note over Hero: 檢定真元消耗與 GCD (觸發公共冷卻)
        Loop->>Rules: 解算技能削韌、Buff 施加與範圍結算
    end
```

- **Tick 頻率**：後端戰鬥虛擬執行緒以每 500ms 一次迴圈（`Thread.sleep(500)`）推進戰鬥時間線與狀態倒數。
- **身法速度加成**：角色身法（Agility）越高，攻擊間隔與技能冷卻縮減越快。

---

## 🎲 二、一元一次擲骰圓桌判定 (One-Roll Combat Table)

傳統 RPG 常常採用多次骰算（先骰是否命中 ➔ 再骰是否閃避 ➔ 再骰是否招架 ➔ 再骰是否暴擊），容易造成極端連鎖或命中膨脹。本專案採用暴雪/魔獸世界經典的 **一元一次擲骰圓桌演算法 (One-Roll Table)**。

### 1. 圓桌扇區切分原理
防禦者的全部防禦能力被攤平在 `[0.0, 1.0]` 的連續區間上，只需產生一次隨機浮點數 `roll = ThreadLocalRandom.current().nextDouble()`，看落入哪個扇區：

```
[0.0 ──────── 未命中 Miss ──────── Dodge 閃避 ──────── Parry 招架 ──────── Block 格擋 ──────── 命中 Hit ──────── 1.0]
```

### 2. 各扇區精確機率公式（摘自 `CombatRules.executeCombatTable`）
1. **未命中 (Miss Chance)**：
   $$\text{Miss} = \text{clamp}(0.02, 0.25, 0.05 + \max(0, \text{Def.DEX} - \text{Atk.DEX}) \times 0.005)$$
2. **身法閃避 (Dodge Chance)**：
   $$\text{Dodge} = \text{clamp}(0.05, 0.75, \text{BaseDodge} + \text{Def.DEX} \times 0.008 - \text{Atk.DEX} \times 0.003)$$
   *(若防禦者處於破防狀態 Poise Broken，閃避機率強制為 0)*
3. **招架偏斜 (Parry Chance)**：
   $$\text{Parry} = \text{clamp}(0.05, 0.60, \text{BaseParry} + \text{Def.STR} \times 0.004 + \text{Def.DEX} \times 0.004)$$
   *(招架成功可減免 50% 物理傷害，並有機會觸發反擊 Riposte)*
4. **盾牌格擋 (Block Chance)**：
   - 若防禦者配備盾牌：
     $$\text{Block} = \text{clamp}(0.10, 0.80, 0.20 + \text{Def.CON} \times 0.006)$$
   - 格擋成功減免護甲固定值，剩餘傷害再穿透。
5. **暴擊 (Critical Strike)**：
   - 命中成功後，根據攻擊者神識（Spirit）與武器暴擊率骰算，造成 $1.5\sim 2.0\times$ 致命傷害。

---

## 💥 三、罡氣削韌與破防機制 (Poise & Break System)

為了體現魂系與動作 RPG 的策略打擊感，生靈除了生命值（HP）外，皆具備 **罡氣韌性條 (Poise / Stamina)**：

1. **韌性扣減**：
   - 技能在 `party_skills.json` 中配置 `poiseDamage`（削韌值）。例如巨錘「碎嶽擊」具備高達 40 點削韌。
   - 敵人每次遭受重擊，韌性條即時減少。
2. **破防虛弱狀態 (Poise Broken / Vulnerable)**：
   - 當韌性歸零時，目標觸發 **「罡氣崩散 · 破防虛弱」**！
   - **破防懲罰**：
     - 目標在 3~5 秒內無法閃避與招架（圓桌閃避率歸零）。
     - 承受之所有傷害提升 30% ~ 50%。
     - 中斷當前蓄力施法。
3. **韌性恢復**：
   - 虛弱時間結束後，目標罡氣重新充能回滿，進入下一輪對抗。

---

## 🌊 四、戰鬥概念四層分離字典與五行逆流回血 (Combat Canonical Taxonomy)

為解決戰鬥概念在各模組手冊間的語意混淆，專案正式確立**「戰鬥四層字典架構」**，嚴格劃分傷害類型、元素家族、狀態標籤與底層 Buff 分類之職責邊界：

```
┌────────────────────────────────────────────────────────────────────────┐
│                    戰鬥概念四層分離字典架構                             │
├────────────────────┬───────────────────────────────────────────────────┤
│ 1. 底層傷害類型     │ DamageType 枚舉 (20 種繼承樹：物理派生/法術派生/特殊)│
├────────────────────┼───────────────────────────────────────────────────┤
│ 2. 天道元素家族     │ 九大靈氣家族 (五行天道/乾坤氣象/陰陽造化，對應抗性與逆流) │
├────────────────────┼───────────────────────────────────────────────────┤
│ 3. 狀態效果標籤     │ Status / Skill Tags (STUN/SILENCE/SEAL/INTERRUPT 矩陣)│
├────────────────────┼───────────────────────────────────────────────────┤
│ 4. 底層 Buff 分類  │ BuffCategory (SHIELD/HOT/DOT/STAT_MODIFIER/CONTROL)   │
└────────────────────┴───────────────────────────────────────────────────┘
```

### 1. 第一層：底層傷害類型 [`DamageType`](../../../src/main/java/com/example/htmlmud/domain/model/enums/DamageType.java)（20 種繼承樹）
代碼中以單一繼承樹組織，支援 `type.is(parent)` 多態檢定：
- **`PHYSICAL` (物理根類別)**：
  - `SLASH`（揮砍）：刀劍類鋒刃斬擊。
  - `PIERCE`（穿刺）：槍矛箭矢點狀破甲。
  - `BLUNT`（鈍擊）：戰錘巨棒重型打擊。
  - `METAL`（金石）：金屬銳器實體衝擊。
- **`MAGIC` (法術根類別)**：
  - `FIRE`（烈火）：赤煞焚燒。
  - `ICE`（冰霜）：玄冥極寒。
  - `WATER`（坎水）：巨浪激流。
  - `LIGHTNING`（神雷）：九霄天雷。
  - `WIND`（神風）：天罡烈風。
  - `EARTH`（厚土）：大地重壓。
  - `WOOD`（草木）：青囊生發。
  - `POISON`（毒素）：陰毒瘴氣。
  - `HOLY`（神聖）/ `LIGHT`（天光）：純陽正道、辟邪驅煞。
  - `DARK`（幽冥）/ `CORRUPT`（陰煞/混沌）：古神侵蝕、魔道濁氣。
- **特殊類別**：
  - `SONIC`（音波）：無視常規物防之震盪傷害。
  - `TRUE`（真實傷害）：天道不可抗拒之真實扣血，不計護甲與抗性折減。

### 2. 第二層：天道元素家族 (九大靈氣家族)
修仙世界講究五行生剋與道法抗衡，用於抗性面板、天道靈氣克制與「順成人逆成仙」之回血維度：
- **五行天道靈氣**：金 (`METAL`)、木 (`WOOD`/`POISON`)、水 (`WATER`/`ICE`)、火 (`FIRE`)、土 (`EARTH`)。
- **乾坤氣象靈氣**：風 (`WIND`)、雷 (`LIGHTNING`)。
- **陰陽造化靈氣**：陰煞 (`DARK`/`CORRUPT`)、純陽 (`HOLY`/`LIGHT`)。

### 3. 第三層：狀態機制標籤 (Status & Skill Mechanism Tags)
用於技能反制檢定、Boss 施法蓄力打斷與戰術決策：
- **控場反制標籤**：`STUN`（眩暈/硬直）、`SILENCE`（沉默/封咒）、`SEAL`（封印/鎮魔）、`CONTROL`（通用受控）、`INTERRUPT`（通用反制打斷）。
- **戰術行為標籤**：`ATTACK`（普攻）、`SPELL`（法術）、`HEAL`（治療）、`DEFENSE`（防護）、`AOE`（群體）、`TAUNT`（嘲諷）、`CLEANSE`（淨化）、`DISPEL`（驅散）、`PARRY`（偏斜招架）。

### 4. 第四層：底層 Buff 分類 [`BuffCategory`](../../../src/main/java/com/example/htmlmud/domain/model/enums/BuffCategory.java)
引擎底層核心運算的分流憑據，規範生命週期與結算邏輯：
- `SHIELD`：傷害吸收護盾（耗盡即破碎提前失效）。
- `HOT`：週期性治療 (Heal over Time)。
- `DOT`：週期性傷害 (Damage over Time)。
- `STAT_MODIFIER`：五維屬性或戰鬥數值加成/減益。
- `CONTROL`：強制控場限制（受 `Buffable.isControlled()` / `isSilenced()` 全入口門控）。

### 5. 傷害抗性折減與「順成人，逆成仙」逆流回血契約
實作於 [`CombatRules.resolveResistance`](../../../src/main/java/com/example/htmlmud/domain/combat/CombatRules.java)：

```java
public static ResistanceResult resolveResistance(int rawDamage, DamageType type, double resistance)
```

1. **物理保底穿透原則**：
   - 物理系傷害（`PHYSICAL` 及其派生 `SLASH/PIERCE/BLUNT/METAL`）不走元素逆流回血。
   - 為防止極限堆防禦導致「絕對無敵死鎖」，物理抗性最高折減至 90%（**保底 10% 穿透**）：
     $$\text{FinalDamage} = \max(1, \text{RawDamage} \times \max(0.1, 1.0 - \text{Resistance}))$$
2. **五行法術逆流回血原則 (Absorption & Healing)**：
   - 當怪物或修士修煉特定真功，使某屬性抗性大於 100%（$\text{Resistance} > 1.0$，如火靈真身火抗 125%）：
   - **承受傷害歸零**，超額部分化為甘霖，**逆流轉化為氣血回復**！
     $$\text{HealAmount} = \text{round}(\text{RawDamage} \times (\text{Resistance} - 1.0))$$
   - 完美還原修仙小說中「用火法轟擊火精靈反而為其療傷」的神妙機制。

### 6. 全系統時間心跳層級排程字典 (Timing & Heartbeat Hierarchy)
系統遵循清晰的階層化排程架構，各層頻率互不混淆：
1. **底層世界定時器 (`WorldPulse.pulse`)**：`@Scheduled(fixedRate = 100)` (100ms 輪詢)，作為伺服端全局最小心跳單位。
2. **戰鬥虛擬執行緒迴圈 (`DrpgCombatLoop`)**：獨立執行緒 500ms 迴圈（`Thread.sleep(500)`），推進行動間隔冷卻與意圖蓄力。
3. **狀態效果推進 (`BuffSettlementService` / `ActiveBuff`)**：基準 1 Tick = 500ms（戰鬥中由 CombatLoop 驅動；常態由 WorldPulse 推進），推進 DoT/HoT 與倒數。
4. **世界生靈巡邏與刷新**：3,000ms（由 `WorldPulse` 計數器計數 30 Ticks 觸發），負責怪物刷新與大世界巡邏。

---

## 🧪 五、Buff 狀態機生命週期與整合

> 📖 **權威專屬手冊**：  
> 生靈之狀態效果、Buff/Debuff 狀態機、WoW 式刷新與層數堆疊、多重護盾吸收優先序、控場門控、罡氣破防條（Poise Break）攻防博弈、九大天道靈氣五行抗性與逆流回血、以及屬性圖示聚合 HUD 契約，已收斂獨立為本戰鬥引擎之專屬子模組手冊：  
> 👉 **請詳閱 [`M3_1_BUFF_AND_STATUS_EFFECTS.md`](M3_1_BUFF_AND_STATUS_EFFECTS.md)**。

本戰鬥引擎（M3）透過 `BuffSettlementService` 與 `Buffable` 介面與 M3-1 子模組緊密整合：
1. **時間線驅動**：`DrpgCombatLoop` 於每輪 Tick 呼叫 `BuffSettlementService.processTicks(entity)` 推進狀態倒數與結算持續傷害/治療 (DoT/HoT)。
2. **控場門控檢驗**：在發起普通攻擊或引導施法前，透過 `buffable.isControlled()` 與 `buffable.isSilenced()` 即時判定是否中斷或跳過行動。
3. **傷害吸收與抗性折抵**：生靈遭受傷害扣血前，先由 `BuffSettlementService.absorbDamage()` 依最短時效優先抵扣護盾，穿透部分再交由 `CombatRules.resolveResistance()` 折算抗性與逆流回血。

---

## ⚡ 六、敵方蓄力前搖與打斷反制狀態機 (Combat FSM & Interrupt System)

為實現現代動作 RPG 的「見招拆招」博弈體驗，怪物行動不僅具備攻擊間隔（Action Interval），在釋放特技或強力招式時更引入 **蓄力施法階段（CASTING Phase）** 與 **即時打斷反制（Interrupt System）**：

### 1. 怪物施法階段狀態機（`CombatFsmService.startEnemyCast`）
- 當怪物普通攻擊或技能觸發蓄力前搖時（由 `MobTemplate.castTimeMs` 或招式 `Timing.castTimeMs` 設定）：
  - 怪物進入 `isCasting = true` 狀態，初始化 `castDurationMs` 與 `castRemainingMs`。
  - 當前意圖 `currentAction`、進度百分比 `actionProgressPercent` 與可打斷標識 `interruptible` 同步封裝至 `BattleEnemyViewDto` 傳輸給前端進行青藍/猩紅蓄力條渲染。

### 2. 即時受創打斷與反制判定（`CombatFsmService.resolveSkillInterrupt`）
- **反制觸發條件**：
  1. 敵方當前處於蓄力施法狀態（`isCasting() == true`）。
  2. 敵方該招式具備可打斷屬性（`isInterruptible() == true`）。
  3. 玩家小隊施放的招式標籤帶有打斷/控場特性（`STUN`、`SILENCE`、`CONTROL`、`SEAL`）。
- **打斷結算後果**：
  - 目標觸發 `triggerInterrupt(reason, stunDurationMs)`。
  - **罡氣崩散**：自動呼叫 `triggerPoiseBreak()` 使怪物進入破防失衡虛弱狀態，清空當前蓄力施法意圖。
  - **行動鎖定**：賦予短暫暈眩時間鎖（`stunnedUntil`），且推遲下次攻擊間隔（`nextAttackTime`）。
  - **戰鬥反饋**：派發 `HitOutcome.INTERRUPTED` 事件，戰鬥日誌記錄 `[反制打斷]`，並通知前端觸發打斷震顫與特效。

---

## ⚔️ 七、現代半即時刷寶 DRPG 戰鬥體系定位 (Action-Tactical DRPG Paradigm)

本戰鬥引擎跳脫 80 年代傳統 DRPG 枯燥死板的純回合制平砍，吸納了現代 **Hack-and-slash (砍殺/連鎖/刷寶)** 的快節奏打擊感與策略連鎖，確立為 **「動作戰術 DRPG (Action-Tactical DRPG)」**：

### 1. 戰鬥內 Hack-and-slash 爽快五大支柱
1. **罡氣削韌與破防重創 (Poise Break & Stagger Crit)**：
   - 目標韌性歸零觸發 1.5x 爆擊加成與虛弱硬直，形成「削韌 $\to$ 破防 $\to$ 爆發」的節奏高潮。
2. **行動預告與即時打斷 (Combat React & Interrupt)**：
   - 敵方蓄力前搖窗口內，打出控制技能打斷大招並震散罡氣，體驗魂系見招拆招的反制成就感。
3. **元素連鎖與狀態引爆 (Elemental Reactions & Primer/Detonator)**：
   - 冰火蒸發（2.0x 穿透）、雷水感電、裂魂印記 (Primer) 配合重擊 (Detonator) 瞬間結算剩餘 DoT 傷害並觸發全場衝擊波。
4. **古仙陣法跨隊員連攜合擊 (Cross Combos & Formation Finisher)**：
   - 3.5 秒動態窗口內由不同隊員接力攻擊相同目標：【雙星合璧】(+25% 傷) $\to$ 【三才混元】(+50% 傷, 回補 SAN) $\to$ 【四象誅仙】(+100% 傷)，積累共鳴印記手動引爆全屏天外合擊。
5. **隊友自動戰術鏈 (Gambit AI Chains)**：
   - 借鑒 FF12 戰術條件鏈，隊友全自動依優先度執行打斷、補血、破防，杜絕半即時下 5 人小隊操作過載。

### 2. 與 DRPG 長線生存的架構邊界 (Boundary Separation)
- **單場戰鬥 (Local Scope)**：氣血 HP、法力 MP 與連攜點數自由流轉，技能反饋強烈，杜絕空藍卡頓。
- **地牢大局 (Global DRPG Scope)**：**火把光明度 (Torchlight)** 與 **道心理智 (SAN 值)** 為不可逆衰減資源。戰鬥贏得再漂亮，若火把耗盡或 SAN 跌破 30%，小隊依然面臨不可名狀靈視視界畸變與滅團風險。
- **刷寶動力 (Loot Grinding)**：戰鬥掉落高隨機詞綴之「工程師生物零件」、「黑煞血肉合金」、「千星科技晶片」，直接驅動玩家反覆挑戰更深層地牢。

---

## 🔗 八、代表類別與資料設定檔

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **純領域規則** | `com.example.htmlmud.domain.combat.CombatRules` | 一元一次圓桌判定、五行抗性與逆流結算 |
| **戰鬥編排** | `com.example.htmlmud.domain.dungeon.battle.DrpgBattleService` | 戰鬥初始化、地牢遭遇戰、野外遭遇戰 5 人小隊協同迎敵 (`startFieldBattle`)、指令派發 |
| **時間線迴圈** | `com.example.htmlmud.domain.dungeon.battle.DrpgCombatLoop` | 500ms Tick 虛擬執行緒迴圈推進、攻擊觸發與戰鬥演出廣播 |
| **狀態機與打斷服務** | `com.example.htmlmud.domain.dungeon.battle.CombatFsmService` | 蓄力前搖管理、標籤比對與即時打斷反制結算 |
| **智慧索敵** | `com.example.htmlmud.domain.dungeon.battle.DrpgEnemyTacticsService` | 前排加成、嘲諷係數與仇恨演算 |
| **戰果結算** | `com.example.htmlmud.domain.dungeon.battle.DrpgRewardService` | 經驗派發、40格小隊公共行囊入庫與任務擊殺事件通知 |
| **生靈狀態連動** | `BattleContext.onBattleEnd` | 戰鬥結束大世界 Mob 實體標記死亡、停止心跳與移出房間 |
| **戰況視圖契約** | `com.example.htmlmud.domain.dungeon.dto.BattleEnemyViewDto` | 前端戰況實體 DTO（含 `currentAction`, `actionProgressPercent`, `interruptible`） |
| **精簡敵方傳輸** | `com.example.htmlmud.domain.dungeon.dto.BattleEnemyDto` / `CombatMobDto` | 敵方蓄力與打斷即時狀態傳輸合約 |
| **資料設定** | `src/main/resources/data/party/party_skills.json` | 小隊技能傷害、削韌值、蓄力 `castTimeMs` 與打斷 `interruptible` 標籤 |
| **資料設定** | `src/main/resources/data/zones/*/mobs.json` | 怪物基礎屬性、五行抗性、攻擊間隔與怪物蓄力前搖 `castTimeMs` |

---

## 🚀 八、未來架構演進與現代化擴充規劃 (Future Roadmap & Scalability)

為使核心戰鬥引擎達到現代動作 RPG（如暗黑破壞神、黑神話、魂系）的策略交鋒深度，確立以下四大演進方向：

### 1. 🎯 現代遊戲機制深化 (Modern Gameplay & Depth)
- **敵方行動預告與蓄力打斷機制 (Telegraph & Interrupt System)**：
  首領在施放毀天滅地的終極仙術（如「九幽天劫引」）前，會進入 2.5~4 秒的「神通蓄力期」，介面展示明顯的暗紅警戒進度條。此時首領進入霸體但露出弱點，小隊若能在蓄力完成前打出足夠的削韌傷害（Poise Damage），即可觸發「神通反噬·強力擊暈」，給予極大的博弈成就感。
- **陣法五行連攜共鳴 (Elemental Chain & Resonance)**：
  隊友技能引入元素印記（如冰魄符疊加「寒霜印」），後續隊友使用離火劍訣擊中時觸發「冰火蒸騰爆裂」，造成 $1.8\times$ 範圍穿透傷害，將單純的數值拼殺升級為小隊招式連攜策略。

### 2. 🛡️ 穩定性與狀態防禦 (Robustness & Resilience)
- **戰鬥結算責任鏈管線 (Combat Pipeline Architecture)**：
  將判定流程重構為標準責任鏈（`PreAttackCheck` ➔ `RoundTableRoll` ➔ `PoiseBreakResolution` ➔ `ElementalMitigation` ➔ `LifeDrainOrHeal` ➔ `PostHitTriggers`）。每個環節單一職責且獨立單元測試，徹底杜絕巨石方法中的副作用蔓延。
- **確定性戰鬥重播引擎 (Deterministic Replay Engine)**：
  基於固定隨機種子（Random Seed）記錄玩家每個操作 Tick，支援戰鬥過程 100% 確定性重播，為後續競速排行榜防作弊檢驗與疑難 Bug 復現提供堅實底座。

### 3. ⚡ 效能與高頻運算最佳化 (Performance & Concurrency)
- **高頻 Tick 零垃圾回收分配 (Zero-Allocation Tick Design)**：
  戰鬥迴圈（每 500ms 一次）避免頻繁創建短命 `DamageResult`、`HitEvent` 物件，改採物件池（ThreadLocal Object Pool）或不可變 Record 複用，將戰鬥期間的 JVM Minor GC 頻率降至趨近於零，杜絕任何微小卡頓。
- **無鎖化生靈屬性並行結算**：多實體在場時，生靈 Buff 結算與 ATB 推進支援並行流（Parallel Stream）安全結算，高密度群怪戰鬥幀率極致平穩。

### 4. 🔌 可擴充性與模組化 (Extensibility & Modding)
- **五行天道環境共鳴場 (Battlefield Leyline System)**：
  在戰鬥舞台中注入環境靈脈濃度（如「火山溶洞·離火濃郁」、「極地玄冰·坎水極致」）。JSON 可直接宣告環境共鳴規則，環境會動態干涉法術威力與冷卻，無需硬編碼特定地圖特例。
