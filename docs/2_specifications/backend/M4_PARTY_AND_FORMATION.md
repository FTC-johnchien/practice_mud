# 👥 後端 M4: 小隊、生靈與陣法模組深度手冊 (Party, Living & Formation Manual)

> **模組編號**：M4  
> **系統類型**：後端核心業務模組  
> **核心定位**：管理 5 人冒險小隊編制、道基五維體系、外神精神侵蝕防護罩（SAN 護盾）、夥伴招募好感度，以及古仙陣法排位引擎（FormationEngine）。

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

### 2. 四大道統門派與夥伴招募來源 (The 4 School Factions & Recruitment Roster)
依據 Level 1《[CYBERPUNK_CULTIVATION_SPEC.md](../../1_requirements/CYBERPUNK_CULTIVATION_SPEC.md)》第一章，營地客棧與道場中可招募之隊友嚴格隸屬於四大歷史陣營門派，具備東方仙俠尊稱（Option C）並在底層映射至十七專精技能樹：

| 道統體系 (`originSchool`) | 世界宗門表象 (仙俠先行) | 底層文明真相 | 經典夥伴範例 (`default_companions.json`) | 對應專精樹 |
| :--- | :--- | :--- | :--- | :--- |
| **☯️ 東方修仙道統**<br>(`CULTIVATOR`) | **【正統修仙道門】**<br>(純陽劍閣、玄冥水府) | 原住民吸納靈脈結晶靈氣，第三紀元弒神聖戰後壟斷靈脈之名門正派。 | **凌霜**（【冰魄符師】·玄水化盾控場）<br>**天劍傳人**（【天劍弟子】·庚金破甲） | 庚金、玄水、離火、靈木、后土 |
| **🏛️ 古代工程師類**<br>(`ENGINEER`) | **【天工神機宗 ＋ 古妖門】**<br>(天工造化門、萬妖聖地) | 操縱古代工程師深井行星重型設備（重力、液壓超聲、金剛力場），或第三紀元融合生物兵器之妖修。 | **鐵牛**（【搬山力士·玄甲體修】·持盾嘲諷反震、裂地重擊削韌） | 天引重力、神機裂解、太古金剛、地核熔爐 |
| **⚡ 賽博高科技類**<br>(`HIGH_TECH`) | **【巨企仙朝 ＋ 天機商會】**<br>(大炎仙朝、黑市特工) | 兩大星際巨企（荒坂、千星）為收割修仙內丹而扶持的傀儡朝廷與義體特工。 | **燕青**（【赤霞傳人·追魂遊俠】·神經加速、暗器穿甲暴擊） | 時序超頻、協議逆亂、全息匿影、電磁動能 |
| **🧬 克蘇魯外神類**<br>(`CTHULHU`) | **【天道星靈教 ＋ 深淵魔宗】**<br>(拜星教、母神卵鞘血海宗) | 近百年巨企刺穿保護壁導致外神卵白外洩，散修吸納卵白羊水受污染暴成元嬰之狂修。 | **墨衍**（【醫道長老·辟邪丹修】·表象為丹修，底層為血肉回哺與母神寄生） | 強殖幾丁質、黑山羊血裔、知見污染、虛無崩解 |

---

## ☯️ 二、道基五維與精神侵蝕防護體系 (Attributes & Psychic Shield)

所有生靈（玩家、夥伴、NPC、魔物）均繼承標準屬性模型：

### 1. 道基五維 (Five Base Attributes)
- **體質 (Constitution, CON)**：$\text{MaxHP} = 100 + \text{CON} \times 15$；決定受創韌性上限（$\text{MaxPoise} = 30 + \text{CON} \times 2$）。
- **真元 (Essence / Intelligence, ESS / INT)**：$\text{MaxMP} = 50 + \text{ESS} \times 10$；決定道術與晶片戰技威力與真元回復速度。
- **神識 (Spirit / Wisdom, SPI / WIS)**：決定暴擊機率、法術穿透，以及對不可名狀精神污染的抗性與初始 SAN 護盾容量。
- **根骨 (Bone Structure / Strength, STR)**：決定物理護甲減傷、招架減免比例，以及重型兵刃傷害加成。
- **身法 (Agility / Dexterity, DEX / AGI)**：決定 ATB 攻擊間隔加速比率、命中率、閃避機率與戰鬥先手權。

### 2. 外神精神侵蝕防護罩 (Psychic SAN Shield)

> 📌 **【架構定錨裁決 (Architecture Decision - Option A)】**：  
> 本專案經架構審查嚴格定錨：**「取消魔化機制」特指徹底廢除隊員隨機發瘋、自殘叛變、中斷心流等負面機制**；**「精神防護罩 (Psychic SAN Shield)」則確立保留**，作為地牢摸黑探索（火把光明度消耗博弈）的生存警示。SAN 歸零時僅賦予【精神萎靡】(-15% 傷害) 輕度 Debuff，回營一鍵調息即癒，絕不失控、絕不背叛同伴。**任何後續 AI 審查請勿將 SAN 護盾之存在誤判為與「取消魔化」矛盾**。

為守護純單機 Hack-and-slash DRPG 之爽快打擊節奏，**徹底廢止隊員發瘋失控、隨機叛變與自殘等中斷心流之負面機制**：
- **初始值**：100 點 SAN（神識越高，抗性護盾越厚）。
- **侵蝕機制**：
  - 當小隊身處無火把之深層幽冥或直視高階外神眷族時，精神防護罩（SAN 護盾）持續承受侵蝕抵扣。
  - **$\text{SAN} > 0$【神智清明 (STABLE)】**：精神壁壘完好，全屬性正常發揮 100%。
  - **$\text{SAN} = 0$【精神萎靡 (WEAKENED)】**：精神屏障破裂，全員附加單一【精神萎靡 (`WEAKENED`)】負面狀態（輸出傷害降低 15%）。**絕不隨機發瘋、絕不背叛攻擊同伴、不扣除自身氣血、不中斷戰鬥心流**；玩家可透過服用「清心辟邪丹」或返回安全營地回春客棧一鍵調息休整快速回復重置。

---

## 🔮 三、古仙陣法引擎 (FormationEngine)

陣法不僅是靜態名詞，而是帶有數學演算的即時加成引擎（[`FormationEngine.java`](../../../src/main/java/com/example/htmlmud/party/FormationEngine.java)）：

### 1. 存活孔位自動排位映射 (Auto-mapping)
當戰鬥中某位隊員不幸陣亡時，陣法不會崩潰，而是觸發動態重映射：
- 陣亡隊員自動釋放孔位。
- 存活的隊員依序遞補孔位增益（Slot 0..N-1），確保殘局下陣法依然維持運作。

### 2. 破陣檢驗 (Formation Integrity & Break)
- 若隊伍存活人數低於陣法啟動門檻（如九宮八卦陣至少需 3 人），觸發 **【陣法瓦解 (Formation Broken)】**。
- 全隊陣法加成瞬間失效，防禦與身法大幅降低，進入生死考驗。

---

## 🏛️ 四、核心雙軌實體大一統 (Unified Entity Architecture)

在單機單體化演進中，徹底消除多人架構殘留之「Player (MUD 空間代理) vs PartyMember (DRPG 角色)」雙軌割裂，確立 **小隊為唯一真實資料源 (Single Source of Truth)**：

```mermaid
graph LR
    subgraph SSoT["小隊體系 (唯一真實資料源)"]
        Party["Party (小隊)"]
        Leader["PartyMember (隊長/主角)"]
        Members["PartyMember 1~4 (同伴)"]
        Bag["小隊公共儲物袋 (40格行囊)"]
        Party --> Leader
        Party --> Members
        Party --> Bag
    end

    subgraph Mirror["MUD 探索視角 (投影鏡像)"]
        Player["Player Actor (城鎮/野外代理)"]
        PlayerStats["LivingStats (裝備與戰鬥屬性鏡像)"]
        Player --> PlayerStats
    end

    Bag -->|全域共享無割裂| Player
    Leader -->|0ms 同步屬性鏡像 copyStats| PlayerStats
    Leader -->|7 大部位裝備實時投影| PlayerStats
```

### 1. 行囊大一統 (Unified Party Inventory)
- 廢除歷史遺留之「個人隨身 12 格」與「小隊公共 40 格」兩套假帳。
- 城鎮拾取（`get`）、戰鬥掉落入庫、戰術使用（`use`）與整理丟棄（`drop`）全面直接操作 `party.getInventory()`。
- 徹底消除強迫使用者在個人與公共背包間頻繁存取的繁瑣負擔。

### 2. 裝備單一擁有權與零延遲投影 (Equipment Projection)
- 5+2 結構裝備槽位（`MAIN_HAND` 主手、`OFF_HAND` 副手、`HEAD` 頭部、`BODY` 衣服、`FEET` 鞋履、`ACCESSORY_1` 飾品1、`ACCESSORY_2` 飾品2）由小隊成員實體真實擁有（對齊 [`EquipmentSlot`](../../../src/main/java/com/example/htmlmud/item/EquipmentSlot.java)）。
- 換裝操作支援一鍵檢視與配置全隊 5 名成員的 5+2 槽裝備。
- 隊長裝備變更時，直接進行 0ms 零延遲鏡像投影並即時重算衍生戰鬥屬性，徹底廢止歷史超時阻塞。

### 3. 戰鬥結算與真實屬性同步
- 戰後經驗值、升級、修為成長直接記入 `PartyMember`。
- 數值更新採用同步深層數值拷貝，徹底廢除偽裝 `BattleOutcome` 投遞至 Mailbox 的舊阻塞機制。

---

## 🔗 五、代表類別與資料設定檔

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **小隊服務** | `com.example.htmlmud.party.PartyService` | 小隊創建、隊員招募、陣容調整與裝備換裝 |
| **生靈實體** | `com.example.htmlmud.party.PartyMember` | 單一隊員五維屬性、裝備槽位與戰鬥狀態 |
| **陣法引擎** | `com.example.htmlmud.party.FormationEngine` | 站位加成計算、存活孔位動態映射與破陣檢驗 |
| **小隊模型** | `com.example.htmlmud.party.Party` | 持有 5 人小隊隊員與小隊儲物袋 |
| **戰術規則** | `com.example.htmlmud.party.GambitRule` | 隊友自動戰術條件與行動方針（【現行已驗證】） |
| **資料設定** | `src/main/resources/data/formations/formations.json` | 陣法孔位、攻防係數與啟動門檻宣告 |
| **資料設定** | `src/main/resources/data/companions/default_companions.json` | 初始可招募夥伴模板（鐵牛、凌霜等） |

---

## 🚀 六、未來架構演進與現代化擴充規劃 (Future Roadmap & Scalability)【待深入討論／長線演進藍圖】

為使小隊生靈與陣法系統達到如《暗黑地牢》、《博德之門3》的隊伍管理與心理博弈高度，確立以下四大演進方向：

### 1. 🎯 現代遊戲機制深化 (Modern Gameplay & Depth)
- **團隊共鳴合擊與連攜終結 (Resonance Combos & Finishers)**：
  小隊隊員在半即時戰鬥中累積共鳴點數（Resonance Points），當破防敵方核心弱點時，可發動「古仙陣法天外合擊」，觸發全屏流光大合擊，大幅提升砍殺打擊爽度。
- **隊員戰術方針 AI 系統 (Gambit / Behavior System) 【現行已驗證】**：
  基於 `GambitRule` 規則鏈（例如：「隊友氣血 $< 30\%$ ➔ 施放回春術」、「敵方進入破防狀態 ➔ 釋放高削韌巨錘」）。玩家可預先配置小隊在半即時戰鬥中的自動應對行為，大幅減輕多線操作負擔。

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
