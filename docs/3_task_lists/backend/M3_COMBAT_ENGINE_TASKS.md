# ⚔️ M3: 戰鬥引擎與規則結算模組實作進度清單 (Combat Engine Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/backend/M3_COMBAT_ENGINE.md`](../../2_specifications/backend/M3_COMBAT_ENGINE.md)  
> **核心職責**：管理 500ms 輕量時間線輪詢 (`DrpgCombatLoop`)、罡氣破防削韌 (Poise)、行動預告打斷 (Interrupt)、九大靈氣元素反應與古仙陣法跨隊員連攜合擊。  
> **當前完成度**：95% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心服務與結算模型**：
  - [`DrpgCombatLoop.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/combat/DrpgCombatLoop.java)（虛擬執行緒戰鬥主迴圈，500ms 心跳）
  - [`CombatRules.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/combat/CombatRules.java)（純函數式數值判定、破防爆擊、元素反應）
  - [`ComboResolver.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/combat/ComboResolver.java)（3.5s 連攜窗口推進與天外合擊終結技發動）
  - [`DrpgEnemyTacticsService.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/combat/DrpgEnemyTacticsService.java)（敵我 AI 行動方針評估器）
- **關聯驗證測試**：
  - [`BossPhaseAndPoiseStaggerTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/BossPhaseAndPoiseStaggerTest.java)（首領轉階段與破防爆擊 100% 綠燈）
  - [`CombatReactInterruptTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/CombatReactInterruptTest.java)（預告蓄力條與打斷 100% 綠燈）
  - [`ElementalReactionAndDetonateTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/ElementalReactionAndDetonateTest.java)（元素反應與 Primer 引爆 100% 綠燈）
  - [`CrossComboAndFormationResonanceTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/CrossComboAndFormationResonanceTest.java)（跨隊員連攜與合擊技 100% 綠燈）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **M3-PKG-ENCAPSULATION (Package-by-Feature 獨立封裝 / Phase 3 Step R3.8)**
  - 建立一級套件 `com.example.htmlmud.combat`，將戰鬥核心迴圈 `DrpgCombatLoop`、`CombatRules`、`ComboResolver`、`BattleContext`、`DrpgBattleService`、`BuffSettlementService`、戰鬥事件與 DTO 等 40 份類別全面收斂至獨立套件，全專案 393 項測試 100% 綠燈通過。
- [x] **M3-POISE-BREAK (罡氣削韌與破防重創)**
  - 實作 `CombatRules.applyPoiseBreakCrit`：目標罡氣歸零 (`isPoiseBroken`) 享受 100% 致命暴擊與 1.5 倍傷害。
- [x] **M3-REACT-INTERRUPT (敵方行動預告與即時打斷)**
  - 敵方蓄力條前搖 (Telegraph)、`CastingState` 狀態流轉，使用反制戰技（如 `taibai_interrupt`）或破防即可中斷施法。
- [x] **M3-ELEMENTAL-REACTION (九大天道靈氣反應)**
  - 實裝純函數式元素反應判定（蒸發 `VAPORIZE` 2.0x 傷、感電 `ELECTRO_CHARGED` 1.5x 傷與同排跳躍打擊、太極混沌湮滅 `CHAOS_ANNIHILATION` 50 削韌強制失衡）。
- [x] **M3-PRIMER-DETONATE (裂魂印記與衝擊波引爆)**
  - Primer 施加標記，Detonator 戰技命中瞬間結算剩餘 DoT 總傷並產生 50% 全場震盪衝擊波。
- [x] **M3-CROSS-COMBO (跨隊員動態連攜與天外合擊)**
  - 3.5 秒動態時間窗口接力打擊：雙星合璧 $\to$ 三才混元 $\to$ 四象誅仙，累積共鳴點手動發動天外合擊終結技。
- [x] **M3-BOSS-PHASES (首領多階段與狂暴過載)**
  - 怪物支援血量閾值（如 `[70, 30]`）多階段轉階段，超載協議震散負面 Debuff、震退全隊施法並獲得 30% 狂暴攻速。
- [x] **M3-PURGE-BETRAYAL (徹底拔除心魔背刺)**
  - 拔除隊員 SAN 歸零後的 40% 心魔攻擊同伴與變身敵方怪物邏輯，純單機化為【精神萎靡】Debuff（-15% 輸出）。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task M3-1 (VS-01-CYBER-SKILLS / ACTIVE-TASK-3): 序章戰技【燃魂溯光·剎那千秋步】與【血肉強殖·玄冥幾丁刃】戰鬥整合驗證**
  - **目標**：在 `DrpgCombatLoop` 實際驗證 `skill_time_tier1_step`（別名 `skill_neural_overclock_step`，Primer 破防 25）接續 `skill_chitin_tier1_blade`（別名 `skill_bio_keratin_blade`，Detonator 破防 45）引爆全場衝擊波。
  - **驗收標準**：戰鬥日誌彩色廣播正確呈現裂魂印記與引爆震盪，`.\test.ps1` 綠燈守護。
- [ ] **Task M3-2 (COMBAT-ACTION-RELOAD): 兵刃熱量/耐久度過載換彈循環**
  - **目標**：機械修真流派高頻飛劍射擊累積過載熱量，達到 100% 需執行 1 回合散熱冷卻換彈動作。
  - **驗收標準**：前端熱量條滿時限制出招，提供手動散熱 Action。
