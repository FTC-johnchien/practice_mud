# 👥 M4: 冒險小隊與古仙陣法模組實作進度清單 (Party & Formation Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/backend/M4_PARTY_AND_FORMATION.md`](../../2_specifications/backend/M4_PARTY_AND_FORMATION.md)  
> **核心職責**：管理 5 人冒險小隊編制、道基五維（體質/真元/神識/根骨/身法）、40 格共用儲物袋、外神精神侵蝕防護罩 (SAN 護盾) 與同伴 Gambit 自動戰術引擎。  
> **當前完成度**：95% 🟢

---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心領域模型**：
  - [`Party.java`](../../../src/main/java/com/example/htmlmud/party/Party.java)（5 人小隊實體，管理成員與共享資源）
  - [`PartyMember.java`](../../../src/main/java/com/example/htmlmud/party/PartyMember.java)（隊員實體、道基五維、SAN 狀態機、裝備與技能槽）
  - [`PartyInventory.java`](../../../src/main/java/com/example/htmlmud/party/PartyInventory.java)（唯一真相源：40 格小隊公共背包）
  - [`FormationEngine.java`](../../../src/main/java/com/example/htmlmud/party/FormationEngine.java)（陣法孔位與動態替補計算）
- **關聯驗證測試**：
  - [`CompanionGambitEngineTest.java`](../../../src/test/java/com/example/htmlmud/CompanionGambitEngineTest.java)（4/4 綠燈）
  - [`PartyInventoryAndMadnessTest.java`](../../../src/test/java/com/example/htmlmud/PartyInventoryAndMadnessTest.java)（小隊行囊與精神萎靡 100% 綠燈）
  - [`UnifiedPartyEconomyTest.java`](../../../src/test/java/com/example/htmlmud/UnifiedPartyEconomyTest.java)（小隊金庫與經濟 100% 綠燈）
  - [`DualTrackPotentialPointsTest.java`](../../../src/test/java/com/example/htmlmud/DualTrackPotentialPointsTest.java)（小隊成員指令加點與潛能點分配 100% 綠燈）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **M4-PKG-ENCAPSULATION (Package-by-Feature 獨立封裝 / Phase 3 Step R3.5)**
  - 建立一級套件 `com.example.htmlmud.party`，將 `PartyMember`、`Party`、`PartyInventory`、`PartyItemSlot`、`FormationEngine`、`PartyService` 等 21 份模型與服務類別徹底自 `domain.party` 移出歸整（完成時歷史基準 395 項測試綠燈，實時狀態以 `.\test.ps1` 為準）。
- [x] **M4-UNIFIED-INVENTORY (小隊 40 格共用儲物袋)**
  - 徹底廢止個人 12 格背包與雙向轉存（`stash` 刪除），以 40 格小隊行囊作為全系統唯一真相源。
- [x] **M4-FORMATION-AUTO (陣法孔位陣亡動態重映射)**
  - 實作 `FormationEngine`：戰鬥中成員陣亡孔位自動釋放，存活成員依序遞補孔位增益，殘局陣法永不崩潰。
- [x] **M4-SAN-SHIELD (外神精神侵蝕防護罩)**
  - 初始 100 點 SAN，身處幽冥或受外神低語侵蝕抵扣；SAN=0 進入【精神萎靡】Debuff，絕無隨機魔化叛變。
- [x] **M4-GAMBIT-ENGINE (隊友自動戰術 AI 鏈)**
  - 實作 `GambitRule`、戰術條件求值器與預設範本庫（【斷法攻堅】、【急救續航】、【守禦護坦】、【破防爆發】），同伴依策略自動打斷與急救。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- [ ] **Task M4-1 (VS-01-AGENT-HUD / ACTIVE-TASK-2): 特工晶片加載 (12%...) 與道基五維初始潛能點加點**
  - **現況與目標**：後端 `PartyCommand`（`stat add`）與 `XpProgressionService.allocateStatPoint` 雖已實作基礎加點邏輯（見 `DualTrackPotentialPointsTest`），惟隊長甦醒初始 5 點自由潛能點配發、DTO 投影與前端小隊面板端到端加點同步仍待完整驗收。
  - **驗收標準**：初始點數與加點後五維衍生屬性（HP/MP/攻防速）立即同步至前端面板，端到端驗收與單元測試綠燈。
- [ ] **Task M4-2 (COMPANION-RECRUITMENT): 營地道場四大道統夥伴招募與流派編隊**
  - **目標**：在營地客棧／道場依據四大歷史道統門派（正統道門、天工神機/古修妖門、巨企仙朝、深淵畸變密教）消耗特定信物或靈石招募後備隊員（對齊 [`M4_PARTY_AND_FORMATION.md`](../../2_specifications/backend/M4_PARTY_AND_FORMATION.md)），確保各流派夥伴具備仙俠表象與真相背後之 17 專精樹技能配置。
  - **驗收標準**：招募成功正確加入小隊 5 人名單，綁定對應流派技能與陣法孔位，單元測試綠燈。
