package com.example.htmlmud.domain.dungeon.service;

import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.dungeon.model.Direction;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.model.DungeonTile;
import com.example.htmlmud.domain.dungeon.model.GridCoord;
import com.example.htmlmud.domain.dungeon.model.StepResult;
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
    GridCoord front = pos.getCoord().step(pos.getFacing());
    if (!floor.isInBounds(front)) {
      return "正前方是冰冷死寂的迷宮邊緣禁制。";
    }
    DungeonTile tile = floor.getTile(front);
    return "凝神望去，正前方 1 步處是【" + tile.getName() + "】(" + tile.getType().getSymbol() + ")。"
        + (tile.getDescription() != null ? " " + tile.getDescription() : "");
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
