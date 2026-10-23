package com.example.htmlmud.dungeon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import com.example.htmlmud.dungeon.GridDirection;
import com.example.htmlmud.dungeon.DungeonFloor;
import com.example.htmlmud.dungeon.DungeonPosition;
import com.example.htmlmud.dungeon.DungeonTile;
import com.example.htmlmud.dungeon.GridCoord;
import com.example.htmlmud.template.DungeonValidatorPort;
import com.example.htmlmud.template.TemplateReader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DungeonManager implements DungeonValidatorPort {

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

  @Override
  public int validateDungeons(TemplateReader templateReader, List<String> errors, List<String> warnings) {
    List<String> floorIds = getAllFloorIds();
    for (String floorId : floorIds) {
      DungeonFloor floor = getFloor(floorId);
      if (floor == null) continue;
      if (floor.getStartCoord() == null) {
        errors.add("地牢樓層 [" + floorId + "] 未設定起點坐標 (startCoord)！");
      } else {
        if (!floor.isInBounds(floor.getStartCoord())) {
          errors.add("地牢樓層 [" + floorId + "] 起點坐標 (" + floor.getStartCoord().x() + ", "
              + floor.getStartCoord().y() + ") 超出邊界！");
        } else if (!floor.getTile(floor.getStartCoord()).isPassable()) {
          errors.add("地牢樓層 [" + floorId + "] 起點坐標 (" + floor.getStartCoord().x() + ", "
              + floor.getStartCoord().y() + ") 位於不可通行之障礙牆體！");
        }
      }
      if (floor.getExitRoomId() != null && !floor.getExitRoomId().isBlank()) {
        if (templateReader.findRoom(floor.getExitRoomId()).isEmpty()) {
          errors.add("地牢樓層 [" + floorId + "] 撤退出口房間 [" + floor.getExitRoomId() + "] 不存在於房間庫！");
        }
      }
      if (floor.getMobPool() != null) {
        for (String mobId : floor.getMobPool()) {
          if (mobId != null && !mobId.isBlank()) {
            if (templateReader.findMob(mobId).isEmpty()) {
              errors.add("地牢樓層 [" + floorId + "] 怪物池怪物 [" + mobId + "] 不存在於怪物庫！");
            }
          }
        }
      }

      // 深度盤查樓層所有地塊之掉落物、首領與特殊事件怪物 (GUARD-01)
      for (int y = 0; y < floor.getHeight(); y++) {
        for (int x = 0; x < floor.getWidth(); x++) {
          DungeonTile tile = floor.getTile(x, y);
          if (tile == null) continue;
          if (tile.getDrops() != null) {
            for (String dropId : tile.getDrops()) {
              if (dropId != null && !dropId.isBlank()) {
                if (templateReader.findItem(dropId).isEmpty()) {
                  errors.add("地牢樓層 [" + floorId + "] 坐標 (" + x + ", " + y + ") 掉落物 [" + dropId + "] 不存在於物品庫！");
                }
              }
            }
          }
          if (tile.getType() == DungeonTile.TileType.BOSS) {
            String bossId = tile.getEventId();
            if (bossId == null || bossId.isBlank()) {
              errors.add("地牢樓層 [" + floorId + "] 首領地塊 (" + x + ", " + y + ") 未配置 eventId (首領怪物 ID)！");
            } else if (templateReader.findMob(bossId).isEmpty()) {
              errors.add("地牢樓層 [" + floorId + "] 首領地塊 (" + x + ", " + y + ") 指定首領怪物 [" + bossId + "] 不存在於怪物庫！");
            }
          } else if (tile.getEventId() != null && !tile.getEventId().isBlank()) {
            if (tile.getEventId().contains("boss_") || tile.getEventId().contains(":mob_") || tile.getEventId().contains("corrupted_")) {
              if (templateReader.findMob(tile.getEventId()).isEmpty()) {
                errors.add("地牢樓層 [" + floorId + "] 事件地塊 (" + x + ", " + y + ") 指定怪物 [" + tile.getEventId() + "] 不存在於怪物庫！");
              }
            }
          }
        }
      }
    }
    return floorIds.size();
  }
}
