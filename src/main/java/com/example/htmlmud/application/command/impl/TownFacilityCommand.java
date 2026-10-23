package com.example.htmlmud.application.command.impl;

import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.town.FacilityDto;
import com.example.htmlmud.town.TownHubDto;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.town.TownHubService;
import com.example.htmlmud.town.TownHubService.CampRestResult;
import com.example.htmlmud.domain.world.GameStateBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 營地機能設施與調息派發指令 (Town Facility & Camp Rest Command)
 * 核心職責：收斂前端點擊營地設施發送的 TOWN_FACILITY 與 CAMP_REST 動作，
 * 直接委派給 TownHubService 處理，徹底擺脫舊 MUD 房間 exits 拓撲與分散指令。
 * 對齊 Level 2《M1_TOWN_HUB_AND_FACILITIES.md》與 Milestone CONV-01 Task 3。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@CommandAlias({"town", "hub", "facility", "town_hub", "camp_facility", "camp_rest"})
public class TownFacilityCommand implements PlayerCommand {

  private final TownHubService townHubService;
  private final PartyService partyService;
  private final GameStateBroadcastService broadcastService;

  @Override
  public String getKey() {
    return "town_facility";
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();
    if (player == null) {
      return;
    }

    String rawInput = MudContext.RAW_INPUT.get();
    String normalized = (rawInput != null) ? rawInput.trim().toLowerCase() : "";
    String trimmedArgs = (args != null) ? args.trim() : "";

    // 1. 若為調息指令 (CAMP_REST 或 town_facility rest/clinic/fac_clinic)
    if (normalized.startsWith("camp_rest")
        || trimmedArgs.equalsIgnoreCase("rest")
        || trimmedArgs.equalsIgnoreCase("clinic")
        || trimmedArgs.equalsIgnoreCase("fac_clinic")) {
      handleCampRest(player);
      return;
    }

    int chipRate = player.getChipRate();
    TownHubDto hub = townHubService.getTownHub(chipRate);

    // 2. 若無參數或查詢營地主選單 (town_facility / town / hub)
    if (trimmedArgs.isEmpty() || trimmedArgs.equalsIgnoreCase("hub") || trimmedArgs.equalsIgnoreCase("list")) {
      handleShowHub(player, hub);
      return;
    }

    // 3. 處理特定設施互動 (town_facility <facility_id>)
    handleFacilityInteraction(player, trimmedArgs, hub);
  }

  private void handleCampRest(Player player) {
    Party party = partyService.getOrCreateParty(player.getName());
    CampRestResult result = townHubService.performCampRest(party);

    player.reply(result.message());
    player.sendJson(Map.of(
        "type", "CAMP_REST_RESULT",
        "success", result.success(),
        "healedCount", result.healedMembersCount(),
        "message", result.message()
    ));

    // 全量廣播刷新狀態 (HP/MP/SAN)
    broadcastService.broadcastState(player);
  }

  private void handleShowHub(Player player, TownHubDto hub) {
    // 推送結構化 JSON 至前端
    player.sendJson(Map.of("type", "TOWN_HUB", "data", hub));

    StringBuilder sb = new StringBuilder();
    sb.append("🏮【").append(hub.zoneName()).append("】\n");
    sb.append("說明：").append(hub.description()).append("\n");
    sb.append("特工晶片加載度：").append(hub.chipRate()).append("%（").append(hub.perceptionTier().getDisplayName()).append("）\n");
    sb.append("──────────────────────\n");
    sb.append("營地機能設施列表：\n");

    for (FacilityDto fac : hub.facilities()) {
      String glitchMark = fac.glitch() ? " [⚠️異象]" : "";
      sb.append("• [").append(fac.id()).append("] ").append(fac.name()).append(glitchMark)
        .append(" - ").append(fac.description()).append("\n");
    }
    sb.append("──────────────────────\n");
    sb.append("💡 輸入 'town_facility <設施ID>' 進入設施，或 'camp_rest' 進行全員調息。");

    player.reply(sb.toString());
  }

  private void handleFacilityInteraction(Player player, String facilityKey, TownHubDto hub) {
    Optional<FacilityDto> matched = hub.facilities().stream()
        .filter(f -> f.id().equalsIgnoreCase(facilityKey)
            || f.id().replace("fac_", "").equalsIgnoreCase(facilityKey)
            || f.facilityType().equalsIgnoreCase(facilityKey))
        .findFirst();

    if (matched.isEmpty()) {
      player.reply("營地中未尋得指定設施: '" + facilityKey + "'。輸入 'town_facility' 查看設施清單。");
      return;
    }

    FacilityDto fac = matched.get();

    // 設施分發
    switch (fac.id()) {
      case "fac_clinic" -> handleCampRest(player);
      case "fac_forge" -> {
        player.reply("🔨 你踏入了【" + fac.name() + "】，四周傳來生化金屬冷卻與靈紋鍛造的陣陣轟鳴。");
        player.sendJson(Map.of("type", "OPEN_FACILITY", "facilityId", fac.id(), "facilityName", fac.name()));
      }
      case "fac_chip_lab" -> {
        player.reply("🔬 你進入了【" + fac.name() + "】，神經光纖與道紋玉簡在暗處交織閃爍。");
        player.sendJson(Map.of("type", "OPEN_FACILITY", "facilityId", fac.id(), "facilityName", fac.name()));
      }
      case "fac_dojo" -> {
        player.reply("🥋 你來到了【" + fac.name() + "】，演武場中氣浪滾滾，可在此研習神通戰技。");
        player.sendJson(Map.of("type", "OPEN_FACILITY", "facilityId", fac.id(), "facilityName", fac.name()));
      }
      case "fac_market" -> {
        player.reply("🛒 你步入了【" + fac.name() + "】，行商與採礦散修在此暗中交換物資。");
        player.sendJson(Map.of("type", "OPEN_FACILITY", "facilityId", fac.id(), "facilityName", fac.name()));
      }
      case "fac_dungeon_portal" -> {
        player.reply("🌀 你抵達了【" + fac.name() + "】，通往深不見底的黑煞宗地底礦脈...");
        player.sendJson(Map.of(
            "type", "OPEN_FACILITY",
            "facilityId", fac.id(),
            "facilityName", fac.name(),
            "targetDungeons", fac.targetDungeons() != null ? fac.targetDungeons() : java.util.List.of()
        ));
      }
      default -> {
        player.reply("你造訪了【" + fac.name() + "】。");
        player.sendJson(Map.of("type", "OPEN_FACILITY", "facilityId", fac.id(), "facilityName", fac.name()));
      }
    }
  }

  @Override
  public String getDescription() {
    return "營地機能設施中樞與休整調息 (town_facility [設施ID] | camp_rest)";
  }
}
