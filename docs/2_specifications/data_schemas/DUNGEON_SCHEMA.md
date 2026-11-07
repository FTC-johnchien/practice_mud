# 🗺️ DRPG 迷宮地牢資料架構規格 (Dungeon Schema Specification)

> **文檔層級**：Level 2 — Module Specifications (data_schemas)  
> **系統類型**：資料驅動拓撲與地圖配置契約  
> **核心定位**：定義純單機第一人稱 2D 矩陣迷宮地牢（10x10 等規格）、暗雷遇敵率、外神偽地殼地貌圖例（Legend）與事件互動契約。

---

## 📌 一、核心設計原則

1. **嚴格矩陣拓撲**：地牢由 `width x height`（預設 10x10）字元二維陣列構成，以 `[y][x]` 索引（原點 `[0,0]` 位於西北角）。
2. **世界觀符號化**：圖例全面對齊 Level 1《世界觀聖經》，地貌涵蓋黑色煞石礦脈、外神卵鞘組織黏膜與壞死神經管道。
3. **動態探索封裝**：玩家即時狀態（探索迷霧 `visited`、座標 `x, y`、朝向 `facing`）由後端 `DungeonPosition` 獨立管理，不污染地圖模板。
4. **地塊互動判定**：現行機關、靈泉與寶箱互動由 `DungeonCommand.java` 依據 `TileType`（`SPRING`, `TRAP`, `DOOR`, `TREASURE`, `EVENT`, `BOSS`）與名稱敘事即時觸發與判定。

---

## 📐 二、地牢 JSON 結構定義 (DungeonFloorJson)

> 💡 **契約對齊**：對齊 `DungeonFloorLoader.java` 與 `DungeonTile.TileType` 真實 10 大地貌枚舉。

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "DungeonFloorJson",
  "type": "object",
  "required": ["floorId", "name", "width", "height", "startCoord", "startFacing", "exitRoomId", "layout", "legend", "encounters"],
  "properties": {
    "floorId": { "type": "string", "description": "地牢樓層唯一識別碼，如 mozhu_mines_b1f, taiyin_tomb_b1f" },
    "name": { "type": "string", "description": "地牢顯示名稱" },
    "description": { "type": "string", "description": "樓層世界觀背景描述（相容 loreDescription）" },
    "width": { "type": "integer", "default": 10 },
    "height": { "type": "integer", "default": 10 },
    "startCoord": {
      "type": "object",
      "required": ["x", "y"],
      "description": "初始出生座標",
      "properties": {
        "x": { "type": "integer" },
        "y": { "type": "integer" }
      }
    },
    "startFacing": { "type": "string", "enum": ["NORTH", "EAST", "SOUTH", "WEST"], "default": "NORTH" },
    "exitRoomId": { "type": "string", "description": "撤離時返回的安全營地設施 ID，如 mozhu_mines:mine_entrance" },
    "encounters": {
      "type": "object",
      "required": ["dangerRate", "mobs"],
      "description": "遇敵規則配置",
      "properties": {
        "dangerRate": { "type": "integer", "description": "遇敵危險度百分比 (例如 15 代表 15%)" },
        "mobs": {
          "type": "array",
          "items": { "type": "string" },
          "description": "可能遭遇的怪物 Template ID 清單"
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
          "type": {
            "type": "string",
            "enum": ["WALL", "FLOOR", "DOOR", "TREASURE", "TRAP", "SPRING", "STAIRS_DOWN", "STAIRS_UP", "EVENT", "BOSS"],
            "description": "對齊 DungeonTile.TileType 真實 10 項地貌枚舉"
          },
          "name": { "type": "string" },
          "description": { "type": "string", "description": "地貌描述（相容 loreDescription）" },
          "passable": { "type": "boolean" },
          "eventId": { "type": "string", "description": "互動事件或首領事件 ID" },
          "drops": {
            "type": "array",
            "items": { "type": "string" },
            "description": "寶箱/採集點固定或機率掉落物 ID 清單"
          }
        }
      }
    },
    "tileOverrides": {
      "type": "array",
      "description": "指定座標之地貌屬性覆蓋清單",
      "items": {
        "type": "object",
        "required": ["x", "y", "type"],
        "properties": {
          "x": { "type": "integer" },
          "y": { "type": "integer" },
          "type": { "type": "string", "enum": ["WALL", "FLOOR", "DOOR", "TREASURE", "TRAP", "SPRING", "STAIRS_DOWN", "STAIRS_UP", "EVENT", "BOSS"] },
          "name": { "type": "string" },
          "description": { "type": "string" },
          "passable": { "type": "boolean" },
          "eventId": { "type": "string" },
          "drops": { "type": "array", "items": { "type": "string" } }
        }
      }
    },
    "layout": {
      "type": "array",
      "items": { "type": "string" },
      "description": "地牢 2D 字元行矩陣（每行長度需等於 width，行數需等於 height）"
    }
  }
}
```

---

## 📝 三、範例：黑煞宗地底礦脈一層 (`mozhu_mines_b1f.json`)

```json
{
  "floorId": "mozhu_mines_b1f",
  "name": "墨竹山廢棄靈石礦坑一層 (B1F)",
  "description": "墨竹山外圍荒廢已久的地下礦坑，岩壁嵌滿了煞氣靈石晶簇與蠕動菌絲。深處傳來畸變大師兄癲狂的咆哮與齒骨磨擦聲。",
  "width": 10,
  "height": 10,
  "startCoord": {
    "x": 1,
    "y": 8
  },
  "startFacing": "NORTH",
  "exitRoomId": "mozhu_mines:mine_entrance",
  "layout": [
    "##########",
    "#...B...S#",
    "#.#####..#",
    "#.#...#..#",
    "#.#.E.#.D#",
    "#.###.####",
    "#...#.#.T#",
    "#.X.#.##.#",
    "#<..#....#",
    "##########"
  ],
  "legend": {
    "#": {
      "type": "WALL",
      "name": "黑煞煞石岩壁",
      "description": "充斥高濃度煞氣的硬化母神卵鞘偽地殼，岩壁縫隙嵌滿了暗紫色異化晶簇，不時滴淌冰涼濁水。",
      "passable": false
    },
    ".": {
      "type": "FLOOR",
      "name": "腐朽枕木坑道",
      "description": "浸潤著外星生化腐殖與退化黏液的採礦坑道，腐朽發黑的枕木上生長著詭異的微光苔蘚與乾涸血跡。",
      "passable": true
    },
    "D": {
      "type": "DOOR",
      "name": "加固避煞木門",
      "description": "採礦雜役用礦坑廢料加固的粗糙木門，門楣貼著一張褪色的避煞黃符。",
      "passable": true
    },
    "T": {
      "type": "TREASURE",
      "name": "雜役遺留木箱",
      "description": "半掩在外神分泌物碎屑與碎石堆中的舊木箱，銅鎖已鏽蝕碎裂。",
      "passable": true,
      "drops": [
        "mozhu_mines:corrupted_spirit_stone",
        "mozhu_mines:miner_pickaxe",
        "newbie_village:village_bread"
      ]
    },
    "E": {
      "type": "EVENT",
      "name": "墨家十戒非攻殘碑",
      "description": "斷裂的墨家古碑，碑文隱現「非攻、節用、兼愛」道紋，在此打坐可使道心稍獲撫平。",
      "passable": true
    },
    "X": {
      "type": "TRAP",
      "name": "外神卵塊菌絲泥沼",
      "description": "地面覆蓋著一層外神卵塊與粉紅色的血肉質菌絲泥沼，踏入將遭受煞氣侵蝕心神！",
      "passable": true
    },
    "<": {
      "type": "STAIRS_UP",
      "name": "腐朽深坑斜道(回地面)",
      "description": "通往墨竹山採掘營地安全設施的向上逃生通道，生路所在。",
      "passable": true
    },
    "B": {
      "type": "BOSS",
      "name": "虛月血肉祭壇",
      "description": "地下圓形石殿中央聳立著遭外神真菌同化的血肉白骨祭壇，畸變大師兄·宋天衡正背對著你散發令人窒息的煞氣！",
      "passable": false,
      "eventId": "mozhu_mines:boss_song_tianheng"
    },
    "S": {
      "type": "STAIRS_DOWN",
      "name": "深層礦脈滑井",
      "description": "下探外神神經管道密集區的古老通風豎井，陣道微風向上吹拂。",
      "passable": true
    }
  },
  "encounters": {
    "dangerRate": 15,
    "mobs": [
      "mozhu_mines:corrupted_servant",
      "mozhu_mines:shao_crystal_beast"
    ]
  }
}
```

---

## 🔮 四、長線演進藍圖：結構化機關檢定與擴充圖例【待深入討論／長線演進藍圖】

> ⚠️ **架構邊界備忘**：
> 1. 現行 `DungeonFloorLoader.java` 與 `DungeonTile.java` 以 10 大核心地貌枚舉（`WALL`, `FLOOR`, `DOOR`, `TREASURE`, `TRAP`, `SPRING`, `STAIRS_DOWN`, `STAIRS_UP`, `EVENT`, `BOSS`）為準。
> 2. 樓層級擴充別名（`zoneId`, `startPosition`, `encounterRate`, `encounterPool`）、擴充圖例枚舉（`CHEST`, `ELITE`）以及結構化五維屬性檢定欄位（`checkType`: `DEX`, `STR`, `TOOL` 及 `checkDc`）列為長線演進藍圖。

```json
{
  "zoneId": "zone_mozhu_mountain",
  "startPosition": { "x": 1, "y": 8 },
  "encounterRate": 15,
  "encounterPool": ["mozhu_mines:corrupted_servant", "mozhu_mines:shao_crystal_beast"],
  "legend": {
    "C": {
      "type": "CHEST",
      "name": "符咒封印秘寶箱",
      "passable": true,
      "checkType": "TOOL",
      "checkDc": 14,
      "drops": ["quest:ancient_jade_key"]
    },
    "M": {
      "type": "ELITE",
      "name": "外神異化巡查使",
      "passable": false,
      "eventId": "event:elite_patrol"
    }
  }
}
```
