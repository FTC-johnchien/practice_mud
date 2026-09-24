# Phase 11: 戰鬥狀態機 (FSM)、全域冷卻 (GCD)、施法唱條 (Casting) 與仇恨威脅度 (Threat/Aggro) 實作計畫

本計畫依據使用者選擇之 **【路徑 1：核心戰鬥推進】**，落地 [GEMINI.md](../../GEMINI.md) 階段一中最後一個核心機制：**戰鬥狀態機 (FSM)、全域冷卻 (GCD)、施法唱條與仇恨/嘲諷系統**。

---

## 一、設計理念與架構邊界 (Design Pillars)

1. **全域冷卻時間 (Global Cooldown - GCD)**：
   - 參照魔獸世界 (WoW) 標準節奏：施展一般主動技能或法術時，觸發 1.0 ~ 1.5 秒（預設 1200ms）的 GCD。
   - 處於 GCD 期間時，無法連續連發其他觸發 GCD 的技能（阻擋手動點擊連打或腳本無腦連發）。
   - 特定防禦技能、應急藥水或解控技能可配置為「離線 GCD (Off-GCD / `triggersGcd: false`)」，提供瞬發救急手感。

2. **施法狀態機 (Casting FSM: Instant vs Casted vs Channeling)**：
   - 技能模板具備 `castTimeMs`（吟唱時間，如強效五行雷法 1500ms、起死回生大術 2000ms；瞬發技能為 0）。
   - 當發起施法時：
     - 若 `castTimeMs == 0`：瞬發結算，觸發 GCD。
     - 若 `castTimeMs > 0`：隊員進入 `CASTING` 狀態，記錄 `castStartTime`、`castEndTime`、`castingSkill`、`castTargetIdx`。
   - 施法中狀態：
     - 普通攻擊暫停。
     - 禁止開啟新技能。
     - 心跳循環 (`DrpgCombatLoop`) 每 500ms 檢查進度，時間到達時自動執行技能效果並廣播「吟唱完成，大招轟出！」。
     - **受擊打斷機制 (Interrupt & Pushback)**：在吟唱期間若遭受昏迷 (Stun) 或高額破防傷害，觸發施法中斷 (`cancelCast`)，並給予文字反饋。

3. **獨立敵方仇恨威脅表 (Per-Enemy Threat Table & Aggro FSM)**：
   - 每個敵人實體 (`BattleEnemy`) 維護獨立的威脅表 `Map<String, Integer> threatTable`（記錄隊員 ID -> 累積仇恨值）。
   - **仇恨獲取規則**：
     - 單體傷害：直接對受擊目標累計 1:1 仇恨。
     - 群體傷害 (AoE)：對所有被擊中的敵人累計仇恨。
     - 隊伍治療 (Heal)：群體或單體治療產生總治療量 50% 的仇恨，平均分攤給當前戰場所有在場敵怪（Healer Aggro！）。
     - 嘲諷 (Taunt)：將嘲諷者在該怪物的仇恨設為 `max(最高仇恨 + 100, 當前仇恨)`，並強行鎖定目標 5 秒。
   - **目標鎖定與轉火 (Aggro Switching)**：
     - 怪物每回合優先鎖定仇恨最高者。
     - 前排 (Front Row) 隊員享有 1.3 倍的有效仇恨加成，後排法師/補師若 OT（超過前排 130% 仇恨），怪物將立刻轉火突襲後排！
   - **怪物「當前鎖定目標」資訊傳遞**：
     - `BattleEnemyViewDto` 新增 `targetMemberId` 與 `targetMemberName`，讓玩家直觀看到「怪物正盯著誰」（目標的目標 Target-of-Target）。

---

## 二、受影響模組與實作任務清單

### 任務 1：領域模型擴展 (Domain Models)
- **`PartyMemberSkill.java`**：
  - 新增 `gcdMs`（預設 1200ms）、`triggersGcd`（預設 true）、`castTimeMs`（預設 0）、`interruptible`（預設 true）。
  - `fromSkillTemplate` 依據 JSON 中 `mechanics.castTime` 或 `mechanics.gcd` 動態解析。
- **`PartyMember.java`**：
  - 新增 `gcdUntil` 欄位與 `isOnGcd()`、`getRemainingGcdMs()`、`triggerGcd(long ms)`。
  - 新增 `CastingState`：
    - `isCasting()`、`getCastingSkill()`、`getCastStartTime()`、`getCastEndTime()`、`getCastTargetIdx()`。
    - `startCasting(PartyMemberSkill skill, int targetIdx, long durationMs)`。
    - `cancelCast(String reason)`。
- **`BattleEnemy.java`**：
  - 新增 `threatTable` (`Map<String, Integer>`)。
  - 新增 `addThreat(String memberId, int amount)`、`getThreat(String memberId)`、`getTopThreatMemberId(Party party)`、`getTargetMemberId()`。
  - 新增 `setTargetMemberId(String id)`、`getTargetMemberName()`。

### 任務 2：仇恨與目標解析升級 (`DrpgEnemyTacticsService.java`)
- 重構 `selectPartyTarget(BattleContext ctx, BattleEnemy enemy)`：
  - 1. 若怪物處於被嘲諷狀態，強制鎖定嘲諷者。
  - 2. 檢驗該怪物的 `threatTable`，計算每名存活隊員的有效仇恨值（前排 x1.3 加成）。
  - 3. 若有最高仇恨者，鎖定為目標並更新 `enemy.setTargetMemberId(target.getId())`。
  - 4. 若全無仇恨，隨機挑選前排活著的隊員，並建立基礎交戰仇恨。

### 任務 3：施法唱條與 GCD 戰鬥循環 (`DrpgCombatLoop.java` & `DrpgBattleService.java`)
- **施法指令入口 (`DrpgBattleService.castSkill`)**：
  - 檢查 `member.isOnGcd()`：若在 GCD 中，提示「招式調息中 (GCD)」。
  - 檢查 `member.isCasting()`：若正在施法，提示「正在引導法術中」。
  - 若 `skill.getCastTimeMs() > 0`：
    - 啟動施法 `member.startCasting(skill, targetIdx, skill.getCastTimeMs())`。
    - 扣除資源（或標記預扣）。
    - 廣播日誌：「🌀 凌霜 開始運轉【九天引雷訣】，周身靈氣匯聚... (吟唱 1.5 秒)」。
    - 觸發 GCD。
  - 若 `skill.getCastTimeMs() == 0`（瞬發）：
    - 正常執行 `applySkillEffects`。
    - 若 `skill.isTriggersGcd()`，觸發 GCD。
- **戰鬥心跳推進 (`DrpgCombatLoop.runBattleLoop`)**：
  - 隊員輪詢時：
    - 若 `member.isCasting()`：
      - 若 `now >= member.getCastEndTime()`：吟唱完成！呼叫 `applySkillEffects`，清理施法狀態，廣播完成日誌。
      - 若尚未完成：跳過普通攻擊，繼續引導。
  - 隊員受擊時 (`victim.takeDamage`)：
    - 若 `victim.isCasting()`：
      - 若受到昏迷 (Stun) 或傷害超過一定閥值（如單次受傷超過最大氣血 20%）：打斷施法 `victim.cancelCast("受創打斷")`，廣播「💥 施法被打斷！」。
- **仇恨傳導加固**：
  - 造成傷害時：`targetEnemy.addThreat(member.getId(), damage)`。
  - 造成治療時：所有在場存活敵人均攤治療仇恨 `enemy.addThreat(member.getId(), healAmount / 2 / enemyCount)`。
  - 施展嘲諷時：`targetEnemy.setTaunt(member.getId(), 5000)`，並賦予額外仇恨。

### 任務 4：DTO 與前端狀態可視化 (`DrpgStateDto.java` & 前端組件)
- **`DrpgStateDto.java`**：
  - `PartyMemberViewDto` 增加：
    - `isOnGcd`: boolean
    - `remainingGcdMs`: long
    - `isCasting`: boolean
    - `castingSkillName`: String
    - `castingDurationMs`: long
    - `castingRemainingMs`: long
  - `BattleEnemyViewDto` 增加：
    - `targetMemberId`: String
    - `targetMemberName`: String
    - `topThreat`: int
- **前端視覺反饋**：
  - 敵人卡片顯示：`🎯 鎖定: 鐵牛 (仇恨: 1250)`。
  - 隊員卡片與技能按鈕：
    - 施法時隊員卡片呈現青藍色吟唱條。
    - 技能按鈕在 GCD 期間顯示半透明調息遮罩。

### 任務 5：自動化純機制測試套件 (`CombatFsmGcdAndThreatTest.java`)
- 測試 1：**GCD 節奏機制驗證**：瞬發技能觸發 GCD，在 GCD 期間再次施放被正確攔截，GCD 結束後恢復可用。
- 測試 2：**施法唱條與完成結算**：技能帶有吟唱時間，發起時進入 CASTING，心跳推進到達時間後正確結算傷害/治療。
- 測試 3：**受擊/昏迷打斷機制**：在吟唱期間遭受昏迷或重創，施法被成功打斷且不結算技能效果。
- 測試 4：**獨立仇恨表與轉火機制**：坦克製造大量仇恨，怪物穩定攻擊坦克；當醫修造成巨額治療產生高仇恨時，怪物正確轉火鎖定醫修。
- 測試 5：**全域回歸測試**：確保全專案所有既有 174 項測試 100% 維持綠燈。

---

## 三、驗證標準 (Definition of Done)
1. `mvnw test` 通過所有測試（174 既有 + 新增 FSM/GCD/Threat 測試），0 失敗、0 錯誤。
2. 保持資料驅動原則：技能的 GCD、吟唱時間、仇恨倍率均由配置或預設機制驅動，無代碼 Hardcode。
3. 更新 `GEMINI.md` 與 `FUTURE_IMPROVEMENTS.md` 狀態。
