package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.htmlmud.domain.dungeon.dto.DrpgStateDto;
import com.example.htmlmud.domain.dungeon.model.Direction;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.model.DungeonTile;
import com.example.htmlmud.domain.dungeon.model.GridCoord;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.service.PartyService;

class DrpgStateDtoTest {

  @Test
  @DisplayName("測試 DrpgStateDto 正確將地牢 10x10 雷達與 6人小隊資料打包成結構化封包")
  void testDrpgStateDtoSerialization() {
    DungeonManager dungeonManager = new DungeonManager();
    dungeonManager.init();

    PartyService partyService = new PartyService();
    partyService.initDefaultFormations();

    DungeonFloor floor = dungeonManager.getFloor("taiyin_tomb_b1f");
    assertThat(floor).isNotNull();

    DungeonPosition pos = new DungeonPosition(floor.getId(), 1, 1, Direction.NORTH, 10, 10);
    Party party = partyService.createInitialParty("道初真人");

    DrpgStateDto state = DrpgStateDto.of(floor, pos, "【正前方視野】青岡石壁 (WALL) - 無法通行", party);

    assertThat(state.type()).isEqualTo("DRPG_STATE");
    assertThat(state.dungeon()).isNotNull();
    assertThat(state.dungeon().floorId()).isEqualTo("taiyin_tomb_b1f");
    assertThat(state.dungeon().floorName()).contains("太陰古塚一層");
    assertThat(state.dungeon().x()).isEqualTo(1);
    assertThat(state.dungeon().y()).isEqualTo(1);
    assertThat(state.dungeon().direction()).isEqualTo("NORTH");
    assertThat(state.dungeon().directionArrow()).isEqualTo("▲");
    assertThat(state.dungeon().tiles().length).isEqualTo(10);
    assertThat(state.dungeon().tiles()[0].length).isEqualTo(10);
    assertThat(state.dungeon().visited()[1][1]).isTrue(); // 起始點周圍已探明

    assertThat(state.party()).isNotNull();
    assertThat(state.party().name()).isEqualTo("道初真人的問道旅團");
    assertThat(state.party().members()).hasSize(5);

    // 驗證成員 1: 道初真人 (前衛)
    var leader = state.party().members().get(0);
    assertThat(leader.name()).isEqualTo("道初真人");
    assertThat(leader.row()).isEqualTo("FRONT");
    assertThat(leader.san()).isEqualTo(100);
    assertThat(leader.sanState()).contains("道心澄澈");
    assertThat(leader.sanLevel()).isEqualTo("NORMAL");

    // 驗證成員 2: 鐵牛 (前衛)
    var iron = state.party().members().get(1);
    assertThat(iron.name()).isEqualTo("鐵牛");
    assertThat(iron.row()).isEqualTo("FRONT");

    // 驗證成員 3: 燕青 (中衛)
    var yan = state.party().members().get(2);
    assertThat(yan.name()).isEqualTo("燕青");
    assertThat(yan.row()).isEqualTo("MIDDLE");

    // 驗證成員 4: 凌霜 (後衛)
    var ling = state.party().members().get(3);
    assertThat(ling.name()).isEqualTo("凌霜");
    assertThat(ling.row()).isEqualTo("BACK");
  }
}
