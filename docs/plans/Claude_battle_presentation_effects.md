# 戰鬥表現特效規劃（Battle Presentation / VFX）

> 作者：Claude｜日期：2026-10-05
> 目標：讓戰鬥區塊（`#battle-arena-panel`）不只是在訊息區顯示文字，而是能「看見」攻擊、受擊、格擋、閃避，並跳出依類型著色的傷害數字。

---

## 0. TL;DR（先講結論）

1. **最核心的問題不在前端特效，而在「資料」**：目前後端每次攻擊只產生一段 ANSI 文字（`broadcastLog(...)`），前端根本不知道「誰打了誰、用什麼武器、什麼屬性、是否暴擊／格擋、打了多少」。所以第一步一定是：**後端在戰鬥迴圈中額外產生結構化的 `BattleEvent`**，與文字 log 並行輸出。
2. **前端新增一層獨立的「特效層」(FX Layer)** 疊在戰場上（`pointer-events:none`），由一個 `BattleFxDirector` 消費事件並依「預設表 (Preset Table)」播放 CSS / Web Animations 動畫與浮動數字。**特效不寫進敵人卡片本身**，避免被現有的 full-rebuild 機制打斷。
3. **資料驅動**：武器類型 → 攻擊動畫、傷害屬性 → 顏色與法術特效、範圍形狀 (單體／一排／一列／全體) → 範圍動畫、判定結果 (HIT/CRIT/BLOCK/PARRY/DODGE/MISS) → 受擊反應。全部寫在一張對照表裡，日後新增技能只改資料。
4. **分 4 個階段做**，第 1 階段 (MVP) 就能看到浮動數字＋受擊／閃避／格擋反應，而且大部分判定資料其實已經存在（`DefenseResolver.DefenseOutcome`）。

---

## 1. 現況分析

### 1.1 後端：戰鬥結果只有字串

| 位置 | 行為 | 問題 |
|---|---|---|
| [DrpgCombatLoop.java#L299-L351](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java#L299-L351) | 隊員普攻：計算 `dmg`、決定武器 icon、`broadcastLog(icon + ... + dmg + " 點傷害！")` | 武器類型 `wt`、傷害值都只存在於字串中 |
| [DrpgCombatLoop.java#L357-L408](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java#L357-L408) | 敵方攻擊：透過 `DefenseResolver` 擲骰，取得 `DefenseResolution` | **已經有** `outcome`(HIT/CRIT/BLOCKED/PARRIED/DODGED/MISS)、`finalDamage`、`riposteDamage`，但最後也只送出 `combatLog` 字串 |
| [DrpgCombatLoop.java#L529-L666](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java#L529-L666) | `applySkillEffects`：治療／護盾／嘲諷／Buff／單體或 AOE 傷害 | 有 `skill.isAoe()`，但沒有「一排」的概念；技能沒有 `damageType` |
| [DrpgCombatLoop.java#L144-L159](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java#L144-L159) | Buff Tick（HoT / DoT） | `BuffSettlementService.processTicks` 只回傳 `List<String>` |
| [DrpgEnemyTacticsService.calculatePlayerDamage](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgEnemyTacticsService.java) | 隊員對敵人的傷害 | **沒有暴擊／未命中判定**，只有 ±10% 浮動 |
| [DefenseResolver.java#L25-L32](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DefenseResolver.java#L25-L32) | `DefenseOutcome` 列舉 | 可直接拿來當受擊反應的種類 ✅ |
| [BattleViewDto.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/dto/BattleViewDto.java) | 前端收到的戰鬥快照：`enemies`, `logs` | 只有「狀態快照」，沒有「發生了什麼事」 |

戰鬥迴圈每 **500ms** 一個 tick，tick 結尾呼叫 `stateBroadcaster.accept(player)` 推送 `DRPG_STATE`（[L410-L415](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java#L410-L415)）。

### 1.2 前端：只會重畫快照

- [mud-core.js#L76-L111](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/mud-core.js#L76-L111) 的 `handleServerMessage` 只認得 `DRPG_STATE / TEXT / ...`。
- [battle-panel.js](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/panels/battle-panel.js) 的 [`renderBattleArena`](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/panels/battle-panel.js#L15-L310)：
  - 有 **structureKey 機制**：結構不變時 in-place 更新 HP（好事，動畫不會被打斷）；
  - 但只要**有怪物死亡或排位改變就 full rebuild**（[L260-L272](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/panels/battle-panel.js#L260-L272)）→ 死亡怪物的卡片瞬間消失，目前**無法播死亡動畫**，掛在卡片上的動畫也會被砍掉。
- 敵人卡片已有 `data-enemy-index`；我方卡片只有 `data-member-idx`（[L400](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/panels/battle-panel.js#L400)），建議再加 `data-member-id`。

### 1.3 結論

| 需求 | 資料是否已存在 | 需要做的事 |
|---|---|---|
| 被擊中／格擋／閃避（敵 → 我） | ✅ `DefenseOutcome` 已有 | 包成事件送出 |
| 被擊中（我 → 敵） | ⚠️ 只有傷害值 | 包成事件；後續加入暴擊/未命中 |
| 武器類型 | ✅ `member.getMainHandWeaponType()` | 放進事件 |
| 法術屬性 | ❌ `PartyMemberSkill` 沒有 `damageType` | 從 SkillTemplate 帶過來（部分 JSON 已有 `damageType`） |
| 單人／全體 | ✅ `skill.isAoe()` | 放進事件 |
| 一整排 | ❌ 機制不存在 | 新增 `targetShape` + 後端選目標邏輯 |
| 暴擊 | ⚠️ 只有敵方打我方有 | 我方攻擊需新增暴擊判定 |

---

## 2. 整體架構

```mermaid
sequenceDiagram
    participant Loop as DrpgCombatLoop (後端 500ms tick)
    participant Ctx as BattleContext.pendingEvents
    participant WS as WebSocket
    participant Core as mud-core.js
    participant Dir as BattleFxDirector
    participant Panel as battle-panel.js
    participant Layer as #battle-fx-layer

    Loop->>Ctx: emit(BattleEvent) (每次攻擊/技能/Tick)
    Loop->>Loop: broadcastLog(文字) (保留現有行為)
    Loop->>WS: sendJson(BATTLE_EVENTS 批次)
    Loop->>WS: sendJson(DRPG_STATE 快照)
    WS->>Core: BATTLE_EVENTS
    Core->>Dir: enqueue(events)
    WS->>Core: DRPG_STATE
    Core->>Panel: renderBattleArena (HP 條更新)
    Dir->>Layer: 依時間軸播放 攻擊特效 → 受擊反應 → 浮動數字
```

設計原則：

1. **雙軌輸出**：文字 log 照舊（訊息區不受影響），新增結構化事件給特效用。日後可以考慮「文字由事件生成」做到單一事實來源，但現在不必。
2. **後端決定「打到誰」，前端只負責「怎麼演」**：例如「一整排」攻擊，被打中的是哪幾隻由後端算好放進 `targets[]`，前端只是依 `shape=ROW` 在那些卡片的包圍框畫一道橫掃。這樣前端的重力堆疊排位（gravity simulation）與後端 `rowIdx` 不一致時也不會出錯。
3. **特效層與資料層分離**：浮動數字、斬擊光、法術爆炸都畫在 `#battle-fx-layer`（絕對定位、覆蓋整個戰場）；只有「抖動／閃白」這類短暫 class 會加在卡片上，就算被 rebuild 砍掉也無傷大雅。
4. **可降級**：支援「完整 / 精簡 / 關閉」三段特效等級，並尊重 `prefers-reduced-motion`。

---

## 3. 後端設計

### 3.1 事件模型

新增 package：`com.example.htmlmud.domain.dungeon.battle.event`

```java
// BattleEventType.java
public enum BattleEventType {
  ATTACK,        // 普攻
  SKILL,         // 主動技能 / 法術 (傷害)
  HEAL,          // 治療
  SHIELD,        // 護盾 / 防禦 Buff
  BUFF,          // 增益 / 嘲諷 等
  CAST_START,    // 開始吟唱
  CAST_INTERRUPT,// 吟唱被打斷
  DOT_TICK,      // 持續傷害跳字
  HOT_TICK,      // 持續治療跳字
  RIPOSTE,       // 破招反擊
  DEATH,         // 單位倒下
  ABERRATION     // 走火入魔異變 (特殊演出)
}

// HitOutcome.java —— 直接對齊 DefenseResolver.DefenseOutcome，再補 ABSORBED
public enum HitOutcome { HIT, CRIT, BLOCKED, PARRIED, DODGED, MISS, ABSORBED, IMMUNE }

// FxShape.java —— 範圍形狀 (只是演出提示，實際命中名單在 targets)
public enum FxShape { SINGLE, ROW, COLUMN, ALL, SELF, ALLY_SINGLE, ALLY_ALL }

// UnitRef.java —— 戰場單位參照
public record UnitRef(Side side, String id, int index) {
  public enum Side { PARTY, ENEMY }
}

// BattleHit.java —— 單一目標的結算結果
public record BattleHit(
    UnitRef target,
    HitOutcome outcome,
    int amount,          // 實際扣血 / 回血量
    int absorbed,        // 護盾吸收量 (可為 0)
    boolean killed
) {}

// BattleEvent.java
public record BattleEvent(
    long seq,               // 單場戰鬥遞增序號 (前端去重/排序)
    long ts,                // System.currentTimeMillis()
    BattleEventType type,
    UnitRef actor,          // 施放者 (DOT_TICK 時可為 null)
    String skillId,         // 普攻時為 null
    String skillName,       // 招式名稱 (例如 "運勁平擊")
    String weaponType,      // WeaponType.name()，例如 SWORD / BOW / STAFF
    String damageType,      // DamageType.name()，例如 SLASH / FIRE / HOLY / TRUE
    FxShape shape,
    String fxKey,           // 可選：技能自訂特效 key (覆寫預設表)
    List<BattleHit> hits
) {}
```

> **為什麼 AOE 是「一個事件 + 多個 hits」而不是多個事件？**
> 7 隻怪的全體攻擊只要送 1 筆，前端可以「一次」播放全屏特效，再依 hits 錯開 60ms 依序跳字，視覺上才是「一招打全體」而不是「連打七下」。

### 3.2 BattleContext 增加事件佇列

在 [BattleContext.java](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/BattleContext.java) 增加（注意它會被戰鬥執行緒與指令執行緒同時存取，用 concurrent 結構）：

```java
private final ConcurrentLinkedQueue<BattleEvent> pendingEvents = new ConcurrentLinkedQueue<>();
private final AtomicLong eventSeq = new AtomicLong();

public long nextEventSeq() { return eventSeq.incrementAndGet(); }

public void emit(BattleEvent e) {
  pendingEvents.add(e);
  while (pendingEvents.size() > 200) pendingEvents.poll(); // 斷線/卡頓保護
}

public List<BattleEvent> drainEvents() {
  List<BattleEvent> out = new ArrayList<>();
  BattleEvent e;
  while ((e = pendingEvents.poll()) != null) out.add(e);
  return out;
}
```

建議另寫一個小工具類 `BattleEventFactory`（或 builder），讓 `DrpgCombatLoop` 內呼叫只需一行，例如：

```java
ctx.emit(BattleEvents.attack(ctx, member, target, wt, moveName, dmg, killed));
```

### 3.3 推送方式：獨立訊息 `BATTLE_EVENTS`

**不建議**把事件塞進 `BattleViewDto`：`createBattleView` 會被很多地方呼叫（城鎮廣播、指令回應的 `pushDrpgState`…），若在那裡 drain 會讓事件被不可預期的訊息「吃掉」；若不 drain 又會重播。

建議在 [DrpgCombatLoop.java#L410-L413](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java#L410-L413) 推送狀態**之前**：

```java
// 推送戰鬥演出事件 (先事件、後快照)
List<BattleEvent> events = ctx.drainEvents();
if (!events.isEmpty() && player != null && player.isValid()) {
  player.sendJson(new BattleEventsDto("BATTLE_EVENTS", ctx.getBattleId(), events));
}
// 推送戰鬥進度
if (stateBroadcaster != null && player != null) {
  stateBroadcaster.accept(player);
}
```

在 `finally` 區塊（[L445-L447](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java#L445-L447)）也要 flush 一次，確保「最後一擊」的事件有送出去。

玩家手動施放技能（`skill cast ...` 走 `DrpgBattleService`）產生的事件，會在下一個 tick（≤500ms）一起送出；若覺得延遲明顯，可以在手動施放後立即 flush 一次。

`BattleEventsDto`：

```java
public record BattleEventsDto(String type, String battleId, List<BattleEvent> events) {}
```

JSON 範例（全體雷法，一隻暴擊、一隻被打死）：

```json
{
  "type": "BATTLE_EVENTS",
  "battleId": "b-7f3a",
  "events": [
    {
      "seq": 128, "ts": 1791201234567, "type": "SKILL",
      "actor": { "side": "PARTY", "id": "m-02", "index": 1 },
      "skillId": "thunder_palm", "skillName": "五雷掌",
      "weaponType": "STAFF", "damageType": "LIGHTNING",
      "shape": "ALL", "fxKey": null,
      "hits": [
        { "target": { "side": "ENEMY", "id": "e-1", "index": 0 }, "outcome": "HIT",  "amount": 42, "absorbed": 0, "killed": false },
        { "target": { "side": "ENEMY", "id": "e-2", "index": 1 }, "outcome": "CRIT", "amount": 77, "absorbed": 0, "killed": true }
      ]
    }
  ]
}
```

### 3.4 戰鬥迴圈埋點清單

| 埋點 | 檔案位置 | 事件 |
|---|---|---|
| 隊員普攻 | `DrpgCombatLoop` L303-L341 | `ATTACK`，`weaponType = wt.name()`，`damageType` 依武器推導（SWORD→SLASH、SPEAR→PIERCE、HAMMER→BLUNT…） |
| 敵方攻擊 | `DrpgCombatLoop` L364-L397 | `ATTACK`，`hits[0].outcome = defenseRes.outcome()`，`amount = finalDamage()` |
| 破招反擊 | `DrpgCombatLoop` L374-L379 | `RIPOSTE`（actor=隊員，target=敵人） |
| 走火入魔背刺 / 狂暴 | `DrpgCombatLoop` L259-L289 | `ATTACK`，`fxKey = "madness"`（紫黑色特效） |
| 異變降世 | `DrpgCombatLoop` L223-L255 | `ABERRATION`（全屏演出） |
| 吟唱開始 / 完成 / 打斷 | L173-L195、L500-L509 | `CAST_START` / `CAST_INTERRUPT` |
| 技能：治療 | L536-L555 | `HEAL`，shape = `ALLY_ALL` 或 `ALLY_SINGLE` |
| 技能：護盾 / Buff / 嘲諷 | L556-L624 | `SHIELD` / `BUFF` |
| 技能：傷害 | L625-L664 | `SKILL`，shape = `ALL` / `ROW` / `SINGLE` |
| Buff Tick | L144-L159 | `DOT_TICK` / `HOT_TICK`（需修改 `BuffSettlementService.processTicks` 回傳結構化結果，或在其內部直接 `emit`） |
| 單位死亡 | `handlePartyMemberDeath`、各處 `!target.isAlive()` | 用 `hits[].killed=true` 即可，不一定需要獨立 `DEATH` 事件 |

### 3.5 補齊缺少的資料

1. **技能傷害屬性**：`PartyMemberSkill` 新增 `damageType`（預設 `PHYSICAL`）、`targetShape`（預設 `SINGLE`，`aoe=true` 時視為 `ALL`）、`fxKey`（選填）。在建立 `PartyMemberSkill` 的轉接處（`SkillBridgeService` / `DrpgTemplateAdapter`）從 SkillTemplate 映射。目前 data 中已有 37 處 `damageType`（如 `lion_roar.json: SONIC`、`gorging_corruption.json: DARK`），可直接沿用；法術類（`category=SPELL`）沒填時可依 `tags` 推導（例如 tags 含 `FIRE`）。
2. **敵人攻擊屬性**：`BattleEnemy` 新增 `damageType`（從 mob 模板的 natural attack 取得，如 `mob_claw: SLASH`、`mob_bite: PIERCE`）。
3. **一整排攻擊**：`targetShape = ROW` 時，後端選出「與主目標相同有效排」的所有存活敵人。建議在 `BattleContext` 加 `List<BattleEnemy> getEnemiesInSameRow(BattleEnemy anchor)`，使用 `rowIdx`（並考慮前排死光後 `checkEnemyRowAdvancement` 的推進結果）。`COLUMN` 同理用 `colIdx`。
4. **我方暴擊 / 未命中**（第 3 階段）：把 `calculatePlayerDamage` 改成回傳 `DamageRoll(int amount, HitOutcome outcome)`，套用簡化版的圓桌（Miss → Crit → Hit），或直接重用 `DefenseResolver` 的參數化版本。這同時也是平衡性調整，**建議另開 PR**，並更新 `OneRollCombatTableTest` 等測試。

---

## 4. 前端設計

### 4.1 檔案結構

```
static/
├─ css/
│  └─ style-07-battle-fx.css      ← 新增：所有 keyframes、浮動數字、特效樣式
└─ js/
   └─ fx/
      ├─ battle-fx-director.js    ← 新增：事件佇列、時間軸排程、效能上限
      ├─ fx-presets.js            ← 新增：武器/屬性/形狀/判定 → 演出 的對照表
      ├─ floating-text.js         ← 新增：浮動數字 (物件池)
      ├─ fx-geometry.js           ← 新增：取得單位卡片座標、排/列包圍框
      └─ fx-settings.js           ← 新增：特效等級 (full/lite/off)、localStorage
```

需要修改：

| 檔案 | 修改 |
|---|---|
| [index.html](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/index.html) | 引入 `style-07-battle-fx.css`；在 `.battle-arena-body` 內最後加 `<div id="battle-fx-layer" class="battle-fx-layer" aria-hidden="true"></div>` |
| [mud-core.js](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/mud-core.js#L88-L91) | `handleServerMessage` 新增 `else if (data.type === 'BATTLE_EVENTS') window.onBattleEvents?.(data);` |
| [app.js](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/app.js) | 匯入 director，`window.onBattleEvents = (d) => fxDirector.enqueue(d.events)`；戰鬥結束 (`inBattle=false`) 時 `fxDirector.clear()` |
| [battle-panel.js](file:///c:/Workspace/my_practice/practice_mud/src/main/resources/static/js/panels/battle-panel.js) | ① 我方卡片加 `data-member-id`；② full rebuild 前呼叫 `fxDirector.captureGhosts(enemiesBox)`，把即將消失的死亡卡片複製到 fx layer 播放死亡動畫；③ 敵人 HP 條加「延遲殘影條」 |

### 4.2 BattleFxDirector（核心）

職責：
1. **去重與排序**：依 `seq` 排序，忽略已播過的 seq（重連時可能重送）。
2. **時間軸**：同一批事件（一個 tick 的量）平均分散在約 0～450ms 內播放，避免 500ms 內所有東西同時爆開；每個事件內部再分「出手 → 命中 → 跳字」三段。
3. **效能保護**：同時存在的浮動數字上限（例如 24 個），超過時合併或丟棄最舊的；分頁在背景（`document.hidden`）時直接丟棄事件，只保留快照。
4. **降級**：`fxSettings.level === 'off'` 只跳數字、`'lite'` 不做全屏閃光與震動。

骨架：

```js
// js/fx/battle-fx-director.js
import { resolvePreset } from './fx-presets.js';
import { spawnFloatingText } from './floating-text.js';
import { getUnitRect, getUnionRect } from './fx-geometry.js';
import { fxSettings } from './fx-settings.js';

const TICK_WINDOW_MS = 450;

class BattleFxDirector {
  constructor() { this.lastSeq = 0; this.timers = new Set(); }

  get layer() { return document.getElementById('battle-fx-layer'); }

  enqueue(events = []) {
    if (document.hidden || !this.layer) return;
    const fresh = events.filter(e => e.seq > this.lastSeq).sort((a, b) => a.seq - b.seq);
    if (!fresh.length) return;
    this.lastSeq = fresh[fresh.length - 1].seq;
    const step = fresh.length > 1 ? TICK_WINDOW_MS / fresh.length : 0;
    fresh.forEach((ev, i) => this.later(() => this.play(ev), i * step));
  }

  play(ev) {
    const preset = resolvePreset(ev);              // 依 type/weapon/damageType/shape 查表
    const actorEl = this.findUnitEl(ev.actor);
    if (actorEl && preset.actorAnim) this.flashClass(actorEl, preset.actorAnim, 300);

    // 1) 出手/範圍特效
    const targetEls = ev.hits.map(h => this.findUnitEl(h.target)).filter(Boolean);
    if (preset.areaFx && ev.shape !== 'SINGLE') {
      this.spawnFx(preset.areaFx, getUnionRect(targetEls, this.layer));
    }

    // 2) 命中 + 3) 跳字 (AOE 依序錯開)
    ev.hits.forEach((hit, i) => this.later(() => {
      const el = this.findUnitEl(hit.target);
      if (!el) return;
      const rect = getUnitRect(el, this.layer);
      if (preset.impactFx) this.spawnFx(preset.impactFx, rect);
      this.flashClass(el, reactionClass(hit.outcome), 450);
      spawnFloatingText(this.layer, rect, hit, ev);
    }, (preset.impactDelay ?? 120) + i * 60));
  }

  findUnitEl(ref) {
    if (!ref) return null;
    return ref.side === 'ENEMY'
      ? document.querySelector(`.enemy-card[data-enemy-index="${ref.index}"]`)
      : document.querySelector(`.battle-party-grid-card[data-member-id="${CSS.escape(ref.id)}"]`);
  }

  flashClass(el, cls, ms) {
    if (!cls) return;
    el.classList.remove(cls); void el.offsetWidth;   // 重新觸發動畫
    el.classList.add(cls);
    this.later(() => el.classList.remove(cls), ms);
  }

  spawnFx(fxClass, rect) {
    if (fxSettings.level === 'off') return;
    const n = document.createElement('div');
    n.className = `fx ${fxClass}`;
    Object.assign(n.style, { left: `${rect.x}px`, top: `${rect.y}px`, width: `${rect.w}px`, height: `${rect.h}px` });
    n.addEventListener('animationend', () => n.remove(), { once: true });
    this.layer.appendChild(n);
  }

  later(fn, ms) { const t = setTimeout(() => { this.timers.delete(t); fn(); }, ms); this.timers.add(t); }
  clear() { this.timers.forEach(clearTimeout); this.timers.clear(); this.layer?.replaceChildren(); this.lastSeq = 0; }
}

function reactionClass(outcome) {
  return {
    HIT: 'react-hit', CRIT: 'react-crit', BLOCKED: 'react-block',
    PARRIED: 'react-parry', DODGED: 'react-dodge', MISS: 'react-miss', ABSORBED: 'react-absorb'
  }[outcome] || 'react-hit';
}

export const fxDirector = new BattleFxDirector();
```

### 4.3 浮動傷害數字（需求 3）

#### 顏色規範

| 類別 | 條件 | 顏色 / 樣式 | 範例 |
|---|---|---|---|
| 物理 | `PHYSICAL/SLASH/PIERCE/BLUNT` | 白 `#f8fafc`，黑描邊 | `42` |
| 火 | `FIRE` | 橘 `#fb923c` | `42` |
| 冰 | `ICE` | 冰藍 `#67e8f9` | `42` |
| 雷 | `LIGHTNING` | 亮黃 `#facc15` | `42` |
| 毒 | `POISON` | 綠 `#4ade80` | `42` |
| 神聖 | `HOLY` | 淡金 `#fde68a` | `42` |
| 暗影 | `DARK` | 紫 `#a78bfa` | `42` |
| 一般法術 | `MAGIC` | 藍 `#60a5fa` | `42` |
| 音波 | `SONIC` | 青綠 `#2dd4bf` | `42` |
| 真實傷害 | `TRUE` | 粉紅 `#f472b6` + 白描邊 | `42` |
| **暴擊** | `outcome=CRIT` | 沿用屬性色，**字級 ×1.6**、橘紅外光暈、前置「暴擊！」、彈跳放大 | `暴擊！77` |
| 我方受傷 | `target.side=PARTY` | 屬性色 + **紅色描邊**，方便一眼分辨「我被打」 | `-35` |
| 治療 | `HEAL/HOT_TICK` | 綠 `#22c55e`，前置 `+` | `+60` |
| 護盾吸收 | `absorbed>0` | 淡藍 `#93c5fd`，小字 | `(吸收 20)` |
| 持續傷害 | `DOT_TICK` | 屬性色、字級 ×0.8、不彈跳 | `8` |
| 格擋 | `BLOCKED` | 鋼灰 `#cbd5e1` + 🛡 | `格擋 12` |
| 招架 | `PARRIED` | 金黃 `#fcd34d` | `招架 18` |
| 閃避 | `DODGED` | 青 `#22d3ee`，斜體 | `閃避` |
| 未命中 | `MISS` | 灰 `#94a3b8` | `未命中` |
| 反擊 | `RIPOSTE` | 青色 + ⚔ | `反擊 25` |

#### 運動軌跡

- 一般：從目標卡片中上方生成，**上飄 40px + 淡出，約 900ms**；每次生成加 ±12px 水平隨機偏移，避免連擊數字重疊。
- 暴擊：先放大到 1.8 倍再縮回 1.4 倍（0~150ms），再上飄，總長 1200ms。
- 同一目標 150ms 內的多個數字：垂直堆疊（每個 +18px）。

```js
// js/fx/floating-text.js
const DMG_COLORS = {
  PHYSICAL: 'dmg-phys', SLASH: 'dmg-phys', PIERCE: 'dmg-phys', BLUNT: 'dmg-phys',
  FIRE: 'dmg-fire', ICE: 'dmg-ice', LIGHTNING: 'dmg-lightning', POISON: 'dmg-poison',
  HOLY: 'dmg-holy', DARK: 'dmg-dark', MAGIC: 'dmg-magic', SONIC: 'dmg-sonic', TRUE: 'dmg-true'
};
const MAX_ALIVE = 24;

export function spawnFloatingText(layer, rect, hit, ev) {
  if (layer.querySelectorAll('.ftext').length >= MAX_ALIVE) layer.querySelector('.ftext')?.remove();

  const el = document.createElement('div');
  const cls = ['ftext'];
  let text;
  switch (hit.outcome) {
    case 'MISS':    cls.push('ft-miss');  text = '未命中'; break;
    case 'DODGED':  cls.push('ft-dodge'); text = '閃避'; break;
    case 'BLOCKED': cls.push('ft-block'); text = `🛡 格擋 ${hit.amount}`; break;
    case 'PARRIED': cls.push('ft-parry'); text = `招架 ${hit.amount}`; break;
    default:
      if (ev.type === 'HEAL' || ev.type === 'HOT_TICK') { cls.push('ft-heal'); text = `+${hit.amount}`; }
      else {
        cls.push(DMG_COLORS[ev.damageType] || 'dmg-phys');
        text = hit.target.side === 'PARTY' ? `-${hit.amount}` : `${hit.amount}`;
        if (hit.outcome === 'CRIT') { cls.push('ft-crit'); text = `暴擊！${hit.amount}`; }
        if (ev.type === 'DOT_TICK') cls.push('ft-small');
      }
  }
  if (hit.target.side === 'PARTY') cls.push('ft-on-ally');

  el.className = cls.join(' ');
  el.textContent = text;                 // 用 textContent，不需要 escape
  el.style.left = `${rect.x + rect.w / 2 + (Math.random() * 24 - 12)}px`;
  el.style.top  = `${rect.y + rect.h * 0.25}px`;
  el.addEventListener('animationend', () => el.remove(), { once: true });
  layer.appendChild(el);

  if (hit.absorbed > 0) { /* 另生成一個小字 (吸收 N) */ }
}
```

### 4.4 攻擊特效（需求 1）

#### 4.4.1 出手者動作（Actor Animation）

| 出手者 | 動作 |
|---|---|
| 我方隊員（在下方） | 卡片往上「突進」6px 再回彈 (`actor-lunge-up`)，邊框亮起武器色 |
| 敵人（在上方） | 卡片往下突進 6px (`actor-lunge-down`)，名稱閃紅 |
| 吟唱中 | 卡片外圈持續旋轉的光環 (`casting-aura`)，直到 `CAST_START` 對應技能施放或 `CAST_INTERRUPT`（打斷時光環碎裂） |

#### 4.4.2 依武器類型（普攻 / 武技）

以後端 `WeaponType` 分組（對齊 [DrpgCombatLoop.java#L328-L339](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java#L328-L339) 已有的分組）：

| 武器組 | 包含 | 命中特效 (impactFx) | 實作手法 |
|---|---|---|---|
| 劍 | SWORD | 細長斜向白色弧光 1 道 | 旋轉 -35° 的細長漸層 div，`scaleX 0→1` + 淡出 |
| 刀／斧 | BLADE, AXE, POLEAXE | 粗弧光 + 橘色殘影 | 同上，較粗較慢，加 `filter: blur` 殘影 |
| 鈍器 | BLUNT, HAMMER, MACE, MAUL, CLUB, FLAIL | 圓形衝擊波 + 卡片下沉抖動 | `radial-gradient` 環 `scale 0.3→1.4` |
| 短刃 | DAGGER, DIRK, KNIFE, STILETTO | 2~3 道快速交叉細線 | 3 個細線錯開 50ms |
| 長兵 | POLEARM, HALBERD, SPEAR, JAVELIN | 垂直穿刺光線（由下往上） | 細長條 `translateY` 由攻擊者方向射入 |
| 弓弩 | BOW, CROSSBOW | 箭矢由出手者飛向目標 | 小元素以 Web Animations API 從 actorRect 飛到 targetRect（200ms） |
| 法器 | STAFF, WAND, ROD, SCEPTER | 光球飛行 + 命中綻開 | 同弓箭，但換成發光圓球 |
| 徒手 | 其他 | 拳印爆點 | 小型星形爆點 |

#### 4.4.3 依法術屬性（`category=SPELL` 或 `damageType` 屬魔法系）

| 屬性 | 特效 |
|---|---|
| FIRE | 橘紅火球爆炸 + 短暫熱浪扭曲（`filter: blur + hue`） |
| ICE | 冰晶碎片放射 + 目標卡片短暫藍白覆蓋（凍結感） |
| LIGHTNING | 由戰場頂端劈下的鋸齒閃電（SVG polyline 或 clip-path）+ 全場白閃 80ms |
| POISON | 綠色煙霧擴散（多個半透明圓，緩慢擴大） |
| HOLY | 由上而下的金色光柱 |
| DARK | 紫黑漩渦向內收縮 |
| SONIC | 同心圓波紋向外擴散（獅子吼） |
| TRUE | 黑白反相閃爍一瞬 |

#### 4.4.4 依範圍形狀（Shape）

| 形狀 | 演出 |
|---|---|
| `SINGLE` | 只在目標卡片上播 impactFx |
| `ROW` | 取所有命中卡片的**包圍框**，畫一道由左至右的橫掃光帶（350ms），接著每張卡片依序 (60ms 間隔) 播 impactFx |
| `COLUMN` | 同上但縱向，由下往上貫穿 |
| `ALL` | 敵方區域整體閃光／屬性色覆蓋 (`area-flash-<damageType>`) + 戰場輕微震動，再依序跳字 |
| `ALLY_SINGLE` / `ALLY_ALL` | 治療：綠色光點上升；護盾：藍色半透明六角護罩浮現；Buff：金色向上箭頭 |

#### 4.4.5 暴擊加成

- 命中時整個 `#battle-arena-panel` 做 120ms 的**畫面震動** (`arena-shake`)；
- 目標卡片紅白閃爍 2 次；
- impactFx 放大 1.4 倍；
- （可選）120ms「頓幀」（hit-stop）：暫停其他排程 80ms，打擊感會強很多。

### 4.5 受擊反應（需求 2）

加在**卡片本身**的短暫 class（450ms 內移除）：

| 判定 | 反應 class | 視覺 |
|---|---|---|
| HIT | `react-hit` | 白閃 (`filter: brightness(2)` 60ms) + 水平小幅抖動 3px |
| CRIT | `react-crit` | 紅閃 2 次 + 抖動 6px + 外框紅色光暈 |
| BLOCKED | `react-block` | 卡片不動，前方浮現 🛡 盾形圖示「噹」一下縮放，藍色火花 |
| PARRIED | `react-parry` | 金屬黃色交叉火花 ⚔，卡片微微後仰（`rotate(-2deg)`） |
| DODGED | `react-dodge` | 卡片快速側移 14px 並半透明，留下殘影（`::after` 複製外框淡出），再滑回 |
| MISS | `react-miss` | 卡片無反應，僅跳灰字 |
| ABSORBED | `react-absorb` | 卡片外浮現護盾泡泡破裂效果 |
| 死亡 (`killed`) | 由 ghost 處理 | 灰階 → 碎裂淡出 / 下沉（600ms） |

CSS 範例：

```css
/* style-07-battle-fx.css */
.battle-arena-body { position: relative; }
.battle-fx-layer {
  position: absolute; inset: 0; pointer-events: none; overflow: hidden; z-index: 30;
}

/* ---- 受擊反應 ---- */
@keyframes react-shake { 0%,100%{transform:translateX(0)} 25%{transform:translateX(-3px)} 75%{transform:translateX(3px)} }
@keyframes react-flash { 0%{filter:brightness(2.2)} 100%{filter:none} }
.react-hit  { animation: react-flash 120ms ease-out, react-shake 220ms ease-in-out; }
.react-crit { animation: react-flash 90ms ease-out 2, react-shake 260ms ease-in-out;
              box-shadow: 0 0 14px 2px rgba(239,68,68,.85) !important; }

@keyframes react-dodge { 0%{transform:translateX(0);opacity:1} 40%{transform:translateX(14px);opacity:.45} 100%{transform:translateX(0);opacity:1} }
.react-dodge { animation: react-dodge 380ms cubic-bezier(.2,.8,.2,1); }

@keyframes react-parry { 0%,100%{transform:rotate(0)} 30%{transform:rotate(-2deg)} }
.react-parry { animation: react-parry 260ms ease-out; }

/* ---- 浮動數字 ---- */
.ftext {
  position: absolute; transform: translate(-50%, 0);
  font: 800 20px/1 "Noto Sans TC", system-ui, sans-serif;
  -webkit-text-stroke: 1px rgba(0,0,0,.85); text-shadow: 0 2px 3px rgba(0,0,0,.8);
  white-space: nowrap; will-change: transform, opacity;
  animation: ft-rise 900ms ease-out forwards;
}
@keyframes ft-rise { 0%{opacity:0; transform:translate(-50%,6px) scale(.8)}
                     15%{opacity:1; transform:translate(-50%,0) scale(1.1)}
                     100%{opacity:0; transform:translate(-50%,-40px) scale(1)} }
.ft-crit { font-size: 32px; filter: drop-shadow(0 0 6px rgba(249,115,22,.9));
           animation: ft-crit 1200ms cubic-bezier(.2,1.4,.4,1) forwards; }
@keyframes ft-crit { 0%{opacity:0; transform:translate(-50%,0) scale(.4)}
                     12%{opacity:1; transform:translate(-50%,-4px) scale(1.8)}
                     25%{transform:translate(-50%,-6px) scale(1.4)}
                     100%{opacity:0; transform:translate(-50%,-50px) scale(1.3)} }
.ft-small { font-size: 15px; animation-duration: 700ms; }
.ft-on-ally { -webkit-text-stroke: 1px #7f1d1d; }

.dmg-phys{color:#f8fafc} .dmg-fire{color:#fb923c} .dmg-ice{color:#67e8f9}
.dmg-lightning{color:#facc15} .dmg-poison{color:#4ade80} .dmg-holy{color:#fde68a}
.dmg-dark{color:#a78bfa} .dmg-magic{color:#60a5fa} .dmg-sonic{color:#2dd4bf}
.dmg-true{color:#f472b6; -webkit-text-stroke:1px #fff}
.ft-heal{color:#22c55e} .ft-miss{color:#94a3b8; font-size:16px}
.ft-dodge{color:#22d3ee; font-style:italic; font-size:17px}
.ft-block{color:#cbd5e1} .ft-parry{color:#fcd34d}

/* ---- 減少動態偏好 ---- */
@media (prefers-reduced-motion: reduce) {
  .react-hit, .react-crit, .react-dodge, .react-parry { animation: react-flash 120ms ease-out; }
  .fx { display: none; }
}
```

### 4.6 與現有渲染的整合重點

1. **死亡動畫（ghost）**：在 `renderBattleArena` 判定 `needFullRebuild` 時，先比對舊 DOM 中有、但 `livingEnemies` 中沒有的 `data-enemy-index`，把那些卡片 `cloneNode(true)`，以 `getBoundingClientRect` 換算成 fx layer 座標後放進 fx layer，加上 `.ghost-dying` class 播 600ms 再移除。這樣不必動到現有「死亡立即移出陣列、空間釋放」的設計。
2. **HP 殘影條**：在 `.enemy-hp-wrap` 與 `.bpg-hp-wrap` 內加一條 `.hp-lag-bar`（淺色、位於主 HP 條下層），主條立即縮短，殘影條 `transition: width 600ms 250ms` 延遲縮短。這能**掩蓋事件動畫與快照抵達時間的落差**，也是格鬥/ARPG 常見手法。
3. **Rebuild 時保留 class**：反應 class 只存活 450ms，就算被 rebuild 洗掉影響也很小；所有長時間特效（光環、數字）都放在 fx layer，不受 rebuild 影響。
4. **座標換算**：`fx-geometry.js` 統一做 `el.getBoundingClientRect()` − `layer.getBoundingClientRect()`；視窗 resize 時不需處理（特效都很短）。

### 4.7 特效預設表（資料驅動）

```js
// js/fx/fx-presets.js
const WEAPON_GROUP = {
  SWORD: 'sword', BLADE: 'blade', AXE: 'blade', POLEAXE: 'blade',
  BLUNT: 'blunt', HAMMER: 'blunt', MACE: 'blunt', MAUL: 'blunt', CLUB: 'blunt', FLAIL: 'blunt',
  DAGGER: 'dagger', DIRK: 'dagger', KNIFE: 'dagger', STILETTO: 'dagger',
  POLEARM: 'spear', HALBERD: 'spear', SPEAR: 'spear', JAVELIN: 'spear',
  BOW: 'bow', CROSSBOW: 'bow', STAFF: 'staff', WAND: 'staff', ROD: 'staff', SCEPTER: 'staff'
};
const MAGIC_TYPES = new Set(['MAGIC','FIRE','ICE','LIGHTNING','POISON','HOLY','DARK','SONIC','TRUE']);

const CUSTOM = {            // 技能自訂 fxKey 覆寫 (由後端 skill JSON 的 fxKey 指定)
  madness: { impactFx: 'fx-dark-vortex', actorAnim: 'actor-madness' },
};

export function resolvePreset(ev) {
  if (ev.fxKey && CUSTOM[ev.fxKey]) return CUSTOM[ev.fxKey];

  const actorAnim = ev.actor?.side === 'ENEMY' ? 'actor-lunge-down' : 'actor-lunge-up';
  switch (ev.type) {
    case 'HEAL': case 'HOT_TICK': return { actorAnim, impactFx: 'fx-heal-sparkle', areaFx: 'fx-heal-area' };
    case 'SHIELD':               return { actorAnim, impactFx: 'fx-shield-hex' };
    case 'BUFF':                 return { actorAnim, impactFx: 'fx-buff-up' };
    case 'DOT_TICK':             return { impactFx: null, impactDelay: 0 };
  }
  if (MAGIC_TYPES.has(ev.damageType)) {
    const t = ev.damageType.toLowerCase();
    return { actorAnim, impactFx: `fx-spell-${t}`, areaFx: `fx-area-${shapeKey(ev.shape)}-${t}`, impactDelay: 180 };
  }
  const g = WEAPON_GROUP[ev.weaponType] || 'fist';
  return { actorAnim, impactFx: `fx-hit-${g}`, areaFx: `fx-area-${shapeKey(ev.shape)}-phys`, impactDelay: 120 };
}
const shapeKey = s => (s === 'ROW' ? 'row' : s === 'COLUMN' ? 'col' : 'all');
```

---

## 5. 分階段執行步驟

### Phase 0：基礎建設（約 0.5 天）

- [ ] 後端：建立 `event` package（`BattleEvent`、`BattleEventType`、`HitOutcome`、`FxShape`、`UnitRef`、`BattleHit`、`BattleEventsDto`）。
- [ ] 後端：`BattleContext` 加入 `pendingEvents` / `emit` / `drainEvents` / `nextEventSeq`。
- [ ] 後端：`DrpgCombatLoop` 在 tick 結尾與 `finally` 推送 `BATTLE_EVENTS`。
- [ ] 前端：`index.html` 加 fx layer 與 `style-07-battle-fx.css`。
- [ ] 前端：`mud-core.js` 路由 `BATTLE_EVENTS`；`app.js` 綁定 `fxDirector`。
- [ ] 前端：我方卡片加 `data-member-id`。
- **驗收**：瀏覽器 DevTools → Network → WS 能看到 `BATTLE_EVENTS`（即使還是空的）。

### Phase 1：MVP —— 浮動數字 + 受擊反應（約 1～1.5 天）⭐ 最有感

- [ ] 後端埋點：隊員普攻、敵方攻擊（直接使用 `defenseRes.outcome()`）、破招反擊、死亡 (`killed`)。
- [ ] 前端：`floating-text.js`（物理白字、暴擊放大、閃避/招架/格擋/未命中文字、我方受傷紅描邊）。
- [ ] 前端：受擊反應 class（hit / crit / block / parry / dodge）。
- [ ] 前端：HP 殘影條。
- [ ] 前端：死亡 ghost 動畫。
- **驗收**：一場普通戰鬥中，能清楚看到每一下攻擊打在哪張卡片、數字多少；敵人攻擊時能看到閃避滑開、格擋盾牌、暴擊震動。

### Phase 2：攻擊特效 —— 武器 / 技能 / AOE / 支援技（約 2 天）

- [ ] 前端：`fx-presets.js` + 武器組 impactFx（8 組）。
- [ ] 前端：出手者突進動畫、吟唱光環（需後端 `CAST_START` / `CAST_INTERRUPT` 埋點）。
- [ ] 後端埋點：`applySkillEffects` 全部分支（SKILL / HEAL / SHIELD / BUFF），AOE 用單一事件多 hits。
- [ ] 前端：`ALL` 形狀全場特效、治療綠光、護盾六角罩、Buff 上升箭頭。
- [ ] 後端：`BuffSettlementService.processTicks` 產生 `DOT_TICK` / `HOT_TICK` 事件。
- **驗收**：不同武器的隊員普攻看起來不一樣；全體技能有一次性全場演出並依序跳字；補血跳綠字。

### Phase 3：屬性、範圍形狀、我方暴擊（約 2～3 天，含平衡調整）

- [ ] 後端：`PartyMemberSkill` 新增 `damageType` / `targetShape` / `fxKey`，於 SkillBridge / Adapter 映射；為 SPELL 類技能 JSON 補上 `damageType`。
- [ ] 後端：`BattleEnemy` 新增 `damageType`（來自 natural attack）。
- [ ] 後端：實作 `ROW` / `COLUMN` 目標選擇（`BattleContext.getEnemiesInSameRow/Column`）。
- [ ] 後端：我方攻擊加入暴擊 / 未命中（`DamageRoll`），更新測試。**建議獨立 PR**，因為會影響戰鬥平衡。
- [ ] 前端：8 種屬性法術特效、屬性數字顏色、`ROW` 橫掃與 `COLUMN` 貫穿。
- **驗收**：火、冰、雷法術可一眼區分；「一整排」技能只打到同排並有橫掃光帶；隊員暴擊時有大數字＋震動。

### Phase 4：打磨與設定（約 1～2 天）

- [ ] 暴擊頓幀 (hit-stop)、畫面震動強度依傷害佔 maxHp 比例縮放。
- [ ] 走火入魔 / 異變降世專屬全屏演出（紫黑扭曲、畫面暗角）。
- [ ] 特效設定：完整 / 精簡 / 關閉、是否顯示數字（`fx-settings.js` + localStorage，可放在現有設定或戰鬥按鈕列）。
- [ ] 音效掛鉤（`fxDirector.on('impact', ...)`），先留介面不實作。
- [ ] 效能檢查：7 隻怪 + 5 隊員 + 全體技能連發，Chrome Performance 面板確認無長任務、無 layout thrashing。

---

## 6. 測試與驗證建議

### 6.1 後端單元測試

- 參考既有的 [DrpgBattleServiceTest](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/DrpgBattleServiceTest.java)、`OneRollCombatTableTest`：
  - `DefenseResolver.resolveEnemyAttack(..., predeterminedRoll)` 已支援指定骰值 → 可精準斷言「擲出閃避時，事件 `outcome=DODGED`、`amount=0`」。
  - AOE 技能：斷言只產生 **1 個** `SKILL` 事件，`hits.size() == 存活敵人數`。
  - `drainEvents()` 之後佇列為空、`seq` 嚴格遞增。
  - `ROW` 技能只命中同排。

### 6.2 前端特效遊樂場（強烈建議）

為了不用每次都進地牢打怪才能調特效，建議在 `battle-fx-director.js` 匯出一個開發用函式：

```js
// 只在開發時掛到 window，在 Console 執行 __fxDemo('CRIT') / __fxDemo('ALL_FIRE')
window.__fxDemo = (kind) => fxDirector.enqueue(buildDemoEvents(kind));
```

或新增一個 `static/debug/fx-playground.html`，用假資料畫一個靜態戰場，一排按鈕觸發所有預設（每種武器、每種屬性、每種形狀、每種判定），讓你調 CSS 時能即時看到效果。

### 6.3 驗收清單

- [ ] 普攻、技能、法術、治療、護盾、DoT/HoT 都有對應演出
- [ ] HIT / CRIT / BLOCK / PARRY / DODGE / MISS 六種受擊反應都看得出差異
- [ ] 數字顏色符合 §4.3 規範，我方受傷有紅描邊
- [ ] 怪物死亡有消散動畫，排位重排不閃爍
- [ ] 戰鬥結束時 fx layer 被清空，無殘留元素（DevTools Elements 檢查）
- [ ] 切到別的分頁再切回來，不會一次爆出大量積壓的特效
- [ ] `prefers-reduced-motion` 下沒有震動
- [ ] 訊息區文字 log 行為完全不變

---

## 7. 風險與注意事項

| 風險 | 說明 | 對策 |
|---|---|---|
| 併發 | `BattleContext` 同時被戰鬥虛擬執行緒與指令執行緒存取 | 事件佇列用 `ConcurrentLinkedQueue`，seq 用 `AtomicLong` |
| 事件與快照不同步 | 事件演 450ms，快照的 HP 是立即更新 | HP 殘影條；數字顯示的是事件內的 `amount`，不依賴快照 |
| 訊息量 | 每 tick 多送一則 JSON | 只在有事件時才送；AOE 合併為單一事件；佇列上限 200 |
| 重連重播 | 斷線重連可能收到舊事件 | 前端依 `seq` 去重；新戰鬥（`battleId` 改變）時重置 `lastSeq` |
| 特效過多造成卡頓 | 全體技能 + 多隊員連發 | 浮動數字上限 24、背景分頁丟棄事件、只用 `transform/opacity` 動畫 |
| 前後端排位不一致 | 前端有重力堆疊、後端用 `rowIdx` | 命中名單由後端決定，前端只依實際命中卡片的包圍框畫範圍特效 |
| 平衡性變動 | 我方新增暴擊會讓戰鬥變簡單 | 放在 Phase 3 並獨立 PR，調整 `GameConfig.combat` 參數 |
| 文字與事件重複維護 | 每個埋點都要同時寫 log 與 event | 短期接受；長期可讓 `BattleEvent` → 文字模板，log 由事件生成 |

---

## 8. 工作量估計總表

| 階段 | 後端 | 前端 | 合計 |
|---|---|---|---|
| Phase 0 基礎建設 | 0.25 天 | 0.25 天 | 0.5 天 |
| Phase 1 MVP 數字＋受擊 | 0.5 天 | 1 天 | 1.5 天 |
| Phase 2 攻擊特效 | 0.75 天 | 1.25 天 | 2 天 |
| Phase 3 屬性／形狀／暴擊 | 1.5 天 | 1 天 | 2.5 天 |
| Phase 4 打磨與設定 | 0.25 天 | 1.25 天 | 1.5 天 |
| **合計** | | | **約 8 天** |

> 建議先完成 **Phase 0 + Phase 1**（約 2 天）就上線試玩，確認節奏與數字可讀性後，再決定 Phase 2 之後特效的華麗程度。

---

## 9. 需要你決定的事項

1. **我方攻擊是否要加入暴擊／未命中？**（會影響平衡；不加的話，我方打敵人只會有 HIT，暴擊數字只出現在敵人打我方時。）
2. **「一整排」的定義**：以敵人的 `rowIdx`（後端排位）為準，還是以前端重力堆疊後的「有效排」為準？建議以後端為準，並讓後端在前排全滅時同步推進 `rowIdx`。
3. **特效風格**：偏「武俠水墨」（筆刷斬擊、墨點飛濺）還是「現代 RPG 光效」（發光、粒子）？本文件的 CSS 範例偏後者，風格可以在 Phase 2 調整而不影響架構。
4. **是否需要音效**？若需要，Phase 4 一併規劃音效資源與音量設定。
