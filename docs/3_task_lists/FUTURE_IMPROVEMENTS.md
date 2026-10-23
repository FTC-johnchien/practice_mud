# 專案後續架構改善與演化藍圖 (Consolidated Future Improvements Roadmap)

> **文件定位與目的**：
> 本文件作為專案後續宏觀演進路徑與全局優先序矩陣之索引總綱。
> 為了防範單一超長文件導致的閱讀分心與上下文稀釋，各領域具體執行規格已全面落實 **與 Level 2 模組 1:1 鏡像對齊**：
> - 📊 **全模組 1:1 實作進度儀表板**：請參閱 👉 [**`README.md`**](README.md)（進度中樞與完成度總表）
> - 🏛️ **後端 10 大模組實作清單**：請參閱 👉 [**`backend/`**](backend/)（`M1` ~ `M9`、`M3.1`）
> - 🎨 **前端 6 大表現模組實作清單**：請參閱 👉 [**`frontend/`**](frontend/)（`FE1` ~ `FE6`）
> - 📜 **資料架構契約實作清單**：請參閱 👉 [**`data_schemas/`**](data_schemas/)（`DUNGEON`、`ITEM`、`SKILL`）
> - 🗄️ **歷史已完成審查與治理歸檔庫**：請參閱 👉 [**`alignment_history/`**](../0_archive/alignment_history/)
> - 📜 **歷史版本發布與詳細變更紀錄**：請參閱 👉 [**`CHANGELOG.md`**](../../CHANGELOG.md)

---

## ⚡ 專案典範轉型公告：純單機 Hack-and-slash DRPG 確立與歷史任務除役說明 (2026-10-22)

> [!IMPORTANT]
> **重大設計裁決與典範轉移宣告**：
> 經專案核心架構裁決，本專案已**全面廢除 MUD 雙軌與文字命令行機制**，轉型為**「現代半即時刷寶純單機 DRPG (Action-Tactical DRPG)」**。  
> 專案全力聚焦於**五大核心支柱 (The Five Pillars)**：
> 1. 🌌 **世界觀的介紹 (World & Lore)**：克蘇魯外神卵鞘 × 賽博巨企掠奪 × 前世特工晶片 × 異形強殖基因。
> 2. ⚡ **戰鬥爽快 (Impactful Combat)**：半即時快節奏砍殺、破防削韌（Poise Break）、反制打斷、元素連鎖、連攜合擊與 FCT 大字浮動傷害。
> 3. 🧬 **技能組合 (Skill Synergy)**：四大流派技能構建、元素反應爆發、Gambit 隊友自動戰術方針。
> 4. 💎 **裝備收集 (Loot Progression)**：高頻掉落、隨機詞綴、多階稀有度與數值爽感。
> 5. 📈 **角色養成 (Character Progression)**：5 人小隊自由構築、特工晶片超頻插槽、基因補完、天賦樹加點。
>
> **歷史任務除役與封存標記 (Deprecations)**：
> - ❌ **MUD 房間拓撲與 exits 位移除役**：舊規劃中涉及 `rooms.json`、`north/south` 房間移動、房間生靈巡邏與文字命令行者全數除役，由 [`backend/M1_TOWN_HUB_AND_FACILITIES.md`](../2_specifications/backend/M1_TOWN_HUB_AND_FACILITIES.md)（選單式營地）與 [`backend/M2_DRPG_DUNGEON.md`](../2_specifications/backend/M2_DRPG_DUNGEON.md)（第一人稱網格地牢）全面取代。
> - ❌ **道心崩潰魔化機制除役**：舊規劃（如 F2-04）中關於隊員理智崩潰、狂亂叛變的負面懲罰機制全數取消，簡化系統以捍衛爽快刷寶體驗。
> - 🎯 **後續開發唯一權威路徑**：請以 [`ACTIVE_TASKS.md`](../4_active_tasks/ACTIVE_TASKS.md) 與 [`SPRINT_GOAL.md`](../4_active_tasks/SPRINT_GOAL.md) 中定義之 **Milestone VS-01 序章垂直切片** 為單一推進焦點。

---

## 🧭 後續推薦實施路徑 (Next Evolution Paths)

> 💡 **歷史已完成與封存戰線**：
> - 歷史大量基礎設施（路徑 0、A、B、C、D、E、F、G、H、L、N、U、X）已全數落地並通過 376+ 測試，詳情請查閱 [`future_improvements_history.md`](../0_archive/future_improvements_history.md)。
> - 歷史 MUD 雙軌割裂分析（原路徑 Q、R）與早期審查補丁表已正式除役並移入 [`alignment_history/`](../0_archive/alignment_history/) 封存。

### 🌟 領域一：純單機 Hack-and-slash 戰鬥與內容推進 (詳見 [`backend/M3_COMBAT_ENGINE_TASKS.md`](backend/M3_COMBAT_ENGINE_TASKS.md) 與 [`data_schemas/`](data_schemas/))
- [ ] **路徑 M: 敵方行動預告、反制窗口與首領階段** (§13) — **Priority: P1 / P2**
- [ ] **路徑 O: 生動畫面呈現與淡化文字閱讀轉型藍圖 (去文字化主舞台)** (§15) — **Priority: P1 / P2**
- [ ] **路徑 Z: 五大核心目標中長程內容拓展藍圖 (二章太陰前進基地、三章外神核心神殿、32+賽博天賦樹、隨機詞綴庫)** (§26) — **Priority: P1 / P2 (內容核心)**

### 🛡️ 領域二：底層品質守護、硬編碼清剿與測試加速 (詳見 [`backend/M8_SAVE_AND_PERSISTENCE_TASKS.md`](backend/M8_SAVE_AND_PERSISTENCE_TASKS.md) 與 [`frontend/FE1_CORE_STATE_TASKS.md`](frontend/FE1_CORE_STATE_TASKS.md))
- [ ] **路徑 P: 全專案功能與設定資料硬編碼實地盤查與清理藍圖** (§16) — **Priority: P1 / P2**
- [ ] **路徑 S: Buff 契約與可靠性護欄第二輪驗收** (§19) — **Priority: P1 / P2**
- [ ] **路徑 Y: 單機語意純化與全域硬編碼大清剿 (SP-PURITY & DATA-CORE)** (§25) — **Priority: P1**
- [ ] **路徑 K: 測試工程分層加速與系統可觀測性** (§11) — **Priority: P3**

---

## 📋 改善項目實施優先序總表 (Execution Priority Matrix)

| 優先序 | 類別代碼 | 改善任務簡述 | 核心影響領域 | 對應章節 | 狀態 |
| :---: | :---: | :--- | :--- | :---: | :---: |
| **P0** | ROOM-01 | 房間狀態與地面掉落物持久化閉環 (`RoomService.record`) | 世界狀態防遺失 | §0.1 | ✅ 已完成 |
| **P0** | COMB-00 | 圓桌戰鬥防禦切片累積總機率收斂 (消除 Crit/Hit 湮滅) | 核心戰鬥數學 | §0.2 | ✅ 已完成 |
| **P0** | ASYNC-01 | Write-Behind 佇列有界背壓與優雅關閉無競態 | 系統併發安全 | §0.3 | ✅ 已完成 |
| **P0** | SEC-01 | 前端未轉義動態插值修補與 53 個 inline 事件委派改造 | 前端安全性與 CSP | §0.4 | ✅ 已完成 |
| **P1** | CANON-01 | Phase 2: Canonical Item (`Definition`+`Instance`+`View`) | 物品單一真相源 | §1.1 | ✅ 已完成 |
| **P1** | CANON-02 | Phase 3: Single `SkillDefinition` (MUD/DRPG Facets) | 技能定義合併 | §1.2 | ✅ 已完成 |
| **P1** | CANON-03 | Phase 4: 徹底移除 `TemplateRepository` 靜態呼叫 (DI) | 測試與架構隔離 | §1.3 | ✅ 已完成 |
| **P1** | CANON-04 | Phase 5: 基於 Snapshot / Outcome 的角色戰鬥同步 | 併發與資料一致性 | §1.4 | ✅ 已完成 |
| **P1** | COMB-01 | 圓桌單次擲骰收斂、Stamina 精力破防、主角潛能點加點 | 戰鬥深度與成長 | §2.3~2.6 | ✅ 已完成 |
| **P1** | MUD-01 | MUD 怪物 AI、擊殺升級閉環、Buff 引擎與規則補齊 | 世界規則與 AI | §3.1~3.4 | ✅ 已完成 |
| **P1** | UI-01 | 全域佈局重構、三種資訊密度 (字級 $\ge 13\text{px}$) 與雙軌契約 | 介面佈局與資源呈現 | §4.1~4.3 | ✅ 已完成 |
| **P1** | ARCH-01 | Domain 層反向依賴反轉 (Phase 10 Clean Architecture) | 六角形純淨架構 | §5.1 | ✅ 已完成 |
| **P1** | ACT-01 | 收斂 Room Actor 狀態修改通道 (修復 `removePlayer` 雙軌修改) | Actor 狀態單一所有 | §7.1 | ✅ 已完成 |
| **P1** | ACT-02 | 戰鬥虛擬執行緒 Executor 治理與時限/閒置熔斷斷路器政策決議 | 執行緒防洩漏與戰鬥守護 | §2.7, §7.2 | ✅ 已完成 (與 COMB-03 合併落地) |
| **P1** | SAVE-01 | 檔案式存檔引入 `schemaVersion` 與版本遷移適配器 | 存檔平滑相容升級 | §8.1 | ✅ 已完成 |
| **P1** | SAVE-02 | 壞檔防禦標記與防覆寫保護 (區分 `empty` 與 `corrupted`) | 進度防毀損保護 | §8.2 | ✅ 已完成 |
| **P1** | DATA-01 | 消除職業反擊特例、陣型比對與技能別名硬編碼 | 100% 資料驅動 | §9.1 | ✅ 已完成（殘留見 DATA-06） |
| **P1** | VAL-01 | 集中式遊戲 JSON 啟動期拓撲校驗 (`DataIntegrityValidator`) | 啟動期 Fail-Fast | §9.2 | ✅ 已完成 |
| **P1** | ASYNC-02 | Write-Behind worker 停機排空協定 (drain protocol) 與拒收通知 | 資料持久化邊界安全 | §6.3 | ✅ 已完成 |
| **P1** | NET-01 | WebSocket 本機綁定模式與公開模式票證驗證 | 網路安全邊界 | §6.1, §6.2 | ✅ 已完成 |
| **P1** | NET-02 | WebSocket 64KB 訊息長度上限與每秒 20 次指令限流 | 資源保護與抗洪水 | §6.2 | ✅ 已完成 |
| **P1** | COMBAT-FX-EVENT-01 | 結構化戰鬥事件模型與 WebSocket `BATTLE_EVENTS` 傳輸 | 戰鬥演出資料管道 | §14.1 | ✅ 已完成 |
| **P1** | COMBAT-FX-MVP-02 | 戰場 FX Layer、受擊/防禦判定動畫與浮動數字 (正交色彩) | 核心戰鬥打擊回饋 | §14.2 | ✅ 已完成 |
| **P1** | COMBAT-FX-PRESETS-03 | 武器類型粗粒度分類、法術粒子骨架與 AOE 掃擊預設庫 | 戰鬥多樣性表現 | §14.3 | ✅ 已完成 |
| **P1** | ENEMY-INTENT-01 | 敵方行動意圖預告 (Intent Icon) 與卡片蓄力唱條 (Cast Bar) | 戰鬥視覺化反制 | §13.1, §15.1 | ✅ 已完成 |
| **P1** | LOG-DIM-01 | 純文字日誌淡化、邊界縮限、摺疊跑馬燈與分類過濾 | 淡化文字閱讀負擔 | §15.5 | ✅ 已完成 |
| **P1** | DATA-03 | 地牢出入口 (`exitRoomId`)、怪池與掉落物 Fallback 硬編碼清理 | 地牢 100% 配置化 | §16.1, §21.3 | ⚠️ 待落實 (代碼仍殘留 if-else 且缺少 exitRoomId，移入 §21.3) |
| **P1** | DATA-04 | 技能橋接規則下沉至 JSON 與屬性補正前綴猜測清理 | 技能單一真相源 | §16.2 | ✅ 已完成 |
| **P1** | COMB-03 | 戰鬥 5 分鐘固定超時與 10 分鐘 AFK 閒置熔斷解耦治理 | 戰鬥守護防誤殺 | §16.2 | ✅ 已完成 |
| **P1** | UNIFY-01 | 背包雙層收斂：個人隨身行囊(12格)+小隊公共倉庫(30格)，stash/withdraw流轉，存檔v2遷移，消除假帳 | 行囊資料一致性 | §17.2, §18.6 | ✅ 已完成 |
| **P1** | UNIFY-02 | 角色與裝備大一統：7 槽位裝備單一狀態持有與實時同步 | 裝備數值一致性 | §17.2, §18.7 | ✅ 已完成 |
| **P1** | UNIFY-03 | 戰鬥引擎收斂與雙模呈現投影（依 §18.2 改為 Strangler 漸進式） | 消除重複戰鬥心跳 | §17.2, §18.8 | ✅ 已完成 |
| **P0** | GUARD-01 | 校驗器擴充（地塊 drops/eventId、橋接目標、商店、開局物資）+ 棘輪防回歸掃描 | 防回歸護欄 | §18.3, §18.4 | ✅ 已完成 |
| **P1** | FALLBACK-01 | 移除生產碼 `new TemplateCatalog()`、無參建構子、`GameConfig.getInstance()` 與 optional 核心注入 | 消除靜默降級 | §18.3 | ✅ 已完成 |
| **P1** | SYNC-01 | Outcome 以真實 battleId 冪等並經 Player 信箱套用，刪除 `copyStats` 跨執行緒覆寫 | Actor 邊界正確性 | §18.2 | ✅ 已完成 |
| **P1** | EVENT-01 | 領域事件骨幹：雙軌發布 MobDead / LevelUp / ItemAcquired / RoomEntered，listener 回 Actor 信箱 | 任務引擎前置 | §18.3 | ✅ 已完成 |
| **P1** | DATA-06 | §16 遺漏硬編碼：首領固定 dex/xp/掉落、`contains("mozhu")`、道核三種 ID、TOMB_ITEMS 三份複本、SWORDSMAN 殘留 | 資料驅動純度 | §18.1 | ✅ 已完成 |
| **P1** | ADR-01 | 城鎮戰鬥模式 / 背包所有權 / 持久化真相源 / 純文字客戶端支援範圍決策 | 統一工程前置 | §18.5 | ✅ 已定案 (2026-10-11) |
| **P1** | COMBAT-VFX-STATUS-02 | 護盾血條疊層 (Shield Bar)、DoT 脈衝與 Buff 倒數光圈 | 戰況狀態動態視覺 | §15.2 | ✅ 已完成 (2026-10-13) |
| **P2** | DUNGEON-VIEW-01 | 地城第一人稱擬 3D 廊道視圖與點擊互動熱區 | 探索空間視覺化 | §15.3 | 🔲 待執行 |
| **P2** | TOWN-UI-01 | 城鎮 NPC 生動對話氣泡、立繪與裝備數值比對浮條 | 劇情與養成視覺化 | §15.4 | 🔲 待執行 |
| **P2** | BATTLE-RESULT-01 | 戰鬥結算翻牌視窗、經驗條充能與修仙境界突破全屏特效 | 養成反饋視覺化 | §15.6 | ✅ 已完成 |
| **P2** | CSS-01 | 以保留原始順序的純搬移方式拆分大型 `style.css`，逐批驗收 | 前端可維護性與視覺回歸風險 | §10.1 | ✅ 已完成 |
| **P2** | FE-01 | 3,141 行 `party-modal.js` 垂直切片模組化拆分 | 前端複雜度解耦 | §10.2 | ✅ 已完成 |
| **P2** | LEG-01 | `static/legacy/drpg-view.js` 正式除役與註解清理 | 代碼庫整潔度 | §10.3 | ✅ 已完成 |
| **P2** | UI-02 | 探索舞台雙欄化與控制回歸羅盤 | 操作流暢度 | §10.4 | ✅ 已完成 |
| **P2** | DATA-02 | 徹底清除 5 大殘存硬編碼 (招式篩選/橋接備用表/夥伴比對/NPC能力/開局物資) | 100% 資料驅動純度 | §9.3 | ✅ 已完成 |
| **P2** | DATA-05 | 走火入魔 CHAOS 異變數值與宋天衡專屬掉落資料化 | 戰鬥機制配置化 | §16.2 | ✅ 已完成 |
| **P2** | TOWN-02 | 客棧標籤化 (`SAFE_ZONE`) 與商店台詞動態化、典當支援 | 城鎮設施通用化 | §16.3 | 🔲 待執行 |
| **P2** | CONF-01 | 開局物資、調息數值與重生房間納入 `GameConfig` 配置 | 全域配置集中化 | §16.3 | ✅ 已完成 |
| **P2** | FE-02 | 地塊 Tooltip 動態化與前端裝備槽位判定純淨化 | 前端健壯性 | §16.4 | 🔲 待執行 |
| **P2** | UNIFY-04 | 生靈與敵怪生命週期閉狂 (`Mob` ↔ `BattleEnemy`) | 世界生態閉環 | §17.2 | 🔲 待執行 |
| **P2** | UNIFY-05 | 技能 Single Source of Truth (Canonical 貫穿全流程) | 技能架構收斂 | §17.2 | 🔲 待執行 |
| **P2** | COMBAT-REACT-01 | 玩家反制、Gambit 意圖協作與首領階段 | 戰術深度與遭遇變化 | §13.2 | 🔲 待執行 |
| **P2** | COMBAT-DATA-DOC-01 | SAN 異變資料化、文件現況與測試摘要同步 | 資料驅動與文件治理 | §13.3 | 🔲 待執行 |
| **P2** | COMBAT-FX-RULE-04 | 我方暴擊/未命中規則與一排目標範圍規則獨立化 | 戰鬥平衡與機制定義 | §14.4 | 🔲 待決議 |
| **P1** | ASYNC-03 | Write-Behind 提交/停機原子協定、滿載明確回報與競態測試 | 持久化資料可靠性 | §19.1 | ✅ 已完成 (2026-10-13) |
| **P1** | NET-01-VERIFY | 重新驗證本機/公開 WebSocket 身分驗證與部署邊界 | 外部連線信任邊界 | §19.2 | 🔲 待驗證 |
| **P1** | BUFF-01 | Buff 效果生命週期、疊層、控場與共用規則結算契約 | 戰鬥規則一致性 | §19.3 | ✅ 已完成 (2026-10-13) |
| **P2** | FALLBACK-02 | 生產 fallback 清零與架構護欄精確化 | 配置失敗可見性 | §19.4 | 🔲 待執行 |
| **P2** | WEB-STATE-01 | 前端瀏覽器儲存 API 故障降級與初始化測試 | 前端啟動健壯性 | §19.5 | 🔲 待執行 |
| **P3** | DOC-STATE-01 | 現況／目標／歷史分流與測試摘要單一同步來源 | 文件治理 | §19.6 | 🔲 待執行 |
| **P3** | TEST-01 | 核心數值與領域規則下沉為純單元測試 (加速 CI) | 測試工程效能 | §11.1 | 🔲 待執行 |
| **P3** | OBS-01 | 系統度量指標擴充與敏感資料日誌審核 | 系統觀測性加固 | §11.2 | 🔲 待執行 |
| **P3** | CODE-01 | 清理 `PlayerLoginListener` 註解死代碼與空 stub | 代碼庫整潔度 | §16.4 | 🔲 待執行 |
| **P2** | DEBUG-01 | 試道傀儡等除錯指令以 Profile / 管理員旗標限制 | 防刷資源 | §18.3 | ✅ 已完成 |
| **P3** | CONTENT-01 | 垂直可玩內容閉環 (創角 -> 滄浪城任務 -> 地宮探險 -> 戰鬥 -> 結算存檔) | 遊戲體驗交付 | §12.1 | ✅ 已完成 |
| **P1** | DEEPCOPY-01 | LivingStats.deepCopy() 補齊抗性與技能裝備防禦性拷貝 | 資料完整性與防遺漏 | §20.1 | ✅ 已完成 (2026-10-14) |
| **P3** | PERSIST-01 | H2 CharacterEntity.inventory 標記棄用與單機存檔收斂 | 持久化架構整潔度 | §20.3 | 🔲 待執行 |
| **P1** | DATA-DRIVEN-COMBAT-01 | 全域九大天道傷害類型、Poise 罡氣破防條與五行抗性資料驅動 | 戰鬥機制配置化 | §20.4 | ✅ 已完成 (2026-10-14) |
| **P0** | GUARD-02 | 核心戰鬥迴圈禁止中文特例比對看門狗測試 (ArchitectureGuardrailsTest) | 專案第一紅線防衛 | §20.5 | ✅ 已完成 (2026-10-14) |
| **P0** | EQUIP-SLOT-CLEAN-01 | 清除裝備槽位中文名稱模糊硬編碼 (FE-02 第一紅線整治) | 前端健壯性與紅線守護 | §21.2 | ✅ 已完成 (2026-10-15) |
| **P0** | DOC-CONSISTENCY-01 | 全專案文件現況與目錄結構同步 (UI_PAGE_STRUCTURE §4 重寫 + ARCHITECTURE §3 補齊) | 文件治理 | §21.1 | ✅ 已完成 (2026-10-15) |
| **P1** | DATA-03-REVISIT | 落實地牢出入口 exitRoomId 與 Tooltip 配置化 (DungeonFloor + legend) | 地牢 100% 配置化 | §21.3 | ✅ 已完成 (2026-10-15) |
| **P1** | SHOP-FALLBACK-01 | 移除 ShopCommand 客棧名稱比對與偽商店 Fallback | 商店資料驅動純度 | §21.4 | ✅ 已完成 (2026-10-15) |
| **P2** | DEADCODE-01 | 清理 PlayerLoginListener 註解死代碼、QuestEventListener 語法與 legacy 巨石備份 | 代碼庫整潔度 | §21.5 | ✅ 已完成 (2026-10-15) |
| **P3** | QUEST-DATA-01 | 任務劇情引擎資料化 (MozhuMinesQuestListener 泛化為 QuestTemplate) | 任務架構演進 | §21.7 | 🔲 待執行 |
| **P0** | DOC-FACT-01 | 消除已除役服務殘留與代碼現況同步 (SkillBridgeService/ScopedValue/v3/324測試) | 文件與代碼真實性 | §23.3 | ✅ 已完成 (2026-10-20) |
| **P1** | DOC-ENUM-01 | 全域遊戲詞彙表分層與裝備枚舉對齊 (EquipmentSlot 5+2 / DamageType vs 靈氣) | 語意與契約一致性 | §23.4 | ✅ 已完成 (2026-10-20) |
| **P1** | DOC-CAP-01 | 小隊公共行囊容量全域標準裁定 (拍板採方案 A：40 格全域同步) | 數值平衡與契約 | §23.5 | ✅ 已完成 (2026-10-20) |
| **P1** | DOC-WS-01 | 前端 WebSocket 模組化獨立成檔、指數退避重連與單機連線合理化 | 前端通訊健壯性 | §23.6 | ✅ 已完成 (2026-10-20) |
| **P2** | DOC-ROADMAP-01 | 收斂 16 本模組手冊重複路線圖至總待辦 | 文件體系維護性 | §23.7 | 🔲 待執行 |
| **P0** | DOC-REJECT-01 | 三方審查不合理／過度設計項目明確否決宣告 (外部腳本/跨服/萬能解析器/15s Ping) | 架構紅線守衛 | §23.10 | ✅ 已否決除役 |
