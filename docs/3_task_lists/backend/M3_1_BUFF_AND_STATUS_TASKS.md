# 🔮 M3.1: 狀態效果與 Buff 引擎模組實作進度清單 (Buff & Status Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md`](../../2_specifications/backend/M3_1_BUFF_AND_STATUS_EFFECTS.md)  
> **核心職責**：管理 500ms 周期 Tick 結算、五大 Buff 分類互斥疊加、DoT 毒火結算、裂魂標記 (Primer) 壽命與控制狀態。  
> **當前完成度**：95% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心實體與枚舉**：
  - [`BuffCategory.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/model/enums/BuffCategory.java)（`STAT_MODIFIER`, `HOT_DOT`, `CONTROL`, `SHIELD`, `PRIMER`）
  - [`CombatRules.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/combat/CombatRules.java)（狀態反應與結算純函數）
  - [`DrpgCombatLoop.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java)（500ms Tick 周期調度）
- **關聯驗證測試**：
  - [`ElementalReactionAndDetonateTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/ElementalReactionAndDetonateTest.java)（7/7 綠燈）
  - [`CombatReactInterruptTest.java`](file:///c:/Workspace/my_practice/practice_mud/src/test/java/com/example/htmlmud/CombatReactInterruptTest.java)（控制眩暈與打斷 100% 綠燈）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **M3_1-CATEGORIES (五大類別生命週期管理)**
  - 嚴格分離屬性增益 (STAT)、周期 DoT (HOT_DOT)、控制強直 (CONTROL)、護盾 (SHIELD) 與連攜印記 (PRIMER)。
- [x] **M3_1-SOUL-REND (裂魂 Primer 印記壽命)**
  - 裂魂印記具備 8 秒存留窗口，受到 Detonator 攻擊時即時觸發 DoT 壓縮結算並清除對應標記。
- [x] **M3_1-MADNESS-DEBUFF (理智歸零精神萎靡 Debuff)**
  - 隊員 SAN 歸零時掛載純單機【精神萎靡】Debuff，輸出傷害永久打 0.85 折直至營地休整或服藥。
- [x] **M3_1-ENRAGE-CLEAR (轉階段淨化協議)**
  - 首領轉階段過載時自動驅散全場施加於首領自身的負面 DoT 與控制狀態。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task M3_1-1 (AURA-RESONANCE): 陣法同調隊伍光環動態共鳴**
  - **目標**：當隊伍達成特定站位時（如雙前排體質 > 50），自動附加【玄武聚頂】全隊物理護甲 +15% 被動光環。
  - **驗收標準**：隊員出戰動態計算光環，隊員陣亡時光環相應動態減弱或解除。
