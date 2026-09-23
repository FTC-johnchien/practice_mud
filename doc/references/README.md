# 遊戲專案架構與設計全景總覽 (Project Reference Index)

歡迎查閱本專案的核心設計與架構參考文件庫。本系列文件依據系統模組精確劃分，旨在提供清晰、全面且具備前瞻性的設計藍圖，供團隊審視、校對與持續迭代。

---

## 一、文件閱讀指引

每一份專題文件均遵循統一的三層結構標準：
1. **【已完成實作 (Implemented)】**：當前程式庫中已編寫完畢、通過單元測試並穩定運行的核心功能。
2. **【先前討論・尚未實作 (Planned / Discussed)】**：團隊成員或歷次對話中已達成共識、納入設計藍圖但尚未完成編碼落地的功能（含即將落實的項目）。
3. **【架構師建議 (Recommendations)】**：技術架構師針對系統擴展性、效能優化、UI/UX 沉浸感及未來演進提出的專業建議與深層架構方案。

---

## 二、全景專題模組導覽

| 編號 | 專題領域 | 核心文件連結 | 關鍵主題與涵蓋內容 |
| :--- | :--- | :--- | :--- |
| **01** | **玩家角色** | [01_player_characters.md](./01_player_characters.md) | 五維道基、修為境界、HP/MP/SP 動能、理智值 (SAN) 與走火入魔、前端雙向同步 |
| **02** | **可組隊夥伴** | [02_party_companions.md](./02_party_companions.md) | 四大經典夥伴（鐵牛/燕青/凌霜/墨衍）、招募與離隊、Gambit 戰術方針 AI、好感度 |
| **03** | **世界與城鎮 NPC** | [03_town_and_world_npcs.md](./03_town_and_world_npcs.md) | 客棧福伯、貨棧錢老九、傳功長老、商店交易協議、對話狀態機與前端 Modal 整合 |
| **04** | **物品與裝備** | [04_items_and_equipment.md](./04_items_and_equipment.md) | 5+2 槽位裝備欄、去綴比對堆疊、道核領悟、耐久與強化、背包抽屜介面規範 |
| **05** | **怪物與深淵畸變** | [05_mobs_and_aberrations.md](./05_mobs_and_aberrations.md) | 野生生物、地城妖物、深淵畸變體（SAN 壓迫感）、首領仇恨與階段狂暴 AI |
| **06** | **任務與冒險** | [06_quests_and_adventure.md](./06_quests_and_adventure.md) | 新手村序章引導、DRPG 地城探索度里程碑、夥伴專屬身世羈絆、委託榜懸賞機制 |
| **07** | **種族與職業** | [07_races_and_classes.md](./07_races_and_classes.md) | 人/妖/靈三界種族特性、四大基礎職業（戰/法/刺/牧）、無武器限制兵器相性體系 |
| **08** | **小隊陣法** | [08_formations.md](./08_formations.md) | 四象封魔陣、前後排承傷權重、陣法專屬絕技、靈威能量槽、全隊被動光環 |
| **09** | **技能與法術** | [09_skills_and_spells.md](./09_skills_and_spells.md) | 三層真相源架構、被動裝配 (Enable) 槽位、2~5 人多人合擊、五行真法、WoW+DQ/FF 介面 |
| **10** | **戰鬥機制** | [10_combat_dynamics.md](./10_combat_dynamics.md) | 命中/閃避/招架/護甲分層判定鏈、仇恨嘲諷、動能引擎、異常狀態、回合敏捷排序 |
| **11** | **城鎮與地城** | [11_town_and_dungeon.md](./11_town_and_dungeon.md) | MUD 10向拓撲圖、DRPG 4向網格迷宮、迷霧探索 (Fog of War)、雷達小地圖、雙模切換 |
| **12** | **畫面 Layout** | [12_ui_ux_layout.md](./12_ui_ux_layout.md) | PC 桌面端優先排版、6 大核心視圖佈局、常用技能快捷列 (1~6)、DQ/FF 階層式抽屜 |

---

## 三、下次工作銜接與執行路線 (Resume Roadmap)

當您休息完畢返回工作時，系統將依據已定之整體演進路線：
```
Direction C (架構解耦) -> Direction A (被動技能裝配與檢驗) -> Direction C (領域邊界解耦)
```

- **當前位置**：**Direction C 第一階段已 100% 完成**（156/156 測試全數通過，Git Commit 已提交）。
- **下次即刻執行目標**：**Direction A (Phase 9) —— 被動技能裝配 (Enable) 與戰鬥檢驗系統**。
  - 詳細可執行技術方案已完整載明於根目錄之 [`implementation_plan.md`](../../implementation_plan.md) 與 [`FUTURE_IMPROVEMENTS.md`](../../FUTURE_IMPROVEMENTS.md)。
  - 無需重新梳理上下文，輸入指令即可無縫繼續編碼實作！
