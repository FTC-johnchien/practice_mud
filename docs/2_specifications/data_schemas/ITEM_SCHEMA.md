# ⚔️ 裝備與隨機詞綴資料架構規格 (Item & Affix Schema Specification)

> **文檔層級**：Level 2 — Module Specifications (data_schemas)  
> **系統類型**：資料驅動裝備模板、掉落表與隨機詞綴契約  
> **核心定位**：定義純單機 Hack-and-slash DRPG 裝備部位（5+2 結構）、稀有度色彩、隨機詞綴庫與分解重鑄契約。

---

## 📌 一、核心設計原則

1. **小隊 5+2 部位裝備制**：每位隊員擁有 7 個獨立裝備槽位：`MAIN_HAND`（主手）、`OFF_HAND`（副手）、`HEAD`（頭部）、`BODY`（胸甲身軀）、`FEET`（足部履鞋）、`ACCESSORY_1`（靈物飾品1）、`ACCESSORY_2`（義體/晶片飾品2）。
2. **七階稀有度體系 (Rarity Tiers)**：`POOR`（灰劣）、`COMMON`（白普）、`UNCOMMON`（綠良）、`RARE`（藍精）、`EPIC`（紫史）、`LEGENDARY`（橙傳）、`MYTHIC`（紅神話）。
3. **隨機詞綴 (Affix System)**：裝備掉落時動態附加 **前綴 (Prefix)** 與 **後綴 (Suffix)**，數值浮動，支援裝備工坊分解與洗鍊重鑄。

---

## 📐 二、裝備與詞綴結構定義 (Schema)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ItemTemplateAndAffixes",
  "type": "object",
  "required": ["id", "name", "slot", "quality", "baseStats"],
  "properties": {
    "id": { "type": "string" },
    "name": { "type": "string" },
    "slot": { "type": "string", "enum": ["MAIN_HAND", "OFF_HAND", "HEAD", "BODY", "FEET", "ACCESSORY_1", "ACCESSORY_2"] },
    "quality": { "type": "string", "enum": ["POOR", "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC"] },
    "itemType": { "type": "string", "enum": ["WEAPON", "ARMOR", "SHIELD", "CHIP_ACCESSORY", "CONSUMABLE"] },
    "requiredLevel": { "type": "integer", "default": 1 },
    "baseStats": {
      "type": "object",
      "properties": {
        "physicalAttack": { "type": "integer" },
        "magicAttack": { "type": "integer" },
        "poiseBreakPower": { "type": "integer", "description": "戰鬥削韌強度" },
        "armor": { "type": "integer", "description": "物理護甲" },
        "speed": { "type": "integer", "description": "出手先攻" }
      }
    },
    "maxAffixes": { "type": "integer", "default": 2 },
    "affixPool": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["id", "type", "name", "statKey", "minValue", "maxValue"],
        "properties": {
          "id": { "type": "string" },
          "type": { "type": "string", "enum": ["PREFIX", "SUFFIX"] },
          "name": { "type": "string" },
          "statKey": { "type": "string" },
          "minValue": { "type": "number" },
          "maxValue": { "type": "number" }
        }
      }
    }
  }
}
```

---

## 📝 三、範例：首領掉落【千星科技加密追蹤晶片】與高頻刷寶裝備

```json
{
  "id": "item_chip_qianxing_tracker",
  "name": "千星科技加密追蹤晶片",
  "slot": "ACCESSORY_2",
  "quality": "RARE",
  "itemType": "CHIP_ACCESSORY",
  "requiredLevel": 1,
  "baseStats": {
    "speed": 8,
    "poiseBreakPower": 12
  },
  "maxAffixes": 2,
  "affixes": [
    {
      "id": "affix_overclock_prec",
      "type": "PREFIX",
      "name": "超頻透鏡之",
      "statKey": "critChancePercent",
      "value": 5.5
    },
    {
      "id": "affix_signal_jam",
      "type": "SUFFIX",
      "name": "干擾頻譜",
      "statKey": "interruptWindowExtendMs",
      "value": 500
    }
  ],
  "loreDescription": "自宋天衡畸變心核殘骸中剝離的千星殖民巨企加密特工晶片，指示燈仍以 12Hz 規律閃爍。"
}
```
