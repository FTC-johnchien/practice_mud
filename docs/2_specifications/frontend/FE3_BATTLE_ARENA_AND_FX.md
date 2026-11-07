# ⚔️ 前端 FE-3: 戰鬥擂台與特效引擎深度手冊 (Battle Arena & FX Manual)

> **模組編號**：FE-3  
> **系統類型**：前端表現層模組  
> **核心定位**：管理戰鬥擂台佈局、魔物卡牌即時血條/罡氣韌性條/施法詠唱條渲染、罡氣破防爆裂與受創螢幕微震、動態打擊飄字系統（暴擊/偏斜/五行逆流回血/元素反應），以及 Buff HUD 屬性膠囊聚合。

---

## 🏛️ 一、模組架構與戰鬥擂台佈局

當伺服器推送之 `battle.inBattle === true` 時，前端自動將中央舞台切換為戰鬥擂台（`#battle-arena-panel`）：

```
┌────────────────────────────────────────────────────────┐
│               戰鬥擂台三段式面板佈局                   │
│                (#battle-arena-panel)                   │
├────────────────────────────────────────────────────────┤
│ 1. 上部敵方 5x5 陣線 (#battle-enemies-container)        │
│    • 魔物卡牌 (.enemy-card) 即時 HP 條、Poise 韌性條   │
│    • 施法詠唱條 (.enemy-cast-track / .enemy-cast-fill) │
│    • 聚合 Buff 膠囊 (.buff-aggregated-chip)            │
├────────────────────────────────────────────────────────┤
│ 2. 中部交鋒分割線 (.battle-clash-divider, 2px 流光線)   │
├────────────────────────────────────────────────────────┤
│ 3. 下部三欄戰備甲板 (#combat-command-dock)             │
│    • 左欄 (#dock-actions-column): 集火、奧義、撤退按鈕 │
│    • 中欄 (#battle-party-grid-5x3): 5x3 戰術隊伍盤     │
│    • 右欄 (#dock-content-column): 技能與狀態詳情       │
├────────────────────────────────────────────────────────┤
│ 4. 頂層特效層 (#battle-fx-layer: 飄字與法術流光)       │
└────────────────────────────────────────────────────────┘
```

---

## ⏱️ 二、即時數值儀表與施法詠唱渲染

戰鬥擂台核心託管於 `src/main/resources/static/js/panels/battle-panel.js`：

1. **平滑過渡動畫 (CSS Transition)**：
   - 氣血條（`.enemy-hp-bar`）採用平滑過渡，掉血呈現流暢動畫。
2. **罡氣韌性條 (Poise Bar)**：
   - 即時顯示敵方剩餘韌性值（`.poise-bar-wrap`）。當韌性歸零時切換為力竭狀態（`.poise-bar-broken`）。
3. **施法詠唱條 vs ATB 進度條**：
   - **`【現行已驗證】` 敵方蓄力施法詠唱條 (`.enemy-cast-track` / `.enemy-cast-fill`)**：當 `enemy.isCasting === true` 時顯示蓄力招式名、進度百分比，並以標籤標註 `⚡ 可打斷` 或 `🔒 霸體`。
   - **`【待深入討論／長線演進藍圖】` 敵方常駐普攻 ATB 進度條**：常態普通攻擊時間線進度條列為長線演進藍圖。

---

## 💥 三、破防爆裂與受創反饋 (Poise Break & Impact VFX)【現行已驗證】

結合 `style-02-combat-arena.css` 與 `style-07-battle-fx.css` 實作多層動效：

1. **破防虛弱動效 (`.poise-broken`)**：
   - 當敵方韌性歸零時，怪物卡片附加 `.poise-broken` 高亮光暈，直觀提示玩家進入輸出黃金時間；飄字發射 `.ftext.ft-poise-break` / `.ft-stagger-crit`（`💥 破防重創!`）。
2. **打斷反制失衡震顫 (`.react-interrupted`)**：
   - 當敵方在蓄力前搖窗口內被眩暈（Stun）或打斷技能命中時，卡片觸發 600ms 高頻震顫、猩紅金電邊框閃爍，並即時噴發 `⚡ 打斷！`（`.ftext.ft-interrupt`）。支援 `@media (prefers-reduced-motion)` 無障礙自動降級。
3. **螢幕微震動效 (`.arena-shake`)**：
   - 當生靈遭受重度暴擊或破防重擊時，由 `battle-fx-director.js` 的 `shakeArena()` 為擂台容器附加 `.arena-shake` 觸發 150ms 震顫。

---

## 🍃 四、多樣態動態打擊飄字系統 (Floating Combat Text)【現行已驗證】

戰鬥中的每一次結算由 `src/main/resources/static/js/fx/floating-text.js`（`spawnFloatingText`）在目標上方生成短暫浮動文字：

| 戰鬥結算事件 | 飄字樣式類別 | 顏色風格 | 呈現效果 |
| :--- | :--- | :--- | :--- |
| **常規物理命中** | `.ftext.dmg-phys` | 純白微亮 | `-45` 向上浮動淡出 |
| **五行法術傷害** | `.ftext.dmg-fire` / `.dmg-ice` / `.dmg-lightning` / `.dmg-poison` 等 | 靈氣專屬色碼 | 烈火赤紅、玄冰淡藍、天雷亮紫、毒素暗綠 |
| **致命暴擊** | `.ftext.ft-crit` | 金黃璀璨 | `★ CRIT -112 ★` 放大彈跳 |
| **破防重創** | `.ftext.ft-stagger-crit` | 紫紅烈焰 | `💥 破防重創!` 伴隨微震 |
| **蓄力打斷反制** | `.ftext.ft-interrupt` | 猩紅金電 | `⚡ 打斷！` 爆裂震顫 |
| **氣血回復** | `.ftext.ft-heal` | 翡翠靈綠 | `+38` 伴隨甘霖升騰 |
| **護盾吸收** | `.ftext.ft-absorb` | 護盾金黃 | `[🛡️ 吸收]` 吸收減免 |
| **身法閃避** | `.ftext.ft-miss` / `.ft-dodge` | 青灰幽影 | `閃避 (Dodge)` 橫向側移 |
| **護甲招架偏斜** | `.ftext.ft-parry` | 秘銀亮藍 | `招架 (Parry -50%)` |
| **盾牌格擋** | `.ftext.ft-block` | 鋼鐵銀灰 | `格擋 (Block)` 堅甲反彈 |

---

## 🧪 五、Buff HUD 圖標與膠囊聚合 (`buff-hud-helper.js`)【現行已驗證】

生靈身上的 Buff / Debuff 透過 `src/main/resources/static/js/core/buff-hud-helper.js` 進行標準化聚合：
- **聚合膠囊 (`renderAggregatedBuffBadges`)**：將同類屬性 Buff 聚合渲染為 `.buff-aggregated-chip` 膠囊，顯示靈氣圖標、每秒總值、堆疊層數 `xN`、剩餘秒數 `(Ns)` 與懸停 `title` 明細。
- **倒數進度光圈【待深入討論／長線演進藍圖】**：環繞圖標邊緣的環形 SVG 倒數光圈列為後續視覺擴充藍圖。

---

## ⚡ 六、高頻戰鬥效能與 DOM 節點防護

1. **節點數量保護**：`floating-text.js` 設有同時存活上限 **`MAX_ALIVE_TEXTS = 24`**。當畫面上飄字超過 24 個時立即清除最舊節點，動畫結束（800ms）後自動執行 `el.remove()` 釋放記憶體。
2. **無障礙自動降級**：透過 `@media (prefers-reduced-motion: reduce)` 自動關閉螢幕震顫與高頻震盪。

---

## 🔗 七、代表檔案清單

| 檔案路徑 | 模組定位 | 關鍵導出項目 |
| :--- | :--- | :--- |
| `src/main/resources/static/js/panels/battle-panel.js` | 戰鬥擂台主控制器 | `renderBattleArena()`, `renderBattlePartyQuickBar()`, `renderCombatCommandDock()`, `hideBattleArena()`, `selectBattleTarget()`, `toggleBattleMode()`, `selectPartyMemberForSkill()` |
| `src/main/resources/static/js/core/buff-hud-helper.js` | Buff 聚合與狀態輔助器 | `aggregateBuffs()`, `renderAggregatedBuffBadges()`, `renderShieldOverlayHtml()`, `renderPoiseBarHtml()`, `resolveStatusPulseClass()` |
| `src/main/resources/static/js/fx/battle-fx-director.js` | 戰鬥動效編排器 | `fxDirector` (`enqueue()`, `playEvent()`, `clear()`) |
| `src/main/resources/static/js/fx/floating-text.js` | 打擊飄字產生器 | `spawnFloatingText()`, `spawnInterruptFloatingText()`, `spawnElementalReactionFloatingText()` |
| `src/main/resources/static/js/fx/fx-geometry.js` | 幾何座標換算器 | `getUnitRect()`, `getUnionRect()` |
| `src/main/resources/static/js/fx/fx-presets.js` | 動效預設配置表 | `FX_PRESETS`, `resolvePreset()` |
| `src/main/resources/static/css/style-02-combat-arena.css` | 戰鬥擂台幾何排版 | `.battle-arena-panel`, `.enemy-card`, `.enemy-hp-bar`, `.poise-bar-wrap`, `.enemy-cast-track` |
| `src/main/resources/static/css/style-03-battle-actions.css` | 戰備甲板與技能卡牌 | `.battle-lower-combat-deck`, `.dock-skill-card`, `.skill-cd-overlay` |
| `src/main/resources/static/css/style-07-battle-fx.css` | 打擊飄字與震屏特效 | `@keyframes ft-float-up`, `.ftext`, `.arena-shake`, `.poise-broken`, `.react-interrupted` |

---

## 🚀 八、未來架構演進規劃 (Roadmap)【待深入討論／長線演進藍圖】

1. **首領蓄力行動預告警示 (Boss Telegraph Action Bar)**：長條倒數光暈與破防定格衝擊（Hit Stop）。
2. **預分配 30 節點循環 DOM Pool**：面對超高頻 AOE 傷害時循環複用固定節點。
3. **Web Animations API (WAAPI) 驅動管線**：全面委託瀏覽器合成器執行緒實現滿幀流暢度。
