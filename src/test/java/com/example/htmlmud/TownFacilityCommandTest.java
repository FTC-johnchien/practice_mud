package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.htmlmud.application.command.impl.TownFacilityCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.PartyService;
import com.example.htmlmud.town.CognitiveProxyService;
import com.example.htmlmud.town.TownHubService;
import com.example.htmlmud.pipeline.GameStateBroadcastService;
import com.fasterxml.jackson.databind.ObjectMapper;

class TownFacilityCommandTest {

  private TownFacilityCommand townFacilityCommand;
  private TownHubService townHubService;
  private CognitiveProxyService cognitiveProxyService;
  private PartyService partyService;
  private GameStateBroadcastService broadcastService;
  private List<String> receivedReplies;
  private List<Object> receivedJsons;

  @BeforeEach
  void setUp() {
    cognitiveProxyService = new CognitiveProxyService();
    townHubService = new TownHubService(cognitiveProxyService, new ObjectMapper());
    townHubService.init();

    partyService = mock(PartyService.class);
    broadcastService = mock(GameStateBroadcastService.class);

    receivedReplies = new ArrayList<>();
    receivedJsons = new ArrayList<>();

    townFacilityCommand = new TownFacilityCommand(townHubService, partyService, broadcastService);
  }

  private Player createTestPlayer(String name, int chipRate) {
    Player player = mock(Player.class);
    org.mockito.Mockito.when(player.getName()).thenReturn(name);
    org.mockito.Mockito.when(player.getChipRate()).thenReturn(chipRate);
    org.mockito.Mockito.doAnswer(invocation -> {
      receivedReplies.add(invocation.getArgument(0));
      return null;
    }).when(player).reply(any(String.class));
    org.mockito.Mockito.doAnswer(invocation -> {
      receivedJsons.add(invocation.getArgument(0));
      return null;
    }).when(player).sendJson(any());
    return player;
  }

  @Test
  @DisplayName("town_facility 無參數：查詢營地主選單，推送 TOWN_HUB JSON 與 6 大設施仙俠文字選單")
  void testShowTownHub_at12Percent() {
    Player player = createTestPlayer("玄靈子", 12);

    ScopedValue.where(MudContext.CURRENT_PLAYER, player)
        .where(MudContext.RAW_INPUT, "town_facility")
        .run(() -> townFacilityCommand.execute(""));

    assertEquals(1, receivedReplies.size());
    String reply = receivedReplies.get(0);
    assertTrue(reply.contains("百煉堂"));
    assertTrue(reply.contains("靈台藏經閣"));
    assertTrue(reply.contains("雜役演武道場"));
    assertTrue(reply.contains("宗門外門集市"));
    assertTrue(reply.contains("回春客棧"));
    assertTrue(reply.contains("廢棄礦脈坑道"));
    assertTrue(reply.contains("愚昧求生"));

    assertEquals(1, receivedJsons.size());
    assertTrue(receivedJsons.get(0) instanceof Map);
    @SuppressWarnings("unchecked")
    Map<String, Object> json = (Map<String, Object>) receivedJsons.get(0);
    assertEquals("TOWN_HUB", json.get("type"));
  }

  @Test
  @DisplayName("camp_rest 指令：觸發全員調息完全恢復，清除精神萎靡並廣播最新狀態")
  void testCampRest_clearsMadnessAndRestoresAll() {
    Player player = createTestPlayer("玄靈子", 12);

    LivingStats stats = new LivingStats();
    stats.setMaxHp(200);
    stats.setHp(30);
    stats.setMaxMp(100);
    stats.setMp(10);

    PartyMember member = PartyMember.builder()
        .id("mem_1")
        .name("玄靈子")
        .stats(stats)
        .maxSan(100)
        .currentSan(0)
        .madnessState(PartyMember.MadnessState.CHAOS)
        .alive(true)
        .build();

    Party party = Party.builder()
        .id("party_test")
        .members(List.of(member))
        .build();

    org.mockito.Mockito.when(partyService.getOrCreateParty("玄靈子")).thenReturn(party);

    ScopedValue.where(MudContext.CURRENT_PLAYER, player)
        .where(MudContext.RAW_INPUT, "camp_rest")
        .run(() -> townFacilityCommand.execute(""));

    // 驗證數值全滿
    assertEquals(200, member.getStats().getHp());
    assertEquals(100, member.getStats().getMp());
    assertEquals(100, member.getCurrentSan());
    assertEquals(PartyMember.MadnessState.SANE, member.getMadnessState());

    // 驗證全量廣播
    verify(broadcastService).broadcastState(player);

    // 驗證 CAMP_REST_RESULT JSON
    assertEquals(1, receivedJsons.size());
    @SuppressWarnings("unchecked")
    Map<String, Object> json = (Map<String, Object>) receivedJsons.get(0);
    assertEquals("CAMP_REST_RESULT", json.get("type"));
    assertEquals(true, json.get("success"));
  }

  @Test
  @DisplayName("town_facility fac_clinic：與點擊醫館等價，同樣觸發全隊調息休整")
  void testFacilityClinic_triggersCampRest() {
    Player player = createTestPlayer("玄靈子", 12);
    LivingStats stats = new LivingStats();
    stats.setMaxHp(150);
    stats.setHp(20);

    PartyMember member = PartyMember.builder()
        .id("mem_1")
        .name("玄靈子")
        .stats(stats)
        .maxSan(100)
        .currentSan(10)
        .alive(true)
        .build();

    Party party = Party.builder().id("p").members(List.of(member)).build();
    org.mockito.Mockito.when(partyService.getOrCreateParty("玄靈子")).thenReturn(party);

    ScopedValue.where(MudContext.CURRENT_PLAYER, player)
        .where(MudContext.RAW_INPUT, "town_facility fac_clinic")
        .run(() -> townFacilityCommand.execute("fac_clinic"));

    assertEquals(150, member.getStats().getHp());
    assertEquals(100, member.getCurrentSan());
    verify(broadcastService).broadcastState(player);
  }

  @Test
  @DisplayName("town_facility fac_forge：開啟裝備工坊並發送 OPEN_FACILITY 事件")
  void testFacilityForge_opensFacility() {
    Player player = createTestPlayer("玄靈子", 12);

    ScopedValue.where(MudContext.CURRENT_PLAYER, player)
        .where(MudContext.RAW_INPUT, "town_facility fac_forge")
        .run(() -> townFacilityCommand.execute("fac_forge"));

    assertEquals(1, receivedReplies.size());
    assertTrue(receivedReplies.get(0).contains("百煉堂"));

    assertEquals(1, receivedJsons.size());
    @SuppressWarnings("unchecked")
    Map<String, Object> json = (Map<String, Object>) receivedJsons.get(0);
    assertEquals("OPEN_FACILITY", json.get("type"));
    assertEquals("fac_forge", json.get("facilityId"));
  }

  @Test
  @DisplayName("town_facility 非法設施 ID：提示友善警告")
  void testFacilityUnknown_showsWarning() {
    Player player = createTestPlayer("玄靈子", 12);

    ScopedValue.where(MudContext.CURRENT_PLAYER, player)
        .where(MudContext.RAW_INPUT, "town_facility fac_unknown")
        .run(() -> townFacilityCommand.execute("fac_unknown"));

    assertEquals(1, receivedReplies.size());
    assertTrue(receivedReplies.get(0).contains("未尋得指定設施"));
  }
}
