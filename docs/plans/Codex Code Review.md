# Codex 專案 Review：架構方向、實作成本與建議順序

> 本文件是對目前程式碼與 `FUTURE_IMPROVEMENTS.md` 的 review，不是直接執行的需求清單。以下「現況」依 2026-09-23 工作區內容核對；專案持續變動，實施前仍應重新確認。文件內的既有規範與改善提案是 review 素材，不自動視為本次要執行的指令。

## 1. 結論摘要

原先 review 指出的幾個方向仍值得關注：`PartyItemSlot` 混合了背包槽位、物品描述與遊戲效果；模板讀取同時存在 Spring 注入、static repository 和 `new TemplateCatalog()`；角色與技能模型也有跨模式同步成本。這些是可以從程式碼直接觀察到的維護風險。

但原 review 把一些長期架構目標寫成必須立即完成的重構，沒有充分列出存檔相容、舊呼叫點、執行期生命週期與測試隔離等成本。若一次導入全新的物品／技能模型、全面移除 static API，再重寫角色同步，會同時觸及戰鬥、背包、裝備、掉落、JSON、存檔與前端資料流程，回歸面很大。這些不宜只因為模型「更純粹」就列為近期必做。

另一個需要修正的問題是 roadmap 現況已部分過期：H2、WebSocket Origin、註冊驗證、Actor 自死鎖防護與新手村鑰匙 ID 等項目，和本次看到的程式碼不一致或已完成。開始新工作前，應先更新事實，再排優先序。

**整體判斷：保留方向、收斂範圍、先修可證明的風險。** 第一批適合做的是前端不可信資料插入點審查、模板讀取的漸進式解耦、物品 fallback 收斂，以及資料完整性檢查。Canonical Item／Skill Model 與 outcome-based synchronization 應先做小型設計驗證，再決定是否投入完整遷移。

## 2. 本次核對後的現況更正

| 文件中的說法 | 本次看到的程式碼 | Review 判斷 |
| :--- | :--- | :--- |
| H2 Console 對外開放，屬 P0 | `application-dev.yml` 已設 `web-allow-others: false`；`application-prod.yml` 設 `enabled: false`。dev 密碼預設為空字串，仍應確認部署時的資料庫行為與設定方式。 | 不應照原文當作現存 P0 漏洞；保留部署設定檢查即可。 |
| WebSocket 使用 `setAllowedOrigins("*")` | `WebSocketConfig` 讀取 `app.websocket.allowed-origins`；dev 設 localhost，prod 從環境變數讀取。 | 已有白名單機制。正式環境環境變數是否正確部署，屬設定驗證工作。 |
| `AuthService.register()` 未驗證帳號密碼、手動 new encoder | 註冊流程會呼叫 `validateUsername()`、`validatePassword()`，並透過建構子注入 `PasswordEncoder`；`SecurityConfig` 宣告 BCrypt Bean。 | 此待辦已完成，不應再列為待修。 |
| Actor 自我等待會死鎖，runLoop 例外會殺死 Actor | `VirtualActor` 已提供 `isActorThread()`，訊息處理有 `catch (Throwable)` 隔離；`Living` 等類別使用 Actor 執行緒判斷。 | 原列出的基本防護已存在。是否有其他 Actor 生命週期缺口，需由具體案例提出，不應重做已完成項目。 |
| 每個 Room 各建排程器，造成執行緒洩漏 | `RoomMessageBuffer` 使用共用 daemon scheduler。 | 此項已完成；可另外檢查關閉生命週期，但目前程式已不是 per-room scheduler。 |
| 房間鑰匙仍引用 `village_elder_key` | 目前 `rooms.json` 引用 `global:village_elder_house_key`，相關 mob 掉落也使用同一 ID。 | 文件中的現況已過時。 |
| `snow/items.json` 有重複 `heavy_hammer` | 檔案中可見 `snow_heavy_hammer`（鍛造鐵鎚）與 `heavy_hammer`（沉重鐵鎚），ID 和資料不同；mob 裝備資料又引用 `snow:heavy_hammer`。 | 名稱相似不足以證明重複。需確認解析器是否把 `snow:heavy_hammer` 正確解析到哪筆資料，再決定是否改 ID。 |
| Domain 全面依賴 Application／Infrastructure | 已存在具體反向依賴，例如 `PlayerService` 引用 Auth、WebSocket handler、GuiBridge；`RoomService` 引用 `WorldFactory`；`TemplateCatalog` 依賴 infra repository；`PartyItemSlot` 呼叫 static `TemplateRepository`。 | 問題真實，但應以會造成測試、循環依賴或修改困難的依賴為優先，不必只為符合分層而全面搬動套件。 |

## 3. 主要改善方向與實作取捨

### 3.1 前端 HTML 插入與 XSS 風險

`drpg-view.js` 存在把資料插入 `innerHTML` 的位置，例如 NPC 名稱、房間名稱、提示訊息與存檔中的角色名稱。這些資料有些來自伺服器或遊戲資料；若玩家能建立或影響該內容，就可能形成 DOM XSS。應依資料來源逐點確認，不宜只用「有 `innerHTML`」就判定可利用，也不宜只加一個 `escapeHtml()` 就宣稱全數安全。

**建議作法：**先盤點帶入 `innerHTML` 的動態值，記錄來源與用途。純文字優先以 `textContent` 建構；確實需要格式化 HTML 時，只對文字內容作正確 escaping，避免將未信任資料放進屬性、URL 或 HTML 結構。再補上可重現的輸入案例驗證輸出。

**優點：**直接降低可利用風險，範圍可以小而明確，也容易逐點驗收。

**代價／注意事項：**動態 HTML 位置很多，不能只審查 `renderTownMainStage` 等少數函式便視為完成；ANSI 轉 HTML 等受控轉換也要確認輸入先經過安全處理。一次重寫整個 view 容易引入顯示回歸。

**優先度：**先做來源盤點，再修有不可信來源且能到達 `innerHTML` 的位置。這比原文件將全站 XSS 無差別定為 P0 更可操作。

### 3.2 TemplateRepository／TemplateCatalog 讀取路徑

`TemplateRepository` 雖由 Spring 管理，仍保留 static singleton、static API 與 static maps；`TemplateCatalog` 也可無參數建構並回退到 singleton。多個服務和 model 仍自行 `new TemplateCatalog()`，而 `PartyItemSlot.toGameItem()` 直接呼叫 static repository。這會讓資料來源和測試狀態不容易追蹤，是目前較清楚的架構債。

**建議作法：**不要先刪除相容 API。先讓應用服務與 factory 優先注入 `TemplateReader`，新增需要的 instance 方法，再逐批遷移 static 呼叫點；最後才評估刪除 singleton／static facade。Domain 物件若需要模板查詢，應把查詢移到 mapper、factory 或 service，而非由 entity 自己連到 infrastructure。

**優點：**逐步改善測試隔離和依賴方向；每一批遷移可單獨編譯與驗證。

**代價／注意事項：**目前 `PartyInventory` 等 POJO 會在建構時建立預設內容，且 `TemplateCatalog` 被多處當作便利 fallback。需先確認 Spring 建立流程、Jackson 反序列化、測試 fixtures、啟動時載入順序；貿然移除 no-arg constructor 或 static API 會造成大量連鎖修改。所謂「production 不允許 new」要落在實際架構約束或靜態檢查，不能只靠文件宣告。

**優先度：**中高，適合漸進重構；不是單一 PR 一次清除全部 static 存取。

### 3.3 PartyItemSlot、GameItem 與物品定義

`PartyItemSlot` 同時保存 `slotId`、數量、顯示欄位、效果和裝備加成；`fromItemTemplate()` 也會推導消耗品效果與裝備數值。`PartyInventory.createFromTemplate()` 在找不到模板時會依 ID 子字串產生備援物品，形成第二套隱含資料規則。這些實作確實會增加不同模式資料漂移的可能。

原先提出的 `ItemDefinition + ItemInstance + ItemView` 是合理的長期模型，但不代表應立刻把現有類別整批替換。DRPG 的物品槽具有自己的裝備欄、堆疊和顯示需求；MUD `GameItem` 也保存名稱、描述等可能受掉落生成或存檔影響的值。需要先決定「實例建立後是否允許改名／詞綴／耐久」及「舊存檔如何解析」，才能判斷哪些欄位可以只從 definition 讀取。

**建議作法：**先做低風險收斂：移除或明確標記 fallback 的用途；找不到模板時回報錯誤或使用一個可觀測的 unknown-item 表示，不要依 ID 猜內容；把效果推導搬到單一 mapper/factory；列出存檔與網路 DTO 對 `PartyItemSlot` 的依賴。完成後選一類物品（例如消耗品）做 definition／instance 試點，再比較實作成本。

**優點：**先消除最容易掩蓋資料錯誤的 fallback，保留既有戰鬥與裝備流程；試點能得到真實遷移成本。

**代價／注意事項：**完整模型仍會影響 Party inventory、MUD 掉落、拾取、裝備、存檔、JSON 欄位與前端 view。需提供舊格式讀取或存檔遷移策略；否則使用者舊存檔可能無法正常載入。新的 DTO 也不應被當作業務實例，需確認其生命週期和更新方向。

**優先度：**中。先處理 fallback／轉換責任；完整 canonical item model 先做設計驗證，不直接排成近期大重構。

### 3.4 SkillTemplate 與 PartyMemberSkill

MUD 的 `SkillTemplate` 和 DRPG 的 `PartyMemberSkill` 有相似的識別、名稱及效果資料，`party_skills.json` 與 global skill JSON 也值得檢查重複範圍。另一方面，兩種模式可能有不同成本、冷卻、熟練度和執行效果；兩套 runtime model 本身不等於錯誤重複。

**建議作法：**先建立欄位對照表，標示哪些是相同的靜態定義、哪些屬於角色習得／熟練度、哪些屬於模式專屬規則。若共用定義可減少重複，再採用共用 definition 加各自 executor／projection；保留模式專屬 runtime 狀態。

**優點：**避免為了「只剩一個 class」而硬把兩種戰鬥規則塞進過度通用的 schema。

**代價／注意事項：**多一層 adapter 和規則分派；資料 schema 變動需要同步 loader、測試資料與既有存檔。先用一個相同技能做垂直切片，確認新增技能是否真的更簡單。

**優先度：**中低，等待物品定義試點與欄位盤點結果。

### 3.5 CharacterSyncService 與狀態權責

`Player` 和 `PartyMember` 不是繼承關係，並由 `CharacterSyncService` 同步部分狀態。原 review 建議戰鬥結束改用 outcome，而不雙向複製 stats。這在責任上較清楚，但要先看現有同步欄位、戰鬥中玩家能否同時從 MUD 狀態變更、傷害／治療／升級如何落盤。

**建議作法：**先列出每個欄位的 owner 與同步時機，例如角色長期成長、當前 HP/MP、戰鬥臨時資源、裝備與物品。只將確定是戰鬥結果的變化改為明確 outcome；不要一次替換所有同步邏輯。

**優點：**可降低複製整份 stats 導致覆寫新狀態的風險，讓戰鬥結算更可測。

**代價／注意事項：**若 outcome 漏了現有同步欄位，就會造成戰鬥結束後資料不一致；若戰鬥中仍允許非戰鬥變更，結算合併規則必須清楚。這是需要流程圖和案例表支援的重構，不只是換一個 DTO。

**優先度：**中低，先做 ownership 清查，再選單一欄位或結算路徑試點。

### 3.6 Domain 分層、命令去重與 Enum 命名

Domain 直接依賴 Application／Infrastructure 的案例存在；`Direction`、`ResourceType` 同名型別也可能增加理解成本。命令別名或重複入口則應區分「重複解析／重複業務邏輯」與「同一功能的合法別名」。

**建議作法：**先處理造成循環依賴、難以單元測試、或令 domain service 難以啟動的依賴；提取小型 port 時以實際呼叫方向為依據。命令先統一到共用 service，再保留不同命令作為語法入口。Enum 改名只有在呼叫情境常混淆時才做，並一次搜尋 Java、JSON、存檔和前端序列化名稱。

**優點：**能改善高摩擦點，同時維持使用者熟悉的命令與資料格式。

**代價／注意事項：**全面 ports/adapters 化會擴大介面數量與 wiring；enum rename 可能破壞 JSON enum 名稱、存檔或 API，相容性成本高於 Java 搜尋替換。

**優先度：**按具體缺陷排序，不以「Clean Architecture」作為獨立交付目標。

### 3.7 存檔隔離與多帳號

`SaveGameService` 將資料寫入 `saves/autosave.json` 和 `saves/slot_N.json`，而方法收有 `playerId`。路徑本身由整數槽位組成，這段程式沒有直接把使用者輸入拼進檔名；因此原文件把路徑遍歷列為現況攻擊，證據不足。但若服務已讓多個玩家共用同一組槽位，玩家間的讀寫隔離就是實際問題。

**建議作法：**確認目前 `SaveCommand`、WebSocket session 和 `SaveGameService.list/load/delete` 是否以登入玩家限制存檔操作。若多玩家可共用服務，優先用 `playerId` 或已驗證 account ID 分目錄，並在 service 層核對存檔內 `playerId` 與呼叫者；槽位範圍固定限定 0–5。新路徑應正規化並確保仍在 saves root 內。若產品確定只有單人本機使用，將隔離放到多人功能前置需求即可。

**優點：**明確保護資料擁有權；與未來帳號擴充需求直接相關。

**代價／注意事項：**需搬遷舊存檔，處理既有使用者與自動存檔；更改目錄而不做授權驗證，並不能完整解決存取控制。

**優先度：**由目前是否存在多使用者共用部署決定。若是線上多帳號，優先度高；若是單機 MVP，先寫清楚邊界並在上線前完成。

### 3.8 測試與死代碼清理

roadmap 指出 `Thread.sleep()`、測試覆蓋不足及部分 dead code，這些需要逐個方法確認。`rg` 搜尋可找到測試和執行期中的 sleep，但 sleep 本身不一定就是錯誤：遊戲攻擊節奏等功能可能刻意延遲；測試中的固定等待才通常是穩定性問題。`PlayerLoginListener` 目前是實際 Java 類別，不能只因文件稱「註解掉」就直接刪除。

**建議作法：**每個測試列出它驗證的行為、斷言和失敗模式；只有固定等待導致 flaky 或耗時的測試才改用條件等待／可控時鐘。清理死代碼前先搜尋呼叫、Spring 掃描註冊、反射與設定引用，再逐項移除。

**優點：**讓測試投資集中在高風險流程，而不是追求測試數量或為換工具而換工具。

**代價／注意事項：**測試替身和時鐘抽象也有維護成本；不值得為很少執行、沒有競態風險的測試引入大套件。

## 4. 建議實作順序

以下排序優先考慮風險、影響範圍和可驗收程度；不是要求同一版本全部完成。

### Phase A：校正現況，建立可執行清單

1. 更新 `FUTURE_IMPROVEMENTS.md`：移除或標記已完成的 H2、Origin、Auth、Actor 和鑰匙項目。
2. 查明雪區兩個鐵鎚的 ID 解析結果，決定它們是兩筆有效物品、alias，或真正的錯誤引用。
3. 將 roadmap 每一項拆成「可重現問題、受影響功能、完成條件、相容性風險」。

**原因：**過期清單會浪費實作時間，也會讓 AI 或開發者重做既有工作。這階段成本低，能提高後續所有優先序判斷的準確度。

### Phase B：處理可以局部驗收的真實風險

1. 審查前端動態 `innerHTML` 的資料來源，修補確認可由玩家或資料檔控制的輸入點。
2. 確認正式環境 WebSocket origins、資料庫密碼與 profile 是實際部署值，而非僅看範例設定。
3. 確認存檔操作是否已有玩家身份授權；依當前單機／多人產品範圍決定是否立即做玩家隔離。
4. 針對有效性、重複 ID、房間鑰匙、掉落物等建立可指出檔案和 ID 的資料完整性檢查；修正確定錯誤的引用。

**原因：**這些工作能針對明確的安全或資料問題，不必先改核心物件模型。前端修補應依來源和上下文逐點處理；安全設定則以部署環境為準。

### Phase C：漸進式收斂模板與物品資料流

1. 盤點 `TemplateCatalog` 建構、static repository 呼叫與 Domain → Infrastructure 依賴。
2. 先遷移常用的 factory/service 至注入 `TemplateReader`，讓測試可以使用獨立 reader。
3. 將 `PartyItemSlot` 的模板轉換和 fallback 邏輯移到集中 mapper/factory；明確處理找不到模板的情況。
4. 用單一消耗品或裝備做 `ItemDefinition + instance state + view` 垂直切片，涵蓋建立、拾取、使用／裝備、序列化與舊資料讀取，再決定是否擴大。

**原因：**這條路徑可先取得 DI 與資料一致性的收益，同時用真實改動估算 canonical model 的成本。先全域換類別會把風險一次推到所有玩法流程。

### Phase D：依實際收益決定較大的模型重構

1. 依 Phase C 試點評估是否需要全面 ItemDefinition／ItemInstance。
2. 盤點 MUD／DRPG 技能欄位和語意，選一個重疊技能驗證共用 definition 的收益。
3. 定義 Player／PartyMember 欄位 owner 與同步時機，再讓一條戰鬥結算路徑回傳明確 outcome。
4. 只對高摩擦的 Domain 外部依賴、命令重複和 Enum 歧義逐項抽介面／改名。

**原因：**這些方向有潛在長期價值，但範圍跨多層。先做資料流與案例驗證，能避免建立過度通用的 schema 或把資料同步拆成難以追查的多段 adapter。

## 5. 原 review 提案的優缺點總表

| 提案 | 保留價值 | 主要實作風險 | 本次建議 |
| :--- | :--- | :--- | :--- |
| Canonical Item Model | 減少模板／執行期／UI 欄位重複，便於跨模式共用定義。 | 背包、裝備、掉落、前端 DTO、Jackson 與舊存檔一起變動。 | 先收斂 fallback 和 mapper，再做單類型垂直切片。 |
| Canonical Skill Definition | 可共用識別與靜態內容，減少定義散落。 | MUD／DRPG 執行語意、角色熟練度和規則不相同。 | 先做欄位與語意盤點，再試一個技能。 |
| TemplateReader 唯一入口 | 改善測試替換與資料來源追蹤。 | static callers、no-arg 建構、反序列化和初始化時序。 | 注入優先、分批遷移，最後移除相容層。 |
| Outcome-based character sync | 明確角色狀態權責，避免整份 stats 覆寫。 | outcome 欄位遺漏、結算合併與同步時序錯誤。 | 先標出欄位 owner，改一條結算路徑。 |
| Domain Output Ports | 降低核心規則對 Web、Application、Infrastructure 的依賴。 | 介面和 adapter 增多，可能只有形式上的分層收益。 | 先抽測試痛點或循環依賴中的邊界。 |
| 前端全域 escapeHtml | 提供一致的 HTML 文字轉義工具。 | 不同 HTML 上下文不能一概而論；錯用仍可能有漏洞或破版。 | 先盤點不可信資料流，以 DOM API 優先逐點修補。 |
| 存檔按帳號分目錄 | 多玩家部署時保護玩家資料隔離。 | 舊存檔搬遷；路徑分隔不能取代 service 授權檢查。 | 先確認多人邊界與授權，再同時設計遷移。 |
| 全面拆分 JS／CSS | 大檔案較容易定位和維護。 | 無 bundler 的原生 module 會涉及載入順序、全域符號、HTML 引用和快取。 | 先抽低耦合模組，逐頁驗證，不以行數本身當交付目標。 |

## 6. Review 原則

- 把「已證實的缺陷」、「有證據的風險」、「架構改善偏好」分開標示。
- 優先寫清楚觸發條件與影響，不把理論上可能發生的情境直接寫成現有漏洞。
- 大型重構需包含相容策略、呼叫點盤點、可分批交付方式和回退界線。
- 完成狀態要連到程式碼或可重現驗證；同一改善不可同時列在已完成和待辦。
- 每個階段設定可觀察的完成條件；不以「完全解耦」、「百分之百可信」等無法驗收的目標結案。
