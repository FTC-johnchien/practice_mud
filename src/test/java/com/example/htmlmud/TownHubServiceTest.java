package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.htmlmud.domain.model.dto.FacilityDto;
import com.example.htmlmud.domain.model.dto.TownHubDto;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.PerceptionTier;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.service.CognitiveProxyService;
import com.example.htmlmud.domain.service.TownHubService;
import com.example.htmlmud.domain.service.TownHubService.CampRestResult;
import com.fasterxml.jackson.databind.ObjectMapper;

class TownHubServiceTest {

  private TownHubService townHubService;
  private CognitiveProxyService cognitiveProxyService;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    cognitiveProxyService = new CognitiveProxyService();
    objectMapper = new ObjectMapper();
    townHubService = new TownHubService(cognitiveProxyService, objectMapper);
    townHubService.init();
  }

  @Test
  @DisplayName("營地設施載入與 12% 序章靈識透鏡投影：6 大仙俠機能設施完備且無 Glitch")
  void testTownHubDto_facilitiesLoadedAndProjectedAt12Percent() {
    TownHubDto hub = townHubService.getTownHub("newbie_village", 12);
    assertNotNull(hub);
    assertEquals("newbie_village", hub.zoneId());
    assertEquals(PerceptionTier.IGNORANT_XIANXIA, hub.perceptionTier());
    assertEquals(6, hub.facilities().size());

    // 驗證六大核心設施仙俠定名
    FacilityDto forge = hub.facilities().stream().filter(f -> "fac_forge".equals(f.id())).findFirst().orElseThrow();
    assertEquals("百煉堂", forge.name());
    assertFalse(forge.glitch());

    FacilityDto chipLab = hub.facilities().stream().filter(f -> "fac_chip_lab".equals(f.id())).findFirst().orElseThrow();
    assertEquals("靈台藏經閣", chipLab.name());
    assertFalse(chipLab.glitch());

    FacilityDto dojo = hub.facilities().stream().filter(f -> "fac_dojo".equals(f.id())).findFirst().orElseThrow();
    assertEquals("雜役演武道場", dojo.name());
    assertFalse(dojo.glitch());

    FacilityDto market = hub.facilities().stream().filter(f -> "fac_market".equals(f.id())).findFirst().orElseThrow();
    assertEquals("宗門外門集市", market.name());
    assertFalse(market.glitch());

    FacilityDto clinic = hub.facilities().stream().filter(f -> "fac_clinic".equals(f.id())).findFirst().orElseThrow();
    assertEquals("回春客棧／通鋪柴房", clinic.name());
    assertFalse(clinic.glitch());

    FacilityDto portal = hub.facilities().stream().filter(f -> "fac_dungeon_portal".equals(f.id())).findFirst().orElseThrow();
    assertEquals("廢棄礦脈坑道", portal.name());
    assertFalse(portal.glitch());
  }

  @Test
  @DisplayName("當特工晶片加載度達 80% 時，設施解鎖賽博科技真相名稱與 Glitch 特效")
  void testTownHubDto_at80PercentTechAwakening() {
    TownHubDto hub = townHubService.getTownHub("newbie_village", 80);
    assertNotNull(hub);
    assertEquals(PerceptionTier.TRANSCENDED_TECH, hub.perceptionTier());

    FacilityDto forge = hub.facilities().stream().filter(f -> "fac_forge".equals(f.id())).findFirst().orElseThrow();
    assertEquals("工程師生化兵器組裝台", forge.name());
    assertTrue(forge.glitch());

    FacilityDto chipLab = hub.facilities().stream().filter(f -> "fac_chip_lab".equals(f.id())).findFirst().orElseThrow();
    assertEquals("千星科技神經元算法實驗室", chipLab.name());
    assertTrue(chipLab.glitch());

    FacilityDto market = hub.facilities().stream().filter(f -> "fac_market".equals(f.id())).findFirst().orElseThrow();
    assertEquals("黑市走私終端與地下交易所", market.name());
    assertTrue(market.glitch());
  }

  @Test
  @DisplayName("營地調息休整：全員氣血、真元、SAN 護盾回滿，清除精神萎靡狀態")
  void testPerformCampRest_healsAllMembersAndPurgesMadness() {
    LivingStats stats1 = new LivingStats();
    stats1.setMaxHp(200);
    stats1.setHp(50);
    stats1.setMaxMp(100);
    stats1.setMp(10);

    PartyMember member1 = PartyMember.builder()
        .id("mem_1")
        .name("玄靈子")
        .stats(stats1)
        .maxSan(100)
        .currentSan(0) // SAN 歸零
        .madnessState(PartyMember.MadnessState.CHAOS) // 精神萎靡
        .alive(true)
        .build();

    LivingStats stats2 = new LivingStats();
    stats2.setMaxHp(300);
    stats2.setHp(30);
    stats2.setMaxMp(50);
    stats2.setMp(0);

    PartyMember member2 = PartyMember.builder()
        .id("mem_2")
        .name("鐵牛")
        .stats(stats2)
        .maxSan(100)
        .currentSan(15)
        .madnessState(PartyMember.MadnessState.SANE)
        .alive(true)
        .build();

    Party party = Party.builder()
        .id("party_test")
        .members(List.of(member1, member2))
        .build();

    CampRestResult result = townHubService.performCampRest(party);
    assertTrue(result.success());
    assertEquals(2, result.healedMembersCount());

    // 驗證 member1 完全回復且解除 CHAOS 狀態
    assertEquals(200, member1.getStats().getHp());
    assertEquals(100, member1.getStats().getMp());
    assertEquals(100, member1.getCurrentSan());
    assertEquals(PartyMember.MadnessState.SANE, member1.getMadnessState());

    // 驗證 member2 完全回復
    assertEquals(300, member2.getStats().getHp());
    assertEquals(50, member2.getStats().getMp());
    assertEquals(100, member2.getCurrentSan());
  }
}
