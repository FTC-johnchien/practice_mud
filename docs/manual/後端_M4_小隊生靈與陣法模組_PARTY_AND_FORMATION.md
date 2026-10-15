# 👥 後端 M4: 小隊、生靈與陣法模組深度手冊 (Party, Living & Formation Manual)

> **模組編號**：M4  
> **系統類型**：後端核心業務模組  
> **核心定位**：管理 5 人冒險小隊編制、道基五維體系、道心理智（SAN 值）四階段異變、夥伴招募好感度，以及古仙陣法排位引擎（FormationEngine）。

---

## 👥 一、5 人標準小隊編制模型 (Party Roster)

在暗黑克蘇魯修仙世界中，單打獨鬥極難生還。遊戲核心圍繞 **滿編 5 人戰術小隊** 展開：

```
                    ┌─── [ 敵 方 陣 線 ] ───┐
                               ▲
                      近戰交鋒 │ (承傷/破防)
                               ▼
[前衛 Front Row, 2人] :  【 主角 / 道主 】   【 先鋒 · 鐵牛 】
                               ▲
                      掩護增益 │ (機動切入)
                               ▼
[中衛 Middle Row, 1人]:        【 遊俠刺客 · 燕青 】
                               ▲
                      遠程護持 │ (法術/治療)
                               ▼
[後衛 Back Row, 2人]  :  【 冰魄符師 · 凌霜 】 【 醫道長老 · 墨衍 】
```

### 1. 站位定義 (`RowPosition`)
- **`FRONT` (前衛)**：承受敵方近戰普攻的主要目標。享有前排格擋加成與嘲諷仇恨放大。
- **`MIDDLE` (中衛)**：全能突擊位。在前排健在時享有掩護，近戰遠程皆宜。
- **`BACK` (後衛)**：法術轟擊與治療輔助核心。當前中衛存活時，免疫大部分單體近戰鎖定。

---

## ☯️ 二、道基五維與道心理智體系 (Attributes & Sanity)

所有生靈（玩家、夥伴、NPC、魔物）均繼承標準屬性模型：

### 1. 道基五維 (Five Base Attributes)
- **體質 (CON)**：$\text{MaxHP} = 100 + \text{CON} \times 15$；決定受創韌性上限（$\text{MaxPoise} = 30 + \text{CON} \times 2$）。
- **真元 (SPI / INT)**：$\text{MaxMP} = 50 + \text{SPI} \times 10$；決定道術威力與真元回復速度。
- **神識 (PER)**：決定暴擊機率、法術穿透，以及對不可名狀精神污染的抗性。
- **根骨 (STR)**：決定物理護甲減傷、招架減免比例，以及重型兵刃傷害加成。
- **身法 (AGI)**：決定 ATB 攻擊間隔加速比率、閃避機率與戰鬥先手權。

### 2. 道心理智與瘋狂異變 (Sanity & Madness)
- **初始值**：100 點 SAN。
- **異變階段**：
  - $\text{SAN} > 75$：**【清醒平靜 (NORMAL)】**，五維發揮 100%。
  - $50 < \text{SAN} \le 75$：**【心生疑竇 (WARNING)】**，偶爾聽見呢喃低語，閃避率微降 5%。
  - $20 < \text{SAN} \le 50$：**【理智崩潰 (DANGER)】**，攻擊命中產生幻覺偏差，隨機目標鎖定。
  - $\text{SAN} \le 20$：**【徹底瘋狂 (MADNESS)】**，道心破碎，攻擊力暴增 50% 但防禦歸零，每秒扣減自身氣血！

---

## 🔮 三、古仙陣法引擎 (FormationEngine)

陣法不僅是靜態名詞，而是帶有數學演算的即時加成引擎（[`FormationEngine.java`](file:///c:/Workspace/my_practice/practice_mud/src/main/java/com/example/htmlmud/domain/party/service/FormationEngine.java)）：

### 1. 存活孔位自動排位映射 (Auto-mapping)
當戰鬥中某位隊員不幸陣亡時，陣法不會崩潰，而是觸發動態重映射：
- 陣亡隊員自動釋放孔位。
- 存活的隊員依序遞補孔位增益（Slot 0..N-1），確保殘局下陣法依然維持運作。

### 2. 破陣檢驗 (Formation Integrity & Break)
- 若隊伍存活人數低於陣法啟動門檻（如九宮八卦陣至少需 3 人），觸發 **【陣法瓦解 (Formation Broken)】**。
- 全隊陣法加成瞬間失效，防禦與身法大幅降低，進入生死考驗。

---

## 🔗 四、代表類別與資料設定檔

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **小隊服務** | `com.example.htmlmud.domain.service.PartyService` | 小隊創建、隊員招募、陣容調整 |
| **生靈管理** | `com.example.htmlmud.domain.service.LivingService` | 生靈屬性快照、氣血扣除與死亡狀態 |
| **陣法引擎** | `com.example.htmlmud.domain.party.service.FormationEngine` | 站位加成計算、存活孔位動態映射與破陣檢驗 |
| **小隊模型** | `com.example.htmlmud.domain.party.model.Party` | 持有 5 人小隊隊員與小隊儲物袋 |
| **資料設定** | `src/main/resources/data/formations/formations.json` | 陣法孔位、攻防係數與啟動門檻宣告 |
| **資料設定** | `src/main/resources/data/companions/default_companions.json` | 初始可招募夥伴模板（鐵牛、凌霜等） |

---

## 🚀 五、未來架構演進與現代化擴充規劃 (Future Roadmap & Scalability)

為使小隊生靈與陣法系統達到如《暗黑地牢》、《博德之門3》的隊伍管理與心理博弈高度，確立以下四大演進方向：

### 1. 🎯 現代遊戲機制深化 (Modern Gameplay & Depth)
- **心魔奪舍與異變討伐 (Madness Possession & Core Extraction)**：
  當隊友道心（SAN 值）歸零並長期處於崩潰狀態時，觸發「心魔奪舍·走火入魔」——該隊友暫時失去理智，攻擊力暴增但有 40% 機率誤傷同門。若未能及時清心辟邪，隊友將徹底異化為敵對精英 Boss！小隊討伐擊敗後可剖出其凝結之【道核】，供其他隊員吸收傳承其核心天賦絕學，將克系悲壯宿命感推至極致。
- **隊員戰術方針 AI 系統 (Gambit / Behavior System)**：
  引入類似 FF12 的戰術指令宣告（例如：「隊友氣血 $< 30\%$ ➔ 施放回春術」、「敵方進入破防狀態 ➔ 釋放高削韌巨錘」）。玩家可預先配置小隊在半即時戰鬥中的自動應對行為，大幅減輕多線操作負擔。

### 2. 🛡️ 穩定性與狀態防禦 (Robustness & Resilience)
- **屬性修飾管道 (Stat Modifier Pipeline)**：
  生靈五維屬性計算全面遷移至純函式 Modifier 裝飾器模式（`BaseStats` ➔ `EquipModifiers` ➔ `FormationModifiers` ➔ `BuffModifiers` ➔ `FinalStats`）。消除屬性手動加減過程中的數值漂移，天然保證任何狀態失效時百分之百安全回退。
- **陣亡孔位防空防禦與併發隔離**：在極限殘局中（如 4 位隊員同時遭受 AOE 暴擊陣亡），`FormationEngine` 提供原子性 CAS 孔位解鎖，嚴防併發競態引發空指針或陣容破裂崩潰。

### 3. ⚡ 效能與記憶體最佳化 (Performance & Concurrency)
- **髒標記（Dirty Flag）快取重算機制**：
  生靈的最終衍生屬性（最大氣血、圓桌閃避率、攻擊力）快取化，僅在穿脫裝備、陣法切換或獲得長效 Buff 時標記為 Dirty 並非同步重算，高頻 Tick 迴圈直接唯讀快取，CPU 運算量節省 85%。

### 4. 🔌 可擴充性與模組化 (Extensibility & Modding)
- **靈寵與機關傀儡戰術召喚位 (Summon & Familiar Slot)**：
  為小隊擴充獨立的「伴生靈寵/護山神獸」槽位（不佔用 5 人主要編制），靈寵具備獨立行動條與被動庇護光環，模型設計直接繼承 `LivingStats`，具備極強的擴充彈性。
