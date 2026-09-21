package com.example.htmlmud.domain.dungeon.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.dungeon.model.Direction;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.model.DungeonTile;
import com.example.htmlmud.domain.dungeon.model.GridCoord;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DungeonManager {

  private final DungeonFloorLoader dungeonFloorLoader;
  private final Map<String, DungeonFloor> floorRegistry = new HashMap<>();
  private final Map<String, DungeonPosition> playerPositions = new ConcurrentHashMap<>();

  public DungeonManager() {
    this(new DungeonFloorLoader(new com.fasterxml.jackson.databind.ObjectMapper()));
  }

  public DungeonManager(DungeonFloorLoader loader) {
    this.dungeonFloorLoader = loader != null ? loader : new DungeonFloorLoader(new com.fasterxml.jackson.databind.ObjectMapper());
  }

  @PostConstruct
  public void init() {
    List<DungeonFloor> floors = dungeonFloorLoader.loadAllFloors();
    for (DungeonFloor floor : floors) {
      floorRegistry.put(floor.getId(), floor);
      log.info("Registered Data-Driven DRPG dungeon floor: {} [{}]", floor.getName(), floor.getId());
    }
    if (floorRegistry.isEmpty()) {
      DungeonFloor b1f = buildTombB1F();
      floorRegistry.put(b1f.getId(), b1f);
      log.warn("No dungeon floors found in JSON, used fallback floor: {}", b1f.getName());
    }
  }

  public DungeonFloor getFloor(String id) {
    return floorRegistry.get(id);
  }

  public List<String> getAllFloorIds() {
    return new ArrayList<>(floorRegistry.keySet());
  }

  public void setPlayerPosition(com.example.htmlmud.domain.model.vo.CharacterId characterId, DungeonPosition pos) {
    if (characterId != null) {
      setPlayerPosition(characterId.value(), pos);
    }
  }

  public void setPlayerPosition(String playerId, DungeonPosition pos) {
    if (playerId != null && pos != null) {
      playerPositions.put(playerId, pos);
    }
  }

  public DungeonPosition getOrCreatePosition(com.example.htmlmud.domain.model.vo.CharacterId characterId, String floorId) {
    return characterId != null ? getOrCreatePosition(characterId.value(), floorId) : getOrCreatePosition("default", floorId);
  }

  public DungeonPosition getOrCreatePosition(String playerId, String floorId) {
    DungeonPosition pos = playerPositions.get(playerId);
    if (pos != null && floorId != null && !floorId.isBlank() && !floorId.equals(pos.getFloorId())) {
      return switchFloor(playerId, floorId);
    }
    return playerPositions.computeIfAbsent(playerId, id -> createInitialPosition(floorId));
  }

  public DungeonPosition getPlayerPosition(com.example.htmlmud.domain.model.vo.CharacterId characterId) {
    return characterId != null ? getPlayerPosition(characterId.value()) : getPlayerPosition("default");
  }

  public DungeonPosition getPlayerPosition(com.example.htmlmud.domain.actor.impl.Player player) {
    if (player == null) return getPlayerPosition("default");
    DungeonPosition pos = playerPositions.get(player.getId());
    if (pos == null && player.getName() != null) {
      pos = playerPositions.get(player.getName());
    }
    if (pos == null) {
      pos = getOrCreatePosition(player.getCharacterId().value(), null);
    }
    return pos;
  }

  public DungeonPosition getPlayerPosition(String playerId) {
    return getOrCreatePosition(playerId, null);
  }

  public DungeonPosition switchFloor(com.example.htmlmud.domain.model.vo.CharacterId characterId, String targetFloorId) {
    return characterId != null ? switchFloor(characterId.value(), targetFloorId) : switchFloor("default", targetFloorId);
  }

  public DungeonPosition switchFloor(String playerId, String targetFloorId) {
    DungeonPosition newPos = createInitialPosition(targetFloorId);
    playerPositions.put(playerId, newPos);
    return newPos;
  }

  private DungeonPosition createInitialPosition(String floorId) {
    DungeonFloor floor = getFloor(floorId);
    if (floor == null && !floorRegistry.isEmpty()) {
      if (floorId != null) {
        floor = floorRegistry.get(floorId);
      }
      if (floor == null) {
        floor = floorRegistry.get("mozhu_mines_b1f");
      }
      if (floor == null) {
        floor = floorRegistry.values().iterator().next();
      }
    }
    if (floor == null) {
      floor = buildTombB1F();
    }
    return new DungeonPosition(
        floor.getId(),
        floor.getStartCoord().x(),
        floor.getStartCoord().y(),
        floor.getStartFacing(),
        floor.getWidth(),
        floor.getHeight()
    );
  }

  private DungeonFloor buildTombB1F() {
    int w = 10;
    int h = 10;
    DungeonTile[][] tiles = new DungeonTile[h][w];

    // 1. 全部預設為牆壁
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        tiles[y][x] = DungeonTile.wall();
      }
    }

    // 2. 雕刻 10x10 迷宮迴廊與房間
    // 西側縱向走廊 (x=1, y=1..8)
    for (int y = 1; y <= 8; y++) {
      tiles[y][1] = DungeonTile.floor("青苔斑駁的石質古道，兩側隱隱有幽綠磷火浮動。");
    }

    // 入口與上樓古階 (1, 8)
    tiles[8][1] = DungeonTile.of(DungeonTile.TileType.STAIRS_UP, "青雲古階(回地表)", "通往青雲觀後山偏殿的向上石階，生路所在。");

    // 石門 (1, 5) 與 藏寶偏室 (2, 5), (3, 5)
    tiles[5][1] = DungeonTile.of(DungeonTile.TileType.DOOR, "蝕靈石門", "厚重的青岡石門，門環雕刻著張著巨口的無名神魔。");
    tiles[5][2] = DungeonTile.floor("密室廊道，腳下有乾涸已久的暗黑血跡。");
    tiles[5][3] = DungeonTile.of(DungeonTile.TileType.TREASURE, "古修遺蛻棺槨", "一具被不可名狀紫黑觸鬚纏繞的古仙棺槨，散發陣陣微弱靈光。");

    // 南側橫向通道 (y=8, x=1..4)
    for (int x = 1; x <= 4; x++) {
      tiles[8][x] = DungeonTile.floor("幽深的甬道，滴水聲在寂靜中迴響。");
    }

    // 陷阱：深淵黏液 (4, 7)
    tiles[7][4] = DungeonTile.of(DungeonTile.TileType.TRAP, "深淵黏液陷阱", "地面滲出發出低語呢喃的詭異綠色黏液，踩上將侵蝕心神！");

    // 中央橫向大廳 (y=4, x=1..8)
    for (int x = 1; x <= 8; x++) {
      tiles[4][x] = DungeonTile.floor("寬闊的大殿廢墟，石柱上雕滿了不可言說的群星軌跡。");
    }

    // 神秘古碑 (4, 4)
    tiles[4][4] = DungeonTile.of(DungeonTile.TileType.EVENT, "殘破太陰古碑", "石碑上以古修真言記載著《四象辟邪陣》與域外星蝕的秘密。");

    // 東側縱向廊道 (x=8, y=1..4)
    for (int y = 1; y <= 4; y++) {
      tiles[y][8] = DungeonTile.floor("寒氣逼人的北側死寂走道。");
    }

    // 北側橫向石道 (y=1, x=1..8)
    for (int x = 1; x <= 8; x++) {
      tiles[1][x] = DungeonTile.floor("通往古塚最深處的筆直廊道，空氣寒冷刺骨。");
    }

    // 通往深淵 B2F 的深井古階 (8, 1)
    tiles[1][8] = DungeonTile.of(DungeonTile.TileType.STAIRS_DOWN, "通往 B2F 深淵枯井", "深不見底的向下幽穴，下層傳來如巨獸呼吸般的震顫聲。");

    return DungeonFloor.builder()
        .id("taiyin_tomb_b1f")
        .name("太陰古塚一層 (B1F)")
        .description("青雲觀後山封印的萬劫古塚，傳說埋葬著初窺天外不可名狀星神而發狂的古仙。")
        .width(w)
        .height(h)
        .tiles(tiles)
        .startCoord(new GridCoord(1, 8)) // 從 (1, 8) 出發
        .startFacing(Direction.NORTH)    // 面向北方
        .build();
  }
}
