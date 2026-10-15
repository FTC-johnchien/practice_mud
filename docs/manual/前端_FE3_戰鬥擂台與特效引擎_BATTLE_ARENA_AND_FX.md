# ⚔️ 前端 FE-3: 戰鬥擂台與特效引擎深度手冊 (Battle Arena & FX Manual)

> **模組編號**：FE-3  
> **系統類型**：前端表現層模組  
> **核心定位**：管理戰鬥擂台佈局、魔物卡牌即時血條/罡氣韌性條/ATB 行動進度條渲染、罡氣破防爆裂與受創螢幕微震、動態打擊飄字系統（暴擊/偏斜/五行逆流回血），以及 Buff HUD 圖標懸停氣泡。

---

## 🏛️ 一、模組架構與戰鬥擂台佈局

當伺服器推送之 `battle.inBattle === true` 時，前端自動將主視圖無縫切換為戰鬥擂台：

```
┌────────────────────────────────────────────────────────┐
│                   戰鬥擂台面板佈局                     │
├────────────────────────────────────────────────────────┤
│                  【 敵 方 陣 線 】                      │
│   ┌───────────────────┐     ┌───────────────────┐      │
│   │   嗜煞食腐鼠      │     │  墨竹山異化魔偶   │      │
│   │ HP: [████████░░]  │     │ HP: [██████████]  │      │
│   │ Poise: [████░░░░] │     │ Poise: [████████] │      │
│   │ ATB:   [████░░░░] │     │ ATB:   [██░░░░░░] │      │
│   │ Buffs: [🔥x2] [🛡️]│     │ Buffs: [⚡x1]     │      │
│   └───────────────────┘     └───────────────────┘      │
├────────────────────────────────────────────────────────┤
│                  【 玩 家 戰 術 列 】                   │
│   [集火目標鎖定]  [技能卡牌快捷列 (GCD 光圈倒數)]      │
└────────────────────────────────────────────────────────┘
```

---

## ⏱️ 二、即時數值儀表與 ATB 動態渲染

戰鬥擂台核心託管於 `static/js/panels/battle-panel.js`：

1. **平滑過渡動畫 (CSS Transition)**：
   - 氣血條（`hp-bar`）採用 `transition: width 0.25s ease-out`，在遭受連續攻擊時呈現流暢掉血動畫。
2. **罡氣韌性條 (Poise Bar)**：
   - 即時顯示敵方剩餘韌性值。當前值越低，顏色由金黃漸變為警戒亮橘。
3. **ATB 行動進度條 (Action Progress Bar)**：
   - 根據後端傳入之 `currentProgressMs / actionIntervalMs` 實時推進行動進度，讓玩家清晰預判魔物下一次普通攻擊或戰技爆發的時間點。

---

## 💥 三、破防爆裂與受創反饋 (Poise Break & Impact VFX)

為營造極致打擊反饋感，結合 `style-07-battle-fx.css` 實作多層動效：

1. **破防虛弱動效 (`vulnerable-pulse`)**：
   - 當敵方韌性歸零（Poise Broken）時，怪物卡片周圍立即爆發紫色碎裂粒子動畫，並附加呼吸式高亮光暈，直觀提示玩家進入輸出黃金時間。
2. **螢幕微震動效 (`screen-shake`)**：
   - 當生靈遭受重度暴擊（Critical Hit）或破防重擊時，主容器容器瞬間觸發 150ms 隨機位移微震，強化魂系厚重感。

---

## 🍃 四、多樣態動態打擊飄字系統 (Floating Combat Text)

戰鬥中的每一次結算均在目標上方生成短暫飄浮動畫文字，隨後淡出銷毀：

| 戰鬥結算事件 | 飄字樣式類別 | 顏色風格 | 呈現效果 |
| :--- | :--- | :--- | :--- |
| **常規物理命中** | `.fct-damage` | 純白微亮 | `-45` 向上緩慢浮動 |
| **致命暴擊** | `.fct-crit` | 金黃璀璨 | `★ CRIT -112 ★` 放大彈跳 |
| **五行吸收逆流回血**| `.fct-heal` | 翡翠靈綠 | `+38 [火靈逆流]` 伴隨甘霖光效 |
| **身法閃避** | `.fct-dodge` | 青灰幽影 | `閃避 (Dodge)` 橫向側移 |
| **護甲招架偏斜** | `.fct-parry` | 秘銀亮藍 | `招架 (Parry -50%)` 火星迸發 |

---

## 🧪 五、Buff HUD 圖標與 Tooltip 懸停氣泡 (`buff-hud-helper.js`)

生靈身上的 Buff / Debuff 透過通用輔助模組進行標準化展示：
- **堆疊層數角標 (Badge)**：當 Buff 層數 $> 1$ 時（如中毒 3 層），右下角自動標註數字標籤。
- **倒數進度光圈**：持續時間環繞圖標邊緣逐步遞減。
- **動態 Tooltip 氣泡**：滑鼠懸停即時呈現純文字修仙描述（如：「【火煞灼心】每秒扣減 15 點氣血，剩餘 4.2 秒」）。

---

## 🔗 六、代表檔案清單

| 檔案路徑 | 模組定位 | 關鍵導出項目 |
| :--- | :--- | :--- |
| `static/js/panels/battle-panel.js` | 戰鬥擂台主控制器 | `renderBattlePanel()`, `spawnDamageNumber()` |
| `static/js/helpers/buff-hud-helper.js` | Buff 狀態圖標輔助器 | `renderBuffIcons()`, `buildBuffTooltip()` |
| `static/css/style-02-combat-arena.css` | 戰鬥擂台幾何排版 | `.battle-arena`, `.enemy-card`, `.hp-bar`, `.poise-bar` |
| `static/css/style-03-battle-actions.css` | 戰鬥技能按鈕與冷卻光圈 | `.skill-button`, `.gcd-overlay`, `.target-selector` |
| `static/css/style-07-battle-fx.css` | 飄字、破防粒子與螢幕震動 | `@keyframes float-up`, `.screen-shake`, `.broken-pulse` |

---

## 🚀 七、未來架構演進與現代化擴充規劃 (Future Roadmap & Scalability)

為使戰鬥擂台視覺表現達到「刀光劍影、拳拳到肉、仙法轟鳴」的頂級動態打擊反饋，確立以下四大演進方向：

### 1. 🎯 現代打擊感與視覺反饋 (Modern Combat VFX & Impact)
- **首領蓄力行動預告警示 (Boss Telegraph Action Bar)**：
  配合後端 M3 的蓄力打斷機制，首領頭頂展開具備倒數光暈的蓄力長條（Channeling Bar）。若為不可名狀禁咒，蓄力條呈現血紅觸手律動；當玩家成功削韌破防時，蓄力條爆裂破碎並伴隨長達 300ms 的慢動作定格衝擊（Hit Stop），爽快感極致爆棚。
- **生靈卡牌 CSS 3D 景深傾斜 (3D Card Parallax & Hit Tilt)**：
  卡牌在受到暴擊重砍時，依據受擊方向產生動態 3D 傾斜角度（RotateX / RotateY）與金屬火花，受擊反饋立體感立竿見影。

### 2. 🛡️ 穩定性與節點防禦 (Robustness & Resilience)
- **打擊飄字 DOM 節點物件池 (FCT DOM Pool)**：
  在面對 AOE 仙術（如同時命中 5 隻魔物並觸發多段傷害）時，短時間內可能產生數十個飄字。採用預先分配 30 個節點的循環物件池（Fixed DOM Pool），重複激活與歸還，徹底杜絕頻繁創建與銷毀 DOM 節點造成的記憶體碎屑與 GC 掉幀。

### 3. ⚡ 效能與硬體加速 (Performance & Concurrency)
- **Web Animations API (WAAPI) 驅動管線**：
  全面採用瀏覽器原生的 `element.animate()` 取代傳統 JS 定時器或頻繁操作 `classList`，直接將飄字與震動委託給合成器執行緒（Compositor Thread），在低階手機或老舊 PC 上始終維持 60 FPS 滿幀運行。

### 4. 🔌 可擴充性與音效合成 (Web Audio Synthesizer)
- **純程式碼合成修仙環境音效 (Procedural Audio Synthesis)**：
  整合現代瀏覽器 Web Audio API，無需下載數十 MB 的 MP3 檔案，純以數學震盪器（Oscillator）與濾波器即時合成立體聲修仙音效：金鐵交戈劍鳴聲、罡氣破碎崩裂震音、五行逆流回血靈音與瀕死心跳囈語。
