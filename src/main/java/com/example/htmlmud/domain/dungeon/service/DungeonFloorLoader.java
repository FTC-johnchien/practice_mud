package com.example.htmlmud.domain.dungeon.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.dungeon.model.GridDirection;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonTile;
import com.example.htmlmud.domain.dungeon.model.GridCoord;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DungeonFloorLoader {

  private final ObjectMapper objectMapper;
  private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class DungeonFloorJson {
    private String floorId;
    private String name;
    private String description;
    private int width;
    private int height;
    private StartCoordJson startCoord;
    private String startFacing;
    private List<String> layout;
    private Map<String, LegendItemJson> legend = new HashMap<>();
    private List<TileOverrideJson> tileOverrides = new ArrayList<>();
    private EncounterJson encounters;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class StartCoordJson {
    private int x;
    private int y;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class LegendItemJson {
    private String type;
    private String name;
    private String description;
    private Boolean passable;
    private String eventId;
    private List<String> drops;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TileOverrideJson {
    private int x;
    private int y;
    private String type;
    private String name;
    private String description;
    private Boolean passable;
    private String eventId;
    private List<String> drops;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class EncounterJson {
    private int dangerRate = 15;
    private List<String> mobs = new ArrayList<>();
  }

  public List<DungeonFloor> loadAllFloors() {
    List<DungeonFloor> floors = new ArrayList<>();
    try {
      Resource[] resources = resourceResolver.getResources("classpath*:data/dungeons/*.json");
      for (Resource res : resources) {
        try (InputStream is = res.getInputStream()) {
          DungeonFloor floor = parseFloor(is);
          if (floor != null) {
            floors.add(floor);
            log.info("Loaded dungeon floor from {}: {} [{}]", res.getFilename(), floor.getName(), floor.getId());
          }
        } catch (Exception e) {
          log.error("Failed to load dungeon floor resource: {}", res.getFilename(), e);
        }
      }
    } catch (Exception e) {
      log.error("Failed to scan dungeon JSON resources", e);
    }
    return floors;
  }

  public DungeonFloor parseFloor(InputStream is) throws Exception {
    DungeonFloorJson json = objectMapper.readValue(is, DungeonFloorJson.class);
    int width = json.getWidth();
    int height = json.getHeight();
    DungeonTile[][] tiles = new DungeonTile[height][width];

    // Default to wall
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        tiles[y][x] = DungeonTile.wall();
      }
    }

    // Process layout matrix
    if (json.getLayout() != null) {
      for (int y = 0; y < Math.min(height, json.getLayout().size()); y++) {
        String row = json.getLayout().get(y);
        for (int x = 0; x < Math.min(width, row.length()); x++) {
          String symbol = String.valueOf(row.charAt(x));
          LegendItemJson legendItem = json.getLegend().get(symbol);
          if (legendItem != null) {
            DungeonTile.TileType type;
            try {
              type = DungeonTile.TileType.valueOf(legendItem.getType().toUpperCase());
            } catch (Exception e) {
              type = DungeonTile.TileType.FLOOR;
            }
            boolean passable = legendItem.getPassable() != null ? legendItem.getPassable() : type.isDefaultPassable();
            DungeonTile tile = DungeonTile.builder()
                .type(type)
                .name(legendItem.getName() != null ? legendItem.getName() : type.getDefaultName())
                .description(legendItem.getDescription() != null ? legendItem.getDescription() : "")
                .passable(passable)
                .eventId(legendItem.getEventId())
                .drops(legendItem.getDrops() != null ? new ArrayList<>(legendItem.getDrops()) : new ArrayList<>())
                .build();
            tiles[y][x] = tile;
          } else {
            // Fallback based on common characters
            if ("#".equals(symbol)) {
              tiles[y][x] = DungeonTile.wall();
            } else {
              tiles[y][x] = DungeonTile.floor(null);
            }
          }
        }
      }
    }

    // Process tile overrides if any
    if (json.getTileOverrides() != null) {
      for (TileOverrideJson ov : json.getTileOverrides()) {
        if (ov.getY() >= 0 && ov.getY() < height && ov.getX() >= 0 && ov.getX() < width) {
          DungeonTile.TileType type = DungeonTile.TileType.FLOOR;
          if (ov.getType() != null) {
            try {
              type = DungeonTile.TileType.valueOf(ov.getType().toUpperCase());
            } catch (Exception ignored) {}
          }
          boolean passable = ov.getPassable() != null ? ov.getPassable() : type.isDefaultPassable();
          DungeonTile tile = DungeonTile.builder()
              .type(type)
              .name(ov.getName() != null ? ov.getName() : type.getDefaultName())
              .description(ov.getDescription() != null ? ov.getDescription() : "")
              .passable(passable)
              .eventId(ov.getEventId())
              .drops(ov.getDrops() != null ? new ArrayList<>(ov.getDrops()) : new ArrayList<>())
              .build();
          tiles[ov.getY()][ov.getX()] = tile;
        }
      }
    }

    GridDirection facing = GridDirection.NORTH;
    if (json.getStartFacing() != null) {
      try {
        facing = GridDirection.valueOf(json.getStartFacing().toUpperCase());
      } catch (Exception ignored) {}
    }

    int startX = json.getStartCoord() != null ? json.getStartCoord().getX() : 1;
    int startY = json.getStartCoord() != null ? json.getStartCoord().getY() : 1;

    int dangerRate = 15;
    List<String> mobs = new ArrayList<>();
    if (json.getEncounters() != null) {
      dangerRate = json.getEncounters().getDangerRate();
      if (json.getEncounters().getMobs() != null) {
        mobs = new ArrayList<>(json.getEncounters().getMobs());
      }
    }

    return DungeonFloor.builder()
        .id(json.getFloorId())
        .name(json.getName())
        .description(json.getDescription())
        .width(width)
        .height(height)
        .tiles(tiles)
        .startCoord(new GridCoord(startX, startY))
        .startFacing(facing)
        .dangerRate(dangerRate)
        .mobPool(mobs)
        .build();
  }
}
