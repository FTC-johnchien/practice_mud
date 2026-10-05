# Copilot 專案檢視與動態戰鬥演進建議

> 檢視日期：2026-10-05  
> 範圍：依據 [`ARCHITECTURE.md`](../../ARCHITECTURE.md)、[`UI_PAGE_STRUCTURE.md`](../UI_PAGE_STRUCTURE.md)、[`FUTURE_IMPROVEMENTS.md`](./FUTURE_IMPROVEMENTS.md)，交叉核對目前 Java / JSON / Web 前端目錄，以及戰鬥核心、DTO、戰鬥面板與相關測試。這是架構與遊戲機制檢視，不是逐行安全稽核；本次未修改遊戲程式碼，也未執行測試。

## 一、結論摘要

目前專案已具備不錯的戰鬥底座：MUD 與 DRPG 共用技能定義、GCD／吟唱、逐怪威脅表、陣法站位、Buff／Debuff、精力與破防，以及同伴 Gambit。`DrpgBattleService` 也已拆分為戰鬥循環、敵方戰術與獎勵服務，整體方向符合模組化單體及機制／資料分離原則。

現階段「動態」主要表現在自動攻擊、狀態持續更新、隨機招式文字與仇恨轉火；玩家在敵方出招前可讀取並回應的戰術訊號仍不足。最值得先做的不是新增更多技能，而是令敵方出招形成「可讀訊號 → 玩家反制 → 結果／窗口」的閉環，並讓敵方技能真正改變戰鬥規則。

## 二、優先改善發現

| 優先度 | 發現 | 證據與影響 | 建議 |
|---|---|---|---|
| **P1** | 戰鬥仍有固定 5 分鐘絕對逾時 | [`BattleContext.java`](../../src/main/java/com/example/htmlmud/domain/dungeon/battle/BattleContext.java#L41) 將 `maxDurationMs` 預設為 300,000ms；`isTimedOut()` 即使玩家持續操作仍會以戰鬥開始時間判斷逾時。循環在 [`DrpgCombatLoop.java`](../../src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java#L129) 無條件檢查此上限。這與待辦中「移除一般戰鬥硬性中斷、以 10 分鐘無操作保護」的設計意圖不一致，長戰／訓練／首領戰可能被提前結束。 | 明確分離「閒置超時」與「伺服器安全上限」：保留 10 分鐘閒置熔斷；若仍需絕對上限，應採可設定且足以涵蓋長戰的安全值，並為訓練與首領戰定義政策。補測「持續有效操作不會觸發閒置逾時」及「不同戰鬥模式的上限」。 |
| **P1** | 敵方技能目前未形成不同的戰鬥效果，且沒有出招預告 | [`DrpgEnemyTacticsService.java`](../../src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgEnemyTacticsService.java#L157) 從種族／技能清單抽取招式名稱；實際敵方傷害仍由同檔 `calculateEnemyDamage()` 依基礎傷害與防禦計算，再由 [`DrpgCombatLoop.java`](../../src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java#L360) 結算。攻擊只在 `nextAttackTime` 到期時發生；[`BattleEnemyViewDto.java`](../../src/main/java/com/example/htmlmud/domain/dungeon/dto/BattleEnemyViewDto.java) 未帶出敵方當前意圖、蓄力時間或預定目標，因此玩家多半只能在受擊後看日誌。 | 把敵方動作提升為真正的 action：具備目標規則、效果、冷卻、蓄力／預告時間、可否打斷等資料；戰鬥引擎以 `IDLE → TELEGRAPH → RESOLVE → RECOVERY` 推進，並經 DTO／戰鬥面板呈現。先支援普通攻擊、單體重擊、前／後排掃擊、蓄力施法、可打斷技能等少量通用動作。 |
| **P2** | SAN 走火入魔及異變首領轉化仍含內容特例與硬編碼數值 | [`DrpgCombatLoop.java`](../../src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgCombatLoop.java#L200) 附近直接實作混亂期進度、40% 誤傷隊友、雙倍傷害、異變後建立特定首領等規則；[`DrpgBattleService.java`](../../src/main/java/com/example/htmlmud/domain/dungeon/battle/DrpgBattleService.java#L805) 與獎勵流程亦以 `aberration-` ID 前綴識別該特殊實體。這使新增類似異變／轉化玩法必須改 Java，與專案資料驅動原則不一致。 | 將通用「狀態階段／觸發條件／行動效果」與轉化目標模板資料化；Java 保留可重用的階段引擎，具體門檻、機率、效果、生成模板及戰利品由資料定義。現有異變可作為第一個遷移案例。 |
| **P2** | 敵方行動與首領階段沒有明確的模板契約 | [`MobTemplate.java`](../../src/main/java/com/example/htmlmud/domain/model/template/MobTemplate.java) 包含基礎屬性、技能與掉落等欄位，但目前沒有可供戰鬥循環執行的招式行動／階段描述；實際多數怪物的技能清單僅用於選招文字，首領階段因此難以在 JSON 中描述及由啟動驗證器檢查。 | 優先沿用既有 `SkillDefinition`／DRPG 技能規則，為敵方技能補上效果與目標語意；若首領階段無法自然表達，再增加精簡的 `phases`／`actions` 模板欄位及啟動期 ID、門檻、引用驗證。暫不引入 Lua 或大型行為樹 DSL。 |
| **P2** | 戰鬥 UI 與文件宣告的模組化／事件規範不完全一致 | [`battle-panel.js`](../../src/main/resources/static/js/panels/battle-panel.js#L133) 透過 `card.onclick` 綁定卡片事件，並在多處以 `innerHTML`、DOM `style` 屬性更新內容；與 [`UI_PAGE_STRUCTURE.md`](../UI_PAGE_STRUCTURE.md) 宣告的事件委派、CSS token／資料投影目標有落差。這不是說 `innerHTML` 一律不安全（部分動態文字已有 escape），而是此面板仍有較高的呈現與狀態耦合。 | 以最小範圍逐步收斂：敵方卡片改用容器事件委派與 `data-action`；純視覺狀態改用 CSS class；動態資料仍須轉義。新增意圖面板時一併採用，不要先擴大舊式渲染模式。 |
| **P2** | 架構文件與目前前端狀態有明顯漂移 | [`index.html`](../../src/main/resources/static/index.html#L12) 已載入 6 個分拆 CSS 檔，但 [`FUTURE_IMPROVEMENTS.md`](./FUTURE_IMPROVEMENTS.md#L270) 的 CSS-01 詳述仍稱只載入單一 `style.css`；該檔同時有 CSS-01 已完成的優先序紀錄，`UI_PAGE_STRUCTURE.md` 的現況表仍將 CSS／`party-modal.js` 巨石拆分列為待辦，而 FUTURE 文件已有相關完成紀錄。 | 整理狀態來源：以實際 HTML 引用、CSS 檔案與目前 JS 模組為準，更新 UI 文件現況表及 CSS-01 完成說明；檢查舊 `style.css` 是否只是保留基準／未使用資產，再決定是否清理，不要僅據文件直接刪檔。 |
| **P3** | 測試總數、測試速度與最新結果記錄互不一致 | [`FUTURE_IMPROVEMENTS.md`](./FUTURE_IMPROVEMENTS.md) 不同章節分別記載 231、238、246、247 項等歷史數字，文件開頭也沿用舊測試總數；實際測試檔／案例數仍應由當次測試輸出確認。 | 在每次完整測試後只更新一個最新測試摘要，將各章節的數字標示為「當時里程碑」或移除易過期摘要；保留既有 P3 測試分層與可觀測性工作。 |
| **P3** | 戰鬥資料載入存在靜默忽略解析錯誤的風險 | [`BattleEnemy.java`](../../src/main/java/com/example/htmlmud/domain/dungeon/battle/BattleEnemy.java#L116) 解析怪物裝備時捕捉 `Exception` 後忽略；非法裝備欄位可能被靜默略過，令傷害／防禦與畫面資訊不符。 | 讓無效裝備資料在啟動驗證階段失敗並指出模板與欄位；執行期若保留容錯，至少記錄具體怪物 ID、裝備槽及原因，不要吞掉例外。 |

## 三、如何讓戰鬥更動態：建議的玩家體驗閉環

### 1. 先讓敵人「出招可讀」

在敵人卡片上呈現「蓄力招式、預定目標／範圍、剩餘時間、可否打斷」；大型範圍招式同步標記受威脅的隊伍列或陣位。預告要早於結算至少數個戰鬥 tick，並依招式危險度提供足夠反應時間。這會讓已有的前後排、仇恨、打斷與陣法從數值系統變成玩家看得見的決策。

### 2. 將預告連到有代價的反制

- **打斷**：成功打斷蓄力招式，消耗技能資源並令敵人短暫失衡。
- **防守／閃避**：將既有格擋、閃避與 Stamina 用於反制指定攻擊，而不是只被動擲骰。
- **調位／護衛**：預告掃擊或鎖定後排時，切換隊伍站位或由前排承接；若來不及處理，依傷害規則承受後果。
- **破綻窗口**：招式被打斷、完美防禦或首領技能落空後，敵方進入短暫可受重創狀態，讓玩家能感受到操作結果。

每種反制都應有清楚狀態與戰報，不應只靠傷害浮動或隱藏機率傳達。

### 3. 用階段與場地改變問題，而非單純提高血量

首領可按生命比例或事件切換階段：改變行動表、目標規則與場地危險區，轉階段時先公告，再給玩家應對時間。由較簡單的單體蓄力、前排橫掃開始，之後才增加延遲爆炸區、召喚物、護盾核心等組合。地形危害應先在現有 DRPG 格網內表達，不另造一套獨立移動模型。

### 4. 同伴 AI 與戰鬥資訊相互配合

將敵方意圖暴露給 Gambit，讓玩家可配置「敵人正在蓄力時打斷」「隊伍將受範圍傷害時施盾／治療」等規則；主角保留手動主導權。避免讓同伴自動解掉所有機制，並提供可解釋的戰術觸發日誌與冷卻／資源代價。

## 四、建議實施順序

1. **先修正戰鬥生命週期規格（P1）**：釐清固定 5 分鐘絕對上限與 10 分鐘閒置規則，補齊有效操作與長戰測試。
2. **敵方行動最小垂直切片（P1）**：挑一種普通怪與一個首領，使用同一套資料驅動 action；落實預告、結算、可打斷／可防守窗口、DTO 與 UI。
3. **補玩家反制與同伴協作（P2）**：連接既有 Stamina、防禦、陣位、技能冷卻與 Gambit；先做少量能形成選擇的機制。
4. **首領階段及場地機制（P2）**：上述 action schema 穩定後，再增加階段切換和地格危險區；用 `DataIntegrityValidator` 驗證引用與數值邊界。
5. **SAN 異變資料化、前端整理與指標（P2/P3）**：將特殊玩法移出 Java 特例，逐步落實 UI 規範，並觀察反制成功率、技能使用率、戰鬥時長與玩家／隊伍倒下原因。

## 五、驗收建議

- 每個敵方 action 都能在 DTO／UI 看見「即將發生什麼、影響誰、何時結算」；正常操作下預告不能與結算同 tick 出現。
- 至少一個普通敵人與一個首領具有不同的機制行動，而非僅更換招式文字或提高傷害。
- 打斷、格擋／閃避、站位與失敗承受結果都有可測試的狀態變化、資源變化及戰報。
- 行動定義完全從資料載入；新增行動或首領階段不需新增 Java 專屬名稱分支；錯誤資料由啟動驗證指出來源。
- 使用可控時鐘與可替換隨機來源驗證蓄力、取消、命中、狀態過期及同 tick 競爭邊界；整合測試確認 WebSocket DTO 與前端操作閉環。
- 以單一首領遭遇驗證整體體驗後再擴寫內容，避免先建大型行為樹／腳本系統卻沒有可玩的機制回饋。

## 六、總結

專案不缺戰鬥系統的數量，下一階段應優先補上「敵方動作有實際效果、玩家能提前讀取、能用既有資源反制、成功後產生可見破綻」這條閉環。以資料驅動的敵方 Action／Telegraph 最小版本作為下一個垂直切片，並先解決逾時政策落差，能比直接增加更多數值、技能或複雜行為樹更有效地提升戰鬥的動態感與可讀性。
