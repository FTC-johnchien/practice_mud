package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.htmlmud.domain.dungeon.dto.DrpgStateDto;
import com.example.htmlmud.domain.dungeon.model.Direction;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;

@SpringBootTest
class TaiyinTombDungeonTest {

  @Test
  @DisplayName("驗證太陰地宮 (taiyin_tomb) 專屬 JSON 資料庫成功加載至 TemplateRepository")
  void testTaiyinTombDataLoaded() {
    // 1. 驗證 Zone
    assertTrue(TemplateRepository.findZone("taiyin_tomb").isPresent(), "taiyin_tomb 應成功註冊");

    // 2. 驗證 Items
    var pill = TemplateRepository.findItem("taiyin_tomb:taiyin_pill");
    assertTrue(pill.isPresent(), "太陰培元丹 應存在");
    assertThat(pill.get().name()).isEqualTo("太陰培元丹");

    var talisman = TemplateRepository.findItem("taiyin_tomb:purify_talisman");
    assertTrue(talisman.isPresent(), "辟邪清心符 應存在");

    var sword = TemplateRepository.findItem("taiyin_tomb:bronze_sword");
    assertTrue(sword.isPresent(), "鏽蝕青銅古劍 應存在");

    // 3. 驗證 Mobs
    var doll = TemplateRepository.findMob("taiyin_tomb:corpse_doll");
    assertTrue(doll.isPresent(), "腐化屍偶 應存在");
    assertThat(doll.get().name()).isEqualTo("腐化屍偶");

    var guard = TemplateRepository.findMob("taiyin_tomb:tomb_guard");
    assertTrue(guard.isPresent(), "異化守墓道奴 應存在");

    // 4. 驗證 Rooms
    assertTrue(TemplateRepository.findRoom("taiyin_tomb:entrance").isPresent(), "地宮入口應存在");
    assertTrue(TemplateRepository.findRoom("taiyin_tomb:hall").isPresent(), "太陰古殿應存在");
  }

  @Test
  @DisplayName("驗證地牢遇敵警戒度 (Danger Gauge) 累加與狀態流轉")
  void testDangerGaugeAccumulation() {
    DungeonPosition pos = new DungeonPosition("taiyin_tomb_b1f", 1, 1, Direction.NORTH, 10, 10);
    assertThat(pos.getDangerLevel()).isEqualTo(0);
    assertThat(pos.getDangerStatus()).isEqualTo("CALM");

    pos.addDanger(45);
    assertThat(pos.getDangerLevel()).isEqualTo(45);
    assertThat(pos.getDangerStatus()).isEqualTo("CAUTION");

    pos.addDanger(30);
    assertThat(pos.getDangerLevel()).isEqualTo(75);
    assertThat(pos.getDangerStatus()).isEqualTo("DANGER");

    pos.addDanger(20);
    assertThat(pos.getDangerLevel()).isEqualTo(95);
    assertThat(pos.getDangerStatus()).isEqualTo("ENCOUNTER");

    pos.resetDanger();
    assertThat(pos.getDangerLevel()).isEqualTo(0);
    assertThat(pos.getDangerStatus()).isEqualTo("CALM");
  }

  @Test
  @DisplayName("驗證開箱前後雷達圖標切換 (◆ -> ◇) 與陷阱傷害/道心侵蝕")
  void testChestAndTrapMechanics() {
    DungeonManager dungeonManager = new DungeonManager();
    dungeonManager.init();
    PartyService partyService = new PartyService();
    partyService.initDefaultFormations();

    DungeonFloor floor = dungeonManager.getFloor("taiyin_tomb_b1f");
    DungeonPosition pos = new DungeonPosition(floor.getId(), 3, 5, Direction.NORTH, 10, 10);
    Party party = partyService.createInitialParty("道初真人");

    // 未開箱時，(3, 5) 即 tiles[5][3] 應為 $
    DrpgStateDto before = DrpgStateDto.of(floor, pos, "", party);
    assertThat(before.dungeon().tiles()[5][3]).isEqualTo("$");
    assertThat(before.dungeon().openedChests()[5][3]).isFalse();

    // 標記開箱後，(3, 5) 應轉換為已開啟的空箱 ◇
    pos.markChestOpened(3, 5);
    DrpgStateDto after = DrpgStateDto.of(floor, pos, "", party);
    assertThat(after.dungeon().tiles()[5][3]).isEqualTo("◇");
    assertThat(after.dungeon().openedChests()[5][3]).isTrue();

    // 陷阱測試：扣血 15 與 扣 SAN 5
    PartyMember leader = party.getMembers().get(0);
    int initialHp = leader.getStats().getHp();
    int initialSan = leader.getCurrentSan();

    leader.takeDamage(15);
    leader.consumeSan(5);

    assertThat(leader.getStats().getHp()).isEqualTo(initialHp - 15);
    assertThat(leader.getCurrentSan()).isEqualTo(initialSan - 5);
  }
}
