# 陣法系統 Review 與重構草案

## 一、總結結論

目前專案中的陣法系統，已具備很好的基礎骨架：

- 以 JSON 資料驅動陣法模板
- 依隊伍人數自動匹配基本/進階陣法
- 依職業條件驗證宣戰資格
- 依孔位綁定攻防倍率與特殊描述
- 依靈威槽與 SAN 成本實作大招體驗
- 有測試覆蓋基本切換、倍率、職業條件與 fallback 機制

整體來說，這已經不是「單純的文本資料」而是「具備戰鬥邏輯的隊伍戰術系統」；但它仍有一個明顯的落差：

- 目前很多 passiveAura、slot bonus、特殊效果只停留在描述層，不完全進入戰鬥抽象規則層。
- 孔位綁定依賴索引而非戰術角色語意，會使後續擴張較難維護。
- 陣法過於偏「模板選擇」與「大招按鈕」，尚未擁有完整的戰場節奏與反制設計。

因此，下一階段最重要的目標不是再增加更多陣法，而是把現有系統從「可切換模板」提升為「真實戰場戰術系統」。

---

## 二、陣法規則總表

### 1. 目前已具備的規則

| 項目 | 現狀 | 說明 | 評價 |
|---|---|---|---|
| 陣法資料定義 | 已完成 | JSON 內定義名稱、描述、被動、基本/進階、職業門檻、孔位、絕技 | 強 |
| 隊伍人數匹配 | 已完成 | 2/3/4/5 人分別對應不同基本陣法 | 強 |
| 職業條件 | 已完成 | requiredClasses 可驗證是否符合陣法要求 | 強 |
| 陣位倍率 | 已完成 | slot attackMultiplier / defenseMultiplier 套用到隊員數值 | 強 |
| 站位切換 | 已完成 | FRONT / BACK / MIDDLE 站位可切換 | 強 |
| 靈威系統 | 已完成 | formationEnergy 0~100，可用於大招 | 中上 |
| 大招 SAN 成本 | 已完成 | 釋放陣法大招可消耗全隊 SAN | 中上 |
| 陣法 fallback | 已完成 | 人數/職業不符時自動退回基本陣法 | 強 |
| 被動光環展示 | 已完成 | passiveAura 顯示在 UI 及狀態欄 | 中 |
| 被動光環實際戰鬥效果 | 未完全完成 | 目前大多只是 doc 文案與 UI 顯示 | 待強化 |
| 陣法反制 / 地形適應 | 未完成 | 缺少強弱關係與戰場條件 | 待開發 |
| 陣眼破壞 / 殘陣降階 | 未完成 | 缺少陣法狀態機 | 待開發 |
| 連鎖/節奏設計 | 未完成 | 大招仍偏單一觸發 | 待開發 |

### 2. 現有規則模型的核心要素

1. 編制條件
   - requiredPartySize：隊伍人數要求
   - basic：是否為基本陣法
   - requiredClasses：職業門檻

2. 戰鬥實際影響
   - 每個陣位的 attackMultiplier、defenseMultiplier
   - 每個成員的 row 分派
   - 每個成員的 buff 會透過 refreshFormation() 掛載

3. 大招資源
   - energyCost：陣法靈威消耗
   - sanCost：全隊理智代價
   - basePower：基礎威力

4. 展示文字
   - description
   - passiveAura
   - slot.specialBonusDesc

### 3. 目前的價值定位

目前的陣法設計，更接近以下定位：

- 隊伍模板系統：選定陣法、改變隊員站位、增加數值加成
- 進階戰術面板：一個獨立的戰鬥資源與大招入口
- 內容管理系統：大量 JSON 配置，方便擴充內容

這是非常好的第一版，因為它讓玩家感受到「陣法不是裝飾，而是有意義的戰鬥選擇」。

但若要達到更高階遊戲性，須再補足「規則層」而非僅「展示層」。

---

## 三、JSON 擴充建議

### 1. 現有 JSON 的優勢

現有 [practice_mud/src/main/resources/data/formations/formations.json](practice_mud/src/main/resources/data/formations/formations.json) 內容已經足夠做中期擴充，因為它已經有：

- 主要配置屬性
- 包含 slot 的具體效果
- 大招的專屬資料
- 必要格式與命名規則

所以最好的擴充方向不是重寫，而是增加更強的結構化欄位。

### 2. 建議新增的 JSON 欄位

#### A. 陣法等級與狀態機

```json
{
  "tier": 2,
  "stability": 100,
  "breakThreshold": 40,
  "status": "NORMAL",
  "requiresCoreMember": true,
  "coreMemberSlot": 0,
  "degradeOnCoreMemberLoss": true,
  "degradeTo": "formation_four_symbols"
}
```

建議新增：

- tier：陣法階位（1~5）
- stability：陣法穩定度
- breakThreshold：壞陣閾值
- status：NORMAL / STABLE / SHAKEN / BROKEN
- requiresCoreMember：是否需要陣眼/核心成員
- coreMemberSlot：核心角色位置
- degradeOnCoreMemberLoss：陣眼死亡時是否降階
- degradeTo：降階後的替代陣法

#### B. 被動光環的規則化字段

```json
{
  "passiveEffects": [
    {
      "type": "ATTACK_MULTIPLIER",
      "target": "ALL_PARTY",
      "value": 0.10,
      "applyWhen": "ALWAYS"
    },
    {
      "type": "DAMAGE_REDUCTION",
      "target": "ALL_PARTY",
      "value": 0.10,
      "applyWhen": "ALWAYS"
    },
    {
      "type": "SAN_RESISTANCE",
      "target": "ALL_PARTY",
      "value": 15,
      "applyWhen": "ALWAYS"
    }
  ]
}
```

這比 passiveAura 只寫字串更強，因為它可以被解析進戰鬥引擎。

#### C. 插槽角色語意

```json
{
  "slotIndex": 0,
  "slotName": "玄武位【巨甲堅壁】",
  "role": "TANK",
  "requiredRow": "FRONT",
  "assignedRow": "FRONT",
  "attackMultiplier": 0.9,
  "defenseMultiplier": 1.35,
  "sanResistanceBonus": 15,
  "specialBonusDesc": "前衛主坦：免傷+35%，受擊率大幅提升"
}
```

建議新增：

- role：TANK / DPS / HEALER / CONTROL / SUPPORT
- priority：高 / 中 / 低
- synergyGroup：彼此協同的陣位群組

#### D. 大招的進階配置

```json
{
  "ultimateSkill": {
    "id": "skill_phoenix_rebirth",
    "name": "鳳凰涅槃天火",
    "energyCost": 100,
    "sanCost": 0,
    "cooldownTurns": 3,
    "target": "ALL_ENEMIES",
    "effectType": "AOE_DAMAGE",
    "basePower": 260,
    "secondaryEffects": [
      { "type": "HEAL", "value": 0.2, "target": "ALL_ALLIES" },
      { "type": "BUFF", "buffId": "rebirth", "duration": 2 }
    ]
  }
}
```

建議增加：

- cooldownTurns
- target（ALL_ENEMIES / RANDOM / SINGLE / ALL_ALLIES）
- secondaryEffects
- triggerCondition
- timing（ROUND_START / ON_CAST / ON_DAMAGE_TAKEN）

### 3. 建議的資料結構策略

建議把目前的 "passiveAura" 文字字段保留作為 UI 展示，但新增一套真正可解析的 effect 節點：

- passiveAura：文案展示
- passiveEffects：規則層配置
- ultimateSkill：大招配置
- slotRules：孔位規則配置

這樣不會破壞舊資料兼容，也能逐步遷移。

---

## 四、Java 類別重構方案

### 1. 現階段的設計亮點

目前的 Java 模型已經具備：

- FormationTemplate：陣法模板
- FormationSlot：孔位配置
- FormationSkill：陣法大招
- Party：隊伍掛載陣法與靈威
- PartyService：管理註冊與候選陣法

這些類別很適合繼續延伸成一個更成熟的陣法引擎。

### 2. 建議新增/重構的類別

#### A. FormationEffectDefinition

新增一個規則類：

- type：ATTACK_MULTIPLIER / DAMAGE_REDUCTION / HEAL / SAN_RESIST / CRIT / AOE
- target：ALL_PARTY / FRONT_ROW / BACK_ROW / SELF / ALL_ALLIES / ALL_ENEMIES
- value：加成值
- applyWhen：ALWAYS / ON_HIT / ON_TAKEN_DAMAGE / ROUND_START
- durationTurns：持續回合

用途：
- 讓 passiveAura 不再只是文案
- 讓戰鬥邏輯可直接處理這些效果

#### B. FormationRole enum

新增：

```java
public enum FormationRole {
  TANK,
  DPS,
  HEALER,
  CONTROL,
  SUPPORT,
  CORE,
  FLEX
}
```

用途：
- 把 slot 角色語意化
- 避免只能靠索引推斷陣位功能

#### C. FormationState enum

新增：

```java
public enum FormationState {
  NORMAL,
  STABLE,
  SHAKEN,
  BROKEN
}
```

用途：
- 管理陣法穩定度
- 允許在戰鬥中因成員死亡、受傷、被制壓而降階

#### D. FormationEngine / FormationResolver

新增一個服務層：

- applyPassiveEffects(Party party)
- evaluateFormationStatus(Party party)
- resolveEligibleFormations(Party party)
- applyFormationBreak(Party party)
- triggerUltimate(Party party, FormationTemplate formation)

用途：
- 把「玩法邏輯」從 Party 類別拆出去
- 避免 Party 同時管理隊伍、戰鬥數值、陣法狀態、UI 展示

#### E. FormationSlotAssignment

如果想更完整地結構化，可新增：

- assignedMemberId
- role requirement
- front/back/middle assignment
- synergies

用途：
- 把「孔位」變成真正的制度，而非單純的 index.

### 3. 建議的重構原則

#### 原則 1：拆分資料層與規則層
- FormationTemplate 只保留結構資料
- FormationEffectDefinition 只保留規則描述
- FormationEngine 處理運算

#### 原則 2：Party 僅保留隊伍狀態
- Party 不應該承擔過多戰鬥規則
- 例如大招觸發、被動累加、陣法斷裂邏輯，可集中到 FormationEngine

#### 原則 3：讓模型語意清晰
- slotIndex 是資料位置
- role 是戰術職能
- assignedRow 是戰鬥站位
- state 是陣法狀態

這樣未來才不會因為增加功能而造成一大堆 if/else 雜亂。

### 4. 最小修改方案（務實版）

如果你不想大改架構，最實用的低風險方案是：

- 保留 FormationTemplate / FormationSlot / FormationSkill
- 新增 FormationEffectDefinition 和 FormationEngine
- 在 Party.refreshFormation() 補上「套用陣法被動效果」的入口
- 在 battle 相關 logic 中，讓法術與傷害計算都會檢查該 passive effect

這可以做到漸進式演進，風險較低。

---

## 五、陣法效果規則設計草案（完整版）

### 1. 設計目標

目標不是讓陣法只增加一個數值，而是讓它形成一個可持續的戰術系統：

- 依隊伍組成決定能否啟動
- 依站位安排決定戰鬥位置與角色責任
- 依陣法穩定度決定強弱與降階
- 依大招資源決定爆發節奏
- 依戰場環境與敵方配置決定有效性

### 2. 遊戲層級設計

#### A. 陣法等級

- 基本陣法：2/3/4/5 人規範陣型
- 中階陣法：5 人進階陣法（如 五行、十字、鳳凰）
- 高階陣法：禁忌或異常陣法（如 玄陰噬魂陣）

#### B. 陣法穩定度

每個陣法有穩定值：

- 100 = 正常
- 60 ~ 79 = 震盪
- 30 ~ 59 = 失衡
- 0 ~ 29 = 破陣

穩定度受以下因素影響：

- 陣眼成員是否存活
- 是否有角色缺位
- 是否處於大規模被控狀態
- 是否受 boss 反制
- 是否在不適合環境中釋放

#### C. 陣眼核心規則

每個陣法都可以指定 1 個 coreMemberSlot。
若該位角色倒下或失去作用，陣法會發生：

- 下降一階穩定值
- 被動效果下調
- 大招可用性下降
- 若穩定低於門檻，陣法降階或失效

這樣就能讓「陣眼」有真正戰術意義，而不只是說明文字。

### 3. 被動光環規則草案

#### A. 全隊共通效果

所有陣法都有一個全隊被動，至少包含三類：

1. 攻擊增益
   - 物理傷害 +X%
   - 法術傷害 +X%
   - 全體暴擊率 +X%

2. 防禦/生存
   - 受擊傷害 -X%
   - 閃避 +X%
   - SAN 抵抗 +X%

3. 戰術節奏
   - 連擊或連攜加成
   - 靈威獲得 +X
   - 治療效能 +X%

#### B. 站位專屬效果

每個孔位只掛載一種主要定位：

- 前排：承傷、吸引仇恨、物理牽制
- 中排：協同、支援、戰場控制
- 後排：法術輸出、治療、遠程支援

例如：

- 玄武位：前排坦克，增加防禦與仇恨吸引
- 白虎位：前排爆發，增加暴擊與破甲
- 朱雀位：後排法核，增加法術傷害
- 青龍位：後排治療，增加治療與淨化

這樣能保留現有設計，又更自然地把角色職責和站位設計連結。

### 4. 大招規則草案

#### A. 大招的通用資源鏈

- 靈威：陣法資源，獲得方式為戰鬥中普攻 / 技能 / 特定事件累積
- SAN：全隊理智代價，代表法力逆天造成的精神代價
- 冷卻：每個大招有冷卻回合，不能無限放

#### B. 大招節奏

- 每場戰鬥可用 1 次高成本大招，或 2 次中成本大招
- 觸發條件：靈威滿、陣法穩定度達標、需核心成員活著
- 施放後會：
  - 消耗靈威
  - 可能增加陣法波動
  - 可能帶來全隊 SAN 成本

#### C. 大招效果類型

建議將目前 effectType 進一步細分：

- AOE_DAMAGE
- TEAM_BUFF
- TEAM_HEAL
- CLEANSE
- SHIELD
- MENTAL_DAMAGE
- SUMMON_EFFECT
- FIELD_CONTROL

這樣效果不再只是打到全體，而是可以形成回合節奏。

### 5. 戰場環境與反制設計草案

每個陣法都能有環境相剋：

- 火系陣法在水澤場地中承受 15% 能量衰減
- 水系陣法在火焰場地中承受 10% 失衡
- 玄陰陣法在純淨聖地中遭到抑制
- 鳳凰陣在長時間持續戰中有更高穩定收益

這讓陣法不再只是固定數值，而是戰場中的戰術選擇。

### 6. 陣法壞掉與降階設計草案

#### 簡化版規則

- 若 coreMember 死亡：陣法穩定度 -25
- 若同時有 2 個以上孔位沒有符合職業/站位：穩定度 -15
- 若大招連續使用 2 次：穩定度 -10
- 若戰場環境反制：穩定度 -10

#### 失衡條件

- 穩定度 < 60：被動效果降低至 50%
- 穩定度 < 30：大招不可用
- 穩定度 = 0：自動失效，退回基礎陣法或隊伍單體站位模式

這能讓陣法有真正的「破陣」感，不會只是數值衰減。

### 7. 建議的最終規則層次

最終目標可拆成四層：

1. 陣法模板層
   - 名稱、描述、條件、孔位、絕技

2. 戰術效果層
   - 被動增益、站位加成、環境增益

3. 狀態層
   - 穩定、震盪、破壞、降階

4. 戰鬥資源層
   - 靈威、SAN、冷卻、觸發條件

這四層就能覆蓋陣法系統的全部玩法。

---

## 六、建議落地順序

### 階段 1：最小有效改造（1~2 天）

- 新增 passiveEffects 結構
- 新增 FormationEffectDefinition 類
- Party.refreshFormation() 先接入這些 effect
- 先實作 3 類效果：攻擊增幅、受擊減傷、SAN 抗性

### 階段 2：站位語意化（3~5 天）

- 增加 FormationRole
- slot 增加 role / priority / synergyGroup
- 讓 row 角色和陣位定位脫鉤

### 階段 3：狀態機與破陣（1 週）

- 增加 FormationState
- 增加 stability / coreMemberSlot / degradeTo
- 實作核心成員倒下造成的降階

### 階段 4：戰場環境與反制（2 週）

- 增加 environment tags
- 增加敵方 boss tags
- 做一套陣法相剋與場地修正規則

### 階段 5：大招節奏深化（後續）

- 加 cooldown
- 加 secondaryEffects
- 加 triggerCondition
- 加 player interaction UI

---

## 七、最終評語

這套陣法系統的最大優勢是：

- 已具備清晰的資料層與戰鬥入口
- 擴充成本低
- 測試覆蓋基礎玩法良好
- 內容設計足夠丰富，具有強烈遊戲特色

它現在不是一個「失敗的系統」，而是一個「準備進入第二階段成熟化的系統」。

真正要做的，並不是再堆更多神奇名稱或額外大招，而是要把目前存在的「描述層」——也就是 passiveAura、slot bonus、角色定位——轉成「規則層」與「戰場層」。

這樣後續才能從：

- 「我看得到陣法」

進化成：

- 「我必須依據陣法、站位、穩定度、環境與資源來決策」

這才會真正成為一套有戰術張力的陣法系統。

---

## 八、簡短建議總結

如果只保留最關鍵的建議，則是：

1. 把 passiveAura 轉成可解析的 passiveEffects
2. 把 slot 索引改成 role + assignment + stability
3. 引入 formation state / core member / break mechanic
4. 將大招提升為具 cooldown、target、secondary effect 的戰術技能
5. 讓陣法與場景、敵方特性、隊伍編制一起運作

這樣，陣法系統才有可能從「美觀的內容系統」提升成為「真正影響戰鬥節奏的核心玩法」。
