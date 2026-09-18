# 實施計畫：怪物死亡去屍體化、戰利品儲物袋 (Loot Box) 生成、同場自動合併與一鍵全收機制

## 1. 背景與動機
- 傳統 MUD 在怪物死亡時會將死屍（`ItemType.CORPSE`）丟到房間地面。
- 出現的問題：
  1. 玩家點擊拾取時，會把整具巨鼠死屍塞入行囊，極度出戲且不合理。
  2. 沒有掉落物的怪死後留下空屍體，造成主舞台按鈕雜亂洗版。
  3. 連續擊殺多隻野鼠時，地面產生多個儲物袋按鈕，需要反覆逐一點擊，操作繁瑣。
  4. Room Actor 非同步郵箱入隊與同步廣播之間存在 Race Condition，導致擊殺後儲物袋短暫漏顯示。

## 2. 架構決策與實施方案 (Architecture Decisions)

### 2.1 怪物死亡處理分流 (`LivingService.java`)
- 怪物氣血歸零時，調用 `WorldFactory.generateMobDrops(mob)` 計算掉落物：
  - **無掉落物**：怪物化作一縷青煙消散，地面**不產生任何實體或按鈕**。
  - **有掉落物**：
    - 首領 / 精英（Boss/Elite）：生成專屬的 `【某某的戰利品寶箱】`，具備獨立靈光與別名。
    - 普通怪物：生成 `【散落的儲物袋】`（`ItemType.CONTAINER`）。

### 2.2 同場戰鬥戰利品自動合併 (方案 B - Option B)
- 若普通怪陣亡時，房間地面已存在 `【散落的儲物袋】`：
  - 不再產生新的儲物袋實體，而是將本次掉落的所有物品直接 append 到既有儲物袋中（`pouch.getContents().addAll(drops)`）。
  - 日誌提示：`💥 $N 被擊敗倒地，戰利品歸攏入地面的【散落的儲物袋】！`。
  - 地面始終保持**單一儲物袋**，杜絕洗版。

### 2.3 容器件數可視化與一鍵全收 (`GameStateBroadcastService.java` & `GetCommand.java`)
- 廣播時若容器含有 contents，按鈕動態顯示：`[👝 搜刮 【散落的儲物袋】 (內含 X 件靈物)]`。
- 玩家發出 `get` / `loot` / `open` 指令時，後端一次性將容器內所有道具移入小隊背包 `PartyInventory`，條列輸出戰利品，容器隨風消散，地面恢復乾淨。
- 禁止任何非任務用的純死屍道具進入背包。

### 2.4 Room Actor 集合立即同步可見性 (`Room.java`)
- `Room` 內的 `items`、`mobs` 本身採用 `CopyOnWriteArrayList`。
- `dropItem`、`removeMob`、`removeItem` 修改為同步操作底層集合並維持 Actor 郵箱處理冪等，徹底根除廣播讀取舊快照的 Race Condition。

## 3. 驗證與測試
- 單元與整合測試：`ItemPickupAndEntitySyncTest.java`
  - `testMultipleMobDeathsAutoMergeIntoSinglePouch`：驗證連續殺怪自動合併至單一儲物袋、一鍵搜刮全收、容器即刻銷毀。
  - `testCorpseCannotEnterBag`：驗證純死屍嚴禁入包。
  - `testProtagonistNameSynchronization`：驗證主角道號一致性。
