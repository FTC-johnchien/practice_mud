# Claude Code Review & UI/UX 重構建議

> **文檔更新時間**：`2026-09-27 00:20 (UTC+8)`
> **主題**：探索舞台、戰鬥介面、隊伍 HUD 與 Footer Bar 全域佈局重構建議
> **參考文件**：[Copilot_Code_Review.md](./Copilot_Code_Review.md)、[Gemini_Code_Review.md](./Gemini_Code_Review.md)

---

## 三份建議的共識與分歧摘要

在深入給出自己的建議之前，先整理 Copilot 與 Gemini 兩份建議中**已達共識**與**仍有分歧**的要點，方便你做決定：

### ✅ 三方共識（可直接推進）

| # | 議題 | 共識 |
|---|------|------|
| 1 | 字體下限 | 全部 ≥ 13px，次要提示可降對比度但不可降字級 |
| 2 | 前/後衛 | 前端完全移除 badge、設定按鈕與換位 UI |
| 3 | Footer 存讀檔/陣法/探查/休息 | 全部移除，這些入口已存在於選單或探索區 |
| 4 | 隊伍 HUD 精簡化 | 只保留 `#編號 姓名 Lv HP/MP/SP/SAN` + 必要 Buff |
| 5 | 四資源並列 | 所有角色同時顯示 HP/MP/SP/SAN，不再 MP-or-SP 二選一 |
| 6 | Footer 方向鍵 | 移除，探索區已有更好的羅盤 |

### ⚠️ 尚有分歧（需要你決策）

| # | 議題 | Copilot | Gemini | 我的立場 |
|---|------|---------|--------|----------|
| A | Footer 高度 | 48~64px context bar | 32~36px hint bar | **見下方第 3 點** |
| B | Footer 是否保留文字輸入 | 保留 cmd input | 不提及 | **移入終端 modal** |
| C | 後端 `RowPosition` 是否移除 | 保留 DTO 相容欄位，前端不渲染 | 後端也廢除 | **分階段，見下方第 2 點** |
| D | 主畫面欄位比例 | 未明確 | 左 60% / 右 40% | **依內容動態分配** |
| E | 隊伍 HUD 位置 | 固定 Summary Rail（獨立於探索區） | 整合至探索舞台底部 | **見下方第 4 點** |
| F | 試道木樁/充能按鈕 | 移入開發者終端 | 不提及 | **同意 Copilot，移入終端** |

---

## 我的建議

### 1. 全域佈局重構：從「固定像素」到「語義區域」

#### 問題根因的精確診斷

從截圖與程式碼中可以量化問題的嚴重程度：

- **CSS 中低於 13px 的 `font-size` 宣告**：超過 140 處（8px ×2、8.5px ×1、9px ×12、10px ×18、11px ×46、12px ×39）
- **JS 中 inline style 寫死小字級**：`party-hud-panel.js` L116 (`11px`)、L121 (`11px`)、L124 (`10px`) 等
- **固定像素高度造成內容溢位**：`.party-card min-height: 46px`、`.dpad-btn 36px`、`.combat-command-dock height: 142px`
- **Party HUD 固定 6 欄 grid**：`grid-template-columns: repeat(6, 1fr)` — 但隊伍上限實為 5 人

#### 重構策略：不要用「換一套固定數字」替代「另一套固定數字」

我**不建議** Gemini 所提的「直接把所有 11px 改成 13px 然後重新算固定高度」。這只是把問題推遲到下一次需求變更。

**核心原則**：讓內容決定容器，而非容器裁剪內容。

```css
/* ❌ 舊做法：固定像素 */
.party-card { min-height: 46px; }
.combat-command-dock { height: 142px; min-height: 142px; max-height: 142px; }

/* ✅ 新做法：語義約束 */
.party-card { min-height: 0; padding: 6px 8px; }
.combat-command-dock { min-height: 120px; max-height: 25vh; }
```

#### 具體佈局架構

我同意 Copilot 的「四區 shell」思路，但要更明確定義各區的 CSS 約束：

```
┌─────────────────────────────────────────────────────────┐
│ Global Header (flex-shrink: 0, 約 40px)                 │
│ 模式 badge ‧ 陣法靈威 ‧ 奧義按鈕                         │
├───────────────────────┬─────────────────────────────────┤
│                       │                                 │
│  Context Stage        │   MUD Log / 戰報                │
│  (flex: 3)            │   (flex: 2, min-width: 320px)   │
│                       │                                 │
│  探索: 羅盤+NPC+掉落  │   font-size: 14px              │
│  or 戰鬥: 敵我+技能台  │   overflow-y: auto             │
│  or 地牢: 10x10 雷達  │   純文字+自動滾動               │
│                       │                                 │
│  ┌─────────────────┐  │                                 │
│  │ Party Summary   │  │                                 │
│  │ Rail (嵌入底部)  │  │                                 │
│  └─────────────────┘  │                                 │
├───────────────────────┴─────────────────────────────────┤
│ Context Action Bar (flex-shrink: 0, 36~48px)            │
│ [選單(C)] [行囊(B)] [終端(~)]  💡 快捷鍵提示            │
└─────────────────────────────────────────────────────────┘
```

#### 字體規範 Token 化

不要在 140+ 處各自設定字級。定義 CSS custom properties 作為全域 token：

```css
:root {
  --font-xs:   13px;   /* 最小可讀，用於 badge、輔助提示 */
  --font-sm:   14px;   /* 正文、按鈕、列表項 */
  --font-md:   15px;   /* 日誌、敘述文本 */
  --font-lg:   16px;   /* 區域標題 */
  --font-xl:   18px;   /* 頁面標題 */
}
```

所有現有 `font-size: Npx` 全面替換為這 5 個 token。未來調整字級只需改一處。

#### 實作策略：漸進式替換，不要一次全改

1. **第一步**：先定義 CSS tokens，把 `style.css` 中的 `font-size` 逐一替換為 token，**暫不改佈局**。這一步可以用腳本批量處理，不會影響功能。
2. **第二步**：把所有 `min-height`/`height` 固定值改為 `min-height: 0` 或基於 `em`/`rem` 的語義值。此步驟可能導致某些區塊高度變化，需要逐一檢視。
3. **第三步**：將 `repeat(6, 1fr)` 改為 `repeat(auto-fit, minmax(180px, 1fr))`，讓 grid 自適應隊員數量。

---

### 2. 移除前/後衛設定與顯示

三方一致同意前端移除。**但後端需要注意以下問題**：

#### 事實盤點：`RowPosition` 目前仍深度耦合於戰鬥系統

根據程式碼分析，`RowPosition` 目前仍**活躍地**用於：

1. **敵怪 AI 仇恨權重** (`DrpgEnemyTacticsService`)：前衛 ×1.3 / 中衛 ×1.0 / 後衛 ×0.8
2. **近戰鎖定** (`DrpgCombatLoop`)：前衛近戰只能打敵方前排
3. **陣法孔位匹配** (`FormationEngine`)：`slot.getRequiredRow()` 與 `member.getRow()` 比對
4. **小隊結構查詢** (`Party.java`)：`getFrontMembers()` / `getBackMembers()`

#### 我的建議：分兩階段處理

**Phase 2a — 前端徹底移除（可立即執行）**：
- 移除 HUD 上的「前衛/後衛」badge（`party-hud-panel.js` L87 `row-${m.row.toLowerCase()}`、L94 `rowBadge`、L115）
- 移除陣法孔位的「⚠️需前衛/後衛」警示文字
- 移除 `party switch` 相關的 UI 按鈕
- 隊員卡片上不再以顏色區分前後衛（紅/藍邊框）

**Phase 2b — 後端遷移至 5×3 陣法座標（需要設計規則遷移，不急）**：
- 將戰鬥引擎的仇恨權重從 `RowPosition` 改為讀取陣法孔位的 X 座標 (0=前/1=中/2=後)
- 將陣法孔位的 `requiredRow` 改為 `requiredColumn` (0/1/2)
- 這是遊戲規則層面的改動，需要設計測試案例驗證戰鬥平衡不受影響
- **RowPosition 欄位暫時保留在 model 中**，由陣法系統自動從 5×3 座標推算並回填，不再暴露給玩家手動設定

> **與 Copilot 的分歧說明**：Copilot 建議「前端不渲染但保留 DTO 欄位」，Gemini 建議「後端也廢除」。我的立場是折衷：前端立即移除，後端保留但從 UI 入口完全封閉，待 Phase 2b 時以 5×3 座標完全取代。

---

### 3. Footer Bar 重新設計

#### 現狀的核心問題

Footer 目前承載了 **9 個按鈕 + 4 個方向鍵 + 1 個文字輸入框**，相當於把整個遊戲的操作面板塞到了一條橫欄裡。這在功能選單完成前有其過渡必要性，但現在許多入口已被選單和探索區取代。

#### 應移除的項目（6 個）

| 項目 | 理由 |
|------|------|
| `▲◀▼▶` 方向鍵 | 探索區已有更大的 3×3 羅盤，鍵盤 WASD 也可用 |
| `💾 存讀檔` | 選單系統頁 + 頂部列 `F5` 已涵蓋 |
| `☯️ 陣法 (F)` | 選單陣法頁 + 頂部陣法靈威區已涵蓋 |
| `👁️ 探查 (I)` | 探索區已有前方探查卡片，鍵盤 `I` 可用 |
| `🌿 休息 (R)` | 應為場景情境互動，不是全域常駐按鈕 |
| `🎯 試道木樁` / `⚡ 充能` | 開發者專用，應移入終端 modal |

#### 應保留的項目（3 個）

| 項目 | 理由 |
|------|------|
| `📜 選單 (C)` | 核心入口，保留 |
| `🎒 行囊 (B)` | 高頻操作（戰鬥中也需要快速使用道具），保留 |
| `⌨️ 終端 (~)` | 開發者入口，但佔用空間極小，保留 |

#### Footer 高度的決策

- Copilot 建議 48~64px → 我認為**偏大**，因為只剩 3 個按鈕 + 提示文字
- Gemini 建議 32~36px → 我認為**合理**，但需要確保按鈕可點擊面積 ≥ 44×44px（行動裝置觸控最低標準）

**我的建議：高度 40px**，內含 32px 按鈕高度 + 4px 上下 padding。

```html
<footer class="context-action-bar">
  <div class="bar-shortcuts">
    <button class="bar-btn" onclick="...">📜 選單 (C)</button>
    <button class="bar-btn" onclick="...">🎒 行囊 (B)</button>
    <button class="bar-btn" onclick="...">⌨️ 終端 (~)</button>
  </div>
  <div class="bar-hints">
    💡 WASD 移動 ‧ C 選單 ‧ B 行囊 ‧ 1~5 招式 ‧ Space 集火
  </div>
</footer>
```

#### 戰鬥模式的 Footer

戰鬥時 hint 內容切換為戰鬥相關提示，按鈕不變。這與 Copilot 的「context action bar」理念一致 — Footer 的內容隨模式語境切換，但結構穩定。

---

### 4. 隊伍 HUD 整合與精簡

#### 位置決策：嵌入 Context Stage 底部

我同意 Gemini 的「整合至探索舞台底部」，而非 Copilot 的「獨立 Summary Rail」。理由：

1. **獨立 Rail 會佔據固定垂直空間**，即使在選單打開時也佔位，浪費
2. **嵌入 Context Stage 底部**可以讓 Log 區域獲得完整的垂直高度，改善日誌閱讀體驗
3. 在戰鬥模式下，HUD 自然成為「我方隊伍」區塊的一部分，不需要另外再渲染一組卡片

#### 成員卡片的精簡規範

每張卡片只保留**即時戰術決策所需**的資訊：

```
┌──────────────────────────────────────┐
│ #1 玄靈子  Lv.1    [🌿] [🛡️] [⚠️]  │
│ HP ■■■■■■■■■■■■■■■■ 120/120         │
│ MP ■■■■■■■■■■■■■■■■  60/60          │
│ SP ■■■■■■■■■■■■■■■■ 100/100         │
│ SAN ■■■■■■■■■■■■■■■ 100/100         │
└──────────────────────────────────────┘
```

#### 應移除的卡片元素

| 目前顯示 | 處置 | 理由 |
|----------|------|------|
| 前衛/後衛 badge | 移除 | 見第 2 點 |
| 職業名稱 (`className`) | 移除 | 選單角色頁有完整資訊 |
| 稱號 (`roleTitle`) | 移除 | 非即時戰術資訊 |
| 陣法孔位詳情 (`formationSlotName`, bonus, requiredRow) | 移除 | 選單陣法頁有完整資訊 |
| 自由點數膠囊 (`freeStatPoints`) | 移除 | 選單角色頁處理 |
| GCD 倒數文字 | 保留 | 即時戰術資訊 |
| 施法進度條 | 保留 | 即時戰術資訊 |
| 走火入魔進度 | 保留 | 即時生存警告 |
| Buff/Debuff | **精簡保留** | 只顯示圖示，hover 顯示詳情 |

#### Grid 修正

```css
/* ❌ 舊做法 */
.party-members-list {
  grid-template-columns: repeat(6, 1fr);
}

/* ✅ 新做法：自適應隊員數量，最少 180px 寬 */
.party-members-list {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 6px;
}
```

這樣 1 人時佔滿寬度、5 人時均分、窄螢幕時自動換行。

---

### 5. 四資源並列：HP / MP / SP / SAN

#### 後端改動（小且安全）

`DrpgStateDto.java` 的 `PartyMemberViewDto` 目前已有 `mp`/`maxMp` 欄位。只需新增 `sp`/`maxSp` 獨立欄位，保留 `resourceType`/`currentResource`/`maxResource` 作為向後相容：

```java
// PartyMemberViewDto record 新增
int sp,          // m.getCurrentSp()
int maxSp,       // m.getMaxSp()
// 保留既有的 resourceType, currentResource, maxResource 以相容舊前端
```

#### 前端改動

`party-hud-panel.js` 中，將目前的「resourceType 判斷→單一資源條」邏輯替換為固定 4 條：

```javascript
// ❌ 舊做法：resourceType 二選一
const resType = m.resourceType || 'MP';
// ... if/else 判斷顯示 MP 或 SP

// ✅ 新做法：固定四條
const bars = [
  { label: 'HP',  cur: m.hp,  max: m.maxHp,  cls: 'hp-fill'  },
  { label: 'MP',  cur: m.mp,  max: m.maxMp,  cls: 'mp-fill'  },
  { label: 'SP',  cur: m.sp,  max: m.maxSp,  cls: 'sp-fill'  },
  { label: 'SAN', cur: m.san, max: m.maxSan, cls: 'san-fill' },
];
```

#### 角色尚無某資源時的顯示

同意 Copilot 的建議：**不可靜默消失**。若角色的 `maxSp === 0`（尚未解鎖戰技），仍顯示該資源條但以灰色 `—` 或 `0/0` 表示「未啟用」。這確保 4 條 bar 的視覺佈局始終一致，不會因角色不同而跳動。

---

## 建議實作順序

| 階段 | 內容 | 影響範圍 | 風險 |
|------|------|----------|------|
| **Phase 0: CSS Token 化** | 定義 `--font-xs` ~ `--font-xl`，批量替換 140+ 處 `font-size` | `style.css` | 低（純 CSS 變更，不影響邏輯）|
| **Phase 1: 資料契約** | `DrpgStateDto` 新增 `sp`/`maxSp` 欄位；補 DTO 測試 | `DrpgStateDto.java` | 低（新增欄位，不改既有欄位）|
| **Phase 2a: 前端移除前後衛** | 移除 HUD badge、色塊、換位按鈕、陣法孔位警示 | `party-hud-panel.js`、`style.css` | 低 |
| **Phase 3: Footer 瘦身** | 移除 6 個按鈕 + D-pad，壓縮為 40px bar | `index.html`、`style.css`、`app.js` | 中（需驗證鍵盤操作仍可用）|
| **Phase 4: HUD 精簡 + 四資源條** | 簡化卡片、固定 4 bar、grid 自適應 | `party-hud-panel.js`、`style.css` | 中 |
| **Phase 5: 探索佈局流式化** | 移除固定高度、羅盤/NPC 清單自適應字體 | `style.css`、`town-panel.js` | 高（涉及多頁面）|
| **Phase 2b: 後端 RowPosition 遷移** | 戰鬥引擎改用 5×3 座標 | `DrpgCombatLoop`、`FormationEngine` 等 | 高（遊戲規則改動，需充分測試）|

> [!IMPORTANT]
> Phase 0 ~ Phase 2a 風險低、改動獨立，建議先快速完成。Phase 5 和 Phase 2b 影響面大，建議各自建立獨立分支並配合充分的迴歸測試。

---

## 最低驗收條件

1. **字體**：探索頁、戰鬥頁、選單頁中所有可讀文字 ≥ 13px（CSS token `--font-xs`）
2. **前後衛**：HUD、Footer、探索區不再出現「前衛/後衛」文字、badge 或設定入口
3. **Footer**：只剩 `選單(C)`、`行囊(B)`、`終端(~)` 三個按鈕 + 快捷鍵提示；高度 ≤ 48px
4. **隊伍 HUD**：每張卡片固定顯示 4 條資源 (HP/MP/SP/SAN)；角色未啟用的資源顯示 `—`
5. **不重複入口**：存讀檔、陣法、方向鍵、探查、休息不會同時出現在兩個以上的地方
6. **隊員數量**：grid 自適應 1~5 人，不再硬編碼 6 欄
7. **功能不退化**：所有鍵盤快捷鍵 (WASD/C/B/F5/~) 仍然可用，只是 UI 按鈕減少
