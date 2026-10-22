# 🗺️ DRPG 迷宮地牢資料架構規格 (Dungeon Schema Specification)

> **文檔層級**：Level 2 — Module Specifications (data_schemas)  
> **系統類型**：資料驅動拓撲與地圖配置契約  
> **核心定位**：定義純單機第一人稱 2D 矩陣迷宮地牢（10x10 等規格）、暗雷遇敵率、外神偽地殼地貌圖例（Legend）與事件互動契約。

---

## 📌 一、核心設計原則

1. **嚴格矩陣拓撲**：地牢由 `width x height`（預設 10x10）字元二維陣列構成，以 `[y][x]` 索引（原點 `[0,0]` 位於西北角）。
2. **世界觀符號化**：圖例全面對齊 Level 1《世界觀聖經》，地貌涵蓋黑色煞石礦脈、外神卵鞘組織黏膜與壞死神經管道。
3. **動態探索封裝**：玩家即時狀態（探索迷霧 `visited`、座標 `x, y`、朝向 `facing`）由後端 `DungeonPosition` 獨立管理，不污染地圖模板。

---

## 📐 二、地牢 JSON 結構定義 (Schema)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "DrpgDungeonTemplate",
  "type": "object",
  "required": ["id", "zoneId", "floor", "name", "width", "height", "layout", "legend", "encounterRate", "encounterPool"],
  "properties": {
    "id": { "type": "string", "description": "地牢唯一識別碼，如 mozhu_mines_b1f" },
    "zoneId": { "type": "string", "description": "所屬大地圖區域，如 mozhu_mines" },
    "floor": { "type": "integer", "description": "樓層編號，如 -1 代表地底一層" },
    "name": { "type": "string", "description": "地牢顯示名稱" },
    "width": { "type": "integer", "default": 10 },
    "height": { "type": "integer", "default": 10 },
    "startPosition": {
      "type": "object",
      "required": ["x", "y", "facing"],
      "properties": {
        "x": { "type": "integer" },
        "y": { "type": "integer" },
        "facing": { "type": "string", "enum": ["NORTH", "EAST", "SOUTH", "WEST"] }
      }
    },
    "exitRoomId": { "type": "string", "description": "撤離時返回的安全營地設施 ID" },
    "encounterRate": { "type": "number", "minimum": 0.0, "maximum": 1.0, "description": "走動暗雷遇敵機率 (如 0.15 代表每步 15%)" },
    "encounterPool": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["mobId", "weight"],
        "properties": {
          "mobId": { "type": "string" },
          "weight": { "type": "integer" },
          "minCount": { "type": "integer", "default": 1 },
          "maxCount": { "type": "integer", "default": 3 }
        }
      }
    },
    "legend": {
      "type": "object",
      "description": "字元與地貌型態映射表",
      "additionalProperties": {
        "type": "object",
        "required": ["type", "name", "passable"],
        "properties": {
          "type": { "type": "string", "enum": ["WALL", "FLOOR", "STAIRS_UP", "STAIRS_DOWN", "CHEST", "ELITE", "BOSS", "DOOR", "EVENT"] },
          "name": { "type": "string" },
          "passable": { "type": "boolean" },
          "loreDescription": { "type": "string" }
        }
      }
    },
    "layout": {
      "type": "array",
      "items": { "type": "string" },
      "description": "地牢 2D 字元行矩陣"
    }
  }
}
```

---

## 📝 三、範例：黑煞宗地底礦脈一層 (`mozhu_mines_b1f.json`)

```json
{
  "id": "mozhu_mines_b1f",
  "zoneId": "mozhu_mines",
  "floor": -1,
  "name": "黑煞宗地底礦脈・一層廢棄採掘道",
  "width": 10,
  "height": 10,
  "startPosition": { "x": 1, "y": 8, "facing": "NORTH" },
  "exitRoomId": "mozhu_mines:slave_camp",
  "encounterRate": 0.18,
  "encounterPool": [
    { "mobId": "mozhu_mines:corrupted_miner", "weight": 60, "minCount": 1, "maxCount": 3 },
    { "mobId": "mozhu_mines:aberrant_crawler", "weight": 40, "minCount": 2, "maxCount": 4 }
  ],
  "legend": {
    "#": { "type": "WALL", "name": "黑煞煞石巖壁", "passable": false, "loreDescription": "充斥高濃度煞氣的硬化母神卵鞘偽地殼。" },
    ".": { "type": "FLOOR", "name": "泥濘碎石路", "passable": true, "loreDescription": "浸潤著退化黏液的採礦坑道。" },
    "<": { "type": "STAIRS_UP", "name": "返回奴役通鋪石梯", "passable": true, "loreDescription": "通往採掘營地安全設施的逃生通道。" },
    ">": { "type": "STAIRS_DOWN", "name": "深層礦脈滑井", "passable": true, "loreDescription": "下探外神神經管道密集區的升降索道。" },
    "$": { "type": "CHEST", "name": "千星殖民殘存補給箱", "passable": true, "loreDescription": "前世巨企科考隊遺落的加密合金保險箱。" },
    "@": { "type": "BOSS", "name": "密室畸變修士·宋天衡", "passable": false, "loreDescription": "道基遭外神真菌同化的高階修士。" }
  },
  "layout": [
    "##########",
    "#........#",
    "#.##.##..#",
    "#.#...#..#",
    "#...$....#",
    "#.#####..#",
    "#.@...#..#",
    "#.#.#.#..#",
    "#<..#...>#",
    "##########"
  ]
}
```
