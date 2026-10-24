# 🏛️ 後端 M1: 營地中樞與功能設施模組手冊 (Town Hub & Facilities Manual)

> **模組編號**：M1  
> **系統類型**：後端核心業務模組  
> **核心定位**：管理純單機 DRPG 營地（如黑煞宗採掘營、地下避難所、黑市據點）的選單式功能設施（裝備工坊、晶片實驗室、道場修煉、醫療站、地牢傳送門）與 NPC 劇情對話交互契約，徹底取代舊有 MUD 房間 exits 與文字移動機制。

---

## 🏛️ 一、模組職責與架構全覽

在純單機 Hack-and-slash DRPG 架構下，城鎮不再是文字 MUD 走房間的拓撲網絡，而是冒險者整備、劇情推進與戰略決策的**選單式安全營地中樞 (Town Hub)**：

```mermaid
graph TD
    Client["前端營地介面 (FE4 Town Hub Panel)"] -->|ActionCommand: ENTER_FACILITY / INTERACT_NPC| Pipeline["M9 動作管線 (ActionPipeline)"]
    Pipeline --> HubService["TownHubService 營地中樞服務"]
    
    HubService --> FacForge["裝備工坊 (Equipment Workshop)"]
    HubService --> FacLab["賽博晶片實驗室 (Cyber Chip Lab)"]
    HubService --> FacDojo["道場與技能傳承 (Dojo & Skill Trees)"]
    HubService --> FacClinic["生化醫療站 (Biochemical Clinic)"]
    HubService --> FacPortal["地牢傳送門 (Dungeon Extraction Gate)"]
    
    HubService --> NpcDialogue["NPC 劇情對話樹 (Dialogue Tree Service)"]
    
    HubService -->|產出營地投影| StateDto["TownHubDto (納入 DrpgStateDto)"]
```

### 核心職責劃分
1. **設施狀態與開放條件**：依據玩家主線劇情進度與營地聲望，動態解鎖設施功能（如初章解鎖黑市鐵匠，擊敗首領解鎖神經晶片插槽改造）。
2. **小隊全面休整與補給**：提供一鍵恢復全隊氣血（HP）與真元（MP），清空地牢負面狀態，重置消耗品。
3. **NPC 劇情與委託交互**：管理營地 NPC（如礦奴長老、叛逃巨企工程師、黑煞宗監工）的樹狀對話、主支線任務交付與好感度/情報交易。
4. **地牢傳送中繼 (Dungeon Gate)**：負責地牢樓層選擇、難度詞綴配置、小隊出發確認，並平滑將遊戲狀態切換至 `M2_DRPG_DUNGEON`。

---

## 🏬 二、六大核心營地設施規格 (Facilities Specification)

為配合「表層仙俠，漸進揭示真相」原則（Option C），營地設施在介面卡牌與對話中**一律優先呈現東方仙俠風格稱謂**，底層掛接相應的 DRPG 業務邏輯：

### 1. 百煉堂 (The Forge & Scrap Workshop)
- **表層仙俠語意**：宗門神兵古鐵百煉堂，長老爐火純青，傳承神兵開光與法寶注靈之法。
- **實戰業務職責**：
  - **分解與提煉 (Salvage)**：將地牢刷出的廢棄法寶與金屬殘骸分解為煉器玄鐵與靈石。
  - **數值強化 (Refinement)**：消耗礦石提升武器物理傷害與防具護甲韌性。
  - **詞綴重鑄 (Affix Reforge)**：洗鍊稀有裝備的隨機詞綴（如提升削韌效率、增加連鎖暴擊率）。

### 2. 靈台藏經閣 (Cultivation Dojo & Chip Lab)
- **表層仙俠語意**：宗門秘藏道統閣，供奉前世宿慧玉簡與通靈靈骨，由引路道者協助修士調試周天。
- **實戰業務職責**：
  - **前世道骨調試 (特工晶片超頻 Agent Core Overclock)**：消耗外神髓液與算力點數，提升主角特工晶片的核心超頻能力。
  - **神經插槽鑲嵌 (Socketing)**：為隊員解鎖神經插槽，安裝特殊道骨晶片（如「子彈時間反制插槽」、「元素共振超導晶片」）。
  - **天賦洗點與技能研習 (Skill Mastery)**：自由調整四大流派技能點數，零成本洗點以鼓勵自由構建 Build。

### 3. 雜役演武道場 (Combat Tactics & Gambit)
- **表層仙俠語意**：外門雜役校場與同門宿營，提供弟子陣法磨合、戰陣演練與夥伴招募。
- **實戰業務職責**：
  - **夥伴招募與編制 (Party Roster)**：依據四大道統陣營門派招募鐵牛（天工神機/妖門）、凌霜（正統道門）、燕青（巨企仙朝）、墨衍（天道星靈），調配 5 人小隊名冊（詳見 [`M4_PARTY_AND_FORMATION.md`](M4_PARTY_AND_FORMATION.md)）。
  - **連招戰術方針配置 (Gambit Auto-Tactics)**：設定隊員在半即時戰鬥中的 AI 觸發條件（如「敵方處於破防狀態時優先釋放重力裂變」）。
  - **陣法調度 (Formation Engine)**：配置五行八卦陣法孔位與站位加成。

### 4. 宗門外門集市 (Outer Sect Market & Smuggling Bazaar)
- **表層仙俠語意**：黑煞宗外門雜役互市與私下黑市地攤，各路採掘散修銷贓換取靈石與丹藥之所。
- **實戰業務職責**：
  - **戰利品收購與銷贓 (Shop Sell)**：收購地牢中採集的黑煞石、妖獸幾丁質材料與未綁定裝備。
  - **生存基礎物資補給 (Shop Buy)**：購買火把、金創藥、辟邪符籙與乾糧，支援地牢探險消耗品配額。

### 5. 回春客棧／通鋪柴房 (Clinic & Rest)
- **表層仙俠語意**：礦脈下方的避煞客棧與雜役通鋪，燃點辟邪香，供修士調息吐納。
- **實戰業務職責**：
  - **一鍵打坐休整 (Rest & Full Recovery)**：全隊生命氣血 (HP)、真元法力 (MP) 與精神防護罩 (SAN) 瞬間回滿，解除【精神萎靡】狀態。
  - **行囊補給配置 (Supplies Packing)**：出征前配置小隊火把數量、金創藥與辟邪符。

### 6. 廢棄礦脈坑道 (Dungeon Extraction Portal)
- **表層仙俠語意**：墨竹山封閉禁區與黑煞宗地底礦道入口，石階延伸向未知地底深淵。
- **實戰業務職責**：
  - **區域選擇**：選擇進入目標地牢（如「黑煞宗地底礦脈・深坑一階」）。
  - **出征安全校驗**：檢查小隊是否編制完整、行囊是否有必要照明物資，確認後進入第一人稱 3D 走格子視圖。

---

### 7. 靈識認知轉譯代理與漸進世界觀真相揭示機制 (Cognitive Lore Proxy)
- **架構核心**：由後端 [`CognitiveProxyService`](#五代表類別與資料結構) 負責在組裝 `TownHubDto` 時進行動態認知過濾：
  - **加載度 < 30% (愚昧求生·純仙俠)**：所有設施名稱與描述 100% 為東方修仙古風（百煉堂、靈台藏經閣、雜役演武道場、宗門外門集市、回春客棧、廢棄礦脈坑道），NPC 對話無任何賽博或外神名詞。
  - **加載度 30% ~ 70% (知見初裂·異象微顯)**：點擊設施時，文字介面邊緣偶現微幅藍色螢幕光斑（Glitch FX），懸停描述中解鎖「活性幾丁質」、「靈流等離子殘痕」等過渡註記。
  - **加載度 > 70% (真理洞察·科技覺醒)**：主角特工晶片算力覺醒，設施選單中解鎖「特工戰術分析視角」，揭露百煉堂實為工程師生化兵器組裝台、藏經閣實為千星科技神經元算法實驗室之真容。

---

## 💬 三、NPC 劇情對話與任務分發機制 (Dialogue & Quests)

NPC 交互徹底廢除 MUD 文字指令（如 `look npc`、`ask elder about sword`），全部由結構化 JSON 驅動，且其對話文字同樣受 `CognitiveProxyService` 轉譯保護：

### 對話樹範本結構 (`dialogues.json`)
```json
{
  "dialogueId": "npc_elder_ore_mine_intro",
  "npcId": "elder_kuang_nu",
  "npcName": "採礦奴隸長老・鐵手",
  "avatar": "assets/npcs/elder_ironhand.webp",
  "introText": "咳咳...小子，看你的眼神，你的前世宿慧道骨覺醒了？別出聲，宗門那幫執事就在礦井上方監視著...",
  "lenses": [
    {
      "minProgress": 0,
      "text": "咳咳...小子，看你的眼神，你的前世宿慧道骨覺醒了？別出聲，宗門那幫執事就在礦井上方監視著..."
    },
    {
      "minProgress": 70,
      "text": "咳咳...小子，看你的瞳孔反射，你的特工神經晶片覺醒了？別出聲，千星科技與宗門執事就在礦井上方監視著...",
      "glitch": true
    }
  ],
  "options": [
    {
      "optionId": "opt_ask_chip",
      "text": "【詢問】老人家，你認得我眉心這枚道骨？",
      "condition": "quest.vs01_awakening == ACTIVE",
      "nextDialogueId": "npc_elder_chip_secret"
    },
    {
      "optionId": "opt_trade_ore",
      "text": "【交易】用剛挖出的血晶原石換取補給",
      "action": "OPEN_FACILITY_SHOP"
    },
    {
      "optionId": "opt_leave",
      "text": "【離開】保持警戒，轉身離去",
      "action": "CLOSE_DIALOGUE"
    }
  ]
}
```

---

## 🔄 四、狀態契約與全景投影 (State Contract)

營地狀態以純單機 DTO 的形式聚合於 `DrpgStateDto`：

```java
public record TownHubDto(
    String hubId,                  // 如 "hub_ore_mine_shelter"
    String hubName,                // 如 "黑煞宗廢棄下層礦脈避難所"
    String backgroundScene,        // 背景場景圖資源路徑
    String atmosphericAudio,       // 營地背景氛圍音效
    List<FacilityInfoDto> facilities, // 可用設施清單（名稱、圖標、解鎖狀態）
    List<NpcSummaryDto> activeNpcs,   // 駐留 NPC 名單
    CurrentDialogueDto currentDialogue // 當前進行中的對話（若有，否則 null）
) {}
```

---

## 🔗 五、代表類別與資料結構

| 項目 | 路徑 / 名稱 | 職責 |
| :--- | :--- | :--- |
| **營地服務** | `com.example.htmlmud.town.TownHubService` | 設施路由、休整結算與傳送進入地牢 |
| **靈識轉譯代理** | `com.example.htmlmud.town.CognitiveProxyService` | 依據晶片加載度動態過濾設施、物品與技能的認知透鏡呈現 |
| **狀態投影** | `com.example.htmlmud.town.TownHubDto` | 傳遞至前端渲染之唯讀營地狀態（經 Proxy 轉譯） |
| **設施模型** | `com.example.htmlmud.town.FacilityDto` | 單一設施狀態投影資料傳輸模型 |
| **認知位階** | `com.example.htmlmud.town.PerceptionTier` | 三階靈識認知位階枚舉 |
| **設施資料** | `src/main/resources/data/zones/newbie_village/facilities.json` | 營地機能設施參數與解鎖條件（含三階透鏡名詞） |

---

## 🎯 六、純單機 Hack-and-slash DRPG 規範守衛

1. **嚴禁引入房間拓撲 (No Room Exits)**：營地內部禁止實作方塊房間移動與 exits 關係，一律以選單與設施按鈕呈現。
2. **極速進入砍殺 (Frictionless Combat Transition)**：從營地整備到進入地牢只需一次點擊，無任何繁瑣命令行，保證玩家隨時投入地牢砍殺刷寶循環。
3. **安全庇護所原則 (Absolute Safe Zone)**：營地內不觸發任何即時戰鬥或野怪入侵，保證玩家在整備時擁有絕對平靜的心流體驗。
