package com.example.htmlmud.dungeon;

import org.springframework.stereotype.Service;
import com.example.htmlmud.dungeon.Direction;
import com.example.htmlmud.dungeon.DungeonFloor;
import com.example.htmlmud.dungeon.DungeonPosition;
import com.example.htmlmud.dungeon.DungeonTile;
import com.example.htmlmud.dungeon.GridCoord;
import com.example.htmlmud.dungeon.StepResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DungeonNavigator {

  public StepResult moveForward(DungeonFloor floor, DungeonPosition pos) {
    GridCoord target = pos.getCoord().step(pos.getFacing());
    return tryMoveTo(floor, pos, target, "向前邁步");
  }

  public StepResult moveBackward(DungeonFloor floor, DungeonPosition pos) {
    GridCoord target = pos.getCoord().step(pos.getFacing().getOpposite());
    return tryMoveTo(floor, pos, target, "向後退步");
  }

  public StepResult turnLeft(DungeonPosition pos) {
    pos.turnLeft();
    return StepResult.ok(
        "小隊轉向左側，當前面向【" + pos.getFacing().getChineseName() + "】方。",
        pos.getCoord(),
        pos.getFacing(),
        null);
  }

  public StepResult turnRight(DungeonPosition pos) {
    pos.turnRight();
    return StepResult.ok(
        "小隊轉向右側，當前面向【" + pos.getFacing().getChineseName() + "】方。",
        pos.getCoord(),
        pos.getFacing(),
        null);
  }

  private StepResult tryMoveTo(DungeonFloor floor, DungeonPosition pos, GridCoord target, String actionName) {
    if (!floor.isInBounds(target)) {
      return StepResult.blocked(actionName + "受阻！前方已是迷宮外緣禁制結界，不可通行。", pos.getCoord(), pos.getFacing());
    }

    DungeonTile tile = floor.getTile(target);
    if (!tile.isPassable()) {
      return StepResult.blocked(actionName + "受阻！前方乃是森冷陰刻的【" + tile.getName() + "】，無法穿越！", pos.getCoord(), pos.getFacing());
    }

    pos.setCoord(target);
    StringBuilder sb = new StringBuilder();
    sb.append(actionName).append("，抵達坐標 ").append(target).append("【").append(tile.getName()).append("】。\n");
    if (tile.getDescription() != null && !tile.getDescription().isBlank()) {
      sb.append(tile.getDescription());
    }

    return StepResult.ok(sb.toString(), target, pos.getFacing(), tile);
  }

  public String inspectForward(DungeonFloor floor, DungeonPosition pos) {
    if (floor == null || pos == null) {
      return "";
    }
    DungeonPosition.TorchStatus status = pos.getTorchStatus() != null ? pos.getTorchStatus() : DungeonPosition.TorchStatus.BRIGHT;
    int maxSteps = switch (status) {
      case BRIGHT -> 3;
      case DIM -> 2;
      case DARKNESS -> 1;
    };

    StringBuilder sb = new StringBuilder();
    boolean blocked = false;
    for (int step = 1; step <= maxSteps; step++) {
      GridCoord target = pos.getCoord().step(pos.getFacing(), step);
      if (!floor.isInBounds(target)) {
        if (step == 1) {
          sb.append("正前方是冰冷死寂的迷宮邊緣禁制。");
        } else {
          sb.append(String.format(" 向前延伸至 %d 步處已抵達迷宮邊緣禁制。", step));
        }
        blocked = true;
        break;
      }
      DungeonTile tile = floor.getTile(target);
      String tileName = (tile != null && tile.getName() != null && !tile.getName().isBlank())
          ? tile.getName()
          : (tile != null ? tile.getType().getDefaultName() : "未知區域");
      String symbol = tile != null ? tile.getType().getSymbol() : "?";

      if (step == 1) {
        sb.append("凝神望去，正前方 1 步處是【").append(tileName).append("】(").append(symbol).append(")。");
        if (tile != null && tile.getDescription() != null && !tile.getDescription().isBlank()) {
          sb.append(" ").append(tile.getDescription());
        }
      } else {
        sb.append(String.format(" 視線穿透火光，前方 %d 步依稀可見【%s】(%s)。", step, tileName, symbol));
      }

      if (tile != null && !tile.isPassable()) {
        blocked = true;
        break;
      }
    }

    if (status == DungeonPosition.TorchStatus.DARKNESS) {
      sb.append(" [🌑 絕對黑暗：火光熄滅，視距僅限身前一尺]");
    } else if (status == DungeonPosition.TorchStatus.DIM && !blocked) {
      sb.append(" [🕯️ 殘焰微光：照度衰減，深處隱入迷霧]");
    }

    return sb.toString().trim();
  }

  public String renderAsciiMap(DungeonFloor floor, DungeonPosition pos) {
    StringBuilder sb = new StringBuilder();
    sb.append("=== 【").append(floor.getName()).append("】 靈識感應地圖 ===\n");
    sb.append("當前位置: ").append(pos.getCoord())
        .append(" | 面向: 【").append(pos.getFacing().getChineseName()).append("】")
        .append(" | 標記: [").append(pos.getFacing().getSymbol()).append("]\n");
    sb.append("--------------------------------------------------\n");

    // 頂部 X 軸標籤
    sb.append("   ");
    for (int x = 0; x < floor.getWidth(); x++) {
      sb.append(" ").append(x).append(" ");
    }
    sb.append("\n");

    for (int y = 0; y < floor.getHeight(); y++) {
      sb.append(String.format("%2d ", y)); // Y 軸標籤
      for (int x = 0; x < floor.getWidth(); x++) {
        if (pos.getX() == x && pos.getY() == y) {
          // 當前玩家位置與朝向
          sb.append("[").append(pos.getFacing().getSymbol()).append("]");
        } else if (pos.isVisited(x, y)) {
          // 已探知格子
          DungeonTile tile = floor.getTile(x, y);
          sb.append(" ").append(tile.getType().getSymbol()).append(" ");
        } else {
          // 未探知迷霧
          sb.append(" ? ");
        }
      }
      sb.append("\n");
    }

    sb.append("--------------------------------------------------\n");
    sb.append("圖例: [^/v/</>] 小隊所在與朝向  [#] 牆壁  [.] 石廊  [+] 石門  [$] 寶藏  [>] 下層\n");
    return sb.toString();
  }
}
