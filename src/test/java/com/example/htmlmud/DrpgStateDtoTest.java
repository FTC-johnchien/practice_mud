package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.htmlmud.dungeon.DrpgStateDto;
import com.example.htmlmud.dungeon.Direction;
import com.example.htmlmud.dungeon.DungeonFloor;
import com.example.htmlmud.dungeon.DungeonPosition;
import com.example.htmlmud.dungeon.DungeonTile;
import com.example.htmlmud.dungeon.GridCoord;
import com.example.htmlmud.dungeon.DungeonManager;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyService;

class DrpgStateDtoTest {

  @Test
  @DisplayName("測試 DrpgStateDto 正確將地牢 10x10 雷達與 6人小隊資料打包成結構化封包")
  void testDrpgStateDtoSerialization() {
    DungeonManager dungeonManager = new DungeonManager();
    dungeonManager.init();

    PartyService partyService = new PartyService(new com.example.htmlmud.template.TemplateCatalog());
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

  @Test
  @DisplayName("ALIGN2-01: 測試 PartyItemSlotViewDto 投影正確包含 poiseBreakPower、affixes、含詞綴全額攻防血理智與背包預設 40 格")
  void testPartyItemSlotAndInventoryViewDtoProjection() {
    com.example.htmlmud.party.PartyItemSlot slot = com.example.htmlmud.party.PartyItemSlot.builder()
        .slotId("slot_001")
        .itemId("frost_blade")
        .name("寒霜靈刃")
        .itemType(com.example.htmlmud.domain.model.enums.ItemType.WEAPON)
        .equipSlot(com.example.htmlmud.item.EquipmentSlot.MAIN_HAND)
        .count(1)
        .bonusMinDamage(10)
        .bonusMaxDamage(20)
        .bonusDefense(5)
        .bonusHp(50)
        .bonusSan(15)
        .poiseBreakPower(25)
        .build();

    com.example.htmlmud.item.ItemAffix prefix = com.example.htmlmud.item.ItemAffix.builder()
        .id("affix_pre_1")
        .type(com.example.htmlmud.item.ItemAffix.AffixType.PREFIX)
        .name("超頻之")
        .statKey("bonusHp")
        .value(30.0)
        .build();
    com.example.htmlmud.item.ItemAffix suffix = com.example.htmlmud.item.ItemAffix.builder()
        .id("affix_suf_1")
        .type(com.example.htmlmud.item.ItemAffix.AffixType.SUFFIX)
        .name("·破陣")
        .statKey("poiseBreakPower")
        .value(15.0)
        .build();
    slot.addAffix(prefix);
    slot.addAffix(suffix);

    var dto = DrpgStateDto.PartyItemSlotViewDto.of(slot);
    assertThat(dto).isNotNull();
    assertThat(dto.slotId()).isEqualTo("slot_001");
    assertThat(dto.name()).isEqualTo("超頻之寒霜靈刃·破陣");
    assertThat(dto.bonusMinDamage()).isEqualTo(10);
    assertThat(dto.bonusMaxDamage()).isEqualTo(20);
    assertThat(dto.bonusDefense()).isEqualTo(5);
    assertThat(dto.bonusHp()).isEqualTo(80); // 50 + 30
    assertThat(dto.bonusSan()).isEqualTo(15);
    assertThat(dto.poiseBreakPower()).isEqualTo(40); // 25 + 15
    assertThat(dto.affixes()).hasSize(2);
    assertThat(dto.affixes().get(0).name()).isEqualTo("超頻之");
    assertThat(dto.affixes().get(1).name()).isEqualTo("·破陣");

    // 驗證空背包 fallback 容量對齊 40 格規範
    var emptyInvDto = DrpgStateDto.PartyInventoryViewDto.of(null);
    assertThat(emptyInvDto.capacity()).isEqualTo(40);
    assertThat(emptyInvDto.usedCount()).isEqualTo(0);
    assertThat(emptyInvDto.slots()).isEmpty();
  }

  @Test
  @DisplayName("ALIGN2-06: 測試 DrpgStateDto.ofTown 投影正確包含 TownHubDto 六大設施，且隨 chipRate 反映三階認知透鏡與 glitch 狀態")
  void testTownViewDtoHubProjection() {
    com.example.htmlmud.town.CognitiveProxyService proxyService = new com.example.htmlmud.town.CognitiveProxyService();
    com.example.htmlmud.town.TownHubService hubService = new com.example.htmlmud.town.TownHubService(proxyService, new com.fasterxml.jackson.databind.ObjectMapper());
    hubService.init();

    // 1. chipRate = 12% (第一階：純仙俠表象)
    com.example.htmlmud.town.TownHubDto hubLow = hubService.getTownHub("newbie_village", 12);
    assertThat(hubLow.facilities()).hasSize(6);
    assertThat(hubLow.perceptionTier()).isEqualTo(com.example.htmlmud.town.PerceptionTier.IGNORANT_XIANXIA);
    assertThat(hubLow.facilities().stream().anyMatch(f -> "fac_forge".equals(f.id()) && f.name().equals("百煉堂"))).isTrue();
    assertThat(hubLow.facilities().stream().allMatch(f -> !f.glitch())).isTrue();

    DrpgStateDto stateLow = DrpgStateDto.ofTown(
        "newbie_village", "新手仙鄉", "newbie_village:inn", "客棧", "溫馨客棧", true,
        java.util.List.of(), java.util.List.of(), java.util.List.of(), null, null, null, hubLow
    );
    assertThat(stateLow.town().hub()).isNotNull();
    assertThat(stateLow.town().hub().facilities()).hasSize(6);
    assertThat(stateLow.town().hub().chipRate()).isEqualTo(12);

    // 2. chipRate = 45% (第二階：異象微顯)
    com.example.htmlmud.town.TownHubDto hubMid = hubService.getTownHub("newbie_village", 45);
    assertThat(hubMid.perceptionTier()).isEqualTo(com.example.htmlmud.town.PerceptionTier.FRACTURED_GLITCH);
    assertThat(hubMid.facilities().stream().anyMatch(com.example.htmlmud.town.FacilityDto::glitch)).isTrue();

    // 3. chipRate = 80% (第三階：科技覺醒 / 殘酷真相)
    com.example.htmlmud.town.TownHubDto hubHigh = hubService.getTownHub("newbie_village", 80);
    assertThat(hubHigh.perceptionTier()).isEqualTo(com.example.htmlmud.town.PerceptionTier.TRANSCENDED_TECH);
    assertThat(hubHigh.facilities().stream().anyMatch(f -> "fac_forge".equals(f.id()) && f.name().contains("千星重工"))).isTrue();

    // 4. 驗證查無 zoneId (例如地牢撤離之 mozhu_mines) 時自動回退 newbie_village
    com.example.htmlmud.town.TownHubDto hubFallback = hubService.getTownHub("mozhu_mines", 12);
    assertThat(hubFallback.facilities()).hasSize(6);
  }
}
