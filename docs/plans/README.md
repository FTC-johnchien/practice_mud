# 專案實施計畫與架構決策歸檔中樞 (Architecture Plans & Decision Records)

本目錄作為專案的核心架構決策紀錄（ADR）與實施計畫（Implementation Plans）的永久留存庫。
每次重要架構重構、核心玩法機制設計、併發安全調整與功能推進時，均在此目錄下留下結構化紀錄。

---

## 📌 AI Review 鐵律規範 (Mandatory Review Rules for AI)
1. **新功能設計對齊**：在啟動重大新模組開發前，AI **必須主動檢索與 Review** 本目錄下相關模組的歷史 Plan，嚴格杜絕與過去架構決策（如 Actor 模型無鎖併發、雙模地圖切換、小隊 5+2 裝備）相衝突。
2. **Bug 排查與重構防禦**：修復問題或重構時，AI 必須 Review 該模組當時的邊界條件假設與防禦設計，避免「修復 A 卻破壞 B」。
3. **持續更新與版本留存**：每次重大架構決策與 Implementation Plan 確定後，必須同步在此目錄新增 Markdown 檔案並提交 Git。

---

## 📂 實施計畫歸檔索引 (Plan Archive Index)

| 日期 | 計畫檔案 | 核心主題與涵蓋範圍 | 狀態 |
| :--- | :--- | :--- | :--- |
| **2026-09-18** | [`2026-09-18_loot_pouch_and_entity_cleanup.md`](./2026-09-18_loot_pouch_and_entity_cleanup.md) | 怪物死亡去屍體化、戰利品儲物袋 (Loot Pouch) 生成、同場自動合併 (方案 B)、一鍵搜刮全拿與 Room Actor 併發時序修復 | ✅ 已完工並驗證 |
| **2026-09-18** | [`2026-09-18_weapon_stances_and_race_attacks.md`](./2026-09-18_weapon_stances_and_race_attacks.md) | 多武器普攻動態綁定、高級劍法專修切換（太極/太陰/天劍）、非人種族天生攻擊池系統 | ✅ 已完工並驗證 |
| **2026-09-18** | [`2026-09-18_wow_spellbook_and_ui_optimization.md`](./2026-09-18_wow_spellbook_and_ui_optimization.md) | WoW 經典三 Tab 法術書典籍、戰鬥技能抽屜 In-place DOM 防閃爍、敵群 1~7 體陣列與集火鎖定 | ✅ 已完工並驗證 |
| **2026-09-18** | [`2026-09-18_data_driven_architecture_and_entity_relations.md`](./2026-09-18_data_driven_architecture_and_entity_relations.md) | 全實體資料驅動 (Data-Driven) 現況審查、Mermaid 實體關係圖與生命週期資料流總覽 | 📋 架構審查完畢 |
| **2026-09-18** | [`2026-09-18_combat_logic_damage_parry_dodge_and_growth.md`](./2026-09-18_combat_logic_damage_parry_dodge_and_growth.md) | 戰鬥邏輯、傷害公式、身法閃避 (Dodge)、兵刃招架 (Parry)、盾牌格擋 (Block)、一次擲骰圓桌判定、魂系精力博弈與角色升級雙軌制 | 📋 架構規範確立 |
