# 🔍 WoW 風格 Buff / Debuff 體系設計文件 Review

> **Review 對象**：[2026-09-24_wow_style_buff_debuff_system_design.md](file:///c:/Workspace/my_practice/practice_mud/docs/plans/2026-09-24_wow_style_buff_debuff_system_design.md)  
> **Review 日期**：2026-09-24  
> **Review 結論**：**設計方向正確，結構嚴謹**，但有 7 項建議需在實作前對齊

---

## ✅ 設計優點

| 面向 | 評價 |
| :--- | :--- |
| **WoW 規範對齊** | 堆疊/刷新規則完全符合 WoW 經典設計，同技能刷新時間 + 不同技能共存疊加的決策清晰 |
| **資料驅動哲學** | `buff` JSON 節點設計合理，招式定義與狀態效果解耦，Java 端不需為每個 Buff 寫特例邏輯 |
| **領域模型完整** | `ActiveBuff` 欄位覆蓋了護盾、HoT、DoT、屬性修飾與控場五大類，`isExpired()` 邊界條件明確 |
| **戰術 AI 防呆** | Gambit AI 自動跳過已存在的同 ID Buff 規則，直接解決凌霜重複施放護盾的原始痛點 |
| **分階段實作** | Phase 1~4 拆分合理，先解決核心痛點再逐步擴展 |

---

## 🔧 建議 1：`ActiveBuff` 時間模型應改為「回合制」而非「毫秒制」

> [!IMPORTANT]
> 這是最關鍵的架構建議，影響整個系統的結算正確性。

### 現狀問題
設計文件使用 `durationMs`、`remainingDurationMs`、`tickIntervalMs`、`nextTickTime` 等毫秒級欄位。然而觀察 [DrpgCombatLoop.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java) 的心跳是 `Thread.sleep(500)` 每 500ms 一次，且戰鬥節奏以「行動回合」為核心（每個角色有 `nextAttackTime` 控制出手間距）。

如果 Buff 用真實毫秒計時，會出現：
- **HoT/DoT 跳算不穩定**：500ms 心跳下，2000ms 的 tick interval 理論上每 4 次心跳跳一次，但 `Thread.sleep` 不精確，可能導致一場戰鬥中 HoT 跳了 4 次或 6 次
- **戰鬥暫停/卡頓穿透**：如果 JVM GC 導致心跳延遲 2 秒，回來後 Buff 會突然連跳多次或直接過期
- **存檔/讀檔困難**：毫秒時間戳在存檔後完全失效

### 建議方案
改為**回合計數器制（Turn-Based Counter）**：

```java
public class ActiveBuff {
    private int durationTurns;        // 總持續回合數 (如 5 回合)
    private int remainingTurns;       // 剩餘回合數
    private int tickIntervalTurns;    // 每 N 回合跳算一次 (如每 1 回合跳一次 HoT)
    private int turnsSinceLastTick;   // 距上次跳算的回合累積
    // ...
}
```

- 每個「心跳循環完成一輪我方+敵方行動」計為 1 回合
- HoT/DoT 的跳算以回合為錨點，完全可預測、可測試
- JSON 中的 `durationSeconds: 20` 改為 `durationTurns: 5`

---

## 🔧 建議 2：護盾吸收順序應明確定義在 `ActiveBuff` 層級

### 現狀問題
設計文件提到護盾吸收使用「先進先出（FIFO）或剩餘時間短者優先消耗」，但未做最終裁定。

### 建議方案
建議採用 **「剩餘時間短者優先（Shortest Duration First）」**，理由：
1. 這是 WoW 的實際行為——快過期的護盾先被消耗，避免它白白過期浪費
2. 可在 `PartyMember` 內以 `PriorityQueue<ActiveBuff>` 按 `remainingTurns` 排序
3. 在 JSON 中可追加一個 `priority` 欄位作為覆寫手段（特殊技能可宣告「最後消耗」）

```java
// PartyMember.java
public int absorbDamage(int rawDamage) {
    int remaining = rawDamage;
    // 按 remainingTurns ASC 排序，短期護盾先吃傷
    var shields = activeBuffs.stream()
        .filter(b -> b.getCategory() == BuffCategory.SHIELD && b.getValue() > 0)
        .sorted(Comparator.comparingInt(ActiveBuff::getRemainingTurns))
        .toList();
    for (ActiveBuff shield : shields) {
        if (remaining <= 0) break;
        int absorbed = Math.min(remaining, shield.getValue());
        shield.setValue(shield.getValue() - absorbed);
        remaining -= absorbed;
    }
    return remaining; // 剩餘穿透傷害
}
```

---

## 🔧 建議 3：`ActiveBuff` 應提取公共介面，讓 `PartyMember` 和 `BattleEnemy` 共用

### 現狀問題
設計文件提到 `ActiveBuff` 要同時掛載在 `PartyMember` 和 `BattleEnemy` 上。目前 [PartyMember.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/party/model/PartyMember.java) 有 783 行已非常龐大，[BattleEnemy.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/BattleEnemy.java) 有 169 行，兩者之間沒有共用介面。

### 建議方案
提取 `Buffable` 介面（或 `StatusEffectHolder`）：

```java
public interface Buffable {
    List<ActiveBuff> getActiveBuffs();
    boolean hasActiveBuff(String buffId);
    void addBuff(ActiveBuff buff);       // 含刷新/堆疊邏輯
    void removeBuff(String buffId);
    int absorbDamage(int rawDamage);     // 護盾吸收
    void tickBuffs();                    // 每回合結算
}
```

`PartyMember` 和 `BattleEnemy` 都實作此介面，Buff 結算邏輯寫一次，避免重複。

---

## 🔧 建議 4：`EffectType` Enum 與 `BuffCategory` 的關係需要釐清

### 現狀問題
專案中已存在 [EffectType.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/model/enums/EffectType.java)，包含 20+ 個 debuff（POISON、STUNED、FROZEN...）和 20+ 個 buff（SHIELD、HEALING、REGENERATION...）。新設計又引入了 `BuffCategory`（SHIELD、HOT、DOT、STAT_MODIFIER、CONTROL）和 `BuffType`（BUFF、DEBUFF、NEUTRAL）。

兩套系統若不整合，會造成語意衝突：
- `EffectType.SHIELD` vs `BuffCategory.SHIELD` — 哪個才是權威？
- `EffectType.POISON` 是否等同於 `BuffCategory.DOT`？
- 戰鬥引擎判斷「目標是否中毒」時，該查 `EffectType` 還是 `ActiveBuff.category`？

### 建議方案
1. 讓 `EffectType` 成為 `ActiveBuff.effectType`（精細效果標識），`BuffCategory` 作為「引擎行為分類」
2. 一個 `ActiveBuff` 同時持有兩者：`effectType = POISON`，`category = DOT`
3. 戰鬥引擎用 `category` 判斷結算行為，戰術 AI 用 `effectType` 判斷精確條件
4. 在 JSON 中：

```json
{
    "buff": {
        "effectType": "POISON",     // ← 精細效果
        "category": "DOT",          // ← 引擎行為
        "type": "DEBUFF"
    }
}
```

---

## 🔧 建議 5：Buff 結算應獨立為 `BuffSettlementService`，不要塞進 `DrpgCombatLoop`

### 現狀問題
設計文件把 Buff 結算流程放在 `DrpgCombatLoop` 的心跳循環中。但目前 [DrpgCombatLoop.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java) 已有 456 行，且職責包含：走火入魔判定、戰術 AI 觸發、傷害計算、獎勵結算。再加入 Buff Tick 結算，會讓這個類持續膨脹。

### 建議方案
新增 `BuffSettlementService`（或 `BuffEngine`）：

```java
@Service
public class BuffSettlementService {
    /** 每回合結算所有實體的 Buff 生命週期 */
    public List<BuffEvent> processBuffTick(BattleContext ctx) { ... }
    
    /** 施加新 Buff（含刷新/堆疊判定） */
    public BuffApplyResult applyBuff(Buffable target, ActiveBuff buff) { ... }
    
    /** 受傷時護盾吸收 */
    public ShieldAbsorbResult absorbDamage(Buffable target, int rawDamage) { ... }
}
```

`DrpgCombatLoop` 在心跳循環中只需一行：
```java
var buffEvents = buffSettlementService.processBuffTick(ctx);
buffEvents.forEach(e -> broadcastLog(player, ctx, e.toLogMessage()));
```

---

## 🔧 建議 6：JSON Schema 中 `shieldPercentOfTargetHp` 應考慮公式化

### 現狀問題
設計文件中 `class_cleric_bless.json` 使用了 `shieldPercentOfTargetHp: 0.25` 和 `baseShield: 35`。但目前程式碼中 [DrpgCombatLoop.java L384](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java#L384) 的護盾計算是硬編碼的：

```java
int shieldAmount = Math.max((int) (targetMaxHp * 0.25), baseAmount);
```

### 建議方案
護盾值計算公式應完全由 JSON 驅動，建議改為：

```json
{
    "buff": {
        "shieldFormula": "max(baseShield, floor(targetMaxHp * percentOfTargetHp))",
        "baseShield": 35,
        "percentOfTargetHp": 0.25
    }
}
```

實作時不需要真的寫公式解析器，但要確保：
```java
int shield = Math.max(buffDef.getBaseShield(), 
                      (int)(target.getMaxHp() * buffDef.getPercentOfTargetHp()));
```
所有數值都從 JSON 讀取，不在 Java 中寫死 `0.25` 或 `0.30`。

---

## 🔧 建議 7：Phase 4 測試設計應強調「機制測試」而非「數值測試」

### 現狀問題
設計文件 Phase 4 列出的測試驗證項目很合理，但需要提醒：根據使用者的核心原則（**「機制運行 + 資料驅動 + 純粹測試」**），測試應驗證的是**機制行為**，測試資料則透過 Test Fixture 注入。

### 建議範例

```java
// ✅ Good — 測試「同 ID Buff 刷新持續時間」的機制
@Test
void sameBuff_shouldRefreshDuration_notStack() {
    var target = createTestMember();
    var buff = createTestBuff("test_shield", BuffCategory.SHIELD, 5/*turns*/, 100/*value*/);
    
    target.addBuff(buff);
    target.tickBuffs(); // 1 回合過去
    assertEquals(4, target.getActiveBuff("test_shield").getRemainingTurns());
    
    var refreshBuff = createTestBuff("test_shield", BuffCategory.SHIELD, 5, 80);
    target.addBuff(refreshBuff); // 再次施加
    
    assertEquals(1, target.getActiveBuffsByCategory(BuffCategory.SHIELD).size()); // 不堆疊
    assertEquals(5, target.getActiveBuff("test_shield").getRemainingTurns());     // 時間重置
    assertEquals(100, target.getActiveBuff("test_shield").getValue());            // 護盾取 max
}

// ❌ Bad — 測試寫死的遊戲數據
@Test
void lingShuangShield_shouldBe45() { // ← 不該測試具體遊戲數值
    assertEquals(45, 凌霜的護盾值);
}
```

---

## 📋 Review 總結與實作建議順序

| # | 建議 | 優先級 | 影響範圍 | 建議處理時機 |
| :---: | :--- | :---: | :--- | :--- |
| 1 | 時間模型改為回合制 | **P0** | `ActiveBuff` 所有時間欄位 | Phase 1 開始前必須決定 |
| 2 | 護盾吸收順序明確化 | **P1** | `absorbDamage` 演算法 | Phase 1 實作時 |
| 3 | 提取 `Buffable` 介面 | **P1** | `PartyMember` + `BattleEnemy` | Phase 1 實作時 |
| 4 | `EffectType` 整合 | **P1** | Enum 設計 + JSON Schema | Phase 1 開始前設計定案 |
| 5 | 獨立 `BuffSettlementService` | **P2** | `DrpgCombatLoop` 解耦 | Phase 2 實作時 |
| 6 | 護盾公式資料驅動 | **P2** | JSON Schema + 計算邏輯 | Phase 1 實作時順帶處理 |
| 7 | 測試設計原則 | **P2** | 測試檔案 | Phase 4 |

> [!TIP]
> **建議在開始 Phase 1 實作前，先更新設計文件中建議 1（回合制）和建議 4（EffectType 整合）的決策，因為這兩項會影響整個資料模型和 JSON Schema 的定義。**
