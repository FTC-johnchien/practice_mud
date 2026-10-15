# ⚔️ 後端 M3: 核心戰鬥引擎模組深度手冊 (Combat Engine Manual)

> **模組編號**：M3  
> **系統類型**：後端核心業務模組  
> **核心定位**：管理半即時戰鬥時間線（200ms Tick ATB/GCD）、一元一次擲骰圓桌判定公式、罡氣削韌與破防（Poise Break）、九大五行抗性與逆流回血、智慧仇恨索敵與 Buff 狀態機。

---

## ⏱️ 一、半即時戰鬥迴圈 (Semi-Realtime ATB Loop)

遊戲跳脫傳統純回合制的僵化等待，採用類似經典 ATB（Active Time Battle）的**半即時時間線機制**：

```mermaid
sequenceDiagram
    autonumber
    participant Loop as DrpgCombatLoop (200ms Tick)
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

- **Tick 頻率**：後端以每 200ms 一次 Tick 推進戰鬥世界時間。
- **身法速度加成**：角色身法（Agility）越高，攻擊與技能冷卻縮減越快。

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

## 🌊 四、九大五行抗性與逆流回血 (Elements & Resistances)

修仙世界講究五行生剋與道法抗衡。專案定義了 9 大傷害類型（`DamageType`）：
- **物理系**：`PHYSICAL`（通用）、`SLASH`（斬擊）、`PIERCE`（穿刺）、`BLUNT`（鈍擊）。
- **五行天道系**：`METAL`（金石）、`WOOD`（草木）、`WATER`（坎水）、`FIRE`（離火）、`EARTH`（厚土）、`WIND`（神風）。
- **陰陽玄黃系**：`CORRUPT`（陰煞/混沌）、`LIGHT`（陽炎/天光）。

### 結算公式：順成人，逆成仙（`CombatRules.resolveResistance`）

```java
public static ResistanceResult resolveResistance(int rawDamage, DamageType type, double resistance)
```

1. **物理保底穿透原則**：
   - 物理系傷害不走元素逆流回血。
   - 為防止極限堆防禦導致「絕對無敵死鎖」，物理抗性最高折減至 90%（**保底 10% 穿透**）：
     $$\text{FinalDamage} = \max(1, \text{RawDamage} \times \max(0.1, 1.0 - \text{Resistance}))$$
2. **五行逆流回血原則 (Absorption)**：
   - 當怪物或修士修煉特定真功，使某屬性抗性大於 100%（$\text{Resistance} > 1.0$，如火靈真身火抗 125%）：
   - **承受傷害歸零**，超額部分化為甘霖，**逆流轉化為氣血回復**！
     $$\text{HealAmount} = \text{round}(\text{RawDamage} \times (\text{Resistance} - 1.0))$$
   - 完美還原修仙小說中「用火法轟擊火精靈反而為其療傷」的神妙機制。

---

## 🧪 五、Buff 狀態機生命週期與整合

> 📖 **權威專屬手冊**：  
> 生靈之狀態效果、Buff/Debuff 狀態機、WoW 式刷新與層數堆疊、多重護盾吸收優先序、控場門控、罡氣破防條（Poise Break）攻防博弈、九大天道靈氣五行抗性與逆流回血、以及屬性圖示聚合 HUD 契約，已收斂獨立為本戰鬥引擎之專屬子模組手冊：  
> 👉 **請詳閱 [`後端_M3-1_狀態效果與Buff結算模組_BUFF_AND_STATUS_EFFECTS.md`](後端_M3-1_狀態效果與Buff結算模組_BUFF_AND_STATUS_EFFECTS.md)**。

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

## 🔗 七、代表類別與資料設定檔

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **純領域規則** | `com.example.htmlmud.domain.combat.CombatRules` | 一元一次圓桌判定、五行抗性與逆流結算 |
| **戰鬥編排** | `com.example.htmlmud.domain.dungeon.battle.DrpgBattleService` | 戰鬥初始化、逃跑、指令派發 |
| **時間線迴圈** | `com.example.htmlmud.domain.dungeon.battle.DrpgCombatLoop` | 200ms Tick 輪詢推進與攻擊觸發 |
| **狀態機與打斷服務** | `com.example.htmlmud.domain.dungeon.battle.CombatFsmService` | 蓄力前搖管理、標籤比對與即時打斷反制結算 |
| **智慧索敵** | `com.example.htmlmud.domain.dungeon.battle.DrpgEnemyTacticsService` | 前排加成、嘲諷係數與仇恨演算 |
| **戰果結算** | `com.example.htmlmud.domain.dungeon.battle.DrpgRewardService` | 經驗派發、掉落物入庫與金幣分配 |
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
  戰鬥迴圈（每 200ms 一次）避免頻繁創建短命 `DamageResult`、`HitEvent` 物件，改採物件池（ThreadLocal Object Pool）或不可變 Record 複用，將戰鬥期間的 JVM Minor GC 頻率降至趨近於零，杜絕任何微小卡頓。
- **無鎖化生靈屬性並行結算**：多實體在場時，生靈 Buff 結算與 ATB 推進支援並行流（Parallel Stream）安全結算，高密度群怪戰鬥幀率極致平穩。

### 4. 🔌 可擴充性與模組化 (Extensibility & Modding)
- **五行天道環境共鳴場 (Battlefield Leyline System)**：
  在戰鬥舞台中注入環境靈脈濃度（如「火山溶洞·離火濃郁」、「極地玄冰·坎水極致」）。JSON 可直接宣告環境共鳴規則，環境會動態干涉法術威力與冷卻，無需硬編碼特定地圖特例。
