package com.example.htmlmud.domain.dungeon.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DungeonTile {

  public enum TileType {
    WALL("#", "青岡石壁", false),
    FLOOR(".", "幽暗石廊", true),
    DOOR("+", "封魔石門", true),
    TREASURE("$", "古修遺骸", true),
    TRAP("~", "深淵蝕骨黏液", true),
    STAIRS_DOWN(">", "下行幽邃古階", true),
    STAIRS_UP("<", "上行生路石階", true),
    EVENT("!", "神秘古仙石碑", true);

    private final String symbol;
    private final String defaultName;
    private final boolean defaultPassable;

    TileType(String symbol, String defaultName, boolean defaultPassable) {
      this.symbol = symbol;
      this.defaultName = defaultName;
      this.defaultPassable = defaultPassable;
    }

    public String getSymbol() {
      return symbol;
    }

    public String getDefaultName() {
      return defaultName;
    }

    public boolean isDefaultPassable() {
      return defaultPassable;
    }
  }

  private TileType type;
  private String name;
  private String description;
  private boolean passable;
  private String eventId;
  private boolean triggered;

  public static DungeonTile wall() {
    return DungeonTile.builder()
        .type(TileType.WALL)
        .name(TileType.WALL.getDefaultName())
        .description("堅硬森冷的青岡石壁，雕刻著不可名狀的扭曲道紋。")
        .passable(false)
        .build();
  }

  public static DungeonTile floor(String desc) {
    return DungeonTile.builder()
        .type(TileType.FLOOR)
        .name(TileType.FLOOR.getDefaultName())
        .description(desc != null ? desc : "潮濕冰冷的石板路，空氣中瀰漫著陳腐的陰煞之氣。")
        .passable(true)
        .build();
  }

  public static DungeonTile of(TileType type, String name, String desc) {
    return DungeonTile.builder()
        .type(type)
        .name(name != null ? name : type.getDefaultName())
        .description(desc != null ? desc : "")
        .passable(type.isDefaultPassable())
        .build();
  }
}
