# WoW 風格 Buff / Debuff（狀態效果）體系架構設計與實作規劃

> **版本**：v1.1 (已吸納架構 Review 決策)  
> **更新日期**：2026-09-24  
> **狀態**：已定案，準備進入 Phase 9.5 實作  
> **核心哲學**：機制運行 + 資料驅動 + 純粹測試 (Pure Mechanism + Data-Driven + 100% Testable)

---

## 1. 背景與現狀痛點

### 1.1 現狀觀察（如實機戰鬥日誌截圖所示）
在現行 DRPG 戰鬥循環中，當同伴（如百草丹修・凌霜）觸發戰術方針施放【金光辟邪護體】（為前衛肉盾・鐵牛施加辟邪護盾）後：
1. 戰術方針條件（如 `敵方存活數量 >= 2` 或 `敵方存在首領`）在整場戰鬥中持續成立。
2. 系統尚未具備**實體狀態追蹤器（Status Tracker）**，無法感知受法目標身上是否已具備該護盾/增益。
3. 導致凌霜在真元充足且招式冷卻就緒時，於後續回合**重複對鐵牛施放相同的【金光辟邪護體】**，造成無效施法、真元浪費與行動節奏卡頓。

### 1.2 目標願景
借鑒《魔獸世界》（World of Warcraft, WoW）成熟、通用且極具戰術深度的狀態系統（Aura / Buff / Debuff System），建立一套支援**心跳計數跳算（Heartbeat Tick）、護盾吸收、屬性修飾、控場限制、持續時間與堆疊判定**的標準化機制。

---

## 2. WoW 風格的狀態刷新與堆疊規則 (Stacking & Refresh Rules)

依照玩家指定並參照 WoW 經典規範：

### 2.1 相同技能 / 同名狀態（Same Spell / Same Buff ID）
* **非層數類狀態（護盾 Shield、屬性加成 Stat Buff、持續治療 HoT 等）**：
  * **刷新持續時間（Refresh Duration）**：若目標已擁有該狀態，手動補施時重置剩餘持續時間至最大值（`remainingTicks = durationTicks`）。
  * **護盾刷新原則**：取當前剩餘護盾與新護盾中的較大值（`Math.max(currentShield, newShield)`），**不無限累加厚度**，避免數值無限膨脹。
  * **戰術方針自動防呆（Gambit Recast Prevention）**：
    * 當戰術方針（Gambit AI）評估目標隊員時，若目標身上**已存在同 ID 且未過期/護盾未破的狀態，直接判定條件不滿足**，跳至下一條優先級規則（例如執行普通攻擊或其他輔助道術），**絕不在有效期內盲目重複施放**。
* **可堆疊類狀態（Stacking Buff / Debuff，如毒素 Poison、流血 Bleed、狂暴 Frenzy）**：
  * 若當前層數未達上限（`stacks < maxStacks`），層數遞增（`stacks++`）並刷新持續時間。
  * 若已達最大層數，維持上限並僅刷新持續時間。

### 2.2 不同技能 / 不同狀態（Different Spells / Different Buff IDs）
* **完全共存並疊加（Coexist & Stack）**：
  * **多重護盾疊加吸收（剩餘持續時間較短者優先消耗 Shortest Duration First）**：
    * 凌霜施加的【金光辟邪護體】（辟邪護盾 45 點，持續 40 Ticks = 20s）與鐵牛自身施展的【不動明王】（金身護盾 54 點，持續 20 Ticks = 10s）可**同時並存於鐵牛身上**。
    * 當鐵牛受到傷害時，系統依照「剩餘持續時間短者優先（Shortest Duration First）」佇列依序吸收扣除，避免較快過期的護盾被浪費。
    * *範例*：不動明王剩餘 10 Ticks，辟邪護盾剩餘 35 Ticks。鐵牛受到 60 點傷害時，先由不動明王吸收 54 點（不動明王護盾破裂移除），剩餘 6 點由辟邪護盾吸收（辟邪護盾剩餘 39 點繼續保留），鐵牛本體氣血無損！
  * **多重屬性增益共存**：
    * 主角道門陣法加持（防禦 +10%）與丹修祝福丹氣（防禦 +15%）可疊加生效，總防禦提升 +25%。

---

## 3. 領域模型設計 (Domain Model)

### 3.1 時間模型：基準心跳計數器（Heartbeat Ticks）
* 戰鬥主循環單次循環（`500ms`）定義為 **1 個 Tick**。
* 不使用易飄移、不可測試且難以存檔的絕對毫秒時間戳，全部採用純整數 `Ticks` 計數。
* 轉換公式：`秒數 = ticks * 0.5`，`ticks = 秒數 * 2`。
* 單元測試可精確呼叫 `tick()` N 次，100% 驗證結算次數與過期時點。

### 3.2 狀態類型分類 (`BuffCategory`) 與語意標籤 (`EffectType`)
* **`BuffCategory`**（引擎底層結算行為分類）：
```java
public enum BuffCategory {
  SHIELD,         // 傷害吸收護盾 (如: 金光辟邪護體, 不滅金身)
  HOT,            // 週期性治療 Heal over Time (如: 回春靈霧, 甘露清音)
  DOT,            // 週期性傷害 Damage over Time (如: 幽冥煞毒, 烈火焚身, 裂傷)
  STAT_MODIFIER,  // 屬性增益/減益 (如: 加防、加攻、加氣血上限、降速、破甲)
  CONTROL         // 強制控制 (如: 暈眩 Stun, 定身 Anchored, 混亂 Confused)
}
```
* **`EffectType`**（現存領域語意枚舉，用於戰術方針條件判斷與日誌展示，如 `EffectType.POISON`、`EffectType.SHIELD`、`EffectType.STUNED`）。

### 3.3 實體共用介面 (`Buffable`)
讓 `PartyMember` 與 `BattleEnemy` 統一實作此介面，避免重複撰寫護盾扣抵與狀態維護代碼：

```java
public interface Buffable {
  String getId();
  String getName();
  boolean isAlive();
  int getHp();
  int getMaxHp();
  void setHp(int hp);
  
  List<ActiveBuff> getActiveBuffs();
  boolean hasActiveBuff(String buffId);
  ActiveBuff getActiveBuff(String buffId);
  void addBuff(ActiveBuff buff);
  void removeBuff(String buffId);
  int absorbShieldDamage(int incomingDmg);
}
```

### 3.4 狀態實體 (`ActiveBuff`)
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveBuff {
  private String id;                    // 狀態 ID (如: buff_gold_shield)
  private String sourceSkillId;         // 來源招式 ID (如: class_cleric_bless)
  private String sourceCasterId;        // 施法者 ID (如: m-ling_shuang)
  private String name;                  // 狀態名稱 (如: 金光辟邪護體)
  private String icon;                  // 圖標符號 (如: 🛡️, 🌿, 🧪, ⚡)
  private BuffType type;                // BUFF / DEBUFF / NEUTRAL
  private BuffCategory category;        // SHIELD / HOT / DOT / STAT_MODIFIER / CONTROL
  private EffectType effectType;        // 語意類型 (如 SHIELD, POISON, STUNED)
  
  // 心跳計數生命週期 (1 Tick = 500ms)
  private int durationTicks;            // 原始總持續 Ticks (如 20s = 40 ticks)
  private int remainingTicks;           // 剩餘持續 Ticks
  
  // 週期性跳算 (Tick Interval)
  private int tickIntervalTicks;        // 跳算週期 (如 2s 一跳 = 4 ticks)
  private int ticksSinceLastTick;       // 距上次跳算的計數累加
  
  // 數值與疊加
  private int value;                    // 當前容量 (護盾剩餘吸收值、每跳基礎治療/傷害)
  private int stacks;                   // 當前層數 (預設 1)
  private int maxStacks;                // 最大層數 (如 5 層毒)
  
  // 屬性百分比/固定值修正
  @Builder.Default
  private Map<String, Double> statModifiers = new HashMap<>(); 
  
  public boolean isExpired() {
    return remainingTicks <= 0 || (category == BuffCategory.SHIELD && value <= 0);
  }

  public void decrementTick() {
    if (this.remainingTicks > 0) {
      this.remainingTicks--;
    }
    this.ticksSinceLastTick++;
  }

  public boolean shouldTickNow() {
    if (tickIntervalTicks <= 0) return false;
    return ticksSinceLastTick >= tickIntervalTicks;
  }

  public void resetTickCounter() {
    this.ticksSinceLastTick = 0;
  }
}
```

---

## 4. 資料驅動規格 (Data-Driven JSON Schema)

招式模板 JSON 檔案中的 `buff` 節點完全定義效果參數，不硬編碼數值：

### 4.1 護盾類招式範例 (`class_cleric_bless.json`)
```json
{
  "id": "class_cleric_bless",
  "name": "金光辟邪護體",
  "school": "CLERIC",
  "tags": ["CLASS", "BUFF", "SHIELD", "MANA"],
  "costs": { "mp": 35 },
  "mechanics": { "damage": 0 },
  "buff": {
    "id": "buff_gold_shield",
    "name": "金光辟邪護盾",
    "icon": "🛡️",
    "type": "BUFF",
    "category": "SHIELD",
    "effectType": "SHIELD",
    "durationSeconds": 20,
    "percentOfTargetHp": 0.25,
    "baseShield": 35,
    "maxStacks": 1
  }
}
```

### 4.2 持續治療類招式範例 (`class_cleric_spring_rain.json` - 春風化雨)
```json
{
  "id": "class_cleric_spring_rain",
  "name": "春風化雨",
  "school": "CLERIC",
  "tags": ["CLASS", "BUFF", "HOT", "MANA"],
  "costs": { "mp": 25 },
  "buff": {
    "id": "buff_spring_heal",
    "name": "春生靈息",
    "icon": "🌿",
    "type": "BUFF",
    "category": "HOT",
    "effectType": "HEALING",
    "durationSeconds": 10,
    "tickIntervalSeconds": 2,
    "healPerTick": 18,
    "healPercentPerTick": 0.05,
    "maxStacks": 1
  }
}
```

### 4.3 持續傷害類招式範例 (`spell_corpse_poison.json` - 屍毒)
```json
{
  "id": "spell_corpse_poison",
  "name": "玄陰屍毒",
  "school": "TAOIST",
  "tags": ["SPELL", "DEBUFF", "DOT"],
  "costs": { "mp": 20 },
  "buff": {
    "id": "debuff_corpse_poison",
    "name": "玄陰屍毒",
    "icon": "🧪",
    "type": "DEBUFF",
    "category": "DOT",
    "effectType": "POISON",
    "durationSeconds": 12,
    "tickIntervalSeconds": 2,
    "damagePerTick": 14,
    "maxStacks": 5
  }
}
```

---

## 5. 獨立狀態結算服務 (`BuffSettlementService`)

為避免 [DrpgCombatLoop.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java) 持續肥大，將所有狀態生命週期管理、HoT/DoT 結算、護盾抵扣抽離為 Spring Bean 服務：

```java
@Service
public class BuffSettlementService {
  /** 結算目標上所有 Buff 經過 1 個 Tick 的變化，並產生戰鬥日誌 */
  public List<String> processTicks(Buffable entity);

  /** 將新狀態施加於目標（實踐 WoW 相同刷新時間、不同共存疊加、護盾取較大值規則） */
  public void applyBuff(Buffable target, ActiveBuff newBuff);

  /** 遭受攻擊時執行多重護盾抵扣（Shortest Duration First） */
  public ShieldAbsorbResult absorbDamage(Buffable target, int incomingDmg);
}
```

在 `DrpgCombatLoop.java` 中每個心跳（500ms）：
```java
// 結算我方隊伍 Buff
for (PartyMember member : ctx.getParty().getMembers()) {
  if (member.isAlive()) {
    List<String> logs = buffSettlementService.processTicks(member);
    logs.forEach(l -> broadcastLog(player, ctx, l));
  }
}
// 結算敵方怪物 Buff
for (BattleEnemy enemy : ctx.getEnemies()) {
  if (enemy.isAlive()) {
    List<String> logs = buffSettlementService.processTicks(enemy);
    logs.forEach(l -> broadcastLog(player, ctx, l));
  }
}
```

---

## 6. 戰術方針條件庫 (Gambit AI) 擴充與防呆

1. **引擎預設防呆原則**：
   * 所有具有 `SHIELD` 或 `BUFF` 標籤的招式，在戰術方針評估時，**若目標已擁有相同 `buffId` 且未過期，直接判定條件不滿足**，自動略過該規則，防止重複連放。
2. **條件庫擴充 (`TacticsCondition`)**：
   * `TARGET_LACKS_BUFF`：目標缺少指定狀態。
   * `TARGET_HAS_BUFF`：目標擁有指定狀態。
   * `BUFF_TIME_LESS_THAN`：目標指定狀態剩餘持續時間 < X 秒。

---

## 7. 前端 UI 與資料傳輸 (DTO & Presentation)

### 7.1 DTO 定義 (`ActiveBuffDto`)
```java
public record ActiveBuffDto(
    String id,
    String name,
    String icon,
    String type,        // BUFF / DEBUFF
    String category,    // SHIELD / HOT / DOT / STAT / CONTROL
    int remainingSeconds,
    int stacks,
    int value           // 當前護盾剩餘值或單次跳血量
) {}
```

### 7.2 前端呈現效果
* **同伴頭像與怪物血條下方**：渲染小膠囊徽章列：
  * `[🛡️ 45 (18s)]` 金光辟邪護體
  * `[⚡ 54 (8s)]` 不動明王金身
  * `[🌿 +18 (6s)]` 春生靈息
  * `[🧪 3層 (9s)]` 玄陰屍毒
* **Tooltip 懸浮浮窗**：滑鼠懸浮時呈現詳細數值、剩餘時間、效果說明與來源施法者。

---

## 8. 分階段實作路徑 (Implementation Roadmap)

1. **Step 1：領域模型與核心介面 (Domain Models)**
   * 建立 `BuffCategory`、`BuffType`、`ActiveBuff`。
   * 建立 `Buffable` 介面，讓 `PartyMember` 與 `BattleEnemy` 實作。
2. **Step 2：結算服務與戰術防呆 (Engine & Settlement)**
   * 實作 `BuffSettlementService`：
     - 同 Buff 刷新時間（護盾取 `Math.max`）。
     - 不同 Buff 共存疊加，護盾按剩餘持續時間升序吸收（Shortest Duration First）。
     - 週期性 HoT / DoT 跳算與過期移除。
   * 在 `DrpgCombatLoop` 與 `DrpgEnemyTacticsService` 加入防重複施放檢查。
3. **Step 3：招式資料驅動 (Data-Driven Configuration)**
   * 擴充技能 JSON 模板（辟邪護盾、不動明王、春生、屍毒等）。
4. **Step 4：純粹機制測試 (Mechanism Unit Tests)**
   * 建立 `BuffDebuffSystemTest`，驗證同技能刷新、多護盾短時間優先抵扣、HoT/DoT 跳算、戰術防呆。
5. **Step 5：前端 UI 視覺化 (UI Presentation)**
   * 戰鬥介面血條下方渲染動態 Buff 列表與 Tooltip。
