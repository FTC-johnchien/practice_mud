package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.htmlmud.domain.dungeon.model.Direction;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonTile;
import com.example.htmlmud.domain.dungeon.service.DungeonFloorLoader;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
class DataDrivenDungeonTest {

  @Test
  @DisplayName("驗證 Data-Driven: taiyin_tomb_b1f.json 正確解析並建構 10x10 地牢網格")
  void testParseFloorJson() throws Exception {
    DungeonFloorLoader loader = new DungeonFloorLoader(new ObjectMapper());
    try (InputStream is = getClass().getResourceAsStream("/data/dungeons/taiyin_tomb_b1f.json")) {
      assertNotNull(is, "taiyin_tomb_b1f.json 必須存在於 classpath");
      DungeonFloor floor = loader.parseFloor(is);

      assertNotNull(floor);
      assertThat(floor.getId()).isEqualTo("taiyin_tomb_b1f");
      assertThat(floor.getName()).contains("太陰古塚一層");
      assertThat(floor.getWidth()).isEqualTo(10);
      assertThat(floor.getHeight()).isEqualTo(10);
      assertThat(floor.getStartCoord().x()).isEqualTo(1);
      assertThat(floor.getStartCoord().y()).isEqualTo(8);
      assertThat(floor.getStartFacing()).isEqualTo(Direction.NORTH);

      // (1, 8) 應為回地表石階 STAIRS_UP
      DungeonTile stairsUp = floor.getTile(1, 8);
      assertThat(stairsUp.getType()).isEqualTo(DungeonTile.TileType.STAIRS_UP);
      assertThat(stairsUp.getName()).contains("青雲古階");

      // (8, 1) 應為深淵枯井 STAIRS_DOWN
      DungeonTile stairsDown = floor.getTile(8, 1);
      assertThat(stairsDown.getType()).isEqualTo(DungeonTile.TileType.STAIRS_DOWN);
      assertThat(stairsDown.getName()).contains("通往 B2F 深淵枯井");

      // (4, 4) 應為神秘古碑 EVENT
      DungeonTile eventTile = floor.getTile(4, 4);
      assertThat(eventTile.getType()).isEqualTo(DungeonTile.TileType.EVENT);
      assertThat(eventTile.getName()).contains("太陰古碑");

      // (3, 5) 應為古仙棺槨 TREASURE，且包含 drops
      DungeonTile treasure = floor.getTile(3, 5);
      assertThat(treasure.getType()).isEqualTo(DungeonTile.TileType.TREASURE);
      assertThat(treasure.getName()).contains("古修遺蛻棺槨");
      assertThat(treasure.getDrops()).contains("taiyin_pill", "bronze_sword");

      // (4, 7) 應為黏液陷阱 TRAP
      DungeonTile trap = floor.getTile(4, 7);
      assertThat(trap.getType()).isEqualTo(DungeonTile.TileType.TRAP);
      assertThat(trap.getName()).contains("深淵黏液陷阱");

      // 遭遇配置驗證
      assertThat(floor.getDangerRate()).isEqualTo(15);
      assertThat(floor.getMobPool()).contains("taiyin_tomb:corpse_doll", "taiyin_tomb:bone_bat");
    }
  }

  @Test
  @DisplayName("驗證 DungeonManager 從 JSON 動態載入並提供樓層")
  void testDungeonManagerLoadsDataDrivenFloors() {
    DungeonManager manager = new DungeonManager();
    manager.init();

    DungeonFloor floor = manager.getFloor("taiyin_tomb_b1f");
    assertNotNull(floor, "Manager 應成功自 JSON 載入 taiyin_tomb_b1f");
    assertThat(floor.getName()).contains("太陰古塚一層");
    assertThat(floor.getWidth()).isEqualTo(10);
    assertThat(floor.getHeight()).isEqualTo(10);
  }
}
