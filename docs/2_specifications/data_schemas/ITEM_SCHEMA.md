# ⚔️ 裝備與隨機詞綴資料架構規格 (Item & Affix Schema Specification)

> **文檔層級**：Level 2 — Module Specifications (data_schemas)  
> **系統類型**：資料驅動裝備模板、掉落表與隨機詞綴契約  
> **核心定位**：定義純單機 Hack-and-slash DRPG 裝備部位（5+2 結構）、稀有度色彩、隨機詞綴庫與分解重鑄契約。

---

## 📌 一、核心設計原則

1. **小隊 5+2 部位裝備制**：每位隊員擁有 7 個獨立裝備槽位：`MAIN_HAND`（主手）、`OFF_HAND`（副手）、`HEAD`（頭部）、`BODY`（胸甲身軀）、`FEET`（足部履鞋）、`ACCESSORY_1`（靈物飾品1）、`ACCESSORY_2`（義體/晶片飾品2）。
2. **七階稀有度體系 (Rarity Tiers)**：`POOR`（灰劣）、`COMMON`（白普）、`UNCOMMON`（綠良）、`RARE`（藍精）、`EPIC`（紫史）、`LEGENDARY`（橙傳）、`MYTHIC`（紅神話）。
3. **隨機詞綴 (Affix System)**：裝備掉落時由 `ItemAffixService.java` 動態附加 **前綴 (Prefix)** 與 **後綴 (Suffix)**，數值浮動，並於 DTO 投影時合成全名（如「超頻透鏡之千星科技加密追蹤晶片」）。

---

## 📐 二、裝備與詞綴結構定義 (Schema)

### 1. 裝備範本定義 (ItemTemplate - 靜態資料表)

> 💡 **契約對齊**：對齊 `ItemTemplate.java`、`ItemType.java` 與 `EquipmentProp.java` 真實結構。頂層宣告 `"type"`（相容 `"QUEST"`、`"KEY"` 別名），裝備屬性集中於 `"stats"`（`@JsonAlias("equipment")`），消耗品屬性集中於 `"behavior"`。

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ItemTemplate",
  "type": "object",
  "required": ["id", "name", "type"],
  "properties": {
    "id": { "type": "string", "description": "物品唯一識別碼" },
    "name": { "type": "string", "description": "物品基礎名稱" },
    "aliases": { "type": "array", "items": { "type": "string" }, "description": "指令識別別名清單" },
    "description": { "type": "string", "description": "物品敘事與功能說明" },
    "type": {
      "type": "string",
      "enum": ["WEAPON", "SHIELD", "ARMOR", "ACCESSORY", "CONSUMABLE", "KEY_ITEM", "MATERIAL", "CONTAINER", "CURRENCY", "CORPSE", "MISC"],
      "description": "物品主類型（相容別名 KEY, QUEST, TRASH）"
    },
    "subType": { "type": "string", "description": "子類型，如 SWORD, CHIP, POTION" },
    "quality": {
      "type": "string",
      "enum": ["POOR", "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC"],
      "default": "COMMON"
    },
    "value": { "type": "integer", "default": 0, "description": "買賣基準靈石價值（相容 price）" },
    "level": { "type": "integer", "default": 1, "description": "物品裝備或需求等級" },
    "isStackable": { "type": "boolean", "default": false, "description": "是否可堆疊存放" },
    "bonusStats": { "type": "object", "description": "自訂屬性加成對應表" },
    "stats": {
      "type": "object",
      "description": "裝備屬性區塊（映射 EquipmentProp.java，相容 equipment 別名）",
      "properties": {
        "slot": {
          "type": "string",
          "enum": ["MAIN_HAND", "OFF_HAND", "HEAD", "BODY", "FEET", "ACCESSORY_1", "ACCESSORY_2"]
        },
        "minDamage": { "type": "integer", "description": "最小傷害" },
        "maxDamage": { "type": "integer", "description": "最大傷害" },
        "defense": { "type": "integer", "description": "基礎防禦" },
        "attackSpeed": { "type": "integer", "description": "攻速（毫秒間隔或攻擊頻率）" },
        "poiseBreakPower": { "type": "integer", "description": "戰鬥削韌強度" },
        "critRate": { "type": "integer", "description": "暴擊率百分比" },
        "critDamage": { "type": "integer", "description": "暴擊傷害百分比" },
        "hitRate": { "type": "integer", "description": "命中率加成" },
        "weight": { "type": "integer", "description": "裝備重量" },
        "maxDurability": { "type": "integer", "description": "最大耐久度（-1 為無限）" }
      }
    },
    "behavior": {
      "type": "object",
      "description": "消耗品效果區塊（映射 ConsumableProp.java）",
      "properties": {
        "effect": { "type": "string", "description": "效果枚舉或方法名稱" },
        "value": { "type": "integer", "description": "效果基礎數值" },
        "cooldown": { "type": "integer", "description": "冷卻時間" },
        "message": { "type": "string", "description": "使用後回饋文本" }
      }
    }
  }
}
```

### 2. 裝備執行個體與行囊槽位定義 (PartyItemSlot - 執行期/存檔持有態)

> 💡 **契約對齊**：對齊 `PartyItemSlot.java` 真實欄位。動態詞綴（`affixes`）由 `ItemAffixService.java` 於掉落時動態賦予，並由 `getDisplayNameWithAffixes()` 組合前後綴呈現。

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "PartyItemSlot",
  "type": "object",
  "required": ["slotId", "itemId", "name", "itemType"],
  "properties": {
    "slotId": { "type": "string", "description": "行囊或裝備唯一槽位代號" },
    "itemId": { "type": "string", "description": "引用的物品範本 ID" },
    "name": { "type": "string", "description": "物品基礎名稱" },
    "icon": { "type": "string", "default": "📦" },
    "itemType": { "type": "string", "description": "物品主類型枚舉" },
    "subType": { "type": "string", "description": "物品子類型" },
    "equipSlot": {
      "type": "string",
      "enum": ["MAIN_HAND", "OFF_HAND", "HEAD", "BODY", "FEET", "ACCESSORY_1", "ACCESSORY_2"],
      "description": "裝備槽位（供前端裝備操作與比對）"
    },
    "count": { "type": "integer", "default": 1 },
    "maxStack": { "type": "integer", "default": 99 },
    "quality": { "type": "string", "default": "COMMON" },
    "bonusMinDamage": { "type": "integer", "default": 0 },
    "bonusMaxDamage": { "type": "integer", "default": 0 },
    "bonusDefense": { "type": "integer", "default": 0 },
    "bonusHp": { "type": "integer", "default": 0 },
    "bonusSan": { "type": "integer", "default": 0 },
    "poiseBreakPower": { "type": "integer", "default": 0, "description": "戰鬥削韌強度" },
    "cyberLore": { "type": "string", "description": "特工加密背景故事文本" },
    "affixes": {
      "type": "array",
      "description": "執行期賦予的隨機詞綴清單",
      "items": {
        "type": "object",
        "required": ["id", "type", "name", "statKey", "value"],
        "properties": {
          "id": { "type": "string" },
          "type": { "type": "string", "enum": ["PREFIX", "SUFFIX"] },
          "name": { "type": "string" },
          "statKey": { "type": "string" },
          "value": { "type": "number" }
        }
      }
    }
  }
}
```

---

## 📝 三、標準資料範例

### 範例一：靜態物品範本 (`quest_items.json` 之 `item_chip_qianxing_tracker`)

```json
{
  "id": "item_chip_qianxing_tracker",
  "name": "千星科技加密追蹤晶片",
  "aliases": [
    "chip",
    "tracker",
    "晶片",
    "追蹤晶片",
    "千星晶片"
  ],
  "description": "自宋天衡畸變心核殘骸中剝離的千星聯合科技加密特工晶片，指示燈仍以 12Hz 規律閃爍。",
  "type": "KEY_ITEM",
  "subType": "CHIP",
  "stats": {
    "slot": "ACCESSORY_2",
    "attackSpeed": 8,
    "poiseBreakPower": 12
  },
  "value": 500,
  "quality": "LEGENDARY"
}
```

### 範例二：執行期行囊槽位與詞綴附加 (`PartyItemSlot`)

```json
{
  "slotId": "slot_acc_01",
  "itemId": "item_chip_qianxing_tracker",
  "name": "千星科技加密追蹤晶片",
  "icon": "💾",
  "itemType": "KEY_ITEM",
  "subType": "CHIP",
  "equipSlot": "ACCESSORY_2",
  "count": 1,
  "maxStack": 1,
  "quality": "LEGENDARY",
  "poiseBreakPower": 12,
  "cyberLore": "千星聯合科技軍規外殖特工專用晶片，內建自毀協定與反制頻譜。",
  "affixes": [
    {
      "id": "affix_overclock_prec",
      "type": "PREFIX",
      "name": "超頻透鏡之",
      "statKey": "critRate",
      "value": 5.0
    },
    {
      "id": "affix_signal_jam",
      "type": "SUFFIX",
      "name": "干擾頻譜",
      "statKey": "attackSpeed",
      "value": 12.0
    }
  ]
}
```

---

## 🔮 四、長線演進藍圖【待深入討論／長線演進藍圖】

> ⚠️ **架構邊界備忘**：以下結構尚未於 `ItemTemplate.java` 靜態類別中實作欄位，屬於中長線演進藍圖。現行隨機詞綴生成統一由 `ItemAffixService.java` 全局池處理，多階認知透鏡則由 `TownHubService` 與 DRPG 視圖渲染器動態解析。

### 1. 範本級詞綴池與多階認知透鏡草案 (`affixPool` & `lenses`)

```json
{
  "id": "blueprint_chip_qianxing_tracker",
  "name": "千星科技加密追蹤晶片（演進藍圖）",
  "affixPool": [
    {
      "id": "affix_overclock_prec",
      "type": "PREFIX",
      "name": "超頻透鏡之",
      "statKey": "critRate",
      "minValue": 3.0,
      "maxValue": 8.0
    },
    {
      "id": "affix_signal_jam",
      "type": "SUFFIX",
      "name": "干擾頻譜",
      "statKey": "attackSpeed",
      "minValue": 5.0,
      "maxValue": 15.0
    }
  ],
  "lenses": [
    {
      "minProgress": 0,
      "name": "一團怪物的血肉",
      "desc": "自宋天衡畸變殘骸剝離的黏稠組織，散發古怪腥香，偶爾抽搐。"
    },
    {
      "minProgress": 30,
      "name": "閃爍幽光的異化道骨",
      "desc": "骨晶內部浮現微弱的金色電路狀細紋，隱約伴隨 12Hz 節律跳動。"
    },
    {
      "minProgress": 70,
      "name": "【千星科技加密追蹤晶片】",
      "desc": "千星聯合科技外殖軍規特工加密晶片，指示燈以 12Hz 規律閃爍，解鎖反制頻譜。"
    }
  ]
}
```
