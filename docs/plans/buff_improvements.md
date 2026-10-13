# ☯️ 太陰地宮 DRPG：Buff / Debuff 狀態系統綜合改善規劃書
# (Buff & Debuff System Master Improvement Specification)

> **文檔版本**：v1.1.0  
> **制定日期**：2026-10-13  
> **文檔定位**：本文件綜合整理自 [`buff_plan.md`](./buff_plan.md)、[`Codex_Code_Review.md`](./Codex_Code_Review.md)、[`Copilot_Code_Review.md`](./Copilot_Code_Review.md) 與 [`Claude_Review_buff_plan.md`](./Claude_Review_buff_plan.md)。  
> 旨在解決「模型與規格先於可執行行為」之歷史技術債，收斂各方架構評審意見與使用者確認之防禦/吸收機制，建立精確、資料驅動且具備完整前後端閉環的 Buff 系統實施藍圖。

---

## 📌 目錄 (Table of Contents)

1. [總體改善目標與設計原則](#1-總體改善目標與設計原則)
2. [不合理與高風險設計之裁決與排除說明](#2-不合理與高風險設計之裁決與排除說明)
3. [核心機制架構與技術契約](#3-核心機制架構與技術契約)
   - 3.1 狀態生命週期與常駐光環契約 (Duration & Permanent Aura)
   - 3.2 狀態刷新、堆疊與多重護盾吸收契約 (WoW-Style Stacking & Absorption)
   - 3.3 屬性修正 (STAT_MODIFIER) 與五行吸收回血機制 (使用者確認之攻防核心)
   - 3.4 控場分級與全動作入口門控 (Control Hierarchy & Action Gates)
   - 3.5 前後端資料流與雙舞台視覺投影契約 (WebSocket DTO & Presentation)
4. [第一階段 (Phase 1 MVP) 聚焦收斂之狀態與技能矩陣](#4-第一階段-phase-1-mvp-聚焦收斂之狀態與技能矩陣)
5. [分階段實施路線圖與驗收標準 (Implementation Roadmap & DoD)](#5-分階段實施路線圖與驗收標準-implementation-roadmap--dod)
   - Phase 1: 核心契約、生命週期與屬性/吸收機制落地 (P0/P1)
   - Phase 2: 控場門控與技能中斷收斂 (P1)
   - Phase 3: DTO 補全與雙舞台視覺投影 (P1)
   - Phase 4: Gambit 戰術方針 AI 反制與全自動化測試 (P1/P2)

---

## 1. 總體改善目標與設計原則

為了徹底終結 `buff_plan.md` 中企劃設定與底層引擎割裂之現況，本改善計畫確立以下五大原則：

1. **唯一真相來源 (Single Source of Truth, SSoT)**：
   - 攻防屬性、抗性計算與傷害減免一律收斂委託 [`CombatRules`](../../src/main/java/com/example/htmlmud/domain/combat/CombatRules.java)，嚴禁在 DRPG 或 MUD 另造平行公式。
2. **機制先行，拒絕空 enum**：
   - 不一次實裝 43 個空枚舉。先實裝 6 個具備完整「配置 $\to$ 施加 $\to$ 結算 $\to$ DTO 廣播 $\to$ 前端雙舞台渲染 $\to$ AI 反制」之垂直切片。
3. **契約驅動生命週期**：
   - 廢除 `-1` 魔術數字模糊語意，明確劃分 `TIMED`（限時心跳）與 `PERMANENT`（常駐光環）。
4. **前端純資料投影 (Pure Projection)**：
   - 前端嚴禁由文字 Log 正則表達式猜測狀態，一律讀取後端推送之 `activeBuffs` 結構化資料，並同時支援戰鬥 HUD 與 1600×900 角色道基詳細面板（`C` 鍵）。
5. **符合使用者裁決之深度防禦博弈**：
   - 物理防禦設有上限防無敵；五行法術抗性支援超越 100% 轉化為**「五行靈氣逆流回血」**機制。

---

## 2. 不合理與高風險設計之裁決與排除說明

經過交叉審查 [`buff_plan.md`](./buff_plan.md)、[`Codex_Code_Review.md`](./Codex_Code_Review.md) 與 [`Copilot_Code_Review.md`](./Copilot_Code_Review.md)，以下項目被裁定為**不合理或過早設計**，並經使用者確認於 Phase 1 予以排除或重新定位：

| 原始提案項目 | 裁決結果 | 排除 / 調整原因說明 |
| :--- | :---: | :--- |
| **走火入魔 (`QI_DEVIATION`) 與定身 (`MIND_SEALED`) 納入 Buff 系統** | ❌ **排除於 Buff 矩陣** | 專案在 Phase 11 已實作完備的 `MadnessState`（`SANE` $\to$ `CHAOS` $\to$ `SEALED` $\to$ `ABERRATION`）及異變計數器。若在 Buff 系統再做一套，會導致兩本帳並存、雙軌競爭與死鎖風險。走火入魔維持現有狀態機運作。 |
| **殘酷 DRPG：永久修為折損 (`ESSENCE_DRAIN`)** | ⏸️ **暫緩至中期** | 遊戲目前尚未具備城鎮除咒神殿、救援機制與存檔回滾功能，直接永久扣除玩家 10% EXP 會造成極端不可逆的挫折感，等城鎮設施齊全後再行開放。 |
| **迷宮步進全隊扣血/扣SAN (`CORPSE_ROT`)** | ⏸️ **暫緩至中期** | 涉及地牢步進事件與探索補給閉環，不應與戰鬥 Buff 心跳混在一起結算。 |
| **直接刪除 43 個 `EffectType`** | ⚠️ **調整為相容收斂** | 既有存檔或 JSON 仍可能含有特定枚舉名稱，直接刪除會破壞序列化相容性。改為保留枚舉但將其語意對齊至 12 種核心機制，其餘標記 `@Deprecated`。 |
| **硬編碼特定技能之特例分支** | ❌ **徹底禁止** | 嚴格遵守 `GEMINI.md` 鐵律：嚴禁在 Java 代碼寫死 `if (k.contains("鐵牛"))` 或 `if (skillId.equals("xxx"))`，一切透過 `BuffConfig` 資料驅動。 |

---

## 3. 核心機制架構與技術契約

### 3.1 狀態生命週期與常駐光環契約 (Duration & Permanent Aura)

徹底解決陣法孔位光環因 `remainingTicks = -1` 在第一個 Tick 即被清除的 Bug：

* **生命週期模型**：
  - 基準心跳：1 Tick = 500ms（由戰鬥心跳迴圈或 `WorldPulse` 驅動）。
  - `durationTicks` 與 `remainingTicks` 語意：
    - `> 0`：限時狀態，每個 Tick `remainingTicks--`，歸零時標記過期。
    - `== PERMANENT_DURATION (-1)`：常駐光環（如陣法孔位加成），不參與秒數遞減，永不過期（除非隊伍破陣由系統顯式 `removeBuff` 移除）。
* **過期判定契約 (`ActiveBuff.isExpired()`)**：
  ```java
  public static final int PERMANENT_DURATION = -1;

  @JsonIgnore
  public boolean isPermanent() {
      return durationTicks == PERMANENT_DURATION;
  }

  @JsonIgnore
  public boolean isExpired() {
      if (isPermanent()) {
          // 常駐光環不隨時間過期；若為常駐護盾則需容量耗盡才過期
          return (category == BuffCategory.SHIELD && value <= 0);
      }
      return remainingTicks <= 0 || (category == BuffCategory.SHIELD && value <= 0);
  }

  public void decrementTick() {
      if (!isPermanent() && this.remainingTicks > 0) {
          this.remainingTicks--;
      }
      this.ticksSinceLastTick++;
  }
  ```

---

### 3.2 狀態刷新、堆疊與多重護盾吸收契約 (WoW-Style Stacking & Absorption)

當對目標施加已存在的狀態時（相同 `id`），執行具名刷新政策：
1. **持續時間刷新**：重置 `remainingTicks = durationTicks`。
2. **層數累加**：若 `maxStacks > 1`，當前層數 `stacks = Math.min(maxStacks, stacks + 1)`；重置 `ticksSinceLastTick = 0`。
3. **護盾值裁決**：若為護盾類，`value = Math.max(existing.value, newBuff.value)`（取較厚者，防止無限膨脹）。
4. **多重護盾吸收優先序 (Shortest Duration First)**：
   - 目標受傷時，先依 `activeBuffs` 中剩餘時間最短的護盾（`remainingTicks` 最小）優先抵扣。
   - 護盾容量扣至 0 時立即破碎化為光屑，穿透傷害繼續扣減下一面護盾或本體 HP。

---

### 3.3 罡氣破防值 (Poise / Break Gauge) 攻防體系與五行吸收回血機制

結合《最終幻想 7 重製版》（FF7R）、《隻狼》（架勢值）與暗黑道門世界觀，將戰鬥核心由單純的「血量扣減」升級為具備爆發爽度與團隊配合的**「罡氣破防條 (Poise / Break Gauge)」**體系：

```
┌────────────────────────────────────────────────────────┐
│  戰鬥實體狀態儀表 (氣血 HP 與 罡氣破防條 Poise)        │
├────────────────────────────────────────────────────────┤
│  ❤️ 氣血生命 (HP):        [████████████████    ] 650/1000 │
│  ⚡ 罡氣破防條 (Poise):    [████████            ]  40/100  │
└────────────────────────────────────────────────────────┘
```

#### 1. 敵方破防機制 (Boss / 菁英戰的爆發節奏)
* **未破防階段 (Poise > 0，罡氣護體中)**：
  - **傷害折減**：敵怪承受的所有攻擊傷害**折減為 1/4 ~ 1/3（約 25% ~ 33% 輸出）**。怪物血條依然有扣減反饋，不會像傳統護盾那樣毫無打擊感。
  - **霸體護身**：**100% 免疫硬控**（眩暈、定身、擊退無效），蓄力意圖（1500ms 大招）不會被常規招式打斷。
  - **削韌積累**：每次受擊時，依招式與武器的【削韌值 (Poise Damage)】扣減破防條。重型武器（巨斧/戰錘）與特定破罡道術具備高削韌加成。
* **破防瞬間 (Poise = 0，力竭失衡期)**：
  - **大招中斷**：若敵方正在蓄力大招，強制中斷並清除意圖！
  - **全額爆發輸出 (1:1 傷害)**：傷害折減解除，所有攻擊恢復 **100% 全額輸出（甚至觸發暴擊增傷）**，帶來極致的「集火爆發期 (Burst Window)」！
  - **全面受控**：眩暈、凍結、麻痺等控場效果 **100% 生效**。
  - **持續時間**：進入持續 **6 ~ 8 秒 (12~16 Ticks) 的力竭期**。
* **罡氣復甦 (Poise Recovery，防無限連暈)**：
  - 力竭期結束後，敵方的破防條**自動回滿**，重新獲得霸體與減傷保護，自然防止無腦無限連暈！

#### 2. 怪物分層機制 (保證 DRPG 小怪清怪流暢度)
* **① 普通小怪 (Trash Mobs，野鼠/骷髏/陰魂)**：
  - **破防值極低 (如 20~30 點)**：戰士一記重劈或刺客一套連招直接當場破防秒殺，戰鬥時間維持在 **5~10 秒內快速清場**，確保地牢暗雷探索不拖沓。
* **② 特殊/持盾精英 (Elite / Shielded，金甲古屍/重裝道兵)**：
  - **中度破防值 (如 80~120 點)**：需全隊配合集火破罡，戰鬥時間約 15~25 秒。
* **③ 樓層首領 Boss (如宋天衡、深淵畸變首領)**：
  - **厚重破防值 (如 300~500 點)**：必須經全隊多輪技能配合打空破防條，觸發力竭進入爆發輸出窗口，戰鬥時間約 45~90 秒。

#### 3. 我方隊伍受傷與破防機制 (魂系團隊防守與救援)
我方角色依據職業定位擁有專屬的【罡氣架勢條】：
* **未破防時 (Poise > 0，罡氣尚存)**：
  - **前排肉盾 (鐵牛 - 重甲/開不動明王)**：天生擁有高額架勢上限（150~200 點），身具霸體，普通攻擊打在身上不會打斷施法或嘲諷。
  - **後排脆皮 (法師/醫修)**：架勢上限較低（30~40 點），被刺客近身容易被快速破罡。
  - 承受傷害依護甲正常扣 HP。
* **我方被打破防瞬間 (Poise = 0，失衡力竭)**：
  - 角色重心失衡，陷入 **3 ~ 4 秒力竭狀態**（無法出招、無法閃避格擋，受到傷害提升 30%~40%）。
  - 戰報警報：`⚠️💥【罡氣潰散】鐵牛 被首領狂暴猛擊破開空門，身形失衡陷入眩暈力竭！受到傷害提升 40%！`
* **隊友援護與架勢回復**：
  - **隊友救場**：醫修施展【回春金剛符】或力士施展【不動明王】，可**瞬間拉滿隊友架勢條，強制解除失衡力竭狀態**！
  - **自然恢復**：未被破防狀態下，脫離受擊 2~3 秒後，架勢條以每秒 20% 的速度快速自然回滿。

#### 4. 五行法術屬性與吸收回血機制 (Elemental Absorption & Healing)
針對五行法術與陰煞傷害（`METAL` 金、`WOOD` 木、`WATER` 水、`FIRE` 火、`EARTH` 土，以及陰煞 `CORRUPT`）：
- 穿透物理護甲，直接檢驗目標的五行抗性。
- 抗性數值 `resistance` 可透過裝備、丹藥、符籙與 Buff 堆疊超越 1.0 (100%)。
- **抗性折算與逆流回血契約**（實作於 `CombatRules.applyResistance`）：
  ```java
  public static ResistanceResult applyResistance(int rawDamage, DamageType type, double resistance) {
      if (rawDamage <= 0) return new ResistanceResult(0, 0);
      
      // 1. 物理傷害：依破防狀態 (未破防 1/4~1/3，已破防 1:1) 結算，不走元素抗性回血
      if (type == DamageType.PHYSICAL) {
          int finalDmg = Math.max(1, rawDamage);
          return new ResistanceResult(finalDmg, 0);
      }
      
      // 2. 五行法術/陰煞傷害：若抗性 > 1.0 (100%)，超額部分轉化為氣血回復 (順成人，逆成仙)
      if (resistance > 1.0) {
          double absorbRatio = resistance - 1.0; // 例如火抗 1.25 -> 25% 吸收回血
          int healAmount = (int) Math.round(rawDamage * absorbRatio);
          return new ResistanceResult(0, healAmount); // 承受 0 點傷害，轉為獲得治療
      } else {
          int finalDmg = (int) Math.round(rawDamage * Math.max(0.0, 1.0 - resistance));
          return new ResistanceResult(finalDmg, 0);
      }
  }
  ```
- **經典戰術套路支援**：允許玩家透過將隊員特定抗性堆過 100%，由隊友朝其丟對應屬性 AOE（如朝火抗 125% 的隊友丟火球），一邊灼燒敵人一邊為隊友回復氣血！克制屬性攻擊還能對敵方破防條造成雙倍削韌！

---

### 3.4 控場分級與全動作入口門控 (Control Hierarchy & Action Gates)

所有控場狀態統一納入 `ActiveBuff`，不再使用零散的 `stunnedUntil` 欄位。控場判定全面覆蓋所有動作入口：

| 控場類別 | 判定條件 | 受影響之動作入口 | 行動反饋與戰報 |
| :--- | :--- | :--- | :--- |
| **強控 (STUN / FROZEN)** | `isControlled()` | 1. 普通攻擊輪次跳過<br>2. 施法吟唱中斷 (`cancelCast`)<br>3. 敵方蓄力意圖清除 (`clearIntent`)<br>4. 同伴 Gambit AI 自動暫停出招 | `💫 [目標] 身陷眩暈/凍結，無法行動！` |
| **封咒 (SILENCE)** | `isSilenced()` | 1. 丹田封閉，禁止念咒施展主動道法 (`MAGIC`/`SPELL`)<br>2. 物理拳腳、兵刃套路與道具使用不受限制 | `🤐 [目標] 丹田真言受制，無法引導道法！` |
| **破冰暴擊 (Shatter)** | 身處 `FROZEN` 狀態 | 受物理鈍器或重擊時必定 100% 暴擊，承受 1.5 倍傷害，並提前破冰解除凍結。 | `❄️💥 破冰碎裂！受到重擊額外震傷！` |
| **補甲解控 (Armor Cleanse)** | 施加帶有護甲之 Buff | 若目標處於眩暈/定身狀態，施加護甲時強制清除控場並站起。 | `🛡️✨ 罡氣加身，震開控制重整旗鼓！` |

---

### 3.5 全域九大靈氣屬性矩陣與「屬性圖示聚合」極簡 HUD 呈現契約

為徹底杜絕「技能名稱太長擠爆戰鬥卡片」之痛點，並解決道門術法中「風刃、九霄神雷、幽冥暗煞」歸屬不明之問題，本系統確立**「九大天道靈氣屬性 (The 9 Great Elements)」**與**「屬性圖示聚合 (Element-Icon Aggregation)」**兩大最高契約：

#### 1. 全域九大天道靈氣屬性擴充矩陣 (The 9 Daoist Elements)
將原本純粹的五行擴充為完整的道門修仙元素體系（對齊代碼中已存在之 `DamageType`）：

| 靈氣屬性 | 對應代碼 (`DamageType`) | 經典天道圖示 | 代表經典招式 (需同步校準技能 JSON) | 吸收/抗性特性 |
| :--- | :--- | :---: | :--- | :--- |
| **金 (Metal)** | `METAL` / `SLASH`/`PIERCE` | 🪙 | 白虎破罡劍、金石重甲、太乙飛劍 | 提升鋒銳穿透與物理防禦 |
| **木 (Wood/Poison)**| `WOOD` / `POISON` | 🌿 / 🧪 | 春風化雨、玄陰屍毒、千株萬毒引 | 草木回春 (HoT) 或 劇毒侵蝕 (DoT) |
| **水/冰 (Water/Ice)**| `WATER` / `ICE` | 💧 / ❄️ | 玄冰聚煞引、九幽玄冰刺、寒潭淨心 | 附帶減速與碎冰暴擊 (Shatter) |
| **火 (Fire)** | `FIRE` | 🔥 | 三昧真火、烈陽焚身咒、朱雀燎原 | 灼燒 (DoT) 與火煞逆流回血 |
| **土 (Earth)** | `EARTH` / `BLUNT` | ⛰️ | 不動明王暗金罡、泰山壓頂、土相厚土壁 | 轉化為極厚物理護甲與架勢韌性 |
| **風 (Wind)** | `WIND` / `SONIC` | 🌪️ / 💨 | 狂風絕息斬、裂空風刃、天罡神風步 | 提升攻速身法 (Haste) 與閃避率 |
| **雷 (Thunder)** | `LIGHTNING` | ⚡ | 神霄派·九天應元雷訣、紫霄神雷、霹靂雷印 | 附帶痙攣麻痺 (Paralyze) 與打斷 |
| **陰/煞 (Dark/Chaos)**| `DARK` / `CORRUPT` | 🌑 / 👁️ | 太陰幽冥劍、古神囈語、萬魂噬血 | 陰煞腐蝕與直接侵蝕道心 SAN 值 |
| **陽/聖 (Holy/Sun)** | `HOLY` / `LIGHT` | ☀️ / ✨ | 道門金光神咒、純陽真火、九品蓮花護元 | 辟邪驅散 (Cleanse) 與免死護盾 |

> [!IMPORTANT]
> **相應技能資料校準提示**：  
> 既有技能庫中之 `thunder_strike` 須統一對齊 `LIGHTNING (⚡)`；`storm_blade` (狂風刀法) 須校準為 `WIND (🌪️)`；`spell_corpse_poison` 校準為 `POISON (🧪)`；`taiyin_sword` 校準為 `DARK (🌑)`。後續新增風雷道法一律依此矩陣歸類。

#### 2. 「屬性圖示聚合 (Element-Icon Aggregated HUD)」極簡呈現規則
戰鬥卡片上**嚴格禁止顯示冗長的技能全名**（如「玄陰屍毒」、「萬魂噬血」），永遠以**「標準屬性圖示 + 聚合數值」**極簡展現：

```
┌──────────────────────────────────────────────┐
│ 💀 畸變大師兄·宋天衡 (BOSS)                  │
│ HP: 650/1000  [████████████████░░░░]         │
│ 罡氣: 40/100  [████████░░░░░░░░░░░░]         │
├──────────────────────────────────────────────┤
│ 狀態欄: [🧪 54/s x3] [🔥 28/s x2] [⚔️ +35%]   │ ── 極簡標準圖示列！
└──────────────────────────────────────────────┘
```

1. **同屬性 DoT 傷害自動累加**：
   - 無論敵方身上中了幾種毒，畫面上只有一個 `[🧪 中毒圖示]`！
   - 數值直接合併為每秒總扣血量：`[🧪 54/s x3 (12s)]`（清楚傳達總層數與總扣血）。
   - 灼燒同理：多個火系技能命中時，統一聚合為 `[🔥 28/s x2 (8s)]`。
2. **同屬性 Buff 增益自動累加**：
   - 物理攻擊增益統一顯示為 `[⚔️ +35% (18s)]`。
   - 身法風系加速統一顯示為 `[💨 +25% (14s)]`。
   - 護盾統一顯示為 `[🛡️ 120 (25s)]`。
3. **懸浮明細卡 (Hover Tooltip)**：
   - 平時保持介面整潔乾淨不破版。
   - 滑鼠懸停於 `[🧪 54/s]` 時，展開來源明細清單：
     ```
     【劇毒侵蝕 (3 層)】每秒造成 54 點陰煞傷害
     ├ 來源 1：玄陰屍毒 (-14/s, 剩餘 8s)
     ├ 來源 2：千蛛萬毒 (-20/s, 剩餘 12s)
     └ 來源 3：墨沼腐毒 (-20/s, 剩餘 5s)
     ```

---

### 3.6 前後端資料流與雙舞台視覺投影契約 (WebSocket DTO & Presentation)

嚴格遵守 `UI_PAGE_STRUCTURE.md` 規範，解決資訊遮蔽問題：

1. **統一狀態 DTO 結構 (`ActiveBuffDto`)**：
   ```java
   public record ActiveBuffDto(
       String id,
       String name,
       String icon,
       String category,      // SHIELD, HOT, DOT, STAT_MODIFIER, CONTROL
       String effectType,    // POISON, HASTE, STUNED...
       String element,       // METAL, WOOD, WATER, FIRE, EARTH, WIND, LIGHTNING, DARK, HOLY
       int value,            // 護盾值或每跳數值
       int stacks,           // 當前層數
       int remainingSeconds, // 剩餘秒數 (常駐為 -1)
       boolean isPermanent,  // 是否為常駐陣法
       String description    // 詳細說明
   ) {}
   ```
2. **DTO 派發管道**：
   - `PartyMemberViewDto`：增加 `List<ActiveBuffDto> activeBuffs`，隨 `DRPG_STATE` 同步推送。
   - `BattleEnemyViewDto`：增加 `List<ActiveBuffDto> activeBuffs`，支援敵怪多重狀態顯示。
3. **前端雙舞台呈現與韌性護欄 (Battle Presentation & Resilience)**：
   - **戰鬥主舞台 (`battle-panel.js` & `party-hud-panel.js`)**：
     - 隊員卡片與敵怪卡片渲染**屬性聚合圖示**（圖示、聚合每秒數值/加成、層數、秒數）。
     - 護盾以半透明天藍色進度條疊加覆蓋在生命條上（Shield Overlay Bar）。
     - 罡氣破防條呈現金色/暗藍色能量條，破防時觸發 `💥 失衡破防` 動態特效。
   - **1600×900 角色道基詳細面板 (`party-equipment-tab.js` - `C` 鍵)**：
     - 在右欄下方新增專屬「☯️ 道身加持與狀態 (Active Auras & Buffs)」展示區塊。
     - 清楚列出常駐陣法加成與作用中狀態卡片，字級嚴格 $\ge 13\text{px}$，避免文字擠壓破版。
   - **非阻塞與快照權威 (Snapshot Authority & Reduced-Motion 護欄)**：
     - **快照優先**：前端永遠以 DTO 快照作為權威狀態來源，動畫或粒子僅為增量演出。即便連線抖動或事件遺失，下一輪 DTO 快照即可瞬時自動校正校準狀態。
     - **無障礙支援**：完整相容 `@media (prefers-reduced-motion)`，在偏好減少動畫時取消破防卡片劇烈震動與脈衝，保留清晰靜態圖示與數值。

---

## 4. 第一階段 (Phase 1 MVP) 聚焦收斂之狀態與技能矩陣

本階段嚴格聚焦於 6 款經典狀態，保證 **100% 具備 JSON 設定與 Java 結算**：

```
┌────────────────────────────────────────────────────────────────────────┐
│                   太陰地宮 Phase 1 核心狀態矩陣                         │
├──────────────────┬───────────┬──────┬───────────────┬──────────────────┤
│ 狀態代碼          │ 名稱/圖示 │ 分類 │ 機制說明      │ 配置技能來源     │
├──────────────────┼───────────┼──────┼───────────────┼──────────────────┤
│ buff_fudo_myoo   │ 不動明王⚡│SHIELD│ 30% MaxHP 護盾│ class_warrior_   │
│                  │           │      │ 減傷 30%      │ iron_wall        │
├──────────────────┼───────────┼──────┼───────────────┼──────────────────┤
│ debuff_corpse_   │ 玄陰屍毒🧪│ DOT  │ 每2秒14點陰煞 │ spell_corpse_    │
│ poison           │           │      │ 傷害，疊加5層 │ poison           │
├──────────────────┼───────────┼──────┼───────────────┼──────────────────┤
│ buff_spring_rain │ 春風化雨🌿│ HOT  │ 每2秒恢復18點 │ class_cleric_    │
│                  │           │      │ 氣血，持續16秒│ spring_rain      │
├──────────────────┼───────────┼──────┼───────────────┼──────────────────┤
│ buff_taiyin_     │ 天罡疾行💨│ STAT │ 攻擊間隔縮短  │ item_speed_charm │
│ haste            │           │      │ 25% (SPEED_PCT│ (符籙/被動)      │
├──────────────────┼───────────┼──────┼───────────────┼──────────────────┤
│ debuff_sunder_   │ 破罡裂甲💔│ STAT │ 護甲防禦降低  │ skill_badao_     │
│ armor            │           │      │ 35% (DEF_PCT) │ sunder           │
├──────────────────┼───────────┼──────┼───────────────┼──────────────────┤
│ debuff_stun      │ 紫霄雷痹💫│CONTRO│ 無法行動/中斷 │ spell_thunder_   │
│                  │           │  L   │ 吟唱，持續4秒 │ strike           │
└──────────────────┴───────────┴──────┴───────────────┴──────────────────┘
```

---

## 5. 分階段實施路線圖與驗收標準 (Implementation Roadmap & DoD)

### Phase 1: 核心契約、生命週期與屬性/吸收機制落地 (P0/P1)
- [ ] **1.1 修復生命週期契約**：
  - 在 `ActiveBuff` 實裝 `PERMANENT_DURATION = -1`，重構 `isExpired()` 與 `decrementTick()`。
  - 驗證陣法孔位 Buff（`remainingTicks = -1`）歷經 100 Ticks 永不消退。
- [ ] **1.2 落地 WoW 式刷新策略**：
  - 在 `BuffSettlementService.applyBuff` 實裝同名 Buff 重置時間、護盾取較大值、堆疊層數累加。
- [ ] **1.3 屬性修正、罡氣破防條 (Poise / Break) 與五行吸收結算**：
  - 在 `PartyMember.java` 與 `BattleEnemy.java` 串接 `STAT_MODIFIER`（`DEF_PCT`, `ATK_PCT`, `SPEED_PCT`）。
  - 引入【罡氣破防條 (Poise Gauge)】：未破防時傷害折減為 1/4~1/3 且免疫硬控；破防時觸發大招打斷、力竭全額 1:1 爆發與控場生效；力竭後自動復甦。
  - 實裝我方隊友架勢條與破防力竭懲罰，支援隊友補盾/回春瞬間拉滿架勢解除失衡。
  - 實裝五行法術抗性 $>100\%$ 逆流回血機制，支援朝高火抗隊友丟火球補血之戰術套路。

### Phase 2: 控場門控與技能中斷收斂 (P1)
- [ ] **2.1 控場介面收斂**：
  - 在 `Buffable` 擴充 `isControlled()` 與 `isSilenced()` 便捷查詢。
  - 改造 `BattleEnemy.isStunned()` 委託 `Buffable.isControlled()`。
- [ ] **2.2 全動作入口中斷**：
  - 在 `DrpgCombatLoop` 攻擊檢定處加入控場阻斷，打斷吟唱並廣播提示。
  - 在敵方意圖預告（1500ms 蓄力）加入眩暈打斷清除機制。

### Phase 3: DTO 補全、屬性圖示聚合與雙舞台視覺投影 (P1)
- [ ] **3.1 建立正規化 `ActiveBuffDto`**：
  - 擴充 `element` 靈氣屬性欄位，將活動狀態快照推送至前端。
- [ ] **3.2 戰鬥卡片「屬性圖示聚合 (Element-Icon Aggregated HUD)」實裝**：
  - 杜絕長技能名稱，戰鬥卡片以 `[🧪 -54/s x3]`、`[🔥 -28/s x2]`、`[⚔️ +35%]` 極簡圖示展現，同屬性自動加總。
  - 滑鼠懸停顯示明細來源（技能名、各別每跳傷害與剩餘秒數）。
  - 在血條上方呈現半透明天藍色護盾條，破防值以金色/暗藍色能量條呈現。
- [ ] **3.3 補全 1600×900 角色道基詳細面板**：
  - 在 `party-equipment-tab.js` 補上「☯️ 道身加持與狀態」區塊，字級嚴格 $\ge 13\text{px}$，安全轉義。
- [ ] **3.4 相應技能資料 JSON 靈氣屬性校準**：
  - 校準 `thunder_strike.json` 為 `LIGHTNING (⚡)`、`storm_blade.json` 為 `WIND (🌪️)`、`spell_corpse_poison.json` 為 `POISON (🧪)`、`taiyin_sword.json` 為 `DARK (🌑)`，完成資料驅動歸類。

### Phase 4: Gambit 戰術方針 AI 反制與全自動化測試 (P1/P2)
- [ ] **4.1 擴充戰術條件 (`TacticsCondition`)**：
  - 新增 `ALLY_HAS_DEBUFF`、`ALLY_IS_CONTROLLED`、`SELF_LACKS_SHIELD`。
  - AI 具備防呆機制：狀態剩餘時效 $> 3$ 秒時不重複施放。
- [ ] **4.2 自動化整合測試 (`BuffDebuffComprehensiveTest`)**：
  - 涵蓋：常駐陣法不消退、短時間護盾優先吸收、破防增傷、五行抗性吸收回血、眩暈中斷施法、AI 自動解毒 6 大場景。
  - 全專案測試 100% 綠燈通過。
- [ ] **4.3 文檔治理同步**：
  - 同步更新 [`FUTURE_IMPROVEMENTS.md`](./FUTURE_IMPROVEMENTS.md) §19.3 `BUFF-01` 狀態為 `✅ 已完成`。
  - 於 [`CHANGELOG.md`](../../CHANGELOG.md) 記錄本次重構版本歷程。

---
本規劃書為《太陰地宮》Buff 系統之最高實施標準，各模組可依 Phase 1 至 Phase 4 漸進實作驗收。
