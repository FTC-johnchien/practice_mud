# 02. 可組隊夥伴 NPC (Recruitable Companions)

> **文件定位**：說明小隊中可結納入隊的夥伴角色（Companion）、其身分背景、預設站位、招募與離隊流程，以及 Gambit 自動戰術方針系統。

---

## 🎯 核心設計概念
玩家在遊戲中不是孤身作戰，而是率領一支最多 5 人的「問道旅團」。
夥伴具備獨立的人格、身分、五維道基、初始裝備、專屬技能池，並可透過指令或彈窗在城鎮客棧中自由招募（`recruit`）與請離（`dismiss`）。在戰鬥中，非隊長夥伴由高度可自訂的 Gambit 戰術方針 AI 驅動。

---

## ✅ 目前專案中【已完成實作】

### 1. 預設四大名宿夥伴 (`data/companions/default_companions.json`)

| 夥伴名稱 | 角色代號 | 門派/稱號 | 職業 | 資源偏好 | 預設站位 | 核心特色與擅長武學 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **鐵牛** | `tie_niu` / `iron` | 搬山力士・玄甲體修 | WARRIOR | SP (舊Rage) | **前衛 (FRONT)** | 高防護體盾擊、持錘裂地、嘲諷吸怪 (`tank_taunt`, `tank_smash`)、習得被動《鐵布衫》。 |
| **燕青** | `yan_qing` / `yan` | 赤霞傳人・追魂遊俠 | ROGUE | SP (舊Combo) | **前衛 (FRONT)** | 雙持匕首、高爆擊靈巧、瞬影連斬 (`rogue_shadow_strike`, `rogue_seven_star`)、習得被動《梯雲縱》。 |
| **凌霜** | `ling_shuang` / `ling` | 妙手醫仙・百草丹修 | CLERIC | MP | **後衛 (BACK)** | 懸壺濟世、氣血單體急救 (`heal_single`)、全隊群療辟邪清心 (`heal_all_purify`)、回穩道心。 |
| **墨衍** | `mo_yan` / `mo` | 九幽冥客・幽冥符修 | MAGE | MP | **後衛 (BACK)** | 太陰定身符 (`taoist_seal`)、五雷天罡決 (`taoist_thunder`)、法術遠程轟炸、控制壓制。 |

### 2. 結納招募與請離回客棧
- **招募指令**：`recruit <名稱/ID>` 或 `party recruit <名稱/ID>`（客棧內點擊 `[🤝 招募]`）。
  - 自動校驗旅團 5 人上限。
  - 成功加入後自動指派預設站位並廣播最新狀態。
- **請離指令**：`dismiss <名稱/ID>` 或 `party dismiss <名稱/ID>`（隊員卡片點擊 `[👋 請離]`）。
  - 主角/隊長無法請離。
  - 離隊隊友抱拳告辭，安然返回新手村客棧等候再次相邀。
- **架構收斂**：招募與請離邏輯已於 Phase 8 完全收斂至 `PartyService.recruitCompanionForPlayer` 與 `PartyService.dismissCompanionForPlayer`，指令層完全解耦。

### 3. 戰術方針 AI 系統 (Gambit System)
- **架構**：每個夥伴可配置多條具備優先級 (`priority`) 的自動行為方針規則 (`TacticsRule`)。
- **條件庫 (`TacticsCondition`)**：
  - `ALWAYS`：無條件常駐施展。
  - `ALLY_HP_LESS_THAN`：任意隊友氣血低於指定百分比（例如低於 50% 觸發急救）。
  - `SELF_HP_LESS_THAN`：自身氣血告急時觸發防禦/護體。
  - `RESOURCE_GTE`：資源高於指定點數（例如 SP >= 40 施展爆發大招）。
  - `ENEMY_COUNT_GTE`：敵怪存活數量 >= 指定值（觸發全體 AOE 橫掃）。
  - `ENEMY_IS_BOSS`：敵怪為首領或具備高血量時觸發。
- **目標庫 (`TacticsTarget`)**：
  - `LOWEST_HP_ALLY`：受傷最重、生命百分比最低之隊友。
  - `CURRENT_ENEMY`：當前交鋒敵怪。
  - `ALL_ENEMIES`：全體敵群。
  - `SELF`：自身。
- **配置方式**：
  - 指令支援：`party tactics <idx>`、`party tactics add <idx> <cond> <target> <skillId>`。
  - UI 支援：在小隊視窗 `TACTICS` 子頁籤具備可視化下拉選單、優先級拖曳與啟用開關。

### 4. 夥伴走火入魔與深淵異化
- 夥伴 SAN 降至 0 時，戰鬥中可能狂亂失控背刺隊友。
- 若夥伴在深淵陣亡，有機率異化為深淵畸變體（`aberration-*`），戰力暴增反咬隊伍，成為殘酷的戰術博弈點。

---

## ⏳ 先前討論【尚未實作】

1. **夥伴專屬好感度與道心羈絆 (Affection & Bonds)**：
   - 歷經生死戰鬥、贈送靈藥法寶增加好感度。
   - 好感度達到「生死之交」解鎖專屬二人專屬情緣合擊技（如燕青與凌霜的「劍膽琴心」）。
2. **夥伴離線/待命派遣歷練 (Companion Expeditions)**：
   - 留守客棧的夥伴可被委派前往外圍森林採集靈草或採集黑鐵礦石，定期帶回材料。
3. **夥伴自訂外觀與稱號進階 (Title Promotion)**：
   - 隨境界提升，稱號可從「浪跡散修」晉陞至「一代宗師」。

---

## 💡 架構師建議【未來擴展】

1. **酒館隨機散修生成器 (Procedural Mercenaries)**：
   - 除了固定四大名宿外，客棧定時隨機刷新 1~2 名不同門派與性格的隨機散修，增加單機隨機性。
2. **走火入魔心理創傷 (Psychological Afflictions)**：
   - 仿照《暗黑地牢》(Darkest Dungeon)，當夥伴道心崩潰時，除了入魔外，也可隨機產生「畏怯」（拒絕站前排）或「暴虐」（搶先出手攻擊）。
