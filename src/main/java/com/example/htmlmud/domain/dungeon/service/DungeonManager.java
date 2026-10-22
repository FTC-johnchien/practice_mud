package com.example.htmlmud.domain.dungeon.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.dungeon.model.GridDirection;
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
      log.error("No dungeon floors found in JSON data/dungeons/*.json!");
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

  public void clearPositions() {
    playerPositions.clear();
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
        floor = floorRegistry.get("taiyin_tomb_b1f");
      }
      if (floor == null) {
        floor = floorRegistry.values().iterator().next();
      }
    }
    if (floor == null) {
      throw new IllegalStateException("無法初始化地牢坐標，查無任何可用地牢資料 (floorId=" + floorId + ")");
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
}
