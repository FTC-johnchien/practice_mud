# 實施計畫：WoW 經典法術書典籍、戰鬥技能抽屜 In-place 防閃爍與敵群集火鎖定

## 1. 背景與動機
- 隨著技能與普攻套路增加，舊有單一彈窗容易擠壓變形、塞不下更多招式。
- 戰鬥中每 500ms 心跳廣播狀態時，前端 DOM 樹若全量銷毀重建，會導致懸停閃爍以及按鈕點擊被吞噬。
- 敵怪多目標時，無法彈性佈局且集火目標鎖定存在索敵 index 0 的 Bug 與光環丟失問題。

## 2. 架構決策與實施方案 (Architecture Decisions)

### 2.1 魔獸世界經典風格修仙武學典籍 (Spellbook Component)
- 彈窗架構（`C` 鍵 / `P` 鍵）：
  1. **右側垂直 Tab 標籤列**：清晰劃分【🗡️ 兵刃套路】、【⚡ 門派絕技】、【☯️ 陣法奧義】。
  2. **左側雙欄網格卡片書頁**：38x38 典雅浮雕圖示、武學名稱、消耗類型、修為心法描述與操作按鈕（啟用主力/快捷施展/參悟中標籤）。
  3. **底部分頁控制器**：支援 `◀ 上一頁`、`下一頁 ▶`，每頁穩定展示 4 門武學，無論習得多少神功絕技皆永不擠壓變形。

### 2.2 戰鬥技能抽屜懸停防閃爍與點擊防丟失 (`drpg-view.js`)
- 在 `renderSkillDrawer` 與 `renderBattlePartyQuickBar` 引進 In-place DOM 局部更新機制：
  - 記憶體快取技能按鈕節點，比對 key/id 進行數值增量更新，避免整塊 `innerHTML = ''`。
  - 按鈕節點永久常駐，懸停流暢無痕且 100% 響應點擊。

### 2.3 敵群 1~7 體彈性陣列與精準集火鎖定 (`drpg-view.js` & `BattleContext.java`)
- 敵群佈局：自適應 CSS Grid 雙排陣列，支援 1 至 7 隻敵怪同場對峙。
- 集火目標鎖定：
  - 修復 `BattleContext.getFrontTargetEnemy()` 改為嚴格優先遵循玩家指定的 `selectedTargetIndex`。
  - 前端收到廣播時比對 `battle.selectedTargetIndex === e.index || e.isTarget` 並配合樂觀更新，確保玩家點擊任意目標時金紅色光環精準常駐。

## 3. 驗證與測試
- 單元測試：`DrpgBattleServiceTest.java`
  - `testTargetSwitchingAndFocusFire` 測試集火轉火鎖定與仇恨。
- 前端互動測試：驗證 Tab 切換、分頁器翻頁、按鈕點擊防閃爍。
