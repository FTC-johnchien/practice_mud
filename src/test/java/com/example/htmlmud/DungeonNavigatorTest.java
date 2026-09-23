package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.htmlmud.domain.dungeon.model.GridDirection;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.model.GridCoord;
import com.example.htmlmud.domain.dungeon.model.StepResult;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.dungeon.service.DungeonNavigator;

class DungeonNavigatorTest {

  private DungeonNavigator navigator;
  private DungeonFloor floor;
  private DungeonPosition position;

  @BeforeEach
  void setUp() {
    navigator = new DungeonNavigator();
    DungeonManager manager = new DungeonManager();
    manager.init();
    floor = manager.getFloor("taiyin_tomb_b1f");
    assertNotNull(floor, "Floor taiyin_tomb_b1f should exist");
    position = manager.getOrCreatePosition("test_player", "taiyin_tomb_b1f");
  }

  @Test
  @DisplayName("測試 10x10 地牢起始位置與朝向")
  void testInitialPosition() {
    assertEquals(10, floor.getWidth());
    assertEquals(10, floor.getHeight());
    assertEquals(new GridCoord(1, 8), position.getCoord());
    assertEquals(GridDirection.NORTH, position.getFacing());
    assertTrue(position.isVisited(1, 8), "起始位置應已探勘解鎖迷霧");
  }

  @Test
  @DisplayName("測試向前移動至合法廊道並解鎖迷霧")
  void testMoveForward() {
    // 從 (1, 8) 面向 NORTH，向前一步應抵達 (1, 7)
    StepResult result = navigator.moveForward(floor, position);
    assertTrue(result.success());
    assertEquals(new GridCoord(1, 7), position.getCoord());
    assertEquals(GridDirection.NORTH, position.getFacing());
    assertTrue(position.isVisited(1, 7), "新抵達坐標應已解鎖");
  }

  @Test
  @DisplayName("測試撞牆判定（撞擊邊緣或石壁不可通行）")
  void testCollisionWithWall() {
    // 從 (1, 8) 轉向 WEST 面向牆壁 (x=0 為青岡石壁)
    navigator.turnLeft(position);
    assertEquals(GridDirection.WEST, position.getFacing());

    StepResult result = navigator.moveForward(floor, position);
    assertFalse(result.success(), "面向牆壁應不可通行");
    assertTrue(result.message().contains("受阻"), "應顯示受阻訊息");
    assertEquals(new GridCoord(1, 8), position.getCoord(), "坐標應保持不變");
  }

  @Test
  @DisplayName("測試 360 度左轉與右轉朝向變更")
  void testRotation() {
    assertEquals(GridDirection.NORTH, position.getFacing());

    // 左轉順序: NORTH -> WEST -> SOUTH -> EAST -> NORTH
    assertEquals(GridDirection.WEST, navigator.turnLeft(position).facing());
    assertEquals(GridDirection.SOUTH, navigator.turnLeft(position).facing());
    assertEquals(GridDirection.EAST, navigator.turnLeft(position).facing());
    assertEquals(GridDirection.NORTH, navigator.turnLeft(position).facing());

    // 右轉順序: NORTH -> EAST -> SOUTH -> WEST -> NORTH
    assertEquals(GridDirection.EAST, navigator.turnRight(position).facing());
    assertEquals(GridDirection.SOUTH, navigator.turnRight(position).facing());
    assertEquals(GridDirection.WEST, navigator.turnRight(position).facing());
    assertEquals(GridDirection.NORTH, navigator.turnRight(position).facing());
  }

  @Test
  @DisplayName("測試 ASCII 地圖渲染器與圖例")
  void testRenderAsciiMap() {
    String mapStr = navigator.renderAsciiMap(floor, position);
    assertNotNull(mapStr);
    assertTrue(mapStr.contains("太陰古塚一層 (B1F)"));
    assertTrue(mapStr.contains("[^]"), "應包含玩家朝北符號 [^]");
    assertTrue(mapStr.contains("圖例"));
  }
}
